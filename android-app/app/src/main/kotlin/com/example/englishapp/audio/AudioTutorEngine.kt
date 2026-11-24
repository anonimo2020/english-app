package com.example.englishapp.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.TutorCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.UUID

data class AudioEngineState(
  val isTtsReady: Boolean = false,
  val isSpeaking: Boolean = false,
  val speakingMessageId: String? = null,
  val speakingText: String = "",
  val speakingLanguage: String = "en",
  val spokenCharRange: IntRange? = null,
  val isListening: Boolean = false,
  val partialTranscript: String = "",
  val rmsNormalized: Float = 0f,
  val lastError: String? = null,
)

class AudioTutorEngine(context: Context) {

  private val appContext = context.applicationContext
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _state = MutableStateFlow(AudioEngineState())
  val state: StateFlow<AudioEngineState> = _state.asStateFlow()

  private var tts: TextToSpeech? = null
  private var mediaPlayer: MediaPlayer? = null
  private var speechRecognizer: SpeechRecognizer? = null
  private var currentUtteranceId: String? = null
  private var pendingSpeak: PendingSpeakRequest? = null
  private var onUtteranceDoneCallback: (() -> Unit)? = null
  private var activeListenToken: Long = 0L
  private var activeOnFinalResult: ((String) -> Unit)? = null
  private var pendingFallbackRunnable: Runnable? = null
  private var isReleased = false

  private data class PendingSpeakRequest(
    val text: String,
    val messageId: String?,
    val character: TutorCharacter,
    val accent: EnglishAccent,
    val speedRate: Float,
    val isSpanish: Boolean = false,
    val onDone: (() -> Unit)?,
  )

  init {
    initTts()
  }

  private fun initTts() {
    tts = TextToSpeech(appContext) { status ->
      if (isReleased) return@TextToSpeech
      if (status == TextToSpeech.SUCCESS) {
        configureTtsListener()
        _state.update { it.copy(isTtsReady = true) }
        val pending = pendingSpeak
        if (pending != null) {
          pendingSpeak = null
          if (pending.isSpanish) {
            speakSpanish(
              text = pending.text,
              messageId = pending.messageId,
              character = pending.character,
              speedRate = pending.speedRate,
              onDone = pending.onDone,
            )
          } else {
            speak(
              text = pending.text,
              messageId = pending.messageId,
              character = pending.character,
              accent = pending.accent,
              speedRate = pending.speedRate,
              onDone = pending.onDone,
            )
          }
        }
      } else {
        _state.update {
          it.copy(
            isTtsReady = false,
            lastError = "No se pudo inicializar el motor de voz (TTS) en este dispositivo.",
          )
        }
      }
    }
  }

  private fun configureTtsListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        if (utteranceId != currentUtteranceId) return
        _state.update { it.copy(isSpeaking = true, lastError = null) }
      }

      override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
        if (utteranceId != currentUtteranceId) return
        if (end > start && start >= 0) {
          _state.update { it.copy(spokenCharRange = start until end) }
        }
      }

      override fun onDone(utteranceId: String?) {
        if (utteranceId != currentUtteranceId) return
        val callback = onUtteranceDoneCallback
        onUtteranceDoneCallback = null
        _state.update {
          it.copy(
            isSpeaking = false,
            speakingMessageId = null,
            speakingText = "",
            spokenCharRange = null,
          )
        }
        if (callback != null) {
          mainHandler.post { callback.invoke() }
        }
      }

      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        handleTtsError(utteranceId)
      }

      override fun onError(utteranceId: String?, errorCode: Int) {
        handleTtsError(utteranceId)
      }
    })
  }

  private fun handleTtsError(utteranceId: String?) {
    if (utteranceId != currentUtteranceId) return
    onUtteranceDoneCallback = null
    _state.update {
      it.copy(
        isSpeaking = false,
        speakingMessageId = null,
        speakingText = "",
        spokenCharRange = null,
      )
    }
  }

  fun playAudioUrlOrTts(
    audioUrl: String?,
    fallbackText: String,
    messageId: String?,
    character: TutorCharacter,
    accent: EnglishAccent,
    speedRate: Float,
    onDone: (() -> Unit)? = null,
  ) {
    val cleanUrl = audioUrl?.trim().orEmpty()
    if (cleanUrl.isEmpty() || isReleased) {
      speak(fallbackText, messageId, character, accent, speedRate, onDone)
      return
    }

    stopSpeaking()
    cancelListening()

    _state.update {
      it.copy(
        isSpeaking = true,
        speakingMessageId = messageId,
        speakingText = fallbackText.trim(),
        speakingLanguage = "en",
        spokenCharRange = null,
        lastError = null,
      )
    }

    try {
      val player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
        )
        setDataSource(cleanUrl)
        setOnPreparedListener { mp ->
          if (isReleased || mediaPlayer !== mp) {
            runCatching { mp.release() }
            return@setOnPreparedListener
          }
          mp.start()
        }
        setOnCompletionListener { mp ->
          if (mediaPlayer === mp) {
            releaseMediaPlayerInternal()
            _state.update {
              it.copy(
                isSpeaking = false,
                speakingMessageId = null,
                speakingText = "",
                spokenCharRange = null,
              )
            }
            onDone?.let { mainHandler.post(it) }
          }
        }
        setOnErrorListener { mp, _, _ ->
          if (mediaPlayer === mp) {
            releaseMediaPlayerInternal()
            speak(fallbackText, messageId, character, accent, speedRate, onDone)
          }
          true
        }
        prepareAsync()
      }
      mediaPlayer = player
    } catch (_: Exception) {
      releaseMediaPlayerInternal()
      speak(fallbackText, messageId, character, accent, speedRate, onDone)
    }
  }

  fun speak(
    text: String,
    messageId: String?,
    character: TutorCharacter,
    accent: EnglishAccent,
    speedRate: Float,
    onDone: (() -> Unit)? = null,
  ) {
    val cleanText = text.trim()
    if (cleanText.isEmpty() || isReleased) return

    val engine = tts
    if (engine == null || !_state.value.isTtsReady) {
      pendingSpeak = PendingSpeakRequest(
        text = cleanText,
        messageId = messageId,
        character = character,
        accent = accent,
        speedRate = speedRate,
        isSpanish = false,
        onDone = onDone,
      )
      return
    }

    val preferredCountries = if (accent == EnglishAccent.UK) listOf("GB", "UK", "") else listOf("US", "")
    val fallbackLocale = if (accent == EnglishAccent.UK) Locale.UK else Locale.US
    enqueueTtsUtterance(
      engine = engine,
      cleanText = cleanText,
      messageId = messageId,
      languageCode = "en",
      preferredCountries = preferredCountries,
      fallbackLocale = fallbackLocale,
      character = character,
      speedRate = speedRate.coerceIn(0.55f, 1.45f),
      onDone = onDone,
    )
  }

  fun speakSpanish(
    text: String,
    messageId: String?,
    character: TutorCharacter = TutorCharacter.MANU,
    speedRate: Float = 0.96f,
    onDone: (() -> Unit)? = null,
  ) {
    val cleanText = text.trim()
    if (cleanText.isEmpty() || isReleased) return

    val engine = tts
    if (engine == null || !_state.value.isTtsReady) {
      pendingSpeak = PendingSpeakRequest(
        text = cleanText,
        messageId = messageId,
        character = character,
        accent = EnglishAccent.US,
        speedRate = speedRate,
        isSpanish = true,
        onDone = onDone,
      )
      return
    }

    val esUs = Locale.Builder().setLanguage("es").setRegion("US").build()
    enqueueTtsUtterance(
      engine = engine,
      cleanText = cleanText,
      messageId = messageId,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "419", "CO", "AR", "ES", ""),
      fallbackLocale = esUs,
      character = character,
      speedRate = speedRate.coerceIn(0.65f, 1.35f),
      onDone = onDone,
    )
  }

  private fun enqueueTtsUtterance(
    engine: TextToSpeech,
    cleanText: String,
    messageId: String?,
    languageCode: String,
    preferredCountries: List<String>,
    fallbackLocale: Locale,
    character: TutorCharacter,
    speedRate: Float,
    onDone: (() -> Unit)?,
  ) {
    releaseMediaPlayerInternal()
    cancelListening()

    val selection = applyVoiceForLanguage(
      engine = engine,
      languageCode = languageCode,
      preferredCountries = preferredCountries,
      fallbackLocale = fallbackLocale,
      character = character,
    )
    engine.setPitch(selection.recommendedPitch)
    engine.setSpeechRate(speedRate)

    val utteranceId = UUID.randomUUID().toString()
    currentUtteranceId = utteranceId
    onUtteranceDoneCallback = onDone

    _state.update {
      it.copy(
        isSpeaking = true,
        speakingMessageId = messageId,
        speakingText = cleanText,
        speakingLanguage = languageCode,
        spokenCharRange = null,
        lastError = null,
      )
    }

    val params = Bundle().apply {
      putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
    }
    engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
  }

  private fun applyVoiceForLanguage(
    engine: TextToSpeech,
    languageCode: String,
    preferredCountries: List<String>,
    fallbackLocale: Locale,
    character: TutorCharacter,
  ): VoiceSelection {
    return runCatching {
      val androidVoices = engine.voices.orEmpty().toList()
      val candidates = androidVoices.map { voice ->
        VoiceCandidate(
          name = voice.name,
          language = voice.locale.language,
          country = voice.locale.country,
          isNetworkRequired = voice.isNetworkConnectionRequired,
          quality = voice.quality,
          features = voice.features.orEmpty(),
        )
      }
      val selection = VoiceProfileMatcher.selectBestVoice(
        candidates = candidates,
        languageCode = languageCode,
        preferredCountries = preferredCountries,
        character = character,
      )
      val chosenVoice = selection.voiceName?.let { chosenName ->
        androidVoices.firstOrNull { it.name == chosenName }
      }
      if (chosenVoice != null) {
        // Call setLanguage with the voice's exact locale BEFORE assigning engine.voice so Android does not reset it.
        engine.setLanguage(chosenVoice.locale)
        engine.voice = chosenVoice
      } else {
        setSupportedLanguageFallback(engine, languageCode, preferredCountries, fallbackLocale)
      }
      Log.d(
        "AudioTutorEngine",
        "TTS char=${character.id} lang=$languageCode voice=${chosenVoice?.name ?: "default"} matchedGender=${selection.matchedExpectedGender} pitch=${selection.recommendedPitch}",
      )
      selection
    }.getOrElse {
      setSupportedLanguageFallback(engine, languageCode, preferredCountries, fallbackLocale)
      VoiceSelection(
        voiceName = null,
        matchedExpectedGender = false,
        recommendedPitch = if (character == TutorCharacter.EDDY) 0.76f else 1.10f,
      )
    }
  }

  private fun setSupportedLanguageFallback(
    engine: TextToSpeech,
    languageCode: String,
    preferredCountries: List<String>,
    fallbackLocale: Locale,
  ) {
    val locales = preferredCountries
      .filter { it.isNotEmpty() }
      .map { Locale.Builder().setLanguage(languageCode).setRegion(it).build() } +
      listOf(fallbackLocale, Locale.Builder().setLanguage(languageCode).build())
    for (loc in locales) {
      val res = engine.setLanguage(loc)
      if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
        return
      }
    }
    engine.setLanguage(fallbackLocale)
  }

  fun stopSpeaking() {
    pendingSpeak = null
    currentUtteranceId = null
    onUtteranceDoneCallback = null
    releaseMediaPlayerInternal()
    runCatching { tts?.stop() }
    _state.update {
      it.copy(
        isSpeaking = false,
        speakingMessageId = null,
        speakingText = "",
        spokenCharRange = null,
      )
    }
  }

  fun startListening(
    accent: EnglishAccent,
    silenceMs: Long,
    onFinalResult: (String) -> Unit,
  ) {
    if (isReleased) return
    mainHandler.post {
      if (isReleased) return@post
      stopSpeaking()

      if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
        _state.update {
          it.copy(
            isListening = false,
            lastError = "Este dispositivo no tiene servicio de dictado por voz. Usa el teclado a la derecha para escribir.",
          )
        }
        return@post
      }

      clearFallbackRunnable()
      destroyRecognizerInternal()

      val token = ++activeListenToken
      activeOnFinalResult = onFinalResult

      val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
      speechRecognizer = recognizer

      recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          if (token != activeListenToken) return
          _state.update {
            it.copy(
              isListening = true,
              partialTranscript = "",
              rmsNormalized = 0.08f,
              lastError = null,
            )
          }
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
          if (token != activeListenToken) return
          val target = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
          _state.update { current ->
            val smoothed = current.rmsNormalized * 0.45f + target * 0.55f
            current.copy(rmsNormalized = smoothed)
          }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          if (token != activeListenToken) return
          _state.update { it.copy(rmsNormalized = 0f) }
        }

        override fun onError(error: Int) {
          if (token != activeListenToken) return
          clearFallbackRunnable()
          val partial = _state.value.partialTranscript.trim()
          val callback = activeOnFinalResult
          activeOnFinalResult = null

          if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
            partial.isNotEmpty()
          ) {
            activeListenToken++
            _state.update {
              it.copy(
                isListening = false,
                partialTranscript = "",
                rmsNormalized = 0f,
                lastError = null,
              )
            }
            callback?.invoke(partial)
            return
          }

          val message = mapSpeechError(error)
          _state.update {
            it.copy(
              isListening = false,
              partialTranscript = "",
              rmsNormalized = 0f,
              lastError = message,
            )
          }
        }

        override fun onResults(results: Bundle?) {
          if (token != activeListenToken) return
          clearFallbackRunnable()
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val recognized = matches?.firstOrNull()?.trim().orEmpty()
          val finalText = recognized.ifEmpty { _state.value.partialTranscript.trim() }
          val callback = activeOnFinalResult
          activeOnFinalResult = null
          activeListenToken++

          _state.update {
            it.copy(
              isListening = false,
              partialTranscript = "",
              rmsNormalized = 0f,
              lastError = if (finalText.isEmpty()) {
                "No te escuché claro. Intenta hablar un poco más fuerte o usa el teclado."
              } else {
                null
              },
            )
          }
          if (finalText.isNotEmpty()) {
            callback?.invoke(finalText)
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          if (token != activeListenToken) return
          val partial = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
          if (partial.isNotEmpty()) {
            _state.update { it.copy(partialTranscript = partial) }
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
      })

      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, accent.localeTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, accent.localeTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
          (silenceMs - 300L).coerceAtLeast(1000L),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
      }

      _state.update {
        it.copy(
          isListening = true,
          partialTranscript = "",
          rmsNormalized = 0.08f,
          lastError = null,
        )
      }
      runCatching { recognizer.startListening(intent) }.onFailure { err ->
        _state.update {
          it.copy(
            isListening = false,
            rmsNormalized = 0f,
            lastError = "No se pudo iniciar el micrófono (${err.message ?: "error"}).",
          )
        }
      }
    }
  }

  /**
   * Detiene la escucha conservando el token activo para recibir `onResults`
   * o enviar `partialTranscript` tras un breve fallback si el sistema demora.
   */
  fun finishListeningAndSubmit() {
    mainHandler.post {
      val token = activeListenToken
      val callback = activeOnFinalResult
      if (!_state.value.isListening || callback == null) {
        cancelListening()
        return@post
      }

      _state.update { it.copy(isListening = false, rmsNormalized = 0f) }
      runCatching { speechRecognizer?.stopListening() }

      clearFallbackRunnable()
      val fallback = Runnable {
        if (token != activeListenToken) return@Runnable
        val partial = _state.value.partialTranscript.trim()
        activeOnFinalResult = null
        activeListenToken++
        _state.update {
          it.copy(
            isListening = false,
            partialTranscript = "",
            rmsNormalized = 0f,
          )
        }
        if (partial.isNotEmpty()) {
          callback.invoke(partial)
        }
      }
      pendingFallbackRunnable = fallback
      mainHandler.postDelayed(fallback, 1100L)
    }
  }

  fun cancelListening() {
    activeListenToken++
    activeOnFinalResult = null
    clearFallbackRunnable()
    mainHandler.post {
      runCatching { speechRecognizer?.stopListening() }
      runCatching { speechRecognizer?.cancel() }
      _state.update {
        it.copy(
          isListening = false,
          partialTranscript = "",
          rmsNormalized = 0f,
        )
      }
    }
  }

  fun stopListening() = cancelListening()

  fun clearError() {
    _state.update { it.copy(lastError = null) }
  }

  fun release() {
    if (isReleased) return
    isReleased = true
    activeListenToken++
    activeOnFinalResult = null
    onUtteranceDoneCallback = null
    clearFallbackRunnable()
    releaseMediaPlayerInternal()
    runCatching { tts?.stop() }
    runCatching { tts?.shutdown() }
    tts = null
    mainHandler.post { destroyRecognizerInternal() }
  }

  private fun clearFallbackRunnable() {
    pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
    pendingFallbackRunnable = null
  }

  private fun releaseMediaPlayerInternal() {
    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.release()
      }
    }
    mediaPlayer = null
  }

  private fun destroyRecognizerInternal() {
    runCatching { speechRecognizer?.cancel() }
    runCatching { speechRecognizer?.destroy() }
    speechRecognizer = null
  }

  private fun mapSpeechError(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "No se pudo capturar audio desde el micrófono."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Activa el permiso de micrófono para hablar."
    SpeechRecognizer.ERROR_NO_MATCH -> "No entendí bien la frase. Toca el micrófono e inténtalo de nuevo."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No detecté voz. Toca el micrófono cuando estés listo."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
      "El dictado por voz necesita conexión o paquete offline. Puedes escribir con el teclado."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocimiento de voz está ocupado, intenta en un segundo."
    else -> "Error del reconocimiento de voz (código $error). Puedes usar el teclado."
  }
}
