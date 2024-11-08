package cloudspace.demoitem;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * The class  is used to capture the screen shot of a URL.
 * This uses the capability of Selenium library to do this.
 * 
 * It also depends upon Xvfb, firefox to be installed in
 * the cloudspace server.
 * 
 * @author Sunil Kamalakar
 */
public class ScreenshotCapturer {

    private static final int DISPLAY_NUMBER = 77;

    private static final String XVFB = "/usr/bin/Xvfb -ac";

    private static final String XVFB_COMMAND = XVFB + " :" + DISPLAY_NUMBER;

    private static long DEFAULT_TIMEOUT = 45;

    private static Process process = null;

    private static WebDriver driver = null;

    private static Logger log = Logger.getLogger(ScreenshotCapturer.class);

    /**
	 * External classes should not call this, but instead call captureScreenshot()
	 * @param url
	 * @param fileLoc
	 * @return
	 * @throws Exception
	 */
    public static boolean takeScreenShot(String url, String fileLoc) throws Exception {
        log.debug("Starting XVFB");
        process = Runtime.getRuntime().exec(XVFB_COMMAND);
        log.debug("Creating firefox binary");
        FirefoxBinary firefox = new FirefoxBinary();
        firefox.setEnvironmentProperty("DISPLAY", ":" + DISPLAY_NUMBER);
        driver = new FirefoxDriver(firefox, null);
        log.debug("Get URL: " + url);
        driver.get(url);
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        log.debug("Writing image to file: " + new File(fileLoc).getAbsolutePath());
        FileUtils.copyFile(scrFile, new File(fileLoc));
        driver.close();
        process.destroy();
        driver = null;
        process = null;
        return true;
    }

    public static synchronized void captureScreenshot(String pURI, String iURI) throws Exception {
        captureScreenshot(pURI, iURI, 0l);
    }

    public static synchronized void captureScreenshot(String pURI, String iURI, long timeout) throws Exception {
        final String programURI = pURI;
        final String imageURI = iURI;
        try {
            timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.invokeAll(Arrays.asList(new ScreenshotTask(programURI, imageURI)), timeout, TimeUnit.SECONDS);
            executor.shutdown();
            if (process == null && driver == null) {
                log.debug("Regular exit");
            } else {
                log.debug("Screenshot Process had to be terminated!!!");
            }
        } catch (Exception e) {
            throw new Exception("Screenshot Capture did not terminate correctly\n" + e.getMessage());
        } finally {
            terminate();
        }
    }

    private static void terminate() {
        log.debug("Terminating process and driver");
        if (driver != null) {
            log.debug("Ending driver!!!");
            driver.close();
            driver = null;
        }
        if (process != null) {
            log.debug("Ending process!!!");
            process.destroy();
            process = null;
        }
        log.debug("Terminate complete");
    }

    public static void main(String[] argv) throws Exception {
        captureScreenshot(argv[0], argv[1], Long.parseLong(argv[2]));
    }
}

class ScreenshotTask implements Callable<String> {

    private String programURI, imageURI;

    public ScreenshotTask(String programURI, String imageURI) {
        this.programURI = programURI;
        this.imageURI = imageURI;
    }

    public String call() throws Exception {
        try {
            ScreenshotCapturer.takeScreenShot(programURI, imageURI);
        } catch (Exception e) {
            throw new Exception("Screenshot capture terminated with error\n" + e.getMessage());
        }
        return null;
    }
}
