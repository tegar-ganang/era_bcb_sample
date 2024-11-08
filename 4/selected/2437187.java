package net.sourceforge.entrainer.eeg.gui;

import static net.sourceforge.entrainer.util.Utils.snooze;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.sourceforge.entrainer.eeg.core.EEGChannelState;
import net.sourceforge.entrainer.eeg.core.EEGChannelValue;
import net.sourceforge.entrainer.eeg.core.EEGDevice;
import net.sourceforge.entrainer.eeg.core.EEGDeviceLoader;
import net.sourceforge.entrainer.eeg.core.EEGDeviceStatusEvent;
import net.sourceforge.entrainer.eeg.core.EEGDeviceStatusListener;
import net.sourceforge.entrainer.eeg.core.EEGException;
import net.sourceforge.entrainer.eeg.core.EEGSignalProcessor;
import net.sourceforge.entrainer.eeg.core.EEGSignalProcessorLoader;
import net.sourceforge.entrainer.eeg.core.FrequencyType;
import net.sourceforge.entrainer.guitools.GuiUtil;
import net.sourceforge.entrainer.guitools.MigHelper;
import net.sourceforge.entrainer.util.Utils;
import org.jdesktop.swingx.RepaintManagerX;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel;
import org.jvnet.substance.watermark.SubstancePlanktonWatermark;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.swing.SwingRepaintTimeline;

/**
 * The FingBrainerz gui.
 * 
 * @author burton
 */
public class FingBrainerz extends JFrame {

    private static final String SELECT_DEVICE = "Select Device...";

    private static final String FILE_MENU = "File";

    private static final String CHOOSE_SIGNAL_PROCESSOR = "Choose Signal Processor...";

    private static final String SET_SAMPLE_FREQUENCY = "Set Sample Frequency";

    private static final String CHANNEL_MENU = "Channels";

    private static final String ADD_CHANNEL = "Add Channel...";

    private static final int FB_WIDTH = 50;

    private static final long serialVersionUID = 1L;

    private static final String CLOSE = "Close Device";

    private static final String CLEAR_CALIBRATION = "Clear Calibration";

    private static final String CALIBRATE = "Calibrate";

    private static final String OPEN = "Open Device";

    private static final int WIDTH = 1200;

    private static final int HEIGHT = 675;

    private Random rand = new Random(Calendar.getInstance().getTimeInMillis());

    private long sampleTime = 400;

    private EEGDevice device;

    private Thread deviceThread;

    private List<FingBrainer> fingBrainers = new ArrayList<FingBrainer>();

    private List<EEGChannelState> states = new ArrayList<EEGChannelState>();

    private JLabel statusLabel = new JLabel();

    private SwingRepaintTimeline repainter = new SwingRepaintTimeline(this);

    private InfiniteProgressPanel glassPane = new InfiniteProgressPanel("Calibrating...");

    private FingBrainerSettings settings = FingBrainerSettings.getInstance();

    private List<EEGDevice> devices = EEGDeviceLoader.getInstance().getEEGObjects();

    private List<EEGSignalProcessor> signalProcessors = EEGSignalProcessorLoader.getInstance().getEEGObjects();

    private boolean isRepainting = false;

    public FingBrainerz() {
        super("FingBrainerz!!!");
        initDevice();
        initStates();
        setResizable(false);
        getContentPane().setBackground(Color.BLACK);
        init();
        setGlassPane(glassPane);
        getGlassPane().setVisible(true);
        setIconImage(GuiUtil.getIcon("/nia.gif").getImage());
    }

    /**
	 * Overridden to set visible property to true always. Circumvents bug when
	 * window is closing and exit is canceled.
	 */
    public void setVisible(boolean b) {
        super.setVisible(true);
    }

    public void paint(Graphics g) {
        super.paint(g);
        paintFingBrainers(g);
    }

    private void initDevice() {
        EEGDevice dev = settings.getDevice();
        if (dev == null) {
            this.device = devices.get(0);
        } else {
            this.device = dev;
        }
        double sampleRate = settings.getSampleRate();
        EEGSignalProcessor signalProcessor = settings.getSignalProcessor();
        if (sampleRate > 0 && signalProcessor != null) {
            for (EEGDevice ed : devices) {
                ed.setSampleFrequencyInHertz(sampleRate);
                ed.setSignalProcessor(signalProcessor);
            }
        }
    }

    private void initStates() {
        if (settings.getSavedStates().isEmpty()) {
            states.add(new EEGChannelState(FrequencyType.ALPHA_LOW, 6, 8, device.getSampleFrequencyInHertz()));
            states.add(new EEGChannelState(FrequencyType.ALPHA_MID, 8, 10, device.getSampleFrequencyInHertz()));
            states.add(new EEGChannelState(FrequencyType.ALPHA_HIGH, 10, 12, device.getSampleFrequencyInHertz()));
            states.add(new EEGChannelState(FrequencyType.BETA_LOW, 12, 14, device.getSampleFrequencyInHertz()));
            states.add(new EEGChannelState(FrequencyType.BETA_MID, 14, 16, device.getSampleFrequencyInHertz()));
            states.add(new EEGChannelState(FrequencyType.BETA_HIGH, 16, 18, device.getSampleFrequencyInHertz()));
        } else {
            states.addAll(settings.getSavedStates());
        }
        addStatesToDevice();
    }

    private void addStatesToDevice() {
        for (EEGChannelState state : states) {
            device.addChannelState(state);
        }
    }

    private void paintFingBrainers(Graphics g) {
        for (FingBrainer fb : fingBrainers) {
            fb.paint(g);
        }
    }

    private void init() {
        Dimension preferredSize = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(preferredSize);
        for (int i = 0; i < states.size(); i++) {
            addFingBrainer(i, states.get(i));
        }
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showPopup(e.getPoint());
                }
            }
        });
        addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                repaintCheck();
            }

            public void focusLost(FocusEvent e) {
                repaintCheck();
            }
        });
        addMenu();
        addStatusLabel();
        setFocusable(true);
    }

    private void saveSettings() {
        settings.saveSettings(device, getFingBrainerStateMap());
    }

    private Map<FrequencyType, FingBrainerState> getFingBrainerStateMap() {
        Map<FrequencyType, FingBrainerState> map = new HashMap<FrequencyType, FingBrainerState>();
        for (FingBrainer fb : fingBrainers) {
            map.put(fb.getState().getFrequencyType(), fb.getFingBrainerState());
        }
        return map;
    }

    private void showPopup(Point point) {
        FingBrainer fb = getFingBrainerForPoint(point);
        if (fb == null) {
            return;
        }
        JPopupMenu pop = new JPopupMenu();
        pop.add(getSettingsItem(fb));
        pop.add(getRemoveItem(fb));
        pop.setInvoker(this);
        pop.setLabel(fb.getState().getFrequencyType().getDescription() + " Menu");
        pop.show(this, point.x, point.y);
    }

    private JMenuItem getRemoveItem(final FingBrainer fb) {
        JMenuItem item = new JMenuItem("Remove " + fb.getState().getFrequencyType().getDescription());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeItem(fb);
            }
        });
        return item;
    }

    private void removeItem(FingBrainer fb) {
        String name = fb.getState().getFrequencyType().getDescription();
        snoozeAndRepaint();
        int choice = JOptionPane.showConfirmDialog(this, "About to remove " + name + ". Confirm?", "Remove " + name, JOptionPane.YES_NO_OPTION);
        snoozeAndRepaint();
        if (choice == JOptionPane.YES_OPTION) {
            removeFingBrainer(fb);
        }
    }

    private void removeFingBrainer(FingBrainer fb) {
        startRepainter();
        device.removeChannelState(fb.getState().getFrequencyType());
        states.remove(fb.getState());
        fingBrainers.remove(fb);
        addToMenu(fb);
        FingBrainer existing;
        for (int i = 0; i < fingBrainers.size(); i++) {
            existing = fingBrainers.get(i);
            moveFingBrainer(i, existing.getState());
        }
        System.gc();
        if (!device.isOpen()) {
            stopRepainter();
        }
    }

    private void addToMenu(FingBrainer fb) {
        JMenuBar bar = getJMenuBar();
        JMenu menu;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            menu = bar.getMenu(i);
            if (menu.getText().equals(CHANNEL_MENU)) {
                addInOrder(menu, fb);
                return;
            }
        }
    }

    private void addInOrder(JMenu menu, FingBrainer fb) {
        JMenu addItems = (JMenu) menu.getItem(0);
        addItems.removeAll();
        List<FrequencyType> types = getUnusedFrequencyTypes();
        for (FrequencyType type : types) {
            addItems.add(getFrequencyTypeItem(type, addItems));
        }
    }

    private JMenuItem getSettingsItem(final FingBrainer fb) {
        JMenuItem item = new JMenuItem("Edit " + fb.getState().getFrequencyType().getDescription() + " settings");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showStateEditor(fb.getState(), fb.getFingBrainerState());
            }
        });
        return item;
    }

    private void showStateEditor(EEGChannelState state, FingBrainerState fingBrainerState) {
        final ChannelStateDialog csd = new ChannelStateDialog(this, state, fingBrainerState);
        csd.pack();
        GuiUtil.centerOnScreen(csd);
        snoozeAndRepaint();
        csd.setResizable(false);
        csd.setVisible(true);
        repaintCheck();
    }

    private void repaintCheck() {
        if (!isRepainting) {
            repaint();
        }
    }

    private void snoozeAndRepaint() {
        if (!isRepainting) {
            Thread thread = new Thread() {

                public void run() {
                    snooze(100);
                    repaint();
                }
            };
            thread.start();
        }
    }

    private FingBrainer getFingBrainerForPoint(Point point) {
        for (FingBrainer fb : fingBrainers) {
            if (fb.getX() <= point.x && fb.getX() + fb.getWidth() >= point.x) {
                return fb;
            }
        }
        return null;
    }

    private void addFingBrainer(int idx, EEGChannelState state) {
        FingBrainer fb = new FingBrainer(calculatePosition(idx), FB_WIDTH, 550, 670, state);
        FingBrainerState fbs = settings.getFingBrainerStates().get(state.getFrequencyType());
        if (fbs != null) {
            fb.setFingBrainerState(fbs);
        }
        if (idx >= fingBrainers.size()) {
            fingBrainers.add(fb);
        } else {
            fingBrainers.add(idx, fb);
        }
        repaintCheck();
    }

    private void moveFingBrainer(int idx, EEGChannelState state) {
        FingBrainer fb = getFingBrainerForState(state);
        Timeline tl = new Timeline(fb);
        tl.setDuration(750);
        tl.addPropertyToInterpolate("x", fb.getX(), (double) calculatePosition(idx));
        tl.play();
    }

    private int calculatePosition(int idx) {
        int taken = FB_WIDTH * 2 * states.size();
        int leftover = (WIDTH - taken) / states.size();
        int position = leftover / 2 + FB_WIDTH / 2;
        for (int i = 1; i <= idx; i++) {
            position += (FB_WIDTH * 2) + (leftover);
        }
        return position;
    }

    private void addStatusLabel() {
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.GREEN);
        MigHelper mh = new MigHelper(getContentPane());
        mh.setLayoutInsets(0, 0, 0, 0).setLayoutTopCenter().add(statusLabel);
        for (EEGDevice ed : devices) {
            ed.addDeviceStatusListener(new EEGDeviceStatusListener() {

                public void statusChanged(EEGDeviceStatusEvent e) {
                    statusLabel.setText(e.getNewStatus());
                }
            });
        }
    }

    private void addMenu() {
        JMenuBar bar = new JMenuBar();
        bar.add(getFileMenu());
        bar.add(getChannelMenu());
        bar.add(getDeviceControlMenu());
        setJMenuBar(bar);
    }

    private JMenu getDeviceControlMenu() {
        JMenu menu = new JMenu(getDeviceDescription());
        menu.add(getDeviceInfoItem());
        menu.add(getChooseSignalProcessorItem());
        menu.add(getSampleFrequencyItem());
        menu.add(getOpenItem());
        menu.add(getCalibrateItem());
        menu.add(getClearCalibrationItem());
        menu.add(getCloseItem());
        return menu;
    }

    private JMenuItem getDeviceInfoItem() {
        JMenuItem item = new JMenuItem("Device Info");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                device.notifyDeviceInfo();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        return item;
    }

    private JMenuItem getChooseSignalProcessorItem() {
        JMenu menu = new JMenu(CHOOSE_SIGNAL_PROCESSOR);
        for (EEGSignalProcessor sig : signalProcessors) {
            menu.add(getSignalProcessorItem(sig));
        }
        return menu;
    }

    private JMenuItem getSignalProcessorItem(final EEGSignalProcessor sig) {
        JMenuItem item = new JMenuItem(sig.getDescription());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                device.setSignalProcessor(sig);
            }
        });
        return item;
    }

    private JMenuItem getSampleFrequencyItem() {
        JMenuItem item = new JMenuItem(SET_SAMPLE_FREQUENCY);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showSampleFrequencyDialog();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        return item;
    }

    protected void showSampleFrequencyDialog() {
        SampleFrequencyDialog sfd = new SampleFrequencyDialog(this, device.getSampleFrequencyInHertz());
        sfd.pack();
        GuiUtil.centerOnScreen(sfd);
        sfd.setVisible(true);
        if (!sfd.isCancelled()) {
            double freq = sfd.getSampleFrequency();
            if (freq != device.getSampleFrequencyInHertz()) {
                device.setSampleFrequencyInHertz(freq);
            }
        }
    }

    private JMenu getChannelMenu() {
        JMenu menu = new JMenu(CHANNEL_MENU);
        menu.add(getAddChannelMenu());
        return menu;
    }

    private JMenu getAddChannelMenu() {
        JMenu menu = new JMenu(ADD_CHANNEL);
        List<FrequencyType> types = getUnusedFrequencyTypes();
        for (FrequencyType type : types) {
            menu.add(getFrequencyTypeItem(type, menu));
        }
        return menu;
    }

    private JMenuItem getFrequencyTypeItem(final FrequencyType type, final JMenu menu) {
        final JMenuItem item = new JMenuItem(type.getDescription());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (addFingBrainer(type)) {
                    menu.remove(item);
                }
            }
        });
        return item;
    }

    private boolean addFingBrainer(FrequencyType type) {
        startRepainter();
        EEGChannelState state = new EEGChannelState(type, 0, 0, device.getSampleFrequencyInHertz());
        showStateEditor(state, new FingBrainerState());
        if (state.getRangeFrom() > 0 && state.getRangeTo() > 0) {
            states.add(state);
            orderStates();
            device.addChannelState(state);
            for (int i = 0; i < states.size(); i++) {
                if (state.equals(states.get(i))) {
                    addFingBrainer(i, states.get(i));
                } else {
                    moveFingBrainer(i, states.get(i));
                }
            }
            if (!device.isOpen()) {
                stopRepainter();
            }
            return true;
        }
        return false;
    }

    private FingBrainer getFingBrainerForState(EEGChannelState state) {
        for (FingBrainer fb : fingBrainers) {
            if (fb.getState().equals(state)) {
                return fb;
            }
        }
        return null;
    }

    private void orderStates() {
        List<EEGChannelState> ordered = new ArrayList<EEGChannelState>();
        for (EEGChannelState state : states) {
            addInOrder(ordered, state);
        }
        states = ordered;
    }

    private void addInOrder(List<EEGChannelState> ordered, EEGChannelState state) {
        EEGChannelState existing;
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            existing = ordered.get(i);
            if (state.getFrequencyType().getOrder() < existing.getFrequencyType().getOrder()) {
                idx = i;
                break;
            }
        }
        if (idx >= ordered.size() || idx == -1) {
            ordered.add(state);
        } else {
            ordered.add(idx, state);
        }
    }

    private List<FrequencyType> getUnusedFrequencyTypes() {
        List<FrequencyType> types = FrequencyType.getFrequencyTypes();
        for (FingBrainer fb : fingBrainers) {
            types.remove(fb.getState().getFrequencyType());
        }
        return types;
    }

    private String getDeviceDescription() {
        return device.getDeviceDescription();
    }

    private JMenuItem getOpenItem() {
        final JMenuItem item = new JMenuItem(OPEN);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openDevice();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        return item;
    }

    private JMenuItem getCalibrateItem() {
        final JMenuItem item = new JMenuItem(CALIBRATE);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                calibrate();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
        item.setEnabled(false);
        return item;
    }

    private JMenuItem getClearCalibrationItem() {
        final JMenuItem item = new JMenuItem(CLEAR_CALIBRATION);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearCalibration();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        return item;
    }

    private JMenuItem getCloseItem() {
        final JMenuItem item = new JMenuItem(CLOSE);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeDevice();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        item.setEnabled(false);
        return item;
    }

    private void stopRepainter() {
        Thread thread = new Thread() {

            public void run() {
                snooze(1500);
                repainter.cancel();
                isRepainting = false;
            }
        };
        thread.start();
    }

    private void setEnableItem(String menuItem, boolean enabled) {
        JMenu deviceMenu = getMenu(getDeviceDescription());
        JMenuItem item = getMenuItem(deviceMenu, menuItem);
        item.setEnabled(enabled);
    }

    private void openDevice() {
        try {
            device.openDevice();
        } catch (EEGException e) {
            GuiUtil.handleProblem(e);
        }
        if (device.isOpen()) {
            startSignals();
            enableDeviceMenu(false);
            setEnableItem(OPEN, false);
            setEnableItem(CHOOSE_SIGNAL_PROCESSOR, false);
            setEnableItem(SET_SAMPLE_FREQUENCY, false);
            setEnableItem(CALIBRATE, true);
            setEnableItem(CLOSE, true);
            startRepainter();
        }
    }

    private void enableDeviceMenu(boolean b) {
        JMenu file = getMenu(FILE_MENU);
        JMenuItem devices = getMenuItem(file, SELECT_DEVICE);
        devices.setEnabled(b);
    }

    private void startRepainter() {
        if (!isRepainting) {
            repainter.playLoop(RepeatBehavior.LOOP);
            isRepainting = true;
        }
    }

    private void closeDevice() {
        try {
            device.closeDevice();
        } catch (EEGException e) {
            GuiUtil.handleProblem(e);
        }
        if (!device.isOpen()) {
            enableDeviceMenu(true);
            setEnableItem(CLOSE, false);
            setEnableItem(OPEN, true);
            setEnableItem(CHOOSE_SIGNAL_PROCESSOR, true);
            setEnableItem(SET_SAMPLE_FREQUENCY, true);
            setEnableItem(CALIBRATE, false);
            stopRepainter();
        }
    }

    private void calibrate() {
        try {
            setEnableItem(CALIBRATE, false);
            setEnableItem(CLEAR_CALIBRATION, false);
            setEnableItem(CLOSE, false);
            glassPane.start();
            device.calibrate();
            Thread thread = new Thread() {

                public void run() {
                    while (device.isCalibrating()) {
                        snooze(50);
                    }
                    setEnableItem(CALIBRATE, true);
                    setEnableItem(CLEAR_CALIBRATION, true);
                    setEnableItem(CLOSE, true);
                    glassPane.stop();
                }
            };
            thread.start();
        } catch (EEGException e) {
            GuiUtil.handleProblem(e);
        }
    }

    private void clearCalibration() {
        try {
            device.clearCalibration();
        } catch (EEGException e) {
            GuiUtil.handleProblem(e);
        }
    }

    private JMenu getFileMenu() {
        JMenu file = new JMenu(FILE_MENU);
        file.add(getDeviceMenu());
        file.add(getExitMenuItem());
        return file;
    }

    private JMenuItem getDeviceMenu() {
        JMenu menu = new JMenu(SELECT_DEVICE);
        for (EEGDevice device : devices) {
            menu.add(addSelectDeviceMenuItem(device));
        }
        return menu;
    }

    private JMenuItem addSelectDeviceMenuItem(final EEGDevice dev) {
        JMenuItem item = new JMenuItem(dev.getDeviceDescription());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                switchDevices(dev);
            }
        });
        return item;
    }

    private void switchDevices(EEGDevice dev) {
        if (dev == device) {
            return;
        }
        String menuText = device.getDeviceDescription();
        JMenu menu = getMenu(menuText);
        menu.setText(dev.getDeviceDescription());
        TextLayout layout = new TextLayout(dev.getDeviceDescription(), menu.getFont(), ((Graphics2D) menu.getGraphics()).getFontRenderContext());
        menu.setSize(new Dimension((int) layout.getBounds().getWidth() + 10, (int) layout.getBounds().getHeight()));
        getJMenuBar().repaint();
        if (device.isOpen()) {
            try {
                device.closeDevice();
            } catch (EEGException e) {
                GuiUtil.handleProblem(e);
            }
        }
        this.device = dev;
        addStatesToDevice();
        device.notifyDeviceInfo();
    }

    private JMenu getMenu(String menuText) {
        JMenuBar bar = getJMenuBar();
        JMenu menu;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            menu = bar.getMenu(i);
            if (menu.getText().equals(menuText)) {
                return menu;
            }
        }
        return null;
    }

    private JMenuItem getMenuItem(JMenu menu, String itemText) {
        JMenuItem item;
        for (int i = 0; i < menu.getItemCount(); i++) {
            item = menu.getItem(i);
            if (item.getText().equals(itemText)) {
                return item;
            }
        }
        return null;
    }

    private JMenuItem getExitMenuItem() {
        JMenuItem item = new JMenuItem("Exit");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        return item;
    }

    private void exit() {
        if (device.isCalibrating()) {
            return;
        }
        snoozeAndRepaint();
        int choice = JOptionPane.showConfirmDialog(this, "Exiting.  Confirm?", "Exit FingBrainerz", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION) {
            closeDevice();
            saveSettings();
            System.exit(0);
        }
    }

    private void startSignals() {
        deviceThread = new Thread() {

            public void run() {
                while (device.isOpen()) {
                    Utils.snooze(sampleTime);
                    setSignals();
                }
                Utils.snooze(sampleTime);
                haltAnimation();
            }
        };
        deviceThread.start();
    }

    private void haltAnimation() {
        for (FingBrainer fb : fingBrainers) {
            animateFingBrainer(fb, 0);
        }
    }

    private void setSignals() {
        List<EEGChannelValue> values = device.getCurrentChannelValues();
        double normalisedValue = getNormalisedValue(values);
        for (EEGChannelValue value : values) {
            value.setNormalizedFactor(normalisedValue);
            setSignal(value);
        }
    }

    private double getNormalisedValue(List<EEGChannelValue> values) {
        double d = 0;
        for (EEGChannelValue value : values) {
            d += value.getChannelStrengthWithCalibration() * value.getChannelStrengthWithCalibration();
        }
        return Math.sqrt(d);
    }

    private void setSignal(EEGChannelValue value) {
        for (FingBrainer fb : fingBrainers) {
            try {
                if (value.isForFrequencyType(fb.getState().getFrequencyType())) {
                    animateFingBrainer(fb, value.getChannelStrengthWithCalibration());
                    return;
                }
            } catch (NullPointerException e) {
            }
        }
    }

    private void animateFingBrainer(FingBrainer fb, double signalStrength) {
        Timeline tl = new Timeline(fb);
        tl.addPropertyToInterpolate("signalStrength", fb.getSignalStrength(), signalStrength);
        tl.addPropertyToInterpolate("lowColour", fb.getLowColour(), getRandomColour());
        tl.addPropertyToInterpolate("middleColour", fb.getMiddleColour(), getRandomColour());
        tl.addPropertyToInterpolate("highColor", fb.getHighColor(), getRandomColour());
        tl.setDuration(sampleTime);
        fb.setTimeLine(tl);
        fb.animate();
    }

    protected float getRandomPositiveFloat() {
        return rand.nextFloat();
    }

    protected Color getRandomColour() {
        float r = getRandomPositiveFloat();
        float g = getRandomPositiveFloat();
        float b = getRandomPositiveFloat();
        return new Color(r, g, b, 0.75f);
    }

    public static void main(String[] args) {
        initGuiSettings();
        Thread thread = new Thread() {

            public void run() {
                FingBrainerzSplash splash = new FingBrainerzSplash();
                snooze(10000);
                splash.setVisible(false);
                splash.dispose();
                showFingBrainerz();
            }
        };
        thread.run();
    }

    private static void showFingBrainerz() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                FingBrainerz fbz = new FingBrainerz();
                fbz.pack();
                GuiUtil.centerOnScreen(fbz);
                fbz.setVisible(true);
                fbz.snoozeAndRepaint();
            }
        });
    }

    private static void initGuiSettings() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        RepaintManager.setCurrentManager(new RepaintManagerX(RepaintManager.currentManager(null)));
        try {
            UIManager.setLookAndFeel(new SubstanceRavenGraphiteGlassLookAndFeel());
            SubstanceLookAndFeel.setCurrentWatermark(new SubstancePlanktonWatermark());
        } catch (Exception e) {
            GuiUtil.handleProblem(e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception f) {
                GuiUtil.handleProblem(f);
                System.exit(-1);
            }
        }
    }
}
