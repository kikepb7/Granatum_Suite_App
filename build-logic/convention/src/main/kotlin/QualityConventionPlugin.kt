import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
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

        // The filter above drops generated files, but not the tasks that generate them. KSP
        // registers its output directories as sources *together with* the tasks producing them,
        // so `ktlintCheck` ran KSP and the iOS compilations KSP depends on just to read files it
        // then excluded. On a Linux CI runner KSP produces no iOS output, those compilations
        // fail, and the lint job went red without a single lint error. Pointing each task at its
        // source set's hand-written directories, as plain files, lints exactly the same code and
        // drops every inherited task dependency.
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
            val buildDir = layout.buildDirectory.get().asFile
            // In afterEvaluate so this runs after ktlint-gradle's own configuration, which
            // otherwise sets the sources again on top of these.
            afterEvaluate {
                tasks.withType<BaseKtLintCheckTask>().configureEach {
                    val sourceSetName = KTLINT_TASK_NAME.matchEntire(name)?.groupValues?.get(1)
                        ?.replaceFirstChar { it.lowercase() } ?: return@configureEach
                    val sourceSet = kotlin.sourceSets.findByName(sourceSetName) ?: return@configureEach
                    setSource(sourceSet.kotlin.srcDirs.filterNot { it.startsWith(buildDir) })
                }
            }
        }
    }
}

private val KTLINT_TASK_NAME = Regex("runKtlint(?:Check|Format)Over(.+)SourceSet")
