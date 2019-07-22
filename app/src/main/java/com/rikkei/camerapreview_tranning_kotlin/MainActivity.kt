package com.rikkei.camerapreview_tranning_kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.rikkei.tra_02t0114camera.constant.Define
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mCamera: Camera? = null
    private var mCameraPreview: CameraPreview? = null
    private var msurfaceHolder: SurfaceHolder? = null
    private var mPicture: Camera.PictureCallback? = null
    private var frontCam: Boolean = false
    private var mediaRecorder: MediaRecorder?= null
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

        ivCamera.setOnClickListener {
            mCamera?.takePicture(null,null,mPicture)
            refreshCamera()
        }

        mediaRecorder?.apply {
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
        }

        ivVideo.setOnClickListener {
            if (isRecording) {
                // stop recording and release camera
                mediaRecorder?.stop() // stop the recording
                releaseMediaRecorder() // release the MediaRecorder object
                mCamera?.lock() // take camera access back from MediaRecorder

                // inform the user that recording has stopped
               // setCaptureButtonText("Capture")
                isRecording = false
            } else {
                // initialize video camera
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder?.start()

                    // inform the user that recording has started
                  //  setCaptureButtonText("Stop")
                    isRecording = true
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder()
                    // inform user
                }
            }
        }

    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable

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


    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyCameraApp"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        Log.d("LOL","${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
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

        mCamera?.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera?.unlock()

            mediaRecorder?.run {
                setCamera(camera)

                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))

                // Step 4: Set output file
                setOutputFile(getOutputMediaFile(Define.MEDIA_TYPE_VIDEO).toString())

                // Step 5: Set the preview output
                setPreviewDisplay(mCameraPreview?.holder?.surface)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)


                // Step 6: Prepare configured MediaRecorder
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

        }
        return false
    }

    override fun onPause() {
        super.onPause()
        releaseMediaRecorder() // if you are using MediaRecorder, release it first
        releaseCamera() // release the camera immediately on pause event
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.reset() // clear recorder configuration
        mediaRecorder?.release() // release the recorder object
        mediaRecorder = null
        mCamera?.lock() // lock camera for later use
    }

    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
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