package com.mustafakemal.wastepicker

import android.util.Log
import com.mustafakemal.wastepicker.models.Container
import java.lang.StringBuilder
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*
//Not Used
class HybridGASA(private val containers: List<Container>, private val activity: MainActivity, val startPos: Int, val endPos: Int) {
    private val populationCount = 100
    private var population =  Array(populationCount){ IntArray(containers.size)}
    private var newGeneration =  Array(populationCount){ IntArray(containers.size)}
    private var bestDistance = Double.MAX_VALUE
    private var bestOrder = IntArray(containers.size)
    private val fitness = DoubleArray(populationCount){0.0}
    private val mutationRate = 0.1
    private val tempOrder = IntArray(containers.size) {i -> i}
    private val generationLimit = 8000
    private var totalFitness = 0.0
    private var generation = 1
    //add mutation

    fun startCalculation(){
        Log.v("HybridGASA", "Triggered")
        initializePopulation()
        Log.v("HybridGASA", "Initialized")
        val tempSequence = sequenceOf(
            generateSequence (80.0){(it - 0.05).takeIf { it2 -> it2 >= 50 }},
            generateSequence (50.0){(it + 0.05).takeIf { it2 -> it2 <= 120}},
            generateSequence (120.0){(it - 0.05).takeIf { it2 -> it2 >= 60}},
            generateSequence (80.0){(it - 0.05).takeIf { it2 -> it2 >= 50 }},
            generateSequence (50.0){(it + 0.05).takeIf { it2 -> it2 <= 120}},
            generateSequence (120.0){(it - 0.05).takeIf { it2 -> it2 >= 60}}
        ).flatMap { it }.toList().toTypedArray().toDoubleArray().let {
            TempSchedule(120, it)
        }
        Log.v("HybridGASA", "Started")

        while (tempSequence.next()){
            Log.v("HybridGASA", "Generation: $generation")
            calculateFitness()
            normalizeData()
            createNewGeneration(tempSequence.heat)
            generation++
        }

        Log.v("hybridGASA", "bestDistance    : $bestDistance")
        val sb = StringBuilder()
        bestOrder.forEach {
            sb.append("$it ")
        }
        Log.v("hybridGASA", "bestOrder    : ${sb.toString()}")
        activity.redrawLayer(bestOrder)

    }

    private fun crossOver(parentA: IntArray, parentB: IntArray): IntArray{
        val child = IntArray(parentA.size){-1}
        val pointA = ThreadLocalRandom.current().nextInt(0, parentA.size / 2)
        val pointB = ThreadLocalRandom.current().nextInt(0, parentA.size / 2)
        for(i in pointA until  (parentA.size - pointB)){
            child[i] = parentA[i]
        }
        for(i in child.indices){
            if(child[i] == -1){
                for(k in parentB){
                    if(!child.contains(k)){
                        child[i] = k
                        break
                    }
                }
            }
        }
        return child
    }

    private fun calculateFitness(){
        totalFitness = 0.0
        for(i in population.indices){
            var tempDistance = 0.0
            for(k in 0 until containers.size -1){
                tempDistance += calculateDistance(containers[population[i][k]] , containers[population[i][k+1]])
            }
            tempDistance.pow(3)
            fitness[i] = 1/ (tempDistance + 1)
            totalFitness  += fitness[i]

            if(tempDistance < bestDistance){
                bestDistance = tempDistance
                bestOrder = population[i].clone()
            }
        }
    }

    private fun normalizeData(){
        for (i in 0 until  populationCount){
            fitness[i] = fitness[i].div(totalFitness)
        }
    }


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

    private fun mutate(child: IntArray): IntArray{
        if(ThreadLocalRandom.current().nextDouble(0.0,1.0)<mutationRate){
            val pointA = ThreadLocalRandom.current().nextInt(1, child.size - 1)
            val pointB = ThreadLocalRandom.current().nextInt(1, child.size - 1)
            val temp = child[pointA]
            child[pointA] = child[pointB]
            child[pointB] = temp
        }
        return child

    }

    private fun createNewGeneration(heat: Double){
        for(i in 0 until populationCount){
            val parentA = pickOne()
            val parentB = pickOne()
            val child = crossOver(parentA, parentB)
            val distanceA = measureDistance(parentA)
            val distanceB = measureDistance(parentB)
            val childDistance = measureDistance(child)
            if(childDistance > (distanceA + distanceB) / 2){
                val probability = exp((-(childDistance * 1000 - (distanceA + distanceB)/2 * 1000)) / heat)
                if(weightedCoinFlip(probability)){
                    val mutatedChild = mutate(child)
                    newGeneration[i] = mutatedChild.clone()
                }else{
                    if(distanceA > distanceB){
                        val mutatedChild = mutate(parentB)
                        newGeneration[i] = mutatedChild.clone()
                    }else{
                        val mutatedChild = mutate(parentA)
                        newGeneration[i] = mutatedChild.clone()
                    }
                }
            }else{
                val mutatedChild = mutate(child)
                newGeneration[i] = mutatedChild.clone()
            }

        }
        population = newGeneration.clone()

    }

    private fun weightedCoinFlip(probability: Double ): Boolean{
        return ThreadLocalRandom.current().nextDouble(0.0,  1.0) < probability
    }

    private fun measureDistance(order: IntArray): Double{
        var tempDistance = 0.0
        for(i in 0 until order.size - 1){
            tempDistance += calculateDistance(containers[order[i]],containers[order[i+1]])
        }
        return tempDistance
    }

    private fun shuffleOrder(){
        var index: Int
        var temp: Int
        for(i in tempOrder.size -2 downTo  1){
            index = (1..i).random()
            temp = tempOrder[index]
            tempOrder[index] = tempOrder[i]
            tempOrder[i] = temp
        }
    }

    private fun pickOne(): IntArray{
        var index = 0
        var prob = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        while (prob > 0){
            prob -=  fitness[index]
            index++
        }
        index--
        return population[index].clone()
    }

    private fun initializePopulation(){
        tempOrder[0] = startPos
        tempOrder[containers.size -1] = endPos
        var index = 1
        for(i in tempOrder.indices){
            if(i == startPos || i == endPos){
                continue
            }
            tempOrder[index] = i
            index++
        }
        for(i in 0 until populationCount){
            shuffleOrder()
            population[i] = tempOrder.clone()
        }
    }
}