package szene.display.event;

import de.enough.polish.io.Serializable;

/**
 * Serialisierbare Eventkalendar-Einstellungen
 * @author Knoll
 *
 */
public class EventcalendarSettings implements Serializable {

    /**
	page [optional]: die Seite, die angezeigt werden soll (f�r Bl�tterfunktion)
	(default 1)
	limit [optional]: das Limit der Events, die pro Seite erscheinen (default
	30, max. 100)
	relevance [optional]: sollen relevante Events angezeigt werden
	channelid [optional]: die ChannelId der Events
	locationid [optional]: die LocationId der Events
	locationname [optional]: der LocationName der Events
	categoryid [optional]: die CategoryId der Events (f�r mehrere IDs diese mit
	�,� [Komma] trennen)
	country [optional]: Der ISO3166-1 alpha-2 countrycode des Landes
	provinceid [optional]: die ProvinceId der Events (f�r mehrere IDs diese mit
	�,� [Komma] trennen)
	eventname [optional]: der Eventname
	from [optional]: ab wann werden die Events angezeigt
	to [optional]: bis wann werden die Events angezeigt
	radius [optional]: wie gro� soll der Suchradius sein? (longitude & latitude
	m�ssen gegeben sein, oder der place)
	longitude [optional]: Longtitude
	latitude [optional]: Latitude
	place [optional]: Adresse eines Ortes (Name, Postleitzahl, Stra�e,
	Hausnummer)
	order [optional]: Das Feld nach dem sortiert werden soll
	orderdirection [optional]: Die Richtung (�ASC� = Aufsteigend, �DESC� =
	Absteigend � auf den order Parameter bezogen)
	loadflyers [optional]: Gibt an ob die Flyers mitgeladen werden, (�true�,
	�false�). Wenn die Flyer mitgeladen werden ist die Abfrageperformance
	schlechter, da auch Abfragen in das Filesystem durchgef�hrt werden m�ssen.
	*/
    private String page;

    private String limit;

    private String relevance;

    private String channelid;

    private String locationid;

    private String locationname;

    private String categoryid;

    private String country;

    private String provinceid;

    private String eventname;

    private String radius;

    private String longitude;

    private String latitude;

    private String order;

    private String orderdirection;

    private String loadflyers;

    private String place;

    /** 
	 * Serialisierbare Einstellungen f�r Eventkalender
	 * Beim Aufruf des Kalendars werden Standardeinstellungen konfiguertet. 
	 * Diese werden nach �nderung gespeichtert.
	 * Die Einstellungen sind NICHT Uservariabel.
	 * 
	 */
    public EventcalendarSettings() {
        page = "1";
        limit = "30";
        relevance = "false";
        radius = "20";
        channelid = "4";
        order = "eventname";
        orderdirection = "ASC";
        loadflyers = "false";
        place = "Linz";
    }

    /**
	 * 
	 * @return die Seite, die angezeigt werden soll (f�r Bl�tterfunktion)
	 */
    public String getPage() {
        return page;
    }

    /**
	 * 
	 * @param page die Seite, die angezeigt werden soll (f�r Bl�tterfunktion)
	 */
    public void setPage(String page) {
        this.page = page;
    }

    /**
	 * 
	 * @return  das Limit der Events, die pro Seite erscheinen (default 30, max. 100)
	 */
    public String getLimit() {
        return limit;
    }

    /**
	 * 
	 * @param limit das Limit der Events, die pro Seite erscheinen (default 30, max. 100)
	 */
    public void setLimit(String limit) {
        this.limit = limit;
    }

    /**
	 * 
	 * @return sollen relevante Events angezeigt werden
	 */
    public String getRelevance() {
        return relevance;
    }

    /**
	 * 
	 * @param relevance sollen relevante Events angezeigt werden
	 */
    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }

    /**
	 * 
	 * @return die ChannelId der Events
	 */
    public String getChannelid() {
        return channelid;
    }

    /**
	 * 
	 * @param channelid die ChannelId der Events
	 */
    public void setChannelid(String channelid) {
        this.channelid = channelid;
    }

    /**
	 * 
	 * @return die LocationId der Events
	 */
    public String getLocationid() {
        return locationid;
    }

    /**
	 * 
	 * @param locationid die LocationId der Events
	 */
    public void setLocationid(String locationid) {
        this.locationid = locationid;
    }

    /**
	 * 
	 * @return  der LocationName der Events
	 */
    public String getLocationname() {
        return locationname;
    }

    /**
	 * 
	 * @param locationname  der LocationName der Events
	 */
    public void setLocationname(String locationname) {
        this.locationname = locationname;
    }

    /**
	 * 
	 * @return die CategoryId der Events (f�r mehrere IDs diese mit �,� [Komma] trennen)
	 */
    public String getCategoryid() {
        return categoryid;
    }

    /**
	 * 
	 * @param categoryid die CategoryId der Events (f�r mehrere IDs diese mit �,� [Komma] trennen)
	 */
    public void setCategoryid(String categoryid) {
        this.categoryid = categoryid;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvinceid() {
        return provinceid;
    }

    public void setProvinceid(String provinceid) {
        this.provinceid = provinceid;
    }

    public String getEventname() {
        return eventname;
    }

    public void setEventname(String eventname) {
        this.eventname = eventname;
    }

    public String getRadius() {
        return radius;
    }

    public void setRadius(String radius) {
        this.radius = radius;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderdirection() {
        return orderdirection;
    }

    public void setOrderdirection(String orderdirection) {
        this.orderdirection = orderdirection;
    }

    public String getLoadflyers() {
        return loadflyers;
    }

    public void setLoadflyers(String loadflyers) {
        this.loadflyers = loadflyers;
    }

    /**
	 * 
	 * @return Adresse eines Ortes (Name, Postleitzahl, Stra�e, Hausnummer)
	 */
    public String getPlace() {
        return place;
    }

    /**
	 * 
	 * @param place Adresse eines Ortes (Name, Postleitzahl, Stra�e, Hausnummer)
	 */
    public void setPlace(String place) {
        this.place = place;
    }
}
