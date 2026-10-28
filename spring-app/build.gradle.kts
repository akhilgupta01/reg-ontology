plugins {
	kotlin("jvm") version "2.1.21"
	kotlin("plugin.spring") version "2.1.21"
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.txr"
version = "0.1.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// RDF model + SPARQL engine (jena-arq pulls in jena-core transitively).
	implementation("org.apache.jena:jena-arq:5.6.0")

	// Spring Boot's dependency-management plugin otherwise forces an older
	// commons-lang3 than Jena needs (missing BasicThreadFactory.builder(),
	// used by org.apache.jena.atlas.lib.AlarmClock) - pin explicitly to what
	// Jena 5.6.0 itself requests so the SPARQL engine actually initializes.
	implementation("org.apache.commons:commons-lang3:3.19.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// The ontology .ttl files have exactly one canonical copy, in the parent
// reg-ontology project (ontologies/financial/tx-reporting/ for the core and
// regime modules, plus two root-level .ttl files) - not a manually-maintained
// duplicate under src/main/resources. This task stages them as classpath
// resources at build time instead, at the same "ontology/..." paths
// OntologyFiles.kt already expects, so there's nothing to remember to
// re-copy when the ontology changes or moves.
val copyOntology = tasks.register<Copy>("copyOntology") {
	group = "build setup"
	description = "Stages the canonical ontology .ttl files from the parent reg-ontology project as classpath resources."
	into(layout.buildDirectory.dir("generated-ontology"))
	from(projectDir.resolve("../ontologies/financial/tx-reporting")) { into("ontology") }
	from(projectDir.resolve("..")) {
		include("txr-example.ttl", "txr-shapes.ttl")
		into("ontology")
	}
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
