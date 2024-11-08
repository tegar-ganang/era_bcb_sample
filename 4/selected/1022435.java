package net.sourceforge.picdev.components;

import net.sourceforge.picdev.annotations.EComponent;
import net.sourceforge.picdev.annotations.EComponentProperty;
import net.sourceforge.picdev.core.*;
import net.sourceforge.picdev.util.CmdLine;
import net.sourceforge.picdev.wb.BasicAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullOutputStream;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;

public abstract class PIC implements ActionListener, Component {

    Project project;

    boolean lst_available = false;

    boolean RA4_old_state = false;

    boolean RB0_old_state = false;

    boolean RB4_old_state = false;

    boolean RB5_old_state = false;

    boolean RB6_old_state = false;

    boolean RB7_old_state = false;

    boolean running = false;

    /**
	 * Memory dump to be shown by DebugWindows.
	 */
    private Dump dump;

    private volatile boolean dumpRequested = true;

    ProgramMemoryWindow progwindow;

    protected ComponentView picwindow;

    BreakpointModel breakpointModel;

    WatchModel watchModel;

    /**
	 * All ports in reverse alphabetical order. (last one "PORTA")
	 */
    protected GPIO[] ports;

    ProgramMemory mem;

    public PICCPU cpu;

    private File sourceFile;

    private File linkerFile;

    private String projectDirectory;

    String filename;

    String error_filename;

    String mpasm_path;

    String icprog_path;

    JButton Step_button;

    JButton Stepover_button;

    JButton Stepout_button;

    JButton Restart_button;

    JButton Skip_button;

    JButton MEM_button;

    JButton Watch_button;

    JButton BP_button;

    JButton watchdog_button;

    ImageIcon watchdog_on;

    ImageIcon watchdog_off;

    private String name;

    final BasicAction buildAction = new BasicAction("Compile", "Save & Compile program code [F11]", "build_exec.gif") {

        public void actionPerformed(ActionEvent e) {
            build();
        }
    };

    final BasicAction programAction = new BasicAction("Program", "Program the microcontroller", "native_lib_path_attrib.gif") {

        public void actionPerformed(ActionEvent e) {
            try {
                CmdLine.execute(icprog_path + " -l\"" + getSourceFilename().substring(0, getSourceFilename().length() - 3) + "hex\" -f3FF9", new NullOutputStream(), System.err);
            } catch (java.io.IOException ioe) {
                System.out.println("Problem with ICProg");
            }
        }
    };

    final BasicAction stepIntoAction = new BasicAction("Step into", "Step into [F??]", "stepinto_co.gif") {

        public void actionPerformed(ActionEvent e) {
            stepInto();
        }
    };

    final BasicAction stepOverAction = new BasicAction("Step over", "Step over [F?]", "stepover_co.gif") {

        public void actionPerformed(ActionEvent e) {
            stepOver();
        }
    };

    final BasicAction stepReturnAction = new BasicAction("Step return", "Step return [F?]", "stepreturn_co.gif") {

        public void actionPerformed(ActionEvent e) {
            stepReturn();
        }
    };

    private final Action[] allActions = new Action[] { stepIntoAction, stepOverAction, stepReturnAction, null, buildAction, programAction };

    public PIC() {
        name = getClass().getAnnotation(EComponent.class).name();
        createPorts();
        createPicWindow();
    }

    public void createTemplateSource(ProjectInfo prjInfo) {
        String prjName = FilenameUtils.getBaseName(prjInfo.getFilename());
        String prjPth = FilenameUtils.getFullPath(prjInfo.getFilename());
        sourceFile = new File(prjPth + prjName, prjName + ".asm");
        File template = new File(System.getProperty("user.dir") + "/" + getTemplateFileName());
        try {
            FileUtils.copyFile(template, sourceFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog((java.awt.Component) picwindow, "Unable to copy temaplate source " + template + " !");
        }
    }

    protected abstract String getTemplateFileName();

    public void init(Project parent) {
        this.project = parent;
        projectDirectory = parent.getRootDirectory();
        createProgramMemory();
        try {
            RandomAccessFile piccfg = new RandomAccessFile("pic.cfg", "r");
            mpasm_path = piccfg.readLine();
            icprog_path = piccfg.readLine();
            piccfg.close();
        } catch (java.io.IOException ioe) {
        }
        JPanel panel = new JPanel();
        watchdog_on = new ImageIcon("images/watchdog_on.gif");
        watchdog_off = new ImageIcon("images/watchdog_off.gif");
        Step_button = new JButton(new ImageIcon("images/stepinto.gif"));
        Step_button.setBorder(BorderFactory.createEmptyBorder());
        Step_button.setToolTipText("Step into [F5]");
        Stepover_button = new JButton(new ImageIcon("images/stepover.gif"));
        Stepover_button.setBorder(BorderFactory.createEmptyBorder());
        Stepover_button.setToolTipText("Step over [F6]");
        Stepout_button = new JButton(new ImageIcon("images/stepout.gif"));
        Stepout_button.setBorder(BorderFactory.createEmptyBorder());
        Stepout_button.setToolTipText("Step out [F7]");
        Restart_button = new JButton(new ImageIcon("images/restart.gif"));
        Restart_button.setBorder(BorderFactory.createEmptyBorder());
        Restart_button.setToolTipText("Reset [F10]");
        Skip_button = new JButton(new ImageIcon("images/skip.gif"));
        Skip_button.setBorder(BorderFactory.createEmptyBorder());
        Skip_button.setToolTipText("Skip next instruction");
        MEM_button = new JButton(new ImageIcon("images/mem.gif"));
        MEM_button.setBorder(BorderFactory.createEmptyBorder());
        MEM_button.setToolTipText("Show/Hide Program memory");
        Watch_button = new JButton(new ImageIcon("images/rw.gif"));
        Watch_button.setBorder(BorderFactory.createEmptyBorder());
        Watch_button.setToolTipText("New Register Watch");
        BP_button = new JButton(new ImageIcon("images/bp.gif"));
        BP_button.setBorder(BorderFactory.createEmptyBorder());
        BP_button.setToolTipText("Show/Hide Breakpoints");
        watchdog_button = new JButton(watchdog_off);
        watchdog_button.setBorder(BorderFactory.createEmptyBorder());
        watchdog_button.setToolTipText("Enable/Disable Watchdog Timer");
        if (mpasm_path == null) buildAction.setEnabled(false);
        if (icprog_path != null) programAction.setEnabled(false);
        JButton separator3 = new JButton(new ImageIcon("images/separator.gif", " "));
        separator3.setBorder(BorderFactory.createEmptyBorder());
        panel.add(separator3);
        panel.add(Step_button);
        Step_button.addActionListener(this);
        panel.add(Stepover_button);
        Stepover_button.addActionListener(this);
        panel.add(Stepout_button);
        Stepout_button.addActionListener(this);
        panel.add(Restart_button);
        Restart_button.addActionListener(this);
        panel.add(Skip_button);
        Skip_button.addActionListener(this);
        JButton separator = new JButton(new ImageIcon("images/separator.gif", " "));
        separator.setBorder(BorderFactory.createEmptyBorder());
        panel.add(separator);
        MEM_button.addActionListener(this);
        panel.add(Watch_button);
        Watch_button.addActionListener(this);
        panel.add(BP_button);
        BP_button.addActionListener(this);
        JButton separator4 = new JButton(new ImageIcon("images/separator.gif", " "));
        separator4.setBorder(BorderFactory.createEmptyBorder());
        panel.add(separator4);
        panel.add(watchdog_button);
        watchdog_button.addActionListener(this);
        parent.addToTab("PIC Microcontroller", panel, true);
        createCPU();
        breakpointModel = new BreakpointModel(mem);
        watchModel = new WatchModel(cpu.ram);
        progwindow = new ProgramMemoryWindow(project, this, this);
        createDebugWindows();
        if (getSourceFile() != null) {
            progwindow.loadSource(this.getSourceFile());
        }
        reset();
        progwindow.selectLine(1);
    }

    protected void createDebugWindows() {
    }

    protected abstract void createCPU();

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == MEM_button) {
        }
        if (evt.getSource() == Step_button) {
            stepInto();
        }
        if (evt.getSource() == Stepover_button) {
            stepOver();
        }
        if (evt.getSource() == Stepout_button) {
            stepReturn();
        }
        if (evt.getSource() == Restart_button) reset();
        if (evt.getSource() == Skip_button && lst_available) {
            cpu.incrementPC();
            progwindow.setCurrentAddress(cpu.getPc());
        }
        if (evt.getSource() == BP_button) {
        }
        if (evt.getSource() == Watch_button) {
        }
        if (evt.getSource() == watchdog_button) {
            if (cpu.watchdog) {
                cpu.watchdog = false;
                watchdog_button.setIcon(watchdog_off);
            } else {
                cpu.watchdog = true;
                watchdog_button.setIcon(watchdog_on);
            }
        }
        String action = new String(evt.getActionCommand());
        if (action.equals("Run here")) {
            Breakpoint b = new Breakpoint("", 0, progwindow.getAddressAtSelectedLine());
            b.setRemoveOnHitOrStop(true);
            breakpointModel.addBreakpoint(b);
            project.start();
        }
        if (action.equals("Set PC here")) {
            int address = progwindow.getAddressAtSelectedLine();
            cpu.setPc(address);
            progwindow.setCurrentAddress(address);
            progwindow.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void step() {
        if (!cpu.sleep && lst_available) {
            if (updateIO()) {
                project.postPinUpdate(this);
                cpu.io_update_has_come = false;
            }
            cpu.step();
            if (updateIO()) {
                project.postPinUpdate(this);
            }
            if (!running) {
                progwindow.setCurrentAddress(cpu.getPc());
            }
            if (!cpu.twoCycleInstruction && breakpointModel.hasBreakpoint(cpu.getPc())) {
                project.stop();
            }
        } else if (cpu.sleep) {
            updateIO();
            cpu.step();
        }
    }

    private boolean updateIO() {
        boolean port_change = false;
        int adr = 0x05 + ports.length - 1;
        for (IO port : ports) {
            boolean change = cpu.updateIO(port, adr);
            if (change) port_change = true;
            adr--;
        }
        return port_change;
    }

    public void reset() {
        project.stop();
        lst_available = progwindow.parseLstFile(sourceFile);
        mem.clearSourceMapping();
        if (lst_available) {
            String codFileName = getSourceFilename().substring(0, getSourceFilename().indexOf('.')) + ".cod";
            try {
                CodFile cod = new CodFile(new File(codFileName));
                cod.fillMemory(mem, 0, mem.getSize());
                if (cpu.getEEProm() != null) {
                    cod.fillMemory(cpu.getEEProm(), 0x2100, cpu.getEEProm().getSize());
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        cpu.reset();
        updateIO();
        if (lst_available) {
            progwindow.setCurrentAddress(cpu.getPc());
        }
        createDump();
    }

    private void build() {
        if (mpasm_path == null) {
            Object options[] = { "   Yes   ", "    No    ", "  Help  " };
            int result = JOptionPane.showOptionDialog((java.awt.Component) picwindow, "PIC Development Studio does not have an inbuilt compiler so\nyou have to download one (i.e. MPASMWIN.EXE). Click on help for more info.\nDo you want to set the path to the compiler now?", "Path to compiler executable is not specified", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (result == 0) {
                JFileChooser lstchooser = new JFileChooser();
                lstchooser.setDialogTitle("Set path to compilator executable");
                if (lstchooser.showOpenDialog((java.awt.Component) picwindow) == JFileChooser.APPROVE_OPTION) {
                    File file = lstchooser.getSelectedFile();
                    mpasm_path = file.getPath();
                    try {
                        RandomAccessFile piccfg = new RandomAccessFile("pic.cfg", "rw");
                        piccfg.writeBytes(mpasm_path);
                        piccfg.close();
                    } catch (java.io.IOException ioe) {
                    }
                }
            }
        }
        if (mpasm_path != null) {
            progwindow.saveSource(getSourceFilename());
            if (linkerFile == null) {
                String runparams[] = { "gpasm", getMpasmParams(), sourceFile.getName() };
                try {
                    int ret = CmdLine.execute(runparams, new NullOutputStream(), System.err, projectDirectory);
                    if (ret >= 5) {
                        System.err.println("Command returned " + ret);
                    }
                } catch (java.io.IOException ioe) {
                    System.out.println("Problem with compilation");
                }
            } else {
                String targetFile = sourceFile.getName().substring(0, sourceFile.getName().length() - 4);
                File target = new File(projectDirectory + "/" + targetFile);
                target.delete();
                project.clearMessages();
                StringBuffer output = new StringBuffer();
                String[] params = { "-c", getMpasmParams() };
                boolean compileSucceeded = compile(sourceFile, params, output);
                project.addMessage(output.toString());
                if (compileSucceeded) {
                    output = new StringBuffer();
                    String[] targets = { sourceFile.getName() };
                    link(targets, targetFile, linkerFile, output);
                    project.addMessage(output.toString());
                }
            }
            reset();
        }
    }

    private boolean compile(File file, String[] options, StringBuffer output) {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out;
        int exitCode = 0;
        String params[] = new String[options.length + 2];
        params[0] = "gpasm";
        params[params.length - 1] = sourceFile.getName();
        System.arraycopy(options, 0, params, 1, options.length);
        try {
            out = new PipedOutputStream(in);
            exitCode = CmdLine.execute(params, out, out, projectDirectory);
        } catch (java.io.IOException ioe) {
            System.out.println("An error accord while compiling " + file.getName());
        }
        try {
            if (in.available() > 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }
            }
        } catch (IOException e) {
        }
        if (exitCode != 0) return false;
        return true;
    }

    private boolean link(String[] files, String targetFile, File linkerFile, StringBuffer output) {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out;
        int exitCode = 0;
        String[] params = new String[files.length + 3];
        params[0] = "gplink";
        params[1] = "-s" + linkerFile.getName();
        params[2] = "-o" + targetFile;
        for (int i = 0; i < files.length; i++) {
            String filenameBase = files[i].substring(0, files[i].length() - 4);
            params[i + 3] = filenameBase + ".o";
        }
        try {
            out = new PipedOutputStream(in);
            exitCode = CmdLine.execute(params, out, System.err, projectDirectory);
        } catch (java.io.IOException ioe) {
            System.out.println("An error occured while linking");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            if (in.available() > 0) {
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }
            }
        } catch (IOException e) {
        }
        if (exitCode != 0) {
            return false;
        }
        return true;
    }

    protected abstract String getMpasmParams();

    public void ioChangeNotify() {
        if (getPortA().state[4] != RA4_old_state) {
            RA4_old_state = getPortA().state[4];
            cpu.RA4_change_occured(getPortA().state[4]);
        }
        if (getPortB().state[0] != RB0_old_state) {
            RB0_old_state = getPortB().state[0];
            cpu.RB0_change_occured(getPortB().state[0]);
        }
        if (getPortB().state[4] != RB4_old_state) {
            RB4_old_state = getPortB().state[4];
            cpu.RB_port_change();
        }
        if (getPortB().state[5] != RB5_old_state) {
            RB5_old_state = getPortB().state[5];
            cpu.RB_port_change();
        }
        if (getPortB().state[6] != RB6_old_state) {
            RB6_old_state = getPortB().state[6];
            cpu.RB_port_change();
        }
        if (getPortB().state[7] != RB7_old_state) {
            RB7_old_state = getPortB().state[7];
            cpu.RB_port_change();
        }
        cpu.io_update_has_come = true;
    }

    public void setInput(int index, boolean state) {
        cpu.io_update_has_come = true;
        int limit = 0;
        for (IO port : ports) {
            int w = port.getWidth();
            if (index < limit + w) {
                port.setInput(index - limit, state);
                return;
            }
            limit += w;
        }
        assert false : index + " : No valid input ! Number of IOs : " + limit;
    }

    public boolean getOutput(int index) {
        int limit = 0;
        for (IO port : ports) {
            int w = port.getWidth();
            if (index < limit + w) {
                return port.getOutput(index - limit);
            }
            limit += w;
        }
        assert false : index + " : No valid output ! Number of IOs : " + limit;
        return false;
    }

    public void clock(int project_clock) {
        step();
        if (dumpRequested) {
            createDump();
        }
    }

    public void starting() {
        progwindow.setEnabled(false);
        running = true;
    }

    public void stopping() {
        running = false;
        createDump();
        progwindow.setEnabled(true);
        breakpointModel.removeTemporaryBreakpoints();
        progwindow.setCurrentAddress(cpu.getPc());
    }

    private void stepInto() {
        if (lst_available && !project.isRunning() && !progwindow.isTouched()) {
            project.clock();
            if (cpu.isTwoCycleInstruction()) {
                project.clock();
            }
        }
    }

    private void stepOver() {
        if (lst_available && !project.isRunning() && !progwindow.isTouched()) {
            if (cpu.isCallInstruction(cpu.getPc())) {
                Breakpoint b = new Breakpoint("", 0, cpu.getPc() + 1);
                b.setRemoveOnHitOrStop(true);
                breakpointModel.addBreakpoint(b);
                project.start();
            } else {
                stepInto();
            }
        }
    }

    private void stepReturn() {
        if (lst_available && !project.isRunning() && !progwindow.isTouched()) {
            if (cpu.stack.size() > 0) {
                int adr = cpu.stack.get(0);
                Breakpoint b = new Breakpoint("", 0, adr);
                b.setRemoveOnHitOrStop(true);
                breakpointModel.addBreakpoint(b);
                project.start();
            } else stepInto();
        }
    }

    protected abstract GPIO getPortA();

    protected abstract GPIO getPortB();

    protected abstract void createProgramMemory();

    protected abstract void createPorts();

    protected abstract void createPicWindow();

    @EComponentProperty(description = "Program source")
    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @EComponentProperty(description = "Linker file")
    public File getLinkerFile() {
        return linkerFile;
    }

    public void setLinkerFile(File linkerFile) {
        this.linkerFile = linkerFile;
    }

    public String getSourceFilename() {
        if (sourceFile == null) return null;
        if (sourceFile.isAbsolute()) return sourceFile.getAbsolutePath();
        File prjFile = new File(project.getRootDirectory());
        return prjFile.getParent() + "/" + sourceFile.getPath();
    }

    @EComponentProperty(description = "Individual name of this Component")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Action[] getActions() {
        return allActions;
    }

    public ComponentView getBreadboardView() {
        return picwindow;
    }

    public void requestDump() {
        if (!project.isRunning()) {
            createDump();
        } else {
            dumpRequested = true;
        }
    }

    private synchronized void createDump() {
        dump = new Dump(cpu.w, cpu.ram, cpu.eeprom, cpu.stack);
        dumpRequested = false;
    }

    public synchronized Dump getDump() {
        return dump;
    }
}
