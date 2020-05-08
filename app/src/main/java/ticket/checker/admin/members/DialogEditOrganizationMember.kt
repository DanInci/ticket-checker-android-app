package ticket.checker.admin.members

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
import ticket.checker.beans.OrganizationMember
import ticket.checker.beans.OrganizationMemberDefinition
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogEditOrganizationMember internal constructor() : DialogFragment(), View.OnClickListener {

    lateinit var editListener: EditListener<OrganizationMember>

    private lateinit var organizationId: UUID
    private lateinit var userId: UUID
    private lateinit var name: String

    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnEdit by lazy {
        dialogView.findViewById<Button>(R.id.btnEdit)
    }
    private val tvTitle by lazy {
        dialogView.findViewById<TextView>(R.id.dialogTitle)
    }
    private val spinnerRole by lazy {
        dialogView.findViewById<Spinner>(R.id.spinnerRole)
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

    private val getCallback : Callback<OrganizationMember> = object : Callback<OrganizationMember> {
        override fun onResponse(call: Call<OrganizationMember>, response: Response<OrganizationMember>) {
            if (response.isSuccessful) {
                loadingSpinner.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                updateWithUserInfo(response.body() as OrganizationMember)
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<OrganizationMember>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            onErrorResponse(call, null)
        }
    }

    private val editCallback: Callback<OrganizationMember> = object : Callback<OrganizationMember> {
        override fun onResponse(call: Call<OrganizationMember>, response: Response<OrganizationMember>) {
            loadingSpinner.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            if (response.isSuccessful) {
                editListener.onEdit(response.body() as OrganizationMember)
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<OrganizationMember>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
            userId = arguments?.getSerializable(USER_ID) as UUID
            name = arguments?.getString(USER_NAME)!!
        }
    }

    override fun onStart() {
        super.onStart()
        val getCall = ServiceManager.getOrganizationService().getOrganizationMemberById(organizationId, userId)
        getCall.enqueue(getCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView = inflater.inflate(R.layout.dialog_edit_member, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener(this)
        btnEdit.setOnClickListener(this)
        tvTitle.text = "Edit '$name'"
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnEdit -> {
                editOrganizationMember(organizationId, userId, spinnerRole.selectedItem as OrganizationRole)
            }
        }
    }

    private fun updateWithUserInfo(member : OrganizationMember) {
        spinnerRole.isClickable = true
        spinnerRole.isFocusable = true
        spinnerRole.adapter = ArrayAdapter(context!!, R.layout.spinner_item, OrganizationRole.values().filter{ r -> r != OrganizationRole.OWNER })
        spinnerRole.setSelection(member.role.ordinal - 1)
    }

    private fun editOrganizationMember(organizationId: UUID, userId: UUID, role : OrganizationRole) {
        tvResult.visibility = View.INVISIBLE
        btnEdit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val definition = OrganizationMemberDefinition(role)
        val call = ServiceManager.getOrganizationService().updateOrganizationMemberById(organizationId, userId, definition)
        call.enqueue(editCallback)
    }

    private fun onErrorResponse(call: Call<OrganizationMember>, response: Response<OrganizationMember>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if (!wasHandled) {
            when(response?.code()) {
                404 -> {
                    bottomContainer.visibility = View.GONE
                    tvResult.visibility = View.VISIBLE
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Organization member was not found"
                }
                else -> {
                    bottomContainer.visibility = View.GONE
                    tvResult.visibility = View.VISIBLE
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Unexpected error occurred"
                }
            }
        }
    }

    companion object {
        private const val ORGANIZATION_ID = "organizationId"
        private const val USER_ID = "userId"
        private const val USER_NAME = "userName"

        fun newInstance(organizationId: UUID, userId: UUID, name: String): DialogEditOrganizationMember {
            val fragment = DialogEditOrganizationMember()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putSerializable(USER_ID, userId)
            args.putString(USER_NAME, name)
            fragment.arguments = args
            return fragment
        }
    }

}