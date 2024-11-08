package gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.*;
import tools.files.FileUtils;
import user.*;
import engine.*;

@SuppressWarnings("serial")
public class NewStoreApp extends JInternalFrame implements ActionListener {

    private JTextField storeName;

    private JTextArea storeDescription;

    private File imageFile = null;

    private JTextField viewPath;

    public NewStoreApp(Object tenant) {
        super("Store Application", false, false, false, false);
        this.setLayout(new GridLayout(5, 1));
        JPanel storePane = new JPanel();
        JLabel storeLab = new JLabel("Store Name: ");
        storeName = new JTextField(25);
        storePane.add(storeLab);
        storePane.add(storeName);
        this.add(storePane);
        JPanel imageSelectPane = new JPanel();
        imageSelectPane.add(new JLabel("Logo File"));
        viewPath = new JTextField(50);
        viewPath.setEditable(false);
        JButton startChooser = new JButton("Browse");
        startChooser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "gif", "tiff", "png");
                chooser.setFileFilter(fileFilter);
                int selected = chooser.showOpenDialog(rootPane);
                if (selected == JFileChooser.APPROVE_OPTION) {
                    imageFile = chooser.getSelectedFile();
                    viewPath.setText(imageFile.getAbsolutePath());
                }
            }
        });
        imageSelectPane.add(viewPath);
        imageSelectPane.add(startChooser);
        this.add(imageSelectPane);
        JPanel textBox = new JPanel(new GridLayout(2, 1));
        JLabel descriptor = new JLabel("Store Description");
        storeDescription = new JTextArea(75, 200);
        storeDescription.setLineWrap(true);
        JScrollPane scroller = new JScrollPane(storeDescription);
        textBox.add(descriptor);
        textBox.add(scroller);
        this.add(textBox);
        JPanel buttonPane = new JPanel();
        JButton submit = new JButton("Submit");
        submit.addActionListener(this);
        submit.setActionCommand("SUBMIT");
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        cancel.setActionCommand("CANCEL");
        buttonPane.add(submit);
        buttonPane.add(cancel);
        this.add(buttonPane);
        Vector<Component> tabOrder = new Vector<Component>(4);
        tabOrder.add(storeName);
        tabOrder.add(startChooser);
        tabOrder.add(storeDescription);
        tabOrder.add(submit);
        tabOrder.add(cancel);
        MR_FocusTraversalPolicy newStoreAppPolicy = new MR_FocusTraversalPolicy(tabOrder);
        this.setFocusTraversalPolicy(newStoreAppPolicy);
        this.setFocusTraversalKeysEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("SUBMIT")) {
            if (storeName.getText().equals("")) {
                JOptionPane.showInternalMessageDialog(this, "Store Name must be filled in!", "Incomplete Application Error", JOptionPane.ERROR_MESSAGE);
            } else {
                if (EngineDriver.getSystemUser() instanceof StoreOwner) {
                    Store newStore = new Store((StoreOwner) EngineDriver.getSystemUser(), storeName.getText());
                    if (imageFile != null) {
                        if (FileUtils.checkImgEncomDirectory(imageFile)) {
                            newStore.setIcon(imageFile);
                        } else {
                            try {
                                File token = new File("ENCOM/images/" + imageFile.getName());
                                FileUtils.copyFile(imageFile, token);
                                newStore.setIcon(token);
                            } catch (IOException ex) {
                                Logger.getLogger(NewStoreApp.class.getPackage().getName()).log(Level.WARNING, "IO ERROR SETTING UP ICON", ex);
                            }
                        }
                    }
                    StoreApplication storeApp = new StoreApplication((StoreOwner) EngineDriver.getSystemUser(), newStore, storeDescription.getText());
                    EngineDriver.saveStoreApplicationTo_DB(storeApp);
                    EngineDriver.storeOwnerUI((StoreOwner) EngineDriver.getSystemUser());
                    this.dispose();
                } else if (EngineDriver.getSystemUser() instanceof Customer) {
                    Store newStore = new Store((Customer) EngineDriver.getSystemUser(), storeName.getText());
                    if (imageFile != null) {
                        if (FileUtils.checkImgEncomDirectory(imageFile)) {
                            newStore.setIcon(imageFile);
                        } else {
                            try {
                                File token = new File("ENCOM/images/" + imageFile.getName());
                                FileUtils.copyFile(imageFile, token);
                                newStore.setIcon(token);
                            } catch (IOException ex) {
                                Logger.getLogger(NewStoreApp.class.getPackage().getName()).log(Level.WARNING, "ERROR: Image not transfered during application process.", ex);
                            }
                        }
                    }
                    StoreApplication storeApp = new StoreApplication((Customer) EngineDriver.getSystemUser(), newStore, storeDescription.getText());
                    EngineDriver.saveStoreApplicationTo_DB(storeApp);
                    EngineDriver.mallUI();
                    this.dispose();
                } else if (EngineDriver.isMasterAdmin()) {
                }
            }
        } else if (e.getActionCommand().equals("CANCEL")) {
            EngineDriver.mallUI();
            this.dispose();
        }
    }
}
