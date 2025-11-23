package com.arbeitszeit.tracker.wear.tile

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import com.arbeitszeit.tracker.wear.data.WearDatabase
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class ArbeitszeitTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return serviceScope.future {
            val database = WearDatabase.getDatabase(applicationContext)
            val timeEntryDao = database.timeEntryDao()

            // Load today's entry
            val today = LocalDate.now()
            val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val entry = timeEntryDao.getEntryByDate(dateString)

            // Calculate today's hours
            val todayMinutes = entry?.getIstMinuten() ?: 0
            val todayHours = todayMinutes / 60
            val todayMins = todayMinutes % 60

            // Calculate week's hours
            val weekField = WeekFields.of(Locale.GERMANY)
            val currentWeek = today.get(weekField.weekOfWeekBasedYear())
            val currentYear = today.get(weekField.weekBasedYear())
            val weekEntries = timeEntryDao.getEntriesByWeek(currentYear, currentWeek)
            val weekMinutes = weekEntries.sumOf { it.getIstMinuten() }
            val weekHours = weekMinutes / 60
            val weekMins = weekMinutes % 60

            // Check if working
            val isWorking = entry?.startZeit != null && entry.endZeit == null

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setTimeline(
                    TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(
                                            LayoutElementBuilders.Column.Builder()
                                                .setWidth(dp(200f))
                                                .setHeight(dp(200f))
                                                .setModifiers(
                                                    ModifiersBuilders.Modifiers.Builder()
                                                        .setPadding(
                                                            ModifiersBuilders.Padding.Builder()
                                                                .setAll(dp(16f))
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .addContent(
                                                    // Title
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText("Arbeitszeit")
                                                        .setFontStyle(
                                                            LayoutElementBuilders.FontStyle.Builder()
                                                                .setSize(sp(14f))
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .addContent(
                                                    // Status
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText(if (isWorking) "⏱ Läuft" else "✓ Gestempelt")
                                                        .setFontStyle(
                                                            LayoutElementBuilders.FontStyle.Builder()
                                                                .setSize(sp(12f))
                                                                .setColor(
                                                                    if (isWorking) {
                                                                        argb(0xFF00FF00.toInt())
                                                                    } else {
                                                                        argb(0xFFFFFFFF.toInt())
                                                                    }
                                                                )
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .addContent(
                                                    LayoutElementBuilders.Spacer.Builder()
                                                        .setHeight(dp(8f))
                                                        .build()
                                                )
                                                .addContent(
                                                    // Today's hours
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText("Heute: $todayHours:${String.format("%02d", todayMins)}")
                                                        .setFontStyle(
                                                            LayoutElementBuilders.FontStyle.Builder()
                                                                .setSize(sp(16f))
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .addContent(
                                                    // Week's hours
                                                    LayoutElementBuilders.Text.Builder()
                                                        .setText("Woche: $weekHours:${String.format("%02d", weekMins)}")
                                                        .setFontStyle(
                                                            LayoutElementBuilders.FontStyle.Builder()
                                                                .setSize(sp(12f))
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )
    }
}
