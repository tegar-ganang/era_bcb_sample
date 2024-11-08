package yahoofinance.spreadsheet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import yahoofinance.data.BalanceSheet;
import yahoofinance.data.Stock;
import yahoofinance.data.model.BalanceSheetDate;
import yahoofinance.spreadsheet.singlestocksheets.BalanceSheetSheet;
import yahoofinance.spreadsheet.singlestocksheets.HistoricalDividendsSheet;
import yahoofinance.spreadsheet.singlestocksheets.HistoricalPricesSheet;
import yahoofinance.spreadsheet.singlestocksheets.KeyStatisticsSheet;
import yahoofinance.spreadsheet.singlestocksheets.QuoteSheet;
import yahoofinance.spreadsheet.util.POIUtils;

public class SingleStockSpreadsheet {

    private final transient List<Stock> stocks;

    private final transient Workbook workbook;

    public SingleStockSpreadsheet(final List<Stock> stocks) {
        this.stocks = stocks;
        workbook = new HSSFWorkbook();
        POIUtils.initializeInstance(workbook);
        createQuoteSheet();
        createKeyStatisticsSheet();
        createHistoricalPricesSheet();
        createHistoricalDividendsSheet();
        createBalanceSheetSheets();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            final Sheet sheet = workbook.getSheetAt(i);
            final Row row = sheet.getRow(0);
            for (int j = 0; j < row.getLastCellNum(); j++) {
                sheet.autoSizeColumn(j);
            }
            sheet.createFreezePane(row.getLastCellNum(), 1);
        }
    }

    private void createQuoteSheet() {
        final Sheet sheet = workbook.createSheet("Quote");
        new QuoteSheet(sheet, stocks);
    }

    private void createKeyStatisticsSheet() {
        final Sheet sheet = workbook.createSheet("Key Statistics");
        new KeyStatisticsSheet(sheet, stocks);
    }

    private void createHistoricalPricesSheet() {
        final Sheet sheet = workbook.createSheet("Historical Prices");
        new HistoricalPricesSheet(sheet, stocks);
    }

    private void createHistoricalDividendsSheet() {
        final Sheet sheet = workbook.createSheet("Historical Dividends");
        new HistoricalDividendsSheet(sheet, stocks);
    }

    private void createBalanceSheetSheets() {
        final Sheet sheet1 = workbook.createSheet("Annual Balance Sheet");
        final Set<BalanceSheetDate> dates1 = new TreeSet<BalanceSheetDate>();
        for (final Stock stock : stocks) {
            for (final BalanceSheetDate date : stock.getAnnualBalanceSheet().getBalanceSheetDates()) {
                dates1.add(date);
            }
        }
        new BalanceSheetSheet(sheet1, stocks, dates1, BalanceSheet.BALANCE_SHEET_ANNUAL);
        final Sheet sheet2 = workbook.createSheet("Quarterly Balance Sheet");
        final Set<BalanceSheetDate> dates2 = new TreeSet<BalanceSheetDate>();
        for (final Stock stock : stocks) {
            for (final BalanceSheetDate date : stock.getQuarterlyBalanceSheet().getBalanceSheetDates()) {
                dates2.add(date);
            }
        }
        new BalanceSheetSheet(sheet2, stocks, dates2, BalanceSheet.BALANCE_SHEET_QUARTERLY);
    }

    public void writeOutSpreadsheet() throws IOException {
        final FileOutputStream fileOut = new FileOutputStream(stocks.get(0).getSymbol() + ".xls");
        workbook.write(fileOut);
        fileOut.close();
    }
}
