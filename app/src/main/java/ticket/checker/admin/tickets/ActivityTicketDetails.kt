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
import ticket.checker.AppTicketChecker
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.Ticket
import ticket.checker.dialogs.DialogInfo
import ticket.checker.extras.DialogType
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME
import ticket.checker.extras.Util.DATE_TIME_FORMAT
import ticket.checker.extras.Util.POSITION
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager

class ActivityTicketDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {

    private var itemWasRemoved: Boolean = false
    private var itemWasEdited: Boolean = false
    private var isFirstLoad: Boolean = true

    private lateinit var ticketId: String
    private var currentTicket: Ticket? = null

    private val ticketPosition: Int by lazy {
        intent.getIntExtra(POSITION, -1)
    }

    private val tvTitle by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvStatus by lazy {
        findViewById<TextView>(R.id.status)
    }
    private val tvSoldTo by lazy {
        findViewById<TextView>(R.id.soldTo)
    }
    private val tvSoldToBirthDate  by lazy {
        findViewById<TextView>(R.id.soldToBirthDate)
    }
    private val tvSoldToTelephone  by lazy {
        findViewById<TextView>(R.id.soldToTelephone)
    }
    private val tvSoldAt by lazy {
        findViewById<TextView>(R.id.soldAt)
    }
    private val tvSoldBy by lazy {
        findViewById<TextView>(R.id.soldBy)
    }
    private val tvValidatedAt by lazy {
        findViewById<TextView>(R.id.validatedAt)
    }
    private val tvValidatedBy by lazy {
        findViewById<TextView>(R.id.validatedBy)
    }
    private val btnEdit by lazy {
        findViewById<ImageButton>(R.id.btnEdit)
    }
    private val btnValidate by lazy {
        findViewById<Button>(R.id.btnValidate)
    }
    private val btnRemove by lazy {
        findViewById<Button>(R.id.btnRemove)
    }
    private val btnBack by lazy {
        findViewById<ImageButton>(R.id.btnBack)
    }
    private val loadingSpinner by lazy {
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
                            if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.OWNER || AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.ADMIN) {
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
                        val dialogRemoveSuccessful = DialogInfo.newInstance("Remove successful", "Ticket #$ticketId was successfully removed", DialogType.SUCCESS)
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

        ticketId = intent.getStringExtra(TICKET_ID)!!

        setSupportActionBar(toolbar)
        btnEdit.setOnClickListener(this)
        btnValidate.setOnClickListener(this)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)

        if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole != OrganizationRole.OWNER && AppTicketChecker.selectedOrganizationMembership!!.pretendedRole != OrganizationRole.ADMIN) {
            btnEdit.visibility = View.GONE
            btnRemove.visibility = View.GONE
        }
        if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.PUBLISHER) {
           findViewById<LinearLayout>(R.id.buttonsContainer).visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getTicketService().getTicketById(AppTicketChecker.selectedOrganizationMembership!!.organizationId, ticketId)
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val ticket = savedInstanceState.getSerializable(CURRENT_TICKET) as Ticket
        updateTicketInfo(ticket)
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
                val dialogEditTicket = DialogEditTicket.newInstance(AppTicketChecker.selectedOrganizationMembership!!.organizationId, ticketId)
                dialogEditTicket.editListener = editListener
                dialogEditTicket.show(supportFragmentManager, "DIALOG_EDIT_TICKET")
            }
            R.id.btnValidate -> {
                switchToLoadingView(true)
                val isValidated = currentTicket?.validatedAt != null
                val call = if(isValidated) {
                    ServiceManager.getTicketService().invalidateTicketById(AppTicketChecker.selectedOrganizationMembership!!.organizationId, ticketId)
                } else {
                    ServiceManager.getTicketService().validateTicketById(AppTicketChecker.selectedOrganizationMembership!!.organizationId, ticketId)
                }
                call.enqueue(ticketCallback as Callback<Ticket>)
            }
            R.id.btnRemove -> {
                val dialogConfirm = DialogInfo.newInstance("Confirm remove", "Are you sure you want to remove ticket #$ticketId ?", DialogType.YES_NO)
                dialogConfirm.dialogResponseListener = this
                dialogConfirm.show(supportFragmentManager, "DIALOG_CONFIRM_REMOVAL")
            }
        }
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            switchToLoadingView(true)
            val call = ServiceManager.getTicketService().deleteTicketById(AppTicketChecker.selectedOrganizationMembership!!.organizationId, ticketId)
            call.enqueue(ticketCallback as Callback<Void>)
        }
    }

    private fun switchToLoadingView(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnValidate.visibility = if (isLoading) View.GONE else View.VISIBLE
        if(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.OWNER || AppTicketChecker.selectedOrganizationMembership!!.pretendedRole == OrganizationRole.ADMIN) {
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
        tvTitle.text = "#${ticket.id}"

        tvSoldTo.text = if (ticket.soldTo.isNullOrEmpty()) "-" else ticket.soldTo
        tvSoldToBirthDate.text = if(ticket.soldToBirthday != null) DATE_FORMAT_MONTH_NAME.format(ticket.soldToBirthday) else "-"
        tvSoldToTelephone.text = ticket.soldToTelephone ?: "-"
        tvSoldAt.text = DATE_TIME_FORMAT.format(ticket.soldAt)
        if (ticket.soldBy != null) {
            tvSoldBy.text = ticket.soldBy.name
        } else {
            tvSoldBy.text = "?"
        }

        if (ticket.validatedAt != null) {
            tvStatus.text = "VALIDATED"
            btnValidate.text = "Invalidate"
            btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
            tvStatus.setTextColor(ContextCompat.getColor(baseContext, R.color.yesGreen))

            tvValidatedAt.text = DATE_TIME_FORMAT.format(ticket.validatedAt)
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
            when(response?.code()) {
                404 -> {
                    val dialogNoTicket = DialogInfo.newInstance("Ticket not found", "The ticket you are trying to access no longer exists", DialogType.ERROR)
                    dialogNoTicket.dialogExitListener = this@ActivityTicketDetails
                    dialogNoTicket.show(supportFragmentManager, "DIALOG_ERROR")
                }
            }
        }
    }

    companion object {
        const val TICKET_ID = "ticketId"
        const val CURRENT_TICKET = "currentTicket"
    }
}
