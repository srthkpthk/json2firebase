package delegates.generator.old.data

import NodeWrapper


data class NodeInfo(
    val stringRepresentation: String,
    val node: NodeWrapper?,
    val mapDeserialization: String?,
    val mapSerialization: String?
) {
    constructor(stringRepresentation: String, name: String):
        this(
            stringRepresentation,
            null,
            "map[\"$name\"],\n",
            "\t\tdata['$name'] = $name;\n"
        )
}