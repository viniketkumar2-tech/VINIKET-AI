import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.media.AudioManager
import android.content.Context
import android.os.BatteryManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var answer: TextView
    private lateinit var mic: Button
    private lateinit var tts: TextToSpeech

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var waitingForAI = false

    // AI chalane ke liye apne backend ka URL yahan lagana.
    // API key ko app ke andar hardcode mat karna.
    private val AI_ENDPOINT = "https://YOUR-SERVER/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(5, 12, 30)
        window.navigationBarColor = Color.rgb(5, 12, 30)

        tts = TextToSpeech(this, this)
        buildScreen()

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(speechListener)
        } else {
            show("Is phone par speech recognition available nahi hai.")
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 101
            )
        }
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 30, 24, 24)
            setBackgroundColor(Color.rgb(5, 12, 30))
        }

        val title = TextView(this).apply {
            text = "N O V A"
            textSize = 34f
            setTextColor(Color.rgb(60, 200, 255))
            gravity = Gravity.CENTER
        }
        root.addView(title)

        status = TextView(this).apply {
            text = "Your voice assistant"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(status)

        val orb = TextView(this).apply {
            text = "◉"
            textSize = 100f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(40, 170, 255))
            setPadding(0, 25, 0, 25)
        }
        root.addView(orb)

        answer = TextView(this).apply {
            text = "Hello! Main Nova hoon. Mujhse baat karo."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 25)
        }
        root.addView(answer)

        mic = Button(this).apply {
            text = "🎙 START LISTENING"
            textSize = 16f
            setOnClickListener {
                if (listening) {
                    stopListening()
                } else {
                    startListening()
                }
            }
        }
        root.addView(mic)

        setContentView(root)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            tts.language = Locale("en", "IN")
            tts.setSpeechRate(0.9f)
        }
    }

    private fun show(message: String) {
        runOnUiThread {
            answer.text = message
        }
    }

    private fun speak(message: String) {
        show(message)
        if (::tts.isInitialized) {
            tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "nova_reply"
            )
        }
    }
        private val speechListener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            status.text = "Nova sun raha hai..."
        }

        override fun onBeginningOfSpeech() {
            status.text = "Listening..."
        }

        override fun onEndOfSpeech() {
            status.text = "Processing..."
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                handleCommand(text)
            }

            if (listening) {
                android.os.Handler(mainLooper).postDelayed({
                    if (listening && !waitingForAI) startListening()
                }, 1000)
            }
        }

        override fun onError(error: Int) {
            if (listening) {
                android.os.Handler(mainLooper).postDelayed({
                    if (listening && !waitingForAI) startListening()
                }, 1200)
            } else {
                status.text = "Tap Start Listening"
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), 101
            )
            return
        }

        if (waitingForAI) return

        listening = true
        mic.text = "■ STOP LISTENING"
        status.text = "Starting microphone..."

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            status.text = "Voice recognition start nahi hua."
        }
    }

    private fun stopListening() {
        listening = false
        try {
            recognizer?.cancel()
        } catch (_: Exception) {}

        mic.text = "🎙 START LISTENING"
        status.text = "Nova is ready"
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim().lowercase(Locale.ROOT)
        val command = text.removePrefix("hey nova")
            .removePrefix("okay nova")
            .trim()
            .trim(',', '.', ' ')

        if (command.isBlank() &&
            (text.contains("hey nova") || text.contains("okay nova"))) {
            speak("Yes, I'm listening. Bolo!")
            return
        }

        when {
            command in listOf("hello", "hello nova", "hi", "hi nova") -> {
                speak("Hello! Main Nova hoon. Batao, main kya karun?")
            }

            command.contains("what time") ||
            command == "time" ||
            command.contains("time kya") -> {
                val now = java.text.SimpleDateFormat(
                    "hh:mm a", Locale.getDefault()
                ).format(java.util.Date())
                speak("Abhi time hai $now")
            }

            command.contains("today's date") ||
            command.contains("what is the date") ||
            command == "date" ||
            command.contains("aaj ki date") -> {
                val today = java.text.SimpleDateFormat(
                    "dd MMMM yyyy", Locale.getDefault()
                ).format(java.util.Date())
                speak("Aaj ki date hai $today")
            }

            command.contains("battery") -> {
                val manager =
                    getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = manager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )
                speak("Battery $level percent hai.")
            }

            command.contains("open youtube") ||
            command == "youtube kholo" -> {
                openApp(
                    "com.google.android.youtube",
                    "https://www.youtube.com"
                )
            }

            command.contains("open chrome") -> {
                openApp(
                    "com.android.chrome",
                    "https://www.google.com"
                )
            }

            command.contains("open settings") ||
            command.contains("settings kholo") -> {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                    speak("Settings khol raha hoon.")
                } catch (_: Exception) {
                    speak("Settings nahi khul paayi.")
                }
            }

            command.startsWith("search for ") ||
            command.startsWith("google par search karo ") ||
            command.startsWith("search google for ") -> {
                val query = when {
                    command.startsWith("search for ") ->
                        command.removePrefix("search for ")
                    command.startsWith("google par search karo ") ->
                        command.removePrefix("google par search karo ")
                    else -> command.removePrefix("search google for ")
                }.trim()

                if (query.isNotBlank()) {
                    val url = "https://www.google.com/search?q=" +
                        Uri.encode(query)
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    speak("Google par search kar raha hoon.")
                } else {
                    speak("Batao kya search karna hai.")
                }
            }

            command.startsWith("open ") -> {
                speak(
                    "Is app ke liye abhi command add nahi hai. " +
                    "App ka naam batao, ya AI se poochho."
                )
            }

            else -> {
                askAI(raw)
            }
        }
    }

    private fun openApp(packageName: String, fallbackUrl: String) {
        try {
            val launch = packageManager
                .getLaunchIntentForPackage(packageName)

            if (launch != null) {
                startActivity(launch)
                speak("Opening app.")
            } else {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                )
                speak("App nahi mila, website khol raha hoon.")
            }
        } catch (_: Exception) {
            speak("App open nahi ho paaya.")
        }
    }
        private fun askAI(question: String) {
        if (AI_ENDPOINT.contains("YOUR-SERVER")) {
            speak(
                "AI abhi connect nahi hai. " +
                "Pehle Nova ka AI server setup karna hoga."
            )
            return
        }

        waitingForAI = true
        status.text = "Nova is thinking..."

        thread {
            var reply: String? = null

            try {
                val connection =
                    URL(AI_ENDPOINT).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )

                val body = JSONObject()
                    .put("message", question)
                    .toString()

                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }

                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                }

                if (connection.responseCode in 200..299 &&
                    !response.isNullOrBlank()) {
                    reply = JSONObject(response).optString("reply")
                }

                connection.disconnect()

            } catch (_: Exception) {
                reply = null
            }

            runOnUiThread {
                waitingForAI = false

                if (!reply.isNullOrBlank()) {
                    speak(reply!!)
                } else {
                    speak(
                        "AI se connection nahi ho paaya. " +
                        "Internet aur server settings check karo."
                    )
                }

                if (listening) {
                    android.os.Handler(mainLooper).postDelayed({
                        if (listening && !waitingForAI) startListening()
                    }, 1500)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speak("Microphone permission mil gayi. Ready hoon!")
        } else if (requestCode == 101) {
            show("Nova chalane ke liye microphone permission do.")
        }
    }

    override fun onDestroy() {
        listening = false

        recognizer?.destroy()
        recognizer = null

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}
