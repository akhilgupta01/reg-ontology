package com.txr.assistant.llm

import org.springframework.stereotype.Component

@Component
class LlmProviderRegistry(providers: List<LlmProvider>) {
    private val byId = providers.associateBy { it.id }

    fun find(id: String): LlmProvider? = byId[id]

    fun all(): List<LlmProvider> = byId.values.toList()
}
