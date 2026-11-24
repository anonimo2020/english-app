package com.example.englishapp.data

import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SkillScores
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class PlacementQuestionType(val labelEs: String) {
  LISTENING("Comprensión auditiva"),
  GRAMMAR("Gramática en contexto"),
  CONVERSATION("Situación real"),
  SPEAKING("Pronunciación en voz alta"),
}

data class PlacementQuestion(
  val id: String,
  val type: PlacementQuestionType,
  val difficulty: Double, // 1.0 = A1, 2.0 = A2, 3.0 = B1, 4.0 = B2, 5.0 = C1, 6.0 = C2
  val promptEs: String,
  val promptEn: String,
  val options: List<String> = emptyList(),
  val correctIndex: Int = -1,
  val explanationEs: String,
)

data class PlacementAnswerRecord(
  val question: PlacementQuestion,
  val isCorrect: Boolean,
  val scoreFraction: Float = if (isCorrect) 1f else 0f,
)

object PlacementEngine {

  const val DEFAULT_TOTAL_STEPS = 6
  const val SPEAKING_PASS_THRESHOLD = 0.65f

  val bank: List<PlacementQuestion> = listOf(
    // --- A1 (1.0) ---
    PlacementQuestion(
      id = "a1_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 1.0,
      promptEs = "Escucha la frase y elige su significado correcto:",
      promptEn = "Hi! I am twenty years old and I live with my family.",
      options = listOf(
        "Tiene veinte años y vive con su familia.",
        "Trabaja desde las ocho con su hermano.",
        "Quiere viajar mañana por la tarde.",
      ),
      correctIndex = 0,
      explanationEs = "'Twenty years old' significa veinte años y 'live with my family' es vivir con su familia.",
    ),
    PlacementQuestion(
      id = "a1_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 1.0,
      promptEs = "Elige la forma correcta para decir tu edad:",
      promptEn = "____ twenty-four years old.",
      options = listOf("I am", "I have", "I has"),
      correctIndex = 0,
      explanationEs = "En inglés la edad siempre se expresa con 'to be': I am 24 years old.",
    ),
    PlacementQuestion(
      id = "a1_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 1.0,
      promptEs = "Alguien te dice: 'Nice to meet you!' ¿Qué respondes?",
      promptEn = "Nice to meet you!",
      options = listOf(
        "Nice to meet you too!",
        "Yes, I have twenty.",
        "I am going to home.",
      ),
      correctIndex = 0,
      explanationEs = "'Nice to meet you too!' es la respuesta natural a 'Mucho gusto'.",
    ),
    PlacementQuestion(
      id = "a1_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 1.0,
      promptEs = "Lee esta frase en voz alta en inglés:",
      promptEn = "I would like a hot coffee, please.",
      explanationEs = "Frase esencial de cortesía para pedir en cualquier lugar.",
    ),
    PlacementQuestion(
      id = "a1_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 1.1,
      promptEs = "Escucha y responde: ¿Qué va a tomar la persona?",
      promptEn = "Excuse me, could I have a glass of cold water, please?",
      options = listOf(
        "Un vaso de agua fría.",
        "Una taza de café caliente.",
        "Un boleto de tren.",
      ),
      correctIndex = 0,
      explanationEs = "'A glass of cold water' significa un vaso de agua fría.",
    ),
    PlacementQuestion(
      id = "a1_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 1.2,
      promptEs = "Completa la negación correcta en presente simple:",
      promptEn = "She ____ like spicy food.",
      options = listOf("doesn't", "don't", "no"),
      correctIndex = 0,
      explanationEs = "Con 'he', 'she' o 'it' usamos 'doesn't' para negar.",
    ),

    // --- A2 (2.0) ---
    PlacementQuestion(
      id = "a2_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 2.0,
      promptEs = "Escucha el anuncio del aeropuerto: ¿Qué debes hacer?",
      promptEn = "Flight two zero four is now boarding at gate B twelve.",
      options = listOf(
        "Ir a la puerta B12 para abordar el vuelo 204.",
        "Recoger las maletas en la cinta número cuatro.",
        "Cambiar la fecha del boleto para mañana.",
      ),
      correctIndex = 0,
      explanationEs = "'Now boarding at gate B12' indica que ya están embarcando en la puerta B12.",
    ),
    PlacementQuestion(
      id = "a2_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 2.0,
      promptEs = "Elige la oración correcta en pasado:",
      promptEn = "I ____ to the cinema yesterday.",
      options = listOf("didn't go", "didn't went", "no went"),
      correctIndex = 0,
      explanationEs = "Después de 'didn't' el verbo vuelve a su forma base: 'go'.",
    ),
    PlacementQuestion(
      id = "a2_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 2.0,
      promptEs = "En un restaurante quieres pedir la cuenta con amabilidad:",
      promptEn = "Waiter: Can I get you anything else?",
      options = listOf(
        "No thanks, could we have the check, please?",
        "Give me the money now.",
        "I am agree with the menu.",
      ),
      correctIndex = 0,
      explanationEs = "'Could we have the check, please?' es natural y educado.",
    ),
    PlacementQuestion(
      id = "a2_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 2.0,
      promptEs = "Di en voz alta esta frase de viaje:",
      promptEn = "I would like a window seat, please.",
      explanationEs = "Practica pronunciar 'would' con la L muda y 'window seat' de corrido.",
    ),
    PlacementQuestion(
      id = "a2_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 2.2,
      promptEs = "Escucha con atención: ¿Dónde está la farmacia?",
      promptEn = "Go straight two blocks and turn left at the traffic light.",
      options = listOf(
        "Dos cuadras recto y a la izquierda en el semáforo.",
        "Detrás de la estación de tren, a la derecha.",
        "A diez minutos en taxi hacia el aeropuerto.",
      ),
      correctIndex = 0,
      explanationEs = "'Go straight two blocks' = sigue recto dos cuadras; 'turn left' = gira a la izquierda.",
    ),
    PlacementQuestion(
      id = "a2_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 2.2,
      promptEs = "Selecciona la preposición correcta:",
      promptEn = "It really depends ____ the weather.",
      options = listOf("on", "of", "in"),
      correctIndex = 0,
      explanationEs = "En inglés siempre se dice 'depend on', nunca 'depend of'.",
    ),

    // --- B1 (3.0) ---
    PlacementQuestion(
      id = "b1_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 3.0,
      promptEs = "Escucha la respuesta en una entrevista: ¿Qué destaca?",
      promptEn = "When deadlines get tight, I usually break the project into clear milestones.",
      options = listOf(
        "Cuando hay poco tiempo, divide el proyecto en hitos claros.",
        "Prefiere posponer el proyecto cuando hay presión.",
        "Nunca trabaja con fechas límite.",
      ),
      correctIndex = 0,
      explanationEs = "'Tight deadlines' = plazos ajustados; 'break into milestones' = dividir en etapas claras.",
    ),
    PlacementQuestion(
      id = "b1_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 3.0,
      promptEs = "Elige la estructura más natural para una experiencia hasta hoy:",
      promptEn = "I ____ in remote teams for three years now.",
      options = listOf("have worked", "am work", "have working"),
      correctIndex = 0,
      explanationEs = "Para acciones que empezaron en el pasado y continúan hoy usamos Present Perfect: 'have worked'.",
    ),
    PlacementQuestion(
      id = "b1_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 3.0,
      promptEs = "Tu paquete llegó dañado y hablas con soporte. ¿Qué suena más natural y firme?",
      promptEn = "Agent: How can I help with your order today?",
      options = listOf(
        "My order arrived damaged, so I would appreciate a free replacement.",
        "The box is bad, you give me new one.",
        "I am boring because the package broke.",
      ),
      correctIndex = 0,
      explanationEs = "'I would appreciate a free replacement' combina firmeza y cortesía profesional.",
    ),
    PlacementQuestion(
      id = "b1_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 3.0,
      promptEs = "Pronuncia esta frase de entrevista laboral:",
      promptEn = "My biggest strength is solving problems calmly.",
      explanationEs = "Cuida el sonido final de 'strength' y la entonación fluida.",
    ),
    PlacementQuestion(
      id = "b1_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 3.1,
      promptEs = "Escucha este comentario cotidiano: ¿Qué significa 'came down with'?",
      promptEn = "I had to skip the meeting because I came down with a bad cold.",
      options = listOf(
        "Se enfermó de un resfriado fuerte.",
        "Bajó las escaleras muy rápido.",
        "Canceló porque hacía calor afuera.",
      ),
      correctIndex = 0,
      explanationEs = "El phrasal verb 'come down with' significa contraer o empezar a sentir una enfermedad.",
    ),
    PlacementQuestion(
      id = "b1_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 3.2,
      promptEs = "Completa con el significado correcto en inglés:",
      promptEn = "We missed the train; ____ we arrived right on time for dinner.",
      options = listOf("however,", "despite,", "although of,"),
      correctIndex = 0,
      explanationEs = "'However,' conecta dos oraciones completas mostrando contraste.",
    ),

    // --- B2 (4.0) ---
    PlacementQuestion(
      id = "b2_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 4.0,
      promptEs = "Escucha este comentario de retrospectiva laboral: ¿Qué habrían evitado?",
      promptEn = "Had we aligned with QA earlier, we could have prevented most of those post-release delays.",
      options = listOf(
        "Habrían evitado la mayoría de los retrasos posteriores al lanzamiento.",
        "Habrían cancelado el proyecto antes de empezar.",
        "Habrían contratado menos personal de soporte.",
      ),
      correctIndex = 0,
      explanationEs = "'Had we aligned...' es un condicional invertido equivalente a 'If we had aligned...'.",
    ),
    PlacementQuestion(
      id = "b2_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 4.0,
      promptEs = "Completa la expresión de adaptación con la forma correcta:",
      promptEn = "It took a few weeks, but I finally got used to ____ across different time zones.",
      options = listOf("collaborating", "collaborate", "collaborated"),
      correctIndex = 0,
      explanationEs = "En 'get used to', 'to' es preposición y exige verbo en gerundio (-ing): 'got used to collaborating'.",
    ),
    PlacementQuestion(
      id = "b2_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 4.0,
      promptEs = "Debes dar retroalimentación delicada a un compañero en nivel B2. ¿Qué opción es más profesional?",
      promptEn = "Colleague: Did my presentation cover everything the client needed?",
      options = listOf(
        "Your visuals were strong, though tightening the timeline slide would make the pitch even sharper.",
        "Your slides were too long and boring for them.",
        "Yes, I am agree with every slide you put.",
      ),
      correctIndex = 0,
      explanationEs = "El contraste constructivo con 'though + recomendación concreta' es estándar profesional B2.",
    ),
    PlacementQuestion(
      id = "b2_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 4.0,
      promptEs = "Pronuncia esta oración de tercer condicional con fluidez:",
      promptEn = "If we had tested earlier, we would have saved time.",
      explanationEs = "Une 'would have' de manera fluida ('wu-dav') manteniendo claro 'tested earlier'.",
    ),
    PlacementQuestion(
      id = "b2_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 4.1,
      promptEs = "Escucha esta deducción en pasado: ¿Qué concluye el hablante?",
      promptEn = "The flight crew must have overlooked our baggage tag during the tight layover.",
      options = listOf(
        "Es casi seguro que pasaron por alto la etiqueta del equipaje durante la escala corta.",
        "El equipaje nunca salió del país de origen.",
        "El pasajero olvidó etiquetar su maleta en casa.",
      ),
      correctIndex = 0,
      explanationEs = "'Must have + participio' expresa alta certeza sobre un hecho pasado.",
    ),
    PlacementQuestion(
      id = "b2_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 4.2,
      promptEs = "Selecciona la estructura causativa correcta:",
      promptEn = "We are going to ____ our office acoustics upgraded before the podcast launches.",
      options = listOf("have", "make", "do"),
      correctIndex = 0,
      explanationEs = "La estructura causativa es 'have + objeto + participio': 'have our acoustics upgraded'.",
    ),

    // --- C1 (5.0) ---
    PlacementQuestion(
      id = "c1_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 5.0,
      promptEs = "Escucha esta opinión ejecutiva: ¿Cuál es la postura del hablante?",
      promptEn = "While automation streamlines repetitive tasks, human judgment remains indispensable for ethical decisions.",
      options = listOf(
        "La automatización agiliza tareas repetitivas, pero el criterio humano sigue siendo clave en lo ético.",
        "Las máquinas reemplazarán por completo el criterio ético humano.",
        "La automatización hace más lentos todos los procesos diarios.",
      ),
      correctIndex = 0,
      explanationEs = "'Streamlines' = agiliza/optimiza; 'remains indispensable' = sigue siendo indispensable.",
    ),
    PlacementQuestion(
      id = "c1_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 5.0,
      promptEs = "Elige la inversión formal correcta de nivel C1:",
      promptEn = "Not only ____ the deadline, but we also exceeded the client's expectations.",
      options = listOf("did we meet", "we met", "met we"),
      correctIndex = 0,
      explanationEs = "Cuando 'Not only' abre la oración se invierte el auxiliar: 'Not only did we meet...'.",
    ),
    PlacementQuestion(
      id = "c1_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 5.0,
      promptEs = "En una negociación salarial, ¿cuál respuesta proyecta mayor madurez profesional?",
      promptEn = "Director: Why do you feel a higher base salary is justified?",
      options = listOf(
        "Given my track record and market benchmarks, a ten percent adjustment reflects the impact I bring.",
        "Because I need more money for my rent this year.",
        "I am agree with any salary you want to pay me.",
      ),
      correctIndex = 0,
      explanationEs = "'Track record' (trayectoria) y 'market benchmarks' (referencias de mercado) elevan tu discurso.",
    ),
    PlacementQuestion(
      id = "c1_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 5.0,
      promptEs = "Lee esta frase ejecutiva con ritmo natural:",
      promptEn = "We cut onboarding time in half while boosting retention.",
      explanationEs = "Conecta 'cut onboarding' de forma fluida y marca el contraste con 'while'.",
    ),
    PlacementQuestion(
      id = "c1_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 5.1,
      promptEs = "Escucha la expresión idiomática: ¿Qué implica 'on the same page'?",
      promptEn = "Before we pitch to the client, let us make sure everyone is on the same page.",
      options = listOf(
        "Asegurarse de que todo el equipo comparte el mismo entendimiento y objetivo.",
        "Imprimir el mismo documento para cada persona.",
        "Leer la primera página del contrato en voz alta.",
      ),
      correctIndex = 0,
      explanationEs = "'Be on the same page' significa estar alineados o de acuerdo sobre un plan.",
    ),
    PlacementQuestion(
      id = "c1_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 5.2,
      promptEs = "Elige el conector más preciso de nivel avanzado:",
      promptEn = "____ the initial skepticism, the pilot program delivered measurable results within weeks.",
      options = listOf("Notwithstanding", "Even although", "Despite of"),
      correctIndex = 0,
      explanationEs = "'Notwithstanding' (a pesar de) funciona como preposición formal sin 'of'.",
    ),

    // --- C2 (6.0) ---
    PlacementQuestion(
      id = "c2_listen_1",
      type = PlacementQuestionType.LISTENING,
      difficulty = 6.0,
      promptEs = "Escucha este fragmento retórico C2: ¿Qué expresa 'speaks volumes'?",
      promptEn = "Enduring literature embraces ambiguity, where a character's hesitation speaks volumes more than an explicit monologue.",
      options = listOf(
        "Transmite muchísimo significado implícito sin necesidad de decirlo explícitamente.",
        "Requiere un volumen de voz más alto al leer en público.",
        "Se refiere a libros publicados en varios tomos.",
      ),
      correctIndex = 0,
      explanationEs = "'Speak volumes' es un modismo culto que significa expresar mucho de forma elocuente e indirecta.",
    ),
    PlacementQuestion(
      id = "c2_grammar_1",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 6.0,
      promptEs = "Selecciona la inversión enfática de maestría C2:",
      promptEn = "Scarcely ____ the keynote address when several analysts challenged the core premise.",
      options = listOf("had she concluded", "she concluded", "did she concluded"),
      correctIndex = 0,
      explanationEs = "'Scarcely + had + sujeto + participio ... when' es una correlación clásica de nivel C2.",
    ),
    PlacementQuestion(
      id = "c2_conv_1",
      type = PlacementQuestionType.CONVERSATION,
      difficulty = 6.0,
      promptEs = "En un comité de inversión te cuestionan la defensibilidad del negocio. ¿Qué réplica demuestra dominio C2?",
      promptEn = "Partner: What stops an incumbent from copying your workflow within six months?",
      options = listOf(
        "Even granting faster copycats, proprietary feedback loops compound switching costs for enterprise cohorts.",
        "Because our company works harder than big competitors.",
        "We hope nobody notices our product until next year.",
      ),
      correctIndex = 0,
      explanationEs = "'Even granting...' concede el punto al crítico antes de rebatirlo con rigor estratégico.",
    ),
    PlacementQuestion(
      id = "c2_speak_1",
      type = PlacementQuestionType.SPEAKING,
      difficulty = 6.0,
      promptEs = "Pronuncia esta frase de mediación ejecutiva con pausa y aplomo:",
      promptEn = "Were we to separate shared outcomes from tactics, common ground emerges.",
      explanationEs = "Marca una micro-pausa clara tras 'tactics' y enlaza 'common ground emerges' sin cortar consonantes.",
    ),
    PlacementQuestion(
      id = "c2_listen_2",
      type = PlacementQuestionType.LISTENING,
      difficulty = 6.1,
      promptEs = "Escucha la advertencia regulatoria: ¿Qué función cumple 'lest'?",
      promptEn = "Algorithmic consent must remain revocable, lest sheer convenience erode individual autonomy.",
      options = listOf(
        "Para evitar el riesgo de que la comodidad termine erosionando la autonomía.",
        "Porque la autonomía desapareció hace muchos años.",
        "Aunque la comodidad siempre supera cualquier regulación.",
      ),
      correctIndex = 0,
      explanationEs = "'Lest' (por temor a que / para que no) introduce una cláusula formal con subjuntivo en inglés.",
    ),
    PlacementQuestion(
      id = "c2_grammar_2",
      type = PlacementQuestionType.GRAMMAR,
      difficulty = 6.2,
      promptEs = "Elige el subjuntivo formal correcto:",
      promptEn = "The ethics board insisted that every dataset ____ anonymized prior to external auditing.",
      options = listOf("be", "is being", "was to be"),
      correctIndex = 0,
      explanationEs = "Tras verbos de exigencia ('insist that...'), el subjuntivo mandativo usa la forma base: 'be anonymized'.",
    ),
  )

  fun initialAbilityFor(estimated: LearnerLevel): Double = when (estimated) {
    LearnerLevel.NOVATO -> 1.15
    LearnerLevel.JUNIOR -> 2.10
    LearnerLevel.INTERMEDIO -> 3.05
    LearnerLevel.INTERMEDIO_ALTO -> 4.00
    LearnerLevel.PRO -> 4.95
    LearnerLevel.MAESTRIA -> 5.85
  }

  fun maxAbilityFor(level: LearnerLevel): Double = when (level) {
    LearnerLevel.NOVATO -> 1.45
    LearnerLevel.JUNIOR -> 2.45
    LearnerLevel.INTERMEDIO -> 3.40
    LearnerLevel.INTERMEDIO_ALTO -> 4.35
    LearnerLevel.PRO -> 5.25
    LearnerLevel.MAESTRIA -> 6.35
  }

  fun nextQuestion(
    ability: Double,
    answeredIds: Set<String>,
    stepIndex: Int,
    allowSpeaking: Boolean = true,
    targetLevel: LearnerLevel = levelForAbility(ability),
  ): PlacementQuestion {
    val desiredType = when (stepIndex % DEFAULT_TOTAL_STEPS) {
      0 -> PlacementQuestionType.LISTENING
      1 -> PlacementQuestionType.GRAMMAR
      2 -> PlacementQuestionType.CONVERSATION
      3 -> PlacementQuestionType.LISTENING
      4 -> if (allowSpeaking) PlacementQuestionType.SPEAKING else PlacementQuestionType.GRAMMAR
      else -> PlacementQuestionType.GRAMMAR
    }

    val unused = bank.filter { it.id !in answeredIds }
    val allowedByTarget = unused.filter { levelForAbility(it.difficulty).ordinal <= targetLevel.ordinal }.ifEmpty { unused }
    val pool = allowedByTarget.filter { it.type == desiredType }.ifEmpty { allowedByTarget }.ifEmpty { bank }
    return pool.minBy { abs(it.difficulty - ability) }
  }

  fun updateAbility(
    currentAbility: Double,
    questionDifficulty: Double,
    isCorrect: Boolean,
    targetLevel: LearnerLevel = LearnerLevel.MAESTRIA,
  ): Double {
    val expectedProb = 1.0 / (1.0 + exp(-(currentAbility - questionDifficulty) * 1.4))
    val actual = if (isCorrect) 1.0 else 0.0
    val stepSize = if (isCorrect) 0.45 else 1.25
    val raw = currentAbility + stepSize * (actual - expectedProb)
    return raw.coerceIn(0.8, maxAbilityFor(targetLevel))
  }

  fun levelForAbility(ability: Double): LearnerLevel = when {
    ability < 1.75 -> LearnerLevel.NOVATO
    ability < 2.65 -> LearnerLevel.JUNIOR
    ability < 3.55 -> LearnerLevel.INTERMEDIO
    ability < 4.45 -> LearnerLevel.INTERMEDIO_ALTO
    ability < 5.35 -> LearnerLevel.PRO
    else -> LearnerLevel.MAESTRIA
  }

  fun evaluateSpokenMatch(expected: String, spoken: String): Float {
    val normExpected = normalizeText(expected)
    val normSpoken = normalizeText(spoken)
    if (normExpected.isEmpty() || normSpoken.isEmpty()) return 0f

    val expectedTokens = normExpected.split(" ").filter { it.isNotBlank() }
    val spokenTokens = normSpoken.split(" ").filter { it.isNotBlank() }.toMutableList()
    var hits = 0
    for (token in expectedTokens) {
      val idx = spokenTokens.indexOf(token)
      if (idx >= 0) {
        hits++
        spokenTokens.removeAt(idx)
      }
    }
    val tokenRecall = if (expectedTokens.isEmpty()) 0f else hits.toFloat() / expectedTokens.size.toFloat()

    val maxLen = max(normExpected.length, normSpoken.length)
    val dist = levenshtein(normExpected, normSpoken)
    val charSim = 1f - (dist.toFloat() / maxLen.toFloat())

    return (tokenRecall * 0.65f + charSim * 0.35f).coerceIn(0f, 1f)
  }

  fun buildResult(
    estimatedLevel: LearnerLevel,
    finalAbility: Double,
    records: List<PlacementAnswerRecord>,
  ): PlacementResult {
    val accuracy = if (records.isEmpty()) 0.0 else records.count { it.isCorrect }.toDouble() / records.size
    val avgFraction = if (records.isEmpty()) 0.5 else records.map { it.scoreFraction.toDouble() }.average()

    val verified = when {
      records.isNotEmpty() && records.none { it.isCorrect } -> LearnerLevel.NOVATO
      else -> {
        val byAbility = levelForAbility(finalAbility)
        val capped = if (byAbility.ordinal > estimatedLevel.ordinal) estimatedLevel else byAbility
        if (estimatedLevel != LearnerLevel.NOVATO && accuracy < 0.66 && capped == estimatedLevel) {
          LearnerLevel.entries[(estimatedLevel.ordinal - 1).coerceAtLeast(0)]
        } else {
          capped
        }
      }
    }

    val levelRelativePct = (avgFraction * 72.0 + accuracy * 24.0 + 3.0).roundToInt().coerceIn(25, 99)

    fun scoreFor(type: PlacementQuestionType, fallback: Int): Int {
      val subset = records.filter { it.question.type == type }
      if (subset.isEmpty()) return fallback
      val subsetAvg = subset.map { it.scoreFraction.toDouble() }.average()
      val subsetPassRate = subset.count { it.isCorrect }.toDouble() / subset.size
      return (subsetAvg * 75.0 + subsetPassRate * 22.0 + 2.0).roundToInt().coerceIn(25, 99)
    }

    val listening = scoreFor(PlacementQuestionType.LISTENING, levelRelativePct)
    val grammar = scoreFor(PlacementQuestionType.GRAMMAR, levelRelativePct)
    val vocabulary = scoreFor(PlacementQuestionType.CONVERSATION, levelRelativePct)
    val speaking = scoreFor(
      PlacementQuestionType.SPEAKING,
      ((listening + vocabulary) / 2.0).roundToInt(),
    )

    val overall = levelRelativePct

    val weakestEntry = listOf(
      "comprensión auditiva" to listening,
      "gramática en uso" to grammar,
      "vocabulario conversacional" to vocabulary,
      "pronunciación al hablar" to speaking,
    ).minBy { it.second }

    val focusText = buildString {
      when {
        verified.ordinal < estimatedLevel.ordinal ->
          append("Ajustamos tu plan a ${verified.labelEs} (${verified.cefr}) para construir bases sólidas sin frustración. ")
        accuracy >= 0.83 ->
          append("¡Excelente dominio en ${verified.labelEs} (${verified.cefr})! Confirmamos tu nivel en ${verified.cefr} (puedes intentar el siguiente nivel cuando quieras desde Configuración). ")
        else ->
          append("Confirmamos tu nivel en ${verified.labelEs} (${verified.cefr}). ")
      }
      append("Priorizaremos ${weakestEntry.first} con escenarios diseñados para ti.")
    }

    return PlacementResult(
      estimatedLevel = estimatedLevel,
      verifiedLevel = verified,
      overallScore = overall,
      skills = SkillScores(
        listening = listening,
        grammar = grammar,
        vocabulary = vocabulary,
        speaking = speaking,
      ),
      recommendedFocusEs = focusText,
    )
  }

  private fun normalizeText(input: String): String = input.lowercase()
    .replace(Regex("[^a-z0-9\\s]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

  private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
      curr[0] = i
      for (j in 1..b.length) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
      }
      val tmp = prev
      prev = curr
      curr = tmp
    }
    return prev[b.length]
  }
}
