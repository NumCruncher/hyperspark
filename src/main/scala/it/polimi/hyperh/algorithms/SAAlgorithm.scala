package it.polimi.hyperh.algorithms

import it.polimi.hyperh.problem.Problem
import it.polimi.hyperh.solution.EvaluatedSolution
import scala.util.Random
import util.Timeout
import it.polimi.hyperh.search.NeighbourhoodSearch

/**
 * @author Nemanja
 */
object SAAlgorithm {
  def apply(p: Problem, temperatureUB: Double, temperatureLB: Double, coolingRate: Double): EvaluatedSolution = {
    evaluate(p, temperatureUB, temperatureLB, coolingRate)
  }
  //temperatureUB: 1.0, temperatureLB: 0.00001, coolingRate: 0.9
  def evaluate(p: Problem, temperatureUB: Double, temperatureLB: Double, coolingRate: Double): EvaluatedSolution = {
    val initEndTimesMatrix = p.jobsInitialTimes()
    def cost(solution: List[Int]) = p.evaluatePartialSolution(solution.toArray, p.jobTimesMatrix, initEndTimesMatrix)
    def neighbour(sol: List[Int]): List[Int] = NeighbourhoodSearch.SHIFT(sol)//forward or backward shift at random
    def acceptanceProbability(delta: Int, temperature: Double): Double = {
      scala.math.pow(2.71828,(-delta/temperature))
    }
    
    var temperature = temperatureUB
    //generate random solution
    var oldSolution = Random.shuffle(p.jobs.toList)
    //calculate its cost
    var evOldSolution = cost(oldSolution)
    
    val timeLimit = p.numOfMachines * (p.numOfJobs / 2.0) * 60 //termination is n*(m/2)*60 milliseconds
    val expireTimeMillis = Timeout.setTimeout(timeLimit)
    
    while ((temperature > temperatureLB) && Timeout.notTimeout(expireTimeMillis)) {
      //generate random neighbouring solution
      val newSolution = neighbour(oldSolution)
      //calculate its cost
      val evNewSolution = cost(newSolution)
        
      val delta = evNewSolution.value - evOldSolution.value
      if(delta <= 0) {
        oldSolution = newSolution
        evOldSolution = evNewSolution
      } else {
        //calculate acceptance probability
        val ap = acceptanceProbability(delta, temperature)
        val randomNo = Random.nextDouble()
        //compare them
        if(randomNo <= ap) {
          oldSolution = newSolution
          evOldSolution = evNewSolution
        }
      }
      temperature = temperature / (1 + coolingRate*temperature)
    }
    evOldSolution
  }
  //call with default parameter values
  def evaluate(p: Problem): EvaluatedSolution = {
    val temperatureUB = p.jobTimesMatrix.map(ar => ar.reduceLeft[Int](_+_)).reduceLeft[Int](_+_) / (5*p.numOfJobs*p.numOfMachines)
    val temperatureLB = 1
    val iterations = scala.math.max(3300*scala.math.log(p.numOfJobs)+7500*scala.math.log(p.numOfMachines)-18250, 2000)
    val coolingRate = (temperatureUB-temperatureLB)/((iterations-1)*temperatureUB*temperatureLB)
    evaluate(p, temperatureUB, temperatureLB, coolingRate)
    
  }
}