package ticket.checker.camera

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView

class CameraSurfaceView(context : Context, attrs : AttributeSet) : SurfaceView(context, attrs) {
    var cameraSource : CameraSource? = null
    var focusView : FocusView? = null

    private var rectIsDrawn = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(cameraSource == null) {
            return false
        }

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x.toInt()
                val y = event.y.toInt()

                val touchRect = Rect(x-100, y-100, x + 100, y + 100)
                val targetFocusRect = Rect(touchRect.left * 2000/this.width - 1000,
                        touchRect.top * 2000/this.height - 1000,
                        touchRect.right * 2000/this.width - 1000,
                        touchRect.bottom * 2000/this.height - 1000)
                cameraSource?.doTouchFocus(targetFocusRect)
                if(!rectIsDrawn && focusView != null) {
                    rectIsDrawn = true
                    focusView?.focus(touchRect)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if(rectIsDrawn && focusView != null) {
                    rectIsDrawn = false
                    Handler().postDelayed({
                        focusView?.release()
                    },1000)
                }
            }
        }
        return false
    }
}