package com.projectailllm.ai.tools

import com.projectailllm.ai.Tool

class GreetingTool : Tool() {
    override val name = "greeting"
    override val description = "MUST USE when user asks to greet, say hi, or introduce to someone by name. Generates personalized greetings."
    override val arguments = "name: string (required), style: string (optional: formal, casual, funny)"
    override val example = "1) User: Say hi to John -> {\"type\":\"tool\",\"action\":\"greeting\",\"params\":{\"name\":\"John\"}} 2) User: Greet Sarah formally -> {\"type\":\"tool\",\"action\":\"greeting\",\"params\":{\"name\":\"Sarah\",\"style\":\"formal\"}} 3) User: Say hello to Mike in a funny way -> {\"type\":\"tool\",\"action\":\"greeting\",\"params\":{\"name\":\"Mike\",\"style\":\"funny\"}}"
    
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