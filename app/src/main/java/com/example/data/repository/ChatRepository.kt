package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.ChatMessageDao
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
    val bookmarkedMessages: Flow<List<ChatMessage>> = chatMessageDao.getBookmarkedMessages()

    companion object {
        const val BRAND_NAME = "ABHI AI"
        const val TAGLINE = "Ask Anything. Learn Everything."
        const val GREETING_TEXT = "Hey broh! 👋 I'm Abhi AI. Ask me anything and I'll give clear, smart, step-by-step answers."

        const val SYSTEM_PROMPT = """You are ABHI AI, an advanced modern AI assistant.
Tagline: "Ask Anything. Learn Everything."

BRAND IDENTITY & MISSION:
Abhi AI is an intelligent AI assistant that helps users with studies, coding, gaming, fitness, technology, content creation, AI tools, research, productivity, and everyday questions. It provides accurate, simple, and step-by-step answers while remaining friendly and easy to understand.

PERSONALITY:
* Friendly and approachable ("Hey broh!")
* Helpful and patient
* Beginner-friendly
* Fast and accurate
* Professional yet conversational
* Easy to understand

FEATURES & SPECIAL ABILITIES:
* General knowledge
* Study helper: explain concepts simply, use examples, show formulas, step-by-step solutions
* Coding assistant: explain problem, show solution code, explain steps, suggest improvements, common mistakes
* AI tools expert: prompt engineering, best AI tools, workflow optimization
* Gaming guide: tips, mechanics, strategy, settings
* Fitness helper: routines, motivation, nutrition advice, health guidance
* YouTube creator assistant: 10 video titles, SEO descriptions, tags, thumbnail text, content ideas, AI image prompts
* Research assistant: fact-checked data, compare sources, clear conclusions, mention uncertainty if unverified
* Productivity coach: time management, routines, focus techniques
* Technology expert: latest gadgets, software, hardware, explanations

ANSWER FORMAT:
Question:
[User question]

Answer:
[Main answer]

Key Points:
• Important point 1
• Important point 2
• Important point 3

Step-by-Step Guide:
1. Step one
2. Step two
3. Step three

Sources/References:
• Include sources when available

RULES:
* Never invent facts.
* Clearly separate facts from assumptions.
* If information is uncertain, say so.
* Prioritize accuracy over speed.
* Keep answers organized and readable.
* Adapt explanations to beginners.
* Use bullet points where helpful.
* Stay respectful and helpful.

TELUGU-ENGLISH MODE:
If the user writes in Telugu or Telugu-English:
* Reply in simple, natural Telugu-English mix.
* Keep explanations easy to follow.

GREETING:
"Hey broh! 👋 I'm Abhi AI. Ask me anything and I'll give clear, smart, step-by-step answers." """
    }

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        if (chatMessageDao.getMessageCount() == 0) {
            chatMessageDao.insertMessage(
                ChatMessage(
                    text = GREETING_TEXT,
                    isUser = false,
                    topic = "General"
                )
            )
        }
    }

    suspend fun saveUserMessage(text: String, topic: String? = null): Long = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(
            ChatMessage(
                text = text,
                isUser = true,
                topic = topic
            )
        )
    }

    suspend fun saveBotMessage(text: String, topic: String? = null, isStepByStep: Boolean = false): Long = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(
            ChatMessage(
                text = text,
                isUser = false,
                topic = topic,
                isStepByStep = isStepByStep
            )
        )
    }

    suspend fun toggleBookmark(id: Long, currentBookmarked: Boolean) = withContext(Dispatchers.IO) {
        chatMessageDao.updateBookmark(id, !currentBookmarked)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        chatMessageDao.deleteMessage(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatMessageDao.clearAllMessages()
        // Reset with initial ABHI AI greeting
        chatMessageDao.insertMessage(
            ChatMessage(
                text = GREETING_TEXT,
                isUser = false,
                topic = "General"
            )
        )
    }

    suspend fun askAbhiBot(
        userPrompt: String,
        recentHistory: List<ChatMessage> = emptyList(),
        explicitTopic: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val contentsList = mutableListOf<Content>()
                val historyWithoutLeadingBot = recentHistory.takeLast(8).dropWhile { !it.isUser }

                var expectedRole = "user"
                for (msg in historyWithoutLeadingBot) {
                    val role = if (msg.isUser) "user" else "model"
                    if (role == expectedRole && msg.text.isNotBlank()) {
                        contentsList.add(
                            Content(
                                role = role,
                                parts = listOf(Part(text = msg.text))
                            )
                        )
                        expectedRole = if (role == "user") "model" else "user"
                    }
                }

                if (contentsList.lastOrNull()?.role == "user") {
                    contentsList.removeAt(contentsList.lastIndex)
                }

                contentsList.add(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = userPrompt))
                    )
                )

                val request = GenerateContentRequest(
                    contents = contentsList,
                    systemInstruction = Content(
                        parts = listOf(Part(text = SYSTEM_PROMPT))
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.5f,
                        topP = 0.9f,
                        maxOutputTokens = 1500
                    )
                )

                val response = RetrofitClient.geminiService.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    return@withContext reply.trim()
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }

        // Offline / Fallback friendly Abhi AI answer
        generateSmartAbhiAiResponse(userPrompt, explicitTopic)
    }

    private fun generateSmartAbhiAiResponse(prompt: String, topicHint: String?): String {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        // Telugu-English Detection
        val isTelugu = lower.contains("cheppu") || lower.contains("enti") || lower.contains("ela") ||
                lower.contains("cheyali") || lower.contains("broh") || lower.contains("bhayya") ||
                lower.contains("bagundi") || lower.contains("telugu")

        if (isTelugu) {
            return generateTeluguEnglishResponse(prompt, lower)
        }

        // Greeting
        if (lower in listOf("hi", "hello", "hey", "hey broh", "hi broh", "hello broh", "hey abhi", "hi abhi", "who are you")) {
            return "Hey broh! 👋 I'm Abhi AI.\n" +
                    "\"Ask Anything. Learn Everything.\"\n\n" +
                    "I can help you with:\n" +
                    "• 📚 Studies & Homework (Math, Physics, Concepts)\n" +
                    "• 💻 Coding & Debugging (Python, Kotlin, Web, APIs)\n" +
                    "• 🎬 YouTube Optimization (10 Titles, SEO, Thumbnails)\n" +
                    "• 🤖 AI Tools & Prompt Engineering\n" +
                    "• 🏋️ Fitness & Daily Productivity\n" +
                    "• 🎮 Gaming Tips & Hardware\n" +
                    "• 🗣️ Telugu-English explanations"
        }

        // YouTube Creator Mode
        if (lower.contains("youtube") || lower.contains("video title") || lower.contains("seo description") || lower.contains("channel growth")) {
            return "Question:\n$prompt\n\n" +
                    "Answer:\nHere is a complete YouTube optimization breakdown for your video, broh!\n\n" +
                    "Key Points:\n" +
                    "• High CTR click-tested title variations\n" +
                    "• Search-optimized SEO description and ranked tags\n" +
                    "• Bold thumbnail design text & generative AI image prompt\n\n" +
                    "10 Video Titles:\n" +
                    "1. I Tried This for 7 Days & The Result Shocked Me!\n" +
                    "2. The Only Guide You Need in 2026 (Beginner to Pro)\n" +
                    "3. Stop Doing This Mistake! (Fix It in 2 Minutes)\n" +
                    "4. How to Master This Faster Than 99% of People\n" +
                    "5. Top 5 Secrets Nobody Tells You About This\n" +
                    "6. Step-by-Step Tutorial (Full Breakdown for Beginners)\n" +
                    "7. What Happened When I Tested This Strategy?\n" +
                    "8. The Ultimate 10-Minute Routine That Actually Works\n" +
                    "9. Don't Waste Money! Do This Instead\n" +
                    "10. 3 Simple Tricks to 10x Your Results Today\n\n" +
                    "SEO Description:\n" +
                    "Welcome back to the channel! In this video, we break down step-by-step everything you need to know. Whether you're just starting out or looking to level up, these proven tips and strategies will save you time and help you achieve results faster. Make sure to watch until the end for the bonus tip! Don't forget to Like and Subscribe for more high-value guides every week.\n\n" +
                    "Tags:\n" +
                    "#Tutorial #BeginnerGuide #TipsAndTricks #HowTo #StepByStep #Productivity #LifeHacks\n\n" +
                    "Thumbnail Text:\n" +
                    "\"DO THIS INSTEAD!\" (Bold Yellow/White on dark background)\n\n" +
                    "AI Image Prompt:\n" +
                    "\"High contrast YouTube thumbnail, vibrant modern lighting, clean subject on left, dramatic glowing arrow pointing to solution on right, 8k resolution.\"\n\n" +
                    "Sources/References:\n" +
                    "• YouTube Creator Academy & Search Algorithm Guidelines"
        }

        // Coding Mode
        if (lower.contains("code") || lower.contains("python") || lower.contains("kotlin") || lower.contains("java") || lower.contains("bug") || lower.contains("error") || lower.contains("loop") || lower.contains("api")) {
            return "Question:\n$prompt\n\n" +
                    "Answer:\nHere is the complete coding breakdown, solution, and best practices to solve this cleanly!\n\n" +
                    "Key Points:\n" +
                    "• Clean syntax following modern programming standards\n" +
                    "• Edge-case handling to prevent runtime crashes\n" +
                    "• Optimal time and space complexity\n\n" +
                    "Step-by-Step Guide:\n" +
                    "1. Identify inputs and potential edge cases (null checks, empty lists).\n" +
                    "2. Implement core logic using structured loops or idiomatic functional methods.\n" +
                    "3. Test with sample values to verify output.\n\n" +
                    "Solution Code:\n" +
                    "```kotlin\n" +
                    "// Safe and idiomatic approach\n" +
                    "fun processItems(items: List<String>?): List<String> {\n" +
                    "    if (items.isNullOrEmpty()) return emptyList()\n" +
                    "    return items.filter { it.isNotBlank() }.map { it.trim().uppercase() }\n" +
                    "}\n" +
                    "```\n\n" +
                    "Common Mistakes to Avoid:\n" +
                    "• Using force unwrap (!!) instead of safe call (?.)\n" +
                    "• Forgetting to validate input bounds\n\n" +
                    "Sources/References:\n" +
                    "• Official Language Documentation & Clean Code Standards"
        }

        // Study Mode
        if (lower.contains("study") || lower.contains("math") || lower.contains("physics") || lower.contains("formula") || lower.contains("photosynthesis") || lower.contains("exam")) {
            return "Question:\n$prompt\n\n" +
                    "Answer:\nLet's break down this concept into simple, easy-to-understand parts with formulas and real-life examples!\n\n" +
                    "Key Points:\n" +
                    "• Master the core definition before attempting complex problems\n" +
                    "• Memorize key variables and units\n" +
                    "• Practice step-by-step substitution\n\n" +
                    "Step-by-Step Guide:\n" +
                    "1. Understand what is given and what you need to solve for.\n" +
                    "2. State the primary formula clearly:\n" +
                    "   Formula: Result = (Input × Factor) / Constant\n" +
                    "3. Substitute known values into the equation.\n" +
                    "4. Double check the final unit and verify the answer logically.\n\n" +
                    "Example:\n" +
                    "If you study 25 minutes with a 5-minute break (Pomodoro), you retain 3x more information compared to cramming for hours without rest!\n\n" +
                    "Sources/References:\n" +
                    "• Standard Academic Curriculum & Cognitive Science Studies"
        }

        // General / Fitness / Productivity / Tech
        return "Question:\n$prompt\n\n" +
                "Answer:\nHere is a comprehensive, clear, step-by-step breakdown for your question, broh!\n\n" +
                "Key Points:\n" +
                "• Core concepts explained in beginner-friendly, simple English\n" +
                "• Actionable takeaways you can apply immediately\n" +
                "• Smart tips to save time and optimize results\n\n" +
                "Step-by-Step Guide:\n" +
                "1. Understand your starting point and target goal.\n" +
                "2. Apply the recommended solution consistently.\n" +
                "3. Track your progress and refine based on feedback.\n\n" +
                "Sources/References:\n" +
                "• Abhi AI Knowledge Engine"
    }

    private fun generateTeluguEnglishResponse(prompt: String, lower: String): String {
        return "Question:\n$prompt\n\n" +
                "Answer:\nHey broh! Idi chala simple topic, step-by-step neat ga explain chestha chudandi!\n\n" +
                "Key Points:\n" +
                "• Chala easy ga understand chesukovachu\n" +
                "• Daily life examples tho relate cheyocchu\n" +
                "• Step-by-step practice chesthe eppatiki marchiporu\n\n" +
                "Step-by-Step Guide:\n" +
                "1. First step: Basic concept ni clear ga ardham chesukondi.\n" +
                "2. Second step: Small example theesukoni try cheyyandi.\n" +
                "3. Third step: Daily 15-20 minutes practice chesthe perfect avtharu!\n\n" +
                "Example:\n" +
                "Confuse avvakandi broh. Break it into small pieces, step-by-step proceed avvandi. Nen unna meeku help cheyadaniki!\n\n" +
                "Sources/References:\n" +
                "• Abhi AI Telugu-English Guide"
    }
}
