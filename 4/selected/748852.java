package keyboardhero;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.channels.*;
import javax.sound.midi.*;
import keyboardhero.MidiSong.*;
import keyboardhero.Util.*;

final class DialogSongList extends AbstractDialog {

    static final class SongSelector extends JScrollPane {

        private static final String[] COLS = new String[] { "Author", "Title", "Difficulty" };

        static final TreeMap<Byte, String> DIFFICULTIES = new TreeMap<Byte, String>();

        static {
            DIFFICULTIES.put((byte) 10, "Easy");
            DIFFICULTIES.put((byte) 20, "ModeratelyEasy");
            DIFFICULTIES.put((byte) 30, "Medium");
            DIFFICULTIES.put((byte) 40, "ModeratelyHard");
            DIFFICULTIES.put((byte) 50, "Hard");
        }

        private static final long serialVersionUID = 3925531085505441301L;

        private static final FilenameFilter MIDI_FILENAME_FILTER = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return (name.endsWith(".mid") || name.endsWith(".midi"));
            }
        };

        private static Object[][] data = new Object[0][3];

        private static URL[] files = new URL[0];

        private static int[] nojars = new int[0];

        private static int rowCount;

        private static ArrayList<SongSelector> instances = new ArrayList<SongSelector>();

        private static JTable lastTable;

        private JTable table;

        private Runnable doubleClick = null, selectionAction = null;

        private String configKey;

        private final boolean noJar;

        SongSelector(String configKey) {
            this(configKey, null, false);
        }

        SongSelector(String configKey, Runnable doubleClickAction) {
            this(configKey, doubleClickAction, false);
        }

        SongSelector(String configKey, Runnable doubleClickAction, final boolean noJar) {
            super(lastTable = new JTable(new AbstractTableModel() {

                private static final long serialVersionUID = 704371080036381343L;

                public String getColumnName(int column) {
                    return Util.getMsg(COLS[column]);
                }

                public int getRowCount() {
                    if (noJar) {
                        return Math.max(nojars.length, 1);
                    } else {
                        return rowCount;
                    }
                }

                public int getColumnCount() {
                    return COLS.length;
                }

                public Object getValueAt(int rowIndex, int colIndex) {
                    if (noJar && nojars.length == 0) {
                        switch(colIndex) {
                            case 0:
                                return "";
                            case 1:
                                return Util.getMsg("NoImportedFiles");
                            default:
                                return "";
                        }
                    } else {
                        if (noJar) rowIndex = nojars[rowIndex];
                        if (data[rowIndex][colIndex] == null) return "";
                        if (colIndex == 2) {
                            final String difficulty = DIFFICULTIES.get(data[rowIndex][2]);
                            if (difficulty == null) return "";
                            return Util.getMsg(difficulty, "");
                        }
                        return data[rowIndex][colIndex];
                    }
                }

                public boolean isCellEditable(int rowIndex, int colIndex) {
                    return false;
                }
            }));
            table = lastTable;
            this.configKey = configKey;
            this.noJar = noJar;
            setAction(doubleClickAction);
            final TableColumnModel columnModel = table.getColumnModel();
            final String widths[] = Util.getProp(configKey + "ColumnWidths").split("\\|");
            for (int i = 0; i < widths.length && i < COLS.length; ++i) {
                try {
                    columnModel.getColumn(i).setPreferredWidth(Integer.parseInt(widths[i]));
                } catch (NumberFormatException e) {
                }
            }
            table.setFillsViewportHeight(true);
            table.setAutoCreateRowSorter(true);
            table.setSelectionModel(new DefaultListSelectionModel() {

                private static final long serialVersionUID = -8239018545766232662L;

                public int getLeadSelectionIndex() {
                    return -1;
                }
            });
            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    if (selectionAction != null) {
                        selectionAction.run();
                    }
                }
            });
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            int sortIndex = Util.getPropInt(configKey + "SortIndex");
            if (sortIndex >= 10) {
                sortIndex -= 10;
                final RowSorter<?> rowSorter = table.getRowSorter();
                rowSorter.toggleSortOrder(sortIndex);
                rowSorter.toggleSortOrder(sortIndex);
            } else {
                table.getRowSorter().toggleSortOrder(sortIndex);
            }
            table.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (doubleClick != null && e.getClickCount() == 2) {
                        doubleClick.run();
                    }
                }
            });
            instances.add(this);
        }

        URL getSelectedFile() {
            int index = table.getSelectedRow();
            if (index == -1) return null;
            index = table.getRowSorter().convertRowIndexToModel(index);
            if (noJar) index = nojars[index];
            return files[index];
        }

        Item<URL, MidiFileInfo> getSelectedInfo() {
            int index = table.getSelectedRow();
            if (index == -1) return null;
            index = table.getRowSorter().convertRowIndexToModel(index);
            if (noJar) index = nojars[index];
            return new Item<URL, MidiFileInfo>(files[index], new MidiFileInfo((String) data[index][0], (String) data[index][1], (Byte) data[index][2]));
        }

        void setSelected(URL selected) {
            if (selected != null) {
                for (int i = 0; i < files.length; ++i) {
                    if (selected.equals(files[i])) {
                        int index;
                        if (noJar) {
                            index = Arrays.binarySearch(nojars, i);
                            if (index == -1) break;
                        } else {
                            index = i;
                        }
                        index = table.getRowSorter().convertRowIndexToView(index);
                        table.getSelectionModel().setSelectionInterval(index, index);
                        return;
                    }
                }
            }
            table.getSelectionModel().setSelectionInterval(-1, -1);
        }

        void setAction(Runnable doubleClickAction) {
            this.doubleClick = doubleClickAction;
        }

        Runnable getAction() {
            return doubleClick;
        }

        void setSelectionAction(Runnable selectionAction) {
            this.selectionAction = selectionAction;
        }

        Runnable getSelectionAction() {
            return selectionAction;
        }

        static URL[] getFiles() {
            return Arrays.copyOf(files, rowCount);
        }

        static void refresh() {
            File[] files1 = new File[0], files2 = new File[0];
            File dir1 = null;
            URL url = Util.getURL(MidiSong.MIDI_FILES_DIR, true);
            if (url != null) {
                try {
                    dir1 = new File(url.toURI());
                    files1 = dir1.listFiles(MIDI_FILENAME_FILTER);
                } catch (Exception e) {
                }
            }
            url = Util.getURL(MidiSong.MIDI_FILES_DIR, false);
            if (url != null) {
                try {
                    File dir = new File(url.toURI());
                    if (!dir.equals(dir1)) {
                        files2 = dir.listFiles(MIDI_FILENAME_FILTER);
                    }
                } catch (Exception e) {
                }
            }
            String[] files3 = Util.getJarDirList(Util.DATA_FOLDER + MidiSong.MIDI_FILES_DIR, MIDI_FILENAME_FILTER);
            int numOfRows = files1.length + files2.length + files3.length;
            files = new URL[numOfRows];
            data = new Object[numOfRows][3];
            ArrayList<Integer> nojars = new ArrayList<Integer>();
            int i = 0;
            for (File file : files1) {
                try {
                    final URL url2 = file.toURI().toURL();
                    if (!"jar".equals(url2.getProtocol())) {
                        nojars.add(i);
                    }
                    MidiFileInfo fileInfo = MidiSong.getMidiFileInfo(url2);
                    data[i][0] = fileInfo.getAuthor();
                    data[i][1] = fileInfo.getTitle();
                    data[i][2] = fileInfo.getDifficulty();
                    files[i++] = url2;
                } catch (IOException e) {
                } catch (InvalidMidiDataException e) {
                }
            }
            for (File file : files2) {
                try {
                    final URL url2 = file.toURI().toURL();
                    if (!"jar".equals(url2.getProtocol())) {
                        nojars.add(i);
                    }
                    MidiFileInfo fileInfo = MidiSong.getMidiFileInfo(url2);
                    data[i][0] = fileInfo.getAuthor();
                    data[i][1] = fileInfo.getTitle();
                    data[i][2] = fileInfo.getDifficulty();
                    files[i++] = url2;
                } catch (IOException e) {
                } catch (InvalidMidiDataException e) {
                }
            }
            for (String fname : files3) {
                try {
                    final URL file = Util.getURL(MidiSong.MIDI_FILES_DIR + fname, true);
                    MidiFileInfo fileInfo = MidiSong.getMidiFileInfo(file);
                    data[i][0] = fileInfo.getAuthor();
                    data[i][1] = fileInfo.getTitle();
                    data[i][2] = fileInfo.getDifficulty();
                    files[i++] = file;
                } catch (Exception e) {
                }
            }
            rowCount = i;
            SongSelector.nojars = Util.toArray(nojars);
            for (SongSelector instance : instances) {
                instance.table.tableChanged(new TableModelEvent(instance.table.getModel()));
            }
        }

        void updateTexts() {
            final TableColumnModel columnModel = table.getTableHeader().getColumnModel();
            for (int i = 0; i < COLS.length; ++i) {
                columnModel.getColumn(i).setHeaderValue(COLS[i]);
            }
            table.tableChanged(new TableModelEvent(table.getModel()));
        }

        public void updateUI() {
            super.updateUI();
            if (table != null) table.updateUI();
        }

        void closure() {
            int songListerSortIndex = 1;
            for (RowSorter.SortKey k : table.getRowSorter().getSortKeys()) {
                final SortOrder sortOrder = k.getSortOrder();
                if (sortOrder == SortOrder.ASCENDING) {
                    songListerSortIndex = k.getColumn();
                    break;
                }
                if (sortOrder == SortOrder.DESCENDING) {
                    songListerSortIndex = k.getColumn() + 10;
                    break;
                }
            }
            Util.setProp(configKey + "SortIndex", songListerSortIndex);
            final StringBuffer buff = new StringBuffer();
            final TableColumnModel columnModel = table.getColumnModel();
            buff.append(columnModel.getColumn(0).getWidth());
            for (int i = 1; i < COLS.length; i++) {
                buff.append('|');
                buff.append(columnModel.getColumn(i).getWidth());
            }
            Util.setProp(configKey + "ColumnWidths", buff.toString());
        }
    }

    private static final long serialVersionUID = -5547093721008793310L;

    private static DialogSongList instance = null;

    private SongSelector songSelector;

    private JTextField author, title;

    private JComboBox difficulty;

    private JButton save;

    private File selection;

    public DialogSongList(JFrame frame) {
        super(frame, "Menu_SongList", "songList");
        setMinimumSize(new Dimension(400, 200));
        JPanel panel, spanel;
        Container contentPane;
        (contentPane = getContentPane()).add(songSelector = new SongSelector(configKey, null, true));
        songSelector.setSelectionAction(new Runnable() {

            public void run() {
                final Item<URL, MidiFileInfo> item = songSelector.getSelectedInfo();
                if (item != null) {
                    try {
                        selection = new File(item.getKey().toURI());
                        author.setEnabled(true);
                        title.setEnabled(true);
                        difficulty.setEnabled(true);
                        save.setEnabled(true);
                        final MidiFileInfo info = item.getValue();
                        author.setText(info.getAuthor());
                        title.setText(info.getTitle());
                        Util.selectKey(difficulty, info.getDifficulty());
                        return;
                    } catch (Exception e) {
                    }
                }
                selection = null;
                author.setEnabled(false);
                title.setEnabled(false);
                difficulty.setEnabled(false);
                save.setEnabled(false);
            }
        });
        contentPane.add(panel = new JPanel(), BorderLayout.SOUTH);
        panel.setLayout(new BorderLayout());
        JScrollPane scrollPane;
        panel.add(scrollPane = new JScrollPane(spanel = new JPanel()), BorderLayout.NORTH);
        scrollPane.setPreferredSize(new Dimension(0, 60));
        Util.addLabeledComponent(spanel, "Lbl_Author", author = new JTextField(10));
        Util.addLabeledComponent(spanel, "Lbl_Title", title = new JTextField(14));
        Util.addLabeledComponent(spanel, "Lbl_Difficulty", difficulty = new JComboBox());
        difficulty.addItem(new Item<Byte, String>((byte) -1, ""));
        for (Map.Entry<Byte, String> entry : SongSelector.DIFFICULTIES.entrySet()) {
            final String value = entry.getValue();
            difficulty.addItem(new Item<Byte, String>(entry.getKey(), Util.getMsg(value, value), value));
        }
        spanel.add(save = new JButton());
        Util.updateButtonText(save, "Save");
        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final File selected = MidiSong.setMidiFileInfo(selection, author.getText(), title.getText(), getAsByte(difficulty));
                SongSelector.refresh();
                try {
                    songSelector.setSelected(selected == null ? null : selected.toURI().toURL());
                } catch (MalformedURLException ex) {
                }
            }
        });
        author.setEnabled(false);
        title.setEnabled(false);
        difficulty.setEnabled(false);
        save.setEnabled(false);
        JButton button;
        panel.add(spanel = new JPanel(), BorderLayout.WEST);
        spanel.add(button = new JButton());
        Util.updateButtonText(button, "Import");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final File inputFile = KeyboardHero.midiFile();
                try {
                    if (inputFile == null) return;
                    final File dir = (new File(Util.DATA_FOLDER + MidiSong.MIDI_FILES_DIR));
                    if (dir.exists()) {
                        if (!dir.isDirectory()) {
                            Util.error(Util.getMsg("Err_MidiFilesDirNotDirectory"), dir.getParent());
                            return;
                        }
                    } else if (!dir.mkdirs()) {
                        Util.error(Util.getMsg("Err_CouldntMkDir"), dir.getParent());
                        return;
                    }
                    File outputFile = new File(dir.getPath() + File.separator + inputFile.getName());
                    if (!outputFile.exists() || KeyboardHero.confirm("Que_FileExistsOverwrite")) {
                        final FileChannel inChannel = new FileInputStream(inputFile).getChannel();
                        inChannel.transferTo(0, inChannel.size(), new FileOutputStream(outputFile).getChannel());
                    }
                } catch (Exception ex) {
                    Util.getMsg(Util.getMsg("Err_CouldntImportSong"), ex.toString());
                }
                SongSelector.refresh();
            }
        });
        spanel.add(button = new JButton());
        Util.updateButtonText(button, "Delete");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (KeyboardHero.confirm(Util.getMsg("Que_SureToDelete"))) {
                    try {
                        new File(songSelector.getSelectedFile().toURI()).delete();
                    } catch (Exception ex) {
                        Util.error(Util.getMsg("Err_CouldntDeleteFile"), ex.toString());
                    }
                    SongSelector.refresh();
                }
            }
        });
        panel.add(spanel = new JPanel(), BorderLayout.CENTER);
        spanel.setLayout(new FlowLayout());
        spanel.add(button = new JButton());
        Util.updateButtonText(button, "Close");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        spanel.add(button = new JButton());
        Util.updateButtonText(button, "Play");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Game.newGame(songSelector.getSelectedFile());
                close();
            }
        });
        panel.add(spanel = new JPanel(), BorderLayout.EAST);
        spanel.add(button = new JButton());
        Util.updateButtonText(button, "Refresh");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SongSelector.refresh();
            }
        });
        getRootPane().setDefaultButton(button);
        instance = this;
    }

    private static byte getAsByte(JComboBox comboBox) {
        final Item<?, ?> item = (Item<?, ?>) comboBox.getSelectedItem();
        if (item == null) return 0;
        final Object key = item.getKey();
        if (key instanceof Byte) {
            return (Byte) key;
        } else if (key instanceof Integer) {
            return ((Integer) key).byteValue();
        } else if (key instanceof String) {
            return Byte.parseByte((String) key);
        } else {
            return 0;
        }
    }

    @Override
    protected void closure() {
        if (wasOpened) {
            super.closure();
            songSelector.closure();
        }
    }

    @Override
    protected AbstractDialog getNewInstance(JFrame frame) {
        return new DialogSongList(frame);
    }

    /**
	 * Creates a string containing the most important information about the
	 * {@link DialogSongList#instance main DialogToplist} object. This method is used only for
	 * debugging and testing purposes.
	 * 
	 * @return the created string.
	 */
    static String getString() {
        return "DialogNewGame(visible=" + instance.isVisible() + "; location=" + instance.getLocationOnScreen() + "; bounds=" + instance.getBounds() + ")";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by
	 * the {@link Connection#integrityCheck()} method; thus the application can only be altered if
	 * the source is known. Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "sdfDg!+%asÉ\tjfg ash";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class.
	 * Its only purpose is debugging or testing.
	 */
    static final Tester TESTER = new Tester("DialogNewGame", new String[] { "getString()" }) {

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("DialogNewGame");
            testEq("getIntegrityString()", "sdfDg!+%asÉ\tjfg ash", DialogSongList.getIntegrityString());
            higherTestEnd();
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the
	 * application in a normal way with the exception that it starts the debugging tool for this
	 * class as well; otherwise exits with an error message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }
}
