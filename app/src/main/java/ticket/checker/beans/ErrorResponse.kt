package ticket.checker.beans

import java.io.Serializable

data class ErrorResponse(val id: String, val message : String) : Serializable