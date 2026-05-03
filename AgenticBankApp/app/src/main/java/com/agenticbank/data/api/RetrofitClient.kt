package com.agenticbank.data.api

import android.content.Context
import com.agenticbank.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ Change this to your backend IP address when running on a real device
    // Use 10.0.2.2 for Android Emulator pointing to localhost
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val token = SessionManager(context).getToken()
                    val request = if (!token.isNullOrEmpty()) {
                        chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .addHeader("Content-Type", "application/json")
                            .build()
                    } else {
                        chain.request()
                    }
                    chain.proceed(request)
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
