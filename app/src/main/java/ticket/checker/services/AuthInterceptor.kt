package ticket.checker.services

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by Dani on 25.01.2018.
 */
class AuthInterceptor(private val token : String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val authenticatedRequest = request.newBuilder()
                .addHeader("Authorization", String.format("Bearer %s", token))
                .addHeader("Content-Type","application/json")
                .addHeader("Accept","application/json")
                .build()

        return chain.proceed(authenticatedRequest)
    }

}