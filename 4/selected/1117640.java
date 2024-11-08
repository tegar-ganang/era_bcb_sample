package beastcalc;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;

public class CalculatorFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextField textField;

    private JTextArea output;

    private String text;

    private ML mouseListener;

    private JPopupMenu outputPopupMenu, inputPopupMenu;

    public CalculatorFrame(int outputLines) {
        super();
        mouseListener = new ML();
        JButton button;
        JPanel menuPanel, textPanel, buttonPanel, lowerPanel, mainFrame;
        JMenuBar menuBar;
        JMenuItem jmItem;
        JMenu menu;
        menuPanel = new JPanel();
        textPanel = new JPanel();
        buttonPanel = new JPanel();
        lowerPanel = new JPanel();
        mainFrame = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.LINE_AXIS));
        menuPanel.setMaximumSize(new java.awt.Dimension(350, 60));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));
        textPanel.setMaximumSize(new java.awt.Dimension(230, 40));
        buttonPanel.setLayout(new GridLayout(4, 4, 5, 5));
        buttonPanel.setMaximumSize(new java.awt.Dimension(233, 200));
        lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.LINE_AXIS));
        lowerPanel.setMaximumSize(new java.awt.Dimension(300, 60));
        text = "";
        menu = new JMenu("Trig");
        jmItem = new JMenuItem("sin");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("cos");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("tan");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("csc");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("sec");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("cot");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menuPanel.add(menuBar);
        button = new JButton(7 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(8 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(9 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton("/");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        menu = new JMenu("Inverse Trig");
        jmItem = new JMenuItem("arcsin");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("arccos");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("arctan");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("arccsc");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("arcsec");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("arccot");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menuPanel.add(menuBar);
        button = new JButton(4 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(5 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(6 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton("*");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        menu = new JMenu("Exponents");
        jmItem = new JMenuItem("" + Main.calculator.SQUARE);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.CUBE);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FOUR);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FIVE);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SIX);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SEVEN);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.EIGHT);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.NINE);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menuPanel.add(menuBar);
        button = new JButton(1 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(2 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(3 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton("-");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        menu = new JMenu("Other");
        jmItem = new JMenuItem("(");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem(")");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("^");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("%");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("log");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("ln");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("abs");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.SQUARE_ROOT);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.CUBE_ROOT);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem("" + Main.calculator.FOUR_ROOT);
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menuPanel.add(menuBar);
        button = new JButton(0 + "");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton(".");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton("( " + Main.calculator.NEGATIVE + " )");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        button = new JButton("+");
        button.addMouseListener(mouseListener);
        buttonPanel.add(button);
        menu = new JMenu("Vars");
        jmItem = new JMenuItem("ans");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem(Main.calculator.E + "");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        jmItem = new JMenuItem(Main.calculator.PI + "");
        jmItem.addMouseListener(mouseListener);
        menu.add(jmItem);
        menuBar = new JMenuBar();
        menuBar.add(menu);
        menuPanel.add(menuBar);
        output = new JTextArea(outputLines, 10);
        output.setEditable(false);
        output.addMouseListener(mouseListener);
        JScrollPane scrollPane = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMaximumSize(new java.awt.Dimension(233, 100));
        textField = new JTextField();
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new UnderscoreFilter() {
        });
        textField.setMaximumSize(new java.awt.Dimension(200, 20));
        textField.addKeyListener(new KL());
        textField.addMouseListener(mouseListener);
        textPanel.add(textField);
        textPanel.add(Box.createHorizontalStrut(10));
        button = new JButton("" + Main.calculator.BACKSPACE);
        button.addMouseListener(mouseListener);
        textPanel.add(button);
        menuBar = new JMenuBar();
        menu = new JMenu("File");
        jmItem = new JMenuItem("Save As...");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        menu.addSeparator();
        jmItem = new JMenuItem("Open Graph");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        jmItem = new JMenuItem("Exit");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        jmItem = new JMenuItem("Exit All");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        menuBar.add(menu);
        menu = new JMenu("Tools");
        jmItem = new JMenuItem("Settings");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        menuBar.add(menu);
        menu = new JMenu("Help");
        jmItem = new JMenuItem("Help Contents");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        menu.addSeparator();
        jmItem = new JMenuItem("About BeastCalc");
        jmItem.addActionListener(this);
        menu.add(jmItem);
        menuBar.add(menu);
        button = new JButton("Enter");
        button.addMouseListener(mouseListener);
        lowerPanel.add(button);
        lowerPanel.add(Box.createHorizontalGlue());
        button = new JButton("Clear Output");
        button.addMouseListener(mouseListener);
        lowerPanel.add(button);
        lowerPanel.add(Box.createHorizontalGlue());
        button = new JButton("Graph");
        button.addMouseListener(mouseListener);
        lowerPanel.add(button);
        mainFrame.setLayout(new BoxLayout(mainFrame, BoxLayout.PAGE_AXIS));
        mainFrame.add(menuPanel);
        mainFrame.add(Box.createVerticalStrut(5));
        mainFrame.add(textPanel);
        mainFrame.add(Box.createVerticalStrut(5));
        mainFrame.add(buttonPanel);
        mainFrame.add(Box.createVerticalStrut(5));
        mainFrame.add(scrollPane);
        mainFrame.add(Box.createVerticalStrut(5));
        mainFrame.add(lowerPanel);
        createOutputPopupMenu();
        createInputPopupMenu();
        setJMenuBar(menuBar);
        getContentPane().add(mainFrame);
        requestFocus();
    }

    public void takeInput() {
        String text = textField.getText();
        if (!text.equals("")) {
            try {
                String t = Main.calculator.clearWhitespace(text);
                output.append(t + "\n" + " = " + (Main.calculator.formatOutput(Main.calculator.solve(Main.calculator.clearWhitespace(Main.calculator.replaceVariables(t))))) + "\n");
                textField.setText("");
            } catch (IllegalArgumentException ex) {
                output.append(ex.getMessage() + "\n");
            }
            output.setCaretPosition(output.getDocument().getLength());
        }
    }

    class KL extends KeyAdapter {

        public void keyPressed(KeyEvent e) {
            char c = e.getKeyChar();
            if (c == KeyEvent.VK_ENTER) {
                takeInput();
            }
        }
    }

    class ML extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            String txt = textField.getText();
            String fcar = txt.substring(0, textField.getCaretPosition());
            String car = txt.substring(textField.getCaretPosition());
            String res = "";
            if (e.getSource().getClass().equals(JButton.class)) {
                text = ((JButton) e.getSource()).getText();
                if (text.equals("" + Main.calculator.BACKSPACE)) {
                    if (fcar.length() > 0) {
                        fcar = fcar.substring(0, fcar.length() - 1);
                        textField.setText(fcar + car);
                        textField.setCaretPosition(textField.getText().length() - car.length());
                    }
                } else if (text.equals("Enter")) {
                    takeInput();
                } else if (text.contains("" + Main.calculator.NEGATIVE)) {
                    res = "" + Main.calculator.NEGATIVE;
                    textField.setText(fcar + res + car);
                    textField.setCaretPosition(textField.getText().length() - car.length());
                } else if (text.equals("Graph")) Main.showGraph(); else if (text.equals("Clear Output")) {
                    output.setText("");
                } else {
                    res = text;
                    textField.setText(fcar + res + car);
                    textField.setCaretPosition(textField.getText().length() - car.length());
                }
                txt = "";
                car = "";
                fcar = "";
                textField.requestFocus();
            } else if (e.getSource().getClass().equals(JMenuItem.class)) {
                text = ((JMenuItem) e.getSource()).getText();
                if (text.length() == 1 || text == "ans") {
                    textField.setText(fcar + text + car);
                    textField.setCaretPosition(textField.getText().length() - car.length());
                } else {
                    textField.setText(fcar + text + "()" + car);
                    textField.setCaretPosition(textField.getText().length() - car.length() - 1);
                }
                txt = "";
                car = "";
                fcar = "";
                textField.requestFocus();
            } else if (e.getSource().getClass().equals(JTextArea.class) || e.getSource().getClass().equals(JTextField.class)) {
                maybeShowPopup(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.getSource().getClass().equals(JTextArea.class) || e.getSource().getClass().equals(JTextField.class)) {
                maybeShowPopup(e);
            }
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (e.getSource().getClass().equals(JTextArea.class)) outputPopupMenu.show(e.getComponent(), e.getX(), e.getY()); else inputPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private void createOutputPopupMenu() {
        outputPopupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Copy");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String s = output.getSelectedText();
                if (s != null && !s.isEmpty()) {
                    output.copy();
                }
            }
        });
        outputPopupMenu.add(menuItem);
    }

    private void createInputPopupMenu() {
        inputPopupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Cut");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String s = textField.getSelectedText();
                if (s != null && !s.isEmpty()) {
                    textField.cut();
                }
            }
        });
        inputPopupMenu.add(menuItem);
        menuItem = new JMenuItem("Copy");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String s = textField.getSelectedText();
                if (s != null && !s.isEmpty()) {
                    textField.copy();
                }
            }
        });
        inputPopupMenu.add(menuItem);
        menuItem = new JMenuItem("Paste");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                textField.paste();
            }
        });
        inputPopupMenu.add(menuItem);
    }

    public void setOutputWrapAround(boolean state) {
        output.setLineWrap(state);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().getClass().equals(JMenuItem.class)) {
            text = ((JMenuItem) e.getSource()).getText();
            if (text.equals("Exit")) {
                if (Main.configFile.trayIcon > 0) System.exit(0); else this.dispose();
            } else if (text.equals("Exit All")) {
                System.exit(0);
            } else if (text.equals("Open Graph")) {
                Main.showGraph();
            } else if (text.equals("Save As...")) {
                JOptionPane.showMessageDialog(this, "Some symbols may not be displayed correctly in the text file.", "Warning", JOptionPane.WARNING_MESSAGE);
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String file = chooser.getSelectedFile().getPath();
                    if (file.lastIndexOf(".") < 0) file += ".txt";
                    if (file.substring(file.lastIndexOf(".") + 1).equals("txt")) {
                        File f = new File(file);
                        if (!f.exists() || JOptionPane.showConfirmDialog(this, "There is already a file with the name " + file.substring(file.lastIndexOf("\\") + 1) + ", are you sure you wish to overwrite it?") == JOptionPane.YES_OPTION) {
                            try {
                                PrintWriter out = new PrintWriter(new FileWriter(f));
                                String[] temp = output.getText().split("\n");
                                for (int i = 0; i < temp.length; i++) {
                                    out.write(Main.calculator.formatOutputASCII(temp[i]));
                                    out.println();
                                }
                                out.close();
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            } else if (text.equals("About BeastCalc")) {
                Main.showAbout();
            } else if (text.equals("Help Contents")) {
                Main.showHelp();
            } else if (text.equals("Settings")) {
                Main.showSettings();
            }
        }
    }
}
