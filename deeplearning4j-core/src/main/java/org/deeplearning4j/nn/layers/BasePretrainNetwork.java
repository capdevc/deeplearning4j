/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.deeplearning4j.nn.layers;


import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.params.PretrainParamInitializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.nd4j.linalg.util.Shape;

import static org.deeplearning4j.util.MathUtils.binomial;


/**
 * Baseline class for any Neural Network used
 * as a layer in a deep network *
 * @author Adam Gibson
 *
 */
public abstract class BasePretrainNetwork extends BaseLayer {

    private static final long serialVersionUID = -7074102204433996574L;

    /**
     *
     * @param conf
     */
    public BasePretrainNetwork(NeuralNetConfiguration conf) {
        super(conf);
    }

    /**
     *
     * @param conf
     * @param input
     */
    public BasePretrainNetwork(NeuralNetConfiguration conf, INDArray input) {
        super(conf, input);
    }


    /**
     * Applies sparsity to the passed in hbias gradient
     * @param hBiasGradient the hbias gradient to apply to
     */
    protected void applySparsity(INDArray hBiasGradient) {
        INDArray change = hBiasGradient.mul(conf.getSparsity()).mul(-conf.getLr() * conf.getSparsity());
        hBiasGradient.addi(change);
    }


    @Override
    public void setScore() {
       if(this.input == null)
           return;

        if(conf.getLossFunction() != LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY) {
            INDArray output = transform(input);
            while(!Shape.shapeEquals(input.shape(), output.shape()))
                output = transform(input);
            score = -LossFunctions.score(
                    input,
                    conf.getLossFunction(),
                    output,
                    conf.getL2(),
                    conf.isUseRegularization());
            //minimization target
            if(conf.isMinimize())
                score = -score;
        }
        else {
            score =  -LossFunctions.reconEntropy(
                    input,
                    getParam(PretrainParamInitializer.BIAS_KEY),
                    getParam(PretrainParamInitializer.VISIBLE_BIAS_KEY),
                    getParam(PretrainParamInitializer.WEIGHT_KEY),
                    conf.getActivationFunction());
            if(conf.isMinimize())
                score = -score;
        }
    }

    /**
     * Corrupts the given input by doing a binomial sampling
     * given the corruption level
     * @param x the input to corrupt
     * @param corruptionLevel the corruption value
     * @return the binomial sampled corrupted input
     */
    public INDArray getCorruptedInput(INDArray x, double corruptionLevel) {
        INDArray corrupted = Nd4j.zeros(x.shape());
        INDArray linear = corrupted.linearView();
        for(int i = 0; i < x.length(); i++)
            linear.putScalar(i,binomial(conf.getRng(),1,1 - corruptionLevel));
        corrupted.muli(x);
        return corrupted;
    }



    protected Gradient createGradient(INDArray wGradient,INDArray vBiasGradient,INDArray hBiasGradient) {
        Gradient ret = new DefaultGradient();
        ret.gradientForVariable().put(PretrainParamInitializer.VISIBLE_BIAS_KEY,vBiasGradient);
        ret.gradientForVariable().put(PretrainParamInitializer.BIAS_KEY,hBiasGradient);
        ret.gradientForVariable().put(PretrainParamInitializer.WEIGHT_KEY,wGradient);
        return ret;
    }

    /**
     * Sample the hidden distribution given the visible
     * @param v the visible to sample from
     * @return the hidden mean and sample
     */
    public abstract Pair<INDArray,INDArray> sampleHiddenGivenVisible(INDArray v);

    /**
     * Sample the visible distribution given the hidden
     * @param h the hidden to sample from
     * @return the mean and sample
     */
    public abstract Pair<INDArray,INDArray> sampleVisibleGivenHidden(INDArray h);

}
