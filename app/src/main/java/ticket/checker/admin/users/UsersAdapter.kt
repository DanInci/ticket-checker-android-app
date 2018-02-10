package ticket.checker.admin.users

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ticket.checker.ActivityAdmin.Companion.LIST_ADMINS
import ticket.checker.ActivityAdmin.Companion.LIST_ALL
import ticket.checker.ActivityAdmin.Companion.LIST_USERS
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.User
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.ROLE_ADMIN
import java.util.*

/**
 * Created by Dani on 09.02.2018.
 */
class UsersAdapter(context : Context) : AItemsAdapter<User, Int>() {
    private val inflater = LayoutInflater.from(context)

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.user_row, parent, false)
        return UserHolder(view)
    }

    override fun inflateHeaderHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.user_row_header, parent, false)
        return HeaderHolder(view)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: User) {
        (holder as UserHolder).updateUserHolderInfo(item)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filter: String, itemStats: Int?) {
        (holder as HeaderHolder).updateUsersHeaderInfo(filter, itemStats)
    }

    private class UserHolder(itemView : View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val icPerson : ImageView = itemView.findViewById(R.id.ic_person)
        private val tvName : TextView = itemView.findViewById(R.id.tvName)
        private val tvRole : TextView = itemView.findViewById(R.id.tvRole)
        private val tvCreatedAt : TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val createdAtRow : LinearLayout = itemView.findViewById(R.id.createdAtRow)

        override fun onClick(v: View?) {}

        fun updateUserHolderInfo(user : User) {
            setName(user.name)
            setRole(user.role)
            setCreatedAt(user.createdDate)
        }

        private fun setName(name : String) {
            tvName.text = name
        }

        private fun setRole(role : String) {
            if(role == ROLE_ADMIN) {
                tvRole.visibility = View.VISIBLE
                icPerson.background = itemView.context.resources.getDrawable(R.drawable.ic_person_green)
            }
            else {
                tvRole.visibility = View.GONE
                icPerson.background = itemView.context.resources.getDrawable(R.drawable.ic_person_grey)
            }
        }

        private fun setCreatedAt(date : Date?) {
            if(date != null) {
                createdAtRow.visibility = View.VISIBLE
                tvCreatedAt.text = DATE_FORMAT.format(date)
            }
            else {
                createdAtRow.visibility = View.GONE
            }
        }
    }

    private class HeaderHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsersNumbers : TextView = itemView.findViewById(R.id.tvUsersNumbers)

        fun updateUsersHeaderInfo(filter : String, totalUsers : Int?) {
            if(totalUsers == null) {
                tvUsersNumbers.visibility = View.GONE
            }
            else {
                tvUsersNumbers.visibility = View.VISIBLE
                val are = if (totalUsers == 1) "is" else "are"
                var users = ""
                when(filter) {
                    LIST_ALL -> {
                        users = if (totalUsers == 1)  "registered account" else "registered accounts"
                    }
                    LIST_ADMINS -> {
                        users = if (totalUsers == 1)  "admin" else "admins"
                    }
                    LIST_USERS -> {
                        users = if (totalUsers == 1)  "user" else "users"
                    }
                }
                tvUsersNumbers.text = "There $are a total of $totalUsers $users"
            }
        }
    }
}