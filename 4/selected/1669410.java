package net.sourceforge.jcoupling2.stresstest;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import net.sourceforge.jcoupling2.persistence.Message;

public class MessageGenerator {

    private int widthLabel = 75;

    private int widthComboBox = 125;

    private int coordXLabel = 25;

    private int coordXComboBox = 125;

    private int coordY = 100;

    private int height = 25;

    JFrame frame;

    Container container;

    JButton btn1, btn2, btn3, btn4;

    JComboBox channelBox, patientBox, locationBox, deviceBox, staffBox, infoBox, staffNameBox;

    JLabel patientLabel, channelLabel, locationLabel, deviceLabel, staffLabel, infoLabel, headlineLabel, staffNameLabel;

    JScrollPane scrollPane;

    JTable table;

    DefaultTableModel model;

    String columnNames[] = { "Messages" };

    MessageData data = null;

    MessageTool tool = null;

    public MessageGenerator(MessageTool tool) {
        this.tool = tool;
        this.data = tool.getMessageData();
        buildGui();
    }

    public JFrame getFrame() {
        return frame;
    }

    private void buildGui() {
        frame = new JFrame("Message generator");
        frame.setSize(900, 575);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        headlineLabel = new JLabel("Properties:");
        headlineLabel.setBounds(coordXLabel, 50, 150, height);
        headlineLabel.setFont(new Font("Serif", Font.ITALIC, height));
        channelLabel = new JLabel("Channel");
        channelLabel.setBounds(coordXLabel, coordY, widthLabel, height);
        channelBox = new JComboBox(data.getChannels());
        channelBox.setBounds(coordXComboBox, coordY, widthComboBox, height);
        patientLabel = new JLabel("Patient");
        patientLabel.setBounds(coordXLabel, coordY + 50, widthLabel, height);
        patientBox = new JComboBox(data.getPatients());
        patientBox.setBounds(coordXComboBox, coordY + 50, widthComboBox, height);
        locationLabel = new JLabel("Location");
        locationLabel.setBounds(coordXLabel, coordY + 100, widthLabel, height);
        locationBox = new JComboBox(data.getLocations());
        locationBox.setBounds(coordXComboBox, coordY + 100, widthComboBox, height);
        deviceLabel = new JLabel("Device");
        deviceLabel.setBounds(coordXLabel, coordY + 150, widthLabel, height);
        deviceBox = new JComboBox(data.getDevices());
        deviceBox.setBounds(coordXComboBox, coordY + 150, widthComboBox, height);
        staffLabel = new JLabel("Staff");
        staffLabel.setBounds(coordXLabel, coordY + 200, widthLabel, height);
        staffBox = new JComboBox(data.getStaff());
        staffBox.setBounds(coordXComboBox, coordY + 200, widthComboBox, height);
        staffNameLabel = new JLabel("Name Staff");
        staffNameLabel.setBounds(coordXLabel, coordY + 250, widthLabel, height);
        staffNameBox = new JComboBox(data.getStaffNames());
        staffNameBox.setBounds(coordXComboBox, coordY + 250, widthComboBox, height);
        infoLabel = new JLabel("Information");
        infoLabel.setBounds(coordXLabel, coordY + 300, widthLabel, height);
        infoBox = new JComboBox(data.getInformation());
        infoBox.setBounds(coordXComboBox, coordY + 300, widthComboBox, height);
        btn1 = new JButton(">>");
        btn1.setBounds(300, 200, 75, height);
        btn1.addActionListener(new btn1ActionListener());
        btn2 = new JButton("<<");
        btn2.setBounds(300, 275, 75, height);
        btn2.addActionListener(new btn2ActionListener());
        btn3 = new JButton("Done");
        btn3.setBounds(75, coordY + 350, 75, height);
        btn3.addActionListener(new btn3ActionListener());
        btn4 = new JButton("Clear");
        btn4.setBounds(200, coordY + 350, 75, height);
        btn4.addActionListener(new btn4ActionListener());
        model = new DefaultTableModel(columnNames, 0);
        class MyTableCellRenderer extends JTextArea implements TableCellRenderer {

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (isSelected) {
                    setForeground(table.getSelectionForeground());
                    setBackground(table.getSelectionBackground());
                } else {
                    setForeground(table.getForeground());
                    setBackground(table.getBackground());
                }
                setWrapStyleWord(true);
                setLineWrap(true);
                setText(value.toString());
                return this;
            }
        }
        table = new JTable(model);
        table.setSize(400, 400);
        table.setRowHeight(180);
        table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
        scrollPane = new JScrollPane(table);
        scrollPane.setBounds(425, 50, 400, 450);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        container = new Container();
        container.add(btn1);
        container.add(btn2);
        container.add(btn3);
        container.add(btn4);
        container.add(patientBox);
        container.add(patientLabel);
        container.add(channelBox);
        container.add(channelLabel);
        container.add(locationBox);
        container.add(locationLabel);
        container.add(deviceBox);
        container.add(deviceLabel);
        container.add(staffBox);
        container.add(staffLabel);
        container.add(infoBox);
        container.add(infoLabel);
        container.add(scrollPane);
        container.add(headlineLabel);
        container.add(staffNameLabel);
        container.add(staffNameBox);
        frame.setContentPane(container);
        frame.setVisible(false);
    }

    class btn1ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            String message = "<JCMessage>" + "<tasktype>Message</tasktype>" + "<TimeStamp>" + data.getTimeStamp() + "</TimeStamp>" + "<Channel>" + (channelBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getChannels()) : channelBox.getSelectedItem().toString()) + "</Channel>" + "<location>" + (locationBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getLocations()) : locationBox.getSelectedItem().toString()) + "</location>" + "<patient>" + (patientBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getPatients()) : patientBox.getSelectedItem().toString()) + "</patient>" + "<device>" + (deviceBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getDevices()) : deviceBox.getSelectedItem().toString()) + "</device>" + "<staff>" + (staffBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getStaff()) : staffBox.getSelectedItem().toString()) + "</staff>" + "<staffName>" + (staffNameBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getStaffNames()) : staffNameBox.getSelectedItem().toString()) + "	</staffName>" + "<information>" + (infoBox.getSelectedItem().toString().equals("Random") ? data.getRandomEntry(data.getInformation()) : infoBox.getSelectedItem().toString()) + "</information>" + "</JCMessage>";
            data.getBuffer().add(message);
            String colData[] = { data.formatString(message) };
            model.insertRow(0, colData);
        }
    }

    class btn2ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int rowIndex = table.getSelectedRow();
            if (rowIndex != -1) {
                data.unlearnMessage((String) model.getValueAt(rowIndex, 0));
                model.removeRow(rowIndex);
            }
        }
    }

    class btn3ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            tool.setVisible(true);
            tool.setMessageField(new Integer(data.getBuffer().size()).toString());
            frame.setVisible(false);
        }
    }

    class btn4ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            tool.setMessageField(new Integer(data.getBuffer().size()).toString());
            model = new DefaultTableModel(columnNames, 0);
            table.setModel(model);
            data.getBuffer().clear();
        }
    }

    public MessageData getMessageData() {
        return data;
    }
}
