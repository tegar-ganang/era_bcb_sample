package unitth.html.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import unitth.core.UnitTH;
import unitth.html.HtmlGenUtils;
import unitth.junit.TestCaseSummary;
import unitth.junit.TestHistory;
import unitth.junit.TestModule;
import unitth.junit.TestModuleSummary;
import unitth.junit.TestRun;

/**
 * This class is responsible for generation of the HTML report pages.
 * 
 * @author Andreas Nyberg
 */
public class HtmlModuleGen extends HtmlGen {

    /**
     * CTOR, sets the history to generate and sets a few of the fixed paths.
     * 
     * @param history
     */
    public HtmlModuleGen(TestHistory history, boolean generateExecTimeGraphs) {
        super(history, generateExecTimeGraphs);
    }

    /**
     * The main entry point for the HTML generation.
     */
    public void generateHtmlHistory() {
        generateModuleRunPages();
    }

    protected void generateModuleRunPages() {
        Object[] moduleNames = th.getUniqueModules();
        for (Object moduleName : moduleNames) {
            generateModuleRunPage((String) moduleName);
        }
    }

    /**
     * Generates a link bar on every page to make it possible to navigate to the
     * correct part on the page.
     * 
     * @param out
     *            The buffer to append to.
     */
    private void generateModuleAnchors(BufferedWriter out) throws IOException {
        out.append("<TABLE class=\"mainAnchors\" width=\"100%\">" + c_LF);
        out.append(t(1) + "<TR>" + c_LF);
        out.append(t(2) + "<TD>");
        out.append("<CENTER><a href=\"#runs\">RUNS</a>");
        out.append("&nbsp;&nbsp;||&nbsp;&nbsp;<a href=\"#testcases\">TEST CASES</a>");
        out.append("&nbsp;&nbsp;||&nbsp;&nbsp;<a href=\"#spread\">TEST CASE SPREAD</a></CENTER>");
        out.append("</TD>" + c_LF);
        out.append(t(1) + "</TR>" + c_LF);
        out.append("</TABLE>" + c_LF);
    }

    private void generateModuleRunPage(String moduleName) {
        String fileName = destDir + File.separator + HTMLMODULEOUTPUTFILE_PREFIX + moduleName + HTMLMODULEOUTPUTFILE_SUFFIX;
        try {
            BufferedWriter out = createFile(fileName);
            startPageModule(out, "UnitTH - " + moduleName);
            startBody(out);
            generateModuleAnchors(out);
            out.append("<TABLE width=\"90%\"><TR><TD>" + c_LF);
            generateModuleModuleSummary(out, th.getTestModuleSummary(moduleName));
            generateModuleModuleInfo(out);
            generateModuleModuleList(out, moduleName);
            generateModuleTestCaseInfo(out);
            generateModuleTestCaseList(out, moduleName);
            generateModuleTestCaseSpreadInfo(out);
            generateModuleTestCaseSpreadList(out, moduleName);
            out.append("</TD></TR></TABLE>" + c_LF);
            endBody(out);
            endPage(out);
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the output to file: " + fileName + " Possible fix: check file system permissions." + " Possible fix: check available disk space.");
            e.printStackTrace();
        }
    }

    /**
     * This method creates the list of runs for a specific module.
     * 
     * @param buf
     *            The buffer to append to. @param moduleName The name of the
     *            module whose runs to generate a list for.
     */
    private void generateModuleModuleItems(BufferedWriter buf, String moduleName) throws IOException {
        int idx = th.getRuns().size();
        for (TestRun tr : th.getRuns()) {
            TestModule tm = tr.getTestModules().get(moduleName);
            if (null != tm) {
                generateModuleModuleItem(buf, tm, idx, tr);
            }
            idx--;
        }
    }

    private void generateModuleModuleSummary(BufferedWriter buf, TestModuleSummary tms) throws IOException {
        buf.write("<H1 class=\"title\">TEST MODULE/TEST CLASS</H1>" + c_LF);
        buf.write("<H2 class=\"title\">" + tms.getName() + "</H2>" + c_LF);
        buf.write(t(1) + "<TABLE class=\"topcontainer\" cellspacing=\"6\">" + c_LF);
        buf.write(t(2) + "<TR>" + c_LF);
        buf.write(t(3) + "<TD valign=\"top\">" + c_LF);
        buf.write(t(4) + "<TABLE id=\"summary\" class=\"sum\" cellspacing=\"4\">" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"sumhdr\" colspan=\"2\">Test&nbsp;module&nbsp;history&nbsp;overview</TD><TD>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Number&nbsp;of&nbsp;runs&nbsp;</TD><TD id=\"mainSmryRuns\" class=\"summid\"> " + tms.getNoRuns() + " </TD></TR>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Number&nbsp;of&nbsp;unique&nbsp;test&nbsp;cases&nbsp;</TD><TD id=\"mainSmryNoTcs\" class=\"summid\"> " + tms.getNoUniqueTestCases() + " </TD></TR>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Best&nbsp;run&nbsp;</TD><TD id=\"mainSmryBest\" class=\"summid\"> " + tms.getBestRun() + "% </TD></TR>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Worst&nbsp;run&nbsp;</TD><TD id=\"mainSmryWorst\" class=\"summid\"> " + tms.getWorstRun() + "% </TD></TR>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"sumbottom\">&nbsp;Average&nbsp;pass&nbsp;rate&nbsp;</TD><TD id=\"mainSmryAve\" class=\"sumbottom\"> <b>" + tms.getPassPct() + "% </b></TD></TR>" + c_LF);
        buf.write(t(4) + "</TABLE>" + c_LF);
        buf.write(t(3) + "</TD>" + c_LF);
        buf.write(t(3) + "<TD VALIGN=\"top\" rowspan=\"2\">" + c_LF);
        buf.write(t(4) + "<TABLE class=\"imageholder\" cellspacing=\"4\">" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"sumhdr\" align=\"center\">Pass&nbsp;rates&nbsp;-&nbsp;all&nbsp;runs</TD></TR>" + c_LF);
        buf.write(t(6) + "<TR><TD><TABLE cellpadding=\"4\">" + c_LF);
        buf.write(t(5) + "<TR><TD><img src=\"./images/" + IMAGE_PASS_PREFIX + tms.getName().replace('.', '-') + IMAGE_SUFFIX + "\"></TD></TR>" + c_LF);
        buf.write(t(6) + "</TABLE></TD></TR>" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"sumhdr\" align=\"center\">Number&nbsp;of&nbsp;test&nbsp;cases&nbsp;-&nbsp;run&nbsp;by&nbsp;run</TD></TR>" + c_LF);
        buf.write(t(6) + "<TR><TD><TABLE cellpadding=\"4\">" + c_LF);
        buf.write(t(5) + "<TR><TD><img src=\"./images/" + IMAGE_TC_PREFIX + tms.getName().replace('.', '-') + IMAGE_SUFFIX + "\"></TD></TR>" + c_LF);
        buf.write(t(6) + "</TABLE></TD></TR>" + c_LF);
        buf.write(t(4) + "</TABLE>" + c_LF);
        buf.write(t(3) + "</TD>" + c_LF);
        buf.write(t(3) + "<TD VALIGN=\"top\" rowspan=\"2\">" + c_LF);
        buf.write(t(4) + "<TABLE class=\"imageholder\"  cellspacing=\"4\">" + c_LF);
        if (generateExecTimeGraphs) {
            buf.write(t(5) + "<TR><TD valign=\"top\" class=\"sumhdr\" align=\"center\">Execution&nbsp;times&nbsp;(" + getExecutionTimeUnit(tms) + ")&nbsp;-&nbsp;all&nbsp;runs</TD></TR>" + c_LF);
            buf.write(t(6) + "<TR><TD><TABLE cellpadding=\"4\">" + c_LF);
            buf.write(t(5) + "<TR><TD><img src=\"./images/" + IMAGE_ET_PREFIX + tms.getName().replace('.', '-') + IMAGE_SUFFIX + "\"></TD></TR>" + c_LF);
            buf.write(t(6) + "</TABLE></TD></TR>" + c_LF);
        }
        buf.write(t(5) + "<TR><TD valign=\"top\" class=\"sumhdr\" align=\"center\">Non&nbsp;passing&nbsp;test&nbsp;cases</TD></TR>" + c_LF);
        buf.write(t(6) + "<TR><TD><TABLE cellpadding=\"4\">" + c_LF);
        buf.write(t(5) + "<TR><TD><img src=\"./images/" + IMAGE_FN_PREFIX + tms.getName().replace('.', '-') + IMAGE_SUFFIX + "\"></TD></TR>" + c_LF);
        buf.write(t(6) + "</TABLE></TD></TR>" + c_LF);
        buf.write(t(4) + "</TABLE>" + c_LF);
        buf.write(t(3) + "</TD>" + c_LF);
        buf.write(t(2) + "</TR>" + c_LF);
        String selectedTrend = "";
        buf.write(t(2) + "<TR>" + c_LF);
        buf.write(t(3) + "<TD valign=\"top\">" + c_LF);
        buf.write(t(4) + "<TABLE id=\"trends\" class=\"sum\">" + c_LF);
        buf.write(t(5) + "<TR><TD class=\"sumhdr\" colspan=\"3\">Trends</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getPrTrendLastRun(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Pass&nbsp;rate&nbsp;trend&nbsp;last&nbsp;run&nbsp;</TD><TD class=\"prTrend\">" + tms.getPrTrendLastRun() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getPrTrendLast5Runs(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Pass&nbsp;rate&nbsp;trend&nbsp;last&nbsp;5&nbsp;runs&nbsp;</TD><TD class=\"prTrend\">" + tms.getPrTrendLast5Runs() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getPrTrendLast10Runs(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Pass&nbsp;rate&nbsp;trend&nbsp;last&nbsp;10&nbsp;runs&nbsp;</TD><TD class=\"prTrend\">" + tms.getPrTrendLast10Runs() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getPrTrendLast3Days(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Pass&nbsp;rate&nbsp;trend&nbsp;last&nbsp;3&nbsp;days&nbsp;</TD><TD class=\"prTrend\">" + tms.getPrTrendLast3Days() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getPrTrendLast7Days(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Pass&nbsp;rate&nbsp;trend&nbsp;last&nbsp;7&nbsp;days&nbsp;</TD><TD class=\"prTrend\">" + tms.getPrTrendLast7Days() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getTcTrendLast3Days(), true);
        buf.write(t(5) + "<TR><TD class=\"summid\">&nbsp;Test&nbsp;case&nbsp;trend&nbsp;last&nbsp;3&nbsp;days&nbsp;</TD><TD class=\"prTrend\">" + tms.getTcTrendLast3Days() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getTcTrendLast7Days(), true);
        buf.write(t(5) + "<TR><TD class=\"sumid\">&nbsp;Test&nbsp;case&nbsp;trend&nbsp;last&nbsp;7&nbsp;days&nbsp;</TD><TD class=\"prTrend\">" + tms.getTcTrendLast7Days() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getFnTrendLast3Days(), false);
        buf.write(t(5) + "<TR><TD class=\"sumid\">&nbsp;Failure&nbsp;trend&nbsp;last&nbsp;3&nbsp;days&nbsp;</TD><TD class=\"prTrend\">" + tms.getFnTrendLast3Days() + "</TD><TD align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        selectedTrend = HtmlGenUtils.getTrendImage(tms.getFnTrendLast7Days(), false);
        buf.write(t(5) + "<TR><TD class=\"sumbottom\">&nbsp;Failure&nbsp;trend&nbsp;last&nbsp;7&nbsp;days&nbsp;</TD><TD class=\"prTrendBottom\">" + tms.getFnTrendLast7Days() + "</TD><TD class=\"prTrendBottom\" align=\"right\" valign=\"middle\">" + "<IMG src=\"" + selectedTrend + "\" height=\"" + UnitTH.ICON_HEIGHT + "\" width=\"" + UnitTH.ICON_WIDTH + "\">" + "</TD></TR>" + c_LF);
        buf.write(t(4) + "</TABLE>" + c_LF);
        buf.write(t(3) + "</TD>" + c_LF);
        buf.write(t(2) + "</TR>" + c_LF);
        buf.write(t(1) + "</TABLE>" + c_LF);
    }

    /**
     * Generates the list of modules in all runs and each row in the table
     * represents a summary of all executions for the specific module.
     * 
     * @param buf
     *            The buffer to append to. @param moduleName The name of the
     *            module to get the statistics from.
     */
    private void generateModuleModuleList(BufferedWriter buf, String moduleName) throws IOException {
        buf.write(t(1) + "<TABLE id=\"run_table\" cellspacing=\"0\" cellpadding=\"2\">" + c_LF);
        buf.write(t(2) + "<THEAD>" + c_LF);
        generateModuleModuleHeader(buf);
        buf.write(t(2) + "</THEAD>" + c_LF);
        buf.write(t(2) + "<TBODY>" + c_LF);
        generateModuleModuleItems(buf, moduleName);
        buf.write(t(2) + "</TBODY>" + c_LF);
        buf.write(t(1) + "</TABLE>" + c_LF);
    }

    /**
     * Generates the list representing all test cases in a test module and all
     * its associated statistics.
     * 
     * @param buf
     *            The buffer to append to. @param moduleName The name of the
     *            module where all the test cases shall be retrieved.
     */
    private void generateModuleTestCaseList(BufferedWriter buf, String moduleName) throws IOException {
        buf.write(t(1) + "<TABLE id=\"testcase_table\" cellspacing=\"0\" cellpadding=\"2\">" + c_LF);
        buf.write(t(2) + "<THEAD>" + c_LF);
        generateModuleTestCaseHeader(buf);
        buf.write(t(2) + "</THEAD>" + c_LF);
        buf.write(t(2) + "<TBODY>" + c_LF);
        generateModuleTestCaseItems(buf, moduleName);
        buf.write(t(2) + "</TBODY>" + c_LF);
        buf.write(t(1) + "</TABLE>" + c_LF);
    }

    /**
     * Generates the header for the list of module runs.
     * 
     * @param buf
     *            The buffer to append to.
     */
    private void generateModuleModuleHeader(BufferedWriter buf) throws IOException {
        buf.write(t(3) + "<TR>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeaderLeftAsc\" abbr=\"input_text\">Run</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Tests&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Pass&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Error&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Fail&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Ignored&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" NOWRAP abbr=\"date\">Exec&nbsp;date</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"float\">Exec&nbsp;time&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" align=\"left\" colspan=\"2\" abbr=\"percent\">TOTAL</TH>" + c_LF);
        buf.write(t(3) + "</TR>" + c_LF);
    }

    /**
     * Generates a row in the test module history list.
     * 
     * @param buf
     *            The buffer to append to. @param tm The test module who's
     *            statistics to print out. @param idx The index to display for
     *            this run.
     * 
     * @param runPath
     *            The path to the parsed files for this run.
     */
    private void generateModuleModuleItem(BufferedWriter buf, TestModule tm, int idx, TestRun tr) throws IOException {
        String idxStr = HtmlGenUtils.calculateRunIdxString(th.getNoRuns(), idx);
        String runName = getHtmlReportLink(tr, "Run-" + idxStr);
        buf.write(t(3) + "<TR>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphItemLeft\" NOWRAP>" + runName + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getNoTestCases() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getNoPassed() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getNoErrors() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getNoFailures() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getNoIgnored() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\" NOWRAP>" + tm.getExecutionDate() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphPercent\" align=\"center\">" + tm.getExecutionTime() + "</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphBarLeft\" align=\"right\">" + tm.getPassPct() + "%</TD>" + c_LF);
        buf.write(t(4) + "<TD class=\"graphBar\">" + c_LF);
        buf.write(t(5) + "<TABLE class=\"barGraph\" cellspacing=\"0\">" + c_LF);
        buf.write(t(6) + "<TBODY>" + c_LF);
        buf.write(t(7) + "<TR>" + c_LF);
        generatePctBar(buf, tm.getPassPctDouble());
        buf.write(t(7) + "</TR>" + c_LF);
        buf.write(t(6) + "</TBODY>" + c_LF);
        buf.write(t(5) + "</TABLE>" + c_LF);
        buf.write(t(4) + "</TD>" + c_LF);
        buf.write(t(3) + "</TR>" + c_LF);
    }

    /**
     * Generates the list of test case items in the test cases list on the
     * module page.
     * 
     * @param buf
     *            The buffer to append to. @param moduleName The name of the
     *            module where all the test cases shall be retrieved.
     */
    private void generateModuleTestCaseItems(BufferedWriter buf, String moduleName) throws IOException {
        Collection<TestCaseSummary> c = th.getTestCaseSummaries().values();
        Iterator<TestCaseSummary> iter = c.iterator();
        while (iter.hasNext()) {
            TestCaseSummary tcs = iter.next();
            if (null != tcs && (tcs.getClassName().equals(moduleName) || tcs.getModuleName().equals(moduleName))) {
                generateTestCaseItem(buf, tcs);
            }
        }
    }

    /**
     * Generates the header for the the table listing all the test case
     * summaries.
     * 
     * @param buf
     *            The buffer to append to.
     */
    private void generateModuleTestCaseHeader(BufferedWriter buf) throws IOException {
        buf.write(t(3) + "<TR>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeaderLeftAsc\"  abbr=\"input_text\">Test&nbsp;case&nbsp;name&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Runs&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Pass&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Error&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Fail&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"number\">Ignored&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" abbr=\"float\">Ave&nbsp;ET*&nbsp;&nbsp;</TH>" + c_LF);
        buf.write(t(4) + "<TH class=\"graphHeader\" align=\"left\" colspan=\"2\" abbr=\"percent\">TOTAL</TH>" + c_LF);
        buf.write(t(3) + "</TR>" + c_LF);
    }

    private void generateModuleModuleInfo(BufferedWriter buf) throws IOException {
        buf.write("<a name=\"runs\"/><H3 class=\"title\">Module runs</H3>" + c_LF);
        buf.write("If regular HTML test reports have been generated click on the run to open the report.<br/><br/>" + c_LF);
    }

    /**
     * Generates the informational text preceding the test case pass rate list.
     * 
     * @param buf
     *            The buffer to append to.
     */
    private void generateModuleTestCaseInfo(BufferedWriter buf) throws IOException {
        buf.write("<a name=\"testcases\"/><H3 class=\"title\">Test cases in this module</H3>" + c_LF);
        buf.write("This list shows the average pass rate and execution times for all test cases in this test module.<br/><br/>" + c_LF);
    }

    private void generateModuleTestCaseSpreadList(BufferedWriter buf, String moduleName) throws IOException {
        buf.write(t(1) + "<TABLE id=\"spread_table\" cellspacing=\"0\" cellpadding=\"2\">" + c_LF);
        buf.write(t(2) + "<THEAD>" + c_LF);
        generateTestCaseSpreadHeader(buf);
        buf.write(t(2) + "</THEAD>" + c_LF);
        buf.write(t(2) + "<TBODY>" + c_LF);
        generateModuleTestCaseSpreadItems(buf, moduleName);
        buf.write(t(2) + "</TBODY>" + c_LF);
        buf.write(t(1) + "</TABLE>" + c_LF);
    }

    private void generateModuleTestCaseSpreadItems(BufferedWriter buf, String moduleName) throws IOException {
        Collection<TestCaseSummary> c = th.getTestCaseSummaries().values();
        Iterator<TestCaseSummary> iter = c.iterator();
        while (iter.hasNext()) {
            TestCaseSummary tcs = iter.next();
            if (null != tcs && (tcs.getClassName().equals(moduleName) || tcs.getModuleName().equals(moduleName))) {
                generateTestCaseSpreadItem(buf, tcs);
            }
        }
    }

    private void generateModuleTestCaseSpreadInfo(BufferedWriter buf) throws IOException {
        buf.write("<a name=\"spread\"/><H3 class=\"title\">Test case verdict spread in this module</H3>" + c_LF);
        buf.write("This list shows the test case verdicts in the order they occured during the test runs. If a regular HTML report has been generated click on a spread item to jump directly to the test report. Legend: (green=Pass, grey=No-run, red=Fail/Error)<br/><br/>" + c_LF);
    }

    protected void startPageModule(BufferedWriter buf, String pageTitle) throws IOException {
        buf.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + c_LF);
        buf.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" >" + c_LF);
        buf.write("<head>" + c_LF);
        buf.write(t(1) + "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" + c_LF);
        buf.write(t(1) + "<title>" + pageTitle + "</title>" + c_LF);
        buf.write(t(1) + "<LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\"css/style.css\" TITLE=\"Style\"/>" + c_LF);
        buf.write(t(1) + "<script src=\"" + UnitTH.JS_DIR + "/" + UnitTH.SORT_SCRIPT + "\" type=\"text/javascript\"></script>" + c_LF);
        buf.write(t(1) + "<script type=\"text/javascript\">" + c_LF);
        buf.write(t(2) + "function sortTables() {" + c_LF);
        buf.write(t(3) + "var runTable = new TableSort;" + c_LF);
        buf.write(t(3) + "var testCaseTable = new TableSort;" + c_LF);
        buf.write(t(3) + "var spreadTable = new TableSort;" + c_LF);
        buf.write(t(3) + "runTable.init(\"run_table\");" + c_LF);
        buf.write(t(3) + "testCaseTable.init(\"testcase_table\");" + c_LF);
        buf.write(t(3) + "spreadTable.init(\"spread_table\");" + c_LF);
        buf.write(t(2) + "}" + c_LF);
        buf.write(t(2) + "window.onload = sortTables;" + c_LF);
        buf.write(t(1) + "</script>" + c_LF);
        buf.write("</head>" + c_LF);
    }
}
