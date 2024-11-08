package org.thadeus.rssreader.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.thadeus.rssreader.RSSReaderPlugin;
import de.nava.informa.core.ChannelIF;

public class ChannelListView extends RSSReaderView {

    private static final String CHANNEL_URLS_PROPERTY = RSSReaderPlugin.CHANNEL_URLS_PROPERTY;

    public static final String VIEW_ID = ChannelListView.class.getName();

    private TableViewer viewer;

    class ViewContentProvider implements IStructuredContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            return (Object[]) parent;
        }
    }

    ChannelIF[] getChannels() {
        Object input = viewer.getInput();
        if (input == null) {
            return new ChannelIF[0];
        } else {
            return (ChannelIF[]) input;
        }
    }

    class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

        public String getColumnText(Object obj, int index) {
            return ((ChannelIF) obj).getTitle();
        }

        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    /**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        IToolBarManager tbMgr = getViewSite().getActionBars().getToolBarManager();
        tbMgr.add(new Action("Add Channel") {

            public void run() {
                try {
                    InputDialog d = new InputDialog(getViewSite().getShell(), "", "RSS URL:", "http://", new IInputValidator() {

                        public String isValid(String newText) {
                            try {
                                new URL(newText);
                                return null;
                            } catch (Exception e) {
                                return e.toString();
                            }
                        }
                    });
                    d.open();
                    String value = d.getValue();
                    if (value != null) {
                        try {
                            URL u = new URL(value);
                            IProgressMonitor monitor = createProgressMonitor();
                            try {
                                synchronized (CHANNEL_URLS_PROPERTY) {
                                    List urls = new LinkedList(Arrays.asList((URL[]) data.get(CHANNEL_URLS_PROPERTY)));
                                    urls.add(u);
                                    data.put(CHANNEL_URLS_PROPERTY, urls.toArray(new URL[0]));
                                    RSSReaderPlugin.getDefault().refreshData(monitor);
                                }
                            } finally {
                                monitor.done();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        tbMgr.add(new Action("Remove Selected") {

            public void run() {
                List selected = ((IStructuredSelection) viewer.getSelection()).toList();
                List everything = new LinkedList(Arrays.asList((ChannelIF[]) viewer.getInput()));
                everything.removeAll(selected);
                data.put(CHANNELS_PROPERTY, everything.toArray(new ChannelIF[0]));
                IProgressMonitor monitor = getViewSite().getActionBars().getStatusLineManager().getProgressMonitor();
                try {
                    List urls = new LinkedList(Arrays.asList((URL[]) data.get(CHANNEL_URLS_PROPERTY)));
                    for (Iterator itr = selected.iterator(); itr.hasNext(); ) {
                        ChannelIF channel = (ChannelIF) itr.next();
                        urls.add(channel.getLocation());
                    }
                    synchronized (CHANNEL_URLS_PROPERTY) {
                        data.put(CHANNEL_URLS_PROPERTY, urls.toArray(new URL[0]));
                    }
                } finally {
                    monitor.done();
                }
            }
        });
        data.addListener(CHANNELS_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                getViewSite().getShell().getDisplay().syncExec(new Runnable() {

                    public void run() {
                        viewer.setInput(evt.getNewValue());
                    }
                });
            }
        });
        viewer.setInput(data.get(CHANNELS_PROPERTY));
        RSSReaderPlugin.getDefault().ensureDataLoaded(createProgressMonitor());
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
