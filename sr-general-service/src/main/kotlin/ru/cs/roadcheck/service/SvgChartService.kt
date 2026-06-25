package ru.cs.roadcheck.service

import org.springframework.stereotype.Service
import ru.cs.roadcheck.repository.DangerousZoneRepository
import ru.cs.roadcheck.repository.ReportRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@Service
class SvgChartService(
    private val reportRepository: ReportRepository,
    private val dangerousZoneRepository: DangerousZoneRepository
) {

    private val svgStyles = """
        <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600&amp;display=swap');
            text { font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; }
            .grid-line { stroke: #e9ecef; stroke-width: 1; }
            .label { font-size: 11px; fill: #6c757d; font-weight: 400; }
            .bar-accidents { fill: #4361ee; }
            .bar-fatalities { fill: #f72585; }
            .bar-injuries { fill: #f8961e; }
            .pie-high { stroke: #ef233c; fill: none; }
            .pie-medium { stroke: #ffb703; fill: none; }
            .pie-low { stroke: #06d6a0; fill: none; }
            .title { font-size: 16px; font-weight: 600; fill: #212529; }
            .legend-label { font-size: 13px; fill: #495057; }
        </style>
    """.trimIndent()

    fun generateMonthlyAccidentsChart(): String {
        val stats = getMonthlyStatistics()
        val content = generateMonthlyBars(stats)
        return wrapInSvg(600, 450, "Динамика происшествий", content)
    }

    fun generateRiskDistributionChart(): String {
        val risk = getRiskDistribution()
        val content = generateRiskPie(risk)
        return wrapInSvg(600, 450, "Анализ зон риска", content)
    }

    fun generateCausesDistributionChart(): String {
        val causes = getCausesStatistics()
        val content = generateCausesBars(causes)
        return wrapInSvg(1100, 500, "Топ причин инцидентов", content)
    }

    private fun wrapInSvg(width: Int, height: Int, title: String, content: String): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<svg width="$width" height="$height" viewBox="0 0 $width $height" xmlns="http://www.w3.org/2000/svg">
    $svgStyles
    <rect width="100%" height="100%" fill="#ffffff" rx="12"/>
    <text x="30" y="45" class="title">$title</text>
    $content
</svg>"""
    }

    private fun getMonthlyStatistics(): List<MonthlyStats> {
        val reports = reportRepository.findAll()
        val statsByMonth = mutableMapOf<YearMonth, Triple<Int, Int, Int>>()
        reports.forEach { r ->
            val ym = YearMonth.from(r.createdAt?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now())
            val curr = statsByMonth.getOrDefault(ym, Triple(0, 0, 0))
            statsByMonth[ym] = Triple(curr.first + 1, curr.second + (r.fatalities ?: 0), curr.third + (r.injuries ?: 0))
        }
        return statsByMonth.entries.sortedBy { it.key }.takeLast(8).map {
            MonthlyStats(it.key.month.getDisplayName(TextStyle.SHORT, Locale("ru")).replaceFirstChar { c -> c.uppercase() }, it.value.first, it.value.second, it.value.third)
        }
    }

    private fun getRiskDistribution(): RiskDistribution {
        val zones = dangerousZoneRepository.findByIsActiveTrue()
        return RiskDistribution(
            high = zones.count { it.riskLevel?.trim()?.lowercase() == "high" },
            medium = zones.count { it.riskLevel?.trim()?.lowercase() == "medium" },
            low = zones.count { it.riskLevel?.trim()?.lowercase() == "low" }
        )
    }

    private fun getCausesStatistics(): List<CauseStats> {
        return reportRepository.findAll().asSequence()
            .mapNotNull { it.cause?.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            .take(8).map { CauseStats(it.key, it.value) }.toList()
    }

    private fun generateMonthlyBars(stats: List<MonthlyStats>): String {
        val xShift = 60; val yBase = 350; val chartH = 250; val chartW = 480
        val maxValue = stats.flatMap { listOf(it.accidents, it.fatalities, it.injuries) }.maxOrNull()?.coerceAtLeast(1) ?: 1
        val step = if (stats.isNotEmpty()) chartW / stats.size else 120

        val grid = (0..4).joinToString("") { i ->
            val y = yBase - (i * chartH / 4)
            """<line x1="$xShift" y1="$y" x2="${xShift + chartW}" y2="$y" class="grid-line"/>"""
        }

        val bars = stats.mapIndexed { i, s ->
            val x = xShift + i * step + 15
            val h1 = (s.accidents.toFloat() / maxValue * chartH).toInt()
            val h2 = (s.fatalities.toFloat() / maxValue * chartH).toInt()
            val h3 = (s.injuries.toFloat() / maxValue * chartH).toInt()
            """
            <rect x="$x" y="${yBase - h1}" width="12" height="$h1" rx="3" class="bar-accidents"/>
            <rect x="${x + 15}" y="${yBase - h2}" width="12" height="$h2" rx="3" class="bar-fatalities"/>
            <rect x="${x + 30}" y="${yBase - h3}" width="12" height="$h3" rx="3" class="bar-injuries"/>
            <text x="${x + 22}" y="${yBase + 25}" text-anchor="middle" class="label">${s.month}</text>
            """
        }.joinToString("")

        return grid + bars + """
            <circle cx="100" cy="410" r="5" fill="#4361ee"/><text x="110" y="415" class="label">ДТП</text>
            <circle cx="180" cy="410" r="5" fill="#f72585"/><text x="190" y="415" class="label">Погибшие</text>
            <circle cx="280" cy="410" r="5" fill="#f8961e"/><text x="290" y="415" class="label">Раненые</text>
        """
    }

    private fun generateRiskPie(risk: RiskDistribution): String {
        val total = (risk.high + risk.medium + risk.low).toDouble()
        if (total == 0.0) return """<text x="300" y="220" text-anchor="middle" class="label">Данные отсутствуют</text>"""

        val cx = 220.0; val cy = 240.0; val r = 110.0; val innerR = 70.0
        val strokeW = r - innerR
        val radiusMid = innerR + (strokeW / 2)
        val circumference = 2 * Math.PI * radiusMid

        val slices = mutableListOf<String>()
        val categories = listOf(risk.high to "pie-high", risk.medium to "pie-medium", risk.low to "pie-low").filter { it.first > 0 }

        if (categories.size == 1) {
            val cls = categories[0].second
            slices.add("""<circle cx="$cx" cy="$cy" r="$radiusMid" fill="none" class="$cls" stroke-width="$strokeW"/>""")
        } else {
            var currentOffset = 0.0
            categories.forEach { (count, cls) ->
                val sliceLength = (count / total) * circumference
                slices.add("""
                    <circle cx="$cx" cy="$cy" r="$radiusMid" fill="none" class="$cls" 
                            stroke-width="$strokeW" stroke-dasharray="$sliceLength $circumference" 
                            stroke-dashoffset="-$currentOffset" transform="rotate(-90 $cx $cy)"/>
                """)
                currentOffset += sliceLength
            }
        }

        return slices.joinToString("") + """
            <circle cx="420" cy="180" r="6" fill="#ef233c"/><text x="435" y="185" class="legend-label">Высокий: ${risk.high}</text>
            <circle cx="420" cy="215" r="6" fill="#ffb703"/><text x="435" y="220" class="legend-label">Средний: ${risk.medium}</text>
            <circle cx="420" cy="250" r="6" fill="#06d6a0"/><text x="435" y="255" class="legend-label">Низкий: ${risk.low}</text>
            <text x="$cx" y="${cy + 8}" text-anchor="middle" class="title" style="font-size: 28px; font-weight: 800;">${total.toInt()}</text>
            <text x="$cx" y="${cy + 28}" text-anchor="middle" class="label" style="letter-spacing: 1px;">ВСЕГО ЗОН</text>
        """
    }

    private fun generateCausesBars(stats: List<CauseStats>): String {
        if (stats.isEmpty()) return """<text x="300" y="220" text-anchor="middle" class="label">Данных по причинам нет</text>"""
        val xShift = 100; val yBase = 350; val chartH = 220; val step = 120
        val maxValue = stats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

        return stats.mapIndexed { i, s ->
            val x = xShift + i * step; val h = (s.count.toFloat() / maxValue * chartH).toInt().coerceAtLeast(10)
            """
            <rect x="$x" y="${yBase - h}" width="45" height="$h" rx="6" class="bar-accidents"/>
            <text x="${x + 22}" y="${yBase + 25}" text-anchor="end" transform="rotate(-35, ${x + 22}, ${yBase + 25})" class="label">${s.causeName}</text>
            <text x="${x + 22}" y="${yBase - h - 10}" text-anchor="middle" class="title" style="font-size: 12px;">${s.count}</text>
            """
        }.joinToString("")
    }

    private data class MonthlyStats(val month: String, val accidents: Int, val fatalities: Int, val injuries: Int)
    private data class RiskDistribution(val high: Int, val medium: Int, val low: Int)
    private data class CauseStats(val causeName: String, val count: Int)
}