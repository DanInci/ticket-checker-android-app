package ticket.checker.extras

import java.io.Serializable

enum class IntervalType : Serializable {
    HOURLY,
    DAILY,
    WEEKLY;

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