package com.mustafakemal.wastepicker.retrofit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DistanceModel {
    @SerializedName("nextContainerId")
    @Expose
    var nextContainerId: Int = -1

    @SerializedName("nextLatitude")
    @Expose
    var nextLatitude: Double = -1.0

    @SerializedName("nextLongitude")
    @Expose
    var nextLongitude: Double = -1.0

    @SerializedName("distanceShortest")
    @Expose
    var distanceShortest: Double = -1.0

    @SerializedName("distanceFormat")
    @Expose
    var distanceFormat: String = "empty"
}