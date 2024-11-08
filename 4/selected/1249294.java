package portablepgp;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import portablepgp.core.PGPUtils;

/**
 * The main class of the application.
 */
public class PortablePGPApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        copyTwoPolicyJar();
        PGPUtils.loadKeyrings(PGPUtils.PUBLIC_KEYRING_FILE, PGPUtils.PRIVATE_KEYRING_FILE);
        PortablePGPView view = new PortablePGPView(this);
        view.init();
        show(view);
        view.popupWelcome();
    }

    /**
     * local_policy.jar & US_export_policy.jar to JAVA_HOME/jre/lib/security
     */
    private void copyTwoPolicyJar() {
        final String LIB_DIR = "lib" + File.separator;
        final String JAR1 = "local_policy.jar";
        final String JAR2 = "US_export_policy.jar";
        final String sJAVA_SECURITY_LIB = String.format("%s%slib%ssecurity%s", SystemUtils.getJavaHome().getAbsolutePath(), File.separator, File.separator, File.separator, File.separator);
        File JAVA_SECURITY_LIB = new File(sJAVA_SECURITY_LIB);
        if (new File(sJAVA_SECURITY_LIB + JAR1).exists() && new File(sJAVA_SECURITY_LIB + JAR2).exists()) {
            System.out.printf("%s and %s all exist. No need to copy again.\n ", JAR1, JAR2);
            return;
        }
        System.out.println("JAVA_SECURITY_LIB= " + JAVA_SECURITY_LIB.getAbsolutePath());
        System.out.println("JAR1= " + LIB_DIR + JAR1);
        System.out.println("JAR2= " + LIB_DIR + JAR2);
        try {
            FileUtils.copyFileToDirectory(new File(LIB_DIR + JAR1), JAVA_SECURITY_LIB);
            FileUtils.copyFileToDirectory(new File(LIB_DIR + JAR2), JAVA_SECURITY_LIB);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.printf("%s and %s copied to %s.\n ", JAR1, JAR2, sJAVA_SECURITY_LIB);
        JAVA_SECURITY_LIB = null;
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of PortablePGPApp
     */
    public static PortablePGPApp getApplication() {
        return Application.getInstance(PortablePGPApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) throws Exception {
        launch(PortablePGPApp.class, args);
    }
}
