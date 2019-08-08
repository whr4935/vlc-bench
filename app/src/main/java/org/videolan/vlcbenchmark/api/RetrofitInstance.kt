package org.videolan.vlcbenchmark.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class RetrofitInstance {

    companion object {
        @JvmStatic
        var retrofit: Retrofit? = null

        @JvmStatic
        fun init(url: String) {
            if (retrofit == null) {
                val client = OkHttpClient.Builder().build()
                retrofit = Retrofit.Builder()
//                        .client(client)
                        .baseUrl(url)
                        .build()
            }
        }
    }
}