package ticket.checker.extras

import java.io.Serializable


enum class TicketCategory(val category: String) : Serializable {
    SOLD("Sold"),
    VALIDATED("Validated");

    companion object {
        fun from(category : String) : TicketCategory? = when(category) {
            "SOLD" -> SOLD
            "VALIDATED" -> VALIDATED
            else -> null
        }

        fun to(category : TicketCategory) : String = when(category) {
            SOLD -> "SOLD"
            VALIDATED -> "VALIDATED"
        }
    }
}