package yahoofinance.spreadsheet;

import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;
import yahoofinance.StockCollection;
import yahoofinance.data.Stock;

public class Driver {

    private static final Logger LOGGER = Logger.getLogger(Driver.class);

    public Driver(final String symbol) throws IOException {
        generateSpreadsheet(symbol);
    }

    private void generateSpreadsheet(final String symbol) throws IOException {
        LOGGER.debug("Generating Spreadsheet for " + symbol);
        final StockCollection collection = new StockCollection(symbol, true);
        final List<Stock> stocks = collection.getStocks();
        final SingleStockSpreadsheet sheet = new SingleStockSpreadsheet(stocks);
        sheet.writeOutSpreadsheet();
    }

    public static void main(final String[] args) throws IOException {
        new Driver("IBM");
    }
}
