package javarequirementstracer;

import static javarequirementstracer.XhtmlBuilder.SPAN_END;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use this class to aggregate multiple traceability reports from multiple modules into an overview report.
 * NB.:
 * <ol>
 * <li>You should still aggregate your main code and system test code reports separately.</li>
 * <li>Generation of the main code reports is typically done by the modules themselves.</li>
 * <li>Generation of the system test code reports is typically done by the system test module.</li>
 * <li>Aggregation of both report sets should be done by the system test module (to ensure both report sets
 * exist and are complete, since the system test module should be the last in the dependency chain).
 * </li>
 * </ol>
 * 
 * @author Ronald Koster
 */
@Requirements("UC-Overview-Report")
public final class ReportAggregator {

    private static final Logger LOGGER = Logger.getInstance(ReportAggregator.class);

    private static final String DEFAULT_OVERVIEW_PARAMS_MAIN_FILENAME = "src/test/config/traceability_overview_params_main.properties";

    private static final String DEFAULT_OVERVIEW_PARAMS_SYSTEM_TEST_FILENAME = "src/test/config/traceability_overview_params_system_test.properties";

    public static final String DEFAULT_OVERVIEW_PARAMS_FILENAMES = DEFAULT_OVERVIEW_PARAMS_MAIN_FILENAME + ", " + DEFAULT_OVERVIEW_PARAMS_SYSTEM_TEST_FILENAME;

    public static final String USAGE = "Usage: java " + ReportAggregator.class.getName() + " [<overviewParamsFilenames>]" + "\n\nWith:" + "\n<overviewParamsFilenames>: Comma separated list of properties filenames each containing all" + "\n                           parameters for a report For each a report will be generated." + "\n                           Default: " + DEFAULT_OVERVIEW_PARAMS_FILENAMES + "." + "\n                           See also the comments in the properties file itself." + "\n                           For an example see:" + "\n                              + " + DEFAULT_OVERVIEW_PARAMS_MAIN_FILENAME + "\n                              + " + DEFAULT_OVERVIEW_PARAMS_SYSTEM_TEST_FILENAME;

    private static final double GRAPH_WIDTH = 150D;

    private String overviewParamsFilename;

    private String overviewName;

    private String[] reportFilenames;

    private String outputFilename = "../../../target/traceability_overview.html";

    public void setOverviewParamsFilename(String overviewParamsFilename) {
        this.overviewParamsFilename = overviewParamsFilename;
    }

    /**
     * @see #USAGE
     */
    public static void main(String[] args) {
        ReportAggregator agg = new ReportAggregator();
        if (agg.helpRequested(args)) {
            Logger.println(USAGE);
            System.exit(1);
        }
        String paramsFilenames = DEFAULT_OVERVIEW_PARAMS_FILENAMES;
        if (args.length >= 1) {
            paramsFilenames = args[0];
        }
        try {
            for (String paramsFilename : JavaRequirementsTracer.split(paramsFilenames)) {
                agg.overviewParamsFilename = paramsFilename;
                agg.run();
            }
        } catch (FileNotFoundException ex) {
            LOGGER.info("No report generated. " + ex.getMessage());
            System.exit(2);
        }
    }

    private boolean helpRequested(String[] args) {
        return args.length == 1 && (args[0].equals("-h") || args[0].equals("-help") || args[0].equals("--help"));
    }

    public void run() throws FileNotFoundException {
        init();
        double totalCodeCoverage = 0;
        double totalRequirementsCoverage = 0;
        double totalLabelCount = 0;
        XhtmlBuilder bldr = new XhtmlBuilder();
        bldr.start("Traceability Overview for " + this.overviewName);
        ReporterUtils.appendReporterInfo(bldr);
        List<WorkTableRow> workTable = new ArrayList<WorkTableRow>();
        for (int i = 0; i < this.reportFilenames.length; i++) {
            String reportFilename = this.reportFilenames[i];
            String report = FileUtils.readFileAsString(reportFilename);
            if (i == 0) {
                String buildNr = substring(report, "<span id='" + AttributeId.BUILD_NUMBER + "'>", SPAN_END);
                ReporterUtils.appendTimestampBuildNumber(bldr, buildNr + " <small>(from first report below)</small>");
            }
            String reportName = extractReportName(report);
            double codeCoverage = extractCodeCoverage(report);
            double requirementsCoverage = extractRequirementsCoverage(report);
            double labelCount = extractLabelCount(report);
            if (!Double.isNaN(codeCoverage)) {
                totalCodeCoverage += labelCount * codeCoverage;
            }
            totalRequirementsCoverage += labelCount * requirementsCoverage;
            totalLabelCount += labelCount;
            String newReportFilename = reportName.replace(' ', '_') + "_" + i + ".html";
            workTable.add(new WorkTableRow(false, "<a href='" + newReportFilename + "'>" + reportName + "</a>", codeCoverage, requirementsCoverage, labelCount));
            copy(reportFilename, newReportFilename);
        }
        totalCodeCoverage /= totalLabelCount;
        totalRequirementsCoverage /= totalLabelCount;
        WorkTableRow totalsRow = new WorkTableRow(true, "Total", totalCodeCoverage, totalRequirementsCoverage, totalLabelCount);
        workTable.add(totalsRow);
        Collection<Collection<String>> reportTable = new ArrayList<Collection<String>>();
        for (WorkTableRow row : workTable) {
            double weight = row.getLabelCount() / totalLabelCount;
            reportTable.add(convertRow(row, weight));
        }
        bldr.table(AttributeId.COVERAGES, reportTable, "Module", "CodeCoverage", "RequirementsCoverage", "RequirementsCount", "Weight");
        ReporterUtils.appendProgressIndicatorEstimate(bldr, true);
        bldr.end();
        String overview = bldr.toString();
        overview = overview.replace("<td>", "<td align='right'>");
        overview = overview.replace("<tr>\n<td align='right'>", "<tr>\n<td>");
        overview = overview.replace("<th>CodeCoverage", "<th colspan='2'>CodeCoverage");
        overview = overview.replace("<th>RequirementsCoverage", "<th colspan='2'>RequirementsCoverage");
        overview = overview.replace("<td align='right'><div", "<td><div");
        overview = overview.replace("<td align='right'><b><div", "<td><b><div");
        FileUtils.writeFile(this.outputFilename, overview);
        LOGGER.info("Overview written to: " + this.outputFilename);
    }

    private void init() throws FileNotFoundException {
        File paramsFile = new File(this.overviewParamsFilename);
        if (!paramsFile.exists()) {
            throw new FileNotFoundException("File not found: " + this.overviewParamsFilename);
        }
        UniqueProperties props = new UniqueProperties();
        props.load(paramsFile);
        this.overviewName = props.getProperty("overview.name");
        String baseDir = paramsFile.getParent() + "/";
        this.reportFilenames = JavaRequirementsTracer.split(props.getProperty("report.filenames"));
        for (int i = 0; i < this.reportFilenames.length; i++) {
            if (!FileUtils.isAbsolute(this.reportFilenames[i])) {
                this.reportFilenames[i] = baseDir + this.reportFilenames[i];
            }
        }
        this.outputFilename = props.getProperty("output.filename", this.outputFilename);
        if (!FileUtils.isAbsolute(this.outputFilename)) {
            this.outputFilename = baseDir + this.outputFilename;
        }
    }

    private void copy(String reportFilename, String newReportFilename) {
        File src = new File(reportFilename);
        File outputDir = new File(FileUtils.getParent(this.outputFilename));
        File target = new File(outputDir, newReportFilename);
        FileUtils.copyFile(src, target);
    }

    private String extractReportName(String str) {
        return substring(str, "<title>Traceabilities for", "</title>").trim();
    }

    private double extractCodeCoverage(String str) {
        double[] fraction = extractFraction(str, ReporterUtils.CODE_COVERAGE_DEF, SPAN_END);
        return fraction[0] / fraction[1];
    }

    private double extractRequirementsCoverage(String str) {
        double[] fraction = extractFraction(str, ReporterUtils.REQUIREMENTS_COVERAGE_DEF, SPAN_END);
        return fraction[0] / fraction[1];
    }

    private double extractLabelCount(String str) {
        double[] fraction = extractFraction(str, ReporterUtils.REQUIREMENTS_COVERAGE_DEF, SPAN_END);
        return fraction[1];
    }

    private double[] extractFraction(String str, String str1, String str2) {
        String fraction = substring(str, str1, str2).trim();
        int index = fraction.indexOf('/');
        double[] result = new double[2];
        result[0] = Double.valueOf(fraction.substring(0, index));
        result[1] = Double.valueOf(fraction.substring(index + 1));
        return result;
    }

    private String substring(String str, String str1, String str2) {
        int index1 = str.indexOf(str1);
        int index2 = str.indexOf(str2, index1);
        try {
            return str.substring(index1 + str1.length(), index2);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("str=" + str + "; str1=" + str1 + "; str2=" + str2, ex);
        }
    }

    private Collection<String> convertRow(WorkTableRow row, double weight) {
        return createRow(row.isBold(), row.getName(), ReporterUtils.formatPercentage(row.getCodeCoverage()), ReporterUtils.getPercentageGraph(row.getCodeCoverage(), weight, GRAPH_WIDTH), ReporterUtils.formatPercentage(row.getRequirementsCoverage()), ReporterUtils.getPercentageGraph(row.getRequirementsCoverage(), weight, GRAPH_WIDTH), String.format("%.0f", row.getLabelCount()), ReporterUtils.formatPercentage(weight));
    }

    private Collection<String> createRow(boolean bold, String... values) {
        Collection<String> row = new ArrayList<String>();
        for (String value : values) {
            if (bold) {
                row.add("<b>" + value + "</b>");
            } else {
                row.add(value);
            }
        }
        return row;
    }

    private static class WorkTableRow {

        private final boolean bold;

        private final String name;

        private final double codeCoverage;

        private final double requirementsCoverage;

        private final double labelCount;

        WorkTableRow(boolean bold, String name, double codeCoverage, double requirementsCoverage, double labelCount) {
            this.bold = bold;
            this.name = name;
            this.codeCoverage = codeCoverage;
            this.requirementsCoverage = requirementsCoverage;
            this.labelCount = labelCount;
        }

        public boolean isBold() {
            return this.bold;
        }

        public String getName() {
            return this.name;
        }

        public double getCodeCoverage() {
            return this.codeCoverage;
        }

        public double getRequirementsCoverage() {
            return this.requirementsCoverage;
        }

        public double getLabelCount() {
            return this.labelCount;
        }
    }
}
