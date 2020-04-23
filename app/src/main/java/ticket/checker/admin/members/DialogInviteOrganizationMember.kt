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
import ticket.checker.beans.OrganizationInvite
import ticket.checker.beans.OrganizationInviteDefinition
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogInviteOrganizationMember internal constructor() : DialogFragment(), View.OnClickListener {

    private lateinit var organizationId: UUID
    private lateinit var dialogView: View

    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnSubmit by lazy {
        dialogView.findViewById<Button>(R.id.btnSubmit)
    }
    private val etEmail by lazy {
        dialogView.findViewById<EditText>(R.id.etEmail)
    }
    private val loadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        dialogView.findViewById<TextView>(R.id.tvResult)
    }

    private val submitCallback = object : Callback<OrganizationInvite> {
        override fun onResponse(call: Call<OrganizationInvite>, response: Response<OrganizationInvite>) {
            loadingSpinner.visibility = View.GONE
            btnSubmit.visibility = View.VISIBLE
            if(response.isSuccessful) {
                tvResult.visibility = View.VISIBLE
                tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.yesGreen))
                tvResult.text = "An email invitation has been sent"
                resetFields()
            }
            else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<OrganizationInvite>, t: Throwable?) {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialogView = inflater.inflate(R.layout.dialog_invite_member, container, false)
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

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnSubmit -> {
                if(validate(etEmail)) {
                    inviteMember(etEmail.text.toString())
                }
            }
        }
    }

    private fun resetFields() {
        etEmail.setText("")
        etEmail.error = null
        etEmail.requestFocus()
    }

    private fun validate(vararg ets : EditText) : Boolean {
        tvResult.visibility = View.INVISIBLE

        var isValid = true
        for(et in ets) {
            if(et.text.isEmpty()) {
                isValid = false
                et.error = "This field can not be empty!"
            } else {
                when(et.id) {
                    R.id.etEmail -> {
                        if(!Util.isEmailValid(et.text.toString())) {
                            isValid = false
                            et.error = "Email format is not correct"
                        }
                    }
                }
            }
        }
        return isValid
    }

    private fun inviteMember(email: String) {
        tvResult.visibility = View.INVISIBLE
        btnSubmit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val invite = OrganizationInviteDefinition(email)
        val call = ServiceManager.getOrganizationService().inviteIntoOrganization(organizationId, invite)
        call.enqueue(submitCallback)
    }

    private fun onErrorResponse(call : Call<OrganizationInvite>, response : Response<OrganizationInvite>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        if(!wasHandled) {
            when(response?.code()) {
                404 -> {
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Organization does not exist"
                }
                409 -> {
                    etEmail.error = "An invitation has already been sent to that email"
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

        fun newInstance(organizationId: UUID): DialogInviteOrganizationMember {
            val fragment = DialogInviteOrganizationMember()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            fragment.arguments = args
            return fragment
        }
    }
}
