package buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Formats source files in place.")
abstract class KotlinAssignmentWrappingFormatTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val maxLineLength: Property<Int>

    init {
        maxLineLength.convention(DEFAULT_MAX_LINE_LENGTH)
    }

    @TaskAction
    fun format() {
        sourceFiles.files
            .filter { it.isFile }
            .forEach { file ->
                val original = file.readText()
                val formatted = KotlinAssignmentWrapping.formatText(
                    text = original,
                    maxLineLength = maxLineLength.get(),
                )
                if (formatted != original) {
                    file.writeText(formatted)
                }
            }
    }
}

@DisableCachingByDefault(because = "Reports source formatting violations.")
abstract class KotlinAssignmentWrappingCheckTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val maxLineLength: Property<Int>

    init {
        maxLineLength.convention(DEFAULT_MAX_LINE_LENGTH)
    }

    @TaskAction
    fun check() {
        val violations = sourceFiles.files
            .filter { it.isFile }
            .flatMap { file ->
                KotlinAssignmentWrapping.findViolations(
                    text = file.readText(),
                    maxLineLength = maxLineLength.get(),
                ).map { violation ->
                    "${file.toRelativeString(project.rootDir)}:${violation.lineNumber}: " +
                        "Move expression start onto the assignment line: ${violation.replacement.trim()}"
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                violations.joinToString(
                    separator = "\n",
                    prefix = "Kotlin assignment wrapping violations found:\n",
                ),
            )
        }
    }
}

private const val DEFAULT_MAX_LINE_LENGTH = 120
