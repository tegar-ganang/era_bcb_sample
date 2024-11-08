package net.sf.mailsomething.gui.mail;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.*;
import net.sf.mailsomething.gui.GridLayout2;
import net.sf.mailsomething.gui.StatusBar;
import net.sf.mailsomething.gui.wizard.DialogContext;
import net.sf.mailsomething.gui.wizard.DialogScreen;
import net.sf.mailsomething.mail.*;
import net.sf.mailsomething.mail.parsers.*;

/**
 * 
 * An import mail dialog.
 * 
 * 
 */
public class ImportMailDialog extends JPanel {

    private File[] files;

    private MessageHolder mailbox;

    private DialogBottomLine dialogBottomLine;

    public ImportMailDialog() {
        super();
        dialogBottomLine = new DialogBottomLine();
        DialogScreen panel = new InitDialog();
        JPanel container = new JPanel(new BorderLayout());
        setLayout(new BorderLayout());
        add(container, BorderLayout.CENTER);
        container.add(panel.getJPanel());
        dialogBottomLine.addActionListener(new DialogController(panel, container));
        add(new JPanel(), BorderLayout.SOUTH);
        add(dialogBottomLine, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(400, 300));
    }

    class InitDialog extends JPanel implements DialogScreen {

        JRadioButton mailFiles, anotherProgram, anotherMailsomething;

        private String dialogText = "This dialog will guide u through the process of importing " + "mails to a specfic mailbox. Source can be either mail-files which " + "is formatted as the rfc822 standard, another program or another " + "instance of MailSomething. Choose ur option below and press next";

        public InitDialog() {
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = GridBagConstraints.RELATIVE;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTH;
            setLayout(new GridLayout(2, 0));
            JTextArea textArea = new JTextArea(dialogText, 5, 50);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setBorder(new EmptyBorder(25, 20, 15, 20));
            textArea.setBackground(getBackground());
            add(textArea);
            JPanel boxPanel = new JPanel();
            boxPanel.setLayout(layout);
            ButtonGroup group = new ButtonGroup();
            mailFiles = new JRadioButton("Mail files (*.eml)", true);
            mailFiles.setBackground(getBackground());
            group.add(mailFiles);
            layout.setConstraints(mailFiles, constraints);
            boxPanel.add(mailFiles);
            anotherProgram = new JRadioButton("Another program (outlook, mozilla mail)", false);
            anotherProgram.setBackground(getBackground());
            group.add(anotherProgram);
            layout.setConstraints(anotherProgram, constraints);
            boxPanel.add(anotherProgram);
            anotherMailsomething = new JRadioButton("Another mailsomething user", false);
            anotherMailsomething.setBackground(getBackground());
            group.add(anotherMailsomething);
            layout.setConstraints(anotherMailsomething, constraints);
            boxPanel.add(anotherMailsomething);
            add(boxPanel);
        }

        public void setDialogContext(DialogContext context) {
        }

        public void setNextDialog(DialogScreen nextdialog) {
        }

        public void setShowing() {
        }

        public DialogScreen getNextDialog() {
            if (mailFiles.isSelected()) {
                return new MailFilesSelectionDialog();
            } else {
                return new NoFeatureDialog();
            }
        }

        public JPanel getJPanel() {
            return this;
        }

        public void addActionListener(ActionListener listener) {
        }
    }

    class MailFilesSelectionDialog extends JPanel implements DialogScreen {

        private JList list;

        private String dialog = "Press field below to add or remove files to be imported. " + "Note, they will all be imported to the same mailbox (which u must " + "choose in next dialog), so if u want to import mails to several mailboxes, " + "u will need to run this dialog again.";

        private boolean filesSelected = false;

        public MailFilesSelectionDialog() {
            setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea(dialog, 5, 50);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setBorder(new EmptyBorder(25, 20, 15, 20));
            textArea.setBackground(getBackground());
            add(textArea, BorderLayout.NORTH);
            list = new JList();
            list.setBorder(new LineBorder(Color.red.darker().darker()));
            list.addMouseListener(new ClickListener());
            list.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JScrollPane pane = new JScrollPane(list);
            pane.setBorder(new EmptyBorder(0, 30, 10, 30));
            pane.setBackground(getBackground());
            add(pane, BorderLayout.CENTER);
        }

        public DialogScreen getNextDialog() {
            return new ChooseMailboxDialog();
        }

        public void setDialogContext(DialogContext context) {
        }

        public void setNextDialog(DialogScreen nextdialog) {
        }

        public JPanel getJPanel() {
            return this;
        }

        public void setShowing() {
        }

        public void addActionListener(ActionListener listener) {
        }

        class ClickListener extends MouseAdapter {

            public ClickListener() {
                super();
            }

            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
                int val = chooser.showOpenDialog(ImportMailDialog.this);
                if (val == JFileChooser.APPROVE_OPTION) {
                    files = chooser.getSelectedFiles();
                    if (files != null) {
                        String[] fileNames = new String[files.length];
                        for (int i = 0; i < files.length; i++) fileNames[i] = files[i].getName();
                        if (fileNames.length > 0) dialogBottomLine.nextButton.setEnabled(true);
                        list.setListData(fileNames);
                        list.revalidate();
                        list.repaint();
                    }
                }
            }
        }
    }

    class DialogBottomLine extends JPanel {

        public JButton cancelButton, backButton, nextButton;

        public DialogBottomLine() {
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setLayout(new GridLayout(0, 4));
            cancelButton = new JButton("Cancel");
            cancelButton.setActionCommand("Cancel");
            add(cancelButton);
            add(new JPanel());
            backButton = new JButton("Back");
            backButton.setActionCommand("Back");
            add(backButton);
            nextButton = new JButton("Next");
            nextButton.setActionCommand("Next");
            add(nextButton);
        }

        public void addActionListener(ActionListener listener) {
            cancelButton.addActionListener(listener);
            nextButton.addActionListener(listener);
            backButton.addActionListener(listener);
        }
    }

    class DialogController implements ActionListener {

        private Vector traceBack;

        private JPanel container;

        private DialogScreen dialog;

        private int current = 0;

        public DialogController(DialogScreen initDialog, JPanel container) {
            traceBack = new Vector();
            this.container = container;
            dialog = initDialog;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "Next") {
                DialogScreen next = dialog.getNextDialog();
                if (next == null) return;
                traceBack.add(dialog);
                dialog = next;
                dialogBottomLine.nextButton.setEnabled(false);
                JPanel panel = dialog.getJPanel();
                container.removeAll();
                container.add(panel);
                container.revalidate();
                container.repaint();
                dialog.setShowing();
                current++;
            } else if (e.getActionCommand() == "Back") {
                if (current < 1) return;
                DialogScreen dialog = (DialogScreen) traceBack.elementAt(--current);
                this.dialog = dialog;
                JPanel panel = dialog.getJPanel();
                container.removeAll();
                container.add(panel);
                container.revalidate();
                container.repaint();
                dialogBottomLine.nextButton.setEnabled(true);
            } else if (e.getActionCommand() == "Cancel") {
                Window window = SwingUtilities.getWindowAncestor(ImportMailDialog.this);
                window.hide();
                window.dispose();
            }
        }
    }

    class NoFeatureDialog extends JPanel implements DialogScreen {

        public NoFeatureDialog() {
            setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea("This feature isnt implemented yet", 5, 50);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setBorder(new EmptyBorder(25, 20, 15, 20));
            textArea.setBackground(getBackground());
            add(textArea, BorderLayout.CENTER);
        }

        public DialogScreen getNextDialog() {
            return new ImportingScreen();
        }

        public JPanel getJPanel() {
            return this;
        }

        public void addActionListener(ActionListener listener) {
        }

        public void setDialogContext(DialogContext context) {
        }

        public void setNextDialog(DialogScreen nextdialog) {
        }

        public void setShowing() {
        }
    }

    /**
	 * Use the chooseMailboxDialog ie lib.gui.mail.ChooseMailboxDialog instead.
	 * 
	 */
    class ChooseMailboxDialog extends JPanel implements DialogScreen {

        private String dialog = "Select the mailbox where imported mails will be placed, " + "and press next to start the import. If u choose a imap-mailbox " + "the mailbox will be synchronized with server.";

        public ChooseMailboxDialog() {
            setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea(dialog, 5, 50);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setBorder(new EmptyBorder(25, 20, 15, 20));
            textArea.setBackground(getBackground());
            add(textArea, BorderLayout.NORTH);
            JPanel treePanel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints con = new GridBagConstraints();
            con.gridx = 0;
            con.gridy = GridBagConstraints.RELATIVE;
            con.weighty = 0;
            con.fill = GridBagConstraints.HORIZONTAL;
            con.anchor = GridBagConstraints.NORTHWEST;
            treePanel.setLayout(layout);
            treePanel.setBackground(Color.white);
            MailService mailuser = MailService.getInstance();
            MailAccount[] accounts = mailuser.getMailAccounts();
            SListener listener = new SListener();
            for (int i = 0; i < accounts.length; i++) {
                MailboxTree tree = new MailboxTree(accounts[i]);
                tree.addTreeSelectionListener(listener);
                tree.setShowMessageCount(false);
                layout.setConstraints(tree, con);
                treePanel.add(tree);
            }
            JScrollPane scrollPane = new JScrollPane(treePanel);
            scrollPane.setBackground(getBackground());
            scrollPane.setBorder(new CompoundBorder(new EmptyBorder(0, 30, 10, 30), new LineBorder(Color.red.darker().darker())));
            add(scrollPane, BorderLayout.CENTER);
        }

        public void setShowing() {
        }

        class SListener implements TreeSelectionListener {

            public SListener() {
            }

            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getNewLeadSelectionPath();
                Object o = ((MailboxTreeNode) path.getLastPathComponent()).getMailbox();
                try {
                    MessageHolder m = (MessageHolder) o;
                    mailbox = m;
                    dialogBottomLine.nextButton.setEnabled(true);
                } catch (ClassCastException f) {
                    mailbox = null;
                }
            }
        }

        public JPanel getJPanel() {
            return this;
        }

        public DialogScreen getNextDialog() {
            return new ImportingScreen();
        }

        public void setNextDialog(DialogScreen nextdialog) {
        }

        public void setDialogContext(DialogContext context) {
        }

        public void addActionListener(ActionListener listener) {
        }
    }

    class ImportingScreen extends JPanel implements DialogScreen, Runnable {

        JLabel label;

        StatusBar bar;

        public ImportingScreen() {
            JPanel panel = new JPanel(new GridLayout2(2, 1));
            label = new JLabel("Importing....");
            label.setBackground(getBackground());
            bar = new StatusBar();
            bar.workStarted(files.length * 10);
            panel.add(label);
            panel.add(bar);
            setLayout(new FlowLayout(FlowLayout.CENTER));
            add(panel);
        }

        public void run() {
            for (int i = 0; i < files.length; i++) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException f) {
                }
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    BufferedReader reader = new BufferedReader(new FileReader(files[i]));
                    int n;
                    while ((n = reader.read()) != -1) stream.write(n);
                    byte[] array = stream.toByteArray();
                    Message m = MailService.createMessage();
                    m.setRemote(false);
                    MailDecoder.decodeMail(array, m);
                    mailbox.addMessage(m);
                } catch (IOException f) {
                }
                bar.workProgress((i + 1) * 10);
            }
            label.setText("<html><body>Done!" + "<br>U can now locate the imported <br>" + "messages in the choosen folder");
        }

        public JPanel getJPanel() {
            return this;
        }

        public DialogScreen getNextDialog() {
            return null;
        }

        public void setNextDialog(DialogScreen nextdialog) {
        }

        public void setDialogContext(DialogContext context) {
        }

        public void addActionListener(ActionListener listener) {
        }

        public void setShowing() {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    public static void main(String[] args) {
        new ImportMailDialog();
    }
}
