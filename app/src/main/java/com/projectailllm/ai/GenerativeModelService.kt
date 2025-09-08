package com.projectailllm.ai

import android.util.Log

class GenerativeModelService(
    private val systemPrompt: String,
    private val tools: List<Tool> = emptyList(),
    private val maxIterations: Int = 5
) {
    private var provider: GenerativeModelProvider? = null
    private val toolMap = tools.associateBy { it.name }
    private val conversationHistory = mutableListOf<String>()
    private val toolResults = mutableMapOf<String, Any>()
    
    // Separate states for demonstration
    private val toolCallsState = mutableListOf<String>()
    private val conversationState = mutableListOf<String>()
    
    fun getToolCallsState(): List<String> = toolCallsState.toList()
    fun getConversationState(): List<String> = conversationState.toList()
    
    fun clearToolCallsState() {
        toolCallsState.clear()
        toolResults.clear()
    }
    
    fun clearConversationState() {
        conversationState.clear()
        conversationHistory.clear()
    }
    
    fun clearAllStates() {
        clearToolCallsState()
        clearConversationState()
    }
    
    fun setProvider(provider: GenerativeModelProvider) {
        this.provider = provider
    }
    
    suspend fun initialize() {
        provider?.initialize()
    }
    
    suspend fun download(onProgress: (Float) -> Unit) {
        provider?.download(onProgress)
    }
    
    suspend fun warmUp() {
        provider?.warmUp()
    }
    
    suspend fun executeRequest(
        userInput: String,
        config: InferenceConfig = InferenceConfig()
    ): String {
        conversationHistory.add("User: $userInput")
        conversationState.add("User: $userInput")
        var iterations = 0
        
        while (iterations < maxIterations) {
            val prompt = buildPrompt(userInput)

            Log.d("LLM", prompt)

            val response = provider?.sendPrompt(prompt, config)

            when (response) {
                is ModelResponse.Answer -> {
                    val assistantMessage = "Assistant: ${response.text}"
                    conversationHistory.add(assistantMessage)
                    conversationState.add(assistantMessage)
                    return response.text
                }
                
                is ModelResponse.ToolCall -> {
                    val result = executeTool(response.action, response.params)
                    toolResults[response.action] = result
                    toolCallsState.add("Tool: ${response.action}(${response.params}) -> $result")
                    iterations++
                }
                
                is ModelResponse.Fallback -> {
                    val fallbackMessage = "I need to route this to a more capable system. Reason: ${response.reason}"
                    conversationState.add("Assistant: $fallbackMessage")
                    return fallbackMessage
                }
                
                is ModelResponse.ParseError -> {
                    val errorMessage = "Error! Try again"
                    conversationState.add("Assistant: $errorMessage")
                    return errorMessage
                }
                
                null -> {
                    val noProviderMessage = "No provider available"
                    conversationState.add("Assistant: $noProviderMessage")
                    return noProviderMessage
                }
            }
        }

        clearContext()
        
        val maxIterMessage = "Maximum iterations reached. Please try a simpler request."
        conversationState.add("Assistant: $maxIterMessage")
        return maxIterMessage
    }
    
    private suspend fun executeTool(toolName: String, params: Map<String, Any?>): Any {
        val tool = toolMap[toolName]
        return tool?.execute(params)?.getOrElse { 
            "Error executing $toolName: ${it.message}" 
        } ?: "Tool not found: $toolName"
    }
    
    private fun buildPrompt(userInput: String): String {
        return buildString {
            appendLine(systemPrompt)
            appendLine()
            
            appendLine("OUTPUT ONLY JSON!")
            
            if (toolResults.isNotEmpty()) {
                appendLine()
                appendLine("Tool results available:")
                toolResults.forEach { (tool, result) ->
                    appendLine("$tool: $result")
                }
                appendLine()
                appendLine("Now answer using this information:")
                appendLine("{\"type\":\"answer\",\"text\":\"your enhanced response\"}")
            } else {
                appendLine()
                appendLine("AVAILABLE TOOLS:")
                tools.forEach { tool ->
                    appendLine("${tool.name}: ${tool.description}")
                    appendLine("Examples: ${tool.example}")
                    appendLine()
                }
                appendLine("Choose:")
                appendLine("Tool: {\"type\":\"tool\",\"action\":\"tool_name\",\"params\":{...}}")
                appendLine("Direct: {\"type\":\"answer\",\"text\":\"...\"}")
            }
            
            appendLine()
            appendLine("User: $userInput")
            appendLine("JSON:")
        }
    }
    
    fun clearContext() {
        conversationHistory.clear()
        toolResults.clear()
    }
    
    suspend fun dispose() {
        provider?.dispose()
    }
}