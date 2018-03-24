package ticket.checker.beans

import ticket.checker.extras.UserType
import java.util.*

/**
 * Created by Dani on 24.01.2018.
 */
data class User(var id : Long?,var username : String, var password : String, var name : String, var role : String, var createdDate : Date?, var soldTicketsNo : Int?, var validatedTicketsNo : Int?) {
    constructor(username : String, password : String, name : String, userType : UserType) : this(null, username, password, name, UserType.fromUserTypeToRole(userType), null, null, null)

    val userType : UserType
        get() = UserType.fromRoleToUserType(role)
}
data class Ticket(val ticketId : String,val soldBy : User?, val soldTo : String, val soldToBirthdate : Date?, val soldAt : Date?, val validatedBy : User?, var validatedAt : Date?) {
    constructor(ticketId : String, soldTo : String, soldToBirthdate : Date?) : this (ticketId, null, soldTo, soldToBirthdate,null, null, null)
}
data class Statistic(val date : Date, val count : Int)
data class ErrorResponse(val timestamp : Date, val message : String, val details : String)