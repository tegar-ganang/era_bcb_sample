package tests.com.ivis.xprocess.abbot.loading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import junit.extensions.ForkedPDETestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import tests.com.ivis.xprocess.ui.util.UITestUtil;
import abbot.tester.swt.Robot;
import com.ivis.xprocess.framework.impl.DatasourceDescriptor;
import com.ivis.xprocess.framework.impl.DatasourceDescriptorFactory;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.load.DatasourceLoadException;
import com.ivis.xprocess.ui.load.LoadManager;
import com.ivis.xprocess.ui.util.DatasourceUtil;
import com.ivis.xprocess.util.FileUtils;

public class TestDatasourceLoading extends ForkedPDETestCase {

    private UITestUtil uitu;

    public TestDatasourceLoading(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        uitu = UITestUtil.getInstance();
        uitu.ensureXProcessIsStarted();
    }

    public void testDefaultDatasourceLoading() {
        final IPreferenceStore preferenceStore = UIPlugin.getDefault().getPreferenceStore();
        preferenceStore.setValue("ivis.xprocess.current.sourcelocation", "");
        File defaultDSFile = new File(DatasourceUtil.getDefaultLocation());
        if (defaultDSFile.exists()) {
            FileUtils.deleteDir(defaultDSFile);
        }
        assertTrue(!defaultDSFile.exists());
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                String datasourceLocation = preferenceStore.getString("ivis.xprocess.current.sourcelocation");
                try {
                    LoadManager.startupLoader(datasourceLocation);
                    DatasourceDescriptor datasourceDescriptor = LoadManager.getDatasourceDescriptor();
                    assertEquals(DatasourceUtil.getDefaultLocation(), datasourceDescriptor.getAbsolutePath());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                preferenceStore.setValue("ivis.xprocess.current.sourcelocation", DatasourceUtil.getDefaultLocation());
                String datasourceLocation = preferenceStore.getString("ivis.xprocess.current.sourcelocation");
                try {
                    LoadManager.startupLoader(datasourceLocation);
                    DatasourceDescriptor datasourceDescriptor = LoadManager.getDatasourceDescriptor();
                    assertEquals(DatasourceUtil.getDefaultLocation(), datasourceDescriptor.getAbsolutePath());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
    }

    public void testDatasourceLoadingByDescriptor() {
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                DatasourceDescriptor datasourceDescriptor = DatasourceDescriptorFactory.getDefault().getDescriptor(DatasourceUtil.getDefaultLocation());
                try {
                    LoadManager.load(datasourceDescriptor);
                    datasourceDescriptor = LoadManager.getDatasourceDescriptor();
                    assertEquals(DatasourceUtil.getDefaultLocation(), datasourceDescriptor.getAbsolutePath());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
    }

    public void testLoadingNonExistentDatasource() {
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                String datasourceLocation = DatasourceUtil.getDatasourceRoot() + File.separator + "this datasource does not exist";
                try {
                    LoadManager.startupLoader(datasourceLocation);
                } catch (DatasourceLoadException e) {
                    assertTrue(LoadManager.hasFailedToLoad());
                }
            }
        });
    }

    public void testLoadingLockedDatasource() {
        String filePath = "C:\\temp\\xprocess.lock";
        final File tempfile = new File(filePath);
        if (!tempfile.exists()) {
            BufferedWriter out;
            try {
                out = new BufferedWriter(new FileWriter(tempfile));
                out.write("");
                out.close();
            } catch (IOException e) {
                fail("Unable to create the lock file in the temp directory");
            }
        }
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                String datasourceLocation = DatasourceUtil.getDefaultLocation();
                try {
                    FileUtils.copyFile(tempfile, new File(datasourceLocation + File.separator + "local" + File.separator + "xprocess.lock"));
                } catch (IOException e1) {
                    fail("Unable to move the lock file to the local directory - " + e1);
                }
                try {
                    LoadManager.startupLoader(datasourceLocation);
                    assertTrue(LoadManager.hasFailedToLoad());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
    }

    public void testLoadingPre2Dot5Datasource() {
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                String datasourceLocation = DatasourceUtil.getDatasourceRoot() + File.separator + "212";
                DatasourceDescriptor datasourceDescriptor = DatasourceDescriptorFactory.getDefault().getDescriptor(datasourceLocation);
                assertTrue(datasourceDescriptor.isOld());
                try {
                    LoadManager.startupLoader(datasourceLocation);
                    assertTrue(LoadManager.hasFailedToLoad());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
        Robot.syncExec(uitu.getDisplay(), null, new Runnable() {

            public void run() {
                String datasourceLocation = DatasourceUtil.getDatasourceRoot() + File.separator + "212";
                DatasourceDescriptor datasourceDescriptor = DatasourceDescriptorFactory.getDefault().getDescriptor(datasourceLocation);
                assertTrue(datasourceDescriptor.isOld());
                try {
                    LoadManager.load(datasourceDescriptor);
                    assertTrue(LoadManager.hasFailedToLoad());
                } catch (DatasourceLoadException e) {
                    fail("DatasourceLoadException - " + e.getMessage());
                }
            }
        });
    }
}
