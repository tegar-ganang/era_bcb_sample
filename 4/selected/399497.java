package gov.sns.apps.mtv;

import javax.swing.*;
import java.beans.*;
import java.awt.*;
import com.cosylab.gui.components.Wheelswitch;
import gov.sns.ca.*;

/**
 * The window representation / view of an xiodiag document
 *
 * @author  jdg
 */
public class WheelPanel extends JPanel {

    private MTVDocument theDoc;

    private JLabel pvLabel, upperLabel, lowerLabel, restoreLabel;

    protected Wheelswitch pvWheel;

    private Number upperLimit, lowerLimit;

    private JButton restoreButton;

    private double restoreValue;

    private JTextField pvInputField;

    private PropertyChangeListener wheelListener;

    protected Channel theChannel;

    /** Creates a new instance of MainWindow */
    public WheelPanel(MTVDocument aDocument) {
        theDoc = aDocument;
        makeContent();
        updateWheelListener();
    }

    protected void makeContent() {
        GridBagLayout gridBag = new GridBagLayout();
        this.setLayout(gridBag);
        int sumy = 0;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.;
        gbc.weighty = 0.;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        JLabel label1 = new JLabel("Manual PV Entry:");
        gridBag.setConstraints(label1, gbc);
        add(label1);
        pvInputField = new JTextField("Put PV Name Here");
        pvInputField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                String name = pvInputField.getText();
                setChannel(name);
            }
        });
        gbc.gridx = 1;
        gridBag.setConstraints(pvInputField, gbc);
        add(pvInputField);
        gbc.gridx = 0;
        gbc.gridy = sumy;
        gbc.gridheight = 2;
        pvLabel = new JLabel("Null");
        gridBag.setConstraints(pvLabel, gbc);
        add(pvLabel);
        pvWheel = new Wheelswitch();
        pvWheel.setFormat("+###.######");
        gbc.gridx = 1;
        gbc.gridy = sumy;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.gridwidth = 1;
        gridBag.setConstraints(pvWheel, gbc);
        add(pvWheel);
        sumy += 2;
        upperLabel = new JLabel("upper  lim = ");
        lowerLabel = new JLabel("lower  lim = ");
        gbc.gridx = 0;
        gbc.gridy = sumy;
        gbc.gridheight = 1;
        gridBag.setConstraints(upperLabel, gbc);
        add(upperLabel);
        gbc.gridx = 1;
        gbc.gridy = sumy++;
        gridBag.setConstraints(lowerLabel, gbc);
        add(lowerLabel);
        restoreButton = new JButton("Restore Original Value");
        restoreButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (!(new Double(restoreValue)).isNaN()) {
                    try {
                        pvWheel.setValue(restoreValue);
                    } catch (Exception ex) {
                        System.out.println("trouble sending to " + theChannel.getId());
                    }
                }
            }
        });
        gbc.gridx = 0;
        gbc.gridy = sumy;
        gridBag.setConstraints(restoreButton, gbc);
        add(restoreButton);
        restoreLabel = new JLabel("null");
        gbc.gridx = 1;
        gbc.gridy = sumy++;
        gridBag.setConstraints(restoreLabel, gbc);
        add(restoreLabel);
    }

    /** a listener to send updated values to a channel */
    private void updateWheelListener() {
        wheelListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                double val = pvWheel.getValue();
                if (theChannel != null) {
                    try {
                        System.out.println(theChannel.getId() + "  " + val);
                        theChannel.putVal(val);
                    } catch (Exception ex) {
                        System.out.println("trouble sending new value to channel theChannel.getId()");
                    }
                }
            }
        };
    }

    protected void setChannel(Channel chan) {
        try {
            pvWheel.removePropertyChangeListener("value", wheelListener);
            theChannel = chan;
            setLimits(chan);
            updateWheelListener();
            pvWheel.addPropertyChangeListener("value", wheelListener);
        } catch (Exception exc) {
            theChannel = null;
            System.out.println("cannot setup channel named " + chan.getId());
        }
    }

    protected void setChannel(String name) {
        try {
            pvWheel.removePropertyChangeListener("value", wheelListener);
            theChannel = ChannelFactory.defaultFactory().getChannel(name);
            setLimits(theChannel);
            updateWheelListener();
            pvWheel.addPropertyChangeListener("value", wheelListener);
        } catch (Exception exc) {
            theChannel = null;
            System.out.println("cannot make channel named " + name);
        }
    }

    private void setLimits(Channel chan) throws ConnectionException, GetException {
        double val = chan.getValDbl();
        pvWheel.setValue(val);
        pvLabel.setText(chan.getId());
        restoreValue = val;
        restoreLabel.setText((new Double(val)).toString());
        if (chan != null) {
            upperLimit = chan.upperControlLimit();
            upperLabel.setText("Upper Lim = " + upperLimit);
            lowerLimit = chan.lowerControlLimit();
            lowerLabel.setText("Lower Lim = " + lowerLimit.toString());
            System.out.println("upper = " + upperLimit);
        } else {
            upperLabel.setText("upper Lim = ");
            lowerLabel.setText("lower Lim = ");
        }
    }
}
