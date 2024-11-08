package de.carne.fs.swt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.PatternSyntaxException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import de.carne.fs.core.FileScanner;
import de.carne.fs.core.FileScannerListener;
import de.carne.fs.core.FileScannerResult;
import de.carne.fs.core.FileScannerResultType;
import de.carne.fs.resource.RB;
import de.carne.fs.swt.widgets.ExportDialog;
import de.carne.fs.swt.widgets.NativeView;
import de.carne.fs.util.FileScannerResultPattern;
import de.carne.io.Closeables;
import de.carne.swt.widgets.ApplicationError;
import de.carne.swt.widgets.Dialogs;
import de.carne.swt.widgets.ToolTips;
import de.carne.util.Debug;
import de.carne.util.Exceptions;
import de.carne.util.RuntimeLimiter;
import de.carne.util.logging.Log;
import de.carne.util.resource.ResourceUnavailableException;

/**
 * This class wraps a single <code>FileScanner</code> instance and connects it with the UI.
 */
class FileScannerUISession implements FileScannerListener {

    private static final Log LOG = new Log(FileScannerUISession.class);

    private FileScannerUILayout layout = null;

    private final Hashtable<FileScannerResult, TreeItem> navTreeItemMap = new Hashtable<FileScannerResult, TreeItem>();

    private final Listener navTreeItemUpdateListener = new Listener() {

        @Override
        public void handleEvent(Event evt) {
            handleNavTreeItemUpdate((TreeItem) evt.item, evt.index);
        }
    };

    private final SelectionListener navTreeItemSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            handleNavTreeItemSelection((TreeItem) evt.item);
        }
    };

    private final SelectionListener cmdEditCopySelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdEditCopy();
        }
    };

    private final SelectionListener cmdEditExportSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdEditExport();
        }
    };

    private final KeyListener searchTextKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent evt) {
            if (evt.keyCode == SWT.CR) {
                if ((evt.stateMask & SWT.SHIFT) != SWT.SHIFT) {
                    cmdSearchNext();
                } else {
                    cmdSearchPrevious();
                }
            }
        }
    };

    private final SelectionListener cmdSearchNextSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdSearchNext();
        }
    };

    private final SelectionListener cmdSearchPreviousSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdSearchPrevious();
        }
    };

    private final SelectionListener cmdGotoFormatStartSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdGotoFormatStart();
        }
    };

    private final SelectionListener cmdGotoFormatEndSelectedListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent evt) {
            cmdGotoFormatEnd();
        }
    };

    private DragSource dataDragSource = null;

    private File dataDragSourceFile = null;

    private ExecutorService scannerThreads = null;

    private FileScanner scanner = null;

    private ExecutorService exportThreads = null;

    /**
	 * Construct <code>FileScannerUISession</code>.
	 * 
	 * @param layout The UI layout to use.
	 * @param fileName The name of the file to scan.
	 * @throws IOException If an error occurs during <code>FileScanner</code> construction.
	 */
    public FileScannerUISession(FileScannerUILayout layout, String fileName) throws IOException {
        assert layout != null;
        assert fileName != null;
        boolean complete = false;
        try {
            this.layout = layout;
            this.layout.navTree().addListener(SWT.SetData, this.navTreeItemUpdateListener);
            this.layout.navTree().addSelectionListener(this.navTreeItemSelectedListener);
            this.layout.cmdEditCopy().addSelectionListener(this.cmdEditCopySelectedListener);
            this.layout.cmdEditExport().addSelectionListener(this.cmdEditExportSelectedListener);
            this.layout.searchText().addKeyListener(this.searchTextKeyListener);
            this.layout.cmdSearchNext().addSelectionListener(this.cmdSearchNextSelectedListener);
            this.layout.cmdSearchPrevious().addSelectionListener(this.cmdSearchPreviousSelectedListener);
            this.layout.cmdGotoFormatStart().addSelectionListener(this.cmdGotoFormatStartSelectedListener);
            this.layout.cmdGotoFormatEnd().addSelectionListener(this.cmdGotoFormatEndSelectedListener);
            this.dataDragSource = new DragSource(this.layout.navTree(), DND.DROP_COPY);
            this.dataDragSource.addDragListener(new DragSourceAdapter() {

                @Override
                public void dragStart(DragSourceEvent evt) {
                    handleDragStart(evt);
                }

                @Override
                public void dragSetData(DragSourceEvent evt) {
                    handleDragSetData(evt);
                }

                @Override
                public void dragFinished(DragSourceEvent evt) {
                    handleDragFinished();
                }
            });
            this.scannerThreads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new ThreadFactory() {

                private final int priority = Thread.currentThread().getPriority() - 2;

                @Override
                public Thread newThread(Runnable r) {
                    final Thread thread = new Thread(r);
                    thread.setPriority(this.priority);
                    thread.setDaemon(true);
                    return thread;
                }
            });
            final Display display = this.layout.shell().getDisplay();
            final FileScannerUISessionMarshaler marshaler = new FileScannerUISessionMarshaler(display, this);
            this.scanner = new FileScanner(this.scannerThreads, marshaler, fileName);
            SWTMain.CLOSEABLES.add(this.scanner);
            this.exportThreads = Executors.newCachedThreadPool();
            updateTitle();
            complete = true;
        } finally {
            if (!complete) {
                close();
            }
        }
        updateViews();
    }

    /**
	 * Stop the session.
	 * <p>
	 * This requests a stop of any ongoing scan activities. But does not discard the scan result.
	 * </p>
	 */
    public void stop() {
        if (this.scanner != null) {
            this.scanner.stop(false);
        }
        if (this.layout != null) {
            this.layout.cmdStopScan().setEnabled(false);
        }
    }

    /**
	 * Close the session.
	 * <p>
	 * This requests a stop of any ongoing scan activities and discards the scan result.
	 * </p>
	 */
    public void close() {
        if (this.exportThreads != null) {
            this.exportThreads.shutdownNow();
            this.exportThreads = null;
        }
        if (this.scanner != null) {
            this.scanner.stop(true);
        }
        this.scannerThreads.shutdownNow();
        this.scannerThreads = null;
        this.dataDragSource.dispose();
        if (this.layout != null) {
            this.layout.navTree().removeAll();
            this.layout.navTree().removeSelectionListener(this.navTreeItemSelectedListener);
            this.layout.navTree().removeListener(SWT.SetData, this.navTreeItemUpdateListener);
            this.layout.cmdEditCopy().removeSelectionListener(this.cmdEditCopySelectedListener);
            this.layout.cmdEditExport().removeSelectionListener(this.cmdEditExportSelectedListener);
            this.layout.searchText().removeKeyListener(this.searchTextKeyListener);
            this.layout.cmdSearchNext().removeSelectionListener(this.cmdSearchNextSelectedListener);
            this.layout.cmdSearchPrevious().removeSelectionListener(this.cmdSearchPreviousSelectedListener);
            this.layout.cmdGotoFormatStart().removeSelectionListener(this.cmdGotoFormatStartSelectedListener);
            this.layout.cmdGotoFormatEnd().removeSelectionListener(this.cmdGotoFormatEndSelectedListener);
            this.navTreeItemMap.clear();
            this.layout.hexView().setFileScannerResult(null);
            this.layout.nativeView().setFileScannerResult(null);
            this.layout = null;
        }
    }

    void cmdEditCopy() {
        this.layout.nativeView().copyToClipboard();
    }

    void cmdEditExport() {
        final NativeView view = this.layout.nativeView();
        if (view.getFileScannerResult() != null) {
            try {
                final ExportDialog export = new ExportDialog(this.layout.shell(), this.exportThreads, view);
                export.dialog().addDisposeListener(new DisposeListener() {

                    @Override
                    public void widgetDisposed(DisposeEvent evt) {
                        ((Shell) evt.widget).getParent().setEnabled(true);
                    }
                });
                this.layout.shell().setEnabled(false);
                Dialogs.alignDialog(export.dialog(), SWT.CENTER, SWT.CENTER);
                export.dialog().open();
            } catch (final ResourceUnavailableException e) {
                ApplicationError.open(this.layout.shell(), e);
            }
        }
    }

    void cmdSearchNext() {
        searchAndSelect(true);
    }

    void cmdSearchPrevious() {
        searchAndSelect(false);
    }

    void handleDragStart(DragSourceEvent evt) {
        final ArrayList<Transfer> transfers = new ArrayList<Transfer>(3);
        if (this.layout.cmdEditExport().getEnabled()) {
            final FileScannerResult result = getSelectedResult();
            if (result != null) {
                final String tmpDir = System.getProperty("java.io.tmpdir");
                File file = new File(tmpDir, result.name());
                try {
                    if (!file.createNewFile()) {
                        file = null;
                    }
                } catch (final IOException e) {
                    Debug.exception(e);
                    file = null;
                }
                FileOutputStream fileOS = null;
                FileChannel fileOSChannel = null;
                try {
                    if (file == null) {
                        file = File.createTempFile(result.name(), "");
                    }
                    fileOS = new FileOutputStream(file);
                    fileOSChannel = fileOS.getChannel();
                    final RuntimeLimiter limiter = new RuntimeLimiter(1000);
                    long transferred = 0;
                    final long resultSize = result.end() - result.start();
                    while (transferred < resultSize && !limiter.limit()) {
                        final long position = result.start() + transferred;
                        transferred += result.input().transferTo(position, Math.min(4096, resultSize - position), fileOSChannel);
                    }
                    if (transferred < resultSize) {
                        file.delete();
                        file = null;
                    }
                } catch (final IOException e) {
                    LOG.error("An I/O error occured while writing result ''{0}'' to file ''{1}''.", result, file);
                    file = null;
                } finally {
                    fileOSChannel = Closeables.saveClose(fileOSChannel);
                    fileOS = Closeables.saveClose(fileOS);
                }
                this.dataDragSourceFile = file;
            }
            if (this.dataDragSourceFile != null) {
                transfers.add(FileTransfer.getInstance());
            } else {
                cmdEditExport();
            }
        }
        this.dataDragSource.setTransfer(transfers.toArray(new Transfer[transfers.size()]));
        evt.doit = transfers.size() > 0;
    }

    void handleDragSetData(DragSourceEvent evt) {
        if (FileTransfer.getInstance().isSupportedType(evt.dataType)) {
            if (this.dataDragSourceFile != null) {
                evt.data = new String[] { this.dataDragSourceFile.getAbsolutePath() };
            }
        }
    }

    void handleDragFinished() {
        if (this.dataDragSourceFile != null) {
            this.dataDragSourceFile.delete();
            this.dataDragSourceFile = null;
        }
    }

    private void searchAndSelect(boolean next) {
        final FileScannerResult start = getSelectedResult();
        if (start != null) {
            try {
                final FileScannerResultPattern pattern = new FileScannerResultPattern(this.layout.searchText().getText());
                FileScannerResult[] currentPath;
                FileScannerResult current = start;
                do {
                    currentPath = (next ? current.next() : current.previous());
                    current = currentPath[0];
                } while (start != current && !pattern.match(current));
                if (start != current) {
                    selectSearchResult(currentPath);
                } else {
                    ToolTips.safeDisplay(this.layout.shell(), SWT.ICON_INFORMATION, RB.FILE_SCANNER_UI_TOOLTIP_SEARCH_FAILED, ToolTips.location(this.layout.searchText(), SWT.LEFT, SWT.BOTTOM));
                }
            } catch (final PatternSyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    void cmdGotoFormatStart() {
        this.layout.hexView().gotoFileScannerResultStart();
    }

    void cmdGotoFormatEnd() {
        this.layout.hexView().gotoFileScannerResultEnd();
    }

    @Override
    public void scanStarted() {
        if (this.layout != null) {
            updateStatus(RB.FILE_SCANNER_UI_STATUS_STARTED);
            updateViews();
            this.layout.cmdStopScan().setEnabled(true);
        }
        Debug.heapStatus();
    }

    @Override
    public void scanResult(FileScannerResult result, int resultInput, int totalInputs) {
        if (this.layout != null) {
            if (result.type() == FileScannerResultType.INPUT) {
                updateStatus(RB.FILE_SCANNER_UI_STATUS_SCAN_INPUT, result.name(), resultInput, totalInputs);
            }
            updateViews();
        }
    }

    @Override
    public void scanProgress(int progress, double ratio) {
        if (this.layout != null) {
            this.layout.setScannerProgress(progress, ratio);
        }
    }

    @Override
    public void scanFinished(boolean aborted, Throwable thrown) {
        Debug.heapStatus();
        if (this.layout != null) {
            this.layout.cmdStopScan().setEnabled(false);
            updateStatus(aborted ? RB.FILE_SCANNER_UI_STATUS_STOPPED : RB.FILE_SCANNER_UI_STATUS_FINISHED);
            updateViews();
            if (thrown != null) {
                Dialogs.safeMessageBox(this.layout.shell(), SWT.ICON_ERROR | SWT.OK, RB.MESSAGES, RB.MESSAGE_SCAN_EXCEPTION, Exceptions.getLocalizedMessage(thrown));
            }
        }
    }

    private void updateTitle() {
        try {
            this.layout.shell().setText(this.layout.ctx().loadWidgetText(RB.FILE_SCANNER_UI_FILE, this.scanner.result().name()));
        } catch (final ResourceUnavailableException e) {
            ApplicationError.open(this.layout.shell(), e);
        }
    }

    private void updateStatus(String[] key, Object... arguments) {
        try {
            this.layout.setScannerStatus(this.layout.ctx().loadWidgetText(key, arguments));
        } catch (final ResourceUnavailableException e) {
            ApplicationError.open(this.layout.shell(), e);
        }
    }

    private void updateViews() {
        final FileScannerResult result = (this.scanner != null ? this.scanner.result() : null);
        if (result != null) {
            TreeItem resultItem = this.navTreeItemMap.get(result);
            int initialNestedItemCount;
            boolean selectResultItem;
            if (resultItem != null) {
                initialNestedItemCount = resultItem.getItemCount();
                selectResultItem = false;
            } else {
                resultItem = new TreeItem(this.layout.navTree(), SWT.NONE);
                initialNestedItemCount = 0;
                selectResultItem = true;
            }
            if (updateNavTreeItem(resultItem, result) > 0 && initialNestedItemCount == 0) {
                resultItem.setExpanded(true);
            }
            if (selectResultItem) {
                this.layout.navTree().setSelection(resultItem);
                handleNavTreeItemSelection(resultItem);
            }
        }
    }

    private int updateNavTreeItem(TreeItem resultItem, FileScannerResult result) {
        final boolean fullUpdate = !result.equals(resultItem.getData());
        if (fullUpdate) {
            resultItem.setData(result);
            this.navTreeItemMap.put(result, resultItem);
            resultItem.setText(result.name());
            switch(result.type()) {
                case INPUT:
                    resultItem.setImage(this.layout.getNavTreeInputImage(result.name()));
                    break;
                case FORMAT:
                    resultItem.setImage(this.layout.getNavTreeFormatImage());
                    break;
                case DECODED:
                    resultItem.setImage(this.layout.getNavTreeDecodedImage());
                    break;
            }
        }
        final FileScannerResult[] nestedResults = result.children();
        resultItem.setItemCount(nestedResults.length);
        for (int nestedResultIndex = 0; nestedResultIndex < nestedResults.length; nestedResultIndex++) {
            final FileScannerResult nestedResult = nestedResults[nestedResultIndex];
            final TreeItem nestedResultItem = this.navTreeItemMap.get(nestedResult);
            if (nestedResultItem != null) {
                int nestedResultIndex2 = resultItem.indexOf(nestedResultItem);
                while (nestedResultIndex2 < nestedResultIndex) {
                    final FileScannerResult nestedResult2 = nestedResults[nestedResultIndex2];
                    final TreeItem nestedResultItem2 = new TreeItem(resultItem, SWT.NONE, nestedResultIndex2);
                    this.navTreeItemMap.put(nestedResult2, nestedResultItem2);
                    updateNavTreeItem(nestedResultItem2, nestedResult2);
                    nestedResultIndex2++;
                }
                updateNavTreeItem(nestedResultItem, nestedResult);
            }
        }
        return nestedResults.length;
    }

    void handleNavTreeItemUpdate(TreeItem item, int index) {
        final TreeItem parentItem = item.getParentItem();
        if (parentItem != null) {
            final FileScannerResult parentResult = getItemResult(parentItem);
            if (parentResult != null) {
                final FileScannerResult[] results = parentResult.children();
                if (index < results.length) {
                    updateNavTreeItem(item, results[index]);
                }
            }
        }
    }

    void handleNavTreeItemSelection(TreeItem item) {
        final FileScannerResult result = getItemResult(item);
        this.layout.hexView().setFileScannerResult(result);
        this.layout.nativeView().setFileScannerResult(result);
        if (result != null) {
            this.layout.cmdEditCopy().setEnabled(result.queryDataHandlers().length > 0);
            this.layout.cmdEditExport().setEnabled(result.queryExportHandlers().length > 0);
            this.layout.searchText().setEnabled(true);
            this.layout.cmdSearchPrevious().setEnabled(true);
            this.layout.cmdSearchNext().setEnabled(true);
            this.layout.cmdGotoFormatStart().setEnabled(true);
            this.layout.cmdGotoFormatEnd().setEnabled(true);
        } else {
            this.layout.cmdEditCopy().setEnabled(false);
            this.layout.cmdEditExport().setEnabled(false);
            this.layout.searchText().setEnabled(false);
            this.layout.cmdSearchPrevious().setEnabled(false);
            this.layout.cmdSearchNext().setEnabled(false);
            this.layout.cmdGotoFormatStart().setEnabled(false);
            this.layout.cmdGotoFormatEnd().setEnabled(false);
        }
    }

    private void selectSearchResult(FileScannerResult[] path) {
        int pathIndex = path.length - 1;
        TreeItem parentItem = null;
        while (pathIndex >= 0) {
            final FileScannerResult itemResult = path[pathIndex];
            TreeItem item = this.navTreeItemMap.get(itemResult);
            if (item == null) {
                assert parentItem != null;
                final FileScannerResult[] parentChildren = path[pathIndex + 1].children();
                int itemIndex = 0;
                while (!itemResult.equals(parentChildren[itemIndex])) {
                    itemIndex++;
                }
                item = parentItem.getItem(itemIndex);
                updateNavTreeItem(item, itemResult);
            }
            if (pathIndex == 0) {
                item.getParent().setSelection(item);
                handleNavTreeItemSelection(item);
            }
            pathIndex--;
            parentItem = item;
        }
    }

    private FileScannerResult getSelectedResult() {
        final TreeItem[] selection = this.layout.navTree().getSelection();
        FileScannerResult result = null;
        if (selection.length == 1) {
            result = getItemResult(selection[0]);
        }
        return result;
    }

    private static FileScannerResult getItemResult(TreeItem item) {
        return (item != null ? (FileScannerResult) item.getData() : null);
    }
}
