package gov.nasa.runjpf.topicpublisher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent.EventType;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;

/**
 * Top component which displays something.
 */
public final class OutputWindow extends TopComponent {

    private static OutputWindow instance;

    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "gov/nasa/runjpf/output/vjpicon.png";

    private static final String PREFERRED_ID = "OutputWindow";

    private TopicOutputParser linkparser = new TopicOutputParser();

    private boolean isSaveable = false;

    private OutputWindow() {
        initComponents();
        setName(NbBundle.getMessage(OutputWindow.class, "CTL_OutputWindow"));
        setToolTipText(NbBundle.getMessage(OutputWindow.class, "HINT_OutputWindow"));
        setIcon(Utilities.loadImage(ICON_PATH, true));
    }

    private void initComponents() {
        outputSplitPane = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        topicList = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        topicTextArea = new javax.swing.JTextPane();
        topicList.setModel(topicListModel);
        topicList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                popupMenu(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                popupMenu(evt);
            }
        });
        topicList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                topicListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(topicList);
        outputSplitPane.setLeftComponent(jScrollPane1);
        topicTextArea.setContentType(org.openide.util.NbBundle.getMessage(OutputWindow.class, "OutputWindow.topicTextArea.contentType"));
        topicTextArea.setEditable(false);
        topicTextArea.setFont(UIManager.getFont("controlFont"));
        topicTextArea.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {

            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                topicTextAreaHyperlinkUpdate(evt);
            }
        });
        jScrollPane2.setViewportView(topicTextArea);
        outputSplitPane.setRightComponent(jScrollPane2);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(outputSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 673, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(outputSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE));
    }

    private void topicListValueChanged(javax.swing.event.ListSelectionEvent evt) {
        String topic = (String) topicList.getSelectedValue();
        if (topic != null) {
            String text = topicListModel.getTopics().get(topic).toString();
            if (linkparser != null) text = linkparser.parse(text);
            topicTextArea.setText("<pre>" + text + "</pre>");
        }
    }

    private void topicTextAreaHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
        if (evt.getEventType() == EventType.ACTIVATED) {
            String[] target = evt.getDescription().split(":");
            String path = target[0];
            int lineNum = Integer.parseInt(target[1]);
            openEditor(path, lineNum);
        }
    }

    private void popupMenu(java.awt.event.MouseEvent evt) {
        if (isSaveable && evt.isPopupTrigger()) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem item = new JMenuItem("Save Result...");
            item.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    int result = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
                    if (result != JFileChooser.APPROVE_OPTION) return;
                    final File file = chooser.getSelectedFile();
                    if (file.exists() && !approveOverwrite(file.getName())) return;
                    Runnable saveFile = new Runnable() {

                        public void run() {
                            try {
                                file.createNewFile();
                                PrintWriter out = new PrintWriter(file);
                                String time = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
                                out.print("Created: ");
                                out.println(time);
                                out.print("Created By: ");
                                out.println(System.getProperty("user.name"));
                                for (Entry<String, Topic> e : topicListModel.getTopics().entrySet()) out.println(e.getValue().getContent());
                                if (out.checkError()) {
                                    NotifyDescriptor.Message error = new NotifyDescriptor.Message("There was an error saving the results.", NotifyDescriptor.ERROR_MESSAGE);
                                    DialogDisplayer.getDefault().notifyLater(error);
                                }
                                out.close();
                            } catch (IOException ex) {
                                String message = "File: " + file.getName() + " could not created.";
                                NotifyDescriptor.Message error = new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE);
                                DialogDisplayer.getDefault().notifyLater(error);
                                return;
                            }
                        }
                    };
                    new Thread(saveFile).start();
                }
            });
            popup.add(item);
            popup.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    private boolean approveOverwrite(String fileName) {
        String message = "File: " + fileName + " already exists.\n" + "Are you sure that you want to overwrite its contents?";
        NotifyDescriptor.Confirmation conf = new NotifyDescriptor.Confirmation(message);
        return DialogDisplayer.getDefault().notify(conf) == NotifyDescriptor.YES_OPTION;
    }

    private void openEditor(String path, int line) {
        FileObject srcFile = linkparser.getSource(path);
        if (srcFile == null) {
            warning("The following source file was not found:\n" + path);
        } else {
            try {
                LineCookie lc = DataObject.find(srcFile).getCookie(LineCookie.class);
                Line l = lc.getLineSet().getOriginal(line - 1);
                l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private TopicListModel topicListModel = new TopicListModel();

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JSplitPane outputSplitPane;

    private javax.swing.JList topicList;

    private javax.swing.JTextPane topicTextArea;

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized OutputWindow getDefault() {
        if (instance == null) {
            instance = new OutputWindow();
        }
        return instance;
    }

    /**
     * Obtain the OutputWindow instance. Never call {@link #getDefault} directly!
     */
    public static synchronized OutputWindow findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(OutputWindow.class.getName()).warning("Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof OutputWindow) {
            return (OutputWindow) win;
        }
        Logger.getLogger(OutputWindow.class.getName()).warning("There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    public void showResults(Map<String, Topic> topics) {
        reset();
        isSaveable = true;
        topicListModel.setTopics(topics);
        topicList.setSelectedIndex(0);
        open();
        toFront();
        setVisible(true);
    }

    public void reset() {
        isSaveable = false;
        topicListModel.clear();
        topicTextArea.setText("");
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    static final class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return OutputWindow.getDefault();
        }
    }

    private class TopicListModel extends AbstractListModel {

        private Map<String, Topic> topics = new HashMap<String, Topic>();

        public void setTopics(Map<String, Topic> topics) {
            this.topics = topics;
            fireIntervalAdded(this, 0, topics.size());
        }

        public void clear() {
            int size = getSize();
            topics.clear();
            fireIntervalRemoved(this, 0, size < 0 ? 0 : size);
        }

        public Map<String, Topic> getTopics() {
            return topics;
        }

        public int getSize() {
            return topics.size();
        }

        public Object getElementAt(int index) {
            return topics.keySet().toArray()[index];
        }
    }

    private static void warning(String msg) {
        NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }
}
