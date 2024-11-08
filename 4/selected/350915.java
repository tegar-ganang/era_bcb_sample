package de.schwarzrot.install.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.ApplicationContext;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.concurrent.Task;
import de.schwarzrot.concurrent.TaskPerThreadExecutor;
import de.schwarzrot.data.access.jdbc.JDBCEntityManager;
import de.schwarzrot.data.support.AbstractEntity;
import de.schwarzrot.install.domain.InstallationConfig;
import de.schwarzrot.install.domain.SetupConfig;
import de.schwarzrot.install.task.AbstractInstallerTask;
import de.schwarzrot.install.task.CleanupTask;
import de.schwarzrot.install.task.CopyFilesTask;
import de.schwarzrot.install.task.GenConfigTask;
import de.schwarzrot.install.task.ImportSamplesTask;
import de.schwarzrot.system.SysInfo;
import de.schwarzrot.system.support.FileUtils;
import de.schwarzrot.ui.control.model.ProgressBarModel;
import de.schwarzrot.ui.support.AbstractWizard;
import de.schwarzrot.ui.support.AbstractWizardPage;
import de.schwarzrot.ui.validation.constraints.VCTaskExecutedSuccessfully;

public class TaskExecutionPage extends AbstractWizardPage<SetupConfig> {

    private static final long serialVersionUID = 713L;

    private static SysInfo sysInfo;

    private static final int MAX_EXEC_STEPS = 80;

    public TaskExecutionPage(AbstractWizard<SetupConfig> master, String id) {
        super(master, id);
        tasks = new ArrayList<AbstractInstallerTask<SetupConfig>>();
        pbm = new ProgressBarModel(0, 1, 0, MAX_EXEC_STEPS);
    }

    @Override
    public ValidationResult check() {
        ValidationResult vr = super.check();
        if (!vr.hasErrors()) {
            saveProtocol();
            pbm.setMessage(msgSource.getMessage(getName() + ".success", null, getName() + ".success", null));
            pbm.setValue(MAX_EXEC_STEPS);
            getWizard().setSuccess(true);
            getWizard().invalidate();
        }
        return vr;
    }

    @Override
    public void reset() {
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
        if (sysInfo.isLinux() && (getPresentationModel().getBean().isSudoConfig() || getPresentationModel().getBean().isSudoDaemon() || getPresentationModel().getBean().isSudoStarter() || getPresentationModel().getBean().isSudoTarget())) {
            getWizard().getProgressPublisher().end();
            while (true) {
                String s = JOptionPane.showInputDialog(this, msgSource.getMessage(getName() + ".nix.start", null, getName() + ".nix.start", null), msgSource.getMessage(getName() + ".start.title", null, getName() + ".start.title", null), JOptionPane.INFORMATION_MESSAGE);
                if ((s != null) && (s.length() > 0)) {
                    getPresentationModel().getBean().setNixUser(s);
                    if (FileUtils.checkSudoAccess(s)) break;
                    JOptionPane.showMessageDialog(this, msgSource.getMessage(getName() + ".sudo.failed", null, getName() + ".sudo.failed", null), msgSource.getMessage(getName() + ".sudo.failed", null, getName() + ".sudo.failed", null), JOptionPane.ERROR_MESSAGE);
                } else {
                    throw new ApplicationException(msgSource.getMessage(getName() + ".nix.abort", null, getName(), null));
                }
            }
        } else {
            getWizard().getProgressPublisher().end();
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this, msgSource.getMessage(getName() + ".win.start", null, getName() + ".win.start", null), msgSource.getMessage(getName() + ".start.title", null, getName() + ".start.title", null), JOptionPane.YES_NO_OPTION)) {
                throw new ApplicationException(msgSource.getMessage(getName() + "win.abort", null, getName(), null));
            }
            getPresentationModel().getBean().setNixUser("windows");
        }
        getWizard().getButtonPane().setEnableNavigation(false);
        createTargetDir();
        getPresentationModel().triggerCommit();
        check();
        execTasks();
    }

    protected void createTargetDir() {
        File targetDir = new File(getPresentationModel().getBean().getTargetDir(), "vdrassistant");
        getLogger().info("installation target directory is [" + targetDir + "] - gonna create it ...");
        if (!(targetDir.exists() && targetDir.isDirectory())) {
            getLogger().info("create directory as user [" + getPresentationModel().getBean().getNixUser() + "]");
            if (getPresentationModel().getBean().isSudoTarget()) {
                FileUtils.createDirectory(targetDir, getPresentationModel().getBean().getNixUser());
            } else {
                targetDir.mkdirs();
            }
            if (!(targetDir.exists() && targetDir.isDirectory())) {
                throw new ApplicationException("could not create target directory [" + targetDir + "]");
            }
        }
        getPresentationModel().setValue(InstallationConfig.PROPERTYNAME_DIR_TARGET, targetDir.getAbsoluteFile());
    }

    protected void execTasks() {
        TaskPerThreadExecutor tte = new TaskPerThreadExecutor();
        resetDataSource();
        tte.execute(new Runnable() {

            @Override
            public void run() {
                TaskPerThreadExecutor executor = new TaskPerThreadExecutor();
                for (Task curTask : tasks) {
                    executor.execute(curTask);
                    while (executor.getThread().getState() != Thread.State.TERMINATED) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (!curTask.getSuccessState()) break;
                }
                pbm.setMessage("ended with " + pbm.getValue() + " steps");
            }
        });
    }

    protected void resetDataSource() {
        ApplicationContext ctx = ApplicationServiceProvider.getService(ApplicationContext.class);
        SetupConfig cfg = getPresentationModel().getBean();
        if (ctx.containsBean("jdbcEntityManager")) {
            JDBCEntityManager em = (JDBCEntityManager) ctx.getBean("jdbcEntityManager");
            BasicDataSource bds = new BasicDataSource();
            bds.setDefaultAutoCommit(false);
            bds.setDriverClassName(cfg.getDrvClassName());
            bds.setUrl(cfg.getDrvUrl());
            bds.setUsername(cfg.getDsUser());
            bds.setPassword(cfg.getDsPassword());
            AbstractEntity.setSchemaName(cfg.getDsSchema());
            em.setDataSource(bds);
        }
    }

    protected void saveProtocol() {
        File tmpProtocol = new File(tasks.get(0).getProtocolFilename());
        FileUtils.copyFile(new File(getPresentationModel().getBean().getTargetDir(), "fe.filelist"), tmpProtocol, getPresentationModel().getBean().getNixUser());
    }

    @Override
    protected void setupComponent() {
        JProgressBar pg = new JProgressBar();
        FormLayout layout = new FormLayout("100dlu:grow", "fill:40dlu, 8, fill:30dlu:grow");
        PanelBuilder builder = new PanelBuilder(layout, (JPanel) getClientArea());
        progressLabel = new JLabel("gonna tell u a story ...");
        pbm.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                progressLabel.setText(pbm.getMessage());
            }
        });
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        pg.setStringPainted(true);
        pg.setModel(pbm);
        builder.add(progressLabel, cc.xy(1, 1));
        builder.add(pg, cc.xy(1, 3));
    }

    @Override
    protected void setupConstraints() {
        setupTasks();
        for (Task curTask : tasks) {
            addConstraint(new VCTaskExecutedSuccessfully<SetupConfig>(getPresentationModel(), curTask.getClass().getSimpleName(), curTask));
        }
    }

    protected void setupTasks() {
        tasks.add(new ImportSamplesTask<SetupConfig>(getPresentationModel(), SetupConfig.PROPERTYNAME_TS_MENUEEDITOR + "|" + SetupConfig.PROPERTYNAME_IMPORT_SAMPLES, pbm, new String[] { "samples/Sample-STANDARD-1299511086.zip", "samples/standard-STANDARD-1299511594.zip", "samples/standard-WIDESCREEN-1299511538.zip" }));
        tasks.add(new CopyFilesTask(getPresentationModel(), "true", pbm));
        tasks.add(new GenConfigTask(getPresentationModel(), "true", pbm));
        tasks.add(new CleanupTask<SetupConfig>(getPresentationModel(), "true", pbm));
    }

    protected List<AbstractInstallerTask<SetupConfig>> tasks;

    protected ProgressBarModel pbm;

    protected JLabel progressLabel;
}
