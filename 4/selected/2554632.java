package view;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import view.View.MY_EVENT;
import model.Packet;

@SuppressWarnings("serial")
public class ComboBar extends JPanel implements ActionListener {

    private MyDropDown staDropDown;

    private MyDropDown apDropDown;

    private MyDropDown rssiDropDown;

    private MyDropDown dirDropDown;

    private MyDropDown channelDropDown;

    private int detailsLevel = 1;

    ArrayList<String> staList;

    ArrayList<String> apList;

    private View view;

    public static enum DIRECTION {

        normal("Both"), uplink("Uplink"), downlink("Downlink");

        private final String s;

        DIRECTION(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

    public ComboBar(View view, ItemListener dropDownHandler, ItemListener radioButtonHandler) {
        this.view = view;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel jp1 = new JPanel();
        jp1.setBorder(BorderFactory.createTitledBorder("Filter"));
        jp1.add(rssiDropDown = new MyDropDown(dropDownHandler, getRssiList(), "RSSI Filter", "FilterSettingsChanged"));
        jp1.add(channelDropDown = new MyDropDown(dropDownHandler, getChannelList(), "Channel Filter", "FilterSettingsChanged"));
        jp1.add(dirDropDown = new MyDropDown(dropDownHandler, DIRECTION.values(), "Show data unicast both directions or selected direction only", "FilterSettingsChanged"));
        jp1.add(staDropDown = new MyDropDown(dropDownHandler, getStaList(), "STA Filter", "FilterSettingsChanged"));
        jp1.add(apDropDown = new MyDropDown(dropDownHandler, getApList(), "AP Filter", "FilterSettingsChanged"));
        JPanel jp2 = new JPanel();
        jp2.setBorder(BorderFactory.createTitledBorder("Details level"));
        ButtonGroup bg = new ButtonGroup();
        JRadioButton rb1 = new JRadioButton();
        addButton(jp2, bg, rb1, "1", "Hide details");
        addButton(jp2, bg, new JRadioButton(), "2", "Add sequence numbers and header details at higher zoom levels");
        addButton(jp2, bg, new JRadioButton(), "3", "Add transaction lines at higher zoom levels");
        addButton(jp2, bg, new JRadioButton(), "4", "Show transactions");
        addButton(jp2, bg, new JRadioButton(), "5", "Show fill packets at higher zoom levels");
        rb1.setSelected(true);
        add(jp1);
        add(jp2);
    }

    private void addButton(JPanel jp, ButtonGroup bg, JRadioButton rb, String name, String tip) {
        rb.setText(name);
        rb.setToolTipText(tip);
        rb.addActionListener(this);
        jp.add(rb);
        bg.add(rb);
    }

    public void update(ArrayList<String> apList, ArrayList<String> staList) {
        this.apList = apList;
        this.staList = staList;
        staDropDown.removeAllItems();
        for (String s : getStaList()) staDropDown.addItem(s);
        staDropDown.setSelectedIndex(0);
        apDropDown.removeAllItems();
        for (String s : getApList()) apDropDown.addItem(s);
        apDropDown.setSelectedIndex(0);
    }

    public class MyDropDown extends JComboBox {

        public MyDropDown(ItemListener handler, String[] choices, String toolTip, String command) {
            super(choices);
            setToolTipText(toolTip);
            addItemListener(handler);
            setActionCommand(command);
        }

        public MyDropDown(ItemListener handler, DIRECTION[] values, String toolTip, String command) {
            super(values);
            setToolTipText(toolTip);
            addItemListener(handler);
            setActionCommand(command);
        }
    }

    private String[] getStaList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("STA");
        if (staList != null) list.addAll(staList);
        return list.toArray(new String[list.size()]);
    }

    private String[] getApList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("AP");
        if (apList != null) list.addAll(apList);
        return list.toArray(new String[list.size()]);
    }

    private static String[] getRssiList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("RSSI");
        for (int i = 100; i > -100; i -= 10) list.add(Integer.toString(i));
        return list.toArray(new String[list.size()]);
    }

    private static String[] getChannelList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Ch");
        for (int i = 1; i <= 13; i++) list.add(Integer.toString(i));
        return list.toArray(new String[list.size()]);
    }

    public int getRssiFilterValue() {
        String s = rssiDropDown.getSelectedItem().toString();
        if (!s.equals("RSSI")) return Integer.parseInt(s); else return 0;
    }

    public int getChannelFilterValue() {
        String s = channelDropDown.getSelectedItem().toString();
        if (!s.equals("Ch")) return Integer.parseInt(s); else return 0;
    }

    public long getStaFilterValue() {
        String s = staDropDown.getSelectedItem().toString();
        if (!s.equals("STA")) return Packet.macToLong(s.split(" ")[0]); else return 0;
    }

    public long getApFilterValue() {
        String s = apDropDown.getSelectedItem().toString();
        if (!s.equals("AP")) return Packet.macToLong(s); else return 0;
    }

    public DIRECTION getDirValue() {
        return (DIRECTION) dirDropDown.getSelectedItem();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        detailsLevel = Integer.parseInt(e.getActionCommand());
        view.fireMyEvent(MY_EVENT.REDRAW);
    }

    public boolean showPacketDetails() {
        return (detailsLevel >= 2);
    }

    public boolean showTransactionLines() {
        return (detailsLevel >= 3);
    }

    public boolean showTransactions() {
        return (detailsLevel >= 4);
    }

    public boolean showFillPackets() {
        return (detailsLevel >= 5);
    }
}
