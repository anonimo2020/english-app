package com.example.englishapp

import com.example.englishapp.data.OfflineDemoTutor
import com.example.englishapp.model.ChatMessage
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.TutorCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDemoTutorTest {

  @Test
  fun generatesNonEmptyResponsesAcrossAllCharactersLevelsAndScenarios() {
    val history = listOf(
      ChatMessage("assistant", "Hi there!"),
      ChatMessage("user", "I want to practice English with you today."),
    )
    for (character in TutorCharacter.entries) {
      for (level in LearnerLevel.entries) {
        for (scenario in ScenarioCatalog.all) {
          val reply = OfflineDemoTutor.generateReply(
            character = character,
            level = level,
            scenarioId = scenario.id,
            accent = EnglishAccent.US,
            history = history,
          )
          assertTrue(
            "Empty reply for ${character.id}/${level.id}/${scenario.id}",
            reply.reply.isNotBlank(),
          )
          assertNotNull(reply.translationEs)
          assertTrue(reply.translationEs!!.isNotBlank())
          assertNotNull(
            "Missing coachNoteEs for ${character.id}/${level.id}/${scenario.id}",
            reply.coachNoteEs,
          )
          assertTrue(
            "Empty coachNoteEs for ${character.id}/${level.id}/${scenario.id}",
            reply.coachNoteEs!!.isNotBlank(),
          )
          assertTrue("XP out of bounds: ${reply.xpEarned}", reply.xpEarned in 10..25)
        }
      }
    }

    val coffeeScenario = ScenarioCatalog.all.first { it.id == "coffee_shop" }
    val opening = OfflineDemoTutor.buildOpeningTurn(
      scenario = coffeeScenario,
      character = TutorCharacter.MANU,
      level = LearnerLevel.NOVATO,
    )
    assertNotNull(opening.coachNoteEs)
    assertTrue(opening.coachNoteEs!!.isNotBlank())
  }

  @Test
  fun respondsToArbitraryOffScriptInputsWithoutRepeatingQuestions() {
    val history = mutableListOf(
      ChatMessage("assistant", "Hi! I am Manu. What can I get started for you today?"),
    )

    // 1. Direct question to tutor
    history.add(ChatMessage("user", "How are you doing today?"))
    val r1 = OfflineDemoTutor.generateReply(
      TutorCharacter.MANU, LearnerLevel.JUNIOR, "coffee_shop", EnglishAccent.US, history,
    )
    assertTrue("Expected tutor to answer how they are doing: ${r1.reply}", r1.reply.contains("wonderfully", ignoreCase = true))
    history.add(ChatMessage("assistant", r1.reply))

    // 2. Personal disclosure with name
    history.add(ChatMessage("user", "My name is Sofia."))
    val r2 = OfflineDemoTutor.generateReply(
      TutorCharacter.MANU, LearnerLevel.JUNIOR, "coffee_shop", EnglishAccent.US, history,
    )
    assertTrue("Expected tutor to use user's name: ${r2.reply}", r2.reply.contains("Sofia"))
    history.add(ChatMessage("assistant", r2.reply))

    // 3. Off-script price question in coffee shop
    history.add(ChatMessage("user", "How much does a green tea cost?"))
    val r3 = OfflineDemoTutor.generateReply(
      TutorCharacter.MANU, LearnerLevel.JUNIOR, "coffee_shop", EnglishAccent.US, history,
    )
    assertTrue("Expected price answer: ${r3.reply}", r3.reply.contains("dollars", ignoreCase = true))
    history.add(ChatMessage("assistant", r3.reply))

    // 4-8. Five completely varied arbitrary turns must never repeat each other
    val arbitraryInputs = listOf(
      "I enjoy playing chess in the park.",
      "My puppy loves sleeping beside my desk.",
      "Sometimes I read astronomy articles before sleep.",
      "Last month we hiked up a misty volcano.",
      "I prefer rainy afternoons with warm soup.",
    )
    val generatedReplies = mutableListOf<String>()
    for (msg in arbitraryInputs) {
      history.add(ChatMessage("user", msg))
      val reply = OfflineDemoTutor.generateReply(
        TutorCharacter.MANU, LearnerLevel.JUNIOR, "free_chat", EnglishAccent.US, history,
      )
      generatedReplies.add(reply.reply)
      history.add(ChatMessage("assistant", reply.reply))
    }

    assertEquals(
      "Consecutive arbitrary replies must all be distinct: $generatedReplies",
      generatedReplies.size,
      generatedReplies.toSet().size,
    )
  }

  @Test
  fun detectsCommonSpanishInterferenceCorrections() {
    val ageErr = OfflineDemoTutor.detectCorrection("Hello, I have 25 years.")
    assertNotNull(ageErr)
    assertTrue(ageErr!!.better.contains("25 years old"))

    val hungryErr = OfflineDemoTutor.detectCorrection("I have hungry right now.")
    assertNotNull(hungryErr)
    assertEquals("I am hungry.", hungryErr!!.better)

    val agreeErr = OfflineDemoTutor.detectCorrection("Yes, I am agree with that idea.")
    assertNotNull(agreeErr)
    assertEquals("I agree with you.", agreeErr!!.better)

    val modalErr = OfflineDemoTutor.detectCorrection("I can to speak English better.")
    assertNotNull(modalErr)
    assertTrue(modalErr!!.why.contains("modales"))

    val cleanMsg = OfflineDemoTutor.detectCorrection("I would like a medium iced latte, please.")
    assertNull(cleanMsg)
  }
}
