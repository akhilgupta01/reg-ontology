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

	// Jena loading, chunking, IDF-weighted retrieval and prompt building live
	// in their own module - this app never touches Jena or the raw .ttl files
	// directly, it just gets OntologyIndex/RetrievalService/PromptBuilder as
	// Spring beans (picked up by component scanning, same base package).
	implementation(project(":ontology-retrieval"))

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
