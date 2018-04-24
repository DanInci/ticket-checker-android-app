package ticket.checker.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import ticket.checker.R

class FocusView(context : Context, attrs: AttributeSet) : View(context, attrs) {
    private var paint : Paint = Paint()
    private var touchArea : Rect? = null

    init {
        paint.color = ContextCompat.getColor(context, R.color.transparentWhite)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
    }

    fun focus(touchArea : Rect) {
        this.touchArea = touchArea
        invalidate()
    }

    fun release() {
        this.touchArea = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if(touchArea!= null) {
            val left = touchArea?.left?.toFloat() ?: 0f
            val top = touchArea?.top?.toFloat() ?: 0f
            val right = touchArea?.right?.toFloat() ?: 0f
            val bottom = touchArea?.bottom?.toFloat() ?: 0f
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}