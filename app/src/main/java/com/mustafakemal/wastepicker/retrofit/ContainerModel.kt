package com.mustafakemal.wastepicker.retrofit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ContainerModel {
    @SerializedName("containerId")
    @Expose
    var containerId: Int = -1

    @SerializedName("latitude")
    @Expose
    var latitude: Double = -1.0

    @SerializedName("longitude")
    @Expose
    var longitude: Double = -1.0

    @SerializedName("fullness")
    @Expose
    var fullness: Int = -1

    @SerializedName("distance")
    @Expose
    lateinit var distance: List<DistanceModel>

}