import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import org.biomage.QuantitationType.*;
import org.biomage.Description.*;
import org.biomage.BioAssay.*;
import org.biomage.Interface.*;

/**
 * FieldConfigPanel corresponds to a row in ColumConfigureFrame.
 *
 *
 */
public class FieldConfigPanel extends JPanel implements MageGuiConstants, MageGuiErrors, Cloneable {

    private static final int NAMETEXT_SIZE = 10;

    private int fieldId;

    private JLabel idLabel;

    private JComboBox typeCombo;

    private JTextField nameText;

    private JComboBox channelCombo;

    private JCheckBox backgroundCBox;

    private JComboBox scaleCombo;

    private JComboBox dTypeCombo;

    private IntTextField confIndText;

    private static int bgboxWidth = 0;

    private static int confIndWidth = 0;

    private static int maxId = 0;

    private QuantitationType quanType;

    private ConfidenceIndicator confIndicator;

    private static ArrayList channels = new ArrayList(2);

    /**
     * Constructs a FieldConfigPanel
     * 
     * @param int id
     *
     */
    public FieldConfigPanel(int id) {
        fieldId = id;
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        setLayout(gbl);
        if (id == 1) {
            for (int j = 0; j < FIELD_NUM; j++) {
                JLabel label = new JLabel(FIELD_NAMES[j], JLabel.CENTER);
                label.setForeground(Color.black);
                label.setFont(new Font("Dialog", Font.BOLD, 12));
                gbc.gridx = j + 1;
                if (FIELD_NAMES[j].equals("Background")) {
                    bgboxWidth = label.getPreferredSize().width;
                } else if (FIELD_NAMES[j].equals("Confidence")) {
                    confIndWidth = label.getPreferredSize().width;
                }
                gbl.setConstraints(label, gbc);
                add(label);
                if (FIELD_NAMES[j].equals("Confidence")) {
                    JLabel secondWord = new JLabel("Indicator", JLabel.CENTER);
                    secondWord.setForeground(Color.black);
                    secondWord.setFont(new Font("Dialog", Font.BOLD, 12));
                    gbc.gridx = j + 1;
                    gbc.gridy = 1;
                    gbc.ipadx = 40;
                    gbl.setConstraints(secondWord, gbc);
                    add(secondWord);
                }
            }
            gbc.gridy = 2;
        }
        gbc.ipadx = 0;
        String idPad = new String();
        if (fieldId < 10) idPad = "0";
        idLabel = new JLabel(idPad + fieldId + ".");
        idLabel.setForeground(Color.black);
        gbc.gridx = 0;
        gbl.setConstraints(idLabel, gbc);
        typeCombo = new JComboBox(TYPES);
        typeCombo.setBackground(bgColor);
        gbc.gridx = 1;
        gbl.setConstraints(typeCombo, gbc);
        nameText = new JTextField(NAMETEXT_SIZE);
        nameText.setEditable(false);
        gbc.gridx = 2;
        gbl.setConstraints(nameText, gbc);
        channelCombo = new JComboBox(CHANNELS);
        channelCombo.setBackground(bgColor);
        channelCombo.setEnabled(false);
        gbc.gridx = 3;
        gbl.setConstraints(channelCombo, gbc);
        backgroundCBox = new JCheckBox();
        backgroundCBox.setBackground(bgColor);
        backgroundCBox.setEnabled(false);
        gbc.gridx = 4;
        Dimension dbox = backgroundCBox.getPreferredSize();
        int leftside = bgboxWidth / 2 - dbox.width / 3;
        gbc.insets = new Insets(0, leftside, 0, bgboxWidth - leftside - dbox.width);
        gbl.setConstraints(backgroundCBox, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        scaleCombo = new JComboBox(SCALES);
        scaleCombo.setEnabled(false);
        scaleCombo.setBackground(bgColor);
        gbc.gridx = 5;
        gbl.setConstraints(scaleCombo, gbc);
        dTypeCombo = new JComboBox(DTYPES);
        dTypeCombo.setEnabled(false);
        dTypeCombo.setBackground(bgColor);
        gbc.gridx = 6;
        gbl.setConstraints(dTypeCombo, gbc);
        confIndText = new IntTextField(2);
        confIndText.setEditable(false);
        gbc.gridx = 7;
        gbc.insets = new Insets(0, confIndWidth / 2, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbl.setConstraints(confIndText, gbc);
        addListeners();
        setBackground(bgColor);
        add(idLabel);
        add(typeCombo);
        add(nameText);
        add(channelCombo);
        add(backgroundCBox);
        add(scaleCombo);
        add(dTypeCombo);
        add(confIndText);
    }

    /**
     * Extracts the core info and builds a Quantype 
     *
     * @returns FieldConfig
     */
    public void makeMageObjects() {
        String type = (String) typeCombo.getSelectedItem();
        String name = nameText.getText();
        boolean isBackground = backgroundCBox.isSelected();
        String chan = (String) channelCombo.getSelectedItem();
        String scale = (String) scaleCombo.getSelectedItem();
        String dType = (String) dTypeCombo.getSelectedItem();
        int confidence = (int) confIndText.getValue();
        try {
            String prefix = "org.biomage.QuantitationType.";
            Class quanClass = Class.forName(prefix + type);
            Class[] paramArr = {};
            Object[] parValArr = {};
            Constructor ctor = quanClass.getConstructor(paramArr);
            quanType = (QuantitationType) ctor.newInstance(parValArr);
            quanType.setName(name);
            quanType.setIdentifier(type + ":" + fieldId);
            if (!chan.equals("N/A")) {
                Channel channel = new Channel();
                channel.setIdentifier("Channel:" + chan);
                channel.setName(chan);
                quanType.setChannel(channel);
                addChannel(channel);
            }
            quanType.setIsBackground(isBackground);
            OntologyEntry ontScale = new OntologyEntry();
            ontScale.setCategory("Scale");
            ontScale.setValue(scale);
            quanType.setScale(ontScale);
            OntologyEntry ontDataType = new OntologyEntry();
            ontDataType.setCategory("DataType");
            ontDataType.setValue(dType);
            quanType.setDataType(ontDataType);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
    }

    /**
     *
     *
     */
    public QuantitationType getQuanType() {
        return quanType;
    }

    /**
     * Implements a kind of error checking
     *
     * @returns boolean
     */
    public int checkValid() {
        int retVal = NO_ERROR;
        String type = (String) typeCombo.getSelectedItem();
        String name = nameText.getText();
        String channel = (String) channelCombo.getSelectedItem();
        String scale = (String) scaleCombo.getSelectedItem();
        String dType = (String) dTypeCombo.getSelectedItem();
        int confidence = (int) confIndText.getValue();
        if (type.equals("<Must Choose>") || name.equals("") || channel.equals("<Must Choose>") || scale.equals("<Must Choose>") || dType.equals("<Must Choose>")) {
            retVal = INCOMPLETE_FIELD;
        } else if ((type.equals("PValue") || type.equals("Error") || type.equals("ExpectedValue")) && ((confidence < 0) || (confidence > maxId) || (confidence == fieldId))) {
            retVal = INVALID_CONF_IND;
        }
        return retVal;
    }

    /**
     *
     *
     */
    public static void setMaxId(int num) {
        maxId = num;
    }

    /**
     *
     *
     */
    public static int getMaxId() {
        return maxId;
    }

    /**
     *
     *
     */
    public int getFieldId() {
        return fieldId;
    }

    /**
     *
     *
     */
    public void setFieldId(int id) {
        fieldId = id;
        String idPad = new String();
        if (fieldId < 10) idPad = "0";
        idLabel.setText(idPad + fieldId + ".");
    }

    /**
     *
     *
     */
    public void addConfidenceIndicator(ConfidenceIndicator confInd) {
        quanType.addToConfidenceIndicators(confInd);
    }

    /**
     *
     *
     */
    public static ArrayList getChannels() {
        return channels;
    }

    /**
     *
     *
     */
    public void copyFrom(FieldConfigPanel fromPanel) {
        typeCombo.setSelectedIndex(fromPanel.typeCombo.getSelectedIndex());
        nameText.setText(fromPanel.nameText.getText());
        channelCombo.setSelectedIndex(fromPanel.channelCombo.getSelectedIndex());
        backgroundCBox.setSelected(fromPanel.backgroundCBox.isSelected());
        scaleCombo.setSelectedIndex(fromPanel.scaleCombo.getSelectedIndex());
        dTypeCombo.setSelectedIndex(fromPanel.dTypeCombo.getSelectedIndex());
        if (fromPanel.getConfIndRef() != 0) {
            confIndText.setValue(fromPanel.getConfIndRef());
        }
    }

    /**
     *
     *
     */
    public void copyFrom(QuantitationType qt) {
        quanType = qt;
        String className = qt.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1, className.length());
        typeCombo.setSelectedItem(className);
        nameText.setText(qt.getName());
        if (qt.getChannel() != null) {
            channelCombo.setSelectedItem(qt.getChannel().getName());
        } else {
            channelCombo.setSelectedItem("N/A");
        }
        backgroundCBox.setSelected(qt.getIsBackground());
        scaleCombo.setSelectedItem(qt.getScale().getValue());
        dTypeCombo.setSelectedItem(qt.getDataType().getValue());
        confIndText.setValue(0);
    }

    /**
     *
     */
    public int getConfIndRef() {
        return (int) confIndText.getValue();
    }

    /**
     *
     */
    public void setConfIndRef(int ref) {
        confIndText.setValue(ref);
    }

    /**
     * Debug purpose
     *
     */
    public String toString() {
        return "ID:" + getFieldId();
    }

    private void addListeners() {
        typeCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String type = (String) typeCombo.getSelectedItem();
                if (type.equals("<Must Choose>")) {
                    nameText.setEditable(false);
                    channelCombo.setEnabled(false);
                    backgroundCBox.setEnabled(false);
                    scaleCombo.setEnabled(false);
                    dTypeCombo.setEnabled(false);
                    ;
                } else {
                    nameText.setEditable(true);
                    channelCombo.setEnabled(true);
                    backgroundCBox.setEnabled(true);
                    scaleCombo.setEnabled(true);
                    dTypeCombo.setEnabled(true);
                    if (type.equals("PValue") || type.equals("Error") || type.equals("ExpectedValue")) {
                        confIndText.setEditable(true);
                    } else {
                        confIndText.setEditable(false);
                    }
                }
            }
        });
        confIndText.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                if (!confIndText.isEditable()) {
                    transferFocus();
                }
            }

            public void focusLost(FocusEvent e) {
                ;
            }
        });
    }

    private void addChannel(Channel channel) {
        boolean found = false;
        Iterator chanIter = channels.iterator();
        while (chanIter.hasNext()) {
            Channel chan = (Channel) chanIter.next();
            String id = chan.getIdentifier();
            if (id.equals(channel.getIdentifier())) {
                found = true;
            }
        }
        if (found == false) {
            channels.add(channel);
        }
    }
}
