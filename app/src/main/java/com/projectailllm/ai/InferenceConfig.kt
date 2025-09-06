package com.projectailllm.ai

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 2048,
    val topK: Int = 40,
    val topP: Float = 0.95f
)