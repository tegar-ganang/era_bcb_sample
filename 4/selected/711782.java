package net.sf.cclearly.ui.widgets;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import net.sf.cclearly.entities.Task;
import net.sf.cclearly.logic.TaskPlanDAO;
import za.dats.util.I18nProvider;
import za.dats.util.injection.Dependant;
import za.dats.util.injection.Inject;
import za.dats.util.injection.Injector;

@Dependant
public class CSVExporter {

    @Inject
    TaskPlanDAO taskPlanDao;

    private boolean approve = false;

    private File saveFile;

    private JFrame exportFrame;

    public CSVExporter() {
        Injector.inject(this);
    }

    public void showDialog(JComponent parent) {
        exportFrame = new JFrame();
        exportFrame.add(new JLabel("       Exporting..."));
        exportFrame.setSize(200, 80);
        exportFrame.setLocationRelativeTo(parent);
        exportFrame.setVisible(true);
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Export Tasks as CSV File");
        saveChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(".csv")) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Comma Delimited Files (*.csv)";
            }
        });
        saveChooser.setSelectedFile(new File("export.csv"));
        if (saveChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveFile = saveChooser.getSelectedFile();
            if (saveFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(parent, saveFile.getName() + " already exists, overwrite?", "CSV Export", JOptionPane.YES_NO_OPTION);
                if (overwrite == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            approve = true;
        }
    }

    public void startExport(JComponent parent, List<Task> taskList) {
        if (!approve) {
            exportFrame.setVisible(false);
            return;
        }
        try {
            PrintWriter writer = new PrintWriter(saveFile);
            writeCSVHeader(writer);
            for (Task task : taskList) {
                writeCSVTask(writer, task);
            }
            writer.close();
            int openFile = JOptionPane.showConfirmDialog(parent, "Tasks exported successfully, would you like to open the file?", "CSV Export", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (openFile == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().open(saveFile);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(parent, "Could not open file: " + e.getMessage(), "CSV Export", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(parent, "Could not write to file: " + e.getMessage(), "CSV Export", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            exportFrame.setVisible(false);
        }
    }

    private void writeCSVHeader(PrintWriter writer) {
        List<String> elements = new LinkedList<String>();
        elements.add("Title");
        elements.add("Requestor");
        elements.add("Actioner");
        elements.add("Priority");
        elements.add("Est. Time");
        elements.add("Folders");
        elements.add("Status");
        elements.add("Date Added");
        elements.add("Target Date");
        elements.add("Date Archived");
        elements.add("Time Planned");
        writeCSVArray(writer, elements);
    }

    private void writeCSVTask(PrintWriter writer, Task task) {
        List<String> elements = new LinkedList<String>();
        elements.add(task.getTitle());
        elements.add(task.getRequestor() == null ? "" : task.getRequestor().toString());
        elements.add(task.getActioner() == null ? "" : task.getActioner().toString());
        elements.add(task.getPriority().getName());
        elements.add(task.getEstimatedTime().toString());
        elements.add(task.getFolderString());
        elements.add(task.getStatus().getName());
        elements.add(I18nProvider.dateToMedium(task.getDateAdded()));
        elements.add(I18nProvider.dateToMedium(task.getTargetDate()));
        elements.add(I18nProvider.dateToMedium(task.getDateArchived()));
        long planned = taskPlanDao.getPlannedTime(task);
        String plannedTime = "" + planned;
        elements.add(plannedTime);
        writeCSVArray(writer, elements);
    }

    private void writeCSVArray(PrintWriter writer, List<String> elements) {
        boolean first = true;
        for (String string : elements) {
            if (first) {
                first = false;
            } else {
                writer.write(",");
            }
            writer.write("\"" + csvEscape(string) + "\"");
        }
        writer.println();
    }

    private String csvEscape(String string) {
        return string.replaceAll("[\r\n]", "").replaceAll("\"", "\"\"");
    }
}
