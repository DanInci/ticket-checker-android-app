package ticket.checker.services

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ticket.checker.AppTicketChecker
import ticket.checker.beans.ErrorResponse

/**
 * Created by Dani on 24.01.2018.
 */
object ServiceManager {
    private const val GSON_SERIALIZER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    private var retrofit: Retrofit? = null

    private var userService: UserService? = null
    private var ticketService: TicketService? = null
    private var organizationService: OrganizationService? = null
    private var statisticsService: StatisticsService? = null

    val errorConverter: Converter<ResponseBody, ErrorResponse>? by lazy {
        retrofit?.responseBodyConverter(ErrorResponse::class.java, emptyArray())
    }

    fun createSession(email: String, password: String) {

    }

    fun createSession(token: String) {
        val interceptor = AuthInterceptor(token)
        val httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val baseUrl = if(AppTicketChecker.port != "") "http://${AppTicketChecker.address}:${AppTicketChecker.port}" else "http://${AppTicketChecker.address}"
        val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setDateFormat(GSON_SERIALIZER_DATE_FORMAT).create()))
                .client(httpClient)
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

    fun getOrganizationService(): OrganizationService {
        if (organizationService == null) {
            organizationService = getRetrofitClient().create(OrganizationService::class.java)
        }
        return organizationService as OrganizationService
    }

    fun getStatisticsService(): StatisticsService {
        if (statisticsService == null) {
            statisticsService = getRetrofitClient().create(StatisticsService::class.java)
        }
        return statisticsService as StatisticsService
    }

    fun invalidateSession() {
        retrofit = null
        userService = null
        ticketService = null
        organizationService = null
        statisticsService = null
    }

    private fun getRetrofitClient() : Retrofit {
        if(retrofit == null) {
            AppTicketChecker.loadSession()
        }
        return retrofit as Retrofit
    }
}