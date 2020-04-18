package ticket.checker

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.*

import ticket.checker.extras.safeLet
import ticket.checker.services.ServiceManager

/**
 * Created by Dani on 16.02.2018.
 */
class AppTicketChecker : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        loadSession()
    }

    companion object {
        lateinit var appContext: Application

        var savedSessionEmail: String? = null
        var savedSessionPassword: String? = null

        fun isLoggedIn(): Boolean { return loggedInUser != null }
        var loggedInUser: UserProfile? = null
        var selectedOrganization: OrganizationMembership? = null

        private val masterKeyAlias by lazy { MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC) }

        val sharedPreferences by lazy {
            EncryptedSharedPreferences.create(
                "TicketCheckerPreferences",
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun clearSession() {
            this.loggedInUser = null
            ServiceManager.invalidateSession()
            deleteSessionPreferences()
        }

        fun saveSession(email: String, password: String) {
            val editor = sharedPreferences.edit()
            editor.putString(PREF_LOGGED_IN_EMAIL, email)
            editor.putString(PREF_LOGGED_IN_PASSWORD, password)
            editor.apply()
        }

        fun loadSession() {
            val email = sharedPreferences.getString(PREF_LOGGED_IN_EMAIL, null)
            val password = sharedPreferences.getString(PREF_LOGGED_IN_PASSWORD, null)
               safeLet(email, password) { e, p ->
                   this.savedSessionEmail = e
                   this.savedSessionPassword = p
               }
        }

        private fun deleteSessionPreferences() {
            val editor = sharedPreferences.edit()
            editor.remove(PREF_LOGGED_IN_EMAIL)
            editor.remove(PREF_LOGGED_IN_PASSWORD)
            editor.apply()
        }

        private const val PREF_LOGGED_IN_EMAIL = "ticket.checker.loggedInEmail"
        private const val PREF_LOGGED_IN_PASSWORD = "ticket.checker.loggedInPassword"
    }
}