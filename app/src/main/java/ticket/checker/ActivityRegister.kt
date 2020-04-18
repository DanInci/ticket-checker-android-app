package ticket.checker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.RegistrationData
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager


class ActivityRegister : AppCompatActivity(), View.OnClickListener {

    private var successfullyRegisteredEmail: String? = null

    private val etEmail by lazy { findViewById<EditText>(R.id.etEmail)}
    private val etPassword by lazy { findViewById<EditText>(R.id.etPassword)}
    private val etPasswordRepeat by lazy { findViewById<EditText>(R.id.etPasswordRepeat)}
    private val etName by lazy { findViewById<EditText>(R.id.etName)}
    private val btnRegister by lazy { findViewById<Button>(R.id.btnRegister) }
    private val btnBack by lazy { findViewById<ImageView>(R.id.btnBack) }
    private val loadingSpinner by lazy {findViewById<ProgressBar>(R.id.loadingSpinner)}
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }

    private val registerCallback : Callback<Void> = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if(response.isSuccessful) {
                tvResult.text = "Account was successfully registered.\n Check your email for confirmation"
                tvResult.setTextColor(ContextCompat.getColor(this@ActivityRegister, R.color.white))
                successfullyRegisteredEmail = etEmail.text.toString()
                initializeFields()
            } else {
                when(response.code()) {
                    400 -> {
                        tvResult.text = "Password criteria was not met"

                    }
                    409 -> {
                        tvResult.text = "Email is already registered"
                    }
                    else -> {
                        tvResult.text = "Unexpected error occurred while registering the account"
                    }
                }
                tvResult.setTextColor(ContextCompat.getColor(this@ActivityRegister, R.color.noRed))
                successfullyRegisteredEmail = null
                initializeFields()
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            tvResult.text = "An error occurred while registering the account"
            tvResult.setTextColor(ContextCompat.getColor(this@ActivityRegister, R.color.noRed))
            successfullyRegisteredEmail = null
            Util.treatBasicError(call, null, supportFragmentManager)
            initializeFields()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        loadBgnImage()
        this.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backToLogin()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        initializeFields()
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnRegister -> {
                if(validate(etEmail, etPassword, etPasswordRepeat, etName)) {
                    startLoading()
                    val definition = RegistrationData(etEmail.text.toString(), etPassword.text.toString(), etName.text.toString())
                    val call = ServiceManager.getAuthService().register(definition)
                    call.enqueue(registerCallback)
                }
            }
            R.id.btnBack -> {
                backToLogin()
            }
        }
    }

    private fun loadBgnImage() {
        val bgnImg = R.drawable.bg_gradient
        val imgView = findViewById<ImageView>(R.id.imgBgn)
        Glide.with(this)
                .load(bgnImg)
                .apply(RequestOptions().centerCrop())
                .into(imgView)
    }


    private fun initializeFields() {
        etEmail.isEnabled = true
        etEmail.setText("")
        etEmail.error = null
        etEmail.requestFocus()
        etPassword.isEnabled = true
        etPassword.setText("")
        etPassword.error = null
        etPasswordRepeat.isEnabled = true
        etPasswordRepeat.setText("")
        etPasswordRepeat.error = null
        etName.isEnabled = true
        etName.setText("")
        etName.error = null
        btnRegister.isClickable = true
        btnRegister.visibility = View.VISIBLE
        btnRegister.setOnClickListener(this)
        btnBack.isClickable = true
        btnBack.setOnClickListener(this)
        loadingSpinner.visibility = View.GONE
        tvResult.visibility = View.VISIBLE
    }

    private fun startLoading() {
        etEmail.isEnabled = false
        etPassword.isEnabled = false
        etPasswordRepeat.isEnabled = false
        etName.isEnabled = false
        btnBack.isClickable = false
        btnRegister.isClickable = false
        btnRegister.visibility = View.GONE
        loadingSpinner.visibility = View.VISIBLE
        tvResult.text = ""
        tvResult.visibility = View.GONE
    }

    private fun validate(vararg ets : EditText) : Boolean {
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
                    R.id.etPassword -> {
                        if(!Util.isPasswordValid(et.text.toString())) {
                            isValid = false
                            et.error = "Password must be at least 6 characters long, including an uppercase and a lowercase letter"
                        }
                    }
                    R.id.etPasswordRepeat -> {
                        val etPassword = ets.find { t -> t.id == R.id.etPassword }
                        if(etPassword != null && etPassword.text.toString() != et.text.toString()) {
                            isValid = false
                            etPassword.error = "Passwords do not match"
                            et.error = "Passwords do not match"
                        }
                    }
                }
            }
        }
        return isValid
    }

    private fun backToLogin() {
        val returnIntent = Intent()
        if(successfullyRegisteredEmail != null) {
            returnIntent.putExtra("result", successfullyRegisteredEmail)
            setResult(Activity.RESULT_OK, returnIntent)
        } else {
            setResult(Activity.RESULT_CANCELED, returnIntent)
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EMAIL_TEXT, etEmail.text.toString())
        outState.putString(PASS_TEXT, etPassword.text.toString())
        outState.putString(REPEAT_PASS_TEXT, etPasswordRepeat.text.toString())
        outState.putString(NAME_TEXT, etName.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastEtUser = savedInstanceState.getString(EMAIL_TEXT)
        val lastEtPass = savedInstanceState.getString(PASS_TEXT)
        val lastEtRepeatPass = savedInstanceState.getString(REPEAT_PASS_TEXT)
        val lastEtName = savedInstanceState.getString(NAME_TEXT)
        etEmail.setText(lastEtUser)
        etPassword.setText(lastEtPass)
        etPasswordRepeat.setText(lastEtRepeatPass)
        etName.setText(lastEtName)
    }

    companion object {
        private const val EMAIL_TEXT = "emailEtText"
        private const val PASS_TEXT = "passEtText"
        private const val REPEAT_PASS_TEXT = "repeatPassEtText"
        private const val NAME_TEXT = "nameEtText"
    }
}
