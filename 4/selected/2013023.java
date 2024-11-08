package uk.org.ogsadai.activity.transform;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.MatchedIterativeActivity;
import uk.org.ogsadai.activity.io.ActivityInput;
import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.ControlBlock;
import uk.org.ogsadai.activity.io.InputStreamActivityInput;
import uk.org.ogsadai.activity.io.PipeClosedException;
import uk.org.ogsadai.activity.io.PipeIOException;
import uk.org.ogsadai.activity.io.PipeTerminatedException;
import uk.org.ogsadai.metadata.MetadataWrapper;
import uk.org.ogsadai.tuple.serialise.TupleInputStream;
import uk.org.ogsadai.tuple.serialise.UnsupportedTupleTypeException;

/**
 * An activity that converts a binary tuple stream into OGSA-DAI tuples.
 * The result is output as an OGSA-DAI list of tuples.
 * <p>
 * Activity inputs:
 * </p>
 * <ul>
 * <li>
 * <code>data</code>. Type: OGSA-DAI list of <code>byte[]</code>. This is a
 * list of tuples rendered in an OGSA-DAI internal binary format to be converted
 * into tuples.
 * </li>
 * </ul>
 * <p>
 * Activity outputs:
 * </p>
 * <ul>
 * <li>
 * <code>result</code> Type: OGSA-DAI list of {@link uk.org.ogsadai.tuple.Tuple}
 * with the first item in the list an instance of
 * {@link uk.org.ogsadai.metadata.MetadataWrapper} containing a
 * {@link uk.org.ogsadai.tuple.TupleMetadata} object. 
 * </li>
 * </ul>
 * <p>
 * Configuration parameters: none.
 * </p>
 * <p>
 * Activity input/output ordering: none.
 * </p>
 * <p>
 * Activity contracts: none.
 * </p>
 * <p>
 * Target data resource: none.
 * </p>
 * <p>
 * Behaviour:
 * </p>
 * <ul>
 * <li>
 * The activity receives as input a list of tuples in binary format and converts
 * the binary data into a list of OGSA-DAI tuples.
 * </li>
 * </ul>
 * 
 * @author The OGSA-DAI Project Team.
 */
public class ByteArraysToTupleActivity extends MatchedIterativeActivity {

    /** Copyright statement. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2009";

    /** Name of activity input data. */
    private static final String DATA_INPUT = "data";

    /** Name of result output. */
    private static final String OUTPUT = "result";

    /** The output block writer. */
    private BlockWriter mOutput;

    /**
     * {@inheritDoc}
     */
    @Override
    protected ActivityInput[] getIterationInputs() {
        return new ActivityInput[] { new InputStreamActivityInput(DATA_INPUT) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preprocess() throws ActivityUserException, ActivityProcessingException, ActivityTerminatedException {
        validateOutput(OUTPUT);
        mOutput = getOutput();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processIteration(Object[] iterationData) throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
        InputStream input = (InputStream) iterationData[0];
        try {
            TupleInputStream tuples = new TupleInputStream(input);
            mOutput.write(ControlBlock.LIST_BEGIN);
            mOutput.write(new MetadataWrapper(tuples.readTupleMetadata()));
            try {
                while (true) {
                    mOutput.write(tuples.readTuple());
                }
            } catch (EOFException e) {
            }
            mOutput.write(ControlBlock.LIST_END);
        } catch (UnsupportedTupleTypeException e) {
            throw new ActivityUserException(e);
        } catch (IOException e) {
            throw new ActivityProcessingException(e);
        } catch (PipeClosedException e) {
            iterativeStageComplete();
        } catch (PipeIOException e) {
            throw new ActivityProcessingException(e);
        } catch (PipeTerminatedException e) {
            throw new ActivityTerminatedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postprocess() throws ActivityUserException, ActivityProcessingException, ActivityTerminatedException {
    }
}
