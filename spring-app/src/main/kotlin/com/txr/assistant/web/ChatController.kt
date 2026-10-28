package com.txr.assistant.web

import com.txr.assistant.llm.ChatMessage
import com.txr.assistant.llm.LlmCallException
import com.txr.assistant.llm.LlmNotConfiguredException
import com.txr.assistant.llm.LlmProviderRegistry
import com.txr.assistant.ontology.OntologyIndex
import com.txr.assistant.ontology.PromptBuilder
import com.txr.assistant.ontology.RetrievalService
import com.txr.assistant.web.dto.AskRequest
import com.txr.assistant.web.dto.AskResponse
import com.txr.assistant.web.dto.ContextExcerptDto
import com.txr.assistant.web.dto.ErrorResponse
import com.txr.assistant.web.dto.ModelInfoDto
import com.txr.assistant.web.dto.OntologyPreviewResponse
import com.txr.assistant.web.dto.OntologySourceDto
import com.txr.assistant.web.dto.ProviderInfoDto
import com.txr.assistant.web.dto.ProvidersResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ChatController(
    private val retrievalService: RetrievalService,
    private val promptBuilder: PromptBuilder,
    private val providerRegistry: LlmProviderRegistry,
    private val ontologyIndex: OntologyIndex,
) {

    @GetMapping("/providers")
    fun providers(): ProvidersResponse = ProvidersResponse(
        providerRegistry.all().map { provider ->
            ProviderInfoDto(
                id = provider.id,
                label = provider.label,
                models = provider.models.map { ModelInfoDto(it.value, it.label) },
                configured = provider.isConfigured(),
            )
        },
    )

    @GetMapping("/ontology/preview")
    fun ontologyPreview(): OntologyPreviewResponse = OntologyPreviewResponse(
        ontologyIndex.rawSources.entries
            .sortedBy { it.key }
            .map { (path, text) -> OntologySourceDto(path, text) },
    )

    @PostMapping("/ask")
    fun ask(@RequestBody request: AskRequest): ResponseEntity<Any> {
        if (request.question.isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse("question must not be blank"))
        }

        val provider = providerRegistry.find(request.provider)
            ?: return ResponseEntity.badRequest().body(ErrorResponse("Unknown provider: ${request.provider}"))

        val priorQuestions = request.history
            .filter { it.role == "user" }
            .joinToString("\n") { it.content }

        val selectedChunks = retrievalService.retrieve(request.question, priorQuestions)
        val systemPrompt = promptBuilder.build(selectedChunks)

        val history = request.history.map { ChatMessage(it.role, it.content) } +
            ChatMessage("user", request.question)

        return try {
            val answer = provider.ask(request.model, systemPrompt, history)
            ResponseEntity.ok(
                AskResponse(
                    answer = answer,
                    contextExcerpts = selectedChunks.map { ContextExcerptDto(it.source, it.subjectLabel) },
                    contextChars = selectedChunks.sumOf { it.text.length },
                    totalChars = ontologyIndex.totalFullChars,
                ),
            )
        } catch (ex: LlmNotConfiguredException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse(ex.message ?: "Provider not configured"))
        } catch (ex: LlmCallException) {
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(ex.message ?: "LLM call failed"))
        }
    }
}
