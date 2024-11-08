package uk.org.sgj.OHCApparatus.ImportExport;

import uk.org.sgj.OHCApparatus.*;
import uk.org.sgj.SGJNifty.Files.*;
import uk.org.sgj.OHCApparatus.Records.*;
import uk.org.sgj.OHCApparatus.Essay.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

public class OHCProjectExport {

    private OHCApparatusProject project;

    private BufferedWriter writer;

    private File exportedProjectName;

    public File getExportedFileName() {
        return (exportedProjectName);
    }

    public OHCProjectExport(OHCApparatusProject vv) {
        project = vv;
    }

    private File selectFile(File lastFile) {
        File selectedFile = FileUtils.selectFileToSave("Cite SBLHS Libraries", "oap", lastFile, lastFile);
        return (selectedFile);
    }

    private void writeLine(String str) throws IOException {
        writer.write(str);
        writer.newLine();
    }

    private void writeProjectHeader() throws IOException {
        writeLine("Oak Hill Apparatus Builder Project");
        writeLine(OHCApparatusProject.getVersion());
        writeLine("Number of entries:");
        writeLine(Long.toString(project.getTotalEntryNumber()));
    }

    private void writeProjectTail() throws IOException {
        writeLine("End of Oak Hill Apparatus Builder Project");
    }

    private void writeSingleRecord(OHCBasicRecord record) throws IOException {
        boolean noParent = false;
        if (record instanceof OHCDerivedRecord) {
            long parentIndex = ((OHCDerivedRecord) record).getParentRecordNumber();
            if (project.findRecordFromNumber(parentIndex) == null) {
                noParent = true;
            }
            writeLine("Begin derived record");
            writeLine("");
            writeLine("Based on the record with the following number");
            writeLine("" + parentIndex);
        } else {
            writeLine("Begin record");
            writeLine("");
        }
        writeLine(record.getRecordTypeString());
        long idx = record.getRecordNumber();
        writeLine(Long.toString(idx));
        writeLine("");
        Iterator<FieldData> data = record.getDataList();
        FieldData fd;
        while (data.hasNext()) {
            fd = data.next();
            writeLine(fd.getLabelString());
            writeLine(fd.getValueString());
        }
        writeLine("End record");
        writeLine("");
        if (noParent) {
            JOptionPane.showMessageDialog(null, "The project contains the following derived record, but the record it was derived from is no longer in the project.\n" + "The derived record will be saved in your project file, but will not be read when you next open the file.\n" + "Please e-mail sgj.apps@gmail.com with a copy of this message and with your project file, plus any other information that might help to debug this problem.\n" + "I'm REALLY sorry for the inconvenience!\n\n" + produceNoParentString(record), "Floating derived record!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String produceNoParentString(OHCBasicRecord record) {
        String ret = "";
        Iterator<FieldData> data = record.getDataList();
        FieldData fd;
        while (data.hasNext()) {
            fd = data.next();
            ret = ret.concat(fd.getLabelString() + "\n");
            ret = ret.concat(fd.getValueString() + "\n");
        }
        return (ret);
    }

    private void writeSingleEssay(OHCEssay essay) throws IOException {
        Iterator data = essay.getIterator();
        writeLine("Begin essay");
        writeLine("");
        writeLine(essay.toString());
        writeLine("");
        writeLine("Last saved Bibliography position");
        File last = essay.getLastSavedBibFile();
        String lastString;
        if (last != null) {
            lastString = last.toString();
        } else {
            lastString = "";
        }
        writeLine(lastString);
        writeLine("Last saved Apparatus position");
        last = essay.getLastSavedAppFile();
        if (last != null) {
            lastString = last.toString();
        } else {
            lastString = "";
        }
        writeLine(lastString);
        while (data.hasNext()) {
            OHCBasicRecord rec = (OHCBasicRecord) data.next();
            String str = rec.getRecordNumber() + "; biblio; footnotes; altcit";
            writeLine(str);
        }
        writeLine("End essay");
        writeLine("");
    }

    private void exportRecords() throws IOException {
        writeLine("Begin all records");
        writeLine("");
        Iterator rr = project.getAllRecords();
        OHCBasicRecord rec;
        while (rr.hasNext()) {
            rec = (OHCBasicRecord) rr.next();
            if (!(rec instanceof OHCDerivedRecord)) {
                writeSingleRecord(rec);
            }
        }
        writeLine("End all records");
        writeLine("");
    }

    private void exportDerivedRecords() throws IOException {
        writeLine("Begin all derived records");
        writeLine("");
        Iterator rr = project.getAllRecords();
        OHCBasicRecord rec;
        while (rr.hasNext()) {
            rec = (OHCBasicRecord) rr.next();
            if (rec instanceof OHCDerivedRecord) {
                writeSingleRecord(rec);
            }
        }
        writeLine("End all derived records");
        writeLine("");
    }

    private void exportEssays() throws IOException {
        writeLine("Begin all essays");
        writeLine("");
        Iterator rr = project.getAllEssays();
        while (rr.hasNext()) {
            writeSingleEssay((OHCEssay) rr.next());
        }
        writeLine("End all essays");
        writeLine("");
    }

    private void exportOptions() throws IOException {
        writeLine("Begin all options");
        writeLine("");
        FontPack ff = project.getFonts();
        Iterator it = ff.getEncoded();
        while (it.hasNext()) {
            writeLine((String) it.next());
        }
        writeLine("End all options");
        writeLine("");
    }

    private void exportProject() throws IOException {
        writeProjectHeader();
        exportRecords();
        exportDerivedRecords();
        exportEssays();
        exportOptions();
        writeProjectTail();
    }

    public boolean saveProjectAs(File lastFile) {
        boolean saved = false;
        File file = selectFile(lastFile);
        if (null != file) {
            saved = saveProject(file);
        }
        return (saved);
    }

    public boolean saveProject(File file) {
        boolean saved = false;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            exportProject();
            writer.close();
            saved = true;
            exportedProjectName = file;
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Trying to write to the following file failed.\n" + file + "\n" + "The file might be read-only or it might be open in another program.\n" + "The project was not saved.\n" + "You should save the project to a different file (select \"Save as...\" in the \"Project\" menu.", "Couldn't write to file!", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            System.err.println("IO " + e);
            e.printStackTrace();
        }
        return (saved);
    }
}
