package test.org.openxml4j.document.word.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipFile;
import org.dom4j.Element;
import org.openxml4j.document.word.MergeStyle;
import org.openxml4j.document.word.Paragraph;
import org.openxml4j.document.word.ParagraphBuilder;
import org.openxml4j.document.word.ParagraphSpacingPredefined;
import org.openxml4j.document.word.Picture;
import org.openxml4j.document.word.UnderlineStyle;
import org.openxml4j.document.word.WordDocument;
import org.openxml4j.document.word.numbering.predefined.ChapterNumbering;
import org.openxml4j.exceptions.OpenXML4JException;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackageAccess;
import test.junitx.framework.OpenXmlAssert;
import test.org.openxml4j.document.AbstractOpenXml4JTestClass;

/**
 * @author CDubettier
 * check that merging doc is OK
 */
public class TestDocumentMerge extends AbstractOpenXml4JTestClass {

    /**
	 * paragraph name for numbering
	 */
    private static final String PARA_NUMBERING_NAME = "test_ChapterNum";

    private boolean mergeFiles(String file1, String file2, String result) throws IOException, OpenXML4JException {
        ZipFile zipFileSource1 = new ZipFile(inputDirectory + file1);
        ZipFile zipFileSource2 = new ZipFile(inputDirectory + file2);
        File destFile = new File(outputDirectory + result);
        if (logger.isDebugEnabled()) {
            logger.debug("merging" + file1 + "," + file2 + "->" + result);
        }
        Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
        WordDocument docxSource1 = new WordDocument(packSource1);
        Package packSource2 = Package.open(zipFileSource2, PackageAccess.Read);
        WordDocument docxSource2 = new WordDocument(packSource2);
        if (docxSource1.merge(docxSource2, MergeStyle.KEEP_DOC_AS_IS)) {
            return docxSource1.save(destFile);
        } else {
            return false;
        }
    }

    public void testImageDetectionInXml() {
        try {
            ZipFile zipFileSource1 = new ZipFile(inputDirectory + "image1.docx");
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocumentForTest docxSource1 = new WordDocumentForTest(packSource1);
            Element doc = docxSource1.getDocumentBodyForTest();
            List listImageNodes = Picture.getListReferenceForImagesAndOle(doc);
            assertEquals(1, listImageNodes.size());
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * merge 2 files with only text (no image). second file has bold and italic char
	 * @throws Exception
	 */
    public void testSimpleMerge() {
        try {
            String resultFilename = "resultSimple.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("first.docx", "simple.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage() + ":" + inputDirectory);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * merge images document. jpg (fist and second doc) and PNG (second doc) are used
	 * Also check paragraph numbering
	 * @throws Exception
	 */
    public void testImageMerge() {
        try {
            String resultFilename = "resultImage.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("image1.docx", "image2.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    public void testTableMerge() {
        try {
            String resultFilename = "resultTable.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("simple.docx", "table.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultTableThenSimple.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("table.docx", "simple.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultTableThenPureTable.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("table.docx", "pure_table.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultPureTableThenTable.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("pure_table.docx", "table.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultSimpleThenPureTable.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("simple.docx", "pure_table.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultPureTableThenSimple.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("pure_table.docx", "simple.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * first doc has a table, second a table with image inside
	 * @throws Exception
	 */
    public void testTableWithImage() {
        try {
            String resultFilename = "resultTableWithImage.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("table.docx", "table_with_img_inside.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultTableWithImageThenTable.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("table_with_img_inside.docx", "table.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    public void testWithDifferentParagraphFormats() {
        try {
            String resultFilename = "resultDifferentParagraphFormats.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("simple.docx", "different_paragraph_format.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultDifferentParagraphFormatsThenSimpleText.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("different_paragraph_format.docx", "simple.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resulttableWithImageThenDifferentParagraphFormats.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("table_with_img_inside.docx", "different_paragraph_format.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "resultDifferentParagraphFormatsThenTableWithImage.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("different_paragraph_format.docx", "table_with_img_inside.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
            resultFilename = "chapter_1_then_2.docx";
            expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("chapter_1.docx", "chapter_2.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    public void testMergeAllFiles() {
        List<String> listOfFilename = new ArrayList<String>();
        listOfFilename.add(inputDirectory + "first.docx");
        listOfFilename.add(inputDirectory + "image1.docx");
        listOfFilename.add(inputDirectory + "table_with_img_inside.docx");
        listOfFilename.add(inputDirectory + "table.docx");
        String resultFilename = "resultMultipleMerge.docx";
        String expectedResult = expectedDirectory + resultFilename;
        File destFile = new File(outputDirectory + resultFilename);
        try {
            assertTrue(WordDocument.mergeAllFiles(destFile, listOfFilename, MergeStyle.KEEP_DOC_AS_IS));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (Exception e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    public void testMergeAllFiles2() {
        List<String> listOfFilename = new ArrayList<String>();
        listOfFilename.add(inputDirectory + "doc_with_tab_cr_as_text.docx");
        listOfFilename.add(inputDirectory + "pure_table.docx");
        listOfFilename.add(inputDirectory + "different_paragraph_format.docx");
        listOfFilename.add(inputDirectory + "read_only_inactivated_in_xml.docx");
        listOfFilename.add(inputDirectory + "read_only_with_write_parts.docx");
        listOfFilename.add(inputDirectory + "chapter_2.docx");
        String resultFilename = "resultMultipleMerge2.docx";
        String expectedResult = expectedDirectory + resultFilename;
        File destFile = new File(outputDirectory + resultFilename);
        try {
            assertTrue(WordDocument.mergeAllFiles(destFile, listOfFilename, MergeStyle.KEEP_DOC_AS_IS));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (Exception e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    public void testMergeWithResultAsReadOnly() {
        try {
            String resultFilename = "resultMergeAsReadOnly.docx";
            String file1 = "empty.docx";
            String file2 = "read_only_inactivated_in_xml.docx";
            final String expectedResult = expectedDirectory + resultFilename;
            final String testResultFile = outputDirectory + resultFilename;
            ZipFile zipFileSource1 = new ZipFile(inputDirectory + file1);
            ZipFile zipFileSource2 = new ZipFile(inputDirectory + file2);
            File destFile = new File(testResultFile);
            if (logger.isDebugEnabled()) {
                logger.debug("merging" + file1 + "," + file2 + "->" + expectedResult);
            }
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            Package packSource2 = Package.open(zipFileSource2, PackageAccess.Read);
            WordDocument docxSource2 = new WordDocument(packSource2);
            if (docxSource1.merge(docxSource2, MergeStyle.MERGE_AS_READ_ONLY)) {
                assertTrue(docxSource1.setDocumentAsReadOnly(true));
                assertTrue(docxSource1.save(destFile));
            } else {
                fail("cannot merge");
            }
            OpenXmlAssert.assertEquals(new File(testResultFile), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage() + ":" + inputDirectory);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    private void fillEmptyDocWithChapters(WordDocument docx) {
        ChapterNumbering chapterNumber = new ChapterNumbering(PARA_NUMBERING_NAME, docx.getNumbering());
        docx.addAbstractParagraphNumbering(chapterNumber);
        ParagraphBuilder paraBuilderTitle = new ParagraphBuilder();
        paraBuilderTitle.setBold(true);
        paraBuilderTitle.setParagraphReferenceFromStyle(chapterNumber);
        paraBuilderTitle.setParagraphNumberShift(0);
        ParagraphBuilder paraBuilderChapter = new ParagraphBuilder();
        paraBuilderChapter.setParagraphReferenceFromStyle(chapterNumber);
        paraBuilderChapter.setParagraphNumberShift(1);
        paraBuilderChapter.setSpacing(ParagraphSpacingPredefined.PARAGRAPH_SPACING_12_PT_UP_6_PT_DOWN);
        paraBuilderChapter.setUnderline(UnderlineStyle.SINGLE);
        Paragraph para = paraBuilderTitle.newParagraph();
        para.addTextAsRunWithParagraphSetting("title level 0");
        docx.appendParagraph(para);
        para = paraBuilderChapter.newParagraph();
        para.addTextAsRunWithParagraphSetting("title level 1");
        docx.appendParagraph(para);
        ParagraphBuilder paraBuilderNormalText = new ParagraphBuilder();
        List paraIntroList = paraBuilderNormalText.newParagraphs("bla bla bla\nbla bla bla line 2\nline3");
        for (Iterator iter = paraIntroList.iterator(); iter.hasNext(); ) {
            Paragraph element = (Paragraph) iter.next();
            docx.appendParagraph(element);
        }
        para = paraBuilderChapter.newParagraph();
        para.addTextAsRunWithParagraphSetting("second title livel 1");
        docx.appendParagraph(para);
    }

    public void testStripAndMerge() {
        try {
            String resultFilename = "resultStripAndMerge.docx";
            String file1 = "steps.docx";
            String file2 = "empty.docx";
            final String expectedResult = expectedDirectory + resultFilename;
            final String testResultFile = outputDirectory + resultFilename;
            ZipFile zipFileSource1 = new ZipFile(inputDirectory + file1);
            ZipFile zipFileSource2 = new ZipFile(inputDirectory + file2);
            File destFile = new File(testResultFile);
            if (logger.isDebugEnabled()) {
                logger.debug("merging" + file1 + "," + file2 + "->" + expectedResult);
            }
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            Package packSource2 = Package.open(zipFileSource2, PackageAccess.ReadWrite);
            WordDocument docxSource2 = new WordDocument(packSource2);
            fillEmptyDocWithChapters(docxSource2);
            if (docxSource2.merge(docxSource1, MergeStyle.MERGE_AS_READ_ONLY)) {
                assertTrue(docxSource2.setDocumentAsReadOnly(true));
                assertTrue(docxSource2.save(destFile));
            } else {
                fail("cannot merge");
            }
            OpenXmlAssert.assertEquals(new File(testResultFile), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage() + ":" + inputDirectory);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * test merging with OLE (ex math formula)
	 */
    public void testOleMerge() {
        try {
            String resultFilename = "resultMergeWithOle.docx";
            String expectedResult = expectedDirectory + resultFilename;
            assertTrue(mergeFiles("simple.docx", "read_only_with_math_formula.docx", resultFilename));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail(e.getMessage());
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * test merging with OLE (ex math formula)
	 */
    public void testMergeWithMultipleFormula() {
        List<String> listOfFilename = new ArrayList<String>();
        listOfFilename.add(inputDirectory + "empty.docx");
        listOfFilename.add(inputDirectory + "pure_table.docx");
        listOfFilename.add(inputDirectory + "read_only_with_math_formula.docx");
        listOfFilename.add(inputDirectory + "maths.docx");
        listOfFilename.add(inputDirectory + "chapter_2.docx");
        String resultFilename = "resultMergeWithFormulas.docx";
        String expectedResult = expectedDirectory + resultFilename;
        File destFile = new File(outputDirectory + resultFilename);
        try {
            assertTrue(WordDocument.mergeAllFiles(destFile, listOfFilename, MergeStyle.KEEP_DOC_AS_IS));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (Exception e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }

    /**
	 * test merging with OLE (ex math formula)
	 *
	 * note the diagram may not appears in word on the result file.
	 * It is just a problem of display setting in word.
	 * Just use menu Display and set it to web and back to normal/page
	 */
    public void testMergeWithDiagramAndMathFormula() {
        List<String> listOfFilename = new ArrayList<String>();
        listOfFilename.add(inputDirectory + "simple.docx");
        listOfFilename.add(inputDirectory + "pure_table.docx");
        listOfFilename.add(inputDirectory + "read_only_with_math_formula.docx");
        listOfFilename.add(inputDirectory + "diagramn_and_formula.docx");
        listOfFilename.add(inputDirectory + "chapter_2.docx");
        String resultFilename = "resultMergeWithFormulaAndDiagramms.docx";
        String expectedResult = expectedDirectory + resultFilename;
        File destFile = new File(outputDirectory + resultFilename);
        try {
            assertTrue(WordDocument.mergeAllFiles(destFile, listOfFilename, MergeStyle.KEEP_DOC_AS_IS));
            OpenXmlAssert.assertEquals(new File(outputDirectory + resultFilename), new File(expectedResult));
        } catch (Exception e) {
            logger.error(e);
            fail(e.getMessage());
        }
    }
}
