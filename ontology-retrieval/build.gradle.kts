// Jena-backed ontology loading, chunking, IDF-weighted retrieval and system-
// prompt building - the "ontology retrieval service". Deliberately NOT a
// Spring Boot module (no `org.springframework.boot` plugin, no bootJar/
// bootRun): it defines plain @Configuration/@Bean/@Service/@Component beans
// that any consumer (spring-app today) picks up via component scanning,
// without pulling in a web stack or anything Boot-specific of its own.
plugins {
	alias(libs.plugins.kotlin.jvm)
	// Opens Kotlin classes annotated @Configuration/@Component/etc. (Kotlin
	// classes are final by default, but Spring's CGLIB proxying of
	// @Configuration classes - e.g. OntologyLoader - requires non-final).
	alias(libs.plugins.kotlin.spring)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
	}
}

dependencies {
	// Gradle's native BOM support (not the io.spring.dependency-management
	// plugin's `dependencyManagement { imports { ... } }` DSL): that DSL
	// triggers a real Kotlin 2.4.x / Gradle 9.8.2 Build Tools API bug in a
	// non-Boot Kotlin module ("Cannot read the array length because the
	// return value of CommonCompilerArguments.getPluginClasspaths() is
	// null") - this plain `platform(...)` import gives the same managed
	// versions (e.g. for spring-context below) without it.
	implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

	// The canonical ontology .ttl files, staged as classpath resources by
	// the :ontology module - see its build.gradle.kts.
	implementation(project(":ontology"))

	implementation(libs.spring.context)

	// RDF model + SPARQL engine (jena-arq pulls in jena-core transitively).
	implementation(libs.jena.arq)

	// The managed commons-lang3 version is otherwise too old for Jena
	// (missing BasicThreadFactory.builder(), used by
	// org.apache.jena.atlas.lib.AlarmClock) - pin explicitly to what Jena
	// itself requests so the SPARQL engine actually initializes.
	implementation(libs.commons.lang3)

	testImplementation(libs.kotlin.test.junit5)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
