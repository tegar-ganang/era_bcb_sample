package org.fao.waicent.kids.giews.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fao.waicent.db.dbConnectionManager;
import org.fao.waicent.db.dbConnectionManagerPool;
import org.fao.waicent.kids.editor.UploadUtil;
import org.fao.waicent.util.CSVTableReader;
import org.fao.waicent.util.CSVTableWriter;
import org.fao.waicent.util.DBFTableReader;
import org.fao.waicent.util.Debug;
import org.fao.waicent.util.FileResource;
import com.mysql.jdbc.ResultSet;

public class PopulateFSDB extends HttpServlet implements SingleThreadModel {

    String _temp_dir = "";

    String _filename = "";

    String database_ini = "";

    int precision = -1;

    String unit = "";

    HashMap gaul_map = null;

    public void upload(HttpServletRequest request) {
        _filename = UploadUtil.uploadFile(request, _temp_dir);
        Debug.println("Uploaded file to " + _filename);
    }

    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        System.out.println("PopulateFSDB START");
        _temp_dir = getServletContext().getRealPath(File.separatorChar + "WEB-INF" + File.separatorChar + "temp_datasets");
        upload(req);
        try {
            boolean is_new_dataset = false;
            boolean is_overwrite_dataset = false;
            boolean is_append_dataset = false;
            String selected_layer = req.getParameter("selected_layer");
            String dimension_size = req.getParameter("selected_dataset_table");
            String selected_dataset = req.getParameter("selected_dataset_name");
            String mode = req.getParameter("mode");
            String dataset_label = req.getParameter("dataset_label");
            String selected_project = req.getParameter("selected_project");
            System.out.println("selected_project = " + selected_project);
            if (mode.equals("new")) {
                is_new_dataset = true;
            }
            if (mode.equals("append")) {
                is_append_dataset = true;
            }
            if (mode.equals("overwrite")) {
                is_overwrite_dataset = true;
            }
            if (is_new_dataset) {
            }
            System.out.println("dimension_size " + dimension_size);
            FileResource file_resource1 = new FileResource(_filename);
            Debug.println("File " + _filename);
            CSVTableReader table_reader1 = new CSVTableReader(file_resource1, true);
            String error_message = validateAreaCodes(table_reader1, selected_layer);
            if (!error_message.equals("")) {
                res.getWriter().println("<font color='red'>Please check your CSV file: There is an error in the Area codes, as specified below: </font> ");
                res.getWriter().println("<br/>" + error_message);
                Debug.println("Please check your file: Error in Area codes!!! ");
                return;
            }
            FileResource file_resource = new FileResource(_filename);
            Debug.println("File " + _filename);
            CSVTableReader table_reader2 = new CSVTableReader(file_resource, true);
            error_message = validateValues(table_reader2);
            if (!error_message.equals("")) {
                res.getWriter().println("<font color='red'>Please check your CSV file: There is an error in the values column, as specified below: </font>");
                res.getWriter().println("<br/>" + error_message);
                Debug.println("Please check your file: Error in values!!! ");
                return;
            }
            FileResource file_resource3 = new FileResource(_filename);
            Debug.println("File " + _filename);
            CSVTableReader table_reader3 = new CSVTableReader(file_resource3, true);
            error_message = validatePrecision(table_reader3);
            if (!error_message.equals("")) {
                res.getWriter().println("<font color='red'>Please check your CSV file: There is an error in the precision column, as specified below: </font>");
                res.getWriter().println("<br/>" + error_message);
                Debug.println("Please check your file: Error in precision!!! " + error_message);
                return;
            }
            if (is_overwrite_dataset) {
                deleteDataset(selected_dataset);
            }
            FileResource file_resource4 = new FileResource(_filename);
            CSVTableReader table_reader4 = new CSVTableReader(file_resource4, true);
            String output_filename = _filename + ".fs.csv";
            File output_file = new File(output_filename);
            if (!output_file.exists()) {
                output_file.createNewFile();
            }
            FileWriter filewriter = new FileWriter(output_file);
            PrintWriter printwriter = new PrintWriter(filewriter);
            CSVTableWriter writer = new CSVTableWriter(table_reader4, printwriter, 0);
            filewriter.close();
            FileResource file_resource7 = new FileResource(_filename);
            Debug.println("File " + _filename);
            CSVTableReader table_reader7 = new CSVTableReader(file_resource7, true);
            error_message = validateDimensions(table_reader7);
            if (!error_message.equals("")) {
                res.getWriter().println("<font color='red'>Please check your CSV file: There is an error in the dimensions column(s), as specified below: </font>");
                res.getWriter().println("<br/>" + error_message);
                Debug.println("Please check your file: Error in dimensions column(s)!!! " + error_message);
                return;
            }
            loadDataIntoDatasetTable(selected_dataset, output_filename, writer.getColumnNames());
            insertProjectLayer(selected_project, selected_layer);
            if (is_new_dataset) {
                insertLayerDataset(selected_layer, dataset_label);
            }
            if (!is_new_dataset) res.getWriter().println("Please launch the GIEWS Workstation site to view the updated dataset (see Table/Chart modules)."); else res.getWriter().println("The dataset has been added to the FS database.  Please launch the GIEWS Workstation site to view the updated dataset (see Table/Chart modules).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteDataset(String selected_dataset) throws SQLException {
        Debug.println("deleteDataset START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Set keysset = gaul_map.keySet();
        Iterator keys = keysset.iterator();
        StringBuffer gaul_codes_buff = new StringBuffer();
        while (keys.hasNext()) {
            gaul_codes_buff.append("'" + (String) keys.next() + "'");
            if (keys.hasNext()) gaul_codes_buff.append(",");
        }
        String gaul_codes = gaul_codes_buff.toString();
        System.out.println("keys = " + gaul_codes);
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String code = "";
        String label = "";
        ArrayList list = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        con.setAutoCommit(false);
        String sql = " delete from " + selected_dataset + " where area_code in (" + gaul_codes + ")";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            System.out.println("SQL " + sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" deleteAttributes exception: " + e.getMessage());
            try {
                throw new SQLException(e.getMessage());
            } catch (SQLException e1) {
                e1.printStackTrace();
                return false;
            }
        }
        Debug.println("deleteDataset END\n");
        return true;
    }

    public int generateNewDatasetID(String selected_dataset_name) throws SQLException {
        Debug.println("generateNewDatasetID START");
        int dataset_index = -1;
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        con.setAutoCommit(true);
        String sql = "select max(Dataset_ID)+1 from dataset";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            ResultSet rs = (ResultSet) pstmt.executeQuery();
            rs.next();
            dataset_index = rs.getInt(1);
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" generateNewDatasetID exception: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
        Debug.println("generateNewDatasetID END\n");
        return dataset_index;
    }

    public boolean createDataset(String selected_dataset, String size, String selected_dataset_name) throws SQLException {
        Debug.println("createDataset START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        con.setAutoCommit(true);
        String dimensions = "" + size + "dimensions";
        String DATA_TABLE_NAME = "data_" + dimensions;
        String sql = "insert into dataset(Dataset_ID, Dataset_DataTableName, " + "Dataset_DataTableType, Dataset_Name, " + "XMLData_ID_Navigator, XMLData_ID_Indicator, Dataset_LastUpdated, Metadata_ID) " + "values(?,?,?,?,?,?,NOW(),?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(selected_dataset));
            pstmt.setString(2, DATA_TABLE_NAME);
            pstmt.setInt(3, Integer.parseInt(size));
            pstmt.setString(4, selected_dataset_name);
            pstmt.setInt(5, -1);
            pstmt.setInt(6, -1);
            pstmt.setString(7, "");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" createDataset exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Debug.println("createDataset END\n");
        return true;
    }

    private boolean loadDataxDimensions(String selected_dataset, String size, String selected_dataset_name, String output_file) throws SQLException {
        Debug.println("loadDataxDimensions START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        con.setAutoCommit(true);
        String dimensions = "" + size + "dimensions";
        String DATA_TABLE_NAME = "data_" + dimensions;
        Debug.println("loadDataxDimensions:   before output_file " + output_file);
        output_file = output_file.replaceAll("\\\\", "\\\\\\\\");
        Debug.println("loadDataxDimensions:   output_file " + output_file);
        String sql = "LOAD DATA LOCAL INFILE '" + output_file + "' " + " INTO TABLE " + DATA_TABLE_NAME + " FIELDS TERMINATED BY ',' " + " LINES TERMINATED BY '\\n' " + " (dataset_id, " + " dimension_1, dimension_2, dimension_3, dimension_4, " + " data_value) ";
        Debug.println("\n\n" + sql);
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" createDataset exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Debug.println("loadDataxDimensions END\n");
        return true;
    }

    private boolean insertLayerDataset(String selected_layer, String dataset_label) throws SQLException {
        Debug.println("insertLayerDataset START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        PreparedStatement pstmt = null;
        try {
            String sql = "insert into layer_dataset values(?, ?)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, selected_layer);
            pstmt.setString(2, dataset_label);
            System.out.println(sql + "; selected_layer = " + selected_layer + "; dataset_label = " + dataset_label);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" insertLayerDataset exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Debug.println("insertLayerDataset END\n");
        return true;
    }

    private boolean insertProjectLayer(String selected_project, String selected_layer) throws SQLException {
        Debug.println("insertProjectLayer START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        PreparedStatement pstmt = null;
        try {
            String sql = "select * from project_layer where proj_id = " + selected_project + " and layer_id = " + selected_layer;
            pstmt = con.prepareStatement(sql);
            ResultSet rs = (ResultSet) pstmt.executeQuery();
            System.out.println(sql + "; checked whether project_layer exists.");
            if (rs.next()) {
                rs.close();
                pstmt.close();
            } else {
                sql = "insert into project_layer values(?, ?, ?)";
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, selected_project);
                pstmt.setString(2, selected_layer);
                pstmt.setString(3, "FEATURE");
                System.out.println(sql + "; selected_country = " + selected_project + "; selected_layer = " + selected_layer);
                pstmt.executeUpdate();
                pstmt.close();
            }
        } catch (SQLException e) {
            Debug.println(" insertProjectLayer exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Debug.println("insertProjectLayer END\n");
        return true;
    }

    private String validateColumns(CSVTableReader csv_table_reader, String selected_layer) {
        Debug.println("validateColumns START\n");
        String error_message = "";
        try {
            csv_table_reader.rewind();
            int col_count = csv_table_reader.getColumnCount();
            System.out.println(csv_table_reader.getRowNumber());
            if (csv_table_reader.readRow() != -1) {
                for (int i = 0; i < col_count; i++) {
                    String col_name = csv_table_reader.getValue(i);
                    String correct_col_name = col_name.replace("_code", "_Code");
                    if (col_name.endsWith("_code")) {
                        error_message += " Column # " + (i + 1) + " should be " + correct_col_name;
                    }
                }
            }
        } catch (Exception se) {
            Debug.println(" validateColumns exception: " + se.getMessage());
            se.printStackTrace();
        }
        Debug.println("validateColumns END\n");
        return error_message;
    }

    private String validateAreaCodes(CSVTableReader csv_table_reader, String selected_layer) {
        String error_message = "";
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Debug.println("validateAreaCodes START");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        try {
            String sql = "SELECT feature_path FROM featurelayer where feature_id = " + selected_layer;
            Debug.println(" validateAreaCodes SQL " + sql);
            PreparedStatement pstmt = con.prepareStatement(sql);
            ResultSet rs = (ResultSet) pstmt.executeQuery();
            String feature_path = "";
            if (rs.next()) {
                feature_path = rs.getString(1);
            }
            Debug.println("feature_path = " + feature_path);
            feature_path = getServletContext().getRealPath("" + File.separatorChar) + feature_path;
            feature_path = feature_path.substring(0, feature_path.lastIndexOf("."));
            feature_path = feature_path + ".dbf";
            Debug.println("complete feature_path = " + feature_path);
            String global_path = getServletContext().getRealPath("" + File.separatorChar);
            FileResource fileresource = new FileResource(feature_path, feature_path, global_path);
            Debug.println("fileresource = " + fileresource.getAbsoluteFilename());
            DBFTableReader table_reader;
            table_reader = new DBFTableReader(fileresource);
            int code_column = 0, label_column = 0;
            if (feature_path != null && feature_path.contains("_1.dbf")) {
                code_column = table_reader.getColumnIndex("ADM1_CODE");
                label_column = table_reader.getColumnIndex("ADM1_NAME");
            } else if (feature_path != null && feature_path.contains("_2.dbf")) {
                code_column = table_reader.getColumnIndex("ADM2_CODE");
                label_column = table_reader.getColumnIndex("ADM2_NAME");
            } else if (feature_path != null && feature_path.contains("_3.dbf")) {
                code_column = table_reader.getColumnIndex("ADM3_CODE");
                label_column = table_reader.getColumnIndex("ADM3_NAME");
            } else if (feature_path != null && (feature_path.contains("cities") || feature_path.contains("markets"))) {
                code_column = table_reader.getColumnIndex("CODE");
                label_column = table_reader.getColumnIndex("NAME");
            } else if (feature_path != null && feature_path.contains("countries") || feature_path.contains("_0")) {
                code_column = table_reader.getColumnIndex("ADM0_CODE");
                label_column = table_reader.getColumnIndex("ADM0_NAME");
            }
            System.out.println("code_column = " + code_column + "; label_column = " + label_column);
            try {
                String code = "";
                String label = "";
                gaul_map = new HashMap();
                while (table_reader.readRow() != -1) {
                    try {
                        code = table_reader.getValue(code_column);
                        label = table_reader.getValue(label_column);
                        gaul_map.put(code, label);
                    } catch (Exception e) {
                        System.out.println("<br/>Error in initializing Area Codes hashtable.  Row " + table_reader.getRowNumber());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            int col_count = csv_table_reader.getColumnCount();
            while (csv_table_reader.readRow() != -1) {
                if (!gaul_map.containsKey(csv_table_reader.getValue(0))) {
                    error_message += "<br/>Unrecognized Area Code error at: Row Number = " + csv_table_reader.getRowNumber() + " where Area Code = " + csv_table_reader.getValue(0) + " and Area Name = " + csv_table_reader.getValue(1) + ", please change the area code";
                }
            }
            rs.close();
            pstmt.close();
        } catch (IOException e) {
            Debug.println(" validateAreaCodes exception: " + e.getMessage());
            e.printStackTrace();
            error_message += "Error in validateAreaCodes" + e.getMessage();
        } catch (SQLException se) {
            Debug.println(" validateAreaCodes exception: " + se.getMessage());
            se.printStackTrace();
            error_message += "Error in validateAreaCodes" + se.getMessage();
        }
        Debug.println("validateAreaCodes END\n");
        return error_message;
    }

    private String validateValues(CSVTableReader table_reader) {
        String error_message = "";
        Debug.println("validateValues START");
        int value_col = table_reader.getColumnIndex("Value");
        System.out.println("value col = " + value_col);
        try {
            table_reader.rewind();
            while (table_reader.readRow() != -1) {
                String value = "";
                try {
                    value = table_reader.getValue(value_col);
                    Double.parseDouble(value);
                } catch (Exception e) {
                    error_message += "<br/>Non-Integer Data Value error at: Row Number = " + table_reader.getRowNumber() + " where Data Value = " + value + ", please change this value to an integer number";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Debug.println("validateValues END");
        return error_message;
    }

    private String validatePrecision(CSVTableReader table_reader) {
        Debug.println("validatePrecision START");
        String error_message = "";
        int precision_col = table_reader.getColumnIndex("Precision_Value");
        String str_precision = "";
        try {
            table_reader.rewind();
            if (table_reader.readRow() != -1) {
                try {
                    str_precision = table_reader.getValue(precision_col);
                    precision = Integer.parseInt(str_precision);
                } catch (Exception e) {
                    error_message += "<br/>Non-Integer Precision Value error at: Row Number = " + table_reader.getRowNumber() + " where precision = " + str_precision + ", please change the precision to an integer number";
                }
            }
        } catch (IOException e) {
            precision = -1;
            e.printStackTrace();
        }
        Debug.println("precision = " + precision + " Row " + table_reader.getRowNumber());
        Debug.println("validatePrecision END\n");
        return error_message;
    }

    private boolean loadDataIntoDatasetTable(String selected_dataset, String output_file, ArrayList column_names) throws SQLException {
        Debug.println("loadDataIntoDatasetTable START");
        database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        con.setAutoCommit(true);
        Debug.println("loadDataxDimensions:   before output_file " + output_file);
        output_file = output_file.replaceAll("\\\\", "\\\\\\\\");
        Debug.println("loadDataxDimensions:   output_file " + output_file);
        String sql = "LOAD DATA LOCAL INFILE '" + output_file + "' " + " INTO TABLE " + selected_dataset + " FIELDS TERMINATED BY ',' " + " LINES TERMINATED BY '\\n' (";
        for (int i = 0; i < column_names.size(); i++) {
            sql += (String) column_names.get(i);
            if (i < column_names.size() - 1) sql += ", ";
        }
        sql += ") ";
        Debug.println("\n\n" + sql);
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            Debug.println(" loadDataIntoDatasetTable exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Debug.println("loadDataIntoDatasetTable END\n");
        return true;
    }

    private String validateDimensions(CSVTableReader table_reader) {
        Debug.println("validateDimensions START");
        String error_message = "";
        int cols = table_reader.getColumnCount();
        String column_name = "";
        Connection con = null;
        dbConnectionManager manager = dbConnectionManagerPool.getConnectionManager(database_ini);
        con = manager.popConnection();
        try {
            table_reader.rewind();
            Debug.println("done validateDimensions rewinding. cols = " + cols + "; row = " + table_reader.readRow());
            while (table_reader.readRow() != -1) {
                Debug.println("----------------------- Row " + table_reader.readRow() + " -----------------------");
                for (int i = 0; i < cols; i++) {
                    column_name = table_reader.getColumn(i);
                    if (column_name.endsWith("_Code") && (!column_name.equalsIgnoreCase("Area_Code") && !column_name.equalsIgnoreCase("Period_Type_Code") && !column_name.equalsIgnoreCase("Source_Code") && !column_name.equalsIgnoreCase("Data_Admin_Code") && !column_name.equalsIgnoreCase("Residence_Code") && !column_name.equalsIgnoreCase("Country_Code") && !column_name.equalsIgnoreCase("AccessType_Code"))) {
                        String dimension_table_name = column_name.substring(0, column_name.indexOf("_Code")).toLowerCase();
                        String dimension_code = table_reader.getValue(i);
                        i++;
                        String label_column_name = table_reader.getColumn(i);
                        String dim_name2 = "";
                        if ((label_column_name.indexOf("_lbl") > 0)) dim_name2 = label_column_name.substring(0, label_column_name.indexOf("_lbl"));
                        if ((label_column_name.indexOf("_label") > 0)) dim_name2 = label_column_name.substring(0, label_column_name.indexOf("_label"));
                        if (!dim_name2.equals("")) {
                            if (dimension_table_name.equalsIgnoreCase(dim_name2)) {
                                String dimension_label = table_reader.getValue(i);
                                if (dimension_table_name.equals("age_range")) dimension_table_name = "agerange";
                                database_ini = getServletContext().getRealPath("/WEB-INF/giews.ini");
                                String sql = "select " + column_name + " from " + dimension_table_name + " where " + column_name + " = " + dimension_code;
                                Debug.println(sql);
                                PreparedStatement pstmt = null;
                                try {
                                    pstmt = con.prepareStatement(sql);
                                    ResultSet rs = (ResultSet) pstmt.executeQuery();
                                    if (rs.next()) {
                                        Debug.println(dimension_code + " already exists in " + dimension_table_name + "!!!!");
                                        rs.close();
                                        pstmt.close();
                                    } else {
                                        rs.close();
                                        pstmt.close();
                                        Debug.println("Inserting to " + dimension_code + " to " + dimension_table_name + "....");
                                        String table_column_names = "";
                                        String question_marks = "";
                                        if (dimension_table_name.equalsIgnoreCase("commodity")) {
                                            table_column_names += " (Commodity_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " Commodity_Label, ";
                                            question_marks += "?, ";
                                            table_column_names += " HS02_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " HS02_Label, ";
                                            question_marks += "?, ";
                                            table_column_names += " Code_Type) ";
                                            question_marks += "?";
                                            sql = "insert into " + dimension_table_name + table_column_names + " values(" + question_marks + ")";
                                            pstmt = null;
                                            pstmt = con.prepareStatement(sql);
                                            pstmt.setString(1, dimension_code);
                                            pstmt.setString(2, dimension_label);
                                            pstmt.setString(3, "");
                                            pstmt.setString(4, "");
                                            pstmt.setString(5, "");
                                        }
                                        if (dimension_table_name.equalsIgnoreCase("indicator")) {
                                            table_column_names += " (Indicator_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " Indicator_Label, ";
                                            question_marks += "?, ";
                                            table_column_names += " Indicator_Description, ";
                                            question_marks += "?, ";
                                            table_column_names += " Note, ";
                                            question_marks += "?, ";
                                            table_column_names += " Theme) ";
                                            question_marks += "? ";
                                            sql = "insert into " + dimension_table_name + table_column_names + " values(" + question_marks + ")";
                                            pstmt = null;
                                            pstmt = con.prepareStatement(sql);
                                            pstmt.setString(1, dimension_code);
                                            pstmt.setString(2, dimension_label);
                                            pstmt.setString(3, "");
                                            pstmt.setString(4, "");
                                            pstmt.setString(5, "");
                                        }
                                        if (dimension_table_name.equalsIgnoreCase("measurement_unit")) {
                                            table_column_names += " (Measurement_Unit_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " Measurement_Unit_Label, ";
                                            question_marks += "?, ";
                                            table_column_names += " Measurement_Unit_Description) ";
                                            question_marks += "? ";
                                            sql = "insert into " + dimension_table_name + table_column_names + " values(" + question_marks + ")";
                                            pstmt = null;
                                            pstmt = con.prepareStatement(sql);
                                            pstmt.setString(1, dimension_code);
                                            pstmt.setString(2, dimension_label);
                                            pstmt.setString(3, "");
                                        }
                                        if (dimension_table_name.equalsIgnoreCase("agerange")) {
                                            table_column_names += " (Age_Range_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " Age_Range_Label, ";
                                            question_marks += "?, ";
                                            table_column_names += " Age_Range_Description) ";
                                            question_marks += "? ";
                                            sql = "insert into agerange " + table_column_names + " values(" + question_marks + ")";
                                            System.out.println(" dimension_code " + dimension_code + "; dimension_label =" + dimension_label);
                                            pstmt = null;
                                            pstmt = con.prepareStatement(sql);
                                            pstmt.setString(1, dimension_code);
                                            pstmt.setString(2, dimension_label);
                                            pstmt.setString(3, "");
                                        }
                                        if (dimension_table_name.equalsIgnoreCase("gender")) {
                                            table_column_names += " (Gender_Code, ";
                                            question_marks += "?, ";
                                            table_column_names += " Gender_Label) ";
                                            question_marks += "? ";
                                            sql = "insert into " + dimension_table_name + table_column_names + " values(" + question_marks + ")";
                                            pstmt = null;
                                            pstmt = con.prepareStatement(sql);
                                            pstmt.setString(1, dimension_code);
                                            pstmt.setString(2, dimension_label);
                                        }
                                        Debug.println("\n" + sql);
                                        pstmt.executeUpdate();
                                        pstmt.close();
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("\tIGNORE THE code; no label column found!");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            error_message += "<br/>Error in validating dimensions: Row Number = " + table_reader.getRowNumber();
            e.printStackTrace();
        }
        Debug.println("validateDimensions END\n");
        return error_message;
    }
}
