package ball;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import util.Files;

public class Code extends JFrame implements ActionListener {

    private static final String VERSION = "v3.6 (beta)";

    public static void main(String... args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Code c = new Code();
                c.setSize(450, 550);
                c.setVisible(true);
                c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                c.setLocationRelativeTo(null);
                c.setMinimumSize(new Dimension(250, 400));
            }
        });
    }

    private JButton run;

    private JPanel codePanel;

    private JTextArea code;

    private JScrollPane codePane;

    private JLabel status;

    private String lang;

    private File savePath = null;

    private PrintStream fileOut;

    private Console cons = null;

    private static final int FILE_CONSOLE = -441;

    private static final int GUI_CONSOLE = -414;

    private int consoleMode = GUI_CONSOLE;

    private boolean isAlreadyRunning = false;

    private boolean stillRun = true;

    private File output = null;

    private JMenuBar menu;

    private JMenu file;

    private JMenu help;

    private JMenuItem xnew;

    private JMenuItem open;

    private JMenuItem save;

    private JMenuItem saveAs;

    private JMenuItem exit;

    private JMenuItem readme;

    private JMenuItem about;

    private JMenuItem options;

    private JMenuItem setout;

    public Code() {
        super("BALL");
        Icon runIcon = new ImageIcon(getClass().getResource("/res/run2.gif"));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        run = new JButton("Run", runIcon);
        run.setHorizontalTextPosition(JButton.RIGHT);
        run.setVerticalTextPosition(JButton.CENTER);
        run.setPreferredSize(new Dimension(380, 50));
        c.ipadx = 250;
        c.weightx = 0;
        c.weighty = 0;
        add(run, c);
        codePanel = new JPanel();
        code = new JTextArea();
        code.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        code.setLineWrap(true);
        code.setWrapStyleWord(true);
        codePane = new JScrollPane(code);
        codePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        codePanel.setLayout(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.BOTH;
        cc.weightx = 1;
        cc.weighty = 1;
        codePanel.add(codePane, cc);
        codePanel.setBorder(BorderFactory.createTitledBorder("BaiSoft All-purpose List-oriented Language"));
        c.ipadx = 380;
        c.ipady = 340;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        add(codePanel, c);
        menu = new JMenuBar();
        xnew = new JMenuItem("New");
        open = new JMenuItem("Open");
        save = new JMenuItem("Save");
        saveAs = new JMenuItem("Save As");
        exit = new JMenuItem("Exit");
        readme = new JMenuItem("Readme");
        about = new JMenuItem("About");
        options = new JMenuItem("Options");
        setout = new JMenuItem("Set Output");
        file = new JMenu("File");
        file.add(xnew);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.addSeparator();
        file.add(exit);
        menu.add(file);
        help = new JMenu("Help");
        help.add(readme);
        help.addSeparator();
        help.add(options);
        help.add(setout);
        help.addSeparator();
        help.add(about);
        menu.add(help);
        run.addActionListener(this);
        xnew.addActionListener(this);
        open.addActionListener(this);
        save.addActionListener(this);
        saveAs.addActionListener(this);
        exit.addActionListener(this);
        readme.addActionListener(this);
        about.addActionListener(this);
        options.addActionListener(this);
        setout.addActionListener(this);
        setJMenuBar(menu);
    }

    public static Thread instance;

    public void makefile() {
        JFileChooser f = new JFileChooser();
        if (f.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            output = f.getSelectedFile();
        }
        consoleMode = FILE_CONSOLE;
    }

    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();
        if (command.equals("Run")) {
            Runnable perform = new Runnable() {

                public void run() {
                    start(code.getText());
                }
            };
            instance = new Thread(perform);
            instance.setPriority(Thread.MIN_PRIORITY);
            instance.start();
        } else if (command.equals("About")) JOptionPane.showMessageDialog(null, "BaiSoft All-purpose List-oriented Language " + VERSION); else if (command.equals("Exit")) System.exit(0); else if (command.equals("Readme")) showReadMe(); else if (command.equals("New")) xnew(); else if (command.equals("Open")) open(); else if (command.equals("Save")) save(); else if (command.equals("Save As")) saveAs(); else if (command.equals("Options")) optionsBox(); else if (command.equals("Set Output")) {
            makefile();
        } else if (command.equals("Cancel")) frame.dispose(); else JOptionPane.showMessageDialog(null, "Feature not implemented yet.");
    }

    private JRadioButton GUI;

    private JRadioButton File;

    private JButton OK;

    private JButton Cancel;

    private JFrame frame;

    public void optionsBox() {
        frame = new JFrame("Options");
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        ButtonGroup group = new ButtonGroup();
        GUI = new JRadioButton("GUI console");
        File = new JRadioButton("File Redirect");
        OK = new JButton("OK");
        Cancel = new JButton("Cancel");
        group.add(GUI);
        group.add(File);
        panel1.setLayout(new GridLayout(2, 1));
        panel1.add(GUI);
        panel1.add(File);
        panel2.setLayout(new GridLayout(1, 2));
        panel2.add(OK);
        panel2.add(Cancel);
        frame.setLayout(new FlowLayout());
        frame.add(new JLabel("Use the GUI, or direct output to a file?"));
        frame.add(panel1);
        frame.add(panel2);
        if (consoleMode == FILE_CONSOLE) File.setSelected(true);
        if (consoleMode == GUI_CONSOLE) GUI.setSelected(true);
        OK.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (GUI.isSelected()) consoleMode = GUI_CONSOLE;
                if (File.isSelected()) {
                    consoleMode = FILE_CONSOLE;
                    makefile();
                }
                frame.dispose();
            }
        });
        Cancel.addActionListener(this);
        frame.setSize(250, 100);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(this);
    }

    public String getCode() {
        return code.getText();
    }

    public void setStatus(String status) {
        this.status.setText(status);
    }

    public void showReadMe() {
        JFrame fra = new JFrame("Readme");
        Scanner i = new Scanner(getClass().getResourceAsStream("/res/readme.txt"));
        StringBuffer o = new StringBuffer();
        while (i.hasNextLine()) o.append(i.nextLine() + "\n");
        String readme = o.toString();
        JTextArea area = new JTextArea(readme);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane pan = new JScrollPane(area);
        fra.add(pan);
        fra.setSize(600, 480);
        fra.setVisible(true);
    }

    public void xnew() {
        int op = JOptionPane.showConfirmDialog(null, "Lose all work?", null, JOptionPane.OK_CANCEL_OPTION);
        if (op == 0) {
            code.setText("");
            savePath = null;
            setStatus("Untitled.txt");
        }
    }

    public void open() {
        try {
            JFileChooser chooser = new JFileChooser(savePath);
            chooser.showOpenDialog(null);
            savePath = chooser.getSelectedFile();
            lang = "";
            Scanner in = null;
            try {
                in = new Scanner(savePath);
            } catch (FileNotFoundException e) {
            }
            while (in.hasNext()) {
                lang += in.nextLine() + "\n";
            }
            code.setText(lang);
            setStatus(savePath.toString());
        } catch (Exception e) {
        }
    }

    public void save() {
        try {
            lang = getCode();
            Scanner in = new Scanner(lang);
            try {
                fileOut = new PrintStream(savePath);
                while (in.hasNext()) {
                    fileOut.println(in.nextLine());
                }
                setStatus(savePath.toString());
            } catch (IOException e) {
            } catch (NullPointerException e) {
                saveAs();
            }
        } catch (Exception e) {
        }
    }

    public void saveAs() {
        try {
            JFileChooser chooser = new JFileChooser(savePath);
            int y = chooser.showSaveDialog(null);
            if (y == JFileChooser.APPROVE_OPTION) {
                savePath = chooser.getSelectedFile();
                save();
            }
        } catch (Exception e) {
        }
    }

    public static Variables variables = null;

    public static int codeLocation = 0;

    public void start(String code) {
        if (!isAlreadyRunning) isAlreadyRunning = true; else {
            JOptionPane.showMessageDialog(null, "This program is already running!");
            return;
        }
        stillRun = true;
        codeLocation = 0;
        Instruction.reset();
        variables = new Variables();
        String[] lines = Files.seperate(code, Files.PARSE_BY_LINE);
        Instruction[] ins = new Instruction[lines.length];
        if (consoleMode == FILE_CONSOLE) cons = new FileConsole(output); else if (consoleMode == GUI_CONSOLE) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        cons = new GUIConsole();
                    }
                });
            } catch (Exception e) {
            }
            int ballx = getLocation().x;
            int widthx = ((GUIConsole) cons).getWidth();
            if (ballx - widthx > 50) ((GUIConsole) cons).setLocation(getLocation().x - widthx, getLocation().y); else if (Toolkit.getDefaultToolkit().getScreenSize().width - getWidth() - widthx > 50) ((GUIConsole) cons).setLocation(getLocation().x + getWidth(), getLocation().y); else ((GUIConsole) cons).setLocationRelativeTo(null);
        }
        cons.clear();
        new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                    }
                    if (cons instanceof GUIConsole) {
                        if (!((GUIConsole) cons).isVisible()) {
                            stillRun = false;
                            isAlreadyRunning = false;
                            break;
                        }
                    }
                }
            }
        }).start();
        try {
            for (int i = 0; i < ins.length; i++) {
                ins[i] = new Instruction(lines[i], cons);
                if (!stillRun) break;
            }
        } catch (SyntaxError e) {
            JOptionPane.showMessageDialog(null, "Compilation Error\n" + e);
        }
        codeLocation = 0;
        try {
            while (true) {
                ins[codeLocation].run();
                if (!stillRun) break;
            }
        } catch (SyntaxError e) {
            JOptionPane.showMessageDialog(null, "Runtime Error\n" + e);
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (Exception e) {
        }
        if (consoleMode == FILE_CONSOLE) JOptionPane.showMessageDialog(null, "Done");
    }
}
