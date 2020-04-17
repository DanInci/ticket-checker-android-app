package ticket.checker.beans

import java.time.OffsetDateTime

data class TicketsStatistic(val count: Int, val startDate: OffsetDateTime, val endDate: OffsetDateTime)