package ch.skyguide.tools.requirement.hmi.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import ch.skyguide.fdp.common.ObjectListModel;
import ch.skyguide.fdp.common.hmi.util.FileChooserLongTermMemory;
import ch.skyguide.fdp.common.hmi.util.ProgressMonitorEx;
import ch.skyguide.tools.requirement.data.AbstractRequirement;
import ch.skyguide.tools.requirement.data.AbstractTestResult;
import ch.skyguide.tools.requirement.data.GenericRequirementVisitor;
import ch.skyguide.tools.requirement.data.Requirement;
import ch.skyguide.tools.requirement.data.RequirementProject;
import ch.skyguide.tools.requirement.hmi.ExtensionFileFilter;
import ch.skyguide.tools.requirement.hmi.RepositoryManager;
import ch.skyguide.tools.requirement.hmi.openoffice.FilterEnum;
import ch.skyguide.tools.requirement.hmi.openoffice.OpenOfficeCalcDocument;
import ch.skyguide.tools.requirement.hmi.openoffice.OpenOfficeException;
import ch.skyguide.tools.requirement.hmi.openoffice.SpreadsheetDocumentProducer;
import ch.skyguide.tools.requirement.hmi.openoffice.TemplateManager.TemplateEnum;
import ch.skyguide.tools.requirement.hmi.openoffice.model.spreadsheet.SpreadsheetDocument;

@SuppressWarnings("serial")
public class GenerateTraceabilityMatrixAction extends AbstractFileAction {

    private final JFileChooser fileChooser;

    public GenerateTraceabilityMatrixAction(RepositoryManager _repositoryManager) {
        super("Traceability Matrix (xls)", _repositoryManager);
        fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new ExtensionFileFilter("Excel files", ".xls"));
        new FileChooserLongTermMemory(getClass(), getClass().getName(), fileChooser);
    }

    public void actionPerformed(ActionEvent _e) {
        int option = fileChooser.showSaveDialog(getTool());
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }
        ExtensionFileFilter.fixChooserSelection(fileChooser);
        final File f = fileChooser.getSelectedFile().getAbsoluteFile();
        if (f.exists()) {
            option = JOptionPane.showConfirmDialog(getTool(), "File already exists. Overwrite?\n" + f, "Confirmation", JOptionPane.YES_NO_OPTION);
            if (option != JFileChooser.APPROVE_OPTION) {
                return;
            }
        }
        final ProgressMonitorEx pm = new ProgressMonitorEx(getTool(), "Generating Traceability Matrix", null, 0, 5);
        pm.setMillisToDecideToPopup(0);
        pm.setMillisToPopup(0);
        pm.incrementProgressLater();
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    generateTraceabilityMatrix(f, pm);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            pm.close();
                        }
                    });
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void generateTraceabilityMatrix(File _f, ProgressMonitorEx _pm) {
        final TestGatherer g = new TestGatherer();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    RequirementProject p = getTool().getRequirementTreeModel().getProject();
                    p.accept(g);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        _pm.incrementProgressLater();
        SpreadsheetDocument d = new SpreadsheetDocument();
        d.setTemplate(TemplateEnum.swalchTraceabilityMatrix);
        List<String> testNameList = new ArrayList<String>(g.testNames);
        Collections.sort(testNameList);
        for (int x = 0; x < testNameList.size(); x++) {
            d.setData(4 + x, 0, testNameList.get(x));
        }
        for (int i = 0; i < g.requirements.size(); i++) {
            Requirement req = g.requirements.get(i);
            int x = 1;
            int y = 1 + i;
            d.setData(x++, y, req.getFullCode());
            Set<String> reqTestNames = g.matrix.get(req);
            int count = reqTestNames == null ? 0 : reqTestNames.size();
            d.setData(x++, y, count);
            x++;
            if (reqTestNames != null) {
                for (String testName : testNameList) {
                    if (reqTestNames.contains(testName)) {
                        d.setData(x, y, "x");
                    }
                    x++;
                }
            }
        }
        _pm.incrementProgressLater();
        OpenOfficeCalcDocument oooDocument;
        try {
            oooDocument = SpreadsheetDocumentProducer.createDocument(d);
        } catch (OpenOfficeException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        _pm.incrementProgressLater();
        try {
            oooDocument.exportTo(_f, FilterEnum.XLS97);
        } catch (OpenOfficeException e) {
            throw new RuntimeException(e);
        }
        _pm.incrementProgressLater();
        RepositoryManager.openInDefaultApplication(_f);
        _pm.incrementProgressLater();
    }

    private static class TestGatherer extends GenericRequirementVisitor {

        private final Set<String> testNames = new LinkedHashSet<String>();

        private final List<Requirement> requirements = new ArrayList<Requirement>();

        private final Map<Requirement, Set<String>> matrix = new IdentityHashMap<Requirement, Set<String>>();

        @Override
        protected void visitAny(AbstractRequirement _requirement) {
        }

        @Override
        public void visit(Requirement _requirement) {
            requirements.add(_requirement);
            ObjectListModel<AbstractTestResult> testResults = _requirement.getTestResults();
            for (AbstractTestResult testResult : testResults) {
                String testName = testResult.getName();
                register(_requirement, testName);
            }
            super.visit(_requirement);
        }

        private void register(Requirement _requirement, String _testName) {
            testNames.add(_testName);
            Set<String> l = matrix.get(_requirement);
            if (l == null) {
                l = new HashSet<String>();
                matrix.put(_requirement, l);
            }
            l.add(_testName);
        }
    }
}
