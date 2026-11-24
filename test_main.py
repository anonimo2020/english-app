from fastapi.testclient import TestClient

import main


def test_health_and_metadata_endpoints():
    with TestClient(main.app) as client:
        r_health = client.get("/health")
        assert r_health.status_code == 200
        body = r_health.json()
        assert body["ok"] is True
        assert body["version"] == "2.0"
        assert "ai_configured" in body

        r_scenarios = client.get("/scenarios")
        assert r_scenarios.status_code == 200
        scenarios = r_scenarios.json()["scenarios"]
        ids = {s["id"] for s in scenarios}
        assert len(scenarios) == 48
        assert {
            "greetings_intro",
            "coffee_shop",
            "daily_routine",
            "shopping_basics",
            "family_friends",
            "grocery_market",
            "weather_seasons",
            "hotel_checkin",
            "free_chat",
            "airport_travel",
            "restaurant",
            "directions_city",
            "weekend_plans",
            "renting_apartment",
            "pharmacy_help",
            "past_vacation",
            "job_interview",
            "doctor_visit",
            "customer_support",
            "movies_tech_opinions",
            "healthy_habits",
            "resolving_conflict",
            "networking_event",
            "business_trip",
            "workplace_feedback",
            "remote_collaboration",
            "media_fake_news",
            "housing_mortgage",
            "travel_complaints",
            "cultural_shock",
            "project_postmortem",
            "career_pivot",
            "daily_standup",
            "salary_negotiation",
            "ai_future_debate",
            "client_pitch",
            "crisis_leadership",
            "executive_interview",
            "ethics_sustainability",
            "global_strategy",
            "diplomatic_mediation",
            "literature_cinema_critique",
            "constitutional_ethics",
            "venture_boardroom",
            "satire_idioms",
            "scientific_breakthrough",
            "philosophical_paradox",
            "keynote_qna",
        }.issubset(ids)

        r_meta = client.get("/meta")
        assert r_meta.status_code == 200
        meta = r_meta.json()
        assert {c["id"] for c in meta["characters"]} == {"manu", "eddy"}
        assert {l["id"] for l in meta["levels"]} == {
            "NOVATO",
            "JUNIOR",
            "INTERMEDIO",
            "INTERMEDIO_ALTO",
            "PRO",
            "MAESTRIA",
        }


def test_parse_output_variants():
    full_raw = """```json
    {
      "reply": "Awesome! Do you prefer hot or iced coffee?",
      "translation_es": "¡Genial! ¿Prefieres café caliente o frío?",
      "coach_note_es": "Usa 'I would like' para pedir con amabilidad.",
      "correction": {
        "better": "I want a medium iced coffee.",
        "why": "En inglés conviene usar 'a' antes de tamaño y bebida.",
        "tip": "Usa 'I would like' para sonar más amable."
      },
      "suggested_replies": ["I would like an iced coffee, please.", "Hot coffee for me."],
      "pronunciation_focus": [
        {"word": "iced", "phonetic": "/aɪst/", "tip_es": "Suena parecido a 'aist', con t suave al final."}
      ]
    }
    ```"""
    parsed_full = main.parse_output(full_raw, level="JUNIOR", user_text="I want medium iced coffee please")
    assert parsed_full["reply"] == "Awesome! Do you prefer hot or iced coffee?"
    assert parsed_full["translation_es"] == "¡Genial! ¿Prefieres café caliente o frío?"
    assert parsed_full["coach_note_es"] == "Usa 'I would like' para pedir con amabilidad."
    assert parsed_full["correction"] == {
        "better": "I want a medium iced coffee.",
        "why": "En inglés conviene usar 'a' antes de tamaño y bebida.",
        "tip": "Usa 'I would like' para sonar más amable.",
    }
    assert len(parsed_full["suggested_replies"]) == 2
    assert parsed_full["pronunciation_focus"][0]["word"] == "iced"
    assert 10 <= parsed_full["xp_earned"] <= 25

    legacy_raw = '{"reply": "Nice! What do you like to drink?", "correction": null}'
    parsed_legacy = main.parse_output(legacy_raw, level="NOVATO", user_text="Hello")
    assert parsed_legacy["reply"] == "Nice! What do you like to drink?"
    assert parsed_legacy["translation_es"] is None
    assert parsed_legacy["correction"] is None
    assert parsed_legacy["suggested_replies"] == []
    assert parsed_legacy["pronunciation_focus"] == []
    assert parsed_legacy["xp_earned"] >= 10

    plain_raw = "Great job today! Tell me about your weekend."
    parsed_plain = main.parse_output(plain_raw, level="PRO", user_text="I traveled with my family.")
    assert parsed_plain["reply"] == plain_raw
    assert parsed_plain["correction"] is None
    assert parsed_plain["suggested_replies"] == []


def test_chat_auth_validation_and_mocked_ai(monkeypatch):
    monkeypatch.setattr(main, "APP_TOKEN", "secret-token")
    monkeypatch.setattr(main, "AI_API_KEY", "fake-key")
    main._hits.clear()

    captured = {}

    async def fake_call_ai(http, system, messages, max_tokens=500):
        captured["system"] = system
        captured["messages"] = messages
        return (
            '{"reply": "Sure thing! What size would you like?", '
            '"translation_es": "¡Claro! ¿Qué tamaño te gustaría?", '
            '"correction": {"better": "Can I have a coffee, please?", "why": "Usa please al pedir."}, '
            '"suggested_replies": ["Medium, please."], '
            '"pronunciation_focus": [{"word": "would", "phonetic": "/wʊd/", "tip_es": "La l es muda."}]}'
        )

    monkeypatch.setattr(main, "call_ai", fake_call_ai)

    with TestClient(main.app) as client:
        unauth = client.post(
            "/chat",
            json={
                "character": "manu",
                "level": "JUNIOR",
                "scenario": "coffee_shop",
                "messages": [{"role": "user", "content": "I want coffee"}],
            },
        )
        assert unauth.status_code == 401

        bad_tail = client.post(
            "/chat",
            headers={"X-App-Token": "secret-token"},
            json={
                "character": "manu",
                "level": "JUNIOR",
                "messages": [{"role": "assistant", "content": "Hello"}],
            },
        )
        assert bad_tail.status_code == 400

        ok = client.post(
            "/chat",
            headers={"X-App-Token": "secret-token"},
            json={
                "character": "eddy",
                "level": "INTERMEDIO",
                "scenario": "coffee_shop",
                "accent": "UK",
                "messages": [{"role": "user", "content": "Can I get coffee"}],
            },
        )
        assert ok.status_code == 200
        body = ok.json()
        assert body["reply"] == "Sure thing! What size would you like?"
        assert body["translation_es"] == "¡Claro! ¿Qué tamaño te gustaría?"
        assert body["correction"]["better"] == "Can I have a coffee, please?"
        assert body["suggested_replies"] == ["Medium, please."]
        assert body["pronunciation_focus"][0]["word"] == "would"
        assert "coffee shop" in captured["system"].lower()
        assert "british english" in captured["system"].lower()


def test_progress_summary_fallback(monkeypatch):
    monkeypatch.setattr(main, "APP_TOKEN", "")
    monkeypatch.setattr(main, "AI_API_KEY", "")
    main._hits.clear()

    with TestClient(main.app) as client:
        res = client.post(
            "/progress/summary",
            json={
                "character": "manu",
                "level": "JUNIOR",
                "scenario": "airport_travel",
                "messages": [
                    {"role": "assistant", "content": "Where are you flying today?"},
                    {"role": "user", "content": "I am flying to Madrid with my sister."},
                    {"role": "user", "content": "I would like a window seat please."},
                ],
            },
        )
        assert res.status_code == 200
        data = res.json()
        assert len(data["strengths_es"]) >= 2
        assert len(data["focus_areas_es"]) >= 1
        assert 40 <= data["fluency_score"] <= 100
        assert data["xp_bonus"] >= 10


def test_call_ai_404_auto_fallback_and_reasoning_strip(monkeypatch):
    import asyncio
    import httpx

    monkeypatch.setattr(main, "AI_API_KEY", "test-key")
    monkeypatch.setattr(main, "AI_MODEL", "retired-model-id")
    monkeypatch.setattr(main, "_resolved_ai_model", None)

    seen_models = []

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path.endswith("/chat/completions"):
            import json
            payload = json.loads(request.content.decode("utf-8"))
            seen_models.append(payload["model"])
            if payload["model"] == "retired-model-id":
                return httpx.Response(404, json={"error": {"message": "Model retired-model-id does not exist"}})
            return httpx.Response(
                200,
                json={
                    "choices": [
                        {
                            "message": {
                                "content": '<think>internal draft</think>\n{"reply": "Hi! How is your day?", "translation_es": "¡Hola! ¿Cómo va tu día?"}'
                            }
                        }
                    ]
                },
            )
        if request.url.path.endswith("/models"):
            return httpx.Response(
                200,
                json={
                    "data": [
                        {"id": "whisper-large-v3", "active": True},
                        {"id": "openai/gpt-oss-20b", "active": True},
                    ]
                },
            )
        return httpx.Response(404)

    async def run():
        transport = httpx.MockTransport(handler)
        async with httpx.AsyncClient(transport=transport) as client:
            return await main.call_ai(client, "sys", [{"role": "user", "content": "hi"}])

    raw = asyncio.run(run())
    assert seen_models == ["retired-model-id", "openai/gpt-oss-20b"]
    assert main._resolved_ai_model == "openai/gpt-oss-20b"
    assert "<think>" not in raw
    parsed = main.parse_output(raw, level="NOVATO", user_text="hi")
    assert parsed["reply"] == "Hi! How is your day?"
    assert parsed["translation_es"] == "¡Hola! ¿Cómo va tu día?"
