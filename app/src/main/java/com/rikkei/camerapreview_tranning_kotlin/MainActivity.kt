@file:Suppress("DEPRECATION")

package com.rikkei.camerapreview_tranning_kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.rikkei.tra_02t0114camera.constant.Define
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_setting_previewsize.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Suppress("DEPRECATION", "PrivatePropertyName", "SpellCheckingInspection")
class MainActivity : AppCompatActivity() {

    private var mCamera: Camera? = null
    private var mCameraPreview: CameraPreview? = null
    private var msurfaceHolder: SurfaceHolder? = null
    private var mPicture: Camera.PictureCallback? = null
    private var frontCam: Boolean = false
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private val TAG = MainActivity::class.java.toString()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        msurfaceHolder = surfaceView!!.holder
        mCamera = getCameraInstance()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                Define.MY_CAMERA_REQUEST_CODE
            )
        } else {

        }
        mCameraPreview = mCamera?.let {
            CameraPreview(this, it, surfaceView)
        }
        //đổi cam trước sau
        ivRotateCamera.setOnClickListener { roTateCamera() }

        mPicture = Camera.PictureCallback { data, _ ->
            val pictureFile: File = getOutputMediaFile(Define.MEDIA_TYPE_IMAGE) ?: run {
                Log.d(TAG, ("Error creating media file, check storage permissions"))
                return@PictureCallback
            }
            try {
                val fos = FileOutputStream(pictureFile)
                fos.write(data)
                fos.close()
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "File not found: ${e.message}")
            } catch (e: IOException) {
                Log.d(TAG, "Error accessing file: ${e.message}")
            }
        }
        // saveImage
        ivCamera.setOnClickListener {
            mCamera?.takePicture(null, null, mPicture)
            refreshCamera()
        }

        // open video
        ivVideo.setBackgroundResource(R.drawable.video64)
        ivVideo.setOnClickListener {
            if (isRecording) {
                it.setBackgroundResource(R.drawable.pausevideo)
                mediaRecorder?.stop()
                releaseMediaRecorder()
                mCamera?.lock()
                isRecording = false
            } else {
                if (prepareVideoRecorder()) {
                    it.setBackgroundResource(R.drawable.video64)
                    mediaRecorder?.start()
                    it.setBackgroundResource(R.drawable.pausevideo)
                    isRecording = true
                } else {
                    releaseMediaRecorder()
                }
            }
        }

        // preview size camera
        ivSetting.setOnClickListener {
            showDialogSettingSize()
        }

    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open()
        } catch (e: Exception) {
            null
        }
    }


    private fun roTateCamera() {
        if (frontCam) {
            val cameraId = isBackCameraExisted()
            if (cameraId >= 0) {
                try {
                    mCamera!!.stopPreview()
                    mCamera!!.release()

                    mCamera = Camera.open(cameraId)
                    mCamera!!.setPreviewDisplay(msurfaceHolder)
                    mCamera!!.startPreview()

                    frontCam = false

                    changeOrientation()
                } catch (e: RuntimeException) {
                } catch (e: Exception) {
                }

                val param = mCamera!!.parameters

                param.setPreviewSize(surfaceView!!.width, surfaceView!!.height)
                param.previewFrameRate = 50
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
        } else {
            val cameraId = isFrontCameraExisted()
            if (cameraId >= 0) {
                try {
                    mCamera!!.stopPreview()
                    mCamera!!.release()

                    mCamera = Camera.open(cameraId)
                    mCamera!!.setPreviewDisplay(msurfaceHolder)
                    mCamera!!.startPreview()

                    frontCam = true

                    changeOrientation()
                } catch (e: RuntimeException) {
                } catch (e: Exception) {
                }

                val param = mCamera!!.parameters

                param.setPreviewSize(surfaceView!!.width, surfaceView!!.height)
                param.previewFrameRate = 30
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
        }
    }


    private fun isBackCameraExisted(): Int {
        var cameraId = -1
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i
                break
            }
        }
        return cameraId
    }

    private fun changeOrientation() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            mCamera!!.setDisplayOrientation(0)
        else
            mCamera!!.setDisplayOrientation(90)
    }

    private fun isFrontCameraExisted(): Int {
        var cameraId = -1
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i
                break
            }
        }
        return cameraId
    }

    private fun refreshCamera() {
        if (msurfaceHolder!!.surface == null) return

        try {
            mCamera!!.stopPreview()
        } catch (e: Exception) {
        }

        try {
            mCamera!!.setPreviewDisplay(msurfaceHolder)
            mCamera!!.startPreview()
        } catch (e: Exception) {
        }

    }


    @SuppressLint("SimpleDateFormat")
    private fun getOutputMediaFile(type: Int): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyCameraApp"
        )

        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        Log.d(TAG, "${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
        return when (type) {
            Define.MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            Define.MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
    }

    private fun prepareVideoRecorder(): Boolean {
        mediaRecorder = MediaRecorder()
        mCamera?.unlock()
        mediaRecorder?.run {
            setCamera(mCamera)
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(getOutputMediaFile(Define.MEDIA_TYPE_VIDEO).toString())
            setVideoSize(640, 480)
            setVideoFrameRate(30)
            setPreviewDisplay(mCameraPreview?.holder?.surface)
//            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
//            setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
            return try {
                prepare()
                true
            } catch (e: IllegalStateException) {
                Log.d(TAG, "IllegalStateException preparing MediaRecorder: ${e.message}")
                releaseMediaRecorder()
                false
            } catch (e: IOException) {
                Log.d(TAG, "IOException preparing MediaRecorder: ${e.message}")
                releaseMediaRecorder()
                false
            }
        }
        return false
    }

    //set preivew size
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getOptimalPreviewSize(sizes: MutableList<Camera.Size>, w: Int, h: Int): Camera.Size? {
        val targetRatio = w.toDouble() / h
        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) > Define.ASPECT_TOLERANCE) continue
            if (abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setPreviewSize(width: Int, height: Int) {
        val parameters = mCamera?.parameters
        val sizes = parameters?.supportedPreviewSizes
        val optimalSize = getOptimalPreviewSize(sizes as MutableList<Camera.Size>, width, height)
        if (optimalSize != null) {
            parameters.setPreviewSize(optimalSize.width, optimalSize.height)
        }
        mCamera?.parameters = parameters
        mCamera?.startPreview()

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showDialogSettingSize() {
        val dialog = Dialog(this)
        dialog.apply {
            setCancelable(true)
            setContentView(R.layout.dialog_setting_previewsize)
            show()
        }
        var width: Int
        var height: Int
        dialog.tv1920x1280.setOnClickListener {
            width = 1280
            height = 1920
            setPreviewSize(width, height)
            dialog.dismiss()
        }
        dialog.tv1280x960.setOnClickListener {
            width = 960
            height = 1280
            setPreviewSize(width, height)
            dialog.dismiss()
        }
        dialog.tv640x320.setOnClickListener {
            width = 320
            height = 640
            setPreviewSize(width, height)
            dialog.dismiss()
        }

    }

    override fun onPause() {
        super.onPause()
        releaseMediaRecorder()
        releaseCamera()
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        mCamera?.lock()
    }

    private fun releaseCamera() {
        mCamera?.release()
        mCamera = null
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        changeOrientation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Define.MY_CAMERA_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show()
            }
            else -> {
            }
        }
    }
}