package ticket.checker.extras

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class IntervalType(val type: String) : Serializable {

    @SerializedName("HOURLY")
    HOURLY("Hourly"),

    @SerializedName("DAILY")
    DAILY("Daily"),

    @SerializedName("WEEKLY")
    WEEKLY("Weekly");

    companion object {
        fun from(interval : String) : IntervalType? = when(interval) {
            "HOURLY" -> HOURLY
            "DAILY" -> DAILY
            "WEEKLY" -> WEEKLY
            else -> null
        }

        fun to(interval : IntervalType) : String = when(interval) {
            HOURLY -> "HOURLY"
            DAILY -> "DAILY"
            WEEKLY -> "WEEKLY"
        }
    }
}