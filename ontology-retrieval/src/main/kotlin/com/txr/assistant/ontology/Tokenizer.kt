package com.txr.assistant.ontology

/**
 * Plain keyword-overlap scoring breaks down on RDF boilerplate: every
 * ReportingField individual's "a txr:ReportingField" / "txr:fieldOfRegime"
 * tokenizes to common words like "reporting"/"regime"/"field", so those
 * words match almost every field chunk with equal weight. IDF weighting
 * (see [OntologyIndex.idf]) fixes this - a token confined to a few chunks
 * (e.g. "deadline") should dominate the score over one that appears in
 * nearly all of them, same idea as TF-IDF search ranking.
 */
object Tokenizer {

    private val STOPWORDS: Set<String> = (
        "a an the is are was were be been being do does did doing have has had " +
            "having will would shall should may might must can could to of in on for with and or but if not no " +
            "yes about between than then from into over after before during per also it its their there these " +
            "those this that what which who whom how when where why am i you he she they we us our your my " +
            "me him her them as at by so such very just only all any each more most other some such nor " +
            "under against while out up down again further once here s t"
        ).split(Regex("\\s+")).toSet()

    fun splitIdentifier(localName: String): String =
        localName.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')

    fun tokenize(text: String): Set<String> =
        text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
}
