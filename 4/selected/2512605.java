package rotorsim.main;

import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.SplashScreen;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import rotorsim.aircraft.Aircraft;
import rotorsim.controller.RSController;
import rotorsim.controller.TXCalibrationDialog;
import rotorsim.controller.RSController.HeliControl;
import util.Vector3f;

public class RotorSim extends JFrame implements ActionListener {

    private static final String CONFIG_FILE = "rotorsim_config.xml";

    private static final float MAX_STEP_SIZE = 0.001f;

    private static final int VERSION_MAJOR = 0;

    private static final int VERSION_MINOR = 3;

    private static final int VERSION_REVISION = 0;

    private MenuBar menuBar = new MenuBar();

    private Menu fileMenu = new Menu("File");

    private Menu simMenu = new Menu("Simulation");

    private Menu helpMenu = new Menu("Help");

    private MenuItem exitItem = new MenuItem("exit");

    private MenuItem controllerSetup = new MenuItem("Controller Setup");

    private MenuItem reset = new MenuItem("Reset");

    private MenuItem aboutItem = new MenuItem("About");

    private TXCalibrationDialog txSetup;

    private Image rotorSymbol;

    private long lastTime;

    private boolean finished;

    private Simulation sim;

    private Camera cam;

    private Vector3f camTarget = new Vector3f(1, 0, 0);

    private Aircraft heli;

    private List<RSController> controllers;

    private RSController curController;

    private SimulationView simView;

    private boolean simPaused;

    FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f });

    FloatBuffer listenerPos = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });

    FloatBuffer listenerVel = BufferUtils.createFloatBuffer(3).put(new float[] { 0.0f, 0.0f, 0.0f });

    Vector3f camDir = new Vector3f();

    private boolean showTXConfig;

    private RotorSim() {
        try {
            rotorSymbol = ImageIO.read(getClass().getClassLoader().getResourceAsStream("images/rotor_symbol.png"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        setIconImage(rotorSymbol);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cam = new Camera();
        cam.setChaseTarget(false);
        finished = false;
        FieldLoader fload = new FieldLoader();
        AircraftLoader heliLoad = new AircraftLoader();
        heli = heliLoad.loadAircraft("models/honeybee");
        cam.setTarget(heli.getPosition());
        simPaused = false;
        showTXConfig = true;
        try {
            simView = new SimulationView();
            add(simView);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        loadControllers();
        txSetup = new TXCalibrationDialog(controllers);
        setMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(simMenu);
        menuBar.add(helpMenu);
        fileMenu.add(exitItem);
        simMenu.add(controllerSetup);
        simMenu.add(reset);
        helpMenu.add(aboutItem);
        exitItem.addActionListener(this);
        controllerSetup.addActionListener(this);
        reset.addActionListener(this);
        aboutItem.addActionListener(this);
        txSetup.setModal(true);
        txSetup.setIconImage(rotorSymbol);
        sim = new Simulation(fload.loadField("fields/test"), heli);
        simView.setCamera(cam);
        simView.setSize(640, 480);
        cam.setPosition(new Vector3f(-2, 0, sim.getField().getHeightAt(10, 10) + 2.0f));
        heli.setPosition(new Vector3f(0, 0, sim.getField().getHeightAt(0, 0) + 0.08f));
        simView.setSimulation(sim);
        loadConfig();
        pack();
        initAL();
    }

    private void loadControllers() {
        ControllerEnvironment environ = ControllerEnvironment.getDefaultEnvironment();
        controllers = new ArrayList<RSController>();
        for (Controller controller : environ.getControllers()) {
            RSController rsController = new RSController(controller);
            if (rsController.getChannelCount() >= 4) {
                controllers.add(rsController);
            }
        }
    }

    public void run() {
        lastTime = System.nanoTime();
        while (!finished) {
            float step = (System.nanoTime() - lastTime) / 1000000000.0f;
            lastTime = System.nanoTime();
            if (showTXConfig) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        actionPerformed(new ActionEvent(controllerSetup, 0, null));
                    }
                });
                showTXConfig = false;
            }
            curController.update();
            if (txSetup.isVisible()) {
                txSetup.updateController();
            } else {
                for (HeliControl control : HeliControl.values()) {
                    heli.setControl(control.name, curController.getControlValue(control));
                }
                if (!simPaused) {
                    while (step > MAX_STEP_SIZE) {
                        sim.update(MAX_STEP_SIZE);
                        cam.update(MAX_STEP_SIZE);
                        step -= MAX_STEP_SIZE;
                    }
                    sim.update(step);
                    cam.update(step);
                    updateAL();
                    simView.repaint();
                }
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException ie) {
            }
        }
    }

    public static void main(String[] arguments) throws Exception {
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }
        RotorSim rs = new RotorSim();
        rs.pack();
        rs.setVisible(true);
        rs.run();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exitItem) {
            finished = true;
            setVisible(false);
            System.exit(0);
        } else if (e.getSource() == controllerSetup) {
            simPaused = true;
            txSetup.pack();
            txSetup.setVisible(true);
            saveConfig();
            simPaused = false;
        } else if (e.getSource() == reset) {
            sim.reset();
            cam.setPosition(new Vector3f(-2, 0, sim.getField().getHeightAt(10, 10) + 2.0f));
            simPaused = false;
        } else if (e.getSource() == aboutItem) {
            String text = "<html><h1>RotorSim " + VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION + "</h1>" + "<p>A radio controlled model helicopter simulator.</p>" + "<a href=\"www.angryoctopus.co.nz/rotorsim\">angryoctopus.com/rotorsim</a><br>" + '©' + " Shannon Smith</html>";
            simPaused = true;
            JOptionPane.showMessageDialog(this, text, "RotorSim Helicopter Simulator", JOptionPane.INFORMATION_MESSAGE);
            simPaused = false;
        }
    }

    public void saveConfig() {
        File tempDir = new File(System.getProperty("user.home"));
        if (!tempDir.exists()) {
            return;
        }
        tempDir = new File(tempDir, ".rotorsim");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File config = new File(tempDir, CONFIG_FILE);
        Document doc = new Document();
        Element root = new Element("config");
        root.setAttribute("version", VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION);
        Element cont = new Element("controllers");
        for (RSController controller : controllers) {
            cont.addContent(controller.toXML());
        }
        root.addContent(cont);
        doc.setRootElement(root);
        XMLOutputter xmlOut = new XMLOutputter();
        try {
            xmlOut.setFormat(Format.getPrettyFormat());
            xmlOut.output(doc, new FileWriter(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        curController = controllers.get(0);
        File tempDir = new File(System.getProperty("user.home"), ".rotorsim");
        if (!tempDir.exists()) {
            return;
        }
        File config = new File(tempDir, "rotorsim_config.xml");
        if (!config.exists()) {
            return;
        }
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(config);
            for (RSController controller : controllers) {
                for (Element ele : (List<Element>) doc.getRootElement().getChild("controllers").getChildren()) {
                    if (ele.getAttributeValue("input").equals(controller.getName())) {
                        controller.loadXML(ele);
                    }
                }
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        txSetup.controllerSelected(0);
        showTXConfig = false;
    }

    public void updateAL() {
        camDir.set(cam.getTarget());
        camDir.sub(cam.getPos());
        camDir.normalise();
        listenerOri.clear();
        listenerOri.put(camDir.x);
        listenerOri.put(camDir.y);
        listenerOri.put(camDir.z);
        listenerOri.put(0);
        listenerOri.put(0);
        listenerOri.put(1);
        listenerOri.flip();
        listenerPos.clear();
        listenerPos.put(cam.getPos().x);
        listenerPos.put(cam.getPos().y);
        listenerPos.put(cam.getPos().z);
        listenerPos.flip();
        listenerVel.clear();
        listenerVel.put(0);
        listenerVel.put(0);
        listenerVel.put(0);
        listenerVel.flip();
        AL10.alListener(AL10.AL_POSITION, listenerPos);
        AL10.alListener(AL10.AL_VELOCITY, listenerVel);
        AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
    }

    public void initAL() {
        try {
            AL.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        AL10.alDopplerFactor(0.02f);
    }
}
