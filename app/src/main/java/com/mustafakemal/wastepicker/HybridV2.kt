package com.mustafakemal.wastepicker


import android.os.Handler
import android.util.Log
import com.mustafakemal.wastepicker.models.Container
import java.lang.StringBuilder
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*
//Not Used
/*

class HybridV2(private val containers: List<Container>, private val activity: MainActivity, val startPos: Int, val endPos: Int) {
    private val populationCount = 100
    private val saPopCount = 20
    private var population =  Array(populationCount){ IntArray(containers.size)}
    private var newGeneration =  Array(populationCount){ IntArray(containers.size)}
    private var bestDistance = Double.MAX_VALUE
    private var bestOrder = IntArray(containers.size)
    private val fitness = DoubleArray(populationCount){0.0}
    private val mutationRate = 0.1
    private val tempOrder = IntArray(containers.size) {i -> i}
    private var saPopulation = Array(saPopCount){ IntArray(containers.size)}
    private var saPopulationMap = IntArray(saPopCount) {-1}
    private var newSAPopulation = Array(saPopCount){ IntArray(containers.size)}
    private val generationLimit = 1600
    private var totalFitness = 0.0


    private val oldDistance = DoubleArray(saPopCount)
        private val newDistance = DoubleArray(saPopCount)


        fun startCalculation(){
            var generation = 0
            initializePopulation()
            doSimulatedAnnealing()
            while (generation < generationLimit){
                generation++
                if(generation % 500 == 0){
                    doSimulatedAnnealing()
                }
                Log.v("endGame", "generation: $generation")
                doGeneticAlgorithm()
            }
            Log.v("endGame", "bestDistance: $bestDistance")
            val filteredBySim = SimulatedAnnealing(containers, bestOrder).startCalculation()
            val filteredDistance = measureDistance(filteredBySim)
            val sb = StringBuilder()
            bestOrder.forEach {
                sb.append("$it ")
            }
        Log.v("endGame", "bestOrder    : ${sb.toString()}")
        val sb2 = StringBuilder()
        filteredBySim.forEach {
            sb2.append("$it ")
        }
        Log.v("endGame", "filteredDistance: $filteredDistance")
        Log.v("endGame", "filteredOrder: ${sb2.toString()}")
        activity.redrawLayer(bestOrder)
        Handler().postDelayed({
            activity.redrawLayer(filteredBySim)
            Log.v("endGame", "updated")
        },20000)
    }

    private fun doGeneticAlgorithm(){
        calculateFitness()
        normalizeData()
        createNewGeneration()
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

    private fun createNewGeneration(){
        for(i in 0 until populationCount){
            val parentA = pickOne()
            val parentB = pickOne()
            val child = crossOver(parentA, parentB)
            val mutatedChild = mutate(child)
            newGeneration[i] = mutatedChild.clone()
        }
        population = newGeneration.clone()

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

    private fun normalizeData(){
        for (i in 0 until  populationCount){
            fitness[i] = fitness[i].div(totalFitness)
        }
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

    private fun doSimulatedAnnealing(){
        var index = 0
        while (index<saPopCount){
            val number = ThreadLocalRandom.current().nextInt(0, populationCount)
            if(!saPopulationMap.contains(number)){
                saPopulationMap[index] = number
                index++
            }
        }

        for (i in 0 until saPopCount){
            saPopulation[i] = population[saPopulationMap[i]].clone()
            oldDistance[i] = measureDistance(saPopulation[i])
        }

        for(i in 0 until saPopCount){
            newSAPopulation[i] = SimulatedAnnealing(containers, saPopulation[i]).startCalculation().clone()
            newDistance[i] = measureDistance(newSAPopulation[i])
        }
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

    private fun measureDistance(order: IntArray): Double{
        var tempDistance = 0.0
        for(i in 0 until order.size - 1){
            tempDistance += calculateDistance(containers[order[i]],containers[order[i+1]])
        }
        return tempDistance
    }

    private fun calculateDistance(containerA: Container, containerB: Container): Double{

        val latA = Math.toRadians(containerA.latLng.latitude)
        val lngA = Math.toRadians(containerA.latLng.longitude)
        val latB = Math.toRadians(containerB.latLng.latitude)
        val lngB = Math.toRadians(containerB.latLng.longitude)
        val radius = 6371 // radius of world
        val dLat = latB - latA
        val dLng = lngB - lngA
        val a = sin(dLat/2).pow(2) + sin(dLng/2).pow(2) * cos(latA) * cos(latB)
        val c = 2 * asin(sqrt(a))
        val result = radius * c
        return  result

        /*
        val xSquare = abs(containerA.latLng.latitude - containerB.latLng.latitude).pow(2)
        val ySquare = abs(containerA.latLng.longitude - containerB.latLng.longitude).pow(2)
        return xSquare + ySquare

         */
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

}

 */