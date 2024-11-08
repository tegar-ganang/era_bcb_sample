package uk.ac.kingston.aqurate.author_UI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

public class JDialogNewQuestion extends JDialog implements MouseListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private JButton jButtonCancel = null;

    private JButton jButtonCreate = null;

    private JPanel jContentPane = null;

    private JPanel jPaneDescription = null;

    private JPanel jPanelButtons = null;

    private JPanel jPanelNewQuestion = null;

    private JPanel jPanelTypes = null;

    private JRadioButton jRadioButtonAssociate = null;

    private JRadioButton jRadioButtonChoice = null;

    private JRadioButton jRadioButtonGraphic = null;

    private JRadioButton jRadioButtonHotspot = null;

    private JRadioButton jRadioButtonInline = null;

    private JRadioButton jRadioButtonOrder = null;

    private JRadioButton jRadioButtonSlider = null;

    private JRadioButton jRadioButtonTextEntry = null;

    private JToolBar jToolBarButtons = null;

    private JLabel labelIllustration = null;

    private String selectedType;

    private JTextArea textarea = null;

    private JRadioButton[] vRadioButtons = new JRadioButton[8];

    ImageIcon choiceIcon = null;

    ImageIcon orderIcon = null;

    ImageIcon associateIcon = null;

    ImageIcon inlineIcon = null;

    ImageIcon textentryIcon = null;

    ImageIcon sliderIcon = null;

    ImageIcon hotspotIcon = null;

    ImageIcon graphicIcon = null;

    AqurateFramework owner = null;

    private JPanel jPanePanelButonsOKCANCEL = null;

    private JPanel panelButtonsOKCANCEL = null;

    private JPanel jPanePanelButons = null;

    private JPanel jPanelQuestion = null;

    private JPanel panelButtons = null;

    /**
	 * @param owner
	 */
    public JDialogNewQuestion(AqurateFramework owner) {
        super();
        this.owner = owner;
        initialize();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(jButtonCancel)) {
            this.selectedType = "";
            AqurateFramework.setNewQuestion(selectedType);
            this.dispose();
        }
        if (e.getSource().equals(jButtonCreate)) {
            this.setVisible(false);
            if (this.jRadioButtonChoice.isSelected()) {
                this.selectedType = "choiceinteraction";
            }
            if (this.jRadioButtonOrder.isSelected()) {
                this.selectedType = "orderinteraction";
            }
            if (this.jRadioButtonAssociate.isSelected()) {
                this.selectedType = "associateinteraction";
            }
            if (this.jRadioButtonInline.isSelected()) {
                this.selectedType = "inlineinteraction";
            }
            if (this.jRadioButtonTextEntry.isSelected()) {
                this.selectedType = "textentryinteraction";
            }
            if (this.jRadioButtonHotspot.isSelected()) {
                this.selectedType = "hotspotinteraction";
            }
            if (this.jRadioButtonGraphic.isSelected()) {
                this.selectedType = "graphicorderinteraction";
            }
            if (this.jRadioButtonSlider.isSelected()) {
                this.selectedType = "sliderinteraction";
            }
            AqurateFramework.setNewQuestion(selectedType);
            this.setVisible(false);
        }
    }

    public GridBagConstraints buildConstraints(int x, int y, int w, int h, int wx, int wy, int fill, int anchor, int top, int left, int bottom, int right) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weightx = wx;
        gbc.weighty = wy;
        gbc.fill = fill;
        gbc.anchor = anchor;
        gbc.insets = new Insets(top, left, bottom, right);
        return gbc;
    }

    /**
	 * This method initializes jButtonCancel
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getJButtonCancel() {
        if (jButtonCancel == null) {
            jButtonCancel = new JButton("Cancel");
        }
        jButtonCancel.setPreferredSize(new Dimension(70, 25));
        jButtonCancel.addActionListener(this);
        return jButtonCancel;
    }

    /**
	 * This method initializes jButtonCreate
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getJButtonCreate() {
        if (jButtonCreate == null) {
            jButtonCreate = new JButton("Create");
        }
        jButtonCreate.setPreferredSize(new Dimension(70, 25));
        jButtonCreate.addActionListener(this);
        return jButtonCreate;
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
        }
        jContentPane.setLayout(new BorderLayout());
        jContentPane.add(getJPanePanelButtonsOKCANCEL(), BorderLayout.SOUTH);
        jContentPane.add(getJPanelTypes(), BorderLayout.WEST);
        jContentPane.add(getJPaneDescription(), BorderLayout.CENTER);
        jContentPane.setVisible(true);
        return jContentPane;
    }

    /**
	 * This method initializes jScrollPaneDescription
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JPanel getJPaneDescription() {
        if (jPaneDescription == null) {
            jPaneDescription = new JPanel();
            jPaneDescription.setPreferredSize(new Dimension(390, 390));
            jPaneDescription.setBorder(new TitledBorder("Description"));
            jPaneDescription.setLayout(new FlowLayout());
        }
        labelIllustration = new JLabel();
        jPaneDescription.add(getJTextArea());
        jPaneDescription.add(labelIllustration);
        return jPaneDescription;
    }

    private JPanel getJPanelButtons() {
        if (jPanelButtons == null) {
            jPanelButtons = new JPanel();
        }
        jPanelButtons.setPreferredSize(new Dimension(500, 30));
        jPanelButtons.setLayout(new GridBagLayout());
        jPanelButtons.setVisible(true);
        jPanelButtons.add(getJButtonCreate(), this.buildConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 10, 0, 30));
        jPanelButtons.add(getJButtonCancel(), this.buildConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 10, 0, 0));
        return jPanelButtons;
    }

    private JPanel getJPanelNewQuestion() {
        if (jPanelNewQuestion == null) {
            jPanelNewQuestion = new JPanel();
        }
        jPanelNewQuestion.setPreferredSize(new Dimension(550, 370));
        jPanelNewQuestion.setLayout(new GridBagLayout());
        jPanelNewQuestion.setVisible(true);
        jPanelNewQuestion.add(getJPanelTypes(), this.buildConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 0, 0, 0));
        jPanelNewQuestion.add(getJPaneDescription(), this.buildConstraints(1, 0, 2, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 0, 0, 0));
        jPanelNewQuestion.add(getJButtonCreate(), this.buildConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 0, 0, 0));
        jPanelNewQuestion.add(getJButtonCancel(), this.buildConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.NONE, 0, 0, 0, 0));
        return jPanelNewQuestion;
    }

    /**
	 * This method initializes jPanelTypes
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJPanelTypes() {
        if (jPanelTypes == null) {
            jPanelTypes = new JPanel();
            jPanelTypes.setLayout(new GridBagLayout());
            jPanelTypes.setPreferredSize(new Dimension(160, 390));
            jPanelTypes.setBorder(new TitledBorder("Types"));
            jPanelTypes.add(getJRadioButtonChoice(), this.buildConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 10, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonOrder(), this.buildConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonAssociate(), this.buildConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonInline(), this.buildConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonTextEntry(), this.buildConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonHotspot(), this.buildConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonGraphic(), this.buildConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            jPanelTypes.add(getJRadioButtonSlider(), this.buildConstraints(0, 7, 1, 1, 0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 0, 0, 10, 0));
            ButtonGroup myButtonGroup = new ButtonGroup();
            for (int i = 0; i < 8; i++) myButtonGroup.add(vRadioButtons[i]);
        }
        return jPanelTypes;
    }

    /**
	 * This method initializes jRadioButtonAssociate
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonAssociate() {
        if (jRadioButtonAssociate == null) {
            jRadioButtonAssociate = new JRadioButton();
            jRadioButtonAssociate.setText("Associate");
        }
        jRadioButtonAssociate.addMouseListener(this);
        vRadioButtons[2] = jRadioButtonAssociate;
        return jRadioButtonAssociate;
    }

    /**
	 * This method initializes jRadioButtonChoice
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonChoice() {
        if (jRadioButtonChoice == null) {
            jRadioButtonChoice = new JRadioButton();
            jRadioButtonChoice.setText("Choice");
        }
        jRadioButtonChoice.addMouseListener(this);
        vRadioButtons[0] = jRadioButtonChoice;
        return jRadioButtonChoice;
    }

    /**
	 * This method initializes jRadioButtonGraphic
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonGraphic() {
        if (jRadioButtonGraphic == null) {
            jRadioButtonGraphic = new JRadioButton();
            jRadioButtonGraphic.setText("Graphic Order");
        }
        jRadioButtonGraphic.addMouseListener(this);
        vRadioButtons[6] = jRadioButtonGraphic;
        return jRadioButtonGraphic;
    }

    /**
	 * This method initializes jRadioButtonHotspot
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonHotspot() {
        if (jRadioButtonHotspot == null) {
            jRadioButtonHotspot = new JRadioButton();
            jRadioButtonHotspot.setText("Hotspot");
        }
        jRadioButtonHotspot.addMouseListener(this);
        vRadioButtons[5] = jRadioButtonHotspot;
        return jRadioButtonHotspot;
    }

    /**
	 * This method initializes jRadioButtonInline
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonInline() {
        if (jRadioButtonInline == null) {
            jRadioButtonInline = new JRadioButton();
            jRadioButtonInline.setText("Inline Choice");
        }
        jRadioButtonInline.addMouseListener(this);
        vRadioButtons[3] = jRadioButtonInline;
        return jRadioButtonInline;
    }

    /**
	 * This method initializes jRadioButtonOrder
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonOrder() {
        if (jRadioButtonOrder == null) {
            jRadioButtonOrder = new JRadioButton();
            jRadioButtonOrder.setText("Order");
        }
        jRadioButtonOrder.addMouseListener(this);
        vRadioButtons[1] = jRadioButtonOrder;
        return jRadioButtonOrder;
    }

    /**
	 * This method initializes jRadioButtonSlider
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonSlider() {
        if (jRadioButtonSlider == null) {
            jRadioButtonSlider = new JRadioButton();
            jRadioButtonSlider.setText("Slider");
        }
        jRadioButtonSlider.addMouseListener(this);
        vRadioButtons[7] = jRadioButtonSlider;
        return jRadioButtonSlider;
    }

    /**
	 * This method initializes jRadioButtonTextEntry
	 * 
	 * @return javax.swing.JRadioButton
	 */
    private JRadioButton getJRadioButtonTextEntry() {
        if (jRadioButtonTextEntry == null) {
            jRadioButtonTextEntry = new JRadioButton();
            jRadioButtonTextEntry.setText("Text Entry");
        }
        jRadioButtonTextEntry.addMouseListener(this);
        vRadioButtons[4] = jRadioButtonTextEntry;
        return jRadioButtonTextEntry;
    }

    private JTextArea getJTextArea() {
        if (textarea == null) {
            textarea = new JTextArea();
            textarea.setPreferredSize(new Dimension(390, 100));
        }
        textarea.setEditable(false);
        textarea.setBackground(this.jPaneDescription.getBackground());
        return textarea;
    }

    private JPanel getJPanePanelButtonsOKCANCEL() {
        if (jPanePanelButonsOKCANCEL == null) {
            jPanePanelButonsOKCANCEL = new JPanel(new BorderLayout());
            jPanePanelButonsOKCANCEL.setPreferredSize(new Dimension(140, 25));
            jPanePanelButonsOKCANCEL.add(getJPanelButtonsOKCANCEL(), BorderLayout.EAST);
        }
        return jPanePanelButonsOKCANCEL;
    }

    private JPanel getJPanelButtonsOKCANCEL() {
        if (panelButtonsOKCANCEL == null) {
        }
        panelButtonsOKCANCEL = new JPanel(new GridLayout(1, 2));
        panelButtonsOKCANCEL.add(getJButtonCreate());
        panelButtonsOKCANCEL.add(getJButtonCancel());
        panelButtonsOKCANCEL.setVisible(true);
        return panelButtonsOKCANCEL;
    }

    /**
	 * This method initializes jToolBarButtons
	 * 
	 * @return javax.swing.JToolBar
	 */
    private JToolBar getJToolBarButtons() {
        if (jToolBarButtons == null) {
            jToolBarButtons = new JToolBar();
        }
        jToolBarButtons.setPreferredSize(new Dimension(580, 35));
        jToolBarButtons.setFloatable(false);
        jToolBarButtons.setRollover(true);
        jToolBarButtons.add(getJButtonCreate());
        jToolBarButtons.addSeparator(new Dimension(20, 35));
        jToolBarButtons.add(getJButtonCancel());
        return jToolBarButtons;
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(600, 400);
        this.setResizable(false);
        this.setTitle("Create New Question");
        this.setModal(true);
        this.setContentPane(getJContentPane());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        String desChoice = "Respondents select the item(s) that are correct \nfrom a list.\n\n" + "For example:\n";
        textarea.append(desChoice);
        jRadioButtonChoice.setSelected(true);
        if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
            String userDir = System.getProperty("user.dir");
            System.out.println(userDir);
            choiceIcon = new ImageIcon("images/newchoice.gif");
            orderIcon = new ImageIcon("images/neworder.gif");
            associateIcon = new ImageIcon("images/newassociate.gif");
            inlineIcon = new ImageIcon("images/newinline.gif");
            textentryIcon = new ImageIcon("images/newtext.gif");
            sliderIcon = new ImageIcon("images/newslider.gif");
            hotspotIcon = new ImageIcon("images/newhotspot.gif");
            graphicIcon = new ImageIcon("images/newgraphicorder.gif");
            labelIllustration.setIcon(choiceIcon);
        }
    }

    public void mouseClicked(MouseEvent e) {
        textarea.setText(null);
        labelIllustration.setIcon(null);
        if (e.getSource().equals(this.jRadioButtonChoice)) {
            String desChoice = "Respondents select the item(s) that are correct \nfrom a list.\n\n" + "For example:\n";
            textarea.append(desChoice);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(choiceIcon);
            }
            jRadioButtonChoice.setSelected(true);
        } else if (e.getSource().equals(this.jRadioButtonOrder)) {
            String desOrder = "Respondents place a set of items in a correct \norder.\n\n" + "For example:\n";
            textarea.append(desOrder);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(orderIcon);
            }
            jRadioButtonOrder.setSelected(true);
        } else if (e.getSource().equals(this.jRadioButtonAssociate)) {
            String desAssociate = "Respondents indicate the items in a set that are \nrelated.\n\n" + "For example:\n";
            textarea.append(desAssociate);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(associateIcon);
            }
            jRadioButtonAssociate.setSelected(true);
        } else if (e.getSource().equals(this.jRadioButtonInline)) {
            String desInline = "A choice is embedded in a line of text. Respondents \nselect the option that is correct.\n\n" + "For example:\n";
            textarea.append(desInline);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(inlineIcon);
            }
            jRadioButtonInline.setSelected(true);
        } else if (e.getSource().equals(this.jRadioButtonTextEntry)) {
            String desText = "Respondents generate a word or phrase that is \ncorrect.\n\n" + "For example:\n";
            textarea.append(desText);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(textentryIcon);
            }
            jRadioButtonTextEntry.setSelected(true);
        } else if (e.getSource().equals(this.jRadioButtonHotspot)) {
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(hotspotIcon);
                String desHotspot = "Respondents select the point on an image that is \ncorrect.\n\n" + "For example:\n";
                textarea.append(desHotspot);
                jRadioButtonHotspot.setSelected(true);
            } else {
                String desGraphic = "The Applet cannot handle this type of question because it cannot read or write to disk";
                textarea.append(desGraphic);
            }
        } else if (e.getSource().equals(this.jRadioButtonGraphic)) {
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                String desGraphic = "Respondents arrange points on an image in order.\n\n" + "For example:\n";
                textarea.append(desGraphic);
                labelIllustration.setIcon(graphicIcon);
                jRadioButtonGraphic.setSelected(true);
            } else {
                String desGraphic = "The Applet cannot handle this type of question because it cannot read or write to disk";
                textarea.append(desGraphic);
            }
        } else if (e.getSource().equals(this.jRadioButtonSlider)) {
            String desSlider = "Respondents select the correct value with a \nslider.\n\n" + "For example:\n";
            textarea.append(desSlider);
            if (owner.getClass().getSuperclass().getSimpleName().equals("JFrame")) {
                labelIllustration.setIcon(sliderIcon);
            }
            jRadioButtonSlider.setSelected(true);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
