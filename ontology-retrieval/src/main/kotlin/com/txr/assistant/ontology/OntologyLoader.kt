package com.txr.assistant.ontology

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import kotlin.math.ln

/**
 * Loads every ontology .ttl file into an Apache Jena [Model] at startup and
 * builds the [OntologyIndex] the rest of the app queries. Each regime module
 * is parsed into its own Model (not merged into one big graph) so a chunk's
 * source file stays unambiguous - the equivalent of the old client-side
 * app's per-file fetch, now done with Jena's RIOT reader instead of raw text.
 */
@Configuration
class OntologyLoader {

    @Bean
    fun ontologyIndex(): OntologyIndex {
        val rawSources = LinkedHashMap<String, String>()

        val alwaysText = OntologyFiles.ALWAYS.joinToString("\n\n") { path ->
            val text = readResourceText(path)
            rawSources[OntologyFiles.sourceLabel(path)] = text
            "--- ${OntologyFiles.sourceLabel(path)} ---\n$text"
        }

        val chunkedModels = LinkedHashMap<String, Model>()
        OntologyFiles.CHUNKED.forEach { path ->
            val model = loadModel(path)
            val label = OntologyFiles.sourceLabel(path)
            chunkedModels[label] = model
            rawSources[label] = readResourceText(path)
        }

        val chunks = mutableListOf<Chunk>()
        chunkedModels.forEach { (label, model) ->
            chunks += buildChunksForModel(model, label, chunks.size)
        }

        val idToChunk = HashMap<String, Int>()
        chunks.forEach { chunk ->
            chunk.subjectUri?.let { uri -> idToChunk.putIfAbsent(uri, chunk.globalIndex) }
        }

        val fullIndexText = buildFullIndex(chunkedModels)
        val totalFullChars = rawSources.values.sumOf { it.length }

        return OntologyIndex(
            chunks = chunks,
            idToChunk = idToChunk,
            idf = buildIdf(chunks),
            alwaysText = alwaysText,
            fullIndexText = fullIndexText,
            rawSources = rawSources,
            totalFullChars = totalFullChars,
        )
    }

    private fun readResourceText(path: String): String =
        ClassPathResource(path).inputStream.bufferedReader().use { it.readText() }.trim()

    private fun loadModel(path: String): Model {
        val model = ModelFactory.createDefaultModel()
        ClassPathResource(path).inputStream.use { input ->
            RDFDataMgr.read(model, input, null, Lang.TURTLE)
        }
        return model
    }

    /** One chunk per named (non-blank-node) subject: all its triples, rendered as compact Turtle. */
    private fun buildChunksForModel(model: Model, source: String, startIndex: Int): List<Chunk> {
        val subjects = model.listSubjects().toList()
            .filter { it.isURIResource }
            .sortedBy { it.uri }

        var idx = startIndex
        val result = mutableListOf<Chunk>()
        for (subject in subjects) {
            val stmts = model.listStatements(subject, null as Property?, null as RDFNode?).toList()
            if (stmts.isEmpty()) continue

            val subjectLabel = model.shortForm(subject.uri)
            val lines = stmts.mapIndexed { i, stmt ->
                val pred = model.shortForm(stmt.predicate.uri)
                val obj = formatObject(model, stmt.getObject())
                val terminator = if (i == stmts.size - 1) " ." else " ;"
                "    $pred $obj$terminator"
            }
            val text = "$subjectLabel\n" + lines.joinToString("\n")

            val referenced = stmts.mapNotNull { stmt ->
                stmt.getObject().takeIf { it.isURIResource }?.asResource()?.uri
            }.toSet()

            val idWords = (referenced + subject.uri).joinToString(" ") { uri -> Tokenizer.splitIdentifier(localName(uri)) }
            val tokens = Tokenizer.tokenize(text + " " + idWords)

            result += Chunk(idx, source, subjectLabel, subject.uri, text, referenced, tokens)
            idx++
        }
        return result
    }

    private fun formatObject(model: Model, node: RDFNode): String = when {
        node.isURIResource -> model.shortForm(node.asResource().uri)
        node.isAnon -> "[ ]"
        else -> {
            val lit = node.asLiteral()
            val escaped = lit.lexicalForm.replace("\\", "\\\\").replace("\"", "\\\"")
            val lang = lit.language
            val datatypeUri = lit.datatypeURI
            when {
                !lang.isNullOrEmpty() -> "\"$escaped\"@$lang"
                datatypeUri != null && datatypeUri != "http://www.w3.org/2001/XMLSchema#string" ->
                    "\"$escaped\"^^${model.shortForm(datatypeUri)}"
                else -> "\"$escaped\""
            }
        }
    }

    private fun localName(uri: String): String {
        val cut = uri.indexOfLast { it == '#' || it == '/' }
        return if (cut >= 0) uri.substring(cut + 1) else uri
    }

    private fun buildIdf(chunks: List<Chunk>): Map<String, Double> {
        val documentFrequency = HashMap<String, Int>()
        chunks.forEach { chunk -> chunk.tokens.forEach { t -> documentFrequency.merge(t, 1, Int::plus) } }
        val n = chunks.size.coerceAtLeast(1)
        return documentFrequency.mapValues { (_, count) -> ln(1.0 + n.toDouble() / count) }
    }

    /** A cheap always-on index of every named subject per file, built with a real SPARQL query via ARQ. */
    private fun buildFullIndex(sources: Map<String, Model>): String {
        val query = QueryFactory.create(
            """
            SELECT DISTINCT ?s WHERE {
                ?s ?p ?o .
                FILTER(isIRI(?s))
            }
            ORDER BY ?s
            """.trimIndent(),
        )
        return sources.entries.joinToString("\n") { (label, model) ->
            val names = QueryExecutionFactory.create(query, model).use { exec ->
                exec.execSelect().asSequence()
                    .map { solution -> model.shortForm(solution.getResource("s").uri) }
                    .toList()
            }
            "$label identifiers: ${names.joinToString(", ")}"
        }
    }
}
