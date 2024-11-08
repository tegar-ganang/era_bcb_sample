package exec.visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import planning.editor.PlannerEditor;
import planning.editor.PlannerEditorController;
import planning.file.ExecuterPropConverter;
import planning.file.props.ExecuterProp;
import planning.model.World;
import planning.plan.Executer;
import simulation.editor.SimulationScreenListener;
import simulation.editor.SimulatorEditor;
import simulation.editor.SimulatorEditorController;
import simulation.model.Simulator;
import simulation.model.WorldContent;
import simulation.shell.Shell;
import simulation.visual.SimulationScreen;
import simulation.visual.WorldScreenMapper;
import util.FrameUtils;

public class RobotSimContentController implements IRobotSimContentController {

    private SimpleSimulatePanel ssp;

    private Simulator simulator;

    private JFrame simulationFrame;

    private SimulationScreen screen;

    private SimulatorEditor simulatorEditor;

    private JFrame simulatorEditorFrame;

    private PlannerEditor plannerEditor;

    private JFrame plannerEditorFrame;

    private Executer executer;

    private Shell shell;

    private ExecuterPropConverter execPropConterver;

    private JFileChooser fileChooserGrab;

    private String lastScreenShot = ".";

    public RobotSimContentController(SSPListener sspListener) {
        fileChooserGrab = new JFileChooser();
        fileChooserGrab.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String path = f.getAbsolutePath().toLowerCase();
                if (path.endsWith("jpeg") || path.endsWith("png") || path.endsWith("bmp") || path.endsWith("gif") || path.endsWith("tiff") || path.endsWith("wbmp")) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "JPEG, PNG, BMP, GIF, TIFF, WBMP";
            }
        });
        simulator = new Simulator();
        shell = new Shell(simulator);
        screen = new SimulationScreen(simulator, new WorldScreenMapper(World.DEFAULT_RADIUS * 2, World.DEFAULT_RADIUS * 2, 0, 0, 640, 480));
        executer = new Executer();
        execPropConterver = new ExecuterPropConverter();
        SimulatorEditorController simEditController = new SimulatorEditorController();
        simulatorEditor = new SimulatorEditor(simEditController);
        simEditController.initialize(simulatorEditor, this);
        screen.editorListener = new SimulationScreenListener(simEditController);
        ssp = new SimpleSimulatePanel(sspListener, this);
        ssp.getJPanelSimulation().add(screen, BorderLayout.CENTER);
        PlannerEditorController plannerEditorController = new PlannerEditorController();
        plannerEditor = new PlannerEditor(plannerEditorController);
        ExecuterProp initExecuterProp = new ExecuterProp();
        plannerEditorController.initialize(plannerEditor, this, initExecuterProp);
        plannerEditorFrame = new JFrame("Planner Editor");
        plannerEditorFrame.setContentPane(plannerEditor);
        plannerEditorFrame.pack();
        simulatorEditorFrame = new JFrame("Simulation Editor");
        simulatorEditorFrame.setContentPane(simulatorEditor);
        simulatorEditorFrame.pack();
        simulationFrame = new JFrame();
        simulationFrame.setTitle("Little Prince Path Planning Simulator v1.21 - by Fuat GELERI");
        simulationFrame.setContentPane(ssp);
        simulationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        simulationFrame.pack();
        FrameUtils.carryToTheCenter(simulationFrame);
        FrameUtils.carryToTheCenter(simulatorEditorFrame);
        FrameUtils.carryToTheCenter(plannerEditorFrame);
        simulationFrame.setVisible(true);
    }

    public JFrame getSimulationFrame() {
        return simulationFrame;
    }

    public SimulationScreen getSimulationScreen() {
        return screen;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public void setSimulationScreen(SimulationScreen screen) {
        screen.editorListener = this.screen.editorListener;
        ssp.getJPanelSimulation().removeAll();
        ssp.getJPanelSimulation().add(screen, BorderLayout.CENTER);
        this.screen = screen;
        ssp.getJPanelSimulation().invalidate();
        simulationFrame.pack();
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
        shell = new Shell(simulator);
    }

    public Executer getExecuter() {
        return executer;
    }

    public void setExecuter(Executer executer) {
        this.executer = executer;
    }

    public ExecuterProp getExecuterProp() {
        return execPropConterver.generateExecuterProp(executer);
    }

    public void setExecuter(ExecuterProp executerProp) throws NoSuchFieldException {
        this.executer = execPropConterver.generateExecuter(executerProp, getShell());
    }

    public Shell getShell() {
        return shell;
    }

    public void showHidePlannerEditor() {
        plannerEditorFrame.setVisible(true);
    }

    public void showHideSimulatorEditor() {
        simulatorEditorFrame.setVisible(true);
    }

    JFrame aboutFrame = null;

    public void showAbout() {
        if (aboutFrame == null) {
            aboutFrame = new JFrame("About");
            AboutPanel aboutPanel = new AboutPanel();
            aboutFrame.getContentPane().add(aboutPanel);
            aboutFrame.invalidate();
            aboutFrame.pack();
            FrameUtils.carryToTheCenter(aboutFrame);
        }
        if (aboutFrame.isVisible()) {
            aboutFrame.setVisible(false);
        } else {
            aboutFrame.setVisible(true);
        }
    }

    public WorldContent getSimulationWorld() {
        return shell.simulator.worldContent;
    }

    public void takeSnapshot() {
        Dimension size = screen.getSize();
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        screen.paintAll(g);
        String filename = "";
        try {
            boolean continues = true;
            while (continues) {
                continues = false;
                fileChooserGrab.setCurrentDirectory(new File(lastScreenShot));
                if (fileChooserGrab.showSaveDialog(screen) != JFileChooser.APPROVE_OPTION) return;
                File file = fileChooserGrab.getSelectedFile();
                if (file != null) {
                    if (file.exists()) {
                        int selection = JOptionPane.showConfirmDialog(screen, "File already exist. Do you want to overwrite?");
                        if (selection == JOptionPane.YES_OPTION) {
                        } else if (selection == JOptionPane.NO_OPTION) {
                            continues = true;
                        } else {
                            return;
                        }
                    }
                    filename = file.getAbsolutePath();
                    if (!continues) {
                        OutputStream out = new FileOutputStream(filename);
                        String ext = (filename.lastIndexOf(".") == -1) ? "" : filename.substring(filename.lastIndexOf(".") + 1, filename.length());
                        ImageIO.write(image, ext, out);
                        out.close();
                        lastScreenShot = filename;
                        ssp.setStatus("Scene is saved to image : " + filename);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(screen, "Could not save the screenshot to file : \n" + filename + "\nException : " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public IExecutionListener getExecutionListener() {
        return ssp;
    }
}
