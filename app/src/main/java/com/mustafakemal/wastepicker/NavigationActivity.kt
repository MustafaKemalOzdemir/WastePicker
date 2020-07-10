package com.mustafakemal.wastepicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mustafakemal.wastepicker.constants.Constants
import com.mustafakemal.wastepicker.retrofit.ContainerModel
import kotlinx.android.synthetic.main.activity_navigation.*

//Google maps not used for just testing
class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var bestRoute: IntArray ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val brazilMain = LatLng(-15.776719, -47.878446)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(brazilMain, 13F))

        Constants.containerData?.let {data ->
            data.forEach {containerModel ->
                mMap.addMarker(MarkerOptions().position(LatLng(containerModel.latitude, containerModel.longitude)).title(containerModel.containerId.toString()))
            }
            btn_draw_path.setOnClickListener {
                OptimizedRoutePlanner(data,0, 4, Constants.NAV_MODE_START_END, object: CalculationFinished{
                    override fun returnRoute(route: IntArray) {
                        bestRoute = route
                        val callUrl = getCallString(route,data)
                        callUrl.httpGet().responseString{ request, response, result ->
                            when(result){
                                is Result.Failure -> {
                                    val ex = result.getException()
                                    Log.v("OptimizedCheck", "error $ex")
                                }
                                is Result.Success -> {
                                    val resultData = result.get()
                                    Log.v("OptimizedCheck", "Sucess $resultData")
                                }
                            }
                        }
                    }
                }).startCalculation()
            }
        }

        // Add a marker in Sydney and move the camera
        //mMap.addMarker(MarkerOptions().position(brazilMain).title("Marker in Sydney"))

    }
    fun getCallString(route: IntArray, data: List<ContainerModel>): String{
        val origin = "origin=${data[0].latitude}, ${data[0].longitude}"
        val destination = "destination=${data[route.size -1].latitude}, ${data[route.size -1].longitude}"
        val mode = "mode=driving"
        val parameters = "$origin&$destination&$mode"
        val output = "json"
        val url = "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=${getString(R.string.google_maps_key)}"
        return url
    }
}
