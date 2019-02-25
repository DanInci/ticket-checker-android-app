package ticket.checker.admin.users

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ticket.checker.ActivityControlPanel
import ticket.checker.ActivityControlPanel.Companion.FILTER_ROLE
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.User
import ticket.checker.extras.UserType
import ticket.checker.extras.Util.CURRENT_USER
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.POSITION
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

    override fun setHeaderVisibility(holder: RecyclerView.ViewHolder, isVisible: Boolean) {
        (holder as HeaderHolder).setVisibility(isVisible)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType: String?, filterValue : String, itemStats: Int?) {
        (holder as HeaderHolder).updateUsersHeaderInfo(filterType, filterValue, itemStats)
    }

    override fun launchInfoActivity(view: View, position : Int) {
        if(isItemPosition(position)) {
            val activity = context as Activity
            val intent  = Intent(activity, ActivityUserDetails::class.java)
            intent.putExtra(POSITION, position)
            intent.putExtra(CURRENT_USER, items[position-1])
            activity.startActivityForResult(intent, ActivityControlPanel.CHANGES_TO_ADAPTER_ITEM)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun itemAdded(addedItem: User) {
        var newItemStats = itemStats?: 0
        items.add(0,addedItem)
        notifyItemInserted(1)
        newItemStats++
        updateHeaderInfo(filterType, filterValue, newItemStats)
    }

    override fun itemEdited(editedItem: User, position: Int) {
        if(isItemPosition(position)) {
            items.removeAt(position - 1)
            items.add(position - 1, editedItem)
            notifyItemChanged(position)
        }
    }

    override fun itemRemoved(position: Int) {
        if(isItemPosition(position)) {
            var newItemStats = itemStats?:0
            newItemStats--
            items.removeAt(position - 1)
            notifyItemRemoved(position)
            updateHeaderInfo(filterType, filterValue, newItemStats)
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
            setUserType(user.userType)
            setCreatedAt(user.createdAt)
        }

        private fun setName(name : String) {
            tvName.text = name
        }

        private fun setUserType(userType : UserType) {
            tvRole.text = userType.name
            if(userType == UserType.USER) {
                tvRole.visibility = View.GONE
            }
            else {
                tvRole.visibility = View.VISIBLE
                tvRole.setTextColor(ContextCompat.getColor(itemView.context, userType.colorResource))
            }

            if(userType ==  UserType.ADMIN) {
                icPerson.background = ContextCompat.getDrawable(itemView.context, R.drawable.ic_admin)
            }
            else {
                icPerson.background = ContextCompat.getDrawable(itemView.context, R.drawable.ic_user)
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
            else -> items[position-1].userId!!
        }
    }

    private class HeaderHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsersNumbers : TextView = itemView.findViewById(R.id.tvUsersNumbers)

        fun setVisibility(isVisible : Boolean) {
            tvUsersNumbers.visibility = if(isVisible) View.VISIBLE else View.INVISIBLE
        }

        fun updateUsersHeaderInfo(filterType : String?, filterValue : String, totalUsers : Int?) {
            if(totalUsers == null)
                return

            if(totalUsers == 0) {
                tvUsersNumbers.visibility = View.VISIBLE
                tvUsersNumbers.text = "No accounts found"
            }
            else {
                tvUsersNumbers.visibility = View.VISIBLE
                val users = when(filterType) {
                    null -> {
                        if (totalUsers == 1)  "account" else "accounts"
                    }
                    FILTER_ROLE -> {
                        if (totalUsers == 1)  filterValue else filterValue + "s"
                    }
                    else -> {
                        if (totalUsers == 1)  "account" else "accounts"
                    }
                }
                tvUsersNumbers.text = "There is a total of $totalUsers $users"
            }
        }
    }
}