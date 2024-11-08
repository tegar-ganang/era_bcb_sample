package com.fatsatsuma.spreadsheets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * @author $Id: SpreadSheetWriterTestCase.java,v 1.1 2007/06/07 10:12:56 gevans
 *         Exp $
 */
public class SpreadSheetWriterTestCase extends TestCase {

    private static final String FILE = "target/testcase-output/test-spreadsheet-output.xls";

    private static final String[] HEADERS = { "Country", "Code", "Tax Code 1", "Tax Code 2", "Tax Code 3" };

    public void testBasicSpreadsheetWriter() throws IOException, InvalidWorkbookException, WorksheetDoesNotExistException {
        SpreadsheetWriter writer = new SpreadsheetWriter();
        writer.createSheets("Country Infomation", "Zone Information");
        writer.setHeaders("Country Infomation", HEADERS);
        writer.addDataRow("Country Infomation", "UK", "001", "0.175", "0.175", "0.175");
        writer.addDataRow("Country Infomation", "US", "002", "0.15", "0.10", "0.05");
        writer.addDataRow("Country Infomation", "AUS", "003", "0.05", "0.05", "0.05");
        writer.addDataRow("Country Infomation", "FRANCE", "004", "0.15", null, null);
        writer.addDataRow("Country Infomation", "CANADA", "005", ".2", ".2", ".2");
        writer.addDataRow("Country Infomation", "SPAIN", "006", ".03", ".03", ".03");
        File file = new File(FILE);
        file.getParentFile().mkdirs();
        writer.write(file);
        assertTrue(file.exists());
        SpreadsheetReader reader = new SpreadsheetReader(file);
        List<String> headers = reader.getHeaders("Country Infomation");
        System.out.println(headers);
        assertTrue(headers.size() == HEADERS.length);
        List<Map<String, Object>> data = reader.getData("Country Infomation");
        assertTrue(data.size() == 6);
    }
}
