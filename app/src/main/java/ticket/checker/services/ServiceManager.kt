package ticket.checker.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Dani on 24.01.2018.
 */
object ServiceManager {
    private const val API_BASE_URL  = "http://89.42.135.219:8080/"
    private var httpClient = OkHttpClient.Builder()
    private var builder = Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
    private var retrofit = builder.build()

    private var userService: UserService? = null
    private var ticketService: TicketService? = null
    private var numbersService: NumbersService? = null

    fun createSession(username : String, password : String) {
        val interceptor = AuthInterceptor(username,password)
        httpClient.interceptors().clear()
        httpClient.addInterceptor(interceptor)
        builder.client(httpClient.build())
        retrofit = builder.build()
    }

    fun getUserService() : UserService {
        if(userService == null) {
            userService = retrofit.create(UserService::class.java)
        }
        return userService as UserService
    }

    fun getTicketService() : TicketService {
        if(ticketService == null) {
            ticketService = retrofit.create(TicketService::class.java)
        }
        return ticketService as TicketService
    }

    fun getNumbersService() : NumbersService {
        if(numbersService == null) {
            numbersService = retrofit.create(NumbersService::class.java)
        }
        return numbersService as NumbersService
    }

    fun invalidateSession() {
        httpClient.interceptors().clear()
        builder.client(httpClient.build())
        retrofit = builder.build()
        userService = null
        ticketService = null
    }
}