import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.net.*;

public class Rce extends JFrame {

    protected JTextArea editor;

    protected JTextArea debug;

    protected String newline = "\n";

    JMenuBar menuBar;

    JMenu file, edit, compile, run, help;

    JMenuItem nnew, open, save, saveas, Exit;

    JMenuItem cut, copy, paste;

    JMenuItem comp, optc;

    JMenuItem runn, optr;

    JMenuItem contents, reg, about;

    String filename = "Untitled.java";

    String shortfilename = "";

    String dirname = "";

    String hostname = null;

    public Rce() {
        super("Remote Compiler");
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        nnew = new JMenuItem("New ", new ImageIcon("images/new.gif"));
        open = new JMenuItem("Open", new ImageIcon("images/open.gif"));
        save = new JMenuItem("Save", new ImageIcon("images/save.gif"));
        saveas = new JMenuItem("Save As", new ImageIcon("images/saveas.gif"));
        Exit = new JMenuItem("Exit", new ImageIcon("images/exit.gif"));
        nnew.addActionListener(new New());
        open.addActionListener(new Open());
        save.addActionListener(new Save());
        saveas.addActionListener(new SaveAs());
        Exit.addActionListener(new Exit());
        file.add(nnew);
        file.add(open);
        file.addSeparator();
        file.add(save);
        file.add(saveas);
        file.addSeparator();
        file.add(Exit);
        menuBar.add(file);
        setJMenuBar(menuBar);
        edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        copy = new JMenuItem("Copy ", new ImageIcon("images/copy.gif"));
        cut = new JMenuItem("Cut  ", new ImageIcon("images/cut.gif"));
        paste = new JMenuItem("Paste", new ImageIcon("images/paste.gif"));
        copy.addActionListener(new Copy());
        cut.addActionListener(new Cut());
        paste.addActionListener(new Paste());
        edit.add(copy);
        edit.add(cut);
        edit.add(paste);
        menuBar.add(edit);
        compile = new JMenu("Compile");
        compile.setMnemonic(KeyEvent.VK_C);
        comp = new JMenuItem("Compile ", new ImageIcon("images/compile.gif"));
        optc = new JMenuItem("Options", new ImageIcon("images/opt.gif"));
        comp.addActionListener(new Compiler());
        optc.addActionListener(new Optionsc());
        compile.add(comp);
        compile.add(optc);
        menuBar.add(compile);
        run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        runn = new JMenuItem("Run", new ImageIcon("images/run.gif"));
        optr = new JMenuItem("Options ", new ImageIcon("images/opt.gif"));
        runn.addActionListener(new Runners());
        optr.addActionListener(new Optionsr());
        run.add(runn);
        run.add(optr);
        menuBar.add(run);
        help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        reg = new JMenuItem("REgister", new ImageIcon("images/reg.gif"));
        about = new JMenuItem("About", new ImageIcon("images/about.gif"));
        contents = new JMenuItem("Contents ", new ImageIcon("images/contents.gif"));
        about.addActionListener(new About());
        reg.addActionListener(new Reg());
        contents.addActionListener(new Contents());
        help.add(about);
        help.addSeparator();
        help.add(contents);
        help.add(reg);
        menuBar.add(help);
        JToolBar toolBar = new JToolBar();
        addButtons(toolBar);
        editor = new JTextArea(5, 30);
        JScrollPane scrollPane = new JScrollPane(editor);
        debug = new JTextArea(5, 10);
        JScrollPane scrollPane2 = new JScrollPane(debug);
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setPreferredSize(new Dimension(400, 100));
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(scrollPane2, BorderLayout.SOUTH);
        setContentPane(contentPane);
    }

    protected void addButtons(JToolBar toolBar) {
        JButton button = null;
        nnew.addActionListener(new New());
        button = new JButton(new ImageIcon("images/new.gif"));
        button.setToolTipText("New blank source file");
        button.addActionListener(new New());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/open.gif"));
        button.setToolTipText("Open");
        button.addActionListener(new Open());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/save.gif"));
        button.setToolTipText("Save");
        button.addActionListener(new Save());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/saveas.gif"));
        button.setToolTipText("Save as");
        button.addActionListener(new SaveAs());
        toolBar.add(button);
        toolBar.addSeparator();
        button = new JButton(new ImageIcon("images/copy.gif"));
        button.setToolTipText("Copy");
        button.addActionListener(new Copy());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/cut.gif"));
        button.setToolTipText("Cut");
        button.addActionListener(new Cut());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/paste.gif"));
        button.setToolTipText("Paste");
        button.addActionListener(new Paste());
        toolBar.add(button);
        toolBar.addSeparator();
        button = new JButton(new ImageIcon("images/compile.gif"));
        button.setToolTipText("Compile");
        button.addActionListener(new Compiler());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/run.gif"));
        button.setToolTipText("Run");
        button.addActionListener(new Runners());
        toolBar.add(button);
        toolBar.addSeparator();
        button = new JButton(new ImageIcon("images/contents.gif"));
        button.setToolTipText("Help contents");
        button.addActionListener(new Contents());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/reg.gif"));
        button.setToolTipText("Register");
        button.addActionListener(new Reg());
        toolBar.add(button);
        toolBar.addSeparator();
        button = new JButton(new ImageIcon("images/font.gif"));
        button.setToolTipText("Font ");
        button.addActionListener(new Runners());
        toolBar.add(button);
        button = new JButton(new ImageIcon("images/color.gif"));
        button.setToolTipText("Color ");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                displayResult("Color");
            }
        });
        toolBar.add(button);
        for (int sepcount = 0; sepcount < 30; sepcount++) toolBar.addSeparator();
        button = new JButton(new ImageIcon("images/about.gif"));
        button.setToolTipText("About ");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            }
        });
        toolBar.add(button);
    }

    protected void displayResult(String actionDescription) {
        editor.append(actionDescription + newline);
    }

    class New implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            editor.setText("");
            debug.setText("                                                               *-*-*-*-*-*-*-*-*-*-*-*  ## Remote Compiler Debug Info ##*-*-*-*-*-*-*-*-*-*-*-*" + "\n");
            setTitle("Remote compiler --" + filename);
        }
    }

    class Open implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            FileDialog fd = new FileDialog(Rce.this, "Select File", FileDialog.LOAD);
            fd.show();
            if (fd.getFile() != null) {
                dirname = fd.getDirectory();
                filename = fd.getDirectory() + fd.getFile();
                shortfilename = fd.getFile();
                setTitle("Remote compiler -- " + filename);
                ReadFile();
            }
            editor.requestFocus();
        }
    }

    class Save implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            FileDialog fd = new FileDialog(Rce.this, "Select File", FileDialog.SAVE);
            if (filename.compareTo("Untitled.java") == 0) {
                fd.show();
                if (fd.getFile() != null) {
                    dirname = fd.getDirectory();
                    filename = fd.getDirectory() + fd.getFile();
                    setTitle("Remote compiler -- " + filename);
                    try {
                        DataOutputStream d = new DataOutputStream(new FileOutputStream(filename));
                        String line = editor.getText();
                        BufferedReader br = new BufferedReader(new StringReader(line));
                        while ((line = br.readLine()) != null) d.writeBytes(line + "\r\n");
                        d.close();
                    } catch (Exception ex) {
                        System.out.println("File not found");
                    }
                    editor.requestFocus();
                }
            } else {
                try {
                    DataOutputStream d = new DataOutputStream(new FileOutputStream(filename));
                    String line = editor.getText();
                    BufferedReader br = new BufferedReader(new StringReader(line));
                    while ((line = br.readLine()) != null) d.writeBytes(line + "\r\n");
                    d.close();
                } catch (Exception ex) {
                    System.out.println("File not found");
                }
                editor.requestFocus();
            }
        }
    }

    class SaveAs implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            FileDialog fd = new FileDialog(Rce.this, "Select File", FileDialog.SAVE);
            fd.show();
            if (fd.getFile() != null) {
                dirname = fd.getDirectory();
                filename = fd.getDirectory() + fd.getFile();
                setTitle("Remote compiler -- " + filename);
                try {
                    DataOutputStream d = new DataOutputStream(new FileOutputStream(filename));
                    String line = editor.getText();
                    BufferedReader br = new BufferedReader(new StringReader(line));
                    while ((line = br.readLine()) != null) d.writeBytes(line + "\r\n");
                    d.close();
                } catch (Exception ex) {
                    System.out.println("File not found");
                }
                editor.requestFocus();
            }
        }
    }

    class Exit implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    Clipboard clip = getToolkit().getSystemClipboard();

    class Copy implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String sel = editor.getSelectedText();
            StringSelection clipString = new StringSelection(sel);
            clip.setContents(clipString, clipString);
            editor.requestFocus();
        }
    }

    class Cut implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String sel2 = editor.getSelectedText();
            StringSelection ss = new StringSelection(sel2);
            clip.setContents(ss, ss);
            editor.replaceRange("", editor.getSelectionStart(), editor.getSelectionEnd());
            editor.requestFocus();
        }
    }

    class Paste implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Transferable cliptran = clip.getContents(Rce.this);
            try {
                String sel = (String) cliptran.getTransferData(DataFlavor.stringFlavor);
                editor.replaceRange(sel, editor.getSelectionStart(), editor.getSelectionEnd());
                editor.requestFocus();
            } catch (Exception ep) {
                System.out.println("Clip board not available");
            }
        }
    }

    void ReadFile() {
        BufferedReader d;
        StringBuffer sb = new StringBuffer();
        try {
            d = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = d.readLine()) != null) sb.append(line + "\n");
            editor.setText(sb.toString());
            d.close();
        } catch (Exception fe) {
            System.out.println("File not found");
        }
    }

    class Optionsc implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String address = null;
            hostname = JOptionPane.showInputDialog(Rce.this, "Enter The Host id");
            if (hostname == null || hostname.length() == 0) {
                System.out.println("Zero data");
            } else System.out.println("Entered data" + hostname);
        }
    }

    class Compiler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (!(filename.equalsIgnoreCase("Untitled.java"))) {
                debug.setText("Preparing Source File...........\n.");
                String sendfilename = new String("Temp.java");
                try {
                    DataOutputStream d = new DataOutputStream(new FileOutputStream(sendfilename));
                    String line = editor.getText();
                    BufferedReader br = new BufferedReader(new StringReader(line));
                    while ((line = br.readLine()) != null) d.writeBytes(line + "\r\n");
                    d.close();
                } catch (Exception ex) {
                    System.out.println("File not found");
                }
                editor.requestFocus();
                debug.append("Source File Prepared Successfully...........\n.");
                try {
                    debug.append("\nPreparing to send Source File send to : Host ");
                    send(sendfilename);
                } catch (IOException ioe) {
                    System.out.println("File saving error");
                }
            } else {
                JOptionPane.showMessageDialog(Rce.this, "First Edit The source File", "Source Edit", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        public void send(String s) throws IOException {
            Socket clientSocket = null;
            BufferedReader in = null;
            try {
                clientSocket = new Socket(InetAddress.getLocalHost(), 1947);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Could n't io for the  host");
                System.exit(1);
            }
            PrintWriter out = null;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (Exception e) {
                System.out.println("Could't get Socket");
                System.exit(0);
            }
            String inputLine, outputLine;
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            File f = new File(s);
            BufferedReader d = null;
            if (f.exists()) {
                try {
                    d = new BufferedReader(new FileReader(s));
                } catch (Exception e) {
                    System.out.println("File not found");
                }
                String line;
                out.write(shortfilename + "\n");
                out.flush();
                while ((line = d.readLine()) != null) {
                    out.write(line + "\n");
                    out.flush();
                }
                d.close();
            }
            out.close();
            clientSocket.close();
            debug.append("Source File send to : Host\n ");
            debug.append(" Compiling................\n.");
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(1948);
            } catch (IOException e) {
                System.err.println("Could n't ilisten to port 99");
                System.exit(0);
            }
            Socket clientSocket2 = null;
            try {
                System.out.println("In listen mode");
                clientSocket2 = serverSocket.accept();
                System.out.println("Connecteed to  : " + clientSocket);
            } catch (IOException e) {
                System.err.println("Acceptance failed");
                System.exit(0);
            }
            System.out.println("-------------at =------------1-------");
            in = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
            String filename = new String(" ");
            DataInputStream take = new DataInputStream(System.in);
            filename = in.readLine();
            while (true) {
                System.out.println(filename);
                debug.append(filename + "\n");
                if (filename.equalsIgnoreCase("quit.rcs")) break; else {
                    System.out.println("\n Filename  :  " + filename + "\n");
                    String userinput;
                    DataOutputStream f2 = null;
                    try {
                        f2 = new DataOutputStream(new FileOutputStream(dirname + filename));
                    } catch (Exception e) {
                        System.out.println("File not found");
                    }
                    System.out.println("Receiving started : ............");
                    int i = 0;
                    i = in.read();
                    while (true) {
                        System.out.print(i + " ");
                        if ((i == 63) | (i == -1)) break; else f2.write(i);
                        i = in.read();
                    }
                    f2.close();
                    System.out.println("--------------------------------------------");
                }
                filename = in.readLine();
            }
            System.out.println("--Successfully Compiled---");
            String er = null;
            while ((er = in.readLine()) != null) {
                debug.append("\n" + er);
                System.out.print(er + " ");
            }
            debug.append("\n");
            in.close();
            debug.append("\n" + "Successfully Compiled ........");
            clientSocket2.close();
            serverSocket.close();
        }
    }

    class Runners implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(Rce.this, "Future Use", "Run", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    class Optionsr implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(Rce.this, "Future Use", "Run Options", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    class About implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(Rce.this, "Remote Compiler\nThe team\n                    P C Varma\n                    A N Choudary\n                    P N Varma\n                    N Jyothsna\n                    Vneela", "About Remote Compiler ", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    class Reg implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(Rce.this, "Register at www.designlabs.netfirms.com", "Register", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    class Contents implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFrame help = new HtmlHelp();
            help.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    return;
                }
            });
            help.pack();
            help.setVisible(true);
        }
    }

    public static void main(String[] args) {
        Rce frame = new Rce();
        frame.pack();
        frame.setVisible(true);
    }
}
