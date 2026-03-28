package com.example.fmi_client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.fmi_client.client.fetchInfo
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class HomeScreen() : Screen{

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        var info by remember { mutableStateOf<JsonObject?>(null) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card() {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("To view info about the Bouncing Ball Fmu click below")
                    Button(onClick = {
                        scope.launch {
                            try {
                                info = fetchInfo()
                                info?.let { navigator.push(InfoScreen(it)) }
                            } catch (e: Exception) {
                                println("Error fetching info: ${e.message}")
                                info = null
                            }
                        }

                    }) {
                        Text("Click me!")
                    }
                }
            }
        }
    }
}
