package com.github.soundtricker.androidthings.analogdigitalconvert

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.things.contrib.driver.pwmservo.Servo
import java.io.IOException


class MainActivity : Activity() {


    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val PIN_CS = "BCM25"
        private val PIN_CLOCK = "BCM17"
        private val PIN_MOS_IN = "BCM24"
        private val PIN_MOS_OUT = "BCM23"
        private val PIN_SERVO = "PWM1"

        private val CHANNEL_POTENTIONMETER = 0x0
        private val MIN_VOL = 0
        private val MAX_VOL = 410
        private val MIN_ANGLE = 0
        private val MAX_ANGLE = 180

    }

    private lateinit var mcp3008: MCP3008
    private lateinit var mcpHandler: Handler

    private lateinit var servo: Servo

    private val readChannelMoveServo = Runnable {
        readAndMoveServo(CHANNEL_POTENTIONMETER)
    }

    private fun readAndMoveServo(readChannel: Int) {
        val vol = this.mcp3008.readAdc(readChannel)
        Log.i(TAG, "ADC $readChannel: $vol")

        val angle = map(vol, MIN_VOL, MAX_VOL, MIN_ANGLE, MAX_ANGLE)
        val prevAngle = this.servo.angle

        if (Math.abs(prevAngle - angle) <= 3) {
            return
        }
        Log.i(TAG, "ANGLE $readChannel: $angle")
        this.servo.angle = angle
        runReadChannel()
    }


    private fun runReadChannel() {
        this.mcpHandler.postDelayed(this.readChannelMoveServo, 10L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            this.servo = Servo(PIN_SERVO)
            this.servo.setPulseDurationRange(0.6, 2.35)
        } catch (e: IOException) {
            Log.e(TAG, "servo can't initialized", e)
            return
        }
        this.mcp3008 = MCP3008(PIN_CS, PIN_CLOCK, PIN_MOS_IN, PIN_MOS_OUT)
        try {
            this.mcp3008.register()

        } catch (e: IOException) {
            Log.e(TAG, "mcp can't initialized", e)
            return
        }
        mcpHandler = Handler()
        mcpHandler.post(this.readChannelMoveServo)


    }

    override fun onDestroy() {
        super.onDestroy()
        this.mcp3008.unregister()
        this.servo.close()
    }

    private fun map(x: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Double {
        return ((x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin).toDouble()
    }


}
