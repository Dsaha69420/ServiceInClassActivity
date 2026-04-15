package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var startButton: Button? = null
    private var stopButton: Button? = null
    private var textView: TextView? = null

    private val defaultValue = 100

    private var timerBinder: TimerService.TimerBinder? = null
    private var isBound = false

    private val timerHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            textView?.text = msg.what.toString()
            updateButtonText()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as TimerService.TimerBinder
            isBound = true

            timerBinder?.setHandler(timerHandler)

            val savedValue = timerBinder?.getSavedPausedValue() ?: -1
            if (savedValue > 0) {
                textView?.text = savedValue.toString()
            } else {
                textView?.text = defaultValue.toString()
            }

            updateButtonText()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerBinder = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        textView = findViewById(R.id.textView)

        startButton?.setOnClickListener {
            val binder = timerBinder

            if (binder != null) {
                if (binder.isRunning) {
                    binder.pause()
                } else {
                    val savedValue = binder.getSavedPausedValue()

                    if (savedValue > 0) {
                        binder.start(savedValue)
                    } else {
                        binder.start(defaultValue)
                    }
                }

                updateButtonText()
            }
        }

        stopButton?.setOnClickListener {
            timerBinder?.stop()
            textView?.text = "0"
            updateButtonText()
        }
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, TimerService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun updateButtonText() {
        val binder = timerBinder

        if (binder == null) {
            startButton?.text = "Start"
        } else if (binder.isRunning) {
            startButton?.text = "Pause"
        } else {
            startButton?.text = "Start"
        }
    }
}