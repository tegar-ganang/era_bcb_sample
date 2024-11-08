package org.makagiga.plugins.weather;

import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import org.makagiga.commons.FS;
import org.makagiga.commons.MDate;
import org.makagiga.commons.xml.SimpleXMLReader;

/**
 * @see <a href="http://developer.yahoo.com/weather/">http://developer.yahoo.com/weather/</a>
 */
public final class Weather {

    private final Info condition = new Info();

    private String channelLink;

    private String channelTitle;

    private String description;

    private String language;

    private String link;

    private String title;

    private Temperature temperatureUnit = Temperature.CELSIUS;

    public Weather(final int code) {
        condition.code = code;
        condition.date = MDate.now();
        condition.temperature = 25;
        condition.text = "Text Test";
        description = "Description Test";
        title = "Title Test";
    }

    public Weather(final InputStream input) throws IOException {
        Objects.requireNonNull(input);
        SimpleXMLReader reader = null;
        try {
            reader = new SimpleXMLReader() {

                private boolean inChannel;

                private boolean inItem;

                @Override
                protected void onEnd(final String name) {
                    if (inChannel) {
                        switch(name) {
                            case "channel":
                                inChannel = false;
                                break;
                            case "item":
                                inItem = false;
                                break;
                        }
                    }
                }

                @Override
                protected void onStart(final String name) {
                    if (!inChannel && "channel".equals(name)) {
                        inChannel = true;
                    } else if (inItem) {
                        switch(name) {
                            case "link":
                                if (link == null) link = this.getValue("");
                                break;
                            case "title":
                                if (title == null) title = this.getValue("");
                                break;
                            default:
                                if ("yweather".equals(this.getReader().getPrefix())) {
                                    switch(name) {
                                        case "condition":
                                            condition.code = this.getIntegerAttribute("code", Weather.Info.UNDEFINED_CODE);
                                            String dateAttr = this.getStringAttribute("date");
                                            java.util.Date d = MDate.parseRFC822(dateAttr);
                                            if (d == null) d = MDate.parse(dateAttr, "EEE, d MMM yyyy K:mm a z", Locale.US, null);
                                            condition.date = d;
                                            condition.temperature = this.getIntegerAttribute("temp", Weather.Info.UNDEFINED_TEMPERATURE);
                                            condition.text = this.getStringAttribute("text");
                                            break;
                                    }
                                }
                        }
                    } else if (inChannel) {
                        switch(name) {
                            case "description":
                                if (description == null) description = this.getValue("");
                                break;
                            case "item":
                                inItem = true;
                                break;
                            case "language":
                                if (language == null) language = this.getValue("");
                                break;
                            case "link":
                                if (channelLink == null) channelLink = this.getValue("");
                                break;
                            case "title":
                                if (channelTitle == null) channelTitle = this.getValue("");
                                break;
                            case "units":
                                if ("yweather".equals(this.getReader().getPrefix())) {
                                    temperatureUnit = Temperature.of(this.getStringAttribute("temperature"));
                                }
                                break;
                        }
                    }
                }
            };
            reader.read(input);
        } finally {
            FS.close(reader);
        }
    }

    public String getChannelLink() {
        return Objects.toString(channelLink, "");
    }

    public String getChannelTitle() {
        return Objects.toString(channelTitle, "");
    }

    public Info getCondition() {
        return condition;
    }

    public String getDescription() {
        return Objects.toString(description, "");
    }

    public String getLanguage() {
        return Objects.toString(language, "");
    }

    public String getLink() {
        return Objects.toString(link, "");
    }

    public Temperature getTemperatureUnit() {
        return temperatureUnit;
    }

    public String getTitle() {
        return Objects.toString(title, "");
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public static final class Info {

        public static final int UNDEFINED_CODE = -1;

        public static final int UNDEFINED_TEMPERATURE = Integer.MAX_VALUE;

        private java.util.Date date;

        private int code = UNDEFINED_CODE;

        private int temperature = UNDEFINED_TEMPERATURE;

        private String text;

        public int getCode() {
            return code;
        }

        public MDate getDate() {
            return (date == null) ? MDate.invalid() : new MDate(date);
        }

        public int getTemperature() {
            return temperature;
        }

        public String getTemperature(final Temperature unit) {
            if (temperature == UNDEFINED_TEMPERATURE) return "?";
            return unit.format(temperature);
        }

        @Override
        public String toString() {
            return Objects.toString(text, "?");
        }

        private Info() {
        }
    }
}
