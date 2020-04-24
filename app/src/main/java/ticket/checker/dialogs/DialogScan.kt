package ticket.checker.dialogs

import android.content.DialogInterface
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
import ticket.checker.beans.Ticket
import ticket.checker.beans.TicketDefinition
import ticket.checker.extras.ErrorCodes.TICKET_EXISTS
import ticket.checker.extras.ErrorCodes.TICKET_IS_VALIDATED
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME
import ticket.checker.listeners.IScanDialogListener
import ticket.checker.services.ServiceManager
import java.text.ParseException
import java.util.*

/**
 * Created by Dani on 31.01.2018.
 */
class DialogScan : DialogFragment(), View.OnClickListener {
    var scanDialogListener: IScanDialogListener? = null

    private lateinit var ticketNumber: String
    private lateinit var organizationId: UUID
    private lateinit var organizationRole: OrganizationRole

    private lateinit var tvTicketNumber: TextView
    private lateinit var tvOwnerName : TextView
    private lateinit var tvOwnerBirthDate : TextView
    private lateinit var tvOwnerTelephone : TextView
    private lateinit var viewValidateTicket : LinearLayout
    private lateinit var viewSellTicket : LinearLayout
    private lateinit var viewDeleteTicket : LinearLayout
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var tvDescription: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var etSoldTo: EditText
    private lateinit var etSoldToBirthDate: EditText
    private lateinit var etSoldToTelephone: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnValidate: Button
    private lateinit var btnDelete: Button

    private var isTicketValidated : Boolean = false
    private var soldToBirthday : Date? = null

    private val ticketCallback = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            if(response.isSuccessful) {
                switchViews(response.body())
            }
            else {
                if(response.code() == 404) {
                    switchViews(null)
                } else {
                    onErrorResponse(call, response)
                }
            }
        }

        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            errorResult("Error connecting to the server!")
        }
    }
    private val verificationCallback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            errorResult("Error connecting to the server!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            ticketNumber = arguments?.getString(TICKET_NUMBER) ?: "NONE"
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
            organizationRole =  arguments?.getSerializable(ORGANIZATION_ROLE) as OrganizationRole
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_scan, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        scanDialogListener?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTicketNumber = view.findViewById(R.id.tvTicketNumber)
        tvTicketNumber.text = ticketNumber
        tvOwnerName = view.findViewById(R.id.tvOwnerName)
        tvOwnerBirthDate = view.findViewById(R.id.tvOwnerBirthDate)
        tvOwnerTelephone = view.findViewById(R.id.tvOwnerTelephone)
        viewValidateTicket = view.findViewById(R.id.viewValidateTicket)
        viewSellTicket = view.findViewById(R.id.viewSellTicket)
        viewDeleteTicket = view.findViewById(R.id.viewDeleteTicket)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvDescription = view.findViewById(R.id.tvDescription)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose.setOnClickListener(this)
        etSoldTo = view.findViewById(R.id.etSoldTo)
        etSoldToBirthDate = view.findViewById(R.id.etSoldToBirthDate)
        etSoldToTelephone = view.findViewById(R.id.etSoldToTelephone)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener(this)
        btnValidate = view.findViewById(R.id.btnValidate)
        btnValidate.setOnClickListener(this)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnDelete.setOnClickListener(this)

        requestTicketInfo()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
                scanDialogListener?.dismiss()
            }
            R.id.btnValidate -> {
                showLoading()

                val call = if(isTicketValidated) {
                     ServiceManager.getTicketService().invalidateTicketById(organizationId, ticketNumber)
                } else {
                     ServiceManager.getTicketService().validateTicketById(organizationId, ticketNumber)
                }
                call.enqueue(ticketCallback)
            }
            R.id.btnAdd -> {
                if(validatedAdd()) {
                    showLoading()
                    val soldTo = etSoldTo.text?.toString() ?: ""
                    val telephone = etSoldToTelephone.text?.toString()
                    val definition = TicketDefinition(ticketNumber, soldTo, soldToBirthday, telephone)
                    val call = ServiceManager.getTicketService().createTicketForOrganization(organizationId, definition)
                    call.enqueue(verificationCallback as Callback<Ticket>)
                }
            }
            R.id.btnDelete -> {
                showLoading()
                val call = ServiceManager.getTicketService().deleteTicketById(organizationId, ticketNumber)
                call.enqueue(verificationCallback as Callback<Void>)
            }
        }
    }

    private fun requestTicketInfo() {
        tvOwnerName.visibility = View.GONE
        tvOwnerBirthDate.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE
        tvDescription.visibility = View.GONE
        viewValidateTicket.visibility = View.GONE
        viewSellTicket.visibility = View.GONE
        viewDeleteTicket.visibility = View.GONE

        if(Util.isTicketFormatValid(ticketNumber)) {
            val call = ServiceManager.getTicketService().getTicketById(organizationId, ticketNumber)
            call.enqueue(ticketCallback)
        }
        else {
            errorResult("Ticket id format is invalid")
        }
    }

    private fun switchViews(ticket : Ticket?) {
        loadingSpinner.visibility = View.GONE
        tvDescription.visibility = View.VISIBLE

        if(ticket != null) {
            viewSellTicket.visibility = View.GONE
            tvOwnerName.visibility = if (ticket.soldTo.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvOwnerBirthDate.visibility = if (ticket.soldToBirthday == null) View.GONE else View.VISIBLE
            tvOwnerTelephone.visibility = if (ticket.soldToTelephone == null) View.GONE else View.VISIBLE
            tvOwnerName.text = if (ticket.soldTo.isNullOrEmpty()) "~not specified~" else ticket.soldTo
            tvOwnerBirthDate.text = if (ticket.soldToBirthday == null) "~not specified~" else DATE_FORMAT_MONTH_NAME.format(ticket.soldToBirthday)
            tvOwnerTelephone.text = if (ticket.soldToTelephone.isNullOrEmpty()) "~not specified~" else ticket.soldToTelephone

            when(organizationRole) {
                OrganizationRole.OWNER, OrganizationRole.ADMIN -> {
                    viewDeleteTicket.visibility = View.VISIBLE
                    viewValidateTicket.visibility = View.VISIBLE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is already validated")
                        btnValidate.text = "Invalidate Ticket"
                        btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
                        isTicketValidated = true
                    }
                    else {
                        errorResult("This ticket hasn't been validated")
                        btnValidate.text = "Validate Ticket"
                        btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white, 0)
                        isTicketValidated = false
                    }
                }
                OrganizationRole.PUBLISHER -> {
                    viewDeleteTicket.visibility = View.GONE
                    viewValidateTicket.visibility = View.GONE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is already validated")
                    }
                    else {
                        errorResult("This ticket hasn't been validated")
                    }
                }
                OrganizationRole.VALIDATOR -> {
                    viewDeleteTicket.visibility = View.GONE
                    viewValidateTicket.visibility = View.VISIBLE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is already validated")
                        btnValidate.text = "Invalidate Ticket"
                        btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
                        isTicketValidated = true
                    }
                    else {
                        errorResult("This ticket hasn't been validated")
                        btnValidate.text = "Validate Ticket"
                        btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white, 0)
                        isTicketValidated = false
                    }
                }
                OrganizationRole.USER -> {
                    viewDeleteTicket.visibility = View.GONE
                    viewValidateTicket.visibility = View.GONE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is already validated")
                    }
                    else {
                        errorResult("This ticket hasn't been validated")
                    }
                }
            }
        }
        else {
            tvOwnerName.visibility = View.GONE
            tvOwnerBirthDate.visibility = View.GONE
            tvOwnerTelephone.visibility = View.GONE
            viewValidateTicket.visibility = View.GONE
            viewDeleteTicket.visibility = View.GONE

            when(organizationRole) {
                OrganizationRole.OWNER, OrganizationRole.ADMIN -> {
                    viewSellTicket.visibility = View.VISIBLE
                }
                OrganizationRole.PUBLISHER -> {
                    viewSellTicket.visibility = View.VISIBLE
                }
                OrganizationRole.VALIDATOR -> {
                    viewSellTicket.visibility = View.GONE
                    errorResult("A ticket with this id was not found")
                }
                OrganizationRole.USER -> {
                    viewSellTicket.visibility = View.GONE
                    errorResult("A ticket with this id was not found")
                }
            }
        }
    }

    private fun validatedAdd() : Boolean {
        var isValid = true

        val soldTo = etSoldTo.text.toString()
        if(soldTo.isEmpty()) {
            etSoldTo.error = "You forgot the name"
            isValid = false
        }

        val birthDateString = etSoldToBirthDate.text.toString()
        if(birthDateString.isNotEmpty()) {
            try {
                this.soldToBirthday = DATE_FORMAT.parse(birthDateString)!!
                val now = Date()
                if(this.soldToBirthday!!.after(now)) {
                    etSoldToBirthDate.error = "The birth date cannot be in the future"
                    isValid = false
                }
            }
            catch(e : ParseException) {
                etSoldToBirthDate.error =  "Not valid date format. Required (dd.mm.yyyy)"
                isValid = false
            }
        }
        return isValid
    }

    private fun showLoading() {
        tvDescription.visibility = View.GONE
        viewValidateTicket.visibility = View.GONE
        viewSellTicket.visibility = View.GONE
        viewDeleteTicket.visibility = View.GONE
        btnClose.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if (!wasHandled) {
            when (response?.code()) {
                404 -> {
                    errorResult("It looks like the ticket doesn't exist anymore")
                }
                409 -> {
                    when (Util.convertError(response.errorBody())?.id) {
                        TICKET_EXISTS -> {
                            errorResult("This ticket has already been added")
                        }
                        TICKET_IS_VALIDATED -> {
                            errorResult("This ticket is already validated")
                        }
                        TICKET_IS_VALIDATED -> {
                            errorResult("This ticket is already not validated")
                        }
                    }
                }
                else -> {
                    errorResult("Unexpected error")
                }
            }
        } else {
            dismiss()
        }
    }

    private fun errorResult(errorMsg: String) {
        btnClose.visibility = View.VISIBLE
        loadingSpinner.visibility = View.GONE
        tvDescription.text = errorMsg
        tvDescription.setTextColor(ContextCompat.getColor(context!!, R.color.materialYellow))
        tvDescription.visibility = View.VISIBLE
    }

    companion object {
        private const val TICKET_NUMBER = "ticketNumber"
        private const val ORGANIZATION_ID = "organizationId"
        private const val ORGANIZATION_ROLE = "organizationRole"

        fun newInstance(ticketNumber: String, organizationId: UUID, role: OrganizationRole): DialogScan {
            val fragment = DialogScan()
            val args = Bundle()
            args.putString(TICKET_NUMBER, ticketNumber)
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putSerializable(ORGANIZATION_ROLE, role)
            fragment.arguments = args
            return fragment
        }
    }
}