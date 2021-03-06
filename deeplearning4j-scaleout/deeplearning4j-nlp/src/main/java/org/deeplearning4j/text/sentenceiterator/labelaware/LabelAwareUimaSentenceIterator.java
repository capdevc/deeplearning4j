package org.deeplearning4j.text.sentenceiterator.labelaware;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.deeplearning4j.text.annotator.SentenceAnnotator;
import org.deeplearning4j.text.annotator.TokenizerAnnotator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.UimaSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.uima.UimaResource;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Uima sentence iterator that is aware of the current file
 * @author Adam Gibson
 */
public class LabelAwareUimaSentenceIterator extends UimaSentenceIterator implements LabelAwareSentenceIterator {

    public LabelAwareUimaSentenceIterator(SentencePreProcessor preProcessor, String path,UimaResource resource) {
        super(preProcessor, path, resource);
    }

    public LabelAwareUimaSentenceIterator(String path, AnalysisEngine engine) throws ResourceInitializationException {
        super(path, new UimaResource(engine));
    }


    /**
     * Returns the current label for nextSentence()
     *
     * @return the label for nextSentence()
     */
    @Override
    public String currentLabel() {

        try {
            /**
             * Little bit hacky, but most concise way to do it.
             * Get the parent collection reader's current file.
             * The collection reader is basically a wrapper for a file iterator.
             * We can use this to ge the current file for the iterator.
             */
            Field f = reader.getClass().getDeclaredField("currentFile");
            f.setAccessible(true);
            File file = (File) f.get(reader);
            return file.getParentFile().getName();
        }

        catch (NullPointerException e1) {
            return "NONE";
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Creates a uima sentence iterator with the given path
     * @param path the path to the root directory or file to read from
     * @return the uima sentence iterator for the given root dir or file
     * @throws Exception
     */
    public static LabelAwareSentenceIterator createWithPath(String path) throws Exception {
        return new LabelAwareUimaSentenceIterator(null,path,new UimaResource(AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(TokenizerAnnotator.getDescription(), SentenceAnnotator.getDescription()))));
    }
    @Override
    public List<String> currentLabels() {
        return Arrays.asList(currentLabel());
    }
}
