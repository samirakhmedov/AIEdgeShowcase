package com.projectailllm.ai.tools

import com.projectailllm.ai.Tool

class GreetingTool : Tool() {
    override val name = "greeting"
    override val description = "Say hi to someone user asked to!"
    override val arguments = "name: string (required), style: string (optional: formal, casual, funny)"
    
    override suspend fun execute(params: Map<String, Any?>): Result<Any> {
        return try {
            val name = params["name"] as? String ?: return Result.failure(Exception("Name is required"))
            val style = params["style"] as? String ?: "casual"
            
            val greeting = when (style.lowercase()) {
                "formal" -> "Good day, $name. It's a pleasure to meet you."
                "funny" -> "Meow! Hello there, $name! *purrs loudly*"
                else -> "Hey $name! Nice to meet you!"
            }
            
            Result.success(greeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}