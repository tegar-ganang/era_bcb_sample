import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.File;
import java.io.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.*;

public class SenezConsole extends JFrame implements ActionListener, KeyListener {

    final int DEFAULT_HEIGHT = 400;

    final int DEFAULT_WIDTH = 600;

    final Dimension DEFAULT_SIZE = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    final int DEFAULT_X = 0;

    final int DEFAULT_Y = 0;

    final Point DEFAULT_LOCATION = new Point(DEFAULT_X, DEFAULT_Y);

    final int MENU_HEIGHT = 25;

    final double INPUT_RATIO = .85;

    final int DEFAULT_FONTSIZE = 12;

    final Font DEFAULT_FONT = new Font("Courier New", Font.PLAIN, DEFAULT_FONTSIZE);

    private JMenuBar menu = new JMenuBar();

    private JScrollPane scrollPane;

    private ArrayList menuItems;

    private String[] menuTitles = { "File", "Edit", "Tools", "Help", "About" };

    private String[][] itemTitles = { { "Open", "Save", "Close" }, { "Copy Selection", "Copy Input", "Paste to Console", "Paste to Input", "Select All" }, { "Toggle Edit Lock" }, { "Help Me!" }, { "About SC++", "Contact Senez", "Submit a Bug" } };

    private JTextPane console;

    int caretPos;

    ArrayList styles = new ArrayList();

    Color currentColor;

    int currentFontSize;

    SenezReader input;

    boolean waiting;

    private boolean editLock;

    private JTextField sysIn;

    private JButton sysCr;

    private void init(int x, int y, int h, int w) {
        setSize(w, h);
        setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("SenezConsole++");
        getContentPane().setLayout(null);
        menuItems = new ArrayList();
        for (int i = 0; i < menuTitles.length; i++) {
            JMenu title = new JMenu(menuTitles[i]);
            for (int j = 0; j < itemTitles[i].length; j++) {
                JMenuItem item = new JMenuItem(itemTitles[i][j]);
                item.addActionListener(this);
                title.add(item);
                menuItems.add(item);
            }
            menu.add(title);
        }
        menu.setSize(w, MENU_HEIGHT);
        caretPos = 0;
        editLock = true;
        getContentPane().add(menu);
        console = new JTextPane();
        setEditLock(editLock);
        currentColor = Color.black;
        currentFontSize = DEFAULT_FONTSIZE;
        console.setFont(DEFAULT_FONT);
        scrollPane = new JScrollPane(console);
        scrollPane.setLocation(0, MENU_HEIGHT);
        scrollPane.setSize(w - 7, h - 2 * MENU_HEIGHT - 36);
        getContentPane().add(scrollPane);
        waiting = false;
        sysIn = new JTextField();
        input = new SenezReader(this, sysIn);
        sysCr = new JButton("Enter");
        sysIn.setSize((int) (w * INPUT_RATIO), MENU_HEIGHT);
        sysCr.setSize(w - sysIn.getWidth(), MENU_HEIGHT);
        sysIn.setLocation(1, MENU_HEIGHT + scrollPane.getHeight());
        sysCr.setLocation(sysIn.getWidth(), MENU_HEIGHT + scrollPane.getHeight());
        getContentPane().add(sysIn);
        getContentPane().add(sysCr);
        sysCr.addActionListener(this);
        sysIn.addKeyListener(this);
        addWindowListener(new WindowAdapter() {

            public void windowOpened(WindowEvent e) {
                sysIn.requestFocus();
            }
        });
        setVisible(true);
        setResizable(false);
    }

    public SenezConsole(int x, int y, int h, int w) {
        init(x, y, h, w);
    }

    public SenezConsole(Dimension size, Point loc) {
        init(loc.x, loc.y, size.height, size.width);
    }

    public SenezConsole(Dimension size) {
        init(DEFAULT_LOCATION.x, DEFAULT_LOCATION.y, size.height, size.width);
    }

    public SenezConsole(Point loc) {
        init(loc.x, loc.y, DEFAULT_SIZE.height, DEFAULT_SIZE.width);
    }

    public SenezConsole() {
        init(DEFAULT_LOCATION.x, DEFAULT_LOCATION.y, DEFAULT_SIZE.height, DEFAULT_SIZE.width);
    }

    public void center() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) (screen.getWidth() / 2.0 - getWidth() / 2.0);
        int y = (int) (screen.getHeight() / 2.0 - getHeight() / 2.0);
        setLocation(x, y);
    }

    private void setEditLock(boolean lock) {
        console.setEditable(!lock);
    }

    public void print(boolean s) {
        print(s ? "true" : "false");
    }

    public void print(char s) {
        print(s + "");
    }

    public void print(char[] s) {
        String temp = "";
        for (int i = 0; i < s.length; i++) {
            temp += s[i];
        }
        print(temp);
    }

    public void print(double s) {
        print(s + "");
    }

    public void print(float s) {
        print(s + "");
    }

    public void print(int s) {
        print(s + "");
    }

    public void print(long s) {
        print(s + "");
    }

    public void print(Object s) {
        print(s.toString());
    }

    public void println() {
        print("\n");
    }

    public void println(boolean s) {
        print((s ? "true" : "false") + "\n");
    }

    public void println(char s) {
        print(s + "\n");
    }

    public void println(char[] s) {
        String temp = "";
        for (int i = 0; i < s.length; i++) {
            temp += s[i];
        }
        print(temp + "\n");
    }

    public void println(double s) {
        print(s + "\n");
    }

    public void println(float s) {
        print(s + "\n");
    }

    public void println(int s) {
        print(s + "\n");
    }

    public void println(long s) {
        print(s + "\n");
    }

    public void println(Object s) {
        print(s.toString() + "\n");
    }

    public void println(String s) {
        print(s + "\n");
    }

    public void print(String s) {
        console.setText(console.getText() + s);
        CStyle st = new CStyle(caretPos, s.length(), currentColor, currentFontSize);
        styles.add(st);
        for (int i = 0; i < styles.size(); i++) {
            st = (CStyle) styles.get(i);
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setForeground(attr, st.col);
            StyleConstants.setFontSize(attr, st.font);
            StyledDocument sdoc = console.getStyledDocument();
            sdoc.setCharacterAttributes(st.start, st.len, attr, !true);
        }
        caretPos += s.length();
        console.select(console.getText().length(), console.getText().length());
    }

    public void setTextColor(Color c) {
        currentColor = c;
    }

    public void clear() {
        console.setText("");
        styles.clear();
        caretPos = 0;
        print("");
    }

    public void setBackgroundColor(Color c) {
        console.setBackground(c);
    }

    public void resetTextColor(Color c) {
        for (int i = 0; i < styles.size(); i++) {
            CStyle st = (CStyle) styles.get(i);
            st.col = c;
        }
        print("");
    }

    public void setFontSize(int f) {
        currentFontSize = f;
    }

    private void setClipboard(String text) {
        Transferable clip = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clip, null);
    }

    private String getClipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return "";
        }
    }

    public void keyTyped(KeyEvent e) {
        int key = (int) e.getKeyChar();
        if (key == 10 || key == 13) {
            if (!waiting) {
                input.retrieve();
            }
            waiting = false;
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sysCr) {
            if (!waiting) {
                input.retrieve();
            }
            waiting = false;
        } else {
            for (int i = 0; i < menuItems.size(); i++) {
                JMenuItem item = (JMenuItem) menuItems.get(i);
                if (e.getSource() == item) {
                    menuAction(item.getText());
                    break;
                }
            }
        }
    }

    public void menuAction(String name) {
        if (name.equals("Open")) {
            JFileChooser jfc = new JFileChooser("c:\\");
            jfc.showOpenDialog(null);
            File file = jfc.getSelectedFile();
            Scanner sc = null;
            try {
                sc = new Scanner(file);
            } catch (FileNotFoundException ex) {
            }
            clear();
            while (sc.hasNextLine()) {
                println(sc.nextLine());
            }
        } else if (name.equals("Save")) {
            JFileChooser jfc = new JFileChooser("c:\\");
            jfc.showSaveDialog(null);
            File file = jfc.getSelectedFile();
            PrintStream ps = null;
            try {
                ps = new PrintStream(file);
            } catch (FileNotFoundException ex1) {
            }
            String s = console.getText();
            ps.print(s);
            ps.close();
            JOptionPane.showMessageDialog(null, "Output has been saved as\n" + file.getPath(), "File Saved", JOptionPane.DEFAULT_OPTION);
        } else if (name.equals("Close")) {
            System.exit(0);
        } else if (name.equals("Copy Selection")) {
            setClipboard(console.getSelectedText());
        } else if (name.equals("Copy Input")) {
            setClipboard(sysIn.getText());
        } else if (name.equals("Paste to Input")) {
            sysIn.setText(getClipboard());
        } else if (name.equals("Paste to Console")) {
            println(getClipboard());
        } else if (name.equals("Select All")) {
            console.setSelectionStart(0);
            console.setSelectionEnd(console.getText().length());
        } else if (name.equals("Toggle Edit Lock")) {
            editLock = !editLock;
            setEditLock(editLock);
        } else if (name.equals("Help Me!")) {
            JOptionPane.showMessageDialog(null, "Coming Soon!", "Help", JOptionPane.DEFAULT_OPTION);
        } else if (name.equals("About SC++")) {
            JOptionPane.showMessageDialog(null, "SenezConsole++\nVersion 1.0\n\nby Chris Senez", "SC++", JOptionPane.DEFAULT_OPTION);
        } else if (name.equals("Contact Senez")) {
            JOptionPane.showMessageDialog(null, "email: csenez64@yahoo.com", "Contact", JOptionPane.DEFAULT_OPTION);
        } else if (name.equals("Submit a Bug")) {
            JOptionPane.showMessageDialog(null, "email: csenez64@yahoo.com\nsubject: sc++ bug report\nmessage: details", "Bugfix", JOptionPane.DEFAULT_OPTION);
        }
    }
}
