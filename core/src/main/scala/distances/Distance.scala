package clustering4ever.math.distances
/**
 * @author Beck Gaël
 */
import scala.language.higherKinds
import clustering4ever.scala.measurableclass.BinaryScalarVector
/**
 * Most general notion of Distance, taking two object of type O and returning a Double
 */
trait Distance[O] extends Serializable {
	def d(o1: O, o2: O): Double
}
/**
 *
 */
trait DistanceSeq[@specialized(Int, Double) N, @specialized V <: Seq[N]] extends Distance[V]
/**
 *
 */
trait ClusterizableDistance[O, V, D <: Distance[V]] extends Distance[O] {
	val classicalMetric: D
}
/**
 *
 */
trait RealClusterizableDistance[O, V <: Seq[Double], D <: ContinuousDistance[V]] extends ClusterizableDistance[O, V, D]
/**
 *
 */
trait BinaryClusterizableDistance[O, V <: Seq[Int], D <: BinaryDistance[V]] extends ClusterizableDistance[O, V, D]
/**
 *
 */
trait MixtClusterizableDistance[O, Vb <: Seq[Int], Vs <: Seq[Double], D <: MixtDistance[Vb, Vs]] extends ClusterizableDistance[O, BinaryScalarVector[Vb, Vs], D]
/**
 *
 */
trait ContinuousDistance[V <: Seq[Double]] extends DistanceSeq[Double, V]
/**
 *
 */
trait BinaryDistance[V <: Seq[Int]] extends DistanceSeq[Int, V]
/**
 *
 */
trait MixtDistance[Vb <: Seq[Int], Vs <: Seq[Double]] extends Distance[BinaryScalarVector[Vb, Vs]]