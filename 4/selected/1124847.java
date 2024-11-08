package com.protocol7.casilda.editors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.PageBook;
import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.protocol7.casilda.model.BinaryMessageData;
import com.protocol7.casilda.model.MQMD;
import com.protocol7.casilda.model.MQMessage;
import com.sun.corba.se.spi.ior.WriteContents;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class MessageEditor extends EditorPart implements IResourceChangeListener {

    private PageBook book;

    private MQMessage message = new MQMessage();

    private MessageConsole myConsole;

    private Text qmText;

    private Text queueText;

    private MQMDPage mqmdPage;

    private DataPage dataPage;

    /**
     * Creates a multi-page editor example.
     */
    public MessageEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    /**
     * @param book
     * @param pages
     * @param tree
     */
    private void createDataPage(PageBook book, List pages, Tree tree) {
        dataPage = new DataPage(book, SWT.NONE);
        TreeItem dataItem = new TreeItem(tree.getItem(0), SWT.NONE);
        dataItem.setText("Data");
        dataItem.setData("page", dataPage);
        pages.add(dataPage);
    }

    /**
     * @param book
     * @param pages
     * @param tree
     */
    private void createMQMDPage(PageBook book, List pages, Tree tree) {
        mqmdPage = new MQMDPage(book, SWT.NONE);
        TreeItem mqmdItem = new TreeItem(tree.getItem(0), SWT.NONE);
        mqmdItem.setText("MQMD");
        mqmdItem.setData("page", mqmdPage);
        mqmdItem.setExpanded(true);
        pages.add(mqmdPage);
    }

    /**
     * @param composite
     */
    private Tree createTree(Composite composite) {
        Tree tree = new Tree(composite, SWT.BORDER);
        tree.setSize(150, 500);
        GridData data = new GridData(150, 500);
        data.verticalAlignment = SWT.TOP;
        tree.setLayoutData(data);
        TreeItem messageItem = new TreeItem(tree, SWT.NONE);
        messageItem.setText("Message");
        messageItem.setExpanded(true);
        tree.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Object page = e.item.getData("page");
                if (page instanceof Composite) {
                    book.showPage((Composite) page);
                }
            }
        });
        return tree;
    }

    /**
     * @param composite
     */
    private void createReadButton(Composite composite) {
        Button writeButton = new Button(composite, SWT.NONE);
        writeButton.setText("Read from queue");
        writeButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                MQQueueManager qm = null;
                MQQueue queue = null;
                try {
                    qm = new MQQueueManager(qmText.getText());
                    queue = qm.accessQueue(queueText.getText(), MQC.MQOO_INPUT_SHARED);
                    com.ibm.mq.MQMessage mqMessage = new com.ibm.mq.MQMessage();
                    queue.get(mqMessage);
                    MQMD mqmd = new MQMD(mqMessage);
                    mqmdPage.setMQMD(mqmd);
                    BinaryMessageData messageData = new BinaryMessageData(mqMessage);
                    dataPage.setMessageData(messageData);
                    writeToConsole("Message read from queue (lenght: " + mqMessage.getTotalMessageLength() + ")");
                } catch (MQException ex) {
                    writeToConsole(ex.getMessage());
                } catch (IOException ex) {
                    writeToConsole(ex.getMessage());
                } finally {
                    closeQuitely(queue);
                    closeQuitely(qm);
                }
            }
        });
    }

    /**
     * @param composite
     */
    private void createWriteButton(Composite composite) {
        Button writeButton = new Button(composite, SWT.NONE);
        writeButton.setText("Write to queue");
        writeButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                MQQueueManager qm = null;
                MQQueue queue = null;
                try {
                    qm = new MQQueueManager(qmText.getText());
                    queue = qm.accessQueue(queueText.getText(), MQC.MQOO_OUTPUT);
                    com.ibm.mq.MQMessage mqMessage = new com.ibm.mq.MQMessage();
                    mqmdPage.getMQMD().writeToMessage(mqMessage);
                    dataPage.getMessageData().writeToMessage(mqMessage);
                    queue.put(mqMessage);
                    queue.close();
                    qm.close();
                    writeToConsole("Sent message");
                } catch (Exception ex) {
                    writeToConsole(ex.getMessage());
                } finally {
                    closeQuitely(queue);
                    closeQuitely(qm);
                }
            }
        });
    }

    protected void closeQuitely(MQQueue queue) {
        if (queue != null) {
            try {
                queue.close();
            } catch (MQException e) {
            }
        }
    }

    protected void closeQuitely(MQQueueManager qm) {
        if (qm != null) {
            try {
                qm.close();
            } catch (MQException e) {
            }
        }
    }

    /**
     * The <code>MultiPageEditorPart</code> implementation of this
     * <code>IWorkbenchPart</code> method disposes all nested editors.
     * Subclasses may extend.
     */
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    /**
     * Saves the multi-page editor's document.
     */
    public void doSave(IProgressMonitor monitor) {
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Closes all project files on project close.
     */
    public void resourceChanged(final IResourceChangeEvent event) {
    }

    public void doSaveAs() {
    }

    public boolean isDirty() {
        return false;
    }

    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout(1, false);
        parent.setLayout(layout);
        Composite toolBar = new Composite(parent, SWT.NONE);
        toolBar.setLayout(new RowLayout());
        Label qmLabel = new Label(toolBar, SWT.NONE);
        qmLabel.setText("QM");
        qmText = new Text(toolBar, SWT.NONE);
        qmText.setText("QM1");
        Label queueLabel = new Label(toolBar, SWT.NONE);
        queueLabel.setText("Queue");
        queueText = new Text(toolBar, SWT.NONE);
        queueText.setText("Q1");
        createWriteButton(toolBar);
        createReadButton(toolBar);
        Composite treeAndBook = new Composite(parent, SWT.NONE);
        treeAndBook.setLayout(new GridLayout(2, false));
        Tree tree = createTree(treeAndBook);
        book = new PageBook(treeAndBook, SWT.BORDER);
        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        gridData.grabExcessHorizontalSpace = true;
        book.setLayoutData(gridData);
        List pages = new ArrayList();
        createMQMDPage(book, pages, tree);
        createDataPage(book, pages, tree);
        book.showPage((Control) pages.get(0));
        initConsole();
    }

    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) if (name.equals(existing[i].getName())) return (MessageConsole) existing[i];
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    private void initConsole() {
        myConsole = findConsole("WebSphere MQ");
    }

    private void writeToConsole(String msg) {
        try {
            showConsole();
        } catch (PartInitException e1) {
            e1.printStackTrace();
        }
        MessageConsoleStream stream = myConsole.newMessageStream();
        try {
            stream.println(msg);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @throws PartInitException 
     * 
     */
    private void showConsole() throws PartInitException {
        IWorkbenchPage page = getSite().getPage();
        String id = IConsoleConstants.ID_CONSOLE_VIEW;
        IConsoleView view;
        view = (IConsoleView) page.showView(id);
        view.display(myConsole);
    }

    public void setFocus() {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }
}
