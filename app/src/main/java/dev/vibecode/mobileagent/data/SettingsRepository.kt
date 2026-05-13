package dev.vibecode.mobileagent.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        prefs =
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value).apply()
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, "").orEmpty()

    fun setBaseUrl(value: String) {
        prefs.edit().putString(KEY_BASE_URL, value).apply()
    }

    fun getModelName(): String = prefs.getString(KEY_MODEL, "").orEmpty()

    fun setModelName(value: String) {
        prefs.edit().putString(KEY_MODEL, value).apply()
    }

    fun getProviderId(): String = prefs.getString(KEY_PROVIDER, DEFAULT_PROVIDER).orEmpty()

    fun setProviderId(value: String) {
        prefs.edit().putString(KEY_PROVIDER, value).apply()
    }

    fun getAgentBackendId(): String = prefs.getString(KEY_AGENT_BACKEND, DEFAULT_AGENT_BACKEND).orEmpty()

    fun setAgentBackendId(value: String) {
        prefs.edit().putString(KEY_AGENT_BACKEND, value).apply()
    }

    companion object {
        private const val PREFS_FILE = "mobile_agent_secure_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model_name"
        private const val KEY_PROVIDER = "provider_id"
        private const val KEY_AGENT_BACKEND = "agent_backend_id"

        const val DEFAULT_PROVIDER = "openai_compatible"

        const val DEFAULT_AGENT_BACKEND = "stub"
        const val AGENT_BACKEND_STUB = "stub"
        const val AGENT_BACKEND_TERMUX_OPENCLAUDE = "termux_openclaude"

        val providerOptions: List<Pair<String, String>> =
            listOf(
                "openai_compatible" to "OpenAI 兼容",
                "anthropic_stub" to "Anthropic（占位）",
                "local_stub" to "本地（占位）",
            )

        val agentBackendOptions: List<Pair<String, String>> =
            listOf(
                AGENT_BACKEND_STUB to "Stub（本地模拟流）",
                AGENT_BACKEND_TERMUX_OPENCLAUDE to "OpenClaude（Termux RUN_COMMAND）",
            )
    }
}
