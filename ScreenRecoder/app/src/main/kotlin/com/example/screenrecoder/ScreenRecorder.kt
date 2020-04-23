package com.example.screenrecoder

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class ScreenRecorder : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_popupmessage.text = "Welcome to screen recorder"
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv_settingTime.text = String.format("Setting Time : %d (s)", seekBar!!.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.d("ScreenRecorder", "start time setting")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d("ScreenRecorder", "end time setting")
            }
        })

        if (Recorder.check_permission(this)!!) {
            Recorder.init()
            initializeRecordButtons()
            tv_popupmessage.text = "Recorder is ready!!!"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Recorder.REQUEST_CODE_MEDIAPROJECTION && resultCode == Activity.RESULT_OK) {
            Recorder.onFinshedMPMCreate(resultCode, data!!)
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            Recorder.REQUEST_CODE_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("ScreenRecorder", "Permissions granted")
                    Recorder.init()
                    initializeRecordButtons()
                }else{
                    Log.d("ScreenRecorder", "Permissions not granted")
                }
                return
            }
            else -> return
        }
    }

    private fun initializeRecordButtons() {
        Log.d("ScreenRecorder","Permission Ready!!!")
        btn_start.setOnClickListener{
            tv_popupmessage.text = "Start Recording"
            Recorder.startRecording()
            if (sb.progress != 0) {
                Log.d("test", sb.progress.toString())
                Handler().postDelayed({
                    Recorder.stopRecording()
                    tv_popupmessage.text = "Finished Recording"
                }, (sb.progress*1000).toLong())

            }
        }

        btn_stop.setOnClickListener{
            Recorder.stopRecording()
            tv_popupmessage.text = "Finished Recording"
        }

        btn_play.setOnClickListener{
            Recorder.play()
        }
    }
}
