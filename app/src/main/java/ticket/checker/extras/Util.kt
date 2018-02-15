package ticket.checker.extras

import okhttp3.ResponseBody
import ticket.checker.beans.ErrorResponse
import ticket.checker.services.ServiceManager
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Dani on 25.01.2018.
 */
object Util {
    var userId : Long? = null
    var userName : String? = null
    var userRole : String? = null
    var userCreatedDate : Date? = null
    var userSoldTicketsNo : Int? = null
    var userValidatedTicketsNo  : Int? = null

    val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy")
    val DATE_FORMAT_WITH_HOUR = SimpleDateFormat("dd MMM yyyy HH:mm")

    const val ROLE_USER = "ROLE_USER"
    const val ROLE_ADMIN = "ROLE_ADMIN"

    const val ERROR_TICKET_VALIDATION = "TicketValidationException"
    const val ERROR_TICKET_EXISTS = "TicketExistsException"
    const val ERROR_USERNAME_EXISTS = "UsernameExistsException"

    const val TICKET_NUMBER = "ticketNumber"
    const val TICKET_STATUS = "ticketStatus"
    const val POSITION = "adapterPosition"

    fun formatDate(date : Date) : String {
        val then = date.time
        val now = Date().time
        val diff = now - then

        return when(diff) {
            in 0..59999 -> {
                "Validated a few seconds ago"
            }
            in 60000..3599999 -> {
                "Validated " + diff/60000 + " minutes ago"
            }
            in 3600000..86399999 -> {
                "Validated " + diff/3600000 + " hours ago"
            }
            in 86400000..604799999 -> {
                "Validated " + diff/86400000 + " days ago"
            }
            else -> {
                "Validated " + "at " + DATE_FORMAT.format(date)
            }
        }
    }

    fun convertError(errorBody : ResponseBody?) : ErrorResponse {
        return try {
            ServiceManager.errorConverter.convert(errorBody)
            }
        catch (e : Exception) {
            ErrorResponse(Date(),"","")
        }
    }

    fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }
}