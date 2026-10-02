package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AbhiBotDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedCategory: String = "All",
    val speakingMessageId: Long? = null,
    val showBookmarkDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val searchQuery: String = "",
    val isTtsReady: Boolean = false,
    val isDarkMode: Boolean = true,
    val fontSizeScale: Float = 1.0f, // 0.85f (Small), 1.0f (Medium), 1.2f (Large)
    val accentTheme: String = "Neon Cyber" // Neon Cyber, Electric Blue, Violet Glow
)

class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: ChatRepository
    private var textToSpeech: TextToSpeech? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val database = AbhiBotDatabase.getDatabase(application)
        repository = ChatRepository(database.chatMessageDao())

        // Initialize TTS
        try {
            textToSpeech = TextToSpeech(application, this)
        } catch (e: Exception) {
            // Ignore if TTS not supported
        }

        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
    }

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedMessages: StateFlow<List<ChatMessage>> = repository.bookmarkedMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recent queries for the ChatGPT-style sidebar
    val recentChatTitles: StateFlow<List<ChatMessage>> = repository.allMessages
        .map { list ->
            list.filter { it.isUser }.reversed().distinctBy { it.text.trim().take(40) }.take(10)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            _uiState.value = _uiState.value.copy(isTtsReady = true)
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setShowBookmarkDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showBookmarkDialog = show)
    }

    fun setShowAboutDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAboutDialog = show)
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    fun toggleDarkMode() {
        _uiState.value = _uiState.value.copy(isDarkMode = !_uiState.value.isDarkMode)
    }

    fun setFontSizeScale(scale: Float) {
        _uiState.value = _uiState.value.copy(fontSizeScale = scale)
    }

    fun setAccentTheme(theme: String) {
        _uiState.value = _uiState.value.copy(accentTheme = theme)
    }

    fun startNewChat() {
        stopSpeech()
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.value = _uiState.value.copy(
                inputText = "",
                searchQuery = "",
                selectedCategory = "All"
            )
        }
    }

    fun exportChat(context: Context) {
        val messages = allMessages.value
        if (messages.isEmpty()) {
            Toast.makeText(context, "No chat history to export", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("         ABHI AI CHAT TRANSCRIPT         \n")
        sb.append("  Ask Anything. Learn Everything.        \n")
        sb.append("  Exported on: ${dateFormat.format(Date())}\n")
        sb.append("=========================================\n\n")

        for (msg in messages) {
            val sender = if (msg.isUser) "USER" else "ABHI AI"
            val time = dateFormat.format(Date(msg.timestamp))
            sb.append("[$time] $sender:\n")
            sb.append("${msg.text}\n\n")
        }

        sb.append("=========================================\n")
        sb.append("Powered by ABHI AI Knowledge Engine\n")

        val transcript = sb.toString()

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ABHI AI Chat Transcript")
                putExtra(Intent.EXTRA_TEXT, transcript)
            }
            val chooser = Intent.createChooser(shareIntent, "Export ABHI AI Chat")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ABHI AI Chat", transcript))
            Toast.makeText(context, "Copied transcript to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    fun sendMessage(overrideText: String? = null, topic: String? = null) {
        val textToSend = (overrideText ?: _uiState.value.inputText).trim()
        if (textToSend.isBlank() || _uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            inputText = "",
            isLoading = true
        )

        viewModelScope.launch {
            // 1. Save user message to database
            repository.saveUserMessage(textToSend, topic)

            // 2. Fetch answer from Abhi AI
            val currentHistory = allMessages.value
            val botAnswer = repository.askAbhiBot(
                userPrompt = textToSend,
                recentHistory = currentHistory,
                explicitTopic = topic
            )

            // 3. Save bot response to database
            repository.saveBotMessage(
                text = botAnswer,
                topic = topic,
                isStepByStep = textToSend.lowercase(Locale.ROOT).contains("step by step")
            )

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun sendQuickPrompt(promptText: String, topic: String) {
        sendMessage(overrideText = promptText, topic = topic)
    }

    fun toggleBookmark(message: ChatMessage) {
        viewModelScope.launch {
            repository.toggleBookmark(message.id, message.isBookmarked)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearChat() {
        stopSpeech()
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun speakMessage(message: ChatMessage) {
        if (_uiState.value.speakingMessageId == message.id) {
            stopSpeech()
        } else {
            stopSpeech()
            textToSpeech?.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, "AbhiAiTTS_${message.id}")
            _uiState.value = _uiState.value.copy(speakingMessageId = message.id)
        }
    }

    fun stopSpeech() {
        textToSpeech?.stop()
        _uiState.value = _uiState.value.copy(speakingMessageId = null)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
