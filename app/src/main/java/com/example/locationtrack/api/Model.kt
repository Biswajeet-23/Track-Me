package com.example.locationtrack.api

import com.google.gson.annotations.SerializedName

data class Model(
    @SerializedName("lt")
    val latitude: String,

    @SerializedName("lg")
    val longitude: String,

    @SerializedName("sp")
    val speed: String,

    @SerializedName("ph")
    val phoneNumber: String,

    @SerializedName("dv")
    val deviceId: String
    )
