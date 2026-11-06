package com.jacobrozell.puzzlebuddy.support.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleCollectionCollageLayout
import com.jacobrozell.puzzlebuddy.domain.share.PuzzleShareSummary
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.surface.AppLinks
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object PuzzleCollageRenderer {
    const val CANVAS_WIDTH = 1080
    const val CANVAS_HEIGHT = 1350

    fun featuredTiles(puzzles: List<Puzzle>): List<Puzzle> {
        val sorted = puzzles.sortedWith(
            compareByDescending<Puzzle> { it.hasImage }.thenByDescending { it.completionDate },
        )
        val displayed = PuzzleCollectionCollageLayout.gridDimensions(sorted.size).displayed
        return sorted.take(displayed)
    }

    fun render(puzzles: List<Puzzle>, coverBytes: Map<String, ByteArray?>): ByteArray {
        val sorted = puzzles.sortedWith(
            compareByDescending<Puzzle> { it.hasImage }.thenByDescending { it.completionDate },
        )
        val stats = CollectionStats.compute(sorted)
        val layout = PuzzleCollectionCollageLayout.gridDimensions(sorted.size)
        val tiles = sorted.take(layout.displayed)
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas)
        drawHeader(canvas, stats)
        drawGrid(canvas, tiles, layout, coverBytes)
        if (sorted.size > layout.displayed) {
            drawCentered(
                canvas,
                "+${sorted.size - layout.displayed} more puzzles",
                footerRect(),
                color(0.05f, 0.55f, 0.62f),
                22f,
                600,
            )
        } else {
            drawCentered(
                canvas,
                AppLinks.shareFooterLabel,
                footerRect(),
                color(0.35f, 0.38f, 0.42f),
                20f,
                500,
            )
        }
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        bitmap.recycle()
        return bytes.toByteArray()
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(color(0.95f, 0.97f, 0.98f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                CANVAS_WIDTH.toFloat(),
                CANVAS_HEIGHT * 0.35f,
                color(0.10f, 0.45f, 0.85f, 0.18f),
                color(0.08f, 0.58f, 0.55f, 0.12f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), paint)
    }

    private fun drawHeader(canvas: Canvas, stats: CollectionStats) {
        val title = textPaint(color(0.08f, 0.09f, 0.11f), 42f, 700)
        val subtitle = textPaint(color(0.35f, 0.38f, 0.42f), 24f, 500)
        drawTopLeft(canvas, "Puzzle Buddy", 48f, 48f, title)
        drawTopLeft(canvas, PuzzleShareSummary.statsLine(stats), 48f, 104f, subtitle)
    }

    private fun drawGrid(
        canvas: Canvas,
        tiles: List<Puzzle>,
        layout: PuzzleCollectionCollageLayout.Grid,
        coverBytes: Map<String, ByteArray?>,
    ) {
        val spacing = 12f
        val grid = RectF(48f, 230f, CANVAS_WIDTH - 48f, CANVAS_HEIGHT - 120f)
        val cellWidth = (grid.width() - spacing * (layout.columns - 1)) / layout.columns
        val cellHeight = (grid.height() - spacing * (layout.rows - 1)) / layout.rows
        for (index in 0 until layout.displayed) {
            val column = index % layout.columns
            val row = index / layout.columns
            val cell = RectF(
                grid.left + column * (cellWidth + spacing),
                grid.top + row * (cellHeight + spacing),
                grid.left + column * (cellWidth + spacing) + cellWidth,
                grid.top + row * (cellHeight + spacing) + cellHeight,
            )
            val puzzle = tiles.getOrNull(index)
            if (puzzle == null) drawPlaceholder(canvas, cell) else drawTile(canvas, puzzle, cell, coverBytes[puzzle.id])
        }
    }

    private fun drawTile(canvas: Canvas, puzzle: Puzzle, rect: RectF, bytes: ByteArray?) {
        val path = rounded(rect, 16f)
        canvas.save()
        canvas.clipPath(path)
        val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, coverRect(bitmap.width.toFloat(), bitmap.height.toFloat(), rect), Paint(Paint.ANTI_ALIAS_FLAG))
            bitmap.recycle()
        } else {
            drawPlaceholderFill(canvas, rect)
        }
        canvas.restore()
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.argb((0.85f * 255).roundToInt(), 255, 255, 255)
            },
        )
        drawTileLabel(canvas, puzzle.name, rect)
    }

    private fun drawPlaceholder(canvas: Canvas, rect: RectF) {
        canvas.save()
        canvas.clipPath(rounded(rect, 16f))
        drawPlaceholderFill(canvas, rect)
        canvas.restore()
    }

    private fun drawPlaceholderFill(canvas: Canvas, rect: RectF) {
        canvas.drawRect(rect, Paint().apply { color = color(0.95f, 0.97f, 0.98f) })
        val size = minOf(rect.width(), rect.height()) * 0.28f
        val icon = RectF(
            rect.centerX() - size / 2f,
            rect.centerY() - size / 2f - 12f,
            rect.centerX() + size / 2f,
            rect.centerY() + size / 2f - 12f,
        )
        canvas.drawRoundRect(icon, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(0.05f, 0.55f, 0.62f) })
    }

    private fun drawTileLabel(canvas: Canvas, name: String, rect: RectF) {
        val label = RectF(rect.left + 8f, rect.bottom - 42f, rect.right - 8f, rect.bottom - 8f)
        canvas.drawRoundRect(label, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(115, 0, 0, 0) })
        val paint = textPaint(Color.WHITE, 14f, 600).apply { textAlign = Paint.Align.CENTER }
        val shown = TextUtils.ellipsize(name, paint, label.width() - 12f, TextUtils.TruncateAt.END).toString()
        val baseline = label.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(shown, label.centerX(), baseline, paint)
    }

    private fun drawCentered(canvas: Canvas, text: String, rect: RectF, color: Int, size: Float, weight: Int) {
        val paint = textPaint(color, size, weight).apply { textAlign = Paint.Align.CENTER }
        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, rect.centerX(), baseline, paint)
    }

    private fun drawTopLeft(canvas: Canvas, text: String, x: Float, top: Float, paint: Paint) {
        canvas.drawText(text, x, top - paint.ascent(), paint)
    }

    private fun footerRect() = RectF(48f, CANVAS_HEIGHT - 88f, CANVAS_WIDTH - 48f, CANVAS_HEIGHT - 48f)

    private fun coverRect(imageWidth: Float, imageHeight: Float, bounds: RectF): RectF {
        if (imageWidth <= 0f || imageHeight <= 0f) return bounds
        val scale = max(bounds.width() / imageWidth, bounds.height() / imageHeight)
        val width = imageWidth * scale
        val height = imageHeight * scale
        return RectF(
            bounds.centerX() - width / 2f,
            bounds.centerY() - height / 2f,
            bounds.centerX() + width / 2f,
            bounds.centerY() + height / 2f,
        )
    }

    private fun rounded(rect: RectF, radius: Float) = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }

    private fun textPaint(color: Int, size: Float, weight: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, weight, false)
    }

    private fun color(red: Float, green: Float, blue: Float, alpha: Float = 1f): Int =
        Color.argb((alpha * 255).roundToInt(), (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
}
