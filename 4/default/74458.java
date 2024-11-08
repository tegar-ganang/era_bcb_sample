import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI.TabSelectionHandler;
import org.jfree.io.FileUtilities;
import gui.QDeleteButton;
import gui.TableController;
import gui.Ui_MainWindow;
import com.trolltech.qt.QThread;
import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.QtConcurrent;
import com.trolltech.qt.core.QAbstractAnimation.DeletionPolicy;
import com.trolltech.qt.core.QEventLoop.ProcessEventsFlag;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QTableWidgetItem;

public class Gui {

    private static Ui_MainWindow ui;

    private static Gui _instance = new Gui();

    private ExecutiveClass ceo;

    private Thread simulationThread;

    private Gui() {
    }

    public void setCEO(ExecutiveClass ceo) {
        this.ceo = ceo;
    }

    private static synchronized void createInstance() {
        if (_instance == null) _instance = new Gui();
    }

    public static Gui getInstance() {
        if (_instance == null) createInstance();
        return _instance;
    }

    public void startGui(String[] args) {
        QApplication.initialize(args);
        ui = new Ui_MainWindow();
        QMainWindow qm = new QMainWindow();
        ui.setupUi(qm);
        ui.lbl_logo_3.setPixmap(new QPixmap(("images/icon.png")));
        ui.lbl_logo.setPixmap(new QPixmap(("images/icon.png")));
        qm.setWindowIcon(new QIcon(new QPixmap("images/icon.png")));
        ui.select_algorithm.clear();
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "BubbleSort", null));
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "SelectionSort", null));
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "QuickSort", null));
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "FlipSort", null));
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "InsertionSort", null));
        ui.select_algorithm.addItem(com.trolltech.qt.core.QCoreApplication.translate("MainWindow", "HeapSort", null));
        getInstance().connectSlots();
        getInstance().setSimulationProgressBar(0);
        qm.show();
        System.out.println("curdir: " + getCurrentDir());
        QApplication.exec();
    }

    private void connectSlots() {
        ui.btn_startsimulation.released.connect(this, "startSimulation()");
        ui.spin_maxsize.valueChanged.connect(this, "setint(int)");
        ui.btn_addtask.released.connect(this, "addTask()");
        ui.btn_stopsimulation.released.connect(this, "stopSimulationThread()");
        ui.btn_openfile.released.connect(this, "openFile()");
        ui.tasklist.cellPressed.connect(this, "deleteTask(int, int)");
    }

    private void editTask(QTableWidgetItem item) {
        int row = item.row();
        String str = ui.tasklist.readItem(row, 1);
    }

    private void deleteTask(int row, int col) {
        if (col == 5) {
            ceo.deleteTask(row);
            ui.tasklist.removeRow(row);
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(chooser);
        File file = chooser.getSelectedFile();
        String filename = file.getName();
        System.out.println("open custom open file:");
        File file2 = new File(getCurrentDir() + "/customAlgorithms/" + filename);
        try {
            CopyFile(file, file2);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ui.txt_classname.setText(filename.split(".java")[0] + "");
    }

    private void startSimulation() {
        Runnable runnable2 = new Runnable() {

            @Override
            public void run() {
                ceo.startSimulation();
                ceo.showPlot();
            }
        };
        Runnable runnable3 = new Runnable() {

            @Override
            public void run() {
            }
        };
        ui.btn_stopsimulation.setEnabled(true);
        simulationThread = new Thread(runnable2);
        Thread thread3 = new Thread(runnable3);
        simulationThread.start();
        int previous = 0;
        int now = 0;
        int taskprogressdelay = 400;
        double time1 = System.currentTimeMillis();
        ;
        double time2 = 0;
        double originalTime = System.currentTimeMillis();
        while (simulationThread.isAlive()) {
            QCoreApplication.processEvents();
            now = ceo.getSimulator().getCurrentTask();
            if (previous <= now) {
                for (int r = 0; r < now; r++) {
                    ui.tasklist.changeRowBackground(r, new int[] { 193, 255, 166 });
                }
            }
            previous = now;
            ui.tasklist.changeRowBackground(ceo.getSimulator().getCurrentTask() - 1, new int[] { 255, 230, 153 });
            if ((time2 - time1) > taskprogressdelay) {
                ui.progressBar_currenttask.setValue(readCurrentTaskProgress());
                ui.progressBar_simulation.setValue(ceo.getSimulator().getCurrentTask() * 100 / (ceo.getTaskHolder().getTasks().size()));
                time1 = time2;
                double milliseconds = (System.currentTimeMillis() - originalTime);
                int seconds = (int) (milliseconds / 1000) % 60;
                int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
                int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
                String time = hours + ":" + minutes + ":" + seconds;
                ui.lbl_timeelapsed_time.setText(time);
            }
            time2 = System.currentTimeMillis();
            QCoreApplication.processEvents();
        }
        ui.lbl_plotimage.setPixmap(new QPixmap((FileManager.getCurrentDir() + "/temp/chart.png")));
    }

    private int readCurrentTaskProgress() {
        File file = new File(FileManager.getCurrentDir() + "/temp/taskprogress.txt");
        int progress = 0;
        try {
            FileInputStream fstream = new FileInputStream(FileManager.getCurrentDir() + "/temp/taskprogress.txt");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                progress = Integer.parseInt(strLine);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return progress;
    }

    private void addTask() {
        if (ui.tabs_tasks.currentIndex() == 0) {
            Boolean customflag = false;
            Algorithm algorithm = null;
            if (ui.tab_algorithm.currentIndex() == 0) {
                String alg = ui.select_algorithm.currentText();
                algorithm = new SortingAlgorithm(alg, alg);
            } else {
                customflag = true;
                String alg = ui.txt_classname.toPlainText();
                String customtype = text2CustomType(ui.custom_type.currentText()) + "";
                System.out.println("Customtype: " + customtype);
                algorithm = new CustomAlgorithm(alg, alg, customtype);
            }
            int arrayType = text2ArrayType(ui.select_arraytype.currentText());
            int startsize = ui.spin_startsize.value();
            int maxsize = ui.spin_maxsize.value();
            int stepsize = ui.spin_stepsize.value();
            System.out.println(startsize);
            AlgorithmTask task = new AlgorithmTask(algorithm, startsize, maxsize, stepsize, arrayType);
            ceo.getTaskHolder().addTask(task);
            ui.tasklist.addRow(new String[] { task.getAlgorithm().getClassName(), task.getArrayType() + "", "" + startsize, "" + maxsize, "" + stepsize });
            if (!customflag) {
                ui.tasklist.setDefaultAlgorithm(ui.tasklist.rowCount() - 1);
            }
        } else if (ui.tabs_tasks.currentIndex() == 1) {
            int startsize = ui.spin_startsize_function.value();
            int maxsize = ui.spin_maxsize_function.value();
            int stepsize = ui.spin_stepsize_function.value();
            Function task = new Function(ui.txt_function.text(), startsize, stepsize, maxsize);
            ceo.getTaskHolder().addTask(task);
            ui.tasklist.addRow(new String[] { task.getExpression(), "", startsize + "", "" + maxsize, "" + stepsize });
            ui.tasklist.setFunction(ui.tasklist.rowCount() - 1);
        }
    }

    private void echoprint() {
        System.out.println("echoprint");
    }

    private void setint(int inte) {
        System.out.println(inte);
    }

    public static int text2ArrayType(String str) {
        str = str.trim();
        int output = 0;
        if (str.equals("Random")) {
            output = ArrayGenerator.ARRAY_RANDOM;
        } else if (str.equals("Reversed")) {
            output = ArrayGenerator.ARRAY_REVERSED;
        } else if (str.equals("Sorted")) {
            output = ArrayGenerator.ARRAY_SORTED;
        } else if (str.equals("Few Unique Keys")) {
            output = ArrayGenerator.ARRAY_FEWUNIQUE;
        }
        return output;
    }

    public static int text2CustomType(String str) {
        str = str.trim();
        int output = CustomAlgorithm.ALGORITHM_ARRAY;
        if (str.equals("Array")) {
            output = CustomAlgorithm.ALGORITHM_ARRAY;
        } else if (str.equals("Array & Number")) {
            output = CustomAlgorithm.ALGORITHM_ARRAYNUMBER;
        } else if (str.equals("Number")) {
            output = CustomAlgorithm.ALGORITHM_NUMBER;
        }
        return output;
    }

    private void setSimulationProgressBar(int value) {
        ui.progressBar_simulation.setValue(value);
    }

    private void stopSimulationThread() {
        Simulator.setStopSimulation(true);
        this.simulationThread.interrupt();
        Task dummytask = new Task() {

            @Override
            public ArrayList<TimeEvent> run() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }
        };
        this.ceo.addTask(dummytask);
        this.ceo.getTaskHolder().removeTask(dummytask);
        ui.btn_stopsimulation.setDisabled(true);
    }

    public void CopyFile(File source, File destination) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addFilesToExistingZip(File zipFile, File[] files) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), null);
        tempFile.delete();
        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException("could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }
        byte[] buf = new byte[1024];
        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            boolean notInFiles = true;
            for (File f : files) {
                if (f.getName().equals(name)) {
                    notInFiles = false;
                    break;
                }
            }
            if (notInFiles) {
                out.putNextEntry(new ZipEntry(name));
                int len;
                while ((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        zin.close();
        for (int i = 0; i < files.length; i++) {
            InputStream in = new FileInputStream(files[i]);
            out.putNextEntry(new ZipEntry(files[i].getName()));
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
        out.close();
        tempFile.delete();
    }

    public String getCurrentDir() {
        File file = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        return file.getParent();
    }
}
