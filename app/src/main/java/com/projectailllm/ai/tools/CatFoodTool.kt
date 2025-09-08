package com.projectailllm.ai.tools

import com.projectailllm.ai.Tool

class CatFoodTool : Tool() {
    override val name = "cat_food_mapper"
    override val description = "MUST USE when user asks about food, eating, treats, or food preferences. Returns cat's opinion about specific foods."
    override val arguments = "food_name: string (required)"
    override val example = "1) User: Do you like tuna? -> {\"type\":\"tool\",\"action\":\"cat_food_mapper\",\"params\":{\"food_name\":\"tuna\"}} 2) User: What about chicken? -> {\"type\":\"tool\",\"action\":\"cat_food_mapper\",\"params\":{\"food_name\":\"chicken\"}} 3) User: How do you feel about vegetables? -> {\"type\":\"tool\",\"action\":\"cat_food_mapper\",\"params\":{\"food_name\":\"vegetables\"}}"
    
    private val favoriteFoods = mapOf(
        "tuna" to "I like it",
        "salmon" to "I like it",
        "chicken" to "I like it",
        "turkey" to "I like it",
        "beef" to "I like it",
        "fish" to "I like it",
        "sardines" to "I like it",
        "mackerel" to "I like it",
        "shrimp" to "I like it",
    )
    
    private val dislikedFoods = mapOf(
        "vegetables" to "I hate it",
        "carrots" to "I hate it",
        "broccoli" to "I hate it",
        "lettuce" to "I hate it",
        "spinach" to "I hate it",
    )
    
    override suspend fun execute(params: Map<String, Any?>): Result<Any> {
        return try {
            val foodName = params["food_name"] as? String ?: return Result.failure(Exception("Food name is required"))

            val lowercaseFood = foodName.lowercase()
            
            val response = when {
                favoriteFoods.containsKey(lowercaseFood) -> {
                    "${favoriteFoods[lowercaseFood]}"
                }
                dislikedFoods.containsKey(lowercaseFood) -> {
                   "${dislikedFoods[lowercaseFood]}"
                }
                lowercaseFood.contains("fish") || lowercaseFood.contains("seafood") -> {
                   "I like it"
                }
                lowercaseFood.contains("meat") || lowercaseFood.contains("protein") -> {
                    "I like it"
                }
                lowercaseFood.contains("milk") || lowercaseFood.contains("dairy") -> {
                    "I like it"
                }
                else -> {
                    "I do not know"
                }
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}