package org.codecover.eclipse.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import org.codecover.eclipse.CodeCoverPlugin;
import org.codecover.eclipse.Messages;
import org.codecover.eclipse.preferences.PreferencePageRoot;
import org.codecover.eclipse.preferences.RGBWithBoundaries;
import org.codecover.eclipse.tscmanager.ActiveTSContainerInfo;
import org.codecover.eclipse.tscmanager.TSContainerInfo;
import org.codecover.eclipse.tscmanager.TSContainerManagerListener;
import org.codecover.eclipse.views.controls.LegendControl;
import org.codecover.eclipse.views.controls.MatrixControl;
import org.codecover.eclipse.views.controls.LegendControl.ILegendContentProvider;
import org.codecover.eclipse.views.controls.MatrixControl.CorrelationInfo;
import org.codecover.eclipse.views.controls.MatrixControl.IMatrixContentProvider;
import org.codecover.eclipse.views.controls.MatrixControl.TestCaseInfo;
import org.codecover.metrics.Metric;
import org.codecover.metrics.MetricProvider;
import org.codecover.metrics.correlation.CorrelationMetric;
import org.codecover.metrics.correlation.CorrelationResult;
import org.codecover.model.TestCase;
import org.codecover.model.TestSession;
import org.codecover.model.TestSessionContainer;
import org.codecover.model.utils.ChangeType;
import org.codecover.model.utils.Logger;
import org.codecover.model.utils.criteria.Criterion;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * This class represents an eclipse view. It is used to calculate and display
 * the results of the available {@link CorrelationMetric}s.
 * 
 * @author Markus Wittlinger
 * @version 1.0 ($Id: CorrelationView.java 1 2007-12-12 17:37:26Z t-scheller $)
 */
public class CorrelationView extends ViewPart {

    private static final Logger logger = CodeCoverPlugin.getDefault().getLogger();

    private static final String TAG_WEIGHTS_LENGTH = "WeightsLength";

    private static final String TAG_FILTER_TREE_ACTION = "filterTreeAction";

    private static final String TAG_SHOW_LEGEND_ACTION = "showLegendAction";

    private static final String TAG_AUTO_CALCULATE_ACTION = "autoCalculateAction";

    private static final String TAG_SELECTED = "Selected";

    private static final String TAG_SASH_WEIGHTS = "sashWeights";

    private static final String TAG_SASH_WEIGHT_PREFIX = "sashWeight";

    private static final String TAG_ALL_CORRELATIONS_ACTION = "allCorrelationsAction";

    private final Set<CorrelationMetric> correlationMetrics;

    private final List<IAction> correlationActions;

    private IAction allCorrelationsAction;

    private IAction autoCalculateAction;

    private IAction filterTreeAction;

    private MatrixControl matrixControl;

    private TreeViewer treeViewer;

    private IAction showLegendAction;

    private final Map<CorrelationMetric, CorrelationResult> resultMap;

    private final GraphElement rootChainElement;

    private final List<TestCase> activeTestCases;

    private final List<TestCase> lastUsedTestCases;

    private ScrolledComposite scrolledComposite;

    private final Set<CorrelationMetric> activeMetrics;

    private IMemento memento;

    private SashForm sashForm;

    private boolean calculationPending = false;

    private final Object lock = new Object();

    private final CorrelationMetricComparator correlationMetricComparator = new CorrelationMetricComparator();

    private final TestCaseComparator testCaseComparator = new TestCaseComparator();

    private static final String TREE_LABEL_FORMAT = Messages.getString("CorrelationView.TREE_LABEL_FORMAT");

    private static final String TOOLTIP_SHOW_LEGEND_ACTION = Messages.getString("CorrelationView.TOOLTIP_SHOW_LEGEND_ACTION");

    private static final String DESCRIPTION_CHOOSE_CORRELATION_ACTION = Messages.getString("CorrelationView.DESCRIPTION_CHOOSE_CORRELATION_ACTION");

    private static final String TOOLTIP_CHOOSE_CORRELATION_ACTION = Messages.getString("CorrelationView.TOOLTIP_CHOOSE_CORRELATION_ACTION");

    private static final String LABEL_CHOOSE_CORRELATION_ACTION = Messages.getString("CorrelationView.LABEL_CHOOSE_CORRELATION_ACTION");

    private static final String TOOLTIP_AUTO_CALCULATE_ACTION = Messages.getString("CorrelationView.TOOLTIP_AUTO_CALCULATE_ACTION");

    private static final String DESCRIPTION_AUTO_CALCULATE_ACTION = Messages.getString("CorrelationView.DESCRIPTION_AUTO_CALCULATE_ACTION");

    private static final String LABEL_AUTO_CALCULATE_ACTION = Messages.getString("CorrelationView.LABEL_AUTO_CALCULATE_ACTION");

    private static final String DESCRIPTION_SHOW_LEGEND_ACTION = Messages.getString("CorrelationView.DESCRIPTION_SHOW_LEGEND_ACTION");

    private static final String LABEL_SHOW_LEGEND_ACTION = Messages.getString("CorrelationView.LABEL_SHOW_LEGEND_ACTION");

    private static final String TOOLTIP_FILTER_TREE_ACTION = Messages.getString("CorrelationView.TOOLTIP_FILTER_TREE_ACTION");

    private static final String DESCRIPTION_FILTER_TREE_ACTION = Messages.getString("CorrelationView.DESCRIPTION_FILTER_TREE_ACTION");

    private static final String LABEL_FILTER_TREE_ACTION = Messages.getString("CorrelationView.LABEL_FILTER_TREE_ACTION");

    private static final String DESCRIPTION_ALL_CORRELATION_METRICS = Messages.getString("CorrelationView.DESCRIPTION_ALL_CORRELATION_METRICS");

    private static final String LABEL_ALL_CORRELATION_METRICS = Messages.getString("CorrelationView.LABEL_ALL_CORRELATION_METRICS");

    private static final String LABEL_CSV_EXPORT_FILE_DIALOG = Messages.getString("CorrelationView.LABEL_CSV_EXPORT_FILE_DIALOG");

    private static final String LABEL_CSV_EXPORT_ACTION = Messages.getString("CorrelationView.LABEL_CSV_EXPORT_ACTION");

    private static final String CSV_EXPORT_TOOLTIP_DESCRIPTION = Messages.getString("CorrelationView.CSV_EXPORT_TOOLTIP_DESCRIPTION");

    /**
     * Constructor. 
     */
    public CorrelationView() {
        this.resultMap = new HashMap<CorrelationMetric, CorrelationResult>();
        this.activeTestCases = new Vector<TestCase>();
        this.lastUsedTestCases = new Vector<TestCase>();
        this.activeMetrics = new TreeSet<CorrelationMetric>(this.correlationMetricComparator);
        this.correlationMetrics = new TreeSet<CorrelationMetric>(this.correlationMetricComparator);
        this.correlationActions = new Vector<IAction>();
        this.rootChainElement = new GraphElement();
        CodeCoverPlugin.getDefault().getTSContainerManager().addListener(new TSManagerListener());
    }

    /**
     * Gets all the {@link CorrelationMetric}s
     * 
     * @return the {@link Set} of {@link CorrelationMetric}s
     */
    private static final Set<CorrelationMetric> getCorrelationMetrics() {
        Set<CorrelationMetric> correlationMetrics = new HashSet<CorrelationMetric>();
        for (Metric metric : MetricProvider.getAvailabeMetrics(CodeCoverPlugin.getDefault().getEclipsePluginManager().getPluginManager(), CodeCoverPlugin.getDefault().getLogger())) {
            if (metric instanceof CorrelationMetric) {
                correlationMetrics.add((CorrelationMetric) metric);
            }
        }
        return correlationMetrics;
    }

    @Override
    public void createPartControl(Composite parent) {
        this.correlationMetrics.addAll(getCorrelationMetrics());
        this.filterTreeAction = createActionFilterTree();
        this.showLegendAction = createActionShowLegend();
        this.autoCalculateAction = createActionAutoCalculate();
        this.correlationActions.addAll(createCorrelationActions());
        this.allCorrelationsAction = createAllCorrelationsAction();
        initializeToolBar();
        this.sashForm = new SashForm(parent, SWT.HORIZONTAL);
        Composite leftComposite = new Composite(this.sashForm, SWT.NONE);
        leftComposite.setLayout(new GridLayout(1, false));
        createTreeViewerPane(leftComposite);
        Composite rightComposite = new Composite(this.sashForm, SWT.NONE);
        rightComposite.setLayout(new GridLayout(2, false));
        createCorrelationMatrixPane(rightComposite);
        this.sashForm.setWeights(getWeightsFromMemento());
        calculateCorrelation();
        this.lastUsedTestCases.clear();
        this.activeTestCases.clear();
        ActiveTSContainerInfo activeTSContainer = CodeCoverPlugin.getDefault().getTSContainerManager().getActiveTSContainer();
        if (activeTSContainer != null) {
            this.lastUsedTestCases.addAll(sortTestCases(activeTSContainer.getActiveTestCases()));
            this.activeTestCases.addAll(sortTestCases(activeTSContainer.getActiveTestCases()));
        }
        calculateSumbsumptionChain();
        refreshTreeViewer();
        refreshMatrix();
        CodeCoverPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new PreferenceChangeListener());
        this.treeViewer.setInput(this.rootChainElement);
    }

    private final int[] getWeightsFromMemento() {
        int[] weights = new int[] { 1, 4 };
        if (this.memento != null) {
            IMemento sashWeightsMemento = this.memento.getChild(TAG_SASH_WEIGHTS);
            if (sashWeightsMemento != null) {
                int length = sashWeightsMemento.getInteger(TAG_WEIGHTS_LENGTH);
                weights = new int[length];
                for (int i = 0; i < length; i++) {
                    weights[i] = sashWeightsMemento.getInteger(TAG_SASH_WEIGHT_PREFIX + i);
                }
            }
        }
        return weights;
    }

    private final void createTreeViewerPane(Composite mainComposite) {
        this.treeViewer = new TreeViewer(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
        this.treeViewer.setContentProvider(new TreeContentProvider());
        this.treeViewer.setLabelProvider(new TreeLabelProvider());
        this.treeViewer.setComparator(new TreeComparator());
        this.treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    /**
     * Creates the matrix pane in the given composite
     * 
     * @param mainComposite
     *            the given composite.
     */
    private final void createCorrelationMatrixPane(Composite mainComposite) {
        this.scrolledComposite = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
        this.matrixControl = new MatrixControl(this.scrolledComposite, SWT.BORDER);
        this.matrixControl.setMatrixContentProvider(new MatrixContentProvider());
        this.matrixControl.setLegendContentProvider(new LegendContentProvider());
        this.matrixControl.setShowLegend(this.showLegendAction.isChecked());
        this.scrolledComposite.setContent(this.matrixControl);
        this.scrolledComposite.setExpandVertical(true);
        this.scrolledComposite.setExpandHorizontal(true);
        this.scrolledComposite.setAlwaysShowScrollBars(true);
        this.scrolledComposite.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                CorrelationView.this.scrolledComposite.setMinSize(CorrelationView.this.matrixControl.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        this.scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    @Override
    public void setFocus() {
        this.matrixControl.forceFocus();
    }

    private final Color getColorForCorrelation(double correlation) {
        Display display = getViewSite().getShell().getDisplay();
        for (RGBWithBoundaries boundaries : PreferencePageRoot.getCorrelationMatrixColors()) {
            double lower = (double) boundaries.getLowerBoundary() / 100;
            double upper = (double) boundaries.getUpperBoundary() / 100;
            if (lower == upper) {
                if (correlation == lower) {
                    return new Color(display, boundaries.getRGB());
                }
            } else {
                if (correlation >= lower && correlation < upper) {
                    return new Color(display, boundaries.getRGB());
                }
            }
        }
        return display.getSystemColor(SWT.COLOR_BLACK);
    }

    private final List<CorrelationInfo> getCorrelationInfos() {
        List<CorrelationInfo> list = new Vector<CorrelationInfo>();
        if (this.activeMetrics.isEmpty()) {
            return list;
        }
        for (int i = 0; i < this.lastUsedTestCases.size(); i++) {
            TestCase firstTestCase = this.lastUsedTestCases.get(i);
            for (int a = 0; a < this.lastUsedTestCases.size(); a++) {
                TestCase secondTestCase = this.lastUsedTestCases.get(a);
                Set<TestCase> testCases = new HashSet<TestCase>();
                testCases.add(firstTestCase);
                testCases.add(secondTestCase);
                int amountFirstTestCase = 0;
                int amountSecondTestCase = 0;
                int amountShared = 0;
                double correlation = getCummulatedCorrelation(firstTestCase, secondTestCase);
                for (CorrelationMetric correlationMetric : this.activeMetrics) {
                    CorrelationResult result = this.resultMap.get(correlationMetric);
                    if (result == null) {
                        continue;
                    }
                    amountFirstTestCase += result.getCoverableItemCount(firstTestCase);
                    amountSecondTestCase += result.getCoverableItemCount(secondTestCase);
                    amountShared += result.getSharedCoverableItemCount(testCases);
                }
                CorrelationInfo correlationInfo = this.matrixControl.new CorrelationInfo(correlation, amountShared, amountFirstTestCase, amountSecondTestCase);
                list.add(correlationInfo);
            }
        }
        return list;
    }

    private final List<TestCaseInfo> getTestCaseInfos() {
        List<TestCaseInfo> list = new Vector<TestCaseInfo>();
        if (this.activeMetrics.isEmpty()) {
            return list;
        }
        for (TestCase testCase : this.lastUsedTestCases) {
            TestCaseInfo testCaseInfo = this.matrixControl.new TestCaseInfo(testCase.getName(), truncateString(testCase.getName(), 9), testCase.getTestSession().getName());
            list.add(testCaseInfo);
        }
        return list;
    }

    private final String truncateString(String text, int length) {
        String trailString = "…";
        if (text.length() <= length) {
            return text;
        }
        String truncatedString = text.substring(0, length - trailString.length());
        truncatedString += trailString;
        return truncatedString;
    }

    private final void initializeToolBar() {
        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        toolBarManager.add(createActionCSVExport());
        toolBarManager.add(this.filterTreeAction);
        toolBarManager.add(createActionChooseMetric());
        toolBarManager.add(this.autoCalculateAction);
        toolBarManager.add(this.showLegendAction);
    }

    private final IAction createActionFilterTree() {
        IAction filterTreeAction = new Action(LABEL_FILTER_TREE_ACTION, IAction.AS_CHECK_BOX) {

            @Override
            public void run() {
                onFilterTree();
            }
        };
        filterTreeAction.setChecked(getBooleanFromMemento(TAG_FILTER_TREE_ACTION, false));
        filterTreeAction.setDescription(DESCRIPTION_FILTER_TREE_ACTION);
        filterTreeAction.setToolTipText(TOOLTIP_FILTER_TREE_ACTION);
        filterTreeAction.setImageDescriptor(CodeCoverPlugin.getDefault().getImageRegistry().getDescriptor(CodeCoverPlugin.Image.HIDE_TREE_ITEMS.getPath()));
        return filterTreeAction;
    }

    private final IAction createActionShowLegend() {
        IAction showLegendAction = new Action(LABEL_SHOW_LEGEND_ACTION, IAction.AS_CHECK_BOX) {

            @Override
            public void run() {
                onShowLegend();
            }
        };
        showLegendAction.setChecked(getBooleanFromMemento(TAG_SHOW_LEGEND_ACTION, true));
        showLegendAction.setDescription(DESCRIPTION_SHOW_LEGEND_ACTION);
        showLegendAction.setToolTipText(TOOLTIP_SHOW_LEGEND_ACTION);
        showLegendAction.setImageDescriptor(CodeCoverPlugin.getDefault().getImageRegistry().getDescriptor(CodeCoverPlugin.Image.SHOW_LEGEND.getPath()));
        return showLegendAction;
    }

    private final IAction createActionAutoCalculate() {
        IAction autoCalculateAction = new Action(LABEL_AUTO_CALCULATE_ACTION, IAction.AS_CHECK_BOX) {

            @Override
            public void run() {
                if (CorrelationView.this.autoCalculateAction.isChecked()) {
                    onCalculateCorrelation();
                }
            }
        };
        autoCalculateAction.setChecked(getBooleanFromMemento(TAG_AUTO_CALCULATE_ACTION, true));
        autoCalculateAction.setDescription(DESCRIPTION_AUTO_CALCULATE_ACTION);
        autoCalculateAction.setToolTipText(TOOLTIP_AUTO_CALCULATE_ACTION);
        autoCalculateAction.setImageDescriptor(CodeCoverPlugin.getDefault().getImageRegistry().getDescriptor(CodeCoverPlugin.Image.AUTO_CALCULATE.getPath()));
        return autoCalculateAction;
    }

    private final IAction createActionChooseMetric() {
        IAction chooseMetricAction = new Action(LABEL_CHOOSE_CORRELATION_ACTION, IAction.AS_DROP_DOWN_MENU) {

            @Override
            public void run() {
                onCalculateCorrelation();
            }
        };
        chooseMetricAction.setDescription(DESCRIPTION_CHOOSE_CORRELATION_ACTION);
        chooseMetricAction.setToolTipText(TOOLTIP_CHOOSE_CORRELATION_ACTION);
        chooseMetricAction.setImageDescriptor(CodeCoverPlugin.getDefault().getImageRegistry().getDescriptor(CodeCoverPlugin.Image.CALCULATE_CORRELATION.getPath()));
        chooseMetricAction.setMenuCreator(new MetricMenuCreator());
        return chooseMetricAction;
    }

    private final IAction createActionCSVExport() {
        IAction exportCSVAction = new Action(LABEL_CSV_EXPORT_ACTION, IAction.AS_PUSH_BUTTON) {

            @Override
            public void run() {
                onExportCSV();
            }
        };
        exportCSVAction.setDescription(CSV_EXPORT_TOOLTIP_DESCRIPTION);
        exportCSVAction.setToolTipText(CSV_EXPORT_TOOLTIP_DESCRIPTION);
        exportCSVAction.setImageDescriptor(CodeCoverPlugin.getDefault().getImageRegistry().getDescriptor(CodeCoverPlugin.Image.CSV_EXPORT.getPath()));
        return exportCSVAction;
    }

    private final void refreshLastUsedTestCases(Collection<TestCase> testCases) {
        this.lastUsedTestCases.clear();
        this.lastUsedTestCases.addAll(sortTestCases(testCases));
    }

    private final void onActiveTestCasesChanged(Set<TestCase> testCases) {
        refreshActiveTestCaseList(testCases);
        if (this.autoCalculateAction.isChecked()) {
            refreshLastUsedTestCases(this.activeTestCases);
            refreshTreeViewer();
            refreshMatrix();
        }
    }

    private final void onTSCChanged(Set<TestCase> testCases) {
        synchronized (lock) {
            this.calculationPending = false;
        }
        refreshActiveTestCaseList(testCases);
        if (this.autoCalculateAction.isChecked()) {
            onCalculateCorrelation();
        }
    }

    private final void onExportCSV() {
        FileDialog fileDialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        fileDialog.setText(LABEL_CSV_EXPORT_FILE_DIALOG);
        fileDialog.setFilterExtensions(new String[] { "*.csv", "*.*" });
        String location = fileDialog.open();
        if (location != null) {
            File exportFile = new File(location);
            String outputString = this.matrixControl.createCSVExportString();
            exportFile.delete();
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(exportFile, true);
                FileChannel outChannel = fileOutputStream.getChannel();
                ByteBuffer buffer = ByteBuffer.allocate(outputString.length());
                byte[] bytes = outputString.getBytes();
                buffer.put(bytes);
                buffer.flip();
                outChannel.write(buffer);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                CorrelationView.logger.error("A FileNotFoundException " + "occurred during csv export", e);
            } catch (IOException e) {
                CorrelationView.logger.error("A IOException occurred during csv export", e);
            }
        }
    }

    private final void onShowLegend() {
        this.matrixControl.setShowLegend(!this.matrixControl.isShowLegend());
        refreshMatrix();
    }

    private final void onChooseMetric(CorrelationMetric correlationMetric) {
        this.activeMetrics.clear();
        this.activeMetrics.add(correlationMetric);
        refreshTreeViewer();
        refreshMatrix();
    }

    private final void onChooseAllMetrics() {
        this.activeMetrics.addAll(this.correlationMetrics);
        refreshTreeViewer();
        refreshMatrix();
    }

    private final void onFilterTree() {
        boolean visible = !this.filterTreeAction.isChecked();
        for (GraphElement graphElement : this.rootChainElement.children) {
            if (graphElement.children.isEmpty()) {
                graphElement.visible = visible;
            } else {
                graphElement.visible = true;
            }
        }
        refreshTreeViewer();
    }

    private final void onCalculateCorrelation() {
        calculateCorrelation();
        calculateSumbsumptionChain();
        refreshTreeViewer();
        refreshMatrix();
    }

    private final List<TestCase> getAllTestCasesFromActiveContainer() {
        List<TestCase> testCases = new LinkedList<TestCase>();
        ActiveTSContainerInfo activeTSContainer = CodeCoverPlugin.getDefault().getTSContainerManager().getActiveTSContainer();
        if (activeTSContainer == null) {
            return testCases;
        }
        TestSessionContainer testSessionContainer = activeTSContainer.getTestSessionContainer();
        for (TestSession testSession : testSessionContainer.getTestSessions()) {
            testCases.addAll(testSession.getTestCases());
        }
        return testCases;
    }

    private final void calculateCorrelation() {
        ActiveTSContainerInfo activeTSContainer = CodeCoverPlugin.getDefault().getTSContainerManager().getActiveTSContainer();
        if (activeTSContainer == null) {
            return;
        }
        TestSessionContainer testSessionContainer = activeTSContainer.getTestSessionContainer();
        List<TestCase> testCases = new Vector<TestCase>();
        for (TestSession testSession : testSessionContainer.getTestSessions()) {
            testCases.addAll(testSession.getTestCases());
        }
        if (testCases.isEmpty()) {
            return;
        }
        Set<TestCase> allTestCases = new HashSet<TestCase>(testCases);
        Set<TestCase> lastUsed = new HashSet<TestCase>(this.activeTestCases);
        lastUsed.retainAll(allTestCases);
        refreshLastUsedTestCases(lastUsed);
        this.resultMap.clear();
        Set<Criterion> availableCriteria = testSessionContainer.getCriteria();
        for (CorrelationMetric correlationMetric : this.correlationMetrics) {
            if (availableCriteria.containsAll(correlationMetric.getRequiredCriteria())) {
                CorrelationResult result = correlationMetric.calculateCorrelation(testCases);
                this.resultMap.put(correlationMetric, result);
            }
        }
    }

    private final boolean isTestCaseActive(TestCase testCase) {
        return this.lastUsedTestCases.contains(testCase);
    }

    private final void calculateSumbsumptionChain() {
        List<GraphElement> elements = new Vector<GraphElement>();
        this.rootChainElement.reset();
        if (this.activeMetrics.size() == 0) {
            return;
        }
        List<TestCase> testCases = new LinkedList<TestCase>(getAllTestCasesFromActiveContainer());
        if (testCases.isEmpty()) {
            return;
        }
        for (TestCase testCase : testCases) {
            GraphElement element = new GraphElement();
            element.testCases.add(testCase);
            elements.add(element);
        }
        for (GraphElement topElement : elements) {
            boolean noChange = false;
            while (!noChange) {
                noChange = true;
                for (TestCase testCase : testCases) {
                    double correlation = getCummulatedCorrelation(new Vector<TestCase>(topElement.testCases).firstElement(), testCase);
                    if (correlation != 1.0) {
                        continue;
                    } else {
                        noChange &= !recurseGraphElement(topElement, testCase);
                    }
                }
            }
        }
        for (GraphElement graphElement : elements) {
            GraphElement subElement = graphElement;
            while (subElement.parent != null) {
                subElement = subElement.parent;
            }
            subElement.parent = this.rootChainElement;
            this.rootChainElement.children.add(subElement);
            if (subElement.children.isEmpty()) {
                subElement.visible = !this.filterTreeAction.isChecked();
            }
        }
    }

    private final boolean recurseGraphElement(GraphElement element, TestCase testCase) {
        boolean anyInsertion = false;
        Iterator<GraphElement> iterator = new HashSet<GraphElement>(element.children).iterator();
        while (iterator.hasNext()) {
            anyInsertion |= recurseGraphElement(iterator.next(), testCase);
        }
        if (!anyInsertion) {
            double topCorrelation = getCummulatedCorrelation(new Vector<TestCase>(element.testCases).firstElement(), testCase);
            double bottomCorrelation = getCummulatedCorrelation(testCase, new Vector<TestCase>(element.testCases).firstElement());
            if (topCorrelation == 1.0 && bottomCorrelation == 1.0) {
                anyInsertion = element.testCases.add(testCase);
            } else if (topCorrelation == 1.0) {
                GraphElement newElement = new GraphElement();
                newElement.testCases.add(testCase);
                newElement.parent = element;
                boolean alreadyPresentInTree = false;
                for (GraphElement child : element.children) {
                    double childCorrelation = getCummulatedCorrelation(new Vector<TestCase>(child.testCases).firstElement(), testCase);
                    if (childCorrelation == 1.0) {
                        alreadyPresentInTree = true;
                        break;
                    }
                }
                if (!alreadyPresentInTree) {
                    anyInsertion = element.children.add(newElement);
                }
            } else if (bottomCorrelation == 1.0 && element.parent != null) {
                GraphElement newElement = new GraphElement();
                newElement.testCases.add(testCase);
                GraphElement parent = element.parent;
                double parentTopCorrelation = getCummulatedCorrelation(new Vector<TestCase>(parent.testCases).firstElement(), testCase);
                double parentBottonCorrelation = getCummulatedCorrelation(testCase, new Vector<TestCase>(parent.testCases).firstElement());
                if (parentTopCorrelation == 1.0 && parentBottonCorrelation == 1.0) {
                    anyInsertion = false;
                } else if (parentTopCorrelation == 1.0) {
                    parent.children.remove(element);
                    parent.children.add(newElement);
                    newElement.parent = parent;
                    newElement.children.add(element);
                    element.parent = newElement;
                    anyInsertion = true;
                }
            }
        }
        return anyInsertion;
    }

    private final double getCummulatedCorrelation(TestCase case1, TestCase case2) {
        double correlation = 0.0;
        int summands = 0;
        for (CorrelationMetric correlationMetric : this.activeMetrics) {
            CorrelationResult result = this.resultMap.get(correlationMetric);
            if (result == null) {
                continue;
            }
            if (result.getCoverableItemCount(case2) > 0) {
                correlation += result.getCorrelation(case1, case2);
                ++summands;
            }
        }
        if (summands > 0) {
            correlation /= summands;
        } else {
            correlation = 1.0;
        }
        return correlation;
    }

    private final List<IAction> createCorrelationActions() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        ISharedImages platformImages = workbench.getSharedImages();
        final List<IAction> correlationActions = new Vector<IAction>();
        for (final CorrelationMetric correlationMetric : this.correlationMetrics) {
            String name = correlationMetric.getName();
            IAction action = new Action(name, IAction.AS_RADIO_BUTTON) {

                @Override
                public void run() {
                    onChooseMetric(correlationMetric);
                }
            };
            action.setDescription(correlationMetric.getDescription());
            action.setImageDescriptor(platformImages.getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
            action.setChecked(getBooleanFromMemento(escapeWhiteSpaces(name), false));
            if (action.isChecked()) {
                this.activeMetrics.add(correlationMetric);
            }
            correlationActions.add(action);
        }
        return correlationActions;
    }

    private final IAction createAllCorrelationsAction() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        ISharedImages platformImages = workbench.getSharedImages();
        IAction action = new Action(LABEL_ALL_CORRELATION_METRICS, IAction.AS_RADIO_BUTTON) {

            @Override
            public void run() {
                onChooseAllMetrics();
            }
        };
        action.setDescription(DESCRIPTION_ALL_CORRELATION_METRICS);
        action.setImageDescriptor(platformImages.getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
        action.setChecked(getBooleanFromMemento(TAG_ALL_CORRELATIONS_ACTION, areNoDistinctCorrelationActionChecked()));
        if (action.isChecked()) {
            this.activeMetrics.addAll(this.correlationMetrics);
        }
        return action;
    }

    private final boolean areNoDistinctCorrelationActionChecked() {
        for (IAction correlationAction : this.correlationActions) {
            if (correlationAction.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private final boolean getBooleanFromMemento(String tagName, boolean defaultValue) {
        boolean checked = defaultValue;
        if (this.memento != null) {
            IMemento mem = this.memento.getChild(tagName);
            if (mem != null) {
                checked = Boolean.parseBoolean(mem.getString(TAG_SELECTED));
            }
        }
        return checked;
    }

    private final void refreshMatrix() {
        this.matrixControl.refreshMatrix();
        this.scrolledComposite.setMinSize(this.matrixControl.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private final void refreshTreeViewer() {
        this.treeViewer.refresh();
    }

    private final void refreshActiveTestCaseList(Set<TestCase> testCases) {
        this.activeTestCases.clear();
        this.activeTestCases.addAll(sortTestCases(testCases));
    }

    private final List<TestCase> sortTestCases(Collection<TestCase> testCases) {
        List<TestCase> list = new Vector<TestCase>();
        list.addAll(testCases);
        Collections.sort(list, this.testCaseComparator);
        return list;
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        this.memento = memento;
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        for (IAction action : this.correlationActions) {
            IMemento mem = memento.createChild(escapeWhiteSpaces(action.getText()));
            mem.putString(TAG_SELECTED, Boolean.toString(action.isChecked()));
        }
        IMemento allCorrelationsMemento = memento.createChild(TAG_ALL_CORRELATIONS_ACTION);
        allCorrelationsMemento.putString(TAG_SELECTED, Boolean.toString(this.allCorrelationsAction.isChecked()));
        IMemento autoCalculateMemento = memento.createChild(TAG_AUTO_CALCULATE_ACTION);
        autoCalculateMemento.putString(TAG_SELECTED, Boolean.toString(this.autoCalculateAction.isChecked()));
        IMemento showLegendMemento = memento.createChild(TAG_SHOW_LEGEND_ACTION);
        showLegendMemento.putString(TAG_SELECTED, Boolean.toString(this.showLegendAction.isChecked()));
        IMemento hideTopLevelMemento = memento.createChild(TAG_FILTER_TREE_ACTION);
        hideTopLevelMemento.putString(TAG_SELECTED, Boolean.toString(this.filterTreeAction.isChecked()));
        IMemento sashWeightsMemento = memento.createChild(TAG_SASH_WEIGHTS);
        int[] weights = this.sashForm.getWeights();
        sashWeightsMemento.putInteger(TAG_WEIGHTS_LENGTH, weights.length);
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            sashWeightsMemento.putInteger(TAG_SASH_WEIGHT_PREFIX + i, weight);
        }
    }

    private final String escapeWhiteSpaces(String text) {
        return text.replace(' ', '_');
    }

    private Runnable getTSCChangedRunnable(final ActiveTSContainerInfo tscInfo) {
        return new Runnable() {

            public void run() {
                if (CorrelationView.this.matrixControl.isDisposed()) {
                    return;
                }
                if (tscInfo != null) {
                    onTSCChanged(tscInfo.getActiveTestCases());
                } else {
                    onTSCChanged(new HashSet<TestCase>());
                }
            }
        };
    }

    private Runnable getActiveTestCasesChangedRunnable(final ActiveTSContainerInfo tscInfo) {
        return new Runnable() {

            public void run() {
                if (CorrelationView.this.matrixControl.isDisposed()) {
                    return;
                }
                if (tscInfo != null) {
                    onActiveTestCasesChanged(tscInfo.getActiveTestCases());
                } else {
                    onActiveTestCasesChanged(new HashSet<TestCase>());
                }
            }
        };
    }

    private final class TestCaseComparator implements Comparator<TestCase> {

        /**
         * Compares two {@link TestCase}s
         * 
         * @param testCase1
         *            the first {@link TestCase}
         * @param testCase2
         *            the second {@link TestCase}
         * @return the comparison result of the names of the {@link TestCase}'s
         *         {@link TestSession}s, or, if those names are equal, the
         *         comparison result of the names of the {@link TestCase}s
         */
        public int compare(TestCase testCase1, TestCase testCase2) {
            int result;
            result = testCase1.getTestSession().getName().compareTo(testCase2.getTestSession().getName());
            if (result == 0) {
                result = testCase1.getName().compareTo(testCase2.getName());
            }
            return result;
        }
    }

    private final class GraphElement {

        GraphElement parent;

        boolean visible = true;

        final Set<TestCase> testCases = new TreeSet<TestCase>(CorrelationView.this.testCaseComparator);

        final Set<GraphElement> children = new HashSet<GraphElement>();

        /**
         * Resets the data of the element.
         */
        void reset() {
            this.parent = null;
            this.visible = true;
            this.testCases.clear();
            this.children.clear();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GraphElement)) {
                return false;
            }
            GraphElement that = (GraphElement) obj;
            if (!this.testCases.equals(that.testCases)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (TestCase testCase : this.testCases) {
                hash += testCase.getName().hashCode();
                hash += testCase.getTestSession().getName().hashCode();
            }
            return hash;
        }
    }

    private final class MetricMenuCreator implements IMenuCreator {

        public void dispose() {
        }

        public Menu getMenu(Control parent) {
            Menu dropDownMenu = new Menu(parent);
            for (IAction action : CorrelationView.this.correlationActions) {
                ActionContributionItem contributionItem = new ActionContributionItem(action);
                contributionItem.fill(dropDownMenu, -1);
            }
            ActionContributionItem allMetricsItem = new ActionContributionItem(CorrelationView.this.allCorrelationsAction);
            allMetricsItem.fill(dropDownMenu, -1);
            return dropDownMenu;
        }

        public Menu getMenu(Menu parent) {
            return null;
        }
    }

    private final class MatrixContentProvider implements IMatrixContentProvider {

        public Color getColorForCorrelation(double correlation) {
            return CorrelationView.this.getColorForCorrelation(correlation);
        }

        public List<CorrelationInfo> getCorrelationInfos() {
            return CorrelationView.this.getCorrelationInfos();
        }

        public List<TestCaseInfo> getTestCaseInfos() {
            return CorrelationView.this.getTestCaseInfos();
        }
    }

    private final class LegendContentProvider implements ILegendContentProvider {

        private LegendControl legendControl;

        public void setClient(LegendControl legendControl) {
            this.legendControl = legendControl;
        }

        public List<Color> getColors() {
            List<Color> list = new Vector<Color>();
            if (this.legendControl == null) {
                return list;
            }
            Display display = this.legendControl.getDisplay();
            for (RGBWithBoundaries boundaries : PreferencePageRoot.getCorrelationMatrixColors()) {
                list.add(new Color(display, boundaries.getRGB()));
            }
            return list;
        }

        public List<String> getLabels() {
            List<String> list = new Vector<String>();
            if (this.legendControl == null) {
                return list;
            }
            for (RGBWithBoundaries boundaries : PreferencePageRoot.getCorrelationMatrixColors()) {
                int lower = boundaries.getLowerBoundary();
                int upper = boundaries.getUpperBoundary();
                StringBuilder sb = new StringBuilder();
                if (lower == upper) {
                    sb.append(String.format("%1$d%%", lower));
                } else {
                    sb.append(String.format("%1$d%% - %2$d%%", lower, upper));
                }
                list.add(sb.toString());
            }
            return list;
        }
    }

    private final class TreeContentProvider implements ITreeContentProvider {

        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof GraphElement) {
                List<GraphElement> list = new Vector<GraphElement>();
                GraphElement element = (GraphElement) parentElement;
                for (GraphElement testCaseChainElement : element.children) {
                    list.addAll(rec(testCaseChainElement));
                }
                return removeDuplicateEntries(list).toArray();
            }
            return null;
        }

        private final List<GraphElement> rec(GraphElement graphElement) {
            List<GraphElement> list = new LinkedList<GraphElement>();
            boolean anySelection = false;
            for (TestCase testCase : graphElement.testCases) {
                anySelection |= isTestCaseActive(testCase);
            }
            if (anySelection && graphElement.visible) {
                list.add(graphElement);
            } else {
                for (GraphElement element : graphElement.children) {
                    list.addAll(rec(element));
                }
            }
            return list;
        }

        private final List<GraphElement> removeDuplicateEntries(List<GraphElement> inputList) {
            List<GraphElement> newList = new LinkedList<GraphElement>();
            for (GraphElement graphElement : inputList) {
                boolean alreadyPresent = false;
                for (GraphElement element : newList) {
                    if (element.testCases.equals(graphElement.testCases)) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if (!alreadyPresent) {
                    newList.add(graphElement);
                }
            }
            return newList;
        }

        public Object getParent(Object element) {
            if (element instanceof GraphElement) {
                return ((GraphElement) element).parent;
            }
            return null;
        }

        public boolean hasChildren(Object element) {
            if (element instanceof GraphElement) {
                if (((GraphElement) element).children.isEmpty()) {
                    return false;
                } else {
                    List<GraphElement> list = new LinkedList<GraphElement>();
                    for (GraphElement graphElement : ((GraphElement) element).children) {
                        return list.addAll(rec(graphElement));
                    }
                    return !list.isEmpty();
                }
            }
            return false;
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    private final class TreeLabelProvider implements ILabelProvider {

        public Image getImage(Object element) {
            return null;
        }

        public String getText(Object element) {
            if (element instanceof GraphElement) {
                StringBuilder sb = new StringBuilder();
                boolean firstElement = true;
                for (TestCase testCase : ((GraphElement) element).testCases) {
                    if (!isTestCaseActive(testCase)) {
                        continue;
                    }
                    String name = testCase.getName();
                    String sessionName = testCase.getTestSession().getName();
                    if (firstElement) {
                        firstElement = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(String.format(TREE_LABEL_FORMAT, name, sessionName));
                }
                return sb.toString();
            }
            return null;
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
    }

    private final class TreeComparator extends ViewerComparator {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            IBaseLabelProvider baseLabelProvider = ((ContentViewer) viewer).getLabelProvider();
            if (baseLabelProvider instanceof ILabelProvider) {
                ILabelProvider labelProvider = (ILabelProvider) baseLabelProvider;
                return labelProvider.getText(e1).compareTo(labelProvider.getText(e2));
            }
            return super.compare(viewer, e1, e2);
        }
    }

    private final class TSManagerListener implements TSContainerManagerListener {

        public void testCaseChanged(final ActiveTSContainerInfo tscInfo, ChangeType changeType, TestCase testCase) {
            Display d = getSite().getShell().getDisplay();
            switch(changeType) {
                case CHANGE:
                    synchronized (lock) {
                        if (CorrelationView.this.calculationPending) {
                            return;
                        }
                        d.asyncExec(getActiveTestCasesChangedRunnable(tscInfo));
                    }
                    break;
                case ADD:
                    break;
                case REMOVE:
                    break;
                default:
                    return;
            }
        }

        public void testCasesActivated(final ActiveTSContainerInfo tscInfo) {
            Display disp = getSite().getShell().getDisplay();
            synchronized (lock) {
                if (CorrelationView.this.calculationPending) {
                    return;
                }
                disp.asyncExec(getActiveTestCasesChangedRunnable(tscInfo));
            }
        }

        public void testSessionChanged(final ActiveTSContainerInfo tscInfo, ChangeType changeType, TestSession testSession) {
            Display disp = getSite().getShell().getDisplay();
            synchronized (lock) {
                if (CorrelationView.this.calculationPending) {
                    return;
                }
                CorrelationView.this.calculationPending = true;
                disp.asyncExec(getTSCChangedRunnable(tscInfo));
            }
        }

        public void testSessionContainerActivated(final ActiveTSContainerInfo tscInfo) {
            Display disp = getSite().getShell().getDisplay();
            synchronized (lock) {
                if (CorrelationView.this.calculationPending) {
                    return;
                }
                CorrelationView.this.calculationPending = true;
                disp.asyncExec(getTSCChangedRunnable(tscInfo));
            }
        }

        public void testSessionContainerAdded(TSContainerInfo tscInfo, int index) {
        }

        public void testSessionContainerChanged(ChangeType changeType, final ActiveTSContainerInfo tscInfo) {
            Display disp = getSite().getShell().getDisplay();
            synchronized (lock) {
                if (CorrelationView.this.calculationPending) {
                    return;
                }
                CorrelationView.this.calculationPending = true;
                disp.asyncExec(getTSCChangedRunnable(tscInfo));
            }
        }

        public void testSessionContainerRemoved(TSContainerInfo tscInfo) {
        }

        public void synchronizedStateChanged(TSContainerInfo tscInfo, boolean isSynchronized) {
        }
    }

    private final class PreferenceChangeListener implements IPropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            CorrelationView.this.refreshMatrix();
        }
    }

    private final class CorrelationMetricComparator implements Comparator<CorrelationMetric> {

        /**
         * Compares two {@link CorrelationMetric}s according to their names.
         * 
         * @param o1
         *            the first {@link CorrelationMetric}
         * @param o2
         *            the second {@link CorrelationMetric}
         * @return the result of the comparison of their names.
         */
        public int compare(CorrelationMetric o1, CorrelationMetric o2) {
            int result = 0;
            result = o1.getName().compareTo(o2.getName());
            return result;
        }
    }
}
