package com.mustafakemal.wastepicker

import com.mustafakemal.wastepicker.retrofit.ContainerModel
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*

class SimulatedAnnealing(private val containers: List<ContainerModel>, initialOrder: IntArray) {
    private val order = initialOrder.clone()
    private var bestDistance = Double.MAX_VALUE
    private var currentDistance = 0.0
    private var oldDistance = 0.0
    private var bestOrder = initialOrder.clone()

    fun startCalculation(): IntArray{
        val tempSequence = sequenceOf(
            generateSequence (80.0){(it - 0.01).takeIf { it2 -> it2 >= 50 }},
            generateSequence (50.0){(it + 0.01).takeIf { it2 -> it2 <= 120}},
            generateSequence (120.0){(it - 0.01).takeIf { it2 -> it2 >= 60}}
        ).flatMap { it }.toList().toTypedArray().toDoubleArray().let {
            TempSchedule(120, it)
        }
        oldDistance = measureDistance(order)
        bestDistance = oldDistance
        var count = 0
        while (tempSequence.next()){
            val pointA = ThreadLocalRandom.current().nextInt(1, order.size -1)
            val pointB = ThreadLocalRandom.current().nextInt(1, order.size -1 )
            if(pointA == pointB){
                tempSequence.back()
                continue
            }
            doSwap(pointA, pointB)
            currentDistance = measureDistance(order)
            if(currentDistance < oldDistance && (currentDistance < bestDistance)){
                val a = currentDistance
                bestDistance = currentDistance
                bestOrder = order.clone()
                oldDistance = currentDistance
                //accepted
            }else{
                val probability = exp((-(currentDistance * 1000 - bestDistance * 1000)) / tempSequence.heat)
                if(weightedCoinFlip(probability)){
                    oldDistance = currentDistance
                }else{
                    //rejected
                    doSwap(pointA, pointB)
                }
            }
            count++
        }
        /*
        val sb = StringBuilder()
        for(i in bestOrder.indices){
            sb.append(bestOrder[i])
            sb.append(" ")
        }
        Log.v("SimulatedAnnealing", sb.toString())
        Log.v("SimulatedAnnealing", "best distance: $bestDistance")
        Log.v("SimulatedAnnealing", "count: $count")
        */
        return bestOrder
    }

    private fun measureDistance(route: IntArray): Double {
        var totalDistance = 0.0
        for (i in 0 until route.size - 1) {
            for (k in containers[route[i]].distance) {
                if (k.nextContainerId == route[i + 1]) {
                    totalDistance += k.distanceShortest
                }
            }
        }
        return totalDistance
    }
        /*
        private fun measureDistance(newOrder: IntArray): Double{
            var distance = 0.0
            for (i in 0 until newOrder.size - 1){
                distance += calculateDistance(containers[order[i]],containers[order[i + 1]])
            }
            return distance
        }

         */
    /*
    private fun calculateDistance(containerA: Container, containerB: Container): Double{
        val latA = Math.toRadians(containerA.latLng.latitude)
        val lngA = Math.toRadians(containerA.latLng.longitude)
        val latB = Math.toRadians(containerB.latLng.latitude)
        val lngB = Math.toRadians(containerB.latLng.longitude)
        val radius = 6371
        val dLat = latB - latA
        val dLng = lngB - lngA
        val a = sin(dLat/2).pow(2) + sin(dLng/2).pow(2) * cos(latA) * cos(latB)
        val c = 2 * asin(sqrt(a))
        return radius * c
        /*
        val xSquare = abs(containerA.latLng.latitude - containerB.latLng.latitude).pow(2)
        val ySquare = abs(containerA.latLng.longitude - containerB.latLng.longitude).pow(2)
        return xSquare + ySquare
        */
    }

     */

    private fun doSwap (pointA: Int, pointB: Int){
        val temp = order[pointA]
        order[pointA] = order[pointB]
        order[pointB] = temp
    }

    private fun weightedCoinFlip(probability: Double ): Boolean{
        return ThreadLocalRandom.current().nextDouble(0.0,  1.0) < probability
    }

}

class TempSchedule(val maxTemp: Int, val temperatureSequence: DoubleArray){
    var index = -1
    val heat get() = temperatureSequence[index]
    fun back(){
        index--
    }
    fun next(): Boolean{
        return if(index == temperatureSequence.size -1){
            false
        }else{
            index++
            true
        }
    }
}