package org.laboratory.investment.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.log4j.Logger;
import org.laboratory.investment.config.LaboratoryContext;
import org.laboratory.investment.config.SymbolsConfig;
import org.laboratory.investment.config.LaboratoryContext.BEANS;
import org.laboratory.investment.dataUniverse.Quote;

public class ReaderWriterPersistanceImpl extends MemoryQuotesPersistanceImpl {

    private DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");

    private static Logger logger = Logger.getLogger(ReaderWriterPersistanceImpl.class);

    protected Reader reader;

    protected Writer writer;

    protected String currentLine = null;

    SymbolsConfig sc;

    protected ReaderWriterPersistanceImpl(String ticker) {
        super(ticker);
        sc = LaboratoryContext.getBean(BEANS.SYMBOLSCONFIG);
    }

    public ReaderWriterPersistanceImpl(Reader reader, Writer writer, String ticker) {
        super(ticker);
        this.reader = reader;
        this.writer = writer;
        sc = LaboratoryContext.getBean(BEANS.SYMBOLSCONFIG);
    }

    /**
	 * Si no existe Reader entonces se comporta como un MemoryPersistance
	 * 
	 * @see org.laboratory.investment.service.impl.MemoryQuotesPersistanceImpl#getQuotes()
	 */
    @Override
    public Collection<Quote> getQuotes() {
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
        quotes = new ArrayList<Quote>();
        BufferedReader in;
        in = new BufferedReader(reader);
        while ((currentLine = in.readLine()) != null) {
            String[] values = currentLine.split(Character.toString(sc.getGroupingSeparator()));
            Quote quote = new Quote();
            if (values.length == 6) {
                quote.setDate(DATE_FORMATTER.parse(values[0]));
                quote.setOpen(Double.valueOf(values[1]));
                quote.setClose(Double.valueOf(values[2]));
                quote.setHigh(Double.valueOf(values[3]));
                quote.setLow(Double.valueOf(values[4]));
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
    public void saveQuotes(Collection<Quote> quotes) {
        try {
            internalSaveQuotes(quotes);
            writer.close();
        } catch (IOException e) {
            logger.fatal("Error en escritura al guardar las quotes", e);
        }
    }

    protected void internalSaveQuotes(Collection<Quote> quotes) throws IOException {
        super.saveQuotes(quotes);
        if (writer == null) return;
        Character separator = sc.getSeparator();
        ArrayList<Quote> list = new ArrayList<Quote>(quotes);
        Collections.sort(list);
        for (Quote quote : list) {
            StringBuffer line = new StringBuffer();
            line.append(DATE_FORMATTER.format(quote.getDate()));
            line.append(separator);
            line.append(quote.getOpen());
            line.append(separator);
            line.append(quote.getClose());
            line.append(separator);
            line.append(quote.getHigh());
            line.append(separator);
            line.append(quote.getLow());
            line.append(separator);
            line.append(quote.getVolume());
            line.append("\r\n");
            writer.write(line.toString());
        }
        writer.flush();
    }
}
