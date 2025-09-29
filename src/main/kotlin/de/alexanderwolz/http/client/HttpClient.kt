package de.alexanderwolz.http.client

import okhttp3.OkHttpClient

class HttpClient {

    val okHttpClient = createOkHttpClient()

    fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        return builder.build()
    }

}