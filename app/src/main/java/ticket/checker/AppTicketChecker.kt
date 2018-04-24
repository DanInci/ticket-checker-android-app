package ticket.checker

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import org.acra.ACRA
import org.acra.ReportField.*
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraToast
import ticket.checker.extras.UserType
import ticket.checker.reports.CustomReportsSenderFactory
import ticket.checker.services.ServiceManager
import java.util.*

/**
 * Created by Dani on 16.02.2018.
 */
@AcraCore(reportSenderFactoryClasses = [CustomReportsSenderFactory::class],
        reportContent = [USER_APP_START_DATE, APP_VERSION_NAME, APP_VERSION_CODE, ANDROID_VERSION, PHONE_MODEL, STACK_TRACE])
@AcraToast(resText = R.string.error_report_notification_text)
class AppTicketChecker : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        loadConnectionConfig()
        loadSession()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }

    companion object {
        lateinit var appContext: Application

        var appName = "Ticket Checker"
        var address = ""
        var port = ""
        var isLoggedIn = false

        var loggedInUserId: Long? = null
        var loggedInUserName: String? = null
        var loggedInUserType: UserType = UserType.USER
        var loggedInUserCreatedDate: Date? = null
        var loggedInUserSoldTicketsNo: Int? = null
        var loggedInUserValidatedTicketsNo: Int? = null

        var pretendedUserType: UserType = UserType.USER

        fun clearSession() {
            ServiceManager.invalidateSession()
            deleteSessionPreferences()
            isLoggedIn = false
            loggedInUserId = null
            loggedInUserName = null
            loggedInUserType = UserType.USER
            loggedInUserCreatedDate = null
            loggedInUserSoldTicketsNo = null
            loggedInUserValidatedTicketsNo = null
            pretendedUserType = UserType.USER
        }

        fun saveConnectionConfig(appName: String, address: String, port: String) {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val editor = pref.edit()
            editor.putString(SAVED_APP_NAME, appName)
            editor.putString(SAVED_ADDRESS, address)
            editor.putString(SAVED_PORT, port)
            editor.apply()
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

        private fun loadConnectionConfig() {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val appName = pref.getString(SAVED_APP_NAME, NOT_FOUND)
            val address = pref.getString(SAVED_ADDRESS, NOT_FOUND)
            val port = pref.getString(SAVED_PORT, NOT_FOUND)
            if (appName != NOT_FOUND && address != NOT_FOUND && port != NOT_FOUND) {
                this.address = address
                this.port = port
                this.appName = appName
            }
        }

        private fun deleteSessionPreferences() {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val editor = pref.edit()
            editor.remove(SAVED_USERNAME)
            editor.remove(SAVED_PASSWORD)
            editor.apply()
        }

        private const val SAVED_ADDRESS = "ticket.checker.savedAddress"
        private const val SAVED_PORT = "ticket.checker.savedPort"
        private const val SAVED_APP_NAME = "ticket.checker.savedAppName"
        private const val SAVED_USERNAME = "ticket.checker.savedUsername"
        private const val SAVED_PASSWORD = "ticket.checker.savedPassword"
        private const val NOT_FOUND = "NOT_FOUND"
    }
}