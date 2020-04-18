package ticket.checker.extras

import com.google.gson.annotations.SerializedName
import java.io.Serializable


enum class TicketCategory(val category: String) : Serializable {

    @SerializedName("SOLD")
    SOLD("Sold"),

    @SerializedName("VALIDATED")
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