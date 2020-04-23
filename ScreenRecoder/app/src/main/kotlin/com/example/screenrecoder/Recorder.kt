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

object Recorder {

    val REQUEST_CODE_PERMISSIONS = 100
    val REQUEST_CODE_MEDIAPROJECTION = 101

    private val videoFilePath:String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/MediaProjection/"
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoFileName: String? = null
    private var STATE: State = State.NONE

    private var currentActivity: Activity? = null

    public var resultCode:Int? = null
    public var data:Intent? = null

    fun init(){
        Log.d("ScreenRecorder::Init", "Start Init")
        val savedPath = File(videoFilePath)
        if (!savedPath.exists()) {
            savedPath.mkdir()
        }

        STATE = State.INIT
        Log.d("ScreenRecorder::Init", "3")
        createMPMInstance()
    }


    fun check_permission(activity: Activity) : Boolean {
        Log.d("ScreenRecorder::check_permission", "start check_permission")
        currentActivity = activity

        var permissions: MutableList<String> = arrayListOf()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        for (permission in permissions.toTypedArray()) {
            Log.d("ScreenRecorder::check_permission", permission)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
            return false
        }
        else {
            return true
        }
    }

    fun createMPMInstance() {
        Log.d("ScreenRecorder::createMPMInstance", "Start createMPMInstnace")
        if (STATE != State.INIT && STATE != State.RECORDED && STATE != State.NONE) {
            Log.d("ScreenRecorder::createMPMInstance", String.format("Recorder instance is invalid status : %s", STATE))
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && mediaProjectionManager == null) {
            mediaProjectionManager = currentActivity!!.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            currentActivity!!.startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION)
        }
        else{
            makeModules()
            reset()
            STATE = State.READY
            Log.d("ScreenRecorder::createMPMInstance", String.format("Recorder instance is ready!!! : %s", STATE))
        }
    }

    fun onFinshedMPMCreate(resultCode:Int, data: Intent) {
        Log.d("ScreenRecorder::onFinishedMPMCreate", "Start onFinishedMPMCreate")
        this.resultCode = resultCode
        this.data = data
        makeModules()
        reset()
        STATE = State.READY
    }

    fun makeModules() {
        Log.d("ScreenRecorder::makeModules", "Start makeModules")
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode!!, data!!)
        if (mediaRecorder == null)
            mediaRecorder = MediaRecorder()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reset() {
        Log.d("ScreenRecorder::reset", "Start reset")
        val currentTime: LocalDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        Log.d("ScreenRecorder::reset", currentTime.format(formatter))
        videoFileName = videoFilePath + currentTime.format(formatter) + ".mp4"
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        mediaRecorder!!.setOutputFile(videoFileName)
        Log.d("ScreenRecorder::reset", videoFileName)
        val displayMetrics = Resources.getSystem().displayMetrics
        mediaRecorder!!.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
        try{
            mediaRecorder!!.prepare()
            Log.d("ScreenRecorder::reset", "recorder is ready")
        } catch(e: IOException) {
            Log.d("ScreenRecorder::reset", "recorder has error")
            e.printStackTrace()
        }

        val callback = object: MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaProjection?.unregisterCallback(this)
                mediaProjection = null
                Log.d("ScreenRecorder::callback", "1")
                init()
            }
        }

        mediaProjection?.registerCallback(callback, null)
        mediaProjection?.createVirtualDisplay(
            "sample",
            displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null)
    }

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecording() {
        Log.d("ScreenRecorder::startRecording", "Start Recording")
        if (STATE != State.READY) {
            Log.d("ScreenRecorder::startRecording", String.format("Recorder Instance is invalid state : %s", STATE))
            return
        }
        mediaRecorder?.start()
        STATE = State.RECORDING

    }

    @JvmStatic
    fun stopRecording(){
        Log.d("ScreenRecorder::stopRecording", "Start stopReording")
        if (STATE == State.RECORDING) {
            mediaProjection?.stop()
            STATE = State.RECORDED
            Log.d("ScreenRecorder::stopRecording", "2")
        }
        else
            Log.d("ScreenRecorder::stopRecording", String.format("Recorder instance does not recording now. : %s", STATE))
    }

    @JvmStatic
    fun play() {
        if (STATE != State.RECORDING && STATE != State.INIT) {
            Log.d("ScreenRecorder::play", String.format("Recorder has not recorded video. : %s", STATE))
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(videoFileName), "video/mp4")
        try {
            currentActivity!!.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}