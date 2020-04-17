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
        loadConnectionConfig()
        loadSession()
    }

    companion object {
        lateinit var appContext: Application

        var host: String = ""
        var port: String = ""
        var isLoggedIn = false
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

        private val loginCallback : Callback<LoginResponse> = object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if(response.isSuccessful && response.body() != null) {
                    isLoggedIn = true
                    loggedInUser = response.body()!!.profile
                    ServiceManager.createSession(response.body()!!.token)

                }
                else {
                    ServiceManager.invalidateSession()
                    isLoggedIn = false
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable?) {
                ServiceManager.invalidateSession()
                isLoggedIn = false
            }
        }


        fun clearSession() {
            ServiceManager.invalidateSession()
            deleteSessionPreferences()
            this.isLoggedIn = false
            this.loggedInUser = null
        }

        fun saveConnectionConfig(host: String, port: String) {
            val editor = sharedPreferences.edit()
            editor.putString(PREF_HOST, host)
            editor.putString(PREF_PORT, port)
            editor.apply()
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
                   val call : Call<LoginResponse> = ServiceManager.getAuthService().login(LoginData(e, p))
                   call.enqueue(loginCallback)
               }
        }

        private fun loadConnectionConfig() {
            val host = sharedPreferences.getString(PREF_HOST, null)
            val port = sharedPreferences.getString(PREF_PORT,  null)
            safeLet(host, port) { h, p ->
                this.host = h
                this.port = p
            }
        }

        private fun deleteSessionPreferences() {
            val editor = sharedPreferences.edit()
            editor.remove(PREF_LOGGED_IN_EMAIL)
            editor.remove(PREF_LOGGED_IN_PASSWORD)
            editor.apply()
        }

        private const val PREF_HOST = "ticket.checker.host"
        private const val PREF_PORT = "ticket.checker.post"
        private const val PREF_LOGGED_IN_EMAIL = "ticket.checker.loggedInEmail"
        private const val PREF_LOGGED_IN_PASSWORD = "ticket.checker.loggedInPassword"
    }
}