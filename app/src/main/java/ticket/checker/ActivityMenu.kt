package ticket.checker

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.AppTicketChecker.Companion.loggedInUserCreatedDate
import ticket.checker.AppTicketChecker.Companion.loggedInUserId
import ticket.checker.AppTicketChecker.Companion.loggedInUserName
import ticket.checker.AppTicketChecker.Companion.loggedInUserSoldTicketsNo
import ticket.checker.AppTicketChecker.Companion.loggedInUserType
import ticket.checker.AppTicketChecker.Companion.loggedInUserValidatedTicketsNo
import ticket.checker.AppTicketChecker.Companion.pretendedUserType
import ticket.checker.beans.User
import ticket.checker.extras.UserType
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.services.ServiceManager

class ActivityMenu : AppCompatActivity(), View.OnClickListener {

    private var headerHasLoaded = false

    private val collapsingToolbar by lazy {
        findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val appBarLayout by lazy {
        findViewById<AppBarLayout>(R.id.appBar)
    }
    private val appBarOffsetChangeListener = object : AppBarLayout.OnOffsetChangedListener {
        var scrollRange = -1

        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                menuIsShown = true
                collapsingToolbar.title = AppTicketChecker.appName
                invalidateOptionsMenu()
            } else if (menuIsShown) {
                menuIsShown = false
                if (headerHasLoaded) {
                    collapsingToolbar.title = " "
                }
                invalidateOptionsMenu()
            }
        }
    }
    private val updateUserInfoCallback: Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            if (response.isSuccessful) {
                val user = response.body() as User
                loggedInUserId = user.id
                loggedInUserName = user.name
                loggedInUserCreatedDate = user.createdDate
                loggedInUserType = user.userType
                loggedInUserSoldTicketsNo = user.soldTicketsNo
                loggedInUserValidatedTicketsNo = user.validatedTicketsNo

                if (!firstLoadHappen) {
                    pretendedUserType = user.userType
                    firstLoadHappen = true
                    switchViews()
                }
                updateProfileInfo()
            } else {
                Util.treatBasicError(call, response, supportFragmentManager)
            }
        }

        override fun onFailure(call: Call<User>, t: Throwable?) {
            Util.treatBasicError(call, null, supportFragmentManager)
        }
    }
    private val tvName by lazy {
        findViewById<TextView>(R.id.name)
    }
    private val tvCreated by lazy {
        findViewById<TextView>(R.id.created)
    }
    private val tvHighestRole by lazy {
        findViewById<TextView>(R.id.highestRole)
    }
    private val tvCurrentRole by lazy {
        findViewById<TextView>(R.id.currentRole)
    }
    private val tvCreatedTickets by lazy {
        findViewById<TextView>(R.id.createdTickets)
    }
    private val tvValidatedTickets by lazy {
        findViewById<TextView>(R.id.validatedTickets)
    }
    private val cvScan by lazy {
        findViewById<CardView>(R.id.scan)
    }
    private val cvStatistics by lazy {
        findViewById<CardView>(R.id.statistics)
    }
    private val cvControlPanel by lazy {
        findViewById<CardView>(R.id.controlPanel)
    }

    var menuIsShown = false
    var firstLoadHappen = false

    private var currentMenuItemId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMenuItemId = savedInstanceState?.getInt(CURRENT_MENU_ITEM_ID) ?: -1

        setContentView(R.layout.activity_menu)
        loadCollapsingToolbarImg()
        setSupportActionBar(toolbar)
        appBarLayout.addOnOffsetChangedListener(appBarOffsetChangeListener)
        cvControlPanel.setOnClickListener(this)
        cvScan.setOnClickListener(this)
        cvStatistics.setOnClickListener(this)

        if (loggedInUserId != null) {
            firstLoadHappen = true
            switchViews()
        }
    }

    override fun onStart() {
        super.onStart()
        updateProfileInfo()
        val call: Call<User> = ServiceManager.getUserService().getUser()
        call.enqueue(updateUserInfoCallback)
    }

    private fun loadCollapsingToolbarImg() {
        val collapsingToolbarBackground = findViewById<ImageView>(R.id.bg_collapsingToolbar)
        val baseUrl = if(AppTicketChecker.port != "") "http://${AppTicketChecker.address}:${AppTicketChecker.port}" else "http://${AppTicketChecker.address}"
        Glide.with(applicationContext)
                .asBitmap()
                .load("$baseUrl/images/header.png")
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        headerHasLoaded=true
                        collapsingToolbar.title = " "
                        collapsingToolbarBackground.setImageBitmap(resource)
                        collapsingToolbarBackground.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when (loggedInUserType) {
            UserType.ADMIN -> menuInflater.inflate(R.menu.menu_admin, menu)
            UserType.PUBLISHER -> menuInflater.inflate(R.menu.menu_publisher, menu)
            UserType.VALIDATOR -> menuInflater.inflate(R.menu.menu_validator, menu)
            UserType.USER -> menuInflater.inflate(R.menu.menu_user, menu)
        }
        if (!menuIsShown) {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = false
            }
        } else {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = true
            }
        }
        if(currentMenuItemId == -1) {
            menu.getItem(0).isChecked = true
        }
        else {
            checkMenuItem(currentMenuItemId)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var validSelection = true
        when (item.itemId) {
            R.id.action_admin_mode -> {
                pretendedUserType = UserType.ADMIN
                switchViews()
                tvCurrentRole.text = pretendedUserType.name
                currentMenuItemId = R.id.action_admin_mode
            }
            R.id.action_publisher_mode -> {
                pretendedUserType = UserType.PUBLISHER
                switchViews()
                tvCurrentRole.text = pretendedUserType.name
                currentMenuItemId = R.id.action_publisher_mode
            }
            R.id.action_validator_mode -> {
                pretendedUserType = UserType.VALIDATOR
                switchViews()
                tvCurrentRole.text = pretendedUserType.name
                currentMenuItemId = R.id.action_validator_mode
            }
            R.id.action_user_mode -> {
                pretendedUserType = UserType.USER
                switchViews()
                tvCurrentRole.text = pretendedUserType.name
                currentMenuItemId = R.id.action_user_mode
            }
            R.id.action_logout -> logout()
            else -> {
                validSelection = false
            }
        }
        if (validSelection) {
            checkMenuItem(item.itemId)
        }
        return validSelection
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.controlPanel -> {
                val intent = Intent(this, ActivityControlPanel::class.java)
                startActivity(intent)
            }
            R.id.scan -> {
                val intent = Intent(this, ActivityScan::class.java)
                startActivity(intent)
            }
            R.id.statistics -> {
                val intent = Intent(this, ActivityStatistics::class.java)
                startActivity(intent)
            }
        }
    }

    private fun switchViews() {
        when (pretendedUserType) {
            UserType.ADMIN -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Administration area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your administrator priveleges to add, delete tickets or see user information"
            }
            UserType.PUBLISHER -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Publisher area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your publisher priveleges to add tickets or see information about them"
            }
            UserType.VALIDATOR -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Validator area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your validator priveleges to validate/invalidate tickets or see information about them"
            }
            UserType.USER -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.GONE
            }
        }
    }

    private fun updateProfileInfo() {
        if (firstLoadHappen) {
            findViewById<ProgressBar>(R.id.lsName).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCreated).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsHighestRole).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCurrentRole).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCreatedTickets).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsValidatedTickets).visibility = View.GONE

            tvName.text = loggedInUserName
            tvCreated.text = DATE_FORMAT.format(loggedInUserCreatedDate)
            tvHighestRole.text = loggedInUserType.name
            tvCurrentRole.text = pretendedUserType.name
            tvCreatedTickets.text = loggedInUserSoldTicketsNo.toString()
            tvValidatedTickets.text = loggedInUserValidatedTicketsNo.toString()
        }
    }

    private fun checkMenuItem(menuItemId: Int) {
        val menu = toolbar.menu
        if (menuItemId != R.id.action_logout) {
            (0 until (menu.size() - 1))
                    .map { menu.getItem(it) }
                    .forEach { it.isChecked = it.itemId == menuItemId }
        }
    }

    private fun logout() {
        AppTicketChecker.clearSession()
        val intent = Intent(this, ActivityLogin::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_MENU_ITEM_ID, currentMenuItemId)
    }

    companion object {
        private const val CURRENT_MENU_ITEM_ID = "currentMenuItemId"
    }

}
