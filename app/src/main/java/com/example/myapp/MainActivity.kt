
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
import android.net.Uriu
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

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
                    answer(words[0])
                } else {
                    status.text = "I couldn't hear that. Try again."
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (ttsReady) tts.language = Locale("en", "IN")
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
            setTextColor(Color.rgb(85, 230, 255))
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL VOICE ASSISTANT"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 28)
        }
        root.addView(subtitle, matchWrap())

        val orb = TextView(this).apply {
            text = "◉"
            textSize = 76f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(53, 217, 255))
        }
        root.addView(orb, matchWrap())

        status = TextView(this).apply {
            text = "Ready! Tap the microphone and speak."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 20)
        }
        root.addView(status, matchWrap())

        input = EditText(this).apply {
            hint = "Type your question..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(16, 12, 16, 12)
            background = rounded(Color.rgb(18, 48, 75))
        }
        root.addView(input, matchWrap())

        val talk = Button(this).apply {
            text = "🎙  TALK TO VINIKET"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(23, 106, 145))
            setOnClickListener { listen() }
        }
        val talkParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        talkParams.setMargins(0, 16, 0, 8)
        root.addView(talk, talkParams)

        val send = Button(this).apply {
            text = "SEND QUESTION"
            setOnClickListener {
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) answer(question)
            }
        }
        root.addView(send, matchWrap())

        setContentView(root)
    }

    private fun listen() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to VINIKET AI")
            }
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            status.text = "Voice recognition is unavailable on this phone."
        }
    }

    private fun answer(text: String) {
        val q = text.lowercase(Locale.ROOT)
        val reply = when {
            "hello" in q || "hi" in q ->
                "Hello! I am VINIKET AI. How can I help you?"

            "your name" in q || "w
            ho are you" in q ->
    "I am VINIKET AI, your personal voice assistant."

"time" in q ->
    "The time is " +
        java.text.SimpleDateFormat(
            "h:mm a", Locale.getDefault()
        ).format(java.util.Date())

"date" in q ->
    "Today is " +
        java.text.SimpleDateFormat(
            "EEEE, d MMMM yyyy", Locale.getDefault()
        ).format(java.util.Date())

"open youtube" in q -> {
    openWeb("https://www.youtube.com")
    "Opening YouTube."
}

"open google" in q -> {
    openWeb("https://www.google.com")
    "Opening Google."
}

else -> {
    status.text = "Searching the web for: $text"
    openWeb(
        "https://www.google.com/search?q=" + Uri.encode(text)
    )
    "I am searching the web for your question."
}
}

status.text = reply
if (ttsReady) {
    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "viniket_reply")
}
}

private fun openWeb(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        status.text = "Unable to open the website."
    }
}

private fun matchWrap(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

private fun rounded(color: Int) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = 24f
}

override fun onDestroy() {
    if (::tts.isInitialized) {
        tts.stop()
        tts.shutdown()
    }
    super.onDestroy()
}
}
