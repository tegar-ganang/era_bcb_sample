package com.amwebexpert.csv.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import au.com.bytecode.opencsv.CSVReader;
import com.amwebexpert.converter.AbstractGenericWkbConverter;
import com.amwebexpert.converter.ConvertionStepEnum;
import com.amwebexpert.utils.MemoryUtils;

/**
 * Convert CSV files into HTML to facilitate view/interaction inside a browser
 * 
 * @author amwebexpert@gmail.com
 */
public class HtmlConverter extends AbstractGenericWkbConverter {

    public static final int COL_WIDTH_FACTOR = 8;

    public static final int COL_WIDTH_MAX = 400;

    private List<String> headers;

    public int[] convert(CSVReader reader, File firstFile, int maxPerPage, int nbTotalRows, String pageBegin, String pageEnd) throws IOException, InterruptedException {
        Validate.notNull(reader, "CSVReader");
        Validate.notNull(firstFile, "firstFile");
        Validate.isTrue(maxPerPage > 0, "maxPerPage == 0");
        Validate.isTrue(nbTotalRows > 0, "nbTotalRows == 0");
        final File dir = firstFile.getParentFile();
        String nextFilename = StringUtils.replace(firstFile.getName(), ".html", ".tmp");
        File currentFile = new File(dir, nextFilename);
        Writer w = new BufferedWriter(new FileWriter(currentFile));
        int currentFileNumber = 1;
        List<File> files = new ArrayList<File>();
        files.add(currentFile);
        int currentRow = 1;
        String[] nextLine = reader.readNext();
        int[] colsCharCount = new int[nextLine.length];
        String header = buildHeaderLine(nextLine);
        w.write(header);
        while (currentRow < nbTotalRows) {
            currentRow++;
            if (progress != null) {
                float percent = ((float) currentRow / (float) nbTotalRows) * 100f;
                progress.updateProgress(ConvertionStepEnum.PROCESSING_ROWS, percent);
            }
            nextLine = reader.readNext();
            if (nextLine == null) {
                break;
            }
            writeTableRow(w, nextLine, colsCharCount);
            if (((currentRow - 1) % maxPerPage) == 0) {
                currentFileNumber++;
                w.flush();
                w.close();
                nextFilename = StringUtils.replace(firstFile.getName(), ".html", currentFileNumber + ".tmp");
                currentFile = new File(dir, nextFilename);
                files.add(currentFile);
                w = new BufferedWriter(new FileWriter(currentFile));
                w.write(header);
            }
        }
        if (w != null) {
            w.flush();
            w.close();
        }
        MemoryUtils.doGC();
        if (progress != null) {
            progress.updateProgress(ConvertionStepEnum.PROCESSING_NAVIGATION, 100);
            Thread.sleep(50);
        }
        completePagesNavigation(files, maxPerPage, nbTotalRows, pageBegin, pageEnd, colsCharCount);
        return colsCharCount;
    }

    private void completePagesNavigation(List<File> files, int maxPerPage, int nbTotalRows, String pageBegin, String pageEnd, int[] colsCharCount) throws IOException, InterruptedException {
        final String tableTag = buildTableTag(colsCharCount);
        String veryFirstFilenane = files.get(0).getName();
        String veryLastFilenane = files.get(files.size() - 1).getName();
        String previousFile = null;
        String nextFile = null;
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (i == 0) {
                previousFile = veryFirstFilenane;
            } else {
                previousFile = files.get(i - 1).getName();
            }
            if (i + 1 == files.size()) {
                nextFile = veryLastFilenane;
            } else {
                nextFile = files.get(i + 1).getName();
            }
            previousFile = StringUtils.replace(previousFile, ".tmp", ".html");
            nextFile = StringUtils.replace(nextFile, ".tmp", ".html");
            veryFirstFilenane = StringUtils.replace(veryFirstFilenane, ".tmp", ".html");
            veryLastFilenane = StringUtils.replace(veryLastFilenane, ".tmp", ".html");
            String pageBeginHtml = StringUtils.replace(pageBegin, "<PREVIOUS>", previousFile);
            pageBeginHtml = StringUtils.replace(pageBeginHtml, "<NEXT>", nextFile);
            pageBeginHtml = StringUtils.replace(pageBeginHtml, "<CURRENT_PAGE>", "" + (i + 1));
            pageBeginHtml = StringUtils.replace(pageBeginHtml, "<NB_PAGES>", "" + files.size());
            pageBeginHtml = StringUtils.replace(pageBeginHtml, "<VERY_FIRST>", veryFirstFilenane);
            pageBeginHtml = StringUtils.replace(pageBeginHtml, "<VERY_LAST>", veryLastFilenane);
            pageBeginHtml += tableTag;
            String filename = StringUtils.replace(f.getName(), ".tmp", ".html");
            File finalFile = new File(f.getParentFile(), filename);
            Writer w = new BufferedWriter(new FileWriter(finalFile));
            Reader r = new BufferedReader(new FileReader(f));
            mergeFiles(w, new StringReader(pageBeginHtml), r);
            w.write("</table>");
            w.flush();
            w.close();
            f.delete();
        }
        MemoryUtils.doGC();
    }

    private String buildTableTag(int[] colsCharCount) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writeTableStart(stringWriter, colsCharCount);
        writeColsDefinitions(stringWriter, colsCharCount);
        stringWriter.flush();
        String tableTag = stringWriter.toString();
        stringWriter.close();
        return tableTag;
    }

    private String buildHeaderLine(String[] nextLine) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writeTableRowHeader(stringWriter, nextLine);
        stringWriter.flush();
        String header = stringWriter.toString();
        stringWriter.close();
        return header;
    }

    public void convert(CSVReader reader, Writer writer, int nbTotalRows) throws IOException, InterruptedException {
        Validate.notNull(reader, "CSVReader");
        Validate.notNull(writer, "Writer");
        Writer bufferedWriter = new BufferedWriter(writer);
        File fileForColsDef = createTempFileForCss();
        BufferedWriter colsDefWriter = new BufferedWriter(new FileWriter(fileForColsDef));
        File fileForTable = createTempFileForTable();
        BufferedWriter tableWriter = new BufferedWriter(new FileWriter(fileForTable));
        try {
            int currentRow = 0;
            String[] nextLine = reader.readNext();
            if (nextLine != null) {
                int[] colsCharCount = new int[nextLine.length];
                writeTableRowHeader(tableWriter, nextLine);
                while ((nextLine = reader.readNext()) != null) {
                    currentRow++;
                    if (progress != null) {
                        float percent = ((float) currentRow / (float) nbTotalRows) * 100f;
                        progress.updateProgress(ConvertionStepEnum.PROCESSING_ROWS, percent);
                    }
                    writeTableRow(tableWriter, nextLine, colsCharCount);
                }
                writeTableStart(colsDefWriter, colsCharCount);
                writeColsDefinitions(colsDefWriter, colsCharCount);
            }
            writeConverterInfos(bufferedWriter);
            writeTableEnd(tableWriter);
            flushAndClose(tableWriter);
            flushAndClose(colsDefWriter);
            BufferedReader colsDefReader = new BufferedReader(new FileReader(fileForColsDef));
            BufferedReader tableReader = new BufferedReader(new FileReader(fileForTable));
            mergeFiles(bufferedWriter, colsDefReader, tableReader);
        } finally {
            closeQuietly(tableWriter);
            closeQuietly(colsDefWriter);
            fileForTable.delete();
            fileForColsDef.delete();
        }
    }

    private void writeColsDefinitions(Writer writer, int[] widths) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            sb.append(TAB);
            sb.append("<col");
            sb.append(" width=").append(q(Math.min(COL_WIDTH_MAX, widths[i] * COL_WIDTH_FACTOR)));
            sb.append("/>").append(CRLF);
        }
        writer.write(sb.toString());
    }

    private void writeTableStart(Writer w, int[] colsCharCount) throws IOException {
        int totalWidth = 0;
        for (int i = 0; i < colsCharCount.length; i++) {
            totalWidth += (colsCharCount[i] * COL_WIDTH_FACTOR);
        }
        w.write("<table class='sortable' id='table_id' cellpadding='0' cellspacing='0' width='");
        w.write(String.valueOf(totalWidth));
        w.write("'>");
        w.write(CRLF);
    }

    private void writeTableEnd(Writer w) throws IOException {
        w.write("</table>");
        w.write(CRLF);
    }

    private void writeTableRowHeader(Writer w, String[] line) throws IOException {
        w.write(TAB);
        w.write("<tr>");
        for (int i = 0; i < line.length; i++) {
            w.write("<th>");
            w.write(line[i]);
            w.write("</th>");
        }
        w.write("</tr>");
        w.write(CRLF);
        setHeaders(Arrays.asList(line));
    }

    private void writeTableRow(Writer w, String[] line, int[] colsCharCount) throws IOException {
        w.write(TAB);
        w.write("<tr>");
        for (int i = 0; i < line.length; i++) {
            w.write("<td>");
            w.write(line[i]);
            w.write("</td>");
            if (i < colsCharCount.length) {
                if (line[i].length() > colsCharCount[i]) {
                    colsCharCount[i] = line[i].length();
                }
            }
        }
        w.write("</tr>");
        w.write(CRLF);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
}
