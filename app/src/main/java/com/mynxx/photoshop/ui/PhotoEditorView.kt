package com.mynxx.photoshop.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mynxx.photoshop.filters.FilterManager
import kotlin.math.max
import kotlin.math.min

class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ToolMode {
        NONE,
        BRUSH,
        ERASER,
        TEXT
    }

    data class Stroke(
        val path: Path,
        val color: Int,
        val strokeWidth: Float,
        val isEraser: Boolean
    )

    data class TextItem(
        var text: String,
        var xNorm: Float, // Normalized 0.0 .. 1.0 coordinates relative to bitmap
        var yNorm: Float,
        var color: Int = Color.WHITE,
        var textSize: Float = 48f
    )

    sealed class EditorAction {
        data class AddStroke(val stroke: Stroke) : EditorAction()
        data class AddText(val textItem: TextItem) : EditorAction()
    }

    private var baseBitmap: Bitmap? = null
    private var colorMatrix: ColorMatrix? = null

    var currentTool: ToolMode = ToolMode.NONE
    var brushColor: Int = Color.RED
    var brushSize: Float = 16f
    var eraserSize: Float = 32f

    private val strokes = mutableListOf<Stroke>()
    private val textItems = mutableListOf<TextItem>()

    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    // Active drawing path
    private var currentPath: Path? = null
    private var lastX = 0f
    private var lastY = 0f

    // Active selected text for dragging
    private var activeTextItem: TextItem? = null

    // Destination rect of bitmap on screen
    private val bitmapDestRect = RectF()

    // Paints
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    fun setBitmap(bitmap: Bitmap) {
        this.baseBitmap = bitmap
        this.strokes.clear()
        this.textItems.clear()
        this.undoStack.clear()
        this.redoStack.clear()
        this.colorMatrix = null
        requestLayout()
        invalidate()
    }

    fun getBitmap(): Bitmap? = baseBitmap

    fun setColorMatrix(matrix: ColorMatrix?) {
        this.colorMatrix = matrix
        if (matrix != null) {
            bitmapPaint.colorFilter = ColorMatrixColorFilter(matrix)
        } else {
            bitmapPaint.colorFilter = null
        }
        invalidate()
    }

    fun addText(text: String, color: Int = Color.WHITE, size: Float = 56f) {
        val item = TextItem(text = text, xNorm = 0.5f, yNorm = 0.5f, color = color, textSize = size)
        textItems.add(item)
        undoStack.add(EditorAction.AddText(item))
        redoStack.clear()
        invalidate()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val action = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(action)

        when (action) {
            is EditorAction.AddStroke -> strokes.remove(action.stroke)
            is EditorAction.AddText -> textItems.remove(action.textItem)
        }
        invalidate()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val action = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(action)

        when (action) {
            is EditorAction.AddStroke -> strokes.add(action.stroke)
            is EditorAction.AddText -> textItems.add(action.textItem)
        }
        invalidate()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBitmapDestRect()
    }

    private fun calculateBitmapDestRect() {
        val bmp = baseBitmap ?: return
        if (width <= 0 || height <= 0) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()

        val scale = min(viewWidth / bmpWidth, viewHeight / bmpHeight)
        val destW = bmpWidth * scale
        val destH = bmpHeight * scale

        val left = (viewWidth - destW) / 2f
        val top = (viewHeight - destH) / 2f
        bitmapDestRect.set(left, top, left + destW, top + destH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = baseBitmap ?: return

        calculateBitmapDestRect()

        // 1. Draw filtered base bitmap
        canvas.drawBitmap(bmp, null, bitmapDestRect, bitmapPaint)

        // Clip drawing and text inside bitmap bounds so strokes don't bleed outside the photo
        canvas.save()
        canvas.clipRect(bitmapDestRect)

        // 2. Draw committed strokes
        for (stroke in strokes) {
            val p = createPaintForStroke(stroke)
            canvas.drawPath(stroke.path, p)
        }

        // 3. Draw active drawing path
        currentPath?.let { path ->
            val stroke = Stroke(
                path = path,
                color = brushColor,
                strokeWidth = if (currentTool == ToolMode.ERASER) eraserSize else brushSize,
                isEraser = currentTool == ToolMode.ERASER
            )
            val p = createPaintForStroke(stroke)
            canvas.drawPath(path, p)
        }

        // 4. Draw text items
        for (item in textItems) {
            textPaint.color = item.color
            textPaint.textSize = item.textSize
            textPaint.setShadowLayer(6f, 2f, 2f, Color.BLACK)

            val px = bitmapDestRect.left + (item.xNorm * bitmapDestRect.width())
            val py = bitmapDestRect.top + (item.yNorm * bitmapDestRect.height())
            canvas.drawText(item.text, px, py, textPaint)
        }

        canvas.restore()
    }

    private fun createPaintForStroke(stroke: Stroke): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = stroke.strokeWidth
            if (stroke.isEraser) {
                // Eraser mode: Paint in background color or blend mode
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = stroke.color
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (currentTool) {
            ToolMode.BRUSH, ToolMode.ERASER -> {
                handleDrawingTouch(event, x, y)
                return true
            }
            ToolMode.TEXT -> {
                handleTextTouch(event, x, y)
                return true
            }
            ToolMode.NONE -> {
                return super.onTouchEvent(event)
            }
        }
    }

    private fun handleDrawingTouch(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply {
                    moveTo(x, y)
                }
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f)
                lastX = x
                lastY = y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let { path ->
                    val stroke = Stroke(
                        path = path,
                        color = brushColor,
                        strokeWidth = if (currentTool == ToolMode.ERASER) eraserSize else brushSize,
                        isEraser = currentTool == ToolMode.ERASER
                    )
                    strokes.add(stroke)
                    undoStack.add(EditorAction.AddStroke(stroke))
                    redoStack.clear()
                }
                currentPath = null
                invalidate()
            }
        }
    }

    private fun handleTextTouch(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if user tapped near any existing text
                activeTextItem = textItems.lastOrNull { item ->
                    val tx = bitmapDestRect.left + (item.xNorm * bitmapDestRect.width())
                    val ty = bitmapDestRect.top + (item.yNorm * bitmapDestRect.height())
                    val dx = x - tx
                    val dy = y - ty
                    (dx * dx + dy * dy) < (80f * 80f)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                activeTextItem?.let { item ->
                    if (bitmapDestRect.width() > 0 && bitmapDestRect.height() > 0) {
                        item.xNorm = ((x - bitmapDestRect.left) / bitmapDestRect.width()).coerceIn(0.05f, 0.95f)
                        item.yNorm = ((y - bitmapDestRect.top) / bitmapDestRect.height()).coerceIn(0.05f, 0.95f)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                activeTextItem = null
            }
        }
    }

    /**
     * Bakes the base bitmap + ColorMatrix filters + drawing paths + text overlays
     * into a new full-resolution Bitmap.
     */
    fun generateFinalBitmap(): Bitmap? {
        val bmp = baseBitmap ?: return null

        // 1. Apply color matrix to base bitmap
        val filtered = if (colorMatrix != null) {
            FilterManager.applyMatrixToBitmap(bmp, colorMatrix!!)
        } else {
            bmp.copy(Bitmap.Config.ARGB_8888, true)
        }

        val finalCanvas = Canvas(filtered)

        // Scale factors from screen view to original bitmap
        val scaleX = filtered.width.toFloat() / bitmapDestRect.width()
        val scaleY = filtered.height.toFloat() / bitmapDestRect.height()

        finalCanvas.save()
        finalCanvas.scale(scaleX, scaleY)
        finalCanvas.translate(-bitmapDestRect.left, -bitmapDestRect.top)

        // 2. Draw all strokes
        for (stroke in strokes) {
            val p = createPaintForStroke(stroke)
            finalCanvas.drawPath(stroke.path, p)
        }

        // 3. Draw text items
        finalCanvas.restore()
        for (item in textItems) {
            textPaint.color = item.color
            // Scale text size proportional to original bitmap resolution
            val scaledTextSize = item.textSize * (filtered.width.toFloat() / max(1f, bitmapDestRect.width()))
            textPaint.textSize = scaledTextSize
            textPaint.setShadowLayer(scaledTextSize * 0.1f, 2f, 2f, Color.BLACK)

            val px = item.xNorm * filtered.width
            val py = item.yNorm * filtered.height
            finalCanvas.drawText(item.text, px, py, textPaint)
        }

        return filtered
    }
}
