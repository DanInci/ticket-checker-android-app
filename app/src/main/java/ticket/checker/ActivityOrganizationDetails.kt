package ticket.checker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityOrganizations.Companion.EDITED_ORGANIZATION
import ticket.checker.ActivityOrganizations.Companion.ITEM_EDITED
import ticket.checker.ActivityOrganizations.Companion.ITEM_REMOVED
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.Organization
import ticket.checker.dialogs.DialogEditOrganization
import ticket.checker.dialogs.DialogInfo
import ticket.checker.extras.DialogType
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager

class ActivityOrganizationDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener, EditListener<Organization> {

    private lateinit var currentOrganization : Organization
    private var itemsWasEdited = false
    private var itemWasRemoved = false

    private val organizationPosition by lazy {
        intent.getIntExtra(Util.POSITION, -1)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvOrganizationName by lazy {
        findViewById<TextView>(R.id.organizationName)
    }
    private val tvOrganizationRole by lazy {
        findViewById<TextView>(R.id.organizationRole)
    }
    private val tvJoinedAt by lazy {
        findViewById<TextView>(R.id.joinedAt)
    }
    private val tvCreatedAt by lazy {
        findViewById<TextView>(R.id.createdAt)
    }
    private val btnEdit by lazy {
        findViewById<ImageButton>(R.id.btnEdit)
    }
    private val btnLeave by lazy {
        findViewById<Button>(R.id.btnLeave)
    }
    private val btnDelete by lazy {
        findViewById<Button>(R.id.btnDelete)
    }
    private val btnBack by lazy {
        findViewById<ImageButton>(R.id.btnBack)
    }

    private val callback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if (response.isSuccessful) {
                when(method) {
                    "GET" -> {
                        val organization = response.body() as Organization
                        updateOrganizationInfo(organization)
                        if(organization.membership.role == OrganizationRole.OWNER) {
                            btnLeave.visibility = View.GONE
                            btnDelete.visibility = View.VISIBLE
                            btnEdit.visibility = View.VISIBLE
                        } else {
                            btnDelete.visibility = View.GONE
                            btnEdit.visibility = View.GONE
                            btnLeave.visibility = View.VISIBLE
                        }
                    }
                    "DELETE" -> {
                        if(currentOrganization.membership.role == OrganizationRole.OWNER) {
                            val dialogDeleteSuccessful = DialogInfo.newInstance("Deletion successful", "Organization '${currentOrganization.name}' was successfully deleted", DialogType.SUCCESS)
                            dialogDeleteSuccessful.dialogExitListener = this@ActivityOrganizationDetails
                            dialogDeleteSuccessful.show(supportFragmentManager, "DIALOG_DELETE_SUCCESSFUL")
                        } else {
                            val dialogLeaveSuccessful = DialogInfo.newInstance("Leave successful", "You left organization '${currentOrganization.name}'", DialogType.SUCCESS)
                            dialogLeaveSuccessful.dialogExitListener = this@ActivityOrganizationDetails
                            dialogLeaveSuccessful.show(supportFragmentManager, "DIALOG_LEAVE_SUCCESSFUL")
                        }

                    }
                }
            }
            else {
                btnDelete.visibility = View.GONE
                btnEdit.visibility = View.GONE
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable?) {
            btnDelete.visibility = View.GONE
            btnEdit.visibility = View.GONE
            onErrorResponse(call, null)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organization_details)

        val org = savedInstanceState?.getString(CURRENT_ORGANIZATION) as Organization? ?: intent.getSerializableExtra(CURRENT_ORGANIZATION) as Organization
        updateOrganizationInfo(org)

        setSupportActionBar(toolbar)
        btnLeave.setOnClickListener(this)
        btnDelete.setOnClickListener(this)
        btnBack.setOnClickListener(this)
        btnEdit.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getOrganizationService().getOrganizationById(currentOrganization.id)
        call.enqueue(callback as Callback<Organization>)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnEdit -> {
                val dialog = DialogEditOrganization.newInstance(currentOrganization.id, currentOrganization.name)
                dialog.editListener = this
                dialog.show(supportFragmentManager, "DIALOG_EDIT_ORGANIZATION")
            }
            R.id.btnDelete -> {
                val dialog = DialogInfo.newInstance("Confirm deletion", "Are you sure you want to delete organization '${currentOrganization.name}' ?", DialogType.YES_NO)
                dialog.dialogResponseListener = this
                dialog.show(supportFragmentManager, "DIALOG_CONFIRM_DELETE_ORGANIZATION")
            }
            R.id.btnLeave -> {
                val dialog = DialogInfo.newInstance("Confirm leave", "Are you sure you want to leave organization '${currentOrganization.name}' ?", DialogType.YES_NO)
                dialog.dialogResponseListener = this
                dialog.targetRequestCode
                dialog.show(supportFragmentManager, "DIALOG_CONFIRM_DELETE_ORGANIZATION")
            }
        }
    }

    override fun onBackPressed() {
        if (itemWasRemoved) {
            val data = Intent()
            data.putExtra(Util.POSITION, organizationPosition)
            setResult(ITEM_REMOVED, data)
        }
        else {
            if(itemsWasEdited) {
                val data = Intent()
                data.putExtra(Util.POSITION, organizationPosition)
                data.putExtra(EDITED_ORGANIZATION, currentOrganization)
                setResult(ITEM_EDITED, data)
            }
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            btnLeave.visibility = View.GONE
            btnDelete.visibility = View.GONE
            btnEdit.visibility = View.GONE

            if(currentOrganization.membership.role == OrganizationRole.OWNER) {
                val call = ServiceManager.getOrganizationService().deleteOrganizationById(currentOrganization.id)
                call.enqueue(callback as Callback<Void>)
            }
            else {
                val call = ServiceManager.getOrganizationService().deleteOrganizationMemberById(currentOrganization.id, AppTicketChecker.loggedInUser!!.id)
                call.enqueue(callback as Callback<Void>)
            }
        }
    }

    override fun onEdit(editedObject: Organization) {
        itemsWasEdited = true
        updateOrganizationInfo(editedObject)
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
        onBackPressed()
    }

    private fun updateOrganizationInfo(organization: Organization) {
        findViewById<ProgressBar>(R.id.lsOrganizationName).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsOrganizationRole).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsJoinedAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsCreatedAt).visibility = View.INVISIBLE

        currentOrganization = organization

        tvOrganizationName.text = organization.name
        tvOrganizationRole.text = organization.membership.role.role
        tvOrganizationRole.setTextColor(ContextCompat.getColor(applicationContext, organization.membership.role.colorResource))
        tvJoinedAt.text = Util.DATE_TIME_FORMAT.format(organization.membership.joinedAt)
        tvCreatedAt.text = Util.DATE_TIME_FORMAT.format(organization.createdAt)
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = Util.treatBasicError(call, response, supportFragmentManager)
        if (!wasHandled) {
            when(response?.code()) {
                404 -> {
                    val dialogNoOrganization = DialogInfo.newInstance("Organization not found", "The organization you are trying to access no longer exists", DialogType.ERROR)
                    dialogNoOrganization.dialogExitListener = this@ActivityOrganizationDetails
                    dialogNoOrganization.show(supportFragmentManager, "DIALOG_ERROR")
                }
                else -> {
                    val dialogNoOrganization = DialogInfo.newInstance("Unknown error", "An unknown error has happened", DialogType.ERROR)
                    dialogNoOrganization.dialogExitListener = this@ActivityOrganizationDetails
                    dialogNoOrganization.show(supportFragmentManager, "DIALOG_ERROR")
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(CURRENT_ORGANIZATION, currentOrganization)
    }

    companion object {
        const val DIALOG_DELETE = 0
        const val DIALOG_LEAVE = 1
        const val CURRENT_ORGANIZATION = "currentOrganization"
    }
}
