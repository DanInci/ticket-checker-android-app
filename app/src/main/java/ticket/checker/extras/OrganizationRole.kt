package ticket.checker.extras

import ticket.checker.R
import java.io.Serializable
import com.google.gson.annotations.SerializedName;


enum class OrganizationRole(val role : String, val colorResource : Int ) : Serializable {

    @SerializedName("OrganizationOwner")
    OWNER("Owner", R.color.noRed),

    @SerializedName("Admin")
    ADMIN("Admin", R.color.yesGreen),

    @SerializedName("Publisher")
    PUBLISHER("Publisher", R.color.deepPurple),

    @SerializedName("Validator")
    VALIDATOR("Validator", R.color.materialYellow),

    @SerializedName("User")
    USER("User", R.color.darkerGrey);

    companion object {
        fun from(role : String) : OrganizationRole? = when(role) {
            "OrganizationOwner" -> OWNER
            "Admin" -> ADMIN
            "Publisher" -> PUBLISHER
            "Validator" -> VALIDATOR
            "User" -> USER
            else -> null
        }

        fun to(role : OrganizationRole) : String = when(role) {
            OWNER -> "OrganizationOwner"
            ADMIN -> "Admin"
            PUBLISHER -> "Publisher"
            VALIDATOR -> "Validator"
            USER -> "User"
        }
    }
}