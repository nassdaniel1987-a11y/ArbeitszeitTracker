package com.arbeitszeit.tracker.wear.sync

import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service für die Synchronisation von Daten zwischen Handy und Uhr
 *
 * Empfängt Updates vom Handy und aktualisiert die lokale Datenbank
 */
class DataLayerListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        // Data paths für verschiedene Sync-Typen
        const val TIME_ENTRY_PATH = "/arbeitszeit/time_entry"
        const val SETTINGS_PATH = "/arbeitszeit/settings"
        const val SYNC_REQUEST_PATH = "/arbeitszeit/sync_request"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    TIME_ENTRY_PATH -> {
                        handleTimeEntrySync(event.dataItem)
                    }
                    SETTINGS_PATH -> {
                        handleSettingsSync(event.dataItem)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            SYNC_REQUEST_PATH -> {
                // Handy fordert Sync an - sende aktuelle Daten
                scope.launch {
                    syncDataToPhone()
                }
            }
        }
    }

    private fun handleTimeEntrySync(dataItem: DataItem) {
        scope.launch {
            try {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                // TODO: Parse TimeEntry from DataMap and update local database
                // This would deserialize the data and update the Room database
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSettingsSync(dataItem: DataItem) {
        scope.launch {
            try {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                // TODO: Parse UserSettings from DataMap and update local database
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncDataToPhone() {
        // TODO: Send local data to phone
        // This would read from the Room database and send via Data Layer API
    }
}
