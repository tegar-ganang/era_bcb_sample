package org.xngr.browser.document;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import org.bounce.FormLayout;
import org.bounce.QPanel;
import org.bounce.image.ImageLoader;
import org.xngr.browser.ExchangerDocument;

/**
 * The properties dialog for a document.
 *
 * @version	$Revision: 1.4 $, $Date: 2007/06/28 13:14:18 $
 * @author Edwin Dankert <edankert@cladonia.com>
 */
public class DocumentPropertiesDialog extends JDialog {

    private static final long serialVersionUID = 8355388895256679551L;

    private static final Dimension SIZE = new Dimension(400, 225);

    private static final ImageIcon SELECTED_ICON = ImageLoader.get().getImage("/org/xngr/browser/icons/CheckedIcon.gif");

    ;

    private static final ImageIcon UNSELECTED_ICON = ImageLoader.get().getImage("/org/xngr/browser/icons/UncheckedIcon.gif");

    ;

    private ExchangerDocument document = null;

    private JPanel panel = null;

    private QPanel formPanel = null;

    private JPanel buttonPanel = null;

    private JButton cancelButton = null;

    private JButton okButton = null;

    private JTextField nameField = null;

    private JTextField locationField = null;

    private JCheckBox validateField = null;

    private JTextField modifiedField = null;

    private JTextField sizeField = null;

    private JLabel readBox = null;

    private JLabel writeBox = null;

    private JLabel hiddenBox = null;

    /**
	 * The dialog that displays the properties for the document.
	 *
	 * @param frame the parent frame.
	 */
    public DocumentPropertiesDialog(JFrame frame) {
        super(frame, true);
        setResizable(false);
        setSize(SIZE);
        formPanel = new QPanel(new FormLayout(5, 3));
        formPanel.setBorder(new CompoundBorder(new LineBorder(Color.darkGray), new EmptyBorder(5, 5, 5, 5)));
        formPanel.setGradientBackground(true);
        nameField = new JTextField();
        formPanel.add(new JLabel("Name:"), FormLayout.LEFT);
        formPanel.add(nameField, FormLayout.RIGHT_FILL);
        validateField = new JCheckBox("Validate");
        validateField.setBorder(null);
        formPanel.add(validateField, FormLayout.RIGHT);
        JPanel separator1 = new JPanel();
        separator1.setPreferredSize(new Dimension(100, 10));
        separator1.setOpaque(false);
        formPanel.add(separator1, FormLayout.FULL);
        locationField = new JTextField();
        locationField.setBorder(null);
        locationField.setEditable(false);
        locationField.setOpaque(false);
        formPanel.add(new JLabel("Location:"), FormLayout.LEFT);
        formPanel.add(locationField, FormLayout.RIGHT_FILL);
        modifiedField = new JTextField();
        modifiedField.setBorder(null);
        modifiedField.setEditable(false);
        modifiedField.setOpaque(false);
        formPanel.add(new JLabel("Modified:"), FormLayout.LEFT);
        formPanel.add(modifiedField, FormLayout.RIGHT_FILL);
        sizeField = new JTextField();
        sizeField.setBorder(null);
        sizeField.setEditable(false);
        sizeField.setOpaque(false);
        formPanel.add(new JLabel("Size:"), FormLayout.LEFT);
        formPanel.add(sizeField, FormLayout.RIGHT_FILL);
        JPanel separator2 = new JPanel();
        separator2.setPreferredSize(new Dimension(100, 5));
        separator2.setOpaque(false);
        formPanel.add(separator2, FormLayout.FULL);
        formPanel.add(new JLabel("Attributes:"), FormLayout.LEFT);
        readBox = new JLabel("Read");
        readBox.setFont(readBox.getFont().deriveFont(Font.PLAIN));
        readBox.setForeground(Color.black);
        readBox.setOpaque(false);
        writeBox = new JLabel("Write");
        writeBox.setFont(readBox.getFont());
        writeBox.setForeground(Color.black);
        writeBox.setOpaque(false);
        hiddenBox = new JLabel("Hidden");
        hiddenBox.setFont(readBox.getFont());
        hiddenBox.setForeground(Color.black);
        hiddenBox.setOpaque(false);
        JPanel attributesPanel = new JPanel(new GridLayout(0, 3));
        attributesPanel.setOpaque(false);
        attributesPanel.add(readBox);
        attributesPanel.add(writeBox);
        attributesPanel.add(hiddenBox);
        formPanel.add(attributesPanel, FormLayout.RIGHT_FILL);
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBorder(new EmptyBorder(5, 0, 3, 0));
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(cancelButton.getFont().deriveFont(Font.PLAIN));
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelButtonPressed();
            }
        });
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okButtonPressed();
            }
        });
        getRootPane().setDefaultButton(okButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(panel);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private void okButtonPressed() {
        document.setProperties(document.getURL(), nameField.getText());
        document.getProperties().setValidate(validateField.isSelected());
        setVisible(false);
    }

    private void cancelButtonPressed() {
        setVisible(false);
    }

    /**
	 * Sets the properties for the dialog.
	 *
	 * @param document the document to show the properties for.
	 */
    public void setProperties(ExchangerDocument document) {
        this.document = document;
        setTitle(document.getName() + " Properties");
        nameField.setText(document.getName());
        validateField.setSelected(document.getProperties().validate());
        locationField.setText(document.getURL().toExternalForm());
        locationField.setCaretPosition(0);
        File file = new File(document.getURL().getPath());
        modifiedField.setText(DateFormat.getDateTimeInstance().format(new Date(file.lastModified())));
        sizeField.setText("" + file.length());
        readBox.setIcon(file.canRead() ? SELECTED_ICON : UNSELECTED_ICON);
        writeBox.setIcon(file.canWrite() ? SELECTED_ICON : UNSELECTED_ICON);
        hiddenBox.setIcon(file.isHidden() ? SELECTED_ICON : UNSELECTED_ICON);
    }
}
