package ru.cs.roadcheck.job

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.cs.roadcheck.config.JobsProperties
import java.io.IOException

@Component
class PredictionJob(
    jobsProperties: JobsProperties,
) : Job {
    private val scriptPath: String = jobsProperties.predictions.script.path

    private val logger = LoggerFactory.getLogger(PredictionJob::class.java)

    override fun execute(context: JobExecutionContext?) {
        try {
            // Сначала пересчёт опасных зон через dangerous_code.py
            val dangerousScriptPath = scriptPath
                .replace("risk.py", "dangerous_code.py")

            val dangerousOk = runPythonScript(
                dangerousScriptPath,
                "dangerous zones recalculation"
            )

            if (!dangerousOk) {
                logger.error("Skipping risk predictions script because dangerous_code.py failed")
                return
            }

            // Затем расчёт рисков через основной скрипт (risk.py)
            runPythonScript(
                scriptPath,
                "risk predictions"
            )
        } catch (e: IOException) {
            logger.error("Error executing Python scripts", e)
        } catch (e: InterruptedException) {
            logger.error("Python script execution interrupted", e)
            Thread.currentThread().interrupt()
        }
    }

    private fun runPythonScript(path: String, description: String): Boolean {
        logger.info("Starting Python script ($description): $path")
        val processBuilder = ProcessBuilder("python", path)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val exitCode = process.waitFor()
        val output = process.inputStream.bufferedReader().readText()

        return if (exitCode == 0) {
            logger.info("Python script ($description) executed successfully. Output:\n$output")
            true
        } else {
            logger.error("Python script ($description) failed with exit code $exitCode. Output:\n$output")
            false
        }
    }
}
