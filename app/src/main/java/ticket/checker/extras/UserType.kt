package ticket.checker.extras

import ticket.checker.R
import java.io.Serializable

/**
 * Created by Dani on 24.03.2018.
 */
enum class UserType(val role : String, val colorResource : Int ) : Serializable {
    ADMIN("Admin", R.color.yesGreen),
    PUBLISHER("Publisher", R.color.deepPurple),
    VALIDATOR("Validator", R.color.materialYellow),
    USER("User", R.color.darkerGrey);

    companion object {
        fun fromRoleToUserType(role : String?) : UserType = when(role) {
            "ROLE_ADMIN" -> ADMIN
            "ROLE_PUBLISHER" -> PUBLISHER
            "ROLE_VALIDATOR" -> VALIDATOR
            else -> USER
        }

        fun fromUserTypeToRole(userType : UserType) : String = when(userType) {
            ADMIN -> "ROLE_ADMIN"
            PUBLISHER -> "ROLE_PUBLISHER"
            VALIDATOR -> "ROLE_VALIDATOR"
            USER -> "ROLE_USER"
        }
    }
}