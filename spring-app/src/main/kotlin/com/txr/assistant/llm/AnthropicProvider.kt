package com.txr.assistant.llm

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient

@Component
class AnthropicProvider(
    @Value("\${txr.llm.anthropic.api-key:}") private val apiKey: String,
    private val objectMapper: ObjectMapper,
) : LlmProvider {

    override val id = "anthropic"
    override val label = "Anthropic"
    override val models = listOf(
        LlmModel("claude-sonnet-5-5", "Sonnet 5.5 (default)"),
        LlmModel("claude-haiku-5-5", "Haiku 5.5 (faster)"),
        LlmModel("claude-opus-5-5", "Opus 5.5 (deepest)"),
    )

    private val client = RestClient.create()

    override fun isConfigured() = apiKey.isNotBlank()

    override fun ask(model: String, systemPrompt: String, history: List<ChatMessage>): String {
        if (!isConfigured()) {
            throw LlmNotConfiguredException("Anthropic API key is not configured on the server (set ANTHROPIC_API_KEY).")
        }

        val requestBody = mapOf(
            "model" to model,
            "max_tokens" to 2500,
            "system" to systemPrompt,
            "messages" to history.map { mapOf("role" to it.role, "content" to it.content) },
        )

        val responseBody = try {
            client.post()
                .uri("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String::class.java) ?: "{}"
        } catch (ex: HttpStatusCodeException) {
            val message = extractErrorMessage(ex.responseBodyAsString) ?: ex.message
            throw LlmCallException("Anthropic request failed (${ex.statusCode.value()}): $message")
        }

        val root = objectMapper.readTree(responseBody)
        val text = root.path("content")
            .filter { it.path("type").asText() == "text" }
            .joinToString("\n") { it.path("text").asText("") }
            .trim()

        return text.ifBlank { "(no response)" }
    }

    private fun extractErrorMessage(body: String): String? = try {
        objectMapper.readTree(body).path("error").path("message").asText(null)
    } catch (ex: Exception) {
        null
    }
}
