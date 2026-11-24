package com.example.englishapp.data

import com.example.englishapp.model.ChatMessage
import com.example.englishapp.model.ConversationScenario
import com.example.englishapp.model.ConversationTurn
import com.example.englishapp.model.Correction
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PronunciationTip
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.SessionSummary
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.model.TutorReply

/**
 * Motor conversacional offline inteligente para Manu y Eddy.
 * Analiza lo que realmente dice o pregunta el usuario, responde al tema real,
 * evita repetir preguntas y detecta errores comunes de hispanohablantes.
 */
object OfflineDemoTutor {

  private data class StepTemplate(
    val manuEn: String,
    val eddyEn: String,
    val novatoEn: String,
    val proEn: String,
    val translationEs: String,
    val suggestions: List<String>,
    val tips: List<PronunciationTip> = emptyList(),
  )

  private val adjectiveStopList = setOf(
    "good", "fine", "great", "ok", "okay", "tired", "happy", "sad", "ready",
    "hungry", "thirsty", "sick", "busy", "nervous", "excited", "bored",
    "boring", "sorry", "late", "early", "here", "there", "from", "learning",
    "working", "studying", "doing", "going", "looking", "agree", "sure",
    "confident", "afraid", "cold", "hot", "sleepy", "well", "better",
  )

  private val englishStopwords = setOf(
    "i", "me", "my", "mine", "you", "your", "yours", "he", "she", "it", "we",
    "they", "the", "a", "an", "and", "or", "but", "so", "because", "if", "when",
    "in", "on", "at", "to", "for", "of", "with", "about", "from", "by", "as",
    "is", "am", "are", "was", "were", "be", "been", "being", "have", "has", "had",
    "do", "does", "did", "will", "would", "can", "could", "should", "may", "might",
    "this", "that", "these", "those", "very", "really", "just", "also", "too",
    "not", "no", "yes", "yeah", "well", "then", "now", "today", "yesterday",
    "tomorrow", "here", "there", "like", "want", "need", "think", "know", "feel",
    "get", "got", "make", "made", "go", "going", "went", "say", "said", "tell",
    "please", "hello", "hi", "hey", "thanks", "thank", "much", "more", "some",
    "any", "many", "one", "two", "three", "lot", "thing", "things", "something",
  )

  fun buildOpeningTurn(
    scenario: ConversationScenario,
    character: TutorCharacter,
    level: LearnerLevel,
  ): ConversationTurn {
    val intro = when (character) {
      TutorCharacter.MANU -> when (level) {
        LearnerLevel.NOVATO ->
          "Hi! I am Manu. ${scenario.openingEn} (Sin prisa, responde con tus palabras)."
        else -> "Hi! I am Manu. ${scenario.openingEn}"
      }
      TutorCharacter.EDDY -> when (level) {
        LearnerLevel.NOVATO ->
          "Hello! Eddy here. ${scenario.openingEn} (Habla libremente, sin miedo)."
        else -> "Hey! Eddy here. ${scenario.openingEn}"
      }
    }
    val introCoach = scenario.coachIntroEs.ifBlank {
      "Escucha primero y responde a tu ritmo. Toca 'Traducir' u 'Oír en español' si necesitas ayuda."
    }
    return ConversationTurn(
      id = "open-${scenario.id}-${System.nanoTime()}",
      role = "assistant",
      content = intro,
      translationEs = scenario.openingEs,
      suggestedReplies = scenario.starterSuggestions,
      pronunciationFocus = openingPronunciationFor(scenario.id),
      coachNoteEs = introCoach,
    )
  }

  fun generateReply(
    character: TutorCharacter,
    level: LearnerLevel,
    scenarioId: String,
    accent: EnglishAccent,
    history: List<ChatMessage>,
  ): TutorReply {
    val userMessages = history.filter { it.role == "user" }
    val assistantHistory = history.filter { it.role == "assistant" }.map { it.content }
    val lastUserText = userMessages.lastOrNull()?.content.orEmpty().trim()
    val turnNumber = userMessages.size
    val correction = detectCorrection(lastUserText)

    val template = chooseContextualReply(
      lastUserText = lastUserText,
      turnNumber = turnNumber,
      scenarioId = scenarioId,
      accent = accent,
      character = character,
      level = level,
      previousAssistantTexts = assistantHistory,
    )

    val replyText = selectByCharacterAndLevel(template, character, level)
    val wordCount = lastUserText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val xpEarned = (level.baseXp + (wordCount / 4).coerceAtMost(5)).coerceIn(10, 25)
    val coachNote = buildCoachNoteEs(
      level = level,
      scenarioId = scenarioId,
      replyText = replyText,
      translationEs = template.translationEs,
      correction = correction,
      suggestions = template.suggestions,
      tips = template.tips,
    )

    return TutorReply(
      reply = replyText,
      translationEs = template.translationEs,
      correction = correction,
      suggestedReplies = template.suggestions,
      pronunciationFocus = template.tips,
      xpEarned = xpEarned,
      coachNoteEs = coachNote,
    )
  }

  private fun buildCoachNoteEs(
    level: LearnerLevel,
    scenarioId: String,
    replyText: String,
    translationEs: String,
    correction: Correction?,
    suggestions: List<String>,
    tips: List<PronunciationTip>,
  ): String {
    val exampleStarter = suggestions.firstOrNull().orEmpty()
    val wordTip = tips.firstOrNull()?.let { "${it.word} (${it.tipEs})" }
    val scenarioFocus = ScenarioCatalog.byId(scenarioId).focusSkillEs

    return when (level) {
      LearnerLevel.NOVATO -> buildString {
        append("Guía rápida (A1): Te acabo de decir: \"$translationEs\". ")
        if (exampleStarter.isNotBlank()) {
          append("Para responder fácil usa una frase corta como \"$exampleStarter\". ")
        }
        if (correction != null) {
          append("Ojo con el detalle de arriba: ${correction.why}")
        } else if (wordTip != null) {
          append("Pronunciación clave: $wordTip")
        }
      }.trim()

      LearnerLevel.JUNIOR -> buildString {
        append("Nota A2 ($scenarioFocus): ")
        if (correction != null) {
          append("Buen intento; ${correction.why} ")
        } else {
          append("Responde conectando una idea en presente o pasado simple. ")
        }
        if (exampleStarter.isNotBlank()) {
          append("Puedes inspirarte en: \"$exampleStarter\".")
        }
      }.trim()

      LearnerLevel.INTERMEDIO -> buildString {
        append("Tip B1 ($scenarioFocus): ")
        if (wordTip != null) {
          append("Fíjate en '$wordTip'. ")
        } else if (replyText.contains("?")) {
          append("Responde dando una razón con 'because' o 'however' para ganar fluidez. ")
        }
        if (correction != null) {
          append("Ajuste clave: ${correction.why}")
        }
      }.trim()

      LearnerLevel.INTERMEDIO_ALTO -> buildString {
        append("Guía B2 ($scenarioFocus): ")
        if (correction != null) {
          append("Ajuste recomendado: ${correction.why} ")
        } else {
          append("Conecta ideas con 'although', 'whereas' o condicionales para sonar más natural. ")
        }
        if (wordTip != null) {
          append("Foco de voz: $wordTip")
        }
      }.trim()

      LearnerLevel.PRO -> buildString {
        append("Matiz C1 ($scenarioFocus): ")
        if (correction != null) {
          append("Refina este detalle: ${correction.why} ")
        } else {
          append("Busca precisión léxica o un conector de contraste para elevar tu respuesta. ")
        }
        if (wordTip != null) {
          append("Detalle fonético: $wordTip")
        }
      }.trim()

      LearnerLevel.MAESTRIA -> buildString {
        append("Estilo C2 ($scenarioFocus): ")
        if (correction != null) {
          append("Pulido retórico: ${correction.why} ")
        } else {
          append("Juega con inversión formal ('Were we to...', 'Not only...') o sutileza idiomática para precisión nativa. ")
        }
        if (wordTip != null) {
          append("Matiz fonético: $wordTip")
        }
      }.trim()
    }
  }

  private fun chooseContextualReply(
    lastUserText: String,
    turnNumber: Int,
    scenarioId: String,
    accent: EnglishAccent,
    character: TutorCharacter,
    level: LearnerLevel,
    previousAssistantTexts: List<String>,
  ): StepTemplate {
    val lower = lastUserText.lowercase().trim()
    val usedLower = previousAssistantTexts.map { it.lowercase() }

    fun isUnused(template: StepTemplate): Boolean {
      val text = selectByCharacterAndLevel(template, character, level).lowercase()
      val questionPart = text.substringAfterLast(". ", text).trim()
      return usedLower.none { prev ->
        prev == text || (questionPart.length > 14 && prev.contains(questionPart))
      }
    }

    // 1. Direct questions or meta requests to the tutor
    directQuestionReply(lower, character, previousAssistantTexts)?.let {
      if (isUnused(it)) return it
    }

    // 2. Personal disclosures (name, place, job, age)
    personalDisclosureReply(lastUserText, lower)?.let {
      if (isUnused(it)) return it
    }

    // 3. Emotions & mood
    emotionReply(lower)?.let {
      if (isUnused(it)) return it
    }

    // 4. Scenario-specific reactive intents
    scenarioSpecificIntentReply(scenarioId, lower, accent)?.let {
      if (isUnused(it)) return it
    }

    // 5. General conversational topic match (works everywhere when user goes free-form)
    generalTopicReply(lower)?.let {
      if (isUnused(it)) return it
    }

    // 6. Scenario progression (only if unused and user followed scenario flow)
    val scenarioCandidates = scenarioFlowCandidates(scenarioId, turnNumber, lastUserText, accent)
    scenarioCandidates.firstOrNull { isUnused(it) }?.let { return it }

    // 7. Dynamic reflection using user's actual words so we NEVER ignore or repeat!
    val dynamicPool = buildDynamicReflectionPool(lastUserText, turnNumber)
    return dynamicPool.firstOrNull { isUnused(it) }
      ?: dynamicPool[turnNumber % dynamicPool.size]
  }

  private fun directQuestionReply(
    lower: String,
    character: TutorCharacter,
    previousAssistantTexts: List<String>,
  ): StepTemplate? {
    val tutorName = character.displayName

    if (Regex("\\b(no entiendo|ayuda|don't understand|do not understand|repeat|say that again|speak slower|what did you say)\\b").containsMatchIn(lower)) {
      val lastPrompt = previousAssistantTexts.lastOrNull()
        ?.replace(Regex("\\([^)]*\\)"), "")
        ?.trim()
        ?: "Let us take it step by step."
      return StepTemplate(
        manuEn = "No worries at all! Let me say it more simply: $lastPrompt Take your time!",
        eddyEn = "All good, my friend! Here it is slowly: $lastPrompt You got this!",
        novatoEn = "No problem! Slowly: $lastPrompt",
        proEn = "Of course—let me rephrase that clearly: $lastPrompt Feel free to answer in your own words.",
        translationEs = "¡Sin problema! Te lo repito más sencillo: $lastPrompt Tómate tu tiempo.",
        suggestions = listOf(
          "Okay, now I understand!",
          "Can you give me an example?",
          "Let me try to answer now.",
        ),
        tips = listOf(
          PronunciationTip("worries", "/ˈwɜːriz/", "Suena 'wér-is', muy usada para decir sin problema."),
        ),
      )
    }

    if (Regex("\\bhow are you\\b|\\bhow's it going\\b|\\bhow are u\\b|\\bwhat's up\\b|\\bwhats up\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "I am doing wonderfully, thanks for asking! Helping you practice always brightens my day. How are you feeling today?",
        eddyEn = "Doing awesome, thanks! I just had my coffee so I am wide awake. How is your energy today?",
        novatoEn = "I am very good, thank you! And you? How are you today?",
        proEn = "I am doing fantastic, appreciate you asking! It is always energizing to start a fresh session. How has your day been treating you?",
        translationEs = "¡Estoy muy bien, gracias por preguntar! ¿Cómo te sientes tú hoy?",
        suggestions = listOf(
          "I am doing great today, thank you!",
          "A little tired, but ready to learn.",
          "Pretty good! I am excited to practice.",
        ),
        tips = listOf(
          PronunciationTip("wonderfully", "/ˈwʌndərfəli/", "Acento al inicio: 'wán-der-fu-li'."),
        ),
      )
    }

    if (Regex("\\bwhat is your name\\b|\\bwhat's your name\\b|\\bwho are you\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "I am $tutorName, your personal English speaking coach! And what should I call you?",
        eddyEn = "I am $tutorName, your friendly conversation buddy! What is your name?",
        novatoEn = "My name is $tutorName! What is your name?",
        proEn = "I am $tutorName, your AI speaking coach here to help you sound effortless. Tell me, what is your name and what brings you here today?",
        translationEs = "Soy $tutorName, tu tutor de inglés. ¿Cómo te llamas tú?",
        suggestions = listOf(
          "My name is Carlos. Nice to meet you!",
          "You can call me Ana.",
          "Nice to meet you, $tutorName!",
        ),
      )
    }

    if (Regex("\\bwhere are you from\\b|\\bwhere do you live\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "I live right here in your pocket, ready whenever you want to practice! Where are you talking to me from?",
        eddyEn = "I am from the cloud—great Wi-Fi, zero traffic! How about you, which city are you in right now?",
        novatoEn = "I live in your phone! Where are you from?",
        proEn = "I hail from the digital cloud, which means zero commute and plenty of time to chat! Where are you connecting from today?",
        translationEs = "¡Vivo en tu teléfono listo para practicar! ¿Desde qué ciudad me hablas tú?",
        suggestions = listOf(
          "I am from Mexico.",
          "I live in Colombia right now.",
          "Haha, that is funny! I am in Spain.",
        ),
      )
    }

    if (Regex("\\bthank you\\b|\\bthanks\\b|\\bappreciate it\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "You are so welcome! Every sentence you say makes your English stronger. What else would you like to talk about?",
        eddyEn = "Anytime! You are doing awesome. Want to keep chatting or tell me what is next in your day?",
        novatoEn = "You are welcome! What do you want to do next?",
        proEn = "My pleasure! You are building real momentum today. Where would you like to steer our conversation next?",
        translationEs = "¡Con mucho gusto! Cada frase fortalece tu inglés. ¿De qué más te gustaría hablar?",
        suggestions = listOf(
          "Let us keep practicing!",
          "Ask me another question.",
          "I want to tell you about my plans.",
        ),
        tips = listOf(
          PronunciationTip("welcome", "/ˈwelkəm/", "Termina suave en 'kom': 'wél-kom'."),
        ),
      )
    }

    if (Regex("\\b(goodbye|bye|see you|have to go|gotta go)\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "It was amazing chatting with you today! Tap Finalizar above whenever you are ready to see your fluency summary.",
        eddyEn = "Catch you later! You crushed it today. Hit Finalizar at the top to grab your bonus XP!",
        novatoEn = "Goodbye! Great job today. Tap Finalizar to finish!",
        proEn = "Great session today! Whenever you are ready to wrap up, tap Finalizar above to review your takeaways and earn bonus XP.",
        translationEs = "¡Fue genial charlar contigo hoy! Toca 'Finalizar' arriba cuando quieras ver tu resumen y ganar XP extra.",
        suggestions = listOf(
          "See you next time!",
          "Actually, let us do one more question.",
        ),
      )
    }

    return null
  }

  private fun personalDisclosureReply(rawText: String, lower: String): StepTemplate? {
    val nonNameWords = adjectiveStopList + setOf(
      "an", "the", "not", "just", "very", "really", "also", "still",
      "living", "practicing", "feeling", "having", "getting", "trying", "into",
    )
    val nameMatch = Regex(
      "\\b(?:my name is|call me|i am|i'm)\\s+([A-Za-zÁÉÍÓÚáéíóúÑñ]{2,18})\\b",
      RegexOption.IGNORE_CASE,
    ).find(rawText.trim())
    if (nameMatch != null) {
      val candidate = nameMatch.groupValues[1]
      if (candidate.lowercase() !in nonNameWords && !lower.contains("years")) {
        val cleanName = candidate.replaceFirstChar { it.uppercaseChar() }
        return StepTemplate(
          manuEn = "Awesome to meet you, $cleanName! You have a great name. Tell me, what do you enjoy doing in your free time?",
          eddyEn = "Nice to meet you, $cleanName! Glad you are here. What kind of things are you into—music, sports, tech, or something else?",
          novatoEn = "Nice to meet you, $cleanName! What do you like to do?",
          proEn = "Pleasure meeting you, $cleanName! Always great putting a name to the voice. What are you most passionate about outside of work or study?",
          translationEs = "¡Mucho gusto, $cleanName! Cuéntame, ¿qué te gusta hacer en tu tiempo libre?",
          suggestions = listOf(
            "I like listening to music and watching movies.",
            "I enjoy working out and spending time with friends.",
            "I love learning new technology.",
          ),
          tips = listOf(
            PronunciationTip("pleasure", "/ˈpleʒər/", "Suena suave como 'plé-yer'."),
          ),
        )
      }
    }

    val placeMatch = Regex(
      "\\b(?:i am from|i'm from|i live in|from)\\s+([A-Za-zÁÉÍÓÚáéíóúÑñ]+(?:\\s+[A-Za-zÁÉÍÓÚáéíóúÑñ]+)?)",
      RegexOption.IGNORE_CASE,
    ).find(rawText.trim())
    if (placeMatch != null) {
      val place = placeMatch.groupValues[1].trim()
      if (place.lowercase() !in setOf("here", "there", "home", "work")) {
        val nicePlace = place.split(" ").joinToString(" ") { w ->
          w.replaceFirstChar { it.uppercaseChar() }
        }
        return StepTemplate(
          manuEn = "Oh, $nicePlace! I have heard wonderful things about the people and food there. What is your favorite thing about living in $nicePlace?",
          eddyEn = "No way, $nicePlace! That sounds like an awesome place. What is the best food or spot to visit in $nicePlace?",
          novatoEn = "Cool! $nicePlace is nice! Do you like the food there?",
          proEn = "$nicePlace sounds vibrant! Every place has its own rhythm—what do you love most about life in $nicePlace?",
          translationEs = "¡Qué genial, $nicePlace! ¿Qué es lo que más te gusta de vivir allí o de su comida?",
          suggestions = listOf(
            "The food and the weather are amazing.",
            "People here are very warm and friendly.",
            "There are great parks and coffee shops.",
          ),
        )
      }
    }

    val jobMatch = Regex("\\b(?:i work as a|i work as an|i am a|i'm a)\\s+([a-zA-Z]{3,22})\\b")
      .find(lower)
    if (jobMatch != null) {
      val role = jobMatch.groupValues[1]
      if (role !in adjectiveStopList && role !in setOf("person", "human", "man", "woman", "boy", "girl")) {
        return StepTemplate(
          manuEn = "Being a $role sounds super interesting! What is the best part of your work as a $role?",
          eddyEn = "Oh nice, a $role! Respect! Do you use English much in your day-to-day work?",
          novatoEn = "Cool! A $role! Do you like your job?",
          proEn = "Working as a $role takes real skill. What kind of challenges or projects keep you busiest in that role right now?",
          translationEs = "¡Trabajar en $role suena muy interesante! ¿Qué es lo que más disfrutas de tu trabajo?",
          suggestions = listOf(
            "I enjoy solving problems and learning new things.",
            "Yes, I want to use more English at work.",
            "My favorite part is working with my team.",
          ),
        )
      }
    }

    val ageMatch = Regex("\\b(?:i am|i'm|i have)\\s+(\\d{1,2})\\s*(?:years(?:\\s+old)?)?\\b").find(lower)
    if (ageMatch != null) {
      val age = ageMatch.groupValues[1]
      return StepTemplate(
        manuEn = "Got it, $age is a great age! You have so much ahead of you. What is one big dream or goal you have this year?",
        eddyEn = "Nice! $age is prime time! What is something fun or exciting you want to achieve this year?",
        novatoEn = "Great! What is one goal you have this year?",
        proEn = "Appreciate you sharing that! At $age, what milestone—personal or professional—are you most focused on achieving next?",
        translationEs = "¡Genial! A los $age años, ¿qué meta o sueño te gustaría lograr este año?",
        suggestions = listOf(
          "I want to speak English fluently.",
          "I want to travel to a new country.",
          "I want to grow in my career.",
        ),
      )
    }

    return null
  }

  private fun emotionReply(lower: String): StepTemplate? {
    if (Regex("\\b(tired|exhausted|sleepy|stressed|nervous|anxious|busy|sick|sad|bad|terrible)\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "I hear you—some days really take a lot of energy. Let us keep this relaxed and easy. What helps you unwind after a long day?",
        eddyEn = "Aww, rough day? Take a deep breath, zero pressure here! What usually helps you recharge when you feel worn out?",
        novatoEn = "I understand. Rest is important! Do you like music or tea to relax?",
        proEn = "Totally understandable—we all hit days like that. Taking five minutes to focus on something positive helps. What is your go-to way to recharge?",
        translationEs = "Te entiendo, a veces el día pesa mucho. ¿Qué te ayuda a desconectar y recargar energías?",
        suggestions = listOf(
          "Listening to music and drinking tea helps me.",
          "I like going for a walk outside.",
          "Watching a funny show helps me relax.",
        ),
        tips = listOf(
          PronunciationTip("unwind", "/ʌnˈwaɪnd/", "Significa relajarse: 'an-wáind'."),
        ),
      )
    }

    if (Regex("\\b(happy|excited|amazing|awesome|wonderful|fantastic|great|good|well)\\b").containsMatchIn(lower) &&
      !lower.contains("how are you")
    ) {
      return StepTemplate(
        manuEn = "I love hearing that positive energy! When you feel good, learning flows twice as fast. What put you in such a good mood?",
        eddyEn = "Yes! Love that vibe! Tell me what good news or little win made your day so good?",
        novatoEn = "That is great! Why are you happy today?",
        proEn = "That is wonderful to hear—positive momentum makes all the difference. What sparked that great mood today?",
        translationEs = "¡Me encanta esa buena energía! ¿Qué buena noticia o detalle te puso de tan buen humor?",
        suggestions = listOf(
          "I learned something new today!",
          "I had a nice time with my family.",
          "The weather is really nice today.",
        ),
      )
    }

    return null
  }

  private fun scenarioSpecificIntentReply(
    scenarioId: String,
    lower: String,
    accent: EnglishAccent,
  ): StepTemplate? {
    // Price / cost questions in cafes, restaurants, shopping
    if (Regex("\\b(how much|price|cost|expensive|cheap|dollars|pay|card|cash|discount)\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "Great question! It is five dollars and fifty cents, and we accept card, phone, or cash. Would you like to go ahead with that?",
        eddyEn = "Super reasonable—just five fifty! Card, contactless, or cash all work here. Should I ring that up for you?",
        novatoEn = "It is five dollars! Card or cash?",
        proEn = "That comes to five dollars and fifty cents, and we accept tap-to-pay, cards, or cash. Would you like me to finalize that for you?",
        translationEs = "¡Buena pregunta! Cuesta cinco dólares con cincuenta y aceptamos tarjeta, celular o efectivo. ¿Lo preparamos?",
        suggestions = listOf(
          "Yes, I will pay by card, please.",
          "That is a good price. I will take it!",
          "Do you have anything slightly smaller?",
        ),
        tips = listOf(
          PronunciationTip("cost", "/kɔːst/", "Vocal abierta y T suave al final."),
        ),
      )
    }

    // Drinks / water / juice / food preferences
    if (scenarioId in setOf("coffee_shop", "restaurant") &&
      Regex("\\b(water|juice|tea|soda|beer|wine|pizza|pasta|salad|chicken|sandwich|burger|vegan|vegetarian|allergy|sugar|milk)\\b").containsMatchIn(lower)
    ) {
      val item = Regex("\\b(water|juice|tea|soda|coffee|latte|pizza|pasta|salad|chicken|sandwich|burger)\\b")
        .find(lower)?.groupValues?.get(1) ?: "item"
      return StepTemplate(
        manuEn = "Of course! Our $item is super fresh today. Would you like anything else on the side, or is that everything for now?",
        eddyEn = "Coming right up! Great choice with the $item. Want anything else with it, or should I put that order in?",
        novatoEn = "Yes, we have $item! Anything else?",
        proEn = "Certainly, our $item is very popular today. Would you care for anything alongside it, or shall I send that straight to preparation?",
        translationEs = "¡Por supuesto! Nuestro $item está fresco hoy. ¿Deseas agregar algo más o está bien así?",
        suggestions = listOf(
          "That is everything for now, thank you!",
          "Yes, could I also have some water?",
          "How long will it take?",
        ),
      )
    }

    // Bathroom / wifi / menu questions
    if (Regex("\\b(bathroom|restroom|toilet|wifi|wi-fi|password|menu)\\b").containsMatchIn(lower)) {
      return StepTemplate(
        manuEn = "Sure thing! The restroom is right down the hall to the left, and the Wi-Fi password is right on your receipt. Can I help you with anything else?",
        eddyEn = "Easy! Restroom is around the corner on the left, and our Wi-Fi is free with no password. Anything else you need?",
        novatoEn = "It is on the left! Anything else?",
        proEn = "Certainly—the restroom is down the corridor to your left, and complimentary Wi-Fi is available under FluentGuest. Is there anything else I can get you?",
        translationEs = "¡Claro! El baño está al fondo a la izquierda y el Wi-Fi es gratuito. ¿Te ayudo con algo más?",
        suggestions = listOf(
          "Thank you, that is very helpful!",
          "Yes, could I order one more thing?",
        ),
      )
    }

    // Directions / city transport
    if (scenarioId == "directions_city" &&
      Regex("\\b(station|bus|train|metro|subway|pharmacy|bank|hospital|hotel|museum|airport|street|walk|far)\\b").containsMatchIn(lower)
    ) {
      return StepTemplate(
        manuEn = "Easy! Walk straight for two blocks, then turn right at the traffic light—you will see it across the street in five minutes. Are you walking or taking transport?",
        eddyEn = "Super close! Go straight two blocks, take a right at the corner cafe, and boom, right there! Walking or hopping on the bus?",
        novatoEn = "Go straight two blocks and turn right. Are you walking?",
        proEn = "It is actually quite straightforward: head straight for two blocks, turn right at the main intersection, and it will be on your left. Would you prefer to walk or catch the subway?",
        translationEs = "¡Fácil! Camina dos cuadras recto y gira a la derecha en el semáforo. ¿Vas caminando o en transporte?",
        suggestions = listOf(
          "I think I will walk, five minutes is close.",
          "Which bus or train goes there?",
          "Thank you so much for the clear directions!",
        ),
        tips = listOf(
          PronunciationTip("straight", "/streɪt/", "La GH es muda: suena 'stréit'."),
        ),
      )
    }

    // Airport / baggage / gate
    if (scenarioId == "airport_travel" &&
      Regex("\\b(passport|ticket|bag|luggage|suitcase|carry-on|window|aisle|gate|delay|flight|london|madrid|york|miami)\\b").containsMatchIn(lower)
    ) {
      return StepTemplate(
        manuEn = "Everything is checked in smoothly! Your gate is B12 near the coffee lounge, and boarding starts in thirty minutes. Do you need a window or aisle seat preference noted?",
        eddyEn = "Awesome, all set! Your flight leaves from Gate B12. Want me to put you near the window for the view, or aisle for legroom?",
        novatoEn = "Good! Gate B12! Window seat or aisle seat?",
        proEn = "Wonderful, your luggage and boarding details are confirmed for Gate B12. Before I print your pass, would you prefer a window or aisle seat?",
        translationEs = "¡Todo registrado! Tu puerta es la B12. ¿Prefieres asiento de ventana o pasillo?",
        suggestions = listOf(
          "A window seat, please.",
          "An aisle seat so I can move easily.",
          "What time does boarding begin?",
        ),
        tips = listOf(
          PronunciationTip("aisle", "/aɪl/", "La S es muda: suena 'áil'."),
        ),
      )
    }

    // Doctor symptoms
    if (scenarioId == "doctor_visit" &&
      Regex("\\b(headache|fever|cough|pain|throat|stomach|cold|flu|tired|dizzy|days|yesterday|medicine|pills)\\b").containsMatchIn(lower)
    ) {
      return StepTemplate(
        manuEn = "Thank you for describing that clearly. Make sure to rest, stay hydrated, and take a mild pain reliever after meals. Do you have any allergies to medication?",
        eddyEn = "Got it, sorry you are feeling rough! Drink lots of water and get some rest. Before I write the note, are you allergic to any medicine?",
        novatoEn = "Okay, drink water and rest! Do you have any allergies?",
        proEn = "Those symptoms sound like a mild viral bug. I recommend plenty of fluids and rest over the next two days. Before I prescribe anything, do you have any medication allergies?",
        translationEs = "Gracias por explicarlo claro. Descansa e hidrátate bien. ¿Tienes alguna alergia a medicamentos?",
        suggestions = listOf(
          "No, I do not have any allergies.",
          "Yes, I am allergic to penicillin.",
          "How often should I take the medicine?",
        ),
        tips = listOf(
          PronunciationTip("medicine", "/ˈmedɪsɪn/", "Se dice en 2 o 3 golpes suaves: 'mé-di-sin'."),
        ),
      )
    }

    // Customer support
    if (scenarioId == "customer_support" &&
      Regex("\\b(package|order|damaged|broken|refund|replace|money|charged|twice|late|missing|delivery)\\b").containsMatchIn(lower)
    ) {
      return StepTemplate(
        manuEn = "I completely understand your frustration and I am fixing this right away. Would you prefer an express free replacement or a full refund to your card?",
        eddyEn = "Ouch, totally our bad—let us make it right! Do you want me to ship a brand-new replacement today or refund your card right now?",
        novatoEn = "Sorry about that! Do you want a new one or your money back?",
        proEn = "I sincerely apologize for the inconvenience—that is definitely not our standard. Would you rather I dispatch an express replacement today or issue an immediate full refund?",
        translationEs = "Entiendo totalmente tu molestia y lo resuelvo ya mismo. ¿Prefieres un reemplazo exprés gratis o reembolso total a tu tarjeta?",
        suggestions = listOf(
          "A free express replacement, please.",
          "I would prefer a full refund to my card.",
          "When will I receive confirmation?",
        ),
        tips = listOf(
          PronunciationTip("refund", "/ˈriːfʌnd/", "Sustantivo con acento al inicio: 'rí-fand'."),
        ),
      )
    }

    // Salary / pitch / AI debate / standup / interview
    if (scenarioId in setOf("job_interview", "salary_negotiation", "daily_standup", "client_pitch", "ai_future_debate") &&
      Regex("\\b(salary|offer|remote|bonus|experience|team|project|client|roi|ai|future|creative|jobs|blocker|meeting|strength|problem)\\b").containsMatchIn(lower)
    ) {
      return StepTemplate(
        manuEn = "That is a very sharp and clear point! Supporting your idea with a real example makes it twice as persuasive. Could you give one brief example or number to back that up?",
        eddyEn = "Solid point! You sound super confident right there. Give me one quick example or story that proves it!",
        novatoEn = "Very good point! Can you give one small example?",
        proEn = "Compelling perspective—that shows genuine strategic thinking. How would you back that point up with a concrete metric or real-world example?",
        translationEs = "¡Excelente argumento! ¿Podrías respaldar esa idea con un ejemplo breve o un dato concreto?",
        suggestions = listOf(
          "For example, last year we improved delivery speed by twenty percent.",
          "In my previous team, I led a similar project successfully.",
          "That is a great question—let me explain how it works.",
        ),
      )
    }

    if (scenarioId == "coffee_shop" && accent == EnglishAccent.UK && lower.contains("tea")) {
      return StepTemplate(
        manuEn = "A classic cup of British tea—lovely! Would you like milk or lemon with that?",
        eddyEn = "Proper cup of tea! Milk, honey, or straight up?",
        novatoEn = "Nice! Milk or lemon in your tea?",
        proEn = "A splendid choice—we brew fresh Earl Grey and English Breakfast. Would you take yours with milk or a slice of lemon?",
        translationEs = "¡Un clásico té británico! ¿Lo prefieres con leche o con limón?",
        suggestions = listOf(
          "Just a splash of milk, please.",
          "With a slice of lemon, thank you.",
        ),
      )
    }

    return null
  }

  private fun generalTopicReply(lower: String): StepTemplate? {
    return when {
      Regex("\\b(music|song|songs|singer|band|movie|movies|film|series|netflix|youtube|podcast|book|read)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Oh, I love that! Stories and music are one of the funnest ways to absorb natural English. What specific artist, show, or book has caught your attention lately?",
          eddyEn = "Nice taste! Nothing beats getting hooked on a great song or show. Which one could you recommend to me right now?",
          novatoEn = "Cool! What song, movie, or book do you like most?",
          proEn = "Media is such a rich bridge into idiomatic English. What is one show, film, or album you have had on repeat lately—and why does it resonate with you?",
          translationEs = "¡Qué genial! ¿Qué artista, serie, película o libro te ha llamado más la atención últimamente?",
          suggestions = listOf(
            "I really like watching comedy and sci-fi shows.",
            "I listen to pop and rock music every morning.",
            "I am reading a short book in English right now.",
          ),
          tips = listOf(
            PronunciationTip("genre", "/ˈʒɑːnrə/", "Suena suave como 'yán-ra'."),
          ),
        )

      Regex("\\b(work|job|office|company|code|coding|software|program|design|engineer|business|client|boss|colleague)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Work keeps life busy! Practicing English for your career opens amazing global doors. What kind of tasks take up most of your week?",
          eddyEn = "Respect the hustle! What is the coolest—or trickiest—part of your work week right now?",
          novatoEn = "Good job! Do you work from home or in an office?",
          proEn = "Career growth is one of the strongest reasons to master English. What kind of projects or challenges are keeping you sharpest at work lately?",
          translationEs = "¡El trabajo te mantiene activo! ¿Qué tipo de tareas o proyectos ocupan más tu semana?",
          suggestions = listOf(
            "I work from home and talk with teammates online.",
            "I am building new projects and learning every day.",
            "I want to use English for international clients.",
          ),
          tips = listOf(
            PronunciationTip("colleague", "/ˈkɑːliːɡ/", "Suena 'ká-lig', con la U muda."),
          ),
        )

      Regex("\\b(study|studying|school|university|college|english|learn|learning|practice|class|exam)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Your dedication to learning really shows! Speaking out loud even ten minutes a day makes a huge difference. Which skill feels easiest for you—listening, reading, or speaking?",
          eddyEn = "Awesome mindset! Learning a language is like going to the gym for your brain. Which part do you want to level up fastest?",
          novatoEn = "You are doing great! Is English easy or hard for you today?",
          proEn = "Consistency beats intensity every single time in language mastery. Which aspect of English—pronunciation, vocabulary nuance, or spontaneous speaking—do you want to sharpen next?",
          translationEs = "¡Se nota tu dedicación! ¿Qué habilidad quieres mejorar primero: oído, vocabulario o hablar fluido?",
          suggestions = listOf(
            "I want to speak faster without translating in my head.",
            "Listening to native speed is my biggest challenge.",
            "I want to learn useful everyday expressions.",
          ),
        )

      Regex("\\b(food|eat|eating|dinner|lunch|breakfast|cook|cooking|restaurant|pizza|tacos|coffee|hungry)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Now we are talking about my favorite topic—food! Do you enjoy cooking your own meals or discovering new places to eat?",
          eddyEn = "Okay, you officially made me hungry! What is your all-time favorite dish that never disappoints?",
          novatoEn = "Yum! What is your favorite food?",
          proEn = "Food always sparks the best conversations! Are you someone who loves experimenting with recipes at home, or do you have a favorite local spot?",
          translationEs = "¡Mi tema favorito: la comida! ¿Prefieres cocinar en casa o cuál es tu plato favorito?",
          suggestions = listOf(
            "I love cooking pasta and fresh salads at home.",
            "My favorite food is tacos with spicy salsa.",
            "I like trying new restaurants on weekends.",
          ),
          tips = listOf(
            PronunciationTip("recipe", "/ˈresəpi/", "La E final SÍ suena: 'ré-se-pi'."),
          ),
        )

      Regex("\\b(travel|trip|vacation|country|city|beach|mountain|plane|hotel|visit|holiday)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Traveling gives us the best memories and real reasons to use English! If you could hop on a plane tomorrow, where would you go?",
          eddyEn = "Pack your suitcase! Which dream destination is calling your name this year?",
          novatoEn = "Nice! Where do you want to travel?",
          proEn = "Travel stretches both our perspective and our vocabulary. If you had two free weeks anywhere in the world, where would you go and what would you do first?",
          translationEs = "¡Viajar crea los mejores recuerdos! Si pudieras viajar mañana mismo, ¿a qué lugar irías?",
          suggestions = listOf(
            "I would love to visit London or New York.",
            "I prefer relaxing on a quiet beach.",
            "I want to explore mountains and nature.",
          ),
        )

      Regex("\\b(sport|sports|soccer|football|basketball|gym|run|running|exercise|walk|game|gaming|hobby|hobbies|weekend)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Staying active and having fun hobbies keeps life balanced! How often do you make time for that during the week?",
          eddyEn = "Love that! Having hobbies keeps life awesome. Do you do that solo to unwind or with friends?",
          novatoEn = "Fun! How often do you do that?",
          proEn = "Having outlets outside daily responsibilities is essential. What got you into that activity, and how often do you carve out time for it?",
          translationEs = "¡Qué buen pasatiempo! ¿Sueles hacerlo solo para desconectar o con amigos?",
          suggestions = listOf(
            "Usually two or three times a week.",
            "I mostly do it on weekends with friends.",
            "Whenever I have thirty free minutes!",
          ),
        )

      Regex("\\b(family|mom|dad|mother|father|sister|brother|friend|friends|dog|cat|pet|kids|son|daughter|wife|husband)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "Family, friends, and pets mean everything! They always make the best stories. What do you enjoy doing together most?",
          eddyEn = "Aww, love that! Good company makes any day ten times better. What did you all do together recently?",
          novatoEn = "That is very nice! Do you spend weekends together?",
          proEn = "Our closest circles shape so many of our best stories. What kind of traditions or favorite memories do you share together?",
          translationEs = "¡La familia, los amigos y las mascotas lo son todo! ¿Qué disfrutan hacer juntos?",
          suggestions = listOf(
            "We like cooking dinner and talking together.",
            "We usually go out for coffee or a walk.",
            "My pet always makes me laugh every morning.",
          ),
        )

      Regex("\\b(i don't know|dont know|not sure|maybe|depends|no idea)\\b").containsMatchIn(lower) ->
        StepTemplate(
          manuEn = "No problem at all—it is totally okay to think out loud! Let me make it easier: do you prefer quiet mornings at home or busy evenings out with friends?",
          eddyEn = "Haha, fair enough! Let us pick something super easy: coffee or tea, and early bird or night owl?",
          novatoEn = "That is okay! Coffee or tea?",
          proEn = "Totally fine to mull it over! Let us pivot to a quick either-or: do you feel more creative early in the morning or late at night?",
          translationEs = "¡No pasa nada! Hagámoslo fácil: ¿prefieres mañanas tranquilas en casa o planes activos con amigos?",
          suggestions = listOf(
            "I definitely prefer quiet mornings with coffee!",
            "I am more of a night owl.",
            "I like a mix of both depending on the day.",
          ),
        )

      else -> null
    }
  }

  private fun scenarioFlowCandidates(
    scenarioId: String,
    turn: Int,
    userText: String,
    accent: EnglishAccent,
  ): List<StepTemplate> = when (scenarioId) {
    "coffee_shop" -> coffeeShopCandidates(userText, accent)
    "job_interview" -> listOf(
      StepTemplate(
        manuEn = "That sounds very solid! What do you consider your biggest strength when working with a team?",
        eddyEn = "Nice! Tell me, what is that one strength your teammates always count on?",
        novatoEn = "Good! What is one thing you do very well?",
        proEn = "Appreciate that context. What core strength consistently helps you deliver results under tight deadlines?",
        translationEs = "¡Muy bien! ¿Cuál dirías que es tu mayor fortaleza al trabajar en equipo?",
        suggestions = listOf("I stay calm and solve problems step by step.", "I communicate clearly with everyone."),
        tips = listOf(PronunciationTip("strength", "/streŋθ/", "Termina con TH suave.")),
      ),
      StepTemplate(
        manuEn = "That is a valuable skill! Could you tell me about a recent challenge you handled well?",
        eddyEn = "Love it! Give me a quick story about a tricky problem you solved lately.",
        novatoEn = "Great! Tell me about one small problem you fixed.",
        proEn = "Very impressive. Could you walk me through a specific challenge you turned around recently?",
        translationEs = "¡Gran habilidad! ¿Puedes contarme brevemente un reto que hayas resuelto bien?",
        suggestions = listOf("Last month I reorganized a tight schedule and met the goal."),
        tips = listOf(PronunciationTip("challenge", "/ˈtʃælɪndʒ/", "Empieza fuerte como 'chál-inch'.")),
      ),
    )
    "airport_travel" -> listOf(
      StepTemplate(
        manuEn = "Great! Are you checking any suitcases today, or traveling light with just a carry-on?",
        eddyEn = "Awesome! Got any big bags to check in, or just carry-on today?",
        novatoEn = "Thank you! One bag or two bags today?",
        proEn = "Your travel details look ready. Will you be checking luggage into the hold today, or flying with hand luggage only?",
        translationEs = "¡Genial! ¿Vas a facturar maleta hoy o viajas solo con equipaje de mano?",
        suggestions = listOf("Just one carry-on bag, please.", "I have one suitcase to check in."),
        tips = listOf(PronunciationTip("luggage", "/ˈlʌɡɪdʒ/", "Suena 'lá-guich'.")),
      ),
      StepTemplate(
        manuEn = "Perfect! Your flight boards at Gate B12. Would you like a window seat or an aisle seat?",
        eddyEn = "Nice! Before I print your pass, window view or aisle seat?",
        novatoEn = "Great! Window seat or aisle seat?",
        proEn = "Everything is confirmed for Gate B12. Would you prefer a window seat or an aisle seat for extra legroom?",
        translationEs = "¡Perfecto! Embarcas en la puerta B12. ¿Prefieres ventana o pasillo?",
        suggestions = listOf("A window seat, please.", "An aisle seat, thank you."),
        tips = listOf(PronunciationTip("aisle", "/aɪl/", "La S es muda: suena 'áil'.")),
      ),
    )
    "restaurant" -> listOf(
      StepTemplate(
        manuEn = "Right this way! Today's chef special is grilled salmon with vegetables. Are you ready to order, or would you like a drink first?",
        eddyEn = "Follow me! Today we have amazing grilled salmon or homemade pasta. Want a drink first or ready to order food?",
        novatoEn = "Here is your table! Water, juice, or food first?",
        proEn = "Right this way to your table. Tonight our chef recommends the pan-seared salmon. Would you like to start with a beverage?",
        translationEs = "¡Por aquí! El especial es salmón a la parrilla. ¿Te traigo primero algo de tomar o ya deseas ordenar?",
        suggestions = listOf("Sparkling water with lemon, please.", "I would like the grilled salmon."),
        tips = listOf(PronunciationTip("salmon", "/ˈsæmən/", "La L es muda: 'sá-mon'.")),
      ),
    )
    else -> emptyList()
  }.drop((turn - 1).coerceAtLeast(0).coerceAtMost(1))

  private fun coffeeShopCandidates(userText: String, accent: EnglishAccent): List<StepTemplate> {
    val drink = when {
      userText.contains("latte", ignoreCase = true) -> "latte"
      userText.contains("tea", ignoreCase = true) -> "tea"
      userText.contains("cappuccino", ignoreCase = true) -> "cappuccino"
      else -> "coffee"
    }
    val sizeWord = if (accent == EnglishAccent.UK) "regular or large" else "medium or large"
    return listOf(
      StepTemplate(
        manuEn = "Wonderful choice! Would you like your $drink $sizeWord, and hot or iced today?",
        eddyEn = "Ooh, a $drink! Classic pick. Do you want it $sizeWord, and hot or over ice?",
        novatoEn = "Good choice! Hot or iced $drink?",
        proEn = "Excellent taste. Would you like a $sizeWord $drink, and do you prefer it hot or iced?",
        translationEs = "¡Excelente elección! ¿Lo quieres mediano o grande, caliente o frío?",
        suggestions = listOf("Medium and iced, please.", "Hot and large, please."),
        tips = listOf(PronunciationTip("iced", "/aɪst/", "Suena 'áist' con T suave.")),
      ),
      StepTemplate(
        manuEn = "Got it! We also just pulled warm croissants and chocolate muffins from the oven. Would you like to add one?",
        eddyEn = "Awesome! Quick question: warm croissant or blueberry muffin on the side?",
        novatoEn = "Okay! Do you want a muffin or cookie too?",
        proEn = "Perfect. Would you care to pair that with a fresh almond croissant or blueberry muffin?",
        translationEs = "¡Anotado! ¿Te gustaría acompañarlo con un croissant o un muffin?",
        suggestions = listOf("Yes, a chocolate muffin please.", "No thank you, just the drink."),
        tips = listOf(PronunciationTip("pastry", "/ˈpeɪstri/", "Suena 'péis-tri'.")),
      ),
    )
  }

  private fun buildDynamicReflectionPool(lastUserText: String, turnNumber: Int): List<StepTemplate> {
    val words = lastUserText.lowercase()
      .replace(Regex("[^a-z'\\s]"), " ")
      .split(Regex("\\s+"))
      .filter { it.length >= 3 && it !in englishStopwords }
      .distinct()
    val topicWord = words.maxByOrNull { it.length }
    val snippet = lastUserText.trim().take(48).trimEnd('.', '!', '?')

    return listOf(
      StepTemplate(
        manuEn = if (topicWord != null) {
          "Ooh, '$topicWord' caught my ear! Thanks for sharing \"$snippet\". Tell me more—why is $topicWord on your mind today?"
        } else {
          "I appreciate how naturally you shared \"$snippet\"! What made you think about that today?"
        },
        eddyEn = if (topicWord != null) {
          "Haha, love that you brought up $topicWord! Tell me the story behind \"$snippet\"!"
        } else {
          "Nice, I hear you on \"$snippet\"! Give me a bit more detail—what happened next?"
        },
        novatoEn = if (topicWord != null) {
          "Nice! You said $topicWord. Do you like $topicWord?"
        } else {
          "Good English! Tell me one more thing about today."
        },
        proEn = if (topicWord != null) {
          "Interesting point about $topicWord! Unpacking \"$snippet\" a bit further, what perspective or experience shaped your view on that?"
        } else {
          "That is a thoughtful point: \"$snippet\". Could you elaborate a bit on what led you to that view?"
        },
        translationEs = if (topicWord != null) {
          "¡Qué interesante lo que mencionas sobre '$topicWord'! Cuéntame un poco más al respecto."
        } else {
          "¡Me gusta cómo lo expresaste! Cuéntame un poco más de detalle sobre eso."
        },
        suggestions = listOf(
          "Well, let me explain a little more about it.",
          "It started earlier this week and I find it interesting.",
          "Actually, what do you think about it?",
        ),
      ),
      StepTemplate(
        manuEn = "You expressed \"$snippet\" very clearly! If you had to explain that to a friend in English, what is one extra detail you would add?",
        eddyEn = "Solid English right there! Just out of curiosity, how does that fit into the rest of your week?",
        novatoEn = "Very good! And what do you want to do tomorrow?",
        proEn = "Nicely phrased! Taking that thought one step further, how do you see that playing out over the next few weeks?",
        translationEs = "¡Lo expresaste muy claro! ¿Qué detalle extra le agregarías si se lo contaras a un amigo?",
        suggestions = listOf(
          "One extra detail is that I enjoy doing it with friends.",
          "Tomorrow I plan to keep practicing.",
          "Ask me something fun in English!",
        ),
      ),
      StepTemplate(
        manuEn = "Great flow! You are getting noticeably smoother with each reply. Quick pivot: what is one small win you are proud of this week?",
        eddyEn = "Love the rhythm! Let us keep the momentum rolling—what is one funny or cool thing that happened to you recently?",
        novatoEn = "Great job! Tell me one good thing from your week.",
        proEn = "Your phrasing is sounding increasingly natural. Shifting gears slightly—what is one recent win or surprise from your week worth celebrating?",
        translationEs = "¡Excelente fluidez! Cuéntame un pequeño logro o algo bueno que te pasó esta semana.",
        suggestions = listOf(
          "I practiced English without stopping!",
          "I finished an important task on time.",
          "I had a really relaxing afternoon.",
        ),
      ),
      StepTemplate(
        manuEn = "Awesome effort! Keep thinking directly in English without worrying about being perfect. What topic would you enjoy practicing next—travel, work, or daily life?",
        eddyEn = "You are doing awesome! Quick dealer's choice—do you want to talk about travel dreams, favorite food, or everyday life?",
        novatoEn = "Good! Travel, food, or work—which topic do you like?",
        proEn = "Impressive spontaneity! Where would you like to take our conversation next—a hypothetical scenario, career talk, or cultural trends?",
        translationEs = "¡Gran esfuerzo! ¿Qué tema prefieres seguir practicando: viajes, trabajo, comida o vida diaria?",
        suggestions = listOf(
          "Let us talk about travel!",
          "Tell me about your favorite food topic.",
          "Let us practice work and career English.",
        ),
      ),
    ).let { list ->
      val shift = turnNumber % list.size
      list.drop(shift) + list.take(shift)
    }
  }

  fun detectCorrection(userText: String): Correction? {
    val text = userText.trim()
    if (text.isEmpty()) return null
    val lower = text.lowercase()

    Regex("\\bi\\s+have\\s+(\\d+)\\s+years\\b").find(lower)?.let { match ->
      val age = match.groupValues[1]
      return Correction(
        better = "I am $age years old.",
        why = "En inglés la edad se dice con el verbo 'to be' (am/is/are), no con 'have'.",
        tip = "Recuerda: I am $age, o I am $age years old.",
      )
    }

    Regex("\\bi\\s+(?:have|had)\\s+(hungry|thirsty|cold|hot|sleepy|fear)\\b").find(lower)?.let { match ->
      val adj = when (match.groupValues[1]) {
        "fear" -> "scared"
        else -> match.groupValues[1]
      }
      return Correction(
        better = "I am $adj.",
        why = "Hambre, sed, frío, calor o sueño en inglés usan 'to be' (I am...), nunca 'have'.",
        tip = "Ejemplo: I am hungry / I am cold.",
      )
    }

    if (Regex("\\bi\\s+(am|'m)\\s+agree\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I agree with you.",
        why = "'Agree' ya es un verbo, así que no lleva 'am' delante.",
        tip = "Para negar usa: I don't agree.",
      )
    }

    if (Regex("\\bpeople\\s+is\\b").containsMatchIn(lower)) {
      return Correction(
        better = "People are very friendly here.",
        why = "'People' siempre es plural en inglés, por eso lleva 'are'.",
        tip = "Un singular equivalente es 'one person is'.",
      )
    }

    if (Regex("\\b(go|going|went|come)\\s+to\\s+home\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I am going home now.",
        why = "Con 'home' no se usa 'to' cuando indica dirección.",
        tip = "Se dice 'go home', 'come home' y 'get home'.",
      )
    }

    if (Regex("\\bi\\s+no\\s+[a-z]+").containsMatchIn(lower)) {
      return Correction(
        better = "I don't really do that.",
        why = "Para negar verbos en presente simple usamos 'don't' (do not), no solo 'no'.",
        tip = "Usa 'I don't + verbo' en presente y 'I didn't + verbo' en pasado.",
      )
    }

    if (Regex("\\bmore\\s+better\\b|\\bmore\\s+good\\b").containsMatchIn(lower)) {
      return Correction(
        better = "That sounds much better.",
        why = "'Better' ya significa 'más bueno/mejor'; no necesita 'more'.",
        tip = "Para dar énfasis usa 'much better' o 'way better'.",
      )
    }

    if (Regex("\\bdepend\\s+of\\b|\\bdepends\\s+of\\b").containsMatchIn(lower)) {
      return Correction(
        better = "It depends on the day.",
        why = "El verbo 'depend' siempre se acompaña de 'on', nunca de 'of'.",
        tip = "Memoriza el bloque completo: 'it depends on...'",
      )
    }

    if (Regex("\\blike\\s+listen\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I like listening to music.",
        why = "Después de 'like' usa '-ing' o 'to', y 'listen' siempre pide 'to' antes del objeto.",
        tip = "Fórmula segura: listen to + algo.",
      )
    }

    if (Regex("\\bmake\\s+a\\s+party\\b").containsMatchIn(lower)) {
      return Correction(
        better = "We are going to throw a party.",
        why = "En inglés se dice 'throw a party' o 'have a party', no 'make a party'.",
        tip = "Para planes casuales, 'have a party' suena súper natural.",
      )
    }

    if (Regex("\\b(didn't|did not|don't|do not)\\s+went\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I didn't go out yesterday.",
        why = "Después de 'didn't' el verbo vuelve a su forma base ('go', no 'went').",
        tip = "El auxiliar 'didn't' ya marca el pasado.",
      )
    }

    Regex("\\b(he|she|it)\\s+don't\\b").find(lower)?.let { match ->
      val subj = match.groupValues[1].replaceFirstChar { it.uppercaseChar() }
      return Correction(
        better = "$subj doesn't like coffee.",
        why = "Con 'he', 'she' e 'it' la negación es 'doesn't', no 'don't'.",
        tip = "Piensa: he/she/it -> does / doesn't.",
      )
    }

    Regex("\\b(he|she)\\s+have\\b").find(lower)?.let { match ->
      val subj = match.groupValues[1].replaceFirstChar { it.uppercaseChar() }
      return Correction(
        better = "$subj has a lot of energy.",
        why = "En presente simple, 'he' y 'she' usan 'has' en lugar de 'have'.",
        tip = "Solo con I/you/we/they se usa 'have'.",
      )
    }

    if (Regex("\\bexplain\\s+me\\b").containsMatchIn(lower)) {
      return Correction(
        better = "Could you explain that to me?",
        why = "Con 'explain' siempre usamos 'to' antes de la persona: 'explain to me'.",
        tip = "Igual pasa con 'say to me'.",
      )
    }

    if (Regex("\\bin\\s+the\\s+night\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I usually study at night.",
        why = "Para la noche en general se dice 'at night', aunque con mañana y tarde usamos 'in the'.",
        tip = "In the morning, in the afternoon, pero AT night.",
      )
    }

    if (Regex("\\bi\\s+am\\s+go\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I am going to practice more.",
        why = "Después de 'I am' el verbo de acción lleva '-ing' ('going').",
        tip = "Presente continuo: I am + verbo-ing.",
      )
    }

    if (Regex("\\bi\\s+am\\s+boring\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I am feeling a bit bored today.",
        why = "'Boring' describe algo aburrido; si tú sientes aburrimiento, usa 'bored'.",
        tip = "-ed = cómo te sientes; -ing = lo que causa esa sensación.",
      )
    }

    if (Regex("\\b(can|could|must|should|will)\\s+to\\s+[a-z]+").containsMatchIn(lower)) {
      return Correction(
        better = "I can speak more clearly now.",
        why = "Los verbos modales (can, could, should, will, must) nunca llevan 'to' después.",
        tip = "Directo al verbo base: can speak, should go.",
      )
    }

    if (Regex("\\bfor\\s+(buy|eat|sleep|learn|practice|study|work|see)\\b").containsMatchIn(lower)) {
      return Correction(
        better = "I came here to practice English.",
        why = "Para expresar propósito antes de un verbo usa 'to + verbo', no 'for + verbo'.",
        tip = "'For' va con sustantivos (for coffee) o '-ing' (for learning).",
      )
    }

    if (lower.startsWith("is possible") || lower.startsWith("is important") || lower.startsWith("is good")) {
      return Correction(
        better = "It is possible to improve every day.",
        why = "En inglés casi siempre necesitas un sujeto explícito ('It is...') al iniciar.",
        tip = "Evita empezar frases solo con 'Is...'; di 'It is...'.",
      )
    }

    return null
  }

  fun buildSessionSummary(
    character: TutorCharacter,
    level: LearnerLevel,
    scenarioId: String,
    turns: List<ConversationTurn>,
  ): SessionSummary {
    val userTurns = turns.filter { it.role == "user" }
    val correctionsCount = turns.count { it.correction != null }
    val totalWords = userTurns.sumOf { it.content.split(Regex("\\s+")).count { w -> w.isNotBlank() } }
    val avgWords = if (userTurns.isNotEmpty()) totalWords.toFloat() / userTurns.size else 0f
    val scenario = ScenarioCatalog.byId(scenarioId)

    val fluency = (64 + userTurns.size * 5 + (avgWords * 1.4f).toInt() - correctionsCount * 2)
      .coerceIn(55, 98)
    val xpBonus = (userTurns.size * 6 + level.baseXp).coerceIn(15, 60)

    val strengths = buildList {
      add("Practicaste ${userTurns.size} intervenciones en '${scenario.titleEs}' con ${character.displayName}.")
      if (avgWords >= 6f) {
        add("Expresaste ideas completas en lugar de respuestas de una sola palabra.")
      } else {
        add("Mantuviste el ritmo de respuesta en inglés sin bloquearte.")
      }
      if (correctionsCount == 0 && userTurns.isNotEmpty()) {
        add("Excelente precisión gramatical durante toda la charla.")
      }
    }

    val focus = buildList {
      if (correctionsCount > 0) {
        add("Repasa las $correctionsCount correcciones detectadas escuchándolas a 0.7x en tu cuaderno.")
      } else {
        add("Intenta añadir un detalle extra ('because...' o 'especially...') en tu próxima sesión.")
      }
      add("Practica en voz alta las palabras clave destacadas para afianzar la pronunciación.")
    }

    return SessionSummary(
      strengthsEs = strengths,
      focusAreasEs = focus,
      fluencyScore = fluency,
      xpBonus = xpBonus,
    )
  }

  private fun selectByCharacterAndLevel(
    template: StepTemplate,
    character: TutorCharacter,
    level: LearnerLevel,
  ): String {
    return when (level) {
      LearnerLevel.NOVATO -> template.novatoEn
      LearnerLevel.JUNIOR, LearnerLevel.INTERMEDIO ->
        if (character == TutorCharacter.MANU) template.manuEn else template.eddyEn
      LearnerLevel.INTERMEDIO_ALTO ->
        if (character == TutorCharacter.MANU) template.manuEn else template.proEn
      LearnerLevel.PRO, LearnerLevel.MAESTRIA -> template.proEn
    }
  }

  private fun openingPronunciationFor(scenarioId: String): List<PronunciationTip> = when (scenarioId) {
    "coffee_shop" -> listOf(
      PronunciationTip("latte", "/ˈlɑːteɪ/", "Suena 'lá-tei', con la E final abierta."),
    )
    "job_interview" -> listOf(
      PronunciationTip("experience", "/ɪkˈspɪriəns/", "Acento en la segunda sílaba: 'ik-spí-riens'."),
    )
    "airport_travel" -> listOf(
      PronunciationTip("passport", "/ˈpæspɔːrt/", "A abierta al inicio y R suave al final."),
    )
    "restaurant" -> listOf(
      PronunciationTip("reservation", "/ˌrezərˈveɪʃən/", "Termina como 'véi-shon'."),
    )
    "daily_standup" -> listOf(
      PronunciationTip("focusing", "/ˈfoʊkəsɪŋ/", "Vocal larga al inicio: 'fóu-ke-sing'."),
    )
    "shopping_basics" -> listOf(
      PronunciationTip("jacket", "/ˈdʒækɪt/", "J fuerte inicial: 'yá-kit'."),
    )
    "doctor_visit" -> listOf(
      PronunciationTip("headache", "/ˈhedeɪk/", "Termina como 'éik', no 'ache': 'hé-deik'."),
    )
    "salary_negotiation" -> listOf(
      PronunciationTip("compensation", "/ˌkɑːmpənˈseɪʃən/", "Termina en 'séi-shon'."),
    )
    "workplace_feedback", "remote_collaboration" -> listOf(
      PronunciationTip("perspective", "/pərˈspektɪv/", "Sin E antes de la S intermedia: 'per-SPÉK-tiv'."),
    )
    "project_postmortem", "travel_complaints" -> listOf(
      PronunciationTip("voucher", "/ˈvaʊtʃər/", "V vibrante con labio y 'ch' firme: 'váu-cher'."),
    )
    "diplomatic_mediation", "constitutional_ethics" -> listOf(
      PronunciationTip("autonomy", "/ɔːˈtɑːnəmi/", "Acento en la segunda sílaba: 'o-TÓ-no-mi'."),
    )
    else -> listOf(
      PronunciationTip("practice", "/ˈpræktɪs/", "Acento claro al inicio: 'prák-tis'."),
    )
  }
}
