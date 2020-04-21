package ticket.checker.admin.invitations

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.admin.listeners.InviteResponseListener
import ticket.checker.beans.OrganizationInviteList
import ticket.checker.extras.Util
import java.util.*

class InvitesAdapter(context : Context, private val inviteResponseListener: InviteResponseListener) : AItemsAdapter<OrganizationInviteList>(context) {

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.invite_row, parent, false)
        return InviteHolder(view, inviteResponseListener)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: OrganizationInviteList) {
        (holder as InviteHolder).updateInviteHolderItem(item)
    }

    override fun getItemId(item: OrganizationInviteList): Long {
        return item.id.mostSignificantBits
    }

    fun getItemPosition(organizationId: UUID, inviteId: UUID): Int? {
        val foundItem = items.withIndex().find { (_, t) -> t.id == inviteId && t.organizationId == organizationId }
        if(foundItem != null) {
            return foundItem.index
        }
        return null
    }

    private class InviteHolder(itemView : View, private val inviteResponseListener: InviteResponseListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val tvOrganizationName: TextView = itemView.findViewById(R.id.tvOrganizationName)
        private val tvInvitedAt: TextView = itemView.findViewById(R.id.tvInvitedAt)
        private val btnAccept: ImageButton = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: ImageButton = itemView.findViewById(R.id.btnDecline)
        private val loadingSpinner: ProgressBar = itemView.findViewById(R.id.loadingSpinner)

        lateinit var item: OrganizationInviteList

        init {
            btnAccept.setOnClickListener(this)
            btnDecline.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            when(v?.id) {
                R.id.btnAccept -> {
                    btnAccept.visibility = View.GONE
                    btnDecline.visibility = View.GONE
                    loadingSpinner.visibility = View.VISIBLE
                    inviteResponseListener.inviteAccepted(this.item)
                }
                R.id.btnDecline -> {
                    btnAccept.visibility = View.GONE
                    btnDecline.visibility = View.GONE
                    loadingSpinner.visibility = View.VISIBLE
                    inviteResponseListener.inviteDeclined(this.item)
                }
            }
        }

        fun updateInviteHolderItem(item: OrganizationInviteList) {
            btnAccept.visibility = View.VISIBLE
            btnDecline.visibility = View.VISIBLE
            loadingSpinner.visibility = View.GONE

            this.item = item

            tvOrganizationName.text = "Organization Name"
            tvInvitedAt.text = Util.DATE_FORMAT_MONTH_NAME.format(item.invitedAt)
        }
    }

}