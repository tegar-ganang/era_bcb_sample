package game.report.srobjects;

import ar.com.fdvs.dj.domain.ColumnProperty;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.entities.columns.PropertyColumn;
import ar.com.fdvs.dj.domain.entities.columns.SimpleColumn;
import game.report.DJReportRenderer;
import game.report.SRDJRenderer;
import java.awt.*;
import java.util.ArrayList;

public class SRTableRendererDJ implements ISRObjectRenderer {

    protected SRTable srTable;

    protected SRDJRenderer srdjRenderer;

    Border border = DJReportRenderer.getConfigureData().getJasper().getTableBorder();

    Border headerBorder = DJReportRenderer.getConfigureData().getJasper().getTableHeaderBorder();

    Color borderColor = DJReportRenderer.getConfigureData().getJasper().getTableBorderColor();

    Color headerBackgroundColor = DJReportRenderer.getConfigureData().getJasper().getTableHeaderBackgroundColor();

    boolean colorBackgroundOddRows = DJReportRenderer.getConfigureData().getJasper().getTableColorBackgroundOddRows();

    Color backgroundColor = DJReportRenderer.getConfigureData().getJasper().getTableBackgroundColor();

    Double rounding = DJReportRenderer.getConfigureData().getJasper().getTableNumberRounding();

    int maxTableSize = DJReportRenderer.getConfigureData().getJasper().getMaxTableSize();

    public SRTableRendererDJ(SRTable srTable, SRDJRenderer srdjRenderer) {
        this.srTable = srTable;
        this.srdjRenderer = srdjRenderer;
    }

    public void render() {
        String[][] data = srTable.getAsMatrix();
        FastReportBuilder rb = new FastReportBuilder();
        ArrayList<DJReportParameters> DJReportParameters = new ArrayList<DJReportParameters>();
        for (int j = 0; j < data[0].length; j++) {
            try {
                PropertyColumn column = new SimpleColumn();
                column.setTitle(srTable.isHeaderFirstRow() ? data[0][j] : "");
                column.setName(String.class.getName());
                column.setColumnProperty(new ColumnProperty("param[" + j + "]", String.class.getName()));
                column.setWidth(100);
                Style style = new Style();
                style.setBorderTop(border);
                style.setBorderColor(borderColor);
                Style styleHeader = new Style();
                styleHeader.setBackgroundColor(headerBackgroundColor);
                column.setStyle(style);
                styleHeader.setBorder(headerBorder);
                Color colorHeader = new Color(255, 251, 174);
                rb.addColumn(column);
                if (data.length > 3 && colorBackgroundOddRows == true) rb.setPrintBackgroundOnOddRows(true);
                Style style2 = new Style();
                style2.setBackgroundColor(backgroundColor);
                rb.setOddRowBackgroundStyle(style2);
            } catch (Exception e) {
            }
        }
        for (int i = 0; i < data.length; i++) {
            String ff[] = null;
            boolean header = (i == 0 && srTable.isHeaderFirstRow());
            ff = new String[maxTableSize];
            for (int j = 0; j < data[i].length; j++) {
                if (srTable.isHeaderFirstRow() == true) {
                    if (i < data.length - 1) ff[j] = data[i + 1][j];
                } else ff[j] = data[i][j];
                if (ff[j] != null && ff[j].contains(".") == true) {
                    try {
                        Double number = Double.parseDouble(ff[j]);
                        {
                            number = Math.round(number * rounding) / rounding;
                            ff[j] = number.toString();
                        }
                    } catch (Exception e) {
                    }
                }
            }
            DJReportParameters.add(new DJReportParameters(ff));
        }
        rb.setMargins(5, 5, 20, 20);
        rb.setUseFullPageWidth(true);
        rb.setTitle(srTable.getCaption());
        if (srTable.isHeaderFirstRow() == false) rb.setPrintColumnNames(false);
        DynamicReport dr = rb.build();
        DJReportRenderer.addParams("" + DJReportRenderer.getSubreportNumber(), DJReportParameters);
        DJReportRenderer.addSubList(dr);
    }
}
