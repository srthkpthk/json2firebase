import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.DialogBuilder
import delegates.generator.GeneratorDelegate
import views.Json2Firebase

class JsontoFirebaseAction(
    private val generatorDelegate: GeneratorDelegate = GeneratorDelegate()
) : AnAction("Convert json to dart") {

    override fun actionPerformed(event: AnActionEvent) {
        DialogBuilder().apply {
            val form = Json2Firebase()
            form.setOnGenerateListener { fileName, json ->
                window.dispose()
                generatorDelegate.runGeneration(event, fileName, json)
            }
            setCenterPanel(form.rootView)
            setTitle("Json2Dart")
            removeAllActions()
            show()
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.VIRTUAL_FILE)?.isDirectory ?: false
    }
}