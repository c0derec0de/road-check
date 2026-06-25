package ru.cs.roadcheck.service

import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class DocumentationService {

    private val docsRootPath: Path = Paths.get("docs")
    
    private val parser: Parser
    private val renderer: HtmlRenderer

    init {
        val extensions: List<Extension> = listOf(TablesExtension.create())
        
        parser = Parser.builder()
            .extensions(extensions)
            .build()
        
        renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()
    }

    fun getAllDocuments(): List<DocInfo> {
        if (!Files.exists(docsRootPath)) {
            return emptyList()
        }

        return Files.walk(docsRootPath).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }
                .map { path ->
                    val relativePath = docsRootPath.relativize(path).toString()
                    val title = extractTitle(path) ?: formatTitle(path.fileName.toString())
                    val section = extractSection(relativePath)
                    DocInfo(
                        path = relativePath.replace("\\", "/"),
                        title = title,
                        section = section,
                        fileName = path.fileName.toString()
                    )
                }
                .toList()
                .sortedWith(compareBy<DocInfo>({ it.section }, { it.title }))
        }
    }

    fun renderDocument(path: String): String? {
        val filePath = docsRootPath.resolve(path.replace("/", "\\"))
        
        if (!Files.exists(filePath)) {
            return null
        }

        return try {
            val markdown = Files.readString(filePath)
            val document: Node = parser.parse(markdown)
            renderer.render(document)
        } catch (e: IOException) {
            null
        }
    }

    fun getDocumentInfo(path: String): DocInfo? {
        val filePath = docsRootPath.resolve(path.replace("/", "\\"))
        
        if (!Files.exists(filePath)) {
            return null
        }

        val title = extractTitle(filePath) ?: formatTitle(filePath.fileName.toString())
        val section = extractSection(path)
        
        return DocInfo(
            path = path.replace("\\", "/"),
            title = title,
            section = section,
            fileName = filePath.fileName.toString()
        )
    }

    private fun extractTitle(path: Path): String? {
        return try {
            Files.lines(path)
                .filter { it.startsWith("# ") }
                .findFirst()
                .map { it.substring(2).trim() }
                .orElse(null)
        } catch (e: IOException) {
            null
        }
    }

    private fun formatTitle(fileName: String): String {
        return fileName
            .removeSuffix(".md")
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun extractSection(path: String): String {
        val parts = path.replace("\\", "/").split("/")
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString("/")
        } else {
            "main"
        }
    }

    data class DocInfo(
        val path: String,
        val title: String,
        val section: String,
        val fileName: String
    )
}
