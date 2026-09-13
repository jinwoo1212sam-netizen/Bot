package com.ultron.assistant

import com.ultron.assistant.command.LocalCommandParser
import com.ultron.assistant.command.ParsedCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalCommandParserTest {

    private lateinit var parser: LocalCommandParser

    @Before
    fun setUp() {
        parser = LocalCommandParser()
    }

    @Test
    fun testYouTubeSearchArbitraryQuery() {
        // "youtube par kesariya search karo"
        val res1 = parser.parse("youtube par kesariya search karo")
        assertTrue(res1 is ParsedCommand.Single)
        val single1 = res1 as ParsedCommand.Single
        assertEquals("search_youtube", single1.toolCall.type)
        assertEquals("kesariya", single1.toolCall.parameters["query"])

        // "kesariya chalao"
        val res2 = parser.parse("kesariya chalao")
        assertTrue(res2 is ParsedCommand.Single)
        val single2 = res2 as ParsedCommand.Single
        assertEquals("search_youtube", single2.toolCall.type)
        assertEquals("kesariya", single2.toolCall.parameters["query"])

        // "youtube par arijit singh ka song search karo"
        val res3 = parser.parse("youtube par arijit singh ka song search karo")
        assertTrue(res3 is ParsedCommand.Single)
        val single3 = res3 as ParsedCommand.Single
        assertEquals("search_youtube", single3.toolCall.type)
        assertEquals("arijit singh ka song", single3.toolCall.parameters["query"])
    }

    @Test
    fun testAppLaunchCommands() {
        // "youtube kholo"
        val res1 = parser.parse("youtube kholo")
        assertTrue(res1 is ParsedCommand.Single)
        val single1 = res1 as ParsedCommand.Single
        assertEquals("open_app", single1.toolCall.type)
        assertEquals("youtube", single1.toolCall.parameters["app"])

        // "instagram kholo"
        val res2 = parser.parse("instagram kholo")
        assertTrue(res2 is ParsedCommand.Single)
        val single2 = res2 as ParsedCommand.Single
        assertEquals("open_app", single2.toolCall.type)
        assertEquals("instagram", single2.toolCall.parameters["app"])
    }

    @Test
    fun testVolumeCommands() {
        // "volume 50 percent karo"
        val res1 = parser.parse("volume 50 percent karo")
        assertTrue(res1 is ParsedCommand.Single)
        val single1 = res1 as ParsedCommand.Single
        assertEquals("set_volume", single1.toolCall.type)
        assertEquals("50", single1.toolCall.parameters["value"])

        // "volume badhao"
        val res2 = parser.parse("volume badhao")
        assertTrue(res2 is ParsedCommand.Single)
        val single2 = res2 as ParsedCommand.Single
        assertEquals("set_volume", single2.toolCall.type)
        assertEquals("increase", single2.toolCall.parameters["mode"])

        // "volume full karo"
        val res3 = parser.parse("volume full karo")
        assertTrue(res3 is ParsedCommand.Single)
        val single3 = res3 as ParsedCommand.Single
        assertEquals("set_volume", single3.toolCall.type)
        assertEquals("full", single3.toolCall.parameters["mode"])
    }

    @Test
    fun testCallingCommands() {
        // "mummy ko call karo"
        val res1 = parser.parse("mummy ko call karo")
        assertTrue(res1 is ParsedCommand.Single)
        val single1 = res1 as ParsedCommand.Single
        assertEquals("call_contact", single1.toolCall.type)
        assertEquals("mummy", single1.toolCall.parameters["contact"])

        // "rahul ko call karo"
        val res2 = parser.parse("rahul ko call karo")
        assertTrue(res2 is ParsedCommand.Single)
        val single2 = res2 as ParsedCommand.Single
        assertEquals("call_contact", single2.toolCall.type)
        assertEquals("rahul", single2.toolCall.parameters["contact"])
    }

    @Test
    fun testTimerCommands() {
        // "10 minute ka timer lagao"
        val res1 = parser.parse("10 minute ka timer lagao")
        assertTrue(res1 is ParsedCommand.Single)
        val single1 = res1 as ParsedCommand.Single
        assertEquals("set_timer", single1.toolCall.type)
        assertEquals("600", single1.toolCall.parameters["seconds"])
    }

    @Test
    fun testMultiActionCommand() {
        // "YouTube kholo, Kesariya search karo aur volume 50 percent karo"
        val raw = "YouTube kholo, Kesariya search karo aur volume 50 percent karo"
        val res = parser.parse(raw)
        assertTrue(res is ParsedCommand.Multi)
        val multi = res as ParsedCommand.Multi
        assertEquals(3, multi.commands.size)
        assertEquals("open_app", multi.commands[0].toolCall.type)
        assertEquals("search_youtube", multi.commands[1].toolCall.type)
        assertEquals("set_volume", multi.commands[2].toolCall.type)
    }

    @Test
    fun testPhoneOnSecurityExplanation() {
        val res = parser.parse("Ultron phone on kar de")
        assertTrue(res is ParsedCommand.SpecialResponse)
        val resp = res as ParsedCommand.SpecialResponse
        assertTrue(resp.responseText.contains("power off"))
    }
}
