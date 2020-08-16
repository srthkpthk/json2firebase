package delegates.generator.data
import com.fasterxml.jackson.databind.JsonNode
import toClassName
import toSneakCase

data class NodeWrapper(
    val node: JsonNode?,
    val fieldName: String,
    val sneakCaseName: String = toSneakCase(fieldName),
    val className: String = toClassName(fieldName)
)