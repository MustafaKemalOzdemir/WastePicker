package com.mustafakemal.wastepicker.models

import com.mapbox.mapboxsdk.geometry.LatLng

class Container (id: Int, latitude : Double, longitude : Double){

    lateinit var latLng: LatLng
    var cityId: Int = id
    init {
        val temp = LatLng()
        temp.latitude = latitude
        temp.longitude = longitude
        latLng = temp
    }

}