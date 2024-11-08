package org.processmining.converting;

import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.processmining.exporting.bpel.BPELExport;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.models.bpel.BPEL;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.framework.ui.MainUI;
import org.processmining.framework.ui.Message;
import org.processmining.framework.util.CenterOnScreen;
import org.processmining.importing.tpn.TpnImport;
import org.processmining.mining.petrinetmining.PetriNetResult;
import java.awt.TextArea;
import java.awt.BorderLayout;

/**
 * <p>
 * Title: BPEL To TPN conversion plug-in
 * </p>
 * 
 * <p>
 * Description: Converts a BPEL process into a Petri net, using the
 * BPEL2PNML/WofBPEL toolset.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author Eric Verbeek
 * @version 1.0
 */
public class BPELToTPN implements ConvertingPlugin {

    /**
	 * Additional ptions for both tools.
	 */
    String bpel2pnmlOptions = "";

    String wofbpelOptions = "";

    public BPELToTPN() {
    }

    public String getName() {
        return "BPEL process to TPN net";
    }

    public String getHtmlDescription() {
        return "http://www.win.tue.nl/~hverbeek/doku.php?id=projects:prom:plug-ins:conversion:bpel2tpn";
    }

    /**
	 * 
	 * @param object
	 *            ProvidedObject Should contain at least one BPEL object.
	 * @return PetriNetResult The BPEL object converted into a PetriNetResult.
	 */
    public PetriNetResult convert(ProvidedObject object) {
        BPEL provided = null;
        LogReader log = null;
        for (int i = 0; provided == null && i < object.getObjects().length; i++) {
            if (object.getObjects()[i] instanceof BPEL) {
                provided = (BPEL) object.getObjects()[i];
            }
            if (object.getObjects()[i] instanceof LogReader) {
                log = (LogReader) object.getObjects()[i];
            }
        }
        if (provided == null) {
            return null;
        }
        return result(provided);
    }

    /**
	 * 
	 * @param object
	 *            ProvidedObject
	 * @return boolean whether object contains a BPEL object.
	 */
    public boolean accepts(ProvidedObject object) {
        for (int i = 0; i < object.getObjects().length; i++) {
            if (object.getObjects()[i] instanceof BPEL) {
                return true;
            }
        }
        return false;
    }

    /**
	 * 
	 * @param source
	 *            BPEL The BPEL object.
	 * @return PetriNetResult The BPEL object converted into a PetriNetResult.
	 */
    public PetriNetResult result(BPEL source) {
        Object[] objects = new Object[] { source };
        ProvidedObject object = new ProvidedObject("temp", objects);
        BPELExport exportPlugin = new BPELExport();
        TpnImport importPlugin = new TpnImport();
        PetriNetResult target = null;
        BPELToTPNUI ui = new BPELToTPNUI(this);
        ui.setVisible(true);
        try {
            File bpelFile = File.createTempFile("pmt", ".bpel");
            bpelFile.deleteOnExit();
            FileOutputStream stream = new FileOutputStream(bpelFile);
            exportPlugin.export(object, stream);
            String cmd0 = null, cmd1 = null;
            String pnmlFilePath = bpelFile.getCanonicalPath().replaceFirst(".bpel", "BPEL2PNML.xml");
            if (System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
                cmd0 = "javaw -jar lib" + System.getProperty("file.separator") + "plugins" + System.getProperty("file.separator") + "BPEL2PNML.jar " + bpel2pnmlOptions + "\"" + bpelFile.getCanonicalPath() + "\"";
                cmd1 = "lib" + System.getProperty("file.separator") + "plugins" + System.getProperty("file.separator") + "wofbpel2tpn.exe " + wofbpelOptions + "\"" + pnmlFilePath + "\"";
            }
            if (cmd0 != null && cmd1 != null) {
                Process process0 = Runtime.getRuntime().exec(cmd0);
                StreamReader inputReader = new StreamReader(process0.getInputStream());
                StreamReader errorReader = new StreamReader(process0.getErrorStream());
                inputReader.start();
                errorReader.start();
                Message.add(inputReader.getResult(), Message.DEBUG);
                Message.add(errorReader.getResult(), Message.ERROR);
                process0.waitFor();
                Message.add("<BPELPNML options=\"" + bpel2pnmlOptions + "\"/>", Message.TEST);
                Message.add("<WofBPEL options=\"" + wofbpelOptions + "\"/>", Message.TEST);
                Process process1 = Runtime.getRuntime().exec(cmd1);
                target = importPlugin.importFile(process1.getInputStream());
            } else {
                Message.add("Unable to execute bpel2tpn on this platform: " + System.getProperty("os.name", ""), Message.DEBUG);
                target = new PetriNetResult(new PetriNet());
            }
        } catch (Exception e) {
            Message.add("Unable to convert BPEL process to TPN Net: " + e.toString(), Message.DEBUG);
        }
        return target;
    }

    /**
	 * 
	 * @param version
	 *            String Which version of BPEL is to be expected. Should be
	 *            either "1.1"or "2.0".
	 */
    public void setBPEL(String version) {
        if (version.equalsIgnoreCase("1.1")) {
            bpel2pnmlOptions += "B1.1 ";
        }
    }

    /**
	 * 
	 * @param reduce
	 *            boolean Whether to use Murata-based reduction rules on the
	 *            resulting Petri net.
	 */
    public void setReduce(boolean reduce) {
        if (reduce) {
            wofbpelOptions += "+r ";
        }
    }

    /**
	 * 
	 * <p>
	 * Title: StreamReader
	 * </p>
	 * 
	 * <p>
	 * Description: Reads a stream using a separate thread. Used to read
	 * BPEL2PNML's stdout and stderr, as BPEL2PNML might block on writing these
	 * streams while we're weaiting for it to complete.
	 * </p>
	 */
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

/**
 * 
 * <p>
 * Title: BPELToTPNUI
 * </p>
 * 
 * <p>
 * Description: Dialog to obtain available options for the BPELToTPN conversion.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author Eric Verbeek
 * @version 1.0
 */
class BPELToTPNUI extends JDialog implements ActionListener {

    BPELToTPN bpeltotpn;

    HashSet<Place> sourcePlaces;

    HashSet<Place> sinkPlaces;

    JComboBox bpelCombo;

    Checkbox reduceCheckbox;

    JButton doneButton;

    public BPELToTPNUI(BPELToTPN bpeltotpn) {
        super(MainUI.getInstance(), "Select options...", true);
        this.bpeltotpn = bpeltotpn;
        this.sourcePlaces = sourcePlaces;
        this.sinkPlaces = sinkPlaces;
        try {
            setUndecorated(false);
            jbInit();
            pack();
            CenterOnScreen.center(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        JPanel panel = new JPanel();
        JPanel aboutPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        panel.setLayout(layout);
        String[] options = { "1.1", "2.0" };
        bpelCombo = new JComboBox(options);
        Label bpelLabel = new Label("BPEL version:");
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        layout.setConstraints(bpelLabel, constraints);
        panel.add(bpelLabel);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 1;
        constraints.gridy = 0;
        layout.setConstraints(bpelCombo, constraints);
        panel.add(bpelCombo);
        reduceCheckbox = new Checkbox("");
        Label reduceLabel = new Label("simplify");
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 1;
        layout.setConstraints(reduceLabel, constraints);
        panel.add(reduceLabel);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 1;
        constraints.gridy = 1;
        layout.setConstraints(reduceCheckbox, constraints);
        panel.add(reduceCheckbox);
        doneButton = new JButton("Done");
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 1;
        constraints.gridy = 2;
        layout.setConstraints(doneButton, constraints);
        panel.add(doneButton);
        doneButton.addActionListener(this);
        String aboutBPEL2PNML = "This plug-in uses BPEL2PNML and WofBPEL.\n\n";
        aboutBPEL2PNML += "BPEL2PNML is a software developed by Stephan Breutel, Chun Ouyang and Marlon Dumas. ";
        aboutBPEL2PNML += "It is provided on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n\n";
        aboutBPEL2PNML += "WofBPEL was written by Eric Verbeek.\n";
        aboutBPEL2PNML += "See http://www.bpm.fit.qut.edu.au/projects/babel/tools/ for more information.\n\n";
        aboutBPEL2PNML += "Copyright (C) 2006 Eric Verbeek.";
        TextArea text = new TextArea(aboutBPEL2PNML, 10, 50, TextArea.SCROLLBARS_NONE);
        text.setEditable(false);
        aboutPanel.add(text);
        this.setLayout(new BorderLayout());
        this.add(panel, BorderLayout.CENTER);
        this.add(aboutPanel, BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == doneButton) {
            bpeltotpn.setBPEL((String) bpelCombo.getSelectedItem());
            bpeltotpn.setReduce(reduceCheckbox.getState());
            dispose();
        }
    }
}
