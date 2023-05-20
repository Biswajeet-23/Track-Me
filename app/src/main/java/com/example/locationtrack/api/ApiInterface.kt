package com.example.locationtrack.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {
    @Multipart
    @POST("apiv1/PostLocation")
    suspend fun postLocation(
        @Part("lt") lt: String,
        @Part("lg") lg: String,
        @Part("sp") sp: String,
        @Part("ph") ph: String,
        @Part("dv") dv: String

    ): Response<ResponseBody>
}