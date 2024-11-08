package org.cleartk.classifier.svmlight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.cleartk.classifier.CleartkProcessingException;
import org.cleartk.classifier.encoder.features.BooleanEncoder;
import org.cleartk.classifier.encoder.features.FeatureVectorFeaturesEncoder;
import org.cleartk.classifier.encoder.features.NumberEncoder;
import org.cleartk.classifier.encoder.features.StringEncoder;
import org.cleartk.classifier.encoder.outcome.StringToIntegerOutcomeEncoder;
import org.cleartk.classifier.jar.DataWriter_ImplBase;
import org.cleartk.classifier.util.featurevector.FeatureVector;

/**
 * <br>
 * Copyright (c) 2009, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 */
public class OVASVMlightDataWriter extends DataWriter_ImplBase<OVASVMlightClassifierBuilder, FeatureVector, String, Integer> {

    public OVASVMlightDataWriter(File outputDirectory) throws IOException {
        super(outputDirectory);
        FeatureVectorFeaturesEncoder myFeaturesEncoder = new FeatureVectorFeaturesEncoder();
        myFeaturesEncoder.addEncoder(new NumberEncoder());
        myFeaturesEncoder.addEncoder(new BooleanEncoder());
        myFeaturesEncoder.addEncoder(new StringEncoder());
        this.setFeaturesEncoder(myFeaturesEncoder);
        this.setOutcomeEncoder(new StringToIntegerOutcomeEncoder());
        allFalseFile = this.trainingDataFile;
        allFalseWriter = this.trainingDataWriter;
        trainingDataWriters = new TreeMap<Integer, PrintWriter>();
    }

    @Override
    public void writeEncoded(FeatureVector features, Integer outcome) throws CleartkProcessingException {
        if (outcome != null && !trainingDataWriters.containsKey(outcome)) {
            try {
                addClass(outcome);
            } catch (IOException e) {
                throw new CleartkProcessingException(e);
            }
        }
        StringBuffer featureString = new StringBuffer();
        for (FeatureVector.Entry entry : features) {
            featureString.append(String.format(Locale.US, " %d:%.7f", entry.index, entry.value));
        }
        StringBuffer output = new StringBuffer();
        if (outcome == null) output.append("0"); else output.append("-1");
        output.append(featureString);
        allFalseWriter.println(output);
        for (int i : trainingDataWriters.keySet()) {
            output = new StringBuffer();
            if (outcome == null) output.append("0"); else if (outcome == i) output.append("+1"); else output.append("-1");
            output.append(featureString);
            trainingDataWriters.get(i).println(output);
        }
    }

    @Override
    public void finish() throws CleartkProcessingException {
        allFalseWriter.close();
        allFalseFile.delete();
        for (PrintWriter pw : trainingDataWriters.values()) {
            pw.flush();
            pw.close();
        }
        super.finish();
    }

    @Override
    protected OVASVMlightClassifierBuilder newClassifierBuilder() {
        return new OVASVMlightClassifierBuilder();
    }

    private void addClass(int label) throws IOException {
        File newTDFile = this.classifierBuilder.getTrainingDataFile(this.outputDirectory, label);
        newTDFile.delete();
        allFalseWriter.flush();
        copyFile(allFalseFile, newTDFile);
        trainingDataWriters.put(label, new PrintWriter(new BufferedWriter(new FileWriter(newTDFile, true))));
    }

    private void copyFile(File source, File target) throws IOException {
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(target).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    private File allFalseFile;

    private PrintWriter allFalseWriter;

    private Map<Integer, PrintWriter> trainingDataWriters;
}
