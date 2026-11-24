"""
Adaptadores modulares de fuentes educativas externas abiertas y APIs gratuitas (Fase 4).

Clasificación por modo de integración:
- API REST pública: Tatoeba, Gutendex (Project Gutenberg), Wikimedia Commons,
  Simple English Wikipedia, LibriVox, Datamuse.
- Dataset descargable (CC0): Mozilla Common Voice (importador TSV local / MDC env).
- Enlace externo autorizado: British Council LearnEnglish, OpenLearn (OER).
"""

from __future__ import annotations

import csv
import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any
from urllib.parse import quote

import httpx

from edu_db.security import normalize_license_code, sanitize_external_text

DEFAULT_USER_AGENT = "FluentVoiceEdu/2.0 (Educational-App; https://fluentvoice.local/)"


@dataclass
class NormalizedExternalItem:
    provider_id: str
    external_id: str
    title: str
    title_es: str
    description: str
    body_en: str
    original_url: str
    content_type: str
    access_mode: str            # open_reusable | external_link | live_api
    license_id: str | None
    author: str
    attribution_text: str
    has_full_body_copy: bool = False
    cefr_hint: str | None = None
    category_hint: str | None = None
    skill_hint: str | None = None
    estimated_duration_min: int = 5
    offline_compatible: int = 0
    payload: dict[str, Any] = field(default_factory=dict)


class BaseProviderAdapter:
    provider_id: str = ""
    allowed_domains: tuple[str, ...] = ()

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 10) -> list[NormalizedExternalItem]:
        raise NotImplementedError


class TatoebaAdapter(BaseProviderAdapter):
    provider_id = "tatoeba"
    allowed_domains = ("tatoeba.org", "api.tatoeba.org")
    SEED_QUERIES = [
        ("coffee", "A1", "everyday_english"),
        ("airport", "A2", "everyday_english"),
        ("interview", "B1", "professional_english"),
        ("although", "B1", "grammar"),
        ("perspective", "B2", "academic_english"),
        ("negotiate", "C1", "professional_english"),
    ]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 12) -> list[NormalizedExternalItem]:
        items: list[NormalizedExternalItem] = []
        per_query = max(2, min(4, (limit + len(self.SEED_QUERIES) - 1) // len(self.SEED_QUERIES)))
        for query, cefr_hint, skill_hint in self.SEED_QUERIES:
            if len(items) >= limit:
                break
            url = "https://tatoeba.org/en/api_v0/search"
            params = {
                "from": "eng",
                "to": "spa",
                "query": query,
                "trans_filter": "limit",
                "trans_to": "spa",
                "trans_link": "direct",
                "sort": "relevance",
            }
            resp = await http.get(url, params=params, headers={"User-Agent": DEFAULT_USER_AGENT})
            resp.raise_for_status()
            data = resp.json()
            results = data.get("results") or []
            added_for_q = 0
            for row in results:
                if len(items) >= limit or added_for_q >= per_query:
                    break
                sid = row.get("id")
                en_text = sanitize_external_text(row.get("text"), max_len=350)
                if not sid or not en_text or len(en_text.split()) < 3:
                    continue
                owner = sanitize_external_text(row.get("user", {}).get("username") if isinstance(row.get("user"), dict) else None, max_len=80) or "Tatoeba contributor"
                trans_groups = row.get("translations") or []
                spa_text = ""
                for group in trans_groups:
                    if isinstance(group, list):
                        for t in group:
                            if isinstance(t, dict) and t.get("text"):
                                spa_text = sanitize_external_text(t.get("text"), max_len=350)
                                break
                    elif isinstance(group, dict) and group.get("text"):
                        spa_text = sanitize_external_text(group.get("text"), max_len=350)
                    if spa_text:
                        break
                if not spa_text:
                    continue
                sent_url = f"https://tatoeba.org/en/sentences/show/{sid}"
                lic = normalize_license_code(row.get("license")) or "CC-BY-2.0-FR"
                items.append(
                    NormalizedExternalItem(
                        provider_id=self.provider_id,
                        external_id=f"sent_{sid}",
                        title=en_text,
                        title_es=f"Tatoeba: {en_text[:60]}",
                        description=f"Oración bilingüe real: «{spa_text}»",
                        body_en=en_text,
                        original_url=sent_url,
                        content_type="sentence_pair",
                        access_mode="open_reusable",
                        license_id=lic,
                        author=owner,
                        attribution_text=f"Tatoeba Sentence #{sid} por {owner} ({lic}).",
                        has_full_body_copy=True,
                        cefr_hint=None,
                        category_hint="grammar_academy",
                        skill_hint=skill_hint,
                        estimated_duration_min=2,
                        offline_compatible=1,
                        payload={
                            "sentence_id": sid,
                            "english": en_text,
                            "spanish": spa_text,
                            "query_topic": query,
                        },
                    )
                )
                added_for_q += 1
        return items


class GutendexAdapter(BaseProviderAdapter):
    provider_id = "gutenberg_gutendex"
    allowed_domains = ("gutendex.com", "www.gutenberg.org", "gutenberg.org", "openlibrary.org")
    TARGET_IDS = "11,21,1661,1952,74,84,1342,98"

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 8) -> list[NormalizedExternalItem]:
        try:
            url = "https://gutendex.com/books"
            params = {"languages": "en", "copyright": "false", "ids": self.TARGET_IDS}
            resp = await http.get(url, params=params, headers={"User-Agent": DEFAULT_USER_AGENT}, timeout=12.0)
            resp.raise_for_status()
            data = resp.json()
            items: list[NormalizedExternalItem] = []
            for book in (data.get("results") or [])[:limit]:
                bid = book.get("id")
                title = sanitize_external_text(book.get("title"), max_len=200)
                if not bid or not title:
                    continue
                authors_raw = book.get("authors") or []
                author_name = (
                    sanitize_external_text(authors_raw[0].get("name"), max_len=120)
                    if authors_raw and isinstance(authors_raw[0], dict)
                    else "Public Domain Author"
                )
                formats = book.get("formats") or {}
                html_url = formats.get("text/html") or formats.get("text/plain; charset=utf-8") or f"https://www.gutenberg.org/ebooks/{bid}"
                if html_url.startswith("http://"):
                    html_url = "https://" + html_url[len("http://"):]
                subjects = [sanitize_external_text(s, max_len=80) for s in (book.get("subjects") or [])[:3]]
                items.append(
                    NormalizedExternalItem(
                        provider_id=self.provider_id,
                        external_id=f"pg_{bid}",
                        title=title,
                        title_es=f"Clásico (Dominio Público): {title}",
                        description=f"Obra de dominio público de {author_name}. Temas: {', '.join(subjects) or 'Literatura inglesa'}.",
                        body_en="",
                        original_url=html_url,
                        content_type="book",
                        access_mode="open_reusable",
                        license_id="PDM-1.0",
                        author=author_name,
                        attribution_text=f"Project Gutenberg eBook #{bid} — {title} ({author_name}), Dominio Público (USA).",
                        has_full_body_copy=False,
                        cefr_hint=None,
                        category_hint="reading_library",
                        skill_hint="reading",
                        estimated_duration_min=20,
                        offline_compatible=0,
                        payload={
                            "gutenberg_id": bid,
                            "authors": [author_name],
                            "subjects": subjects,
                            "download_count": book.get("download_count", 0),
                            "read_url": html_url,
                            "plain_text_url": formats.get("text/plain; charset=utf-8") or formats.get("text/plain; charset=us-ascii") or "",
                        },
                    )
                )
            if items:
                return items
        except Exception:
            pass

        # Respaldo oficial vía índice Open Library para ficción clásica de Project Gutenberg en dominio público (<= 1928)
        ol_resp = await http.get(
            "https://openlibrary.org/search.json",
            params={
                "q": "id_project_gutenberg:* language:eng subject:fiction",
                "fields": "key,title,author_name,first_publish_year,id_project_gutenberg,ia",
                "limit": max(limit * 3, 18),
            },
            headers={"User-Agent": DEFAULT_USER_AGENT},
        )
        ol_resp.raise_for_status()
        items = []
        seen_ids: set[str] = set()
        for doc in (ol_resp.json().get("docs") or []):
            if len(items) >= limit:
                break
            pg_ids = doc.get("id_project_gutenberg") or []
            pub_year = doc.get("first_publish_year")
            if not pg_ids or (isinstance(pub_year, int) and pub_year > 1928):
                continue
            bid = str(pg_ids[0])
            if bid in seen_ids:
                continue
            title = sanitize_external_text(doc.get("title"), max_len=200)
            lower_t = title.lower()
            if not title or "bible" in lower_t or "complete works" in lower_t:
                continue
            seen_ids.add(bid)
            authors = doc.get("author_name") or []
            author_name = sanitize_external_text(authors[0] if authors else "Public Domain Author", max_len=120)
            ol_key = str(doc.get("key") or "")
            ol_url = f"https://openlibrary.org{ol_key}" if ol_key.startswith("/") else f"https://www.gutenberg.org/ebooks/{bid}"
            pg_url = f"https://www.gutenberg.org/ebooks/{bid}"
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=f"pg_{bid}",
                    title=title,
                    title_es=f"Clásico (Dominio Público): {title}",
                    description=f"Obra clásica de {author_name} ({pub_year or 'Dominio Público'}) · Gutenberg #{bid}.",
                    body_en="",
                    original_url=ol_url,
                    content_type="book",
                    access_mode="open_reusable",
                    license_id="PDM-1.0",
                    author=author_name,
                    attribution_text=f"Project Gutenberg #{bid} / Open Library — {title} ({author_name}), Dominio Público.",
                    has_full_body_copy=False,
                    cefr_hint=None,
                    category_hint="reading_library",
                    skill_hint="reading",
                    estimated_duration_min=20,
                    offline_compatible=0,
                    payload={"gutenberg_id": bid, "authors": [author_name], "first_publish_year": pub_year, "read_url": pg_url, "openlibrary_url": ol_url},
                )
            )
        return items


class WikimediaCommonsAdapter(BaseProviderAdapter):
    provider_id = "wikimedia_commons"
    allowed_domains = ("commons.wikimedia.org", "upload.wikimedia.org")
    SEED_AUDIO_WORDS = [
        ("En-us-hello.ogg", "hello", "A1"),
        ("En-us-water.ogg", "water", "A1"),
        ("En-us-comfortable.ogg", "comfortable", "A2"),
        ("En-us-schedule.ogg", "schedule", "B1"),
        ("En-us-resilient.ogg", "resilient", "C1"),
        ("En-us-nuance.ogg", "nuance", "C1"),
    ]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 6) -> list[NormalizedExternalItem]:
        chosen = self.SEED_AUDIO_WORDS[:limit]
        titles_param = "|".join(f"File:{f}" for f, _, _ in chosen)
        url = "https://commons.wikimedia.org/w/api.php"
        params = {
            "action": "query",
            "format": "json",
            "prop": "imageinfo",
            "iiprop": "extmetadata|url",
            "iiextmetadatafilter": "LicenseShortName|UsageTerms|Artist|LicenseUrl",
            "titles": titles_param,
        }
        resp = await http.get(url, params=params, headers={"User-Agent": DEFAULT_USER_AGENT})
        resp.raise_for_status()
        pages = (resp.json().get("query") or {}).get("pages") or {}
        word_map = {f"File:{f}".lower(): (word, cefr) for f, word, cefr in chosen}
        items: list[NormalizedExternalItem] = []
        for page in pages.values():
            if not isinstance(page, dict) or "missing" in page:
                continue
            title_raw = str(page.get("title") or "")
            ii_list = page.get("imageinfo") or []
            if not ii_list:
                continue
            info = ii_list[0]
            ext = info.get("extmetadata") or {}
            lic_raw = (ext.get("LicenseShortName") or {}).get("value")
            lic_id = normalize_license_code(lic_raw)
            if not lic_id:
                continue
            artist = sanitize_external_text((ext.get("Artist") or {}).get("value"), max_len=140) or "Wikimedia Commons contributor"
            audio_url = info.get("url") or ""
            desc_url = info.get("descriptionurl") or f"https://commons.wikimedia.org/wiki/{quote(title_raw)}"
            word, cefr = word_map.get(title_raw.lower(), (title_raw.replace("File:", "").replace(".ogg", ""), None))
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=title_raw,
                    title=f"Pronunciation Audio: {word}",
                    title_es=f"Audio nativo Wikimedia: {word}",
                    description=f"Archivo de pronunciación abierta en inglés ({lic_id}) por {artist}.",
                    body_en="",
                    original_url=audio_url or desc_url,
                    content_type="audio_clip",
                    access_mode="open_reusable",
                    license_id=lic_id,
                    author=artist,
                    attribution_text=f"Wikimedia Commons ({title_raw}) por {artist}, licencia {lic_id}.",
                    has_full_body_copy=False,
                    cefr_hint=None,
                    category_hint="pronunciation_studio",
                    skill_hint="pronunciation",
                    estimated_duration_min=2,
                    offline_compatible=0,
                    payload={"word": word, "audio_url": audio_url, "page_url": desc_url, "license_short": lic_raw},
                )
            )
        return items


class SimpleWikipediaAdapter(BaseProviderAdapter):
    provider_id = "simple_wikipedia"
    allowed_domains = ("simple.wikipedia.org",)
    SEED_ARTICLES = [
        ("Coffee", "A2", "reading_library", "reading"),
        ("Solar_energy", "B1", "english_news", "comprehension"),
        ("Photosynthesis", "B1", "academic_english", "academic_english"),
        ("Artificial_intelligence", "B2", "english_news", "reading"),
        ("Public_library", "A2", "reading_library", "everyday_english"),
        ("Critical_thinking", "B2", "academic_english", "academic_english"),
    ]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 6) -> list[NormalizedExternalItem]:
        items: list[NormalizedExternalItem] = []
        for slug, cefr, cat_hint, skill_hint in self.SEED_ARTICLES[:limit]:
            url = f"https://simple.wikipedia.org/api/rest_v1/page/summary/{quote(slug)}"
            resp = await http.get(url, headers={"User-Agent": DEFAULT_USER_AGENT})
            resp.raise_for_status()
            data = resp.json()
            title = sanitize_external_text(data.get("title"), max_len=140)
            extract = sanitize_external_text(data.get("extract"), max_len=1800)
            page_url = (
                ((data.get("content_urls") or {}).get("desktop") or {}).get("page")
                or f"https://simple.wikipedia.org/wiki/{slug}"
            )
            if not title or not extract:
                continue
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=f"simplewiki_{slug}",
                    title=f"{title} (Simple English)",
                    title_es=f"Lectura abierta: {title}",
                    description=extract[:220],
                    body_en=extract,
                    original_url=page_url,
                    content_type="reading",
                    access_mode="open_reusable",
                    license_id="CC-BY-SA-4.0",
                    author="Simple English Wikipedia contributors",
                    attribution_text=f"Simple English Wikipedia: '{title}' (CC BY-SA 4.0).",
                    has_full_body_copy=True,
                    cefr_hint=None,
                    category_hint=cat_hint,
                    skill_hint=skill_hint,
                    estimated_duration_min=4,
                    offline_compatible=1,
                    payload={"title": title, "body_en": extract, "page_url": page_url, "wikibase_item": data.get("wikibase_item", "")},
                )
            )
        return items


class LibriVoxAdapter(BaseProviderAdapter):
    provider_id = "librivox"
    allowed_domains = ("librivox.org", "www.archive.org", "archive.org")
    SEED_IDS = ["49", "52", "1340", "174"]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 4) -> list[NormalizedExternalItem]:
        items: list[NormalizedExternalItem] = []
        try:
            for bid in self.SEED_IDS[:limit]:
                url = "https://librivox.org/api/feed/audiobooks/"
                params = {"id": bid, "extended": "1", "format": "json"}
                resp = await http.get(url, params=params, headers={"User-Agent": DEFAULT_USER_AGENT}, timeout=10.0)
                resp.raise_for_status()
                data = resp.json()
                books = data.get("books")
                book = books[0] if isinstance(books, list) and books else (next(iter(books.values()), None) if isinstance(books, dict) else None)
                if not isinstance(book, dict):
                    continue
                title = sanitize_external_text(book.get("title"), max_len=160)
                desc = sanitize_external_text(book.get("description"), max_len=400)
                book_url = book.get("url_librivox") or f"https://librivox.org/search?q={bid}"
                sections = book.get("sections") or []
                first_audio = sections[0].get("listen_url") or "" if isinstance(sections, list) and sections and isinstance(sections[0], dict) else ""
                authors = book.get("authors") or []
                author_name = "LibriVox Volunteer Readers"
                if authors and isinstance(authors[0], dict):
                    fn = authors[0].get("first_name", "")
                    ln = authors[0].get("last_name", "")
                    author_name = sanitize_external_text(f"{fn} {ln}".strip(), max_len=100) or author_name
                items.append(
                    NormalizedExternalItem(
                        provider_id=self.provider_id,
                        external_id=f"lv_{bid}",
                        title=f"Audiobook: {title}",
                        title_es=f"Audiolibro LibriVox: {title}",
                        description=desc or "Audiolibro clásico narrado por voluntarios en dominio público.",
                        body_en="",
                        original_url=book_url,
                        content_type="audio_clip",
                        access_mode="open_reusable",
                        license_id="PDM-1.0",
                        author=author_name,
                        attribution_text=f"LibriVox Public Domain Recording — {title} ({author_name}).",
                        has_full_body_copy=False,
                        cefr_hint=None,
                        category_hint="listening_lab",
                        skill_hint="listening",
                        estimated_duration_min=15,
                        offline_compatible=0,
                        payload={"librivox_id": bid, "totaltime": book.get("totaltime", ""), "listen_url": first_audio, "book_url": book_url},
                    )
                )
            if items:
                return items
        except Exception:
            pass

        # Espejo oficial Internet Archive (donde LibriVox aloja sus archivos MP3 de dominio público)
        search_resp = await http.get(
            "https://archive.org/advancedsearch.php",
            params={
                "q": "collection:librivoxaudio AND language:eng AND mediatype:audio",
                "fl[]": ["identifier", "title", "creator"],
                "sort[]": "downloads desc",
                "rows": limit,
                "page": 1,
                "output": "json",
            },
            headers={"User-Agent": DEFAULT_USER_AGENT},
        )
        search_resp.raise_for_status()
        docs = ((search_resp.json().get("response") or {}).get("docs") or [])[:limit]
        items = []
        for doc in docs:
            ident = str(doc.get("identifier") or "").strip()
            title = sanitize_external_text(doc.get("title"), max_len=160)
            if not ident or not title:
                continue
            creator = sanitize_external_text(
                doc.get("creator")[0] if isinstance(doc.get("creator"), list) and doc.get("creator") else doc.get("creator"),
                max_len=120,
            ) or "LibriVox Readers"
            archive_url = f"https://archive.org/details/{ident}"
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=f"lv_ia_{ident}",
                    title=f"Audiobook: {title}",
                    title_es=f"Audiolibro LibriVox: {title}",
                    description=f"Audiolibro en inglés de dominio público narrado por voluntarios LibriVox ({creator}).",
                    body_en="",
                    original_url=archive_url,
                    content_type="audio_clip",
                    access_mode="open_reusable",
                    license_id="PDM-1.0",
                    author=creator,
                    attribution_text=f"LibriVox / Internet Archive ({ident}) — {title} (Dominio Público).",
                    has_full_body_copy=False,
                    cefr_hint=None,
                    category_hint="listening_lab",
                    skill_hint="listening",
                    estimated_duration_min=15,
                    offline_compatible=0,
                    payload={"archive_identifier": ident, "book_url": archive_url, "listen_url": archive_url},
                )
            )
        return items


class FreeDictionaryAdapter(BaseProviderAdapter):
    provider_id = "free_dictionary"
    allowed_domains = ("api.dictionaryapi.dev", "en.wiktionary.org")
    SEED_WORDS = [
        ("resilient", "C1"),
        ("accomplish", "B1"),
        ("commute", "B1"),
        ("perspective", "B2"),
        ("lucid", "C2"),
    ]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 5) -> list[NormalizedExternalItem]:
        items: list[NormalizedExternalItem] = []
        for word, cefr in self.SEED_WORDS[:limit]:
            resp = await http.get(
                f"https://api.dictionaryapi.dev/api/v2/entries/en/{word}",
                headers={"User-Agent": DEFAULT_USER_AGENT},
            )
            resp.raise_for_status()
            arr = resp.json()
            if not isinstance(arr, list) or not arr:
                continue
            entry = arr[0]
            phonetic = sanitize_external_text(entry.get("phonetic"), max_len=40)
            audio_url = ""
            for ph in entry.get("phonetics") or []:
                if isinstance(ph, dict) and ph.get("audio"):
                    audio_url = str(ph["audio"]).strip()
                    if not phonetic and ph.get("text"):
                        phonetic = sanitize_external_text(ph["text"], max_len=40)
                    break
            meanings = entry.get("meanings") or []
            definition = ""
            example = ""
            if meanings and isinstance(meanings[0], dict):
                defs = meanings[0].get("definitions") or []
                if defs and isinstance(defs[0], dict):
                    definition = sanitize_external_text(defs[0].get("definition"), max_len=240)
                    example = sanitize_external_text(defs[0].get("example"), max_len=240)
            src_urls = entry.get("sourceUrls") or []
            src_url = src_urls[0] if src_urls else f"https://en.wiktionary.org/wiki/{word}"
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=f"fdict_{word}",
                    title=f"Dictionary Audio & IPA: {word}",
                    title_es=f"Definición y audio IPA: {word}",
                    description=f"{phonetic} · {definition}" if definition else f"Definición abierta de {word}",
                    body_en=example or definition,
                    original_url=src_url,
                    content_type="vocabulary",
                    access_mode="open_reusable",
                    license_id="CC-BY-SA-3.0",
                    author="Wiktionary / Free Dictionary API",
                    attribution_text=f"Wiktionary via Free Dictionary API — '{word}' (CC BY-SA 3.0).",
                    has_full_body_copy=True,
                    cefr_hint=None,
                    category_hint="vocabulary_master",
                    skill_hint="vocabulary",
                    estimated_duration_min=2,
                    offline_compatible=1,
                    payload={"headword": word, "ipa": phonetic, "definition": definition, "example_en": example, "audio_url": audio_url},
                )
            )
        return items


class DatamuseAdapter(BaseProviderAdapter):
    provider_id = "datamuse"
    allowed_domains = ("api.datamuse.com", "www.datamuse.com")
    SEED_WORDS = [
        ("resilient", "C1"),
        ("achieve", "B1"),
        ("perspective", "B2"),
        ("concise", "C1"),
        ("reliable", "B2"),
    ]

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 5) -> list[NormalizedExternalItem]:
        items: list[NormalizedExternalItem] = []
        for word, cefr in self.SEED_WORDS[:limit]:
            syn_resp = await http.get("https://api.datamuse.com/words", params={"rel_syn": word, "max": 6}, headers={"User-Agent": DEFAULT_USER_AGENT})
            syn_resp.raise_for_status()
            ant_resp = await http.get("https://api.datamuse.com/words", params={"rel_ant": word, "max": 4}, headers={"User-Agent": DEFAULT_USER_AGENT})
            ant_resp.raise_for_status()
            synonyms = [sanitize_external_text(w.get("word"), max_len=40) for w in (syn_resp.json() or []) if isinstance(w, dict) and w.get("word")]
            antonyms = [sanitize_external_text(w.get("word"), max_len=40) for w in (ant_resp.json() or []) if isinstance(w, dict) and w.get("word")]
            items.append(
                NormalizedExternalItem(
                    provider_id=self.provider_id,
                    external_id=f"dm_{word}",
                    title=f"Semantic Cluster: {word}",
                    title_es=f"Red semántica Datamuse: {word}",
                    description=f"Sinónimos: {', '.join(synonyms[:4]) or 'n/a'} · Antónimos: {', '.join(antonyms[:3]) or 'n/a'}.",
                    body_en="",
                    original_url=f"https://www.datamuse.com/api/",
                    content_type="vocabulary",
                    access_mode="live_api",
                    license_id="FREE-API-NONCOMM",
                    author="Datamuse API",
                    attribution_text="Relaciones léxicas obtenidas vía Datamuse API (uso no comercial gratuito).",
                    has_full_body_copy=False,
                    cefr_hint=None,
                    category_hint="word_games",
                    skill_hint="vocabulary",
                    estimated_duration_min=3,
                    offline_compatible=1,
                    payload={"headword": word, "synonyms": synonyms, "antonyms": antonyms},
                )
            )
        return items


def parse_common_voice_tsv(tsv_path: Path | str, limit: int = 50) -> list[NormalizedExternalItem]:
    """Importa metadatos y frases de un archivo TSV descargado de Mozilla Common Voice (CC0 1.0)."""
    path = Path(tsv_path)
    if not path.is_file():
        return []
    items: list[NormalizedExternalItem] = []
    with path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for idx, row in enumerate(reader, start=1):
            if len(items) >= limit:
                break
            sentence = sanitize_external_text(row.get("sentence"), max_len=280)
            clip_path = sanitize_external_text(row.get("path") or f"clip_{idx}", max_len=160)
            accent = sanitize_external_text(row.get("accents") or row.get("accent") or "English", max_len=60)
            if not sentence or len(sentence.split()) < 3:
                continue
            items.append(
                NormalizedExternalItem(
                    provider_id="mozilla_common_voice",
                    external_id=f"mcv_{clip_path}",
                    title=f"Common Voice: {sentence[:70]}",
                    title_es=f"Frase grabada Common Voice ({accent})",
                    description=f"Frase validada CC0 del dataset Mozilla Common Voice ({accent}): «{sentence}».",
                    body_en=sentence,
                    original_url="https://datacollective.mozillafoundation.org/",
                    content_type="audio_clip",
                    access_mode="open_reusable",
                    license_id="CC0-1.0",
                    author="Mozilla Common Voice Contributors",
                    attribution_text="Mozilla Common Voice Dataset (CC0 1.0 Public Domain).",
                    has_full_body_copy=True,
                    cefr_hint=None,
                    category_hint="listening_lab",
                    skill_hint="listening",
                    estimated_duration_min=2,
                    offline_compatible=1,
                    payload={"sentence": sentence, "clip_file": clip_path, "accent": accent},
                )
            )
    return items


class MozillaCommonVoiceAdapter(BaseProviderAdapter):
    provider_id = "mozilla_common_voice"
    allowed_domains = ("datacollective.mozillafoundation.org", "commonvoice.mozilla.org")

    async def fetch_items(self, http: httpx.AsyncClient, limit: int = 20) -> list[NormalizedExternalItem]:
        local_tsv = os.getenv("COMMON_VOICE_TSV_PATH", "").strip()
        if local_tsv:
            return parse_common_voice_tsv(local_tsv, limit=limit)
        # Sin descarga TSV local configurada: mantiene las fichas verificadas del catálogo MDC sin inventar clips.
        return []


PROVIDER_ADAPTERS: dict[str, BaseProviderAdapter] = {
    "tatoeba": TatoebaAdapter(),
    "gutenberg_gutendex": GutendexAdapter(),
    "wikimedia_commons": WikimediaCommonsAdapter(),
    "simple_wikipedia": SimpleWikipediaAdapter(),
    "librivox": LibriVoxAdapter(),
    "datamuse": DatamuseAdapter(),
    "free_dictionary": FreeDictionaryAdapter(),
    "mozilla_common_voice": MozillaCommonVoiceAdapter(),
}
