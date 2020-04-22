package ticket.checker.admin.tickets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.Ticket
import ticket.checker.beans.TicketList
import ticket.checker.beans.TicketUpdateDefinition
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.text.ParseException
import java.util.*

class DialogEditTicket : DialogFragment(), View.OnClickListener {
    lateinit var editListener: EditListener<Ticket>

    private lateinit var organizationId: UUID
    private lateinit var ticketId : String
    private lateinit var soldToBirthday : Date

    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnEdit by lazy {
        dialogView.findViewById<Button>(R.id.btnEdit)
    }
    private val tvTitle by lazy {
        dialogView.findViewById<TextView>(R.id.tvTitle)
    }
    private val etSoldTo by lazy {
        dialogView.findViewById<EditText>(R.id.etSoldTo)
    }
    private val etSoldToBirthDate by lazy {
        dialogView.findViewById<EditText>(R.id.etSoldToBirthDate)
    }
    private val etSoldToTelephone by lazy {
        dialogView.findViewById<EditText>(R.id.etSoldToTelephone)
    }
    private val bottomContainer by lazy {
        dialogView.findViewById<LinearLayout>(R.id.bottomContainer)
    }

    private val loadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        dialogView.findViewById<TextView>(R.id.tvResult)
    }

    private val getCallback : Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            if (response.isSuccessful) {
                loadingSpinner.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                updateWithTicketInfo(response.body() as Ticket)
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            onErrorResponse(call, null)
        }
    }

    private val editCallback: Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            loadingSpinner.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            if (response.isSuccessful) {
                editListener.onEdit(response.body() as Ticket)
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
            ticketId = arguments?.getString(TICKET_ID)!!
        }
    }

    override fun onStart() {
        super.onStart()
        val getCall = ServiceManager.getTicketService().getTicketById(organizationId, ticketId)
        getCall.enqueue(getCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView = inflater.inflate(R.layout.dialog_edit_ticket, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener(this)
        btnEdit.setOnClickListener(this)

        tvTitle.text = "Edit #$ticketId"
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnEdit -> {
                if (validate()) {
                    editTicket(ticketId, etSoldTo.text.toString(), soldToBirthday, etSoldToTelephone.text.toString())
                }
            }
        }
    }

    private fun updateWithTicketInfo(ticket : Ticket) {
        etSoldTo.isEnabled = true
        etSoldTo.setText(ticket.soldTo)
        etSoldTo.error = null
        etSoldTo.post { etSoldTo.setSelection(ticket.soldTo?.length ?: 0) }
        etSoldToBirthDate.isEnabled = true
        etSoldToBirthDate.setText(if(ticket.soldToBirthday != null)  Util.DATE_FORMAT.format(ticket.soldToBirthday) else "")
        etSoldToBirthDate.error = null
        etSoldToTelephone.isEnabled = true
        etSoldToTelephone.setText(ticket.soldToTelephone)
        etSoldToTelephone.error = null
    }

    private fun validate(): Boolean {
        var isValid = true
        tvResult.visibility = View.INVISIBLE

        val soldTo = etSoldTo.text.toString()
        if(soldTo.isEmpty()) {
            etSoldTo.error = "You forgot the name"
            isValid = false
        }

        val birthDateString = etSoldToBirthDate.text.toString()
        if(birthDateString.isNotEmpty()) {
            try {
                this.soldToBirthday = Util.DATE_FORMAT.parse(birthDateString)!!
            }
            catch(e : ParseException) {
                etSoldToBirthDate.error =  "Not valid date format. Required (dd.mm.yyyy)"
                isValid = false
            }
        }
        val now = Date()
        if(this.soldToBirthday.after(now)) {
            etSoldToBirthDate.error = "The birth date cannot be in the future"
            isValid = false
        }
        return isValid
    }

    private fun editTicket(ticketNumber : String, soldTo: String, soldToBirthday: Date?, etSoldToTelephone: String?) {
        tvResult.visibility = View.INVISIBLE
        btnEdit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val definition = TicketUpdateDefinition(soldTo, soldToBirthday, etSoldToTelephone)
        val call = ServiceManager.getTicketService().updateTicketById(organizationId, ticketNumber, definition)
        call.enqueue(editCallback)
    }

    private fun onErrorResponse(call: Call<Ticket>, response: Response<Ticket>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if (!wasHandled) {
            if (response?.code() == 404) {
                bottomContainer.visibility = View.GONE
                tvResult.visibility = View.VISIBLE
                tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                tvResult.text = "A ticket with this id was not found"
            }
        }
    }

    companion object {
        private const val ORGANIZATION_ID = "organizationId"
        private const val TICKET_ID = "ticketId"

        fun newInstance(organizationId: UUID, ticketId: String): DialogEditTicket {
            val fragment = DialogEditTicket()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putString(TICKET_ID, ticketId)
            fragment.arguments = args
            return fragment
        }
    }

}