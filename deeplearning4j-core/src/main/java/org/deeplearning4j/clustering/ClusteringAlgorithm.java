package org.deeplearning4j.clustering;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface ClusteringAlgorithm {

	ClusterSet applyTo(List<INDArray> points);
	
}
