package org.liris.schemerger.ui.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.liris.schemerger.core.dataset.SimSequence;
import org.liris.schemerger.ui.SchEmergerLabelProvider;
import org.liris.schemerger.ui.SchEmergerPlugin;
import org.liris.schemerger.ui.model.TimeScale;
import org.liris.schemerger.ui.model.TraceViewProperties;

public class SequenceView extends ViewPart {

    public static final String ID = "org.liris.schemerger.ui.views.SequenceView";

    private SequenceViewer sequenceViewer;

    private PropertyChangeListener pluginListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SchEmergerPlugin.PROP_UI_ADAPTER) || evt.getPropertyName().equals(SchEmergerPlugin.PROP_PROJECT_PATH)) sequenceViewer.setContents(null);
            final SchEmergerPlugin plugin = SchEmergerPlugin.getDefault();
            if (evt.getPropertyName().equals(SchEmergerPlugin.PROP_ACTIVE_SEQUENCE)) {
                sequenceLabel.setText(plugin.getActiveSequencePath() == null ? "" : plugin.getActiveSequencePath());
                updateViewerContents();
                refreshTimeFormatComboValue();
            }
            if (evt.getPropertyName().equals(SchEmergerPlugin.PROP_TIME_FORMAT)) {
                refreshTimeFormatComboValue();
            }
        }
    };

    private void updateViewerContents() {
        SimSequence<?> activeSequence = SchEmergerPlugin.getDefault().getActiveSequence();
        if (activeSequence != null && SchEmergerPlugin.getDefault().getUIAdapter() != null) {
            this.sequenceViewer.setContents(activeSequence);
            final String timeFormat = SchEmergerPlugin.getDefault().getTimeFormat();
            final long firstDate = activeSequence.getFirst().getDate().asLong();
            final String timeUnit = activeSequence.getTimeUnit();
            startDateLabel.setText("Start: " + SchEmergerLabelProvider.getTimeLabel(firstDate, timeUnit, timeFormat));
            final long lastDate = activeSequence.getLast().getDate().asLong();
            endDateLabel.setText("End: " + SchEmergerLabelProvider.getTimeLabel(lastDate, activeSequence.getTimeUnit(), timeFormat));
            nbEventsLabel.setText("Size: " + activeSequence.size());
            timeFormatCombo.removeAll();
            for (String format : SchEmergerPlugin.getAvailableTimeFormats(activeSequence.getTimeUnit())) {
                timeFormatCombo.add(format);
            }
            int sel = SchEmergerPlugin.getAvailableTimeFormats(timeUnit).indexOf(SchEmergerPlugin.getDefault().getTimeFormat());
            timeFormatCombo.setSelection(new Point(sel, sel));
            timeFormatCombo.update();
            timeScaleCombo.removeAll();
            for (TimeScale timeScale : TraceViewProperties.getAvailableTimeScales(activeSequence.getTimeUnit())) {
                timeScaleCombo.add(timeScale.getLabel());
            }
            sel = TraceViewProperties.getAvailableTimeScales(activeSequence.getTimeUnit()).indexOf(activeSequence.getTimeUnit());
            timeScaleCombo.setSelection(new Point(sel, sel));
            updateTraceViewPropertiesLabels();
            this.clientArea.layout(true, true);
        }
    }

    private void updateTraceViewPropertiesLabels() {
        timeScaleCombo.setText(sequenceViewer.getTraceViewProperties().getTimeScale().getLabel());
        refreshTimeFormatComboValue();
    }

    private void refreshTimeFormatComboValue() {
        int sel;
        final SimSequence<?> activeSequence = SchEmergerPlugin.getDefault().getActiveSequence();
        if (activeSequence != null) {
            final String timeUnit = activeSequence.getTimeUnit();
            sel = SchEmergerPlugin.getAvailableTimeFormats(timeUnit).indexOf(SchEmergerPlugin.getDefault().getTimeFormat());
            timeFormatCombo.setSelection(new Point(sel, sel));
        }
        timeFormatCombo.update();
        timeFormatCombo.layout(true);
        timeFormatCombo.redraw();
    }

    private Composite clientArea;

    @Override
    public void createPartControl(Composite parent) {
        this.clientArea = parent;
        parent.setLayout(new GridLayout(1, false));
        sequenceLabel = new Text(parent, SWT.WRAP);
        final GridData sequenceLabelLayoutData = new GridData(SWT.FILL, SWT.None, true, false);
        sequenceLabel.setLayoutData(sequenceLabelLayoutData);
        sequenceLabel.setBackground(ColorConstants.menuBackground);
        final String activeSequencePath = SchEmergerPlugin.getDefault().getActiveSequencePath();
        sequenceLabel.setText(activeSequencePath == null ? "null" : activeSequencePath);
        Composite labelContainer = new Composite(parent, SWT.None);
        labelContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        final RowLayout layout = new RowLayout();
        layout.center = true;
        layout.spacing = 20;
        labelContainer.setLayout(layout);
        startDateLabel = new Text(labelContainer, SWT.None);
        startDateLabel.setBackground(ColorConstants.menuBackground);
        endDateLabel = new Text(labelContainer, SWT.None);
        endDateLabel.setBackground(ColorConstants.menuBackground);
        nbEventsLabel = new Text(labelContainer, SWT.None);
        nbEventsLabel.setBackground(ColorConstants.menuBackground);
        Composite timeFormatContainer = new Composite(labelContainer, SWT.None);
        timeFormatContainer.setLayout(layout);
        new Label(timeFormatContainer, SWT.None).setText("Time format: ");
        timeFormatCombo = new CCombo(timeFormatContainer, SWT.SINGLE);
        timeFormatCombo.setEditable(false);
        timeFormatCombo.setBackground(ColorConstants.menuBackground);
        timeFormatCombo.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                final CCombo combo = (CCombo) event.widget;
                final int selectionIndex = combo.getSelectionIndex();
                final String item = combo.getItem(selectionIndex);
                SchEmergerPlugin.getDefault().setTimeFormat(item, true);
            }

            ;
        });
        Composite timeScaleContainer = new Composite(labelContainer, SWT.None);
        timeScaleContainer.setLayout(layout);
        new Label(timeScaleContainer, SWT.None).setText("Scale: ");
        timeScaleCombo = new CCombo(timeScaleContainer, SWT.SINGLE);
        timeScaleCombo.setEditable(false);
        timeScaleCombo.setBackground(ColorConstants.menuBackground);
        timeScaleCombo.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                final CCombo combo = (CCombo) event.widget;
                final int selectionIndex = combo.getSelectionIndex();
                final String item = combo.getItem(selectionIndex);
                for (TimeScale scale : TraceViewProperties.getAvailableTimeScales(SchEmergerPlugin.getDefault().getActiveSequence().getTimeUnit())) {
                    if (scale.getLabel().equals(item)) {
                        sequenceViewer.getTraceViewProperties().setTimeScale(scale);
                        break;
                    }
                }
            }

            ;
        });
        Composite parentComposite = new Composite(parent, SWT.BORDER);
        final GridData sequenceCompositeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        parentComposite.setLayoutData(sequenceCompositeLayoutData);
        this.sequenceViewer = new SequenceViewer(parentComposite, true, this);
        getSite().setSelectionProvider(this.sequenceViewer);
        sequenceViewer.getTraceViewProperties().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                updateTraceViewPropertiesLabels();
            }
        });
        makeActions();
        parent.pack();
    }

    private void makeActions() {
        for (IAction action : sequenceViewer.getAllActions()) {
            getViewSite().getActionBars().getToolBarManager().add(action);
        }
    }

    @Override
    public void dispose() {
        SchEmergerPlugin.getDefault().removePropertyChangeListener(pluginListener);
        super.dispose();
    }

    public ScrollingGraphicalViewer getGraphicalViewer() {
        return this.sequenceViewer;
    }

    private Text sequenceLabel;

    private Text nbEventsLabel;

    private Text startDateLabel;

    private Text endDateLabel;

    private CCombo timeFormatCombo;

    private CCombo timeScaleCombo;

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        SchEmergerPlugin.getDefault().addPropertyChangeListener(pluginListener);
    }

    @Override
    public void setFocus() {
        this.sequenceViewer.getControl().setFocus();
    }
}
