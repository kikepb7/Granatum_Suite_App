import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File

/**
 * Static analysis and coverage for every module, configured once here rather than module by
 * module (constitution principle XI). Applied through `KmpLibraryConventionPlugin` and
 * `CmpApplicationConventionPlugin`, which between them cover all thirteen modules.
 *
 * Coverage is only *collected* here. Aggregating it and enforcing the ratchet happens in the
 * root build.gradle.kts, the one place that can see every module — see the comment there for
 * why that cannot be a convention plugin too.
 *
 * ktlint only reports. The codebase predates it, so making it blocking now would mean fixing
 * every existing violation in one go. Flipping `ignoreFailures` is a constitution amendment
 * once the code has been cleaned up.
 *
 * Both tools are `compileOnly` in build-logic and loaded once by the root with `apply false`,
 * the same arrangement the build already uses for AGP and the Kotlin plugin.
 */
class QualityConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<KtlintExtension> {
            ignoreFailures.set(true)
            reporters {
                reporter(ReporterType.PLAIN)
                // Machine-readable, so CI can count violations for the run summary.
                reporter(ReporterType.CHECKSTYLE)
            }
            // BuildKonfig, Compose resources and KSP all generate sources under build/. Linting
            // code nobody wrote only buries the violations that matter.
            filter {
                exclude { element -> element.file.path.contains("${File.separator}build${File.separator}") }
            }
        }
    }
}
