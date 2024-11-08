package com.jsystem.webdriver;

import java.io.File;
import jsystem.framework.report.ListenerstManager;
import jsystem.framework.report.Reporter;
import jsystem.utils.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverEventListener;

/**
 * This class if for Event Handler by the Dispatcher of the WebDriverEventListener Class. the
 * 
 * @author Liel Ran ,Create Date - 22.12.11
 */
public class WebDriverScreenshotEventHandler implements WebDriverEventListener {

    private final Reporter reporter;

    private String screenshotFolderName = "Screenshots";

    private final String prefix = "Screenshot";

    private String path;

    public WebDriverScreenshotEventHandler() {
        reporter = ListenerstManager.getInstance();
    }

    public WebDriverScreenshotEventHandler(String screenShotPath, String screenshotFolderName) {
        path = screenShotPath;
        reporter = ListenerstManager.getInstance();
        this.screenshotFolderName = screenshotFolderName;
    }

    @Override
    public void afterChangeValueOf(WebElement arg0, WebDriver arg1) {
        takeScreenshot(arg1, "After ChangeValueOf Screenshot");
    }

    @Override
    public void afterClickOn(WebElement arg0, WebDriver arg1) {
        takeScreenshot(arg1, "after ClickOn Screenshot");
    }

    @Override
    public void afterFindBy(By arg0, WebElement arg1, WebDriver arg2) {
    }

    @Override
    public void afterNavigateBack(WebDriver arg0) {
        takeScreenshot(arg0, "After NavigateBack Screenshot");
    }

    @Override
    public void afterNavigateForward(WebDriver arg0) {
        takeScreenshot(arg0, "After NavigateForward Screenshot");
    }

    @Override
    public void afterNavigateTo(String arg0, WebDriver arg1) {
        takeScreenshot(arg1, "After NavigateTo Screenshot");
    }

    @Override
    public void afterScript(String arg0, WebDriver arg1) {
    }

    /**
	 * Called before {@link WebElement#clear WebElement.clear()}, {@link WebElement#sendKeys WebElement.sendKeys(...)},
	 * or {@link WebElement#toggle WebElement.toggle()}.
	 */
    @Override
    public void beforeChangeValueOf(WebElement arg0, WebDriver arg1) {
        takeScreenshot(arg1, "Before ChangeValueOf Screenshot");
    }

    @Override
    public void beforeClickOn(WebElement arg0, WebDriver arg1) {
        try {
            File scrFile = ((TakesScreenshot) arg1).getScreenshotAs(OutputType.FILE);
            scrFile = copyFileToScreenshotFolder(scrFile);
            addToReport(scrFile, "before ClickOn Screenshot");
        } catch (Exception e) {
        }
    }

    @Override
    public void beforeFindBy(By arg0, WebElement arg1, WebDriver arg2) {
    }

    @Override
    public void beforeNavigateBack(WebDriver arg0) {
        takeScreenshot(arg0, "Before NavigateBack Screenshot");
    }

    @Override
    public void beforeNavigateForward(WebDriver arg0) {
        takeScreenshot(arg0, "Before NavigateForward Screenshot");
    }

    @Override
    public void beforeNavigateTo(String arg0, WebDriver arg1) {
        takeScreenshot(arg1, "Before NavigateTo Screenshot");
    }

    @Override
    public void beforeScript(String arg0, WebDriver arg1) {
    }

    @Override
    public void onException(Throwable arg0, WebDriver arg1) {
    }

    private File copyFileToScreenshotFolder(File file) {
        File dest = null;
        if (file != null) {
            try {
                dest = new File(path + "\\" + screenshotFolderName + "\\" + prefix + System.currentTimeMillis() + ".png");
                FileUtils.copyFile(file, dest);
                file.delete();
            } catch (Exception e) {
            }
        }
        return dest;
    }

    /**
	 * this function will take the screenshot
	 * 
	 * @param driver - the active webdriver (Htmlunit will not work)
	 * @param title - the title in the HTML report file.
	 * @return true in case of success else return false;
	 */
    private boolean takeScreenshot(WebDriver driver, String title) {
        boolean screenshot = true;
        try {
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            scrFile = copyFileToScreenshotFolder(scrFile);
            addToReport(scrFile, title);
        } catch (Exception e) {
            screenshot = false;
        }
        return screenshot;
    }

    private void addToReport(File file, String title) {
        String fileName = file.getName();
        String linkPath = "..\\" + screenshotFolderName + "\\" + fileName;
        reporter.addLink(title + " (fileName=" + fileName + ").", linkPath);
    }
}
