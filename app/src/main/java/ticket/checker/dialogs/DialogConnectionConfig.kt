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
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import ticket.checker.AppTicketChecker
import ticket.checker.R

/**
 * Created by Dani on 22.03.2018.
 */
class DialogConnectionConfig : DialogFragment(), View.OnClickListener {

    private var btnClose : ImageButton? = null
    private var etAddress : EditText? = null
    private var etPort : EditText? = null
    private var btnSubmit : Button?  = null
    private var loadingSpinner : ProgressBar? = null
    private var tvResult : TextView? = null

    private var host : String = ""
    private var port : String = ""

    private val callback = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            loadingSpinner?.visibility = View.GONE
            btnSubmit?.visibility = View.VISIBLE
            tvResult?.visibility = View.VISIBLE
            when(response.code()) {
                200 -> {
                    AppTicketChecker.saveConnectionConfig(host, port)
                    AppTicketChecker.host = host
                    AppTicketChecker.port = port
                    tvResult?.text = "Connection established successfully"
                    tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.yesGreen))
                }
                else -> {
                    etAddress?.setText("")
                    etPort?.setText("")
                    tvResult?.text = "Connection has failed"
                    tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                }
            }
        }
        override fun onFailure(call: Call<Void>, t: Throwable?) {
            etAddress?.setText("")
            etPort?.setText("")
            loadingSpinner?.visibility = View.GONE
            btnSubmit?.visibility = View.VISIBLE
            tvResult?.visibility = View.VISIBLE
            tvResult?.text = "Connection has failed"
            tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            host = it!!.getString(CURRENT_HOST, "")
            port = it.getString(CURRENT_PORT, "")!!
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_connection_config, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etAddress = view.findViewById(R.id.etAddress)
        etAddress?.setText(host)
        etPort = view.findViewById(R.id.etPortNumber)
        etPort?.setText(port)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnSubmit?.setOnClickListener(this)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvResult = view.findViewById(R.id.tvResult)
    }

    override fun onClick(v: View) {
       when(v.id) {
           R.id.btnClose -> dismiss()
           R.id.btnSubmit -> {
               tvResult?.visibility = View.INVISIBLE
               if(validate()) {
                   btnSubmit?.visibility = View.GONE
                   loadingSpinner?.visibility = View.VISIBLE
                   getConnectionDetails()
               }
           }
       }
    }

    private fun validate() : Boolean {
        val address = etAddress?.text.toString()
        if(address.isEmpty()) {
            etAddress?.error =  "This field is required"
            return false
        }
        return true
    }

    private fun getConnectionDetails() {
        host = etAddress?.text.toString()
        port = etPort?.text.toString()
        val builder =  Retrofit.Builder()
                .baseUrl("http://$host:$port/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(OkHttpClient.Builder().build())
        val retrofit = builder.build()
        val connectionService = retrofit.create(ConnectionService::class.java)
        val call : Call<Void> = connectionService.healthCheck()
        call.enqueue(callback)
    }


    private interface ConnectionService {
        @GET("/health")
        fun healthCheck() : Call<Void>
    }

    companion object {
        private const val CURRENT_HOST = "currentHost"
        private const val CURRENT_PORT = "currentPort"

        fun newInstance(currentHost: String, currentPort: String): DialogConnectionConfig {
            val fragment = DialogConnectionConfig()
            val args = Bundle()
            args.putString(CURRENT_HOST, currentHost)
            args.putString(CURRENT_PORT, currentPort)
            fragment.arguments = args
            return fragment
        }
    }
}

