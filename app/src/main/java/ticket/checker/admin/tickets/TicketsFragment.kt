package ticket.checker.admin.tickets

import android.os.Bundle
import ticket.checker.ActivityAdmin.Companion.LIST_ALL
import ticket.checker.ActivityAdmin.Companion.LIST_NOT_VALIDATED
import ticket.checker.ActivityAdmin.Companion.LIST_VALIDATED
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.admin.FilterChangeListener
import ticket.checker.beans.Ticket
import ticket.checker.services.ServiceManager

class TicketsFragment : AAdminFragment<Ticket, Array<Int>>(), FilterChangeListener {

    override fun setupItemsAdapter(): AItemsAdapter<Ticket, Array<Int>> {
       return TicketsAdapter(activity.applicationContext)
    }

    override fun loadHeader(filter : String) {
        val call = ServiceManager.getNumbersService().getFilteredTicketNumbers(filter)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filter : String) {
        val ticketService = ServiceManager.getTicketService()
        when (filter) {
            LIST_ALL -> {
                val call = ticketService.getTickets(null, page, LOAD_LIMIT)
                call.enqueue(itemsCallback)
            }
            LIST_VALIDATED -> {
                val call = ticketService.getTickets(true, page, LOAD_LIMIT)
                call.enqueue(itemsCallback)
            }
            LIST_NOT_VALIDATED -> {
                val call = ticketService.getTickets(false, page, LOAD_LIMIT)
                call.enqueue(itemsCallback)
            }
        }
    }

    companion object {
        private const val LOAD_LIMIT = 20

        fun newInstance(ticketsFilter : String): TicketsFragment {
            val fragment = TicketsFragment()
            val args = Bundle()
            args.putString(FILTER, ticketsFilter)
            fragment.arguments = args
            return fragment
        }
    }
}