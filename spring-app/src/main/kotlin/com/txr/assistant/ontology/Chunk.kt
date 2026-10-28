package com.txr.assistant.ontology

/**
 * All RDF statements sharing one subject, from one source file, rendered as
 * compact Turtle-ish text for an LLM prompt. One subject = one chunk: this
 * is a cleaner, more principled unit than splitting files by blank lines
 * (the original client-side prototype's approach), since it follows the
 * actual RDF graph structure instead of incidental file formatting.
 */
data class Chunk(
    val globalIndex: Int,
    val source: String,
    val subjectLabel: String,
    val subjectUri: String?,
    val text: String,
    val referencedUris: Set<String>,
    val tokens: Set<String>,
)
