package uk.ac.shef.wit.aleph.algorithm;

import uk.ac.shef.wit.aleph.AlephException;
import uk.ac.shef.wit.aleph.dataset.Dataset;
import uk.ac.shef.wit.aleph.dataset.io.DatasetReader;
import uk.ac.shef.wit.aleph.dataset.io.DatasetWriter;
import uk.ac.shef.wit.commons.UtilFiles;
import java.io.*;
import java.net.URI;

/**
 * Encapsulates common functionality of wrappers for external classifiers that are run as separate OS processes.
 * External classifiers have an associated {@link DatasetReader} and {@link DatasetWriter}, since datasets are serialized
 * to disk to be used by the external tools.
 *
 * @author Jose' Iria, NLP Group, University of Sheffield
 *         (<a  href="mailto:J.Iria@dcs.shef.ac.uk" >email</a>)
 */
public abstract class LearnerExternal implements Learner, Serializable {

    private final String _pathToExecutables;

    protected LearnerExternal(final String pathToExecutables) {
        _pathToExecutables = UtilFiles.addSeparator(pathToExecutables);
    }

    public Classifier learn(final Dataset dataset) throws AlephException {
        final LearnerExternal learner = this;
        final String directory = UtilFiles.addSeparator('.' + getClass().getName());
        final URI model = createDatasetWriter(directory, dataset.label(), "model").getURI();
        final DatasetWriter train = createDatasetWriter(directory, dataset.label(), "train");
        train.write(dataset);
        learnExternal(new File(train.getURI()).getAbsolutePath(), new File(model).getAbsolutePath());
        return new Classifier() {

            private LearnerExternal _learner = learner;

            private String _directory = directory;

            private URI _model = model;

            private String _label = dataset.label();

            public Dataset classify(final Dataset dataset) throws AlephException {
                final DatasetWriter test = _learner.createDatasetWriter(_directory, _label, "test");
                final DatasetReader predictions = _learner.createDatasetReader(_directory, _label, "predictions", "test");
                test.write(dataset);
                _learner.classifyExternal(new File(test.getURI()).getAbsolutePath(), new File(_model).getAbsolutePath(), new File(predictions.getURI()).getAbsolutePath());
                return predictions.read();
            }

            private void writeObject(final ObjectOutputStream s) throws IOException {
                s.writeObject(_model);
                s.writeObject(_label);
                s.writeObject(_learner);
                s.writeObject(UtilFiles.getContent(_model.toURL()).toString());
            }

            private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
                _directory = UtilFiles.addSeparator('.' + getClass().getName());
                _model = (URI) s.readObject();
                _label = (String) s.readObject();
                _learner = (LearnerExternal) s.readObject();
                final String directory = UtilFiles.addSeparator(_directory + _label);
                new File(directory).mkdirs();
                UtilFiles.writeToFile((String) s.readObject(), directory + new File(_model).getName());
            }
        };
    }

    public Classifier learn(final Dataset dataset, final Classifier classifier) throws AlephException {
        throw new AlephException("not implemented");
    }

    public String getPathToExecutables() {
        if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
            return _pathToExecutables.replaceAll("/", "\\\\");
        }
        return _pathToExecutables;
    }

    protected abstract void learnExternal(String train, String model) throws AlephException;

    protected abstract void classifyExternal(String test, String model, String predictions) throws AlephException;

    protected abstract DatasetReader createDatasetReader(String path, String label, String name, String idName);

    protected abstract DatasetWriter createDatasetWriter(String path, String label, String name);
}
