package be.novelfaces.webdriver;

import static be.novelfaces.showcase.webdriver.festassert.SourcePanelAssert.assertThat;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import be.novelfaces.component.webdriver.SourcePanel;
import be.novelfaces.showcase.webdriver.util.ResourceBundleManager;

public class WebdriverRule extends TestWatcher {

    private static final String BUILD_INDENTIFIER = "Build_" + new DateTime().toString("yyyyMMdd_HHmmss");

    private final String basePath;

    public WebdriverRule() {
        String property = System.getProperty("browser");
        Browser browser = Browser.FIREFOX;
        if (StringUtils.isNotBlank(property)) {
            browser = Browser.valueOf(property);
        }
        WebDriverManager.initWebDriver(browser);
        basePath = System.getProperty("profile") != null ? "" : "/novelfaces-showcase";
    }

    @Override
    protected void starting(Description description) {
        WebDriverManager.getWebdriver().get("http://localhost:8080" + basePath + "/");
    }

    @Override
    protected void failed(Throwable e, Description description) {
        final String testOutputDirectory = "./target";
        WebDriver webdriver = WebDriverManager.getWebdriver();
        TakesScreenshot takesScreenshot = (TakesScreenshot) webdriver;
        String pageSource = webdriver.getPageSource();
        File screenshotAs = takesScreenshot.getScreenshotAs(OutputType.FILE);
        String path = testOutputDirectory + "/failed-webdriver-tests" + "/" + BUILD_INDENTIFIER + "/" + description.getClassName() + "_" + description.getMethodName() + "/";
        try {
            FileUtils.copyFile(screenshotAs, new File(path + "screenshot.png"));
            FileUtils.writeStringToFile(new File(path + "source.html"), pageSource);
            e.printStackTrace(new PrintStream(new File(path + "trace.txt")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return ResourceBundleManager.getResourceBundle().getProperty(key);
    }

    public void assertSourcePanel(SourcePanel sourcePanel, String headerKey, String contentToContain) {
        assertThat(sourcePanel).isRendered().isCollapsed(true).hasHeader(getProperty(headerKey));
        sourcePanel.toggle();
        assertThat(sourcePanel).isCollapsed(false).containsContent(contentToContain);
    }
}
