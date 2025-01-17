package de.bielefeld.uni.cebitec.contigorderingproject;

import de.bielefeld.uni.cebitec.common.CustomFileFilter;
import de.bielefeld.uni.cebitec.common.MiscFileUtils;
import de.bielefeld.uni.cebitec.matchingtask.CombinedNetbeansProgressReporter;
import de.bielefeld.uni.cebitec.matchingtask.MatchingTask;
import de.bielefeld.uni.cebitec.qgram.MatchList;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.netbeans.api.project.ProjectUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public final class NewReferenceAction extends AbstractAction implements ContextAwareAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new ContextAction(context);
    }

    private final class ContextAction extends AbstractAction implements PropertyChangeListener {

        private final ContigOrderingProject p;

        private Queue<MatchingTask> taskQueue;

        public ContextAction(Lookup context) {
            taskQueue = new LinkedList<MatchingTask>();
            p = context.lookup(ContigOrderingProject.class);
            setEnabled(p != null);
            putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
            if (p != null) {
                String name = ProjectUtils.getInformation(p).getDisplayName();
                putValue(NAME, "&Match contigs on new reference genome");
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File contigs = null;
            Properties projectProperties = p.getLookup().lookup(Properties.class);
            if (projectProperties != null) {
                contigs = new File(projectProperties.getProperty("contigs"));
            }
            if (contigs != null && !contigs.exists()) {
                JOptionPane.showMessageDialog(null, "Contigs are not readable", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File[] references = new FileChooserBuilder("ReferenceFastaFile").addFileFilter(new CustomFileFilter(".fas,.fna,.fasta", "FASTA File")).setFilesOnly(true).setTitle("Select Reference Genome(s)").showMultiOpenDialog();
            if (references != null) {
                nextreference: for (int i = 0; i < references.length; i++) {
                    File reference = references[i];
                    String referenceString = MiscFileUtils.getFileNameWithoutExtension(reference);
                    FileObject existingMatches = p.getProjectDirectory().getFileObject(referenceString, "r2c");
                    if (existingMatches != null) {
                        NotifyDescriptor.Confirmation confirm = new NotifyDescriptor.Confirmation(existingMatches.getNameExt() + " already exists\nDo you want to overwrite this file?", "Already existing matchfile");
                        Object returnvalue = DialogDisplayer.getDefault().notify(confirm);
                        if (returnvalue == NotifyDescriptor.Confirmation.YES_OPTION) {
                            try {
                                FileObject logfile = FileUtil.findBrother(existingMatches, "log");
                                if (logfile != null) {
                                    logfile.delete();
                                }
                                existingMatches.delete();
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        } else {
                            continue nextreference;
                        }
                    }
                    MatchingTask matcher = new MatchingTask(contigs, reference);
                    CombinedNetbeansProgressReporter progress = new CombinedNetbeansProgressReporter("Matching Contigs:" + MiscFileUtils.getFileNameWithoutExtension(contigs), "Matching on " + referenceString, referenceString);
                    matcher.setProgressReporter(progress);
                    matcher.addPropertyChangeListener(this);
                    taskQueue.add(matcher);
                }
            }
            startNextTask();
        }

        private void startNextTask() {
            if (!taskQueue.isEmpty()) {
                taskQueue.poll().execute();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().matches("state")) {
                if ((SwingWorker.StateValue) evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        MatchingTask matcher = ((MatchingTask) evt.getSource());
                        String referenceString = matcher.getReferenceFilenameWithoutExtension();
                        MatchList matches = matcher.get();
                        if (matches != null && !matches.isEmpty()) {
                            FileObject matchFile = p.getProjectDirectory().createData(referenceString, "r2c");
                            matches.writeToFile(FileUtil.toFile(matchFile));
                        }
                        if (matcher.getProgressReporter() != null && matcher.getProgressReporter() instanceof CombinedNetbeansProgressReporter) {
                            CombinedNetbeansProgressReporter progress = (CombinedNetbeansProgressReporter) matcher.getProgressReporter();
                            FileObject logfile = p.getProjectDirectory().createData(referenceString, "log");
                            progress.writeCommentsToFile(FileUtil.toFile(logfile));
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (ExecutionException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    startNextTask();
                }
            }
        }
    }
}
