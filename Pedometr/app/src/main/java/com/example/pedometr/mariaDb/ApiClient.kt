package com.example.pedometr.mariaDb

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .sslSocketFactory(getUnsafeOkHttpClient(), getTrustManager())
        .hostnameVerifier { _, _ -> true } // Игнорирование проверки хоста (для тестов)
        .build()

    private fun getTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    }

    private fun getUnsafeOkHttpClient(): javax.net.ssl.SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(getTrustManager())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://node-production-907e.up.railway.app/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val activityApi: ActivityApi = retrofit.create(ActivityApi::class.java)
}