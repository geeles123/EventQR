package com.thedavelopers.eventqr.core.api

import android.content.Context
import com.google.gson.GsonBuilder
import com.thedavelopers.eventqr.core.session.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant

object ApiClient {
    @Volatile
    private var apiService: ApiService? = null

    fun getService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(context.applicationContext).also { apiService = it }
        }
    }

    private fun buildService(context: Context): ApiService {
        val sessionManager = SessionManager(context)
        val gson = GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter)
            .setLenient()
            .create()

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
