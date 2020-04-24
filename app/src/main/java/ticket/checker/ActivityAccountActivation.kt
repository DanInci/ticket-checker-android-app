package ticket.checker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.services.ServiceManager

class ActivityAccountActivation : AppCompatActivity(), View.OnClickListener {

    private var isStartedFromLogin: Boolean = false

    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val btnSubmit by lazy {
        findViewById<Button>(R.id.btnSubmit)
    }
    private val submitContainer by lazy {
        findViewById<LinearLayout>(R.id.submitContainer)
    }
    private val submitLoadingSpinner by lazy {
        findViewById<ProgressBar>(R.id.submitLoadingSpinner)
    }
    private val loadingSpinner by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val etVerificationCode by lazy {
        findViewById<EditText>(R.id.etVerificationCode)
    }
    private val tvResult by lazy {
        findViewById<TextView>(R.id.tvResult)
    }

    private val callback: Callback<Void> = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            submitContainer.visibility = View.GONE
            loadingSpinner.visibility = View.GONE
            btnBack.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            if(response.isSuccessful) {
                tvResult.text = "Your account has been verified\n You can now login"
                tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.yesGreen))
            } else {
                tvResult.text = "Failed to verify account"
                tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.noRed))
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            submitContainer.visibility = View.GONE
            loadingSpinner.visibility = View.GONE
            btnBack.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            tvResult.text = "Failed to verify account"
            tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.noRed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_activation)

        isStartedFromLogin = intent.getBooleanExtra(STARTED_FROM_LOGIN, false)
    }

    override fun onStart() {
        super.onStart()

        btnBack.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)

        val data = intent?.data
        val code: String? = data?.pathSegments?.get(0)

        initializeFields(code == null)
        if(code != null) {
            loadingSpinner.visibility = View.VISIBLE
            submitVerificationCode(code)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(!isStartedFromLogin) {
            val intent = Intent(this, ActivityLogin::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        finish()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnBack -> {
                if(!isStartedFromLogin) {
                    val intent = Intent(this, ActivityLogin::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                finish()
            }
            R.id.btnSubmit -> {
                if(validate(etVerificationCode)) {
                    etVerificationCode.isEnabled = false
                    btnSubmit.visibility = View.GONE
                    submitLoadingSpinner.visibility = View.VISIBLE
                    submitVerificationCode(etVerificationCode.text.toString())
                }
            }
        }
    }

    private fun validate(vararg ets : EditText) : Boolean {
        var isValid = true
        for(et in ets) {
            if(et.text.isEmpty()) {
                isValid = false
                et.error = "This field can not be empty"
            }
        }
        return isValid
    }

    private fun submitVerificationCode(code: String) {
        val call = ServiceManager.getAuthService().verifyAccount(code)
        call.enqueue(callback)
    }

    private fun initializeFields(isInput: Boolean) {
        loadingSpinner.visibility = View.GONE
        tvResult.visibility = View.GONE
        if(isInput) {
            submitContainer.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE

            etVerificationCode.isEnabled = true
            etVerificationCode.setText("")
            etVerificationCode.error = null
        } else {
            submitContainer.visibility = View.GONE
            btnBack.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VERIFICATION_CODE, etVerificationCode.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        etVerificationCode.setText(savedInstanceState.getString(VERIFICATION_CODE) ?: "")
    }

    companion object {
        const val STARTED_FROM_LOGIN = "startedFromLogin"
        private const val VERIFICATION_CODE = "verificationCode"
    }
}
