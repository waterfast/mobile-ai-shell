package dev.vibecode.mobileagent

import android.app.Application
import dev.vibecode.mobileagent.data.SettingsRepository

class MobileAgentApp : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
