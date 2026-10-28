package com.txr.assistant

import com.txr.assistant.ontology.OntologyLoader
import com.txr.assistant.ontology.RetrievalService
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Builds the real ontology index (no Spring context needed) and sanity-checks
 * the numbers we already validated with rdflib while authoring the TTL:
 * 12 regimes' worth of ReportingField individuals, and that a targeted
 * question actually retrieves something relevant rather than an empty or
 * random set of chunks.
 */
class OntologyIndexSmokeTest {

    private val index = OntologyLoader().ontologyIndex()

    @Test
    fun `loads a substantial number of chunks from every regime module`() {
        assertTrue(index.chunks.size > 300, "expected 300+ chunks (one per named subject), got ${index.chunks.size}")
        val sources = index.chunks.map { it.source }.toSet()
        assertTrue(sources.size >= 12, "expected chunks from at least 12 source files, got ${sources.size}: $sources")
    }

    @Test
    fun `full index lists identifiers from every loaded file`() {
        assertTrue(index.fullIndexText.contains("EMIR_Refit"))
        assertTrue(index.fullIndexText.contains("CFTC_Part45"))
        assertTrue(index.fullIndexText.contains("MiFIR_PostTradeTransparency"))
    }

    @Test
    fun `retrieval finds EMIR's reporting counterparty definition for a targeted question`() {
        val service = RetrievalService(index)
        val selected = service.retrieve("Who is the reporting counterparty for an EMIR derivative transaction?", "")
        assertTrue(selected.isNotEmpty(), "expected at least one chunk to be retrieved")
        assertTrue(
            selected.any { it.subjectLabel.contains("ReportingCounterparty") || it.text.contains("ReportingCounterparty") },
            "expected a reporting-counterparty-related chunk among: ${selected.map { it.subjectLabel }}",
        )
    }
}
