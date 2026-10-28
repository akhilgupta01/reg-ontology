package com.txr.assistant.ontology

/**
 * Which .ttl files make up the ontology, and where they live on the classpath.
 *
 * ALWAYS files are small composite "index" files (they just list which regime
 * modules exist via owl:imports) and are included in full on every question.
 * CHUNKED files are split into per-subject chunks and only the relevant ones
 * are sent to the LLM - see [RetrievalService].
 */
object OntologyFiles {

    val ALWAYS: List<String> = listOf(
        "ontology/reg-tx-report-ontology.ttl",
        "ontology/txr-shapes.ttl",
    )

    private val REGIMES: List<String> = listOf(
        "emir", "uk-emir", "cftc-part45", "cftc-part43", "sec-reg-sbsr",
        "mifir", "sftr", "mas", "asic", "jfsa",
    )

    val CHUNKED: List<String> = listOf(
        "ontology/txr-core.ttl",
        "ontology/txr-shapes-core.ttl",
    ) + REGIMES.map { "ontology/txr-$it.ttl" } + listOf(
        "ontology/txr-example.ttl",
    )

    /** Classpath resource path -> the label shown to the user/LLM (strips the "ontology/" prefix). */
    fun sourceLabel(classpathPath: String): String = classpathPath.removePrefix("ontology/")
}
