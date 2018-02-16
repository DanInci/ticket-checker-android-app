package ticket.checker

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import ticket.checker.services.ServiceManager
import java.util.*

/**
 * Created by Dani on 16.02.2018.
 */
class AppTicketChecker : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        loadSession()
    }

    companion object {
        private var appContext : Context? = null

        var isLoggedIn = false

        var userId: Long? = null
        var userName: String? = null
        var userRole: String? = null
        var userCreatedDate: Date? = null
        var userSoldTicketsNo: Int? = null
        var userValidatedTicketsNo: Int? = null

        fun clearSession() {
            ServiceManager.invalidateSession()
            deleteSessionPreferences()
            isLoggedIn = false
            userId = null
            userName = null
            userRole = null
            userCreatedDate = null
            userSoldTicketsNo = null
            userValidatedTicketsNo = null
        }

        fun saveSession(username: String, password: String) {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val editor = pref.edit()
            editor.putString(SAVED_USERNAME, username)
            editor.putString(SAVED_PASSWORD, password)
            editor.apply()
        }

        fun loadSession() {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val username = pref.getString(SAVED_USERNAME, NOT_FOUND)
            val password = pref.getString(SAVED_PASSWORD, NOT_FOUND)
            if (username != NOT_FOUND && password != NOT_FOUND) {
                ServiceManager.createSession(username, password, false)
                isLoggedIn = true
            }
        }

        private fun deleteSessionPreferences() {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val editor = pref.edit()
            editor.remove(SAVED_USERNAME)
            editor.remove(SAVED_PASSWORD)
            editor.apply()
        }

        private const val SAVED_USERNAME = "ticket.checker.savedUsername"
        private const val SAVED_PASSWORD = "ticket.checker.savedPassword"
        private const val NOT_FOUND = "NOT_FOUND"
    }


}