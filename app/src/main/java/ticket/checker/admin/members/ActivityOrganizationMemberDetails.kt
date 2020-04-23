package ticket.checker.admin.members

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityControlPanel
import ticket.checker.ActivityControlPanel.Companion.EDITED_OBJECT
import ticket.checker.AppTicketChecker
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.OrganizationMember
import ticket.checker.dialogs.DialogInfo
import ticket.checker.extras.DialogType
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util.DATE_TIME_FORMAT
import ticket.checker.extras.Util.POSITION
import ticket.checker.extras.Util.treatBasicError
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager
import java.util.*

class ActivityOrganizationMemberDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {

    private var isFirstLoad: Boolean = true
    private var itemsWasEdited: Boolean = false
    private var itemWasRemoved: Boolean = false

    private lateinit var organizationId: UUID
    private lateinit var userId: UUID

    private var currentMember : OrganizationMember? = null

    private val memberPosition : Int by lazy {
        intent.getIntExtra(POSITION, -1)
    }

    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvTitle by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
    }
    private val tvRole by lazy {
        findViewById<TextView>(R.id.role)
    }
    private val tvJoinedAt by lazy {
        findViewById<TextView>(R.id.joinedAt)
    }
    private val tvTicketsCreated by lazy {
        findViewById<TextView>(R.id.ticketsCreated)
    }
    private val tvTicketsValidated by lazy {
        findViewById<TextView>(R.id.ticketsValidated)
    }
    private val btnEdit by lazy {
        findViewById<ImageButton>(R.id.btnEdit)
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

    private val editListener : EditListener<OrganizationMember> = object : EditListener<OrganizationMember> {
        override fun onEdit(editedObject: OrganizationMember) {
            itemsWasEdited = true
            updateMemberInfo(editedObject)
        }
    }

    private val memberCallback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if (response.isSuccessful) {
                when(method) {
                    "GET" -> {
                        updateMemberInfo(response.body() as OrganizationMember)
                        if(isFirstLoad) {
                            isFirstLoad = false
                            btnRemove.visibility = View.VISIBLE
                        }
                    }
                    "DELETE" -> {
                        loadingSpinner.visibility = View.GONE
                        val dialogDeleteSuccessful = DialogInfo.newInstance("Deletion successful", "User '${currentMember!!.name}' was successfully deleted", DialogType.SUCCESS)
                        dialogDeleteSuccessful.dialogExitListener = this@ActivityOrganizationMemberDetails
                        dialogDeleteSuccessful.show(supportFragmentManager, "DIALOG_DELETE_SUCCESSFUL")
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
        setContentView(R.layout.activity_member_details)

        organizationId = intent.getSerializableExtra(ORGANIZATION_ID) as UUID
        userId = intent.getSerializableExtra(USER_ID) as UUID

        setSupportActionBar(toolbar)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)
        btnEdit.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getOrganizationService().getOrganizationMemberById(organizationId, userId)
        call.enqueue(memberCallback as Callback<OrganizationMember>)
    }

    override fun onBackPressed() {
        if (itemWasRemoved) {
            val data = Intent()
            data.putExtra(POSITION, memberPosition)
            setResult(ActivityControlPanel.ITEM_REMOVED, data)
        }
        else {
            if(itemsWasEdited) {
                val data = Intent()
                data.putExtra(POSITION, memberPosition)
                data.putExtra(EDITED_OBJECT, currentMember)
                setResult(ActivityControlPanel.ITEM_EDITED, data)
            }
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val member = savedInstanceState.getSerializable(CURRENT_MEMBER) as OrganizationMember
        updateMemberInfo(member)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(CURRENT_MEMBER, currentMember)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnEdit -> {
                if(currentMember != null) {
                    when {
                        currentMember!!.userId == AppTicketChecker.loggedInUser!!.id -> {
                            val dialog = DialogInfo.newInstance("Edit failed", "You can not edit yourself", DialogType.ERROR)
                            dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                        }
                        currentMember!!.role == OrganizationRole.ADMIN && AppTicketChecker.selectedOrganizationMembership!!.role != OrganizationRole.OWNER -> {
                            val dialog = DialogInfo.newInstance("Edit failed", "You are not allowed to edit another admin", DialogType.ERROR)
                            dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                        }
                        else -> {
                            val dialog = DialogEditOrganizationMember.newInstance(organizationId, userId, currentMember!!.name)
                            dialog.editListener = editListener
                            dialog.show(supportFragmentManager, "DIALOG_EDIT_MEMBER")
                        }
                    }
                }
            }
            R.id.btnRemove -> {
                if(currentMember != null) {
                    val dialog: DialogInfo?
                    when {
                        currentMember!!.userId == AppTicketChecker.loggedInUser!!.id -> {
                            dialog = DialogInfo.newInstance("Delete failed", "You can not remove yourself", DialogType.ERROR)
                            dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                        }
                        currentMember!!.role == OrganizationRole.ADMIN && AppTicketChecker.selectedOrganizationMembership!!.role != OrganizationRole.OWNER -> {
                            dialog = DialogInfo.newInstance("Delete failed", "You are not allowed to remove another admin", DialogType.ERROR)
                            dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                        }
                        else -> {
                            dialog = DialogInfo.newInstance("Confirm deletion", "Are you sure you want to remove member ${currentMember!!.name} from organization?", DialogType.YES_NO)
                            dialog.dialogResponseListener = this
                            dialog.show(supportFragmentManager, "DIALOG_CONFIRM_DELETE")
                        }
                    }
                }
            }
        }
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            switchToLoadingView(true)
            val call = ServiceManager.getOrganizationService().deleteOrganizationMemberById(organizationId, userId)
            call.enqueue(memberCallback as Callback<Void>)
        }
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
        onBackPressed()
    }

    private fun switchToLoadingView(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRemove.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateMemberInfo(member: OrganizationMember) {
        findViewById<ProgressBar>(R.id.lsJoinedAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsTicketsCreated).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsTicketsValidated).visibility = View.INVISIBLE

        currentMember = member

        tvTitle.text = member.name
        tvRole.text = member.role.role
        tvRole.setTextColor(ContextCompat.getColor(applicationContext, member.role.colorResource))

        tvJoinedAt.text = DATE_TIME_FORMAT.format(member.joinedAt)
        tvTicketsCreated.text = "${member.soldTicketsNo}"
        tvTicketsValidated.text = "${member.validatedTicketsNo}"
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = treatBasicError(call, response, supportFragmentManager)
        if (!wasHandled) {
            if (response?.code() == 400) {
                val dialogNoTicket = DialogInfo.newInstance("User not found", "The user you are trying to access no longer exists", DialogType.ERROR)
                dialogNoTicket.dialogExitListener = this@ActivityOrganizationMemberDetails
                dialogNoTicket.show(supportFragmentManager, "DIALOG_ERROR")
            }
        }
    }

    companion object {
        const val ORGANIZATION_ID = "organizationId"
        const val USER_ID = "userId"
        const val CURRENT_MEMBER = "currentMember"
    }
}
