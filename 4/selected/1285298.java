package org.nbplugin.jpa.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import org.nbplugin.jpa.execution.JPAExecutor;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import org.nbplugin.jpa.execution.InitializationResult;
import org.nbplugin.jpa.execution.JPAExecutionException;
import org.nbplugin.jpa.execution.JPAExecutionListener;
import org.nbplugin.jpa.execution.JPAExecutorResult;
import org.nbplugin.jpa.tools.ExceptionUtil;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component for JPA editor
 *
 * @author shofmann
 * @version $Revision: 1.14 $
 * last modified by $Author: sebhof $
 */
public class JPAEditorTopComponent extends TopComponent implements JPAExecutionListener {

    private static JPAEditorTopComponent instance;

    private static final String PREFERRED_ID = "JPAEditorTopComponent";

    /** NB io */
    private InputOutput io = IOProvider.getDefault().getIO(JPAExecutor.NAME, false);

    TreeModel treeMdl;

    OutlineModel mdl;

    Outline outline;

    int actualScrollPosition;

    int actualResultSize;

    int actualStartPosition;

    JPAExecutorResult result;

    String lastCommand;

    private JPAEditorTopComponent() {
        initComponents();
        this.jpaEditorPane.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.isControlDown() && (ke.getKeyCode() == KeyEvent.VK_ENTER || ke.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                    execute();
                    actualStartPosition = 0;
                }
                if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    io.getOut().append("Step backward");
                    performStepBackward();
                }
                if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    io.getOut().append("Step forwar");
                    performStepForward();
                }
            }
        });
        this.jList1.setListData(JPAHistoryListManager.getHistory());
        setName(NbBundle.getMessage(JPAEditorTopComponent.class, "CTL_JPAEditorTopComponent"));
        setToolTipText(NbBundle.getMessage(JPAEditorTopComponent.class, "HINT_JPAEditorTopComponent"));
    }

    private void initComponents() {
        jScrollPane2 = new javax.swing.JScrollPane();
        outlineView1 = new org.openide.explorer.view.OutlineView();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jpaEditorPane = new javax.swing.JEditorPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        resultPanel = new org.openide.explorer.view.OutlineView();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        executeButton = new javax.swing.JButton();
        messageLabel = new javax.swing.JLabel();
        progressCircle = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jScrollPane2.setViewportView(outlineView1);
        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                jSplitPane1KeyTyped(evt);
            }
        });
        jpaEditorPane.setBackground(new java.awt.Color(254, 254, 254));
        jpaEditorPane.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                jpaEditorPaneKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(jpaEditorPane);
        jSplitPane1.setTopComponent(jScrollPane1);
        resultPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                resultPanelMouseWheelMoved(evt);
            }
        });
        resultPanel.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                resultPanelKeyTyped(evt);
            }
        });
        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.resultPanel.TabConstraints.tabTitle_1"), resultPanel);
        treeMdl = new JPAResultTreeModel(new JPAResultTreeNode());
        mdl = DefaultOutlineModel.createOutlineModel(treeMdl, new JPAResultRowModel(), true);
        outline = new Outline();
        outline.setRenderDataProvider(new JPAResultTreeRenderer());
        outline.setModel(mdl);
        outline.setRootVisible(false);
        resultPanel.setViewportView(outline);
        resultPanel.setMinimumSize(new Dimension(200, 200));
        jList1.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jList1);
        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.jScrollPane3.TabConstraints.tabTitle_1"), jScrollPane3);
        jSplitPane1.setRightComponent(jTabbedPane1);
        jPanel1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                jSplitPane1KeyTyped(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(executeButton, org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.executeButton.text"));
        executeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeButtonActionPerformed(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(messageLabel, org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.messageLabel.text"));
        progressCircle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbplugin/jpa/ajax-loader.gif")));
        org.openide.awt.Mnemonics.setLocalizedText(progressCircle, org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.progressCircle.text"));
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addGap(54, 54, 54).addComponent(progressCircle).addGap(18, 18, 18).addComponent(messageLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 699, Short.MAX_VALUE).addComponent(executeButton).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(progressCircle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(executeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(messageLabel))).addContainerGap()));
        jPanel2.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                jSplitPane1KeyTyped(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(JPAEditorTopComponent.class, "JPAEditorTopComponent.jButton1.text"));
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup().addContainerGap(774, Short.MAX_VALUE).addComponent(jButton1).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addComponent(jButton1).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 901, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)));
    }

    private void executeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        execute();
        actualStartPosition = 0;
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        stop();
    }

    private void jpaEditorPaneKeyTyped(java.awt.event.KeyEvent evt) {
    }

    private void jSplitPane1KeyTyped(java.awt.event.KeyEvent evt) {
        System.out.println("KeyTyped event ");
        if (evt.isShiftDown() && (evt.getKeyCode() == KeyEvent.VK_ENTER || evt.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            execute();
        }
    }

    private void resultPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
        if (evt.isControlDown()) {
            if (evt.getWheelRotation() < 0) {
                performStepBackward();
            } else {
                performStepForward();
            }
        }
    }

    private void resultPanelKeyTyped(java.awt.event.KeyEvent evt) {
    }

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            String text = jpaEditorPane.getText();
            String textBeforeInsert = text.substring(0, jpaEditorPane.getCaretPosition());
            String textAfterInsert = text.substring(jpaEditorPane.getCaretPosition());
            jpaEditorPane.setText(String.format("%s%s%s", textBeforeInsert, jList1.getSelectedValue(), textAfterInsert));
        }
    }

    private javax.swing.JButton executeButton;

    private javax.swing.JButton jButton1;

    private javax.swing.JList jList1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JSplitPane jSplitPane1;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JEditorPane jpaEditorPane;

    private javax.swing.JLabel messageLabel;

    private org.openide.explorer.view.OutlineView outlineView1;

    private javax.swing.JLabel progressCircle;

    private org.openide.explorer.view.OutlineView resultPanel;

    private JPAExecutor jpaExecutor = null;

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized JPAEditorTopComponent getDefault() {
        if (instance == null) {
            instance = new JPAEditorTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the JPAEditorTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized JPAEditorTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(JPAEditorTopComponent.class.getName()).warning("Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof JPAEditorTopComponent) {
            return (JPAEditorTopComponent) win;
        }
        Logger.getLogger(JPAEditorTopComponent.class.getName()).warning("There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
        this.jpaExecutor.unregisterJPAExecutionListener(this);
        stop();
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

    public void computeResult(JPAExecutorResult result) {
        this.result = result;
        this.progressCircle.setVisible(false);
        if (result.hasErrors()) {
            writeErrorMessage("Error executing Query: " + result.getErrorMessage());
        } else {
            JPAHistoryListManager.addStatementToHistory(lastCommand);
            jList1.setListData(JPAHistoryListManager.getHistory());
            writeSuccessMessage(String.format("Result %s - %s | %s", actualStartPosition, result.getResultList().size() > ResetViewThread.totalAmountOfItemsToDisplay ? actualStartPosition + ResetViewThread.totalAmountOfItemsToDisplay : result.getResultList().size(), result.getResultList().size()));
            actualResultSize = result.getResultList().size();
            JPAResultTreeNode root = new JPAResultTreeNode();
            root.setName("Result");
            root.setValue("Rückgabewerte des ausgefürten Query");
            root.setClass(JPAResultTreeNode.class);
            SwingUtilities.invokeLater(new ResetViewThread(root, this.result, treeMdl, mdl, outline, actualStartPosition));
            jTabbedPane1.setSelectedIndex(0);
        }
    }

    public void performStepForward() {
        this.progressCircle.setVisible(true);
        if (actualResultSize > ResetViewThread.totalAmountOfItemsToDisplay) {
            actualStartPosition = actualStartPosition + ResetViewThread.totalAmountOfItemsToDisplay;
            if (actualStartPosition >= actualResultSize) {
                actualStartPosition = actualResultSize - ResetViewThread.totalAmountOfItemsToDisplay;
            }
            io.getOut().append("Actual Start " + actualStartPosition + "\n");
            computeResult(result);
        }
        this.progressCircle.setVisible(false);
    }

    public void performStepBackward() {
        this.progressCircle.setVisible(true);
        actualStartPosition = actualStartPosition - ResetViewThread.totalAmountOfItemsToDisplay;
        if (actualStartPosition < 0) {
            actualStartPosition = 0;
        }
        io.getOut().append("Actual Start " + actualStartPosition + "\n");
        computeResult(result);
        this.progressCircle.setVisible(false);
    }

    public void stopped() {
        writeMessage("JPAExecutor stopped");
    }

    public void initialized(InitializationResult initializationResult) {
        if (initializationResult.hasErrors()) {
            io.getErr().write("Error initializing EntityManager\n");
            initializationResult.getThrowable().printStackTrace(io.getErr());
            io.getErr().flush();
            writeErrorMessage("Unable to initialize EntityManager :" + ExceptionUtil.findRootCause(initializationResult.getThrowable()).getMessage());
        } else {
            writeMessage("Ready to execute queries");
            this.setAllEnabled(true);
        }
        this.progressCircle.setVisible(false);
    }

    static final class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return JPAEditorTopComponent.getDefault();
        }
    }

    private void setAllEnabled(boolean enabled) {
        this.jpaEditorPane.setEnabled(enabled);
        this.jpaEditorPane.setContentType("text/x-sql");
        this.executeButton.setEnabled(enabled);
        this.jButton1.setEnabled(enabled);
        this.jpaEditorPane.requestFocus();
    }

    public void setJPAExecutor(JPAExecutor executor) {
        if (this.jpaExecutor != null) {
            this.jpaExecutor.stop();
        }
        this.progressCircle.setVisible(true);
        writeMessage("Trying to find and initialize Persistence Unit...");
        this.setAllEnabled(false);
        this.jpaExecutor = executor;
        this.jpaExecutor.registerJPAExecutionListener(this);
        try {
            this.jpaExecutor.init();
        } catch (JPAExecutionException ex) {
            writeErrorMessage(ex.getMessage());
            Exceptions.printStackTrace(ex);
        }
    }

    private void execute() {
        lastCommand = null;
        lastCommand = this.jpaEditorPane.getSelectedText();
        if (lastCommand == null || lastCommand.trim().equals("")) {
            lastCommand = this.jpaEditorPane.getText();
        }
        if (lastCommand == null) {
            return;
        } else {
            lastCommand = lastCommand.trim();
        }
        writeMessage("Executing command...");
        this.progressCircle.setVisible(true);
        this.jpaExecutor.executeQuery(lastCommand);
    }

    private void stop() {
        this.jpaExecutor.stop();
        setAllEnabled(false);
    }

    private void writeMessage(String message) {
        this.messageLabel.setForeground(Color.black);
        this.messageLabel.setText(message);
    }

    private void writeErrorMessage(String message) {
        this.messageLabel.setForeground(Color.red);
        this.messageLabel.setText(message);
    }

    private void writeSuccessMessage(String message) {
        this.messageLabel.setForeground(Color.green);
        this.messageLabel.setText(message);
    }

    public void keyReleased(KeyEvent ke) {
        System.out.println("KeyTyped event ");
        if (ke.isShiftDown() && (ke.getKeyCode() == KeyEvent.VK_ENTER || ke.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            execute();
        }
    }
}
