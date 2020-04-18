package ticket.checker

import android.content.Intent
import android.os.Bundle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.UserProfile
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
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
                collapsingToolbar.title = R.string.app_name.toString()
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

    private val updateUserInfoCallback: Callback<UserProfile> = object : Callback<UserProfile> {
        override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
            if (response.isSuccessful) {
                AppTicketChecker.loggedInUser = response.body() as UserProfile

                if (!firstLoadHappen) {
                    AppTicketChecker.selectedOrganization = AppTicketChecker.selectedOrganization?.copy(role = OrganizationRole.USER)
                    firstLoadHappen = true
                    switchViews()
                }
                updateProfileInfo()
            } else {
                Util.treatBasicError(call, response, supportFragmentManager)
            }
        }

        override fun onFailure(call: Call<UserProfile>, t: Throwable?) {
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
//        loadCollapsingToolbarImg()
        setSupportActionBar(toolbar)
        appBarLayout.addOnOffsetChangedListener(appBarOffsetChangeListener)
        cvControlPanel.setOnClickListener(this)
        cvScan.setOnClickListener(this)
        cvStatistics.setOnClickListener(this)

        if (AppTicketChecker.loggedInUser != null) {
            firstLoadHappen = true
            switchViews()
        }
    }

    override fun onStart() {
        super.onStart()
        updateProfileInfo()
        val call: Call<UserProfile> = ServiceManager.getUserService().getUserById(AppTicketChecker.loggedInUser!!.id)
        call.enqueue(updateUserInfoCallback)
    }

//    private fun loadCollapsingToolbarImg() {
//        val collapsingToolbarBackground = findViewById<ImageView>(R.id.bg_collapsingToolbar)
//        val baseUrl = if(AppTicketChecker.port != "") "http://${AppTicketChecker.address}:${AppTicketChecker.port}" else "http://${AppTicketChecker.address}"
//        Glide.with(applicationContext)
//                .asBitmap()
//                .load("$baseUrl/images/header.png")
//                .into(object : SimpleTarget<Bitmap>() {
//                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                        headerHasLoaded=true
//                        collapsingToolbar.title = " "
//                        collapsingToolbarBackground.setImageBitmap(resource)
//                        collapsingToolbarBackground.scaleType = ImageView.ScaleType.CENTER_CROP
//                    }
//                })
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when (AppTicketChecker.selectedOrganization!!.role) {
            OrganizationRole.OWNER, OrganizationRole.ADMIN -> menuInflater.inflate(R.menu.menu_admin, menu)
            OrganizationRole.PUBLISHER -> menuInflater.inflate(R.menu.menu_publisher, menu)
            OrganizationRole.VALIDATOR -> menuInflater.inflate(R.menu.menu_validator, menu)
            OrganizationRole.USER -> menuInflater.inflate(R.menu.menu_user, menu)
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
                AppTicketChecker.selectedOrganization = AppTicketChecker.selectedOrganization?.copy(pretendedRole = OrganizationRole.ADMIN)
                switchViews()
                tvCurrentRole.text = AppTicketChecker.selectedOrganization!!.pretendedRole.name
                currentMenuItemId = R.id.action_admin_mode
            }
            R.id.action_publisher_mode -> {
                AppTicketChecker.selectedOrganization = AppTicketChecker.selectedOrganization?.copy(pretendedRole = OrganizationRole.PUBLISHER)
                switchViews()
                tvCurrentRole.text = AppTicketChecker.selectedOrganization!!.pretendedRole.name
                currentMenuItemId = R.id.action_publisher_mode
            }
            R.id.action_validator_mode -> {
                AppTicketChecker.selectedOrganization = AppTicketChecker.selectedOrganization?.copy(pretendedRole = OrganizationRole.VALIDATOR)
                switchViews()
                tvCurrentRole.text = AppTicketChecker.selectedOrganization!!.pretendedRole.name
                currentMenuItemId = R.id.action_validator_mode
            }
            R.id.action_user_mode -> {
                AppTicketChecker.selectedOrganization = AppTicketChecker.selectedOrganization?.copy(pretendedRole = OrganizationRole.USER)
                switchViews()
                tvCurrentRole.text = AppTicketChecker.selectedOrganization!!.pretendedRole.name
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
//                val intent = Intent(this, ActivityControlPanel::class.java)
//                startActivity(intent)
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
        when (AppTicketChecker.selectedOrganization!!.pretendedRole) {
            OrganizationRole.OWNER, OrganizationRole.ADMIN -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Administration area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your administrator priveleges to add, delete tickets or see user information"
            }
            OrganizationRole.PUBLISHER -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Publisher area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your publisher priveleges to add tickets or see information about them"
            }
            OrganizationRole.VALIDATOR -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Validator area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your validator priveleges to validate/invalidate tickets or see information about them"
            }
            OrganizationRole.USER -> {
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

            tvName.text = AppTicketChecker.loggedInUser?.name
//            tvCreated.text = DATE_FORMAT_MONTH_NAME.format(AppTicketChecker.loggedInUser?.createdAt)
            tvHighestRole.text = AppTicketChecker.selectedOrganization!!.role.role
            tvCurrentRole.text = AppTicketChecker.selectedOrganization!!.pretendedRole.role
            tvCreatedTickets.text = "0"
            tvValidatedTickets.text = "0"
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
