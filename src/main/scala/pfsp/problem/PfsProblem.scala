package pfsp.problem

import Array._
import scala.io.Source
import it.polimi.hyperh._
import it.polimi.hyperh.types.Types._
import it.polimi.hyperh.problem.Problem
import it.polimi.hyperh.solution.Solution
import it.polimi.hyperh.solution.EvaluatedSolution
import pfsp.util.PfsProblemParser
import java.io.InputStream
import pfsp.solution.PfsSolution
import pfsp.solution.PfsEvaluatedSolution

@SerialVersionUID(100L)
class PfsProblem(
    val numOfJobs: Int,
    val numOfMachines: Int,
    val jobTimesMatrix: Array[Array[Int]] // delays(i)(j) of j-th job on i-th machine (0<=i<numOfMachines) (0<=j<numOfJobs)
    ) extends Problem {

  val jobs = (1 to numOfJobs) toArray

  //associate sequentially job key to array of times present in matrix
  def associateJobToArray(jobs: Array[Int], jobTimesMatrix: Array[Array[Int]]) = {
    jobs zip jobTimesMatrix
  }
  //returns array of times when provided the job value (key)
  def getTimesArrayByJobKey(job: Int, pairs: Array[(Int, Array[Int])]) = pairs.filter(_._1 == job).map(pair => pair._2).flatten

  //calculate end (completion) times for all jobs on each machine
  def jobsInitialTimes(): Array[Array[Int]] = {
    val resultingMatrix = Array.ofDim[Int](numOfMachines, numOfJobs)
    resultingMatrix(0) = jobTimesMatrix(0)
    for (jInd <- 0 until numOfJobs; mInd <- 1 until numOfMachines) {
      resultingMatrix(mInd)(jInd) = resultingMatrix(mInd - 1)(jInd) + jobTimesMatrix(mInd)(jInd)
    }
    resultingMatrix
  }
  val initEndTimesMatrix = jobsInitialTimes()
  //extracts end times from initEndTimesMatrix, e.g. Array(33, 31, 32, 29, 24)
  def extractEndTimes(matrix: Array[Array[Int]]): Array[Int] = {
    matrix(matrix.size - 1)
  }
  //associates job key to value of its end time.e.g. Array((1,33), (2,31), (3,32), (4,29), (5,24))
  def createJobValuePairs(jobs: Array[Int], times: Array[Int]) = {
    jobs zip times
  }
  //sort pairs of (jobkey,endTime) by second parameter, in a decreasing order
  def sortJobsDecreasing(pairs: Array[(Int, Int)]) = {
    pairs.sortBy(_._2).reverse
  }
  //gets first N jobs from a sorted (in decreased order) list of pairs (jobkey,endTime)
  def getWorstNJobs(pairs: Array[(Int, Int)], n: Int) = {
    val sorted = sortJobsDecreasing(pairs)
    pairs.take(n)
  }

  def evaluatePartialSolution(jobsPermutation: Permutation): PfsEvaluatedSolution = {
    val numOfPartJobs = jobsPermutation.length
    val numOfMachines = jobTimesMatrix.size
	//table represents completion time matrix
    val table = Array.ofDim[Int](numOfMachines, jobsPermutation.length)
	//first column is the same as in initEndTimesMatrix
    for (mInd <- 0 until numOfMachines)
      table(mInd)(0) = initEndTimesMatrix(mInd)(jobsPermutation(0) - 1)
	//calculate the rest of completion time matrix
    for (jInd <- 1 until numOfPartJobs; mInd <- 0 until numOfMachines) {
      if (mInd > 0)
        table(mInd)(jInd) = Math.max(table(mInd - 1)(jInd), table(mInd)(jInd - 1)) + jobTimesMatrix(mInd)(jobsPermutation(jInd) - 1)
      else
        table(mInd)(jInd) = table(mInd)(jInd - 1) + jobTimesMatrix(mInd)(jobsPermutation(jInd) - 1)
    }
    def encapsulate(value: Value, permutation: Array[Int]) = new PfsEvaluatedSolution(value, permutation)
    encapsulate(table(table.size - 1).max, jobsPermutation)
  }
  def evaluatePartialSolution(jobsPermutation: List[Int]): PfsEvaluatedSolution = {
    evaluatePartialSolution(jobsPermutation.toArray)
  }
  def sumJobTimesMatrix(): Int = {
    jobTimesMatrix.map(ar => ar.reduceLeft[Int](_ + _)).reduceLeft[Int](_ + _)
  }
  def evaluate(s: Solution): EvaluatedSolution = {
    val solution = s.asInstanceOf[PfsSolution]
    evaluatePartialSolution(solution.permutation.toList)
  }
  
  def getExecutionTime(): Double = {
    numOfMachines * (numOfJobs / 2.0) * 60 //termination is n*(m/2)*60 milliseconds
  }
}

//Problem Factory
object PfsProblem {
  /**
   * @arg path - path to a file
   */
  def fromFile(path: String): PfsProblem = PfsProblemParser(Source.fromFile(path).getLines().mkString(" x ") + " x ").getOrElse(throw new RuntimeException("ParserError"))
  /**
   * @arg name - name of a resource in src/main/resources and src/test/resources
   */
  def fromResources(name: String): PfsProblem = {
    val stream: InputStream = getClass.getResourceAsStream("/" + name)
    PfsProblemParser(Source.fromInputStream(stream).getLines().mkString(" x ") + " x ").getOrElse(throw new RuntimeException("ParserError"))
  }

}