package com.rikkei.camerapreview_tranning_kotlin

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.rikkei.tra_02t0114camera.constant.Define

class ConfirmationDialog: DialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity)
            .setMessage(R.string.request_permission)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragment?.requestPermissions(arrayOf(Manifest.permission.CAMERA),
                    Define.MY_CAMERA_REQUEST_CODE)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                parentFragment?.activity?.finish()
            }
            .create()
 }