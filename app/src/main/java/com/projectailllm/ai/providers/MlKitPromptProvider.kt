package com.projectailllm.ai.providers

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.projectailllm.ai.GenerativeModelProvider
import com.projectailllm.ai.InferenceConfig
import com.projectailllm.ai.ModelResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MlKitPromptProvider() : GenerativeModelProvider {
    private var generativeModel: GenerativeModel? = null

    override suspend fun initialize() {
        generativeModel = Generation.getClient()
    }

    override suspend fun download(onProgress: (Float) -> Unit) {
        var totalBytes: Long = 0

        generativeModel?.download()?.collect { status ->
            when (status) {
                is DownloadStatus.DownloadProgress ->
                    onProgress((status.totalBytesDownloaded / totalBytes).toFloat())

                is DownloadStatus.DownloadStarted -> totalBytes = status.bytesToDownload
                else -> {}
            }
        }
    }

    override suspend fun warmUp() {
       generativeModel?.warmup()
    }

    override suspend fun sendPrompt(
        prompt: String,
        config: InferenceConfig
    ): ModelResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel?.generateContent(
                    generateContentRequest(
                        TextPart(prompt),
                    ) {
                        temperature = config.temperature
                        topK = config.topK
                        maxOutputTokens = config.maxOutputTokens
                    }
                )

                val text = response?.candidates[0]!!.text

                parseJsonResponse(text)
            } catch (e: Exception) {
                ModelResponse.ParseError("", "Generation failed: ${e.message}")
            }
        }
    }


    override suspend fun dispose() {
        generativeModel?.clearCaches()
        generativeModel?.close()
        generativeModel = null
    }

    private fun parseJsonResponse(text: String): ModelResponse {
        return try {
            // Extract JSON from response (handle potential markdown blocks)
            val jsonText = text
                .substringAfter("```json", "")
                .substringBefore("```", text)
                .trim()
                .let { it.ifEmpty { text } }

            val json = JSONObject(jsonText)
            when (json.getString("type")) {
                "answer" -> ModelResponse.Answer(
                    text = json.getString("text"),
                    confidence = json.optDouble("confidence", 0.9).toFloat()
                )
                "tool" -> ModelResponse.ToolCall(
                    action = json.getString("action"),
                    params = json.getJSONObject("params").toMap()
                )
                "fallback" -> ModelResponse.Fallback(
                    reason = json.getString("reason")
                )
                else -> ModelResponse.ParseError(text, "Unknown response type")
            }
        } catch (e: Exception) {
            // If JSON parsing fails, try to extract information from plain text
            ModelResponse.ParseError(text, "JSON parse error: ${e.message}")
        }
    }


    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            map[key] = get(key)
        }
        return map
    }
}