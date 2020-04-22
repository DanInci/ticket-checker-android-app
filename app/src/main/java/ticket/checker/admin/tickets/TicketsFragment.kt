package ticket.checker.admin.tickets

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.ActivityControlPanel.Companion.FILTER_VALIDATED
import ticket.checker.ActivityMenu.Companion.ORGANIZATION_ID
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.beans.TicketList
import ticket.checker.extras.TicketCategory
import ticket.checker.services.ServiceManager
import java.util.*

class TicketsFragment : AAdminFragment<TicketList, Int>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapterWithHeader<TicketList, Int> {
       return TicketsAdapter(context!!)
    }

    override fun loadHeader(filterType : String?, filterValue : String) {
        val call = when(filterType) {
            FILTER_VALIDATED ->
                when(filterValue) {
                    "true" -> ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, TicketCategory.VALIDATED, null)
                    "false" -> ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, TicketCategory.SOLD, null)
                    else -> ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, null, null)
                }
            FILTER_SEARCH -> ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, null, filterValue)
            else -> ServiceManager.getStatisticsService().getTicketsNumbers(organizationId, null, null)
        }
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filterType: String?, filterValue : String?) {
        val call = when(filterType) {
            FILTER_VALIDATED ->
                when(filterValue) {
                    "true" -> ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, TicketCategory.VALIDATED, null, null)
                    "false" -> ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, TicketCategory.SOLD, null, null)
                    else -> ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, null, null, null)
                }
            FILTER_SEARCH -> ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, null,null, filterValue)
            else -> ServiceManager.getTicketService().getTicketsForOrganization(organizationId, page, loadLimit, null, null, null)
        }
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
        fun newInstance(organizationId: UUID, filterType : String?, filterValue : String): TicketsFragment {
            val fragment = TicketsFragment()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putString(FILTER_TYPE, filterType)
            args.putString(FILTER_VALUE, filterValue)
            fragment.arguments = args
            return fragment
        }
    }
}