package com.example.englishapp

import com.example.englishapp.audio.DetectedVoiceGender
import com.example.englishapp.audio.VoiceCandidate
import com.example.englishapp.audio.VoiceProfileMatcher
import com.example.englishapp.model.TutorCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceProfileMatcherTest {

  private fun voice(
    name: String,
    lang: String,
    country: String,
    network: Boolean = false,
    quality: Int = 400,
    features: Set<String> = emptySet(),
  ) = VoiceCandidate(name, lang, country, network, quality, features)

  @Test
  fun neverClassifiesFemaleNamesOrLabelsAsMale() {
    assertEquals(
      DetectedVoiceGender.FEMALE,
      VoiceProfileMatcher.detectGender(voice("en-us-x-sfg#female_1-local", "en", "US")),
    )
    assertEquals(
      DetectedVoiceGender.FEMALE,
      VoiceProfileMatcher.detectGender(voice("es-us-x-sfb-local", "es", "US")),
    )
    assertEquals(
      DetectedVoiceGender.FEMALE,
      VoiceProfileMatcher.detectGender(voice("Samantha", "en", "US")),
    )
    assertEquals(
      DetectedVoiceGender.MALE,
      VoiceProfileMatcher.detectGender(voice("es-us-x-esd-local", "es", "US")),
    )
    assertEquals(
      DetectedVoiceGender.MALE,
      VoiceProfileMatcher.detectGender(voice("es-es-x-eed-local", "es", "ES")),
    )
    assertEquals(
      DetectedVoiceGender.MALE,
      VoiceProfileMatcher.detectGender(voice("en-us-x-iom#male_2-local", "en", "US")),
    )
  }

  @Test
  fun eddySelectsMaleSpanishVoiceEvenAcrossSpanishRegionsBeforeFemaleVoice() {
    val candidates = listOf(
      voice("es-us-x-sfb-local", "es", "US", quality = 500),
      voice("es-es-x-eee-local", "es", "ES", quality = 500),
      voice("es-es-x-eed-local", "es", "ES", quality = 400),
      voice("en-us-x-iom-local", "en", "US", quality = 500),
    )

    val eddySpanish = VoiceProfileMatcher.selectBestVoice(
      candidates = candidates,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "419", "ES", ""),
      character = TutorCharacter.EDDY,
    )
    assertEquals("es-es-x-eed-local", eddySpanish.voiceName)
    assertTrue(eddySpanish.matchedExpectedGender)
    assertEquals(0.95f, eddySpanish.recommendedPitch, 0.001f)

    val manuSpanish = VoiceProfileMatcher.selectBestVoice(
      candidates = candidates,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "419", "ES", ""),
      character = TutorCharacter.MANU,
    )
    assertEquals("es-us-x-sfb-local", manuSpanish.voiceName)
    assertTrue(manuSpanish.matchedExpectedGender)
    assertEquals(1.04f, manuSpanish.recommendedPitch, 0.001f)
  }

  @Test
  fun eddyUsesUsMaleVoiceWhenAvailableAndDropsPitchFallbackIfOnlyFemaleExists() {
    val withUsMale = listOf(
      voice("es-us-x-sfb-local", "es", "US"),
      voice("es-us-x-esd-local", "es", "US"),
      voice("es-es-x-eed-local", "es", "ES"),
    )
    val preferred = VoiceProfileMatcher.selectBestVoice(
      candidates = withUsMale,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "419", "ES", ""),
      character = TutorCharacter.EDDY,
    )
    assertEquals("es-us-x-esd-local", preferred.voiceName)
    assertTrue(preferred.matchedExpectedGender)

    val onlyFemale = listOf(
      voice("es-us-x-sfb-local", "es", "US"),
    )
    val fallback = VoiceProfileMatcher.selectBestVoice(
      candidates = onlyFemale,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "419", "ES", ""),
      character = TutorCharacter.EDDY,
    )
    assertFalse(fallback.matchedExpectedGender)
    assertEquals(0.76f, fallback.recommendedPitch, 0.001f)
  }

  @Test
  fun skipsNotInstalledVoicesWhenInstalledAlternativeExists() {
    val candidates = listOf(
      voice("es-us-x-esd-local", "es", "US", features = setOf("notInstalled")),
      voice("es-es-x-eef-local", "es", "ES"),
    )
    val selected = VoiceProfileMatcher.selectBestVoice(
      candidates = candidates,
      languageCode = "es",
      preferredCountries = listOf("US", "MX", "ES"),
      character = TutorCharacter.EDDY,
    )
    assertEquals("es-es-x-eef-local", selected.voiceName)
    assertTrue(selected.matchedExpectedGender)
  }
}
