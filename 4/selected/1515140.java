package pcgen.gui.tabs;

import pcgen.core.*;
import pcgen.core.character.Follower;
import pcgen.core.utils.MessageType;
import pcgen.core.utils.ShowMessageDelegate;
import pcgen.gui.CharacterInfo;
import pcgen.gui.CharacterInfoTab;
import pcgen.gui.PCGen_Frame1;
import pcgen.gui.filter.FilterAdapterPanel;
import pcgen.gui.filter.FilterConstants;
import pcgen.gui.filter.FilterFactory;
import pcgen.gui.panes.FlippingSplitPane;
import pcgen.gui.tabs.resources.AvailableFollowerModel;
import pcgen.gui.tabs.resources.SelectedFollowerModel;
import pcgen.gui.utils.*;
import pcgen.io.PCGIOHandler;
import pcgen.io.PCGFile;
import pcgen.util.Logging;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import pcgen.util.PropertyFactory;

/**
 *  <code>InfoResources</code> creates a new tabbed panel that is used to
 *  allow creating/adding familiars, cohorts, companions, intelligent items
 *  vehicles and buildings
 *
 * @author Jayme Cox <jaymecox@users.sourceforge.net>
 * @version  $Revision: 1.67 $
 *
 **/
public class InfoResources extends FilterAdapterPanel implements CharacterInfoTab {

    static final long serialVersionUID = 7236403406005940947L;

    private static boolean needsUpdate = true;

    private FlippingSplitPane botSplit = new FlippingSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private FlippingSplitPane centerSplit = new FlippingSplitPane(JSplitPane.VERTICAL_SPLIT);

    private FlippingSplitPane topSplit = new FlippingSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    private AvailableFollowerModel availableModel = null;

    private SelectedFollowerModel selectedModel = null;

    private JButton addButton = new JButton();

    private JButton addModButton = new JButton();

    private JButton delButton = new JButton();

    private JButton delModButton = new JButton();

    private JButton loadButton = new JButton();

    private JButton updateButton = new JButton();

    private JCheckBox shouldLoadCompanion = new JCheckBox("Auto load companions");

    private final JLabel modeLabel = new JLabel("Select Type of Resource:");

    private JComboBoxEx viewModeBox = new JComboBoxEx();

    private final JLabel sortLabel = new JLabel("Sort");

    private JComboBoxEx viewSortBox = new JComboBoxEx();

    private JLabelPane followerInfo = new JLabelPane();

    private JLabelPane infoLabel = new JLabelPane();

    private JPanel botPane = new JPanel();

    private JPanel followerPane = new JPanel();

    private JPanel masterPane = new JPanel();

    private JPanel modePane = new JPanel();

    private JPanel topPane = new JPanel();

    private JTreeTable availableTable;

    private JTreeTable selectedTable;

    private JTreeTableSorter availableSort = null;

    private JTreeTableSorter selectedSort = null;

    private TreePath selPath;

    private boolean hasBeenSized = false;

    private int viewMode = 0;

    private int viewSortMode = 0;

    PlayerCharacter pc;

    private int serial = 0;

    private boolean readyForRefresh = false;

    /**
	 *  Constructor for the InfoEquips object
	 * @param pc
	 **/
    public InfoResources(PlayerCharacter pc) {
        this.pc = pc;
        setName(Constants.tabNames[Constants.TAB_RESOURCES]);
        initComponents();
        initActionListeners();
    }

    public void setPc(PlayerCharacter pc) {
        if (this.pc != pc || pc.getSerial() > serial) {
            this.pc = pc;
            serial = pc.getSerial();
            availableModel.setCharacter(pc);
            selectedModel.setCharacter(pc);
            forceRefresh();
        }
    }

    public PlayerCharacter getPc() {
        return pc;
    }

    public int getTabOrder() {
        return SettingsHandler.getPCGenOption(".Panel.Resources.Order", Constants.TAB_RESOURCES);
    }

    public void setTabOrder(int order) {
        SettingsHandler.setPCGenOption(".Panel.Resources.Order", order);
    }

    public String getTabName() {
        GameMode game = SettingsHandler.getGame();
        return game.getTabName(Constants.TAB_RESOURCES);
    }

    public boolean isShown() {
        GameMode game = SettingsHandler.getGame();
        return game.getTabShown(Constants.TAB_RESOURCES);
    }

    /**
	 * Retrieve the list of tasks to be done on the tab.
	 * @return List of task descriptions as Strings.
	 */
    public List getToDos() {
        List toDoList = new ArrayList();
        return toDoList;
    }

    public void refresh() {
        if (pc.getSerial() > serial) {
            serial = pc.getSerial();
            forceRefresh();
        }
    }

    public void forceRefresh() {
        if (readyForRefresh) {
            needsUpdate = true;
            updateCharacterInfo();
        } else {
            serial = 0;
        }
    }

    public JComponent getView() {
        return this;
    }

    /**
	 * specifies whether the "match any" option should be available
	 * @return true
	 **/
    public final boolean isMatchAnyEnabled() {
        return true;
    }

    /**
	 * Sets the update flag for this tab
	 * It's a lazy update and will only occur
	 * on other status change
	 * @param flag
	 **/
    public static void setNeedsUpdate(boolean flag) {
        needsUpdate = flag;
    }

    /**
	 * specifies whether the "negate/reverse" option should be available
	 * @return true
	 **/
    public final boolean isNegateEnabled() {
        return true;
    }

    /**
	 * specifies the filter selection mode
	 * @return FilterConstants.DISABLED_MODE = -2
	 **/
    public final int getSelectionMode() {
        return FilterConstants.DISABLED_MODE;
    }

    /**
	 * implementation of Filterable interface
	 **/
    public final void initializeFilters() {
        FilterFactory.registerAllSourceFilters(this);
    }

    /**
	 * implementation of Filterable interface
	 **/
    public final void refreshFiltering() {
        updateAvailableModel();
        updateSelectedModel();
    }

    /**
	 * This recalculates the states of everything based
	 * upon the currently selected character.
	 */
    public final void updateCharacterInfo() {
        if ((pc == null) || !needsUpdate) {
            return;
        }
        shouldLoadCompanion.setSelected(pc.getLoadCompanion());
        if (pc.getMaster() != null) {
            setFollowerInfo(pc);
            this.remove(masterPane);
            this.add(followerPane, BorderLayout.CENTER);
            followerPane.setVisible(true);
            masterPane.setVisible(false);
            followerPane.updateUI();
            this.updateUI();
        } else {
            updateAvailableModel();
            updateSelectedModel();
            this.remove(followerPane);
            this.add(masterPane, BorderLayout.CENTER);
            masterPane.setVisible(true);
            followerPane.setVisible(false);
            followerPane.updateUI();
            masterPane.updateUI();
            this.updateUI();
        }
        needsUpdate = false;
    }

    /**
	 * set the Follower Info text in the FollowerInfo panel
	 * @param obj
	 **/
    private void setFollowerInfo(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof PlayerCharacter) {
            Follower aF = pc.getMaster();
            PlayerCharacter mPC = null;
            for (Iterator p = Globals.getPCList().iterator(); p.hasNext(); ) {
                PlayerCharacter nPC = (PlayerCharacter) p.next();
                if (aF.getFileName().equals(nPC.getFileName())) {
                    mPC = nPC;
                }
            }
            if (mPC == null) {
                followerInfo.setText("NOTICE: Load Master from File to display info");
                return;
            }
            StringBuffer b = new StringBuffer();
            b.append("<html>");
            b.append(followerStatBlock(aF, pc));
            b.append("<p>");
            b.append("<font size=+1><b>Master Information</b></font><br>");
            b.append("<b>PC Name:</b> ").append(mPC.getName());
            b.append("<br>");
            b.append("<b>File:</b> ").append(mPC.getFileName());
            b.append("<br>");
            b.append("<b>Race:</b> ").append(mPC.getRace());
            b.append("<br>");
            b.append("<b>").append(Globals.getGameModeHPAbbrev()).append("</b>: ").append(mPC.hitPoints());
            b.append("<br>");
            int bonus = mPC.baseAttackBonus();
            b.append("<b>BAB</b>: ").append((bonus >= 0) ? "+" : "").append(bonus);
            b.append("<br>");
            b.append("</html>");
            followerInfo.setText(b.toString());
            followerInfo.setVisible(true);
            followerInfo.repaint();
        }
    }

    private void addButton() {
        if ("".equals(pc.getFileName())) {
            ShowMessageDelegate.showMessageDialog("You must save the current character first", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        TreePath avaCPath = availableTable.getTree().getSelectionPath();
        TreePath selCPath = selectedTable.getTree().getSelectionPath();
        String target;
        if (selCPath == null) {
            ShowMessageDelegate.showMessageDialog("First select destination", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        target = selCPath.getPathComponent(1).toString();
        Object endComp = avaCPath.getLastPathComponent();
        PObjectNode fNode = (PObjectNode) endComp;
        if ((fNode.getItem() instanceof Race)) {
            Race aRace = (Race) fNode.getItem();
            if (aRace == null) {
                return;
            }
            String nName;
            String aType;
            Logging.errorPrint("addButton:race: " + aRace.getName() + " -> " + target);
            Object nValue = JOptionPane.showInputDialog(null, "Please enter a name for new " + target + ":", Constants.s_APPNAME, JOptionPane.QUESTION_MESSAGE);
            if (nValue != null) {
                nName = ((String) nValue).trim();
            } else {
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Save new " + target + " named: " + nName);
            fc.setSelectedFile(new File(SettingsHandler.getPcgPath(), nName + Constants.s_PCGEN_CHARACTER_EXTENSION));
            fc.setCurrentDirectory(SettingsHandler.getPcgPath());
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = fc.getSelectedFile();
            if (!PCGFile.isPCGenCharacterFile(file)) {
                file = new File(file.getParent(), file.getName() + Constants.s_PCGEN_CHARACTER_EXTENSION);
            }
            if (file.exists()) {
                int iConfirm = JOptionPane.showConfirmDialog(null, "The file " + file.getName() + " already exists. Are you sure you want to overwrite it?", "Confirm OverWrite", JOptionPane.YES_NO_OPTION);
                if (iConfirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            PlayerCharacter newPC = new PlayerCharacter();
            newPC.setName(nName);
            newPC.setFileName(file.getAbsolutePath());
            for (Iterator i = newPC.getStatList().getStats().iterator(); i.hasNext(); ) {
                final PCStat aStat = (PCStat) i.next();
                aStat.setBaseScore(10);
            }
            newPC.setAlignment(pc.getAlignment(), true, true);
            newPC.setRace(aRace);
            if (newPC.getRace().hitDice(pc) != 0) {
                newPC.getRace().rollHP(pc);
            }
            newPC.setDirty(true);
            aType = target;
            Follower newMaster = new Follower(pc.getFileName(), pc.getName(), aType);
            newPC.setMaster(newMaster);
            Follower newFollower = new Follower(file.getAbsolutePath(), nName, aType);
            newFollower.setRace(newPC.getRace().getName());
            pc.addFollower(newFollower);
            pc.setDirty(true);
            pc.setCalcFollowerBonus(pc);
            pc.setAggregateFeatsStable(false);
            pc.setVirtualFeatsStable(false);
            ShowMessageDelegate.showMessageDialog("Saving " + nName + " and switching tabs", Constants.s_APPNAME, MessageType.INFORMATION);
            try {
                (new PCGIOHandler()).write(newPC, file.getAbsolutePath());
            } catch (Exception ex) {
                ShowMessageDelegate.showMessageDialog("Could not save " + newPC.getDisplayName(), Constants.s_APPNAME, MessageType.ERROR);
                Logging.errorPrint("Could not save " + newPC.getDisplayName(), ex);
                return;
            }
            setNeedsUpdate(true);
            pc.calcActiveBonuses();
            PlayerCharacter loadedChar = PCGen_Frame1.getInst().loadPCFromFile(file);
            loadedChar.calcActiveBonuses();
            CharacterInfo pane = PCGen_Frame1.getCharacterPane();
            pane.setPaneForUpdate(pane.infoSummary());
            pane.refresh();
        } else if ((fNode.getItem() instanceof Equipment)) {
            Equipment eqI = (Equipment) fNode.getItem();
            if (eqI == null) {
                return;
            }
            Logging.errorPrint("addButton:item: " + eqI.getName() + " -> " + target);
            pc.setDirty(true);
            updateSelectedModel();
        }
    }

    private void addFileButton() {
        if ("".equals(pc.getFileName())) {
            ShowMessageDelegate.showMessageDialog("You must save the current character first", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        TreePath selCPath = selectedTable.getTree().getSelectionPath();
        String target;
        if (selCPath == null) {
            ShowMessageDelegate.showMessageDialog("First select destination", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        target = selCPath.getPathComponent(1).toString();
        String aType;
        File file = null;
        file = findPCGFile(file);
        if ((file == null) || !file.exists()) {
            return;
        }
        PlayerCharacter newPC = null;
        PlayerCharacter oldPC = pc;
        int oldIndex = PCGen_Frame1.getBaseTabbedPane().getSelectedIndex();
        int newIndex = PCGen_Frame1.FIRST_CHAR_TAB;
        for (Iterator i = Globals.getPCList().iterator(); i.hasNext(); ) {
            PlayerCharacter iPC = (PlayerCharacter) i.next();
            if (iPC.getFileName().equals(file.toString())) {
                Logging.errorPrint("already open");
                PCGen_Frame1.getBaseTabbedPane().setSelectedIndex(newIndex);
                newPC = iPC;
                break;
            }
            newIndex++;
        }
        if (newPC == null) {
            newPC = PCGen_Frame1.getInst().loadPCFromFile(file);
            if (newPC == null) {
                Logging.errorPrint("Unable to load " + file.toString());
                Globals.setCurrentPC(oldPC);
                return;
            }
        }
        aType = target;
        Follower newMaster = new Follower(oldPC.getFileName(), oldPC.getName(), aType);
        newPC.setMaster(newMaster);
        Follower newFollower = new Follower(file.getAbsolutePath(), newPC.getName(), aType);
        newFollower.setRace(newPC.getRace().getName());
        oldPC.addFollower(newFollower);
        oldPC.setDirty(true);
        newPC.setDirty(true);
        PCGen_Frame1.getInst().savePC(oldPC, false);
        PCGen_Frame1.getInst().savePC(newPC, false);
        PCGen_Frame1.getBaseTabbedPane().setSelectedIndex(oldIndex);
        PCGen_Frame1.getInst().revertToSavedItem_actionPerformed(null);
        setNeedsUpdate(true);
        CharacterInfo pane = PCGen_Frame1.getCharacterPane();
        pane.setPaneForUpdate(pane.infoInventory());
        pane.refresh();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                formComponentShown();
            }
        });
    }

    private final void createAvailableModel() {
        if (availableModel == null) {
            availableModel = new AvailableFollowerModel(pc, viewSortMode);
        } else {
            availableModel.resetModel(viewSortMode);
        }
        if (availableSort != null) {
            availableSort.setRoot((PObjectNode) availableModel.getRoot());
        }
    }

    /**
	 * This creates the GUI pane that a "follower" sees
	 * displays the followers stats and let's them update from master
	 **/
    private void createFollowerView() {
        followerPane.setLayout(new BorderLayout());
        JPanel aPanel = new JPanel();
        aPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
        updateButton.setText("Update from Master");
        updateButton.setEnabled(true);
        aPanel.add(updateButton);
        followerPane.add(aPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(followerInfo);
        TitledBorder sTitle = BorderFactory.createTitledBorder("Follower Information");
        sTitle.setTitleJustification(TitledBorder.CENTER);
        scrollPane.setBorder(sTitle);
        followerInfo.setBackground(topPane.getBackground());
        scrollPane.setViewportView(followerInfo);
        scrollPane.setVisible(true);
        followerPane.add(scrollPane, BorderLayout.CENTER);
        followerPane.setVisible(true);
    }

    /**
	 * This creates the GUI pane that a "master" sees to allow the PC
	 * to add new followers, familiars, artifacts, etc
	 **/
    private void createMasterView() {
        masterPane.setLayout(new BorderLayout());
        masterPane.setBorder(BorderFactory.createEtchedBorder());
        topPane.setLayout(new BorderLayout());
        JPanel leftPane = new JPanel();
        JPanel rightPane = new JPanel();
        leftPane.setLayout(new BorderLayout());
        rightPane.setLayout(new BorderLayout());
        topSplit.setLeftComponent(leftPane);
        topSplit.setRightComponent(rightPane);
        topSplit.setOneTouchExpandable(true);
        topSplit.setDividerSize(10);
        topSplit.setBorder(BorderFactory.createEtchedBorder());
        JPanel aPanel = new JPanel();
        JPanel alPanel = new JPanel();
        aPanel.setLayout(new BorderLayout(4, 0));
        alPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        shouldLoadCompanion.setSelected(pc.getLoadCompanion());
        aPanel.add(shouldLoadCompanion, BorderLayout.WEST);
        alPanel.add(sortLabel);
        alPanel.add(viewSortBox);
        ImageIcon newImage;
        newImage = IconUtilitities.getImageIcon("Forward16.gif");
        addButton.setIcon(newImage);
        Utility.setDescription(addButton, "Click to add");
        addButton.setEnabled(false);
        alPanel.add(addButton);
        aPanel.add(alPanel, BorderLayout.CENTER);
        leftPane.add(aPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(availableTable);
        Utility.setDescription(scrollPane, "Right click to add");
        leftPane.add(scrollPane, BorderLayout.CENTER);
        availableTable.setColAlign(1, SwingConstants.CENTER);
        availableTable.setColAlign(4, SwingConstants.CENTER);
        aPanel = new JPanel();
        aPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        newImage = IconUtilitities.getImageIcon("Back16.gif");
        delButton.setIcon(newImage);
        Utility.setDescription(delButton, "Click to remove from List");
        delButton.setEnabled(false);
        aPanel.add(delButton);
        loadButton.setText("Load...");
        loadButton.setEnabled(false);
        aPanel.add(loadButton);
        rightPane.add(aPanel, BorderLayout.NORTH);
        scrollPane = new JScrollPane(selectedTable);
        Utility.setDescription(scrollPane, "Right click to remove");
        rightPane.add(scrollPane, BorderLayout.CENTER);
        topPane.add(topSplit, BorderLayout.CENTER);
        botPane.setLayout(new BorderLayout());
        JScrollPane bLeftPane = new JScrollPane(infoLabel);
        JPanel bRightPane = new JPanel();
        botSplit.setLeftComponent(bLeftPane);
        botSplit.setRightComponent(bRightPane);
        botSplit.setOneTouchExpandable(true);
        botSplit.setDividerSize(10);
        botPane.add(botSplit, BorderLayout.CENTER);
        TitledBorder sTitle = BorderFactory.createTitledBorder("Information");
        sTitle.setTitleJustification(TitledBorder.CENTER);
        bLeftPane.setBorder(sTitle);
        infoLabel.setBackground(topPane.getBackground());
        bLeftPane.setViewportView(infoLabel);
        JPanel iPanel = new JPanel();
        iPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        addModButton.setText("Add Modifier");
        addModButton.setEnabled(false);
        iPanel.add(addModButton);
        delModButton.setText("Delete Modifier");
        delModButton.setEnabled(false);
        iPanel.add(delModButton);
        bRightPane.add(iPanel);
        centerSplit.setTopComponent(topPane);
        centerSplit.setBottomComponent(botPane);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setDividerSize(10);
        modePane.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        modePane.add(modeLabel, null);
        modePane.add(viewModeBox, null);
        masterPane.add(modePane, BorderLayout.NORTH);
        masterPane.add(centerSplit, BorderLayout.CENTER);
    }

    /**
	 * Given a Follower and PC, return string of vital stats
	 * @param aF
	 * @param newPC
	 * @return follower stat block
	 **/
    private static String followerStatBlock(Follower aF, PlayerCharacter newPC) {
        StringBuffer b = new StringBuffer();
        b.append("<font size=+1><b>Name:</b> ");
        b.append(newPC.getName()).append("</font>");
        b.append("<br>");
        b.append("<b>Type:</b> ").append(aF.getType());
        b.append("<br>");
        b.append("<b>Race:</b> ").append(newPC.getRace());
        b.append("<br>");
        for (int i = 0; i < SettingsHandler.getGame().s_ATTRIBSHORT.length; i++) {
            b.append("<b>").append(SettingsHandler.getGame().s_ATTRIBSHORT[i]).append("</b>: ").append(newPC.getStatList().getTotalStatFor(SettingsHandler.getGame().s_ATTRIBSHORT[i])).append(" ");
        }
        b.append("<br>");
        if (Globals.getGameModeACText().length() != 0) {
            b.append("<b>").append(Globals.getGameModeACText()).append("</b> ");
            b.append("<i>Total</i>: ").append(newPC.getACTotal());
            b.append(" <i>Flatfooted</i>: ").append(newPC.flatfootedAC());
            b.append(" <i>Touch</i>: ").append(newPC.touchAC());
            b.append("<br>");
        } else {
            b.append("<b>AC</b> ");
            b.append("<i>Total</i>: ").append((int) newPC.getTotalBonusTo("COMBAT", "AC"));
            b.append("<br>");
        }
        final int initMod = newPC.initiativeMod();
        b.append("<b>Init</b>: ").append((initMod >= 0) ? "+" : "").append(initMod);
        b.append("<br>");
        int bonus = newPC.baseAttackBonus();
        b.append(" <b>BAB</b>: ").append((bonus >= 0) ? "+" : "").append(bonus);
        b.append("<br>");
        b.append(" <b>").append(Globals.getGameModeHPAbbrev()).append("</b>: ").append(newPC.hitPoints());
        if (Globals.getGameModeAltHPText().length() != 0) {
            b.append(" <b>").append(Globals.getGameModeAltHPAbbrev()).append("</b>: ").append(newPC.altHP());
        }
        b.append("<br>");
        b.append("<b>Saves</b>: ");
        for (int z = 0; z < SettingsHandler.getGame().getUnmodifiableCheckList().size(); z++) {
            bonus = (int) newPC.getBonus(z + 1, true);
            b.append(" <i>").append(SettingsHandler.getGame().getUnmodifiableCheckList().get(z).toString()).append("</i>: ").append((bonus >= 0) ? "+" : "").append(bonus);
        }
        b.append("<br>");
        bonus = newPC.getSR();
        if (bonus > 0) {
            b.append("<b>Spell Resistance</b>: ").append(newPC.getSR());
        }
        b.append("<br>");
        b.append("<b>Special Abilities:</b>");
        b.append("<ul>");
        for (Iterator ii = newPC.getSpecialAbilityListStrings().iterator(); ii.hasNext(); ) {
            String sa = (String) ii.next();
            b.append("<li>").append(sa).append("</li>");
        }
        b.append("</ul>");
        b.append("<br>");
        return b.toString();
    }

    private void setInfoText(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Race) {
            Race aRace = (Race) obj;
            if (aRace.getName().startsWith("<none")) {
                return;
            }
            StringBuffer b = new StringBuffer();
            String bString = "";
            b.append("<html><font size=+1><b>").append(aRace.getName()).append("</b></font>");
            b.append("  <b>Type:</b>").append(aRace.getType());
            if (aRace.getMovement() != null) {
                bString = aRace.getMovement().toString();
            }
            if (bString.length() > 0) {
                b.append(" <b>Move</b>:").append(bString);
            }
            bString = aRace.getSize();
            if (bString.length() > 0) {
                b.append(" <b>Size</b>:").append(bString);
            }
            bString = aRace.getSource();
            if (bString.length() > 0) {
                b.append(" <b>SOURCE:</b> ").append(bString);
            }
            b.append("</html>");
            infoLabel.setText(b.toString());
        } else if (obj instanceof Follower) {
            Follower aF = (Follower) obj;
            PlayerCharacter newPC = null;
            for (Iterator p = Globals.getPCList().iterator(); p.hasNext(); ) {
                PlayerCharacter nPC = (PlayerCharacter) p.next();
                if (aF.getFileName().equals(nPC.getFileName())) {
                    newPC = nPC;
                }
            }
            if (newPC == null) {
                infoLabel.setText("NOTICE: Load from File to display info");
                return;
            }
            StringBuffer b = new StringBuffer();
            b.append("<html>");
            b.append(followerStatBlock(aF, newPC));
            b.append("</html>");
            infoLabel.setText(b.toString());
        }
    }

    private static int getSelectedIndex(ListSelectionEvent e) {
        final DefaultListSelectionModel model = (DefaultListSelectionModel) e.getSource();
        if (model == null) {
            return -1;
        }
        return model.getMinSelectionIndex();
    }

    /**
	 * Creates the FollowerModels
	 **/
    private final void createModels() {
        createAvailableModel();
        createSelectedModel();
    }

    private final void createSelectedModel() {
        if (selectedModel == null) {
            selectedModel = new SelectedFollowerModel(pc);
        } else {
            selectedModel.resetModel();
        }
        if (selectedSort != null) {
            selectedSort.setRoot((PObjectNode) selectedModel.getRoot());
        }
    }

    private class AvailableClickHandler implements ClickHandler {

        public void singleClickEvent() {
        }

        public void doubleClickEvent() {
            addButton();
        }

        public boolean isSelectable(Object obj) {
            return !(obj instanceof String);
        }
    }

    private class SelectedClickHandler implements ClickHandler {

        public void singleClickEvent() {
        }

        public void doubleClickEvent() {
            addFileButton();
        }

        public boolean isSelectable(Object obj) {
            return !(obj instanceof String);
        }
    }

    private final void createTreeTables() {
        availableTable = new JTreeTable(availableModel);
        availableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JTree atree = availableTable.getTree();
        atree.setRootVisible(false);
        atree.setShowsRootHandles(true);
        atree.setCellRenderer(new LabelTreeCellRenderer());
        availableTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final int idx = getSelectedIndex(e);
                    if (idx < 0) {
                        return;
                    }
                    final Object temp = atree.getPathForRow(idx).getLastPathComponent();
                    if (temp == null) {
                        infoLabel.setText();
                        return;
                    }
                    PObjectNode fNode = (PObjectNode) temp;
                    if (fNode.getItem() != null) {
                        addButton.setEnabled(true);
                        setInfoText(fNode.getItem());
                    }
                }
            }
        });
        selectedTable = new JTreeTable(selectedModel);
        selectedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JTree stree = selectedTable.getTree();
        stree.setRootVisible(false);
        stree.setShowsRootHandles(true);
        stree.setCellRenderer(new LabelTreeCellRenderer());
        selectedTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final int idx = getSelectedIndex(e);
                    if (idx < 0) {
                        return;
                    }
                    final Object temp = stree.getPathForRow(idx).getLastPathComponent();
                    if (temp == null) {
                        return;
                    }
                    PObjectNode fN = (PObjectNode) temp;
                    if ((fN.getItem() != null) && !(fN.getItem() instanceof String)) {
                        delButton.setEnabled(true);
                        loadButton.setEnabled(true);
                        setInfoText(fN.getItem());
                        return;
                    }
                    infoLabel.setText("None");
                    loadButton.setEnabled(false);
                    return;
                }
            }
        });
        availableTable.addMouseListener(new JTreeTableMouseAdapter(availableTable, new AvailableClickHandler(), false));
        selectedTable.addMouseListener(new JTreeTableMouseAdapter(selectedTable, new SelectedClickHandler(), false));
        hookupPopupMenu(availableTable);
        hookupPopupMenu(selectedTable);
    }

    /**
	 * removes an item from the selected table
	 **/
    private void delButton() {
        TreePath selCPath = selectedTable.getTree().getSelectionPath();
        if (selCPath == null) {
            ShowMessageDelegate.showMessageDialog("Select the object to remove", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        Object endComp = selCPath.getLastPathComponent();
        PObjectNode fNode = (PObjectNode) endComp;
        int iConfirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (iConfirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (fNode.getItem() instanceof Follower) {
            pc.delFollower((Follower) fNode.getItem());
        } else if (fNode.getItem() instanceof Equipment) {
            pc.removeEquipment((Equipment) fNode.getItem());
        } else {
            return;
        }
        pc.setDirty(true);
        updateSelectedModel();
    }

    /**
	 * Prompt the user to find the Followers .pcg file
	 * @param file
	 * @return PCG File
	 **/
    private File findPCGFile(File file) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Find file");
        fc.setCurrentDirectory(SettingsHandler.getPcgPath());
        if (fc.showOpenDialog(InfoResources.this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        file = fc.getSelectedFile();
        if (file.exists() && file.canWrite()) {
            return file;
        }
        return null;
    }

    private void formComponentShown() {
        refresh();
        requestFocus();
        PCGen_Frame1.setMessageAreaTextWithoutSaving("Add followers/cohorts, animal companions, special mounts or familiars");
        int top = topSplit.getDividerLocation();
        int bot = botSplit.getDividerLocation();
        int cent = centerSplit.getDividerLocation();
        int width;
        if (!hasBeenSized) {
            hasBeenSized = true;
            Component c = getParent();
            top = SettingsHandler.getPCGenOption("InfoResources.topSplit", ((c.getWidth() * 7) / 10));
            bot = SettingsHandler.getPCGenOption("InfoResources.botSplit", (c.getWidth() - 300));
            cent = SettingsHandler.getPCGenOption("InfoResources.centerSplit", (c.getHeight() - 100));
            for (int i = 0; i < selectedTable.getColumnCount(); i++) {
                TableColumn sCol = selectedTable.getColumnModel().getColumn(i);
                width = Globals.getCustColumnWidth("ResSel", i);
                if (width != 0) {
                    sCol.setPreferredWidth(width);
                }
                sCol.addPropertyChangeListener(new ResizeColumnListener(selectedTable, "ResSel", i));
            }
            for (int i = 0; i < availableTable.getColumnCount(); i++) {
                TableColumn sCol = availableTable.getColumnModel().getColumn(i);
                width = Globals.getCustColumnWidth("ResAva", i);
                if (width != 0) {
                    sCol.setPreferredWidth(width);
                }
                sCol.addPropertyChangeListener(new ResizeColumnListener(availableTable, "ResAva", i));
            }
        }
        if (top > 0) {
            topSplit.setDividerLocation(top);
            SettingsHandler.setPCGenOption("InfoResources.topSplit", top);
        }
        if (bot > 0) {
            botSplit.setDividerLocation(bot);
            SettingsHandler.setPCGenOption("InfoResources.botSplit", bot);
        }
        if (cent > 0) {
            centerSplit.setDividerLocation(cent);
            SettingsHandler.setPCGenOption("InfoResources.centerSplit", cent);
        }
    }

    private void hookupPopupMenu(JTreeTable treeTable) {
        treeTable.addMouseListener(new resPopupListener(treeTable, new ResourcesPopupMenu(treeTable)));
    }

    private void initActionListeners() {
        addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent evt) {
                formComponentShown();
            }
        });
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                int i = topSplit.getDividerLocation();
                if (i > 0) {
                    SettingsHandler.setPCGenOption("InfoResources.topSplit", i);
                }
                i = botSplit.getDividerLocation();
                if (i > 0) {
                    SettingsHandler.setPCGenOption("InfoResources.botSplit", i);
                }
                i = centerSplit.getDividerLocation();
                if (i > 0) {
                    SettingsHandler.setPCGenOption("InfoResources.centerSplit", i);
                }
            }
        });
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                addButton();
            }
        });
        delButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                delButton();
            }
        });
        loadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                loadButton();
            }
        });
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                updateButton();
            }
        });
        viewModeBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                viewModeBoxActionPerformed();
            }
        });
        viewSortBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                viewSortBoxActionPerformed();
            }
        });
        shouldLoadCompanion.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                pc.setLoadCompanion(shouldLoadCompanion.isSelected());
            }
        });
    }

    /**
	 * This method is called from within the constructor to
	 * initialize the form.
	 **/
    private void initComponents() {
        readyForRefresh = true;
        createModels();
        createTreeTables();
        viewModeBox.addItem("Followers   ");
        Utility.setDescription(viewModeBox, "Choose View Mode");
        viewSortBox.addItem(PropertyFactory.getString("in_typeName") + "   ");
        viewSortBox.addItem(PropertyFactory.getString("in_nameLabel") + "   ");
        viewSortBox.addItem(PropertyFactory.getString("in_racetypeName") + "   ");
        Utility.setDescription(viewSortBox, "Sort Sort");
        this.setLayout(new BorderLayout());
        createMasterView();
        createFollowerView();
        availableSort = new JTreeTableSorter(availableTable, (PObjectNode) availableModel.getRoot(), availableModel);
        selectedSort = new JTreeTableSorter(selectedTable, (PObjectNode) selectedModel.getRoot(), selectedModel);
        addFocusListener(new FocusAdapter() {

            public void focusGained(FocusEvent evt) {
                refresh();
            }
        });
    }

    /**
	 * load Follower from .pcg file and create new tab
	 **/
    private void loadButton() {
        TreePath selCPath = selectedTable.getTree().getSelectionPath();
        if (selCPath == null) {
            ShowMessageDelegate.showMessageDialog("Select the object to load", Constants.s_APPNAME, MessageType.ERROR);
            return;
        }
        Object endComp = selCPath.getLastPathComponent();
        PObjectNode fNode = (PObjectNode) endComp;
        if (fNode.getItem() instanceof Follower) {
            Follower aF = (Follower) fNode.getItem();
            if (aF == null) {
                return;
            }
            for (Iterator p = Globals.getPCList().iterator(); p.hasNext(); ) {
                PlayerCharacter nPC = (PlayerCharacter) p.next();
                if (aF.getFileName().equals(nPC.getFileName())) {
                    ShowMessageDelegate.showMessageDialog(aF.getName() + " is already loaded", Constants.s_APPNAME, MessageType.INFORMATION);
                    return;
                }
            }
            File file = new File(aF.getFileName());
            if (!file.exists()) {
                ShowMessageDelegate.showMessageDialog(aF.getFileName() + " has moved/changed. Please select the new .pcg filename ", Constants.s_APPNAME, MessageType.INFORMATION);
                Logging.errorPrint("b File: " + file.getAbsolutePath());
                file = findPCGFile(file);
                if (file == null) {
                    return;
                }
                Logging.errorPrint("a File: " + file.getAbsolutePath());
            }
            aF.setFileName(file.getAbsolutePath());
            ShowMessageDelegate.showMessageDialog("Loading " + aF.getName() + " from " + aF.getFileName() + " and switching tabs", Constants.s_APPNAME, MessageType.INFORMATION);
            PCGen_Frame1.getInst().loadPCFromFile(file);
            setNeedsUpdate(true);
            CharacterInfo pane = PCGen_Frame1.getCharacterPane();
            pane.setPaneForUpdate(pane.infoSummary());
            pane.refresh();
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    formComponentShown();
                }
            });
            return;
        } else if (fNode.getItem() instanceof Equipment) {
        }
        pc.setDirty(true);
        updateSelectedModel();
    }

    /**
	 * Updates the Available table
	 **/
    private void updateAvailableModel() {
        List pathList = availableTable.getExpandedPaths();
        createAvailableModel();
        availableTable.updateUI();
        availableTable.expandPathList(pathList);
    }

    /**
	 * Update Follower if master has been loaded
	 **/
    private void updateButton() {
        if (pc.getMaster() != null) {
            pc.setMaster(pc.getMaster());
            CharacterInfo pane = PCGen_Frame1.getCharacterPane();
            pane.setPaneForUpdate(pane.infoSkills());
            pane.setPaneForUpdate(pane.infoSummary());
            pane.setPaneForUpdate(pane.infoClasses());
            pane.setPaneForUpdate(pane.infoSpells());
            pane.setPaneForUpdate(pane.infoInventory());
            pane.refresh();
            ShowMessageDelegate.showMessageDialog("Done updating " + pc.getName(), Constants.s_APPNAME, MessageType.INFORMATION);
        }
        pc.setDirty(true);
        setFollowerInfo(pc);
    }

    /**
	 * Updates the Selected table
	 **/
    private void updateSelectedModel() {
        List pathList = selectedTable.getExpandedPaths();
        createSelectedModel();
        selectedTable.updateUI();
        selectedTable.expandPathList(pathList);
    }

    private void viewModeBoxActionPerformed() {
        final int index = viewModeBox.getSelectedIndex();
        if (index != viewMode) {
            viewMode = index;
            updateAvailableModel();
            updateSelectedModel();
        }
    }

    private void viewSortBoxActionPerformed() {
        final int index = viewSortBox.getSelectedIndex();
        if (index != viewSortMode) {
            viewSortMode = index;
            updateAvailableModel();
        }
    }

    /**
	 * create right click menus and listeners
	 **/
    private class ResourcesPopupMenu extends JPopupMenu {

        static final long serialVersionUID = 7236403406005940947L;

        private String lastSearch = "";

        ResourcesPopupMenu(JTreeTable treeTable) {
            if (treeTable == availableTable) {
                ResourcesPopupMenu.this.add(createAddMenuItem("Add New to List", "shortcut EQUALS"));
                this.addSeparator();
                ResourcesPopupMenu.this.add(Utility.createMenuItem("Find item", new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        lastSearch = availableTable.searchTree(lastSearch);
                    }
                }, "searchItem", (char) 0, "shortcut F", "Find item", null, true));
            } else {
                ResourcesPopupMenu.this.add(createDelMenuItem("Remove from List", "shortcut MINUS"));
                ResourcesPopupMenu.this.add(createAddFileMenuItem("Add from existing File", "shortcut PLUS"));
                this.addSeparator();
                ResourcesPopupMenu.this.add(Utility.createMenuItem("Find item", new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        lastSearch = selectedTable.searchTree(lastSearch);
                    }
                }, "searchItem", (char) 0, "shortcut F", "Find item", null, true));
            }
        }

        private JMenuItem createAddFileMenuItem(String label, String accelerator) {
            return Utility.createMenuItem(label, new AddFileActionListener(), "add 1", (char) 0, accelerator, "Add Existing File", "Add16.gif", true);
        }

        private JMenuItem createAddMenuItem(String label, String accelerator) {
            return Utility.createMenuItem(label, new AddActionListener(), "add 1", (char) 0, accelerator, "Add to List", "Add16.gif", true);
        }

        private JMenuItem createDelMenuItem(String label, String accelerator) {
            return Utility.createMenuItem(label, new DelActionListener(), "remove 1", (char) 0, accelerator, "Remove from List", "Remove16.gif", true);
        }

        private class AddActionListener extends resActionListener {

            public void actionPerformed(ActionEvent evt) {
                addButton();
            }
        }

        private class AddFileActionListener extends resActionListener {

            public void actionPerformed(ActionEvent evt) {
                addFileButton();
            }
        }

        private class DelActionListener extends resActionListener {

            public void actionPerformed(ActionEvent evt) {
                delButton();
            }
        }

        private class resActionListener implements ActionListener {

            public void actionPerformed(ActionEvent evt) {
            }
        }
    }

    private class resPopupListener extends MouseAdapter {

        private JTree tree;

        private ResourcesPopupMenu menu;

        resPopupListener(JTreeTable treeTable, ResourcesPopupMenu aMenu) {
            tree = treeTable.getTree();
            menu = aMenu;
            KeyListener myKeyListener = new KeyListener() {

                public void keyTyped(KeyEvent e) {
                    dispatchEvent(e);
                }

                public void keyPressed(KeyEvent e) {
                    final int keyCode = e.getKeyCode();
                    if (keyCode != KeyEvent.VK_UNDEFINED) {
                        final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                        for (int i = 0; i < menu.getComponentCount(); i++) {
                            final Component menuComponent = menu.getComponent(i);
                            if (menuComponent instanceof JMenuItem) {
                                KeyStroke ks = ((JMenuItem) menuComponent).getAccelerator();
                                if ((ks != null) && keyStroke.equals(ks)) {
                                    selPath = tree.getSelectionPath();
                                    ((JMenuItem) menuComponent).doClick(2);
                                    return;
                                }
                            }
                        }
                    }
                    dispatchEvent(e);
                }

                public void keyReleased(KeyEvent e) {
                    dispatchEvent(e);
                }
            };
            treeTable.addKeyListener(myKeyListener);
        }

        public void mousePressed(MouseEvent evt) {
            maybeShowPopup(evt);
        }

        public void mouseReleased(MouseEvent evt) {
            maybeShowPopup(evt);
        }

        private void maybeShowPopup(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                selPath = tree.getClosestPathForLocation(evt.getX(), evt.getY());
                if (selPath == null) {
                    return;
                }
                tree.setSelectionPath(selPath);
                menu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }
}
