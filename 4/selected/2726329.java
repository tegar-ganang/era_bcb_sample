package foursquare4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import foursquare4j.exception.FoursquareException;
import foursquare4j.types.Categories;
import foursquare4j.types.CheckinResult;
import foursquare4j.types.Checkins;
import foursquare4j.types.Friends;
import foursquare4j.types.Requests;
import foursquare4j.types.Response;
import foursquare4j.types.Settings;
import foursquare4j.types.Tip;
import foursquare4j.types.TipsResult;
import foursquare4j.types.Todos;
import foursquare4j.types.User;
import foursquare4j.types.Users;
import foursquare4j.types.Venue;
import foursquare4j.types.Venues;
import foursquare4j.xml.handler.CategoriesHandler;
import foursquare4j.xml.handler.CheckinHandler;
import foursquare4j.xml.handler.CheckinsHandler;
import foursquare4j.xml.handler.FriendsHandler;
import foursquare4j.xml.handler.Handler;
import foursquare4j.xml.handler.HistoryHandler;
import foursquare4j.xml.handler.RequestsHandler;
import foursquare4j.xml.handler.ResponseHandler;
import foursquare4j.xml.handler.SettingsHandler;
import foursquare4j.xml.handler.TipHandler;
import foursquare4j.xml.handler.TipsResultHandler;
import foursquare4j.xml.handler.TodosHandler;
import foursquare4j.xml.handler.UserHandler;
import foursquare4j.xml.handler.UsersHandler;
import foursquare4j.xml.handler.VenueHandler;
import foursquare4j.xml.handler.VenuesHandler;

public abstract class FoursquareBase implements Foursquare {

    private static final Logger logger = Logger.getLogger(FoursquareBase.class.getName());

    protected static enum HttpMethod {

        POST, GET
    }

    protected String userAgent = Foursquare.USER_AGENT;

    public void setUserAgent(final CharSequence userAgent) {
        if (userAgent == null) throw new NullPointerException("userAgent is null.");
        this.userAgent = userAgent.toString();
    }

    public Checkins checkins(final CharSequence geolat, final CharSequence geolong) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        return execute(HttpMethod.GET, Foursquare.API_CHECKINS_URL, parameters, new CheckinsHandler());
    }

    public CheckinResult checkin(final CharSequence vid, final CharSequence venue, final CharSequence shout, final CharSequence _private, final CharSequence twitter, final CharSequence facebook, final CharSequence geolat, final CharSequence geolong) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        parameters.put("venue", venue);
        parameters.put("shout", shout);
        parameters.put("private", _private);
        parameters.put("twitter", twitter);
        parameters.put("facebook", facebook);
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        return execute(HttpMethod.POST, Foursquare.API_CHECKIN_URL, parameters, new CheckinHandler());
    }

    public Checkins history(final CharSequence l, final CharSequence sinceid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("l", l);
        parameters.put("sinceid", sinceid);
        return execute(HttpMethod.GET, Foursquare.API_HISTORY_URL, parameters, new HistoryHandler());
    }

    public User user(final CharSequence uid, final CharSequence twitter, final CharSequence badges, final CharSequence mayor) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("uid", uid);
        parameters.put("twitter", twitter);
        parameters.put("badges", badges);
        parameters.put("mayor", mayor);
        return execute(HttpMethod.GET, Foursquare.API_USER_URL, parameters, new UserHandler());
    }

    public Friends friends(final CharSequence uid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("uid", uid);
        return execute(HttpMethod.GET, Foursquare.API_FRIENDS_URL, parameters, new FriendsHandler());
    }

    public Venues venues(final CharSequence geolat, final CharSequence geolong, final CharSequence l, final CharSequence q) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        parameters.put("l", l);
        parameters.put("q", q);
        return execute(HttpMethod.GET, Foursquare.API_VENUES_URL, parameters, new VenuesHandler());
    }

    public Venue venue(final CharSequence vid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        return execute(HttpMethod.GET, Foursquare.API_VENUE_URL, parameters, new VenueHandler());
    }

    public Categories categories() throws FoursquareException {
        return execute(HttpMethod.GET, Foursquare.API_CATEGORIES_URL, new Parameters(), new CategoriesHandler());
    }

    public Venue addVenue(final CharSequence name, final CharSequence address, final CharSequence crossstreet, final CharSequence city, final CharSequence state, final CharSequence zip, final CharSequence phone, final CharSequence geolat, final CharSequence geolong, final CharSequence primarycategoryid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("name", name);
        parameters.put("address", address);
        parameters.put("crossstreet", crossstreet);
        parameters.put("city", city);
        parameters.put("state", state);
        parameters.put("zip", zip);
        parameters.put("phone", phone);
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        parameters.put("primarycategoryid", primarycategoryid);
        return execute(HttpMethod.POST, Foursquare.API_ADDVENUE_URL, parameters, new VenueHandler());
    }

    public Response proposeVenueEdit(final CharSequence vid, final CharSequence name, final CharSequence address, final CharSequence crossstreet, final CharSequence city, final CharSequence state, final CharSequence zip, final CharSequence phone, final CharSequence geolat, final CharSequence geolong) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        parameters.put("name", name);
        parameters.put("address", address);
        parameters.put("crossstreet", crossstreet);
        parameters.put("city", city);
        parameters.put("state", state);
        parameters.put("zip", zip);
        parameters.put("phone", phone);
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        return execute(HttpMethod.POST, Foursquare.API_PROPOSEEDIT_URL, parameters, new ResponseHandler());
    }

    public Response flagVenueAsMislocated(final CharSequence vid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        return execute(HttpMethod.POST, Foursquare.API_FLAGMISLOCATED_URL, parameters, new ResponseHandler());
    }

    public Response flagVenueAsDuplicate(final CharSequence vid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        return execute(HttpMethod.POST, Foursquare.API_FLAGDUPLICATE_URL, parameters, new ResponseHandler());
    }

    public Response flagVenueAsClosed(final CharSequence vid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        return execute(HttpMethod.POST, Foursquare.API_FLAGCLOSED_URL, parameters, new ResponseHandler());
    }

    public TipsResult tips(final CharSequence geolat, final CharSequence geolong, final CharSequence filter, final CharSequence uid, final CharSequence sort, final CharSequence l) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        parameters.put("filter", filter);
        parameters.put("uid", uid);
        parameters.put("sort", sort);
        parameters.put("l", l);
        return execute(HttpMethod.GET, Foursquare.API_TIPS_URL, parameters, new TipsResultHandler());
    }

    public Tip addTip(final CharSequence vid, final CharSequence text, final CharSequence geolat, final CharSequence geolong) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("vid", vid);
        parameters.put("text", text);
        parameters.put("type", "tip");
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        return execute(HttpMethod.POST, Foursquare.API_ADDTIP_URL, parameters, new TipHandler());
    }

    public Tip markTipAsTodo(final CharSequence tid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("tid", tid);
        return execute(HttpMethod.POST, Foursquare.API_TIP_MARKTODO_URL, parameters, new TipHandler());
    }

    public Tip markTipAsDone(final CharSequence tid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("tid", tid);
        return execute(HttpMethod.POST, Foursquare.API_TIP_MARKDONE_URL, parameters, new TipHandler());
    }

    public Tip unmarkTip(final CharSequence tid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("tid", tid);
        return execute(HttpMethod.POST, Foursquare.API_TIP_UNMARK_URL, parameters, new TipHandler());
    }

    public Tip getTipDetails(final CharSequence tid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("tid", tid);
        return execute(HttpMethod.POST, Foursquare.API_TIP_DETAIL_URL, parameters, new TipHandler());
    }

    public Todos todos(final CharSequence geolat, final CharSequence geolong, final CharSequence uid, final CharSequence sort, final CharSequence l) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("geolat", geolat);
        parameters.put("geolong", geolong);
        parameters.put("uid", uid);
        parameters.put("sort", sort);
        parameters.put("l", l);
        return execute(HttpMethod.POST, Foursquare.API_TODOS_URL, parameters, new TodosHandler());
    }

    public Requests pendingFriendRequests() throws FoursquareException {
        return execute(HttpMethod.GET, Foursquare.API_FRIEND_REQUESTS_URL, new Parameters(), new RequestsHandler());
    }

    public User approveFriendRequest(final CharSequence uid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("uid", uid);
        return execute(HttpMethod.POST, Foursquare.API_FRIEND_APPROVE_URL, parameters, new UserHandler());
    }

    public User denyFriendRequest(final CharSequence uid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("uid", uid);
        return execute(HttpMethod.POST, Foursquare.API_FRIEND_DENY_URL, parameters, new UserHandler());
    }

    public User sendFriendRequest(final CharSequence uid) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("uid", uid);
        return execute(HttpMethod.POST, Foursquare.API_FRIEND_SENDREQUEST_URL, parameters, new UserHandler());
    }

    public Users findFriendsByName(final CharSequence q) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("q", q);
        return execute(HttpMethod.GET, Foursquare.API_FINDFRIENDS_BYNAME_URL, parameters, new UsersHandler());
    }

    public Users findFriendsByPhone(final CharSequence q) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("q", q);
        return execute(HttpMethod.GET, Foursquare.API_FINDFRIENDS_BYPHONE_URL, parameters, new UsersHandler());
    }

    public Users findFriendsByTwitter(final CharSequence q) throws FoursquareException {
        final Parameters parameters = new Parameters();
        parameters.put("q", q);
        return execute(HttpMethod.GET, Foursquare.API_FINDFRIENDS_BYTWITTER_URL, parameters, new UsersHandler());
    }

    public Settings setPings(final CharSequence uid, final CharSequence pings) throws FoursquareException {
        final Parameters parameters = new Parameters();
        if (uid == null) parameters.put(null, pings); else parameters.put(uid.toString(), pings);
        return execute(HttpMethod.POST, Foursquare.API_SETTINGS_SETPINGS_URL, parameters, new SettingsHandler());
    }

    protected abstract <T> T execute(final HttpMethod method, final String url, final Parameters parameters, final Handler<T> handler) throws FoursquareException;

    protected <T> T parseBody(InputStream body, final Handler<T> handler) throws ParserConfigurationException, SAXException, IOException {
        if (body == null) return null;
        if (logger.isLoggable(Level.FINE)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buff = new byte[1024];
            int size = 0;
            while ((size = body.read(buff, 0, buff.length)) > -1) baos.write(buff, 0, size);
            final byte[] data = baos.toByteArray();
            logger.fine(new String(data, "UTF-8"));
            body = new ByteArrayInputStream(data);
        }
        final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(body, handler);
        return handler.getObject();
    }

    protected static class Parameters extends LinkedHashMap<String, String> {

        private static final long serialVersionUID = 1L;

        public String put(final String key, final CharSequence value) {
            if (value == null) return null;
            return put(key, value.toString());
        }

        @Override
        public String put(final String key, final String value) {
            if (value == null) return null;
            return super.put(key, value);
        }
    }
}
