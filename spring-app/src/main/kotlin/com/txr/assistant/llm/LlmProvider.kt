package com.txr.assistant.llm

data class ChatMessage(val role: String, val content: String)

data class LlmModel(val value: String, val label: String)

class LlmNotConfiguredException(message: String) : RuntimeException(message)
class LlmCallException(message: String) : RuntimeException(message)

interface LlmProvider {
    val id: String
    val label: String
    val models: List<LlmModel>

    /** Whether this provider has an API key configured on the server. */
    fun isConfigured(): Boolean

    /** @throws LlmNotConfiguredException if [isConfigured] is false, [LlmCallException] on a failed API call. */
    fun ask(model: String, systemPrompt: String, history: List<ChatMessage>): String
}
