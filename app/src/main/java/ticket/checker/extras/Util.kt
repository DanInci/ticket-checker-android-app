package ticket.checker.extras

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Dani on 25.01.2018.
 */
object Util {
    val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy")

    const val ROLE_USER = "ROLE_USER"
    const val ROLE_ADMIN = "ROLE_ADMIN"

    const val SESSION_USER_ID = "userId"
    const val SESSION_USER_NAME = "userName"
    const val SESSION_USER_ROLE = "userRole"
    const val SESSION_USER_CREATED_DATE = "userCreatedDate"
    const val SESSION_USER_SOLD_TICKETS = "userSoldTickets"
    const val SESSION_USER_VALIDATED_TICKETS = "userValidatedTickets"

    const val PRETENDED_USER_ROLE = "pretendedUserRole"
    const val CURRENT_TOOLBAR_IMG = "currentToolbarImg"

    fun formatDate(date : Date) : String {
        val then = date.time
        val now = Date().time
        val diff = now - then

        return when(diff) {
            in 0..3599999 -> {
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
}