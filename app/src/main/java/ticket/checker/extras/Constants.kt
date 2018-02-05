package ticket.checker.extras

import java.text.SimpleDateFormat

/**
 * Created by Dani on 25.01.2018.
 */
object Constants {
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

    const val SCANNED_TICKET_NUMBER = "scannedTicketNumber"
}