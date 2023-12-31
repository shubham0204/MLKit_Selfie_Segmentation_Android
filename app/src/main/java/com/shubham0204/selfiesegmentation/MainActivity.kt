/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shubham0204.selfiesegmentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ml.shubham0204.facenetdetection.ui.theme.AppTheme


class MainActivity : ComponentActivity() {

    private val cameraPermissionStatus = mutableStateOf( true )
    private val alertDialogShowStatus = mutableStateOf( false )

    private val alertDialogObjectParams = object {
        var title = ""
        var text = ""
        var positiveButtonText: String? = ""
        var negativeButtonText: String? = ""
        var positiveButtonOnClick: (() -> Unit)? = null
        var negativeButtonOnClick: (() -> Unit)? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActivityUI()
        }

        // Request the CAMERA permission as we'll require it for displaying the camera preview.
        // See https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            cameraPermissionStatus.value = true
        }

    }

    @Composable
    private fun ActivityUI() {
        AppTheme {
            Surface( modifier = Modifier
                .background(Color.White)
                .fillMaxSize() ) {
                Box {
                    Camera()
                    ShowAlertDialog()
                }
            }
        }
    }

    private fun createAlertDialog(
        dialogTitle: String,
        dialogText: String,
        dialogPositiveButtonText: String?,
        dialogNegativeButtonText: String?,
        onPositiveButtonClick: (() -> Unit)?,
        onNegativeButtonClick: (() -> Unit)?
    ) {
        alertDialogObjectParams.title = dialogTitle
        alertDialogObjectParams.text = dialogText
        alertDialogObjectParams.positiveButtonOnClick = onPositiveButtonClick
        alertDialogObjectParams.negativeButtonOnClick = onNegativeButtonClick
        alertDialogObjectParams.positiveButtonText = dialogPositiveButtonText
        alertDialogObjectParams.negativeButtonText = dialogNegativeButtonText
        alertDialogShowStatus.value = true
    }

    @Composable
    private fun ShowAlertDialog() {
        val visible by remember{ alertDialogShowStatus }
        if( visible ) {
            AlertDialog(
                title = { Text(text = alertDialogObjectParams.title) },
                text = { Text(text = alertDialogObjectParams.text)},
                onDismissRequest = { /* All alert dialogs are non-cancellable */ },
                confirmButton = {
                    if ( alertDialogObjectParams.positiveButtonText != null ) {
                        TextButton(onClick = {
                            alertDialogShowStatus.value = false
                            alertDialogObjectParams.positiveButtonOnClick?.let { it() }
                        }) {
                            Text(text = alertDialogObjectParams.positiveButtonText ?: "" )
                        }
                    }
                },
                dismissButton = {
                    if( alertDialogObjectParams.negativeButtonText != null ) {
                        TextButton(onClick = {
                            alertDialogShowStatus.value = false
                            alertDialogObjectParams.negativeButtonOnClick?.let{ it() }
                        }) {
                            Text(text = alertDialogObjectParams.negativeButtonText ?: "" )
                        }
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalGetImage::class) @Composable
    private fun Camera() {
        val cameraPermissionStatus by remember{ cameraPermissionStatus }
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        DelayedVisibility( cameraPermissionStatus ) {
            AndroidView(
                modifier = Modifier.fillMaxSize() ,
                factory = {
                    SegmentationOverlay(
                        lifecycleOwner ,
                        context
                    )
                }
            )
        }
        DelayedVisibility( !cameraPermissionStatus ) {
            Box( modifier = Modifier.fillMaxSize() ) {
                Column( modifier = Modifier.align( Alignment.Center )) {
                    Text( text = "Allow Camera Permissions" )
                    Text( text = "The app cannot work without the camera permission." )
                    Button(
                        onClick = { requestCameraPermission() } ,
                        modifier = Modifier.align( Alignment.CenterHorizontally )
                    ) {
                        Text(text = "Allow")
                    }
                }
            }
        }
    }


    @Composable
    private fun DelayedVisibility( visible: Boolean , content: @Composable (() -> Unit) ) {
        AnimatedVisibility(
            visible = visible ,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            content()
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }
    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult( ActivityResultContracts.RequestPermission() ) { isGranted ->
            if ( isGranted ) { cameraPermissionStatus.value = true }
            else {
                createAlertDialog(
                    "Camera Permission" ,
                    "The app couldn't function without the camera permission." ,
                    "ALLOW" ,
                    "CLOSE" ,
                    onPositiveButtonClick = {
                        requestCameraPermission()
                    } ,
                    onNegativeButtonClick = {
                        finish()
                    }
                )
            }
        }

}
