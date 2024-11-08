package com.googlecode.fannj;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static org.junit.Assert.assertTrue;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class FannTrainerTest {

    @Test
    public void testTrainingDefault() throws IOException {
        File temp = File.createTempFile("fannj_", ".tmp");
        temp.deleteOnExit();
        IOUtils.copy(this.getClass().getResourceAsStream("xor.data"), new FileOutputStream(temp));
        List<Layer> layers = new ArrayList<Layer>();
        layers.add(Layer.create(2));
        layers.add(Layer.create(3, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        layers.add(Layer.create(1, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        Fann fann = new Fann(layers);
        Trainer trainer = new Trainer(fann);
        float desiredError = .001f;
        float mse = trainer.train(temp.getPath(), 500000, 1000, desiredError);
        assertTrue("" + mse, mse <= desiredError);
    }

    @Test
    public void testTrainingQuickprop() throws IOException {
        File temp = File.createTempFile("fannj_", ".tmp");
        temp.deleteOnExit();
        IOUtils.copy(this.getClass().getResourceAsStream("xor.data"), new FileOutputStream(temp));
        List<Layer> layers = new ArrayList<Layer>();
        layers.add(Layer.create(2));
        layers.add(Layer.create(3, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        layers.add(Layer.create(1, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        Fann fann = new Fann(layers);
        Trainer trainer = new Trainer(fann);
        trainer.setTrainingAlgorithm(TrainingAlgorithm.FANN_TRAIN_QUICKPROP);
        float desiredError = .001f;
        float mse = trainer.train(temp.getPath(), 500000, 1000, desiredError);
        assertTrue("" + mse, mse <= desiredError);
    }

    @Test
    public void testTrainingBackprop() throws IOException {
        File temp = File.createTempFile("fannj_", ".tmp");
        temp.deleteOnExit();
        IOUtils.copy(this.getClass().getResourceAsStream("xor.data"), new FileOutputStream(temp));
        List<Layer> layers = new ArrayList<Layer>();
        layers.add(Layer.create(2));
        layers.add(Layer.create(3, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        layers.add(Layer.create(2, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        layers.add(Layer.create(1, ActivationFunction.FANN_SIGMOID_SYMMETRIC));
        Fann fann = new Fann(layers);
        Trainer trainer = new Trainer(fann);
        trainer.setTrainingAlgorithm(TrainingAlgorithm.FANN_TRAIN_INCREMENTAL);
        float desiredError = .001f;
        float mse = trainer.train(temp.getPath(), 500000, 1000, desiredError);
        assertTrue("" + mse, mse <= desiredError);
    }

    @Test
    public void testCascadeTraining() throws IOException {
        File temp = File.createTempFile("fannj_", ".tmp");
        temp.deleteOnExit();
        IOUtils.copy(this.getClass().getResourceAsStream("parity8.train"), new FileOutputStream(temp));
        Fann fann = new FannShortcut(8, 1);
        Trainer trainer = new Trainer(fann);
        float desiredError = .00f;
        float mse = trainer.cascadeTrain(temp.getPath(), 30, 1, desiredError);
        assertTrue("" + mse, mse <= desiredError);
    }
}
