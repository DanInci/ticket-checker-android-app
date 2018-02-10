package ticket.checker.services

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by Dani on 25.01.2018.
 */
class AuthInterceptor(user : String, password : String) : Interceptor {
    private val credentials : String = Credentials.basic(user, password)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = request.newBuilder()
                .addHeader("Authorization", credentials)
                .addHeader("Content-Type","application/json")
                .addHeader("Accept","application/json")
                .build()
        return chain.proceed(authenticatedRequest)
    }
}