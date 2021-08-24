package com.shubham0204.selfiesegmentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.ml.shubham0204.ocms.FrameAnalyser
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

import android.graphics.Bitmap
import android.renderscript.*


class MainActivity : AppCompatActivity() {

    private lateinit var previewView : PreviewView
    private lateinit var drawingOverlay: DrawingOverlay
    private lateinit var cameraProviderListenableFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var frameAnalyser : FrameAnalyser

    private val CAMERA_PERMISSION_REQUESTCODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove the status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_main)

        previewView = findViewById( R.id.camera_preview_view )
        drawingOverlay = findViewById( R.id.camera_drawing_overlay )
        drawingOverlay.setWillNotDraw(false)
        drawingOverlay.setZOrderOnTop(true)
        frameAnalyser = FrameAnalyser( drawingOverlay )



        if (ActivityCompat.checkSelfPermission( this , Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        }
        else {
            setupCameraProvider()
        }


    }

    private fun requestCameraPermission() {
        requestCameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission() ) {
            isGranted : Boolean ->
        if ( isGranted ) {
            setupCameraProvider()
        }
        else {
            val alertDialog = AlertDialog.Builder( this ).apply {
                setTitle( "Permissions" )
                setMessage( "The app requires the camera permission to function." )
                setPositiveButton( "GRANT") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraPermission()
                }
                setNegativeButton( "CLOSE" ) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                setCancelable( false )
                create()
            }
            alertDialog.show()
        }
    }


    private fun setupCameraProvider() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderListenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderListenableFuture.get()
                bindPreview(cameraProvider)
            }
            catch (e: ExecutionException) {
                Log.e("APP", e.message!!)
            }
            catch (e: InterruptedException) {
                Log.e("APP", e.message!!)
            }
        }, ContextCompat.getMainExecutor( this ))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val displayMetrics = resources.displayMetrics
        val screenSize = Size( displayMetrics.widthPixels, displayMetrics.heightPixels)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution( screenSize )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer( Executors.newSingleThreadExecutor() , frameAnalyser )
        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

}
