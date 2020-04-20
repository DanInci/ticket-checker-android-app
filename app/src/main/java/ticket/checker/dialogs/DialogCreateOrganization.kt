package ticket.checker.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.R
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.beans.*
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager

class DialogCreateOrganization : DialogFragment(), View.OnClickListener {

    lateinit var listChangeListener: ListChangeListener<OrganizationList>
    private lateinit var dialogView: View

    private val etOrganizationName by lazy {
        dialogView.findViewById<EditText>(R.id.etOrganizationName)
    }
    private val btnClose by lazy {
        dialogView.findViewById<ImageButton>(R.id.btnClose)
    }
    private val btnSubmit by lazy {
        dialogView.findViewById<Button>(R.id.btnSubmit)
    }
    private val loadingSpinner by lazy {
        dialogView.findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        dialogView.findViewById<TextView>(R.id.tvResult)
    }

    private val submitCallback = object : Callback<OrganizationProfile> {
        override fun onResponse(call: Call<OrganizationProfile>, response: Response<OrganizationProfile>) {
            loadingSpinner.visibility = View.GONE
            btnSubmit.visibility = View.VISIBLE
            if(response.isSuccessful) {
                val organization = response.body() as OrganizationProfile
                listChangeListener.onAdd(organization.toOrganizationList())
                dismiss()
            }
            else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<OrganizationProfile>, t: Throwable?) {
            loadingSpinner.visibility = View.GONE
            btnSubmit.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialogView =  inflater.inflate(R.layout.dialog_create_organization, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCanceledOnTouchOutside(false)
        btnClose.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)

        resetFields()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnSubmit -> {
                if(validate(etOrganizationName)) {
                    submitOrganization(etOrganizationName.text.toString())
                }
            }
        }
    }

    private fun validate(vararg ets : EditText) : Boolean {
        var isValid = true
        for(et in ets) {
            if (et.text.isEmpty()) {
                isValid = false
                et.error = "This field can not be empty!"
            }
        }
        return isValid
    }

    private fun submitOrganization(name: String) {
        tvResult.visibility = View.INVISIBLE
        btnSubmit.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE

        val definition = OrganizationDefinition(name)
        val call = ServiceManager.getOrganizationService().createOrganization(definition)
        call.enqueue(submitCallback)
    }

    private fun onErrorResponse(call : Call<OrganizationProfile>, response : Response<OrganizationProfile>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
        tvResult?.visibility = View.VISIBLE
        if(!wasHandled){
            when(response?.code()) {
                400 -> {
                    etOrganizationName.error = "Organization with this name already exists!"
                }
                else -> {
                    tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                    tvResult.text = "Unknown error has happened"
                }
            }
        }
    }

    private fun resetFields() {
        etOrganizationName.setText("")
        etOrganizationName.error = null
        etOrganizationName.requestFocus()
    }


}
