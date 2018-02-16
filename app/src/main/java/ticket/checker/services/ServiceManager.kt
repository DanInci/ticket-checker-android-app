package ticket.checker.services

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ticket.checker.AppTicketChecker
import ticket.checker.beans.ErrorResponse
import ticket.checker.extras.Util.hashString

/**
 * Created by Dani on 24.01.2018.
 */
object ServiceManager {
    private const val API_BASE_URL = "http://89.42.135.219:8080/"
    private var httpClient = OkHttpClient.Builder()
    private var builder = Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
    private var retrofit: Retrofit? = null

    private var userService: UserService? = null
    private var ticketService: TicketService? = null
    private var numbersService: NumbersService? = null

    val errorConverter: Converter<ResponseBody, ErrorResponse>? by lazy {
        retrofit?.responseBodyConverter<ErrorResponse>(ErrorResponse::class.java, emptyArray())
    }

    fun createSession(username: String, password: String, hashRequired: Boolean) {
        var encryptedPassword = password
        if (hashRequired) {
            encryptedPassword = hashString("SHA-256", password)
        }
        val interceptor = AuthInterceptor(username, encryptedPassword)
        httpClient.interceptors().clear()
        httpClient.addInterceptor(interceptor)
        builder.client(httpClient.build())
        retrofit = builder.build()
    }

    fun getUserService(): UserService {
        if (userService == null) {
            userService = getRetrofitClient().create(UserService::class.java)
        }
        return userService as UserService
    }

    fun getTicketService(): TicketService {
        if (ticketService == null) {
            ticketService = getRetrofitClient().create(TicketService::class.java)
        }
        return ticketService as TicketService
    }

    fun getNumbersService(): NumbersService {
        if (numbersService == null) {
            numbersService = getRetrofitClient().create(NumbersService::class.java)
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

    private fun getRetrofitClient() : Retrofit {
        if(retrofit == null) {
            AppTicketChecker.loadSession()
        }
        return retrofit as Retrofit
    }
}