package com.bayareasoftware.chartengine.chart;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.bayareasoftware.chartengine.ds.DSFactory;
import com.bayareasoftware.chartengine.ds.DataInference;
import com.bayareasoftware.chartengine.ds.DataSource;
import com.bayareasoftware.chartengine.ds.DataStream;
import com.bayareasoftware.chartengine.ds.JdbcDataSource;
import com.bayareasoftware.chartengine.functions.BuiltInFunctions;
import com.bayareasoftware.chartengine.model.Arg;
import com.bayareasoftware.chartengine.model.ArgType;
import com.bayareasoftware.chartengine.model.BaseDescriptor;
import com.bayareasoftware.chartengine.model.ChartBundle;
import com.bayareasoftware.chartengine.model.ChartConstants;
import com.bayareasoftware.chartengine.model.ChartInfo;
import com.bayareasoftware.chartengine.model.DataSourceInfo;
import com.bayareasoftware.chartengine.model.DataType;
import com.bayareasoftware.chartengine.model.LogoInfo;
import com.bayareasoftware.chartengine.model.MarkerDescriptor;
import com.bayareasoftware.chartengine.model.Metadata;
import com.bayareasoftware.chartengine.model.PlotType;
import com.bayareasoftware.chartengine.model.SeriesDescriptor;
import com.bayareasoftware.chartengine.model.SimpleProps;
import com.bayareasoftware.chartengine.model.StandardProps;
import com.bayareasoftware.chartengine.model.StringUtil;
import com.bayareasoftware.chartengine.model.TimeConstants;

public class CreateImageTest {

    static File cacheRoot;

    static ChartDiskCache cache;

    static ChartDriver fac;

    static SimpleProps h2dbprops = new SimpleProps();

    private static SimpleProps templateChartProps = new SimpleProps();

    private static String user = "TestUser";

    private static String mathPNGName;

    @BeforeClass
    public static void setupClass() throws Exception {
        System.setProperty("chartengine.allowFileUrls", "true");
        h2dbprops.put(StandardProps.JDBC_DRIVER, "org.h2.Driver");
        h2dbprops.put(StandardProps.JDBC_URL, "jdbc:h2:mem:charttest;DB_CLOSE_DELAY=-1");
        h2dbprops.put(StandardProps.JDBC_USERNAME, "sa");
        h2dbprops.put(StandardProps.JDBC_PASSWORD, "");
        cacheRoot = new File("test.out/image");
        cache = new ChartDiskCache(cacheRoot);
        cache.clear();
        fac = ChartDriverManager.getChartDriver(ChartDriverManager.JFREECHART);
        templateChartProps.put("chart.backgroundPaint", "gradient:#bbeaff,light_gray,0,0,0,400,true");
        templateChartProps.put("plot.backgroundAlpha", "0");
        templateChartProps.put("title.font", "SansSerif-bold-16");
        templateChartProps.put("title.paint", "#ff0000");
        createTables(JdbcDataSource.createConnection(h2dbprops));
    }

    @AfterClass
    public static void cleanupClass() throws Exception {
        dropTables(JdbcDataSource.createConnection(h2dbprops));
    }

    private Random rand = new Random(0);

    private int randInt(int v) {
        int flip = rand.nextInt();
        if (flip % 2 == 0) {
            v += 1;
        } else if (v > 1) {
            v -= 1;
        }
        return v;
    }

    private String getRandomWalkValues() {
        StringBuilder sb = new StringBuilder();
        java.util.Date now = new java.util.Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, -100);
        Date d = cal.getTime();
        int v1 = 50, v2 = 50, v3 = 50;
        rand.setSeed(System.currentTimeMillis() + rand.nextInt(100000));
        while (d.before(now)) {
            sb.append(d.toString());
            sb.append(',');
            v1 = randInt(v1);
            sb.append(String.valueOf(v1));
            sb.append(',');
            v2 = randInt(v2);
            sb.append(String.valueOf(v2));
            sb.append(',');
            v3 = randInt(v3);
            sb.append(String.valueOf(v3));
            sb.append('\n');
            cal.add(Calendar.DAY_OF_YEAR, 1);
            d = cal.getTime();
        }
        return sb.toString();
    }

    private String getRandomXYValues() {
        StringBuilder sb = new StringBuilder();
        int v1, v2, v3;
        rand.setSeed(System.currentTimeMillis() + rand.nextInt(100000));
        for (int i = 0; i < 30; i++) {
            v1 = rand.nextInt(100);
            sb.append(String.valueOf(v1));
            sb.append(',');
            v2 = rand.nextInt(140);
            sb.append(String.valueOf(v2));
            sb.append(',');
            v3 = rand.nextInt(180);
            sb.append(String.valueOf(v3));
            sb.append('\n');
        }
        return sb.toString();
    }

    private ChartDiskResult createChart(ChartBundle cb) throws Exception {
        LogoInfo logo = new LogoInfo();
        logo.setVisible(true);
        logo.setUrl("/com/bayareasoftware/chartengine/chart/cmlogo-150-black.png");
        return createChart(cb, logo, cache.prepChartResult(cb, null));
    }

    /**
     * create chart for test with a given logo
     * @param cb
     * @param logo
     * @return
     * @throws Exception
     */
    private ChartDiskResult createChart(ChartBundle cb, LogoInfo logo) throws Exception {
        return createChart(cb, logo, cache.prepChartResult(cb, null));
    }

    private ChartDiskResult createChart(ChartBundle cb, LogoInfo logo, ChartDiskResult dr) throws Exception {
        Map<Integer, DataStream> smap = new HashMap<Integer, DataStream>();
        Map<String, String> paramValues = new HashMap<String, String>();
        ChartInfo ci = cb.getChartInfo();
        for (BaseDescriptor bd : ci.getSortedDescriptors()) {
            String src = bd.getSource();
            if (src != null) {
                DataSourceInfo dsi = cb.getDataSourceByID(src);
                DataSource dataSource = DSFactory.createDataSource(dsi);
                DataStream r = DSFactory.eval(dataSource, paramValues);
                if (bd instanceof SeriesDescriptor) {
                    if (paramValues != null) {
                        String sstr = paramValues.get(ChartConstants.PARAM_START_DATE);
                        String estr = paramValues.get(ChartConstants.PARAM_END_DATE);
                        if (sstr != null || estr != null) {
                            r = ChartDataUtil.limitStreamByDateInterval(r, (SeriesDescriptor) bd, sstr, estr);
                        }
                    }
                }
                smap.put(bd.getSid(), r);
            } else {
                if (bd.getFunc() != null) {
                    DataStream d = ChartDataUtil.computeDerivedDataStream(ci, bd, smap);
                    smap.put(bd.getSid(), d);
                }
            }
        }
        ChartResult cr = new ChartResult(dr);
        cr = fac.create(cb.getChartInfo(), logo, smap, templateChartProps, cr);
        return cr.getDiskResult();
    }

    /**
     * draw a chart with a moving average series that has too large a window
     * @throws Exception
     */
    @Test
    public void testMovingAverageTooLargeWindow() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, getRandomWalkValues());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "d");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "stock 1");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "stock 2");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "stock 3");
        md.setColumnType(4, DataType.INTEGER);
        ds.setInputMetadata(md);
        String title = "Random Walk with Moving Average With Too Large Window";
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setDescription("Random Walk with Moving Average With Too Large Window (moving average line should be missing)");
        SeriesDescriptor sd0;
        sd0 = new SeriesDescriptor();
        sd0.setName("Stock");
        sd0.setYColumn(2);
        sd0.setSource(ds.getId());
        ci.addSeriesDescriptor(sd0);
        SeriesDescriptor sd;
        double maDays = 10000;
        sd = new SeriesDescriptor();
        sd.setName(maDays + " Day Moving Average");
        sd.setFunc(BuiltInFunctions.FN_MVAVG);
        sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
        sd.addArg(new Arg(ArgType.NUMBER, maDays));
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", title);
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        cb.validate();
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println(title + " chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "RandomWalkWithMovingAverageWithTooLargeWindow");
    }

    /**
     * draw a chart with a moving average series that is derived from another series
     * @throws Exception
     */
    @Test
    public void testMovingAverage() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, getRandomWalkValues());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "d");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "stock 1");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "stock 2");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "stock 3");
        md.setColumnType(4, DataType.INTEGER);
        ds.setInputMetadata(md);
        String title = "Random Walk with Moving Average";
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setDescription("Random Walk with Moving Average");
        SeriesDescriptor sd0;
        sd0 = new SeriesDescriptor();
        sd0.setName("Stock");
        sd0.setYColumn(2);
        sd0.setSource(ds.getId());
        ci.addSeriesDescriptor(sd0);
        SeriesDescriptor sd;
        double maDays = 100;
        {
            sd = new SeriesDescriptor();
            sd.setName(maDays + " Day Moving Average");
            sd.setFunc(BuiltInFunctions.FN_MVAVG);
            sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
            sd.addArg(new Arg(ArgType.NUMBER, maDays));
            ci.addSeriesDescriptor(sd);
        }
        sd = new SeriesDescriptor();
        maDays = 20;
        sd.setName(maDays + " Day Moving Average");
        sd.setFunc(BuiltInFunctions.FN_MVAVG);
        sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
        sd.addArg(new Arg(ArgType.NUMBER, maDays));
        ci.addSeriesDescriptor(sd);
        sd = new SeriesDescriptor();
        maDays = 15;
        sd.setName(maDays + " Day Moving Average");
        sd.setFunc(BuiltInFunctions.FN_MVAVG);
        sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
        sd.addArg(new Arg(ArgType.NUMBER, maDays));
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", title);
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        cb.validate();
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println(title + " chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "RandomWalkWithMovingAverage");
    }

    /**
     * draw a chart with a random series and then two other series
     * that are scaled versions of the first
     * @throws Exception
     */
    @Test
    public void testScale() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, getRandomWalkValues());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "d");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "stock 1");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "stock 2");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "stock 3");
        md.setColumnType(4, DataType.INTEGER);
        ds.setInputMetadata(md);
        String title = "Scale by Multiple Factors";
        ChartInfo ci = new ChartInfo();
        ci.setDescription("Scale by Multiple Factors");
        SeriesDescriptor sd0;
        sd0 = new SeriesDescriptor();
        sd0.setName("Original");
        sd0.setYColumn(2);
        sd0.setSource(ds.getId());
        ci.addSeriesDescriptor(sd0);
        SeriesDescriptor sd = new SeriesDescriptor();
        double factor = 1.5;
        sd.setName("Scale by " + factor);
        sd.setFunc(BuiltInFunctions.FN_SCALE);
        sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
        sd.addArg(new Arg(ArgType.NUMBER, factor));
        ci.addSeriesDescriptor(sd);
        sd = new SeriesDescriptor();
        factor = 0.5;
        sd.setName("Scale by " + factor);
        sd.setFunc(BuiltInFunctions.FN_SCALE);
        sd.addArg(new Arg(ArgType.SID, sd0.getSid()));
        sd.addArg(new Arg(ArgType.NUMBER, factor));
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", title);
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println(title + " chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "ScaleByMultipleFactors");
    }

    static final String MAISY_DATA = "Date,Weight,Length\n" + "6/21/2010,7.05,22.00\n" + "7/21/2010,8.01,22.00\n" + "8/18/2010,11.13,23.00\n" + "10/20/2010,15.01,25.00\n" + "12/22/2010,16.05,25.50\n" + "3/23/2011,17.13,27.50\n" + "6/22/2011,19.10,29.25";

    @Test
    public void testMaisyGrowth() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, MAISY_DATA);
        Metadata md = new Metadata(3);
        md.setColumnName(1, "Date");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "Weight");
        md.setColumnType(2, DataType.DOUBLE);
        md.setColumnName(3, "Height");
        md.setColumnType(3, DataType.DOUBLE);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("Maisy Growth\n=props=\n" + "labels=true\n" + "label.0.paint=#ff0000\n" + "label.1.paint=#000000\n" + "label.itemAnchor=OUTSIDE12\n" + "label.textAnchor=BOTTOM_CENTER\n");
        ci.setProperty("renderer.shadowVisible", "false");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("Weight");
        sd.setSource(ds.getId());
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setVisible(true);
        ci.addSeriesDescriptor(sd);
        sd = new SeriesDescriptor();
        sd.setName("Height");
        sd.setSource(ds.getId());
        sd.setXColumn(1);
        sd.setYColumn(3);
        sd.setRenderer("Bar");
        sd.setTimePeriod(TimeConstants.TIME_MONTH);
        {
            SimpleProps sp = new SimpleProps();
            sp.put("renderer.shadowVisible", "false");
            sd.setRendererProps(sp);
        }
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "Maisy");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("Maisy chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "Maisy");
    }

    @Test
    public void testGantt() throws Exception {
        ChartBundle cb = new ChartBundle();
        String[][] data = { { "Process Mapping", "6/16/2008", "3/31/2009", "All" }, { "IT Strategy", "6/16/2008", "12/31/2009", "Erin" }, { "Quick Sigma: AP #1 Incomplete POs", "9/15/2008", "2/27/2009", "Erin" }, { "Enterprise Account Structure: Customers", "1/1/2009", "6/1/2009", "Erin" }, { "Enterprise Account Structure: Managers", "3/2/2009", "12/30/2009", "Erin" }, { "Pre Define: Document Retention & Storage", "1/1/2009", "1/30/2009", "Erin" }, { "Pre Define: Personal Printers", "1/1/2009", "1/30/2009", "Erin" }, { "JDI: Selling Used Oil (PPS)", "11/3/2008", "1/30/2009", "Jim" }, { "DMAIC: Vehicle Program", "12/1/2008", "3/2/2009", "Jim" }, { "Pre Define: Freight Expense", "12/1/2008", "12/31/2008", "Jim" }, { "Pre-Define: Fleet Management", "1/1/2009", "1/30/2009", "Jim" }, { "Pre Define: Document Retention & Storage", "1/1/2009", "1/30/2009", "Jim" }, { "Pre-Define: O-Parts", "1/1/2009", "1/30/2009", "Jim" }, { "DMEDI: Recycling Oil #1 (PPS)", "3/2/2009", "12/31/2009", "Jim" }, { "DMAIC: AP #3 Payment Methods", "4/1/2009", "12/31/2009", "Jim" }, { "DMAIC: Utilities", "6/12/2008", "2/27/2009", "Alex" }, { "Quick Sigma: AP #1 Incomplete POs", "9/15/2008", "2/27/2009", "Alex" }, { "DMAIC: AP #2 Process Improvement", "10/1/2008", "3/31/2009", "Alex" }, { "Pre Define: Freight Expense", "12/1/2008", "12/31/2008", "Alex" }, { "Pre-Define: Asset Management", "12/1/2008", "12/31/2008", "Alex" }, { "Pre-Define: Fleet Management", "1/1/2009", "1/30/2009", "Alex" }, { "Pre-Define: Parts Reconciliation", "1/1/2009", "1/30/2009", "Alex" }, { "DMAIC: Repair Communication (PTCo)", "2/2/2009", "12/31/2009", "Alex" }, { "DMAIC: CCE Equipment Standardization (PMCo)", "12/1/2008", "9/1/2009", "Bill" }, { "JDI: Inventory Personnel (PMCo)", "12/1/2008", "1/30/2009", "Bill" }, { "Pre-Define: Asset Management", "12/1/2008", "12/31/2008", "Bill" }, { "Pre-Define: O-Parts", "1/1/2009", "1/30/2009", "Bill" }, { "Pre-Define: Parts Reconciliation", "1/1/2009", "1/30/2009", "Bill" }, { "DMAIC: Parts Reconciliation #1", "2/9/2009", "12/31/2009", "Bill" } };
        StringBuilder ganttdata = new StringBuilder();
        for (String[] row : data) {
            ganttdata.append(row[0]).append(',');
            ganttdata.append(row[1]).append(',');
            ganttdata.append(row[2]).append(',');
            ganttdata.append(row[3]).append('\n');
        }
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, ganttdata.toString());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "Project");
        md.setColumnType(1, DataType.STRING);
        md.setColumnName(2, "Start Date");
        md.setColumnType(2, DataType.DATE);
        md.setColumnName(3, "Projected End");
        md.setColumnType(3, DataType.DATE);
        md.setColumnName(4, "Black Belt");
        md.setColumnType(4, DataType.STRING);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("Sample Gantt Chart");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("Series");
        sd.setSource(ds.getId());
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setZColumn(3);
        sd.setSeriesNameFromData(4);
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_GANTT);
        ci.setRenderType("Gantt");
        ci.setProperty("title.text", "Gantt Chart XXX");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("Gantt chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "Gantt Chart");
    }

    @Test
    public void testRandomWalk() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, getRandomWalkValues());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "d");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "stock 1");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "stock 2");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "stock 3");
        md.setColumnType(4, DataType.INTEGER);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("A Random Walk of stock prices\nDown Wall Street");
        for (int i = 1; i <= 3; i++) {
            SeriesDescriptor sd = new SeriesDescriptor();
            sd.setName("Stock " + i);
            sd.setYColumn(i + 1);
            sd.setSource(ds.getId());
            ci.addSeriesDescriptor(sd);
        }
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", "Random Walk - test overticking on y axis");
        ci.setProperty("range-axis-0.tickUnit", "0.5");
        ci.setProperty("range-axis-0.autoTickUnitSelection", "false");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("RANDOM WALK OVERTICK chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "RandomWalk");
    }

    @Test
    public void testRandomWalkWithSameSeriesNames() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        ds.setProperty(DataSourceInfo.CSV_DATA, getRandomWalkValues());
        Metadata md = new Metadata(4);
        md.setColumnName(1, "d");
        md.setColumnType(1, DataType.DATE);
        md.setColumnName(2, "stock 1");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "stock 2");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "stock 3");
        md.setColumnType(4, DataType.INTEGER);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("A Random Walk of stock prices\nDown Wall Street");
        for (int i = 1; i <= 3; i++) {
            SeriesDescriptor sd = new SeriesDescriptor();
            sd.setName("Stock");
            sd.setYColumn(i + 1);
            sd.setSource(ds.getId());
            ci.addSeriesDescriptor(sd);
        }
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", "Random Walk With Same Series Name");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("RANDOM WALK chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        t = System.currentTimeMillis();
        cr = createChart(cb);
        System.err.println("RANDOM WALK chart with no template props create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "RandomWalk With Same Series Names");
    }

    @Test
    public void testOHLC() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "OHLC");
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Candlestick");
        cb.setChartInfo(ci);
        DataSourceInfo ds = h2Info("TIMESERIES");
        ds.setDataScript("SELECT d,x,x+1,x-1,x+(RAND()-1),y FROM TIMESERIES ORDER BY D DESC LIMIT 20");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("ohlc 1");
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setZColumn(3);
        sd.setRenderer("Candlestick");
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(ds);
        cb.validate();
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "ohlc");
        {
            ci.setHeight(150);
            ci.setWidth(250);
            SimpleProps sp = new SimpleProps();
            sp.put("renderer.baseSeriesVisibleInLegend", "false");
            sp.put("title.text", "");
            ci.loadProperties(sp);
            cr = createChart(cb);
            cache.putChart(cr, cb, null);
            this.renameChart(cr, "ohlcsmall");
        }
    }

    @Test
    public void testTimeBubble() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "Tiny Pink Bubbles...(Don Ho!)");
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Bubble");
        cb.setChartInfo(ci);
        DataSourceInfo ds = h2Info("TIMESERIES");
        ds.setDataScript("SELECT d,x,y/2 FROM TIMESERIES ORDER BY D DESC LIMIT 10");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("time bubble");
        sd.setColor("gradient:#ffbbbb,#e0e0e0,0,0,300,300,true");
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setZColumn(3);
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(ds);
        cb.validate();
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "time_bubble");
    }

    @Test
    public void testTimeBubbleWithCMLogo() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "Tiny Pink Bubbles...(With ChartMechanic Logo)");
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Bubble");
        cb.setChartInfo(ci);
        DataSourceInfo ds = h2Info("TIMESERIES");
        ds.setDataScript("SELECT d,x,y/2 FROM TIMESERIES ORDER BY D DESC LIMIT 10");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("time bubble");
        sd.setColor("gradient:#ffbbbb,#e0e0e0,0,0,300,300,true");
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setZColumn(3);
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(ds);
        cb.validate();
        LogoInfo logo = new LogoInfo();
        logo.setVisible(true);
        logo.setUrl("/com/bayareasoftware/chartengine/chart/cmlogo-150-black.png");
        ChartDiskResult cr = createChart(cb, logo);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "time_bubble with CM logo");
    }

    @Test
    public void testTimeBubbleWithTextStringLogo() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "Tiny Pink Bubbles...(With Text String Logo)");
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Bubble");
        cb.setChartInfo(ci);
        DataSourceInfo ds = h2Info("TIMESERIES");
        ds.setDataScript("SELECT d,x,y/2 FROM TIMESERIES ORDER BY D DESC LIMIT 10");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("time bubble");
        sd.setColor("gradient:#ffbbbb,#e0e0e0,0,0,300,300,true");
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setZColumn(3);
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(ds);
        cb.validate();
        LogoInfo logo = new LogoInfo();
        logo.setVisible(true);
        logo.setTxt("Eat at Joes");
        ChartDiskResult cr = createChart(cb, logo);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "time_bubble with text string logo");
    }

    @Test
    public void testXYWithNulls() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        String s = "10,1\n" + "20,2\n" + "  ,3\n" + "40,4\n";
        ds.setProperty(DataSourceInfo.CSV_DATA, s);
        Metadata md = new Metadata(3);
        md.setColumnName(1, "colX");
        md.setColumnType(1, DataType.INTEGER);
        md.setColumnName(2, "colY");
        md.setColumnType(2, DataType.INTEGER);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("XY Plot with nulls");
        SeriesDescriptor sd1 = new SeriesDescriptor();
        sd1.setName("Series 1");
        sd1.setXColumn(1);
        sd1.setYColumn(2);
        sd1.setSource(ds.getId());
        ci.addSeriesDescriptor(sd1);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_XY);
        ci.setRenderType("Line and Shape");
        ci.setProperty("title.text", "XY Plot with nulls (should have no data point at Y=3)");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "XY Plot with nulls");
    }

    @Test
    public void testXY() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        String s = getRandomXYValues();
        ds.setProperty(DataSourceInfo.CSV_DATA, s);
        Metadata md = new Metadata(3);
        md.setColumnName(1, "colX");
        md.setColumnType(1, DataType.INTEGER);
        md.setColumnName(2, "colY");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "colZ");
        md.setColumnType(3, DataType.INTEGER);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("A Random XY");
        SeriesDescriptor sd1 = new SeriesDescriptor();
        sd1.setName("Series 1");
        sd1.setXColumn(1);
        sd1.setYColumn(2);
        sd1.setSource(ds.getId());
        ci.addSeriesDescriptor(sd1);
        SeriesDescriptor sd2 = new SeriesDescriptor();
        sd2.setName("Series 2");
        sd2.setXColumn(1);
        sd2.setYColumn(3);
        sd2.setSource(ds.getId());
        ci.addSeriesDescriptor(sd2);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_XY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", "XY Random");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        {
            MarkerDescriptor avg = new MarkerDescriptor();
            avg.setName("S2 Average");
            avg.setFunc(BuiltInFunctions.FN_AVG);
            avg.addArg(new Arg(ArgType.SID, sd2.getSid()));
            avg.setRange(true);
            avg.setAxisIndex(0);
            ci.addMarkerDescriptor(avg);
            SimpleProps sp = new SimpleProps();
            sp.put("paint", "dark_blue");
            sp.put("stroke", "line=2.0|dash=2");
            sp.put("labelTextAnchor", "BOTTOM_CENTER");
            sp.put("labelAnchor", "CENTER");
            sp.put("labelFont", "SansSerif-bold-12");
            avg.setMarkerProps(sp);
        }
        {
            MarkerDescriptor avg = new MarkerDescriptor();
            avg.setName("S1 Average");
            avg.setFunc(BuiltInFunctions.FN_AVG);
            avg.addArg(new Arg(ArgType.SID, sd1.getSid()));
            avg.setRange(true);
            avg.setAxisIndex(0);
            ci.addMarkerDescriptor(avg);
            SimpleProps sp = new SimpleProps();
            sp.put("paint", "red");
            sp.put("stroke", "line=2.0|dash=2");
            sp.put("labelTextAnchor", "TOP_CENTER");
            sp.put("labelAnchor", "CENTER");
            sp.put("labelFont", "SansSerif-bold-12");
            avg.setMarkerProps(sp);
        }
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("XY Random walk chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        t = System.currentTimeMillis();
        cr = createChart(cb);
        System.err.println("XY Random walk chart with no template props create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "XY Random");
    }

    @Test
    public void testXYWithSameSeriesName() throws Exception {
        ChartBundle cb = new ChartBundle();
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        String s = getRandomXYValues();
        ds.setProperty(DataSourceInfo.CSV_DATA, s);
        Metadata md = new Metadata(4);
        md.setColumnName(1, "colX");
        md.setColumnType(1, DataType.INTEGER);
        md.setColumnName(2, "colY");
        md.setColumnType(2, DataType.INTEGER);
        ds.setInputMetadata(md);
        ChartInfo ci = new ChartInfo();
        ci.setDescription("A Random XY Walk");
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("Series");
        sd.setXColumn(1);
        sd.setYColumn(2);
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        sd = new SeriesDescriptor();
        sd.setName("Series");
        sd.setXColumn(1);
        sd.setYColumn(3);
        sd.setSource(ds.getId());
        ci.addSeriesDescriptor(sd);
        ci.setWidth(400);
        ci.setHeight(400);
        ci.setPlotType(PlotType.PLOT_XY);
        ci.setRenderType("Line");
        ci.setProperty("title.text", "XY Random With Same Series Name");
        cb.setChartInfo(ci);
        cb.addDataSource(ds);
        long t = System.currentTimeMillis();
        ChartDiskResult cr = createChart(cb);
        System.err.println("XY Random walk chart create took: " + (System.currentTimeMillis() - t) + " msecs");
        t = System.currentTimeMillis();
        cr = createChart(cb);
        System.err.println("XY Random walk chart with no template props create took: " + (System.currentTimeMillis() - t) + " msecs");
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "XY Random with same series name");
    }

    @Test
    public void testHistogramGaussian() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        ci.setPlotType(PlotType.PLOT_HISTOGRAM);
        cb.setChartInfo(ci);
        DataSourceInfo ds = h2Info("GAUSSIAN");
        ds.setDataScript("SELECT x FROM GAUSSIAN");
        SeriesDescriptor sd = new SeriesDescriptor("gaussian distribution", ds.getId());
        sd.setYColumn(1);
        sd.setHistogramMin(-20d);
        sd.setHistogramMax(20d);
        sd.setHistogramBinSize(2.0d);
        ci.setRenderType("Line And Shape");
        ci.setProperty("title.text", "Gaussian Histogram");
        ci.setWidth(500);
        ci.setHeight(400);
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(ds);
        cb.validate();
        HashMap<String, String> noParams = null;
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, noParams);
        this.renameChart(cr, "gaussian");
    }

    @Test
    public void testLegendConsistency() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            ds = h2Info("LINEAR");
            ds.setDataScript("SELECT x,y FROM LINEAR");
            s = new SeriesDescriptor("y=x", ds.getId());
            s.setRenderer("Line And Shape");
            s.setColor("yellow");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("LINEAR");
            ds.setDataScript("SELECT x,10-x FROM LINEAR");
            s = new SeriesDescriptor("y=10-x", ds.getId());
            s.setRenderer("Line And Shape");
            s.setColor("dark_green");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("RANDOM");
            ds.setDataScript("SELECT x,y FROM RANDOM");
            s = new SeriesDescriptor("y={random}", ds.getId());
            s.setRenderer("Line");
            s.setColor("dark_blue");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
            MarkerDescriptor min = new MarkerDescriptor();
            min.setType(MarkerDescriptor.MARKER_TYPE_NUMERIC);
            min.setRange(true);
            min.setAxisIndex(0);
            min.setFunc(BuiltInFunctions.FN_MIN);
            min.addArg(new Arg(ArgType.SID, s.getSid()));
            min.setName("random MIN");
            SimpleProps sp = new SimpleProps();
            sp.put("paint", "#ff0000");
            sp.put("labelTextAnchor", "BOTTOM_CENTER");
            sp.put("labelAnchor", "CENTER");
            min.setMarkerProps(sp);
            ci.addMarkerDescriptor(min);
            MarkerDescriptor max = new MarkerDescriptor();
            max.setType(MarkerDescriptor.MARKER_TYPE_NUMERIC);
            max.setRange(true);
            max.setAxisIndex(0);
            max.setFunc(BuiltInFunctions.FN_MAX);
            max.addArg(new Arg(ArgType.SID, s.getSid()));
            max.setName("random MAX");
            max.setMarkerProps(sp);
            ci.addMarkerDescriptor(max);
        }
        ci.setProperty("title.text", "Should Be 3 Legend Items");
        ci.setWidth(600);
        ci.setHeight(600);
        ci.setPlotType(PlotType.PLOT_XY);
        ci.setRenderType("Line");
        {
            SimpleProps sp = new SimpleProps();
            sp.put("renderer.stroke", "line=2.0|dash=3");
            sp.put("renderer.paint", "#ff0000");
            ci.loadProperties(sp);
        }
        {
        }
        HashMap<String, String> noParams = null;
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, noParams);
        this.renameChart(cr, "legend_consistency");
    }

    @Test
    public void testRecessionRange() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "US Recession Interval Marker");
        ci.setTimePeriod(TimeConstants.TIME_YEAR);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Line");
        DataSourceInfo mi = getUSRecessionDataSource();
        MarkerDescriptor md = new MarkerDescriptor();
        md.setType(MarkerDescriptor.MARKER_TYPE_DATE_INTERVAL);
        md.setName("US Recessions");
        md.setSource(mi.getId());
        cb.addDataSource(mi);
        SimpleProps sp = new SimpleProps();
        sp.put("paint", "#d0d0d0");
        md.setMarkerProps(sp);
        ci.addMarkerDescriptor(md);
        DataSourceInfo dsi = getRandomAnnualData(1920);
        SeriesDescriptor sd = new SeriesDescriptor("junk timeseries", dsi.getId());
        sd.setYColumn(2);
        ci.addSeriesDescriptor(sd);
        sd = new SeriesDescriptor("junk timeseries #2 ", dsi.getId());
        sd.setYColumn(3);
        ci.addSeriesDescriptor(sd);
        cb.addDataSource(dsi);
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "recession_marker");
    }

    @Test
    public void testCategory1() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_CATEGORY);
        ci.setProperty("title.text", "Multi-series, Multi-axis category");
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Bar");
        DataSourceInfo ds;
        SeriesDescriptor s;
        {
            ds = h2Info("CATEGORY");
            ds.setDataScript("SELECT name,val1 FROM CATEGORY");
            s = new SeriesDescriptor("S1", ds.getId());
            s.setAxisIndex(0);
            s.setVisible(true);
            cb.addDataSource(ds);
            ci.addSeriesDescriptor(s);
        }
        {
            ds = h2Info("CATEGORY");
            ds.setDataScript("SELECT name,val2 FROM CATEGORY");
            s = new SeriesDescriptor("S2", ds.getId());
            s.setRenderer("Bar");
            s.setAxisIndex(1);
            s.setVisible(true);
            cb.addDataSource(ds);
            ci.addSeriesDescriptor(s);
        }
        {
            List<Double> interval = new ArrayList();
            interval.add(90.0);
            interval.add(95.0);
            MarkerDescriptor md = cb.addNumericMarker("90-95 PINK", interval);
            md.setRange(true);
            md.setAxisIndex(1);
            SimpleProps sp = new SimpleProps();
            sp.put("paint", "#ffaaaa");
            md.setMarkerProps(sp);
        }
        SimpleProps sp = new SimpleProps();
        sp.put("plot.orientation", "HORIZONTAL");
        setRangeProps(sp, 0, "S1 Axis", null, null, null);
        setRangeProps(sp, 1, "S2 Axis", null, null, null);
        ci.loadProperties(sp);
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "category_1");
    }

    @Test
    public void testMedalPie() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_PIE);
        ci.setProperty("title.text", "Gold Medals by Country");
        ci.setProperty("legend.visible", "false");
        ci.setRenderType("Pie");
        ci.setWidth(600);
        ci.setHeight(400);
        DataSourceInfo ds = this.getMedalDataSource();
        cb.addDataSource(ds);
        SeriesDescriptor s;
        s = new SeriesDescriptor("does-not-matter", ds.getId());
        s.setXColumn(1);
        s.setYColumn(2);
        ci.addSeriesDescriptor(s);
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "gold_medal_pie");
    }

    @Test
    public void testStackedBarColors() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_CATEGORY);
        ci.setProperty("title.text", "2008 Olympic Medal Totals (as of 8/9/08)");
        ci.setProperty("legend.position", "RIGHT");
        ci.setProperty("legend.verticalAlignment", "TOP");
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Stacked Bar");
        DataSourceInfo ds = this.getMedalDataSource();
        cb.addDataSource(ds);
        String bronzeRGB = "#CD7F32";
        String silverRGB = "#C0C0C0";
        String goldRGB = "#FFDD70";
        SeriesDescriptor s;
        s = new SeriesDescriptor("Bronze", ds.getId());
        s.setColor(bronzeRGB);
        s.setYColumn(4);
        ci.addSeriesDescriptor(s);
        s = new SeriesDescriptor("Silver", ds.getId());
        s.setColor(silverRGB);
        s.setYColumn(3);
        ci.addSeriesDescriptor(s);
        s = new SeriesDescriptor("Gold", ds.getId());
        s.setYColumn(2);
        s.setColor(goldRGB);
        ci.addSeriesDescriptor(s);
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "stacked_barchart");
    }

    @Test
    public void testBrokerage() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "dividends/interest by month and symbol");
        ci.setProperty("renderer.drawBarOutline", "true");
        ci.setProperty("renderer.shadowVisible", "false");
        ci.setTimePeriod(TimeConstants.TIME_MONTH);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Stacked Bar");
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            ds = h2Info("BROKERAGE");
            ds.setDataScript("SELECT date_trunc('month',d) as DT,symbol,SUM(amount) FROM BROKERAGE" + " GROUP BY DT,symbol order by DT");
            s = new SeriesDescriptor("W", ds.getId());
            s.setXColumn(1);
            s.setYColumn(3);
            s.setSeriesNameFromData(2);
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            List<Double> interval = new ArrayList();
            interval.add(8000.0);
            interval.add(9000.0);
            MarkerDescriptor md = cb.addNumericMarker("8k-9k yellow", interval);
            md.setRange(true);
            md.setAxisIndex(0);
            SimpleProps sp = new SimpleProps();
            sp.put("paint", "light_yellow");
            sp.put("labelAnchor", "CENTER");
            md.setMarkerProps(sp);
        }
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "brokerage_month_symbol");
        cb = new ChartBundle();
        ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "dividends/interest by year");
        ci.setProperty("renderer.paint", "dark_green");
        ci.setTimePeriod(TimeConstants.TIME_YEAR);
        ci.setWidth(600);
        ci.setHeight(400);
        ci.setRenderType("Bar");
        {
            ds = h2Info("BROKERAGE");
            ds.setDataScript("SELECT date_trunc('year',d) as DT,SUM(amount) FROM BROKERAGE" + " GROUP BY DT order by DT");
            s = new SeriesDescriptor("Dividends/Interest", ds.getId());
            s.setXColumn(1);
            s.setYColumn(2);
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "brokerage_annual");
    }

    @Test
    public void testStackedTimeSeries() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "90-day STACKED timeseries from 2008-01-01 (Default: Stacked Bar)");
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Stacked Bar");
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,w FROM TIMESERIES");
            s = new SeriesDescriptor("W", ds.getId());
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,z FROM TIMESERIES");
            s = new SeriesDescriptor("Z", ds.getId());
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,w+z+2 FROM TIMESERIES");
            s = new SeriesDescriptor("W+Z", ds.getId());
            s.setRenderer("Line");
            s.setColor("#000000");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "stacked_timeseries");
    }

    @Test
    public void testStackedTimeSeries2() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "90-day STACKED timeseries from 2008-01-01 (Default Renderer: Line and Shape)");
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Line and Shape");
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,w FROM TIMESERIES");
            s = new SeriesDescriptor("W", ds.getId());
            s.setRenderer("Stacked Bar");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,z FROM TIMESERIES");
            s = new SeriesDescriptor("Z", ds.getId());
            s.setRenderer("Stacked Bar");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,w+z+2 FROM TIMESERIES");
            s = new SeriesDescriptor("W+Z", ds.getId());
            s.setColor("#000000");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "stacked_timeseries_2");
    }

    @Test
    public void testOnlyTimestamp() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "Timestamp only, 1 second interval");
        ci.setTimePeriod(TimeConstants.TIME_SECOND);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Line");
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            File f = new File("../ds/test/data/PingOfBaseStation.txt");
            ds = DataInference.get().inferFromURL(f.toURL().toString(), 200).getDataSource();
            s = new SeriesDescriptor("Ping Time", ds.getId());
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "only_timestamps");
        p("created chart: " + cr);
    }

    @Test
    public void testSimpleTimeSeries() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_TIME);
        ci.setProperty("title.text", "90-day timeseries from 2008-01-01");
        ci.setTimePeriod(TimeConstants.TIME_DAY);
        ci.setWidth(700);
        ci.setHeight(400);
        ci.setRenderType("Line And Shape");
        SeriesDescriptor s;
        DataSourceInfo ds;
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,x FROM TIMESERIES");
            s = new SeriesDescriptor("X", ds.getId());
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("TIMESERIES");
            ds.setDataScript("SELECT d,y FROM TIMESERIES");
            s = new SeriesDescriptor("Y", ds.getId());
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            MarkerDescriptor md;
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2008);
            cal.set(Calendar.DAY_OF_YEAR, 31 + 28 + 2);
            md = cb.addDateMarker("MARCH 2nd", cal.getTime());
            SimpleProps sp = new SimpleProps();
            sp.put("stroke", "line=2.0|dash=2");
            sp.put("paint", "#ffaa00");
            sp.put("labelTextAnchor", "BOTTOM_CENTER");
            sp.put("labelAnchor", "CENTER");
            sp.put("labelFont", "SansSerif-bold-12");
            sp.put("labelPaint", "#ffaa00");
            md.setMarkerProps(sp);
            md = cb.addDateIntervalMarker("TUESDAY", getTuesdayRange());
        }
        ChartDiskResult cr = createChart(cb);
        cache.putChart(cr, cb, null);
        this.renameChart(cr, "simple_timeseries");
        p("created chart: " + cr);
    }

    @Test
    public void testBadSeries() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        ci.setPlotType(PlotType.PLOT_HISTOGRAM);
        SeriesDescriptor sd = new SeriesDescriptor();
        sd.setName("badSeries SHOULD NOT BE DISPLAYED");
        sd.setSource("100");
        ci.addSeriesDescriptor(sd);
        ci.setRenderType("Line And Shape");
        ci.setProperty("title.text", "Gaussian Histogram");
        ci.setWidth(500);
        ci.setHeight(400);
        try {
            ChartDiskResult cr = cache.prepChartResult(cb, null);
            cr.setShowErrorImage(true);
            cr = createChart(cb, null, cr);
            cache.putChart(cr, cb, null);
            this.renameChart(cr, "bad_series");
            p("created chart: " + cr);
        } catch (Exception e) {
            p("creation of chart with bad data source name failed as expected");
        }
    }

    @Test
    public void testMathFunctions() throws Exception {
        ChartBundle cb = new ChartBundle();
        ChartInfo ci = new ChartInfo();
        cb.setChartInfo(ci);
        DataSourceInfo ds;
        ci.setId("math");
        ci.setProperty("title.text", "Math Functions");
        ci.setWidth(600);
        ci.setHeight(600);
        ci.setPlotType(PlotType.PLOT_XY);
        ci.setRenderType("Dot");
        SimpleProps props = new SimpleProps();
        props.put("renderer.dotWidth", "10");
        props.put("renderer.baseShape", "triangle-up");
        props.put("renderer.dotHeight", "10");
        props.put("plot.outlineVisible", "false");
        setRangeProps(props, 0, "y=x", 0d, 50d, "yellow");
        setRangeProps(props, 1, "y=x^2 LOG axis", 0d, 100d, "dark_green");
        setRangeProps(props, 2, "y={random}", 0d, 30d, "dark_blue");
        props.put(ChartInfo.getRangeAxisPropertyPrefix(2) + "." + ChartConstants.CM_PROP_PREFIX + "axisLocation", "TOP_OR_LEFT");
        props.put(ChartInfo.getRangeAxisPropertyPrefix(1) + "." + ChartConstants.CM_PROP_PREFIX + "axisType", "log");
        setRangeProps(props, 3, "y=100-20*x", -100d, 100d, "#a500ff");
        ci.loadProperties(props);
        DataSourceInfo linearDS = null;
        SeriesDescriptor s;
        {
            ds = linearDS = h2Info("LINEAR");
            ds.setDataScript("SELECT x,y FROM LINEAR");
            s = new SeriesDescriptor("y=x (bar,yellow)", ds.getId());
            s.setRenderer("Bar");
            s.setColor("yellow");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("QUADRATIC");
            ds.setDataScript("SELECT x,y FROM QUADRATIC");
            s = new SeriesDescriptor("y=x^2 (line/shape,dark_green)", ds.getId());
            s.setAxisIndex(1);
            s.setRenderer("Line And Shape");
            s.setColor("dark_green");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("RANDOM");
            ds.setDataScript("SELECT x,y FROM RANDOM");
            s = new SeriesDescriptor("y={noise,baseline=10,range=+[0-10]} (step,dark_blue)", ds.getId());
            s.setAxisIndex(2);
            s.setRenderer("Step");
            s.setColor("dark_blue");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        {
            ds = h2Info("LINEAR");
            ds.setDataScript("SELECT x,100-20*x FROM LINEAR");
            s = new SeriesDescriptor("y=100-20*x (default render/dot,purple)", ds.getId());
            s.setColor("#a500ff");
            ci.addSeriesDescriptor(s);
            cb.addDataSource(ds);
        }
        ds = h2Info("LINEAR");
        ds.setDataScript("SELECT x,50+10*x FROM LINEAR");
        s = new SeriesDescriptor("y=50+10*x (SHOULD NOT BE VISIBLE)", ds.getId());
        cb.addDataSource(ds);
        s.setRenderer("Line And Shape");
        s.setColor("blue");
        s.setVisible(false);
        ci.addSeriesDescriptor(s);
        MarkerDescriptor md = new MarkerDescriptor();
        cb.addNumericMarker("Seven (domain)", 7.0);
        cb.addNumericMarker("Three (domain)", 3.0);
        md = cb.addNumericMarker("Twenty-three (range 2)", 23.0);
        md.setRange(true);
        md.setAxisIndex(2);
        md.setVisible(true);
        SimpleProps sp = new SimpleProps();
        sp.put("paint", "dark_blue");
        sp.put("stroke", "line=2.0|dash=2");
        sp.put("labelTextAnchor", "BOTTOM_CENTER");
        sp.put("labelAnchor", "CENTER");
        sp.put("labelFont", "SansSerif-bold-12");
        md.setMarkerProps(sp);
        HashMap<String, String> noParams = null;
        ChartDiskResult dr = cache.prepChartResult(cb, noParams);
        dr.setGenerateImageMap(true);
        ChartDiskResult cr = createChart(cb, null, dr);
        cache.putChart(cr, cb, noParams);
        mathPNGName = cr.getImageMapId();
        this.renameChart(cr, "math");
        p("created chart: " + cr);
    }

    private static void setRangeProps(SimpleProps props, int index, String label, Double lower, Double upper, String color) {
        String prefix = ChartInfo.getRangeAxisPropertyPrefix(index) + '.';
        if (lower != null) props.put(prefix + "lowerBound", "" + lower);
        if (upper != null) props.put(prefix + "upperBound", "" + upper);
        if (label != null) props.put(prefix + "label", label);
        if (color != null) {
            props.put(prefix + "axisLinePaint", color);
            props.put(prefix + "labelPaint", color);
            props.put(prefix + "tickLabelPaint", color);
            props.put(prefix + "tickMarkPaint", color);
        }
    }

    private static void p(String s) {
        System.err.println("[CreateImgTest] " + s);
    }

    private static void dropTables(Connection conn) throws Exception {
        try {
            Statement st = conn.createStatement();
            st.execute("DROP TABLE LINEAR");
            st.execute("DROP TABLE QUADRATIC");
            st.execute("DROP TABLE RANDOM");
            st.execute("DROP TABLE GAUSSIAN");
            st.execute("DROP TABLE TIMESERIES");
            st.execute("DROP TABLE CATEGORY");
            st.close();
        } catch (SQLException ignore) {
        } finally {
            conn.close();
        }
    }

    private static void setupBrokerage(Connection conn) throws Exception {
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE BROKERAGE (d TIMESTAMP, symbol VARCHAR(20), amount FLOAT8)");
        st.execute("CREATE ALIAS DATE_TRUNC FOR \"com.bayareasoftware.chartengine.h2.Functions.dateTrunc\"");
        CallableStatement cs = conn.prepareCall("INSERT INTO BROKERAGE (d,symbol,amount) VALUES (?,?,?)");
        File brokerFile = new File("../ds/test/data/brokerage.csv");
        String brokerUrl = brokerFile.toURI().toString();
        DataSourceInfo dsi = DataInference.get().inferFromURL(brokerUrl, -1).getDataSource();
        DataStream ds = DSFactory.createDataSource(dsi).getDataStream();
        while (ds.next()) {
            Date d = ds.getDate(1);
            String symbol = ds.getString(4);
            Double amt = ds.getDouble(6);
            if (amt != null) {
                cs.setTimestamp(1, new Timestamp(d.getTime()));
                cs.setString(2, symbol);
                cs.setDouble(3, amt.doubleValue());
                cs.execute();
            }
        }
        if (false) {
            ResultSet rs = st.executeQuery("SELECT date_trunc('month',d) as DT,symbol,SUM(amount) FROM BROKERAGE" + " GROUP BY DT,symbol order by DT");
            while (rs.next()) {
                Date d = rs.getDate(1);
                String symbol = rs.getString(2);
                double amt = rs.getDouble(3);
                p(d + "," + symbol + "," + amt);
            }
        }
        ds.close();
        st.close();
        cs.close();
    }

    private static void createTables(Connection conn) throws Exception {
        Random rand = new Random(System.currentTimeMillis());
        try {
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE LINEAR (x INTEGER, y INTEGER)");
            st.execute("CREATE TABLE QUADRATIC (x INTEGER, y INTEGER)");
            st.execute("CREATE TABLE RANDOM (x INTEGER, y INTEGER)");
            st.execute("CREATE TABLE GAUSSIAN (x FLOAT8)");
            st.execute("CREATE TABLE TIMESERIES (d TIMESTAMP, x FLOAT8, y FLOAT8, w FLOAT8, z FLOAT8)");
            st.execute("CREATE TABLE CATEGORY (name VARCHAR(20), val1 INTEGER, val2 INTEGER)");
            st.close();
            CallableStatement c1 = conn.prepareCall("INSERT INTO LINEAR (x,y) VALUES (?, ?)");
            CallableStatement c2 = conn.prepareCall("INSERT INTO QUADRATIC (x,y) VALUES (?, ?)");
            CallableStatement c3 = conn.prepareCall("INSERT INTO RANDOM (x,y) VALUES (?, ?)");
            CallableStatement c4 = conn.prepareCall("INSERT INTO CATEGORY (name,val1,val2) VALUES (?, ?, ?)");
            for (int x = 0; x <= 10; x++) {
                c1.setInt(1, x);
                c1.setInt(2, x);
                c2.setInt(1, x);
                c2.setInt(2, x * x);
                int r = rand.nextInt();
                r = Math.abs(r);
                r %= 10;
                r += 10;
                c3.setInt(1, x);
                c3.setInt(2, r);
                c1.execute();
                c2.execute();
                c3.execute();
                c4.setString(1, "Cat-" + x);
                c4.setInt(2, (rand.nextInt() % 10) + 1);
                c4.setInt(3, Math.abs(rand.nextInt() % 100));
                c4.execute();
            }
            c1.close();
            c2.close();
            c3.close();
            c4.close();
            CallableStatement gs = conn.prepareCall("INSERT INTO GAUSSIAN(x) VALUES (?)");
            for (int i = 0; i < 1000; i++) {
                double val = rand.nextGaussian();
                gs.setDouble(1, val);
                gs.execute();
            }
            setupTimeseries(conn);
            setupBrokerage(conn);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private static void setupTimeseries(Connection conn) throws SQLException {
        CallableStatement call = conn.prepareCall("INSERT INTO TIMESERIES (d,x,y,w,z) VALUES (?, ?, ?, ?, ?)");
        Random rand = new Random(0L);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        for (int i = 0; i < 90; i++) {
            call.setDate(1, new java.sql.Date(cal.getTime().getTime()));
            call.setDouble(2, rand.nextDouble() * 10.0d + 10);
            call.setDouble(3, rand.nextDouble() * 10.0d);
            call.setDouble(4, Math.abs(rand.nextDouble()) * 10.0d);
            call.setDouble(5, Math.abs(rand.nextDouble()) * 10.0d);
            if (call.executeUpdate() == CallableStatement.EXECUTE_FAILED) {
                throw new RuntimeException("insert to TIMESERIES failed...");
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        call.close();
    }

    private DataSourceInfo getRandomAnnualData(int startYear) throws Exception {
        StringBuilder sb = new StringBuilder();
        rand = new Random();
        for (int i = startYear; i <= 2008; i++) {
            int val = rand.nextInt() % 10;
            int val2 = rand.nextInt() % 10;
            sb.append(i).append(',').append(val).append(',').append(val2).append('\n');
        }
        return DataInference.get().inferFromCSV(sb.toString()).getDataSource();
    }

    private DataSourceInfo getUSRecessionDataSource() throws Exception {
        String url = new File("../ds/test/data/us-recessions.csv").toURI().toString();
        return DataInference.get().inferFromURL(url, -1).getDataSource();
    }

    private DataSourceInfo getMedalDataSource() {
        DataSourceInfo ds = new DataSourceInfo(DataSourceInfo.CSV_TYPE);
        StringBuilder sb = new StringBuilder();
        sb.append("US,2,1,3,6\n");
        sb.append("China,2,1,0,3\n");
        sb.append("South Korea,2,1,0,3\n");
        sb.append("Czech Republic,1,0,0,1\n");
        ds.setProperty(DataSourceInfo.CSV_DATA, sb.toString());
        Metadata md = new Metadata(5);
        md.setColumnName(1, "country");
        md.setColumnType(1, DataType.STRING);
        md.setColumnName(2, "gold");
        md.setColumnType(2, DataType.INTEGER);
        md.setColumnName(3, "silver");
        md.setColumnType(3, DataType.INTEGER);
        md.setColumnName(4, "bronze");
        md.setColumnType(4, DataType.INTEGER);
        md.setColumnName(5, "total");
        md.setColumnType(5, DataType.INTEGER);
        ds.setInputMetadata(md);
        return ds;
    }

    private List<Date[]> getTuesdayRange() {
        List<Date[]> ret = new ArrayList<Date[]>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        while (cal.get(Calendar.MONTH) < Calendar.APRIL) {
            Date[] da = new Date[2];
            da[0] = cal.getTime();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            da[1] = cal.getTime();
            ret.add(da);
            cal.add(Calendar.DAY_OF_YEAR, 6);
        }
        return ret;
    }

    private static final String JS = "<script language=\"javascript\"  type=\"text/javascript\">\n" + " function hilight_chart(series,x,y) {\n" + " var cd = document.getElementById('chart_desc');\n" + "cd.style.color='red';\n" + "cd.innerHTML = series + ',' + x + ',' + y;\n" + "}\n" + "</script>";

    @AfterClass
    public static void produceIndexOfCharts() throws IOException {
        File f = new File(cacheRoot, "index.html");
        Writer out = new FileWriter(f);
        out.write("<html><head><title>image cache listing</title></head><body>\n");
        out.write(JS);
        String[] names = cacheRoot.list();
        Arrays.sort(names);
        List<String> l = getChartNames(names);
        out.write("<p>\n");
        for (String n : l) {
            out.write("<a href=\"#" + n + "\">" + n + "</a><br/>\n");
        }
        for (String n : l) {
            out.write("<a name=\"" + n + "\"><b>" + n + "</b></a><br/>\n");
            if (n.equals("math")) {
                out.write("<div id=\"chart_desc\"></div>");
                out.write("<img src=\"" + n + ".png\" border=\"0\" usemap=\"#" + mathPNGName + "\"/>\n");
                out.write("<img src=\"" + n + "_t.png\"/><br/>\n");
                out.write(readContents(new File(cacheRoot, n + ".imap")));
            } else {
                out.write("<img src=\"" + n + ".png\" border=\"0\"/>\n");
                String tname = n + "_t.png";
                if (new File(cacheRoot, tname).exists()) {
                    out.write("<img src=\"" + tname + "\"/>\n");
                }
                out.write("<br/>\n");
            }
        }
        out.write("</p>\n");
        out.write("</body></html>");
        out.close();
    }

    private static List<String> getChartNames(String[] names) {
        List<String> ret = new ArrayList<String>();
        for (String n : names) {
            if (n.endsWith(".png") && !n.endsWith("_t.png")) {
                int len = n.length();
                ret.add(n.substring(0, len - 4));
            }
        }
        return ret;
    }

    private void renameChart(ChartDiskResult dr, String newbase) {
        String p = dr.getImagePath();
        String tp = dr.getThumbPath();
        if (p == null || tp == null) {
            return;
        }
        File f = new File(p);
        File to = new File(cacheRoot, newbase + ".png");
        f.renameTo(to);
        f = new File(tp);
        to = new File(cacheRoot, newbase + "_t.png");
        f.renameTo(to);
        f = new File(dr.getImageMapPath());
        to = new File(cacheRoot, newbase + ".imap");
        f.renameTo(to);
    }

    private static String readContents(File f) throws IOException {
        FileReader rdr = new FileReader(f);
        StringBuilder sb = new StringBuilder();
        try {
            char[] buf = new char[1024];
            int r;
            while ((r = rdr.read(buf)) > 0) {
                sb.append(buf, 0, r);
            }
        } finally {
            rdr.close();
        }
        return sb.toString();
    }

    private static DataSourceInfo h2Info(String tableName) {
        DataSourceInfo d = new DataSourceInfo(DataSourceInfo.JDBC_TYPE);
        d.setTableName(tableName);
        d.loadProperties(h2dbprops);
        return d;
    }
}
