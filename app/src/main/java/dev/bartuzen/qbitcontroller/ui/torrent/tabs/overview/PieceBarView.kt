package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.utils.getColorCompat
import kotlin.math.roundToInt

class PieceBarView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var pieces: List<PieceState> = emptyList()

    private val colorNotDownloaded = Paint().apply {
        color = context.getColorCompat(R.color.piece_not_downloaded)
    }
    private val colorDownloading = Paint().apply {
        color = context.getColorCompat(R.color.piece_downloading)
    }
    private val colorDownloaded = Paint().apply {
        color = context.getColorCompat(R.color.piece_downloaded)
    }

    fun setPieces(pieces: List<PieceState>) {
        this.pieces = pieces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), colorNotDownloaded)

        var start = -1
        var end = -1
        var prev = PieceState.NOT_DOWNLOADED

        pieces.forEachIndexed { index, value ->
            if (value != prev) {
                if (start != -1) {
                    drawPieceSegment(canvas, start, end, prev)
                }
                start = index
            }
            end = index
            prev = value
        }

        drawPieceSegment(canvas, start, end, prev)
    }

    private fun drawPieceSegment(canvas: Canvas, start: Int, end: Int, pieceState: PieceState) {
        val color = when (pieceState) {
            PieceState.DOWNLOADING -> colorDownloading
            PieceState.DOWNLOADED -> colorDownloaded
            else -> return
        }

        val segmentWidth = width.toFloat() / pieces.size
        canvas.drawRect(
            (start * segmentWidth).roundToInt().toFloat(),
            0f,
            (end * segmentWidth).roundToInt().toFloat() + 1,
            height.toFloat(),
            color
        )
    }
}
