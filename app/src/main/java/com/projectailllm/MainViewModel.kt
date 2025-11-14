package com.projectailllm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectailllm.ai.GenerativeModelService
import com.projectailllm.ai.InferenceConfig
import com.projectailllm.ai.providers.MlKitPromptProvider
import com.projectailllm.ai.tools.CatFoodTool
import com.projectailllm.ai.tools.GreetingTool
import com.projectailllm.ai.tools.MoodTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _pageState = MutableStateFlow<MainState>(MainState.LoadingModel(0.0f))
    val pageState: StateFlow<MainState> = _pageState.asStateFlow()

    private val _toolCallsState = MutableStateFlow<List<String>>(emptyList())
    val toolCallsState: StateFlow<List<String>> = _toolCallsState.asStateFlow()

    private val _conversationState = MutableStateFlow<List<String>>(emptyList())
    val conversationState: StateFlow<List<String>> = _conversationState.asStateFlow()

    private lateinit var modelService: GenerativeModelService

    fun initializeAndLoadModel() {
        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are a cat assistant. You are just here to make user happy and not alone. Your name is Jim, you really love to cuddle and play games!
                """.trimIndent()

                val tools = listOf(
                    GreetingTool(),
                    MoodTool(),
                    CatFoodTool()
                )

                modelService = GenerativeModelService(systemPrompt, tools)
                val provider = MlKitPromptProvider()
                modelService.setProvider(provider)
                
                modelService.initialize()
                modelService.download { progress ->
                    _pageState.value = MainState.LoadingModel(progress)
                }
                modelService.warmUp()
                _pageState.value = MainState.ModelReady
            } catch (e: Exception) {
                _pageState.value = MainState.Error(message = "Ошибка инициализации модели: ${e.localizedMessage}")
            }
        }
    }

    fun askQuestion(question: String) {
        if (!::modelService.isInitialized) {
            _pageState.value = MainState.Error(message = "Модель не загружена. Пожалуйста, подождите.")
            return
        }
        
        viewModelScope.launch {
            try {
                _pageState.value = MainState.PromptProcessing
                
                val config = InferenceConfig(
                    temperature = 0.8f,
                    maxOutputTokens = 100,
                    topK = 16
                )
                
                val response = modelService.executeRequest(question, config)
                _pageState.value = MainState.Success(data = response)
                
                // Update state variables
                _toolCallsState.value = modelService.getToolCallsState()
                _conversationState.value = modelService.getConversationState()
            } catch (e: Exception) {
                _pageState.value = MainState.Error(message = "Ошибка генерации: ${e.localizedMessage}")
            }
        }
    }

    fun clearAllStates() {
        if (::modelService.isInitialized) {
            modelService.clearAllStates()
            _toolCallsState.value = modelService.getToolCallsState()
            _conversationState.value = modelService.getConversationState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::modelService.isInitialized) {
            viewModelScope.launch {
                modelService.clearAllStates()
                modelService.dispose()
            }
        }
    }
}

sealed class MainState {
    data class Success(val data: String) : MainState()
    data class Error(val message: String) : MainState()
    data class LoadingModel(val progress: Float) : MainState()

    // Новое состояние
    data object ModelReady : MainState()
    data object PromptProcessing : MainState()
}