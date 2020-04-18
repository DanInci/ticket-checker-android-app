package ticket.checker.admin.organizations

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.OrganizationList
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME

class OrganizationsAdapter(context : Context) : AItemsAdapter<OrganizationList>(context) {

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.organization_row, parent, false)
        return OrganizationHolder(view)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: OrganizationList) {
        (holder as OrganizationHolder).updateOrganizationHolderItem(item)
    }

    override fun getItemId(item: OrganizationList): Long {
        return item.id.mostSignificantBits
    }

    private class OrganizationHolder(itemView : View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val tvOrganizationName: TextView = itemView.findViewById(R.id.tvOrganizationName)
        private val tvOrganizationRole: TextView = itemView.findViewById(R.id.tvOrganizationRole)
        private val tvJoinedAt: TextView = itemView.findViewById(R.id.tvJoinedAt)

        override fun onClick(v: View?) {}

        fun updateOrganizationHolderItem(item: OrganizationList) {
            tvOrganizationName.text = item.name
            tvOrganizationRole.text = item.membership.role.role
            tvOrganizationRole.setTextColor(ContextCompat.getColor(itemView.context, item.membership.role.colorResource))
            tvJoinedAt.text = DATE_FORMAT_MONTH_NAME.format(item.membership.joinedAt)
        }
    }

}