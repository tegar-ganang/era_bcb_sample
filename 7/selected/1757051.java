package org.jcrosstab;

import java.io.*;
import jxl.*;
import jxl.write.*;

public class DisplayConverter {

    String[][] hor;

    String[][] vert;

    String[][] data_grid;

    public String getTabDelimitedTable(AccumulatorDefinition ad, boolean measures_on_row, int vert_axis_slice_size) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < hor.length; i++) {
            if (ad.getMeasureCount() == 1) {
                if (i == 0) {
                    str.append(ad.get(0).getName() + "\t");
                    for (int x = 0; x < vert_axis_slice_size - 1; x++) str.append("\t");
                } else {
                    for (int x = 0; x < vert_axis_slice_size; x++) str.append("\t");
                }
            } else if (measures_on_row) {
                for (int x = 0; x <= vert_axis_slice_size; x++) str.append("\t");
                if (ad.isMultiMeasure() && ad.isMultiMetric()) str.append("\t");
            } else {
                for (int x = 0; x < vert_axis_slice_size; x++) str.append("\t");
            }
            for (int j = 0; j < hor[i].length; j++) {
                str.append(hor[i][j].trim() + "\t");
            }
            str.append("\n");
        }
        for (int i = 0; i < vert.length; i++) {
            for (int j = 0; j < vert[i].length; j++) {
                str.append(vert[i][j].trim() + "\t");
            }
            for (int k = 0; k < data_grid[i].length; k++) {
                str.append(data_grid[i][k].trim() + "\t");
            }
            str.append("\n");
        }
        str.append("\n");
        return str.toString();
    }

    public String getHtmlTable(AccumulatorDefinition ad, boolean measures_on_row, int vert_axis_slice_size, int hor_axis_slice_size) {
        return getHtmlTable(ad, measures_on_row, false, vert_axis_slice_size, hor_axis_slice_size);
    }

    public String getHtmlTable(AccumulatorDefinition ad, boolean measures_on_row, boolean write_entire_page, int vert_axis_slice_size, int hor_axis_slice_size) {
        StringBuffer str = new StringBuffer();
        if (write_entire_page) {
            str.append("<html>\n");
            str.append("<head>\n");
            str.append("\t<link rel=stylesheet href=\"../app.css\" type=\"text/css\">\n");
            str.append("</head>\n");
            str.append("<body>\n");
        }
        str.append("<table>");
        for (int i = 0; i < hor.length; i++) {
            str.append("<tr>");
            if (i == 0) {
                if (ad.getMeasureMetricColumnCount() == 1) {
                    MeasureDefinition md = ad.get(0);
                    str.append("<th colspan=" + vert_axis_slice_size + " rowspan=" + hor_axis_slice_size + ">" + md.getName() + "\n");
                } else if (measures_on_row) {
                    if (ad.getMeasureCount() == 1) str.append("<th colspan=" + (vert_axis_slice_size + 1) + " rowspan=" + hor_axis_slice_size + ">" + ad.get(0).getName() + "\n"); else {
                        if (ad.isMultiMetric() && ad.isMultiMeasure()) str.append("<th colspan=" + (vert_axis_slice_size + 2) + " rowspan=" + hor_axis_slice_size + ">\n"); else str.append("<th colspan=" + (vert_axis_slice_size + 1) + " rowspan=" + hor_axis_slice_size + ">\n");
                    }
                } else {
                    if (ad.getMeasureCount() == 1) str.append("<th colspan=" + vert_axis_slice_size + " rowspan=" + (hor_axis_slice_size + 1) + ">" + ad.get(0).getName() + "\n"); else {
                        if (ad.isMultiMeasure() && ad.isMultiMetric()) str.append("<th colspan=" + vert_axis_slice_size + " rowspan=" + (hor_axis_slice_size + 2) + ">\n"); else str.append("<th colspan=" + vert_axis_slice_size + " rowspan=" + (hor_axis_slice_size + 1) + ">\n");
                    }
                }
            }
            for (int j = 0; j < hor[i].length; j++) {
                str.append("<th ");
                int colspan = 1;
                while ((j < hor[i].length - 1) && (hor[i][j].equals(hor[i][j + 1]))) {
                    j++;
                    colspan++;
                }
                if (colspan > 1) str.append(" colspan=" + colspan);
                str.append(" nowrap>");
                str.append(hor[i][j].trim());
            }
        }
        int[] skip_cells = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        boolean odd_row = true;
        String odd_even_row = "odd";
        int data_row_index = 0;
        int measure_index = 0;
        for (int i = 0; i < vert.length; i++) {
            if (odd_row) odd_even_row = "odd"; else odd_even_row = "";
            str.append("<tr class=\"" + odd_even_row + "\">");
            for (int j = 0; j < vert[i].length; j++) {
                if (skip_cells[j] > 1) {
                    skip_cells[j]--;
                } else {
                    str.append("<th nowrap ");
                    int x = i;
                    while ((x < vert.length - 1) && (vert[x][j].equals(vert[x + 1][j]))) {
                        x++;
                        skip_cells[j]++;
                    }
                    str.append(" rowspan=" + skip_cells[j] + " ");
                    str.append(">" + vert[i][j].trim());
                }
            }
            for (int l = 0; l < data_grid[i].length; l++) {
                if (data_grid[i][l].equals("")) str.append("<td></td>\n"); else {
                    str.append("<td>" + data_grid[i][l] + "</td>\n");
                }
            }
            odd_row = !odd_row;
        }
        str.append("</table>\n");
        if (write_entire_page) {
            str.append("</body>\n");
            str.append("</html>\n");
        }
        return str.toString();
    }

    /** This method writes an Excel workbook, using JExcelApi from sf.net.
	*/
    public void writeWorkbook(OutputStream os, AccumulatorDefinition ad) {
        System.out.println("DisplayConverter.java, 236: " + "entered writeWorkbook");
        try {
            WritableWorkbook w = Workbook.createWorkbook(os);
            WritableSheet s = w.createSheet("ExcelBuilder", 0);
            WritableCellFormat h_cell = new WritableCellFormat();
            h_cell.setAlignment(jxl.format.Alignment.CENTRE);
            h_cell.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);
            h_cell.setBackground(jxl.format.Colour.AQUA);
            int base_col = vert[0].length;
            for (int i = 0; i < hor.length; i++) {
                for (int j = 0; j < hor[i].length; j++) {
                    s.addCell(new Label(j + base_col, i, hor[i][j].trim(), h_cell));
                    int cell_count_to_merge = 0;
                    int j_start = j;
                    while ((j < hor[i].length - 1) && (hor[i][j].equals(hor[i][j + 1]))) {
                        cell_count_to_merge++;
                        j++;
                    }
                    if (cell_count_to_merge > 0) {
                        s.mergeCells(j_start + base_col, i, j_start + base_col + cell_count_to_merge, i);
                    }
                }
            }
            int base_row = hor.length;
            for (int i = 0; i < vert.length; i++) {
                for (int j = 0; j < vert[i].length; j++) {
                    if (i == 0) s.addCell(new Label(j, i + base_row, vert[i][j].trim(), h_cell)); else if (!vert[i][j].equals(vert[i - 1][j])) s.addCell(new Label(j, i + base_row, vert[i][j].trim(), h_cell));
                }
            }
            for (int j = 0; j < vert[0].length; j++) {
                for (int i = 0; i < vert.length; i++) {
                    int i_start = i;
                    int cell_count_to_merge = 0;
                    while ((i < vert.length - 1) && (vert[i][j].equals(vert[i + 1][j]))) {
                        cell_count_to_merge++;
                        i++;
                    }
                    if (cell_count_to_merge > 0) {
                        s.mergeCells(j, i_start + base_row, j, i_start + base_row + cell_count_to_merge);
                    }
                }
            }
            WritableCellFormat data_cell = new WritableCellFormat();
            for (int row = 0; row < vert.length; row++) {
                for (int col = 0; col < data_grid[row].length; col++) {
                    if (data_grid[row][col].equals("")) {
                    } else {
                        s.addCell(new jxl.write.Number(base_col + col, base_row + row, Float.parseFloat(data_grid[row][col])));
                    }
                }
            }
            WritableCellFormat grid_cell_name = new WritableCellFormat();
            grid_cell_name.setAlignment(jxl.format.Alignment.CENTRE);
            grid_cell_name.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);
            if (ad.getMeasureCount() == 1) {
                MeasureDefinition md = ad.get(0);
                s.addCell(new Label(0, 0, md.getName(), grid_cell_name));
            } else s.addCell(new Label(0, 0, "", grid_cell_name));
            s.mergeCells(0, 0, vert[0].length - 1, hor.length - 1);
            w.write();
            w.close();
        } catch (IOException ioe) {
            System.out.println("DisplayConverter.java, 341: " + "IOException caught " + ioe.getMessage());
        } catch (jxl.write.WriteException we) {
            System.out.println("DisplayConverter.java, 345: " + "jxl.write.WriteException caught " + we.getMessage());
        }
    }

    public String writeDataGrid() {
        StringBuffer str = new StringBuffer();
        for (int row = 0; row < data_grid.length; row++) {
            for (int col = 0; col < data_grid[row].length; col++) {
                str.append(data_grid[row][col] + " ");
            }
            str.append("\n");
        }
        return str.toString();
    }

    public void sortOnColumn(int col_idx) {
        sortOnColumn(true, col_idx);
    }

    public void sortOnColumn(boolean sort_ascending, int col_idx) {
        boolean made_change = true;
        while (made_change) {
            made_change = false;
            for (int i = 0; i < data_grid.length - 1; i++) {
                boolean flip = false;
                if (data_grid[i][col_idx].equals("") || data_grid[i + 1][col_idx].equals("")) {
                    if (data_grid[i][col_idx].equals("") && data_grid[i + 1][col_idx].equals("")) {
                    } else {
                        if ((sort_ascending && !data_grid[i][col_idx].equals("") && data_grid[i + 1][col_idx].equals("")) || (!sort_ascending && data_grid[i][col_idx].equals("") && !data_grid[i + 1][col_idx].equals(""))) {
                            flip = true;
                        }
                    }
                } else {
                    if ((sort_ascending && (Float.parseFloat(data_grid[i][col_idx]) > Float.parseFloat(data_grid[i + 1][col_idx]))) || (!sort_ascending && (Float.parseFloat(data_grid[i][col_idx]) < Float.parseFloat(data_grid[i + 1][col_idx])))) {
                        flip = true;
                    } else {
                    }
                }
                if (flip) {
                    String[] temp = data_grid[i];
                    data_grid[i] = data_grid[i + 1];
                    data_grid[i + 1] = temp;
                    String[] stemp = vert[i];
                    vert[i] = vert[i + 1];
                    vert[i + 1] = stemp;
                    made_change = true;
                }
            }
        }
    }

    public void sortOnRow(int row_idx) {
        sortOnRow(true, row_idx);
    }

    public void sortOnRow(boolean sort_ascending, int row_idx) {
        boolean made_change = true;
        while (made_change) {
            made_change = false;
            for (int j = 0; j < data_grid[row_idx].length - 1; j++) {
                boolean flip = false;
                if (data_grid[row_idx][j].equals("") || data_grid[row_idx][j + 1].equals("")) {
                    if (data_grid[row_idx][j].equals("") && data_grid[row_idx][j + 1].equals("")) {
                    } else {
                        if ((sort_ascending && !data_grid[row_idx][j].equals("") && data_grid[row_idx][j + 1].equals("")) || (!sort_ascending && data_grid[row_idx][j].equals("") && !data_grid[row_idx][j + 1].equals(""))) {
                            flip = true;
                        }
                    }
                } else {
                    if ((sort_ascending && (Float.parseFloat(data_grid[row_idx][j]) > Float.parseFloat(data_grid[row_idx][j + 1]))) || (!sort_ascending && (Float.parseFloat(data_grid[row_idx][j]) < Float.parseFloat(data_grid[row_idx][j + 1])))) {
                        flip = true;
                    } else {
                    }
                }
                if (flip) {
                    for (int ii = 0; ii < hor.length; ii++) {
                        String stemp = hor[ii][j];
                        hor[ii][j] = hor[ii][j + 1];
                        hor[ii][j + 1] = stemp;
                    }
                    for (int ii = 0; ii < data_grid.length; ii++) {
                        String temp = data_grid[ii][j];
                        data_grid[ii][j] = data_grid[ii][j + 1];
                        data_grid[ii][j + 1] = temp;
                    }
                    made_change = true;
                }
            }
        }
    }
}
