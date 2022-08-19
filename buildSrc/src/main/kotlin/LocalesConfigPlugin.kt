import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.TaskContainerScope

fun TaskContainerScope.registerLocalesConfigTask(project: Project): TaskProvider<Task> {
    return with(project) {
        register("generateLocalesConfig") {
            val emptyResourcesElement = "<resources>\\s*<\\/resources>|<resources\\/>".toRegex()
            val valuesPrefix = "values-?".toRegex()

            val languages = fileTree("$projectDir/src/main/res/")
                .matching {
                    include("**/strings.xml")
                }
                .filterNot {
                    it.readText().contains(emptyResourcesElement)
                }
                .joinToString(separator = "\n") {
                    val language = it.parentFile.name
                        .replace(valuesPrefix, "")
                        .takeIf(String::isNotBlank) ?: "en"
                    "   <locale android:name=\"$language\"/>"
                }


            val content = """
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
$languages
</locale-config>
    """.trimIndent()

            val localeFile = file("$projectDir/src/main/res/xml/locales_config.xml")
            localeFile.parentFile.mkdirs()
            localeFile.writeText(content)
        }
    }
}

