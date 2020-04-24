package ticket.checker.extras

import androidx.fragment.app.FragmentManager
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import ticket.checker.AppTicketChecker
import ticket.checker.beans.ErrorResponse
import ticket.checker.dialogs.DialogInfo
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


/**
 * Created by Dani on 25.01.2018.
 */
object Util {
    val DATE_FORMAT_MONTH_NAME = SimpleDateFormat("dd MMM yyyy")
    val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy")
    val DATE_TIME_FORMAT = SimpleDateFormat("dd MMM yyyy HH:mm")

    const val POSITION = "adapterPosition"
    const val PAGE_SIZE = 20

    fun formatDate(then : Date, shortFormat : Boolean) : String {
        val now = Date()
        val diff = (now.time - then.time) / 1000

        return when(diff) {
            in 0..59 -> {
                if(shortFormat) "few seconds ago"  else "Validated a few seconds ago"
            }
            in 60..3599 -> {
                if(shortFormat) "${diff/60} minutes ago"  else "Validated ${diff/60} minutes ago"
            }
            in 3600..86399 -> {
                if(shortFormat) "${diff/3600} hours ago"  else "Validated ${diff/3600} hours ago"
            }
            in 86400..604799 -> {
                if(shortFormat) "${diff/86400} days ago"  else "Validated ${diff/86400} days ago"
            }
            else -> {
                if(shortFormat) DATE_FORMAT_MONTH_NAME.format(then)  else "Validated at " + DATE_FORMAT_MONTH_NAME.format(then)
            }
        }
    }

    fun isTicketFormatValid(ticketNumber : String?) : Boolean {
        var isValid = ticketNumber != null
        if(isValid) {
            if (ticketNumber!!.contains(" ") || ticketNumber.contains("http://") || ticketNumber.contains("https://")) {
                isValid = false
            }
        }
        return isValid
    }

    fun isPasswordValid(pass: String): Boolean {
        return Pattern.compile("(?=^.{6,}$)(?=.*[A-Z])(?=.*[a-z]).*$").matcher(pass).matches()
    }

    fun isEmailValid(email: String): Boolean {
        return Pattern.compile(
                "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                        + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                        + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                        + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                        + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
        ).matcher(email).matches()
    }

    fun convertError(errorBody : ResponseBody?) : ErrorResponse? {
        return if (errorBody != null){
            try {
                val jsonError = JSONObject(errorBody.string())
                ErrorResponse(jsonError.getString("id"),jsonError.getString("message"))
            }
            catch (e : Exception) {
                null
            }
        } else {
            null
        }

    }

    fun <T> treatBasicError(call : Call<T>, response : Response<T>?, fragmentManager : FragmentManager) : Boolean {
        var hasResponded = false
        if(response == null) {
            val dialogConnectionError = DialogInfo.newInstance("Connection error", "There was an error connecting to the server", DialogType.ERROR)
            dialogConnectionError.show(fragmentManager, "DIALOG_ERROR")
            hasResponded = true
        }
        else {
            when(response.code()) {
                401 -> {
                    AppTicketChecker.clearSession()
                    val dialogAuthError = DialogInfo.newInstance("Session expired", "You need to provide your authentication once again", DialogType.AUTH_ERROR)
                    dialogAuthError.isCancelable = false
                    dialogAuthError.show(fragmentManager, "DIALOG_SESSION_ERROR")
                    hasResponded = true
                }
                403 -> {
                    val dialogError = DialogInfo.newInstance("Authorization required", "You are not allowed to do this action", DialogType.ERROR)
                    dialogError.show(fragmentManager, "DIALOG_AUTH_ERROR")
                    hasResponded = true
                }
                500 -> {
                    val dialogServerError = DialogInfo.newInstance("Server error", "Oops, there was a server error", DialogType.ERROR)
                    dialogServerError.show(fragmentManager, "DIALOG_SERVER_ERROR")
                    hasResponded = true
                }
            }
        }
        return hasResponded
    }
}