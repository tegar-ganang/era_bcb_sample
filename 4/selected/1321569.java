package net.sf.howabout.plugins.uolesporte;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import net.sf.howabout.plugin.Event;
import net.sf.howabout.plugin.Query;
import net.sf.howabout.plugin.api.HowAboutPlugin;
import net.sf.howabout.plugin.Day;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Provides methods for retrieving data from UOL Esportes. It basically
 * implements the <code>net.sf.howabout.plugin.api.HowAboutPlugin</code>
 * interface.
 * @author Paulo Roberto Massa Cereda
 * @version 1.0
 * @since 1.0
 */
public class UOLEsportePlugin implements HowAboutPlugin {

    public List<Event> getEvents(Query query) {
        GregorianCalendar date = new GregorianCalendar();
        String lookup = "";
        if (query.getDay() == Day.TOMORROW) {
            date.add(Calendar.DATE, 1);
        }
        lookup = String.valueOf(date.get(Calendar.YEAR)) + "/" + addZeroes(date.get(Calendar.MONTH) + 1) + "/" + addZeroes(date.get(Calendar.DATE)) + "/";
        ArrayList<Event> list = new ArrayList<Event>();
        Event event = new Event();
        try {
            Document document = Jsoup.connect("http://esporte.uol.com.br/programacao-de-tv/data/" + lookup).get();
            Elements elements = document.getElementsByTag("tbody");
            for (Element element : elements) {
                Elements items = element.getElementsByTag("tr");
                for (Element item : items) {
                    Elements tabular = item.getElementsByTag("td");
                    event = new Event();
                    event.setDate(getDateFromString(date, tabular.get(0).text()));
                    event.setGenre(tabular.get(1).text());
                    event.setName(tabular.get(2).text());
                    event.setDescription(tabular.get(2).text());
                    event.setChannel(tabular.get(3).text());
                    if (!query.getChannel().equals("#")) {
                        if (!query.getGenre().equals("#")) {
                            if (event.getChannel().toLowerCase().equals(query.getChannel().toLowerCase())) {
                                if (event.getGenre().toLowerCase().equals(query.getGenre().toLowerCase())) {
                                    list.add(event);
                                }
                            }
                        } else {
                            if (event.getChannel().toLowerCase().equals(query.getChannel().toLowerCase())) {
                                list.add(event);
                            }
                        }
                    } else {
                        if (!query.getGenre().equals("#")) {
                            if (event.getGenre().toLowerCase().equals(query.getGenre().toLowerCase())) {
                                list.add(event);
                            }
                        } else {
                            list.add(event);
                        }
                    }
                }
            }
        } catch (Exception e) {
            list.clear();
        }
        return list;
    }

    public String getPluginName() {
        return "HowAbout UOL Esporte Plugin";
    }

    public String getPluginVersion() {
        return "1.0";
    }

    public String getPluginAuthor() {
        return "Paulo Roberto Massa Cereda";
    }

    public String getPluginFullPackageName() {
        return "net.sf.howabout.plugins.uolesporte";
    }

    public String getPluginHelp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Convert the parameter to string and add a zero if necessary.
     * @param value The integer value.
     * @return A string containing the integer and a zero, if necessary.
     */
    private String addZeroes(int value) {
        String newValue = String.valueOf(value);
        if ((value >= 0) && (value <= 9)) {
            newValue = "0" + newValue;
        }
        return newValue;
    }

    /**
     * Gets the current date and the string with the time and returns a calendar
     * with the correct time set.
     * @param date The current date.
     * @param value The time represented as a string.
     * @return The current date with the correct time.
     */
    private GregorianCalendar getDateFromString(GregorianCalendar date, String value) {
        StringTokenizer token = new StringTokenizer(value, "h");
        String hour = token.nextToken();
        String minute = token.nextToken();
        GregorianCalendar newdate = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE), Integer.parseInt(hour), Integer.parseInt(minute), 0);
        return newdate;
    }
}
