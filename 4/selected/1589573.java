package org.lnicholls.galleon.togo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.util.Tools;

public class Rule implements Serializable {

    private static Logger log = Logger.getLogger(Rule.class.getName());

    private String mType;

    private String mCode;

    private Date mDateRecorded;

    private int mDuration;

    private long mSize;

    public static String CRITERIA_FLAG = "flag";

    public static String CRITERIA_TITLE = "title";

    public static String CRITERIA_DESCRIPTION = "description";

    public static String CRITERIA_EPISODE = "episode";

    public static String CRITERIA_CHANNEL = "channel";

    public static String CRITERIA_STATION = "station";

    public static String CRITERIA_RATING = "rating";

    public static String CRITERIA_QUALITY = "quality";

    public static String CRITERIA_GENRE = "genre";

    public static String CRITERIA_TYPE = "type";

    public static String CRITERIA_DATE = "date";

    public static String CRITERIA_DURATION = "duration";

    public static String CRITERIA_SIZE = "size";

    public static String COMPARISON_EQUALS = "equals";

    public static String COMPARISON_CONTAINS = "contains";

    public static String COMPARISON_STARTS_WITH = "startsWith";

    public static String COMPARISON_ENDS_WITH = "endsWith";

    public static String COMPARISON_MORE_THAN = "moreThan";

    public static String COMPARISON_LESS_THAN = "lessThan";

    public static String FLAG_EXPIRES = "expires";

    public static String FLAG_EXPIRED = "expired";

    public static String FLAG_SAVED = "saved";

    public static String ANY_TIVO = "";

    public Rule() {
        mCriteria = "";
        mComparison = "";
        mValue = "";
        mDownload = false;
    }

    public Rule(String criteria, String comparison, String value, String tivo, boolean download) {
        mCriteria = criteria;
        mComparison = comparison;
        mValue = value.toLowerCase().trim();
        mTiVo = value;
        mDownload = download;
    }

    public String getCriteria() {
        return mCriteria;
    }

    public void setCriteria(String value) {
        mCriteria = value;
    }

    public String getComparison() {
        return mComparison;
    }

    public void setComparison(String value) {
        mComparison = value;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value.toLowerCase().trim();
    }

    public String getTiVo() {
        return mTiVo == null ? ANY_TIVO : mTiVo;
    }

    public void setTiVo(String value) {
        mTiVo = value;
    }

    public boolean getDownload() {
        return mDownload;
    }

    public void setDownload(boolean value) {
        mDownload = value;
    }

    public boolean match(Video video) {
        if (mValue.length() == 0) return true;
        if (mTiVo != null && !mTiVo.equals(ANY_TIVO)) {
            if (!video.getTivo().equals(mTiVo)) return false;
        }
        if (mCriteria.equals(CRITERIA_FLAG)) {
            if (video.getIcon() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) {
                    mValue = mValue.toLowerCase().trim();
                    if (FLAG_EXPIRES.indexOf(mValue) != -1) return (video.getIcon().toLowerCase().equals("expires-soon-recording")); else if (FLAG_EXPIRED.indexOf(mValue) != -1) return (video.getIcon().toLowerCase().equals("expired-recording")); else if (FLAG_SAVED.indexOf(mValue) != -1) return (video.getIcon().toLowerCase().equals("save-until-i-delete-recording"));
                } else if (mComparison.equals(COMPARISON_EQUALS)) {
                    mValue = mValue.toLowerCase().trim();
                    if (FLAG_EXPIRES.equals(mValue)) return (video.getIcon().toLowerCase().equals("expires-soon-recording")); else if (FLAG_EXPIRED.equals(mValue)) return (video.getIcon().toLowerCase().equals("expired-recording")); else if (FLAG_SAVED.equals(mValue)) return (video.getIcon().toLowerCase().equals("save-until-i-delete-recording"));
                } else if (mComparison.equals(COMPARISON_STARTS_WITH)) {
                    mValue = mValue.toLowerCase().trim();
                    if (FLAG_EXPIRES.startsWith(mValue)) return (video.getIcon().toLowerCase().equals("expires-soon-recording")); else if (FLAG_EXPIRED.startsWith(mValue)) return (video.getIcon().toLowerCase().equals("expired-recording")); else if (FLAG_SAVED.startsWith(mValue)) return (video.getIcon().toLowerCase().equals("save-until-i-delete-recording"));
                } else if (mComparison.equals(COMPARISON_ENDS_WITH)) {
                    mValue = mValue.toLowerCase().trim();
                    if (FLAG_EXPIRES.endsWith(mValue)) return (video.getIcon().toLowerCase().equals("expires-soon-recording")); else if (FLAG_EXPIRED.endsWith(mValue)) return (video.getIcon().toLowerCase().equals("expired-recording")); else if (FLAG_SAVED.endsWith(mValue)) return (video.getIcon().toLowerCase().equals("save-until-i-delete-recording"));
                }
            }
        } else if (mCriteria.equals(CRITERIA_TITLE)) {
            if (video.getTitle() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getTitle().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getTitle().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getTitle().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getTitle().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_DESCRIPTION)) {
            if (video.getDescription() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getDescription().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getDescription().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getDescription().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getDescription().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_EPISODE)) {
            if (video.getEpisodeTitle() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getEpisodeTitle().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getEpisodeTitle().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getEpisodeTitle().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getEpisodeTitle().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_CHANNEL)) {
            if (video.getChannel() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getChannel().toLowerCase().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getChannel().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getChannel().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getChannel().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_STATION)) {
            if (video.getStation() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getStation().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getStation().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getStation().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getStation().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_RATING)) {
            if (video.getRating() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getRating().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getRating().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getRating().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getRating().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_QUALITY)) {
            if (video.getRecordingQuality() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getRecordingQuality().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getRecordingQuality().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getRecordingQuality().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getRecordingQuality().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_GENRE)) {
            if (video.getProgramGenre() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getProgramGenre().toLowerCase().indexOf(mValue) != -1);
            }
        } else if (mCriteria.equals(CRITERIA_TYPE)) {
            if (video.getShowType() != null) {
                if (mComparison.equals(COMPARISON_CONTAINS)) return (video.getShowType().toLowerCase().indexOf(mValue) != -1); else if (mComparison.equals(COMPARISON_EQUALS)) return (video.getShowType().toLowerCase().equals(mValue)); else if (mComparison.equals(COMPARISON_STARTS_WITH)) return (video.getShowType().toLowerCase().startsWith(mValue)); else if (mComparison.equals(COMPARISON_ENDS_WITH)) return (video.getShowType().toLowerCase().endsWith(mValue));
            }
        } else if (mCriteria.equals(CRITERIA_DATE)) {
            if (video.getDateRecorded() != null) {
                try {
                    DateFormat dateFormat = new SimpleDateFormat();
                    Date date = dateFormat.parse(mValue);
                    if (mComparison.equals(COMPARISON_EQUALS)) return (video.getDateRecorded().equals(date)); else if (mComparison.equals(COMPARISON_MORE_THAN)) return (video.getDateRecorded().after(date)); else if (mComparison.equals(COMPARISON_LESS_THAN)) return (video.getDateRecorded().before(date));
                } catch (ParseException ex) {
                    Tools.logException(Rule.class, ex);
                }
            }
        } else if (mCriteria.equals(CRITERIA_DURATION)) {
            try {
                int duration = Integer.parseInt(mValue) * 1000 * 60;
                if (mComparison.equals(COMPARISON_EQUALS)) return video.getDuration() == duration; else if (mComparison.equals(COMPARISON_MORE_THAN)) return video.getDuration() > duration; else if (mComparison.equals(COMPARISON_LESS_THAN)) return video.getDuration() < duration;
            } catch (NumberFormatException ex) {
                Tools.logException(Rule.class, ex);
            }
        } else if (mCriteria.equals(CRITERIA_SIZE)) {
            long size = Long.parseLong(mValue) * 1024 * 1024;
            if (mComparison.equals(COMPARISON_EQUALS)) return video.getSize() == size; else if (mComparison.equals(COMPARISON_MORE_THAN)) return video.getSize() > size; else if (mComparison.equals(COMPARISON_LESS_THAN)) return video.getSize() < size;
        }
        return false;
    }

    public String getCriteriaString() {
        if (mCriteria.equals(CRITERIA_FLAG)) return "Flag"; else if (mCriteria.equals(CRITERIA_TITLE)) return "Title"; else if (mCriteria.equals(CRITERIA_DESCRIPTION)) return "Description"; else if (mCriteria.equals(CRITERIA_EPISODE)) return "Episode"; else if (mCriteria.equals(CRITERIA_CHANNEL)) return "Channel"; else if (mCriteria.equals(CRITERIA_STATION)) return "Station"; else if (mCriteria.equals(CRITERIA_RATING)) return "Rating"; else if (mCriteria.equals(CRITERIA_QUALITY)) return "Quality"; else if (mCriteria.equals(CRITERIA_GENRE)) return "Genre"; else if (mCriteria.equals(CRITERIA_TYPE)) return "Type"; else if (mCriteria.equals(CRITERIA_DATE)) return "Date"; else if (mCriteria.equals(CRITERIA_DURATION)) return "Duration"; else if (mCriteria.equals(CRITERIA_SIZE)) return "Size";
        return "";
    }

    public String getComparisonString() {
        if (mComparison.equals(COMPARISON_EQUALS)) return "Equals"; else if (mComparison.equals(COMPARISON_CONTAINS)) return "Contains"; else if (mComparison.equals(COMPARISON_STARTS_WITH)) return "Starts With"; else if (mComparison.equals(COMPARISON_ENDS_WITH)) return "Ends With"; else if (mComparison.equals(COMPARISON_MORE_THAN)) return "More Than"; else if (mComparison.equals(COMPARISON_LESS_THAN)) return "Less Than";
        return "";
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append("Criteria=" + mCriteria + '\n');
            buffer.append("Comparison=" + mComparison + '\n');
            buffer.append("Value=" + mValue + '\n');
            buffer.append("TiVo=" + mTiVo + '\n');
            buffer.append("Download=" + mDownload + '\n');
        }
        return buffer.toString();
    }

    private String mCriteria;

    private String mComparison;

    private String mValue;

    private String mTiVo;

    private boolean mDownload;
}
