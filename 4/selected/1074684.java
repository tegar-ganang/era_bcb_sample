package be.roam.drest.service.youtube.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import be.roam.drest.service.youtube.YouTubeComment;
import be.roam.drest.service.youtube.YouTubeVideo;
import be.roam.util.StringUtil;

public class YouTubeVideoHandler extends YouTubeResponseHandler {

    private YouTubeVideo currentVideo;

    private List<YouTubeVideo> videoList;

    private boolean inComment;

    public List<YouTubeVideo> getVideoList() {
        return videoList;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (!isErrorResponse()) {
            if ("id".equals(localName) || "author".equals(localName) || "title".equals(localName) || "rating_avg".equals(localName) || "rating_count".equals(localName) || "tags".equals(localName) || "description".equals(localName) || "update_time".equals(localName) || "view_count".equals(localName) || "upload_time".equals(localName) || "length_seconds".equals(localName) || "recording_date".equals(localName) || "recording_location".equals(localName) || "recording_country".equals(localName) || "text".equals(localName) || "time".equals(localName) || "channel".equals(localName) || "thumbnail_url".equals(localName) || "embed_status".equals(localName)) {
                characterBuffer = new StringBuilder();
            } else if ("comment".equals(localName)) {
                inComment = true;
                if (currentVideo.getCommentList() == null) {
                    currentVideo.setCommentList(new ArrayList<YouTubeComment>());
                }
                currentVideo.getCommentList().add(new YouTubeComment());
            } else if ("video".equals(localName) || "video_details".equals(localName)) {
                currentVideo = new YouTubeVideo();
                if (videoList == null) {
                    videoList = new ArrayList<YouTubeVideo>();
                }
                videoList.add(currentVideo);
            } else {
                super.startElement(uri, localName, qName, attributes);
            }
        } else {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!isErrorResponse()) {
            if ("comment".equals(localName)) {
                inComment = false;
            } else if ("author".equals(localName)) {
                if (inComment) {
                    currentVideo.getCommentList().get(currentVideo.getCommentList().size() - 1).setAuthor(getCharacters(true));
                } else {
                    currentVideo.setAuthor(getCharacters(true));
                }
            } else if ("id".equals(localName)) {
                currentVideo.setId(getCharacters(true));
            } else if ("title".equals(localName)) {
                currentVideo.setTitle(getCharacters(true));
            } else if ("rating_avg".equals(localName)) {
                currentVideo.setRatingAverage(StringUtil.parseDouble(getCharacters(true)));
            } else if ("rating_count".equals(localName)) {
                currentVideo.setRatingCount(StringUtil.parseInt(getCharacters(true)));
            } else if ("tags".equals(localName)) {
                currentVideo.setTags(getCharacters(true));
            } else if ("description".equals(localName)) {
                currentVideo.setDescription(getCharacters(true));
            } else if ("update_time".equals(localName)) {
                String time = getCharacters(true);
                if (!StringUtil.isNullOrEmpty(time)) {
                    currentVideo.setTimeUpdated(new Date(StringUtil.parseLong(time) * 1000));
                }
            } else if ("upload_time".equals(localName)) {
                String time = getCharacters(true);
                if (!StringUtil.isNullOrEmpty(time)) {
                    currentVideo.setTimeUploaded(new Date(StringUtil.parseLong(time) * 1000));
                }
            } else if ("length_seconds".equals(localName)) {
                currentVideo.setLengthInSeconds(StringUtil.parseInt(getCharacters(true)));
            } else if ("recording_date".equals(localName)) {
                currentVideo.setRecordingDate(getCharacters(true));
            } else if ("recording_location".equals(localName)) {
                currentVideo.setRecordingLocation(getCharacters(true));
            } else if ("recording_country".equals(localName)) {
                currentVideo.setRecordingCountry(getCharacters(true));
            } else if ("text".equals(localName)) {
                currentVideo.getCommentList().get(currentVideo.getCommentList().size() - 1).setText(getCharacters(true));
            } else if ("thumbnail_url".equals(localName)) {
                currentVideo.setThumbnailUrl(getCharacters(true));
            } else if ("embed_status".equals(localName)) {
                currentVideo.setEmbedable("ok".equals(getCharacters(true)));
            } else if ("time".equals(localName)) {
                String time = getCharacters(true);
                if (!StringUtil.isNullOrEmpty(time)) {
                    currentVideo.getCommentList().get(currentVideo.getCommentList().size() - 1).setTime(new Date(StringUtil.parseLong(time)));
                }
            } else if ("channel".equals(localName)) {
                if (currentVideo.getChannelList() == null) {
                    currentVideo.setChannelList(new ArrayList<String>());
                }
                currentVideo.getChannelList().add(getCharacters(true));
            } else {
                super.endElement(uri, localName, qName);
            }
        } else {
            super.endElement(uri, localName, qName);
        }
    }
}
