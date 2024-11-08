package com.amwebexpert.csv.converter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import au.com.bytecode.opencsv.CSVReader;

public class HtmlConverterTest extends TestCase {

    public void _testCsv() {
        File csvFile = new File("C:/DE311/solution_workspace/WorkbookTaglib/WorkbookTagDemoWebapp/src/main/resources/csv/google.csv");
        try {
            CSVReader reader = new CSVReader(new BufferedReader(new FileReader(csvFile)), ',', '\t');
            List<String[]> lines = reader.readAll();
            for (String[] line : lines) {
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testConvertIntoMultipleFiles() {
        File csvFile = new File("C:/DE311/solution_workspace/WorkbookTaglib/WorkbookTagDemoWebapp/src/main/resources/csv/google.csv");
        try {
            Charset guessedCharset = com.glaforge.i18n.io.CharsetToolkit.guessEncoding(csvFile, 4096);
            String htmlEnd = readTextFile(new FileInputStream("C:/DE311/solution_workspace/WorkbookTaglib/WorkbookTagDemoWebapp/src/main/resources/templates/html_end.txt"));
            String htmlBegin = readTextFile(new FileInputStream("C:/DE311/solution_workspace/WorkbookTaglib/WorkbookTagDemoWebapp/src/main/resources/templates/html_start.txt"));
            htmlBegin = StringUtils.replace(htmlBegin, "<CHARSET>", guessedCharset.name());
            CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), guessedCharset)));
            File firstFile = new File("/temp/test.html");
            int nbLines = CsvConverterUtils.countLines(new BufferedReader(new FileReader(csvFile)));
            HtmlConverter conv = new HtmlConverter();
            conv.convert(reader, firstFile, 20, nbLines, htmlBegin, htmlEnd);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
	 * This method reads simple text file
	 * 
	 * @param inputStream
	 * @return data from file
	 */
    private String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
        }
        return outputStream.toString();
    }

    public void _testConvertIntoOneFile() {
        File csvFile = new File("C:/DE311/solution_workspace/WorkbookTaglib/WorkbookTagDemoWebapp/src/main/resources/csv/google.csv");
        try {
            Charset guessedCharset = com.glaforge.i18n.io.CharsetToolkit.guessEncoding(csvFile, 4096);
            CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), guessedCharset)));
            Writer writer = new FileWriter("/temp/test.html");
            int nbLines = CsvConverterUtils.countLines(new BufferedReader(new FileReader(csvFile)));
            HtmlConverter conv = new HtmlConverter();
            conv.convert(reader, writer, nbLines);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
