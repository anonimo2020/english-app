"""
Extractor fiel del currículo base desde los archivos Kotlin fuente de Android.

Recupera sin pérdidas el 100% del inventario existente:
- 6 niveles CEFR
- 24 unidades temáticas (learning_units)
- 96 lecciones (lessons) + 96 retos de voz
- 288 tarjetas educativas (teaching_cards)
- 288 mini-tests (quizzes / quiz_questions)
- 48 escenarios de conversación (conversation_scenarios)
- 288 palabras de vocabulario (vocabulary)
- 18 historias interactivas (stories)
- 30 ejercicios fonéticos (phonetic_exercises)
- 36 preguntas diagnósticas (diagnostic_questions)
- 8 portales educativos (external_resources)
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT_DIR = Path(__file__).resolve().parent.parent
ANDROID_KOTLIN_BASE = ROOT_DIR / "android-app" / "app" / "src" / "main" / "kotlin" / "com" / "example" / "englishapp"

LEVEL_ENUM_TO_CEFR = {
    "LearnerLevel.NOVATO": "A1",
    "LearnerLevel.JUNIOR": "A2",
    "LearnerLevel.INTERMEDIO": "B1",
    "LearnerLevel.INTERMEDIO_ALTO": "B2",
    "LearnerLevel.PRO": "C1",
    "LearnerLevel.MAESTRIA": "C2",
}


@dataclass
class KotlinCall:
    name: str
    positional: list[Any]
    named: dict[str, Any]


def _strip_comments(src: str) -> str:
    """Elimina comentarios // y /* ... */ respetando cadenas Kotlin."""
    out: list[str] = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith('"""', i):
            end = src.find('"""', i + 3)
            if end == -1:
                out.append(src[i:])
                break
            out.append(src[i : end + 3])
            i = end + 3
        elif src[i] == '"':
            j = i + 1
            while j < n:
                if src[j] == "\\" and j + 1 < n:
                    j += 2
                elif src[j] == '"':
                    j += 1
                    break
                else:
                    j += 1
            out.append(src[i:j])
            i = j
        elif src.startswith("//", i):
            nl = src.find("\n", i)
            if nl == -1:
                break
            out.append("\n")
            i = nl + 1
        elif src.startswith("/*", i):
            end = src.find("*/", i + 2)
            if end == -1:
                break
            i = end + 2
        else:
            out.append(src[i])
            i += 1
    return "".join(out)


def _find_matching_paren(s: str, open_idx: int) -> int:
    depth = 0
    i = open_idx
    n = len(s)
    while i < n:
        if s.startswith('"""', i):
            end = s.find('"""', i + 3)
            i = (end + 3) if end != -1 else n
        elif s[i] == '"':
            j = i + 1
            while j < n:
                if s[j] == "\\" and j + 1 < n:
                    j += 2
                elif src_char(s, j) == '"':
                    j += 1
                    break
                else:
                    j += 1
            i = j
        elif s[i] == "(":
            depth += 1
            i += 1
        elif s[i] == ")":
            depth -= 1
            if depth == 0:
                return i
            i += 1
        else:
            i += 1
    raise ValueError(f"Unclosed parenthesis starting at {open_idx}")


def src_char(s: str, idx: int) -> str:
    return s[idx]


def _split_top_level_args(arg_str: str) -> list[str]:
    args: list[str] = []
    cur: list[str] = []
    depth = 0
    i = 0
    n = len(arg_str)
    while i < n:
        if arg_str.startswith('"""', i):
            end = arg_str.find('"""', i + 3)
            if end == -1:
                cur.append(arg_str[i:])
                break
            cur.append(arg_str[i : end + 3])
            i = end + 3
        elif arg_str[i] == '"':
            j = i + 1
            while j < n:
                if arg_str[j] == "\\" and j + 1 < n:
                    j += 2
                elif arg_str[j] == '"':
                    j += 1
                    break
                else:
                    j += 1
            cur.append(arg_str[i:j])
            i = j
        elif arg_str[i] in "([{":
            depth += 1
            cur.append(arg_str[i])
            i += 1
        elif arg_str[i] in ")]}":
            depth -= 1
            cur.append(arg_str[i])
            i += 1
        elif arg_str[i] == "," and depth == 0:
            part = "".join(cur).strip()
            if part:
                args.append(part)
            cur = []
            i += 1
        else:
            cur.append(arg_str[i])
            i += 1
    tail = "".join(cur).strip()
    if tail:
        args.append(tail)
    return args


def _find_top_level_equals(expr: str) -> int:
    depth = 0
    i = 0
    n = len(expr)
    while i < n:
        if expr[i] == '"':
            j = i + 1
            while j < n:
                if expr[j] == "\\" and j + 1 < n:
                    j += 2
                elif expr[j] == '"':
                    j += 1
                    break
                else:
                    j += 1
            i = j
        elif expr[i] in "([{":
            depth += 1
            i += 1
        elif expr[i] in ")]}":
            depth -= 1
            i += 1
        elif expr[i] == "=" and depth == 0:
            return i
        else:
            i += 1
    return -1


def _decode_kotlin_string(expr: str) -> str:
    if expr.startswith('"""') and expr.endswith('"""'):
        return expr[3:-3]
    inner = expr[1:-1]
    out: list[str] = []
    i = 0
    n = len(inner)
    while i < n:
        if inner[i] == "\\" and i + 1 < n:
            nxt = inner[i + 1]
            escapes = {"n": "\n", "t": "\t", "r": "\r", '"': '"', "\\": "\\", "$": "$"}
            out.append(escapes.get(nxt, nxt))
            i += 2
        else:
            out.append(inner[i])
            i += 1
    return "".join(out)


def _eval_kotlin_expr(expr: str) -> Any:
    s = expr.strip()
    if (s.startswith('"') and s.endswith('"')) or (s.startswith('"""') and s.endswith('"""')):
        return _decode_kotlin_string(s)
    if s == "null":
        return None
    if s == "true":
        return True
    if s == "false":
        return False
    if s in LEVEL_ENUM_TO_CEFR:
        return LEVEL_ENUM_TO_CEFR[s]
    if re.fullmatch(r"-?\d+", s):
        return int(s)
    if re.fullmatch(r"-?\d+\.\d+", s):
        return float(s)
    if s.startswith("emptyList("):
        return []
    m = re.match(r"^([A-Za-z_][A-Za-z0-9_]*)\((.*)\)$", s, re.DOTALL)
    if m:
        ctor = m.group(1)
        body = m.group(2)
        items = _split_top_level_args(body)
        if ctor == "listOf":
            return [_eval_kotlin_expr(it) for it in items]
        return parse_call_from_text(ctor, body)
    return s


def parse_call_from_text(name: str, body: str) -> KotlinCall:
    parts = _split_top_level_args(body)
    positional: list[Any] = []
    named: dict[str, Any] = {}
    for p in parts:
        eq = _find_top_level_equals(p)
        if eq > 0 and re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", p[:eq].strip()):
            key = p[:eq].strip()
            val = _eval_kotlin_expr(p[eq + 1 :])
            named[key] = val
        else:
            positional.append(_eval_kotlin_expr(p))
    return KotlinCall(name=name, positional=positional, named=named)


def extract_top_level_calls(src: str, ctor_name: str) -> list[KotlinCall]:
    clean = _strip_comments(src)
    pattern = re.compile(rf"\b{re.escape(ctor_name)}\s*\(")
    calls: list[KotlinCall] = []
    pos = 0
    while True:
        m = pattern.search(clean, pos)
        if not m:
            break
        open_idx = m.end() - 1
        close_idx = _find_matching_paren(clean, open_idx)
        body = clean[open_idx + 1 : close_idx]
        calls.append(parse_call_from_text(ctor_name, body))
        pos = close_idx + 1
    return calls


def _extract_array_block_calls(src: str, anchor: str, ctor_name: str) -> list[KotlinCall]:
    idx = src.find(anchor)
    if idx == -1:
        raise ValueError(f"Anchor no encontrado en fuente Kotlin: {anchor}")
    return extract_top_level_calls(src[idx:], ctor_name)


def extract_baseline_inventory(base_dir: Path | None = None) -> dict[str, Any]:
    """Extrae el 100% del currículo y recursos originales declarados en Kotlin."""
    root = base_dir or ANDROID_KOTLIN_BASE

    # 1. Unidades, Lecciones, Tarjetas y Mini-tests (24 / 96 / 288 / 288)
    units: list[dict[str, Any]] = []
    for fname in ["CurriculumA1.kt", "CurriculumA2.kt", "CurriculumB1.kt", "CurriculumB2.kt", "CurriculumC1.kt", "CurriculumC2.kt"]:
        text = (root / "data" / "curriculum" / fname).read_text(encoding="utf-8")
        for u_call in extract_top_level_calls(text, "CourseUnit"):
            u = u_call.named
            lessons_parsed: list[dict[str, Any]] = []
            for l_call in u.get("lessons", []):
                l = l_call.named
                items_parsed: list[dict[str, Any]] = []
                for idx_i, item_call in enumerate(l.get("items", []), start=1):
                    it = item_call.named
                    mt = it["miniTest"].named
                    items_parsed.append(
                        {
                            "id": it["id"],
                            "order_in_lesson": idx_i,
                            "kind": str(it["kind"]).split(".")[-1],
                            "title_es": it["titleEs"],
                            "title_en": it["titleEn"],
                            "explanation_es": it["explanationEs"],
                            "model_en": it["modelEn"],
                            "model_es": it["modelEs"],
                            "tip_es": it["tipEs"],
                            "mini_test": {
                                "kind": str(mt["kind"]).split(".")[-1],
                                "prompt_es": mt["promptEs"],
                                "audio_prompt_en": mt.get("audioPromptEn"),
                                "options": mt.get("options", []),
                                "correct_index": mt.get("correctIndex", 0),
                                "target_tokens": mt.get("targetTokens", []),
                                "distraction_tokens": mt.get("distractionTokens", []),
                                "explanation_es": mt["explanationEs"],
                            },
                        }
                    )
                lessons_parsed.append(
                    {
                        "id": l["id"],
                        "unit_id": l["unitId"],
                        "cefr_level": l["level"],
                        "order_in_unit": l["orderInUnit"],
                        "title_es": l["titleEs"],
                        "title_en": l["titleEn"],
                        "duration_min": l["durationMin"],
                        "speaking_target_en": l["speakingTargetEn"],
                        "speaking_target_es": l["speakingTargetEs"],
                        "speaking_tip_es": l["speakingTipEs"],
                        "items": items_parsed,
                    }
                )
            units.append(
                {
                    "id": u["id"],
                    "cefr_level": u["level"],
                    "order_in_level": u["orderInLevel"],
                    "title_es": u["titleEs"],
                    "title_en": u["titleEn"],
                    "description_es": u["descriptionEs"],
                    "specialized_focus_es": u["specializedFocusEs"],
                    "icon_name": u["iconName"],
                    "lessons": lessons_parsed,
                }
            )

    # 2. Escenarios (48)
    tutor_models_src = (root / "model" / "TutorModels.kt").read_text(encoding="utf-8")
    scenarios: list[dict[str, Any]] = []
    for s_call in _extract_array_block_calls(tutor_models_src, "object ScenarioCatalog", "ConversationScenario"):
        s = s_call.named
        scenarios.append(
            {
                "id": s["id"],
                "cefr_level": s["recommendedLevel"],
                "title_es": s["titleEs"],
                "title_en": s["titleEn"],
                "subtitle_es": s["subtitleEs"],
                "opening_en": s["openingEn"],
                "opening_es": s["openingEs"],
                "starter_suggestions": s.get("starterSuggestions", []),
                "icon_name": s["iconName"],
                "focus_skill_es": s.get("focusSkillEs", "Conversación práctica"),
                "coach_intro_es": s.get("coachIntroEs", ""),
            }
        )

    # 3. Vocabulario (288)
    vocab_src = (root / "data" / "VocabularyCatalog.kt").read_text(encoding="utf-8")
    vocab_entries: list[dict[str, Any]] = []
    for v_call in _extract_array_block_calls(vocab_src, "val all: List<VocabEntry>", "VocabEntry"):
        p = v_call.positional
        n = v_call.named
        vocab_entries.append(
            {
                "id": n.get("id", p[0] if len(p) > 0 else ""),
                "word": n.get("word", p[1] if len(p) > 1 else ""),
                "ipa": n.get("ipa", p[2] if len(p) > 2 else ""),
                "spanish": n.get("spanish", p[3] if len(p) > 3 else ""),
                "category": n.get("category", p[4] if len(p) > 4 else ""),
                "cefr_level": n.get("level", p[5] if len(p) > 5 else "A1"),
                "example_en": n.get("exampleEn", p[6] if len(p) > 6 else ""),
                "example_es": n.get("exampleEs", p[7] if len(p) > 7 else ""),
                "tip_es": n.get("tipEs", p[8] if len(p) > 8 else ""),
            }
        )

    # 4. Historias (18), Ejercicios fonéticos (30), Portales web (8)
    lib_src = (root / "data" / "LearningLibrary.kt").read_text(encoding="utf-8")
    stories: list[dict[str, Any]] = []
    for st_call in _extract_array_block_calls(lib_src, "val stories: List<AudioStory>", "AudioStory"):
        st = st_call.named
        vocab_items = [
            {
                "word": sv.named.get("word", sv.positional[0] if len(sv.positional) > 0 else ""),
                "spanish": sv.named.get("spanish", sv.positional[1] if len(sv.positional) > 1 else ""),
                "phonetic": sv.named.get("phonetic", sv.positional[2] if len(sv.positional) > 2 else ""),
            }
            for sv in st.get("vocab", [])
        ]
        q_items = [
            {
                "question_es": sq.named.get("questionEs", sq.positional[0] if len(sq.positional) > 0 else ""),
                "options": sq.named.get("options", sq.positional[1] if len(sq.positional) > 1 else []),
                "correct_index": sq.named.get("correctIndex", sq.positional[2] if len(sq.positional) > 2 else 0),
            }
            for sq in st.get("questions", [])
        ]
        stories.append(
            {
                "id": st["id"],
                "title_es": st["titleEs"],
                "title_en": st["titleEn"],
                "cefr_level": st["level"],
                "duration_label": st["durationLabel"],
                "body_en": st["bodyEn"],
                "body_es": st["bodyEs"],
                "vocab": vocab_items,
                "questions": q_items,
            }
        )

    drills: list[dict[str, Any]] = []
    for d_call in _extract_array_block_calls(lib_src, "val drills: List<PronunciationDrill>", "PronunciationDrill"):
        p = d_call.positional
        n = d_call.named
        drills.append(
            {
                "id": n.get("id", p[0] if len(p) > 0 else ""),
                "title": n.get("title", p[1] if len(p) > 1 else ""),
                "cefr_level": n.get("level", p[2] if len(p) > 2 else "A1"),
                "category": n.get("category", p[3] if len(p) > 3 else ""),
                "ipa": n.get("ipa", p[4] if len(p) > 4 else ""),
                "target_phrase": n.get("targetPhrase", p[5] if len(p) > 5 else ""),
                "example_sentence": n.get("exampleSentence", p[6] if len(p) > 6 else ""),
                "spanish_tip": n.get("spanishTip", p[7] if len(p) > 7 else ""),
            }
        )

    portals: list[dict[str, Any]] = []
    for p_call in _extract_array_block_calls(lib_src, "val webPortals: List<FreeWebPortal>", "FreeWebPortal"):
        pos = p_call.positional
        nm = p_call.named
        portals.append(
            {
                "name": nm.get("name", pos[0] if len(pos) > 0 else ""),
                "cefr_badge": nm.get("cefrBadge", pos[1] if len(pos) > 1 else ""),
                "category_es": nm.get("categoryEs", pos[2] if len(pos) > 2 else ""),
                "tagline_es": nm.get("taglineEs", pos[3] if len(pos) > 3 else ""),
                "url": nm.get("url", pos[4] if len(pos) > 4 else ""),
                "min_level": nm.get("minLevel", pos[5] if len(pos) > 5 else "A1"),
                "max_level": nm.get("maxLevel", pos[6] if len(pos) > 6 else "C2"),
            }
        )

    # 5. Preguntas diagnósticas (36)
    placement_src = (root / "data" / "PlacementEngine.kt").read_text(encoding="utf-8")
    diagnostics: list[dict[str, Any]] = []
    diff_to_cefr = {1: "A1", 2: "A2", 3: "B1", 4: "B2", 5: "C1", 6: "C2"}
    for pq_call in _extract_array_block_calls(placement_src, "val bank: List<PlacementQuestion>", "PlacementQuestion"):
        q = pq_call.named
        diff = float(q["difficulty"])
        cefr = diff_to_cefr.get(max(1, min(6, int(diff))), "A1")
        diagnostics.append(
            {
                "id": q["id"],
                "cefr_level": cefr,
                "question_type": str(q["type"]).split(".")[-1],
                "difficulty": diff,
                "prompt_es": q["promptEs"],
                "prompt_en": q["promptEn"],
                "options": q.get("options", []),
                "correct_index": q.get("correctIndex", -1),
                "explanation_es": q["explanationEs"],
            }
        )

    return {
        "units": units,
        "scenarios": scenarios,
        "vocabulary": vocab_entries,
        "stories": stories,
        "drills": drills,
        "portals": portals,
        "diagnostics": diagnostics,
    }


def summarize_baseline_counts(inv: dict[str, Any]) -> dict[str, int]:
    lessons = sum(len(u["lessons"]) for u in inv["units"])
    cards = sum(len(l["items"]) for u in inv["units"] for l in u["lessons"])
    return {
        "cefr_levels": 6,
        "units": len(inv["units"]),
        "lessons": lessons,
        "teaching_cards": cards,
        "mini_tests": cards,
        "speaking_targets": lessons,
        "scenarios": len(inv["scenarios"]),
        "vocabulary": len(inv["vocabulary"]),
        "stories": len(inv["stories"]),
        "drills": len(inv["drills"]),
        "diagnostics": len(inv["diagnostics"]),
        "web_portals": len(inv["portals"]),
    }
