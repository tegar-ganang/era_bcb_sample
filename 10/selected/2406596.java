package org.fao.waicent.kids.giews;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
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
import java.util.StringTokenizer;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import org.fao.waicent.attributes.DataLegendDefinition;
import org.fao.waicent.attributes.SourceDefinition;
import org.fao.waicent.db.dbConnectionManager;
import org.fao.waicent.db.dbConnectionManagerPool;
import org.fao.waicent.kids.Configuration;
import org.fao.waicent.util.Debug;
import org.fao.waicent.util.FileResource;
import org.fao.waicent.util.ImageCreator;
import org.fao.waicent.util.XOutline;
import org.fao.waicent.util.XPatternOutline;
import org.fao.waicent.util.XPatternPaint;
import org.fao.waicent.xmap2D.FeatureLayer;
import org.fao.waicent.xmap2D.Map;
import org.fao.waicent.xmap2D.MapContext;
import org.fao.waicent.xmap2D.RasterData;
import org.fao.waicent.xmap2D.RasterLayer;
import org.fao.waicent.xmap2D.coordsys.ProjectionCategories;
import org.fao.waicent.xmap2D.util.ParamList;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;

public class TIFFImageCropper {

    private static String new_filename;

    private static String tiff_name;

    private static final String DEM = "Digital Elevation Model";

    private static String image_window;

    private static String context_path;

    private static float area_x;

    private static float area_y;

    private static float area_width;

    private static float area_height;

    public TIFFImageCropper(String database_ini) {
        this.database_ini = database_ini;
    }

    public void generateTIFFAll(String output_filepath, Calendar from_date, Calendar to_date, String[] countries) throws Exception {
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
        String SQL = "select a.Proj_Code, a.Proj_Name, b.ProjCoordSys_Xmin, " + "b.ProjCoordSys_Ymin, b.ProjCoordSys_Area_Xmax, " + "b.ProjCoordSys_Area_Ymax " + "from project a, projectcoordsystem b " + "where a.Proj_ID = b.Proj_ID and a.Proj_Code in (" + country_codes + ")";
        Connection con = popConnection(database_ini);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                String area_code = rs.getString(1);
                String area_label = rs.getString(2);
                area_x = (float) rs.getDouble(3);
                area_y = (float) rs.getDouble(4);
                area_width = (float) rs.getDouble(5);
                area_height = (float) rs.getDouble(6);
                Rectangle2D area_bounds = new Rectangle2D.Double(rs.getDouble(3), rs.getDouble(4), rs.getDouble(5), rs.getDouble(6));
                String area_output_filepath = output_filepath + File.separator + area_code;
                new File(area_output_filepath).mkdirs();
                generateTIFFforArea(con, from_date, to_date, area_code, area_label, area_bounds, output_maximum_size, area_output_filepath);
            }
            con.commit();
        } catch (Exception e) {
            Debug.println("ImageCropper.generateTIFFAll e: " + e.getMessage());
            con.rollback();
            throw new Exception("An exception thrown in generateTIFFAll():" + e);
        } finally {
            try {
                rs.close();
                stmt.close();
                pushConnection(con);
            } catch (Exception e) {
                Debug.println("Exception in closing resources!!!");
                throw new Exception("Exception in generateTIFFAll: " + e);
            }
        }
    }

    public static void generateTIFFforArea(Connection con, Calendar from_date, Calendar to_date, String area_code, String area_label, Rectangle2D area_bounds, int output_maximum_size, String output_filepath) throws Exception {
        Debug.println("ImageCropper.generateTIFFforArea begin");
        String products = getCountryProducts(con, area_code);
        Statement stmt = null;
        ResultSet rs = null;
        String SQL = "select Raster_Category, Raster_Label, Raster_TimeSeries, " + "Raster_Image_File_Pattern, Raster_Legend_File, " + "Raster_Diff_Legend_File, Raster_X, Raster_Y, " + "Raster_Width, Raster_Height, Raster_Generate, " + "Raster_AVG_Image_File_Pattern " + "from rastergeneration " + "where Raster_X is not null and Raster_Category in (" + products + ") and Raster_Generate = 'T' " + "order by Raster_Category, Raster_Timeseries";
        Debug.println("generateTIFFforArea SQL: " + SQL);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(SQL);
            Rectangle2D area_bounds_ref = new Rectangle2D.Double(area_bounds.getX(), area_bounds.getY(), area_bounds.getWidth(), area_bounds.getHeight());
            while (rs.next()) {
                String category = rs.getString(1);
                String raster_label = rs.getString(2);
                String timeseries = rs.getString(3);
                int time_period = getTimeseries(timeseries);
                if (timeseries != null && timeseries.length() > 0) {
                    raster_label += "[" + timeseries + "]";
                }
                Rectangle2D raster_bounds = new Rectangle2D.Double(rs.getDouble(7), rs.getDouble(8), rs.getDouble(9), rs.getDouble(10));
                String source_pattern = rs.getString(4);
                String legend_filename = rs.getString(5);
                String diff_legend_filename = rs.getString(6);
                String avg_pattern = rs.getString(12);
                if (raster_bounds.contains(area_bounds_ref)) {
                    String image_label = area_label + " " + raster_label;
                    String topic_code = "";
                    String note = "";
                    if (time_period == TIMESERIES_NONE) {
                        String output_filename = output_filepath + File.separator + category + ".tiff";
                        area_bounds.setRect(area_bounds_ref);
                        generateTIFF(con, category, area_code, topic_code, timeseries, null, null, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_pattern, null, legend_filename, output_filename, output_maximum_size);
                    } else {
                        Calendar time = new GregorianCalendar();
                        time.set(2001, 12, 1);
                        Calendar start = from_date;
                        Calendar end = to_date;
                        while (!time.after(end)) {
                            if (time.after(start) && time.before(end)) {
                                String source_filename = expand(time, source_pattern);
                                String avg_filename = expand(time, avg_pattern);
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
                                    String output_pattern = getTIFFOutputPattern(output_filepath, category, time_period, diff_period, false);
                                    String output_filename = expand(time, output_pattern);
                                    area_bounds.setRect(area_bounds_ref);
                                    generateTIFF(con, category, area_code, topic_code, timeseries, diff_timeseries, time, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_filename, diff_filename, use_legend_filename, output_filename, output_maximum_size);
                                }
                                if (avg_filename != null) {
                                    use_legend_filename = diff_legend_filename;
                                    String output_pattern = getTIFFOutputPattern(output_filepath, category, time_period, 2, true);
                                    String output_filename = expand(time, output_pattern);
                                    area_bounds.setRect(area_bounds_ref);
                                    generateTIFF(con, category, area_code, topic_code, timeseries, timeseries, time, area_label, raster_label, image_label, note, area_bounds, raster_bounds, source_filename, avg_filename, use_legend_filename, output_filename, output_maximum_size);
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
            Debug.println("Exception generateTIFFforArea e: " + e.getMessage());
            con.rollback();
            throw new Exception("Exception generateTIFFforArea: " + e);
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
        Debug.println("ImageCropper.generateTIFFforArea end");
    }

    private static void generateTIFF(Connection con, String category, String area_code, String topic_code, String timeseries, String diff_timeseries, Calendar time, String area_label, String raster_label, String image_label, String note, Rectangle2D bounds, Rectangle2D raster_bounds, String source_filename, String diff_filename, String legend_filename, String output_filename, int output_maximum_size) throws SQLException, IOException {
        Debug.println("ImageCropper.generateTIFF begin");
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
            String tiff_filename = "";
            try {
                tiff_filename = formatPath(category, timeseries, output_filename);
                new File(new_filename).mkdirs();
                Debug.println("tiff_filename: " + tiff_filename);
                BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_BYTE_INDEXED);
                bi.createGraphics().drawImage(image, 0, 0, null);
                File f = new File(tiff_filename);
                FileOutputStream out = new FileOutputStream(f);
                TIFFEncodeParam param = new TIFFEncodeParam();
                param.setCompression(TIFFEncodeParam.COMPRESSION_PACKBITS);
                TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec.createImageEncoder("tiff", out, param);
                encoder.encode(bi);
                out.close();
            } catch (IOException e) {
                Debug.println("ImageCropper.generateTIFF TIFFCodec e: " + e.getMessage());
                throw new IOException("GenerateTIFF.IOException: " + e);
            }
            PreparedStatement pstmt = null;
            try {
                String query = "select Proj_ID, AccessType_Code from project " + "where Proj_Code= '" + area_code.trim() + "'";
                Statement stmt = null;
                ResultSet rs = null;
                int proj_id = -1;
                int access_code = -1;
                stmt = con.createStatement();
                rs = stmt.executeQuery(query);
                if (rs.next()) {
                    proj_id = rs.getInt(1);
                    access_code = rs.getInt(2);
                }
                rs.close();
                stmt.close();
                String delete_raster = "delete from rasterlayer where " + "Raster_Name='" + tiff_name.trim() + "' and Group_Code='" + category.trim() + "' and Proj_ID =" + proj_id;
                Debug.println("***** delete_raster: " + delete_raster);
                pstmt = con.prepareStatement(delete_raster);
                boolean del = pstmt.execute();
                pstmt.close();
                String insert_raster = "insert into rasterlayer(Raster_Name, " + "Group_Code, Proj_ID, Raster_TimeCode, Raster_Xmin, " + "Raster_Ymin, Raster_Area_Xmin, Raster_Area_Ymin, " + "Raster_Visibility, Raster_Order, Raster_Path, " + "AccessType_Code, Raster_TimePeriod) values(?,?,?,?, " + "?,?,?,?,?,?,?,?,?)";
                pstmt = con.prepareStatement(insert_raster);
                pstmt.setString(1, tiff_name);
                pstmt.setString(2, category);
                pstmt.setInt(3, proj_id);
                pstmt.setString(4, timeseries);
                pstmt.setDouble(5, raster_bounds.getX());
                pstmt.setDouble(6, raster_bounds.getY());
                pstmt.setDouble(7, raster_bounds.getWidth());
                pstmt.setDouble(8, raster_bounds.getHeight());
                pstmt.setString(9, "false");
                int sequence = 0;
                if (tiff_name.endsWith("DP")) {
                    sequence = 1;
                } else if (tiff_name.endsWith("DY")) {
                    sequence = 2;
                } else if (tiff_name.endsWith("DA")) {
                    sequence = 3;
                }
                pstmt.setInt(10, sequence);
                pstmt.setString(11, tiff_filename);
                pstmt.setInt(12, access_code);
                if (time == null) {
                    pstmt.setNull(13, java.sql.Types.DATE);
                } else {
                    pstmt.setDate(13, new java.sql.Date(time.getTimeInMillis()));
                }
                pstmt.executeUpdate();
            } catch (SQLException e) {
                Debug.println("SQLException occurred e: " + e.getMessage());
                con.rollback();
                throw new SQLException("GenerateTIFF.SQLException: " + e);
            } finally {
                pstmt.close();
            }
        } catch (Exception e) {
            Debug.println("ImageCropper.generateTIFF e: " + e.getMessage());
        }
        Debug.println("ImageCropper.generateTIFF end");
    }

    public static RasterLayer getRasterLayer(Map map, String name, String file, String diff_file, String type, String legend_file) {
        Debug.println("***** getRasterLayer START *****");
        String cntxt_legend_file = context_path + legend_file;
        String cntxt_diff_file = context_path + diff_file;
        RasterLayer rlayer = null;
        ParamList raster_params = new ParamList();
        try {
            raster_params.put("LayerName", name);
            raster_params.put("RasterFileResource", new FileResource(file));
            raster_params.put("Legend", new DataLegendDefinition(new FileResource(cntxt_legend_file), true));
            byte raster_type = RasterData.parseRasterType(type);
            if (raster_type == -1) {
                raster_type = RasterData.TIFF_RASTER_TYPE;
            }
            Debug.println("***** getRasterLayer raster_type: *****" + raster_type);
            raster_params.put("RasterType", raster_type);
            raster_params.put("BOUNDS", map.getBounds2D());
            if (diff_file != null) {
                raster_params.put("RasterOperand2FileResource", new FileResource(diff_file));
                DifferenceImageOperator diff_op = new DifferenceImageOperator();
                raster_params.put("PixelOperator", diff_op);
            }
            rlayer = new RasterLayer(raster_params);
        } catch (IOException e) {
            Debug.println("getRasterLayer exception e: " + e.getMessage());
        }
        Debug.println("***** getRasterLayer END *****");
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
            Debug.println("flayer");
            bounds_ll = flayer.getBounds2D();
            Debug.println("layer.getBounds2D() from =" + flayer.getName() + ":" + bounds_ll);
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

    public static String getTIFFOutputPattern(String output_filepath, String category, int time_period, int diff_period, boolean average_image) {
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
        output_pattern.append(".tif");
        Debug.println("output_pattern.toString(): " + output_pattern.toString());
        return output_pattern.toString();
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
        Debug.println("******* here in expand TIFFImageCropper value: " + value);
        return value;
    }

    String database_ini = null;

    Connection con = null;

    public TIFFImageCropper(String database_ini, Calendar from_date, Calendar to_date, String area_code, int output_maximum_size, String output_filepath) {
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

    public static String formatPath(String category, String time_series, String output_filename) {
        Debug.println("TIFFImageCropper.formatPath START");
        StringTokenizer st = null;
        String os = System.getProperties().get("os.name").toString();
        if (os.equalsIgnoreCase("Linux")) {
            st = new StringTokenizer(output_filename, "//");
        } else {
            st = new StringTokenizer(output_filename, "\\");
        }
        int ctr = st.countTokens();
        String image_filename = "";
        String tiff_folder = "";
        String path_name = "";
        for (int i = 0; i < ctr; i++) {
            if (i == (ctr - 1)) {
                image_filename = st.nextToken();
            } else {
                path_name = path_name + st.nextToken() + File.separator;
            }
        }
        Debug.println("TIFFImageCropper.formatPath: " + path_name);
        if (!category.equalsIgnoreCase("DEM_TIFF")) {
            tiff_folder = createTIFFFolder(category, time_series, image_filename);
        } else {
            tiff_folder = DEM;
        }
        tiff_name = tiff_folder;
        new_filename = path_name + tiff_folder;
        path_name = path_name + tiff_folder + File.separator + image_filename;
        Debug.println("**** format_path   :  new_filename: " + new_filename);
        Debug.println("**** format_path   :  path_name: " + path_name);
        Debug.println("TIFFImageCropper.formatPath END");
        return path_name;
    }

    public static String createTIFFFolder(String category, String time_series, String image_filename) {
        String tiff_folder = "";
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
            mo = Integer.parseInt(str_date.substring(4, 6));
            period = str_date.substring(6);
            tiff_folder = months[mo - 1] + " " + year + " " + DEK + " " + period;
        } else if (time_series.equals("Month")) {
            str_date = image_filename.substring(ext_index - 6, ext_index);
            year = str_date.substring(0, 4);
            mo = Integer.parseInt(str_date.substring(4, 6));
            tiff_folder = months[mo - 1] + " " + year + " " + MONTH;
        }
        StringTokenizer st = new StringTokenizer(image_filename, "_");
        int ctr = st.countTokens();
        String diff_code = "";
        if (ctr > 2) {
            for (int i = 0; i < ctr; i++) {
                if (i == 1) {
                    diff_code = st.nextToken();
                    if (diff_code.equalsIgnoreCase("D")) {
                        tiff_folder = tiff_folder + " DP";
                    } else {
                        tiff_folder = tiff_folder + " " + diff_code;
                    }
                } else {
                    Debug.println(st.nextToken());
                }
            }
        }
        return tiff_folder;
    }

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

    private static String getCountryProducts(Connection con, String country) throws SQLException {
        String products = "";
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String query = "select Raster_Product from rasterproducts " + "where Proj_Code=? and Raster_Generate='T'";
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

    public static PlanarImage cropImage(float x, float y, float width, float height, PlanarImage input) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(input);
        pb.add(x);
        pb.add(y);
        pb.add(width);
        pb.add(height);
        PlanarImage output = JAI.create("crop", pb, null);
        return output;
    }
}
