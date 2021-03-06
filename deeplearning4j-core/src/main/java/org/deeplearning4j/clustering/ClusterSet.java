package org.deeplearning4j.clustering;

import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.distancefunction.DistanceFunction;
import org.nd4j.linalg.factory.Nd4j;

public class ClusterSet {

	private Class<? extends DistanceFunction>	distanceFunction;
	private List<Cluster>						clusters	= new ArrayList<Cluster>();

	public ClusterSet() {

	}

	public ClusterSet(INDArray centers) {
		for (Integer idx = 0, count = centers.rows(); idx < count; idx++) {
			clusters.add(new Cluster(centers.getRow(idx)));
		}
	}
	
	public ClusterSet(Class<? extends DistanceFunction> distanceFunction) {
		this.distanceFunction = distanceFunction;
	}

	
	public void addNewClusterWithCenter(INDArray center) {
		getClusters().add(new Cluster(center));
	}
	
	public INDArray getCenters() {
		INDArray centers = Nd4j.create(clusters.size(), clusters.get(0).getCenter().columns());
		for (Integer idx = 0, count = clusters.size(); idx < count; idx++) {
			centers.putRow(idx, clusters.get(idx).getCenter());
		}
		return centers;
	}
	
	public void addPoint(INDArray point) {
		nearestCluster(point).addPoint(point, true);
	}
	public void addPoint(INDArray point, boolean moveClusterCenter) {
		nearestCluster(point).addPoint(point, moveClusterCenter);
	}
	
	public void addPoints(List<INDArray> points) {
		addPoints(points, true);
	}
	public void addPoints(List<INDArray> points, boolean moveClusterCenter) {
		for( INDArray point : points )
			addPoint(point, moveClusterCenter);
	}
	
	public Cluster classify(INDArray point) {
		return classify(point, distanceFunction);
	}

	public Cluster classify(INDArray point, Class<? extends DistanceFunction> distanceFunction) {
		return nearestCluster(point);
	}

	protected Cluster nearestCluster(INDArray point) {

		Cluster nearestCluster = null;
		double minDistance = Float.MAX_VALUE;

		double currentDistance;
		for (Cluster cluster : getClusters()) {
			INDArray currentCenter = cluster.getCenter();
			if (currentCenter != null) {
				currentDistance = getDistance(currentCenter, point);
				if (currentDistance < minDistance) {
					minDistance = currentDistance;
					nearestCluster = cluster;
				}
			}
		}

		return nearestCluster;
	}

	private double getDistance(INDArray m1, INDArray m2) {
		DistanceFunction function;
		try {
			function = distanceFunction.getConstructor(INDArray.class).newInstance(m1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return function.apply(m2);
	}
	
	public double getDistanceFromNearestCluster(INDArray point) {
		Cluster nearestCluster = nearestCluster(point);
		return getDistance(nearestCluster.getCenter(), point);
	}
	
	public int getClusterCount() {
		return getClusters()==null ? 0 : getClusters().size();
	}
	
	public void removePoints() {
		for(Cluster cluster : getClusters() )
			cluster.removePoints();
	}

	public List<Cluster> getClusters() {
		return clusters;
	}

	public void setClusters(List<Cluster> clusters) {
		this.clusters = clusters;
	}

	public Class<? extends DistanceFunction> getDistanceFunction() {
		return distanceFunction;
	}

	public void setDistanceFunction(Class<? extends DistanceFunction> distanceFunction) {
		this.distanceFunction = distanceFunction;
	}

}
