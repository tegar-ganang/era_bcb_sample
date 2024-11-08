import NCParser.*;
import NCScale.NCScale;
import NCWriter.NCWriter.*;
import NCData.NCProfileInfo.*;
import NCUi.*;
import NCUi.NCMsgBox.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;

public class NCManager implements ItemListener, MouseListener {

    protected static final int MODE_SRC_OPEN = 1;

    protected static final int MODE_DEST_OPEN = 2;

    private JFrame frame;

    private String nc_file_name;

    private String new_nc_file_name;

    private int current_profile;

    private int[] selected_profiles;

    private NCProfiles profiles;

    public JProgressBar progress_bar;

    private List list;

    private JTextField txt_tol;

    private JButton bt_tol;

    private JButton bt_makeNC;

    private JButton bt_open_src_file;

    private JButton bt_open_dest_file;

    private JTextField txt_src_file;

    private JTextField txt_dest_file;

    private JButton bt_Read_nc;

    private JButton bt_Div100;

    private NCWriter ncwriter;

    private NCScale ncscale;

    public NCManager() {
        current_profile = -1;
        profiles = new NCProfiles();
        ncwriter = new NCWriter();
        ncscale = new NCScale();
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() instanceof List) {
            List listTmp = (List) e.getSource();
            String str;
            str = (String) listTmp.getSelectedItem();
            System.out.println(str);
            txt_tol.setText(getTolFromProfile(str));
        }
    }

    public void mouseClicked(MouseEvent me) {
        if (me.getSource() instanceof JButton) {
            JButton btTmp = (JButton) me.getSource();
            if (btTmp.getName().equals(new String("BT_TOL_APPLY"))) {
                applyTolerance();
            } else if (btTmp.getName().equals(new String("BT_MAKE_NC"))) {
                makeNewNcFile();
            } else if (btTmp.getName().equals(new String("BT_OPEN_DEST_FILE"))) {
                System.out.println("open");
                openFile(MODE_DEST_OPEN);
            } else if (btTmp.getName().equals(new String("BT_OPEN_SRC_FILE"))) {
                System.out.println("open");
                openFile(MODE_SRC_OPEN);
            } else if (btTmp.getName().equals(new String("BT_READ_NC_FILE"))) {
                System.out.println("Read Nc file");
                readFile();
            } else if (btTmp.getName().equals(new String("BT_SCALE"))) {
                System.out.println("Scale function");
                rescaleNC();
            }
        }
    }

    private void rescaleNC() {
        if (txt_dest_file.getText().isEmpty()) {
            NCMsgBox msg = new NCMsgBox(frame, "Error", "Select files", 150, 150, 150, 100);
            return;
        }
        if (ncwriter.isRunning() || ncscale.isRunning()) {
            NCMsgBox msg = new NCMsgBox(frame, "Error", "NC Writer or NC Scale is already running", 150, 150, 150, 100);
            return;
        }
        try {
            progress_bar.setValue(0);
            ncscale.setNC(txt_src_file.getText(), txt_dest_file.getText(), progress_bar, 2);
            Thread t = new Thread(ncscale);
            t.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void mousePressed(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    private String getTolFromProfile(String toolNum) {
        String str = NCParser.getCodeNumber('L', '\n', toolNum);
        System.out.println(str);
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.getAt(i).getToolNum().equals(str)) {
                current_profile = i;
                return profiles.getAt(i).getTol();
            }
        }
        return null;
    }

    private void readNcFile() throws Exception {
        FileInputStream fis = new FileInputStream(nc_file_name);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String out_temp_file_name = new String(nc_file_name + ".tmp");
        FileOutputStream fos = new FileOutputStream(out_temp_file_name);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);
        profiles.removeAll();
        while (true) {
            String str = br.readLine();
            bw.write(str, 0, str.length());
            bw.newLine();
            bw.flush();
            System.out.println(str);
            if (str.isEmpty() || str == null) {
                break;
            }
            if (str.indexOf("G62") > -1) {
                NCProfileInfo profile = new NCProfileInfo();
                profile.setTolCode(str);
                profile.setTol(NCParser.getCodeNumber('T', '\n', str));
                while (true) {
                    String strTool = br.readLine();
                    bw.write(strTool, 0, strTool.length());
                    bw.newLine();
                    bw.flush();
                    if (strTool.indexOf("M09") > -1) {
                        break;
                    }
                    if (strTool.indexOf("T") > -1) {
                        if (NCParser.isToolCall(strTool)) {
                            profile.setToolCallCode(strTool);
                            profile.setToolNum(NCParser.getCodeNumber('T', 'G', strTool));
                            profiles.addProfile(profile);
                            break;
                        }
                    }
                }
            }
        }
        br.close();
        bw.close();
    }

    private boolean makeNewNcFile() {
        if (txt_dest_file.getText().isEmpty()) {
            NCMsgBox msg = new NCMsgBox(frame, "Error", "Select files", 150, 150, 150, 100);
            return false;
        }
        new_nc_file_name = txt_dest_file.getText();
        try {
            if (ncwriter.isRunning() || ncscale.isRunning()) {
                NCMsgBox msg = new NCMsgBox(frame, "Error", "NC Writer or NC Scale is already running", 150, 150, 150, 100);
                return false;
            }
            ncwriter.setNC(new_nc_file_name, nc_file_name, profiles, progress_bar);
            progress_bar.setValue(0);
            Thread writer_t = new Thread(ncwriter);
            writer_t.start();
            System.out.println(new_nc_file_name + "  " + nc_file_name);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    private void applyTolerance() {
        if (current_profile < 0) return;
        if (txt_tol.getText().isEmpty()) return;
        profiles.getAt(current_profile).setTol(txt_tol.getText());
        System.out.println(profiles.getAt(current_profile).getTol());
        System.out.println(current_profile);
    }

    private void openFile(int mode) {
        switch(mode) {
            case MODE_SRC_OPEN:
                FileDialog fdlgo = new FileDialog(frame, "open", FileDialog.LOAD);
                fdlgo.setVisible(true);
                nc_file_name = fdlgo.getDirectory() + fdlgo.getFile();
                if (!nc_file_name.isEmpty()) txt_src_file.setText(nc_file_name);
                break;
            case MODE_DEST_OPEN:
                FileDialog fdlgs = new FileDialog(frame, "save", FileDialog.SAVE);
                fdlgs.setVisible(true);
                new_nc_file_name = fdlgs.getDirectory() + fdlgs.getFile();
                if (!new_nc_file_name.isEmpty()) txt_dest_file.setText(new_nc_file_name);
                break;
            default:
                break;
        }
    }

    private void readFile() {
        if (txt_src_file.getText().isEmpty()) {
            NCMsgBox msg = new NCMsgBox(frame, "Error", "Select files", 150, 150, 150, 100);
            return;
        }
        try {
            readNcFile();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        setListItem();
    }

    private void setListItem() {
        list.removeAll();
        for (int i = 0; i < profiles.size(); i++) {
            String itemname = "TOOL" + profiles.getAt(i).getToolNum();
            list.add(itemname);
        }
    }

    private void createAndShowGUI() {
        frame = new JFrame("NcManager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        Container cp = frame.getContentPane();
        list = new List(50);
        list.addItemListener(this);
        cp.add(list, BorderLayout.WEST);
        Panel panel = new Panel();
        JLabel lbl_tol = new JLabel("Tolerance");
        panel.add(lbl_tol);
        txt_tol = new JTextField(20);
        panel.add(txt_tol);
        bt_tol = new JButton("Apply");
        bt_tol.setName(new String("BT_TOL_APPLY"));
        bt_tol.addMouseListener(this);
        panel.add(bt_tol);
        cp.add(panel, BorderLayout.CENTER);
        Box rootBottomBox = Box.createVerticalBox();
        progress_bar = new JProgressBar(0, 100);
        progress_bar.setValue(0);
        progress_bar.setStringPainted(true);
        rootBottomBox.add(progress_bar);
        bt_makeNC = new JButton("Make NC");
        bt_makeNC.setName(new String("BT_MAKE_NC"));
        bt_makeNC.addMouseListener(this);
        bt_makeNC.setAlignmentX(JButton.CENTER_ALIGNMENT);
        rootBottomBox.add(bt_makeNC);
        cp.add(rootBottomBox, BorderLayout.SOUTH);
        Box rootBox = Box.createVerticalBox();
        Box topBox = Box.createHorizontalBox();
        JLabel lbl_title = new JLabel("TYP NC manager");
        topBox.add(lbl_title);
        Box topSrcBox = Box.createHorizontalBox();
        JLabel lbl_src_file = new JLabel("    Source");
        topSrcBox.add(lbl_src_file);
        txt_src_file = new JTextField(20);
        topSrcBox.add(txt_src_file);
        bt_open_src_file = new JButton("...");
        bt_open_src_file.setName(new String("BT_OPEN_SRC_FILE"));
        bt_open_src_file.addMouseListener(this);
        topSrcBox.add(bt_open_src_file);
        Box topDestBox = Box.createHorizontalBox();
        JLabel lbl_dest_file = new JLabel("    Dest  ");
        topDestBox.add(lbl_dest_file);
        txt_dest_file = new JTextField(20);
        topDestBox.add(txt_dest_file);
        bt_open_dest_file = new JButton("...");
        bt_open_dest_file.setName(new String("BT_OPEN_DEST_FILE"));
        bt_open_dest_file.addMouseListener(this);
        topDestBox.add(bt_open_dest_file);
        Box topControlBox = Box.createHorizontalBox();
        bt_Read_nc = new JButton("Read Nc file");
        bt_Read_nc.setName(new String("BT_READ_NC_FILE"));
        bt_Read_nc.addMouseListener(this);
        topControlBox.add(bt_Read_nc);
        bt_Div100 = new JButton("Scale");
        bt_Div100.setName(new String("BT_SCALE"));
        bt_Div100.addMouseListener(this);
        topControlBox.add(bt_Div100);
        rootBox.add(topBox);
        rootBox.add(topSrcBox);
        rootBox.add(topDestBox);
        rootBox.add(topControlBox);
        cp.add(rootBox, BorderLayout.NORTH);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        NCManager mgr = new NCManager();
        mgr.createAndShowGUI();
    }
}
