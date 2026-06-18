package org.speculum.update

/** Marker shown (and stored) for a non-release / development build. */
const val DEV_VERSION = "dev"

/**
 * The installed version, detected automatically — never read from or persisted
 * to config. A development run reports [DEV_VERSION]; a packaged build uses the
 * `speculum.version` system property the host injects (else its manifest
 * Implementation-Version, else [DEV_VERSION]).
 *
 * Dev is detected by the absence of `jpackage.app-path`, the system property the
 * jpackage native launcher sets only for the packaged/installed app — so running
 * via `./gradlew run` always shows "dev" regardless of the injected property.
 */
fun detectVersion(): String =
    if (isDevRuntime()) DEV_VERSION
    else (System.getProperty("speculum.version")
        ?: Release::class.java.`package`?.implementationVersion
        ?: DEV_VERSION).removePrefix("v")

/** True when not running from the packaged app (jpackage launcher sets app-path). */
fun isDevRuntime(): Boolean =
    System.getProperty("jpackage.app-path").isNullOrBlank()

/** A development build has no comparable numeric version, so updates aren't checked. */
fun isDevVersion(version: String): Boolean =
    version.isBlank() || version.equals(DEV_VERSION, ignoreCase = true) ||
        version.substringBefore('-').split('.').none { it.toIntOrNull() != null }

/**
 * True if [latest] is a strictly higher version than [current]. Leading "v" is
 * ignored and versions are compared component-by-component as integers
 * (e.g. "1.2.0" > "1.1.9"); a pre-release suffix is dropped at the first dash.
 */
fun isNewer(latest: String, current: String): Boolean {
    fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
    val a = parts(latest)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
