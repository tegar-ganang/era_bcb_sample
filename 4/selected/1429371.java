package net.sf.howabout.printer;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import net.sf.howabout.plugin.Event;

/**
 * Provides a method for drawing a TV Guide events table in the terminal. It
 * basically takes a list of <code>net.sf.howabout.plugin.Event</code> and a
 * vector of booleans to determine which columns will be printed.
 * @author Paulo Roberto Massa Cereda
 * @version 1.0
 * @since 1.0
 */
public class TablePrinter {

    private List<Event> list;

    /**
     * Constructor method. Sets the events list.
     * @param list
     */
    public TablePrinter(List<Event> list) {
        this.list = list;
    }

    /**
     * Empty constructor list.
     */
    public TablePrinter() {
    }

    /**
     * Setter method for the events list.
     * @param list The events list.
     */
    public void setList(List<Event> list) {
        this.list = list;
    }

    /**
     * Draws the TV Guide events table according to the events list and
     * the boolean vector.
     * @param columns A vector of booleans of which columns will be shown.
     */
    public void draw(boolean[] columns) {
        StringBuilder stringbuilder = new StringBuilder();
        String line = "";
        if (columns[0]) {
            stringbuilder.append(addSpaces("TIME", 10));
            line = line.concat("----------");
        }
        if (columns[1]) {
            stringbuilder.append(addSpaces("CHANNEL", 20));
            line = line.concat("--------------------");
        }
        if (columns[2]) {
            stringbuilder.append(addSpaces("DESCRIPTION", 45));
            line = line.concat("---------------------------------------------");
        }
        if (columns[3]) {
            stringbuilder.append(addSpaces("GENRE", 15));
            line = line.concat("---------------");
        }
        stringbuilder.append("\n");
        stringbuilder.append(line).append("\n");
        if (list.isEmpty()) {
            stringbuilder.append("No entries found, sorry.");
        } else {
            for (Event event : list) {
                if (columns[0]) {
                    stringbuilder.append(addSpaces(" " + getTimeFormat(event.getDate()), 10));
                }
                if (columns[1]) {
                    stringbuilder.append(addSpaces(" " + event.getChannel(), 20));
                }
                if (columns[2]) {
                    stringbuilder.append(addSpaces(" " + event.getDescription(), 45));
                }
                if (columns[3]) {
                    stringbuilder.append(addSpaces(" " + event.getGenre(), 15));
                }
                stringbuilder.append("\n");
            }
            stringbuilder.append(line).append("\n");
        }
        System.out.println(stringbuilder.toString());
    }

    /**
     * Transforms a calendar format to a string containing the event time.
     * @param date The event date.
     * @return A string containing the event time.
     */
    private String getTimeFormat(GregorianCalendar date) {
        DateFormat dateformat = DateFormat.getTimeInstance(DateFormat.SHORT);
        return dateformat.format(date.getTime());
    }

    /**
     * Add spaces and cuts long strings if necessary.
     * @param string The text.
     * @param value The number of spaces to be added or the substring to be extracted.
     * @return The new string.
     */
    private String addSpaces(String string, int value) {
        if (string.length() == value) {
            return string;
        } else {
            if (string.length() > value) {
                return string.substring(0, value - 1).concat(" ");
            } else {
                while (string.length() != value) {
                    string = string.concat(" ");
                }
                return string;
            }
        }
    }
}
