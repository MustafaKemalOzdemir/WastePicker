package com.mustafakemal.wastepicker.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {
    companion object{
        private lateinit var retrofit: Retrofit
        private const val baseUrl = "http://192.168.1.105:1502/"

        fun getClient(): Retrofit{
            this.retrofit = Retrofit.Builder().apply {
                baseUrl(baseUrl)
                addConverterFactory(GsonConverterFactory.create())
                client(OkHttpClient())
            }.build()
            return  retrofit
        }
    }
}