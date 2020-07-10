package com.mustafakemal.wastepicker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_VIEWPORT
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mustafakemal.wastepicker.adapters.MainRwAdapter
import com.mustafakemal.wastepicker.constants.Constants
import com.mustafakemal.wastepicker.models.Container
import com.mustafakemal.wastepicker.retrofit.ContainerModel
import com.mustafakemal.wastepicker.retrofit.RestInterface
import com.mustafakemal.wastepicker.retrofit.RetrofitClient
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    private var mapView: MapView? = null
    private val ID_ICON_MARKER = "id_icon_marker"
    private lateinit var containerList: List<Container>
    private lateinit var lineOrderList: Array<Point>
    private var myStyle: Style? = null
    private val LINE_LAYER_ID = "my-line-layer"
    private val LINE_SOURCE_ID = "line-source"
    private lateinit var sync_button : Button
    private lateinit var lastSyncText: TextView

    @SuppressLint("SetTextI18n")
    private fun handleLastStncText(){
        val defaultSharedPreferences = getSharedPreferences(Constants.SHARED_PREF_ID, Context.MODE_PRIVATE)
        val lastSyncTime = defaultSharedPreferences.getLong(Constants.DATA_SYNC_TIME_STAMP, -1L)
        if(lastSyncTime == -1L){
            lastSyncText.text = getString(R.string.not_synced)
        }else{
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = lastSyncTime
            lastSyncText.text = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH)}/${calendar.get(Calendar.YEAR)} ${calendar.get(Calendar.HOUR_OF_DAY)}h ${calendar.get(Calendar.MINUTE)}m ${calendar.get(Calendar.SECOND)}s"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        /** rettofit stuff start**/
        val defaultSharedPreferences = getSharedPreferences(Constants.SHARED_PREF_ID, Context.MODE_PRIVATE)
        val recyclerView: RecyclerView = findViewById(R.id.rw_containerList)
        val mapButton: Button = findViewById(R.id.btn_openMap)

        lastSyncText = findViewById(R.id.tx_last_sync)
        handleLastStncText()
        recyclerView.layoutManager = LinearLayoutManager(this)

        val syncData = defaultSharedPreferences.getString(Constants.DATA_SYNC_STRING, " ")
        if(syncData != " "){
            val data: List<ContainerModel> = Gson().fromJson(syncData, object: TypeToken<List<ContainerModel>>(){}.type)
            recyclerView.adapter = MainRwAdapter(data)
            Constants.containerData = data
        }
        mapButton.setOnClickListener {
            if(defaultSharedPreferences.getString(Constants.DATA_SYNC_STRING, " ") == " "){
                Toast.makeText(applicationContext, "You need to sync data first", Toast.LENGTH_SHORT).show()
            }else{
                val intent = Intent(applicationContext, MapBoxNavigationActivity::class.java)
                startActivity(intent)
            }

        }
        sync_button = findViewById(R.id.btn_sync)
        sync_button.setOnClickListener {
            val restInterface = RetrofitClient.getClient().create(RestInterface::class.java)
            val call: Call<List<ContainerModel>> = restInterface.getContainers()
            call.enqueue(object : Callback<List<ContainerModel>> {
                override fun onFailure(call: Call<List<ContainerModel>>, t: Throwable) {
                    Log.v("RetrofitControl", "message -> ${t.message}")
                    Toast.makeText(applicationContext,"Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }

                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<List<ContainerModel>>, response: Response<List<ContainerModel>>) {
                    response.body()?.let {
                        recyclerView.adapter = MainRwAdapter(it)
                        Constants.containerData = it
                        val editor = defaultSharedPreferences.edit()
                        val calendar = Calendar.getInstance()
                        val lastSyncTime = System.currentTimeMillis()
                        calendar.timeInMillis = lastSyncTime
                        lastSyncText.text = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH)}/${calendar.get(Calendar.YEAR)} ${calendar.get(Calendar.HOUR_OF_DAY)}h ${calendar.get(Calendar.MINUTE)}m ${calendar.get(Calendar.SECOND)}s"
                        editor.putLong(Constants.DATA_SYNC_TIME_STAMP, System.currentTimeMillis())
                        editor.putString(Constants.DATA_SYNC_STRING, Gson().toJson(it))
                        Toast.makeText(applicationContext, "Successfully Synced", Toast.LENGTH_SHORT)
                        editor.commit()
                    }
                    Log.v("RetrofitControl", "Completed")
                }
            })
        }





        /** rettofit stuff end**/
        /*
        initializeContainers()
        initializeLines()
        Log.v("updatedOrder", "started")

        //map operators
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {style ->
                addMarkerToStyle(style)
                myStyle = style
                style.addSource( GeoJsonSource(LINE_SOURCE_ID,
                    FeatureCollection.fromFeatures(List<Feature>(lineOrderList.size){Feature.fromGeometry(
                        LineString.fromLngLats(lineOrderList.toList())
                    )}))
                )
                style.addLayer( LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID).withProperties(
                    PropertyFactory.lineDasharray(arrayOf (0.01f, 2.0f)),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineColor(Color.parseColor("#000000"))
                ))



                mapView?.let { mapViewTrue->
                    val symbolManager = SymbolManager(mapViewTrue, mapboxMap, style)
                    symbolManager.iconAllowOverlap = true
                    symbolManager.iconTranslate = arrayOf(-4f, 5f)
                    symbolManager.iconRotationAlignment = ICON_ROTATION_ALIGNMENT_VIEWPORT

                    for(container : Container in  containerList){
                        symbolManager.create(
                            SymbolOptions()
                                .withLatLng(LatLng(container.latLng.latitude, container.latLng.longitude))
                                .withIconImage(ID_ICON_MARKER)
                                .withIconSize(0.04f)).apply {
                            iconOffset = PointF(0f,9f)
                        }
                    }

                }

            }
        }
        runBlocking {
            Fuel.post("http://www.mapquestapi.com/directions/v2/route?key=0CicTOKa32WGJ0abIf33j0GY3yGb7TF6").jsonBody(getJsonBody("a", "b", "c", "d")).response{
                    response ->
                Log.v("ResposeSyring", response.toString())
            }
        }
        /*
        Handler().postDelayed({
           // val geneticAlgorithm = GeneticAlgorithm(containerList, this)
            //geneticAlgorithm.startCalculation()

        },3000)
        Handler().postDelayed({
           //val simulatedAnnealing = SimulatedAnnealing(containerList, )
            //simulatedAnnealing.startCalculation()

        },3000)
        Handler().postDelayed({
            //val endGame = EndGame(containerList, this)
            //endGame.startCalculation()
        }, 1000)
        */
        Handler().postDelayed({
            val h = HybridV2(containerList, this, 10, 3)
            //val h = HybridV2(containerList, this, 0, 10)
            //h.startCalculation()
        }, 1000)
        /*
        Handler().postDelayed({
            val  h = HybridGASA(containerList, this, 10, 3)
            h.startCalculation()
        },1000)
        */
        Log.v("updatedOrder", "ended")
        */
    }

    private fun addMarkerToStyle(style: Style) {
        BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.marker, null))?.let {
            style.addImage(ID_ICON_MARKER, it, true)
        }
    }

    private fun initializeContainers() {
        /*
        val container0 = Container(0, -15.744001, -47.850570)
        val container1 = Container(1, -15.770963, -47.888502)
        val container2 = Container(2, -15.759711, -47.880712)
        val container3 = Container(3, -15.755850, -47.884122)
        val container4 = Container(4, -15.741696, -47.892770)
        val container5 = Container(5, -15.752528, -47.928705)
        val container6 = Container(6, -15.716746, -47.883326)
        val container7 = Container(7, -15.716918, -47.880843)
        val container8 = Container(8, -15.720775, -47.883350)
        val container9 = Container(9, -15.733281, -47.864906)
        //dummy Coordinates
        val container10 = Container(10, -15.796108, -47.894447)
        val container11 = Container(11, -15.794317, -47.921025)
        containerList = listOf(container0, container1, container2, container3, container4, container5, container6, container7, container8, container9, container10, container11)

         */


        val container0 = Container(0, 38.729295, 35.478767) //okul
        val container1 = Container(1, 38.729295, 35.478767)
        val container2 = Container(2, 38.730650, 35.482745)
        val container3 = Container(3, 38.740576, 35.478990)
        val container4 = Container(4, 38.731253, 35.487205)
        val container5 = Container(5, 38.722602, 35.487256)
        val container6 = Container(6, 38.716797, 35.480981)
        val container7 = Container(7, 38.717069, 35.488666)
        val container8 = Container(8, 38.727801, 35.518344)
        val container9 = Container(9, 38.717354, 35.493312)
        //dummy Coordinates
        val container10 = Container(10, 38.705619, 35.484764) // ev
        val container11 = Container(11, 38.725370, 35.464494)
        val container12 = Container(12, 38.727859, 35.509451)
        //val container13 = Container(13, 38.736487, 35.421008)
        containerList = listOf(
            container0,
            container1,
            container2,
            container3,
            container4,
            container5,
            container6,
            container7,
            container8,
            container9,
            container10,
            container11,
            container12
        )


    }

    private fun getJsonBody(latA: String, lonA: String, latB: String, lonB: String): String {
        return "{\n" +
                "    \"locations\": [\n" +
                "        \"-15.770963,-47.8885022\",\n" +
                "        \"-15.759711, -47.8807123\"\n" +
                "    ],\n" +
                "    \"options\": {\n" +
                "        \"avoids\": [],\n" +
                "        \"avoidTimedConditions\": false,\n" +
                "        \"doReverseGeocode\": true,\n" +
                "        \"shapeFormat\": \"raw\",\n" +
                "        \"generalize\": 0,\n" +
                "        \"routeType\": \"fastest\",\n" +
                "        \"timeType\": 1,\n" +
                "        \"locale\": \"en_US\",\n" +
                "        \"unit\": \"m\",\n" +
                "        \"enhancedNarrative\": false,\n" +
                "        \"drivingStyle\": 2,\n" +
                "        \"highwayEfficiency\": 21.0\n" +
                "    }\n" +
                "}"

    }

    private fun initializeLines() {
        lineOrderList = Array(containerList.size) { i ->
            Point.fromLngLat(
                containerList[i].latLng.longitude,
                containerList[i].latLng.latitude
            )
        }
    }

    fun updateLines(order: IntArray) {
        for (i in order.indices) {
            lineOrderList[i] = Point.fromLngLat(
                containerList[order[i]].latLng.longitude,
                containerList[order[i]].latLng.latitude
            )
        }
    }

    fun redrawLayer(order: IntArray) {
        for (i in order.indices) {
            lineOrderList[i] = Point.fromLngLat(
                containerList[order[i]].latLng.longitude,
                containerList[order[i]].latLng.latitude
            )
        }
        for (i in order.indices) {
            print("${order[i]} + ")
        }
        println("")
        Log.v("mainCheck", "called")
        myStyle?.let {
            Log.v("mainCheck", "triggered")
            it.removeLayer(LINE_LAYER_ID)
            it.removeSource(LINE_SOURCE_ID)

            it.addSource(
                GeoJsonSource(LINE_SOURCE_ID,
                    FeatureCollection.fromFeatures(List<Feature>(lineOrderList.size) {
                        Feature.fromGeometry(
                            LineString.fromLngLats(lineOrderList.toList())
                        )
                    })
                )
            )
            it.addLayer(
                LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID).withProperties(
                    PropertyFactory.lineDasharray(arrayOf(0.01f, 2.0f)),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineColor(Color.parseColor("#000000"))
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}
