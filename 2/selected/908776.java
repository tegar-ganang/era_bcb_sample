package org.blogtrader.platform.modules.dataserver.basic;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.blogtrader.platform.core.data.dataserver.AbstractQuoteDataServer;
import org.blogtrader.platform.core.analysis.descriptor.QuoteDataSourceDescriptor;
import org.blogtrader.platform.core.stock.Quote;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class YahooQuoteDataServer extends AbstractQuoteDataServer {

    private long firstTime;

    private long lastTime;

    /**
     * Retrive data from Yahoo finance website
     * @param String stock code
     *
     * Template:
     * http://table.finance.yahoo.com/table.csv?s=^HSI&a=01&b=20&c=1990&d=07&e=18&f=2005&g=d&ignore=.csv
     */
    protected long loadFromSource(long afterThisTime) {
        long startTime = System.currentTimeMillis();
        QuoteDataSourceDescriptor quoteDataSourceDescriptor = (QuoteDataSourceDescriptor) dataSourceDescriptor;
        List<Quote> dataPool = dataPools.get(quoteDataSourceDescriptor.sourceSymbol);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        SimpleDateFormat sdf = new SimpleDateFormat(quoteDataSourceDescriptor.dateFormat, Locale.US);
        Date fromDate = new Date();
        Date toDate = new Date();
        if (afterThisTime == FIRST_TIME_LOAD) {
            fromDate = quoteDataSourceDescriptor.fromDate;
            toDate = quoteDataSourceDescriptor.toDate;
        } else {
            calendar.setTimeInMillis(afterThisTime);
            fromDate = calendar.getTime();
        }
        calendar.setTime(fromDate);
        int a = calendar.get(Calendar.MONTH);
        int b = calendar.get(Calendar.DAY_OF_MONTH);
        int c = calendar.get(Calendar.YEAR);
        calendar.setTime(toDate);
        int d = calendar.get(Calendar.MONTH);
        int e = calendar.get(Calendar.DAY_OF_MONTH);
        int f = calendar.get(Calendar.YEAR);
        BufferedReader dis;
        StringBuffer urlStr = new StringBuffer();
        urlStr.append("http://table.finance.yahoo.com/table.csv").append("?s=");
        urlStr.append(quoteDataSourceDescriptor.sourceSymbol);
        urlStr.append("&a=" + a + "&b=" + b + "&c=" + c + "&d=" + d + "&e=" + e + "&f=" + f);
        urlStr.append("&g=d&ignore=.csv");
        try {
            URL url = new URL(urlStr.toString());
            System.out.println(url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            dis = new BufferedReader(isr);
            String s = dis.readLine();
            int iDateTime = 0;
            int iOpen = 1;
            int iHigh = 2;
            int iLow = 3;
            int iClose = 4;
            int iVolume = 5;
            int iAdjClose = 6;
            count = 0;
            calendar.clear();
            while ((s = dis.readLine()) != null) {
                String[] items;
                items = s.split(",");
                if (items.length < 6) {
                    break;
                }
                Date date = null;
                try {
                    date = sdf.parse(items[iDateTime].trim());
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    continue;
                }
                calendar.clear();
                calendar.setTime(date);
                long time = calendar.getTimeInMillis();
                if (time <= afterThisTime) {
                    continue;
                }
                Quote quote = new Quote();
                quote.time = time;
                quote.open = Float.parseFloat(items[iOpen].trim());
                quote.high = Float.parseFloat(items[iHigh].trim());
                quote.low = Float.parseFloat(items[iLow].trim());
                quote.close = Float.parseFloat(items[iClose].trim());
                quote.volume = Float.parseFloat(items[iVolume].trim()) / 100f;
                quote.amount = -1;
                quote.close_adj = Float.parseFloat(items[iAdjClose].trim());
                if (quote.high * quote.low * quote.close == 0) {
                    quote = null;
                    continue;
                }
                dataPool.add(quote);
                if (count == 0) {
                    firstTime = time;
                }
                lastTime = time;
                setAscending((lastTime >= firstTime) ? true : false);
                count++;
            }
        } catch (Exception ex) {
            System.out.println("Error in Reading File: " + ex.getMessage());
        }
        long newestTime = (lastTime >= firstTime) ? lastTime : firstTime;
        return newestTime;
    }

    public String getName() {
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormat() {
        return "dd-MMM-yy";
    }
}
