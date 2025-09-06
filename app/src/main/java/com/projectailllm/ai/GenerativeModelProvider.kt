package com.projectailllm.ai

interface GenerativeModelProvider {
    suspend fun initialize()
    suspend fun download(onProgress: (Float) -> Unit)
    suspend fun warmUp()
    suspend fun sendPrompt(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): ModelResponse
    suspend fun dispose()
}