package abbot.swt.tester.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import abbot.swt.display.IntResult;
import abbot.swt.script.Condition;
import abbot.swt.tester.CComboTester;
import abbot.swt.utilities.Wait;

public class CComboTesterTest extends WidgetTestFixture {

    private static final int ENTRY_COUNT = 10;

    private static final int[] INDICES = new int[] { 2, 0, 0, 5, 8, 9, 9, 4 };

    /** See constructor. */
    private final CComboTester tester;

    /** See constructor. */
    private final String[] entries;

    /** See setUp()/tearDown(). */
    private Shell shell;

    /** See setUp()/tearDown(). */
    private CCombo rwCombo;

    /** See setUp()/tearDown(). */
    private CCombo roCombo;

    /** See setUp()/tearDown(). */
    private CCombo rwFlatCombo;

    /** See setUp()/tearDown(). */
    private CCombo roFlatCombo;

    public CComboTesterTest(String name) {
        super(name);
        tester = CComboTester.getCComboTester();
        entries = new String[ENTRY_COUNT];
        for (int i = 0; i < ENTRY_COUNT; i++) {
            entries[i] = "entry" + i;
        }
    }

    protected void setUpDisplay() {
        shell = new Shell(getDisplay(), SWT.SHELL_TRIM);
        shell.setText(getName());
        shell.setLayout(new GridLayout(2, false));
        new Label(shell, SWT.NONE).setText("Standard read/write:");
        rwCombo = new CCombo(shell, SWT.BORDER);
        rwCombo.setItems(entries);
        rwCombo.setData("id", "standard, read/write");
        new Label(shell, SWT.NONE).setText("Standard read-only:");
        roCombo = new CCombo(shell, SWT.READ_ONLY | SWT.BORDER);
        roCombo.setItems(entries);
        roCombo.setData("id", "standard, read-only");
        new Label(shell, SWT.NONE).setText("Flat read/write:");
        rwFlatCombo = new CCombo(shell, SWT.FLAT | SWT.BORDER);
        rwFlatCombo.setItems(entries);
        rwFlatCombo.setData("id", "flat, read/write");
        new Label(shell, SWT.NONE).setText("Flat read-only:");
        roFlatCombo = new CCombo(shell, SWT.FLAT | SWT.READ_ONLY | SWT.BORDER);
        roFlatCombo.setItems(entries);
        roFlatCombo.setData("id", "flat, read-only");
        shell.pack();
        shell.open();
        shell.forceActive();
    }

    protected void tearDownDisplay() {
        closeShell(shell);
    }

    public void testGetItem() {
        assertGetItem(rwCombo);
        assertGetItem(roCombo);
        assertGetItem(rwFlatCombo);
        assertGetItem(roFlatCombo);
    }

    private void assertGetItem(CCombo combo) {
        for (int i = 0; i < entries.length; i++) {
            assertTrue(entries[i].equals(tester.getItem(combo, i)));
        }
    }

    public void testGetItemCount() {
        assertEquals(entries.length, tester.getItemCount(rwCombo));
        assertEquals(entries.length, tester.getItemCount(roCombo));
        assertEquals(entries.length, tester.getItemCount(rwFlatCombo));
        assertEquals(entries.length, tester.getItemCount(roFlatCombo));
    }

    public void testGetItems() {
        assertGetItems(rwCombo);
        assertGetItems(roCombo);
        assertGetItems(rwFlatCombo);
        assertGetItems(roFlatCombo);
    }

    private void assertGetItems(CCombo combo) {
        String[] items = tester.getItems(combo);
        assertEquals(entries.length, items.length);
        for (int i = 0; i < entries.length; i++) {
            assertTrue(entries[i].equals(items[i]));
            assertTrue(entries[i].equals(tester.getItem(combo, i)));
        }
    }

    public void testActionSelectIndex() {
        assertSelectIndex(rwCombo);
        assertSelectIndex(roCombo);
        assertSelectIndex(rwFlatCombo);
        assertSelectIndex(roFlatCombo);
    }

    private void assertSelectIndex(CCombo combo) {
        final SelectedListener listener = new SelectedListener();
        addSelectionListener(combo, listener);
        try {
            int previous = -1;
            for (int index : INDICES) {
                listener.reset(index);
                tester.selectIndex(combo, index);
                if (index != previous) {
                    Wait.wait(new Condition() {

                        public boolean test() {
                            return listener.isSelected();
                        }
                    }, 2000);
                }
                assertEquals(index, getSelectionIndex(combo));
                previous = index;
            }
        } finally {
            removeSelectionListener(combo, listener);
        }
    }

    private int getSelectionIndex(final CCombo combo) {
        return syncExec(new IntResult() {

            public int result() {
                return combo.getSelectionIndex();
            }
        });
    }

    public void testActionSelectItem() {
        assertSelectItem(rwCombo);
        assertSelectItem(roCombo);
        assertSelectItem(rwFlatCombo);
        assertSelectItem(roFlatCombo);
    }

    private void assertSelectItem(CCombo combo) {
        final SelectedListener listener = new SelectedListener();
        addSelectionListener(combo, listener);
        try {
            int previous = -1;
            for (int index : INDICES) {
                listener.reset(index);
                tester.selectItem(combo, entries[index]);
                if (index != previous) {
                    Wait.wait(new Condition() {

                        public boolean test() {
                            return listener.isSelected();
                        }
                    }, 2000);
                }
                assertEquals(index, getSelectionIndex(combo));
                previous = index;
            }
        } finally {
            removeSelectionListener(combo, listener);
        }
    }

    private static class SelectedListener implements SelectionListener {

        private boolean selected;

        private int index;

        public synchronized void reset(int index) {
            this.index = index;
            selected = false;
        }

        public synchronized boolean isSelected() {
            return selected;
        }

        public synchronized void widgetDefaultSelected(SelectionEvent e) {
            fail("unexpected");
        }

        public synchronized void widgetSelected(SelectionEvent e) {
            if (((CCombo) e.widget).getSelectionIndex() == index) selected = true;
        }
    }

    private void addSelectionListener(final CCombo combo, final SelectionListener listener) {
        syncExec(new Runnable() {

            public void run() {
                combo.addSelectionListener(listener);
            }
        });
    }

    private void removeSelectionListener(final CCombo combo, final SelectionListener listener) {
        syncExec(new Runnable() {

            public void run() {
                combo.removeSelectionListener(listener);
            }
        });
    }

    public void testAssertions() {
        assertAssertions(rwCombo);
        assertAssertions(roCombo);
        assertAssertions(rwFlatCombo);
        assertAssertions(roFlatCombo);
    }

    private void assertAssertions(CCombo combo) {
        for (int selectedIndex = 0; selectedIndex < entries.length; selectedIndex++) {
            select(combo, selectedIndex);
            for (int index = 0; index < entries.length; index++) {
                if (index == selectedIndex) {
                    assertTrue(tester.assertIndexSelected(combo, index));
                    assertTrue(tester.assertItemSelected(combo, entries[index]));
                } else {
                    assertFalse(tester.assertIndexSelected(combo, index));
                    assertFalse(tester.assertItemSelected(combo, entries[index]));
                }
            }
        }
    }

    private void select(final CCombo combo, final int index) {
        syncExec(new Runnable() {

            public void run() {
                combo.select(index);
            }
        });
    }

    public void testSetText() {
        assertSetText(rwCombo, "no such item");
        assertSetText(roCombo, "no such item!");
        assertSetText(rwFlatCombo, "no such item");
        assertSetText(roFlatCombo, "no such item");
        assertSetText(rwCombo, "entry4");
        assertSetText(roCombo, "entry4");
        assertSetText(rwFlatCombo, "entry4");
        assertSetText(roFlatCombo, "entry4");
    }

    private void assertSetText(CCombo combo, String text) {
        tester.setText(combo, text);
        assertEquals(text, tester.getText(combo));
    }

    public static void main(String[] args) {
        CComboTesterTest test = new CComboTesterTest("main");
        test.setUpDisplay();
        Shell shell = test.shell;
        Display display = shell.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }
}
