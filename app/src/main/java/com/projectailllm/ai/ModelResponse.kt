package com.projectailllm.ai

sealed class ModelResponse {
    data class Answer(
        val text: String,
        val confidence: Float
    ) : ModelResponse()
    
    data class ToolCall(
        val action: String,
        val params: Map<String, Any?>
    ) : ModelResponse()
    
    data class Fallback(
        val reason: String
    ) : ModelResponse()
    
    data class ParseError(
        val rawText: String,
        val error: String
    ) : ModelResponse()
}