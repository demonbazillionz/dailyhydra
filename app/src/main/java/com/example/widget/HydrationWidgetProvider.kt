package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.HydrationDatabase
import com.example.data.IntakeEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class HydrationWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_ADD = "com.example.ACTION_QUICK_ADD"

        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, HydrationWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, HydrationWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_QUICK_ADD) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HydrationDatabase.getDatabase(context)
                    val dao = db.hydrationDao()
                    
                    // Retrieve default quick add amount
                    val defaultQuickAddSetting = dao.getSetting("default_quick_add_mls")
                    val amountMl = defaultQuickAddSetting?.value?.toIntOrNull() ?: 250
                    
                    // Log the water intake entry
                    val entry = IntakeEntry(amountMl = amountMl, timestamp = System.currentTimeMillis())
                    dao.insertIntakeEntry(entry)

                    // Re-calculate stats for immediate toast feedback
                    val startOfToday = getStartOfTodayMs()
                    val entriesToday = dao.getIntakeEntriesBetween(startOfToday, getEndOfTodayMs()).first()
                    val newTotal = entriesToday.sumOf { it.amountMl }
                    
                    val goalSetting = dao.getSetting("daily_goal")
                    val goal = goalSetting?.value?.toIntOrNull() ?: 2500

                    _showToastOnMain(context, "Logged +${amountMl}ml! Today: ${newTotal}/${goal}ml")

                    // Update widgets
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, HydrationWidgetProvider::class.java)
                    )
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun _showToastOnMain(context: Context, msg: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.hydration_widget)

        // Use global IO scope to read metrics asynchronously from Room
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HydrationDatabase.getDatabase(context)
                val dao = db.hydrationDao()

                // Find Today's Intake Entries
                val startOfToday = getStartOfTodayMs()
                val endOfToday = getEndOfTodayMs()
                val entriesToday = dao.getIntakeEntriesBetween(startOfToday, endOfToday).first()
                val currentIntake = entriesToday.sumOf { it.amountMl }

                // Get preferences
                val goalSetting = dao.getSetting("daily_goal")
                val goal = goalSetting?.value?.toIntOrNull() ?: 2500

                val unitSetting = dao.getSetting("goal_unit")
                val unit = unitSetting?.value ?: "ml"

                // UI formats depending on selected units
                val currentText: String
                val remainingText: String
                if (unit == "Liter") {
                    val currentL = currentIntake / 1000f
                    val goalL = goal / 1000f
                    currentText = String.format("%.2f / %.1f L", currentL, goalL)
                    val rem = (goal - currentIntake).coerceAtLeast(0) / 1000f
                    remainingText = if (rem > 0) String.format("%.2f L left today", rem) else "Goal achieved!"
                } else {
                    currentText = "$currentIntake / $goal ml"
                    val rem = (goal - currentIntake).coerceAtLeast(0)
                    remainingText = if (rem > 0) "${rem}ml left today" else "Goal achieved!"
                }

                val progressPercent = if (goal > 0) ((currentIntake.toFloat() / goal) * 100).toInt() else 0

                // Generate and set modern circular progress bitmap
                val sizePx = dpToPx(context, 160f).toInt()
                val progressBitmap = createProgressBitmap(context, sizePx, currentIntake, goal, progressPercent)
                views.setImageViewBitmap(R.id.widget_progress_canvas, progressBitmap)

                // Set PendingIntent to launch app when clicking the widget container
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    pendingFlags
                )
                views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                // Push update to the specific widget instance
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createProgressBitmap(
        context: Context,
        size: Int,
        current: Int,
        goal: Int,
        percent: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Antialiasing setup for a high-quality visual
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.08f // beautiful proportionate thickness
            strokeCap = Paint.Cap.ROUND
        }

        val padding = paint.strokeWidth / 2f + size * 0.02f
        val rectF = RectF(padding, padding, size - padding, size - padding)

        // 1. Draw outer circle (track)
        paint.color = Color.parseColor("#122a4d") // Sophisticated dark slate blue track
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - padding, paint)

        // 2. Draw progress arc (Neon Aqua Blue)
        if (goal > 0) {
            val sweepAngle = (percent.toFloat() / 100f * 360f).coerceAtMost(360f)
            paint.shader = SweepGradient(
                size / 2f,
                size / 2f,
                intArrayOf(Color.parseColor("#4FC3F7"), Color.parseColor("#00E5FF"), Color.parseColor("#4FC3F7")),
                null
            ).apply {
                // Rotate shader to start from top
                val matrix = Matrix()
                matrix.postRotate(-90f, size / 2f, size / 2f)
                setLocalMatrix(matrix)
            }
            canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
        }

        // Reset brush shader for text
        paint.shader = null
        paint.style = Paint.Style.FILL

        // 3. Draw percentage text inside circle
        paint.color = Color.parseColor("#E0F7FA") // Crisp high-contrast typography
        paint.textSize = size * 0.22f // scaled text size
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val text = "$percent%"
        // Align text vertically center
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textY = (size / 2f) - textBounds.exactCenterY()
        canvas.drawText(text, size / 2f, textY - size * 0.04f, paint)

        // 4. Draw small action descriptor underneath
        paint.color = Color.parseColor("#80DEEA")
        paint.textSize = size * 0.11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Hydrated", size / 2f, textY + size * 0.15f, paint)

        return bitmap
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    private fun getStartOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
