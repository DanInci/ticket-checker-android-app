package ticket.checker.dialogs

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
import ticket.checker.beans.OrganizationDefinition
import ticket.checker.beans.Organization
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogEditOrganization internal constructor(): DialogFragment(), View.OnClickListener {

    lateinit var editListener: EditListener<Organization>

    private lateinit var organizationId: UUID
    private lateinit var organizationName: String

    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnEdit by lazy {
        dialogView.findViewById<Button>(R.id.btnEdit)
    }
    private val etOrganizationName by lazy {
        dialogView.findViewById<EditText>(R.id.etOrganizationName)
    }
    private val bottomContainer by lazy {
        dialogView.findViewById<EditText>(R.id.buttonsContainer)
    }
    private val loadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        dialogView.findViewById<TextView>(R.id.tvResult)
    }

    private val callback : Callback<Organization> = object : Callback<Organization> {
        override fun onResponse(call: Call<Organization>, response: Response<Organization>) {
            if (response.isSuccessful) {
                when(call.request().method()){
                    "GET" -> {
                        loadingSpinner.visibility = View.GONE
                        btnEdit.visibility = View.VISIBLE
                        updateOrganizationInfo(response.body() as Organization)
                    }
                    "PUT" -> {
                        editListener.onEdit(response.body() as Organization)
                        dismiss()
                    }
                }
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<Organization>, t: Throwable) {
            when(call.request().method()) {
                "GET" -> {
                    loadingSpinner.visibility = View.GONE
                }
                "PUT" -> {
                    loadingSpinner.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                }
            }

            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
            organizationName = arguments?.getString(ORGANIZATION_NAME) ?: "NONE"
        }
    }

    override fun onStart() {
        super.onStart()
        val getCall = ServiceManager.getOrganizationService().getOrganizationById(organizationId)
        getCall.enqueue(callback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView =  inflater.inflate(R.layout.dialog_edit_organization, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener(this)
        btnEdit.setOnClickListener(this)

        updateOrganizationInfo(organizationName)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnEdit -> {
                if (validate(etOrganizationName)) {
                    editOrganization(etOrganizationName.text.toString())
                }
            }
        }
    }

    private fun updateOrganizationInfo(organization: Organization) {
        updateOrganizationInfo(organization.name)
    }

    private fun updateOrganizationInfo(name: String) {
        etOrganizationName.isEnabled = true
        etOrganizationName.setText(name)
        etOrganizationName.error = null
        etOrganizationName.post { etOrganizationName.setSelection(name.length) }

        organizationName = name
    }

    private fun validate(vararg ets : EditText) : Boolean {
        var isValid = true
        for(et in ets) {
            if (et.text.isEmpty()) {
                isValid = false
                et.error = "This field is required"
            }
        }
        return isValid
    }

    private fun editOrganization(name: String) {
        tvResult.visibility = View.INVISIBLE
        btnEdit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val definition = OrganizationDefinition(name)
        val call = ServiceManager.getOrganizationService().updateOrganizationById(organizationId, definition)
        call.enqueue(callback)
    }

    private fun onErrorResponse(call: Call<Organization>, response: Response<Organization>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if (!wasHandled) {
            bottomContainer?.visibility = View.GONE
            tvResult?.visibility = View.VISIBLE
            when(response?.code()) {
                  400 -> {
                      etOrganizationName.error = "Organization with this name already exists!"
                  }
                404 -> {
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Organization was not found"
                }
                else -> {
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Unknown error has happened"
                }
            }
        }
    }

    companion object {
        private const val ORGANIZATION_ID = "organizationId"
        private const val ORGANIZATION_NAME = "organizationName"

        fun newInstance(organizationId: UUID, organizationName: String): DialogEditOrganization {
            val fragment = DialogEditOrganization()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putString(ORGANIZATION_NAME, organizationName)
            fragment.arguments = args
            return fragment
        }
    }

}