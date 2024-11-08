package org.jcvi.assembly.tasm;

import java.io.PrintWriter;
import org.jcvi.common.util.Range;
import org.jcvi.sequence.SequenceDirection;

/**
 * @author dkatzel
 *
 *
 */
public class TigrAssemblyFilePrinter implements TigrAssemblyFileVisitor {

    private final PrintWriter writer;

    public TigrAssemblyFilePrinter() {
        this.writer = new PrintWriter(System.out, true);
    }

    /**
     * @param writer
     */
    public TigrAssemblyFilePrinter(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public void visitContigAttribute(String key, String value) {
        writer.printf("Contig attr[ %s : %s ]%n", key, value);
    }

    @Override
    public void visitReadAttribute(String key, String value) {
        writer.printf("\t\tRead attr[ %s : %s ]%n", key, value);
    }

    @Override
    public void visitConsensusBasecallsLine(String consensus) {
        writer.printf("Contig consensus = %s%n", consensus);
    }

    @Override
    public void visitNewContig(String contigId) {
        writer.printf("Contig Id = %s%n", contigId);
    }

    @Override
    public void visitNewRead(String readId, int offset, Range validRange, SequenceDirection dir) {
        if (readId.equals("SBPQA03T48E02PA1950R")) {
            System.out.println("here");
        }
        writer.printf("\tRead %s start = %d validRange = %s dir = %s%n", readId, offset, validRange, dir);
    }

    @Override
    public void visitReadBasecallsLine(String lineOfBasecalls) {
        writer.printf("\t%s%n", lineOfBasecalls);
    }

    @Override
    public void visitLine(String line) {
    }

    @Override
    public void visitEndOfFile() {
    }

    @Override
    public void visitFile() {
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitBeginContigBlock() {
        writer.println("{");
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitBeginReadBlock() {
        writer.println("\t{");
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitEndContigBlock() {
        writer.println("}");
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void visitEndReadBlock() {
        writer.println("\t}");
    }
}
