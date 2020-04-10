package com.example.screenrecoder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Recorder {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoFileName: String? = null
    private var STATE: State = State.NONE

    fun init(){
        val savedPath = File(videoFilePath)
        if (!savedPath.exists()) {
            savedPath.mkdir()
        }

        STATE = State.INIT
    }

    fun makeRecordingModules(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && (STATE == State.INIT || STATE == State.RECORDED)) {
            mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activity.startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION)
        }
        else{
            Log.d("test", String.format("Recorder instance is not ready!!! : %s", STATE))
        }
    }

    fun check_permission(activity: Activity) : Boolean {
        Log.d("test", "start check_permission")
        var permissions: MutableList<String> = arrayListOf()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        for (permission in permissions.toTypedArray()) {
            Log.d("test", permission)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
            return false
        }
        else {
            return true
        }
    }

    fun stopRecording(){
        if (STATE == State.RECORDING) {
            mediaProjection?.stop()
            STATE = State.RECORDED
        }
        else
            Log.d("test", String.format("Recorder instance does not recording now. : %s", STATE))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecording(resultCode: Int, data: Intent?) {
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
        mediaRecorder = createInnerRecorder() ?: throw NullPointerException("Recorder is null")

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
        mediaRecorder?.start()
        STATE = State.RECORDING

    }

    fun play(activity: Activity) {
        if (STATE != State.RECORDED) {
            Log.d("test", String.format("Recorder has not recorded video. : %s", STATE))
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(videoFileName), "video/mp4")
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createInnerRecorder(): MediaRecorder? {
        val currentTime: LocalDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        Log.d("test", currentTime.format(formatter))
        videoFileName = videoFilePath + currentTime.format(formatter) + ".mp4"
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        recorder.setOutputFile(videoFileName)
        Log.d("test", videoFileName)
        val displayMetrics = Resources.getSystem().displayMetrics
        recorder.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
        try{
            recorder.prepare()
            Log.d("test", "recorder is ready")
        } catch(e: IOException) {
            Log.d("test", "recorder has error")
            e.printStackTrace()
        }
        return recorder
    }

    companion object {
        val videoFilePath:String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/MediaProjection/"
        val REQUEST_CODE_PERMISSIONS = 100
        val REQUEST_CODE_MEDIAPROJECTION = 101
    }
}