package org.uppaal.port.ui.dialog;

import java.util.ArrayList;
import org.uppaal.port.ui.Activator;
import org.uppaal.port.ui.Breakpoint;
import org.uppaal.port.ui.PushButton;
import org.uppaal.port.ui.trace.PortApplication;
import org.uppaal.port.ui.trace.PortTrace.State;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class BreakDialog extends Dialog implements SelectionListener {

    private ArrayList<Breakpoint> breaks;

    private ArrayList<Breakpoint> oldBreaks;

    private PortApplication app;

    private List list;

    private ComponentList cs;

    private Button read;

    private Button write;

    private PushButton remove;

    public BreakDialog(Shell parent, ArrayList<Breakpoint> bps, PortApplication app) {
        super(parent);
        this.app = app;
        oldBreaks = bps;
        breaks = new ArrayList<Breakpoint>();
        for (Breakpoint bp : bps) {
            breaks.add(bp);
        }
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(2, true));
        list = new List(composite, SWT.BORDER | SWT.V_SCROLL);
        list.setLayoutData(Activator.getDefault().grabAndFill);
        list.addSelectionListener(this);
        addBreakpoints();
        Group bp = new Group(composite, SWT.SHADOW_ETCHED_IN);
        bp.setText("Breakpoint");
        bp.setLayout(new GridLayout(1, true));
        bp.setLayoutData(Activator.getDefault().grabAndFill);
        new Label(bp, SWT.NONE).setText("Component");
        cs = new ComponentList(bp, app);
        Composite buttons = new Composite(bp, SWT.NONE);
        buttons.setLayout(new GridLayout(2, true));
        buttons.setLayoutData(Activator.getDefault().hFill);
        read = new Button(buttons, SWT.RADIO);
        read.setSelection(true);
        read.setText("Read");
        write = new Button(buttons, SWT.RADIO);
        write.setText("Write");
        new PushButton(buttons, "Add", "Add breakpoint") {

            @Override
            public void pushed() {
                Breakpoint bp = new RWBreak();
                breaks.add(bp);
                list.add(bp.toString());
            }
        };
        remove = new PushButton(buttons, "Remove", "Remove breakpoint") {

            @Override
            public void pushed() {
                int i = list.getSelectionIndex();
                if (i != -1) {
                    list.remove(i);
                    breaks.remove(i);
                }
                setEnabled(list.getSelectionIndex() != -1);
            }
        };
        remove.setEnabled(list.getSelectionIndex() != -1);
        return composite;
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Breakpoints");
        newShell.setSize(340, 280);
    }

    protected void okPressed() {
        super.okPressed();
        oldBreaks.clear();
        for (Breakpoint bp : breaks) {
            oldBreaks.add(bp);
        }
    }

    private void addBreakpoints() {
        for (Breakpoint b : breaks) {
            list.add(b.toString());
        }
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
        int s = list.getSelectionIndex();
        remove.setEnabled(s != -1);
        if (s != -1) {
            Breakpoint bp = breaks.get(s);
            if (bp instanceof RWBreak) {
                RWBreak rw = (RWBreak) bp;
                read.setSelection(rw.breakOnRead);
                write.setSelection(rw.breakOnWrite);
                cs.select(rw.index);
            }
        }
    }

    private class RWBreak implements Breakpoint {

        int index = cs.getSelectionIndex();

        boolean breakOnRead = read.getSelection();

        boolean breakOnWrite = write.getSelection();

        public boolean stop(State state) {
            String li = state.locations[index];
            if (breakOnRead) return li.equals("</TRIGGERED/>"); else return breakOnWrite && li.startsWith("</FINAL/>");
        }

        @Override
        public String toString() {
            return app.getComponentLabel(index) + (breakOnRead ? ": read" : ": write");
        }
    }
}
