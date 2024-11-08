package de.cinek.rssview;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * @author  Matthias Schmidt
 * @version $Id: RssFontSetupPanel.java,v 1.4 2004/10/27 23:19:08 saintedlama Exp $
 */
public class RssFontSetupPanel extends JPanel implements SetupComponent {

    JTextField TreeFontName;

    JTextField ArticleFontName;

    JTextField TableFontName;

    JButton TreeFontButton;

    JButton ArticleFontButton;

    JButton TableFontButton;

    Font TreeFont;

    Font ArticleFont;

    Font TableFont;

    private static final int FONT_TEXT_SIZE = 22;

    private ResourceBundle rb;

    RssSettings settings;

    /** Creates a new instance of RssProxySetupPanel */
    public RssFontSetupPanel() {
        super(new GridBagLayout());
        rb = ResourceBundle.getBundle("rssview");
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.gridwidth = GridBagConstraints.REMAINDER;
        gbcRight.anchor = GridBagConstraints.WEST;
        gbcRight.insets = new Insets(2, 2, 2, 2);
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.anchor = GridBagConstraints.EAST;
        gbcLeft.insets = new Insets(2, 2, 2, 2);
        GridBagConstraints gbcHeaderLabel = new GridBagConstraints();
        gbcHeaderLabel.insets = new Insets(2, 2, 2, 2);
        gbcHeaderLabel.weightx = 1.0;
        gbcHeaderLabel.anchor = GridBagConstraints.EAST;
        gbcHeaderLabel.fill = GridBagConstraints.HORIZONTAL;
        gbcHeaderLabel.gridwidth = GridBagConstraints.REMAINDER;
        add(new de.cinek.rssview.ui.JOptionsTitle(rb.getString("FONT_SETTINGS")), gbcHeaderLabel);
        settings = RssSettings.getInstance();
        TreeFont = settings.getTreeFont();
        ArticleFont = settings.getArticleFont();
        TableFont = settings.getTableFont();
        add(new JLabel(rb.getString("Tree_Font")), gbcLeft);
        TreeFontName = new JTextField(FONT_TEXT_SIZE);
        TreeFontName.setHorizontalAlignment(SwingConstants.RIGHT);
        add(TreeFontName, gbcLeft);
        TreeFontButton = new JButton(new RssFontSetupPanel.ChooseFontAction());
        add(TreeFontButton, gbcRight);
        TreeFontName.setEditable(false);
        TreeFontName.setText(fontToName(TreeFont));
        add(new JLabel(rb.getString("Table_Font")), gbcLeft);
        TableFontName = new JTextField(FONT_TEXT_SIZE);
        TableFontName.setHorizontalAlignment(SwingConstants.RIGHT);
        add(TableFontName, gbcLeft);
        TableFontButton = new JButton(new RssFontSetupPanel.ChooseFontAction());
        add(TableFontButton, gbcRight);
        TableFontName.setEditable(false);
        TableFontName.setText(fontToName(TableFont));
        gbcLeft.weighty = 1.0;
        add(new JLabel(), gbcLeft);
    }

    public void acceptChanges() {
        settings.setTreeFont(TreeFont);
        settings.setArticleFont(ArticleFont);
        settings.setTableFont(TableFont);
        Container parent = this;
        while (parent.getParent() != null) parent = parent.getParent();
        (((RssView) parent).getChannelList()).repaintTree();
        (((RssView) parent).getChannelTitle()).repaintTable();
    }

    public String fontToName(Font font) {
        if (font == null) return ("Font undefined");
        String StyleName = "";
        switch(font.getStyle()) {
            case (Font.PLAIN):
                StyleName = "Regular";
                break;
            case (Font.ITALIC):
                StyleName = "Italic";
                break;
            case (Font.BOLD):
                StyleName = "Bold";
                break;
            case (Font.BOLD + Font.ITALIC):
                StyleName = "BoldItalic";
                break;
        }
        return font.getName() + "-" + StyleName + "-" + font.getSize();
    }

    private class ChooseFontAction extends javax.swing.AbstractAction {

        public ChooseFontAction() {
            super(rb.getString("ChooseFont"));
        }

        public void actionPerformed(ActionEvent e) {
            Font font;
            if (e.getSource() == TreeFontButton) {
                font = FontChooser.showDialog((Component) null, rb.getString("Select_Tree_font"), TreeFont);
                if (font != null) {
                    TreeFont = font;
                    TreeFontName.setText(fontToName(TreeFont));
                }
            } else if (e.getSource() == ArticleFontButton) {
                font = FontChooser.showDialog((Component) null, rb.getString("Select_Article_font"), ArticleFont);
                if (font != null) {
                    ArticleFont = font;
                    ArticleFontName.setText(fontToName(ArticleFont));
                }
            } else if (e.getSource() == TableFontButton) {
                font = FontChooser.showDialog((Component) null, rb.getString("Select_Table_font"), TableFont);
                if (font != null) {
                    TableFont = font;
                    TableFontName.setText(fontToName(TableFont));
                }
            }
        }
    }
}
