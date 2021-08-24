package com.ml.shubham0204.ocms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.shubham0204.selfiesegmentation.DrawingOverlay

class FrameAnalyser( private var drawingOverlay: DrawingOverlay) : ImageAnalysis.Analyzer {

    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode( SelfieSegmenterOptions.STREAM_MODE )
            .build()
    val segmenter = Segmentation.getClient(options)
    private var frameMediaImage : Image? = null


    override fun analyze(image: ImageProxy) {
        frameMediaImage = image.image
        if ( frameMediaImage != null) {
            val inputImage = InputImage.fromMediaImage( frameMediaImage , image.imageInfo.rotationDegrees )
            segmenter.process( inputImage )
                .addOnSuccessListener { segmentationMask ->
                    val mask = segmentationMask.buffer
                    val maskWidth = segmentationMask.width
                    val maskHeight = segmentationMask.height
                    mask.rewind()
                    val bitmap = Bitmap.createBitmap( maskWidth , maskHeight , Bitmap.Config.ARGB_8888 )
                    bitmap.copyPixelsFromBuffer( mask )

                    drawingOverlay.maskBitmap = bitmap
                    drawingOverlay.invalidate()
                }
                .addOnFailureListener { exception ->
                    Log.e( "App" , exception.message!! )
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }
}