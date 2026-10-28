plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.txr"
version = "0.1.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
	}
}

dependencies {
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.kotlin.reflect)

	// The ontology .ttl files, staged as classpath resources by the :ontology
	// module - see its build.gradle.kts. There's exactly one canonical copy
	// (ontologies/financial/tx-reporting/ at the root of this build); nothing
	// here owns a duplicate or a copy task of its own.
	implementation(project(":ontology"))

	// RDF model + SPARQL engine (jena-arq pulls in jena-core transitively).
	implementation(libs.jena.arq)

	// Spring Boot's dependency-management plugin otherwise forces an older
	// commons-lang3 than Jena needs (missing BasicThreadFactory.builder(),
	// used by org.apache.jena.atlas.lib.AlarmClock) - pin explicitly to what
	// Jena itself requests so the SPARQL engine actually initializes.
	implementation(libs.commons.lang3)

	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.kotlin.test.junit5)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
