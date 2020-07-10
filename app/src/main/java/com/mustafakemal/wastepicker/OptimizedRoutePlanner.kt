package com.mustafakemal.wastepicker

import android.util.Log
import com.mustafakemal.wastepicker.constants.Constants
import com.mustafakemal.wastepicker.retrofit.ContainerModel
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

class OptimizedRoutePlanner(private val containers: List<ContainerModel>, private val startPos: Int, private val endPos: Int,private val mode: Int, private val calculationFinished: CalculationFinished) {
    private val populationSize = 300
    private val saPopCount = 20
    private var population = Array(populationSize) {IntArray(containers.size)}
    private var newGeneration =  Array(populationSize){ IntArray(containers.size)}
    private var populationFitness = DoubleArray(populationSize) {0.0}
    private val tempOrder = IntArray(containers.size) {i -> i}
    private val generationLimit = 100
    private var bestDistance = Double.MAX_VALUE
    private var bestRoute = IntArray(containers.size) {-1}
    private val mutationRate = 0.2
    private var generation = 0
    private var saPopulation = Array(saPopCount){ IntArray(containers.size)}
    private var saPopulationMap = IntArray(saPopCount) {-1}
    private var newSAPopulation = Array(saPopCount){ IntArray(containers.size)}

    fun startCalculation(){
        if(mode == Constants.NAV_MODE_START_END){
            Log.v("OptimizedCheck", "Start End Started")
            initializeBetterPopulationBoth()
            doSimulatedAnnealing()
            for (i in 0 until generationLimit){
                generation++
                calculatePopulationFitness()
                createNewGeneration()
            }
            Log.v("OptimizedCheck", bestRoute.contentToString())
            calculationFinished.returnRoute(bestRoute)
            tempOrder[0]
        }
    }


    private fun doSimulatedAnnealing(){
        var index = 0
        while (index<saPopCount){
            val number = ThreadLocalRandom.current().nextInt(0, populationSize)
            if(!saPopulationMap.contains(number)){
                saPopulationMap[index] = number
                index++
            }
        }
        for (i in 0 until saPopCount){
            saPopulation[i] = population[saPopulationMap[i]].clone()
            //oldDistance[i] = calculateRouteDistance(saPopulation[i])
        }
        for(i in 0 until saPopCount){
            newSAPopulation[i] = SimulatedAnnealing(containers, saPopulation[i]).startCalculation().clone()
            population[saPopulationMap[i]] = newSAPopulation[i]
            //newDistance[i] = measureDistance(newSAPopulation[i])
        }
    }

    private fun initializeBetterPopulationBoth(){
        val route = IntArray(containers.size) {i -> i}
        for (k in population.indices){
            val selected = ArrayList<Int>()
            selected.add(startPos)
            selected.add(endPos)
            route[0] = startPos
            route[route.size - 1] = endPos
            var current = startPos
            for (i in 1 until route.size - 1){
                val selectedContainer = pickNextContainer(selected, current)
                current = selectedContainer
                selected.add(selectedContainer)
                route[i] = selectedContainer
            }
            population[k] = route.clone()

        }
        return
    }

    private fun calculatePopulationFitness(){
        var totalDistance = 0.0
        var totalFitness = 0.0
        for((index, value) in population.withIndex()){
            val distance = calculateRouteDistance(value)
            totalDistance += distance
            populationFitness[index] = (1 / totalDistance) + 0.5
            totalFitness += populationFitness[index]
            if(distance < bestDistance){
                bestDistance = distance
                bestRoute = population[index]
            }
        }

        for((index, value) in populationFitness.withIndex()){
            populationFitness[index] = value / totalFitness
        }
        Log.v("OptimizedCheck", "generation: $generation distance: $bestDistance")
    }

    private fun createNewGeneration(){
        for(i in 0 until populationSize){
            val parentA = pickOne()
            val parentB = pickOne()
            val child = crossOver(parentA, parentB)
            val mutatedChild = mutate(child)
            newGeneration[i] = mutatedChild.clone()
        }
        population = newGeneration.clone()
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

    private fun pickOne(): IntArray{
        var index = 0
        var prob = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        while (prob > 0){
            prob -=  populationFitness[index]
            index++
        }
        index--
        return population[index].clone()
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

    private fun calculateRouteDistance(route: IntArray): Double{
        var totalDistance = 0.0
        for(i in 0 until route.size -1){
            for(k in containers[route[i]].distance){
                if(k.nextContainerId == route[i + 1]){
                    totalDistance+= k.distanceShortest
                }
            }
        }
        return totalDistance
    }

    private fun pickNextContainer(selectedContainers: ArrayList<Int>, current: Int): Int{
        var totalDistance = 0.0
        var totalFitness = 0.0
        val fitnessArray = DoubleArray(containers[current].distance.size - selectedContainers.size +1) {0.0}
        val distanceArray = DoubleArray(containers[current].distance.size -selectedContainers.size +1) {-1.0}
        val idArray = IntArray(containers[current].distance.size) {-1}
        for(container in containers){
            if (container.containerId == current){
                var arrayIndex = 0
                for((index, value) in container.distance.withIndex()){
                    if(selectedContainers.contains(value.nextContainerId)){
                        continue
                    }
                    totalDistance += value.distanceShortest
                    distanceArray[arrayIndex] = value.distanceShortest
                    fitnessArray[arrayIndex] = (1 / value.distanceShortest) + 0.5
                    totalFitness+= fitnessArray[arrayIndex]
                    idArray[arrayIndex] = value.nextContainerId
                    arrayIndex++
                }
                for(index in fitnessArray.indices){
                    fitnessArray[index] = fitnessArray[index].div(totalFitness)
                }
                return pickNextContainerFromFitnessArray(fitnessArray, idArray)
            }
        }
        return -1
    }

    private fun pickNextContainerFromFitnessArray(distanceArray: DoubleArray, idArray: IntArray): Int{
        var index = 0
        var prob = ThreadLocalRandom.current().nextDouble(0.0, 1.0)
        while (prob > 0){
            prob -=  distanceArray[index]
            index++
        }
        index--
        return idArray[index]
    }
/*
    private fun shuffleOrderBoth(){
        var index: Int
        var temp: Int
        for(i in tempOrder.size -2 downTo  1){
            index = (1..i).random()
            temp = tempOrder[index]
            tempOrder[index] = tempOrder[i]
            tempOrder[i] = temp
        }
    }



    private fun shuffleOrderSingle(){
        var index: Int
        var temp: Int
        for(i in tempOrder.size -1 downTo  1){
            index = (1..i).random()
            temp = tempOrder[index]
            tempOrder[index] = tempOrder[i]
            tempOrder[i] = temp
        }
    }

    private fun initializePopulationSingle(){
        val tempElement = tempOrder[0]
        tempOrder[0] = startPos
        tempOrder[startPos] = tempElement
        for(i in 0 until populationSize){
            shuffleOrderBoth()
            population[i] = tempOrder.clone()
        }
    }

    private fun initializePopulationBoth(){
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
        for(i in 0 until populationSize){
            shuffleOrderBoth()
            population[i] = tempOrder.clone()
        }
    }

 */
}