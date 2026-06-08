package com.example

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import com.example.ui.GameScreen

@RunWith(RobolectricTestRunner::class)
class GameScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testScreen() {
        rule.setContent {
            GameScreen()
        }
        rule.waitForIdle()
    }
}
