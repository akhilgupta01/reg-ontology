# TXR Ontology Assistant (Spring Boot / Kotlin / Jena)

Server-backed rewrite of the original static-HTML TXR Ontology Assistant prototype
(now removed in favour of this app): the ontology is parsed and queried with
**Apache Jena** on the backend instead of client-side JavaScript, and LLM API
keys live in server config (environment variables by default, see
`application.yml`) instead of browser `localStorage`.

## What it does

Loads the TXR ontology (the shared core, one Turtle module per regulatory
regime, and the worked example — staged from the parent `reg-ontology`
project at build time, see "Where the ontology lives" below) into Apache
Jena `Model`s at startup. Each named RDF subject becomes one retrieval
"chunk". A question is answered by:

1. Tokenizing the question and scoring every chunk by **IDF-weighted**
   keyword overlap (see `Tokenizer`/`RetrievalService`) — this is what keeps
   boilerplate RDF structure (`a txr:ReportingField`, `txr:fieldOfRegime`)
   from flooding every result with common words.
2. Expanding one hop: pulling in chunks referenced by the top-scoring chunks
   (e.g. a `Report` field chunk pulls in the `RegulatoryRegime` it points to).
3. Stopping once a character budget is spent, instead of sending the whole
   ontology on every request.
4. Building a system prompt from the selected excerpts plus a cheap
   always-on index (every named subject, by name only, built with a real
   SPARQL query via ARQ) and calling the chosen LLM (Anthropic or Gemini)
   server-side.

## Running it

Requires a JDK 21 (or newer; a Gradle toolchain will provision one if needed)
and at least one LLM API key:

```bash
export ANTHROPIC_API_KEY=sk-ant-...   # and/or
export GEMINI_API_KEY=...

./gradlew bootRun
```

Then open <http://localhost:8080>.

## Project layout

```
src/main/kotlin/com/txr/assistant/
  ontology/   Jena loading, chunking, IDF scoring, retrieval, prompt building
  llm/        Anthropic & Gemini HTTP clients (Spring RestClient)
  web/        REST controller (/api/providers, /api/ontology/preview, /api/ask)
src/main/resources/
  static/     the frontend (plain HTML/JS, no build step)
src/test/kotlin/...  a smoke test over the real ontology (no Spring context needed)
```

## Where the ontology lives

There is exactly **one** copy of the `.ttl` files: `common/`, `regulations/`,
and the root-level `.ttl` files in the parent `reg-ontology` project — this
project has no `src/main/resources/ontology/` of its own. The `copyOntology`
Gradle task (see `build.gradle.kts`) stages them into `build/generated-ontology/`
as a normal part of every build (wired into `processResources`), so they land
on the classpath automatically. Edit the ontology in the parent project and
just rebuild — there's nothing to manually re-copy, and nothing to go stale.

This does mean the project isn't meant to be checked out on its own: it
expects to find `../common` and `../regulations` next to it.
