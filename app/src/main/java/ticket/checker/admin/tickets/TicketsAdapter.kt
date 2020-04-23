package ticket.checker.admin.tickets

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import ticket.checker.ActivityControlPanel.Companion.FILTER_VALIDATED
import ticket.checker.R
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.beans.TicketList
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME
import java.util.*

/**
 * Created by Dani on 08.02.2018.
 */
class TicketsAdapter(val context: Context) : AItemsAdapterWithHeader<TicketList, Int>(context) {

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.ticket_row, parent, false)
        return TicketHolder(view)
    }

    override fun inflateHeaderHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.ticket_row_header, parent, false)
        return HeaderHolder(view)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: TicketList) {
        (holder as TicketHolder).updateTicketHolderInfo(item)
    }

    override fun setHeaderVisibility(holder: RecyclerView.ViewHolder, isVisible: Boolean) {
        (holder as HeaderHolder).setVisibility(isVisible)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType: String?, filterValue : String, itemStats: Int?) {
        (holder as HeaderHolder).updateTicketHeaderInfo(filterType, filterValue, itemStats)
    }

    override fun itemAdded(addedItem: TicketList) {
        var newItemStats = headerItem ?: 0
        newItemStats++
        updateHeaderInfo(filterType, filterValue, newItemStats)
        super.itemAdded(addedItem)
    }

    override fun itemRemoved(position: Int) {
        if(isItemPosition(position)) {
            var newItemStats = headerItem ?: 0
            newItemStats--
            if (items[position - 1].validatedAt != null) {
                newItemStats--
            }
            updateHeaderInfo(filterType, filterValue, newItemStats)
        }
        super.itemRemoved(position)
    }

    override fun getItemId(item: TicketList): Long {
        return item.id.hashCode().toLong()
    }

    private class TicketHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val leftBar = itemView.findViewById<View>(R.id.leftBar)
        private val tvTicketId = itemView.findViewById<TextView>(R.id.tvTicketId)
        private val tvValidated = itemView.findViewById<TextView>(R.id.tvValidated)
        private val tvSoldToText = itemView.findViewById<TextView>(R.id.tvSoldToText)
        private val tvTicketSoldTo = itemView.findViewById<TextView>(R.id.tvTicketSoldTo)
        private val tvSoldAtText = itemView.findViewById<TextView>(R.id.tvSoldAtText)
        private val tvTicketSoldAt = itemView.findViewById<TextView>(R.id.tvTicketSoldAt)


        fun updateTicketHolderInfo(ticket: TicketList) {
            setTicketId(ticket.id)
            setValidated(ticket.validatedAt)
            setSoldTo(ticket.soldTo)
            setSoldAt(ticket.soldAt)
        }

        private fun setTicketId(ticketId: String) {
            tvTicketId.text = "#$ticketId"
        }

        private fun setValidated(date: Date?) {
            if (date != null) {
                tvValidated.visibility = View.VISIBLE
                if((itemView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    tvValidated.text = Util.formatDate(date, true)
                }
                else {
                    tvValidated.text = Util.formatDate(date, false)
                }
                leftBar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.yesGreen))
            } else {
                tvValidated.visibility = View.GONE
                leftBar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.darkerGrey))
            }
        }

        private fun setSoldTo(soldTo: String?) {
            if(soldTo.isNullOrEmpty()) {
                tvSoldToText.text = "Sold"
                tvTicketSoldTo.visibility = View.GONE
            }
            else {
                tvSoldToText.text = "Sold to "
                tvTicketSoldTo.visibility = View.VISIBLE
                tvTicketSoldTo.text = soldTo
            }
        }

        private fun setSoldAt(soldAt: Date?) {
            if (soldAt == null) {
                tvSoldAtText.visibility = View.GONE
                tvTicketSoldAt.visibility = View.GONE
            } else {
                tvSoldAtText.visibility = View.VISIBLE
                tvTicketSoldAt.visibility = View.VISIBLE
                tvTicketSoldAt.text = DATE_FORMAT_MONTH_NAME.format(soldAt)
            }
        }
    }

    private class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTotalTickets = itemView.findViewById<TextView>(R.id.ticketNumbers)

        fun setVisibility(isVisible : Boolean) {
            tvTotalTickets.visibility = if(isVisible) View.VISIBLE else View.INVISIBLE
        }

        fun updateTicketHeaderInfo(filterType : String?, filterValue : String, ticketNumbers : Int?) {
            if(ticketNumbers == null)
                return

            if(ticketNumbers == 0) {
                tvTotalTickets?.visibility = View.GONE
            }
            else {
                tvTotalTickets?.visibility = View.VISIBLE
                val tickets = when(filterType) {
                    FILTER_VALIDATED -> {
                        if (ticketNumbers == 1) {
                            if (filterValue == "true") {
                                "validated ticket"
                            } else {
                                "not validated ticket"
                            }
                        } else {
                            if (filterValue == "true") {
                                "validated tickets"
                            } else {
                                "not validated tickets"
                            }
                        }
                    }
                    else -> {
                        if (ticketNumbers == 1) "ticket" else "tickets"

                    }
                }
                tvTotalTickets.text = "There is a total of $ticketNumbers $tickets"
            }
        }
    }
}