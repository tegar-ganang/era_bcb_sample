package net.hypotenubel.jaicwain.gui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import net.hypotenubel.util.swing.*;
import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.session.irc.*;

/**
 * Presents a Join Channel dialog to the user. Complete with favourite channels.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: JoinDialog.java 153 2006-10-07 13:15:39Z captainnuss $
 */
public class JoinDialog extends JDialog implements ActionListener {

    /**
     * Constant identifying the Custom Channel option.
     */
    public static final int OPT_CUSTOM = 0;

    /**
     * Constant identifying the Favourite Channel option.
     */
    public static final int OPT_FAVOURITE = 1;

    /**
     * Stores what the user did.
     */
    private int result = JOptionPane.CANCEL_OPTION;

    private JDialogHeaderPanel header = null;

    private JPanel contentPanel = null;

    private ButtonGroup radioButtonGroup = null;

    private JRadioButton customChannelRadioButton = null;

    private JLabel customChannelNameLabel = null;

    private JTextField customChannelNameTextField = null;

    private JLabel customChannelKeyLabel = null;

    private JTextField customChannelKeyTextField = null;

    private JRadioButton favouritesRadioButton = null;

    private JTable favouritesTable = null;

    private DefaultTableModel favouritesTableModel = null;

    private JScrollPane favouritesTableScrollPane = null;

    private JButton favouritesAdd = null;

    private JButton favouritesRemove = null;

    private JButton favouritesEdit = null;

    private JButton joinButton = null;

    private JButton cancelButton = null;

    private JButton helpButton = null;

    /**
     * Creates a new {@code JoinDialog}. The option that was selected the
     * last time the dialog was shown will automatically be selected.
     *
     * @param frame {@code Frame} being the parent of this dialog.
     */
    public JoinDialog(Frame frame) {
        this(frame, App.options.getIntOption("gui", "joindialog.option", OPT_CUSTOM));
    }

    /**
     * Creates a new {@code JoinDialog}.
     *
     * @param frame {@code Frame} being the parent of this dialog.
     * @param option {@code int} specifying which radio button to select.
     *               Must be one of the constants defined by this class. If it
     *               isn't, the custom channel option will be selected.
     */
    public JoinDialog(Frame frame, int option) {
        super(frame, true);
        createUI();
        this.setLocationRelativeTo(frame);
        if (favouritesTableModel.getRowCount() != 0) {
            favouritesTable.setRowSelectionInterval(0, 0);
        } else {
            favouritesEdit.setEnabled(false);
            favouritesRemove.setEnabled(false);
        }
        if (option == OPT_FAVOURITE) favouritesRadioButton.doClick(); else customChannelRadioButton.doClick();
    }

    /**
     * Initializes the GUI stuff.
     */
    private void createUI() {
        FormLayout layout = new FormLayout("10dlu, right:pref, 4dlu, fill:pref:grow", "p, 3dlu, p, 3dlu, p, 7dlu, p, 3dlu, top:min:grow, 2dlu, p, 7dlu, p, 5dlu, p");
        layout.setRowGroups(new int[][] { { 3, 5 }, { 1, 7 } });
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        CellConstraints cc2 = new CellConstraints();
        TextComponentSelector selector = new TextComponentSelector();
        radioButtonGroup = new ButtonGroup();
        customChannelRadioButton = new JRadioButton();
        customChannelRadioButton.setFont(customChannelRadioButton.getFont().deriveFont(Font.BOLD));
        customChannelRadioButton.setActionCommand("customChannelRadioButton");
        customChannelRadioButton.addActionListener(this);
        radioButtonGroup.add(customChannelRadioButton);
        builder.add(customChannelRadioButton, cc.xyw(1, 1, 4));
        customChannelNameLabel = new JLabel();
        customChannelNameTextField = new JTextField("");
        selector.registerTextComponent(customChannelNameTextField);
        builder.add(customChannelNameLabel, cc.xy(2, 3), customChannelNameTextField, cc2.xy(4, 3));
        customChannelKeyLabel = new JLabel();
        customChannelKeyTextField = new JTextField("");
        selector.registerTextComponent(customChannelKeyTextField);
        builder.add(customChannelKeyLabel, cc.xy(2, 5), customChannelKeyTextField, cc2.xy(4, 5));
        favouritesRadioButton = new JRadioButton();
        favouritesRadioButton.setFont(favouritesRadioButton.getFont().deriveFont(Font.BOLD));
        favouritesRadioButton.setActionCommand("favouritesRadioButton");
        favouritesRadioButton.addActionListener(this);
        radioButtonGroup.add(favouritesRadioButton);
        builder.add(favouritesRadioButton, cc.xyw(1, 7, 4));
        favouritesTableModel = new DefaultTableModel();
        favouritesTableModel.addColumn(App.localization.localize("app", "joindialog.favouritestable.channel", "Channel"));
        favouritesTableModel.addColumn(App.localization.localize("app", "joindialog.favouritestable.key", "Key"));
        favouritesTableScrollPane = new JScrollPane(favouritesTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        builder.add(favouritesTableScrollPane, cc.xyw(2, 9, 3, "fill, fill"));
        favouritesAdd = new JButton("");
        favouritesAdd.setActionCommand("favouritesAdd");
        favouritesAdd.addActionListener(this);
        favouritesRemove = new JButton("");
        favouritesRemove.setActionCommand("favouritesRemove");
        favouritesRemove.addActionListener(this);
        favouritesEdit = new JButton("");
        favouritesEdit.setActionCommand("favouritesEdit");
        favouritesEdit.addActionListener(this);
        JPanel buttonBar = ButtonBarFactory.buildAddRemovePropertiesRightBar(favouritesAdd, favouritesRemove, favouritesEdit);
        builder.add(buttonBar, cc.xyw(2, 11, 3));
        builder.addSeparator("", cc.xyw(1, 13, 4));
        joinButton = new JButton();
        joinButton.setActionCommand("joinButton");
        joinButton.addActionListener(this);
        cancelButton = new JButton();
        cancelButton.setActionCommand("cancelButton");
        cancelButton.addActionListener(this);
        helpButton = new JButton();
        helpButton.setEnabled(false);
        helpButton.setActionCommand("helpButton");
        helpButton.addActionListener(this);
        helpButton.setEnabled(false);
        buttonBar = ButtonBarFactory.buildOKCancelHelpBar(joinButton, cancelButton, helpButton);
        builder.add(buttonBar, cc.xyw(1, 15, 4));
        header = new JDialogHeaderPanel("", "", null);
        contentPanel = builder.getPanel();
        setTexts();
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(header, BorderLayout.NORTH);
        this.getContentPane().add(contentPanel, BorderLayout.CENTER);
        this.getRootPane().setDefaultButton(joinButton);
        this.setSize(470, 500);
    }

    /**
     * Sets the texts of all those text things.
     */
    private void setTexts() {
        header.setHeading(App.localization.localize("app", "joindialog.header.heading", "Join Channel"));
        header.setDetails(App.localization.localize("app", "joindialog.header.details", "Enter a channel name to join or select one or more of your " + "favourite channels and click Join. The channel key isn't " + "required in most cases."));
        customChannelRadioButton.setText(App.localization.localize("app", "joindialog.customchannelradiobutton.text", "Custom channel"));
        customChannelNameLabel.setText(App.localization.localize("app", "joindialog.customchannelnamelabel.text", "Channel name:"));
        customChannelKeyLabel.setText(App.localization.localize("app", "joindialog.customchannelkeylabel.text", "Channel key:"));
        favouritesRadioButton.setText(App.localization.localize("app", "joindialog.favouritesradiobutton.text", "Favourite channels"));
        favouritesAdd.setText(App.localization.localize("app", "joindialog.favouritesadd.text", "Add"));
        favouritesEdit.setText(App.localization.localize("app", "joindialog.favouritesedit.text", "Edit"));
        favouritesRemove.setText(App.localization.localize("app", "joindialog.favouritesremove.text", "Remove"));
        helpButton.setText(App.localization.localize("app", "general.help", "Help"));
        joinButton.setText(App.localization.localize("app", "joindialog.joinbutton.text", "Join"));
        cancelButton.setText(App.localization.localize("app", "general.cancel", "Cancel"));
        this.setTitle(App.localization.localize("app", "joindialog.text", "Join Channel"));
    }

    /**
     * Returns the user's action.
     * 
     * @return {@code int} specifying whether the user hit the Connect
     *         button ({@code JOptionPane.OK_OPTION}) or not
     *         ({@code JOptionPane.CANCEL_OPTION}).
     */
    public int getResult() {
        return result;
    }

    /**
     * Returns the selected channel objects.
     * 
     * @return Array of {@code IRCChannel}s the user wants to join.
     */
    public IRCChannel[] getChannels() {
        if (customChannelRadioButton.isSelected()) {
        }
        ArrayList<IRCChannel> list = new ArrayList<IRCChannel>(favouritesTable.getSelectedRowCount());
        return list.toArray(new IRCChannel[0]);
    }

    public void actionPerformed(ActionEvent e) {
    }
}
