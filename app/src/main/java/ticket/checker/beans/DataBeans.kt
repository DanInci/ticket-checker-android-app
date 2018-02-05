package ticket.checker.beans

import java.util.*

/**
 * Created by Dani on 24.01.2018.
 */
data class User(var id : Long,var username : String, var password : String, var name : String, var role : String, var createdDate : Date, var soldTicketsNo : Int, var validatedTicketsNo : Int)
data class Ticket(var ticketId: String, var soldTo : String) {
    constructor(ticketId : String, soldBy : User, soldTo : String, soldAt : Date, validatedBy : User, validatedAt : Date) : this (ticketId, soldTo)
}