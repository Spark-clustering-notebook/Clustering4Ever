package clustering4ever.util

import scala.collection.immutable

/**
 * @author Beck Gaël
 *
 *
 **/
object LSH
{
  /**
   * Create a random vector where component are taken on normal law N(0,1) for LSH
   */
  def obtainHashVector(dim: Int): immutable.Seq[Double] = immutable.Seq.fill(dim)(scala.util.Random.nextGaussian)

  /**
   *  Generate the hash value for a given vector x depending on w, b, hashVector
   */
  def hf(x: immutable.Seq[Double], w: Double, b: Double, hv: immutable.Seq[Double]): Double = ( x.zip(hv).map{ case (a, b) => a * b }.sum + b ) / w
}