package com.shubham0204.selfiesegmentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.util.concurrent.Executors

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class SegmentationOverlay (
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context
) : FrameLayout( context ) {

    private lateinit var drawingOverlay: DrawingOverlay
    private lateinit var previewView: PreviewView

    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode( SelfieSegmenterOptions.STREAM_MODE )
            .build()
    private val segmenter = Segmentation.getClient(options)

    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0
    private var isProcessing = false
    private var cameraFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var maskBitmap : Bitmap? = null

    init {
        initializeCamera( cameraFacing )
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun initializeCamera(
        cameraFacing: Int
    ) {
        this.cameraFacing = cameraFacing
        val cameraProviderFuture = ProcessCameraProvider.getInstance( context )
        val previewView = PreviewView( context )
        val executor = ContextCompat.getMainExecutor( context )
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing( cameraFacing )
                .build()
            val frameAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy( AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY )
                        .build()
                )
                .setBackpressureStrategy( ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST )
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888 )
                .build()
            frameAnalyzer.setAnalyzer( Executors.newSingleThreadExecutor() , analyzer )
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview ,
                frameAnalyzer
            )
        }, executor )
        if ( childCount == 2 ){
            removeView( this.previewView )
            removeView( this.drawingOverlay )
        }
        this.previewView = previewView
        addView( this.previewView )

        val drawingOverlayParams = LayoutParams(
            LayoutParams.MATCH_PARENT ,
            LayoutParams.MATCH_PARENT
        )
        this.drawingOverlay = DrawingOverlay( context )
        this.drawingOverlay.setWillNotDraw( false )
        this.drawingOverlay.setZOrderOnTop( true )
        addView( this.drawingOverlay , drawingOverlayParams )
    }

    private val analyzer = ImageAnalysis.Analyzer { it ->
        if( isProcessing ) {
            it.close()
            return@Analyzer
        }
        isProcessing = true
        if ( it.image != null) {
            val inputImage = InputImage.fromMediaImage( it.image!! , it.imageInfo.rotationDegrees )
            segmenter.process( inputImage )
                .addOnSuccessListener { segmentationMask ->
                    // Convert the mask to a Bitmap
                    val mask = segmentationMask.buffer
                    val maskWidth = segmentationMask.width
                    val maskHeight = segmentationMask.height
                    mask.rewind()
                    val bitmap = Bitmap.createBitmap( maskWidth , maskHeight , Bitmap.Config.ARGB_8888 )
                    bitmap.copyPixelsFromBuffer( mask )
                    // Update the DrawingOverlay
                    maskBitmap = bitmap
                    drawingOverlay.invalidate()
                }
                .addOnFailureListener { exception ->
                    Log.e( "App" , exception.message!! )
                }
                .addOnCompleteListener { _ ->
                    it.close()
                    isProcessing = false
                }
        }
    }


    // Overlay to draw the segmentation over the PreviewView
    inner class DrawingOverlay(context : Context) : SurfaceView( context ) , SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            TODO("Not yet implemented")
        }


        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            TODO("Not yet implemented")
        }


        override fun surfaceDestroyed(holder: SurfaceHolder) {
            TODO("Not yet implemented")
        }

        // Flip the given `Bitmap` vertically.
        // See this SO answer -> https://stackoverflow.com/a/36494192/10878733
        private fun flipBitmap( source: Bitmap): Bitmap {
            val matrix = Matrix()
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            return Bitmap.createScaledBitmap( bitmap , overlayWidth , overlayHeight , true )
        }

        override fun onDraw(canvas: Canvas) {
            if ( maskBitmap != null ) {
                canvas.drawBitmap( flipBitmap( maskBitmap!! ), 0f , 0f , null )
            }
        }

    }


}