package com.example.convertvideotomp3

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YoutubeDL.getInstance().init(application)
        
        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
        
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
    var progress by remember { mutableStateOf(0f) }
    var conversionComplete by remember { mutableStateOf(false) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var conversionJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("YouTube to MP3 Converter", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConverting
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        if (isConverting) {
            Text("Converting... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Convert Button
        Button(
            onClick = {
                if (url.text.isNotEmpty()) {
                    isConverting = true
                    conversionComplete = false
                    progress = 0f
                    
                    conversionJob = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                ?: File(context.cacheDir, "mp3")
                            outputDir.mkdirs()
                            
                            val request = YoutubeDLRequest(url.text)
                            request.addOption("-x")
                            request.addOption("--audio-format", "mp3")
                            request.addOption("--audio-quality", "0")
                            request.addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")
                            
                            YoutubeDL.getInstance().execute(request)
                            
                            // Find the generated file
                            val files = outputDir.listFiles { file ->
                                file.extension == "mp3"
                            }?.sortedByDescending { it.lastModified() }
                            
                            outputFile = files?.firstOrNull()
                            
                            withContext(Dispatchers.Main) {
                                if (outputFile != null) {
                                    progress = 1f
                                    conversionComplete = true
                                    Toast.makeText(context, "Conversion completed!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "File not found after conversion", Toast.LENGTH_LONG).show()
                                }
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
            enabled = !isConverting && !conversionComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Convert to MP3")
        }
        
        // Cancel Button
        if (isConverting) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    conversionJob?.cancel()
                    isConverting = false
                    progress = 0f
                    Toast.makeText(context, "Conversion cancelled", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel")
            }
        }
        
        // Download Button (shown after conversion)
        if (conversionComplete && outputFile != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("File ready: ${outputFile!!.name}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            Toast.makeText(context, "File saved to: ${outputFile!!.absolutePath}", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download MP3 (Saved)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            url = TextFieldValue("")
                            isConverting = false
                            conversionComplete = false
                            progress = 0f
                            outputFile = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Convert Another")
                    }
                }
            }
        }
    }
}