package com.android.traceview;

import com.android.sdkstats.SdkStatsService;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Properties;

public class MainWindow extends ApplicationWindow {

    private static final String PING_NAME = "Traceview";

    private static final String PING_VERSION = "1.0";

    private TraceReader mReader;

    private String mTraceName;

    public static HashMap<String, String> sStringCache = new HashMap<String, String>();

    public MainWindow(String traceName, TraceReader reader) {
        super(null);
        mReader = reader;
        mTraceName = traceName;
    }

    public void run() {
        setBlockOnOpen(true);
        open();
        Display.getCurrent().dispose();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Traceview: " + mTraceName);
        shell.setBounds(100, 10, 1282, 900);
    }

    @Override
    protected Control createContents(Composite parent) {
        ColorController.assignMethodColors(parent.getDisplay(), mReader.getMethods());
        SelectionController selectionController = new SelectionController();
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        parent.setLayout(gridLayout);
        Display display = parent.getDisplay();
        Color darkGray = display.getSystemColor(SWT.COLOR_DARK_GRAY);
        SashForm sashForm1 = new SashForm(parent, SWT.VERTICAL);
        sashForm1.setBackground(darkGray);
        sashForm1.SASH_WIDTH = 3;
        GridData data = new GridData(GridData.FILL_BOTH);
        sashForm1.setLayoutData(data);
        new TimeLineView(sashForm1, mReader, selectionController);
        new ProfileView(sashForm1, mReader, selectionController);
        return sashForm1;
    }

    /**
     * Convert the old two-file format into the current concatenated one.
     *
     * @param base Base path of the two files, i.e. base.key and base.data
     * @return Path to a temporary file that will be deleted on exit.
     * @throws IOException
     */
    private static String makeTempTraceFile(String base) throws IOException {
        File temp = File.createTempFile(base, ".trace");
        temp.deleteOnExit();
        FileChannel dstChannel = new FileOutputStream(temp).getChannel();
        FileChannel srcChannel = new FileInputStream(base + ".key").getChannel();
        long size = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        srcChannel = new FileInputStream(base + ".data").getChannel();
        dstChannel.transferFrom(srcChannel, size, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
        return temp.getPath();
    }

    /**
     * Returns the tools revision number.
     */
    private static String getRevision() {
        Properties p = new Properties();
        try {
            String toolsdir = System.getProperty("com.android.traceview.toolsdir");
            File sourceProp;
            if (toolsdir == null || toolsdir.length() == 0) {
                sourceProp = new File("source.properties");
            } else {
                sourceProp = new File(toolsdir, "source.properties");
            }
            p.load(new FileInputStream(sourceProp));
            String revision = p.getProperty("Pkg.Revision");
            if (revision != null && revision.length() > 0) {
                return revision;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public static void main(String[] args) {
        TraceReader reader = null;
        boolean regression = false;
        String revision = getRevision();
        if (revision != null) {
            SdkStatsService.ping(PING_NAME, revision, null);
        }
        int argc = 0;
        int len = args.length;
        while (argc < len) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-r")) {
                regression = true;
            } else {
                break;
            }
            argc++;
        }
        if (argc != len - 1) {
            System.out.printf("Usage: java %s [-r] trace%n", MainWindow.class.getName());
            System.out.printf("  -r   regression only%n");
            return;
        }
        String traceName = args[len - 1];
        File file = new File(traceName);
        if (file.exists() && file.isDirectory()) {
            System.out.printf("Qemu trace files not supported yet.\n");
            System.exit(1);
        } else {
            if (!file.exists()) {
                if (new File(traceName + ".trace").exists()) {
                    traceName = traceName + ".trace";
                } else if (new File(traceName + ".data").exists() && new File(traceName + ".key").exists()) {
                    try {
                        traceName = makeTempTraceFile(traceName);
                    } catch (IOException e) {
                        System.err.printf("cannot convert old trace file '%s'\n", traceName);
                        System.exit(1);
                    }
                } else {
                    System.err.printf("trace file '%s' not found\n", traceName);
                    System.exit(1);
                }
            }
            reader = new DmTraceReader(traceName, regression);
        }
        reader.getTraceUnits().setTimeScale(TraceUnits.TimeScale.MilliSeconds);
        new MainWindow(traceName, reader).run();
    }
}
