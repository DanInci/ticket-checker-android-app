package ticket.checker.extras

import java.io.Serializable

enum class InviteStatus : Serializable {
    PENDING,
    ACCEPTED,
    DECLINED;

    companion object {
        fun from(status : String) : InviteStatus? = when(status) {
            "PENDING" -> PENDING
            "ACCEPTED" -> ACCEPTED
            "DECLINED" -> DECLINED
            else -> null
        }

        fun to(status : InviteStatus) : String = when(status) {
            PENDING -> "PENDING"
            ACCEPTED -> "ACCEPTED"
            DECLINED -> "DECLINED"
        }
    }
}
