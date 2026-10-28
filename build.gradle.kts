// Root of the multi-module Gradle build. Intentionally minimal: each
// subproject (ontology/, spring-app/) declares its own plugins and
// dependencies from the shared version catalog at gradle/libs.versions.toml;
// repository resolution is centralized in settings.gradle.kts instead of
// being repeated here or per-module.
