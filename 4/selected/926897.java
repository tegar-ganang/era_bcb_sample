package com.fitagilifier.format;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormatTest extends TestCase {

    public void testFormatFixtureTableSingleCol() throws TableFormatError {
        String resultTableContent = FormatUtils.fixtureTable(concat(FormatTestDataProvider.sampleLinesSingleColumn));
        assertEquals("Fit test table has been formatted correctly.", concat(FormatTestDataProvider.expectedResultSingleColumn), resultTableContent);
    }

    public void testFormatFixtureTableSingleColWithAttributes() throws TableFormatError {
        String resultTableContent = FormatUtils.fixtureTable(concat(FormatTestDataProvider.sampleLinesSingleColumnWithAttributes));
        assertEquals("Fit test table has been formatted correctly.", concat(FormatTestDataProvider.expectedResultSingleColumnWithAttributes), resultTableContent);
    }

    public void testFormatFixtureTableMultipleCols() throws TableFormatError {
        String resultTableContent = FormatUtils.fixtureTable(concat(FormatTestDataProvider.sampleLinesMultipleColumns));
        assertEquals("Fit test table has been formatted correctly.", concat(FormatTestDataProvider.expectedResultMultipleColumns), resultTableContent);
    }

    private String concat(String[] strings) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            str.append(strings[i]);
        }
        return str.toString();
    }

    public void testFormatMultipleRowSingleColumn() throws TableFormatError {
        String[] resultTable = FormatUtils.fixtureRow(FormatTestDataProvider.sampleLinesSingleColumn);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultSingleColumn[0], resultTable[0]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultSingleColumn[1], resultTable[1]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultSingleColumn[2], resultTable[2]);
    }

    public void testFormatMultipleRowMultipleColumn() throws TableFormatError {
        String[] resultTable = FormatUtils.fixtureRow(FormatTestDataProvider.sampleLinesMultipleColumns);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultMultipleColumns[0], resultTable[0]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultMultipleColumns[1], resultTable[1]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultMultipleColumns[2], resultTable[2]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultMultipleColumns[3], resultTable[3]);
        assertEquals("Fit test table has been formatted correctly.", FormatTestDataProvider.expectedResultMultipleColumns[4], resultTable[4]);
    }

    public void testFormatASingleTd() {
        String resultTd = FormatUtils.singleRow(FormatTestDataProvider.sampleLinesSingleColumn[0], Arrays.asList(12));
        assertEquals("Single td correctly reformated", FormatTestDataProvider.expectedResultSingleColumn[0], resultTd);
    }

    public void testFormatNothing() {
        String resultTd = FormatUtils.singleRow(FormatTestDataProvider.sampleLinesSingleColumn[0].replaceAll("<", "-"), Arrays.asList(12));
        assertEquals("No td not reformated", FormatTestDataProvider.sampleLinesSingleColumn[0].replaceAll("<", "-"), resultTd);
        assertFalse("No td not as expected when reformated", FormatTestDataProvider.expectedResultSingleColumn[0].replaceAll("<", "-").equals(resultTd));
    }

    public void testLeftPad() {
        StringBuilder s = new StringBuilder("BAR");
        FormatUtils.leftPad(s, 'i', 5);
        assertEquals(s.toString(), "BARiiiii");
    }

    public void testLeftPadNoOp() {
        StringBuilder s = new StringBuilder("BAR");
        FormatUtils.leftPad(s, 'i', 0);
        assertEquals(s.toString(), "BAR");
    }

    public void testLeftPadNegative() {
        StringBuilder s = new StringBuilder("BAR");
        FormatUtils.leftPad(s, 'i', -2);
        assertEquals(s.toString(), "BAR");
    }

    public void testFilterMaxSizes() {
        List<Integer> maxSizes = new ArrayList<Integer>();
        FormatUtils.filterMaxSizes(maxSizes, Arrays.asList(4, 1, 2, 0, 4, 4));
        FormatUtils.filterMaxSizes(maxSizes, Arrays.asList(2, 4, 0, -1, -1, -1));
        FormatUtils.filterMaxSizes(maxSizes, Arrays.asList(2, 1, 2, 4, 4, 3));
        FormatUtils.filterMaxSizes(maxSizes, Arrays.asList(1, 3, 4, 3, 4, 3));
        assertEquals(6, maxSizes.size());
        for (int i = 0; i < 6; i++) {
            assertEquals("Expect max size remain for dimenssion " + i + " to be:", 4, maxSizes.toArray()[i]);
        }
    }

    public void testTdContentSizes() throws TableFormatError {
        List<Integer> contentSizes = FormatUtils.tdContentSizes("  <td>  </td> <td>0123456789</td><td></td>    \t<td>abcdefghijklmnopqrstuvwxyz</td>\t  ");
        assertEquals(4, contentSizes.size());
        assertEquals(new Integer(2), contentSizes.get(0));
        assertEquals(new Integer(10), contentSizes.get(1));
        assertEquals(new Integer(0), contentSizes.get(2));
        assertEquals(new Integer(26), contentSizes.get(3));
    }

    public void testTdContentSizesSmallest() throws TableFormatError {
        List<Integer> contentSizes = FormatUtils.tdContentSizes("<td></td><td></td><td></td><td></td>");
        assertEquals(4, contentSizes.size());
        assertEquals(new Integer(0), contentSizes.get(0));
        assertEquals(new Integer(0), contentSizes.get(1));
        assertEquals(new Integer(0), contentSizes.get(2));
        assertEquals(new Integer(0), contentSizes.get(3));
    }

    public void testFormatingAFileWorstCase() throws IOException, TableFormatError {
        FileWriter sampleFile = new FileWriter("test/com/fitagilifier/format/resource/SampleFitFileWorst.html");
        sampleFile.write(FormatTestDataProvider.fileContentWorst);
        sampleFile.close();
        Format.fitFile("test/com/fitagilifier/format/resource/SampleFitFileWorst.html", "test/com/fitagilifier/format/resource/SampleFitFileWorstResult.html");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertEquals("Fit file must be well formated", FormatTestDataProvider.fileContentWorstFormated, FileUtils.readFileToString(new File("test/com/fitagilifier/format/resource/SampleFitFileWorstResult.html")));
    }

    public void testFormatingAFileWithOverwriting() throws IOException, TableFormatError {
        FileWriter sampleFile = new FileWriter("test/com/fitagilifier/format/resource/SampleFitFileWorstOverwrite.html");
        sampleFile.write(FormatTestDataProvider.fileContentWorst);
        sampleFile.close();
        Format.fitFile("test/com/fitagilifier/format/resource/SampleFitFileWorstOverwrite.html", "test/com/fitagilifier/format/resource/SampleFitFileWorstOverwrite.html");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertEquals("Fit file must be well formated", FormatTestDataProvider.fileContentWorstFormated, FileUtils.readFileToString(new File("test/com/fitagilifier/format/resource/SampleFitFileWorstOverwrite.html")));
    }

    public void testFormatingAFile() throws IOException, TableFormatError {
        FileWriter sampleFile = new FileWriter("test/com/fitagilifier/format/resource/SampleFitFile.html");
        sampleFile.write(FormatTestDataProvider.fileContentStandard);
        sampleFile.close();
        Format.fitFile("test/com/fitagilifier/format/resource/SampleFitFile.html", "test/com/fitagilifier/format/resource/SampleFitFileResult.html");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertEquals("Fit file must be well formated", FormatTestDataProvider.fileContentStandardFormated, FileUtils.readFileToString(new File("test/com/fitagilifier/format/resource/SampleFitFileResult.html")));
    }

    public void testGetColspanAtt() {
        assertEquals(345, FormatUtils.getColspanAtt("<td style=\"color:blue\" colspan=\"345\">"));
        assertEquals(345, FormatUtils.getColspanAtt("<td colspan='345'>"));
        assertEquals(345, FormatUtils.getColspanAtt("<td colspan=\\'345\\'>"));
        assertEquals(3, FormatUtils.getColspanAtt("<td colspan=" + '"' + "3" + '"' + ">"));
        assertEquals(1, FormatUtils.getColspanAtt("<td>"));
        assertEquals(345, FormatUtils.getColspanAtt("<td style=\"color:blue\" colspan=\"345\" class='foo' id='bar' >"));
        assertEquals(1, FormatUtils.getColspanAtt("<td colspan=345>"));
    }

    public void testFormatAPeaceOfTextWithoutTable() throws TableFormatError {
        String peaceOfText = "lsdfj <span>dfjkdfh</span>lsdfkjksdf";
        assertEquals(peaceOfText, Format.peaceOfText(peaceOfText));
    }

    public void testFormatAPeaceOfTextWithOnlyTds() throws TableFormatError {
        String peaceOfText = "<td>dfjkdfh</td><td>jkfhhsdf</td><td>ldfdsfj</td>";
        assertEquals(peaceOfText, Format.peaceOfText(peaceOfText));
    }

    public void testFormatAPeaceOfTextWithTdsInsideTrs() throws TableFormatError {
        String peaceOfText = "<tr><td>dfjkdfh</td><td>jkfhhsdf</td><td>ldfdsfj</td></tr>" + "<tr><td>dfj</td><td>jkfhhsdf456</td><td>l</td></tr>";
        String resultExpected = "<tr><td>dfjkdfh</td><td>jkfhhsdf   </td><td>ldfdsfj</td></tr>" + "<tr><td>dfj    </td><td>jkfhhsdf456</td><td>l      </td></tr>";
        assertEquals(resultExpected, Format.peaceOfText(peaceOfText));
    }

    public void testFormatAPeaceOfTextWithTdsInsideTrsInsideTable() throws TableFormatError {
        String peaceOfText = "<table><tr><td>dfjkdfh</td><td>jkfhhsdf</td><td>ldfdsfj</td></tr>" + "<tr><td>dfj</td><td>jkfhhsdf456</td><td>l</td></tr></table>";
        String resultExpected = "<table><tr><td>dfjkdfh</td><td>jkfhhsdf   </td><td>ldfdsfj</td></tr>" + "<tr><td>dfj    </td><td>jkfhhsdf456</td><td>l      </td></tr></table>";
        assertEquals(resultExpected, Format.peaceOfText(peaceOfText));
    }

    public void testFormatAPeaceOfTextWithTdsInsideTrsInsideTableWithOtherInformations() throws TableFormatError {
        String peaceOfText = " bla bla bla bla bla bla bla bla bla <table><tr><td>dfjkdfh</td><td>jkfhhsdf</td><td>ldfdsfj</td></tr>" + "<tr><td>dfj</td><td>jkfhhsdf456</td><td>l</td></tr></table> bla bla bla bla bla bla bla bla bla ";
        String resultExpected = " bla bla bla bla bla bla bla bla bla <table><tr><td>dfjkdfh</td><td>jkfhhsdf   </td><td>ldfdsfj</td></tr>" + "<tr><td>dfj    </td><td>jkfhhsdf456</td><td>l      </td></tr></table> bla bla bla bla bla bla bla bla bla ";
        assertEquals(resultExpected, Format.peaceOfText(peaceOfText));
    }

    public void testFormatAPeaceOfTextMalformedTable() {
        String peaceOfText = "<table><tr><td>dfjkdfh</td></tr>" + "<tr><td>l</td></tr>";
        try {
            Format.peaceOfText(peaceOfText);
            fail("Expect an exception " + TableFormatError.class);
        } catch (TableFormatError tableFormatError) {
        }
    }

    public void testFormatAPeaceOfTextMalformedTd() {
        String peaceOfText = "<table><tr><td>dfjkdfh<td></tr>" + "<tr><td>l<td></tr></table>";
        try {
            Format.peaceOfText(peaceOfText);
            fail("Expect an exception " + TableFormatError.class);
        } catch (TableFormatError tableFormatError) {
        }
    }

    public void testFormatAPeaceOfTextMalformedTr() throws TableFormatError {
        String peaceOfText = "<table><tr><td>dfjkdfh</td><tr>" + "<tr><td>l</td><tr></table>";
        assertEquals(peaceOfText, Format.peaceOfText(peaceOfText));
    }
}
