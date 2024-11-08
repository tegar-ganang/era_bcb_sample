package jds.com.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import jds.com.config.ConfigUtils;
import org.apache.log4j.Logger;
import org.lab.dataUniverse.Quote;
import org.lab.dataUniverse.QuoteComparator;
import org.lab.dataUniverse.Quotes;

public class ReaderWriterPersistanceImpl extends MemoryQuotesPersistanceImpl {

    private DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");

    private static Logger logger = Logger.getLogger(ReaderWriterPersistanceImpl.class);

    protected Reader reader;

    protected Writer writer;

    protected String currentLine = null;

    protected ReaderWriterPersistanceImpl(String ticker) {
        super(ticker);
    }

    public ReaderWriterPersistanceImpl(Reader reader, Writer writer, String ticker) {
        super(ticker);
        this.reader = reader;
        this.writer = writer;
    }

    /**
	 * Si no existe Reader entonces se comporta como un MemoryPersistance
	 * 
	 * @see jds.com.service.impl.MemoryQuotesPersistanceImpl#getQuotes()
	 */
    @Override
    public Quotes getQuotes() {
        try {
            internalGetQuotes();
            if (reader != null) reader.close();
        } catch (IOException e) {
            logger.fatal("Error lectura", e);
        } catch (ParseException e) {
            logger.fatal("Error parseando", e);
        }
        return quotes;
    }

    /**
	 * Metodo de ayuda en la herencia para delegar las excepciones a la clase
	 * hija Actualiza currenline y quotes
	 * 
	 * @return
	 * @throws Exception
	 */
    protected void internalGetQuotes() throws IOException, ParseException {
        if (reader == null) {
            super.getQuotes();
            return;
        }
        quotes = new Quotes();
        BufferedReader in;
        in = new BufferedReader(reader);
        while ((currentLine = in.readLine()) != null) {
            String[] values = currentLine.split(Character.toString(ConfigUtils.getConfig().getSymbolsConfig().getSeparator()));
            Quote quote = new Quote();
            if (values.length == 6) {
                quote.setTicker(ticker);
                quote.setDate(DATE_FORMATTER.parse(values[0]));
                quote.setOpen(Double.valueOf(values[1]));
                quote.setClose(Double.valueOf(values[2]));
                quote.setMax(Double.valueOf(values[3]));
                quote.setMin(Double.valueOf(values[4]));
                quote.setVolume(Long.valueOf(values[5]));
                if (!quotes.contains(quote)) {
                    quotes.add(quote);
                }
            } else {
                logger.error("Error en el formato leido " + currentLine + " en posicion numero " + (quotes.size() + 1));
            }
        }
    }

    @Override
    public void saveQuotes(Quotes quotes) {
        try {
            internalSaveQuotes(quotes);
            writer.close();
        } catch (IOException e) {
            logger.fatal("Error en escritura al guardar las quotes", e);
        }
    }

    protected void internalSaveQuotes(Quotes quotes) throws IOException {
        super.saveQuotes(quotes);
        if (writer == null) return;
        Character separator = ConfigUtils.getConfig().getSymbolsConfig().getSeparator();
        ArrayList<Quote> list = new ArrayList<Quote>(quotes);
        QuoteComparator quoteComparator = new QuoteComparator();
        Collections.sort(list, quoteComparator);
        for (Quote quote : list) {
            StringBuffer line = new StringBuffer();
            line.append(DATE_FORMATTER.format(quote.getDate()));
            line.append(separator);
            line.append(quote.getOpen());
            line.append(separator);
            line.append(quote.getClose());
            line.append(separator);
            line.append(quote.getMax());
            line.append(separator);
            line.append(quote.getMin());
            line.append(separator);
            line.append(quote.getVolume());
            line.append("\r\n");
            writer.write(line.toString());
        }
        writer.flush();
    }
}
