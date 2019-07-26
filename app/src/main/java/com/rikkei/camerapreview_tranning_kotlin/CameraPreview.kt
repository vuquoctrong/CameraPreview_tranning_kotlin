package com.rikkei.camerapreview_tranning_kotlin

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

@Suppress("DEPRECATION")
class CameraPreview(context: Context, private var mCamera: Camera, mSurfaceView: SurfaceView): SurfaceView(context),SurfaceHolder.Callback{
    private val TAG = CameraPreview::class.java.toString()
    private val mHolder: SurfaceHolder = mSurfaceView!!.holder.apply {
        addCallback(this@CameraPreview)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        try {
            mCamera!!.setPreviewDisplay(holder)
            mCamera!!.startPreview()
        } catch (e: Exception) {
            // intentionally left blank for a test
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mCamera!!.stopPreview()
        mCamera!!.release()

    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings

        mCamera.apply {
            try {
                setDisplayOrientation(90)
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }

}