import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.io.File;
import java.net.URI;
import java.text.BreakIterator;

public class GUImain {

    Init app;

    public static final String OS = System.getProperty("os.name");

    public static UIManager.LookAndFeelInfo laf;

    JFrame framer = new JFrame(Init.VERSION_STRING);

    private static final Dimension FRAME_SIZE = new Dimension(1024, 768);

    private final String creditsString = "Copyright (C) 2007  Robert A. Rawls - University of Virginia Class of '09";

    private Vector<Card> theDeckVector;

    private boolean frontsAreShowing;

    private boolean activeCardIsFlipped;

    private int activeCardIndex;

    private Container c;

    private JPanel north = new JPanel();

    private JPanel central = new JPanel();

    private JPanel south = new JPanel();

    private JPanel filterAndSearchPanel = new JPanel();

    private JPanel filterPanel = new JPanel();

    private JPanel searchPanel = new JPanel();

    private JPanel deckPanel = new JPanel();

    private JPanel cardPanel = new JPanel();

    private JPanel innerCardPanel = new JPanel();

    private JMenuBar menu;

    public JFileChooser fc;

    private JMenuItem fileMenuGetLatestVersion;

    private JMenuItem fileMenuExit;

    private JMenuItem viewMenuTheme;

    private JMenuItem viewMenuGPL;

    private JMenuItem libraryMenuClear;

    private JMenuItem libraryMenuImport;

    private JMenuItem libraryMenuExport;

    private JMenuItem deckMenuExport;

    private JMenuItem deckMenuSort;

    private JMenuItem deckMenuShuffle;

    private JMenuItem deckMenuEdit;

    private JMenuItem deckMenuImport;

    private JMenuItem cardMenuNew;

    private JMenuItem cardMenuEdit;

    public JTextField searchTextField;

    public JTable filterTable;

    private JScrollPane filterTableScrollPane;

    private FilterTableModel filterTableModel;

    public JButton renameFilterButton;

    public JButton deleteFilterButton;

    private JLabel viewFrontOrBackLabel;

    private JRadioButton radioFront;

    private JRadioButton radioBack;

    private ButtonGroup radioGroup;

    public JList theDeck;

    private JScrollPane theDeckScrollPane;

    private JButton editDeckButton;

    private JButton deleteListButton;

    private JLabel wordLabel;

    private ImageIcon imageIndexCard;

    private JButton prevCardButton;

    private JButton nextCardButton;

    private JButton flipCardButton;

    private JLabel creditsLabel;

    private static int maxNumNewCards = 99;

    GUImain(Init app) {
        this.app = app;
        theDeckVector = new Vector<Card>(app.getLibrary());
        frontsAreShowing = true;
        activeCardIndex = 0;
        activeCardIsFlipped = false;
    }

    public void runGUI() {
        ImageIcon icon;
        try {
            framer.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            icon = Methods.createImageIcon("icon.gif", "icon");
            Image image = icon.getImage();
            framer.setIconImage(image);
        } catch (Exception e) {
        }
        framer.setSize(FRAME_SIZE);
        framer.addWindowListener(new LFramerCloser());
        c = framer.getContentPane();
        menu = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu viewMenu = new JMenu("View");
        JMenu libraryMenu = new JMenu("Library");
        JMenu deckMenu = new JMenu("Deck");
        JMenu cardMenu = new JMenu("Card");
        fc = new JFileChooser();
        {
            boolean latestVersion = Init.checkAppVersion();
            if (!latestVersion) {
                fileMenuGetLatestVersion = new JMenuItem("<html><u>" + (latestVersion ? "Get" : "Check for") + " Latest Version</u></html>");
                fileMenu.add(fileMenuGetLatestVersion);
                fileMenuGetLatestVersion.addActionListener(new LMenuFileGetLatestVersion(this));
                fileMenuGetLatestVersion.setCursor(new Cursor(Cursor.HAND_CURSOR));
                fileMenuGetLatestVersion.setForeground(Color.blue);
            }
            fileMenuExit = new JMenuItem("Exit");
            fileMenuExit.setFont(new Font("Tahoma", Font.BOLD, 11));
            fileMenuExit.addActionListener(new LMenuFileExit(this));
            fileMenu.add(fileMenuExit);
            menu.add(fileMenu);
            viewMenuTheme = new JMenuItem("Theme...");
            viewMenu.add(viewMenuTheme);
            viewMenuTheme.addActionListener(new LMenuViewTheme(this));
            viewMenuGPL = new JMenuItem("<html><u>GNU Public Licence</u></html>");
            viewMenuGPL.setForeground(Color.blue);
            viewMenu.add(viewMenuGPL);
            viewMenuGPL.addActionListener(new LMenuViewGPL(this));
            viewMenuGPL.setCursor(new Cursor(Cursor.HAND_CURSOR));
            menu.add(viewMenu);
            JMenuItem libraryMenuSave = new JMenuItem("Save");
            libraryMenu.add(libraryMenuSave);
            libraryMenuSave.addActionListener(new LMenuLibrarySave(this));
            libraryMenuImport = new JMenuItem("Import...");
            libraryMenu.add(libraryMenuImport);
            libraryMenuImport.addActionListener(new LMenuLibraryImport(this));
            libraryMenuExport = new JMenuItem("Export...");
            libraryMenu.add(libraryMenuExport);
            libraryMenuExport.addActionListener(new LMenuLibraryExport(this));
            libraryMenuClear = new JMenuItem("Clear");
            libraryMenu.add(libraryMenuClear);
            libraryMenuClear.addActionListener(new LMenuLibraryClear(this));
            menu.add(libraryMenu);
            deckMenuEdit = new JMenuItem("Edit...");
            deckMenu.add(deckMenuEdit);
            deckMenuEdit.addActionListener(new LMenuDeckEdit(this));
            deckMenuImport = new JMenuItem("Import...");
            deckMenu.add(deckMenuImport);
            deckMenuImport.addActionListener(new LMenuDeckImport(this));
            deckMenuExport = new JMenuItem("Export...");
            deckMenu.add(deckMenuExport);
            deckMenuExport.addActionListener(new LMenuDeckExport(this));
            deckMenuSort = new JMenuItem("Sort");
            deckMenu.add(deckMenuSort);
            deckMenuSort.addActionListener(new LMenuDeckSort(this));
            deckMenuShuffle = new JMenuItem("Shuffle");
            deckMenu.add(deckMenuShuffle);
            deckMenuShuffle.addActionListener(new LMenuDeckShuffle(this));
            menu.add(deckMenu);
            cardMenuNew = new JMenuItem("New...");
            cardMenu.add(cardMenuNew);
            cardMenuNew.addActionListener(new LMenuCardNew(this));
            cardMenuEdit = new JMenuItem("Edit...");
            cardMenu.add(cardMenuEdit);
            cardMenuEdit.addActionListener(new LMenuCardEdit(this));
            menu.add(cardMenu);
        }
        searchTextField = new JTextField(12);
        searchTextField.addKeyListener(new LKeySearch(this));
        filterTableModel = new FilterTableModel(this, app.getFilters(), true);
        filterTable = new JTable(filterTableModel);
        filterTable.addKeyListener(new LFilterTable(this));
        filterTable.addMouseListener(new LFilterTable(this));
        filterTable.setCellSelectionEnabled(false);
        filterTable.setRowSelectionAllowed(true);
        filterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterTable.setShowGrid(false);
        filterTable.setTableHeader(null);
        filterTable.setBackground(Color.white);
        filterTableScrollPane = new JScrollPane(filterTable);
        filterTableScrollPane.createHorizontalScrollBar();
        renameFilterButton = new JButton("Rename");
        renameFilterButton.addActionListener(new LButtonFilterRename(this));
        deleteFilterButton = new JButton("Delete");
        deleteFilterButton.addActionListener(new LButtonFilterDelete(this));
        viewFrontOrBackLabel = new JLabel("View:");
        radioFront = new JRadioButton("Fronts", true);
        radioFront.addActionListener(new LRadioFrontVisible(this));
        radioBack = new JRadioButton("Backs");
        radioBack.addActionListener(new LRadioBackVisible(this));
        radioGroup = new ButtonGroup();
        radioGroup.add(radioFront);
        radioGroup.add(radioBack);
        theDeck = new JList();
        theDeck.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        theDeckScrollPane = new JScrollPane();
        theDeck.addKeyListener(new LTheDeck(this));
        theDeck.addMouseListener(new LTheDeck(this));
        editDeckButton = new JButton("Edit");
        editDeckButton.addActionListener(new LButtonDeckEdit(this));
        deleteListButton = new JButton("Delete");
        deleteListButton.addActionListener(new LButtonDeckDelete(this));
        imageIndexCard = Methods.createImageIcon("index_card.jpg", "index_card");
        wordLabel = new JLabel(":-)", imageIndexCard, JLabel.CENTER);
        wordLabel.setVerticalTextPosition(JLabel.CENTER);
        wordLabel.setHorizontalTextPosition(JLabel.CENTER);
        wordLabel.setBorder(BorderFactory.createRaisedBevelBorder());
        prevCardButton = new JButton("<- Back");
        prevCardButton.addActionListener(new LButtonPrevCard(this));
        nextCardButton = new JButton("Next ->");
        nextCardButton.addActionListener(new LButtonNextCard(this));
        flipCardButton = new JButton("Flip!");
        flipCardButton.addActionListener(new LButtonFlipCard(this));
        creditsLabel = new JLabel(creditsString);
        creditsLabel.setFont(new Font("Tahoma", 0, 11));
        north.setLayout(new BorderLayout());
        north.add(menu);
        searchPanel.setLayout(new FlowLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Library "));
        searchPanel.setPreferredSize(new Dimension(200, 60));
        searchPanel.add(searchTextField);
        filterPanel.setLayout(new BorderLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter Library"));
        filterPanel.setPreferredSize(new Dimension(200, 1));
        JPanel rdp = new JPanel(new FlowLayout());
        rdp.add(renameFilterButton);
        rdp.add(deleteFilterButton);
        filterPanel.add(filterTableScrollPane, BorderLayout.CENTER);
        filterPanel.add(rdp, BorderLayout.SOUTH);
        filterAndSearchPanel = new JPanel();
        filterAndSearchPanel.add(searchPanel);
        filterAndSearchPanel.add(filterPanel);
        filterAndSearchPanel.setLayout(new BoxLayout(filterAndSearchPanel, BoxLayout.Y_AXIS));
        deckPanel.setLayout(new BorderLayout());
        deckPanel.setBorder(BorderFactory.createTitledBorder("The Deck"));
        deckPanel.setPreferredSize(new Dimension(200, 1));
        JPanel deckPanelNorth = new JPanel(new FlowLayout());
        JPanel lpn = new JPanel(new FlowLayout());
        JPanel lpnStack = new JPanel(new BorderLayout());
        lpnStack.add(radioFront, BorderLayout.NORTH);
        lpnStack.add(radioBack, BorderLayout.SOUTH);
        lpn.add(viewFrontOrBackLabel);
        lpn.add(lpnStack);
        deckPanelNorth.add(lpn);
        deckPanel.add(deckPanelNorth, BorderLayout.NORTH);
        theDeckScrollPane = new JScrollPane(theDeck, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        deckPanel.add(theDeckScrollPane, BorderLayout.CENTER);
        JPanel deckPanelSouth = new JPanel(new FlowLayout());
        deckPanelSouth.add(editDeckButton);
        deckPanelSouth.add(deleteListButton);
        deckPanel.add(deckPanelSouth, BorderLayout.SOUTH);
        cardPanel.setLayout(new FlowLayout());
        cardPanel.setPreferredSize(new Dimension(500, 360));
        innerCardPanel = new JPanel(new BorderLayout());
        innerCardPanel.setPreferredSize(new Dimension(500, 355));
        innerCardPanel.setBorder(BorderFactory.createTitledBorder("Current Card"));
        JPanel idpCenter = new JPanel();
        idpCenter.setLayout(new BorderLayout());
        idpCenter.setBackground(Color.white);
        idpCenter.add(wordLabel, BorderLayout.CENTER);
        JPanel idpSouth = new JPanel(new FlowLayout());
        idpSouth.add(prevCardButton);
        idpSouth.add(flipCardButton);
        idpSouth.add(nextCardButton);
        innerCardPanel.add(idpCenter, BorderLayout.CENTER);
        innerCardPanel.add(idpSouth, BorderLayout.SOUTH);
        cardPanel.add(innerCardPanel);
        south.add(creditsLabel);
        central.setLayout(new BoxLayout(central, BoxLayout.X_AXIS));
        central.add(filterAndSearchPanel);
        central.add(deckPanel);
        central.add(cardPanel);
        c.add(north, BorderLayout.NORTH);
        c.add(central, BorderLayout.CENTER);
        c.add(south, BorderLayout.SOUTH);
        initLookAndFeel();
        centerFrame();
        updateTheDeck();
        updateActiveCard();
        updateFilterTable();
        flipCardButton.grabFocus();
        framer.setVisible(true);
        if (app.splashed) {
            app.drySplash();
            framer.toFront();
        }
    }

    public void centerFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int frameWidth = framer.getWidth();
        int frameHeight = framer.getHeight();
        int x = (screenWidth / 2) - (frameWidth / 2);
        int y = (screenHeight / 2) - (frameHeight / 2);
        framer.setLocation(x, y);
    }

    public void updateTheDeck() {
        Vector<String> viewingVect = new Vector<String>();
        if (theDeckVector.isEmpty()) {
            deckPanel.setBorder(BorderFactory.createTitledBorder("The Deck: Empty!"));
        } else {
            deckPanel.setBorder(BorderFactory.createTitledBorder("The Deck: (" + theDeckVector.size() + " of " + app.getLibrary().size() + ")"));
            Iterator<Card> iter = theDeckVector.iterator();
            int count = 1;
            while (iter.hasNext()) {
                Card card = iter.next();
                if (frontsAreShowing) {
                    viewingVect.add(count++ + ") " + Methods.capFront(card.getFront()));
                } else {
                    viewingVect.add(count++ + ") " + Methods.capFront(card.getBack()));
                }
            }
        }
        theDeck.removeAll();
        theDeck.setListData(new Vector<String>());
        theDeck.setListData(viewingVect);
        theDeckScrollPane.setViewportView(theDeck);
        setActiveCardIndex(0);
        updateActiveCard();
        flipCardButton.grabFocus();
        updateStateChanges();
    }

    public void updateStateChanges() {
        boolean fullLib = !app.getLibrary().isEmpty();
        libraryMenuExport.setEnabled(fullLib);
        libraryMenuClear.setEnabled(fullLib);
        boolean fullDeck = !theDeckVector.isEmpty();
        deckMenuExport.setEnabled(fullDeck);
        deckMenuSort.setEnabled(fullDeck);
        deckMenuShuffle.setEnabled(fullDeck);
        deckMenuEdit.setEnabled(fullDeck);
        boolean cardThere = !app.getLibrary().isEmpty();
        cardMenuEdit.setEnabled(cardThere);
    }

    public void updateActiveCard() {
        String text;
        if (!theDeckVector.isEmpty()) {
            Card card = theDeckVector.get(activeCardIndex);
            if (frontsAreShowing) {
                if (!activeCardIsFlipped) text = Methods.capFront(card.getFront()); else text = Methods.capFront(card.getBack());
            } else {
                if (!activeCardIsFlipped) text = Methods.capFront(card.getBack()); else text = Methods.capFront(card.getFront());
            }
            innerCardPanel.setBorder(BorderFactory.createTitledBorder("Card " + (activeCardIndex + 1) + " of " + theDeckVector.size()));
            wrapAndSetLabelText(wordLabel, text);
            wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            wordLabel.setText("");
            innerCardPanel.setBorder(BorderFactory.createTitledBorder("No Card To Display!"));
        }
        if ((frontsAreShowing && !activeCardIsFlipped) || (!frontsAreShowing && activeCardIsFlipped)) {
            wordLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder()));
            wordLabel.setIcon(imageIndexCard);
            wordLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 48));
        } else {
            wordLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            wordLabel.setIcon(null);
            wordLabel.setFont(new Font("Comic Sans MS", Font.ITALIC, 48));
        }
    }

    public void nextCard() {
        if (activeCardIndex <= theDeckVector.size() - 1) {
            if (activeCardIndex == theDeckVector.size() - 1) {
                setActiveCardIndex(0);
            } else {
                activeCardIndex++;
            }
            updateActiveCard();
            theDeck.setSelectedIndex(activeCardIndex);
            flipCardButton.grabFocus();
        }
        unFlipActiveCard();
    }

    public void prevCard() {
        if (activeCardIndex > 0) {
            activeCardIndex--;
            updateActiveCard();
            theDeck.setSelectedIndex(activeCardIndex);
            flipCardButton.grabFocus();
        }
        unFlipActiveCard();
    }

    public void flipActiveCard() {
        activeCardIsFlipped = !activeCardIsFlipped;
        updateActiveCard();
        nextCardButton.grabFocus();
    }

    public void unFlipActiveCard() {
        activeCardIsFlipped = false;
        updateActiveCard();
    }

    public void clearTheDeck() {
        theDeckVector.clear();
        theDeck.removeAll();
        updateTheDeck();
    }

    public void clearLibrary() {
        Object[] options = { "Yes", "No!" };
        int answer = JOptionPane.showOptionDialog(framer, "Clearing library clears all cards and tags, Are you sure?", "Clear Library?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (answer == JOptionPane.YES_OPTION) {
            app.clearLibrary();
            clearTheDeck();
            updateFilterTable();
        }
    }

    public void exportLibrary() {
        if (app.getLibrary().isEmpty()) return;
        String path = "";
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter(null, Filer.FILE_EXT));
        int returnVal = fc.showSaveDialog(framer);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (Filer.fileExists(file)) {
                int answer = JOptionPane.showConfirmDialog(framer, "File Already Exists, Overwrite?", "File Exists", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) exportLibrary();
            }
            path = file.getAbsolutePath();
            app.exportLibrary(path);
        }
    }

    public void importLibrary() {
        String path = "";
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter(null, Filer.FILE_EXT));
        int returnVal = fc.showOpenDialog(framer);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            path = file.getAbsolutePath();
            app.importLibrary(path);
            filterLibrary();
            updateTheDeck();
            updateFilterTable();
            updateStateChanges();
        }
        if (path.isEmpty()) return;
    }

    public void importDecks(String[] paths) {
        if (paths.length < 1) return;
        app.importDecks(paths);
        filterLibrary();
        updateTheDeck();
        updateFilterTable();
        updateStateChanges();
    }

    public void setFrontVisible(boolean visible) {
        this.frontsAreShowing = visible;
        updateTheDeck();
        updateActiveCard();
        if (activeCardIsFlipped) flipActiveCard();
    }

    public void setDeck(ArrayList<Card> newDeck) {
        this.theDeckVector = new Vector<Card>(newDeck);
        updateTheDeck();
    }

    public int getActiveCardIndex() {
        return activeCardIndex;
    }

    public Card getActiveCard() {
        return theDeckVector.get(activeCardIndex);
    }

    public void setActiveCardIndex(int activeCardIndex) {
        this.activeCardIndex = activeCardIndex;
        if (activeCardIsFlipped) flipActiveCard();
        updateActiveCard();
        flipCardButton.grabFocus();
    }

    public boolean isFrontVisible() {
        return frontsAreShowing;
    }

    public Card removeFromTheDeck(int index) {
        return theDeckVector.remove(index);
    }

    public void removeSelectedCardsFromTheDeck() {
        ArrayList<Card> removed = new ArrayList<Card>();
        int[] indices = theDeck.getSelectedIndices();
        if (indices.length <= 0) return;
        Object[] options = { "Yes", "No!" };
        int remove = JOptionPane.showOptionDialog(framer, "Remove cards from active list?", "Removing Cards", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (remove == JOptionPane.YES_OPTION) {
            for (int i = indices.length - 1; i >= 0; i--) {
                int dex = indices[i];
                Card card = removeFromTheDeck(dex);
                removed.add(card);
            }
            setActiveCardIndex(0);
            updateTheDeck();
            int delete = JOptionPane.showOptionDialog(framer, "Permanently DELETE cards from Library as well?", "Deleting Cards...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (delete == JOptionPane.YES_OPTION) deleteCardsFromLibrary(removed);
        }
        theDeck.grabFocus();
    }

    public boolean deleteCardsFromLibrary(ArrayList<Card> cards) {
        boolean successful = app.deleteCards(cards);
        updateActiveCard();
        updateTheDeck();
        updateFilterTable();
        return successful;
    }

    public boolean addCardsToLibrary(ArrayList<Card> cards) {
        boolean successful = app.addCards(cards);
        updateStateChanges();
        updateFilterTable();
        return successful;
    }

    public void deleteSelectedFilter() {
        Tag filter = getSelectedFilter();
        if (filter == null) return;
        String heading = "Delete '" + filter + "'?";
        String prompt = "Deleting '" + filter + "' will remove the '" + filter + "' tag from all cards";
        Object[] options = { "Do it", "Never Mind!" };
        int answer = JOptionPane.showOptionDialog(framer, prompt, heading, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (answer == JOptionPane.OK_OPTION) {
            app.deleteFilter(filter);
            updateFilterTable();
            updateTheDeck();
        }
    }

    public void renameSelectedFilter(String name) {
        Tag filter = getSelectedFilter();
        app.renameFilter(filter, name);
        resetFilterTable();
        updateFilterTable();
    }

    public Card getCardFromTheDeck(int index) {
        return theDeckVector.get(index);
    }

    public void filterLibrary() {
        clearTheDeck();
        app.cleanFilters();
        ArrayList<Boolean> filterStates = getFilterStates();
        app.setFilterStates(filterStates);
        theDeckVector = new Vector<Card>(app.getFilteredLibrary());
        updateTheDeck();
    }

    public ArrayList<Card> getSelectedCards() {
        ArrayList<Card> cds = new ArrayList<Card>();
        int[] indices = this.theDeck.getSelectedIndices();
        for (int i = 0; i < indices.length; i++) {
            Card card = theDeckVector.get(indices[i]);
            cds.add(card);
        }
        return cds;
    }

    public void editSelectedCards() {
        ArrayList<Card> selectedCards = getSelectedCards();
        if (!selectedCards.isEmpty()) {
            GUIcardsEdit ecg = new GUIcardsEdit(this, getSelectedCards());
            ecg.framer.toFront();
            framer.setVisible(false);
        }
        sortTheDeck();
    }

    public void editActiveCard() {
        ArrayList<Card> card = new ArrayList<Card>(1);
        card.add(getActiveCard());
        GUIcardsEdit ecg = new GUIcardsEdit(this, card);
        ecg.framer.toFront();
        framer.setVisible(false);
    }

    public void editEntireDeck() {
        GUIcardsEdit ecg = new GUIcardsEdit(this, getDeck());
        ecg.framer.toFront();
        framer.setVisible(false);
    }

    public void exportDeck() {
        if (theDeckVector.isEmpty()) return;
        String path = "";
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter(null, Filer.FILE_EXT));
        int returnVal = fc.showSaveDialog(framer);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (Filer.fileExists(file)) {
                int answer = JOptionPane.showConfirmDialog(framer, "File Already Exists, Overwrite?", "File Exists", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) exportDeck();
            }
            path = file.getAbsolutePath();
            Filer.saveCardList(app, path, new ArrayList<Card>(theDeckVector));
        }
    }

    public void importDeck() {
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter(null, Filer.FILE_EXT));
        fc.setMultiSelectionEnabled(true);
        int returnVal = fc.showOpenDialog(framer);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            if (files.length < 1) return;
            String[] paths = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                paths[i] = files[i].getAbsolutePath();
            }
            importDecks(paths);
        }
        fc.setMultiSelectionEnabled(false);
    }

    public void newCards() {
        GUIcardsNew ncg = new GUIcardsNew(this, maxNumNewCards);
        ncg.framer.toFront();
        framer.setVisible(false);
    }

    public void exitProgram() {
        framer.dispose();
        app.saveLibrary();
        System.exit(0);
    }

    public ArrayList<Card> getDeck() {
        return new ArrayList<Card>(theDeckVector);
    }

    public Tag getSelectedFilter() {
        int index = filterTable.getSelectedRow();
        if (index < 0) return null;
        return app.getFilters().get(index);
    }

    public ArrayList<Boolean> getFilterStates() {
        FilterTableModel ftm = filterTableModel;
        int numFilters = ftm.getRowCount();
        ArrayList<Boolean> filterStates = new ArrayList<Boolean>(numFilters);
        if (numFilters < 1) return filterStates;
        for (int i = 0; i < numFilters; i++) {
            boolean state = (Boolean) ftm.getValueAt(i, 0);
            filterStates.add(state);
        }
        return filterStates;
    }

    public void sortTheDeckBy(Comparator<Card> c) {
        Collections.sort(theDeckVector, c);
        unFlipActiveCard();
        updateTheDeck();
    }

    @SuppressWarnings("unchecked")
    public void sortTheDeck() {
        if (isFrontVisible()) {
            sortTheDeckBy(new CmpFront());
        } else {
            sortTheDeckBy(new CmpBack());
        }
    }

    public void shuffleTheDeck() {
        Collections.shuffle(theDeckVector);
        updateTheDeck();
        setActiveCardIndex(0);
        unFlipActiveCard();
        updateActiveCard();
    }

    public void updateFilterTable() {
        app.cleanFilters();
        filterTableModel = new FilterTableModel(this, app.getFilters(), true);
        filterTableModel.addTableModelListener(new LTableModel(this));
        filterTableModel.populateFilterTable();
        filterTable.setModel(filterTableModel);
        TableColumnModel cm = filterTable.getColumnModel();
        TableColumn active = cm.getColumn(0);
        active.setMaxWidth(16);
    }

    public void resetFilterTable() {
        app.resetFilters();
        updateFilterTable();
    }

    public void renameFilter() {
        String name = "";
        Tag filter = getSelectedFilter();
        if (filter == null) return;
        while (!newFilterNameIsValid(name)) {
            name = JOptionPane.showInputDialog("Please enter new name for '" + filter + "': ");
            if (name == null) return;
        }
        renameSelectedFilter(name);
    }

    private boolean newFilterNameIsValid(String str) {
        if (str.length() < 1) return false;
        if (str.contains(":") || str.contains("[") || str.contains("]") || str.contains("|")) {
            Methods.displaySimpleError(framer, "Illegal Character!   : [ ] | ");
            return false;
        }
        if (app.filterAlreadyExists(str)) {
            Methods.displaySimpleError(framer, "Filter already exists...");
            return false;
        }
        return true;
    }

    public void cancelSearch() {
        resetFilterTable();
        filterLibrary();
    }

    public void initLookAndFeel() {
        try {
            UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
            int osIndex = 0;
            Object[] UINames = new Object[feels.length];
            for (int i = 0; i < feels.length; i++) {
                String name = feels[i].getName();
                if (UIManager.getSystemLookAndFeelClassName().contains(name)) {
                    osIndex = i;
                } else if (UIManager.getSystemLookAndFeelClassName().contains("GTK") && name.contains("GTK")) {
                    osIndex = i;
                }
                UINames[i] = name;
            }
            UIManager.LookAndFeelInfo ui = feels[osIndex];
            laf = ui;
            String uiClassName = ui.getClassName().toString();
            UIManager.setLookAndFeel(uiClassName);
            SwingUtilities.updateComponentTreeUI(framer);
            framer.pack();
        } catch (Exception e) {
        }
    }

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (Exception x) {
        }
    }

    public void chooseTheme() {
        String OS = GUImain.OS;
        UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        int currentIndex = 0;
        Object[] UINames = new Object[feels.length];
        for (int i = 0; i < feels.length; i++) {
            UIManager.LookAndFeelInfo feel = feels[i];
            String name = feel.getName();
            if (OS.contains(name)) {
                name = OS;
            } else if (feel.getName().contains("Metal")) {
                name = "Java";
            }
            if (laf.getName().equals(feel.getName())) {
                currentIndex = i;
            }
            UINames[i] = name;
        }
        Object selectedValue = JOptionPane.showInputDialog(framer, "Pick a Theme", "Theme Chooser", JOptionPane.INFORMATION_MESSAGE, null, UINames, UINames[currentIndex]);
        if (selectedValue == null) return;
        String selStr = ((String) selectedValue);
        UIManager.LookAndFeelInfo ui = null;
        for (int i = 0; i < feels.length; i++) {
            if (selStr.equals(UINames[i])) ui = feels[i];
        }
        laf = ui;
        try {
            UIManager.setLookAndFeel(ui.getClassName());
            SwingUtilities.updateComponentTreeUI(this.framer);
        } catch (Exception x) {
        }
    }

    private void wrapAndSetLabelText(JLabel label, String text) {
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Container container = label.getParent();
        int containerWidth = container.getWidth();
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(text);
        StringBuffer trial = new StringBuffer();
        StringBuffer real = new StringBuffer("<html><center>");
        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            String word = text.substring(start, end);
            trial.append(word);
            int trialWidth = SwingUtilities.computeStringWidth(fm, trial.toString());
            if (trialWidth > containerWidth) {
                trial = new StringBuffer(word);
                real.append("<br>");
            }
            real.append(word);
        }
        real.append("</center></html>");
        label.setText(real.toString());
    }

    public String getSearchString() {
        return searchTextField.getText().trim();
    }

    class LFramerCloser implements WindowListener {

        public void windowActivated(WindowEvent arg0) {
        }

        public void windowClosed(WindowEvent arg0) {
            exitProgram();
        }

        public void windowClosing(WindowEvent arg0) {
            framer.dispose();
        }

        public void windowDeactivated(WindowEvent arg0) {
        }

        public void windowDeiconified(WindowEvent arg0) {
        }

        public void windowIconified(WindowEvent arg0) {
        }

        public void windowOpened(WindowEvent arg0) {
        }
    }

    class LMenuFileExit implements ActionListener {

        public LMenuFileExit(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            exitProgram();
        }
    }

    class LMenuFileGetLatestVersion implements ActionListener {

        public LMenuFileGetLatestVersion(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent ae) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop d = Desktop.getDesktop();
                    d.browse(new URI(Init.SITE_URL));
                } catch (Exception e) {
                    Methods.displaySimpleError(framer, "Can't access website; check internet connection?\nPlease visit '" + Init.GPL_URL + "' to view GPL");
                }
            } else {
                Methods.displaySimpleAlert(framer, "Please visit '" + Init.SITE_URL + "' to check latest version");
            }
        }
    }

    class LMenuViewTheme implements ActionListener {

        public LMenuViewTheme(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            chooseTheme();
        }
    }

    class LMenuViewGPL implements ActionListener {

        public LMenuViewGPL(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent ae) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop d = Desktop.getDesktop();
                    d.browse(new URI(Init.GPL_URL));
                } catch (Exception e) {
                    Methods.displaySimpleError(framer, "Can't access website; check internet connection?\nPlease visit '" + Init.GPL_URL + "' to view GPL");
                }
            } else {
                Methods.displaySimpleAlert(framer, "Please visit '" + Init.GPL_URL + "' to view GPL");
            }
        }
    }

    class LMenuLibraryClear implements ActionListener {

        public LMenuLibraryClear(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            clearLibrary();
        }
    }

    class LMenuLibraryExport implements ActionListener {

        public LMenuLibraryExport(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            exportLibrary();
        }
    }

    class LMenuLibraryImport implements ActionListener {

        public LMenuLibraryImport(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            importLibrary();
        }
    }

    class LMenuLibrarySave implements ActionListener {

        public LMenuLibrarySave(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            app.saveLibrary();
        }
    }

    class LMenuDeckEdit implements ActionListener {

        public LMenuDeckEdit(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            editEntireDeck();
        }
    }

    class LMenuDeckExport implements ActionListener {

        public LMenuDeckExport(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            exportDeck();
        }
    }

    class LMenuDeckImport implements ActionListener {

        public LMenuDeckImport(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            importDeck();
        }
    }

    class LMenuDeckShuffle implements ActionListener {

        public LMenuDeckShuffle(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            shuffleTheDeck();
        }
    }

    class LMenuDeckSort implements ActionListener {

        public LMenuDeckSort(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            sortTheDeck();
        }
    }

    class LMenuCardEdit implements ActionListener {

        public LMenuCardEdit(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            editActiveCard();
        }
    }

    class LMenuCardNew implements ActionListener {

        public LMenuCardNew(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            newCards();
        }
    }

    class LButtonDeckDelete implements ActionListener {

        public LButtonDeckDelete(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            removeSelectedCardsFromTheDeck();
        }
    }

    class LButtonDeckEdit implements ActionListener {

        public LButtonDeckEdit(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            editSelectedCards();
        }
    }

    class LButtonFilterDelete implements ActionListener {

        public LButtonFilterDelete(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelectedFilter();
        }
    }

    class LButtonFilterRename implements ActionListener {

        public LButtonFilterRename(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            renameFilter();
        }
    }

    class LButtonFlipCard implements ActionListener {

        public LButtonFlipCard(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            flipActiveCard();
        }
    }

    class LButtonNextCard implements ActionListener {

        public LButtonNextCard(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            nextCard();
        }
    }

    class LButtonPrevCard implements ActionListener {

        public LButtonPrevCard(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            prevCard();
        }
    }

    class LButtonSearchCancel implements ActionListener {

        public LButtonSearchCancel(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            cancelSearch();
        }
    }

    class LRadioFrontVisible implements ActionListener {

        public LRadioFrontVisible(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            setFrontVisible(true);
        }
    }

    class LRadioBackVisible implements ActionListener {

        public LRadioBackVisible(GUImain guiFrame) {
        }

        public void actionPerformed(ActionEvent e) {
            setFrontVisible(false);
        }
    }

    class LKeySearch implements KeyListener {

        public LKeySearch(GUImain guiFrame) {
        }

        public void keyPressed(KeyEvent arg0) {
        }

        public void keyTyped(KeyEvent arg0) {
        }

        public void keyReleased(KeyEvent arg0) {
            if (KeyEvent.getKeyText(arg0.getKeyCode()).equalsIgnoreCase("enter")) {
                String searchString = getSearchString();
                if (searchString == null) return;
                resetFilterTable();
                setDeck(app.searchLibrary(searchString));
                searchTextField.setText("");
                searchTextField.grabFocus();
            } else if (KeyEvent.getKeyText(arg0.getKeyCode()).equalsIgnoreCase("escape")) {
                cancelSearch();
            }
        }
    }

    class LTableModel implements TableModelListener {

        public LTableModel(GUImain guiFrame) {
        }

        public void tableChanged(TableModelEvent e) {
            filterLibrary();
        }
    }

    class LTheDeck implements KeyListener, MouseListener {

        public LTheDeck(GUImain guiFrame) {
        }

        public void keyPressed(KeyEvent evt) {
        }

        public void keyTyped(KeyEvent evt) {
        }

        public void keyReleased(KeyEvent evt) {
            if (KeyEvent.getKeyText(evt.getKeyCode()).equalsIgnoreCase("delete")) {
                removeSelectedCardsFromTheDeck();
            } else if (KeyEvent.getKeyText(evt.getKeyCode()).equalsIgnoreCase("enter")) {
                int dex = theDeck.getSelectedIndex();
                setActiveCardIndex(dex);
            }
        }

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                int dex = theDeck.locationToIndex(evt.getPoint());
                setActiveCardIndex(dex);
            }
        }

        public void mouseEntered(MouseEvent evt) {
        }

        public void mouseExited(MouseEvent evt) {
        }

        public void mousePressed(MouseEvent evt) {
        }

        public void mouseReleased(MouseEvent evt) {
        }
    }

    class LFilterTable implements KeyListener, MouseListener {

        public LFilterTable(GUImain guiFrame) {
        }

        public void keyPressed(KeyEvent evt) {
        }

        public void keyTyped(KeyEvent evt) {
        }

        public void keyReleased(KeyEvent evt) {
            if (KeyEvent.getKeyText(evt.getKeyCode()).equalsIgnoreCase("delete")) {
                deleteSelectedFilter();
            } else if (KeyEvent.getKeyText(evt.getKeyCode()).equalsIgnoreCase("enter")) {
                int dex = filterTable.getSelectedRow() - 1;
                TableModel tm = filterTable.getModel();
                tm.setValueAt(!(Boolean) (tm.getValueAt(dex, 0)), dex, 0);
            }
        }

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                int dex = filterTable.getSelectedRow();
                TableModel tm = filterTable.getModel();
                tm.setValueAt(!(Boolean) (tm.getValueAt(dex, 0)), dex, 0);
            }
        }

        public void mouseEntered(MouseEvent evt) {
        }

        public void mouseExited(MouseEvent evt) {
        }

        public void mousePressed(MouseEvent evt) {
        }

        public void mouseReleased(MouseEvent evt) {
        }
    }
}
