package edu.temple.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit

class TimerService : Service() {

    private var isRunning = false
    private var paused = false
    private var currentValue = 0

    private var timerHandler: Handler? = null
    private var timerThread: TimerThread? = null

    private val preferences by lazy {
        getSharedPreferences("timer_pref", Context.MODE_PRIVATE)
    }

    inner class TimerBinder : Binder() {

        val isRunning: Boolean
            get() = this@TimerService.isRunning

        val paused: Boolean
            get() = this@TimerService.paused

        fun setHandler(handler: Handler) {
            timerHandler = handler
        }

        fun start(startValue: Int) {
            this@TimerService.startTimer(startValue)
        }

        fun pause() {
            this@TimerService.pauseTimer()
        }

        fun stop() {
            this@TimerService.stopTimer(true)
        }

        fun getSavedPausedValue(): Int {
            return preferences.getInt("paused_value", -1)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TimerService", "Created")
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }

    private fun startTimer(startValue: Int) {
        timerThread?.interrupt()

        paused = false
        isRunning = true
        currentValue = startValue

        timerThread = TimerThread(startValue)
        timerThread?.start()
    }

    private fun pauseTimer() {
        if (isRunning) {
            paused = true
            isRunning = false

            preferences.edit {
                putInt("paused_value", currentValue)
            }

            Log.d("TimerService", "Paused at $currentValue")
        }
    }

    private fun stopTimer(clearSavedValue: Boolean) {
        paused = false
        isRunning = false

        timerThread?.interrupt()
        timerThread = null

        currentValue = 0
        timerHandler?.sendEmptyMessage(0)

        if (clearSavedValue) {
            preferences.edit {
                remove("paused_value")
            }
        }

        Log.d("TimerService", "Stopped")
    }

    inner class TimerThread(private val startValue: Int) : Thread() {

        override fun run() {
            try {
                for (i in startValue downTo 0) {

                    currentValue = i
                    timerHandler?.sendEmptyMessage(i)
                    Log.d("Countdown", i.toString())

                    if (i == 0) {
                        break
                    }

                    while (paused && !isInterrupted) {
                        sleep(200)
                    }

                    if (isInterrupted) {
                        break
                    }

                    sleep(1000)
                }

                if (!paused && currentValue == 0) {
                    preferences.edit {
                        remove("paused_value")
                    }
                }

            } catch (e: InterruptedException) {
                Log.d("TimerService", "Timer interrupted")
            } finally {
                if (!paused) {
                    isRunning = false
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (paused) {
            timerThread?.interrupt()
            timerThread = null
            isRunning = false
        } else {
            stopTimer(true)
        }

        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerService", "Destroyed")
    }
}