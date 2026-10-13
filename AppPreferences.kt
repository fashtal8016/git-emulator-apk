package com.example.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.core.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kotlinforge_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "DARK") ?: "DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _gitAuthorName = MutableStateFlow(prefs.getString(KEY_GIT_NAME, "KotlinForge Dev") ?: "KotlinForge Dev")
    val gitAuthorName: StateFlow<String> = _gitAuthorName.asStateFlow()

    private val _gitAuthorEmail = MutableStateFlow(prefs.getString(KEY_GIT_EMAIL, "dev@kotlinforge.local") ?: "dev@kotlinforge.local")
    val gitAuthorEmail: StateFlow<String> = _gitAuthorEmail.asStateFlow()

    private val _defaultBranch = MutableStateFlow(prefs.getString(KEY_DEFAULT_BRANCH, "main") ?: "main")
    val defaultBranch: StateFlow<String> = _defaultBranch.asStateFlow()

    private val _activeProjectId = MutableStateFlow(prefs.getLong(KEY_ACTIVE_PROJECT_ID, -1L))
    val activeProjectId: StateFlow<Long> = _activeProjectId.asStateFlow()

    private val _editorFontSize = MutableStateFlow(prefs.getInt(KEY_FONT_SIZE, 13))
    val editorFontSize: StateFlow<Int> = _editorFontSize.asStateFlow()

    private val _editorWordWrap = MutableStateFlow(prefs.getBoolean(KEY_WORD_WRAP, true))
    val editorWordWrap: StateFlow<Boolean> = _editorWordWrap.asStateFlow()

    private val _editorLineNumbers = MutableStateFlow(prefs.getBoolean(KEY_LINE_NUMBERS, true))
    val editorLineNumbers: StateFlow<Boolean> = _editorLineNumbers.asStateFlow()

    private val _autoSave = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SAVE, true))
    val autoSave: StateFlow<Boolean> = _autoSave.asStateFlow()

    private val _aiModel = MutableStateFlow(prefs.getString(KEY_AI_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash")
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    // Encrypted token management via Android Keystore
    fun saveGitHubToken(token: String) {
        val encrypted = CryptoManager.encrypt(token)
        prefs.edit().putString(KEY_GITHUB_TOKEN_ENC, encrypted).apply()
    }

    fun getGitHubToken(): String {
        val encrypted = prefs.getString(KEY_GITHUB_TOKEN_ENC, "") ?: ""
        return CryptoManager.decrypt(encrypted)
    }

    fun clearGitHubToken() {
        prefs.edit().remove(KEY_GITHUB_TOKEN_ENC).remove(KEY_GITHUB_USER).remove(KEY_GITHUB_AVATAR).apply()
    }

    fun saveGitHubUserInfo(username: String, avatarUrl: String) {
        prefs.edit()
            .putString(KEY_GITHUB_USER, username)
            .putString(KEY_GITHUB_AVATAR, avatarUrl)
            .apply()
    }

    fun getGitHubUsername(): String = prefs.getString(KEY_GITHUB_USER, "") ?: ""
    fun getGitHubAvatar(): String = prefs.getString(KEY_GITHUB_AVATAR, "") ?: ""

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _language.value = lang
    }

    fun setGitAuthor(name: String, email: String) {
        prefs.edit().putString(KEY_GIT_NAME, name).putString(KEY_GIT_EMAIL, email).apply()
        _gitAuthorName.value = name
        _gitAuthorEmail.value = email
    }

    fun setDefaultBranch(branch: String) {
        prefs.edit().putString(KEY_DEFAULT_BRANCH, branch).apply()
        _defaultBranch.value = branch
    }

    fun setActiveProjectId(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE_PROJECT_ID, id).apply()
        _activeProjectId.value = id
    }

    fun setEditorSettings(fontSize: Int, wordWrap: Boolean, lineNumbers: Boolean, autoSave: Boolean) {
        prefs.edit()
            .putInt(KEY_FONT_SIZE, fontSize)
            .putBoolean(KEY_WORD_WRAP, wordWrap)
            .putBoolean(KEY_LINE_NUMBERS, lineNumbers)
            .putBoolean(KEY_AUTO_SAVE, autoSave)
            .apply()
        _editorFontSize.value = fontSize
        _editorWordWrap.value = wordWrap
        _editorLineNumbers.value = lineNumbers
        _autoSave.value = autoSave
    }

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
        _aiModel.value = model
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LANG = "pref_lang"
        private const val KEY_GIT_NAME = "pref_git_name"
        private const val KEY_GIT_EMAIL = "pref_git_email"
        private const val KEY_DEFAULT_BRANCH = "pref_default_branch"
        private const val KEY_ACTIVE_PROJECT_ID = "pref_active_project_id"
        private const val KEY_FONT_SIZE = "pref_font_size"
        private const val KEY_WORD_WRAP = "pref_word_wrap"
        private const val KEY_LINE_NUMBERS = "pref_line_numbers"
        private const val KEY_AUTO_SAVE = "pref_auto_save"
        private const val KEY_GITHUB_TOKEN_ENC = "pref_gh_token_enc"
        private const val KEY_GITHUB_USER = "pref_gh_user"
        private const val KEY_GITHUB_AVATAR = "pref_gh_avatar"
        private const val KEY_AI_MODEL = "pref_ai_model"
    }
}
