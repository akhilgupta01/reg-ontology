# TXR Ontology Assistant (Spring Boot / Kotlin / Jena)

The `:spring-app` module of this repo's multi-module Gradle build (see the
[root README](../README.md) for build/run commands — there's no standalone
wrapper here, build from the repo root).

Server-backed rewrite of an earlier static-HTML prototype of this same idea:
the ontology is parsed and queried with **Apache Jena** on the backend
instead of client-side JavaScript, and LLM API keys live in server config
(environment variables by default, see `application.yml`) instead of browser
`localStorage`.

## What it does

Ontology loading, chunking, retrieval and prompt building live in the
sibling `:ontology-retrieval` module (which in turn depends on `:ontology`
for the staged `.ttl` files — see the root README); this app just gets
`OntologyIndex`/`RetrievalService`/`PromptBuilder` as Spring beans via
component scanning and otherwise only knows about LLM calls and the web
layer. `:ontology-retrieval` loads the TXR ontology into Apache Jena
`Model`s at startup, with each named RDF subject becoming one retrieval
"chunk". A question is answered by:

1. Tokenizing the question and scoring every chunk by **IDF-weighted**
   keyword overlap (see `Tokenizer`/`RetrievalService` in `:ontology-retrieval`)
   — this is what keeps boilerplate RDF structure (`a txr:ReportingField`,
   `txr:fieldOfRegime`) from flooding every result with common words.
2. Expanding one hop: pulling in chunks referenced by the top-scoring chunks
   (e.g. a `Report` field chunk pulls in the `RegulatoryRegime` it points to).
3. Stopping once a character budget is spent, instead of sending the whole
   ontology on every request.
4. Building a system prompt from the selected excerpts plus a cheap
   always-on index (every named subject, by name only, built with a real
   SPARQL query via ARQ) and calling the chosen LLM (Anthropic or Gemini)
   server-side.

## Project layout

```
src/main/kotlin/com/txr/assistant/
  llm/        Anthropic & Gemini HTTP clients (Spring RestClient)
  web/        REST controller (/api/providers, /api/ontology/preview, /api/ask)
src/main/resources/
  static/     the frontend (plain HTML/JS, no build step)
```

See `../ontology-retrieval/` for the Jena loading, chunking, IDF scoring,
retrieval and prompt-building code, and its own smoke test over the real
ontology (no Spring context needed).
