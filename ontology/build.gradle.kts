// This module has no Kotlin/Java source of its own - it exists purely to
// expose the canonical ontology .ttl files (ontologies/financial/tx-reporting/
// at the root of this build) as a normal Gradle dependency, so spring-app (or
// any future consumer) gets them via `implementation(project(":ontology"))`
// instead of reaching across module boundaries with relative paths.
plugins {
    `java-library`
}

// The canonical ontology source lives here (edit it there, not in this
// module - there is exactly one copy, this just stages it onto the
// classpath at build time, at the same "ontology/..." paths the consuming
// app's OntologyFiles.kt expects).
val canonicalOntologyDir = rootProject.projectDir.resolve("ontologies/financial/tx-reporting")

val copyOntology = tasks.register<Copy>("copyOntology") {
    group = "build setup"
    description = "Stages the canonical ontology .ttl files as this module's classpath resources."
    into(layout.buildDirectory.dir("generated-ontology"))
    from(canonicalOntologyDir) { into("ontology") }
}

sourceSets {
    main {
        resources {
            srcDir(copyOntology)
        }
    }
}

tasks.named("processResources") {
    dependsOn(copyOntology)
}
