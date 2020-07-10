package com.mustafakemal.wastepicker

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mustafakemal.wastepicker.constants.Constants
import com.mustafakemal.wastepicker.retrofit.ContainerModel
import kotlinx.android.synthetic.main.activity_map_box_navigation.*


class MapBoxNavigationActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val ICON_ID = "ICON_ID"
    private val SOURCE_ID = "SOURCE_ID"
    private val LAYER_ID = "LAYER_ID"
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"
    private val LINE_LAYER_ID = "line-layer-id"
    private val LINE_SOURCE_ID = "line-sourcen-id"
    private var lineOrderList =  ArrayList<Point>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map_box_navigation)
        mapView = findViewById<MapView>(R.id.mapboxMapView)
        mapView.onCreate(savedInstanceState)

        btn_start_navigation_mapBox.setOnClickListener {
            startActivity(Intent(this, NavigationActivity::class.java))
        }

        mapView.getMapAsync { mapboxMap ->


            val symbolLayerIconFeatureList = ArrayList<Feature>()
            Constants.containerData?.let { data ->
                data.forEach { containerModel ->
                    lineOrderList.add(
                        Point.fromLngLat(containerModel.longitude, containerModel.latitude)
                    )
                    symbolLayerIconFeatureList.add(
                        Feature.fromGeometry(
                            Point.fromLngLat(containerModel.longitude, containerModel.latitude)
                        )
                    )

                }

            }
            mapboxMap.setStyle(
                Style.Builder()
                    .fromUri(Style.MAPBOX_STREETS).withImage(ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default))
                    .withSource(
                        GeoJsonSource(
                            SOURCE_ID,
                            FeatureCollection.fromFeatures(symbolLayerIconFeatureList)
                        )
                    )
                    .withLayer(
                        SymbolLayer(LAYER_ID, SOURCE_ID)
                            .withProperties(iconImage(ICON_ID), iconAllowOverlap(true), iconIgnorePlacement(true))
                            .withProperties(PropertyFactory.textField(Expression.get("FEATURE-PROPERTY-KEY")))
                    )
            ) { style ->

                style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))

                val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
                routeLayer.setProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineWidth(5f),
                    lineColor(Color.parseColor("#009688"))
                )
                style.addLayer(routeLayer)

                btn_draw_path_mapBox.setOnClickListener {
                    Constants.containerData?.let { data ->
                        OptimizedRoutePlanner(data, edt_origin.text.toString().toInt(), edt_destination.text.toString().toInt(), Constants.NAV_MODE_START_END, object : CalculationFinished {
                            override fun returnRoute(route: IntArray) {
                                redrawLayer(route, data, style)
                            }
                            /*
                            override fun returnRoute(route: IntArray) {
                                val builder = NavigationRoute.builder(this@MapBoxNavigationActivity).apply {
                                    accessToken(getString(R.string.mapbox_access_token))
                                    origin(Point.fromLngLat(data.first().longitude, data.first().latitude))
                                    destination(Point.fromLngLat(data[10].longitude, data[10].latitude))
                                    /*
                                    for (i in 1 until route.size -1){
                                        addWaypoint(Point.fromLngLat(data[route[i]].longitude, data[route[i]].latitude))
                                    }
                                    */
                                }
                                builder.addWaypoint(Point.fromLngLat(data[route[1]].longitude, data[route[1]].latitude))
                                builder.addWaypoint(Point.fromLngLat(data[route[2]].longitude, data[route[2]].latitude))
                                builder.addWaypoint(Point.fromLngLat(data[route[3]].longitude, data[route[3]].latitude))
                                //builder.addWaypoint(Point.fromLngLat(data[route[2]].longitude, data[route[2]].latitude))
                                //builder.addWaypoint(Point.fromLngLat(data[route[2]].longitude, data[route[2]].latitude))

                                /*
                                for (i in 1 until route.size - 8) {
                                    builder.addWaypoint(Point.fromLngLat(data[route[i]].longitude, data[route[i]].latitude))
                                }

                                 */
                                builder.build().getRoute(object : Callback<DirectionsResponse?> {
                                    override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                                        Log.v("OptimisedRoute", "failed ${t.message}")
                                    }

                                    @SuppressLint("LogNotTimber")
                                    override fun onResponse(call: Call<DirectionsResponse?>, response: Response<DirectionsResponse?>) {
                                        response.body()?.let { directionsResponse ->
                                            if (directionsResponse.routes().size != 0) {
                                                val directionRoute = directionsResponse.routes()[0]
                                                mapboxMap.getStyle {
                                                    val source: GeoJsonSource? = it.getSourceAs(ROUTE_SOURCE_ID)
                                                    if (source != null) {
                                                        directionRoute.geometry()?.let { geometry ->
                                                            source.setGeoJson(LineString.fromPolyline(geometry, PRECISION_6))
                                                            Log.v("OptimisedRoute", "route Has drawn")
                                                        }

                                                    }
                                                }
                                            } else {
                                                Log.v("OptimisedRoute", "no Route")
                                            }
                                        }
                                        if(response.body() == null){
                                            Log.v("OptimisedRoute", "it is null")
                                        }
                                        Log.v("OptimisedRoute", "finished")
                                    }

                                })

                            }

                            */

                        }).startCalculation()
                    }
                }


            }

        }
    }

    @SuppressLint("LogNotTimber")
    fun redrawLayer(order: IntArray, list: List<ContainerModel>, style: Style) {
        for (i in order.indices) {
            lineOrderList[i] = Point.fromLngLat(
                list[order[i]].longitude,
                list[order[i]].latitude
            )
        }
        for (i in order.indices) {
            print("${order[i]} + ")
        }
        println("")
        Log.v("mainCheck", "called")
        style.let {
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
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
