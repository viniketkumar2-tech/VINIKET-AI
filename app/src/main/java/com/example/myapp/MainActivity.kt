
package com.example.myapp

import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
import android.net.Uri
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private var tts: TextToSpeech? = null

    private val voiceLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val words = result.data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                if (!words.isNullOrEmpty()) {
                    input.setText(words[0])
                    runCommand(words[0])
                } else {
                    say("Please try speaking again.")
                }
            }
        }

    private val contactLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val cursor = contentResolver.query(
                        uri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null, null, null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val number = it.getString(0)
                            val dialIntent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:$number")
                            )
                            startActivity(dialIntent)
                            say("Please confirm the call.")
                        } else {
                            say("No phone number found.")
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
            }
        }

        window.statusBarColor = Color.rgb(7, 20, 38)
        window.navigationBarColor = Color.rgb(7, 20, 38)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(7, 20, 38))
        }

        val title = TextView(this).apply {
            text = "VINIKET AI"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(85, 230, 255))
        }
        root.addView(title, matchWrap())

        status = TextView(this).apply {
            text = "Ready! Tap and speak."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
        root.addView(status, matchWrap())

        input = EditText(this).apply {
            hint = "Type a command..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(16, 12, 16, 12)
            background = rounded(Color.rgb(18, 48, 75))
        }
        root.addView(input, matchWrap())

        val talk = Button(this).apply {
            text = "TALK TO VINIKET"
            textSize = 16f
            setOnClickListener { listen() }
        }
        root.addView(talk, matchWrap())

        val send = Button(this).apply {
            text = "RUN COMMAND"
            setOnClickListener {
                val command = input.text.toString().trim()
                if (command.isNotEmpty()) runCommand(command)
            }
        }
        root.addView(send, matchWrap())

        setContentView(root)
    }
    
    private fun listen() {
        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "hi-IN"
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak to VINIKET AI"
                )
            }
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            say("Voice recognition is unavailable.")
        }
    }

    private fun runCommand(command: String) {
        val q = command.lowercase(Locale.ROOT)

        when {
            q.contains("call ") ||
            q.startsWith("call") ||
            q.contains("phone lagao") ||
            q.contains("call karo") -> {
                val intent = Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                )
                contactLauncher.launch(intent)
                say("Choose the contact to call.")
            }

            q.contains("bluetooth") -> {
                try {
                    startActivity(
                        Intent(
                            android.provider.Settings
                                .ACTION_BLUETOOTH_SETTINGS
                        )
                    )
                    say("Opening Bluetooth settings.")
                } catch (e: Exception) {
                    say("Unable to open Bluetooth settings.")
                }
            }

            q.contains("settings") ||
            q.contains("setting") ||
            q.contains("settings kholo") -> {
                startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                )
                say("Opening settings.")
            }

            q.contains("youtube") -> {
                openApp(
                    "com.google.android.youtube",
                    "YouTube"
                )
            }

            q.contains("whatsapp") -> {
                openApp("com.whatsapp", "WhatsApp")
            }

            q.contains("google kholo") ||
            q.contains("open google") -> {
                openWeb(
                    "https://www.google.com",
                    "Opening Google."
                )
            }

            q.contains("search") ||
            q.contains("khojo") ||
            q.contains("dhundo") ||
            q.contains("google par") -> {
                val query = q
                    .replace("google par", "")
                    .replace("google pe", "")
                    .replace("search", "")
                    .replace("khojo", "")
                    .replace("dhundo", "")
                    .trim()

                openWeb(
                    "https://www.google.com/search?q=" +
                        Uri.encode(query.ifBlank { command }),
                    "Searching Google."
                )
            }

            q.contains("time") ||
            q.contains("samay") ||
            q.contains("kitne baje") -> {
                val time = java.text.SimpleDateFormat(
                    "h:mm a", Locale.getDefault()
                ).format(java.util.Date())
                say("The time is $time")
            }

            q.contains("date") ||
            q.contains("tarikh") -> {
                val date = java.text.SimpleDateFormat(
                    "d MMMM yyyy", Locale.getDefault()
                ).format(java.util.Date())
                say("Today's date is $date")
            }

            else -> {
                openWeb(
                    "https://www.google.com/search?q=" +
                        Uri.encode(command),
                    "Searching Google."
                )
            }
        }
    }

    private fun openApp(
        packageName: String,
        appName: String
    ) {
        val intent =
            packageManager.getLaunchIntentForPackage(packageName)

        if (intent != null) {
            startActivity(intent)
            say("Opening $appName.")
        } else {
            say("$appName is not installed.")
        }
    }
    
    private fun openWeb(url: String, message: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
            say(message)
        } catch (e: Exception) {
            say("Unable to open this page.")
        }
    }

    private fun say(message: String) {
        status.text = message
        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "viniket_reply"
        )
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = 24f
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
