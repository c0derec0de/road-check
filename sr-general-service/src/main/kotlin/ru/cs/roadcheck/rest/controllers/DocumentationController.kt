package ru.cs.roadcheck.rest.controllers

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.cs.roadcheck.service.DocumentationService

@RestController
@RequestMapping("/docs")
class DocumentationController(
    private val documentationService: DocumentationService
) {

    @GetMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun getDocumentationIndex(): String {
        val documents = documentationService.getAllDocuments()

        val groupedDocs = documents.groupBy { it.section }
        
        return buildHtmlPage(
            title = "Документация",
            content = buildIndexContent(groupedDocs),
            currentPath = ""
        )
    }

    @GetMapping(value = ["/{path}/**", "/{path}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getDocument(@PathVariable path: String, request: jakarta.servlet.http.HttpServletRequest): String {
        val fullPath = request.requestURI.substringAfter("/docs/").trim('/')
        
        val htmlContent = documentationService.renderDocument(fullPath)
        val docInfo = documentationService.getDocumentInfo(fullPath)
        
        if (htmlContent == null || docInfo == null) {
            return buildHtmlPage(
                title = "Документ не найден",
                content = """
                    <div class="error-message">
                        <h2>404 - Документ не найден</h2>
                        <p>Документ по пути "$fullPath" не существует.</p>
                        <a href="/docs" class="back-link">← Вернуться к списку документов</a>
                    </div>
                """.trimIndent(),
                currentPath = fullPath
            )
        }
        
        return buildHtmlPage(
            title = docInfo.title,
            content = htmlContent,
            currentPath = fullPath
        )
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDocumentsJson(): List<Map<String, String>> {
        return documentationService.getAllDocuments().map { doc ->
            mapOf(
                "path" to doc.path,
                "title" to doc.title,
                "section" to doc.section,
                "fileName" to doc.fileName
            )
        }
    }

    private fun buildHtmlPage(title: String, content: String, currentPath: String): String {
        val documents = documentationService.getAllDocuments()
        val groupedDocs = documents.groupBy { it.section }
        
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title - RoadCheck Documentation</title>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                <style>
                    :root {
                        --primary-color: #2563eb;
                        --primary-hover: #1d4ed8;
                        --sidebar-width: 280px;
                        --header-height: 60px;
                        --bg-color: #f1f5f9;
                        --sidebar-bg: #0f172a;
                        --sidebar-text: #e2e8f0;
                        --sidebar-active: #38bdf8;
                        --card-bg: #ffffff;
                        --text-color: #0f172a;
                        --text-muted: #64748b;
                        --border-color: #e2e8f0;
                        --code-bg: #0d1117;
                    }

                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-color);
                        line-height: 1.6;
                    }

                    /* Header */
                    .header {
                        position: fixed;
                        top: 0;
                        left: 0;
                        right: 0;
                        height: var(--header-height);
                        background: linear-gradient(120deg, #0f172a 0%, #1e3a8a 45%, #2563eb 100%);
                        border-bottom: 1px solid rgba(255,255,255,0.08);
                        display: flex;
                        align-items: center;
                        padding: 0 24px;
                        z-index: 100;
                        box-shadow: 0 4px 24px rgba(15, 23, 42, 0.25);
                    }

                    .header-logo {
                        font-size: 1.2rem;
                        font-weight: 700;
                        letter-spacing: -0.02em;
                        color: #f8fafc;
                        text-decoration: none;
                    }

                    .header-logo:hover {
                        color: #e0f2fe;
                    }

                    /* Sidebar */
                    .sidebar {
                        position: fixed;
                        top: var(--header-height);
                        left: 0;
                        bottom: 0;
                        width: var(--sidebar-width);
                        background: var(--sidebar-bg);
                        overflow-y: auto;
                        padding: 20px 0;
                        z-index: 99;
                    }

                    .sidebar-section {
                        margin-bottom: 24px;
                    }

                    .sidebar-section-title {
                        padding: 8px 20px;
                        font-size: 0.75rem;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.05em;
                        color: var(--text-muted);
                    }

                    .sidebar-link {
                        display: block;
                        padding: 10px 20px;
                        color: var(--sidebar-text);
                        text-decoration: none;
                        font-size: 0.9rem;
                        transition: all 0.2s;
                        border-left: 3px solid transparent;
                    }

                    .sidebar-link:hover {
                        background: rgba(255,255,255,0.1);
                        border-left-color: var(--sidebar-active);
                    }

                    .sidebar-link.active {
                        background: rgba(59, 130, 246, 0.2);
                        border-left-color: var(--sidebar-active);
                        color: #fff;
                    }

                    /* Main Content */
                    .main-content {
                        margin-left: var(--sidebar-width);
                        margin-top: var(--header-height);
                        min-height: calc(100vh - var(--header-height));
                        padding: 40px;
                        max-width: 1200px;
                    }

                    /* Index Page */
                    .docs-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                        gap: 24px;
                        margin-top: 24px;
                    }

                    .doc-card {
                        background: var(--card-bg);
                        border-radius: 12px;
                        padding: 24px;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                        transition: all 0.2s;
                        border: 1px solid var(--border-color);
                    }

                    .doc-card:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                        border-color: var(--primary-color);
                    }

                    .doc-card h3 {
                        font-size: 1.1rem;
                        margin-bottom: 8px;
                        color: var(--text-color);
                    }

                    .doc-card p {
                        font-size: 0.875rem;
                        color: var(--text-muted);
                        margin-bottom: 16px;
                    }

                    .doc-card-link {
                        display: inline-block;
                        color: var(--primary-color);
                        text-decoration: none;
                        font-weight: 500;
                        font-size: 0.875rem;
                    }

                    .doc-card-link:hover {
                        text-decoration: underline;
                    }

                    /* Markdown Content Styles */
                    .markdown-body {
                        background: var(--card-bg);
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    }

                    .markdown-body h1 {
                        font-size: 2rem;
                        margin-bottom: 24px;
                        padding-bottom: 16px;
                        border-bottom: 2px solid var(--border-color);
                    }

                    .markdown-body h2 {
                        font-size: 1.5rem;
                        margin-top: 32px;
                        margin-bottom: 16px;
                        padding-bottom: 8px;
                        border-bottom: 1px solid var(--border-color);
                    }

                    .markdown-body h3 {
                        font-size: 1.25rem;
                        margin-top: 24px;
                        margin-bottom: 12px;
                    }

                    .markdown-body h4, .markdown-body h5, .markdown-body h6 {
                        margin-top: 20px;
                        margin-bottom: 10px;
                    }

                    .markdown-body p {
                        margin-bottom: 16px;
                        line-height: 1.8;
                    }

                    .markdown-body code {
                        background: var(--bg-color);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: 'SF Mono', Monaco, Consolas, monospace;
                        font-size: 0.875em;
                        color: #e83e8c;
                    }

                    .markdown-body pre {
                        background: var(--code-bg);
                        padding: 20px;
                        border-radius: 8px;
                        overflow-x: auto;
                        margin-bottom: 20px;
                    }

                    .markdown-body pre code {
                        background: none;
                        padding: 0;
                        color: #c9d1d9;
                    }

                    .markdown-body a {
                        color: var(--primary-color);
                        text-decoration: none;
                    }

                    .markdown-body a:hover {
                        text-decoration: underline;
                    }

                    .markdown-body ul, .markdown-body ol {
                        margin-bottom: 16px;
                        padding-left: 24px;
                    }

                    .markdown-body li {
                        margin-bottom: 8px;
                    }

                    .markdown-body blockquote {
                        border-left: 4px solid var(--primary-color);
                        padding-left: 16px;
                        margin-left: 0;
                        margin-bottom: 16px;
                        color: var(--text-muted);
                        font-style: italic;
                    }

                    .markdown-body table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                        overflow-x: auto;
                        display: block;
                    }

                    .markdown-body th, .markdown-body td {
                        border: 1px solid var(--border-color);
                        padding: 12px 16px;
                        text-align: left;
                    }

                    .markdown-body th {
                        background: var(--bg-color);
                        font-weight: 600;
                    }

                    .markdown-body tr:nth-child(even) {
                        background: var(--bg-color);
                    }

                    .markdown-body img {
                        max-width: 100%;
                        height: auto;
                        border-radius: 8px;
                    }

                    .markdown-body hr {
                        border: none;
                        border-top: 2px solid var(--border-color);
                        margin: 32px 0;
                    }

                    /* Error Message */
                    .error-message {
                        text-align: center;
                        padding: 60px 20px;
                    }

                    .error-message h2 {
                        font-size: 1.5rem;
                        margin-bottom: 16px;
                    }

                    .back-link {
                        display: inline-block;
                        margin-top: 20px;
                        color: var(--primary-color);
                        text-decoration: none;
                    }

                    .back-link:hover {
                        text-decoration: underline;
                    }

                    /* Mobile Responsive */
                    @media (max-width: 768px) {
                        .sidebar {
                            transform: translateX(-100%);
                            transition: transform 0.3s;
                        }

                        .sidebar.open {
                            transform: translateX(0);
                        }

                        .main-content {
                            margin-left: 0;
                            padding: 20px;
                        }

                        .markdown-body {
                            padding: 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <header class="header">
                    <a href="/docs" class="header-logo">RoadCheck — документация</a>
                </header>

                <nav class="sidebar">
                    ${buildSidebarHtml(groupedDocs, currentPath)}
                </nav>

                <main class="main-content">
                    <article class="markdown-body">
                        $content
                    </article>
                </main>

                <script>
                    hljs.highlightAll();
                    
                    // Mobile sidebar toggle
                    document.addEventListener('DOMContentLoaded', function() {
                        const sidebar = document.querySelector('.sidebar');
                        const toggleBtn = document.querySelector('.sidebar-toggle');
                        
                        if (toggleBtn) {
                            toggleBtn.addEventListener('click', function() {
                                sidebar.classList.toggle('open');
                            });
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildSidebarHtml(groupedDocs: Map<String, List<DocumentationService.DocInfo>>, currentPath: String): String {
        return groupedDocs.entries.joinToString("\n") { (section, docs) ->
            val sectionTitle = if (section == "main") "Основное" else section.replace("/", " / ").capitalize()
            """
            <div class="sidebar-section">
                <div class="sidebar-section-title">$sectionTitle</div>
                ${docs.joinToString("") { doc ->
                    val isActive = doc.path == currentPath
                    val activeClass = if (isActive) "active" else ""
                    """<a href="/docs/${doc.path}" class="sidebar-link $activeClass">${doc.title}</a>"""
                }}
            </div>
            """.trimIndent()
        }
    }

    private fun buildIndexContent(groupedDocs: Map<String, List<DocumentationService.DocInfo>>): String {
        return """
            <h1>📚 Документация RoadCheck</h1>
            <p style="color: var(--text-muted); margin-bottom: 24px;">
                Добро пожаловать в документацию проекта RoadCheck. Выберите документ из меню слева или из списка ниже.
            </p>
            
            <div class="docs-grid">
                ${groupedDocs.entries.joinToString("") { (section, docs) ->
                    docs.joinToString("") { doc ->
                        val sectionLabel = if (section == "main") "" else "<span style=\"font-size: 0.75rem; color: var(--text-muted);\">${section.replace("/", " / ")}</span>"
                        """
                        <div class="doc-card">
                            <h3>${doc.title}</h3>
                            $sectionLabel
                            <a href="/docs/${doc.path}" class="doc-card-link">Открыть документ →</a>
                        </div>
                        """.trimIndent()
                    }
                }}
            </div>
        """.trimIndent()
    }
}
