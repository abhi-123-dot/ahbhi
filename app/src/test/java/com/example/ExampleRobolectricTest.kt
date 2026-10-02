package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AbhiBotDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Abhi AI", appName)
    }

    @Test
    fun `verify Abhi AI greeting match`() {
        assertEquals("Hey broh! 👋 I'm Abhi AI. Ask me anything and I'll give clear, smart, step-by-step answers.", ChatRepository.GREETING_TEXT)
    }

    @Test
    fun `test Room database persists user and bot messages`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AbhiBotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.chatMessageDao()

        // 1. Insert user message
        val userMsgId = dao.insertMessage(
            ChatMessage(
                text = "Explain loops in Python",
                isUser = true,
                topic = "Coding"
            )
        )

        // 2. Insert bot response
        val botMsgId = dao.insertMessage(
            ChatMessage(
                text = "A loop repeats code automatically!",
                isUser = false,
                topic = "Coding"
            )
        )

        // 3. Verify messages are persisted
        val messages = dao.getAllMessages().first()
        assertEquals(2, messages.size)
        assertEquals("Explain loops in Python", messages[0].text)
        assertTrue(messages[0].isUser)
        assertEquals("A loop repeats code automatically!", messages[1].text)
        assertFalse(messages[1].isUser)

        // 4. Test bookmark persistence
        dao.updateBookmark(botMsgId, true)
        val bookmarked = dao.getBookmarkedMessages().first()
        assertEquals(1, bookmarked.size)
        assertEquals(botMsgId, bookmarked[0].id)
        assertTrue(bookmarked[0].isBookmarked)

        db.close()
    }
}
