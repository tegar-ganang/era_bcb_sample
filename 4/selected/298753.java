package abbot.swt.tester.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import abbot.swt.display.IntResult;
import abbot.swt.script.Condition;
import abbot.swt.tester.ComboTester;
import abbot.swt.tester.WidgetTester;
import abbot.swt.utilities.Wait;

public class ComboTesterTest extends WidgetTestFixture {

    private static final int ENTRY_COUNT = 10;

    private static final int[] INDICES = new int[] { 2, 0, 0, 5, 8, 9, 9, 4 };

    /** See constructor. */
    private final ComboTester tester;

    /** See constructor. */
    private final String[] entries;

    /** See setUp()/tearDown(). */
    private Shell shell;

    /** See setUp()/tearDown(). */
    private Combo simpleRWCombo;

    /** See setUp()/tearDown(). */
    private Combo simpleROCombo;

    /** See setUp()/tearDown(). */
    private Combo dropDownRWCombo;

    /** See setUp()/tearDown(). */
    private Combo dropDownROCombo;

    public ComboTesterTest(String name) {
        super(name);
        tester = (ComboTester) WidgetTester.getTester(Combo.class);
        entries = new String[ENTRY_COUNT];
        for (int i = 0; i < ENTRY_COUNT; i++) {
            entries[i] = "entry" + i;
        }
    }

    protected void setUpDisplay() {
        shell = new Shell(getDisplay(), SWT.SHELL_TRIM);
        shell.setText(getName());
        shell.setLayout(new GridLayout(2, false));
        new Label(shell, SWT.NONE).setText("Simple read/write:");
        simpleRWCombo = new Combo(shell, SWT.SIMPLE | SWT.BORDER);
        simpleRWCombo.setItems(entries);
        simpleRWCombo.setData("id", "simple, read/write");
        new Label(shell, SWT.NONE).setText("Simple read-only:");
        simpleROCombo = new Combo(shell, SWT.SIMPLE | SWT.READ_ONLY | SWT.BORDER);
        simpleROCombo.setItems(entries);
        simpleROCombo.setData("id", "simple, read-only");
        new Label(shell, SWT.NONE).setText("Drop-down read/write:");
        dropDownRWCombo = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER);
        dropDownRWCombo.setItems(entries);
        dropDownRWCombo.setData("id", "drop-down, read/write");
        new Label(shell, SWT.NONE).setText("Drop-down read-only:");
        dropDownROCombo = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
        dropDownROCombo.setItems(entries);
        dropDownROCombo.setData("id", "drop-down, read-only");
        shell.pack();
        shell.open();
        shell.forceActive();
    }

    protected void tearDownDisplay() {
        closeShell(shell);
    }

    public void testGetItems() {
        assertGetItems(simpleRWCombo);
        assertGetItems(simpleROCombo);
        assertGetItems(dropDownRWCombo);
        assertGetItems(dropDownROCombo);
    }

    private void assertGetItems(Combo combo) {
        String[] items = tester.getItems(combo);
        assertEquals(entries.length, items.length);
        for (int i = 0; i < entries.length; i++) {
            assertTrue(entries[i].equals(items[i]));
            assertTrue(entries[i].equals(tester.getItem(combo, i)));
        }
    }

    public void testActionSelectIndex() {
        assertSelectIndex(simpleRWCombo);
        assertSelectIndex(simpleROCombo);
        assertSelectIndex(dropDownRWCombo);
        assertSelectIndex(dropDownROCombo);
    }

    private void assertSelectIndex(Combo combo) {
        final SelectedListener listener = new SelectedListener();
        addSelectionListener(combo, listener);
        try {
            int previous = -1;
            for (int index : INDICES) {
                listener.reset(index);
                tester.selectIndex(combo, index);
                assertEquals(index, getSelectionIndex(combo));
                if (index != previous) {
                    Wait.wait(new Condition() {

                        public boolean test() {
                            return listener.isSelected();
                        }
                    }, 2000);
                }
                previous = index;
            }
        } finally {
            removeSelectionListener(combo, listener);
        }
    }

    private int getSelectionIndex(final Combo combo) {
        return syncExec(new IntResult() {

            public int result() {
                return combo.getSelectionIndex();
            }
        });
    }

    public void testActionSelectItem() {
        assertSelectItem(simpleRWCombo);
        assertSelectItem(simpleROCombo);
        assertSelectItem(dropDownRWCombo);
        assertSelectItem(dropDownROCombo);
    }

    private void assertSelectItem(Combo combo) {
        final SelectedListener listener = new SelectedListener();
        addSelectionListener(combo, listener);
        try {
            int previous = -1;
            for (int index : INDICES) {
                listener.reset(index);
                tester.selectItem(combo, entries[index]);
                int selectionIndex = getSelectionIndex(combo);
                assertEquals(index, selectionIndex);
                if (index != previous) {
                    Wait.wait(new Condition() {

                        public boolean test() {
                            return listener.isSelected();
                        }
                    }, 2000);
                }
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

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public synchronized void widgetSelected(SelectionEvent e) {
            if (((Combo) e.widget).getSelectionIndex() == index) selected = true;
        }
    }

    private void addSelectionListener(final Combo combo, final SelectionListener listener) {
        syncExec(new Runnable() {

            public void run() {
                combo.addSelectionListener(listener);
            }
        });
    }

    private void removeSelectionListener(final Combo combo, final SelectionListener listener) {
        syncExec(new Runnable() {

            public void run() {
                combo.removeSelectionListener(listener);
            }
        });
    }

    public void testAssertions() {
        assertAssertions(simpleRWCombo);
        assertAssertions(simpleROCombo);
        assertAssertions(dropDownRWCombo);
        assertAssertions(dropDownROCombo);
    }

    private void assertAssertions(Combo combo) {
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

    private void select(final Combo combo, final int index) {
        syncExec(new Runnable() {

            public void run() {
                combo.select(index);
            }
        });
    }

    public void testSetText() {
        assertSetText(simpleRWCombo, "no such item");
        assertSetText(simpleROCombo, "no such item!");
        assertSetText(dropDownRWCombo, "no such item");
        assertSetText(dropDownROCombo, "no such item");
        assertSetText(simpleRWCombo, "entry4");
        assertSetText(simpleROCombo, "entry4");
        assertSetText(dropDownRWCombo, "entry4");
        assertSetText(dropDownROCombo, "entry4");
    }

    private void assertSetText(Combo combo, String text) {
        boolean isReadOnly = (tester.getStyle(combo) & SWT.READ_ONLY) != 0;
        boolean isItem = tester.indexOf(combo, text) != -1;
        String expected = isReadOnly && !isItem ? tester.getText(combo) : text;
        tester.setText(combo, text);
        assertEquals(expected, tester.getText(combo));
    }

    public static void main(String[] args) {
        ComboTesterTest test = new ComboTesterTest("main");
        test.setUpDisplay();
        Shell shell = test.shell;
        Display display = shell.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }
}
