"""
Módulo de seguridad, control de licencias, sanitización y detección de duplicados.

Aplica las reglas de la Fase 10:
- Evita prompt injection o ejecución de código en contenido externo.
- Valida URLs contra SSRF y exige HTTPS.
- Hace cumplir políticas de licencia (almacenamiento vs metadata vs enlace externo).
- Protege el currículo interno contra mutaciones desde proveedores externos.
"""

from __future__ import annotations

import hashlib
import html
import ipaddress
import re
import socket
from dataclasses import dataclass
from urllib.parse import urlparse

_HTML_TAG_RE = re.compile(r"<[^>]+>")
_SCRIPT_BLOCK_RE = re.compile(r"<\s*(script|style|iframe|object|embed)[^>]*>.*?<\s*/\s*\1\s*>", re.IGNORECASE | re.DOTALL)
_CONTROL_CHARS_RE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
_MULTI_WS_RE = re.compile(r"[ \t]+")

_INJECTION_PATTERNS = [
    re.compile(r"ignore\s+(?:all\s+|any\s+)?previous\s+instructions", re.IGNORECASE),
    re.compile(r"disregard\s+(?:the\s+)?system\s+prompt", re.IGNORECASE),
    re.compile(r"you\s+are\s+now\s+(?:a|an|in)\s+developer\s+mode", re.IGNORECASE),
    re.compile(r"<\|(?:im_start|im_end|system|assistant|user)\|>", re.IGNORECASE),
    re.compile(r"^\s*###\s*(?:system|instruction)\s*:", re.IGNORECASE | re.MULTILINE),
    re.compile(r"javascript\s*:", re.IGNORECASE),
    re.compile(r"data\s*:\s*text/html", re.IGNORECASE),
]

STANDARD_LICENSES: dict[str, dict] = {
    "FLUENTVOICE-OWNED": {
        "id": "FLUENTVOICE-OWNED",
        "spdx_code": "LicenseRef-FluentVoice-Owned",
        "name": "Contenido educativo propio FluentVoice",
        "url": "https://fluentvoice.local/licenses/owned",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 0,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Material pedagógico original creado internamente para FluentVoice.",
    },
    "CC0-1.0": {
        "id": "CC0-1.0",
        "spdx_code": "CC0-1.0",
        "name": "Creative Commons Zero 1.0 Universal (Public Domain)",
        "url": "https://creativecommons.org/publicdomain/zero/1.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 0,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Dedicación a dominio público (p. ej. Mozilla Common Voice datasets).",
    },
    "PDM-1.0": {
        "id": "PDM-1.0",
        "spdx_code": "PDM-1.0",
        "name": "Public Domain Mark 1.0",
        "url": "https://creativecommons.org/publicdomain/mark/1.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Obras de dominio público (p. ej. Project Gutenberg US y audiolibros LibriVox).",
    },
    "CC-BY-2.0-FR": {
        "id": "CC-BY-2.0-FR",
        "spdx_code": "CC-BY-2.0",
        "name": "Creative Commons Attribution 2.0 France",
        "url": "https://creativecommons.org/licenses/by/2.0/fr/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Requerida atribución a Tatoeba y sus contribuyentes.",
    },
    "CC-BY-3.0": {
        "id": "CC-BY-3.0",
        "spdx_code": "CC-BY-3.0",
        "name": "Creative Commons Attribution 3.0 Unported",
        "url": "https://creativecommons.org/licenses/by/3.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Requiere atribución explícita al autor original.",
    },
    "CC-BY-4.0": {
        "id": "CC-BY-4.0",
        "spdx_code": "CC-BY-4.0",
        "name": "Creative Commons Attribution 4.0 International",
        "url": "https://creativecommons.org/licenses/by/4.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "store_full",
        "notes": "Uso abierto con atribución al autor y enlace a licencia.",
    },
    "CC-BY-SA-3.0": {
        "id": "CC-BY-SA-3.0",
        "spdx_code": "CC-BY-SA-3.0",
        "name": "Creative Commons Attribution-ShareAlike 3.0",
        "url": "https://creativecommons.org/licenses/by-sa/3.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 1,
        "storage_policy": "store_full",
        "notes": "Requiere atribución y mantener licencia equivalente en derivados.",
    },
    "CC-BY-SA-4.0": {
        "id": "CC-BY-SA-4.0",
        "spdx_code": "CC-BY-SA-4.0",
        "name": "Creative Commons Attribution-ShareAlike 4.0 International",
        "url": "https://creativecommons.org/licenses/by-sa/4.0/",
        "allows_storage": 1,
        "allows_commercial": 1,
        "requires_attribution": 1,
        "requires_share_alike": 1,
        "storage_policy": "store_full",
        "notes": "Utilizada por Simple English Wikipedia, Wiktionary y NGSL/NAWL.",
    },
    "CC-BY-NC-SA-4.0": {
        "id": "CC-BY-NC-SA-4.0",
        "spdx_code": "CC-BY-NC-SA-4.0",
        "name": "Creative Commons Attribution-NonCommercial-ShareAlike 4.0",
        "url": "https://creativecommons.org/licenses/by-nc-sa/4.0/",
        "allows_storage": 1,
        "allows_commercial": 0,
        "requires_attribution": 1,
        "requires_share_alike": 1,
        "storage_policy": "store_metadata_only",
        "notes": "Solo uso educativo no comercial; FluentVoice almacena ficha, metadatos y enlace canónico a OpenLearn.",
    },
    "FREE-API-NONCOMM": {
        "id": "FREE-API-NONCOMM",
        "spdx_code": "LicenseRef-Free-API-NonCommercial",
        "name": "Free Educational API (Límite de cuota y uso justo)",
        "url": "https://www.datamuse.com/api/",
        "allows_storage": 1,
        "allows_commercial": 0,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "store_metadata_only",
        "notes": "Respuestas léxicas efímeras o en caché local respetando límites diarios (Datamuse 100k req/día).",
    },
    "ALL-RIGHTS-RESERVED-LINK-ONLY": {
        "id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
        "spdx_code": "LicenseRef-AllRightsReserved-LinkOnly",
        "name": "Todos los derechos reservados — Solo enlace externo autorizado",
        "url": "https://learnenglish.britishcouncil.org/",
        "allows_storage": 0,
        "allows_commercial": 0,
        "requires_attribution": 1,
        "requires_share_alike": 0,
        "storage_policy": "link_only",
        "notes": "Prohibido copiar o almacenar el contenido del sitio; únicamente se almacena referencia descriptiva y URL oficial.",
    },
}


def has_prompt_injection(text: str | None) -> bool:
    if not text:
        return False
    return any(p.search(text) for p in _INJECTION_PATTERNS)


def sanitize_external_text(text: str | None, max_len: int = 4000, reject_injection: bool = False) -> str:
    """Limpia HTML, scripts, caracteres de control y opcionalmente rechaza prompt injection."""
    if not text:
        return ""
    if reject_injection and has_prompt_injection(text):
        raise ValueError("Contenido externo rechazado por patrón sospechoso de inyección o script.")
    cleaned = _SCRIPT_BLOCK_RE.sub(" ", text)
    cleaned = _HTML_TAG_RE.sub(" ", cleaned)
    cleaned = html.unescape(cleaned)
    cleaned = _CONTROL_CHARS_RE.sub("", cleaned)
    for pattern in _INJECTION_PATTERNS:
        cleaned = pattern.sub("[contenido filtrado]", cleaned)
    cleaned = _MULTI_WS_RE.sub(" ", cleaned).strip()
    if len(cleaned) > max_len:
        cleaned = cleaned[: max_len - 3].rstrip() + "..."
    return cleaned


def validate_external_url(
    url: str | None,
    allowed_domains: tuple[str, ...] | None = None,
    resolve_dns: bool = False,
) -> tuple[bool, str]:
    """Valida que la URL externa sea HTTPS segura y no apunte a direcciones privadas (SSRF)."""
    if not url or not isinstance(url, str):
        return False, "URL vacía"
    raw = url.strip()
    parsed = urlparse(raw)
    if parsed.scheme.lower() != "https":
        return False, f"Esquema no permitido ({parsed.scheme}); se requiere HTTPS"
    host = (parsed.hostname or "").lower().strip(".")
    if not host or "." not in host:
        return False, "Host inválido"
    if host in {"localhost", "127.0.0.1", "0.0.0.0", "::1"} or host.endswith(".local") or host.endswith(".internal"):
        return False, f"Host interno bloqueado: {host}"

    # Verificar si es una IP literal privada/reservada
    try:
        ip = ipaddress.ip_address(host)
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast:
            return False, f"IP privada o reservada bloqueada: {host}"
    except ValueError:
        pass

    if allowed_domains:
        matched = any(host == d or host.endswith(f".{d}") for d in allowed_domains)
        if not matched:
            return False, f"Dominio {host} fuera de lista autorizada {allowed_domains}"

    if resolve_dns:
        try:
            infos = socket.getaddrinfo(host, None)
            for info in infos:
                addr = info[4][0]
                ip = ipaddress.ip_address(addr.split("%")[0])
                if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
                    return False, f"DNS resolvió a IP privada bloqueada: {addr}"
        except OSError:
            return False, f"No se pudo resolver host: {host}"

    return True, "ok"


def normalize_license_code(raw_name: str | None) -> str | None:
    """Normaliza cadenas de licencia devueltas por APIs externas (p. ej. Wikimedia Commons)."""
    if not raw_name:
        return None
    norm = raw_name.strip().upper()
    mapping = {
        "CC0": "CC0-1.0",
        "CC0 1.0": "CC0-1.0",
        "CC-ZERO": "CC0-1.0",
        "PUBLIC DOMAIN": "PDM-1.0",
        "PD": "PDM-1.0",
        "PDM": "PDM-1.0",
        "PD-OLD": "PDM-1.0",
        "PD-US": "PDM-1.0",
        "CC BY 2.0": "CC-BY-2.0-FR",
        "CC BY 2.0 FR": "CC-BY-2.0-FR",
        "CC-BY-2.0": "CC-BY-2.0-FR",
        "CC BY 3.0": "CC-BY-3.0",
        "CC-BY-3.0": "CC-BY-3.0",
        "CC BY 4.0": "CC-BY-4.0",
        "CC-BY-4.0": "CC-BY-4.0",
        "CC BY-SA 3.0": "CC-BY-SA-3.0",
        "CC-BY-SA-3.0": "CC-BY-SA-3.0",
        "CC BY-SA 4.0": "CC-BY-SA-4.0",
        "CC-BY-SA-4.0": "CC-BY-SA-4.0",
        "CC BY-NC-SA 4.0": "CC-BY-NC-SA-4.0",
        "CC-BY-NC-SA-4.0": "CC-BY-NC-SA-4.0",
    }
    if norm in STANDARD_LICENSES:
        return norm
    for prefix, target in mapping.items():
        if norm == prefix or norm.startswith(prefix):
            return target
    if "PUBLIC DOMAIN" in norm or "NO KNOWN COPYRIGHT" in norm:
        return "PDM-1.0"
    if "CC-BY-SA 4.0" in norm or "BY-SA 4.0" in norm:
        return "CC-BY-SA-4.0"
    if "CC-BY-SA 3.0" in norm or "BY-SA 3.0" in norm:
        return "CC-BY-SA-3.0"
    if "CC-BY-4.0" in norm or "CC BY 4.0" in norm:
        return "CC-BY-4.0"
    if "CC-BY-3.0" in norm or "CC BY 3.0" in norm:
        return "CC-BY-3.0"
    return None


@dataclass(frozen=True)
class LicenseDecision:
    allowed: bool
    license_id: str | None
    storage_policy: str
    can_store_full_body: bool
    requires_attribution: bool
    reason: str


class LicensePolicyEngine:
    """Evalúa qué puede almacenarse o solo enlazarse según la licencia declarada."""

    def __init__(self, licenses: dict[str, dict] | None = None) -> None:
        self.licenses = licenses or STANDARD_LICENSES

    def evaluate(
        self,
        license_id: str | None,
        requested_access_mode: str,
        has_full_body_copy: bool,
        attribution_text: str | None,
    ) -> LicenseDecision:
        if not license_id or license_id not in self.licenses:
            return LicenseDecision(
                allowed=False,
                license_id=license_id,
                storage_policy="blocked",
                can_store_full_body=False,
                requires_attribution=True,
                reason=f"Licencia desconocida o no verificada: {license_id}",
            )

        lic = self.licenses[license_id]
        policy = lic["storage_policy"]
        requires_attr = bool(lic["requires_attribution"])

        if policy == "blocked":
            return LicenseDecision(
                allowed=False,
                license_id=license_id,
                storage_policy=policy,
                can_store_full_body=False,
                requires_attribution=requires_attr,
                reason=f"La licencia {license_id} bloquea su incorporación.",
            )

        if requires_attr and (not attribution_text or not attribution_text.strip()):
            return LicenseDecision(
                allowed=False,
                license_id=license_id,
                storage_policy=policy,
                can_store_full_body=False,
                requires_attribution=True,
                reason=f"Falta atribución obligatoria para licencia {license_id}.",
            )

        if policy == "link_only":
            if has_full_body_copy or requested_access_mode == "open_reusable":
                return LicenseDecision(
                    allowed=False,
                    license_id=license_id,
                    storage_policy=policy,
                    can_store_full_body=False,
                    requires_attribution=requires_attr,
                    reason=(
                        f"Licencia {license_id} solo permite enlace externo y ficha descriptiva; "
                        "se prohíbe copiar o almacenar texto completo."
                    ),
                )
            return LicenseDecision(
                allowed=True,
                license_id=license_id,
                storage_policy=policy,
                can_store_full_body=False,
                requires_attribution=requires_attr,
                reason="Permitido como enlace externo autorizado.",
            )

        if policy == "store_metadata_only":
            if has_full_body_copy:
                return LicenseDecision(
                    allowed=False,
                    license_id=license_id,
                    storage_policy=policy,
                    can_store_full_body=False,
                    requires_attribution=requires_attr,
                    reason=(
                        f"Licencia {license_id} permite solo metadatos/referencia o caché efímera API, "
                        "no copiar obras completas."
                    ),
                )
            return LicenseDecision(
                allowed=True,
                license_id=license_id,
                storage_policy=policy,
                can_store_full_body=False,
                requires_attribution=requires_attr,
                reason="Permitido como metadato/consulta API con atribución.",
            )

        return LicenseDecision(
            allowed=True,
            license_id=license_id,
            storage_policy=policy,
            can_store_full_body=True,
            requires_attribution=requires_attr,
            reason="Licencia abierta compatible para almacenamiento.",
        )


def compute_content_hash(provider_id: str, stable_key: str, text: str = "") -> str:
    """Calcula huella SHA-256 estable para evitar duplicados."""
    normalized = re.sub(r"\s+", " ", f"{provider_id}|{stable_key}|{text.strip().lower()}").strip()
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def assert_external_cannot_mutate_core(source_type: str, target_source_type: str | None = None) -> None:
    """Impide que procesos externos sobrescriban o modifiquen material propio."""
    if source_type != "owned" and target_source_type == "owned":
        raise PermissionError("Un recurso externo no puede modificar ni sobrescribir contenido propio.")
