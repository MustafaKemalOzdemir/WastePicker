package com.mustafakemal.wastepicker.retrofit

import retrofit2.Call
import retrofit2.http.GET

interface RestInterface {

    @GET("/container")
    fun getContainers(): Call<List<ContainerModel>>

}