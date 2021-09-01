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

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

class FrameAnalyser( private var drawingOverlay: DrawingOverlay) : ImageAnalysis.Analyzer {

    // Configure Selfie Segmenter
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode( SelfieSegmenterOptions.STREAM_MODE )
            .build()
    private val segmenter = Segmentation.getClient(options)
    private var frameMediaImage : Image? = null


    override fun analyze(image: ImageProxy) {
        frameMediaImage = image.image
        if ( frameMediaImage != null) {
            val inputImage = InputImage.fromMediaImage( frameMediaImage!! , image.imageInfo.rotationDegrees )
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