package ticket.checker.beans

import java.util.*

/**
 * Created by Dani on 24.01.2018.
 */
data class User(var id : Long?,var username : String, var password : String, var name : String, var role : String, var createdDate : Date?, var soldTicketsNo : Int?, var validatedTicketsNo : Int?) {
    constructor(username : String, password : String, name : String, role : String) : this(null, username, password, name, role, null, null, null)
}
data class Ticket(val ticketId : String,val soldBy : User?, val soldTo : String, val soldAt : Date?, val validatedBy : User?, var validatedAt : Date?) {
    constructor(ticketId : String, soldTo : String ) : this (ticketId, null, soldTo, null, null, null)
}
data class Statistic(val date : Date, val count : Int)
data class ErrorResponse(val timestamp : Date, val message : String, val details : String)