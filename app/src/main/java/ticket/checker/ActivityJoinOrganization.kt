package ticket.checker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.beans.Organization
import ticket.checker.services.ServiceManager

class ActivityJoinOrganization : AppCompatActivity(), View.OnClickListener {
    private var isStartedFromApp: Boolean = false
    private var shouldRememberInviteCode: Boolean = false
    private lateinit var inviteCode: String

    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val loadingSpinner by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }
    private val tvResult by lazy {
        findViewById<TextView>(R.id.tvResult)
    }

    private val callback: Callback<Organization> = object : Callback<Organization> {
        override fun onResponse(call: Call<Organization>, response: Response<Organization>) {
            loadingSpinner.visibility = View.GONE
            btnBack.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            if(response.isSuccessful) {
                val organization = response.body() as Organization
                tvResult.text = "Congratulations! You have joined organization \n ${organization.name}"
                tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.yesGreen))
            } else {
                tvResult.text = "Failed to join organization"
                tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.noRed))
            }
        }

        override fun onFailure(call: Call<Organization>, t: Throwable) {
            loadingSpinner.visibility = View.GONE
            btnBack.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            tvResult.text = "Failed to join organization"
            tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.noRed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_organization)

        isStartedFromApp = intent.getBooleanExtra(STARTED_FROM_APP, false)
        if(isStartedFromApp) {
            inviteCode = intent.getStringExtra(INVITE_CODE)!!
        }
    }

    override fun onStart() {
        super.onStart()

        btnBack.setOnClickListener(this)

        if(!isStartedFromApp) {
            val scheme = intent?.scheme
            val data = intent?.data
            inviteCode = if(scheme != null && scheme == "ticheck") {
                data!!.pathSegments!![0]
            } else {
                data!!.pathSegments?.get(1) ?: ""
            }
        }

        if(AppTicketChecker.isLoggedIn()) {
            loadingSpinner.visibility = View.VISIBLE
            btnBack.visibility = View.GONE
            tvResult.visibility = View.GONE
            joinOrganization(inviteCode)
        } else {
            loadingSpinner.visibility = View.GONE
            btnBack.visibility = View.VISIBLE
            tvResult.visibility = View.VISIBLE
            tvResult.text = "You must be logged in to join an organization"
            tvResult.setTextColor(ContextCompat.getColor(applicationContext, R.color.darkerGrey))
            shouldRememberInviteCode = true
        }

    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnBack -> {
                if(AppTicketChecker.isLoggedIn()) {
                    val intent = Intent(this, ActivityOrganizations::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ActivityLogin::class.java)
                    if(shouldRememberInviteCode) {
                        intent.putExtra(INVITE_CODE, inviteCode)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                finish()
            }
        }
    }

    private fun joinOrganization(code: String) {
        val call = ServiceManager.getOrganizationService().joinOrganizationByInviteCode(code)
        call.enqueue(callback)
    }

    companion object {
        const val STARTED_FROM_APP = "startedFromApp"
        const val INVITE_CODE = "inviteCode"
    }
}
