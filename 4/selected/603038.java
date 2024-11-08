package quickdisktest;

import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import org.jdesktop.application.Action;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.TrayIcon;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Timer;
import java.io.File;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import myutils.ErrDialog;
import myutils.ErrUtils;
import myutils.GuiUtils;
import myutils.HumanReadableDuration;
import myutils.HumanReadableSize;
import myutils.UserNotification;
import myutils.WorkerThreadBase;

public class ProgressWindow extends javax.swing.JFrame implements KeyEventDispatcher {

    public enum ClosingStatus {

        CR_CANCELLED_BY_USER, CR_ERROR, CR_WRITING_COMPLETE, CR_SKIP_MANUAL_RECONNECTION, CR_VERIFICATION_COMPLETE
    }

    private QuickDiskTestView parent;

    private Timer timer;

    private static final int TIMER_PERIOD_MS = 2 * 1000;

    private long elapsedTimeMs;

    private boolean isPaused;

    private boolean isCancelled;

    private WorkerThreadBase worker;

    private final boolean isVerifyMode;

    private final File testDataFilesPath;

    private UserNotification notification = new UserNotification();

    public ProgressWindow(QuickDiskTestView parent, File testDataFilesPath, boolean isVerifyMode, TestDataConfig writeModeConfig, TestDataGenerator testDataGenerator) {
        String errMsgPrefix = "";
        try {
            if (isVerifyMode) {
                worker = new WorkerThreadVerifier(testDataFilesPath, testDataGenerator);
            } else {
                worker = new WorkerThreadWriter(testDataFilesPath, writeModeConfig, testDataGenerator);
            }
            errMsgPrefix = "Error initialising the progress window: ";
            initComponents();
            this.parent = parent;
            this.testDataFilesPath = testDataFilesPath;
            this.isVerifyMode = isVerifyMode;
            setLocationRelativeTo(parent.getFrame());
            setIconImage(Common.appIcon);
            updateTitle();
            jLabelPaused.setVisible(isPaused);
            ActionListener timerTaskPerformer = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    timerTick(TIMER_PERIOD_MS);
                }
            };
            updateProgress();
            timer = new Timer(TIMER_PERIOD_MS, timerTaskPerformer);
            GuiUtils.disallowVerticalResizing((javax.swing.JFrame) this, getHeight());
            jToggleButtonPause.requestFocusInWindow();
            jCheckBoxSkipReconnection.setEnabled(!isVerifyMode);
            jLabelSkipReconnection.setEnabled(!isVerifyMode);
            jCheckBoxSkipReconnection.setSelected(false);
            GuiUtils.copyReadOnlyTextFieldBackgroundColourFromLabel(jTextFieldStartPath, jLabelCurrentPath);
            GuiUtils.copyReadOnlyTextFieldBackgroundColourFromLabel(jTextFieldTargetSize, jLabelCurrentPath);
            GuiUtils.copyReadOnlyTextFieldBackgroundColourFromLabel(jTextFieldElapsedTime, jLabelCurrentPath);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldStartPath);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldTargetSize);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldElapsedTime);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldTotalSize);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldEstimatedTime);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldSpeed);
            GuiUtils.addStandardPopupMenuAndUndoSupport(jTextFieldCurrentPath);
            PropertyChangeListener propChangeListener = new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    assert "state".equals(evt.getPropertyName()) && evt.getNewValue().equals("done") : ErrUtils.assertionFailed();
                    processingDone();
                }
            };
            worker.setPropertyChangeListener(propChangeListener);
            setVisible(true);
        } catch (Throwable e) {
            dispose();
            throw ErrUtils.asRuntimeException(e, errMsgPrefix);
        }
    }

    public void init() {
        try {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
            timer.start();
            worker.start();
        } catch (Throwable e) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
            dispose();
            throw myutils.ErrUtils.asRuntimeException(e, "");
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jTextFieldStartPath = new javax.swing.JTextField();
        jButtonOpenLocation = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jTextFieldTotalSize = new javax.swing.JTextField();
        jTextFieldTargetSize = new javax.swing.JTextField();
        jTextFieldEstimatedTime = new javax.swing.JTextField();
        jTextFieldElapsedTime = new javax.swing.JTextField();
        jTextFieldSpeed = new javax.swing.JTextField();
        jLabelSpeed = new javax.swing.JLabel();
        jLabelCurrentPath = new javax.swing.JLabel();
        jTextFieldCurrentPath = new javax.swing.JTextField();
        jProgressBarFill = new javax.swing.JProgressBar();
        jCheckBoxSkipReconnection = new javax.swing.JCheckBox();
        jLabelSkipReconnection = new javax.swing.JLabel();
        jToggleButtonPause = new javax.swing.JToggleButton();
        jLabelPaused = new javax.swing.JLabel();
        jButtonCancel = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(quickdisktest.QuickDiskTestApp.class).getContext().getResourceMap(ProgressWindow.class);
        setTitle(resourceMap.getString("Form.title"));
        setMinimumSize(new java.awt.Dimension(396, 320));
        setName("Form");
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        jTextFieldStartPath.setBackground(resourceMap.getColor("jTextFieldStartPath.background"));
        jTextFieldStartPath.setEditable(false);
        jTextFieldStartPath.setText(resourceMap.getString("jTextFieldStartPath.text"));
        jTextFieldStartPath.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextFieldStartPath.setMinimumSize(new java.awt.Dimension(0, 26));
        jTextFieldStartPath.setName("jTextFieldStartPath");
        jTextFieldStartPath.setPreferredSize(new java.awt.Dimension(110, 26));
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(quickdisktest.QuickDiskTestApp.class).getContext().getActionMap(ProgressWindow.class, this);
        jButtonOpenLocation.setAction(actionMap.get("openTestDataLocation"));
        jButtonOpenLocation.setMnemonic('d');
        jButtonOpenLocation.setText(resourceMap.getString("jButtonOpenLocation.text"));
        jButtonOpenLocation.setToolTipText(null);
        jButtonOpenLocation.setName("jButtonOpenLocation");
        jPanel1.setName("jPanel1");
        jTextFieldTotalSize.setEditable(false);
        jTextFieldTotalSize.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldTotalSize.setName("jTextFieldTotalSize");
        jTextFieldTargetSize.setBackground(resourceMap.getColor("jTextFieldStartPath.background"));
        jTextFieldTargetSize.setEditable(false);
        jTextFieldTargetSize.setText(resourceMap.getString("jTextFieldTargetSize.text"));
        jTextFieldTargetSize.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextFieldTargetSize.setMinimumSize(new java.awt.Dimension(0, 26));
        jTextFieldTargetSize.setName("jTextFieldTargetSize");
        jTextFieldTargetSize.setPreferredSize(new java.awt.Dimension(195, 26));
        jTextFieldEstimatedTime.setEditable(false);
        jTextFieldEstimatedTime.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldEstimatedTime.setName("jTextFieldEstimatedTime");
        jTextFieldElapsedTime.setBackground(resourceMap.getColor("jTextFieldStartPath.background"));
        jTextFieldElapsedTime.setEditable(false);
        jTextFieldElapsedTime.setText(resourceMap.getString("jTextFieldElapsedTime.text"));
        jTextFieldElapsedTime.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextFieldElapsedTime.setMinimumSize(new java.awt.Dimension(0, 26));
        jTextFieldElapsedTime.setName("jTextFieldElapsedTime");
        jTextFieldElapsedTime.setPreferredSize(new java.awt.Dimension(166, 26));
        jTextFieldSpeed.setEditable(false);
        jTextFieldSpeed.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextFieldSpeed.setName("jTextFieldSpeed");
        jLabelSpeed.setText(resourceMap.getString("jLabelSpeed.text"));
        jLabelSpeed.setName("jLabelSpeed");
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTextFieldTotalSize, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jTextFieldEstimatedTime, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE).addComponent(jTextFieldSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabelSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE).addComponent(jTextFieldTargetSize, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE).addComponent(jTextFieldElapsedTime, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE)).addContainerGap()));
        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jTextFieldEstimatedTime, jTextFieldSpeed, jTextFieldTotalSize });
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jTextFieldTotalSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jTextFieldTargetSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jTextFieldEstimatedTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jTextFieldElapsedTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabelSpeed).addComponent(jTextFieldSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))));
        jLabelCurrentPath.setText(resourceMap.getString("jLabelCurrentPath.text"));
        jLabelCurrentPath.setName("jLabelCurrentPath");
        jTextFieldCurrentPath.setEditable(false);
        jTextFieldCurrentPath.setText(resourceMap.getString("jTextFieldCurrentPath.text"));
        jTextFieldCurrentPath.setName("jTextFieldCurrentPath");
        jProgressBarFill.setName("jProgressBarFill");
        jProgressBarFill.setRequestFocusEnabled(false);
        jProgressBarFill.setString(resourceMap.getString("jProgressBarFill.string"));
        jProgressBarFill.setStringPainted(true);
        jCheckBoxSkipReconnection.setMnemonic('s');
        jCheckBoxSkipReconnection.setText(resourceMap.getString("jCheckBoxSkipReconnection.text"));
        jCheckBoxSkipReconnection.setName("jCheckBoxSkipReconnection");
        jLabelSkipReconnection.setText(resourceMap.getString("jLabelSkipReconnection.text"));
        jLabelSkipReconnection.setName("jLabelSkipReconnection");
        jToggleButtonPause.setAction(actionMap.get("pauseTest"));
        jToggleButtonPause.setMnemonic('p');
        jToggleButtonPause.setText(resourceMap.getString("jToggleButtonPause.text"));
        jToggleButtonPause.setToolTipText(null);
        jToggleButtonPause.setName("jToggleButtonPause");
        jLabelPaused.setText(resourceMap.getString("jLabelPaused.text"));
        jLabelPaused.setName("jLabelPaused");
        jButtonCancel.setAction(actionMap.get("cancelButtonPressed"));
        jButtonCancel.setText(resourceMap.getString("jButtonCancel.text"));
        jButtonCancel.setToolTipText(null);
        jButtonCancel.setName("jButtonCancel");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()).addGroup(layout.createSequentialGroup().addGap(21, 21, 21).addComponent(jLabelSkipReconnection, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(layout.createSequentialGroup().addComponent(jToggleButtonPause, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabelPaused, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 112, Short.MAX_VALUE).addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(layout.createSequentialGroup().addComponent(jCheckBoxSkipReconnection, javax.swing.GroupLayout.PREFERRED_SIZE, 332, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jProgressBarFill, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE).addComponent(jTextFieldCurrentPath, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)).addGap(22, 22, 22)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(jTextFieldStartPath, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButtonOpenLocation, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(layout.createSequentialGroup().addComponent(jLabelCurrentPath, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()))));
        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jButtonCancel, jToggleButtonPause });
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButtonOpenLocation).addComponent(jTextFieldStartPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabelCurrentPath).addGap(3, 3, 3).addComponent(jTextFieldCurrentPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jProgressBarFill, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jCheckBoxSkipReconnection).addGap(5, 5, 5).addComponent(jLabelSkipReconnection).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jToggleButtonPause).addComponent(jLabelPaused).addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        pack();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        cancelButtonPressed();
    }

    public void timerTick(int elapsedTickTimeInMs) {
        if (!isPaused) {
            elapsedTimeMs += elapsedTickTimeInMs;
            worker.timerTick(elapsedTickTimeInMs);
        }
        updateProgress();
    }

    private static class ProgressInformation {

        public static long UNKNOWN = -1;

        public static long TOO_LONG = -2;

        public String currentFilename;

        public long totalSize;

        public long targetSize;

        public long processedDataSize;

        public int calculatePercentage() {
            if (targetSize == 0) {
                return 0;
            } else {
                return (int) (totalSize * 100.0 / targetSize);
            }
        }

        public long calculateEstimatedRemainingTime(long elapsedTimeInSeconds) {
            if (elapsedTimeInSeconds < 2 || totalSize < 1 || totalSize >= targetSize) {
                return ProgressInformation.UNKNOWN;
            } else {
                double v = ((double) targetSize - totalSize) * elapsedTimeInSeconds / processedDataSize;
                if (v <= 0 || v > Long.MAX_VALUE) {
                    return ProgressInformation.UNKNOWN;
                } else {
                    return (long) v;
                }
            }
        }
    }

    private void updateVerifyProgress(ProgressUpdateVerifying progressVerifier, ProgressInformation progressInfo) {
        progressInfo.currentFilename = progressVerifier.currentFilename;
        progressInfo.processedDataSize = progressVerifier.processedDataSize;
        progressInfo.totalSize = progressVerifier.processedDataSize;
        progressInfo.targetSize = progressVerifier.targetSize;
    }

    private void updateCreateProgress(final ProgressUpdateWriting progressWriter, ProgressInformation progressInfo) {
        progressInfo.currentFilename = progressWriter.currentFilename;
        progressInfo.processedDataSize = progressWriter.processedDataSize;
        progressInfo.totalSize = progressWriter.currentDataSize;
        progressInfo.targetSize = progressWriter.targetDataSize;
    }

    public final void updateProgress() {
        WorkerThreadBase.ProgressUpdateBase progressUpdate = worker.getCurrentProgress();
        ProgressInformation progressInfo = new ProgressInformation();
        if (isVerifyMode) updateVerifyProgress((ProgressUpdateVerifying) progressUpdate, progressInfo); else updateCreateProgress((ProgressUpdateWriting) progressUpdate, progressInfo);
        setTextIfChanged(jTextFieldCurrentPath, progressInfo.currentFilename);
        jTextFieldCurrentPath.setToolTipText(progressInfo.currentFilename);
        String totalSizeStr = HumanReadableSize.format(progressInfo.totalSize, Common.HRS_DEC_COUNT);
        setTextIfChanged(jTextFieldTotalSize, totalSizeStr);
        setTextIfChanged(jTextFieldTargetSize, String.format(isVerifyMode ? "of %s verified" : "of %s created", HumanReadableSize.format(progressInfo.targetSize, Common.HRS_DEC_COUNT)));
        int displayedPercent = Math.max(progressInfo.calculatePercentage(), 0);
        displayedPercent = Math.min(displayedPercent, 99);
        if (jProgressBarFill.getValue() != displayedPercent) jProgressBarFill.setValue(displayedPercent);
        String pausedSuffix = isPaused ? " (paused)" : "";
        String progressMsg = displayedPercent + Common.PERCENT_SUFFIX + pausedSuffix;
        if (!progressMsg.equals(jProgressBarFill.getString())) jProgressBarFill.setString(progressMsg);
        final long elapsedSec = elapsedTimeMs / 1000;
        String estimatedRemaining = String.format("est. remaining, elapsed %s", HumanReadableDuration.formatSeconds(elapsedSec));
        setTextIfChanged(jTextFieldElapsedTime, estimatedRemaining);
        jTextFieldElapsedTime.setToolTipText(estimatedRemaining);
        if (elapsedSec == 0) {
            setTextIfChanged(jTextFieldSpeed, "");
        } else {
            setTextIfChanged(jTextFieldSpeed, HumanReadableSize.format(progressInfo.processedDataSize / elapsedSec, Common.HRS_DEC_COUNT) + "/" + HumanReadableDuration.getSecAbbrev());
        }
        long estimatedRemainingTime = progressInfo.calculateEstimatedRemainingTime(elapsedSec);
        String estimatedTimeStr;
        if (estimatedRemainingTime == ProgressInformation.UNKNOWN) {
            estimatedTimeStr = "";
        } else if (estimatedRemainingTime == ProgressInformation.TOO_LONG) {
            estimatedTimeStr = "<too long>";
        } else {
            estimatedTimeStr = HumanReadableDuration.formatSeconds(estimatedRemainingTime);
        }
        setTextIfChanged(jTextFieldEstimatedTime, estimatedTimeStr);
        jTextFieldEstimatedTime.setToolTipText(estimatedTimeStr);
    }

    private void processingDone() {
        ClosingStatus status = ClosingStatus.CR_ERROR;
        try {
            timer.stop();
            timerTick(0);
            worker.myJoin();
            if (isCancelled) {
                status = ClosingStatus.CR_CANCELLED_BY_USER;
            } else {
                worker.throwAnyStoredWorkerThreadException();
                if (isVerifyMode) {
                    status = ClosingStatus.CR_VERIFICATION_COMPLETE;
                } else {
                    if (!isPaused && jCheckBoxSkipReconnection.isSelected()) {
                        status = ClosingStatus.CR_SKIP_MANUAL_RECONNECTION;
                    } else {
                        status = ClosingStatus.CR_WRITING_COMPLETE;
                    }
                }
            }
        } catch (Throwable ex) {
            status = ClosingStatus.CR_ERROR;
            String failureMsg = isVerifyMode ? "Test data verification failed" : "Test data creation failed";
            notification.display(this, failureMsg, Common.appIcon, TrayIcon.MessageType.ERROR);
            ErrDialog.errorDialog(this, ErrUtils.getExceptionMessage(ex));
        } finally {
            notification.destroy();
            parent.showDisabledAtPreviousPosition();
            final ClosingStatus status2 = status;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    parent.onAfterClosingProgressWindow(status2, worker.getCurrentProgress(), elapsedTimeMs / 1000);
                }
            });
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
            dispose();
        }
    }

    @Action
    public void cancelButtonPressed() {
        isCancelled = true;
        worker.cancelProcessing();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        jToggleButtonPause.setEnabled(false);
        jButtonCancel.setEnabled(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UNDEFINED && e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            if (GuiUtils.isFocusInThisFrame(this)) {
                e.consume();
                cancelButtonPressed();
                return true;
            }
        }
        return false;
    }

    @Action
    public void pauseTest() {
        if (isPaused) worker.continueProcessing(); else worker.pauseProcessing();
        isPaused = !isPaused;
        jLabelPaused.setVisible(isPaused);
        updateTitle();
        updateProgress();
    }

    private void updateTitle() {
        String pausedPrefix = isPaused ? "(paused) " : "";
        String titleFmt = isVerifyMode ? "Verifying \"%s\" ..." : "Filling \"%s\" ...";
        String title = String.format(titleFmt, testDataFilesPath);
        setTitle(pausedPrefix + title);
        setTextIfChanged(jTextFieldStartPath, title);
        jTextFieldStartPath.setToolTipText(title);
    }

    @Action
    public void openTestDataLocation() {
        myutils.Misc.desktopOpenAction(GuiUtils.getWindowForComponent(this), testDataFilesPath);
    }

    private void setTextIfChanged(JTextComponent textField, String txt) {
        if (!textField.getText().equals(txt)) {
            textField.setText(txt);
            GuiUtils.scrollTextComponentToStartPos(textField);
        }
    }

    private javax.swing.JButton jButtonCancel;

    private javax.swing.JButton jButtonOpenLocation;

    private javax.swing.JCheckBox jCheckBoxSkipReconnection;

    private javax.swing.JLabel jLabelCurrentPath;

    private javax.swing.JLabel jLabelPaused;

    private javax.swing.JLabel jLabelSkipReconnection;

    private javax.swing.JLabel jLabelSpeed;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JProgressBar jProgressBarFill;

    private javax.swing.JTextField jTextFieldCurrentPath;

    private javax.swing.JTextField jTextFieldElapsedTime;

    private javax.swing.JTextField jTextFieldEstimatedTime;

    private javax.swing.JTextField jTextFieldSpeed;

    private javax.swing.JTextField jTextFieldStartPath;

    private javax.swing.JTextField jTextFieldTargetSize;

    private javax.swing.JTextField jTextFieldTotalSize;

    private javax.swing.JToggleButton jToggleButtonPause;
}
