package delegates.generator.data

import NotAFlutterProject
import SyntaxException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*


class DartClassGenerator {
    private var isRootFile = true
    fun generateFromJson(source: String, destiny: File, rootName: String, isFinal: Boolean) {
        val nodesToProcessStack = initStack(source, rootName)
        val packageTemplate = extractPackageName(destiny)
        val finalMode = if (isFinal) "final " else ""
        var nodeWrapper: NodeWrapper
        var buffer: FileOutputStream
        var target: FileOutputStream
        var constructorBuilder: StringBuilder
        var serializatorBuilder: StringBuilder
        var classConstructorBuilder: StringBuilder
        var bufferFile: File

        while (nodesToProcessStack.isNotEmpty()) {
            nodeWrapper = nodesToProcessStack.pop()
            bufferFile = File(destiny, "__${nodeWrapper.sneakCaseName}.dart")
            buffer = FileOutputStream(bufferFile)
            target = FileOutputStream(File(destiny, "${nodeWrapper.sneakCaseName}.dart"))
            constructorBuilder = createConstructorStart(nodeWrapper)
            serializatorBuilder = createSerializatorStart()

            buffer.writeText("\nclass ${nodeWrapper.className} {\n\n")
            try {
                if (isRootFile) target.writeText("import 'package:cloud_firestore/cloud_firestore.dart';")
                nodeWrapper.node?.fields()?.forEach { (name, node) ->
                    processNode(buffer, node, name, finalMode).let { nodeInfo ->
                        nodeInfo.node?.apply {
                            nodesToProcessStack.push(this)
                            target.writeText("import '$packageTemplate$sneakCaseName.dart';\n")
                        }
                        serializatorBuilder.append(nodeInfo.mapSerialization)
                        constructorBuilder.append("\t\t$name = ${nodeInfo.mapDeserialization}")
                    }
                }

                completeGenerating(buffer, bufferFile, target, constructorBuilder, serializatorBuilder)
            } finally {
                buffer.close()
                target.close()
            }
        }
    }

    private fun initStack(source: String, rootName: String) =
        Stack<NodeWrapper>().apply {
            try {
                add(
                    NodeWrapper(
                        node = jacksonObjectMapper().readTree(source),
                        fieldName = rootName,
                        sneakCaseName = rootName,
                        className = extractRootClassName(rootName)
                    )
                )
            } catch (e: Exception) {
                throw SyntaxException()
            }
        }


    private fun completeGenerating(
        buffer: FileOutputStream,
        bufferFile: File,
        target: FileOutputStream,
        constructorBuilder: StringBuilder,
        serializatorBuilder: StringBuilder

    ) {
        constructorBuilder.apply {
            deleteCharAt(length - 1).deleteCharAt(length - 1).append(";\n")
        }

        serializatorBuilder
            .append("\t\treturn data;\n")
            .append("\t}\n")

        buffer.writeText(constructorBuilder.toString()).writeText("\n")
        buffer.writeText(serializatorBuilder.toString())
        buffer.writeText("}")
        buffer.close()

        mergeBufferAndTarget(target, bufferFile)
    }

    private fun processNode(
        fout: FileOutputStream, node: JsonNode, name: String, finalMode: String
    ): NodeInfo {
        val nodeInfo = extractNodeInfo(node, name)
        fout.writeText("  $finalMode${nodeInfo.stringRepresentation} $name;\n")
        return nodeInfo
    }

    private fun extractNodeInfo(node: JsonNode, name: String): NodeInfo {
        return when {
            node.isDouble || node.isFloat || node.isBigDecimal ->
                NodeInfo("double", name)

            node.isShort || node.isInt || node.isLong || node.isBigInteger ->
                NodeInfo("int", name)

            node.isBoolean ->
                NodeInfo("bool", name)

            node.isTextual ->
                NodeInfo("String", name)

            node.isArray ->
                extractArrayData(node as ArrayNode, name)

            node.isObject ->
                NodeWrapper(node, name).toObjectNodeInfo()

            else -> NodeInfo("Object", name)
        }
    }

    private fun extractArrayData(node: ArrayNode, name: String): NodeInfo {
        val iterator = node.iterator()
        if (!iterator.hasNext()) {
            return NodeInfo("List<Object>", name)
        }
        val elementInfo = extractNodeInfo(iterator.next(), name)
        return NodeInfo(
            "List<${elementInfo.stringRepresentation}>",
            elementInfo.node,
            elementInfo.buildListDeserialization(name),
            elementInfo.buildListSerialization(name)
        )
    }

    private fun createConstructorStart(nodeWrapper: NodeWrapper) =
        StringBuilder()
            .append("\n\t${nodeWrapper.className}.fromJsonMap(Map<String, dynamic> map): \n")


    private fun createSerializatorStart() =
        StringBuilder()
            .append("\tMap<String, dynamic> toJson() {\n")
            .append("\t\tfinal Map<String, dynamic> data = new Map<String, dynamic>();\n")

    private fun mergeBufferAndTarget(targetStream: FileOutputStream, bufferFile: File) {
        BufferedReader(FileReader(bufferFile)).useLines { lines ->
            lines.forEach {
                targetStream.writeText(it).writeText("\n")
            }
        }

        bufferFile.delete()
    }

    private fun extractPackageName(dir: File): String {
        val absolutePath = dir.absolutePath
        val splitted = absolutePath.split(System.getProperty("file.separator"))
        val libIndex = splitted.indexOf("lib")
        if (libIndex == -1) {
            throw NotAFlutterProject()
        }
        val fold = splitted
            .subList(libIndex + 1, splitted.size)
            .fold(StringBuilder()) { builder, s -> builder.append(s).append("/") }
        return "package:${splitted[libIndex - 1]}/$fold"
    }

    private fun FileOutputStream.writeText(text: String): FileOutputStream {
        write(text.toByteArray(Charsets.UTF_8))
        return this
    }

    private fun NodeWrapper.toObjectNodeInfo(): NodeInfo {
        val field = this.fieldName
        return NodeInfo(
            className,
            this,
            "$className.fromJsonMap(map[\"$fieldName\"]),\n",
            "\t\tdata()['$field'] = $field == null ? null : $field.toJson();\n"
        )
    }

    private fun NodeInfo.buildListDeserialization(rawName: String) =
        if (node != null) {
            "List<${node.className}>.from(map[\"${node.fieldName}\"]" +
                    ".map((it) => ${node.className}.fromJsonMap(it))),\n"
        } else {
            "List<$stringRepresentation>.from(map[\"$rawName\"]),\n"
        }

    private fun NodeInfo.buildListSerialization(rawName: String) =
        if (node != null) {
            "\t\tdata()['$rawName'] = ${node.fieldName} != null ? \n" +
                    "\t\t\tthis.${node.fieldName}.map((v) => v.toJson()).toList()\n" +
                    "\t\t\t: null;\n"
        } else {
            "\t\tdata()['$rawName'] = $rawName;\n"
        }


    private fun extractRootClassName(rootFileName: String): String {
        var needUp = true
        val builder = StringBuilder()
        val i = rootFileName.iterator()
        var element: Char

        while (i.hasNext()) {
            element = i.nextChar()
            if (element == '_') {
                needUp = true
                continue
            }
            if (needUp) {
                element = element.toUpperCase()
                needUp = false
            }

            builder.append(element)
        }
        return builder.toString()
    }
}