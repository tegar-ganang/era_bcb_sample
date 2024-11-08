package net.sf.sageplugins.webserver;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import sage.SageTV;
import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.Translate;

/**
 * @author Owner
 *
 */
public class Favorite {

    static final String[] RATINGS = new String[] { "G", "PG", "PG-13", "R", "NC-17", "AO", "NR" };

    static final String[] PARENTAL_RATINGS = new String[] { "TVY", "TVY7", "TVG", "TVPG", "TV14", "TVM" };

    static final String[] DAYS = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

    static final String[] TIMES_AM_PM = new String[] { "12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM", "11AM", "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM", "11PM" };

    static final String[] TIMES_24 = new String[] { "0:00", "1:00", "2:00", "3:00", "4:00", "5:00", "6:00", "7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00" };

    private Object sageFavorite;

    private int id;

    Favorite(HttpServletRequest req) throws Exception {
        id = -1;
        sageFavorite = null;
        String idStr = req.getParameter("FavoriteId");
        if (idStr != null) {
            try {
                id = Integer.parseInt(idStr);
                sageFavorite = SageApi.Api("GetFavoriteForID", new Object[] { new Integer(id) });
            } catch (NumberFormatException e) {
                id = -1;
                throw new IllegalArgumentException("Favorite Id '" + id + "' not valid");
            }
        }
        if (sageFavorite == null) {
            throw new IllegalArgumentException("Favorite for Id '" + id + "' not found");
        }
    }

    Favorite(int id) throws Exception {
        sageFavorite = SageApi.Api("GetFavoriteForID", new Object[] { new Integer(id) });
        if (sageFavorite == null) {
            throw new IllegalArgumentException("Favorite for Id '" + id + "' not found");
        }
        this.id = id;
    }

    public Favorite(Object sageFavorite) throws Exception {
        if (!SageApi.booleanApi("IsFavoriteObject", new Object[] { sageFavorite })) {
            throw new IllegalArgumentException("Object is not a favorite.");
        }
        this.sageFavorite = sageFavorite;
        this.id = SageApi.IntApi("GetFavoriteID", new Object[] { sageFavorite });
    }

    /** Returns the object to be passed to the Sage API */
    public Object getSageFavorite() {
        return sageFavorite;
    }

    /** Swap the priority of consecutive favorites */
    public static void createFavoritePriority(int higherPriorityFavoriteId, int lowerPriorityFavoriteId) throws Exception {
        createFavoritePriority(new Favorite(higherPriorityFavoriteId), new Favorite(lowerPriorityFavoriteId));
    }

    /** Swap the priority of consecutive favorites */
    public static void createFavoritePriority(Favorite higherPriorityFavorite, Favorite lowerPriorityFavorite) throws Exception {
        SageApi.Api("CreateFavoritePriority", new Object[] { higherPriorityFavorite.getSageFavorite(), lowerPriorityFavorite.getSageFavorite() });
    }

    /** Move a favorite above or below another favorite.  They do not have to be consecutive favorites. */
    public static void createFavoritePriority(int higherPriorityFavoriteId, int lowerPriorityFavoriteId, boolean moveHigherPriorityFavorite) throws Exception {
        createFavoritePriority(new Favorite(higherPriorityFavoriteId), new Favorite(lowerPriorityFavoriteId), moveHigherPriorityFavorite);
    }

    /** Move a favorite above or below another favorite.  They do not have to be consecutive favorites. */
    public static void createFavoritePriority(Favorite higherPriorityFavorite, Favorite lowerPriorityFavorite, boolean moveHigherPriorityFavorite) throws Exception {
        int higherPriorityFavoriteID = higherPriorityFavorite.getID();
        int lowerPriorityFavoriteID = lowerPriorityFavorite.getID();
        if (higherPriorityFavoriteID == lowerPriorityFavoriteID) {
            throw new IllegalArgumentException("Higher priority favorite is equal to lower priority favorite.");
        }
        int higherPriorityFavoriteIndex = 0;
        int lowerPriorityFavoriteIndex = 0;
        Object favoritesList = SageApi.Api("GetFavorites");
        favoritesList = SageApi.Api("Sort", new Object[] { favoritesList, Boolean.FALSE, "FavoritePriority" });
        for (int i = 0; i < SageApi.Size(favoritesList); i++) {
            int currentFavoriteID = SageApi.IntApi("GetFavoriteID", new Object[] { SageApi.GetElement(favoritesList, i) });
            if (higherPriorityFavoriteID == currentFavoriteID) {
                higherPriorityFavoriteIndex = i;
            }
            if (lowerPriorityFavoriteID == currentFavoriteID) {
                lowerPriorityFavoriteIndex = i;
            }
        }
        if ((higherPriorityFavoriteIndex - lowerPriorityFavoriteIndex) == -1) {
            return;
        }
        if (moveHigherPriorityFavorite) {
            if (higherPriorityFavoriteIndex < lowerPriorityFavoriteIndex) {
                for (int i = higherPriorityFavoriteIndex + 1; i < lowerPriorityFavoriteIndex; i++) {
                    Favorite currentFavorite = new Favorite(SageApi.GetElement(favoritesList, i));
                    createFavoritePriority(currentFavorite, higherPriorityFavorite);
                }
            } else {
                for (int i = higherPriorityFavoriteIndex - 1; i >= lowerPriorityFavoriteIndex; i--) {
                    Favorite currentFavorite = new Favorite(SageApi.GetElement(favoritesList, i));
                    createFavoritePriority(higherPriorityFavorite, currentFavorite);
                }
            }
        } else {
            if (higherPriorityFavoriteIndex > lowerPriorityFavoriteIndex) {
                for (int i = lowerPriorityFavoriteIndex + 1; i <= higherPriorityFavoriteIndex; i++) {
                    Favorite currentFavorite = new Favorite(SageApi.GetElement(favoritesList, i));
                    createFavoritePriority(currentFavorite, lowerPriorityFavorite);
                }
            } else {
                for (int i = lowerPriorityFavoriteIndex - 1; i > higherPriorityFavoriteIndex; i--) {
                    Favorite currentFavorite = new Favorite(SageApi.GetElement(favoritesList, i));
                    createFavoritePriority(lowerPriorityFavorite, currentFavorite);
                }
            }
        }
    }

    /** Increase favorite priority by one */
    public int increasePriority() throws Exception {
        int higherPriorityFavoriteID = getID();
        int lowerPriorityFavoriteID = 0;
        Object favoritesList = SageApi.Api("GetFavorites");
        favoritesList = SageApi.Api("Sort", new Object[] { favoritesList, Boolean.FALSE, "FavoritePriority" });
        for (int i = 0; i < SageApi.Size(favoritesList); i++) {
            int currentFavoriteID = SageApi.IntApi("GetFavoriteID", new Object[] { SageApi.GetElement(favoritesList, i) });
            if (higherPriorityFavoriteID == currentFavoriteID) {
                if (i == 0) {
                    return 0;
                }
                lowerPriorityFavoriteID = SageApi.IntApi("GetFavoriteID", new Object[] { SageApi.GetElement(favoritesList, i - 1) });
                createFavoritePriority(higherPriorityFavoriteID, lowerPriorityFavoriteID);
                return lowerPriorityFavoriteID;
            }
        }
        throw new IllegalArgumentException("Favorite id " + getID() + " not found.");
    }

    /** Decrease favorite priority by one */
    public int decreasePriority() throws Exception {
        int higherPriorityFavoriteID = 0;
        int lowerPriorityFavoriteID = getID();
        Object favoritesList = SageApi.Api("GetFavorites");
        favoritesList = SageApi.Api("Sort", new Object[] { favoritesList, Boolean.FALSE, "FavoritePriority" });
        for (int i = 0; i < SageApi.Size(favoritesList); i++) {
            int currentFavoriteID = SageApi.IntApi("GetFavoriteID", new Object[] { SageApi.GetElement(favoritesList, i) });
            if (lowerPriorityFavoriteID == currentFavoriteID) {
                if (i == SageApi.Size(favoritesList) - 1) {
                    return 0;
                }
                higherPriorityFavoriteID = SageApi.IntApi("GetFavoriteID", new Object[] { SageApi.GetElement(favoritesList, i + 1) });
                createFavoritePriority(higherPriorityFavoriteID, lowerPriorityFavoriteID);
                return higherPriorityFavoriteID;
            }
        }
        throw new IllegalArgumentException("Favorite id " + getID() + " not found.");
    }

    /** Returns a list of all of the Airings in the database that match this Favorite. */
    public Object getSageAirings() throws Exception {
        return SageApi.Api("GetFavoriteAirings", new Object[] { sageFavorite });
    }

    /** Returns a list of all of the Airings in the database that match this Favorite. */
    public Airing[] getAirings() throws Exception {
        Object sageAirings = getSageAirings();
        int size = SageApi.Size(sageAirings);
        Airing[] airings = new Airing[size];
        for (int i = 0; i < size; i++) {
            Object sageAiring = SageApi.GetElement(sageAirings, i);
            airings[i] = new Airing(sageAiring);
        }
        return airings;
    }

    /** Returns the category that an Airing must match to be included in this Favorite. */
    public String getCategory() throws Exception {
        String category = SageApi.StringApi("GetFavoriteCategory", new Object[] { sageFavorite });
        return (category != null) ? category : "";
    }

    /** Returns the channel name (call sign) that an Airing must be on to be included in this Favorite. */
    public String getChannel() throws Exception {
        String channel = SageApi.StringApi("GetFavoriteChannel", new Object[] { sageFavorite });
        return (channel != null) ? channel : "";
    }

    /** Returns a list of the channel names (call sign) that an Airing must be on to be included in this Favorite.
      * Supports multi-channel favorites.
      */
    public List<String> getChannels() throws Exception {
        String channels = SageApi.StringApi("GetFavoriteChannel", new Object[] { sageFavorite });
        String[] channelArray = channels.split("[;,]");
        List<String> channelList = new ArrayList<String>();
        for (String channel : channelArray) {
            if ((channel != null) && (channel.trim().length() > 0)) {
                channelList.add(channel);
            }
        }
        return channelList;
    }

    /** Returns the day that matches this Favorite if one exists. */
    public String getDay() throws Exception {
        String timeslot = getTimeslot();
        if ((timeslot == null) || (timeslot.trim().equals(""))) {
            return "";
        }
        String[] timeslotArray = timeslot.split(" ");
        if (timeslotArray.length == 1) {
            if (Arrays.asList(DAYS).contains(timeslotArray[0])) {
                return timeslotArray[0];
            }
        } else if (timeslotArray.length == 2) {
            return timeslotArray[0];
        }
        return "";
    }

    /** Returns a String that describes this Favorite. */
    public String getDescription() throws Exception {
        String description = SageApi.StringApi("GetFavoriteDescription", new Object[] { sageFavorite });
        return (description != null) ? description : "";
    }

    /** Gets the file format to which the Favorite recording will be converted. */
    public String GetFavoriteAutomaticConversionFormat() throws Exception {
        String format = SageApi.StringApi("GetFavoriteAutomaticConversionFormat", new Object[] { sageFavorite });
        return (format != null) ? format : "";
    }

    /** Gets the folder where the converted Favorite recording will be stored. */
    public File GetFavoriteAutomaticConversionDestination() throws Exception {
        File destination = (File) SageApi.Api("GetFavoriteAutomaticConversionDestination", new Object[] { sageFavorite });
        return destination;
    }

    /** Gets the Favorite that matches this Airing if one exists. */
    public static Favorite getFavoriteForAiring(Airing airing) throws Exception {
        Object sageFavorite = SageApi.Api("GetFavoriteForAiring", new Object[] { airing });
        return new Favorite(sageFavorite);
    }

    /** Gets the Favorite object with the corresponding ID from the database. */
    public static Favorite getFavoriteForID(int id) throws Exception {
        Object sageFavorite = SageApi.Api("GetFavoriteForID", new Object[] { new Integer(id) });
        return new Favorite(sageFavorite);
    }

    /** Gets a unique ID for this Favorite which can be used with GetFavoriteForID() for retrieving the object later. */
    public int getID() {
        return id;
    }

    /** Returns the maximum number of recordings that match this Favorite that should be kept on disk. */
    public int getKeepAtMost() throws Exception {
        return SageApi.IntApi("GetKeepAtMost", new Object[] { sageFavorite });
    }

    /** Returns the keyword string that an Airing must match to be included in this Favorite. */
    public String getKeyword() throws Exception {
        String keyword = SageApi.StringApi("GetFavoriteKeyword", new Object[] { sageFavorite });
        return (keyword != null) ? keyword : "";
    }

    /** Returns the network name that an Airing must be on to be included in this Favorite. */
    public String getNetwork() throws Exception {
        String network = SageApi.StringApi("GetFavoriteNetwork", new Object[] { sageFavorite });
        return (network != null) ? network : "";
    }

    /** Returns the parental rating that an Airing must match to be included in this Favorite. */
    public String getParentalRating() throws Exception {
        String parentalRating = SageApi.StringApi("GetFavoriteParentalRating", new Object[] { sageFavorite });
        return (parentalRating != null) ? parentalRating : "";
    }

    /** Returns the person that an Airing must have to be included in this Favorite. */
    public String getPerson() throws Exception {
        String person = SageApi.StringApi("GetFavoritePerson", new Object[] { sageFavorite });
        return (person != null) ? person : "";
    }

    /** Returns the role that an Airing must have the Favorite Person in to be included in this Favorite. */
    public String getPersonRole() throws Exception {
        String personRole = SageApi.StringApi("GetFavoritePersonRole", new Object[] { sageFavorite });
        return (personRole != null) ? personRole : "";
    }

    /** Gets the name of the recording quality that should be used when recording this Favorite. */
    public String getQuality() throws Exception {
        String quality = SageApi.StringApi("GetFavoriteQuality", new Object[] { sageFavorite });
        return (quality != null) ? quality : "";
    }

    public String getRated() throws Exception {
        String rated = SageApi.StringApi("GetFavoriteRated", new Object[] { sageFavorite });
        return (rated != null) ? rated : "";
    }

    /** Returns the amount of time any recording for this Favorite should start before the actual Airing begins. */
    public long getStartPadding() throws Exception {
        return ((Long) SageApi.Api("GetStartPadding", new Object[] { sageFavorite })).longValue();
    }

    /** Returns the amount of time any recording for this Favorite should stop after the actual Airing ends. */
    public long getStopPadding() throws Exception {
        return ((Long) SageApi.Api("GetStopPadding", new Object[] { sageFavorite })).longValue();
    }

    /** Returns the subcategory that an Airing must match to be included in this Favorite. */
    public String getSubCategory() throws Exception {
        String subCategory = SageApi.StringApi("GetFavoriteSubCategory", new Object[] { sageFavorite });
        return (subCategory != null) ? subCategory : "";
    }

    /** Returns the time that matches this Favorite if one exists. */
    public String getTime() throws Exception {
        String timeslot = getTimeslot();
        if ((timeslot == null) || (timeslot.trim().equals(""))) {
            return "";
        }
        String[] timeslotArray = timeslot.split(" ");
        if (timeslotArray.length == 1) {
            if (!Arrays.asList(DAYS).contains(timeslotArray[0])) {
                return timeslotArray[0];
            }
        } else if (timeslotArray.length == 2) {
            return timeslotArray[1];
        }
        return "";
    }

    /** Returns a timeslot given a day and time */
    public static String getTimeslot(String day, String time) throws Exception {
        String timeslot = "";
        day = (day == null) ? "" : day.trim();
        time = (time == null) ? "" : time.trim();
        if ((!day.equals("")) && (!Arrays.asList(DAYS).contains(day))) {
            throw new IllegalArgumentException("Invalid value for day parameter: '" + day + "'.");
        }
        if ((!time.equals("")) && (!Arrays.asList(TIMES_AM_PM).contains(time)) && (!Arrays.asList(TIMES_24).contains(time))) {
            throw new IllegalArgumentException("Invalid value for time parameter: '" + time + "'.");
        }
        if ((day.length() > 0) && (time.length() > 0)) {
            timeslot = day + " " + time;
        } else if (day.length() > 0) {
            timeslot = day;
        } else if (time.length() > 0) {
            timeslot = time;
        }
        return timeslot;
    }

    /** Returns the timeslot that an Airing must be in to be included in this Favorite. */
    public String getTimeslot() throws Exception {
        String timeslot = SageApi.StringApi("GetFavoriteTimeslot", new Object[] { sageFavorite });
        return (timeslot != null) ? timeslot : "";
    }

    /** Returns the title that an Airing must match to be included in this Favorite. */
    public String getTitle() throws Exception {
        String title = SageApi.StringApi("GetFavoriteTitle", new Object[] { sageFavorite });
        return (title != null) ? title : "";
    }

    /** Returns the year that an Airing must match to be included in this Favorite. */
    public String getYear() throws Exception {
        String year = SageApi.StringApi("GetFavoriteYear", new Object[] { sageFavorite });
        return (year != null) ? year : "";
    }

    /** Returns true if this Favorite is SageTV is allowed to automatically delete recordings of this Favorite when it needs more disk space. */
    public boolean isAutoDelete() throws Exception {
        return SageApi.booleanApi("IsAutoDelete", new Object[] { sageFavorite });
    }

    /** Returns true if automatic conversions are supported.  Requires SageTV 7 or higher */
    public boolean isAutomaticConversionSupported() throws Exception {
        boolean isFavoriteAutoConversionSupported = true;
        try {
            SageApi.booleanApi("IsDeleteAfterAutomaticConversion", new Object[] { sageFavorite });
        } catch (InvocationTargetException e) {
            isFavoriteAutoConversionSupported = false;
        }
        return isFavoriteAutoConversionSupported;
    }

    /** Returns true if this Favorite is configured to delete the original file after it's automatically converted */
    public boolean IsDeleteAfterAutomaticConversion() throws Exception {
        return SageApi.booleanApi("IsDeleteAfterAutomaticConversion", new Object[] { sageFavorite });
    }

    /** Returns true if this Favorite is configured to record first runs (it may or may not record reruns) */
    public boolean isFirstRuns() throws Exception {
        return SageApi.booleanApi("IsFirstRuns", new Object[] { sageFavorite });
    }

    /** Returns true if this Favorite is configured to record both first runs and reruns. */
    public boolean isFirstRunsAndReRuns() throws Exception {
        return SageApi.booleanApi("IsFirstRunsAndReRuns", new Object[] { sageFavorite });
    }

    /** Returns true if this Favorite is configured to record first runs but not reruns. */
    public boolean isFirstRunsOnly() throws Exception {
        return SageApi.booleanApi("IsFirstRunsOnly", new Object[] { sageFavorite });
    }

    /** Returns true if this Favorite is configured to record reruns (it may or may not record first runs) */
    public boolean isReRuns() throws Exception {
        return SageApi.booleanApi("IsReRuns", new Object[] { sageFavorite });
    }

    /** Returns true if this Favorite is configured to record reruns but not first runs. */
    public boolean isReRunsOnly() throws Exception {
        return SageApi.booleanApi("IsReRunsOnly", new Object[] { sageFavorite });
    }

    /** Remove a favorite given its id. */
    public static void remove(int id) throws Exception {
        remove(new Favorite(id));
    }

    /** Remove a favorite. */
    public static void remove(Favorite favorite) throws Exception {
        SageApi.Api("RemoveFavorite", new Object[] { favorite.getSageFavorite() });
    }

    /** Save the favorite */
    public static Favorite save(int id, String category, String subCategory, String keyword, String person, String roleForPerson, String title, Boolean firstRunsOnly, Boolean reRunsOnly, String channels, String network, String autoDelete, String keepAtMost, String startPad, String startPadOffsetType, String endPad, String endPadOffsetType, String quality, boolean isAutoConvert, boolean isDeleteAfterFavoriteAutomaticConversion, String favoriteAutomaticConversionFormat, boolean isFavoriteAutomaticConversionDestinationOriginalDir, String favoriteAutomaticConversionDestination, String parentalRating, String rated, String year, String day, String time, String favoritePriorityRelation, String relativeFavoriteId) throws Exception {
        if ((title == null || title.length() == 0) && (category == null || category.length() == 0) && (person == null || person.length() == 0) && (keyword == null || keyword.length() == 0)) {
            throw new IllegalArgumentException("Must specify one of title, category, person or keyword");
        }
        Object sageFavorite = null;
        if (id != -1) sageFavorite = SageApi.Api("GetFavoriteForID", new Object[] { new Integer(id) });
        boolean isAutomaticConversionSupported = true;
        try {
            SageTV.api("GetFavoriteAutomaticConversionFormat", new Object[] { sageFavorite });
        } catch (InvocationTargetException e) {
            isAutomaticConversionSupported = false;
        }
        if (isAutomaticConversionSupported && isAutoConvert && !isFavoriteAutomaticConversionDestinationOriginalDir) {
            if ((favoriteAutomaticConversionDestination == null) || favoriteAutomaticConversionDestination.trim().length() == 0) {
                throw new IllegalArgumentException("Must specify conversion destination folder");
            }
            File favoriteAutomaticConversionDestinationFile = new File(favoriteAutomaticConversionDestination);
            if (!favoriteAutomaticConversionDestinationFile.exists()) {
                throw new IllegalArgumentException("Conversion destination folder does not exist");
            }
            if (!favoriteAutomaticConversionDestinationFile.canWrite()) {
                throw new IllegalArgumentException("Unable to write to conversion destination folder");
            }
        }
        if (sageFavorite == null) {
            sageFavorite = SageApi.Api("AddFavorite", new Object[] { title, firstRunsOnly, reRunsOnly, category, subCategory, person, roleForPerson, rated, year, parentalRating, network, null, Favorite.getTimeslot(day, time), keyword });
        }
        SageApi.Api("UpdateFavorite", new Object[] { sageFavorite, title, firstRunsOnly, reRunsOnly, category, subCategory, person, roleForPerson, rated, year, parentalRating, network, channels, Favorite.getTimeslot(day, time), keyword });
        Favorite favorite = new Favorite(sageFavorite);
        if (favoritePriorityRelation != null && !favoritePriorityRelation.equals("default") && relativeFavoriteId != null && relativeFavoriteId.length() > 0) {
            if (favoritePriorityRelation.equals("Above")) {
                createFavoritePriority(favorite.getID(), Integer.parseInt(relativeFavoriteId), true);
            } else if (favoritePriorityRelation.equals("Below")) {
                createFavoritePriority(Integer.parseInt(relativeFavoriteId), favorite.getID(), false);
            }
        }
        long startPadLong = Long.parseLong(startPad) * 60000;
        if (startPadOffsetType.equals("Later")) {
            startPadLong = -startPadLong;
        }
        SageApi.Api("SetStartPadding", new Object[] { sageFavorite, new Long(startPadLong) });
        long endPadLong = Long.parseLong(endPad) * 60000;
        if (endPadOffsetType.equals("Earlier")) {
            endPadLong = -endPadLong;
        }
        SageApi.Api("SetStopPadding", new Object[] { sageFavorite, new Long(endPadLong) });
        boolean autoDeleteBoolean = new Boolean(autoDelete).booleanValue();
        SageApi.Api("SetDontAutodelete", new Object[] { sageFavorite, new Boolean(!autoDeleteBoolean) });
        int keepAtMostInteger = new Integer(keepAtMost).intValue();
        SageApi.Api("SetKeepAtMost", new Object[] { sageFavorite, new Integer(keepAtMostInteger) });
        SageApi.Api("SetFavoriteQuality", new Object[] { sageFavorite, quality });
        if (favorite.isAutomaticConversionSupported()) {
            if (isAutoConvert && (favoriteAutomaticConversionFormat != null) && (favoriteAutomaticConversionFormat.trim().length() > 0)) {
                SageApi.Api("SetFavoriteAutomaticConversionFormat", new Object[] { sageFavorite, favoriteAutomaticConversionFormat });
                SageApi.Api("SetDeleteAfterAutomaticConversion", new Object[] { sageFavorite, isDeleteAfterFavoriteAutomaticConversion });
                if (isFavoriteAutomaticConversionDestinationOriginalDir) {
                    SageApi.Api("SetFavoriteAutomaticConversionDestination", new Object[] { sageFavorite, null });
                } else {
                    SageApi.Api("SetFavoriteAutomaticConversionDestination", new Object[] { sageFavorite, favoriteAutomaticConversionDestination });
                }
                SageApi.Api("SetProperty", new Object[] { "transcoder/last_replace_choice", (isDeleteAfterFavoriteAutomaticConversion ? "xKeepOnlyConversion" : "xKeepBoth") });
                String[] formatArr = favoriteAutomaticConversionFormat.split("-", 2);
                SageApi.Api("SetProperty", new Object[] { "transcoder/last_format_name", formatArr[0] });
                SageApi.Api("SetProperty", new Object[] { "transcoder/last_format_quality/" + formatArr[0], favoriteAutomaticConversionFormat });
                if (isFavoriteAutomaticConversionDestinationOriginalDir) {
                    SageApi.Api("SetProperty", new Object[] { "transcoder/last_dest_dir", null });
                } else {
                    File destDir = new File(favoriteAutomaticConversionDestination);
                    SageApi.Api("SetProperty", new Object[] { "transcoder/last_dest_dir", destDir.getAbsolutePath() });
                }
            } else {
                SageApi.Api("SetFavoriteAutomaticConversionFormat", new Object[] { sageFavorite, "" });
                SageApi.Api("SetDeleteAfterAutomaticConversion", new Object[] { sageFavorite, Boolean.FALSE });
            }
        }
        return favorite;
    }

    public void printFavoriteTableCell(PrintWriter out, Favorite previousFavorite, Favorite currentFavorite, Favorite nextFavorite, boolean hascheckbox, boolean showfulldescription, boolean showfirstrunsandreruns, boolean showautodelete, boolean showkeepatmost, boolean showpadding, boolean showquality, boolean showratings, boolean showtimeslot, boolean showchannels, boolean usechannellogos, int index) throws Exception {
        String tdclass = "";
        out.println("    <a name=\"FavID" + id + "\"/><table width=\"100%\" class=\"epgcell\">");
        out.println("    <tr>");
        out.println("        <td class=\"showfavorite\">");
        out.println("        <table width=\"100%\" class=\"show_other\">");
        out.println("        <tr>");
        if (hascheckbox) {
            out.print("            <td class=\"checkbox\"><input type=\"checkbox\" name=\"\"");
            out.print("FavoriteId");
            out.println("\" value=\"" + id + "\"/></td>");
        }
        out.println("            <td class=\"titlecell " + tdclass + "\">");
        out.println("            <div class=\"" + tdclass + "\">");
        out.print("                <a href=\"FavoriteDetails?" + getIdArg() + "\">");
        if (index > 0) out.print(index + ".&nbsp;");
        if (showfulldescription) {
            out.print(Translate.encode(getDescription()));
        } else {
            String title = getTitle();
            String category = getCategory();
            String keyword = getKeyword();
            String person = getPerson();
            if ((title != null) && (title.trim().length() > 0)) {
                out.print(Translate.encode(title));
            }
            if ((category != null) && (category.trim().length() > 0)) {
                out.print(Translate.encode(category));
            }
            if ((keyword != null) && (keyword.trim().length() > 0)) {
                out.print(Translate.encode("[" + keyword + "]"));
            }
            if ((person != null) && (person.trim().length() > 0)) {
                out.print(Translate.encode(person));
            }
        }
        out.println("</a>");
        out.println("            </div>");
        out.println("            </td>");
        if (showfirstrunsandreruns) {
            out.println("            <td class=\"favruncell\"><div class=\"" + tdclass + "\">");
            if (isFirstRunsOnly()) {
                out.println("                <img src=\"RecordFavFirst.gif\" alt=\"Favorite - First Runs Only\" title=\"Favorite - First Runs Only\"/>");
            } else if (isReRunsOnly()) {
                out.println("                <img src=\"RecordFavRerun.gif\" alt=\"Favorite - Reruns Only\" title=\"Favorite - Reruns Only\"/>");
            } else {
                out.println("                <img src=\"RecordFavAll.gif\" alt=\"Favorite - First Runs and Reruns\" title=\"Favorite - First Runs and Reruns\"/>");
            }
            out.println("            </div></td>");
        }
        if (showautodelete) {
            out.println("            <td class=\"autodeletecell\"><div class=\"" + tdclass + "\">");
            if (isAutoDelete()) {
                out.println("                <img src=\"MarkerDelete.gif\" alt=\"Auto Delete\" title=\"Auto Delete\"/>");
            }
            out.println("            </div></td>");
        }
        if (showkeepatmost) {
            out.println("            <td class=\"keepatmostcell\"><div class=\"" + tdclass + "\">");
            out.println("                Keep<br/> " + (getKeepAtMost() == 0 ? "All" : Integer.toString(getKeepAtMost())));
            out.println("            </div></td>");
        }
        if (showpadding) {
            long startPadding = getStartPadding() / 60000;
            long stopPadding = getStopPadding() / 60000;
            String startPaddingString = ((startPadding > 0) ? "+" : "") + Long.toString(startPadding);
            String stopPaddingString = ((stopPadding > 0) ? "+" : "") + Long.toString(stopPadding);
            String tooltip = "";
            tooltip = Math.abs(startPadding) + " minute" + (Math.abs(startPadding) != 1 ? "s" : "") + (startPadding < 0 ? " later.  " : " earlier.  ");
            tooltip += Math.abs(stopPadding) + " minute" + (Math.abs(stopPadding) != 1 ? "s" : "") + (stopPadding < 0 ? " earlier." : " later.");
            out.println("            <td class=\"paddingcell\"><div class=\"" + tdclass + "\" title=\"" + Translate.encode(tooltip) + "\">");
            out.println("                Pad<br/> " + Translate.encode(startPaddingString) + "/" + Translate.encode(stopPaddingString));
            out.println("            </div></td>");
        }
        if (showquality) {
            out.println("            <td class=\"qualitycell\"><div class=\"" + tdclass + "\">");
            String qualityName = getQuality();
            RecordingQuality quality = new RecordingQuality(qualityName);
            if ((qualityName == null) || (qualityName.equals("Default")) || (qualityName.trim().length() == 0)) {
                quality = RecordingQuality.getDefaultRecordingQuality();
                qualityName = quality.getName();
                out.println("                <span title=\"" + quality.getName() + " - " + quality.getFormat() + " @ " + quality.getGigabytesPerHour() + " GB/hr\">Default</span>");
            } else {
                out.println("                " + quality.getName() + "<br/>" + quality.getFormat() + " @ " + quality.getGigabytesPerHour() + " GB/hr");
            }
            out.println("            </div></td>");
        }
        if (showratings) {
            String rating = getParentalRating();
            out.println("            <td class=\"ratingcell\"><div class=\"" + tdclass + "\">");
            if ((rating != null) && (rating.length() > 0)) {
                out.println("                <img src=\"Rating_" + rating + ".gif\" alt=\"" + rating + "\" title=\"" + rating + "\"/>");
            }
            out.println("            </div></td>");
            String rated = getRated();
            out.println("            <td class=\"ratedcell\"><div class=\"" + tdclass + "\">");
            if ((rated != null) && (rated.length() > 0)) {
                out.println("                <img src=\"Rating_" + rated + ".gif\" alt=\"" + rated + "\" title=\"" + rated + "\"/>");
            }
            out.println("            </div></td>");
        }
        if (showtimeslot) {
            out.println("            <td class=\"dayhourcell\"><div class=\"" + tdclass + "\">");
            out.println("                " + Translate.encode(getDay()) + "<br/>" + Translate.encode(getTime()));
            out.println("            </div></td>");
        }
        if (showchannels) {
            List<String> favoriteChannelNameList = getChannels();
            out.println("            <td class=\"favchannelcell\"><div class=\"" + tdclass + "\">");
            if ((favoriteChannelNameList != null) && (favoriteChannelNameList.size() == 1)) {
                Object channels = SageApi.Api("GetAllChannels");
                int channelCount = SageApi.Size(channels);
                Object channel = null;
                String channelName = "";
                String channelNumber = "";
                int channelId = 0;
                for (int i = 0; i < channelCount; i++) {
                    Object currentChannel = SageApi.GetElement(channels, i);
                    String currentChannelName = SageApi.StringApi("GetChannelName", new Object[] { currentChannel });
                    if (favoriteChannelNameList.contains(currentChannelName)) {
                        channel = currentChannel;
                        channelName = currentChannelName;
                        channelNumber = SageApi.StringApi("GetChannelNumber", new Object[] { currentChannel });
                        channelId = SageApi.IntApi("GetStationID", new Object[] { currentChannel });
                        break;
                    }
                }
                out.println("                " + Translate.encode(channelNumber) + " - ");
                if (usechannellogos && null != SageApi.Api("GetChannelLogo", channel)) {
                    out.println("                <img class=\"infochannellogo\" src=\"ChannelLogo?ChannelID=" + channelId + "&type=Med&index=1&fallback=true\" alt=\"" + Translate.encode(channelName) + " logo\" title=\"" + Translate.encode(channelName) + "\"/>");
                } else {
                    out.println("                " + Translate.encode(channelName));
                }
            } else if (favoriteChannelNameList.size() > 1) {
                out.println("                " + favoriteChannelNameList.size() + " channels");
            }
            out.println("            </div></td>");
        }
        out.println("            <td class=\"favarrowscell\">");
        if (previousFavorite != null) {
            String commandStr = "FavoriteCommand?command=CreatePriority" + "&amp;LowerId=" + previousFavorite.getID() + "&amp;HigherId=" + currentFavorite.getID() + "&amp;returnto=Favorites";
            out.println("                <a onclick=\"return AiringCommand('" + commandStr + "','FavID" + id + "')\" href=\"" + commandStr + "\">");
            out.print("                    <img alt=\"\" ");
            out.print("style=\"float:left;\" ");
            out.print("width=\"15\" height=\"15\" ");
            out.println("title=\"Increase Favorite Priority\" src=\"up.gif\"/>");
            out.println("                </a>");
        }
        if (nextFavorite != null) {
            String commandStr = "FavoriteCommand?command=CreatePriority" + "&amp;LowerId=" + currentFavorite.getID() + "&amp;HigherId=" + nextFavorite.getID() + "&amp;returnto=Favorites";
            out.println("                <a onclick=\"return AiringCommand('" + commandStr + "','FavID" + id + "')\" href=\"" + commandStr + "\">");
            out.print("                    <img alt=\"\" ");
            out.print("style=\"float:left;\" ");
            out.print("width=\"15\" height=\"15\" ");
            out.println("title=\"Decrease Favorite Priority\" src=\"down.gif\"/>");
            out.println("                </a>");
        }
        out.println("            </td>");
        out.println("        </tr>");
        out.println("        </table>");
        out.println("        </td>");
        out.println("    </tr>");
        out.println("    </table>");
    }

    public void printIdArg(PrintWriter out) throws Exception {
        out.print(getIdArg());
    }

    public String getIdArg() throws Exception {
        return "FavoriteId=" + id;
    }
}
