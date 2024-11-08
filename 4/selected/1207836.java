package test.org.openxml4j.document.word.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;
import org.openxml4j.document.word.CustomTab;
import org.openxml4j.document.word.CustomTabSet;
import org.openxml4j.document.word.CustomTabStyle;
import org.openxml4j.document.word.Paragraph;
import org.openxml4j.document.word.ParagraphBuilder;
import org.openxml4j.document.word.ParagraphIndentation;
import org.openxml4j.document.word.ParagraphSpacingPredefined;
import org.openxml4j.document.word.RunBuilder;
import org.openxml4j.document.word.UnderlineStyle;
import org.openxml4j.document.word.WordDocument;
import org.openxml4j.exceptions.OpenXML4JException;
import org.openxml4j.opc.Package;
import org.openxml4j.opc.PackageAccess;
import test.junitx.framework.OpenXmlAssert;
import test.org.openxml4j.document.AbstractOpenXml4JTestClass;

public class TestDocument extends AbstractOpenXml4JTestClass {

    public void testSetDefaultParagraphName() {
        String fileName = "default_paragraph_style_name.docx";
        String fileInput = inputDirectory + "DIN_X2.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.setStyleForParagraphs();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    /**
	 * same est as testRemoveWriteEnableTags but we do not save the document but keep it in memory
	 */
    public void testRemoveWriteEnableTagsWithDocumentInMemory() {
        String fileName = "read_only_with_write_parts_removed_as_byte.docx";
        String fileInput = inputDirectory + "read_only_with_write_parts.docx";
        String expectedResult = expectedDirectory + "read_only_with_write_parts_removed.docx";
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.removeWriteEnabledTags();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assertTrue(docxSource1.save(outputStream));
            byte fileAsByte[] = outputStream.toByteArray();
            FileOutputStream file = new FileOutputStream(new File(outputDirectory + fileName));
            file.write(fileAsByte);
            file.close();
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("failed" + e.getMessage());
        }
    }

    public void testRemoveWriteEnableTags() {
        String fileName = "read_only_with_write_parts_removed.docx";
        String fileInput = inputDirectory + "read_only_with_write_parts.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.removeWriteEnabledTags();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("failed" + e.getMessage());
        }
    }

    /**
	 * same as testStripReadOnlyParagraph but with a more complex file as input
	 */
    public void testStripReadOnlyKeepTextImageAndTable() {
        String fileName = "write_enable_text_table_image.docx";
        String fileInput = inputDirectory + "write_enable_text_table_image.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("failed" + e.getMessage());
        }
    }

    public void testReadOnlySettingOverwriteDocument() {
        String fileName = "read_only_activated_in_xml.docx";
        String fileInput = inputDirectory + "read_only_inactivated_in_xml.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            assertTrue(docxSource1.setDocumentAsReadOnly(true));
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    public void testStripReadOnlyParagraph() {
        String fileName = "read_only_stripped.docx";
        String fileInput = inputDirectory + "read_only_text_with_write_enable.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    public void testReadOnlyWithWriteEnabledPartsDocument() {
        String fileName = "ReadOnlyWithWriteEnabledParts.docx";
        String fileInput = inputDirectory + "empty.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            assertTrue(docxSource1.setDocumentAsReadOnly(true));
            ParagraphBuilder paraAsReadOnly = new ParagraphBuilder();
            paraAsReadOnly.setBold(true);
            ParagraphBuilder paraBuilderWriteEnabled = new ParagraphBuilder();
            paraBuilderWriteEnabled.setAllowEditionInReadOnlyDoc(true);
            Paragraph para = paraAsReadOnly.newParagraph();
            para.addTextAsRunWithParagraphSetting("this text is read only");
            docxSource1.appendParagraph(para);
            para = paraAsReadOnly.newParagraph();
            para.addTextAsRunWithParagraphSetting("this text is also read only");
            docxSource1.appendParagraph(para);
            para = paraBuilderWriteEnabled.newParagraph();
            para.addTextAsRunWithParagraphSetting("this text can be changed");
            docxSource1.appendParagraph(para);
            para = paraBuilderWriteEnabled.newParagraph();
            para.addTextAsRunWithParagraphSetting("this text can be ALSO changed");
            docxSource1.appendParagraph(para);
            para = paraAsReadOnly.newParagraph();
            para.addTextAsRunWithParagraphSetting("this text is also read only");
            docxSource1.appendParagraph(para);
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    public void testParagraphNumberingStyle() {
        String fileName = "empty_with_numbering_xml_and_style_defined.docx";
        String fileInput = inputDirectory + fileName;
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            ParagraphBuilder paraBuilderTitle = new ParagraphBuilder();
            paraBuilderTitle.setBold(true);
            paraBuilderTitle.setSpacing(ParagraphSpacingPredefined.PARAGRAPH_SPACING_0_PT_UP_6_PT_DOWN);
            paraBuilderTitle.setParagraphRefInNumberingXml(1);
            paraBuilderTitle.setParagraphNumberShift(0);
            paraBuilderTitle.setParagraphStyleName("chapterNumbering");
            ParagraphBuilder paraBuilderNormalText = new ParagraphBuilder();
            ParagraphBuilder paraBuilderTitleLevel2 = new ParagraphBuilder();
            paraBuilderTitleLevel2.setSpacing(ParagraphSpacingPredefined.PARAGRAPH_SPACING_12_PT_UP_6_PT_DOWN);
            paraBuilderTitleLevel2.setUnderline(UnderlineStyle.SINGLE);
            paraBuilderTitleLevel2.setParagraphRefInNumberingXml(1);
            paraBuilderTitleLevel2.setParagraphNumberShift(1);
            ParagraphBuilder paraBuilderTitleLevel3 = new ParagraphBuilder();
            paraBuilderTitleLevel3.setItalic(true);
            paraBuilderTitleLevel3.setParagraphRefInNumberingXml(1);
            paraBuilderTitleLevel3.setParagraphNumberShift(2);
            Paragraph para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("headline");
            docxSource1.appendParagraph(para);
            para = paraBuilderNormalText.newParagraph();
            para.addTextAsRunWithParagraphSetting("normal text");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("second para top level");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("3rd para top level");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel2.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 2nd level->first");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel2.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 2nd level->2nd");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel2.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 2nd level->3rd");
            docxSource1.appendParagraph(para);
            para = paraBuilderNormalText.newParagraph();
            para.addTextAsRunWithParagraphSetting("2nd normal text");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel3.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 3rd level->1");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel3.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 3rd level->2");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitleLevel3.newParagraph();
            para.addTextAsRunWithParagraphSetting("para 3rd level->3");
            docxSource1.appendParagraph(para);
            para = paraBuilderNormalText.newParagraph();
            para.addTextAsRunWithParagraphSetting("3rd normal text");
            docxSource1.appendParagraph(para);
            para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("4th para top level");
            docxSource1.appendParagraph(para);
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    /**
	 * just read a file and save it
	 * Then check that the output is the same than input
	 */
    public void testTestInputOutput() {
        String fileName = "file_with_invalid_uri_in_setting_xml.docx";
        String file1 = inputDirectory + fileName;
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(file1);
            File destFile = new File(outputDirectory + fileName);
            if (logger.isDebugEnabled()) {
                logger.debug("reading and saving" + file1);
            }
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + file1);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + file1);
        }
    }

    public void testAddParagraph() {
        String fileName = "paragraphAdded.docx";
        String fileInput = inputDirectory + "empty.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            ParagraphBuilder paraBuilderTitle = new ParagraphBuilder();
            paraBuilderTitle.setBold(true);
            ParagraphBuilder paraBuilderNormalText = new ParagraphBuilder();
            Paragraph para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("headline");
            docxSource1.appendParagraph(para);
            para = paraBuilderNormalText.newParagraph();
            para.addTextAsRunWithParagraphSetting("introduction");
            docxSource1.appendParagraph(para);
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    public void testStripBeforeAndAfterWriteEnableText() {
        String fileName = "read_only_strippedBeforeAndAfter.docx";
        String fileInput = inputDirectory + "read_only_with_table_and_text.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    /**
	 * check that we also remove images in a stripped read only part
	 */
    public void testStripReadOnlyRemoveAlsoImages() {
        String fileName = "read_only_strippedImagesRemoved.docx";
        String fileInput = inputDirectory + "read_only_with_image_inside.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    public void testStripReadOnlyWithMathFormula() {
        String fileName = "stripedWithformula.docx";
        String fileInput = inputDirectory + "read_only_with_math_formula.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    /**
	 * strip text with read only containing OLE
	 * <p>Note currently we do not get rid of unused OLE binary (OLE that were in the stripped part)</p>
	 * <p>They are still stored in word\embeddings </p>
	 *TODO correct it (see description above)
	 */
    public void testStripReadOnlyWithOle() {
        String fileName = "stripedWithOle.docx";
        String fileInput = inputDirectory + "diagramn_and_formula.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            docxSource1.stripReadOnlyPartOfDocument();
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    /**
	 * check that we also remove images in a stripped read only part
	 */
    public void testStripReadOnlyKeepOnlyOneWriteEnablePart() {
        String fileInput = inputDirectory + "write_enable_parts_3_items.docx";
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            {
                Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
                WordDocument docxSource1 = new WordDocument(packSource1);
                docxSource1.stripReadOnlyPartOfDocument(1);
                String fileName = "write_enable_part_1.docx";
                String expectedResult = expectedDirectory + fileName;
                File destFile = new File(outputDirectory + fileName);
                assertTrue(docxSource1.save(destFile));
                OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
            }
            {
                Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
                WordDocument docxSource1 = new WordDocument(packSource1);
                docxSource1.stripReadOnlyPartOfDocument(2);
                String fileName = "write_enable_part_2.docx";
                String expectedResult = expectedDirectory + fileName;
                File destFile = new File(outputDirectory + fileName);
                assertTrue(docxSource1.save(destFile));
                OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
            }
            {
                Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
                WordDocument docxSource1 = new WordDocument(packSource1);
                docxSource1.stripReadOnlyPartOfDocument(3);
                String fileName = "write_enable_part_3.docx";
                String expectedResult = expectedDirectory + fileName;
                File destFile = new File(outputDirectory + fileName);
                assertTrue(docxSource1.save(destFile));
                OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
            }
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("fail" + e.getMessage());
        }
    }

    public void testAddParagraphWithTabAndIndentation() {
        String fileName = "paragraphAddedWithTabAndIndentation.docx";
        String fileInput = inputDirectory + "empty.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            ParagraphBuilder paraBuilderTitle = new ParagraphBuilder();
            paraBuilderTitle.setBold(true);
            CustomTab tab1 = new CustomTab(851, CustomTabStyle.LEFT);
            CustomTab tab2 = new CustomTab(1000, CustomTabStyle.LEFT);
            CustomTabSet tabSet = new CustomTabSet();
            tabSet.addCustomTab(tab1);
            tabSet.addCustomTab(tab2);
            paraBuilderTitle.setCustomTabSet(tabSet);
            ParagraphIndentation indent = new ParagraphIndentation();
            indent.setLeft(567);
            paraBuilderTitle.setIndentation(indent);
            ParagraphBuilder paraBuilderNormalText = new ParagraphBuilder();
            ParagraphIndentation indentMore = new ParagraphIndentation();
            indentMore.setLeft(1500);
            paraBuilderNormalText.setIndentation(indentMore);
            Paragraph para = paraBuilderTitle.newParagraph();
            para.addTextAsRunWithParagraphSetting("indent and tab");
            docxSource1.appendParagraph(para);
            para = paraBuilderNormalText.newParagraph();
            para.addTextAsRunWithParagraphSetting("indent more without tab");
            docxSource1.appendParagraph(para);
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }

    public void testSubscriptAndSuperscriptRuns() {
        String fileName = "SubscriptAndSuperscriptRuns.docx";
        String fileInput = inputDirectory + "empty.docx";
        String expectedResult = expectedDirectory + fileName;
        ZipFile zipFileSource1;
        try {
            zipFileSource1 = new ZipFile(fileInput);
            Package packSource1 = Package.open(zipFileSource1, PackageAccess.ReadWrite);
            WordDocument docxSource1 = new WordDocument(packSource1);
            ParagraphBuilder paraBuilder = new ParagraphBuilder();
            paraBuilder.setBold(true);
            Paragraph paraTerminaison = paraBuilder.newParagraph();
            List listOfRuns = RunBuilder.createRunsWithVerticalAlignment("T" + RunBuilder.SUPERSCRIPT_SYMBOL + "2" + RunBuilder.SUPERSCRIPT_SYMBOL, paraTerminaison.getCharacterFormat());
            paraTerminaison.addRuns(listOfRuns);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("kl" + RunBuilder.SUBSCRIPT_SYMBOL + "30" + RunBuilder.SUBSCRIPT_SYMBOL, paraTerminaison.getCharacterFormat());
            paraTerminaison.addRuns(listOfRuns);
            Paragraph paraSpecialTerminaison = paraBuilder.newParagraph();
            paraSpecialTerminaison.setItalic(true);
            paraSpecialTerminaison.setBold(false);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("Dubettier Christian kl" + RunBuilder.SUBSCRIPT_SYMBOL + "30 and more" + RunBuilder.SUBSCRIPT_SYMBOL + RunBuilder.SUPERSCRIPT_SYMBOL + "2 or 3" + RunBuilder.SUPERSCRIPT_SYMBOL, paraSpecialTerminaison.getCharacterFormat());
            paraSpecialTerminaison.addRuns(listOfRuns);
            Paragraph paraSpecialStartWithSpecialChar1 = paraBuilder.newParagraph();
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment(RunBuilder.SUBSCRIPT_SYMBOL + "15" + RunBuilder.SUBSCRIPT_SYMBOL, paraSpecialStartWithSpecialChar1.getCharacterFormat());
            paraSpecialStartWithSpecialChar1.addRuns(listOfRuns);
            Paragraph paraSpecialStartWithSpecialChar2 = paraBuilder.newParagraph();
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment(RunBuilder.SUPERSCRIPT_SYMBOL + "30" + RunBuilder.SUPERSCRIPT_SYMBOL, paraSpecialStartWithSpecialChar2.getCharacterFormat());
            paraSpecialStartWithSpecialChar2.addRuns(listOfRuns);
            Paragraph para = paraBuilder.newParagraph();
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("T" + RunBuilder.SUPERSCRIPT_SYMBOL + "2" + RunBuilder.SUPERSCRIPT_SYMBOL, para.getCharacterFormat());
            para.addRuns(listOfRuns);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment(" kl" + RunBuilder.SUBSCRIPT_SYMBOL + "30", para.getCharacterFormat());
            para.addRuns(listOfRuns);
            Paragraph paraSpecialCase = paraBuilder.newParagraph();
            paraSpecialCase.setItalic(true);
            paraSpecialCase.setBold(false);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("Dubettier Christian", paraSpecialCase.getCharacterFormat());
            paraSpecialCase.addRuns(listOfRuns);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment(RunBuilder.SUBSCRIPT_SYMBOL + "300", paraSpecialCase.getCharacterFormat());
            paraSpecialCase.addRuns(listOfRuns);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment(RunBuilder.SUPERSCRIPT_SYMBOL + "30 or 400", paraSpecialCase.getCharacterFormat());
            paraSpecialCase.addRuns(listOfRuns);
            Paragraph paraMultiple = paraBuilder.newParagraph();
            paraMultiple.setBold(false);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("kl" + RunBuilder.SUBSCRIPT_SYMBOL + "30" + RunBuilder.SUPERSCRIPT_SYMBOL + "2", paraMultiple.getCharacterFormat());
            paraMultiple.addRuns(listOfRuns);
            Paragraph paraMultiple2 = paraBuilder.newParagraph();
            paraMultiple2.setItalic(true);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("kl" + RunBuilder.SUBSCRIPT_SYMBOL + "30" + RunBuilder.SUBSCRIPT_SYMBOL + RunBuilder.SUPERSCRIPT_SYMBOL + "2" + RunBuilder.SUPERSCRIPT_SYMBOL, paraMultiple.getCharacterFormat());
            paraMultiple2.addRuns(listOfRuns);
            Paragraph paraInText = paraBuilder.newParagraph();
            paraInText.setBold(false);
            listOfRuns = RunBuilder.createRunsWithVerticalAlignment("this is a paragraph kl" + RunBuilder.SUPERSCRIPT_SYMBOL + "2" + RunBuilder.SUPERSCRIPT_SYMBOL + " t" + RunBuilder.SUBSCRIPT_SYMBOL + "30" + RunBuilder.SUBSCRIPT_SYMBOL + " end of para", paraMultiple.getCharacterFormat());
            paraInText.addRuns(listOfRuns);
            docxSource1.appendParagraph(paraTerminaison);
            docxSource1.appendParagraph(paraSpecialTerminaison);
            docxSource1.appendParagraph(paraSpecialStartWithSpecialChar1);
            docxSource1.appendParagraph(paraSpecialStartWithSpecialChar2);
            docxSource1.appendParagraph(para);
            docxSource1.appendParagraph(paraSpecialCase);
            docxSource1.appendParagraph(paraMultiple);
            docxSource1.appendParagraph(paraMultiple2);
            docxSource1.appendParagraph(paraInText);
            File destFile = new File(outputDirectory + fileName);
            assertTrue(docxSource1.save(destFile));
            OpenXmlAssert.assertEquals(new File(outputDirectory + fileName), new File(expectedResult));
        } catch (IOException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        } catch (OpenXML4JException e) {
            logger.error(e);
            fail("cannot open:" + fileInput);
        }
    }
}
