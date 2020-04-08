package com.example.screenrecoder

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import kotlinx.android.synthetic.main.activity_main.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException

class ScreenRecorder : AppCompatActivity() {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        check_permission()
        btn_submit.setOnClickListener{
            tv_message.text = "Hello, " + et_name.text.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_MEDIAPROJECTION && resultCode == Activity.RESULT_OK) {
            screenRecorder(resultCode, data)
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
    fun check_permission() {
        Log.d("test", "start check_permission")
        var permissions: MutableList<String> = arrayListOf()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        for (permission in permissions.toTypedArray()) {
            Log.d("test", permission)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
        else {
            initializeRecordButtons()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("test", "Permissions granted")
                    initializeRecordButtons()
                }else{
                    Log.d("test", "Permissions not granted")
                }
                return
            }
            else -> return
        }
    }

    private fun initializeRecordButtons() {
        Log.d("test","Permission Ready!!!")
        btn_start.setOnClickListener{
            startMediaProjection()
        }


    }

    private fun startMediaProjection() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION)
        }
    }

    private fun screenRecorder(resultCode: Int, data: Intent?) {
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
        mediaRecorder = createRecorder()
        if (mediaRecorder == null) {
            Log.d("test", "recorder is null")
        }else{
            Log.d("test", "recorder is initialized")
        }

        val callback = object: MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null

                mediaProjection?.unregisterCallback(this)
                mediaProjection = null
            }
        }

        mediaProjection?.registerCallback(callback, null)
        val displayMetrics = Resources.getSystem().displayMetrics
        mediaProjection?.createVirtualDisplay(
            "sample",
            displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null)

        btn_stop.setOnClickListener{
            mediaProjection?.stop()

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(videoFile), "video/mp4")
            startActivity(intent)
        }

        mediaRecorder?.start()
    }

    private fun createRecorder(): MediaRecorder {
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(videoFile)
        Log.d("test", videoFile)
        val displayMetrics = Resources.getSystem().displayMetrics
        recorder.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setVideoEncodingBitRate(512*1000)
        recorder.setVideoFrameRate(30)
        try{
            recorder!!.prepare()
            Log.d("test", "recorder is ready")
        } catch (e: IOException) {
            Log.d("test", "recorder has error")
            e.printStackTrace()
        }
        return recorder
    }

    companion object {
        private val videoFile:String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/MediaProjection.mp4"
        private val REQUEST_CODE_PERMISSIONS = 100
        private val REQUEST_CODE_MEDIAPROJECTION = 101
    }
}
