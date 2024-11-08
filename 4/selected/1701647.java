package org.kadosu.ui.views;

import java.io.IOException;
import java.util.Collection;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.kadosu.exception.KDSCancelException;
import org.kadosu.indexer.KadosuIndexer;
import org.kadosu.misc.Utils;
import org.kadosu.ui.KadosuApplication;
import org.kadosu.ui.plugin.KadosuPlugin;

class IndexButtonMouseListener extends MouseAdapter {

    private final IndexerView fIndexerView;

    /**
   * @param view
   */
    IndexButtonMouseListener(final IndexerView view) {
        fIndexerView = view;
    }

    @Override
    public void mouseUp(final MouseEvent mouseE) {
        boolean overwrite = true;
        final String indexName = fIndexerView.getIndexNameCombo().getText();
        final String pathToIndex = fIndexerView.getPathToIndexText().getText();
        try {
            final String indexDir = KadosuApplication.getIndexDirectory();
            final String indexPath = Utils.concatePaths(indexDir, indexName);
            final Collection indexDirC = Utils.getLuceneIndexes(indexDir);
            if (indexDirC.contains(indexName)) {
                final int buttonPressed = fIndexerView.openDecisionMsgBox("Index allready exists", "The index you want to create allready exists.\nOverwrite it?");
                switch(buttonPressed) {
                    case SWT.YES:
                        overwrite = true;
                        break;
                    case SWT.NO:
                        overwrite = false;
                        break;
                    default:
                        throw new KDSCancelException("Indexing cancelled");
                }
            }
            final KadosuIndexer job = new KadosuIndexer(pathToIndex, indexPath, overwrite);
            job.setUser(true);
            job.addJobChangeListener(job);
            job.setPriority(Job.LONG);
            job.schedule();
            KadosuPlugin.getProgressService().showInDialog(fIndexerView.getShell(), job);
            fIndexerView.initIndexNameCombo();
        } catch (final IOException e) {
            fIndexerView.openErrorMsgBox("IOException", "Possibly can't create index " + indexName + " in your home dir.\n" + e.getLocalizedMessage());
        } catch (final KDSCancelException e) {
        }
    }
}
