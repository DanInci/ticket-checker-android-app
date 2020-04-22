package ticket.checker.admin.members

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ticket.checker.ActivityControlPanel.Companion.FILTER_ROLE
import ticket.checker.R
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.beans.OrganizationMemberList
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME
import java.util.*

class OrganizationMembersAdapter(val context : Context) : AItemsAdapterWithHeader<OrganizationMemberList, Int>(context) {

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.user_row, parent, false)
        return MemberHolder(view)
    }

    override fun inflateHeaderHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.user_row_header, parent, false)
        return HeaderHolder(view)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: OrganizationMemberList) {
        (holder as MemberHolder).updateUserHolderInfo(item)
    }

    override fun setHeaderVisibility(holder: RecyclerView.ViewHolder, isVisible: Boolean) {
        (holder as HeaderHolder).setVisibility(isVisible)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType: String?, filterValue : String, itemStats: Int?) {
        (holder as HeaderHolder).updateMembersHeaderInfo(filterType, filterValue, itemStats)
    }

//    override fun launchInfoActivity(view: View, position : Int) {
//        if(isItemPosition(position)) {
//            val activity = context as Activity
//            val intent  = Intent(activity, ActivityUserDetails::class.java)
//            intent.putExtra(POSITION, position)
//            intent.putExtra(CURRENT_USER, items[position-1])
//            activity.startActivityForResult(intent, ActivityControlPanel.CHANGES_TO_ADAPTER_ITEM)
//            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//        }
//    }

    override fun itemAdded(addedItem: OrganizationMemberList) {
        super.itemAdded(addedItem)
        var newItemStats = headerItem?: 0
        newItemStats++
        updateHeaderInfo(filterType, filterValue, newItemStats)
    }

    override fun itemRemoved(position: Int) {
        super.itemRemoved(position)
        if(isItemPosition(position)) {
            var newItemStats = headerItem ?: 0
            newItemStats--
            updateHeaderInfo(filterType, filterValue, newItemStats)
        }
    }

    override fun getItemId(item: OrganizationMemberList): Long {
        return item.userId.mostSignificantBits
    }

    private class MemberHolder(itemView : View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val icPerson : ImageView = itemView.findViewById(R.id.ic_person)
        private val tvName : TextView = itemView.findViewById(R.id.tvName)
        private val tvOrganizationRole : TextView = itemView.findViewById(R.id.tvOrganizationRole)
        private val tvJoinedAt : TextView = itemView.findViewById(R.id.tvJoinedAt)
        private val joinedAtRow : LinearLayout = itemView.findViewById(R.id.joinedAtRow)

        override fun onClick(v: View?) {}

        fun updateUserHolderInfo(user : OrganizationMemberList) {
            setName(user.name)
            setRole(user.role)
            setJoinedAt(user.joinedAt)
        }

        private fun setName(name : String) {
            tvName.text = name
        }

        private fun setRole(role : OrganizationRole) {
            tvOrganizationRole.text = role.name
            if(role == OrganizationRole.USER) {
                tvOrganizationRole.visibility = View.GONE
            }
            else {
                tvOrganizationRole.visibility = View.VISIBLE
                tvOrganizationRole.setTextColor(ContextCompat.getColor(itemView.context, role.colorResource))
            }

            if(role ==  OrganizationRole.ADMIN) {
                icPerson.background = ContextCompat.getDrawable(itemView.context, R.drawable.ic_admin)
            }
            else {
                icPerson.background = ContextCompat.getDrawable(itemView.context, R.drawable.ic_user)
            }
        }

        private fun setJoinedAt(date : Date?) {
            if(date != null) {
                joinedAtRow.visibility = View.VISIBLE
                tvJoinedAt.text = DATE_FORMAT_MONTH_NAME.format(date)
            }
            else {
                joinedAtRow.visibility = View.GONE
            }
        }
    }

    private class HeaderHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        private val tvMembersNumber : TextView = itemView.findViewById(R.id.tvMembersNumber)

        fun setVisibility(isVisible : Boolean) {
            tvMembersNumber.visibility = if(isVisible) View.VISIBLE else View.INVISIBLE
        }

        fun updateMembersHeaderInfo(filterType : String?, filterValue : String, totalMembers : Int?) {
            if(totalMembers == null)
                return

            if(totalMembers == 0) {
                tvMembersNumber.visibility = View.VISIBLE
                tvMembersNumber.text = "No members found"
            }
            else {
                tvMembersNumber.visibility = View.VISIBLE
                val users = when(filterType) {
                    null -> {
                        if (totalMembers == 1)  "member" else "members"
                    }
                    FILTER_ROLE -> {
                        if (totalMembers == 1)  filterValue else filterValue + "s"
                    }
                    else -> {
                        if (totalMembers == 1)  "member" else "members"
                    }
                }
                tvMembersNumber.text = "There is a total of $totalMembers $users"
            }
        }
    }
}