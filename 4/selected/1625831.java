package com.tecacet.jflat;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;

/**
 * Writes a collection of beans in flat file format. Uses a WriterRowMapper to
 * map beans to tokens and a LineMerger to construct a line from tokens. The
 * write methods can also be used to create the file line by line.
 * 
 * @author Dimitri Papaioannou
 * 
 * @param <T>
 */
public class FlatFileWriter<T> {

    private PrintWriter pw;

    /**
     * The line merger merges sting arrays into lines
     */
    protected LineMerger lineMeger;

    /**
     * RowMapper used to convert beans to tokens
     */
    protected WriterRowMapper<T> rowMapper;

    public FlatFileWriter(Writer writer, LineMerger merger, WriterRowMapper<T> mapper) {
        pw = new PrintWriter(writer);
        this.lineMeger = merger;
        this.rowMapper = mapper;
    }

    /**
     * Writes an entire list of beans to a flat file.
     * 
     * @param beans
     *            a List of beans, each one representing a source of a line in
     *            the file.
     */
    public void writeAll(Collection<T> beans) {
        for (T bean : beans) {
            String[] nextLine = rowMapper.getRow(bean);
            writeNext(nextLine);
        }
        close();
    }

    /**
     * Writes the next line to the file.
     * 
     * @param line
     *            a string array with string of tokens as a separate entry. Uses
     *            a LineMerger to compose the line
     */
    public void writeNext(String[] line) {
        writeNext(lineMeger.makeLine(line));
    }

    /**
     * Writes the next line to the file.
     * 
     * @param line
     *            The line to write
     */
    public void writeNext(String line) {
        pw.write(line);
    }

    /**
     * Flush underlying stream to writer.
     * 
     */
    public void flush() {
        pw.flush();
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     * 
     */
    public void close() {
        pw.flush();
        pw.close();
    }
}
