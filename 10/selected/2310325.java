package org.fao.waicent.kids.giews;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.fao.waicent.attributes.DataLegendDefinition;
import org.fao.waicent.attributes.SourceDefinition;
import org.fao.waicent.db.dbConnectionManager;
import org.fao.waicent.db.dbConnectionManagerPool;
import org.fao.waicent.gif.GifEncoder;
import org.fao.waicent.kids.Configuration;
import org.fao.waicent.util.Debug;
import org.fao.waicent.util.FileResource;
import org.fao.waicent.util.ImageCreator;
import org.fao.waicent.util.XMLUtil;
import org.fao.waicent.util.XOutline;
import org.fao.waicent.util.XPatternOutline;
import org.fao.waicent.util.XPatternPaint;
import org.fao.waicent.xmap2D.FeatureLayer;
import org.fao.waicent.xmap2D.FeatureProperties;
import org.fao.waicent.xmap2D.Map;
import org.fao.waicent.xmap2D.MapContext;
import org.fao.waicent.xmap2D.RasterData;
import org.fao.waicent.xmap2D.RasterLayer;
import org.fao.waicent.xmap2D.coordsys.ProjectionCategories;
import org.fao.waicent.xmap2D.util.ParamList;

public class ImageCropper {

    private static String new_filename;

    private static String gif_name;

    private static final String DEM = "Digital Elevation Model";

    private static String image_window;

    private static String context_path;

    /**
     *  This will create instance of ImageCropper given the database_ini
     *
     * @param     String     database_ini file
     *
     * @author    macdc      08/05/2004
     */
    public ImageCropper(String database_ini) {
        this.database_ini = database_ini;
    }

    /**
     *   This method overloads the old method to include country list
     *
     * @param        String        output file path
     * @param        Calendar      start date
     * @param        Calendar      end date
     * @param        String        countries
     *
     * @author       macdc     06/18/2004
     */
    public void generateGIFAll(String output_filepath, Calendar from_date, Calendar to_date, String[] countries) throws Exception {
        System.out.println("ImageCropper.generateGIFAll begin");
        System.out.println(" ImageCropper: generateGIFAll  BEGIN from_date: " + from_date);
        System.out.println(" ImageCropper: generateGIFAll  BEGIN to_date:  " + to_date);
        System.out.println(" ImageCropper: generateGIFAll total free memory: " + Runtime.getRuntime().totalMemory());
        int output_maximum_size = 1000;
        Statement stmt = null;
        ResultSet rs = null;
        String country_codes = "";
        String append_sql = "";
        for (int j = 0; j < countries.length; j++) {
            if (j == countries.length - 1) {
                country_codes = country_codes + "'" + countries[j] + "'";
            } else {
                country_codes = country_codes + "'" + countries[j] + "',";
            }
        }
        System.out.println("******* country_codes: " + country_codes);
        String SQL = "select ID, CODE, LABEL, X, Y, WIDTH, HEIGHT " + "from MAP " + "where X is not null and CODE in (" + country_codes + ")";
        Connection con = popConnection(database_ini);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                String area_code = rs.getString("CODE");
                String area_label = rs.getString("LABEL");
                Rectangle2D area_bounds = new Rectangle2D.Double(rs.getDouble("X"), rs.getDouble("Y"), rs.getDouble("WIDTH"), rs.getDouble("HEIGHT"));
                String area_output_filepath = output_filepath + File.separator + area_code;
                new File(area_output_filepath).mkdirs();
                generateGIFforArea(con, from_date, to_date, area_code, area_label, area_bounds, output_maximum_size, area_output_filepath);
            }
            con.commit();
        } catch (Exception e) {
            Debug.println("ImageCropper.generateGIFAll e: " + e.getMessage());
            con.rollback();
            throw new Exception("An exception thrown in generateGIFAll():" + e);
        } finally {
            try {
                rs.close();
                stmt.close();
                pushConnection(con);
            } catch (Exception e) {
                Debug.println("Exception in closing resources!!!");
                throw new Exception("Exception in generateGIFAll: " + e);
            }
        }
        System.out.println("ImageCropper.generateGIFAll end");
    }

    public static void generateGIFforArea(Connection con, Calendar from_date, Calendar to_date, String area_code, String area_label, Rectangle2D area_bounds, int output_maximum_size, String output_filepath) throws Exception {
        System.out.println("ImageCropper.generateGIFforArea begin");
        String products = getCountryProducts(con, area_code);
        Statement stmt = null;
        ResultSet rs = null;
        String SQL = "select CATEGORY, LABEL, TIMESERIES, " + "IMAGE_FILE_PATTERN, LEGEND_FILE, " + "DIFF_LEGEND_FILE, X, Y, WIDTH, " + "HEIGHT, NOTES, AVG_IMAGE_FILE_PATTERN " + "from RASTER_LAYER_CATEGORY " + "where X is not null and image_window= '" + image_window + "' and category in (" + products + ") " + "order by CATEGORY, TIMESERIES";
        System.out.println("generateGIFforArea SQL: " + SQL);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(SQL);
            Rectangle2D area_bounds_ref = new Rectangle2D.Double(area_bounds.getX(), area_bounds.getY(), area_bounds.getWidth(), area_bounds.getHeight());
            while (rs.next()) {
                String category = rs.getString("CATEGORY");
                String timeseries = rs.getString("TIMESERIES");
                String raster_label = "to be replaced";
                int time_period = getTimeseries(timeseries);
                if (timeseries != null && timeseries.length() > 0) {
                    raster_label += "[" + rs.getString("TIMESERIES") + "]";
                }
                Rectangle2D raster_bounds = new Rectangle2D.Double(rs.getDouble("X"), rs.getDouble("Y"), rs.getDouble("WIDTH"), rs.getDouble("HEIGHT"));
                String source_pattern = rs.getString("IMAGE_FILE_PATTERN");
                String legend_filename = rs.getString("LEGEND_FILE");
                String diff_legend_filename = rs.getString("DIFF_LEGEND_FILE");
                String avg_pattern = rs.getString("AVG_IMAGE_FILE_PATTERN");
                if (raster_bounds.contains(area_bounds_ref)) {
                    String image_label = area_label + " " + raster_label;
                    String topic_code = "";
                    String note = "";
                    if (time_period == TIMESERIES_NONE) {
                        String output_filename = output_filepath + File.separator + category + ".gif";
                        area_bounds.setRect(area_bounds_ref);
                        generateGIF(con, category, area_code, topic_code, timeseries, null, null, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_pattern, null, legend_filename, output_filename, output_maximum_size);
                    } else {
                        Calendar time = new GregorianCalendar();
                        time.set(2001, 12, 1);
                        Calendar start = from_date;
                        Calendar end = to_date;
                        while (!time.after(end)) {
                            if (time.after(start) && time.before(end)) {
                                String source_filename = expand(time, source_pattern);
                                Debug.println("source_filename: " + source_filename);
                                String avg_filename = expand(time, avg_pattern);
                                Debug.println("avg_filename: " + source_filename);
                                String[] diff_images = { null, "Year", timeseries };
                                String use_legend_filename = legend_filename;
                                for (int d = 0; d < diff_images.length; d++) {
                                    String diff_filename = null;
                                    String diff_timeseries = diff_images[d];
                                    int diff_period = getTimeseries(diff_timeseries);
                                    if (diff_period != TIMESERIES_NONE) {
                                        Calendar diff_time = (Calendar) time.clone();
                                        add(diff_time, start, end, diff_period, -1);
                                        diff_filename = expand(diff_time, source_pattern);
                                        use_legend_filename = diff_legend_filename;
                                    }
                                    String output_pattern = getGIFOutputPattern(output_filepath, category, time_period, diff_period, false);
                                    String output_filename = expand(time, output_pattern);
                                    area_bounds.setRect(area_bounds_ref);
                                    generateGIF(con, category, area_code, topic_code, timeseries, diff_timeseries, time, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_filename, diff_filename, use_legend_filename, output_filename, output_maximum_size);
                                }
                                if (avg_filename != null) {
                                    use_legend_filename = diff_legend_filename;
                                    String output_pattern = getGIFOutputPattern(output_filepath, category, time_period, 2, true);
                                    String output_filename = expand(time, output_pattern);
                                    area_bounds.setRect(area_bounds_ref);
                                    generateGIF(con, category, area_code, topic_code, timeseries, timeseries, time, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_filename, avg_filename, use_legend_filename, output_filename, output_maximum_size);
                                }
                            }
                            if (!add(time, start, end, time_period, 1)) {
                                break;
                            }
                        }
                    }
                } else {
                    Debug.println("Not contains ");
                    Debug.println("Raster bounds " + raster_bounds.toString());
                    Debug.println("Area bounds " + area_bounds.toString());
                }
            }
        } catch (Exception e) {
            Debug.println("Exception generateGIFforArea e: " + e.getMessage());
            con.rollback();
            throw new Exception("Exception generateGIFforArea: " + e);
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                stmt.close();
            } catch (Exception e) {
            }
        }
        System.out.println("ImageCropper.generateGIFforArea end");
    }

    /**
     *   This method overloads the old method to remove the
     *        - insert_raster_layer_pstmt - argument but instead re create this
     *         sql stament inside the function itself and also added Connection
     *         into the argument.
     *
     * @author     macdc     08/05/2004
     */
    private static void generateGIF(Connection con, String category, String area_code, String topic_code, String timeseries, String diff_timeseries, Calendar time, String area_label, String raster_label, String image_label, String note, Rectangle2D bounds, Rectangle2D raster_bounds, String source_filename, String diff_filename, String legend_filename, String output_filename, int output_maximum_size) throws SQLException, IOException {
        System.out.println("ImageCropper.generateGIF begin");
        MapContext map_context = new MapContext("test", new Configuration());
        try {
            Map map = new Map(map_context, area_label, new Configuration());
            map.setCoordSys(ProjectionCategories.default_coordinate_system);
            map.setPatternOutline(new XPatternOutline(new XPatternPaint(Color.white)));
            String type = null;
            RasterLayer rlayer = getRasterLayer(map, raster_label, getLinuxPathEquivalent(source_filename), getLinuxPathEquivalent(diff_filename), type, getLinuxPathEquivalent(legend_filename));
            map.addLayer(rlayer, true);
            map.setBounds2DImage(bounds, true);
            Dimension image_dim = null;
            image_dim = new Dimension((int) rlayer.raster.getDeviceBounds().getWidth() + 1, (int) rlayer.raster.getDeviceBounds().getHeight() + 1);
            if (output_maximum_size > 0) {
                double width_factor = image_dim.getWidth() / output_maximum_size;
                double height_factor = image_dim.getHeight() / output_maximum_size;
                double factor = Math.max(width_factor, height_factor);
                if (factor > 1.0) {
                    image_dim.setSize(image_dim.getWidth() / factor, image_dim.getHeight() / factor);
                }
            }
            map.setImageDimension(image_dim);
            map.scale();
            image_dim = new Dimension((int) map.getBounds2DImage().getWidth(), (int) map.getBounds2DImage().getHeight());
            Image image = null;
            Graphics gr = null;
            image = ImageCreator.getImage(image_dim);
            gr = image.getGraphics();
            try {
                map.paint(gr);
            } catch (Exception e) {
                Debug.println("map.paint error: " + e.getMessage());
            }
            String gif_filename = "";
            try {
                gif_filename = formatPath(category, timeseries, output_filename);
                new File(new_filename).mkdirs();
                new GifEncoder(image, new FileOutputStream(gif_filename)).encode();
            } catch (IOException e) {
                Debug.println("ImageCropper.generateGIF e: " + e.getMessage());
                throw new IOException("GenerateGIF.IOException: " + e);
            }
            PreparedStatement pstmt = null;
            try {
                String delete_raster = "delete raster_layer where " + "label='" + gif_name.trim() + "' and category='" + category.trim() + "' and area_code=' " + area_code.trim() + "'";
                pstmt = con.prepareStatement(delete_raster);
                boolean del = pstmt.execute();
                pstmt.close();
                String insert_raster = "insert into RASTER_LAYER " + "values(RASTER_LAYER_ID.nextval, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "SYSDATE, ?)";
                pstmt = con.prepareStatement(insert_raster);
                pstmt.setString(1, gif_name);
                pstmt.setString(2, category);
                pstmt.setString(3, area_code);
                pstmt.setString(4, topic_code);
                if (time == null) {
                    pstmt.setNull(5, java.sql.Types.DATE);
                } else {
                    pstmt.setDate(5, new java.sql.Date(time.getTimeInMillis()));
                }
                pstmt.setString(6, timeseries);
                pstmt.setString(7, gif_filename);
                pstmt.setNull(8, java.sql.Types.INTEGER);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setDouble(10, raster_bounds.getX());
                pstmt.setDouble(11, raster_bounds.getY());
                pstmt.setDouble(12, raster_bounds.getWidth());
                pstmt.setDouble(13, raster_bounds.getHeight());
                pstmt.setString(14, note);
                int sequence = 0;
                if (gif_name.endsWith("DP")) {
                    sequence = 1;
                } else if (gif_name.endsWith("DY")) {
                    sequence = 2;
                } else if (gif_name.endsWith("DA")) {
                    sequence = 3;
                }
                pstmt.setInt(15, sequence);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                Debug.println("SQLException occurred e: " + e.getMessage());
                con.rollback();
                throw new SQLException("GenerateGIF.SQLException: " + e);
            } finally {
                pstmt.close();
            }
        } catch (Exception e) {
            Debug.println("ImageCropper.generateGIF e: " + e.getMessage());
        }
        System.out.println("ImageCropper.generateGIF end");
    }

    public static RasterLayer getRasterLayer(Map map, String name, String file, String diff_file, String type, String legend_file) throws Exception {
        String cntxt_legend_file = context_path + legend_file;
        String cntxt_diff_file = context_path + diff_file;
        System.out.println("***** legend_file: " + legend_file);
        System.out.println("***** diff_legend_file: " + diff_file);
        RasterLayer rlayer = null;
        ParamList raster_params = new ParamList();
        raster_params.put("LayerName", name);
        Debug.println("getRasterLayer before put RasterFileResource ***file: " + file);
        raster_params.put("RasterFileResource", new FileResource(file));
        raster_params.put("Legend", new DataLegendDefinition(new FileResource(cntxt_legend_file), true));
        byte raster_type = RasterData.parseRasterType(type);
        if (raster_type == -1) {
            raster_type = RasterData.IDA_RASTER_TYPE;
        }
        raster_params.put("RasterType", raster_type);
        if (diff_file != null) {
            raster_params.put("RasterOperand2FileResource", new FileResource(diff_file));
            DifferenceImageOperator diff_op = new DifferenceImageOperator();
            raster_params.put("PixelOperator", diff_op);
        }
        Debug.println("\tRaster: (name,file,diff_file,type,legend)=" + name + ", " + file + ", " + diff_file + ", " + raster_type + ", " + legend_file);
        rlayer = new RasterLayer(raster_params);
        return rlayer;
    }

    public static FeatureLayer getFeatureLayer(Map map, String name, String file, boolean show) throws Exception {
        FeatureLayer flayer = null;
        if (file != null) {
            Debug.println("\tFeature: (name,file,show)=" + name + "," + file + "," + show);
            XPatternPaint fill = new XPatternPaint(new Color(0, 0, 0, 0), Color.black, XPatternPaint.FILL_NONE, (byte) 1);
            XOutline outline = new XOutline(Color.black, XOutline.LINE_NONE, (byte) 1);
            if (show) {
                outline.setStyle(XOutline.LINE_SOLID);
            }
            flayer = new FeatureLayer(name, new SourceDefinition(), ProjectionCategories.default_coordinate_system, "", true, new XPatternOutline(fill, outline), new FileResource(file), -1, -1, null, new Configuration());
        }
        return flayer;
    }

    public static Map getMap(String map_name, String map_file, String raster_name, String raster_file, String raster_type, String raster_legend_file, String feature_name, String feature_file, boolean feature_show, String output_file, int output_maximum_size) throws Exception {
        MapContext map_context = new MapContext(map_file, new Configuration());
        Map map = new Map(map_context, map_name, new Configuration());
        map.setCoordSys(ProjectionCategories.default_coordinate_system);
        map.setPatternOutline(new XPatternOutline(new XPatternPaint(Color.white)));
        RasterLayer rlayer = getRasterLayer(map, raster_name, raster_file, raster_file, raster_type, raster_legend_file);
        map.addLayer(rlayer, true);
        FeatureLayer flayer = getFeatureLayer(map, feature_name, feature_file, feature_show);
        Rectangle2D bounds_ll = null;
        Dimension image_dim = null;
        if (flayer != null) {
            map.addLayer(flayer, true);
            bounds_ll = flayer.getBounds2D();
            map.setBounds2DImage(bounds_ll, true);
            Rectangle2D bounds_raster_image = rlayer.getInRasterSpace(bounds_ll);
            image_dim = new Dimension((int) bounds_raster_image.getWidth() + 1, (int) bounds_raster_image.getHeight() + 1);
        } else {
            bounds_ll = new Rectangle2D.Double();
            rlayer.getLongLatTransform().transform(rlayer.getBounds2D(), bounds_ll, true);
            map.setBounds2DImage(bounds_ll, true);
            image_dim = new Dimension((int) rlayer.raster.getDeviceBounds().getWidth() + 1, (int) rlayer.raster.getDeviceBounds().getHeight() + 1);
        }
        if (output_maximum_size > 0) {
            double width_factor = image_dim.getWidth() / output_maximum_size;
            double height_factor = image_dim.getHeight() / output_maximum_size;
            double factor = Math.max(width_factor, height_factor);
            if (factor > 1.0) {
                Debug.println("INFO: resampling, raster detail too high =" + image_dim + ": threshold=" + output_maximum_size + " factor=" + factor);
                image_dim.setSize(image_dim.getWidth() / factor, image_dim.getHeight() / factor);
            }
        }
        map.setImageDimension(image_dim);
        return map;
    }

    static void generateGIF(String category, String area_code, String topic_code, String timeseries, String diff_timeseries, Calendar time, String area_label, String raster_label, String image_label, String note, Rectangle2D bounds, Rectangle2D raster_bounds, String source_filename, String diff_filename, String legend_filename, String output_filename, int output_maximum_size, PreparedStatement insert_raster_layer_pstmt) throws SQLException, IOException {
        Debug.println("**** ImageCropper.generateGIF begin ****");
        MapContext map_context = new MapContext("test", new Configuration());
        try {
            Map map = new Map(map_context, area_label, new Configuration());
            map.setCoordSys(ProjectionCategories.default_coordinate_system);
            map.setPatternOutline(new XPatternOutline(new XPatternPaint(Color.white)));
            String type = null;
            RasterLayer rlayer = getRasterLayer(map, raster_label, getLinuxPathEquivalent(source_filename), getLinuxPathEquivalent(diff_filename), type, getLinuxPathEquivalent(legend_filename));
            map.addLayer(rlayer, true);
            map.setBounds2DImage(bounds, true);
            Dimension image_dim = null;
            image_dim = new Dimension((int) rlayer.raster.getDeviceBounds().getWidth() + 1, (int) rlayer.raster.getDeviceBounds().getHeight() + 1);
            if (output_maximum_size > 0) {
                double width_factor = image_dim.getWidth() / output_maximum_size;
                double height_factor = image_dim.getHeight() / output_maximum_size;
                double factor = Math.max(width_factor, height_factor);
                if (factor > 1.0) {
                    Debug.println("INFO: resampling =" + image_dim + ": threshold=" + output_maximum_size + " factor=" + factor);
                    image_dim.setSize(image_dim.getWidth() / factor, image_dim.getHeight() / factor);
                }
            }
            map.setImageDimension(image_dim);
            map.scale();
            image_dim = new Dimension((int) map.getBounds2DImage().getWidth(), (int) map.getBounds2DImage().getHeight());
            Image image = null;
            Graphics gr = null;
            image = ImageCreator.getImage(image_dim);
            gr = image.getGraphics();
            try {
                map.paint(gr);
            } catch (Exception e1) {
                Debug.println("map.paint error: " + e1.getMessage());
            }
            String gif_filename = "";
            try {
                gif_filename = formatPath(category, timeseries, output_filename);
                new File(new_filename).mkdirs();
                Debug.println("MAC new_filename: " + new_filename);
                Debug.println("MAC gif_filename: " + gif_filename);
                Debug.println("MAC gif_name: " + gif_name);
                new GifEncoder(image, new FileOutputStream(gif_filename)).encode();
            } catch (IOException ie) {
                Debug.println("ImageCropper.generateGIF IOE: " + ie.getMessage());
            }
            insert_raster_layer_pstmt.setString(1, gif_name);
            insert_raster_layer_pstmt.setString(2, category);
            insert_raster_layer_pstmt.setString(3, area_code);
            insert_raster_layer_pstmt.setString(4, topic_code);
            if (time == null) {
                insert_raster_layer_pstmt.setNull(5, java.sql.Types.DATE);
            } else {
                insert_raster_layer_pstmt.setDate(5, new java.sql.Date(time.getTimeInMillis()));
            }
            insert_raster_layer_pstmt.setString(6, timeseries);
            insert_raster_layer_pstmt.setString(7, gif_filename);
            insert_raster_layer_pstmt.setNull(8, java.sql.Types.INTEGER);
            insert_raster_layer_pstmt.setNull(9, java.sql.Types.INTEGER);
            insert_raster_layer_pstmt.setDouble(10, raster_bounds.getX());
            insert_raster_layer_pstmt.setDouble(11, raster_bounds.getY());
            insert_raster_layer_pstmt.setDouble(12, raster_bounds.getWidth());
            insert_raster_layer_pstmt.setDouble(13, raster_bounds.getHeight());
            insert_raster_layer_pstmt.setString(14, note);
            insert_raster_layer_pstmt.execute();
        } catch (Exception e) {
            Debug.println("ImageCropper.generateGIF e: " + e.getMessage());
        }
        Debug.println("**** ImageCropper.generateGIF end ****");
    }

    public static String getGIFOutputPattern(String output_filepath, String category, int time_period, int diff_period, boolean average_image) {
        StringBuffer output_pattern = new StringBuffer(output_filepath);
        output_pattern.append(File.separator);
        output_pattern.append(category);
        if (diff_period != TIMESERIES_NONE) {
            if (diff_period == time_period) {
                if (average_image) {
                    output_pattern.append("_DA");
                } else {
                    output_pattern.append("_D");
                }
            } else {
                switch(diff_period) {
                    case TIMESERIES_YEAR:
                        output_pattern.append("_DY");
                        break;
                    case TIMESERIES_MONTH:
                        output_pattern.append("_DM");
                        break;
                    case TIMESERIES_DEKAD:
                        if (average_image) {
                            output_pattern.append("_DA");
                        } else {
                            output_pattern.append("_DK");
                        }
                        break;
                    case TIMESERIES_BIMONTH:
                        output_pattern.append("_DB");
                        break;
                    case TIMESERIES_DAY:
                        output_pattern.append("_DD");
                        break;
                }
            }
        }
        switch(time_period) {
            case TIMESERIES_YEAR:
                output_pattern.append("_Y%yyyy%");
                break;
            case TIMESERIES_MONTH:
                output_pattern.append("_M%yyyy%%MM%");
                break;
            case TIMESERIES_DEKAD:
                output_pattern.append("_K%yyyy%%MM%%Dekad%");
                break;
            case TIMESERIES_BIMONTH:
                output_pattern.append("_B%yyyy%%MM%%Bimonth%");
                break;
            case TIMESERIES_DAY:
                output_pattern.append("_D%yyyy%%MM%%dd%");
                break;
        }
        output_pattern.append(".gif");
        Debug.println("output_pattern.toString(): " + output_pattern.toString());
        return output_pattern.toString();
    }

    public static void generateGIFAll(Connection con, String output_filepath, Calendar from_date, Calendar to_date) throws Exception {
        int output_maximum_size = 1000;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            String SQL = " select ID, CODE, LABEL, X, Y, WIDTH, HEIGHT" + " from MAP " + " where X is not null and CODE in ('BEN')";
            Debug.println(">>>>> " + SQL);
            rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                String area_code = rs.getString("CODE");
                String area_label = rs.getString("LABEL");
                Rectangle2D area_bounds = new Rectangle2D.Double(rs.getDouble("X"), rs.getDouble("Y"), rs.getDouble("WIDTH"), rs.getDouble("HEIGHT"));
                String area_output_filepath = output_filepath + File.separator + area_code;
                new File(area_output_filepath).mkdirs();
                Debug.println(">>>>> " + area_code);
                Debug.println(">>>>> bounds: " + area_bounds.toString());
                try {
                    generateGIFforArea(con, from_date, to_date, area_code, area_label, area_bounds, output_maximum_size, area_output_filepath);
                } catch (java.lang.OutOfMemoryError e) {
                    Runtime.getRuntime().gc();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            rs.close();
            stmt.close();
        }
    }

    public static void registerFeatureLayer(Connection con) throws Exception {
        Statement select_layer_stmt = null;
        PreparedStatement update_layer_stmt = null;
        PreparedStatement insert_property_stmt = null;
        PreparedStatement delete_property_stmt = null;
        ResultSet rs = null;
        try {
            String project_name = "giews";
            MapContext map_context = new MapContext(project_name, new Configuration());
            String update_layer_SQL = " update FEATURE_LAYER set X = ?, Y = ?, WIDTH = ?, HEIGHT = ? where ID = ? ";
            update_layer_stmt = con.prepareStatement(update_layer_SQL);
            String insert_property_SQL = " insert into FEATURE_LAYER_PROPERTY values ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
            insert_property_stmt = con.prepareStatement(insert_property_SQL);
            String delete_property_SQL = " delete from FEATURE_LAYER_PROPERTY where FEATURE_LAYER_ID =  ?  ";
            delete_property_stmt = con.prepareStatement(delete_property_SQL);
            String select_layer_SQL = " select PROJECT.LABEL as MAP_LABEL, F.ID as ID, F.LABEL as LABEL, F.CATEGORY, F.AREA_CODE, F.TOPIC_CODE, F.SOURCE_FILE as SOURCE_FILE, F.LEGEND_ID, F.METADATA_ID, F.X, F.Y, F.WIDTH, F.HEIGHT, F.NOTES " + " from FEATURE_LAYER F, PROJECT " + " where PROJECT.AREA_CODE = F.AREA_CODE " + " and F.X is null ";
            select_layer_stmt = con.createStatement();
            rs = select_layer_stmt.executeQuery(select_layer_SQL);
            while (rs.next()) {
                String map_name = rs.getString("MAP_LABEL");
                int ID = rs.getInt("ID");
                String layer_name = rs.getString("LABEL");
                String layer_filename = fixUNC(rs.getString("SOURCE_FILE"));
                try {
                    Debug.println("Register layer : " + ID + ", " + layer_name + ", " + layer_filename);
                    Map map = new Map(map_context, map_name, new Configuration());
                    FeatureLayer flayer = getFeatureLayer(map, layer_name, layer_filename, true);
                    map.addLayer(flayer, true);
                    Rectangle2D bounds_ll = bounds_ll = flayer.getBounds2D();
                    update_layer_stmt.setDouble(1, bounds_ll.getX());
                    update_layer_stmt.setDouble(2, bounds_ll.getY());
                    update_layer_stmt.setDouble(3, bounds_ll.getWidth());
                    update_layer_stmt.setDouble(4, bounds_ll.getHeight());
                    update_layer_stmt.setInt(5, ID);
                    update_layer_stmt.execute();
                    delete_property_stmt.setInt(1, ID);
                    delete_property_stmt.execute();
                    Iterator properties = flayer.getFeaturePropertiesIterator();
                    insert_property_stmt.setInt(1, ID);
                    while (properties.hasNext()) {
                        FeatureProperties prop = (FeatureProperties) properties.next();
                        if (prop.code != null && prop.code.length() > 0) {
                            insert_property_stmt.setString(2, prop.getCode());
                            insert_property_stmt.setString(3, prop.getLabel());
                            insert_property_stmt.setString(4, prop.getChildMap());
                            insert_property_stmt.setDouble(5, prop.getLocation().getX());
                            insert_property_stmt.setDouble(6, prop.getLocation().getY());
                            insert_property_stmt.setInt(7, prop.getAnchor());
                            insert_property_stmt.setString(8, (prop.getShow() ? "Y" : "N"));
                            insert_property_stmt.setString(9, (prop.getShowAnchorTriange() ? "Y" : "N"));
                            insert_property_stmt.setString(10, XMLUtil.XMLtoString(prop.getPatternOutline()));
                            insert_property_stmt.execute();
                        }
                    }
                    con.commit();
                } catch (Exception e) {
                    Debug.println(">>>>>> FAILED : " + ID + ", " + layer_name + ", " + layer_filename);
                    Debug.println(e);
                    con.rollback();
                }
            }
        } catch (Exception e) {
            Debug.println(e);
        } finally {
            try {
                select_layer_stmt.close();
            } catch (Exception e) {
            }
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                insert_property_stmt.close();
            } catch (Exception e) {
            }
            try {
                delete_property_stmt.close();
            } catch (Exception e) {
            }
            try {
                update_layer_stmt.close();
            } catch (Exception e) {
            }
        }
    }

    static String fixUNC(String filename) {
        return filename;
    }

    static final int TIMESERIES_NONE = -1;

    static final int TIMESERIES_YEAR = 0;

    static final int TIMESERIES_MONTH = 1;

    static final int TIMESERIES_DEKAD = 2;

    static final int TIMESERIES_BIMONTH = 3;

    static final int TIMESERIES_DAY = 4;

    public static int getTimeseries(String time_period) {
        int timeseries = TIMESERIES_NONE;
        if (time_period != null && time_period.length() != 0) {
            if (time_period.equalsIgnoreCase("year")) {
                timeseries = TIMESERIES_YEAR;
            } else if (time_period.equalsIgnoreCase("month")) {
                timeseries = TIMESERIES_MONTH;
            } else if (time_period.equalsIgnoreCase("dekad")) {
                timeseries = TIMESERIES_DEKAD;
            } else if (time_period.equalsIgnoreCase("bimonth")) {
                timeseries = TIMESERIES_BIMONTH;
            } else if (time_period.equalsIgnoreCase("day")) {
                timeseries = TIMESERIES_DAY;
            }
        }
        return timeseries;
    }

    public static int getDekad(Calendar time) {
        int dekad = 1;
        int day = time.get(Calendar.DAY_OF_MONTH);
        if (day > 20) {
            dekad = 3;
        } else if (day > 10) {
            dekad = 2;
        }
        return dekad;
    }

    public static int getBimonth(Calendar time) {
        int bimonth = 1;
        int day = time.get(Calendar.DAY_OF_MONTH);
        if (day > 15) {
            bimonth = 2;
        }
        return bimonth;
    }

    public static boolean add(Calendar time, Calendar start, Calendar end, String time_period, int amount) throws SQLException {
        int timeseries = getTimeseries(time_period);
        return add(time, start, end, timeseries, amount);
    }

    public static boolean add(Calendar time, Calendar start, Calendar end, int timeseries, int amount) throws SQLException {
        boolean modified = false;
        switch(timeseries) {
            case TIMESERIES_YEAR:
                time.add(Calendar.YEAR, amount);
                modified = true;
                break;
            case TIMESERIES_MONTH:
                time.add(Calendar.MONTH, amount);
                modified = true;
                break;
            case TIMESERIES_DEKAD:
                int dekad = getDekad(time);
                dekad += amount;
                switch(dekad) {
                    case 0:
                        time.set(Calendar.DAY_OF_MONTH, 25);
                        time.add(Calendar.MONTH, -1);
                        break;
                    case 1:
                        time.set(Calendar.DAY_OF_MONTH, 5);
                        break;
                    case 2:
                        time.set(Calendar.DAY_OF_MONTH, 15);
                        break;
                    case 3:
                        time.set(Calendar.DAY_OF_MONTH, 25);
                        break;
                    case 4:
                        time.set(Calendar.DAY_OF_MONTH, 5);
                        time.add(Calendar.MONTH, 1);
                        break;
                }
                modified = true;
                break;
            case TIMESERIES_BIMONTH:
                int bimonth = getDekad(time);
                bimonth += amount;
                switch(bimonth) {
                    case 0:
                        time.set(Calendar.DAY_OF_MONTH, 20);
                        time.add(Calendar.MONTH, -1);
                        break;
                    case 1:
                        time.set(Calendar.DAY_OF_MONTH, 10);
                        break;
                    case 2:
                        time.set(Calendar.DAY_OF_MONTH, 20);
                        break;
                    case 3:
                        time.set(Calendar.DAY_OF_MONTH, 10);
                        time.add(Calendar.MONTH, 1);
                        break;
                }
                modified = true;
                break;
            case TIMESERIES_DAY:
                time.add(Calendar.DAY_OF_MONTH, amount);
                modified = true;
                break;
        }
        return modified;
    }

    public static String expand(Calendar cal, String pattern) throws SQLException {
        final char QUOTE = '\'';
        final String dekad_symbol = "%Dekad%";
        final String bimonth_symbol = "%Bimonth%";
        final String month_symbol = "%Month%";
        final String year_symbol = "%Year%";
        final String year2_symbol = "%Year2%";
        String bimonth = Integer.toString(getBimonth(cal));
        String dekad = Integer.toString(getDekad(cal));
        String month = "%MM%";
        String year = "%yyyy%";
        String year2 = "%yy%";
        final int SYMBOL = 0;
        final int VALUE = 1;
        String[][] replacements = { { bimonth_symbol, bimonth }, { dekad_symbol, dekad }, { month_symbol, month }, { year_symbol, year }, { year2_symbol, year2 } };
        StringBuffer expand = new StringBuffer(pattern);
        for (int r = 0; r < replacements.length; r++) {
            String symbol = replacements[r][SYMBOL];
            String value = replacements[r][VALUE];
            while (true) {
                int index = expand.indexOf(symbol);
                if (index != -1) {
                    expand = expand.replace(index, index + symbol.length(), value);
                } else {
                    break;
                }
            }
        }
        expand = new StringBuffer(expand.toString().replace('%', QUOTE));
        if (expand.charAt(0) != QUOTE) {
            expand.insert(0, QUOTE);
        }
        if (expand.charAt(expand.length() - 1) != QUOTE) {
            expand.append(QUOTE);
        }
        while (true) {
            String QUOTE_QUOTE = "''";
            int start = expand.indexOf(QUOTE_QUOTE);
            if (start == -1) {
                break;
            } else {
                expand.delete(start, start + QUOTE_QUOTE.length());
            }
        }
        SimpleDateFormat date_format = new SimpleDateFormat(expand.toString());
        String value = date_format.format(cal.getTime());
        return value;
    }

    String database_ini = null;

    Connection con = null;

    public ImageCropper(String database_ini, Calendar from_date, Calendar to_date, String area_code, int output_maximum_size, String output_filepath) {
        this.database_ini = database_ini;
        con = dbConnectionManagerPool.getConnectionManager(database_ini).popConnection();
    }

    public void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    public void dispose() {
        if (con != null) {
            try {
                con.commit();
            } catch (Exception e) {
            }
            dbConnectionManagerPool.getConnectionManager(database_ini).pushConnection(con);
        }
    }

    /**
     *    This method  returns the complete file path of the image to be saved
     *
     * @param        String   category of raster layer
     * @param        String   time series
     * @param        String   file name of the image
     *
     * @return       String   the complete file path of the raster image
     *
     * @author       macdc    01072004
     */
    public static String formatPath(String category, String time_series, String output_filename) {
        StringTokenizer st = null;
        String os = System.getProperties().get("os.name").toString();
        if (os.equalsIgnoreCase("Linux")) {
            st = new StringTokenizer(output_filename, "//");
        } else {
            st = new StringTokenizer(output_filename, "\\");
        }
        int ctr = st.countTokens();
        String image_filename = "";
        String gif_folder = "";
        String path_name = "";
        for (int i = 0; i < ctr; i++) {
            if (i == (ctr - 1)) {
                image_filename = st.nextToken();
            } else {
                path_name = path_name + st.nextToken() + File.separator;
            }
        }
        if (!category.equalsIgnoreCase("DEM")) {
            gif_folder = createGIFFolder(category, time_series, image_filename);
        } else {
            gif_folder = DEM;
        }
        gif_name = gif_folder;
        new_filename = path_name + gif_folder;
        path_name = path_name + gif_folder + File.separator + image_filename;
        return path_name;
    }

    /**
     *    This method  create folder name for the image to be saved
     *
     * @param        String   category of raster layer
     * @param        String   time series
     * @param        String   file name of the image
     *
     * @return       String   the formatted name of the image
     *
     * @author       macdc    01072004
     */
    public static String createGIFFolder(String category, String time_series, String image_filename) {
        String gif_folder = "";
        final String DEK = "dekad";
        final String MONTH = "monthly";
        String months[] = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
        int ext_index = image_filename.indexOf(".");
        String period = "";
        String str_date = "";
        int mo = 0;
        String year = "";
        if (time_series.equals("Dekad")) {
            str_date = image_filename.substring(ext_index - 7, ext_index);
            year = str_date.substring(0, 4);
            Debug.println("year: " + year);
            mo = Integer.parseInt(str_date.substring(4, 6));
            period = str_date.substring(6);
            gif_folder = months[mo - 1] + " " + year + " " + DEK + " " + period;
        } else if (time_series.equals("Month")) {
            str_date = image_filename.substring(ext_index - 6, ext_index);
            year = str_date.substring(0, 4);
            Debug.println("mo year: " + year);
            mo = Integer.parseInt(str_date.substring(4, 6));
            gif_folder = months[mo - 1] + " " + year + " " + MONTH;
        }
        StringTokenizer st = new StringTokenizer(image_filename, "_");
        int ctr = st.countTokens();
        String diff_code = "";
        if (ctr > 2) {
            for (int i = 0; i < ctr; i++) {
                if (i == 1) {
                    diff_code = st.nextToken();
                    if (diff_code.equalsIgnoreCase("D")) {
                        gif_folder = gif_folder + " DP";
                    } else {
                        gif_folder = gif_folder + " " + diff_code;
                    }
                } else {
                    Debug.println(st.nextToken());
                }
            }
        }
        return gif_folder;
    }

    /**
     *  This method checks the current OS, if linux, returns the appropriate
     *       path.
     *
     * @param     String   the path of the file
     *
     * @return    String   the appropriate path of the file
     *
     * @author    macdc    july 27, 2004
     */
    private static String getLinuxPathEquivalent(String file_path) {
        String os = System.getProperties().get("os.name").toString();
        String linux_filename = "";
        if (os.equalsIgnoreCase("Linux")) {
            if (file_path != null) {
                StringTokenizer st = new StringTokenizer(file_path, "\\");
                int ctr = st.countTokens();
                for (int i = 0; i < ctr; i++) {
                    if (i == 0) {
                        linux_filename = File.separator + st.nextToken();
                    } else {
                        linux_filename = linux_filename + File.separator + st.nextToken();
                    }
                }
            } else {
                linux_filename = file_path;
            }
        } else {
            linux_filename = file_path;
        }
        return linux_filename;
    }

    /**
     *  This method returns a list of the products associated with the country
     *       currently on process.
     *
     * @param        Connection   DB connection
     * @param        String       country code
     *
     * @return       String       product codes formatted for sql query
     *                            *** 'CCD', 'SPOT', 'IERF' ***
     *
     * @author       macdc        August 06, 2004
     */
    private static String getCountryProducts(Connection con, String country) throws SQLException {
        String products = "";
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String query = "select product from raster_country_products " + "where country_code=? and generate='T'";
        try {
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, country);
            rs = pstmt.executeQuery();
            int ctr = 0;
            while (rs.next()) {
                if (ctr == 0) {
                    products = "'" + rs.getString(1) + "'";
                } else {
                    products = products + "," + "'" + rs.getString(1) + "'";
                }
                ctr++;
            }
        } catch (SQLException e) {
            String error = "ImageCropper.getCountryProducts SQLException";
            Debug.println(error + e.getMessage());
            throw new SQLException(error + e);
        } finally {
            rs.close();
            pstmt.close();
        }
        return products;
    }

    /**
     *  Get Connection from the connection pool
     *
     * @return    Connection
     *
     * @author    macdc
     */
    private Connection popConnection(String database_ini) {
        Debug.println("ImageCropper.popConnection BEGIN");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        if (con == null) {
            Debug.println("ImageCropper.popConnection Con is NULL!");
        }
        Debug.println("ImageCropper.popConnection END");
        return con;
    }

    /**
     *  Push back the Connection into the connection pool
     *
     *@param      Connection
     *
     * @author    macdc
     */
    private void pushConnection(Connection con) {
        Debug.println("ImageCropper.pushConnection BEGIN");
        if (database_ini != null && con != null) {
            dbConnectionManagerPool.getConnectionManager(database_ini).pushConnection(con);
        } else {
            Debug.println("ImageCropper.pushConnection : null values!");
        }
        Debug.println("ImageCropper.pushConnection END");
    }

    static class DifferenceImageOperator implements RasterLayer.PixelOperator {

        public int compute(int x, int y, int pix1, int pix2) {
            int res = 255;
            try {
                if (pix1 == -1 || pix2 == -1) {
                    res = 255;
                } else {
                    res = (((pix1 & 0xFF) - (pix2 & 0xFF) + 256) >> 1);
                }
            } catch (ArithmeticException ae) {
                Debug.println("Arith Exception: x = " + x + "y =" + y + "pix1 =" + pix1 + "pix2 =" + pix2);
                ae.printStackTrace(System.out);
            } catch (Exception e) {
                Debug.println("compute e: " + e.getMessage());
                e.printStackTrace(System.out);
            }
            return res;
        }

        public int compute(int x, int y, byte pix1, byte pix2) {
            int res = 0;
            try {
                if (pix1 > 0xFD || pix2 > 0xFD) {
                    res = 0xFFFF;
                }
                res = (((pix1 & 0xFF) - (pix2 & 0xFF)) >> 1) & 0xffff;
            } catch (ArithmeticException ae) {
                Debug.println("Arith Exception: x = " + x + "y =" + y + "pix1 =" + pix1 + "pix2 =" + pix2);
            } catch (Exception e) {
                Debug.println("compute int e: " + e.getMessage());
            }
            return res;
        }
    }

    public static void setImageWindow(String img_window) {
        image_window = img_window;
    }

    public static void setContextPath(String path) {
        context_path = path;
    }
}
