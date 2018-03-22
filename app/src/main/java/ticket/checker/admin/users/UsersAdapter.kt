package ticket.checker.admin.users

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ticket.checker.ActivityAdmin
import ticket.checker.ActivityAdmin.Companion.LIST_ADMINS
import ticket.checker.ActivityAdmin.Companion.LIST_ALL
import ticket.checker.ActivityAdmin.Companion.LIST_USERS
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.admin.tickets.ActivityTicketDetails
import ticket.checker.beans.User
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.POSITION
import ticket.checker.extras.Util.ROLE_ADMIN
import ticket.checker.extras.Util.USER_ID
import ticket.checker.extras.Util.USER_NAME
import ticket.checker.extras.Util.USER_ROLE
import java.util.*

/**
 * Created by Dani on 09.02.2018.
 */
class UsersAdapter(val context : Context) : AItemsAdapter<User, Int>(context) {

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

    override fun launchInfoActivity(view: View, position : Int) {
        val activity = context as Activity
        val intent  = Intent(activity, ActivityUserDetails::class.java)
        val userId = items[position-1].id
        val userName = items[position-1].name
        val userRole = items[position-1].role
        intent.putExtra(POSITION, position)
        intent.putExtra(USER_ID, userId)
        intent.putExtra(USER_NAME, userName)
        intent.putExtra(USER_ROLE, userRole)
        activity.startActivityForResult(intent, ActivityAdmin.CHANGES_TO_ADAPTER_ITEM)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun itemAdded(addedItem: User) {
        var newItemStats = itemStats?: 0
        items.add(0,addedItem)
        notifyItemInserted(1)
        newItemStats++
        updateHeaderInfo(filter, newItemStats)
    }

    override fun itemRemoved(position: Int) {
        if(isItemPosition(position)) {
            var newItemStats = itemStats?:0
            newItemStats--
            items.removeAt(position - 1)
            notifyItemRemoved(position)
            updateHeaderInfo(filter, newItemStats)
        }
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

    override fun getItemId(position: Int): Long {
        return when(position) {
            0, items.size + 1 -> RecyclerView.NO_ID
            else -> items[position-1].id!!
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
                tvUsersNumbers.text = "There is a total of $totalUsers $users"
            }
        }
    }
}