package org.clustering4ever.clustering.anttree
/**
 * @author Waris Radji
 * @author Beck Gaël
 */
import org.clustering4ever.clustering.{ClusteringAlgorithmLocal, ClusteringModelLocal}

import scala.language.higherKinds
import org.clustering4ever.clusterizables.Clusterizable
import org.clustering4ever.math.distances.{ContinuousDistance, Distance}
import org.clustering4ever.vectors.GVector

import scala.collection.GenSeq
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._

import scala.collection.{immutable, mutable}
import scalax.collection.mutable.{Graph => MutableGraph}

import scala.collection.mutable.ArrayBuffer

class Tree[T, E[X] <: EdgeLikeIn[X]](node : MutableGraph[T,E]) {
  final val graph = node
  final private val principalsCLuster = ArrayBuffer[graph.NodeT]()

  final def obtainPrincipalCluster(support: T): Unit = principalsCLuster ++= graph.get(support).diSuccessors

  final def getPrincipalCluster: ArrayBuffer[graph.NodeT] = principalsCLuster
}

trait AntTreeAlgoModelAncestor[V <: GVector[V], D <: Distance[V]] {

  val tree: Tree[(Long, Option[V]), UnDiEdge]

  final def allSuccessors(xpos: (Long, Option[V])): immutable.Set[(Long, Option[V])] = {
    val node = tree.graph.get(xpos)
    node.withSubgraph().map(_.value).toSet - node
  }

  final def directSuccessors(xpos: (Long, Option[V])): immutable.Set[(Long, Option[V])] = tree.graph.get(xpos).diSuccessors.map(_.value)

}


trait AntTreeModelAncestor[V <: GVector[V], D <: Distance[V]] extends ClusteringModelLocal[V] with AntTreeAlgoModelAncestor[V, D] {

  val tree: Tree[(Long, Option[V]), UnDiEdge]

  val metric: D

  private val supportID = Long.MinValue

  protected[clustering] final def obtainClustering[O, Cz[Y, Z <: GVector[Z]] <: Clusterizable[Y, Z, Cz], GS[X] <: GenSeq[X]](data: GS[Cz[O, V]]): GS[Cz[O, V]] = {

    val supportChild= tree.getPrincipalCluster.map(e => allSuccessors(e))

    data.map( cz => cz.addClusterIDs(supportChild.indexWhere(_.contains((cz.id, Some(cz.v))))) ).asInstanceOf[GS[Cz[O, V]]]

  }


  protected[clustering] final def predict[O, Cz[Y, Z <: GVector[Z]] <: Clusterizable[Y, Z, Cz], GS[X] <: GenSeq[X]](data: GS[Cz[O, V]]): GS[Cz[O, V]] = {
    def predictOnePoint(point: Cz[O, V]): Int = tree.getPrincipalCluster.indexOf(tree.getPrincipalCluster.maxBy(c =>metric.d(c._2.get, point.v)))
    // à simplifier !
    data.map(cz => cz.addClusterIDs(predictOnePoint(cz))).asInstanceOf[GS[Cz[O, V]]]

  }

}


final case class AntTreeModelScalar[V <: Seq[Double], D[X <: Seq[Double]] <: ContinuousDistance[X]](final val metric: D[V], final val tree: MutableGraph[Long, UnDiEdge])