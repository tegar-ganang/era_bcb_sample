package ui.swt;

import logging.LogBuffer;
import logging.LoggingFacility;
import main.AssemblyCompiler;
import main.SystemConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import simulator.processor.Processor;
import simulator.processor.instructions.Instruction;
import ui.StdOutConsole;
import java.io.File;
import java.util.List;

public class SWTUI {

    static boolean keepGoing = true;

    static Display display = new Display();

    static Shell topShell;

    static Table table;

    static PipelineTableViewer pipelineTable;

    static MemoryViewer memoryViewer;

    static IntegerRegisterViewer integerRegisterViewer;

    static FloatRegisterViewer floatRegisterViewer;

    static int cycles = 14;

    static Table memory;

    static WidgetConsole console;

    static Composite group1;

    static Composite group2;

    static Composite group3;

    static Composite group4;

    static Composite group5;

    static int MAX_CYCLES = 100000;

    static Processor processor;

    static Color colorIX = new Color(display, 255, 64, 64);

    static Color colorDX = new Color(display, 160, 200, 0);

    static Color colorMX = new Color(display, 255, 160, 200);

    static Color colorAX = new Color(display, 160, 200, 255);

    static Color colorWB = new Color(display, 200, 160, 255);

    static Color colorIF = new Color(display, 255, 255, 0);

    static Color colorID = new Color(display, 255, 200, 0);

    static Color colorMEM = new Color(display, 64, 255, 64);

    static Color colorWhite = display.getSystemColor(SWT.COLOR_WHITE);

    static List<Instruction> instructions;

    private static final String HEADER = SystemConstants.HEADER;

    public static void startUI() {
        LoggingFacility stdConsole = new StdOutConsole();
        stdConsole.logn("Starting SWT-based UI.");
        processor = new Processor();
        instructions = processor.getInstructions();
        topShell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN);
        topShell.setText(HEADER);
        topShell.setLayout(new RowLayout(SWT.VERTICAL | SWT.FILL));
        Monitor primaryMonitor = display.getPrimaryMonitor();
        Rectangle bounds = primaryMonitor.getBounds();
        Rectangle rect = topShell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        topShell.setLocation(x, y);
        pipelineTable = new PipelineTableViewer(topShell, processor);
        Composite buttonComposite = new Composite(topShell, SWT.BORDER);
        buttonComposite.setLayoutData(new RowData(topShell.getClientArea().width - 2, 95));
        buttonComposite.setLayout(new GridLayout(5, false));
        createButtons(buttonComposite);
        console = new WidgetConsole(topShell);
        memoryViewer = new MemoryViewer(topShell, processor);
        integerRegisterViewer = new IntegerRegisterViewer(topShell, processor);
        floatRegisterViewer = new FloatRegisterViewer(topShell, processor);
        topShell.pack();
        topShell.open();
        while (!topShell.isDisposed() && keepGoing) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
        stdConsole.logn("SWT-based UI closed.");
    }

    /**
     * **************** *
     */
    private static void changeProcessor(Processor newProcessor) {
        if (newProcessor == null) {
            throw new NullPointerException("Cannot set null processor.");
        }
        processor = newProcessor;
        instructions = processor.getInstructions();
        pipelineTable.setProcessor(processor);
        memoryViewer.setProcessor(processor);
        integerRegisterViewer.setProcessor(processor);
        floatRegisterViewer.setProcessor(processor);
        updateViewers();
    }

    private static void updateViewers() {
        pipelineTable.update();
        memoryViewer.update();
        integerRegisterViewer.update();
        floatRegisterViewer.update();
    }

    private static void createButtons(Composite buttons) {
        group1 = new Composite(buttons, SWT.NONE);
        group1.setLayoutData(new GridData(GridData.FILL_BOTH));
        group1.setLayout(new GridLayout(1, true));
        group2 = new Composite(buttons, SWT.NONE);
        group2.setLayoutData(new GridData(GridData.FILL_BOTH));
        group2.setLayout(new GridLayout(1, true));
        group3 = new Composite(buttons, SWT.NONE);
        group3.setLayoutData(new GridData(GridData.FILL_BOTH));
        group3.setLayout(new GridLayout(1, true));
        group4 = new Composite(buttons, SWT.NONE);
        group4.setLayoutData(new GridData(GridData.FILL_BOTH));
        group4.setLayout(new GridLayout(1, true));
        group5 = new Composite(buttons, SWT.NONE);
        group5.setLayoutData(new GridData(GridData.FILL_BOTH));
        group5.setLayout(new GridLayout(1, true));
        stepButton(group1);
        runUnattendedButton(group2);
        runCycles(group2);
        integerRegisterButton(group3);
        floatRegisterButton(group3);
        dataMemoryButton(group3);
        loadAssemblyButton(group4);
        processorSettingsButton(group4);
        pipelineScreenshotButton(group5);
        quitButton(group5);
    }

    private static void quitButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Quit");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                keepGoing = false;
            }
        });
    }

    private static void pipelineScreenshotButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Pipeline screenshot");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                Image image = pipelineTable.takeShot();
                FileDialog fd = new FileDialog(topShell, SWT.SAVE);
                fd.setFilterExtensions(new String[] { "*.png", "*.*" });
                String filename = fd.open();
                if (filename != null) {
                    boolean saveFile = true;
                    if (new File(filename).exists()) {
                        MessageBox box = new MessageBox(topShell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                        box.setMessage(String.format("File %s already exists, overwrite?", filename));
                        saveFile = (box.open() != SWT.CANCEL);
                    }
                    if (saveFile) {
                        ImageLoader loader = new ImageLoader();
                        ImageData imageData = image.getImageData();
                        loader.data = new ImageData[] { imageData };
                        try {
                            loader.save(filename, SWT.IMAGE_PNG);
                            console.logf("Pipeline screenshot saved to %s.\n", filename);
                        } catch (SWTException ex) {
                            if (ex.code == SWT.ERROR_IO) {
                                console.logf("Cannot save screenshot to %s, I/O problem (%s).\n", filename, ex.throwable.getMessage());
                            } else {
                                throw ex;
                            }
                        }
                    }
                }
                image.dispose();
            }
        });
    }

    private static void processorSettingsButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Processor settings");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                new ProcessorSettings(topShell, processor, console).open();
            }
        });
    }

    private static void loadAssemblyButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Load assembly");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(topShell, SWT.OPEN);
                fd.setFilterExtensions(new String[] { "*.s", "*.*" });
                String filename = fd.open();
                if (filename != null) {
                    Processor newProcessor = new Processor();
                    newProcessor.reset();
                    LogBuffer compilerLog = AssemblyCompiler.compileAssembly(fd.getFilterPath() + "\\" + fd.getFileName(), newProcessor);
                    console.logb(compilerLog);
                    changeProcessor(newProcessor);
                    topShell.setText(HEADER + " (" + filename + ")");
                }
            }
        });
    }

    private static void dataMemoryButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Open data memory");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                memoryViewer.show();
            }
        });
    }

    private static void floatRegisterButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Open float registers");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                floatRegisterViewer.show();
            }
        });
    }

    private static void integerRegisterButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Open integer registers");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                integerRegisterViewer.show();
            }
        });
    }

    private static void runCycles(Composite composite) {
        GridData gd;
        final Composite cycleRun = new Composite(composite, SWT.NONE);
        cycleRun.setLayout(new GridLayout(2, false));
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 24;
        cycleRun.setLayoutData(gd);
        final Spinner cycleCount = new Spinner(cycleRun, SWT.NONE);
        final Button cycleRunButton = new Button(cycleRun, SWT.NONE);
        gd = new GridData(GridData.FILL_VERTICAL);
        gd.verticalAlignment = GridData.CENTER;
        cycleCount.setLayoutData(gd);
        cycleCount.setMinimum(1);
        cycleCount.setMaximum(99);
        cycleCount.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                int count = cycleCount.getSelection();
                cycleRunButton.setText("Run " + count + " cycle" + (count > 1 ? "s" : ""));
                cycleRun.layout();
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.CENTER;
        cycleRunButton.setLayoutData(gd);
        cycleRunButton.setAlignment(SWT.CENTER);
        cycleRunButton.setText("Run 1 cycle");
        cycleRunButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int cycle = 0;
                int totalCycles = cycleCount.getSelection();
                while ((cycle < totalCycles) && !processor.isStopped()) {
                    processor.advance();
                    cycle++;
                }
                updateViewers();
                if (processor.isStopped()) {
                    console.logn("Processor stopped, time: " + processor.getTime());
                }
            }
        });
    }

    private static void runUnattendedButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Run unattended");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (processor.isStopped()) {
                    console.logn("Processor stopped, time: " + processor.getTime());
                    return;
                }
                Runnable unattendedRun = new Runnable() {

                    public void run() {
                        display.syncExec(new Runnable() {

                            public void run() {
                                console.logf("Unattended run started (will run for maximum of %d cycles).\n", MAX_CYCLES);
                                compositeSetEnabled(group1, false);
                                compositeSetEnabled(group2, false);
                                compositeSetEnabled(group3, false);
                                compositeSetEnabled(group4, false);
                                compositeSetEnabled(group5, false);
                            }
                        });
                        long start = System.nanoTime();
                        while (!processor.isStopped() && (processor.getTime() < MAX_CYCLES)) {
                            processor.advance();
                        }
                        long end = System.nanoTime();
                        final float time = (end - start) / 1e6f;
                        final int iterations = processor.getTime();
                        if (!display.isDisposed()) {
                            display.syncExec(new Runnable() {

                                public void run() {
                                    console.logf("Unattended run completed. Processor stopped. %d cycles took %.02f msecs (%.01fK inst/s).\n", iterations, time, iterations / time);
                                    updateViewers();
                                    compositeSetEnabled(group1, true);
                                    compositeSetEnabled(group2, true);
                                    compositeSetEnabled(group3, true);
                                    compositeSetEnabled(group4, true);
                                    compositeSetEnabled(group5, true);
                                }
                            });
                        }
                    }
                };
                Thread run = new Thread(unattendedRun);
                run.setPriority(Thread.NORM_PRIORITY - 2);
                run.start();
            }
        });
    }

    private static void stepButton(Composite composite) {
        Button button = new Button(composite, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_BOTH));
        button.setAlignment(SWT.CENTER);
        button.setText("Step");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (!processor.isStopped()) {
                    processor.advance();
                    updateViewers();
                }
                if (processor.isStopped()) {
                    console.logn("Processor stopped, time: " + processor.getTime());
                }
            }
        });
    }

    private static void compositeSetEnabled(Composite composite, boolean enabled) {
        for (Control c : composite.getChildren()) {
            if (c instanceof Composite) {
                compositeSetEnabled((Composite) c, enabled);
            }
            c.setEnabled(enabled);
        }
    }
}
