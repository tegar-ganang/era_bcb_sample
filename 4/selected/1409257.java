package uk.org.sgj.YAT.ImportExport;

import uk.org.sgj.YAT.*;
import com.csvreader.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import uk.org.sgj.SGJNifty.FontUtils.FontAndColor;
import uk.org.sgj.YAT.Tests.ChapterIncludedInTest;
import uk.org.sgj.YAT.Tests.VocabTestDefinition;
import uk.org.sgj.YAT.Tests.VocabTestGroup;

public class ExportToCSV {

    private YATProject project;

    private VocabTestGroup testGroup;

    private File exportedProjectName;

    public File getExportedFileName() {
        return (exportedProjectName);
    }

    public ExportToCSV(YATProject p, VocabTestGroup v) {
        project = p;
        testGroup = v;
    }

    public ExportToCSV(YATProject p) {
        project = p;
        testGroup = null;
    }

    private File selectFile(File lastFile) {
        File selectedFile = uk.org.sgj.SGJNifty.Files.FileUtils.selectFileToSave("YAT vocab sets", "yatcsv", lastFile, lastFile);
        return (selectedFile);
    }

    public boolean saveProjectAs(File lastFile, boolean convertingData) {
        boolean saved = false;
        File file = selectFile(lastFile);
        if (null != file) {
            saved = saveProject(file, convertingData);
        }
        return (saved);
    }

    public boolean saveProject(File file, boolean convertingData) {
        boolean saved = false;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            CsvWriter csvw = new CsvWriter(osw, ',');
            writeToCSVFile(csvw);
            saved = true;
            exportedProjectName = file;
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Trying to write to the following file failed.\n" + file + "\n" + "The file might be read-only or it might be open in another program.\n" + "The project was not saved.\n" + "You should save the project to a different file", "Couldn't write to file!", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            System.err.println("IO " + e);
            e.printStackTrace();
        }
        return (saved);
    }

    protected static void writeFont(CsvWriter csvw, FontAndColor f) throws IOException {
        csvw.write(f.getFont().getFamily());
        csvw.write(Integer.toString(f.getFont().getStyle()));
        csvw.write(Integer.toString(f.getFont().getSize()));
        csvw.write(Integer.toString(f.getColor().getRGB()));
        csvw.endRecord();
    }

    private void writeTests(CsvWriter csvw) throws IOException {
        if (testGroup == null) {
            csvw.writeComment("There are no tests.");
        } else {
            csvw.writeComment("Here are the tests.");
            Iterator<VocabTestDefinition> testList = testGroup.getTestDefinitions();
            while (testList.hasNext()) {
                writeTestDefinition(csvw, testList.next());
            }
        }
    }

    private void writeTestDefinition(CsvWriter csvw, VocabTestDefinition thisTest) throws IOException {
        csvw.writeComment("Name of the test");
        csvw.write(thisTest.getLabel());
        csvw.endRecord();
        csvw.writeComment("Chapters");
        Vector<ChapterIncludedInTest> chv = thisTest.getChapters();
        Iterator<ChapterIncludedInTest> chs = chv.iterator();
        csvw.write("Number of chapters:");
        csvw.write(Integer.toString(chv.size()));
        csvw.endRecord();
        while (chs.hasNext()) {
            ChapterIncludedInTest ciit = chs.next();
            csvw.write(ciit.getChapter().getLabel());
            csvw.write(Boolean.toString(ciit.isIncluded()));
            csvw.write(Boolean.toString(ciit.isCurrentlyTesting()));
            csvw.endRecord();
        }
        csvw.writeComment("Test properties: first a label, then the value.");
        csvw.write("Random:");
        csvw.write(Boolean.toString(thisTest.getRandom()));
        csvw.write("Filter level:");
        csvw.write(Integer.toString(thisTest.getFilterLevel()));
        csvw.write("Direction:");
        csvw.write(Integer.toString(thisTest.getDirection()));
        csvw.write("Lower limit:");
        csvw.write(Integer.toString(thisTest.getFrequencyLowerLimit()));
        csvw.write("Upper limit:");
        csvw.write(Integer.toString(thisTest.getFrequencyUpperLimit()));
        csvw.write("Include blanks:");
        csvw.write(Boolean.toString(thisTest.isFrequencyIncludeBlanks()));
        csvw.write("Exclude known:");
        csvw.write(Boolean.toString(thisTest.isExcludeKnownWords()));
        csvw.endRecord();
    }

    private void writeFonts(CsvWriter csvw) throws IOException {
        csvw.writeComment("Here are the fonts, in four columns: family, style, size, colour.");
        YATFontSet table = project.getFontsForTableView();
        csvw.writeComment("Here is the table font");
        writeFont(csvw, table.getF());
        writeFont(csvw, table.getFC());
        writeFont(csvw, table.getT());
        writeFont(csvw, table.getTC());
        writeFont(csvw, table.getI());
        YATFontSet test = project.getFontsForTestRuns();
        csvw.writeComment("Here is the test font");
        writeFont(csvw, test.getF());
        writeFont(csvw, test.getFC());
        writeFont(csvw, test.getT());
        writeFont(csvw, test.getTC());
        writeFont(csvw, test.getI());
    }

    private void writeVersion(CsvWriter csvw) throws IOException {
        csvw.writeComment(ProjectLoad.getVersionComment());
        csvw.write(YAT.getVersion());
        csvw.endRecord();
    }

    private void writeVocab(CsvWriter csvw) throws IOException {
        csvw.writeComment("Here is the vocab, in eight columns: chapter, word, gloss, context original, context translated, notes, score, frequency");
        Vocab vocab = project.getVocab();
        Iterator<VocabChapter> chs = vocab.getChapters();
        while (chs.hasNext()) {
            VocabChapter vc = chs.next();
            writeChapter(csvw, vc);
        }
    }

    private void writeChapter(CsvWriter csvw, VocabChapter vc) throws IOException {
        Iterator<VocabPair> pairs = vc.getVocabAsIterator();
        while (pairs.hasNext()) {
            VocabPair vp = (VocabPair) pairs.next();
            writeVP(csvw, vp);
        }
    }

    private void writeVP(CsvWriter csvw, VocabPair vp) throws IOException {
        String csvStrings[] = vp.getVPAsStringArray();
        csvw.writeRecord(csvStrings, true);
    }

    private void writeToCSVFile(CsvWriter csvw) {
        try {
            writeVersion(csvw);
            writeFonts(csvw);
            writeVocab(csvw);
            writeTests(csvw);
            csvw.flush();
            csvw.close();
        } catch (FileNotFoundException e) {
            System.err.println(e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
