package org.jcvi.trace.fourFiveFour.flowgram.sff;

import java.io.IOException;
import java.io.OutputStream;

public class SffVisitorWriter implements SffFileVisitor {

    private final OutputStream out;

    /**
     * @param out
     */
    public SffVisitorWriter(OutputStream out) {
        this.out = out;
    }

    @Override
    public boolean visitCommonHeader(SFFCommonHeader header) {
        try {
            SffWriter.writeCommonHeader(header, out);
        } catch (IOException e) {
            throw new IllegalStateException("error writing Sff Common Header ", e);
        }
        return true;
    }

    @Override
    public boolean visitReadData(SFFReadData readData) {
        try {
            SffWriter.writeReadData(readData, out);
        } catch (IOException e) {
            throw new IllegalStateException("error writing Sff read Data ", e);
        }
        return true;
    }

    @Override
    public boolean visitReadHeader(SFFReadHeader readHeader) {
        try {
            SffWriter.writeReadHeader(readHeader, out);
        } catch (IOException e) {
            throw new IllegalStateException("error writing Sff read header ", e);
        }
        return true;
    }

    @Override
    public void visitEndOfFile() {
    }

    @Override
    public void visitFile() {
    }
}
