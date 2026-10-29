// Root of the multi-module Gradle build. Plugins used by more than one
// subproject are declared once here with `apply false` - each subproject
// then applies them without a version (see their own build.gradle.kts).
// This isn't just tidiness: applying the Kotlin Gradle plugin separately
// per subproject causes Gradle to load it multiple times in the same
// build, which corrupts the Kotlin compiler daemon's state and makes
// `compileKotlin` fail nondeterministically.
plugins {
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.spring.boot) apply false
	alias(libs.plugins.spring.dependency.management) apply false
}
