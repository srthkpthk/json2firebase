package delegates.generator

import delegates.generator.data.DartClassGenerator
import FileIOException
import MessageDelegate
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import java.io.File
import java.io.IOException

class GeneratorDelegate(
    private val messageDelegate: MessageDelegate = MessageDelegate()
) {

    fun runGeneration(event: AnActionEvent, fileName: String, json: String) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(
                event.project, "Dart file generating", false
            ) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        DartClassGenerator().generateFromJson(
                            json,
                            File(event.getData(CommonDataKeys.VIRTUAL_FILE)?.path),
                            fileName.takeIf(String::isNotBlank) ?: "response"
                            , false
                        )
                        messageDelegate.sendNotification("Dart class has been generated")
                        messageDelegate.sendNotification("Currently this Plugin is in development. if any error please raise a issue on github")
                    } catch (e: Throwable) {
                        when (e) {
                            is IOException -> messageDelegate.onException(FileIOException())
                            else -> messageDelegate.onException(e)
                        }
                    } finally {
                        indicator.stop()
                        ProjectView.getInstance(event.project).refresh()
                        event.getData(LangDataKeys.VIRTUAL_FILE)?.refresh(false, true)
                    }
                }
            }
        )
    }
}