package com.txr.assistant.web.dto

data class ChatMessageDto(val role: String, val content: String)

data class AskRequest(
    val provider: String,
    val model: String,
    val question: String,
    val history: List<ChatMessageDto> = emptyList(),
)

data class ContextExcerptDto(val source: String, val subject: String)

data class AskResponse(
    val answer: String,
    val contextExcerpts: List<ContextExcerptDto>,
    val contextChars: Int,
    val totalChars: Int,
)

data class ModelInfoDto(val value: String, val label: String)

data class ProviderInfoDto(
    val id: String,
    val label: String,
    val models: List<ModelInfoDto>,
    val configured: Boolean,
)

data class ProvidersResponse(val providers: List<ProviderInfoDto>)

data class OntologySourceDto(val path: String, val text: String)

data class OntologyPreviewResponse(val sources: List<OntologySourceDto>)

data class ErrorResponse(val error: String)
