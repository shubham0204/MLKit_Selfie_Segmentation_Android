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

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.Bitmap

// Overlay to draw the segmentation over the PreviewView
class DrawingOverlay(context : Context, attributeSet : AttributeSet) : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Segmentation Bitmap.
    // Assigned in FrameAnalyser.kt
    var maskBitmap : Bitmap? = null

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
    private fun flipBitmap( source: Bitmap ): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onDraw(canvas: Canvas?) {
        if ( maskBitmap != null ) {
            canvas?.drawBitmap( flipBitmap( maskBitmap!! ), 0f , 0f , null )
        }
    }

}