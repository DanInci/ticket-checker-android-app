package ticket.checker.admin.tickets

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.ActivityControlPanel.Companion.FILTER_VALIDATED
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.beans.TicketList
import ticket.checker.services.ServiceManager
import java.util.*

class TicketsFragment : AAdminFragment<TicketList, Int>() {

    private lateinit var organizationId: UUID

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapterWithHeader<TicketList, Int> {
       return TicketsAdapter(context!!)
    }

    override fun loadHeader(filterType : String?, filterValue : String) {
        val call = ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, null, null) // TODO include filterType and filterValue
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filterType: String?, filterValue : String?) {
        val call = ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, null, null, null) // TODO include filterType and filterValue
        call.enqueue(itemsCallback)
    }

    override fun onAdd(addedObject: TicketList) {
        when(filterType) {
            null -> {
                itemsAdapter.itemAdded(addedObject)
            }
            FILTER_VALIDATED -> {
                if(filterValue != "true") {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
            FILTER_SEARCH -> {
                if(addedObject.id.startsWith(filterValue, true) || (addedObject.soldTo != null && addedObject.soldTo.startsWith(filterValue, true))) {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
        }
    }

    override fun onEdit(editedObject: TicketList, editedObjectPosition: Int) {
        when(filterType) {
            null -> {
                itemsAdapter.itemEdited(editedObject, editedObjectPosition)
            }
            FILTER_VALIDATED -> {
                if((editedObject.validatedAt == null && filterValue == "false") || (editedObject.validatedAt != null && filterValue == "true")) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
            FILTER_SEARCH -> {
                if((editedObject.soldTo != null && editedObject.soldTo.startsWith(filterValue, true)) || editedObject.id.startsWith(filterValue, true)) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
        }
    }

    companion object {
        fun newInstance(filterType : String?, filterValue : String): TicketsFragment {
            val fragment = TicketsFragment()
            val args = Bundle()
            args.putString(FILTER_TYPE, filterType)
            args.putString(FILTER_VALUE, filterValue)
            fragment.arguments = args
            return fragment
        }
    }
}