package vgsoft.vgsudoku;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

public class MainWindow extends JFrame implements PuzzleGrid.NumberKeyListener {

    private static final long serialVersionUID = 4819764369631103591L;

    private Puzzle puzzle = new Puzzle();

    private PuzzleGrid puzzleGrid = new PuzzleGrid(puzzle);

    private List<PuzzleDescriptor> pDescs = null;

    private JFileChooser fileChooser = new JFileChooser();

    private FileFilter pDescFileFilter, puzzleFileFilter;

    private Random random = null;

    private JMenuItem rndPuzzleBtn, gotoPuzzleBtn, setGivensBtn, showCandsBtn, showHideCandsBtn, aboutBtn;

    private JPanel numBtnPnl = new JPanel();

    private HideCandidatesWidget hideCandsPad = new HideCandidatesWidget(puzzle, puzzleGrid);

    private ChooseColorsDialog ccDialog = new ChooseColorsDialog(this, puzzleGrid, hideCandsPad);

    private MainWindow(Properties properties) {
        setTitle("VG Sudoku");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                saveConfiguration();
                System.exit(0);
            }
        });
        makeUI();
        pack();
        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        DisplayMode dm = getGraphicsConfiguration().getDevice().getDisplayMode();
        Dimension size = getSize();
        setLocation((dm.getWidth() - size.width) / 2, (dm.getHeight() - size.height) / 2);
        puzzleGrid.addNumberKeyListener(this);
        pDescFileFilter = new FileFilter() {

            public String getDescription() {
                return "Sudoku text files";
            }

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".txt");
            }
        };
        puzzleFileFilter = new FileFilter() {

            public String getDescription() {
                return "VG Sudoku saved puzzles";
            }

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".ser");
            }
        };
        if (properties != null) loadConfiguration(properties);
    }

    public static void main(String[] args) {
        Properties props = null;
        File cFile = new File(System.getProperty("user.home"), "VG-Sudoku.conf");
        if (cFile.exists()) {
            props = new Properties();
            InputStream in = null;
            try {
                try {
                    in = new FileInputStream(cFile);
                    props.load(in);
                } finally {
                    in.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
        MainWindow win = new MainWindow(props);
        win.setVisible(true);
    }

    public void setVisible(boolean b) {
        super.setVisible(b);
        puzzleGrid.requestFocusInWindow();
    }

    private void makeUI() {
        JMenuBar mBar = new JMenuBar();
        JMenuItem mItem;
        JMenu menu;
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        mItem = new JMenuItem("New");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newPuzzle();
            }
        });
        menu.add(mItem);
        mItem = new JMenuItem("Restart");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                restartPuzzle();
            }
        });
        menu.add(mItem);
        mItem = new JMenuItem("Open...");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openPuzzle();
            }
        });
        menu.add(mItem);
        mItem = new JMenuItem("Save...");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                savePuzzle();
            }
        });
        menu.add(mItem);
        menu.addSeparator();
        mItem = new JMenuItem("Load descriptor file...");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadDescriptorFile();
            }
        });
        menu.add(mItem);
        rndPuzzleBtn = new JMenuItem("Random puzzle");
        rndPuzzleBtn.setEnabled(false);
        rndPuzzleBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showRandomPuzzle();
            }
        });
        menu.add(rndPuzzleBtn);
        gotoPuzzleBtn = new JMenuItem("Go to puzzle...");
        gotoPuzzleBtn.setEnabled(false);
        gotoPuzzleBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showPuzzleByNumber();
            }
        });
        menu.add(gotoPuzzleBtn);
        menu.addSeparator();
        mItem = new JMenuItem("Exit");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
                System.exit(0);
            }
        });
        menu.add(mItem);
        mBar.add(menu);
        menu = new JMenu("Options");
        menu.setMnemonic(KeyEvent.VK_O);
        setGivensBtn = new JCheckBoxMenuItem("Set givens", false);
        menu.add(setGivensBtn);
        menu.addSeparator();
        showCandsBtn = new JCheckBoxMenuItem("Show candidates", false);
        showCandsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                puzzleGrid.setShowCandidates(showCandsBtn.isSelected());
            }
        });
        menu.add(showCandsBtn);
        mItem = new JCheckBoxMenuItem("Show selected Row / Column / Box", false);
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                puzzleGrid.setShowRowColumnBox(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        menu.add(mItem);
        mBar.add(menu);
        menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);
        mItem = new JCheckBoxMenuItem("Number buttons", false);
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                numBtnPnl.setVisible(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        menu.add(mItem);
        showHideCandsBtn = new JCheckBoxMenuItem("Candidate hide pad", false);
        showHideCandsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                hideCandsPad.setVisible(showHideCandsBtn.isSelected());
                pack();
            }
        });
        menu.add(showHideCandsBtn);
        menu.addSeparator();
        mItem = new JMenuItem("Choose colors...");
        mItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ccDialog.setVisible(true);
            }
        });
        menu.add(mItem);
        mBar.add(menu);
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        aboutBtn = new JMenuItem("About VG Sudoku...");
        aboutBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            }
        });
        menu.add(aboutBtn);
        mBar.add(menu);
        setJMenuBar(mBar);
        Container cp = getContentPane();
        numBtnPnl.setVisible(false);
        numBtnPnl.setLayout(new BoxLayout(numBtnPnl, BoxLayout.X_AXIS));
        numBtnPnl.add(Box.createGlue());
        for (int i = 0; i < 9; i++) {
            JButton btn = new JButton(String.valueOf(i + 1));
            final Integer number = new Integer(i + 1);
            btn.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    numberKeyPressed(number, puzzleGrid.getSelectedRow(), puzzleGrid.getSelectedColumn());
                    puzzleGrid.requestFocusInWindow();
                }
            });
            numBtnPnl.add(btn);
        }
        numBtnPnl.add(Box.createGlue());
        numBtnPnl.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        cp.add(numBtnPnl, BorderLayout.NORTH);
        JPanel compPnl = new JPanel(new GridBagLayout());
        compPnl.add(puzzleGrid, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(12, 12, 12, 12), 0, 0));
        hideCandsPad.setVisible(false);
        compPnl.add(hideCandsPad, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(12, 0, 0, 12), 0, 0));
        cp.add(compPnl, BorderLayout.CENTER);
    }

    public void numberKeyPressed(Integer number, int row, int column) {
        NumberConflict nc;
        if (setGivensBtn.isSelected()) nc = puzzle.setGiven(number, row, column); else nc = puzzle.putNumber(number, row, column);
        if (nc != null) {
            StringBuffer sb = new StringBuffer("One or more number conflicts occured:");
            if (nc.isRowConflict()) sb.append("\nAt Row ").append(nc.getRow()).append(" (").append(nc.getRowIndex()).append(")");
            if (nc.isColumnConflict()) sb.append("\nAt Column ").append(nc.getColumn()).append(" (").append(nc.getColumnIndex()).append(")");
            if (nc.isBoxConflict()) sb.append("\nAt Box ").append(nc.getBoxRow()).append(", ").append(nc.getBoxColumn()).append(" (").append(nc.getBoxRowIndex()).append(", ").append(nc.getBoxColumnIndex()).append(")");
            JOptionPane.showMessageDialog(this, sb.toString(), "Number Conflict", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void useNewPuzzle(Puzzle newPuzzle) {
        puzzle = newPuzzle;
        puzzleGrid.setPuzzle(newPuzzle);
        hideCandsPad.setPuzzle(newPuzzle);
    }

    private void newPuzzle() {
        useNewPuzzle(new Puzzle());
        setTitle("VG Sudoku");
    }

    private void restartPuzzle() {
        PuzzleDescriptor pd = puzzle.getPuzzleDescriptor();
        Puzzle p = new Puzzle();
        for (PuzzleDescriptor.Entry entry : pd) p.setGiven(entry.getNumber(), entry.getRow(), entry.getColumn());
        useNewPuzzle(p);
    }

    private void showRandomPuzzle() {
        if (random == null) random = new Random(System.currentTimeMillis());
        int rndNum = random.nextInt(pDescs.size());
        PuzzleDescriptor pd = pDescs.get(rndNum);
        Puzzle p = new Puzzle();
        for (PuzzleDescriptor.Entry entry : pd) p.setGiven(entry.getNumber(), entry.getRow(), entry.getColumn());
        setTitle("VG Sudoku - " + rndNum);
        useNewPuzzle(p);
    }

    private void showPuzzleByNumber() {
        String numStr = JOptionPane.showInputDialog(this, "Select a puzzle number from 1 to " + pDescs.size());
        try {
            int pnum = Integer.parseInt(numStr);
            if (pnum < 1 || pnum > pDescs.size()) JOptionPane.showMessageDialog(this, "Please type a number from 1 to " + pDescs.size());
            PuzzleDescriptor pd = pDescs.get(pnum);
            Puzzle p = new Puzzle();
            for (PuzzleDescriptor.Entry entry : pd) p.setGiven(entry.getNumber(), entry.getRow(), entry.getColumn());
            setTitle("VG Sudoku - " + pnum);
            useNewPuzzle(p);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please type a number from 1 to " + pDescs.size());
        }
    }

    private void loadDescriptorFile() {
        fileChooser.setFileFilter(pDescFileFilter);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File pFile = fileChooser.getSelectedFile();
            if (!pFile.exists()) {
                JOptionPane.showMessageDialog(this, "The file " + pFile.getName() + " was not found!", "File not found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            final LoadDialog lDialog = new LoadDialog();
            lDialog.setCount((int) (pFile.length() / 82));
            lDialog.setVisible(true);
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            new Thread(new Runnable() {

                public void run() {
                    try {
                        if (pDescs == null) pDescs = new ArrayList<PuzzleDescriptor>(); else pDescs.clear();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pFile), "iso-8859-1"));
                        int count = 0;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.length() == 81) {
                                PuzzleDescriptor pd = new PuzzleDescriptor();
                                char c;
                                for (int i = 0; i < 81; i++) {
                                    c = line.charAt(i);
                                    if (c != '0') pd.putGiven(c - '0', i / 9, i % 9);
                                }
                                pDescs.add(pd);
                            }
                            if (++count % 100 == 0) {
                                final int c = count;
                                SwingUtilities.invokeLater(new Runnable() {

                                    public void run() {
                                        lDialog.setCurrent(c);
                                    }

                                    ;
                                });
                            }
                        }
                        reader.close();
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                lDialog.setVisible(false);
                                rndPuzzleBtn.setEnabled(true);
                                gotoPuzzleBtn.setEnabled(true);
                                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            }

                            ;
                        });
                    }
                }
            }).start();
        }
    }

    private void savePuzzle() {
        fileChooser.setFileFilter(puzzleFileFilter);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File pFile = fileChooser.getSelectedFile();
            if (!pFile.getName().endsWith(".ser")) pFile = new File(pFile.getParentFile(), pFile.getName() + ".ser");
            boolean goOn = true;
            if (pFile.exists()) goOn = JOptionPane.showConfirmDialog(this, "The file " + pFile.getName() + " already exists, overwrite?", "File already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            if (goOn) {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(pFile));
                    try {
                        out.writeObject(puzzle);
                    } finally {
                        out.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    private void openPuzzle() {
        fileChooser.setFileFilter(puzzleFileFilter);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File pFile = fileChooser.getSelectedFile();
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(pFile));
                try {
                    Puzzle p = (Puzzle) in.readObject();
                    setTitle("VG Sudoku - " + pFile.getName());
                    useNewPuzzle(p);
                } finally {
                    in.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    private void loadConfiguration(Properties props) {
        Properties cProps = getSubProperties(props, "colors");
        if (cProps != null) {
            Color c;
            c = Color.decode("0x" + cProps.getProperty("grid"));
            puzzleGrid.setGridColor(c);
            c = Color.decode("0x" + cProps.getProperty("givens"));
            puzzleGrid.setGivenColor(c);
            c = Color.decode("0x" + cProps.getProperty("candidates"));
            puzzleGrid.setCandColor(c);
            c = Color.decode("0x" + cProps.getProperty("numbers"));
            puzzleGrid.setNumberColor(c);
            c = Color.decode("0x" + cProps.getProperty("selected-cell"));
            puzzleGrid.setSelColor(c);
        }
        String fcPath = props.getProperty("files.current-path");
        if (fcPath != null && !fcPath.equals("")) fileChooser.setCurrentDirectory(new File(fcPath));
    }

    private void saveConfiguration() {
        Properties props = new Properties();
        props.setProperty("colors.grid", Integer.toHexString(puzzleGrid.getGridColor().getRGB() & 0x00ffffff));
        props.setProperty("colors.givens", Integer.toHexString(puzzleGrid.getGivenColor().getRGB() & 0x00ffffff));
        props.setProperty("colors.candidates", Integer.toHexString(puzzleGrid.getCandColor().getRGB() & 0x00ffffff));
        props.setProperty("colors.numbers", Integer.toHexString(puzzleGrid.getNumberColor().getRGB() & 0x00ffffff));
        props.setProperty("colors.selected-cell", Integer.toHexString(puzzleGrid.getSelColor().getRGB() & 0x00ffffff));
        props.setProperty("files.current-path", fileChooser.getCurrentDirectory().getAbsolutePath());
        File cFile = new File(System.getProperty("user.home"), "VG-Sudoku.conf");
        try {
            OutputStream out = null;
            try {
                out = new FileOutputStream(cFile);
                props.store(out, "VG-Sudoku configuration");
            } finally {
                out.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    private Properties getSubProperties(Properties props, String prefix) {
        if (props == null) return null;
        Properties subProps = new Properties();
        int pLen = prefix.length();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(prefix)) subProps.setProperty(key.substring(pLen + 1), (String) entry.getValue());
        }
        return subProps;
    }

    @SuppressWarnings("serial")
    private class LoadDialog extends JWindow {

        private JLabel lbl = new JLabel("Initializing...");

        private JProgressBar bar = new JProgressBar();

        private LoadDialog() {
            super(MainWindow.this);
            bar.setIndeterminate(true);
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.add(lbl);
            pnl.add(bar);
            pnl.setBorder(BorderFactory.createRaisedBevelBorder());
            this.add(pnl);
            this.pack();
            Dimension pSize = MainWindow.this.getSize(), size = getSize();
            Point pLocation = MainWindow.this.getLocation();
            setLocation(pLocation.x + (pSize.width - size.width) / 2, pLocation.y + (pSize.height - size.height) / 2);
        }

        private void setCount(int count) {
            bar.setIndeterminate(false);
            bar.setMaximum(count);
            lbl.setText("Loading...");
        }

        private void setCurrent(int current) {
            bar.setValue(current);
            lbl.setText("Loading " + current + " of " + bar.getMaximum());
        }
    }

    @SuppressWarnings("serial")
    private class AboutDialog extends JDialog {

        private AboutDialog() {
            super(MainWindow.this, "About VG Sudoku", true);
        }
    }
}
