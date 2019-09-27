package org.videolan.vlcbenchmark.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap

interface ApiInterface {
    @POST("benchmarks")
    fun uploadBenchmark(@Body body: RequestBody): Call<Void>

    @POST("benchmarks-screenshots")
    fun uploadBenchmarkWithScreenshots(@Body body: RequestBody): Call<Void>
}