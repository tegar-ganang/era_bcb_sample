package com.luxoft.fitpro.plugin.wizards.importexport;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import com.luxoft.fitpro.plugin.messages.PluginMessages;
import fit.Parse;
import fitlibrary.runner.CustomRunner;

/**
 * An operation which does the actual work of copying objects from the local file system into the workspace.
 */
public class ImportOperation extends WorkspaceModifyOperation {

    private List<IPath> targets = new ArrayList<IPath>();

    private IPath sourcePath;

    private IPath targetPath;

    private boolean parseHtml;

    private boolean parseXls;

    public ImportOperation(String source, String target, boolean html, boolean xls) {
        this.sourcePath = new Path(source);
        this.targetPath = new Path(target);
        this.parseHtml = html;
        this.parseXls = xls;
    }

    private FileFilter fileFilter = new FileFilter() {

        public boolean accept(File file) {
            return file.isDirectory() || (parseHtml && (file.getName().endsWith(".htm") || file.getName().endsWith(".html")) || (parseXls && file.getName().endsWith(".xls")));
        }
    };

    protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        monitor.beginTask(PluginMessages.getMessage("fit.runner.importing_operation"), 1000);
        monitor.worked(10);
        initTargets(new File(sourcePath.toOSString()), monitor);
        if (targets.size() == 0) {
            throw new InvocationTargetException(new FileNotFoundException(PluginMessages.getMessage("fit.runner.non_files_was_found_to_import")));
        }
        copyTargets(monitor);
        monitor.done();
    }

    private void initTargets(File root, IProgressMonitor monitor) {
        File[] files = root.listFiles(fileFilter);
        if (files != null) {
            checkCancel(monitor);
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    initTargets(files[i], monitor);
                } else {
                    targets.add(new Path(files[i].toString()));
                }
            }
        }
    }

    private void copyTargets(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
        ContainerGenerator generator = new ContainerGenerator(targetPath);
        IContainer container = generator.generateContainer(monitor);
        IWorkspace workspace = container.getWorkspace();
        for (IPath targ : targets) {
            checkCancel(monitor);
            monitor.subTask(targ.toOSString());
            monitor.worked(10);
            String[] segments = (targ.removeFirstSegments(sourcePath.segmentCount())).segments();
            IPath destination = targetPath;
            for (int i = 0; i < segments.length - 1; ++i) {
                destination = destination.append(segments[i]);
                if (!workspace.getRoot().getFolder(destination).exists()) {
                    workspace.getRoot().getFolder(destination).create(true, true, monitor);
                }
            }
            destination = destination.append(segments[segments.length - 1]);
            destination = destination.removeFileExtension();
            destination = destination.addFileExtension("fit");
            try {
                if (targ.getFileExtension().equalsIgnoreCase("htm") || targ.getFileExtension().equalsIgnoreCase("html")) {
                    createFromHtml(workspace, targ, destination, monitor);
                } else if (targ.getFileExtension().equalsIgnoreCase("xls")) {
                    createFromXls(workspace, targ, destination, monitor);
                }
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    private void createFromHtml(IWorkspace workspace, IPath targ, IPath destination, IProgressMonitor monitor) throws FileNotFoundException, CoreException {
        FileInputStream stream = new FileInputStream(targ.toOSString());
        writeFile(workspace, stream, destination, monitor);
    }

    private void createFromXls(IWorkspace workspace, IPath targ, IPath destination, IProgressMonitor monitor) throws Exception {
        StreamBinder binder = new StreamBinder(targ.toOSString());
        InputStream istream = binder.getStream();
        binder.start();
        writeFile(workspace, istream, destination, monitor);
        istream.close();
        if (binder.isError()) {
            throw new RuntimeException("Couldn't import Excel file: " + targ.toOSString(), binder.getThrowable());
        }
    }

    private void writeFile(IWorkspace workspace, InputStream stream, IPath destination, IProgressMonitor monitor) throws CoreException {
        if (!workspace.getRoot().getFile(destination).exists()) {
            workspace.getRoot().getFile(destination).create(stream, true, monitor);
        } else {
            workspace.getRoot().getFile(destination).setContents(stream, true, false, monitor);
        }
    }

    private void checkCancel(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    private static class StreamBinder extends Thread {

        private PipedOutputStream ostream;

        private PipedInputStream istream;

        private String fileName;

        private boolean error = false;

        private Throwable throwable;

        public StreamBinder(String fileName) throws IOException {
            this.fileName = fileName;
            this.ostream = new PipedOutputStream();
            this.istream = new PipedInputStream(ostream);
        }

        public InputStream getStream() {
            return istream;
        }

        public boolean isError() {
            return error;
        }

        public void run() {
            try {
                doBind();
            } catch (Throwable t) {
                error = true;
                throwable = t;
            }
        }

        private void doBind() throws IOException, InterruptedException {
            PipedWriter writer = null;
            PipedReader reader = null;
            WriterStreamBinder binder = null;
            try {
                File xlsFile = new File(fileName);
                Parse parse = new ExcelSheetCollector().collectTable(xlsFile);
                writer = new PipedWriter();
                reader = new PipedReader(writer);
                binder = new WriterStreamBinder(reader, ostream);
                binder.start();
                parse.print(new PrintWriter(writer));
            } finally {
                if (writer != null) {
                    writer.close();
                }
                if (binder != null) {
                    binder.join();
                    if (!error) {
                        error = binder.isError();
                    }
                }
                ostream.close();
                if (reader != null) {
                    reader.close();
                }
            }
        }

        public Throwable getThrowable() {
            return this.throwable;
        }
    }

    private static class WriterStreamBinder extends Thread {

        private Reader in;

        private OutputStream out;

        private boolean error = false;

        public boolean isError() {
            return error;
        }

        public WriterStreamBinder(Reader in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                int ch;
                while ((ch = in.read()) != -1) {
                    out.write(ch);
                }
            } catch (IOException e) {
                error = true;
            }
        }
    }

    private static class ExcelSheetCollector {

        private boolean tableStarted = false;

        public Parse collectTable(File setUpFile) throws IOException {
            CustomRunner runner = collect(setUpFile, null, null, null);
            return runner.getTables();
        }

        private CustomRunner collect(File inFile, File reportFile, Parse setUpTables, Parse tearDownTables) throws IOException {
            CustomRunner runner = new CustomRunner(inFile.getName(), inFile, reportFile);
            if (setUpTables != null) {
                runner.addTables(setUpTables);
            }
            FileInputStream fileInputStream = new FileInputStream(inFile);
            HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
            String preText = collectTables(runner, workbook);
            fileInputStream.close();
            if (tearDownTables != null) {
                tearDownTables.leader += preText;
                runner.addTables(tearDownTables);
            } else if (!preText.equals("")) {
                runner.addTableTrailer(preText);
            }
            return runner;
        }

        private String collectTables(CustomRunner runner, HSSFWorkbook workbook) {
            HSSFSheet sheet = workbook.getSheetAt(0);
            String preText = "";
            for (Iterator<HSSFRow> it = sheet.rowIterator(); it.hasNext(); ) {
                HSSFRow row = (HSSFRow) it.next();
                HSSFCell[] cells = getCells(row);
                String[] borderedCellValues = getBorderedCellValues(cells, workbook);
                if (borderedCellValues.length > 0) {
                    addRow(runner, borderedCellValues, preText);
                    preText = "";
                } else {
                    String text = allText(cells, workbook) + "\n";
                    if (preText.equals("") && !text.equals("")) {
                        preText = text;
                    } else {
                        preText += "<BR>" + text;
                    }
                }
            }
            return preText;
        }

        private HSSFCell[] getCells(HSSFRow row) {
            ArrayList<HSSFCell> cellList = new ArrayList<HSSFCell>();
            for (Iterator<HSSFCell> r = row.cellIterator(); r.hasNext(); ) {
                HSSFCell cell = (HSSFCell) r.next();
                cellList.add(cell);
            }
            return (HSSFCell[]) cellList.toArray(new HSSFCell[0]);
        }

        private String[] getBorderedCellValues(HSSFCell[] cells, HSSFWorkbook workbook) {
            List<String> list = new ArrayList<String>();
            int cellNo = 0;
            while (cellNo < cells.length) {
                HSSFCell cell = cells[cellNo++];
                if (leftBordered(cell)) {
                    list.add(format(cell, workbook));
                    break;
                }
            }
            while (cellNo < cells.length) {
                HSSFCell cell = cells[cellNo++];
                if (leftBordered(cell)) {
                    list.add(format(cell, workbook));
                }
            }
            String[] result = new String[list.size()];
            int i = 0;
            for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
                result[i++] = (String) it.next();
            }
            return result;
        }

        private boolean leftBordered(HSSFCell cell) {
            if (cell == null) {
                return false;
            }
            return cell.getCellStyle().getBorderLeft() > 0;
        }

        private String format(HSSFCell cell, HSSFWorkbook workbook) {
            if (cell == null) {
                return "";
            }
            String value = value(cell);
            HSSFCellStyle style = cell.getCellStyle();
            HSSFFont font = workbook.getFontAt(style.getFontIndex());
            if (font.getItalic()) {
                value = tag("i", value);
            }
            if (font.getBoldweight() > 400) {
                value = tag("b", value);
            }
            if (font.getUnderline() > 0) {
                value = tag("u", value);
            }
            if (font.getFontHeight() >= 480) {
                value = tag("h1", value);
            } else if (font.getFontHeight() >= 280) {
                value = tag("h2", value);
            } else if (font.getFontHeight() > 200) {
                value = tag("h3", value);
            }
            return value;
        }

        private String tag(String tag, String string) {
            return "<" + tag + ">" + string + "</" + tag + ">";
        }

        private String value(HSSFCell cell) {
            switch(cell.getCellType()) {
                case HSSFCell.CELL_TYPE_BLANK:
                    return "";
                case HSSFCell.CELL_TYPE_BOOLEAN:
                    return "" + cell.getBooleanCellValue();
                case HSSFCell.CELL_TYPE_ERROR:
                    return "ERROR";
                case HSSFCell.CELL_TYPE_FORMULA:
                    if (Double.isNaN(cell.getNumericCellValue())) {
                        try {
                            return "" + cell.getBooleanCellValue();
                        } catch (NumberFormatException ex) {
                            return cell.getRichStringCellValue().getString();
                        }
                    }
                    return number(cell);
                case HSSFCell.CELL_TYPE_NUMERIC:
                    return number(cell);
                case HSSFCell.CELL_TYPE_STRING:
                    return cell.getRichStringCellValue().getString();
                default:
                    return "UNKNOWN";
            }
        }

        private String number(HSSFCell cell) {
            double value = cell.getNumericCellValue();
            if (((int) value) == value) {
                return "" + ((int) value);
            }
            return "" + value;
        }

        private String allText(HSSFCell[] cells, HSSFWorkbook workbook) {
            String result = format(cells[0], workbook);
            for (int i = 1; i < cells.length; i++) {
                String value = format(cells[i], workbook);
                if (!value.equals("")) {
                    result += " " + value;
                }
            }
            return result;
        }

        private void addRow(CustomRunner runner, String[] cells, String preText) {
            if (tableStarted && preText.equals("")) {
                runner.addRow(cells);
            } else {
                runner.addTable(cells, preText);
                tableStarted = true;
            }
        }
    }
}
