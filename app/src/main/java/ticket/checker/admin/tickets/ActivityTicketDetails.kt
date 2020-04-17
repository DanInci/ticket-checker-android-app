package ticket.checker.admin.tickets

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityControlPanel.Companion.EDITED_OBJECT
import ticket.checker.ActivityControlPanel.Companion.ITEM_EDITED
import ticket.checker.ActivityControlPanel.Companion.ITEM_REMOVED
import ticket.checker.AppTicketChecker.Companion.pretendedOrganizationRole
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.Ticket
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.extras.Util.CURRENT_TICKET
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.DATE_FORMAT_WITH_HOUR
import ticket.checker.extras.Util.POSITION
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager

class ActivityTicketDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {
    private var itemWasRemoved: Boolean = false
    private var itemWasEdited: Boolean = false
    private var isFirstLoad = true

    private var currentTicket: Ticket? = null
    private val ticketPosition: Int by lazy {
        intent.getIntExtra(POSITION, -1)
    }

    private val tvTitle : TextView by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvStatus: TextView by lazy {
        findViewById<TextView>(R.id.status)
    }
    private val tvSoldTo: TextView by lazy {
        findViewById<TextView>(R.id.soldTo)
    }
    private val tvSoldToBirthDate : TextView  by lazy {
        findViewById<TextView>(R.id.soldToBirthDate)
    }
    private val tvSoldToTelephone : TextView  by lazy {
        findViewById<TextView>(R.id.soldToTelephone)
    }
    private val tvSoldAt: TextView by lazy {
        findViewById<TextView>(R.id.soldAt)
    }
    private val tvSoldBy: TextView by lazy {
        findViewById<TextView>(R.id.soldBy)
    }
    private val tvValidatedAt: TextView by lazy {
        findViewById<TextView>(R.id.validatedAt)
    }
    private val tvValidatedBy: TextView by lazy {
        findViewById<TextView>(R.id.validatedBy)
    }
    private val btnEdit : ImageButton by lazy {
        findViewById<ImageButton>(R.id.btnEdit)
    }
    private val btnValidate: Button by lazy {
        findViewById<Button>(R.id.btnValidate)
    }
    private val btnRemove: Button by lazy {
        findViewById<Button>(R.id.btnRemove)
    }
    private val btnBack: ImageButton by lazy {
        findViewById<ImageButton>(R.id.btnBack)
    }

    private val loadingSpinner: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }

    private val editListener : EditListener<Ticket> = object : EditListener<Ticket> {
        override fun onEdit(editedObject: Ticket) {
            itemWasEdited = true
            updateTicketInfo(editedObject)
        }
    }

    private val ticketCallback = object<T> :  Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if(response.isSuccessful) {
                when(method) {
                    "GET" -> {
                        updateTicketInfo(response.body() as Ticket)
                        if(isFirstLoad) {
                            isFirstLoad = false
                            if(pretendedOrganizationRole==OrganizationRole.ADMIN) {
                                btnRemove.visibility = View.VISIBLE
                            }
                            btnValidate.visibility =  View.VISIBLE
                        }
                    }
                    "POST" -> {
                        itemWasEdited = true
                        updateTicketInfo(response.body() as Ticket)
                        switchToLoadingView(false)
                    }
                    "DELETE" -> {
                        loadingSpinner.visibility = View.GONE
                        val dialogRemoveSuccessful = DialogInfo.newInstance("Remove successful", "Ticket #${currentTicket?.ticketId} was successfully removed", DialogType.SUCCESS)
                        dialogRemoveSuccessful.dialogExitListener = this@ActivityTicketDetails
                        dialogRemoveSuccessful.show(supportFragmentManager, "DIALOG_REMOVE_SUCCESSFUL")
                    }
                }
            }
            else {
                switchToLoadingView(false)
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable?) {
            switchToLoadingView(false)
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_details)

        val ticket = savedInstanceState?.getSerializable(CURRENT_TICKET) ?: intent.getSerializableExtra(CURRENT_TICKET)
        updateTicketInfo(ticket as Ticket)

        setSupportActionBar(toolbar)
        btnEdit.setOnClickListener(this)
        btnValidate.setOnClickListener(this)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)

        if(pretendedOrganizationRole != OrganizationRole.ADMIN) {
            btnEdit.visibility = View.GONE
            btnRemove.visibility = View.GONE
        }
        if(pretendedOrganizationRole == OrganizationRole.PUBLISHER) {
           findViewById<LinearLayout>(R.id.buttonsContainer).visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getTicketService().getTicketById(currentTicket?.ticketId!!)
        call.enqueue(ticketCallback as Callback<Ticket>)
    }

    override fun onBackPressed() {
        if (itemWasRemoved) {
            val data = Intent()
            data.putExtra(POSITION, ticketPosition)
            setResult(ITEM_REMOVED, data)
        } else {
            if(itemWasEdited) {
                val data = Intent()
                data.putExtra(POSITION, ticketPosition)
                data.putExtra(EDITED_OBJECT, currentTicket)
                setResult(ITEM_EDITED, data)
            }
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(CURRENT_TICKET, currentTicket)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnEdit -> {
                val dialogEditTicket = DialogEditTicket.newInstance(currentTicket?.ticketId!!)
                dialogEditTicket.editListener = editListener
                dialogEditTicket.show(supportFragmentManager, "DIALOG_EDIT_TICKET")
            }
            R.id.btnValidate -> {
                switchToLoadingView(true)
                val isValidated = currentTicket?.validatedAt != null
                val call = ServiceManager.getTicketService().validateTicket(!isValidated, currentTicket?.ticketId!!)
                call.enqueue(ticketCallback as Callback<Ticket>)
            }
            R.id.btnRemove -> {
                val dialogConfirm = DialogInfo.newInstance("Confirm remove", "Are you sure you want to remove ticket #${currentTicket?.ticketId} ?", DialogType.YES_NO)
                dialogConfirm.dialogResponseListener = this
                dialogConfirm.show(supportFragmentManager, "DIALOG_CONFIRM_REMOVAL")
            }
        }
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            switchToLoadingView(true)
            val call = ServiceManager.getTicketService().deleteTicketById(currentTicket?.ticketId!!)
            call.enqueue(ticketCallback as Callback<Void>)
        }
    }

    private fun switchToLoadingView(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnValidate.visibility = if (isLoading) View.GONE else View.VISIBLE
        if(pretendedOrganizationRole == OrganizationRole.ADMIN) {
            btnRemove.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
        onBackPressed()
    }

    private fun updateTicketInfo(ticket: Ticket) {
        findViewById<ProgressBar>(R.id.lsSoldTo).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldToBirthDate).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldToTelephone).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldBy).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsValidatedAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsValidatedBy).visibility = View.INVISIBLE

        currentTicket = ticket
        tvTitle.text = "#${ticket.ticketId}"

        tvSoldTo.text = if (ticket.soldTo.isNullOrEmpty()) "-" else ticket.soldTo
        tvSoldToBirthDate.text = if(ticket.soldToBirthdate != null) DATE_FORMAT.format(ticket.soldToBirthdate) else "-"
        tvSoldToTelephone.text = if(ticket.telephone != null) ticket.telephone else "-"
        tvSoldAt.text = if (ticket.soldAt != null) DATE_FORMAT_WITH_HOUR.format(ticket.soldAt) else "-"
        if (ticket.soldAt != null) {
            if (ticket.soldBy != null) {
                tvSoldBy.text = ticket.soldBy.name
            } else {
                tvSoldBy.text = "?"
            }
        } else {
            tvSoldBy.text = "-"
        }

        if (ticket.validatedAt != null) {
            tvStatus.text = "VALIDATED"
            btnValidate.text = "Invalidate"
            btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
            tvStatus.setTextColor(ContextCompat.getColor(baseContext, R.color.yesGreen))

            tvValidatedAt.text = DATE_FORMAT_WITH_HOUR.format(ticket.validatedAt)
            if (ticket.validatedBy != null) {
                tvValidatedBy.text = ticket.validatedBy.name
            } else {
                tvValidatedAt.text = "?"
            }
        } else {
            btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white, 0)
            tvStatus.setTextColor(ContextCompat.getColor(baseContext, R.color.darkerGrey))
            tvStatus.text = "NOT VALIDATED"
            btnValidate.text = "Validate"

            tvValidatedAt.text = "-"
            tvValidatedBy.text = "-"
        }
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = Util.treatBasicError(call, response, supportFragmentManager)
        if (!wasHandled) {
            if (response?.code() == 404) {
                val dialogNoTicket = DialogInfo.newInstance("Ticket not found", "The ticket you are trying to access no longer exists", DialogType.ERROR)
                dialogNoTicket.dialogExitListener = this@ActivityTicketDetails
                dialogNoTicket.show(supportFragmentManager, "DIALOG_ERROR")
            }
        }
    }
}
