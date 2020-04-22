package ticket.checker.admin.tickets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.Image
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
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.beans.Ticket
import ticket.checker.beans.TicketDefinition
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.text.ParseException
import java.util.*

class DialogAddTicket : DialogFragment(), View.OnClickListener {
    lateinit var listChangeListener: ListChangeListener<Ticket>

    private lateinit var organizationId: UUID
    private lateinit var soldToBirthDate: Date

    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnSubmit by lazy {
        dialogView.findViewById<Button>(R.id.btnSubmit)
    }
    private val etTicketNumber by lazy {
        dialogView.findViewById<EditText>(R.id.etTicketNumber)
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
    private val loadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        dialogView.findViewById<TextView>(R.id.tvResult)
    }

    private val submitCallback: Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            loadingSpinner.visibility = View.GONE
            btnSubmit.visibility = View.VISIBLE
            if (response.isSuccessful) {
                listChangeListener.onAdd(response.body() as Ticket)
                tvResult.visibility = View.VISIBLE
                tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.yesGreen))
                tvResult.text = "You have created the ticket successfully"
                etTicketNumber.setText("")
                etTicketNumber.requestFocus()
                etTicketNumber.error = null
                etSoldTo.setText("")
                etSoldTo.error = null
                etSoldToBirthDate.setText("")
                etSoldToBirthDate.error = null
                etSoldToTelephone.setText("")
                etSoldToTelephone.error = null
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            btnSubmit.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        dialogView = inflater.inflate(R.layout.dialog_add_ticket, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnSubmit -> {
                if (validate()) {
                    submitTicket(organizationId, etTicketNumber?.text.toString(), etSoldTo?.text.toString(), etSoldToTelephone?.text.toString())
                }
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        tvResult.visibility = View.INVISIBLE
        val ticketNumber = etTicketNumber.text.toString()
        if (ticketNumber.isEmpty()) {
            etTicketNumber.error = "This field is required"
            isValid = false
        }

        val soldTo = etSoldTo.text.toString()
        if(soldTo.isEmpty()) {
            etSoldTo.error = "This field is required"
            isValid = false
        }

        val birthDateString = etSoldToBirthDate.text.toString()
        if(birthDateString.isNotEmpty()) {
            try {
                this.soldToBirthDate = Util.DATE_FORMAT.parse(birthDateString)!!
            }
            catch(e : ParseException) {
                etSoldToBirthDate.error =  "Not valid date format. Required (dd.mm.yyyy)"
                isValid = false
            }
        }
        val now = Date()
        if(this.soldToBirthDate.after(now)) {
            etSoldToBirthDate.error = "The birth date cannot be in the future"
            isValid = false
        }
        return isValid
    }

    private fun submitTicket(organizationId: UUID, ticketNumber: String, soldTo: String, etSoldToTelephone: String?) {
        tvResult.visibility = View.INVISIBLE
        btnSubmit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val definition = TicketDefinition(ticketNumber, soldTo, soldToBirthDate, etSoldToTelephone)
        val call = ServiceManager.getTicketService().createTicketForOrganization(organizationId, definition)
        call.enqueue(submitCallback)
    }

    private fun onErrorResponse(call: Call<Ticket>, response: Response<Ticket>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if (!wasHandled) {
            if (response?.code() == 400) {
                etTicketNumber?.error = "This ticket id already exists"
            }
        }
    }

    companion object {
        private const val ORGANIZATION_ID = "organizationId"

        fun newInstance(organizationId: UUID): DialogAddTicket {
            val fragment = DialogAddTicket()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            fragment.arguments = args
            return fragment
        }
    }

}