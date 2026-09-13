package com.ultron.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ultron.assistant.data.AppDatabase
import com.ultron.assistant.data.ContactsRepository
import com.ultron.assistant.data.CustomCommandsRepository
import com.ultron.assistant.data.PreferencesRepository
import com.ultron.assistant.data.SocialAccountsRepository
import com.ultron.assistant.security.SecureStorage

class UltronApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var secureStorage: SecureStorage
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var contactsRepository: ContactsRepository
        private set

    lateinit var socialAccountsRepository: SocialAccountsRepository
        private set

    lateinit var customCommandsRepository: CustomCommandsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        database = AppDatabase.getDatabase(this)
        secureStorage = SecureStorage(this)
        preferencesRepository = PreferencesRepository(this, secureStorage)
        contactsRepository = ContactsRepository(this)
        socialAccountsRepository = SocialAccountsRepository(this)
        customCommandsRepository = CustomCommandsRepository(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_SERVICE = "ultron_service_channel"
        lateinit var instance: UltronApplication
            private set
    }
}
