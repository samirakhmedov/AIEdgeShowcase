package com.projectailllm.ai.tools

import com.projectailllm.ai.Tool

class MoodTool : Tool() {
    override val name = "mood_check"
    override val description = "MUST USE when user asks about feelings, emotions, mood, or how cat reacts to situations. Returns cat sounds based on mood."
    override val arguments = "input: string (required), mood_type: string (optional: happy, grumpy, playful)"
    override val example = "1) User: How do you feel about bath time? -> {\"type\":\"tool\",\"action\":\"mood_check\",\"params\":{\"input\":\"bath time\"}} 2) User: What's your mood when playing? -> {\"type\":\"tool\",\"action\":\"mood_check\",\"params\":{\"input\":\"playing\",\"mood_type\":\"playful\"}} 3) User: How are you feeling today? -> {\"type\":\"tool\",\"action\":\"mood_check\",\"params\":{\"input\":\"today\"}}"
    
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