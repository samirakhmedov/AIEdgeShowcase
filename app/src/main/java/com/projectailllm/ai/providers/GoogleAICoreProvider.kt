package com.projectailllm.ai.providers

import android.util.Log
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.projectailllm.MyApplication
import com.projectailllm.ai.GenerativeModelProvider
import com.projectailllm.ai.InferenceConfig
import com.projectailllm.ai.ModelResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GoogleAICoreProvider(
    private val modelPath: String? = null
) : GenerativeModelProvider {
    
    private var generativeModel: GenerativeModel? = null
    private var downloadCallback: DownloadCallback? = null
    
    override suspend fun initialize() {
        val generationConfig = generationConfig {
            context = MyApplication.appContext
            temperature = 0.7f
            maxOutputTokens = 2048
            topK = 40
        }
        
        val downloadConfig = DownloadConfig()
        generativeModel = GenerativeModel(
            generationConfig = generationConfig,
            downloadConfig = downloadConfig
        )
    }
    
    override suspend fun download(onProgress: (Float) -> Unit) {
        val callback = object : DownloadCallback {
            private var totalBytes: Long = 0
            
            override fun onDownloadStarted(bytesToDownload: Long) {
                totalBytes = bytesToDownload
            }
            
            override fun onDownloadProgress(totalBytesDownloaded: Long) {
                if (totalBytes > 0) {
                    val progress = totalBytesDownloaded.toFloat() / totalBytes.toFloat()
                    onProgress(progress)
                }
            }
            
            override fun onDownloadCompleted() {
                onProgress(1.0f)
            }
            
            override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
                // Handle error
            }
            
            override fun onDownloadDidNotStart(e: GenerativeAIException) {
                // Handle error
            }
        }
        
        // Create model with download callback
        val generationConfig = generationConfig {
            context = MyApplication.appContext
            temperature = 0.7f
            maxOutputTokens = 2048
            topK = 40
        }
        
        val downloadConfig = DownloadConfig(callback)
        generativeModel = GenerativeModel(
            generationConfig = generationConfig,
            downloadConfig = downloadConfig
        )
    }
    
    override suspend fun warmUp() {
        generativeModel?.prepareInferenceEngine()
    }
    
    override suspend fun sendPrompt(
        prompt: String,
        config: InferenceConfig
    ): ModelResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel?.generateContent(prompt)
                val text = response?.text ?: ""

                Log.d("LLM", text)

                parseJsonResponse(text)
            } catch (e: Exception) {
                ModelResponse.ParseError("", "Generation failed: ${e.message}")
            }
        }
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
    
    override suspend fun dispose() {
        generativeModel = null
        downloadCallback = null
    }
    
    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            map[key] = get(key)
        }
        return map
    }
}