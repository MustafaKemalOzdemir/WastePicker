package com.mustafakemal.wastepicker
import android.util.Log
import com.mustafakemal.wastepicker.models.Container
import java.lang.StringBuilder
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.pow

//Not Used
class GeneticAlgorithm(private val containers: List<Container>, private val activity: MainActivity) {
    private val populationCount = 400
    private var population =  Array(populationCount){ IntArray(containers.size)}
    private var newGeneration =  Array(populationCount){ IntArray(containers.size)}
    private val fitness = DoubleArray(populationCount){0.0}
    private var totalFitness = 0.0
    private var generationTarget = 5000
    private var generation = 0
    private var order = IntArray(containers.size){i -> i}
    private var bestDistance = Double.MAX_VALUE
    private var bestOrder = IntArray(containers.size)
    private val mutationRate = 0.1



    fun startCalculation(){
        initializePopulation()

        while(true){
            generation++
            calculateFitness()
            normalizeData()
            createNewGeneration()
            //Log.v("updatedOrder", "updated")
            if(generation> generationTarget){
                break
            }
        }
        Log.v("updatedOrder", "finished")
        Log.v("updatedOrder", "Generation $generation")
        Log.v("bestRoute", "bestRoute: ${printBestRoute(bestOrder)}")
        Log.v("bestRoute", "bestDistance: $bestDistance")
        generation++
    }

    private fun crossOver(orderA: IntArray, orderB: IntArray): IntArray{
        var index = 0
        val newChild = IntArray(containers.size){0}
        val startIndex = ThreadLocalRandom.current().nextInt(containers.size)
        val endIndex = ThreadLocalRandom.current().nextInt(containers.size - startIndex)

        for(i in startIndex until startIndex + endIndex){
            newChild[index] = orderA[i]
            index++
        }

        for(k in containers.indices){
            if(!isIncludes(newChild, orderB[k])){
                newChild[index] = orderB[k]
                index++
            }
        }
        return newChild
    }

    private fun isIncludes(child: IntArray, element: Int): Boolean{
        for(i in child.indices){
            if(child[i] == element){
                return true
            }
        }
        return false

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
            totalFitness += fitness[i]
            if(tempDistance < bestDistance){
                bestDistance = tempDistance
                val temp = population[i].clone()
                bestOrder = population[i].clone()
                activity.redrawLayer(bestOrder)
                generationTarget+=1000
                Log.v("updateOrder", "best distance updated new generation target is $generationTarget")
            }
        }
    }

    private fun normalizeData(){
        for (i in 0 until  populationCount){
            fitness[i] = fitness[i].div(totalFitness)
        }
    }

    private fun mutate(element: IntArray): IntArray{
        for(i in containers.indices){
            val range = ThreadLocalRandom.current().nextDouble(0.0,1.0)
            if(range < mutationRate){
                val indexA = ThreadLocalRandom.current().nextInt(element.size)
                val indexB = ThreadLocalRandom.current().nextInt(element.size)
                val temp = element[indexA]
                element[indexA] = element[indexB]
                element[indexB] = temp
            }
        }
        return element
    }

    private fun calculateDistance(containerA: Container, containerB: Container): Double{
        val xSquare = abs(containerA.latLng.latitude - containerB.latLng.latitude).pow(2)
        val ySquare = abs(containerA.latLng.longitude - containerB.latLng.longitude).pow(2)
        return xSquare + ySquare
    }

    private fun createNewGeneration(){
        var element: IntArray
        for(i in 0 until populationCount){
            val orderA = pickOne()
            val orderB = pickOne()
            element = crossOver(orderA, orderB)
            element = mutate(element)
            newGeneration[i] = element

        }
        population = newGeneration.clone()

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
        for(i in 0 until populationCount){
            shuffleOrder()
            population[i] = order.clone()
        }
    }

    private fun shuffleOrder(){
        var index: Int
        var temp: Int
        for(i in order.size -1 downTo  0){
            index = (0..i).random()
            temp = order[index]
            order[index] = order[i]
            order[i] = temp
        }
    }

    private fun printBestRoute(orderA: IntArray): String{
        val stringBuilder = StringBuilder()
        for (i in orderA.indices){
            stringBuilder.append(orderA[i])
            stringBuilder.append(" ")
        }
        return stringBuilder.toString()
    }

}