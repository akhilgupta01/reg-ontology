package com.txr.assistant.ontology

/** Everything built once at startup from the loaded RDF, used to answer every question. */
class OntologyIndex(
    val chunks: List<Chunk>,
    /** Subject URI -> the chunk that defines it (first file that declares it wins). */
    val idToChunk: Map<String, Int>,
    /** Token -> inverse-document-frequency weight, see [Tokenizer]. */
    val idf: Map<String, Double>,
    /** The small composite index files, always sent in full. */
    val alwaysText: String,
    /** Every named subject across all loaded files, by name only - cheap always-on context. */
    val fullIndexText: String,
    /** Raw original text of every loaded file, keyed by its source label - for the UI preview. */
    val rawSources: Map<String, String>,
    /** Sum of every loaded file's character length, for the "% of full ontology" figure shown to the user. */
    val totalFullChars: Int,
)
