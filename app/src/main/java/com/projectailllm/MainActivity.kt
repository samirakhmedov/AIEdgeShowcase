package com.projectailllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.aicore.DownloadCallback
import com.projectailllm.ui.theme.ProjectAILLLMTheme


class MainActivity : ComponentActivity(), DownloadCallback {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initializeAndLoadModel()

        setContent {
            ProjectAILLLMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        val state by viewModel.pageState.collectAsStateWithLifecycle()

                        Greeting(
                            content = when (state) {
                                is MainState.Success -> (state as MainState.Success).data
                                is MainState.Error -> (state as MainState.Error).message
                                is MainState.LoadingModel -> "Loading model..." + (state as MainState.LoadingModel).progress + "%"
                                is MainState.PromptProcessing -> "Processing prompt..."
                                MainState.ModelReady -> "Model ready"
                            },
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel // Передаем ViewModel в Composable
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(content: String, modifier: Modifier = Modifier, viewModel: MainViewModel) {
    var question by remember { mutableStateOf("") }
    val toolCalls by viewModel.toolCallsState.collectAsStateWithLifecycle()
    val conversation by viewModel.conversationState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.cat),
                    contentDescription = null,
                    modifier = Modifier.size(150.dp, 150.dp)
                )
                Text(
                    text = content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
                TextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Enter your question") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.askQuestion(question) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ask Cat")
                    }
                    Button(
                        onClick = { viewModel.clearAllStates() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
        
        if (conversation.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Conversation History:")
                    }
                }
            }
            items(conversation) { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        if (toolCalls.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Tool Calls:")
                    }
                }
            }
            items(toolCalls) { toolCall ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = toolCall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjectAILLLMTheme {
        // Для Preview, ViewModel не нужна, но можно заглушить ее или передать null
        // В реальном приложении Preview не будет вызывать методы ViewModel
        Greeting("Android", viewModel = MainViewModel())
    }
}