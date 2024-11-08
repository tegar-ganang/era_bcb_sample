package com.rapidminer.report;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * A simple report stream to a file.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class FileReportStream extends AbstractReportStream {

    private PrintWriter writer;

    public FileReportStream(String name, ReportIOProvider ioProvider, String outputName) throws IOException {
        super(name, ioProvider);
        writer = new PrintWriter(ioProvider.createOutputStream(name, "text/plain"));
    }

    public void startSection(String sectionName, int sectionLevel) {
    }

    public void addPageBreak() {
    }

    @Override
    public void append(String name, Readable readable) {
        writer.write(readable.toString() + "\n");
    }

    @Override
    public void append(String name, Renderable renderable, int width, int height) {
        writer.write("Renderable\n");
    }

    @Override
    public void append(String name, Tableable tableable) {
        tableable.prepareReporting();
        for (int row = 0; row < tableable.getRowNumber(); row++) {
            for (int column = 0; column < tableable.getColumnNumber(); column++) {
                writer.write(tableable.getCell(row, column));
                writer.write("\t");
            }
            writer.write("\n");
        }
        tableable.finishReporting();
    }

    public void close() {
        writer.close();
        this.writer = null;
    }
}
