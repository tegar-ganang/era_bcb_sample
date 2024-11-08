package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Performs no transform on the input matrix.
 */
public class NoTransform implements Transform {

    /**
     * {@inheritDoc}
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) throws IOException {
        return inputMatrixFile;
    }

    /**
     * {@inheritDoc}
     */
    public void transform(File inputMatrixFile, MatrixIO.Format inputFormat, File outputMatrixFile) throws IOException {
        FileChannel original = new FileInputStream(inputMatrixFile).getChannel();
        FileChannel copy = new FileOutputStream(outputMatrixFile).getChannel();
        copy.transferFrom(original, 0, original.size());
        original.close();
        copy.close();
    }

    /**
     * {@inheritDoc}
     */
    public Matrix transform(Matrix input) {
        return input;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix transform(Matrix input, Matrix output) {
        Matrices.copyTo(input, output);
        return output;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "no";
    }
}
