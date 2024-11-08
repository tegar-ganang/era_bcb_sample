package visualisation;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import cmd.AboutCmd;
import cmd.AlwaysOnTopCmd;
import cmd.AppChooserCmd;
import cmd.CreateTasksCmd;
import cmd.ExitCmd;
import cmd.SupportCmd;
import com.toedter.calendar.JDateChooser;

@SuppressWarnings({ "serial", "deprecation" })
public class JProgDVBTimer extends JFrame {

    private static final String version = "0.1 beta";

    private File stdProgDVB = new File(System.getenv("ProgramFiles") + "/ProgDVB/ProgDVB.exe");

    private JTextField choosenApp, taskname;

    private JTextField channel;

    private JDateChooser startdate, stopdate;

    private JSpinner starttime, stoptime;

    private JPasswordField winuserpass;

    public JProgDVBTimer() {
        this("ProgDVB Timer v" + version);
    }

    public JProgDVBTimer(String title) {
        super(title);
        initFrame();
        initContent();
        ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource("images/icon.png"));
        this.setIconImage(icon.getImage());
        this.setVisible(true);
    }

    private void initFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(380, 260);
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) screensize.getWidth() / 2 - (this.getWidth() / 2);
        int y = (int) screensize.getHeight() / 2 - (this.getWidth() / 2);
        this.setLocation(x, y);
        this.setJMenuBar(createMenuBar());
    }

    private void initContent() {
        JLabel label = new JLabel("ProgDVB:");
        label.setBounds(10, 15, 55, 20);
        this.add(label);
        choosenApp = new JTextField();
        choosenApp.setBounds(85, 15, 170, 20);
        JButton button = new JButton("w�hlen");
        button.setBounds(260, 15, 75, 20);
        button.addActionListener(new AppChooserCmd(this));
        this.add(button);
        if (stdProgDVB.exists()) {
            this.setChoosenApp(stdProgDVB);
        }
        choosenApp.setEditable(false);
        this.add(choosenApp);
        label = new JLabel("Titel:");
        label.setBounds(10, 40, 55, 20);
        this.add(label);
        taskname = new JTextField();
        taskname.setBounds(85, 40, 250, 20);
        this.add(taskname);
        label = new JLabel("Start:");
        label.setBounds(10, 65, 55, 20);
        this.add(label);
        startdate = new JDateChooser();
        startdate.setDate(new Date());
        startdate.setBounds(85, 65, 160, 20);
        this.add(startdate);
        starttime = new JSpinner();
        starttime.setBounds(255, 65, 80, 20);
        final SpinnerDateModel starttimeModel = new SpinnerDateModel();
        starttime.setModel(starttimeModel);
        starttime.setEditor(new JSpinner.DateEditor(starttime, ("HH:mm")));
        this.add(starttime);
        label = new JLabel("Stop:");
        label.setBounds(10, 90, 55, 20);
        this.add(label);
        stopdate = new JDateChooser();
        stopdate.setDate(new Date());
        stopdate.setBounds(85, 90, 160, 20);
        this.add(stopdate);
        stoptime = new JSpinner();
        stoptime.setBounds(255, 90, 80, 20);
        final SpinnerDateModel stoptimeModel = new SpinnerDateModel();
        stoptime.setModel(stoptimeModel);
        stoptime.setEditor(new JSpinner.DateEditor(stoptime, ("HH:mm")));
        this.add(stoptime);
        label = new JLabel("Sender:");
        label.setBounds(10, 115, 55, 20);
        this.add(label);
        channel = new JTextField();
        channel.setBounds(85, 115, 250, 20);
        this.add(channel);
        label = new JLabel("Win-Passwort:");
        label.setBounds(10, 140, 70, 20);
        this.add(label);
        winuserpass = new JPasswordField();
        winuserpass.setBounds(85, 140, 250, 20);
        this.add(winuserpass);
        JButton createButton = new JButton("Tasks erstellen");
        createButton.setBounds(215, 170, 122, 20);
        createButton.addActionListener(new CreateTasksCmd(this));
        this.add(createButton);
        JButton closeButton = new JButton("Beenden");
        closeButton.setBounds(10, 170, 90, 20);
        closeButton.addActionListener(new ExitCmd());
        this.add(closeButton);
    }

    private JMenuBar createMenuBar() {
        JMenu menu;
        JMenuItem item;
        JMenuBar menuBar = new JMenuBar();
        menu = new JMenu("Datei");
        JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(new AlwaysOnTopCmd(this));
        checkbox.setText("Immer sichtbar");
        checkbox.doClick();
        menu.add(checkbox);
        menu.addSeparator();
        item = new JMenuItem(new ExitCmd());
        item.setText("Beenden");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        menu.add(item);
        menuBar.add(menu);
        menuBar.add(Box.createHorizontalGlue());
        menu = new JMenu("Hilfe");
        item = new JMenuItem(new SupportCmd());
        item.setText("FAQ / Hilfe");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menu.add(item);
        item = new JMenuItem(new AboutCmd(this));
        item.setText("�ber...");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        menu.add(item);
        menuBar.add(menu);
        return menuBar;
    }

    public void setChoosenApp(File file) {
        this.choosenApp.setText(file.getAbsoluteFile().toString());
    }

    public File getChoosenApp() {
        return new File(this.choosenApp.getText());
    }

    public String getTaskname() {
        return taskname.getText();
    }

    public String getChannel() {
        return channel.getText();
    }

    public Calendar getStartDateTime() {
        Calendar start = stopdate.getCalendar();
        start.set(Calendar.HOUR_OF_DAY, ((Date) starttime.getValue()).getHours());
        start.set(Calendar.MINUTE, ((Date) starttime.getValue()).getMinutes());
        return start;
    }

    public Calendar getStopDateTime() {
        Calendar stop = stopdate.getCalendar();
        stop.set(Calendar.HOUR_OF_DAY, ((Date) stoptime.getValue()).getHours());
        stop.set(Calendar.MINUTE, ((Date) stoptime.getValue()).getMinutes());
        return stop;
    }

    public String getWinUserpassword() {
        String pass = new String(winuserpass.getPassword());
        return pass;
    }

    public String getVersion() {
        return version;
    }

    public void showErrorDialog(String title, String text) {
        JOptionPane.showMessageDialog(this, text, title, JOptionPane.ERROR_MESSAGE);
    }

    public int showConfirmDialog(String title, String text) {
        int decision = JOptionPane.showConfirmDialog(this, text, title, JOptionPane.YES_NO_OPTION);
        return decision;
    }

    public static void main(String[] args) {
        new JProgDVBTimer();
    }
}
