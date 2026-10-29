package com.txr.assistant.ontology

import org.springframework.stereotype.Service

/**
 * Picks which ontology chunks are worth sending to the LLM for a given
 * question, instead of sending the whole (now 300+ field, 12-regime)
 * ontology every time: score every chunk by IDF-weighted keyword overlap,
 * pull in anything the top chunks reference (one hop - e.g. a Report field
 * chunk pulls in the RegulatoryRegime/ActionType it points to), and stop
 * once the character budget is spent.
 */
@Service
class RetrievalService(private val index: OntologyIndex) {

    companion object {
        // Large enough to cover a question spanning every regime at once
        // (e.g. "the reporting deadline for each regime") while still being
        // a large reduction versus sending the whole ontology every time.
        const val MAX_CONTEXT_CHARS = 12_000
        const val MAX_EXPANSION_HOPS = 2
    }

    fun retrieve(latestQuestion: String, priorQuestions: String): List<Chunk> {
        val latestTokens = Tokenizer.tokenize(latestQuestion)
        val priorTokens = Tokenizer.tokenize(priorQuestions)

        val scored = index.chunks
            .map { chunk -> chunk to (2 * weightedOverlap(chunk.tokens, latestTokens) + weightedOverlap(chunk.tokens, priorTokens)) }
            .filter { (_, score) -> score > 0.0 }
            .sortedWith(compareByDescending<Pair<Chunk, Double>> { it.second }.thenBy { it.first.globalIndex })

        val selected = LinkedHashMap<Int, Chunk>()
        var totalChars = 0

        fun tryAdd(chunk: Chunk): Boolean {
            if (selected.containsKey(chunk.globalIndex)) return false
            if (totalChars + chunk.text.length > MAX_CONTEXT_CHARS) return false
            selected[chunk.globalIndex] = chunk
            totalChars += chunk.text.length
            return true
        }

        scored.forEach { (chunk, _) -> tryAdd(chunk) }

        // Fallback for a question with no keyword overlap at all: still send
        // something rather than an empty context.
        if (selected.isEmpty()) {
            for (chunk in index.chunks) {
                if (!tryAdd(chunk)) break
            }
        }

        for (hop in 0 until MAX_EXPANSION_HOPS) {
            val referencedUris = selected.values.flatMap { it.referencedUris }.toSet()
            var added = false
            referencedUris.forEach { uri ->
                index.idToChunk[uri]?.let { chunkIndex ->
                    if (tryAdd(index.chunks[chunkIndex])) added = true
                }
            }
            if (!added) break
        }

        return selected.values.sortedBy { it.globalIndex }
    }

    private fun weightedOverlap(chunkTokens: Set<String>, queryTokens: Set<String>): Double =
        queryTokens.sumOf { token -> if (token in chunkTokens) (index.idf[token] ?: 1.0) else 0.0 }
}
