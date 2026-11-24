package com.example.englishapp.data

import com.example.englishapp.model.LearnerLevel

data class StoryVocab(
  val word: String,
  val spanish: String,
  val phonetic: String,
)

data class StoryQuestion(
  val questionEs: String,
  val options: List<String>,
  val correctIndex: Int,
)

data class AudioStory(
  val id: String,
  val titleEs: String,
  val titleEn: String,
  val level: LearnerLevel,
  val durationLabel: String,
  val bodyEn: String,
  val bodyEs: String,
  val vocab: List<StoryVocab>,
  val questions: List<StoryQuestion>,
)

data class PronunciationDrill(
  val id: String,
  val title: String,
  val level: LearnerLevel,
  val category: String,
  val ipa: String,
  val targetPhrase: String,
  val exampleSentence: String,
  val spanishTip: String,
)

data class FreeWebPortal(
  val name: String,
  val cefrBadge: String,
  val categoryEs: String,
  val taglineEs: String,
  val url: String,
  val minLevel: LearnerLevel = LearnerLevel.NOVATO,
  val maxLevel: LearnerLevel = LearnerLevel.MAESTRIA,
)

object LearningLibrary {

  val stories: List<AudioStory> = listOf(
    // --- A1 (3 historias) ---
    AudioStory(
      id = "story_a1_morning",
      titleEs = "Un café antes del trabajo",
      titleEn = "A Morning Coffee",
      level = LearnerLevel.NOVATO,
      durationLabel = "1 min · A1",
      bodyEn = "Every morning at seven, Sofia walks to a small blue cafe near her house. She smiles at the barista and says, 'One hot coffee with milk, please.' Then she sits by the window, drinks her coffee slowly, and reads five pages of a short English book.",
      bodyEs = "Cada mañana a las siete, Sofía camina a una pequeña cafetería azul cerca de su casa. Sonríe al barista y dice: 'Un café caliente con leche, por favor'. Luego se sienta junto a la ventana, bebe su café despacio y lee cinco páginas de un libro corto en inglés.",
      vocab = listOf(
        StoryVocab("near", "cerca de", "/nɪr/"),
        StoryVocab("slowly", "despacio", "/ˈsloʊli/"),
        StoryVocab("window", "ventana", "/ˈwɪndoʊ/"),
      ),
      questions = listOf(
        StoryQuestion("¿A qué hora va Sofía a la cafetería?", listOf("A las siete de la mañana.", "A las nueve de la noche.", "Solo los domingos."), 0),
        StoryQuestion("¿Qué hace mientras toma su café?", listOf("Lee cinco páginas en inglés.", "Ve televisión en la cocina.", "Corre en el parque."), 0),
      ),
    ),
    AudioStory(
      id = "story_a1_market",
      titleEs = "Frutas frescas en el mercado",
      titleEn = "Fresh Fruit at the Market",
      level = LearnerLevel.NOVATO,
      durationLabel = "1 min · A1",
      bodyEn = "On Saturday morning, Mateo visits the street market with his backpack. He buys red apples, sweet bananas, and fresh bread. The seller asks, 'Anything else today?' Mateo smiles and answers, 'No, thank you. How much is everything?' It is only six dollars.",
      bodyEs = "El sábado por la mañana, Mateo visita el mercado callejero con su mochila. Compra manzanas rojas, plátanos dulces y pan fresco. El vendedor pregunta: '¿Algo más hoy?'. Mateo sonríe y responde: 'No, gracias. ¿Cuánto es todo?'. Son solo seis dólares.",
      vocab = listOf(
        StoryVocab("backpack", "mochila", "/ˈbækpæk/"),
        StoryVocab("fresh", "fresco", "/freʃ/"),
        StoryVocab("seller", "vendedor", "/ˈselər/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué día va Mateo al mercado?", listOf("El sábado por la mañana.", "El martes por la tarde.", "El viernes por la noche."), 0),
        StoryQuestion("¿Cuánto paga por todo?", listOf("Seis dólares.", "Dieciséis dólares.", "Sesenta dólares."), 0),
      ),
    ),
    AudioStory(
      id = "story_a1_hotel",
      titleEs = "La llave de la habitación",
      titleEn = "The Hotel Room Key",
      level = LearnerLevel.NOVATO,
      durationLabel = "1 min · A1",
      bodyEn = "Laura arrives at the Blue Sky Hotel at five in the afternoon. She is tired after her bus trip. At the front desk, she says, 'Hello! I have a reservation under Laura Perez.' The receptionist smiles, gives her the room key, and says, 'Breakfast is at seven tomorrow.'",
      bodyEs = "Laura llega al Hotel Blue Sky a las cinco de la tarde. Está cansada después de su viaje en autobús. En recepción dice: '¡Hola! Tengo una reserva a nombre de Laura Pérez'. El recepcionista sonríe, le da la llave y le dice: 'El desayuno es a las siete mañana'.",
      vocab = listOf(
        StoryVocab("front desk", "recepción", "/frʌnt desk/"),
        StoryVocab("tired", "cansada", "/ˈtaɪərd/"),
        StoryVocab("breakfast", "desayuno", "/ˈbrekfəst/"),
      ),
      questions = listOf(
        StoryQuestion("¿Cómo llegó Laura al hotel?", listOf("En autobús.", "En barco.", "En bicicleta."), 0),
        StoryQuestion("¿A qué hora es el desayuno?", listOf("A las siete de la mañana.", "A las diez.", "No hay desayuno."), 0),
      ),
    ),

    // --- A2 (3 historias) ---
    AudioStory(
      id = "story_a2_train",
      titleEs = "El tren equivocado",
      titleEn = "The Wrong Train",
      level = LearnerLevel.JUNIOR,
      durationLabel = "2 min · A2",
      bodyEn = "Last Friday, Lucia was in a hurry to reach the museum before noon. She jumped on the first train without checking the sign. Ten minutes later, she looked out the window and saw green hills instead of tall buildings! She laughed, asked a friendly passenger for directions, and took the next train back downtown.",
      bodyEs = "El viernes pasado, Lucía tenía prisa por llegar al museo antes del mediodía. Se subió al primer tren sin revisar el letrero. Diez minutos después miró por la ventana y vio colinas verdes en vez de edificios altos. Se rió, pidió indicaciones a un pasajero amable y tomó el siguiente tren de regreso al centro.",
      vocab = listOf(
        StoryVocab("in a hurry", "con prisa", "/ɪn ə ˈhɜːri/"),
        StoryVocab("instead of", "en lugar de", "/ɪnˈsted əv/"),
        StoryVocab("downtown", "centro de la ciudad", "/ˌdaʊnˈtaʊn/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué se subió al tren equivocado?", listOf("Tenía prisa y no revisó el letrero.", "El boleto estaba dañado.", "El conductor se lo recomendó."), 0),
        StoryQuestion("¿Cómo resolvió el problema?", listOf("Pidió indicaciones a un pasajero y volvió al centro.", "Caminó dos horas por las colinas.", "Llamó un avión privado."), 0),
      ),
    ),
    AudioStory(
      id = "story_a2_dinner",
      titleEs = "Cena sorpresa en casa",
      titleEn = "A Surprise Dinner",
      level = LearnerLevel.JUNIOR,
      durationLabel = "2 min · A2",
      bodyEn = "Daniel wanted to celebrate his sister's new job, so he decided to cook pasta from scratch. First, he chopped tomatoes and garlic while listening to jazz music. When his sister opened the door, the whole apartment smelled amazing. 'You actually made dinner!' she laughed, and they talked for hours.",
      bodyEs = "Daniel quería celebrar el nuevo trabajo de su hermana, así que decidió cocinar pasta desde cero. Primero picó tomates y ajo mientras escuchaba música jazz. Cuando su hermana abrió la puerta, todo el departamento olía increíble. '¡De verdad hiciste la cena!', rió ella, y charlaron durante horas.",
      vocab = listOf(
        StoryVocab("from scratch", "desde cero", "/frʌm skrætʃ/"),
        StoryVocab("chopped", "picó (cortar)", "/tʃɑːpt/"),
        StoryVocab("actually", "de hecho / en realidad", "/ˈæktʃuəli/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué cocinó Daniel la cena?", listOf("Para celebrar el nuevo trabajo de su hermana.", "Porque no tenía dinero para salir.", "Porque era su cumpleaños."), 0),
        StoryQuestion("¿Qué significa 'from scratch' en la historia?", listOf("Preparado desde cero, con ingredientes frescos.", "Comprado en caja congelada.", "Pedido por aplicación."), 0),
      ),
    ),
    AudioStory(
      id = "story_a2_luggage",
      titleEs = "La maleta casi perdida",
      titleEn = "The Missing Suitcase",
      level = LearnerLevel.JUNIOR,
      durationLabel = "2 min · A2",
      bodyEn = "When Diego landed in Toronto, almost every passenger picked up their bag except him. He stayed calm and walked to the lost luggage desk with his boarding pass. Five minutes later, an agent smiled and said, 'Good news—your suitcase came on the earlier flight and is waiting right inside!'",
      bodyEs = "Cuando Diego aterrizó en Toronto, casi todos recogieron su maleta menos él. Mantuvo la calma y caminó al mostrador de equipaje perdido con su tarjeta de embarque. Cinco minutos después, una agente sonrió y dijo: 'Buenas noticias: tu maleta llegó en el vuelo anterior y te espera adentro'.",
      vocab = listOf(
        StoryVocab("landed", "aterrizó", "/ˈlændɪd/"),
        StoryVocab("suitcase", "maleta", "/ˈsuːtkeɪs/"),
        StoryVocab("earlier", "anterior / más temprano", "/ˈɜːrliər/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué hizo Diego al no ver su maleta?", listOf("Fue con calma al mostrador de equipaje perdido.", "Salió corriendo del aeropuerto.", "Compró ropa nueva de inmediato."), 0),
        StoryQuestion("¿Dónde estaba la maleta?", listOf("Había llegado en un vuelo anterior.", "Se quedó en su casa.", "Se perdió para siempre."), 0),
      ),
    ),

    // --- B1 (3 historias) ---
    AudioStory(
      id = "story_b1_podcast",
      titleEs = "El hábito de diez minutos",
      titleEn = "The Ten-Minute Habit",
      level = LearnerLevel.INTERMEDIO,
      durationLabel = "2 min · B1",
      bodyEn = "For months, Carlos kept putting off his English practice because he thought he needed a whole hour every evening. One Monday, he decided to switch tactics: he started speaking aloud during his ten-minute walk to work. Within six weeks, thinking in English felt far less intimidating, and he finally spoke up during a global team call.",
      bodyEs = "Durante meses, Carlos posponía su práctica de inglés porque creía que necesitaba una hora entera cada tarde. Un lunes decidió cambiar de estrategia: empezó a hablar en voz alta durante su caminata de diez minutos al trabajo. En seis semanas, pensar en inglés le resultó mucho menos intimidante y por fin habló en una llamada global del equipo.",
      vocab = listOf(
        StoryVocab("put off", "posponer", "/pʊt ɔːf/"),
        StoryVocab("speak aloud", "hablar en voz alta", "/spiːk əˈlaʊd/"),
        StoryVocab("intimidating", "intimidante", "/ɪnˈtɪmɪdeɪtɪŋ/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué significa el phrasal verb 'put off'?", listOf("Posponer o aplazar algo.", "Apagar una computadora.", "Subir el volumen."), 0),
        StoryQuestion("¿Qué cambio desbloqueó el progreso de Carlos?", listOf("Practicar diez minutos al día mientras caminaba.", "Estudiar gramática cuatro horas los domingos.", "Dejar su trabajo."), 0),
      ),
    ),
    AudioStory(
      id = "story_b1_remote",
      titleEs = "Primer día en equipo remoto",
      titleEn = "First Day Remote",
      level = LearnerLevel.INTERMEDIO,
      durationLabel = "2 min · B1",
      bodyEn = "Joining an international remote team made Elena nervous at first. Everyone spoke fast, and acronyms popped up in every Slack message. Instead of staying quiet, she kept a small notebook beside her laptop and politely asked teammates to clarify unfamiliar terms. By Friday, her manager praised her proactive communication.",
      bodyEs = "Unirse a un equipo remoto internacional puso nerviosa a Elena al principio. Todos hablaban rápido y aparecían siglas en cada mensaje de Slack. En vez de quedarse callada, mantuvo una libreta junto a su laptop y pidió con cortesía que le aclararan términos nuevos. El viernes su jefa elogió su comunicación proactiva.",
      vocab = listOf(
        StoryVocab("pop up", "aparecer de repente", "/pɑːp ʌp/"),
        StoryVocab("clarify", "aclarar", "/ˈklærəfaɪ/"),
        StoryVocab("proactive", "proactivo", "/proʊˈæktɪv/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué hizo Elena cuando no entendía una sigla?", listOf("La anotó y pidió amablemente que se la aclararan.", "Ignoró todos los mensajes.", "Apagó su cámara."), 0),
        StoryQuestion("¿Cómo reaccionó su jefa el viernes?", listOf("Elogió su comunicación proactiva.", "Le pidió estudiar más horas.", "Canceló la reunión."), 0),
      ),
    ),
    AudioStory(
      id = "story_b1_refund",
      titleEs = "La llamada bien resuelta",
      titleEn = "A Smooth Support Call",
      level = LearnerLevel.INTERMEDIO,
      durationLabel = "2 min · B1",
      bodyEn = "When Andres noticed a double charge on his credit card statement, he feared a stressful argument in English. Instead, he jotted down three clear facts before calling: the order number, the date, and what he needed. Because he stayed polite yet firm, the representative apologized and issued an instant refund in under four minutes.",
      bodyEs = "Cuando Andrés notó un cobro doble en su tarjeta, temió una discusión estresante en inglés. En cambio, anotó tres datos claros antes de llamar: número de orden, fecha y solución esperada. Al mantenerse cortés pero firme, la agente se disculpó y emitió un reembolso inmediato en menos de cuatro minutos.",
      vocab = listOf(
        StoryVocab("jot down", "anotar rápido", "/dʒɑːt daʊn/"),
        StoryVocab("firm", "firme", "/fɜːrm/"),
        StoryVocab("issue", "emitir / otorgar", "/ˈɪʃuː/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué preparó Andrés antes de llamar a soporte?", listOf("Anotó el número de orden, fecha y solución esperada.", "Escribió un ensayo largo.", "Pidió ayuda a un abogado."), 0),
        StoryQuestion("¿Cuánto tardó en resolverse la llamada?", listOf("Menos de cuatro minutos.", "Dos horas completas.", "Una semana."), 0),
      ),
    ),

    // --- B2 (3 historias) ---
    AudioStory(
      id = "story_b2_async",
      titleEs = "El día sin reuniones de los viernes",
      titleEn = "No-Meeting Fridays",
      level = LearnerLevel.INTERMEDIO_ALTO,
      durationLabel = "2.5 min · B2",
      bodyEn = "When Nadia joined a distributed software team across four time zones, her calendar initially overflowed with status calls. Getting used to working asynchronously felt strange at first. Yet after the team shifted to concise written handoffs and no-meeting Fridays, productivity soared—and she wishes they had tried it sooner.",
      bodyEs = "Cuando Nadia se unió a un equipo distribuido entre cuatro husos horarios, su agenda inicialmente rebosaba de llamadas de estatus. Acostumbrarse al trabajo asíncrono le pareció extraño al inicio. Sin embargo, tras pasar a entregas escritas claras y viernes sin reuniones, la productividad despegó—y desearía haberlo probado antes.",
      vocab = listOf(
        StoryVocab("handoff", "entrega entre equipos", "/ˈhændɔːf/"),
        StoryVocab("soared", "se disparó / despegó", "/sɔːrd/"),
        StoryVocab("get used to", "acostumbrarse a", "/ɡet juːst tuː/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué el cambio funcionó tan bien?", listOf("Pasaron a entregas escritas claras y viernes sin llamadas.", "Contrataron más gerentes de reuniones.", "Trabajaron solo de madrugada."), 0),
        StoryQuestion("¿Qué estructura expresa arrepentimiento pasado al final?", listOf("She wishes they had tried it sooner.", "She is trying it tomorrow.", "She got used to calls."), 0),
      ),
    ),
    AudioStory(
      id = "story_b2_flight",
      titleEs = "Conexión perdida en Chicago",
      titleEn = "Missed Connection Recovery",
      level = LearnerLevel.INTERMEDIO_ALTO,
      durationLabel = "2.5 min · B2",
      bodyEn = "After an unexpected storm delayed Daniel's evening plane, he arrived at the transfer desk to learn his connecting flight had already departed. Rather than losing his temper, he calmly stated his itinerary and requested written confirmation for a hotel voucher and morning rebooking. Within ten minutes, the agent had everything sorted out.",
      bodyEs = "Tras retrasarse su vuelo nocturno por una tormenta, Daniel llegó al mostrador de conexiones y supo que su vuelo ya había salido. En lugar de perder la calma, expuso su itinerario con serenidad y pidió confirmación escrita de vale de hotel y reubicación matutina. En diez minutos el agente lo había resuelto todo.",
      vocab = listOf(
        StoryVocab("lose one's temper", "perder la calma", "/luːz ˈtempər/"),
        StoryVocab("voucher", "vale / cupón", "/ˈvaʊtʃər/"),
        StoryVocab("sort out", "resolver / aclarar", "/sɔːrt aʊt/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué estrategia ayudó a Daniel a resolver el reclamo?", listOf("Mantener calma, dar hechos y pedir confirmación escrita.", "Gritar frente al mostrador.", "Comprar un boleto nuevo sin avisar."), 0),
        StoryQuestion("¿Qué significa 'sorted out'?", listOf("Ordenar y resolver un problema.", "Cancelar el equipaje.", "Viajar sin escala."), 0),
      ),
    ),
    AudioStory(
      id = "story_b2_news",
      titleEs = "Detrás del titular viral",
      titleEn = "Beyond the Viral Headline",
      level = LearnerLevel.INTERMEDIO_ALTO,
      durationLabel = "2.5 min · B2",
      bodyEn = "A sensational chart went viral in Diego's group chat claiming coffee prices would triple overnight. Instead of forwarding it impulsively, he cross-checked the primary agricultural report and realized the y-axis had been cropped to exaggerate a minor seasonal shift—a timely reminder to evaluate sources critically.",
      bodyEs = "Un gráfico sensacionalista se viralizó en el chat de Diego afirmando que el café triplicaría su precio de la noche a la mañana. En vez de reenviarlo por impulso, contrastó el informe agrícola primario y notó que habían recortado el eje Y para exagerar un ajuste estacional leve: un recordatorio oportuno para evaluar fuentes con criterio.",
      vocab = listOf(
        StoryVocab("cross-check", "contrastar fuentes", "/ˈkrɔːs tʃek/"),
        StoryVocab("realize", "darse cuenta", "/ˈriːəlaɪz/"),
        StoryVocab("misleading", "engañoso", "/ˌmɪsˈliːdɪŋ/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué descubrió Diego al revisar la fuente primaria?", listOf("Que habían recortado el eje del gráfico exagerando un ajuste menor.", "Que el precio realmente subió un 500%.", "Que nadie toma café en invierno."), 0),
        StoryQuestion("¿Qué significa el falso amigo 'realize' aquí?", listOf("Darse cuenta de algo.", "Realizar una fiesta.", "Volver a leer un correo."), 0),
      ),
    ),

    // --- C1 (3 historias) ---
    AudioStory(
      id = "story_c1_negotiation",
      titleEs = "El arte de hacer pausas",
      titleEn = "The Art of Pausing",
      level = LearnerLevel.PRO,
      durationLabel = "3 min · C1",
      bodyEn = "In high-stakes negotiations, many speakers rush to fill every silence because pauses feel uncomfortable. Yet seasoned leaders treat a two-second pause as leverage: it signals calm authority, invites the other party to elaborate, and prevents rushed concessions. Mastering silence can be just as persuasive as choosing the right idiom.",
      bodyEs = "En negociaciones de alto nivel, muchos hablantes se apresuran a llenar cada silencio porque las pausas se sienten incómodas. Sin embargo, los líderes experimentados usan una pausa de dos segundos como palanca: transmite autoridad serena, invita a la otra parte a profundizar y evita concesiones apresuradas.",
      vocab = listOf(
        StoryVocab("high-stakes", "de alto riesgo o importancia", "/ˌhaɪ ˈsteɪks/"),
        StoryVocab("leverage", "ventaja / palanca", "/ˈlevərɪdʒ/"),
        StoryVocab("concession", "concesión / ceder algo", "/kənˈseʃən/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué muchos hablantes evitan el silencio al negociar?", listOf("Porque las pausas les generan incomodidad.", "Porque olvidan su idioma.", "Porque está prohibido esperar."), 0),
        StoryQuestion("¿Qué logra una pausa breve de dos segundos según el texto?", listOf("Transmite calma y evita concesiones apresuradas.", "Termina la reunión de inmediato.", "Confunde al equipo técnico."), 0),
      ),
    ),
    AudioStory(
      id = "story_c1_ai",
      titleEs = "Curiosidad en la era de la IA",
      titleEn = "Curiosity in the AI Era",
      level = LearnerLevel.PRO,
      durationLabel = "3 min · C1",
      bodyEn = "While generative tools can draft emails and summarize reports in seconds, genuine human rapport still hinges on nuance, active listening, and cultural empathy. Professionals who pair technical fluency with thoughtful storytelling stand out effortlessly—because machines predict patterns, whereas people build trust.",
      bodyEs = "Aunque las herramientas generativas redactan correos y resumen informes en segundos, la conexión humana genuina sigue dependiendo del matiz, la escucha activa y la empatía cultural. Los profesionales que combinan soltura técnica con buena narrativa destacan sin esfuerzo: las máquinas predicen patrones, mientras las personas construyen confianza.",
      vocab = listOf(
        StoryVocab("rapport", "sintonía / buena relación", "/ræˈpɔːr/"),
        StoryVocab("hinge on", "depender clave de", "/hɪndʒ ɑːn/"),
        StoryVocab("stand out", "destacar", "/stænd aʊt/"),
      ),
      questions = listOf(
        StoryQuestion("¿Qué significa 'hinge on' en este contexto?", listOf("Depender fundamentalmente de algo.", "Cerrar una puerta metálica.", "Traducir automáticamente."), 0),
        StoryQuestion("¿Qué diferencia clave destaca el texto entre máquinas y personas?", listOf("Las máquinas predicen patrones; las personas construyen confianza.", "Las personas calculan más rápido que las máquinas.", "La empatía ya no importa en el trabajo."), 0),
      ),
    ),
    AudioStory(
      id = "story_c1_feedback",
      titleEs = "El regalo de la crítica clara",
      titleEn = "Candid Feedback Done Right",
      level = LearnerLevel.PRO,
      durationLabel = "3 min · C1",
      bodyEn = "Delivering tough feedback without eroding trust is a delicate balancing act. Rather than making sweeping generalizations, effective mentors anchor their observations in specific moments and measurable outcomes. When feedback focuses on future growth instead of past blame, teams become both more resilient and more accountable.",
      bodyEs = "Dar retroalimentación difícil sin erosionar la confianza es un acto de equilibrio delicado. En vez de hacer generalizaciones amplias, los mentores eficaces anclan sus observaciones en momentos concretos y resultados medibles. Cuando el feedback se enfoca en el crecimiento futuro en lugar de la culpa pasada, los equipos se vuelven más resilientes y responsables.",
      vocab = listOf(
        StoryVocab("erode", "erosionar / desgastar", "/ɪˈroʊd/"),
        StoryVocab("sweeping", "generalizado / amplio", "/ˈswiːpɪŋ/"),
        StoryVocab("anchor in", "anclar o basar en", "/ˈæŋkər ɪn/"),
      ),
      questions = listOf(
        StoryQuestion("¿En qué basan sus observaciones los mentores eficaces?", listOf("En momentos concretos y resultados medibles.", "En generalizaciones sobre la personalidad.", "En rumores de oficina."), 0),
        StoryQuestion("¿Qué ocurre cuando el feedback mira hacia el crecimiento futuro?", listOf("Los equipos ganan resiliencia y responsabilidad.", "Los empleados renuncian.", "Se pierde la confianza."), 0),
      ),
    ),

    // --- C2 (3 historias) ---
    AudioStory(
      id = "story_c2_mediation",
      titleEs = "El giro en el punto muerto",
      titleEn = "Reframing the Impasse",
      level = LearnerLevel.MAESTRIA,
      durationLabel = "3 min · C2",
      bodyEn = "Both coalitions arrived at the mediation table entrenched in incompatible budget demands. Scarcely had the facilitator opened the session when voices began talking past each other. Rather than litigating past missteps, she asked each delegation what shared outcome they stood to lose—whereupon mutually reciprocal benchmarks quietly emerged.",
      bodyEs = "Ambas coaliciones llegaron a la mesa atrincheradas en exigencias presupuestarias incompatibles. Apenas abrió la sesión la facilitadora, las voces empezaron a hablar sin escucharse. En lugar de litigar tropiezos pasados, preguntó a cada delegación qué resultado común arriesgaban perder; a partir de ahí surgieron criterios recíprocos.",
      vocab = listOf(
        StoryVocab("entrenched", "atrincherado", "/ɪnˈtrentʃt/"),
        StoryVocab("litigate", "litigar / discutir culpas", "/ˈlɪtɪɡeɪt/"),
        StoryVocab("reciprocal", "recíproco", "/rɪˈsɪprəkəl/"),
      ),
      questions = listOf(
        StoryQuestion("¿Cómo logró destrabar la negociación la facilitadora?", listOf("Reenfocó la charla hacia el resultado compartido en riesgo.", "Interrumpió la reunión a los cinco minutos.", "Impuso un recorte unilateral."), 0),
        StoryQuestion("¿Qué estructura C2 abre la segunda oración?", listOf("Scarcely had the facilitator opened ... when...", "Neither ... nor...", "Hadn't it been for..."), 0),
      ),
    ),
    AudioStory(
      id = "story_c2_wit",
      titleEs = "Traducir el understatement",
      titleEn = "Between Candor & Understatement",
      level = LearnerLevel.MAESTRIA,
      durationLabel = "3 min · C2",
      bodyEn = "During Sofia's first London board review, a partner remarked that her forecast contained 'a brave assumption.' Unaware of British understatement, she smiled and thanked him for the compliment! Only later did a colleague gently explain that 'brave' there was tantamount to reckless—teaching her to pair warmth with sharper pragmatic listening.",
      bodyEs = "En la primera junta de Sofía en Londres, un socio comentó que su pronóstico contenía 'una suposición valiente'. Ajena al understatement británico, ¡sonrió y agradeció el cumplido! Solo después un colega le explicó con tacto que allí 'brave' equivalía a temerario, enseñándole a unir calidez con escucha pragmática aguda.",
      vocab = listOf(
        StoryVocab("tantamount", "equivalente a", "/ˈtæntəmaʊnt/"),
        StoryVocab("understatement", "atenuación irónica", "/ˈʌndərsteɪtmənt/"),
        StoryVocab("reckless", "temerario / imprudente", "/ˈrekləs/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué Sofía agradeció al inicio?", listOf("Tomó 'brave' literalmente como un elogio y no como crítica atenuada.", "Quería cambiar de empresa.", "No escuchó la junta en inglés."), 0),
        StoryQuestion("¿Qué significa 'tantamount to reckless'?", listOf("Equivalente en práctica a imprudente.", "Completamente idéntico a brillante.", "Incapaz de entender humor."), 0),
      ),
    ),
    AudioStory(
      id = "story_c2_ethics",
      titleEs = "La analogía y sus límites",
      titleEn = "Precision Beyond Jargon",
      level = LearnerLevel.MAESTRIA,
      durationLabel = "3 min · C2",
      bodyEn = "Public skepticism often swells when scientific uncertainty is spun as dogma or, conversely, stripped of context. Rather than spoon-feeding false certainty, gifted science communicators rely on vivid analogies whose boundary limits are stated upfront—lest convenience tempt the audience to conflate correlation with causation.",
      bodyEs = "El escepticismo público suele crecer cuando la incertidumbre científica se vende como dogma o se saca de contexto. En vez de dar falsa certeza masticada, los grandes divulgadores usan analogías vívidas cuyos límites se declaran desde el inicio, no sea que la comodidad lleve al público a confundir correlación con causalidad.",
      vocab = listOf(
        StoryVocab("lest", "para evitar que", "/lest/"),
        StoryVocab("conflate", "confundir / mezclar", "/kənˈfleɪt/"),
        StoryVocab("spoon-feed", "dar digerido", "/ˈspuːn fiːd/"),
      ),
      questions = listOf(
        StoryQuestion("¿Por qué deben declararse los límites de una analogía científica?", listOf("Para evitar que el público confunda correlación con causalidad.", "Para alargar innecesariamente la charla.", "Para ocultar los datos empíricos."), 0),
        StoryQuestion("¿Qué conjunción introduce el propósito preventivo al final?", listOf("'lest' (+ verbo en subjuntivo 'tempt').", "'although' (+ pasado simple).", "'despite of' (+ verbo)."), 0),
      ),
    ),
  )

  // --- 30 Pronunciation Mastery Drills (5 por nivel A1–C2) ---
  val drills: List<PronunciationDrill> = listOf(
    // A1 (5)
    PronunciationDrill("drill_a1_would", "Silent L: Would / Could / Should", LearnerLevel.NOVATO, "Letras mudas", "/wʊd/ · /kʊd/", "Could I have a coffee, please?", "I would like a cold drink today.", "Nunca pronuncies la L. Suenan 'wud', 'kud' y 'shud' suaves."),
    PronunciationDrill("drill_a1_th", "Sonido TH suave: Think & Thank", LearnerLevel.NOVATO, "Sonidos clave", "/θɪŋk/ · /θæŋk/", "Thank you, I think that is great.", "Thank you for helping me think in English.", "Coloca la punta de la lengua suavemente entre los dientes y deja salir aire."),
    PronunciationDrill("drill_a1_s_start", "S inicial sin 'E': Speak / Study", LearnerLevel.NOVATO, "Inicio sin E", "/spiːk/ · /ˈstʌdi/", "I speak Spanish and study English.", "She starts work in the morning.", "Evita decir 'espeak' o 'estudy': arranca directo silbando la S."),
    PronunciationDrill("drill_a1_h", "H aspirada suave: Hello / Happy / Hungry", LearnerLevel.NOVATO, "Aspiración H", "/həˈloʊ/ · /ˈhæpi/", "Hello! I am happy and hungry.", "Her house has a blue door.", "Suelta aire suave como si empañaras un vidrio, nunca como J fuerte."),
    PronunciationDrill("drill_a1_tobe", "Presentación clara con 'I am'", LearnerLevel.NOVATO, "Fluidez A1", "/aɪ æm/", "Hi! My name is Alex and I am happy.", "I am from Colombia and I am twenty.", "Pronuncia claro 'My name is' y no te comas la palabra 'I'."),

    // A2 (5)
    PronunciationDrill("drill_a2_receipt", "Silent P: Receipt", LearnerLevel.JUNIOR, "Letras mudas", "/rɪˈsiːt/", "Could I get the receipt, please?", "Keep your receipt just in case.", "En 'receipt' la P es muda: suena 'ri-SÍT'."),
    PronunciationDrill("drill_a2_aisle", "Silent S: Aisle vs Island", LearnerLevel.JUNIOR, "Letras mudas", "/aɪl/ · /ˈaɪlənd/", "I would like an aisle seat, please.", "Walk down aisle three for fresh fruit.", "En 'aisle' e 'island' la S es muda: 'áil' y 'ái-land'."),
    PronunciationDrill("drill_a2_salmon", "Silent L: Salmon & Half", LearnerLevel.JUNIOR, "Letras mudas", "/ˈsæmən/ · /hæf/", "I would like the grilled salmon, please.", "Add half a lemon to the salmon.", "En 'salmon' y 'half' la L es muda: 'sá-mon' y 'jaf'."),
    PronunciationDrill("drill_a2_v_b", "Vibración V vs B: Very & Visit", LearnerLevel.JUNIOR, "Consonantes", "/ˈveri/ · /ˈvɪzɪt/", "We visited a very nice village.", "Vegetables are very fresh today.", "Para la V toca el labio inferior con los dientes superiores y hazlo vibrar."),
    PronunciationDrill("drill_a2_straight", "Silent GH: Straight & Right", LearnerLevel.JUNIOR, "Letras mudas", "/streɪt/ · /raɪt/", "Go straight two blocks and turn right.", "Flight five leaves right on time.", "La combinación GH es muda en 'straight', 'right' y 'flight'."),

    // B1 (5)
    PronunciationDrill("drill_b1_figure", "Phrasal Verb: Figure it out", LearnerLevel.INTERMEDIO, "Enlace rítmico", "/ˈfɪɡjər ɪt aʊt/", "We will figure it out together.", "I finally figured out how this works.", "Conecta 'figure-it-out' como un solo bloque fluido: 'figuer-it-aut'."),
    PronunciationDrill("drill_b1_ed", "Terminación -ED: Worked vs Needed", LearnerLevel.INTERMEDIO, "Ritmo y pasado", "/wɜːrkt/ · /ˈniːdɪd/", "I worked hard and needed a break.", "Yesterday I walked home and cooked dinner.", "En 'worked' no agregues sílaba extra (suena 'uerkt'); solo suena '-id' tras T o D."),
    PronunciationDrill("drill_b1_comfortable", "Sílabas ocultas: Comfortable", LearnerLevel.INTERMEDIO, "Reducción natural", "/ˈkʌmftərbəl/", "I feel comfortable speaking English now.", "This chair is surprisingly comfortable.", "No pronuncies 4 sílabas lentas; agrúpalo en 3: 'kámf-tor-bol'."),
    PronunciationDrill("drill_b1_schedule", "US vs UK: Schedule", LearnerLevel.INTERMEDIO, "Acentos", "/ˈskedʒuːl/", "Let me check my schedule tomorrow.", "We stayed ahead of schedule this week.", "En EE.UU. suena 'ské-dyul'; en Reino Unido suena 'shé-dyul'."),
    PronunciationDrill("drill_b1_although", "Conectores B1: Although & Despite", LearnerLevel.INTERMEDIO, "Conectores", "/ɔːlˈðoʊ/ · /dɪˈspaɪt/", "Although it rained, we arrived on time.", "Despite the delay, everyone stayed calm.", "En 'although' la GH no suena y TH vibra suave: 'ol-DOU'."),

    // B2 (5)
    PronunciationDrill("drill_b2_mortgage", "Silent T: Mortgage & Whistle", LearnerLevel.INTERMEDIO_ALTO, "Letras mudas B2", "/ˈmɔːrɡɪdʒ/", "Mortgage rates shifted this quarter.", "Tighten the timeline without rushing quality.", "En 'mortgage' la T intermedia no suena ('MÓR-guich')."),
    PronunciationDrill("drill_b2_whereas", "Énfasis B2: Whereas & Nonetheless", LearnerLevel.INTERMEDIO_ALTO, "Conectores B2", "/ˌwerˈæz/", "Renting stays flexible, whereas buying builds equity.", "Nonetheless, written updates prevent wasted meetings.", "Marca el acento de 'whereas' en la segunda sílaba ('uer-ÁS')."),
    PronunciationDrill("drill_b2_conditionals", "Enlace fluido: Would have & Should have", LearnerLevel.INTERMEDIO_ALTO, "Reducción oral", "/ˈwʊdəv/ · /ˈʃʊdəv/", "If we had tested earlier, we would have saved time.", "They should have notified passengers yesterday.", "Une suave 'would have' ('wu-dav') sin cortar el flujo."),
    PronunciationDrill("drill_b2_voucher", "Sonidos V / CH: Voucher & Achieve", LearnerLevel.INTERMEDIO_ALTO, "Consonantes B2", "/ˈvaʊtʃər/", "I requested a hotel voucher and confirmation.", "Remote teams achieved faster project handoffs.", "Vibra la V inicial con labio y marca firma 'ch' en 'voucher'."),
    PronunciationDrill("drill_b2_separable", "Ritmo Phrasal Separable: Sort it out", LearnerLevel.INTERMEDIO_ALTO, "Enlace verbal", "/sɔːrt ɪt aʊt/", "That issue matters, let us sort it out.", "Why did they turn the proposal down?", "Enlaza 'sort-it-out' sin pausas entre las tres palabras."),

    // C1 (5)
    PronunciationDrill("drill_c1_leverage", "Vocabulario C1: Leverage & Benchmark", LearnerLevel.PRO, "Inglés ejecutivo", "/ˈlevərɪdʒ/", "We can leverage market benchmarks.", "Let us leverage our track record today.", "'Leverage' lleva acento al inicio: 'LÉ-ve-rich'."),
    PronunciationDrill("drill_c1_though", "Trampa -OUGH: Though / Through / Thought", LearnerLevel.PRO, "Trampas clásicas", "/ðoʊ/ · /θruː/ · /θɔːt/", "Even though it was tough, we got through.", "I thought through every detail carefully.", "Though = 'dou', tough = 'taf', through = 'zru', thought = 'zot'."),
    PronunciationDrill("drill_c1_rapport", "T final muda: Rapport & Nuance", LearnerLevel.PRO, "Préstamos clave", "/ræˈpɔːr/ · /ˈnuːɑːns/", "Active listening builds instant rapport.", "Tone carries subtle cultural nuance.", "En 'rapport' la T final es muda: suena 'ra-PÓR'."),
    PronunciationDrill("drill_c1_inversion", "Énfasis C1: Not only did we", LearnerLevel.PRO, "Ritmo retórico", "/nɑːt ˈoʊnli dɪd/", "Not only did we finish, we boosted quality.", "Seldom have I seen such rapid progress.", "Marca una ligera subida de tono en 'Not only' y fluidez en 'did we'."),
    PronunciationDrill("drill_c1_needle", "Modismo C1: Move the needle", LearnerLevel.PRO, "Modismos", "/muːv ðə ˈniːdəl/", "Let us focus on what moves the needle.", "Had we pivoted earlier, we would lead today.", "Suaviza TH en 'the' y alarga ligeramente 'needle': 'ní-dol'."),

    // C2 (5)
    PronunciationDrill("drill_c2_psychology", "Consonante inicial muda: Psychological & Subtle", LearnerLevel.MAESTRIA, "Letras mudas C2", "/ˌsaɪkəˈlɑːdʒɪkəl/ · /ˈsʌtəl/", "Psychological safety creates subtle trust.", "A subtle concession resolved the impasse.", "En 'psychological' la P es muda y en 'subtle' la B es muda ('SÁ-tol')."),
    PronunciationDrill("drill_c2_dichotomy", "CH = K: Dichotomy & Architecture", LearnerLevel.MAESTRIA, "Precisión léxica C2", "/daɪˈkɑːtəmi/", "Reject that false dichotomy between speed and care.", "Proprietary software creates lasting defensibility.", "'Dichotomy' lleva K fuerte y acento en KÓ: 'dai-KÓ-to-mi'."),
    PronunciationDrill("drill_c2_scarcely", "Cadencia invertida C2: Scarcely had we", LearnerLevel.MAESTRIA, "Retórica C2", "/ˈskersli hæd/", "Scarcely had she finished when debate began.", "Were we to separate tactics, agreement emerges.", "Suaviza 'had we' después de 'Scarcely' marcando pausa breve ante 'when'."),
    PronunciationDrill("drill_c2_fraught", "GH muda: Fraught with & Outweighs", LearnerLevel.MAESTRIA, "Colocaciones C2", "/frɔːt wɪð/", "Hasty shortcuts are fraught with unseen risk.", "Consent remains crucial lest trust erode.", "'Fraught' suena limpio 'frót', rimando con 'thought'."),
    PronunciationDrill("drill_c2_antithesis", "Ritmo de antítesis: Captures headlines", LearnerLevel.MAESTRIA, "Prosodia C2", "/ˈkæptʃərz/", "Speed captures headlines; quiet craft endures.", "True mastery turns friction into clarity.", "Pausa medio segundo entre las dos cláusulas para proyectar autoridad."),
  )

  val webPortals: List<FreeWebPortal> = listOf(
    FreeWebPortal("VOA Learning English", "A1 – B2", "Inglés lento y claro", "Noticias y reportajes narrados a ritmo pausado en inglés americano.", "https://learningenglish.voanews.com/", LearnerLevel.NOVATO, LearnerLevel.INTERMEDIO_ALTO),
    FreeWebPortal("New General Service List (NGSL)", "A1 – B2", "Vocabulario científico gratuito", "Las 2,800 palabras esenciales que cubren ~92% del inglés real (CC BY-SA 4.0).", "https://www.newgeneralservicelist.com/", LearnerLevel.NOVATO, LearnerLevel.INTERMEDIO_ALTO),
    FreeWebPortal("Randall's ESL Listening Lab", "A1 – C1", "Entrenamiento de oído", "Cientos de conversaciones cotidianas gratuitas con quizzes por nivel.", "https://www.esl-lab.com/", LearnerLevel.NOVATO, LearnerLevel.PRO),
    FreeWebPortal("British Council LearnEnglish", "A1 – C1", "Podcasts y gramática", "Ejercicios interactivos gratuitos por nivel CEFR, listening y negocios.", "https://learnenglish.britishcouncil.org/", LearnerLevel.NOVATO, LearnerLevel.PRO),
    FreeWebPortal("BBC Learning English", "A2 – C2", "Audio y noticias reales", "6 Minute English, audios reales con transcripción y vocabulario británico.", "https://www.bbc.co.uk/learningenglish/", LearnerLevel.JUNIOR, LearnerLevel.MAESTRIA),
    FreeWebPortal("YouGlish", "A1 – C2", "Pronunciación en contexto", "Escucha cualquier palabra pronunciada por nativos en miles de charlas reales.", "https://youglish.com/", LearnerLevel.NOVATO, LearnerLevel.MAESTRIA),
    FreeWebPortal("Cambridge Dictionary", "A1 – C2", "Diccionario US/UK + NAWL", "Definiciones claras, ejemplos reales y audio doble americano y británico.", "https://dictionary.cambridge.org/", LearnerLevel.NOVATO, LearnerLevel.MAESTRIA),
    FreeWebPortal("Tatoeba & NAWL Academic", "B1 – C2", "Corpus y vocabulario avanzado", "Millones de oraciones bilingües y lista académica NAWL para C1–C2.", "https://tatoeba.org/en/sentences/search?from=eng&to=spa", LearnerLevel.INTERMEDIO, LearnerLevel.MAESTRIA),
  )

  fun storiesForLevel(level: LearnerLevel): List<AudioStory> = stories.filter { it.level == level }

  fun drillsForLevel(level: LearnerLevel): List<PronunciationDrill> = drills.filter { it.level == level }

  fun portalsForLevel(level: LearnerLevel): List<FreeWebPortal> =
    webPortals.filter { level.ordinal in it.minLevel.ordinal..it.maxLevel.ordinal }

  fun wordSuggestionsFor(level: LearnerLevel): List<String> = when (level) {
    LearnerLevel.NOVATO -> listOf("coffee", "schedule", "window", "receipt", "curious")
    LearnerLevel.JUNIOR -> listOf("aisle", "luggage", "comfortable", "commute", "fluent")
    LearnerLevel.INTERMEDIO -> listOf("challenge", "feedback", "deadline", "accomplish", "reliable")
    LearnerLevel.INTERMEDIO_ALTO -> listOf("perspective", "mortgage", "voucher", "whereas", "candid")
    LearnerLevel.PRO -> listOf("resilient", "leverage", "eloquent", "nuance", "benchmark")
    LearnerLevel.MAESTRIA -> listOf("impasse", "candor", "dichotomy", "spurious", "tantamount")
  }
}
