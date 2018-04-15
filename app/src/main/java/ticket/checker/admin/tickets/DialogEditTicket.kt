package ticket.checker.admin.tickets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
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
import ticket.checker.extras.BirthDateFormatException
import ticket.checker.extras.BirthDateIncorrectException
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogEditTicket : DialogFragment(), View.OnClickListener {
    var editListener: EditListener<Ticket>? = null

    private var ticketNumber : String? = null
    private var soldToBirthDate : Date? = null

    private var btnClose: ImageButton? = null
    private var tvTitle: TextView? = null
    private var etSoldTo: EditText? = null
    private var etSoldToBirthDate: EditText? = null
    private var bottomContainer : LinearLayout? = null
    private var editButton: Button? = null
    private var loadingSpinner: ProgressBar? = null
    private var tvResult: TextView? = null

    private val getCallback : Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            if (response.isSuccessful) {
                loadingSpinner?.visibility = View.GONE
                editButton?.visibility = View.VISIBLE
                updateWithTicketInfo(response.body() as Ticket)
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            loadingSpinner?.visibility = View.GONE
            onErrorResponse(call, null)
        }
    }

    private val editCallback: Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            loadingSpinner?.visibility = View.GONE
            editButton?.visibility = View.VISIBLE
            if (response.isSuccessful) {
                editListener?.onEdit(response.body() as Ticket)
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            loadingSpinner?.visibility = View.GONE
            editButton?.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            ticketNumber = arguments?.getString(TICKET_NUMBER) ?: "NONE"
        }
    }

    override fun onStart() {
        super.onStart()
        val getCall = ServiceManager.getTicketService().getTicketById(ticketNumber as String)
        getCall.enqueue(getCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_edit_ticket, container, false)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        tvTitle = view.findViewById(R.id.dialogTitle)
        tvTitle?.text = "Edit #$ticketNumber"
        etSoldTo = view.findViewById(R.id.etSoldTo)
        etSoldToBirthDate = view.findViewById(R.id.etSoldToBirthDate)
        bottomContainer = view.findViewById(R.id.bottomContainer)
        editButton = view.findViewById(R.id.btnEdit)
        editButton?.setOnClickListener(this)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvResult = view.findViewById(R.id.tvResult)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnEdit -> {
                if (validate()) {
                    editTicket(ticketNumber as String, etSoldTo?.text.toString(), soldToBirthDate)
                }
            }
        }
    }

    private fun updateWithTicketInfo(ticket : Ticket) {
        etSoldTo?.isEnabled = true
        etSoldTo?.setText(ticket.soldTo)
        etSoldTo?.error = null
        etSoldTo?.post({ etSoldTo?.setSelection(ticket.soldTo?.length ?: 0) })
        etSoldToBirthDate?.isEnabled = true
        etSoldToBirthDate?.setText(if(ticket.soldToBirthdate != null)  Util.DATE_FORMAT_FORM.format(ticket.soldToBirthdate) else "")
        etSoldToBirthDate?.error = null
    }

    private fun validate(): Boolean {
        var isValid = true
        tvResult?.visibility = View.INVISIBLE

        val soldTo = etSoldTo?.text.toString()
        if(soldTo.isEmpty()) {
            etSoldTo?.error = "You forgot the name"
            isValid = false
        }
        val birthDateString = etSoldToBirthDate?.text.toString()
        if(!birthDateString.isEmpty()) {
            try {
                soldToBirthDate = Util.getBirthdateFromText(birthDateString)
            }
            catch(e : BirthDateIncorrectException) {
                etSoldToBirthDate?.error = "The birth date cannot be in the future"
                isValid = false
            }
            catch(e : BirthDateFormatException) {
                etSoldToBirthDate?.error =  "Not valid date format. (dd.mm.yyyy)"
                isValid = false
            }
        }
        return isValid
    }

    private fun editTicket(ticketNumber : String, soldTo: String, soldToBirthDate : Date?) {
        tvResult?.visibility = View.INVISIBLE
        editButton?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE

        val ticket = Ticket(ticketNumber, soldTo, soldToBirthDate)
        val call = ServiceManager.getTicketService().editTicket(ticketNumber, ticket)
        call.enqueue(editCallback)
    }

    private fun onErrorResponse(call: Call<Ticket>, response: Response<Ticket>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager)
        if (!wasHandled) {
            if (response?.code() == 404) {
                bottomContainer?.visibility = View.GONE
                tvResult?.visibility = View.VISIBLE
                tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                tvResult?.text = "A ticket with this id was not found!"
            }
        }
    }

    companion object {
        private const val TICKET_NUMBER = "ticketNumber"

        fun newInstance(ticketNumber: String): DialogEditTicket {
            val fragment = DialogEditTicket()
            val args = Bundle()
            args.putString(TICKET_NUMBER, ticketNumber)
            fragment.arguments = args
            return fragment
        }
    }

}