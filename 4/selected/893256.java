package org.cleartk.classifier.libsvm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import libsvm.svm_model;
import org.apache.commons.io.IOUtils;

/**
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Steven Bethard
 */
public abstract class LIBSVMClassifierBuilder<CLASSIFIER_TYPE extends LIBSVMClassifier<OUTCOME_TYPE, ENCODED_OUTCOME_TYPE>, OUTCOME_TYPE, ENCODED_OUTCOME_TYPE, MODEL_TYPE> extends GenericLIBSVMClassifierBuilder<CLASSIFIER_TYPE, OUTCOME_TYPE, ENCODED_OUTCOME_TYPE, libsvm.svm_model> {

    public static final String SCALE_FEATURES_KEY = "scaleFeatures";

    public static final String SCALE_FEATURES_VALUE_NORMALIZEL2 = "normalizeL2";

    @Override
    public String getCommand() {
        return "svm-train";
    }

    @Override
    protected String getModelName() {
        return "model.libsvm";
    }

    @Override
    protected svm_model loadModel(InputStream inputStream) throws IOException {
        File tmpFile = File.createTempFile("tmp", ".mdl");
        FileOutputStream output = new FileOutputStream(tmpFile);
        try {
            IOUtils.copy(inputStream, output);
            return libsvm.svm.svm_load_model(tmpFile.getPath());
        } finally {
            output.close();
            tmpFile.delete();
        }
    }
}
