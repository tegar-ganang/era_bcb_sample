package org.formaria.metrobank.support;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.prefs.Preferences;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import org.formaria.aria.Project;
import org.formaria.aria.helper.AriaUtilities;
import org.formaria.metrobank.Welcome;
import org.formaria.swing.Table;

/**
 * A helper class to export tables to the clipboard as CSV records.
 * @author luano
 */
public class ExcelExporter {

    public static final int TEXT = 0;

    public static final int XML = 1;

    public static final int HTML = 2;

    public static final int EXCEL = 3;

    public static final int WORD = 4;

    private WritableWorkbook workbook;

    private WritableSheet worksheet;

    private static File storageDir;

    private static int counter;

    private Preferences prefs;

    private Project project;

    private String tempPrintFileName, tempOpenFileName;

    private int lastFileType = -1;

    private static final String REGQUERY_UTIL = "reg query ";

    private static final String REGSTR_TOKEN = "REG_SZ";

    private static final String REGDWORD_TOKEN = "REG_DWORD";

    public ExcelExporter(Project proj) {
        project = proj;
        prefs = Preferences.userNodeForPackage(Welcome.class);
        counter = prefs.getInt("exportCounter", 1);
    }

    public void exportTableSelection(Table table, boolean doOpen, boolean doPrint) {
        String fileName = null;
        if (table != null) {
            String tempDir = project.getStartupParam("tempDir");
            if ((tempDir == null) || (tempDir.trim().length() == 0)) return;
            storageDir = AriaUtilities.getUserDirectory(tempDir);
            File outputFile = new File(storageDir, "export_" + counter + ".xls");
            prefs.putInt("exportCounter", ++counter);
            createWorksheet(outputFile, table.getName());
            int[] selRows = table.getSelectedRows();
            int numRows = selRows.length;
            if (numRows == 0) {
                numRows = table.getRowCount();
                selRows = null;
            }
            int numCols = table.getModel().getColumnCount();
            for (int j = 0; j < numCols; j++) {
                try {
                    worksheet.addCell(new Label(j, 0, table.getModel().getColumnName(j)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < numRows; i++) {
                int row = selRows == null ? i : selRows[i];
                for (int j = 0; j < numCols; j++) {
                    Object value = table.getValue(row, j);
                    if (value != null) {
                        try {
                            worksheet.addCell(new Label(j, i + 1, value.toString()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (writeWorksheet()) {
                try {
                    fileName = outputFile.getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (fileName != null) {
            if (doOpen) {
                doOpen(fileName, false, EXCEL);
            } else if (doPrint) {
                doOpen(fileName, true, EXCEL);
            }
        }
    }

    private void createWorksheet(File exportFile, String sheetName) {
        try {
            workbook = Workbook.createWorkbook(exportFile);
            if ((sheetName == null) || (sheetName.length() == 0)) sheetName = "HRAdmin";
            worksheet = workbook.createSheet(sheetName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean writeWorksheet() {
        try {
            workbook.write();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
   * Open or print a file
   * @param fileName the file name
   * @param doPrint true to print
   */
    private void doOpen(String fileName, boolean doPrint, int fileType) {
        String command = "start";
        try {
            File tempFile = null;
            String tempFileName = null;
            if (doPrint) {
                switch(fileType) {
                    case TEXT:
                        command = "NOTEPAD /P" + "\"" + fileName + "\"";
                        break;
                    case XML:
                    case HTML:
                    case EXCEL:
                    case WORD:
                        {
                            if ((tempPrintFileName == null) || (fileType != lastFileType)) {
                                tempFile = File.createTempFile("doPrint", ".bat");
                                tempPrintFileName = tempFile.getCanonicalPath();
                                lastFileType = fileType;
                                tempOpenFileName = null;
                            }
                            tempFileName = tempPrintFileName;
                            if (tempFile != null) {
                                OutputStreamWriter writer = new OutputStreamWriter(project.getBufferedOutputStream(tempFileName, false));
                                writer.write(getOfficePath(fileType, doPrint) + " %1 /mFilePrintDefault");
                                writer.flush();
                                writer.close();
                            }
                            command = "start \"" + tempFileName + "\" \"" + fileName + "\"";
                            ;
                        }
                        break;
                }
            } else {
                if ((tempOpenFileName == null) || (lastFileType != fileType)) {
                    tempFile = File.createTempFile("doOpen", ".bat");
                    tempOpenFileName = tempFile.getCanonicalPath();
                    lastFileType = fileType;
                    tempPrintFileName = null;
                }
                tempFileName = tempOpenFileName;
                if (tempFile != null) {
                    OutputStreamWriter writer = new OutputStreamWriter(project.getBufferedOutputStream(tempFileName, false));
                    writer.write("start %1 %2 %3 %4 %5 %6 %7 %8 %9");
                    writer.flush();
                    writer.close();
                }
            }
            String line;
            String cmdLine;
            if (doPrint) cmdLine = command; else cmdLine = "start \"" + tempFileName + "\" \"" + fileName + "\"";
            File targetFile = new File(fileName);
            File startDir = new File(targetFile.getParent());
            String osName = System.getProperty("os.name");
            String[] cmd = new String[3];
            if (osName.equals("Windows 95") || osName.equals("Windows 98")) {
                cmd[0] = "command.com";
                cmd[1] = "/C";
                cmd[2] = cmdLine;
            } else {
                cmd[0] = "cmd.exe";
                cmd[1] = "/C";
                cmd[2] = cmdLine;
            }
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (true) System.out.println(line);
            }
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getOfficePath(int fileType, boolean doPrint) {
        try {
            String applicationQuery = null;
            switch(fileType) {
                case EXCEL:
                    applicationQuery = REGQUERY_UTIL + "\"HKCR\\Applications\\EXCEL.EXE\\shell\\" + (doPrint ? "Print" : "Open") + "\\command";
                    break;
                case HTML:
                    if (doPrint) {
                        return "RUNDLL32.EXE MSHTML.DLL,PrintHTML";
                    } else applicationQuery = REGQUERY_UTIL + "\"HKCR\\Applications\\iexplore.exe\\shell\\open\\command";
                    break;
            }
            Process process = Runtime.getRuntime().exec(applicationQuery);
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(REGSTR_TOKEN);
            if (p == -1) return null;
            result = result.substring(p + REGSTR_TOKEN.length()).trim();
            result = result.substring(0, result.indexOf("\"", 1) + 1).trim();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    static class StreamReader extends Thread {

        private InputStream is;

        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
                ;
            }
        }

        String getResult() {
            return sw.toString();
        }
    }
}
