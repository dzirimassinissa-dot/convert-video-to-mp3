package com.example.convertvideotomp3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YoutubeDL.getInstance().init(application)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConvertScreen()
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ConvertScreen() {
    val context = LocalContext.current
    var url by remember { mutableStateOf(TextFieldValue("")) }
    var isConverting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (url.text.isNotEmpty()) {
                    isConverting = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = YoutubeDLRequest(url.text)
                            request.addOption("-x")
                            request.addOption("--audio-format", "mp3")
                            request.addOption("--audio-quality", "0")
                            request.addOption("-o", "%(title)s.%(ext)s")
                            YoutubeDL.getInstance().execute(request)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Conversion completed!", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isConverting = false
                            }
                        }
                    }
                }
            },
            enabled = !isConverting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isConverting) "Converting..." else "Convert to MP3")
        }
    }
}