# reg-ontology

A Turtle/OWL ontology modelling financial transaction reporting (EMIR, UK
EMIR, CFTC Part 45/43, SEC Reg SBSR, MiFIR, SFTR, MAS, ASIC, JFSA), plus a
Spring Boot app that answers natural-language questions about it, grounded
by querying the RDF with Apache Jena.

This is a multi-module Gradle build rooted here. Build/run everything from
this directory:

```bash
export ANTHROPIC_API_KEY=sk-ant-...   # and/or
export GEMINI_API_KEY=...

./gradlew build              # build every module
./gradlew :spring-app:bootRun   # run the app, then open http://localhost:8080
```

## Layout

```
ontologies/financial/tx-reporting/   the canonical ontology source (.ttl files) - edit it here
ontology/                            Gradle module: exposes the .ttl files above as a
                                      normal dependency (implementation(project(":ontology")))
ontology-retrieval/                  Gradle module: Jena-backed loading, chunking,
                                      IDF-weighted retrieval and system-prompt building
                                      over the :ontology module's .ttl files
spring-app/                          Gradle module: the Spring Boot / Kotlin web app and UI;
                                      depends on :ontology-retrieval for everything ontology-related
gradle/libs.versions.toml            shared version catalog for every module
```

See [spring-app/README.md](spring-app/README.md) for how the app itself works.
