package com.example

import org.junit.Test
import org.junit.Assert.*
import com.example.game.*
import androidx.compose.ui.graphics.Color

class AppStartTest {
    @Test
    fun testStart() {
        val vm = GameViewModel()
        vm.startGame()
        val s = vm.state.value
        assertEquals(GamePhase.BIDDING, s.phase)
        println("START GAME SUCCEEDED")
        println("Trump suit color: ${Color(Suit.CLUBS.color)}")
    }
}
