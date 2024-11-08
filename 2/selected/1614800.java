package com.atech.update.client;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumnModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Node;
import com.atech.help.ComponentHelpCapable;
import com.atech.help.HelpCapable;
import com.atech.i18n.I18nControlAbstract;
import com.atech.update.client.data.UpdateSettings;
import com.atech.update.config.UpdateConfiguration;
import com.atech.update.config.UpdateConfigurationXml;
import com.atech.utils.ATDataAccess;
import com.atech.utils.ATDataAccessAbstract;
import com.atech.utils.ATSwingUtils;
import com.atech.utils.xml.XmlUtil;

public class UpdateDialog extends JDialog implements ActionListener, HelpCapable, ComponentHelpCapable {

    private static final long serialVersionUID = -8822530996424234341L;

    private static Log log = LogFactory.getLog(UpdateDialog.class);

    UpdateSystemModel model = null;

    ATDataAccessAbstract m_da = null;

    I18nControlAbstract ic = null;

    JTabbedPane tabbedPane;

    JPanel panel;

    JLabel label, label_title, status_label;

    JButton btn_check, btn_update, button, help_button;

    Font font_big, font_normal, font_normal_b;

    JTable table;

    UpdateSettings update_settings = null;

    int m_error = 0;

    UpdateConfiguration update_config = null;

    int lastAction = 0;

    long next_version = 0L;

    /**
     * Constructor
     * 
     * @param parent
     * @param uconf
     * @param da
     */
    public UpdateDialog(JDialog parent, UpdateConfiguration uconf, ATDataAccessAbstract da) {
        super(parent, "", true);
        m_da = da;
        ic = m_da.getI18nControlInstance();
        this.update_config = uconf;
        init();
    }

    /**
     * Constructor
     * 
     * @param parent
     * @param uconf
     * @param da
     */
    public UpdateDialog(JFrame parent, UpdateConfiguration uconf, ATDataAccessAbstract da) {
        super(parent, "", true);
        m_da = da;
        ic = m_da.getI18nControlInstance();
        this.update_config = uconf;
        init();
    }

    /**
     * Constructor
     * 
     * @param parent
     * @param da
     */
    public UpdateDialog(JFrame parent, ATDataAccessAbstract da) {
        this(parent, da.getUpdateConfiguration(), da);
    }

    public void init() {
        this.setBounds(130, 50, 650, 450);
        font_big = m_da.getFont(ATDataAccess.FONT_BIG_BOLD);
        font_normal = m_da.getFont(ATDataAccess.FONT_NORMAL);
        font_normal_b = m_da.getFont(ATDataAccess.FONT_NORMAL_BOLD);
        ATSwingUtils.initLibrary();
        this.cmdUpdate();
        this.setResizable(false);
        this.m_da.centerJDialog(this, m_da.getParent());
        this.m_da.addComponent(this);
        update_settings = new UpdateSettings();
    }

    /**
     *   Displays title for dialog
     */
    public void showTitle() {
        String ev = ic.getMessage("UPDATE_APPLICATION");
        this.setTitle(ev);
        label_title.setText(ev);
    }

    /**
     *   Displays GUI
     */
    public void cmdUpdate() {
        Container dgPane = this.getContentPane();
        panel = new JPanel();
        panel.setBounds(5, 5, 620, 400);
        panel.setLayout(null);
        dgPane.add(panel);
        label_title = new JLabel();
        label_title.setBounds(30, 20, 450, 40);
        label_title.setFont(font_big);
        label_title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label_title, null);
        showTitle();
        this.label = ATSwingUtils.getLabel(ic.getMessage("SERVER_STATUS") + ":", 30, 70, 120, 25, panel, ATSwingUtils.FONT_NORMAL_BOLD);
        this.status_label = ATSwingUtils.getLabel(ic.getMessage("NO_STATUS"), 160, 70, 320, 25, panel, ATSwingUtils.FONT_NORMAL);
        this.label = new JLabel(ic.getMessage("LEGEND") + ":");
        label.setBounds(30, 340, 100, 30);
        panel.add(label, null);
        int[] x_kors = { 105, 200, 330, 470 };
        int[] widths = { 100, 150, 150, 200 };
        String[] pictures = { "dot_green.gif", "dot_red.gif", "dot_orange.gif", "dot_blue.gif" };
        String[] leg_label = { "  " + ic.getMessage("NEWEST"), "  " + ic.getMessage("NOT_UPDATED"), "  " + ic.getMessage("NEW"), "  " + ic.getMessage("UNKNOWN_STATUS") };
        for (int i = 0; i < x_kors.length; i++) {
            label = new JLabel(leg_label[i]);
            label.setIcon(m_da.getImageIcon("/icons/", pictures[i], this));
            label.setBounds(x_kors[i], 344, widths[i], 25);
            panel.add(label);
        }
        this.model = new UpdateSystemModel(this.update_config, m_da);
        table = new JTable(this.model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDoubleBuffered(true);
        UpdateTableCellRenderer utcr = new UpdateTableCellRenderer(m_da);
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(utcr);
            cm.getColumn(i).setPreferredWidth(model.getColumnWidth(i, 580));
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(30, 100, 580, 240);
        panel.add(scroll);
        scroll.repaint();
        scroll.updateUI();
        this.btn_check = new JButton("   " + ic.getMessage("CHECK_SERVER"));
        this.btn_check.setBounds(455, 30, 150, 25);
        this.btn_check.setIcon(m_da.getImageIcon("/icons/", "up_down_question.png", 22, 22, this));
        this.btn_check.addActionListener(this);
        this.btn_check.setFont(font_normal);
        this.btn_check.setActionCommand("check_server");
        panel.add(this.btn_check);
        this.btn_update = new JButton("   " + ic.getMessage("RUN_UPDATE"));
        this.btn_update.setBounds(455, 60, 150, 25);
        this.btn_update.addActionListener(this);
        this.btn_update.setIcon(m_da.getImageIcon("/icons/", "download.png", 22, 22, this));
        this.btn_update.setEnabled(false);
        this.btn_update.setFont(font_normal);
        this.btn_update.setActionCommand("run_update");
        panel.add(this.btn_update);
        button = new JButton("   " + ic.getMessage("CLOSE"));
        button.setBounds(350, 385, 120, 25);
        button.setIcon(m_da.getImageIcon("/icons/", "cancel.png", 22, 22, this));
        button.addActionListener(this);
        button.setFont(font_normal);
        button.setActionCommand("close");
        panel.add(button);
        this.help_button = m_da.createHelpButtonByBounds(485, 385, 120, 25, this);
        this.help_button.setFont(font_normal);
        panel.add(help_button);
    }

    /**
     * Show Dialog
     */
    public void showDialog() {
        this.setVisible(true);
    }

    /**
     *  Action Listener
     */
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        if (action.equals("ok")) {
            lastAction = 0;
        } else if (action.equals("run_update")) {
            this.runUpdate();
        } else if (action.equals("check_server")) {
            this.checkServer();
        } else if (action.equals("close")) {
            lastAction = 0;
            this.m_da.removeComponent(this);
            this.dispose();
        } else System.out.println("UpdateDialog::Unknown command: " + action);
    }

    /**
     *  Gets info if action was performed.
     * 
     *  @return true if action was done, false if not.
     */
    public boolean wasAction() {
        if (lastAction == 1) return true; else return false;
    }

    /**
     *  Returns object saved
     * 
     *  @return object of type of Object
     */
    public Object getObject() {
        return null;
    }

    /**
     * Help Id
     */
    private String help_id;

    /** 
     * enableHelp
     */
    public void enableHelp(String _help_id) {
        this.help_id = _help_id;
        m_da.enableHelp(this);
    }

    /** 
     * getComponent
     */
    public Component getComponent() {
        return this;
    }

    /** 
     * getHelpButton
     */
    public JButton getHelpButton() {
        return this.help_button;
    }

    /** 
     * getHelpId
     */
    public String getHelpId() {
        return this.help_id;
    }

    /**
     * Check Server
     */
    public void checkServer() {
        if (!m_da.getDeveloperMode()) {
            JOptionPane.showMessageDialog(this, ic.getMessage("UPDATE_SERVER_NA"), ic.getMessage("INFORMATION"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String server_name = this.update_settings.update_server;
            long current_version = 4;
            long current_db = 7;
            String iLine2;
            iLine2 = getServletData(server_name + "ATechUpdateSystem?action=get_update_status&" + "product_id=" + m_da.getAppName() + "&" + "current_version=" + current_version + "&" + "current_db=" + current_db);
            if (iLine2 == null) {
                return;
            }
            String ret_msg = iLine2.substring(iLine2.indexOf("RETURN_DATA_START__") + "RETURN_DATA_START__<br>".length(), iLine2.indexOf("<br>__RETURN_DATA_END"));
            if (ret_msg.contains("ERR")) {
                String err = "";
                if (ret_msg.startsWith("ERR_NO_SUCH_APP")) {
                    err = "UPD_ERR_NO_SUCH_APP";
                } else if (ret_msg.startsWith("ERR_INTERNAL_ERROR")) {
                    err = "UPD_ERR_INTERNAL_ERROR";
                }
                m_da.showDialog(this, ATDataAccessAbstract.DIALOG_ERROR, ic.getMessage(err));
                status_label.setText(ic.getMessage("STATUS_UPD_FAILED_DATA"));
            } else if (ret_msg.contains(";")) {
                StringTokenizer strtok = new StringTokenizer(ret_msg, ";");
                Hashtable<String, String> msges = new Hashtable<String, String>();
                String return_info = null;
                while (strtok.hasMoreTokens()) {
                    String t = strtok.nextToken();
                    msges.put(t.substring(0, t.indexOf("=")), t.substring(t.indexOf("=") + 1));
                }
                next_version = Long.parseLong(msges.get("NEXT_VERSION"));
                boolean cont = false;
                if (next_version == current_version) {
                    if (msges.containsKey("NEXT_DB_VERSION")) {
                        System.out.println("UPDATE_FOR_HIGHER_DB_FOUND");
                        return_info = String.format(ic.getMessage("UPDATE_FOR_HIGHER_DB_FOUND"), msges.get("NEXT_DB_VERSION_STR"));
                        status_label.setText(ic.getMessage("STATUS_UPD_NO_VALID_UPDATE"));
                    } else {
                        System.out.println("NO_UPDATE_FOUND");
                        return_info = ic.getMessage("NO_UPDATE_FOUND");
                        status_label.setText(ic.getMessage("STATUS_UPD_NO_UPDATE"));
                    }
                } else {
                    cont = true;
                    if (msges.containsKey("NEXT_DB_VERSION")) {
                        System.out.println("UPDATE_FOUND_ALSO_HIGHER_DB");
                        return_info = String.format(ic.getMessage("UPDATE_FOUND_ALSO_HIGHER_DB"), msges.get("NEXT_DB_VERSION_STR"), msges.get("NEXT_VERSION_STR"));
                    } else {
                        System.out.println("UPDATE_FOUND_VERSION");
                        return_info = String.format(ic.getMessage("UPDATE_FOUND_VERSION"), msges.get("NEXT_VERSION_STR"));
                    }
                    status_label.setText(ic.getMessage("STATUS_UPD_UPDATE_FOUND"));
                }
                if (cont) {
                    Object[] options = { ic.getMessage("YES"), ic.getMessage("NO") };
                    int n = JOptionPane.showOptionDialog(this, return_info, ic.getMessage("QUESTION"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (n == JOptionPane.YES_OPTION) {
                        iLine2 = this.getServletData(server_name + "ATechUpdateSystem?action=get_xml&" + "product_id=" + m_da.getAppName() + "&" + "current_version=" + msges.get("NEXT_VERSION"));
                        if (iLine2 == null) {
                            return;
                        } else {
                            ret_msg = iLine2.substring(iLine2.indexOf("RETURN_DATA_START__") + "RETURN_DATA_START__<br>".length(), iLine2.indexOf("<br>__RETURN_DATA_END"));
                            UpdateConfigurationXml ucxml = new UpdateConfigurationXml(ret_msg);
                            this.model.updateModel(ucxml.getUpdateConfiguration());
                            this.model.fireTableDataChanged();
                            this.btn_update.setEnabled(true);
                        }
                    }
                } else m_da.showDialog(this, ATDataAccessAbstract.DIALOG_INFO, return_info);
            } else {
                m_da.showDialog(this, ATDataAccessAbstract.DIALOG_ERROR, ic.getMessage("UPD_ERR_INTERNAL_ERROR"));
                status_label.setText(ic.getMessage("STATUS_UPD_FAILED_COMM_ERROR"));
            }
        } catch (Exception ex) {
            this.status_label.setText(ic.getMessage("UPD_ERROR_CONTACTING_SERVER"));
            m_da.showDialog(this, ATDataAccessAbstract.DIALOG_ERROR, ic.getMessage("UPD_ERROR_CONTACTING_SERVER"));
            ex.printStackTrace();
        }
    }

    private String getServletData(String full_url) {
        try {
            URL url = new URL(full_url);
            log.debug("Servlet::URL (" + full_url + ")");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String iLine, iLine2;
            iLine2 = "";
            while ((iLine = in.readLine()) != null) {
                iLine2 += iLine;
            }
            in.close();
            return iLine2;
        } catch (Exception ex) {
            log.error("Servlet::URL (" + full_url + ")");
            log.error("Error contacting servlet: " + ex, ex);
            this.status_label.setText(ic.getMessage("UPD_ERROR_CONTACTING_SERVER"));
            m_da.showDialog(this, ATDataAccessAbstract.DIALOG_ERROR, ic.getMessage("UPD_ERROR_CONTACTING_SERVER"));
            return null;
        }
    }

    @SuppressWarnings("unused")
    private String getParameter(String parameter, String text) {
        String start_tag = "<" + parameter + ">";
        if (!text.contains(start_tag)) return "";
        String end_tag = "</" + parameter + ">";
        String par = text.substring(text.indexOf(start_tag) + start_tag.length(), text.indexOf(end_tag));
        return par;
    }

    /**
     * Run Update
     */
    public void runUpdate() {
        try {
            String server_name = this.update_settings.update_server;
            long current_version = 4;
            String iLine2;
            iLine2 = getServletData(server_name + "ATechUpdateSystem?action=get_update_list&" + "product_id=" + m_da.getAppName() + "&" + "current_version=" + current_version + "&" + "next_version=" + this.next_version);
            if (iLine2 == null) {
                System.out.println("No data returned !");
                return;
            }
            String ret_msg = iLine2.substring(iLine2.indexOf("RETURN_DATA_START__") + "RETURN_DATA_START__<br>".length(), iLine2.indexOf("<br>__RETURN_DATA_END"));
            processDetailsData(ret_msg);
        } catch (Exception ex) {
            System.out.println("runUpdate(). Ex.: " + ex);
        }
    }

    private void processDetailsData(String xml) {
        try {
            XmlUtil xu = new XmlUtil(xml);
            this.model.updateServerExtendedSettings(xu.getNodes("update_detailed_file/components/component"));
        } catch (Exception ex) {
            log.error("processDetailsData(). Ex.: " + ex, ex);
        }
    }
}
