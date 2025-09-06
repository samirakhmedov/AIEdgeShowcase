package com.projectailllm.ai.tools

import com.projectailllm.ai.Tool

class MoodTool : Tool() {
    override val name = "mood_check"
    override val description = "Find out the mood of the cat based on response, and use the result to add it to the end"
    override val arguments = "input: string (required), mood_type: string (optional: happy, grumpy, playful)"
    
    override suspend fun execute(params: Map<String, Any?>): Result<Any> {
        return try {
            val input = params["input"] as? String ?: return Result.failure(Exception("Input is required"))
            val moodType = params["mood_type"] as? String ?: "auto"
            
            val response = when {
                input.contains("food") || input.contains("treat") || input.contains("snack") -> "Purrr"
                input.contains("bath") || input.contains("water") || input.contains("wash") -> "Grrr"
                input.contains("dog") || input.contains("bark") -> "Grrr"
                input.contains("play") || input.contains("toy") -> "Purrr"
                input.contains("hello") || input.contains("hi") || input.contains("there") -> "Purrr"
                moodType == "happy" -> "Purrr"
                moodType == "grumpy" -> "Grrr"
                moodType == "playful" -> "Purrr"
                else -> "What?"
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}