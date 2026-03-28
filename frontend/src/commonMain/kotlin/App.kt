import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.example.fmi_client.screens.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("Hello from Compose Web!")
            Button(onClick = { println("clicked") }) {
                Text("Click me")
            }
        }
    }
}

@Composable
fun App2() {
    MaterialTheme {
        Navigator(HomeScreen())
    }
}
