package com.nhncorp.cubridqa.result;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import com.nhncorp.cubridqa.Activator;
import com.nhncorp.cubridqa.CUBRIDAdvisor;
import com.nhncorp.cubridqa.cases.ExecuteCaseView;
import com.nhncorp.cubridqa.console.bean.SummaryInfo;
import com.nhncorp.cubridqa.console.util.TestUtil;
import com.nhncorp.cubridqa.model.ExtResult;
import com.nhncorp.cubridqa.navigation.FunctionTab;
import com.nhncorp.cubridqa.navigation.NavigationView;
import com.nhncorp.cubridqa.swtdesigner.ResourceManager;
import com.nhncorp.cubridqa.utils.PropertiesUtil;
import com.nhncorp.cubridqa.utils.TableViewerUtil;

/**
 * 
 * The view with two composite,one is used to display results and answers.
 * @ClassName: ResultView
 * @date 2009-9-4
 * @version V1.0 
 * Copyright (C) www.nhn.com
 */
public class ResultView extends ViewPart {

    public static final String ID = "com.nhncorp.cubridqa.result.ResultView";

    public Action compareAction;

    public Action saveAsAnswerAction;

    public Action restAction;

    private Result result;

    private Answer answer;

    private Shell shell;

    private String resultFilePath;

    private String answerFilePath;

    private String compositeType = "subResult";

    private TreeItem treeItem;

    private Composite composite;

    private Composite parent;

    /**
	 * @Description: Create contents of the view part
	 * 
	 * @param parent
	 */
    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new FillLayout());
        composite = ResultCompositeFactory.getProperComposite(compositeType, parent, this, treeItem);
        this.parent = parent;
        this.shell = parent.getShell();
        createActions();
        initializeToolBar();
    }

    /**
	 * 
	 * @Title: createActions
	 * @Description:Create actions,there are three action,compare,save answers
	 *                     and reset.
	 * @param
	 * @return void
	 * @throws
	 */
    private void createActions() {
        compareAction = new Action() {

            public void run() {
                TableViewerUtil.compareResult(result, answer);
            }
        };
        saveAsAnswerAction = new Action() {

            public void run() {
                NavigationView navigationView = (NavigationView) CUBRIDAdvisor.getView(NavigationView.ID);
                CTabItem cTabItem = navigationView.tabFolder.getSelection();
                if (cTabItem.getControl() instanceof FunctionTab) {
                    ExecuteCaseView view = (ExecuteCaseView) CUBRIDAdvisor.getView(ExecuteCaseView.ID);
                    TableViewer tableViewer = view.getCas().getRs().getTableViewer();
                    StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
                    if (selection.size() > 0) {
                        boolean openConfirm = MessageDialog.openConfirm(getSite().getShell(), "Are you sure?", "Do you really want to save answers?");
                        if (openConfirm) {
                            for (Iterator iterator = selection.iterator(); iterator.hasNext(); ) {
                                ExtResult extResult = (ExtResult) iterator.next();
                                String rs = extResult.getResult();
                                rs = rs.substring(0, rs.lastIndexOf(".")) + ".result";
                                rs = rs.replaceAll("\\\\", "/");
                                String willReppace = "/" + TestUtil.getAnswer4SQLAndOther(rs);
                                String answer = rs.replaceAll("/cases", willReppace);
                                answer = answer.replaceAll(".result", ".answer");
                                String saveDirectory = answer.substring(0, answer.indexOf(willReppace)) + "/" + willReppace;
                                if (!new File(saveDirectory).exists()) {
                                    new File(saveDirectory).mkdir();
                                }
                                try {
                                    FileUtils.copyFile(new File(rs), new File(answer));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            MessageDialog.openInformation(getSite().getShell(), "Save answer", "Answers have been saved");
                        }
                    }
                } else if (cTabItem.getControl() instanceof ResultTreeTab) {
                    ResultListView view = (ResultListView) CUBRIDAdvisor.getView(ResultListView.ID);
                    Composite composite2 = view.getComposite();
                    if (composite2 instanceof ShowListSummary) {
                        ShowListSummary showListSummary = (ShowListSummary) composite2;
                        ShowSummary showSummary = showListSummary.getShwoSummary();
                        TableViewer tableViewer = showSummary.getTableViewer();
                        SummaryInfo parentSummaryInfo = showSummary.getParentSummaryInfo();
                        StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
                        if (selection.size() > 0) {
                            String firstElement = (String) selection.getFirstElement();
                            if (firstElement.endsWith(":ok") || firstElement.endsWith(":nok") || firstElement.endsWith(":norun")) {
                                boolean openConfirm = MessageDialog.openConfirm(getSite().getShell(), "Are you sure?", "Do you really want to save answers?");
                                if (openConfirm) {
                                    for (Iterator iterator = selection.iterator(); iterator.hasNext(); ) {
                                        String rs = (String) iterator.next();
                                        boolean success = true;
                                        if (rs.endsWith(":nok")) {
                                            success = false;
                                        }
                                        String[] strings = rs.split("::runTimes::");
                                        rs = strings[1];
                                        rs = rs.substring(0, rs.lastIndexOf(".")) + ".result";
                                        String resultPath = rs;
                                        if (!success) {
                                            resultPath = PropertiesUtil.getValue("local.path") + parentSummaryInfo.getResultDir().substring(parentSummaryInfo.getLocalPath().length()) + "/" + rs.substring(rs.lastIndexOf("/") + 1, rs.length());
                                        }
                                        rs = rs.replaceAll("\\\\", "/");
                                        String willReppace = "/" + TestUtil.getAnswer4SQLAndOther(rs);
                                        String answer = rs.replaceAll("/cases", willReppace);
                                        answer = answer.replaceAll(".result", ".answer");
                                        String saveDirectory = answer.substring(0, answer.indexOf(willReppace)) + "/" + willReppace;
                                        if (!new File(saveDirectory).exists()) {
                                            new File(saveDirectory).mkdir();
                                        }
                                        try {
                                            FileUtils.copyFile(new File(resultPath), new File(answer));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    MessageDialog.openInformation(getSite().getShell(), "Save answer", "Answers have been saved");
                                }
                            }
                        }
                    }
                }
            }
        };
        restAction = new Action() {

            public void run() {
                Color foreColor = shell.getDisplay().getDefault().getSystemColor(SWT.NONE);
                Color backColor = shell.getDisplay().getDefault().getSystemColor(SWT.COLOR_WHITE);
                for (int i = 0; i < result.tableViewer.getTable().getItems().length; i++) {
                    result.tableViewer.getTable().getItems()[i].setForeground(foreColor);
                    result.tableViewer.getTable().getItem(i).setBackground(backColor);
                }
                for (int i = 0; i < answer.tableViewer.getTable().getItems().length; i++) {
                    answer.tableViewer.getTable().getItems()[i].setForeground(foreColor);
                    answer.tableViewer.getTable().getItem(i).setBackground(backColor);
                }
                result.tableViewer.getTable().setTopIndex(0);
                answer.tableViewer.getTable().setTopIndex(0);
            }
        };
        compareAction.setToolTipText("COMPARE");
        compareAction.setHoverImageDescriptor(ResourceManager.getPluginImageDescriptor(Activator.getDefault(), "icons/compare.gif"));
        saveAsAnswerAction.setToolTipText("SAVE AS ANSWER");
        saveAsAnswerAction.setHoverImageDescriptor(ResourceManager.getPluginImageDescriptor(Activator.getDefault(), "icons/save.gif"));
        restAction.setToolTipText("REST");
        restAction.setHoverImageDescriptor(ResourceManager.getPluginImageDescriptor(Activator.getDefault(), "icons/backward.gif"));
        compareAction.setEnabled(false);
        restAction.setEnabled(false);
    }

    /**
	 * 
	 * @Title: initializeToolBar
	 * @Description:Add actions into tool bar.
	 * @param
	 * @return void
	 * @throws
	 */
    private void initializeToolBar() {
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
        toolbarManager.add(compareAction);
        toolbarManager.add(restAction);
        toolbarManager.add(saveAsAnswerAction);
    }

    @Override
    public void setFocus() {
    }

    public String getResultFilePath() {
        return this.resultFilePath;
    }

    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public String getAnswerFilePath() {
        return answerFilePath;
    }

    public void setAnswerFilePath(String answerFilePath) {
        answerFilePath = answerFilePath.replaceAll("/answers/", "/" + TestUtil.getAnswer4SQLAndOther(answerFilePath) + "/");
        this.answerFilePath = answerFilePath;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public String getCompositeType() {
        return compositeType;
    }

    public void setCompositeType(String compositeType) {
        this.compositeType = compositeType;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public TreeItem getTreeItem() {
        return treeItem;
    }

    public void setTreeItem(TreeItem treeItem) {
        this.treeItem = treeItem;
    }

    public void setContents(String compositeType, TreeItem treeItem) {
        composite.dispose();
        composite = ResultCompositeFactory.getProperComposite(compositeType, parent, this, treeItem);
        parent.layout(true, true);
    }

    public void setContents(String compositeType, String filePath) {
        this.composite.dispose();
        if (compositeType.equalsIgnoreCase("compareResult")) {
            this.compositeType = "compareResult";
            this.compareAction.setEnabled(true);
            this.restAction.setEnabled(true);
            if ("nok".equalsIgnoreCase(filePath)) {
                composite = ResultCompositeFactory.getProperComposite(parent, this, resultFilePath, answerFilePath, true);
            } else {
                composite = ResultCompositeFactory.getProperComposite(parent, this, resultFilePath, answerFilePath, false);
            }
        } else {
            composite = ResultCompositeFactory.getProperComposite(compositeType, parent, this, filePath);
        }
        parent.layout(true, true);
    }

    public void dispose() {
        if (result != null) {
            result.dispose();
        }
        if (answer != null) {
            answer.dispose();
        }
        this.composite.dispose();
        super.dispose();
    }

    public void disposeResult() {
        if (false == (composite instanceof ShowSummary)) {
            return;
        }
        this.composite.dispose();
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());
        TableViewer tableViewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.getTable().setHeaderVisible(false);
        tableViewer.getTable().setLayout(new FillLayout());
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setLabelProvider(new LabelProvider());
        final TableColumn newColumnTableColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
        newColumnTableColumn.setWidth(parent.getBounds().width);
        parent.layout(true, true);
    }
}
