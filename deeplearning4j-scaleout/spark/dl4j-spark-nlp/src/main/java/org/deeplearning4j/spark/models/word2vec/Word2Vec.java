package org.deeplearning4j.spark.models.word2vec;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.word2vec.Huffman;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.spark.text.TextPipeline;
import org.deeplearning4j.spark.text.TokenizerFunction;
import org.deeplearning4j.spark.text.TokentoVocabWord;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Spark version of word2vec
 *
 * @author Adam Gibson
 */
public class Word2Vec {

    private Broadcast<VocabCache> vocabCacheBroadcast;
    private String tokenizerFactoryClazz = DefaultTokenizerFactory.class.getName();


    /**
     * Train and return the result based on the given records.
     * Each string is assumed to be a document
     * @param rdd the rdd to train on
     * @return the vocab and lookup table for the model
     */
    public Pair<VocabCache,WeightLookupTable> train(JavaRDD<String> rdd) {
        TextPipeline pipeline = new TextPipeline(rdd);
        Pair<VocabCache,Long> vocabAndNumWords = pipeline.process(tokenizerFactoryClazz);
        SparkConf conf = rdd.context().getConf();
        JavaSparkContext sc = new JavaSparkContext(rdd.context());
        vocabCacheBroadcast = sc.broadcast(vocabAndNumWords.getFirst());
        InMemoryLookupTable lookupTable = (InMemoryLookupTable) new InMemoryLookupTable.Builder()
                .cache(vocabAndNumWords.getFirst()).lr(conf.getDouble(Word2VecPerformer.ALPHA,0.025))
                .vectorLength(conf.getInt(Word2VecPerformer.VECTOR_LENGTH,100)).negative(conf.getDouble(Word2VecPerformer.NEGATIVE,5))
                .useAdaGrad(conf.getBoolean(Word2VecPerformer.ADAGRAD,false)).build();
        lookupTable.resetWeights();

        Huffman huffman = new Huffman(vocabAndNumWords.getFirst().vocabWords());
        huffman.build();

        Word2VecPerformer performer = new Word2VecPerformer(
                sc,sc.broadcast(new AtomicLong(vocabAndNumWords.getSecond())),lookupTable
        );

        rdd.map(new TokenizerFunction(tokenizerFactoryClazz))
                .map(new TokentoVocabWord(vocabCacheBroadcast)).foreach(performer);


       return new Pair<VocabCache, WeightLookupTable>(vocabCacheBroadcast.getValue(),lookupTable);

    }


}
