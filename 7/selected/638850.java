package net.sf.jpim.contact.gui;

import net.sf.jpim.Pim;
import net.sf.jpim.contact.model.*;
import net.sf.jpim.factory.ContactModelFactory;
import net.sf.mailsomething.gui.mail.options.AbstractOptionPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

/**
 * PersonalPanel is a <tt>JPanel</tt> used in a <tt>JTabbedPane</tt> in order to display the
 * personal details (kids/spouse/birthday/aniversary) of a specific contact.
 *
 * It uses GridLayout2 as a LayoutManager
 * X-WAB-GENDER:a 
 * a should be an integer, 1 --> female 2 --> male
 *
 * For graphical picking a date, this class uses <tt>JCalendarComboBox</tt>. 
 * <tt>JCalendarComboBox</tt> renders a <tt>JCalendar</tt> object 
 * (taken from www.toedter.com) as a popup window of a ComboBox.
 *
 * @author Antonios Chalkiopoulos
 * @version $$VERSION$$ ($$DATE$$)
 */
class PersonalPanel extends AbstractOptionPanel implements ActionListener {

    private Contact contact;

    private JLabel m_l_frame_info, m_l_gender, m_l_birthday, m_l_anniversary, m_l_spouse, m_l_children;

    private JButton m_b_add, m_b_remove;

    private JTextField m_f_spouse = new JTextField();

    private JTextField m_f_children = new JTextField();

    private JList m_list_children;

    private DefaultListModel m_list_model;

    private JScrollPane m_sp_list;

    private String m_s_icon_path, unspecified, male, female;

    private Extensions m_Extensions;

    private SimpleExtension m_SimpleExtension_GN, m_SimpleExtension_FM;

    private JComboBox m_cb_gender;

    JComboBox anniversaryCB;

    JCalendarComboBox birthdayCB;

    /**
	 * Constructor.
	 * Loads resources, initializes components, builds gui sub-components and creates the final view.
	 */
    public PersonalPanel() {
    }

    public void lazyInit() {
        loadResources();
        initComponents();
        setLayout(new BorderLayout(0, 0));
        JPanel m_p_header = ContactPropertiesFrame.buildHeader(m_s_icon_path, m_l_frame_info);
        add(m_p_header, BorderLayout.NORTH);
        add(buildGUI(), BorderLayout.CENTER);
        setIsInited(true);
        setData();
    }

    /**
	 * builds the Main <tt>JPanel</tt>
	 */
    private JPanel buildGUI() {
        birthdayCB = new JCalendarComboBox();
        anniversaryCB = new JComboBox();
        anniversaryCB.setEditable(false);
        Action action = new AbstractAction("Birthday") {

            public void actionPerformed(ActionEvent evt) {
                JCheckBox cb = (JCheckBox) evt.getSource();
                boolean isSel = cb.isSelected();
                if (isSel) {
                    birthdayCB.setEnabled(true);
                } else {
                    birthdayCB.setEnabled(false);
                }
            }
        };
        JCheckBox checkBox1 = new JCheckBox(action);
        JPanel buttonPanel0 = new JPanel();
        buttonPanel0.setLayout(new GridLayout(0, 1, 5, 5));
        buttonPanel0.add(m_b_remove);
        buttonPanel0.add(new JLabel(""));
        buttonPanel0.add(new JLabel(""));
        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.setLayout(new BorderLayout(5, 5));
        buttonPanel2.add(m_b_add, BorderLayout.NORTH);
        buttonPanel2.add(buttonPanel0, BorderLayout.CENTER);
        JPanel buttonPanel1 = new JPanel();
        buttonPanel1.setLayout(new BorderLayout(10, 10));
        buttonPanel1.add(new JLabel("  "), BorderLayout.NORTH);
        buttonPanel1.add(buttonPanel2, BorderLayout.CENTER);
        m_sp_list = new JScrollPane(m_list_children);
        JPanel mainPanel1 = new JPanel();
        mainPanel1.setLayout(new BorderLayout(5, 10));
        mainPanel1.add(m_f_children, BorderLayout.NORTH);
        mainPanel1.add(m_sp_list, BorderLayout.CENTER);
        JPanel mainPanel11 = new JPanel();
        mainPanel11.setLayout(new BorderLayout(5, 5));
        mainPanel11.add(m_f_spouse, BorderLayout.NORTH);
        mainPanel11.add(mainPanel1, BorderLayout.CENTER);
        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BorderLayout(5, 10));
        testPanel.add(m_l_spouse, BorderLayout.NORTH);
        m_l_children.setVerticalAlignment(SwingConstants.TOP);
        testPanel.add(m_l_children, BorderLayout.CENTER);
        JPanel mainPanel3 = new JPanel();
        mainPanel3.setLayout(new GridLayout(0, 1, 5, 5));
        mainPanel3.add(m_l_gender);
        mainPanel3.add(checkBox1);
        mainPanel3.add(m_l_anniversary);
        JPanel ttt = new JPanel();
        ttt.setLayout(new GridLayout(0, 1, 5, 5));
        ttt.add(testPanel);
        ttt.add(mainPanel3);
        JPanel mainPanel4 = new JPanel();
        mainPanel4.setLayout(new GridLayout(0, 1, 5, 10));
        mainPanel4.add(m_cb_gender);
        mainPanel4.add(birthdayCB);
        mainPanel4.add(anniversaryCB);
        JPanel aaa = new JPanel();
        aaa.setLayout(new GridLayout(0, 1, 5, 5));
        aaa.add(mainPanel11);
        aaa.add(mainPanel4);
        JPanel bbb = new JPanel();
        bbb.setLayout(new GridLayout(0, 1, 5, 5));
        bbb.add(buttonPanel1);
        bbb.add(new JLabel(""));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout(5, 5));
        topPanel.add(ttt, BorderLayout.WEST);
        topPanel.add(aaa, BorderLayout.CENTER);
        topPanel.add(bbb, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        return topPanel;
    }

    /**
	 * void loadResources.
	 * loads the i18n resources files.
	 */
    private void loadResources() {
        ResourceBundle resources = ResourceBundle.getBundle(ContactPropertiesFrame.RESOURCE_LOCATION);
        m_l_frame_info = new JLabel(resources.getString("info.PersonalPanel"));
        m_s_icon_path = resources.getString("icon.personal");
        m_l_gender = new JLabel(resources.getString("gender"));
        m_l_birthday = new JLabel(resources.getString("birthday"));
        m_l_anniversary = new JLabel(resources.getString("anniversary"));
        m_l_spouse = new JLabel(resources.getString("spouse"));
        m_l_children = new JLabel(resources.getString("children"));
        m_b_add = new JButton(resources.getString("add"));
        m_b_remove = new JButton(resources.getString("remove"));
        unspecified = resources.getString("unspecified");
        male = resources.getString("male");
        female = resources.getString("female");
    }

    /**
	 * Initializes swing components.
	 */
    private void initComponents() {
        m_f_spouse = new JTextField();
        m_b_add.addActionListener(this);
        m_b_remove.addActionListener(this);
        m_b_add.setActionCommand("ADD");
        m_b_remove.setActionCommand("REMOVE");
        m_list_model = new DefaultListModel();
        m_list_children = new JList(m_list_model);
        m_list_children.setCellRenderer(new MyCellRenderer());
        String[] genderS = { unspecified, male, female };
        m_cb_gender = new JComboBox(genderS);
    }

    protected void setData(Contact contact) {
        this.contact = contact;
        if (isInited()) setData();
    }

    /**
	 * jw: I think this doesnt clear any previous set values. With the current
	 * use in mailsomething its not necessary... but sounds reasonable to
	 * do it.
	 *
	 * @param Contact The <tt>Contact</tt> to load data from.
	 */
    protected void setData() {
        if (contact == null || !isInited()) return;
        int m_temp_gender = 0;
        String m_temp_spouse = "";
        String[] m_temp_children = new String[10];
        if (contact.hasPersonalIdentity()) {
            PersonalIdentity pid = contact.getPersonalIdentity();
            Date birthDate = pid.getBirthDate();
            if (birthDate != null) {
                Calendar birthCal = new GregorianCalendar();
                birthCal.setTime(birthDate);
                birthdayCB.setCalendar(birthCal);
            }
        }
        if (contact.hasExtensions()) {
            m_Extensions = contact.getExtensions();
            m_SimpleExtension_GN = (SimpleExtension) (m_Extensions.get("X-WAB-GENDER"));
            if (m_SimpleExtension_GN != null) {
                try {
                    m_temp_gender = Integer.valueOf(m_SimpleExtension_GN.getValue()).intValue();
                } catch (java.lang.NumberFormatException ne) {
                    m_temp_gender = 0;
                }
            }
            m_SimpleExtension_FM = (SimpleExtension) (m_Extensions.get("X-FAMILY"));
            if (m_SimpleExtension_FM != null) {
                String[] values = m_SimpleExtension_FM.listValues();
                try {
                    m_temp_spouse = values[0];
                    for (int i = 0; i < 10; i++) m_temp_children[i] = values[i + 1];
                } catch (java.lang.IndexOutOfBoundsException ie) {
                }
            }
        }
        if ((m_temp_gender <= 0) && (m_temp_gender >= 2)) m_temp_gender = 0;
        m_cb_gender.setSelectedIndex(m_temp_gender);
        m_f_spouse.setText(m_temp_spouse);
    }

    /**
	 * Saves data from JPanel into a <tt>Contact</tt>.
	 *
	 * @param contact the <tt>Contact</tt> to save data to.
	 */
    public void apply() {
        if (contact == null || !isInited()) return;
        ContactModelFactory cmf = Pim.getContactModelFactory();
        PersonalIdentity pid = cmf.createPersonalIdentity();
        if (contact.hasPersonalIdentity()) {
            pid = contact.getPersonalIdentity();
        }
        pid.setBirthDate(birthdayCB.getCalendar().getTime());
        contact.setPersonalIdentity(pid);
        String[] m_temp_children = new String[10];
        if (contact.hasExtensions()) {
            m_Extensions = contact.getExtensions();
            m_Extensions.remove("X-WAB-GENDER");
            m_Extensions.remove("X-FAMILY");
        } else {
            m_Extensions = cmf.createExtensions();
        }
        m_SimpleExtension_GN = new SimpleExtension("X-WAB-GENDER");
        m_SimpleExtension_FM = new SimpleExtension("X-FAMILY");
        if (m_cb_gender.getSelectedIndex() == 1) {
            m_SimpleExtension_GN.addValue("1");
        } else if (m_cb_gender.getSelectedIndex() == 2) {
            m_SimpleExtension_GN.addValue("2");
        } else {
            m_SimpleExtension_GN.addValue("");
        }
        m_Extensions.add(m_SimpleExtension_GN);
        contact.setExtensions(m_Extensions);
    }

    /**
	 * Method to enable/disable editing & updating of information (through disabling JComponents)
	 *
	 * @param allow true  to allow information to be edited and Buttons to be used.
	 * @param allow false to not allow information to be edited and Buttons to be used.
	 */
    protected void allowEdit(boolean allow) {
        if (!isInited()) return;
        m_f_spouse.setEnabled(allow);
        m_list_children.setEnabled(allow);
        m_cb_gender.setEnabled(allow);
        anniversaryCB.setEnabled(false);
        m_b_add.setEnabled(allow);
        m_b_remove.setEnabled(false);
    }

    public void actionPerformed(ActionEvent actionevent) {
        if ("ADD".equals(actionevent.getActionCommand())) {
            int pos = m_list_model.getSize();
            String newChild = m_f_children.getText().trim();
            m_list_model.add(pos, newChild);
        } else if ("REMOVE".equals(actionevent.getActionCommand())) {
            int index = m_list_children.getSelectedIndex();
            if (index != -1) m_list_model.remove(index);
            if (m_list_model.getSize() == 0) m_b_remove.setEnabled(false);
        }
    }

    public static int UNSPECIFIED = 0;

    public static int MALE = 1;

    public static int FEMALE = 2;

    /**
	 * MyCellRenderer is a custom <tt>ListCellRenderer</tt> that displays an email icon
	 * next to the first item of the JList and paints the background of selected items
	 * with Color.lightGray and unselected items with Color.white
	 */
    class MyCellRenderer extends JLabel implements ListCellRenderer {

        ImageIcon m_l_email_icon = new ImageIcon(this.getClass().getClassLoader().getResource("gifs/jpim/aol.gif"));

        /**
		 * Return a component that has been configured to display the specified
		 * value. That component's <code>paint</code> method is then called to
		 * "render" the cell.  If it is necessary to compute the dimensions
		 * of a list because the list cells do not have a fixed size, this method
		 * is called to generate a component on which <code>getPreferredSize</code>
		 * can be invoked.
		 *
		 * @param list The JList we're painting.
		 * @param value The value returned by list.getModel().getElementAt(index).
		 * @param index The cells index.
		 * @param isSelected True if the specified cell was selected.
		 * @param cellHasFocus True if the specified cell has the focus.
		 * @return A component whose paint() method will render the specified value.
		 *
		 * @see JList
		 * @see ListSelectionModel
		 * @see ListModel
		 *
		 * @author Antonios Chalkiopoulos
		 * @version $$VERSION$$ ($$DATE$$)
		 */
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (list.getSelectedIndex() == -1) m_b_remove.setEnabled(false); else m_b_remove.setEnabled(true);
            setOpaque(true);
            setIcon(m_l_email_icon);
            if (isSelected) {
                setBackground(Color.lightGray);
            } else setBackground(Color.white);
            setText(value.toString());
            return (this);
        }
    }
}
