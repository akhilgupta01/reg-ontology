package com.txr.assistant.llm

import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient

@Component
class GeminiProvider(
    @Value("\${txr.llm.gemini.api-key:}") private val apiKey: String,
    private val objectMapper: ObjectMapper,
) : LlmProvider {

    override val id = "gemini"
    override val label = "Gemini"
    override val models = listOf(
        LlmModel("gemini-2.5-flash", "Gemini 2.5 Flash (default)"),
        LlmModel("gemini-2.5-pro", "Gemini 2.5 Pro (deepest)"),
        LlmModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite (fastest)"),
    )

    private val client = RestClient.create()

    override fun isConfigured() = apiKey.isNotBlank()

    override fun ask(model: String, systemPrompt: String, history: List<ChatMessage>): String {
        if (!isConfigured()) {
            throw LlmNotConfiguredException("Gemini API key is not configured on the server (set GEMINI_API_KEY).")
        }

        val contents = history.map { message ->
            mapOf(
                "role" to if (message.role == "assistant") "model" else "user",
                "parts" to listOf(mapOf("text" to message.content)),
            )
        }
        val requestBody = mapOf(
            "systemInstruction" to mapOf("parts" to listOf(mapOf("text" to systemPrompt))),
            "contents" to contents,
            // Gemini 2.5's internal "thinking" tokens count against this budget too,
            // so it needs real headroom beyond the visible answer length - a value
            // that was too low here previously caused the model to run out of budget
            // mid-"thought" and return nothing.
            "generationConfig" to mapOf("maxOutputTokens" to 8192),
        )

        val responseBody = try {
            client.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String::class.java) ?: "{}"
        } catch (ex: HttpStatusCodeException) {
            val message = extractErrorMessage(ex.responseBodyAsString) ?: ex.message
            throw LlmCallException("Gemini request failed (${ex.statusCode.value()}): $message")
        }

        val root = objectMapper.readTree(responseBody)
        val candidate = root.path("candidates").firstOrNull() ?: return "(no response)"
        val text = candidate.path("content").path("parts")
            .mapNotNull { it.path("text").asString(null) }
            .joinToString("\n")
            .trim()
        val finishReason = candidate.path("finishReason").asString(null)

        return when {
            text.isNotBlank() && finishReason == "MAX_TOKENS" ->
                text + "\n\n*(answer cut off — hit the output token limit; try a narrower question, or a different model)*"
            text.isNotBlank() -> text
            finishReason != null -> "(no text returned — finishReason: $finishReason)"
            else -> "(no response)"
        }
    }

    private fun extractErrorMessage(body: String): String? = try {
        objectMapper.readTree(body).path("error").path("message").asString(null)
    } catch (ex: Exception) {
        null
    }
}
