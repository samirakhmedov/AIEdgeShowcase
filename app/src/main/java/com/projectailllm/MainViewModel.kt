package com.projectailllm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel(), DownloadCallback {
    private val _pageState = MutableStateFlow<MainState>(MainState.LoadingModel(0.0f))
    val pageState: StateFlow<MainState> = _pageState.asStateFlow()

    // Сохраним модель как свойство ViewModel
    private var generativeModel: GenerativeModel? = null

    // Шаг 1: Инициализация и загрузка модели
    fun initializeAndLoadModel() {
        if (generativeModel != null) return // Уже инициализировано

        viewModelScope.launch {
            try {
                val generationConfig = generationConfig {
                    context = MyApplication.appContext
                    temperature = 1f
                    topK = 16
                    maxOutputTokens = 4
                }
                val downloadConfig = DownloadConfig(this@MainViewModel)

                // Создание модели инициирует загрузку
                generativeModel = GenerativeModel(
                    generationConfig = generationConfig,
                    downloadConfig = downloadConfig,
                )

                generativeModel?.prepareInferenceEngine()
            } catch (e: Exception) {
                _pageState.value = MainState.Error(message = "Ошибка инициализации модели: ${e.localizedMessage}")
            }
        }
    }

    // Новый публичный метод для приема вопроса от UI
    fun askQuestion(question: String) {
        if (generativeModel == null) {
            _pageState.value = MainState.Error(message = "Модель не загружена. Пожалуйста, подождите.")
            return
        }
        generateResponse(question)
    }

    // Шаг 2: Генерация текста (теперь принимает вопрос)
    private fun generateResponse(question: String) {
        viewModelScope.launch {
            try {
                // 2.1 Устанавливаем состояние обработки. Теперь UI его точно покажет.
                _pageState.value = MainState.PromptProcessing

                // Формируем prompt с учетом пользовательского вопроса
                val input = """
                    Prompt: You are the cat. Predict a one singular response to my next request, do not proceed further. 
                    
                    "Purrr" means "Yes" or happiness, and "Grrr" means "No" or evil. If you can't answer neither, say "What?".

                    **[EXAMPLES]**
                    User: "Hey, are you there?"
                    You: "Purrr"

                    User: "Should I turn off the lights?"
                    You: "Grrrr"
                    
                    User: "What is the purpose of physics in our world?"
                    You: "What?"

                    User: "Here is a picture of a bird."
                    You: "Purrr"

                    User: "Stop notifying me."
                    You: "Grrrr"

                    User: "Do you like cats?"
                    You: "Purrr"
                    
                    User: "Let's go washing today!"
                    You: "Grrrr"
                    
                    User: "You will not have snacks today"
                    You: "Grrrr"
                    
                    User: "You will have snack today!"
                    You: "Purrrr"
                    
                    User: "Do you like dogs?"
                    You: "Grrrr"
                    
                    User: "What is the meaning of life?"
                    You: "What?"

                    User: "$question" 
                    You:
                """.trimIndent()

                var current = "";

                // 2.2 Выполняем быструю локальную операцию в IO-потоке на всякий случай
                withContext(Dispatchers.IO) {
                    generativeModel?.generateContentStream(input)?.collect { value ->
                        current += value.text ?: "";

                        // 2.3 Показываем финальный результат
                        _pageState.value = MainState.Success(data = current)
                    }
                }
            } catch (e: Exception) {
                _pageState.value = MainState.Error(message = "Ошибка генерации: ${e.localizedMessage}")
            }
        }
    }

// --- Реализация DownloadCallback ---

    override fun onDownloadCompleted() {
        super.onDownloadCompleted()
        println("Download completed")

        // Модель загружена. Обновляем состояние на ModelReady.
        _pageState.value = MainState.ModelReady
        // Убрали автоматический вызов generateProofreadText(), теперь генерация будет по запросу пользователя.
    }

    override fun onDownloadProgress(totalBytesDownloaded: Long) {
        println("Download progress. Total bytes downloaded: $totalBytesDownloaded")
        val progress = if (bytesToDownload != null && bytesToDownload!! > 0) {
            totalBytesDownloaded.toFloat() / bytesToDownload!!.toFloat()
        } else {
            0f
        }
        _pageState.value = MainState.LoadingModel(progress)
        super.onDownloadProgress(totalBytesDownloaded)
    }

    // Остальные методы колбэка без изменений...
    var bytesToDownload: Int? = null
    override fun onDownloadStarted(bytesToDownload: Long) {
        this.bytesToDownload = bytesToDownload.toInt()
        super.onDownloadStarted(bytesToDownload)
    }

    override fun onDownloadPending() {
        println("Download pending")

        super.onDownloadPending()

        _pageState.value = MainState.LoadingModel(0.0f)
    }

    override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
        println("Download failed. Failure status: $failureStatus")

        super.onDownloadFailed(failureStatus, e)

        _pageState.value = MainState.Error(message = "Download failed. Failure status: $failureStatus")
    }

    override fun onDownloadDidNotStart(e: GenerativeAIException) {
        println("Download did not start")

        super.onDownloadDidNotStart(e)

        _pageState.value = MainState.Error(message = "Download did not start")
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