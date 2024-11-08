package pos.test;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.ParallelPort;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author MaGicBank
 */
public class CommPortOpen {

    /** How long to wait for the open to finish up. */
    public static final int TIMEOUTSECONDS = 30;

    /** The baud rate to use. */
    public static final int BAUD = 19200;

    /** The parent JFrame, for the chooser. */
    protected JFrame parent;

    /** The input stream */
    protected BufferedReader is;

    /** The output stream */
    protected PrintStream os;

    /** The chosen Port Identifier */
    CommPortIdentifier thePortID;

    /** The chosen Port itself */
    CommPort thePort;

    public static void main(String[] argv) throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        new CommPortOpen(null).converse();
        System.exit(0);
    }

    public CommPortOpen(JFrame f) throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        PortChooser chooser = new PortChooser(null);
        String portName = null;
        do {
            chooser.setVisible(true);
            portName = chooser.getSelectedName();
            if (portName == null) {
                System.out.println("No port selected. Try again.\n");
            }
        } while (portName == null);
        thePortID = chooser.getSelectedIdentifier();
        System.out.println("Trying to open " + thePortID.getName() + "...");
        switch(thePortID.getPortType()) {
            case CommPortIdentifier.PORT_SERIAL:
                thePort = thePortID.open("DarwinSys DataComm", TIMEOUTSECONDS * 1000);
                SerialPort myPort = (SerialPort) thePort;
                myPort.setSerialPortParams(BAUD, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                break;
            case CommPortIdentifier.PORT_PARALLEL:
                thePort = thePortID.open("DarwinSys Printing", TIMEOUTSECONDS * 1000);
                ParallelPort pPort = (ParallelPort) thePort;
                int mode = pPort.getMode();
                switch(mode) {
                    case ParallelPort.LPT_MODE_ECP:
                        System.out.println("Mode is: ECP");
                        break;
                    case ParallelPort.LPT_MODE_EPP:
                        System.out.println("Mode is: EPP");
                        break;
                    case ParallelPort.LPT_MODE_NIBBLE:
                        System.out.println("Mode is: Nibble Mode.");
                        break;
                    case ParallelPort.LPT_MODE_PS2:
                        System.out.println("Mode is: Byte mode.");
                        break;
                    case ParallelPort.LPT_MODE_SPP:
                        System.out.println("Mode is: Compatibility mode.");
                        break;
                    default:
                        throw new IllegalStateException("Parallel mode " + mode + " invalid.");
                }
                break;
            default:
                throw new IllegalStateException("Unknown port type " + thePortID);
        }
        try {
            is = new BufferedReader(new InputStreamReader(thePort.getInputStream()));
        } catch (IOException e) {
            System.err.println("Can't open input stream: write-only");
            is = null;
        }
        os = new PrintStream(thePort.getOutputStream(), true);
    }

    /** This method will be overridden by non-trivial subclasses
     * to hold a conversation. 
     */
    protected void converse() throws IOException {
        System.out.println("Ready to read and write port.");
        if (is != null) {
            is.close();
        }
        os.close();
    }
}

class PortChooser extends JDialog implements ItemListener {

    /** A mapping from names to CommPortIdentifiers. */
    protected HashMap map = new HashMap();

    /** The name of the choice the user made. */
    protected String selectedPortName;

    /** The CommPortIdentifier the user chose. */
    protected CommPortIdentifier selectedPortIdentifier;

    /** The JComboBox for serial ports */
    protected JComboBox serialPortsChoice;

    /** The JComboBox for parallel ports */
    protected JComboBox parallelPortsChoice;

    /** The JComboBox for anything else */
    protected JComboBox other;

    /** The SerialPort object */
    protected SerialPort ttya;

    /** To display the chosen */
    protected JLabel choice;

    /** Padding in the GUI */
    protected final int PAD = 5;

    /** This will be called from either of the JComboBoxen when the
     * user selects any given item.
     */
    public void itemStateChanged(ItemEvent e) {
        selectedPortName = (String) ((JComboBox) e.getSource()).getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier) map.get(selectedPortName);
        choice.setText(selectedPortName);
    }

    public String getSelectedName() {
        return selectedPortName;
    }

    public CommPortIdentifier getSelectedIdentifier() {
        return selectedPortIdentifier;
    }

    /** A test program to show up this chooser. */
    public static void main(String[] ap) {
        PortChooser c = new PortChooser(null);
        c.setVisible(true);
        System.out.println("You chose " + c.getSelectedName() + " (known by " + c.getSelectedIdentifier() + ").");
        System.exit(0);
    }

    /** Construct a PortChooser --make the GUI and populate the ComboBoxes.
     */
    public PortChooser(JFrame parent) {
        super(parent, "Port Chooser", true);
        makeGUI();
        populate();
        finishGUI();
    }

    /** Build the GUI. You can ignore this for now if you have not
     * yet worked through the GUI chapter. Your mileage may vary.
     */
    protected void makeGUI() {
        Container cp = getContentPane();
        JPanel centerPanel = new JPanel();
        cp.add(BorderLayout.CENTER, centerPanel);
        centerPanel.setLayout(new GridLayout(0, 2, PAD, PAD));
        centerPanel.add(new JLabel("Serial Ports", JLabel.RIGHT));
        serialPortsChoice = new JComboBox();
        centerPanel.add(serialPortsChoice);
        serialPortsChoice.setEnabled(false);
        centerPanel.add(new JLabel("Parallel Ports", JLabel.RIGHT));
        parallelPortsChoice = new JComboBox();
        centerPanel.add(parallelPortsChoice);
        parallelPortsChoice.setEnabled(false);
        centerPanel.add(new JLabel("Unknown Ports", JLabel.RIGHT));
        other = new JComboBox();
        centerPanel.add(other);
        other.setEnabled(false);
        centerPanel.add(new JLabel("Your choice:", JLabel.RIGHT));
        centerPanel.add(choice = new JLabel());
        JButton okButton;
        cp.add(BorderLayout.SOUTH, okButton = new JButton("OK"));
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PortChooser.this.dispose();
            }
        });
    }

    /** Populate the ComboBoxes by asking the Java Communications API
     * what ports it has.  Since the initial information comes from
     * a Properties file, it may not exactly reflect your hardware.
     */
    protected void populate() {
        Enumeration pList = CommPortIdentifier.getPortIdentifiers();
        while (pList.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier) pList.nextElement();
            map.put(cpi.getName(), cpi);
            if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                serialPortsChoice.setEnabled(true);
                serialPortsChoice.addItem(cpi.getName());
            } else if (cpi.getPortType() == CommPortIdentifier.PORT_PARALLEL) {
                parallelPortsChoice.setEnabled(true);
                parallelPortsChoice.addItem(cpi.getName());
            } else {
                other.setEnabled(true);
                other.addItem(cpi.getName());
            }
        }
        serialPortsChoice.setSelectedIndex(-1);
        parallelPortsChoice.setSelectedIndex(-1);
    }

    protected void finishGUI() {
        serialPortsChoice.addItemListener(this);
        parallelPortsChoice.addItemListener(this);
        other.addItemListener(this);
        pack();
    }
}
