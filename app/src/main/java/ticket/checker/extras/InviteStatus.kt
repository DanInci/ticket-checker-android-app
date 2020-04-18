package ticket.checker.extras

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class InviteStatus : Serializable {

    @SerializedName("PENDING")
    PENDING,

    @SerializedName("ACCEPTED")
    ACCEPTED,

    @SerializedName("DECLINED")
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
