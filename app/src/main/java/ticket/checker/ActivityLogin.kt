package ticket.checker

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.User
import ticket.checker.extras.Constants
import ticket.checker.service.ServiceManager

class ActivityLogin : AppCompatActivity() {

    private val USER_TEXT = "userEtText"
    private val PASS_TEXT = "passEtText"

    private val etUsername : EditText by lazy { findViewById<EditText>(R.id.etUsername)}
    private val etPassword : EditText by lazy { findViewById<EditText>(R.id.etPassword)}
    private val btnLogin : Button by lazy { findViewById<Button>(R.id.btnLogin)}

    private val loginHandler : View.OnClickListener = View.OnClickListener {
        if(validate(etUsername,etPassword)) {
            dialog.setMessage("Retrieving user data ... ")
            dialog.show()
            doLogin(etUsername.text.toString(), etPassword.text.toString())
        }
    }
    private val loginCallback : Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>?, response: Response<User>) {
            dialog.let { dialog.dismiss() }
            if(response.isSuccessful) {
                login(response.body() as User)
            }
            else {
                val alertDialog : AlertDialog.Builder = AlertDialog.Builder(this@ActivityLogin)
                        .setCancelable(true)
                        .setTitle("Login failed")
                when(response.code()) {
                    401,403 -> {
                        alertDialog.setMessage("Username or password was incorrect!")
                        alertDialog.show()
                    }
                    in 500..600 -> {
                        alertDialog.setMessage("Server error!")
                        alertDialog.show()
                    }
                }
            }
        }
        override fun onFailure(call: Call<User>?, t: Throwable?) {
            dialog?.let { dialog.dismiss() }
            val alertDialog : AlertDialog.Builder = AlertDialog.Builder(this@ActivityLogin)
                    .setCancelable(false)
                    .setTitle("Login failed")
                    .setMessage("Error connecting to the server!")
            alertDialog.show()
        }
    }

    private val dialog : ProgressDialog by lazy {
        ProgressDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loadBgnImage()
        initialize()
    }

    private fun validate(vararg ets : EditText) : Boolean {
        var isValid = true
        for(et in ets) {
            if(et.text.isEmpty()) {
                isValid = false
                et.error = "This field can not be empty!"
            }
        }
        return isValid
    }

    private fun loadBgnImage() {
        val bgnImg = R.drawable.bg_gradient
        val imgView = findViewById<ImageView>(R.id.imgBgn)
        Glide.with(this)
                .load(bgnImg)
                .apply(RequestOptions().centerCrop())
                .into(imgView)
    }

    private fun initialize() {
        etUsername.setText("")
        etUsername.error = null
        etUsername.requestFocus()
        etPassword.setText("")
        etPassword.error = null
        btnLogin.setOnClickListener(loginHandler)
    }

    private fun doLogin(username: String, password: String) {
        ServiceManager.createSession(username,password)
        val call : Call<User> = ServiceManager.getUserService().getUser()
        call.enqueue(loginCallback)
    }

    private fun login(user : User) {
        Toast.makeText(this@ActivityLogin,"Login was sucessfull!\n" + user,Toast.LENGTH_LONG).show()
        val intent = Intent(this, ActivityMenu::class.java)

        val bundle = Bundle()
        bundle.putLong(Constants.SESSION_USER_ID,user.id)
        bundle.putString(Constants.SESSION_USER_NAME,user.name)
        bundle.putString(Constants.SESSION_USER_ROLE,user.role)
        bundle.putLong(Constants.SESSION_USER_CREATED_DATE,user.createdDate.time)

        intent.putExtras(bundle)
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(USER_TEXT,etUsername.text.toString())
        outState?.putString(PASS_TEXT,etPassword.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastEtUser = savedInstanceState?.getString(USER_TEXT)
        val lastEtPass = savedInstanceState?.getString(PASS_TEXT)
        etUsername.setText(lastEtUser)
        etPassword.setText(lastEtPass)
    }

}
