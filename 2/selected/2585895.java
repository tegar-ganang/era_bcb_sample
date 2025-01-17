package org.mov.quote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.MalformedURLException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mov.prefs.PreferencesManager;
import org.mov.util.Locale;

/**
 * Import intra-day quotes from Yahoo into Venice.
 *
 * @author Andrew Leppard
 */
public class YahooIDQuoteImport {

    private static final String SYMBOLS = "_SYM_";

    private static final String YAHOO_PATTERN = ("?s=" + SYMBOLS + "&f=sl1d1t1c1ohgv&e=.csv");

    private static final String YAHOO_URL_PATTERN = ("http://finance.yahoo.com/d/quotes.csv" + YAHOO_PATTERN);

    private YahooIDQuoteImport() {
        assert false;
    }

    /**
     * Retrieve intra-day quotes from Yahoo.
     *
     * @param symbols the symbols to import.
     * @exception ImportExportException if there was an error retrieving the quotes
     */
    public static List importSymbols(List symbols) throws ImportExportException {
        List quotes = new ArrayList();
        String URLString = constructURL(symbols);
        IDQuoteFilter filter = new YahooIDQuoteFilter();
        PreferencesManager.ProxyPreferences proxyPreferences = PreferencesManager.loadProxySettings();
        try {
            URL url = new URL(URLString);
            InputStreamReader input = new InputStreamReader(url.openStream());
            BufferedReader bufferedInput = new BufferedReader(input);
            String line;
            do {
                line = bufferedInput.readLine();
                if (line != null) {
                    try {
                        IDQuote quote = filter.toIDQuote(line);
                        quote.verify();
                        quotes.add(quote);
                    } catch (QuoteFormatException e) {
                    }
                }
            } while (line != null);
            bufferedInput.close();
        } catch (BindException e) {
            throw new ImportExportException(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
        } catch (ConnectException e) {
            throw new ImportExportException(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
        } catch (UnknownHostException e) {
            throw new ImportExportException(Locale.getString("UNKNOWN_HOST_ERROR", e.getMessage()));
        } catch (NoRouteToHostException e) {
            throw new ImportExportException(Locale.getString("DESTINATION_UNREACHABLE_ERROR", e.getMessage()));
        } catch (MalformedURLException e) {
            throw new ImportExportException(Locale.getString("INVALID_PROXY_ERROR", proxyPreferences.host, proxyPreferences.port));
        } catch (FileNotFoundException e) {
            throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
        } catch (IOException e) {
            throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
        }
        return quotes;
    }

    /**
     * Construct the URL necessary to retrieve all the quotes for the given symbol between
     * the given dates from Yahoo.
     *
     * @param symbols the symbos to import.
     * @return URL string
     */
    private static String constructURL(List symbols) {
        String URLString = YAHOO_URL_PATTERN;
        String symbolString = "";
        for (Iterator iterator = symbols.iterator(); iterator.hasNext(); ) {
            Symbol symbol = (Symbol) iterator.next();
            symbolString = symbolString.concat(symbol.toString());
            if (iterator.hasNext()) symbolString = symbolString.concat("+");
        }
        URLString = replace(URLString, SYMBOLS, symbolString);
        return URLString;
    }

    /**
     * Perform a find replace on a string.
     *
     * @param string the source string
     * @param oldSubString the text which to replace
     * @param newSubString the text to replace with
     * @return the new string
     */
    private static String replace(String string, String oldSubString, String newSubString) {
        Pattern pattern = Pattern.compile(oldSubString);
        Matcher matcher = pattern.matcher(string);
        return matcher.replaceAll(newSubString);
    }
}
