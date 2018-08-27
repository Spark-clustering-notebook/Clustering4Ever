package clustering4ever.spark.indexes

import scala.math.max
import scala.collection.{mutable, immutable}
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import clustering4ever.math.distances.scalar.Euclidean
import clustering4ever.math.distances.ContinuousDistance
import clustering4ever.clustering.ClusteringCommons
import clustering4ever.util.SumArrays
import clustering4ever.scala.indexes.InternalIndexesDBCommons

/**
 * @author Beck Gaël
 * This object is used to compute internals clustering indexes as Davies Bouldin or Silhouette
 */
class InternalIndexes extends ClusteringCommons
{
  private def daviesBouldinIndex(sc: SparkContext, data: RDD[(Int, Seq[Double])], clusterLabels: Seq[Int], metric: ContinuousDistance[Seq[Double]] = new Euclidean[Seq[Double]](squareRoot = true)) =
  {
    if( clusterLabels.size == 1 )
    {
      println(" One Cluster found")
      0D
    }
    else
    {
      val neutralElement = mutable.ArrayBuffer.empty[Seq[Double]]
      def addToBuffer(buff: mutable.ArrayBuffer[Seq[Double]], elem: Seq[Double]) = buff += elem
      def aggregateBuff(buff1: mutable.ArrayBuffer[Seq[Double]], buff2: mutable.ArrayBuffer[Seq[Double]]) = buff1 ++= buff2
      val neutralElement2 = mutable.ArrayBuffer.empty[Double]
      def addToBuffer2(buff: mutable.ArrayBuffer[Double], elem: Double) = buff += elem
      def aggregateBuff2(buff1: mutable.ArrayBuffer[Double], buff2: mutable.ArrayBuffer[Double]) = buff1 ++= buff2

      val clusters = data.aggregateByKey(neutralElement)(addToBuffer, aggregateBuff).collect
      val centers = clusters.map{ case (k, v) => (k, SumArrays.obtainMean(Seq(v:_*))) }
      val scatters = clusters.zipWithIndex.map{ case ((k, v), idCLust) => (k, InternalIndexesDBCommons.scatter(v, centers(idCLust)._2, metric)) }
      val clustersWithCenterandScatters = (centers.map{ case (id, ar) => (id, (Some(ar), None)) } ++ scatters.map{ case (id, v) => (id, (None, Some(v))) })
        .par
        .groupBy(_._1)
        .map{ case (id, aggregate) =>
        {
          val agg = aggregate.map(_._2)
          val a = agg.head
          val b = agg.last
          if( a._1.isDefined ) (id, (b._2.get, a._1.get)) else (id, (a._2.get, b._1.get))
        }}
      val cart = for( i <- clustersWithCenterandScatters; j <- clustersWithCenterandScatters if( i._1 != j._1 ) ) yield (i, j)
      val rijList = sc.parallelize(cart.seq.toSeq).map{ case ((idClust1, (centroid1, scatter1)), (idClust2, (centroid2, scatter2))) => (idClust1, InternalIndexesDBCommons.good(centroid1, centroid2, scatter1, scatter2, metric)) }
      val di = rijList.aggregateByKey(neutralElement2)(addToBuffer2, aggregateBuff2).map{ case (_, goods)=> goods.reduce(max(_, _)) }
      val numCluster = clusterLabels.size
      val daviesBouldinIndex = di.fold(0)(_ + _) / numCluster
      daviesBouldinIndex
    }
  }

  private def ballHallIndex(clusterized: RDD[(ClusterID, immutable.Vector[Double])], metric: ContinuousDistance[Seq[Double]] = new Euclidean[Seq[Double]](squareRoot = true)): Double =
  {
    val neutralElement = mutable.ArrayBuffer.empty[Seq[Double]]
    def addToBuffer(buff: mutable.ArrayBuffer[Seq[Double]], elem: Seq[Double]) = buff += elem
    def aggregateBuff(buff1: mutable.ArrayBuffer[Seq[Double]], buff2: mutable.ArrayBuffer[Seq[Double]]) = buff1 ++= buff2

    val clusters = clusterized.aggregateByKey(neutralElement)(addToBuffer, aggregateBuff).cache

    val prototypes = clusters.map{ case (clusterID, aggregate) =>
    (
      clusterID,
      SumArrays.obtainMean(aggregate)
    )}.collectAsMap

    clusters.map{ case (clusterID, aggregate) => aggregate.map( v => metric.d(v, prototypes(clusterID)) ).sum / aggregate.size }.sum / clusters.count
  }
}

object InternalIndexes extends ClusteringCommons
{
  /**
   * Monothreaded version of davies bouldin index
   * Complexity O(n.c<sup>2</sup>) with n number of individuals and c the number of clusters
   **/
  def daviesBouldinIndexWithLabels(sc: SparkContext, clusterized: RDD[(ClusterID, Seq[Double])], clusterLabels: Seq[Int], metric: ContinuousDistance[Seq[Double]] = new Euclidean[Seq[Double]](squareRoot = true)): Double =
    (new InternalIndexes).daviesBouldinIndex(sc, clusterized, clusterLabels, metric)

  /**
   * Monothreaded version of davies bouldin index
   * Complexity O(n.c<sup>2</sup>) with n number of individuals and c the number of clusters
   **/
  def daviesBouldinIndex(sc: SparkContext, clusterized: RDD[(ClusterID, Seq[Double])], metric: ContinuousDistance[Seq[Double]] = new Euclidean[Seq[Double]](squareRoot = true)): Double =
  {
    val clusterLabels = clusterized.map(_._1).distinct.collect
    daviesBouldinIndexWithLabels(sc, clusterized, clusterLabels, metric)
  }

  def ballHallIndex(clusterized: RDD[(ClusterID, immutable.Vector[Double])], metric: ContinuousDistance[Seq[Double]] = new Euclidean[Seq[Double]](squareRoot = true)): Double =
    (new InternalIndexes).ballHallIndex(clusterized, metric)

}