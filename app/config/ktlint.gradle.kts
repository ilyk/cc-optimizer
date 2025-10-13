/**
 * ktlint Configuration for Kotlin
 * 
 * Enforces Kotlin coding conventions
 * Run: ./gradlew ktlintCheck
 * Fix: ./gradlew ktlintFormat
 */

// Add ktlint configuration
val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:0.50.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

// ktlint check task
val ktlintCheck by tasks.creating(JavaExec::class) {
    group = "verification"
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

// ktlint format task
val ktlintFormat by tasks.creating(JavaExec::class) {
    group = "formatting"
    description = "Fix Kotlin code style violations"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

// Run ktlint check as part of check task
tasks.named("check") {
    dependsOn(ktlintCheck)
}

