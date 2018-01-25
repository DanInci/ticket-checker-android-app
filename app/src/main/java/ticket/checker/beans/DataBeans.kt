package ticket.checker.beans

import java.util.*

/**
 * Created by Dani on 24.01.2018.
 */
data class User(var id : Long,var username : String, var password : String, var name : String, var role : String, var createdDate : Date)
data class Ticket(var ticketId : Long, var soldBy : User, var soldTo : String, var soldAt : Date, var validatedBy : User, var validatedAt : Date)