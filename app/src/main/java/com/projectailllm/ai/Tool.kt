package com.projectailllm.ai

abstract class Tool {
    abstract val name: String
    abstract val description: String
    abstract val arguments: String
    abstract val example: String
    
    abstract suspend fun execute(params: Map<String, Any?>): Result<Any>
}