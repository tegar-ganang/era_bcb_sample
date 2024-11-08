package com.uusee.shipshape.sp.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import com.uusee.framework.model.BaseModel;

@Entity
@Table(name = "ss_sp_ugc")
@SuppressWarnings("serial")
@SequenceGenerator(name = "SEQ", sequenceName = "SS_SP_UGC_SQ", allocationSize = 1)
public class Ugc extends BaseModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    private long id;

    private String domain;

    @Column(name = "video_site")
    private String videoSite;

    @Column(name = "source_site")
    private String sourceSite;

    @Column(name = "play_url")
    private String playUrl;

    private String title;

    private String logo;

    private int length = -1;

    private String quality;

    private String channel;

    private String category;

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String tags = "";

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String summary = "";

    private String vid = "";

    private String evid = "";

    @Column(name = "ORI_LOGO")
    private String oriLogo = "";

    @Column(name = "share_url")
    private String shareUrl = "";

    @Column(name = "TOTAL_PV")
    private int totalPageView;

    @Column(name = "TOTAL_COMMENT")
    private int totalComment;

    @Column(name = "TOTAL_FAV")
    private int totalFav;

    @Column(name = "upload_userid")
    private String uploadUserid = "";

    @Column(name = "upload_username")
    private String uploadUsername = "";

    @Column(name = "upload_userblog")
    private String uploadUserblog = "";

    @Column(name = "upload_date")
    private String uploadDate = "";

    @Column(name = "FILE_SIZE")
    private Long fileSize = -1l;

    @Column(name = "FILE_FORMAT")
    private String fileFormat = "";

    @Column(name = "FILE_EXT")
    private String fileExt = "";

    @Column(name = "FILE_DURATION")
    private Integer fileDuration;

    @Column(name = "VIDEO_FORMAT")
    private String videoFormat = "";

    @Column(name = "VIDEO_WIDTH")
    private Integer videoWidth;

    @Column(name = "VIDEO_HEIGHT")
    private Integer videoHeight;

    @Column(name = "VIDEO_BIT_RATE_X_1000")
    private Integer videoBitRateX1000;

    @Column(name = "VIDEO_FRAME_RATE_X_1000")
    private Integer videoFrameRateX1000;

    @Column(name = "AUDIO_FORMAT")
    private String audioFormat = "";

    @Column(name = "AUDIO_BIT_RATE_X_1000")
    private Integer audioBitRateX1000;

    @Column(name = "AUDIO_SAMPLE_RATE_X_1000")
    private Integer audioSampleRateX1000;

    @Column(name = "AUDIO_CHANNELS")
    private Integer audioChannels;

    @Column(name = "AUDIO_RESOLUTION")
    private Integer audioResolution;

    private int weight;

    @Column(name = "LOGO_STATUS")
    private String logoStatus = "0";

    @Column(name = "status")
    private String status = "0";

    @Column(name = "REMARK")
    private String remark = "";

    @Column(name = "OTHER_1")
    private String other1 = "";

    @Column(name = "OTHER_2")
    private String other2 = "";

    @Column(name = "OTHER_3")
    private String other3 = "";

    @Column(name = "OTHER_4")
    private String other4 = "";

    @Column(name = "crawl_user")
    private String crawlUser = "";

    @Column(name = "crawl_date")
    private Date crawlDate;

    @Column(name = "update_user")
    private String updateUser;

    @Column(name = "update_date")
    private Date updateDate;

    @Transient
    private String playlistId;

    @Transient
    private String playlistVid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getVideoSite() {
        return videoSite;
    }

    public void setVideoSite(String videoSite) {
        this.videoSite = videoSite;
    }

    public String getSourceSite() {
        return sourceSite;
    }

    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getEvid() {
        return evid;
    }

    public void setEvid(String evid) {
        this.evid = evid;
    }

    public String getOriLogo() {
        return oriLogo;
    }

    public void setOriLogo(String oriLogo) {
        this.oriLogo = oriLogo;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public int getTotalPageView() {
        return totalPageView;
    }

    public void setTotalPageView(int totalPageView) {
        this.totalPageView = totalPageView;
    }

    public int getTotalComment() {
        return totalComment;
    }

    public void setTotalComment(int totalComment) {
        this.totalComment = totalComment;
    }

    public int getTotalFav() {
        return totalFav;
    }

    public void setTotalFav(int totalFav) {
        this.totalFav = totalFav;
    }

    public String getUploadUserid() {
        return uploadUserid;
    }

    public void setUploadUserid(String uploadUserid) {
        this.uploadUserid = uploadUserid;
    }

    public String getUploadUsername() {
        return uploadUsername;
    }

    public void setUploadUsername(String uploadUsername) {
        this.uploadUsername = uploadUsername;
    }

    public String getUploadUserblog() {
        return uploadUserblog;
    }

    public void setUploadUserblog(String uploadUserblog) {
        this.uploadUserblog = uploadUserblog;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Integer getFileDuration() {
        return fileDuration;
    }

    public void setFileDuration(Integer fileDuration) {
        this.fileDuration = fileDuration;
    }

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(Integer videoWidth) {
        this.videoWidth = videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(Integer videoHeight) {
        this.videoHeight = videoHeight;
    }

    public Integer getVideoBitRateX1000() {
        return videoBitRateX1000;
    }

    public void setVideoBitRateX1000(Integer videoBitRateX1000) {
        this.videoBitRateX1000 = videoBitRateX1000;
    }

    public Integer getVideoFrameRateX1000() {
        return videoFrameRateX1000;
    }

    public void setVideoFrameRateX1000(Integer videoFrameRateX1000) {
        this.videoFrameRateX1000 = videoFrameRateX1000;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public Integer getAudioBitRateX1000() {
        return audioBitRateX1000;
    }

    public void setAudioBitRateX1000(Integer audioBitRateX1000) {
        this.audioBitRateX1000 = audioBitRateX1000;
    }

    public Integer getAudioSampleRateX1000() {
        return audioSampleRateX1000;
    }

    public void setAudioSampleRateX1000(Integer audioSampleRateX1000) {
        this.audioSampleRateX1000 = audioSampleRateX1000;
    }

    public Integer getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(Integer audioChannels) {
        this.audioChannels = audioChannels;
    }

    public Integer getAudioResolution() {
        return audioResolution;
    }

    public void setAudioResolution(Integer audioResolution) {
        this.audioResolution = audioResolution;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getLogoStatus() {
        return logoStatus;
    }

    public void setLogoStatus(String logoStatus) {
        this.logoStatus = logoStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOther1() {
        return other1;
    }

    public void setOther1(String other1) {
        this.other1 = other1;
    }

    public String getOther2() {
        return other2;
    }

    public void setOther2(String other2) {
        this.other2 = other2;
    }

    public String getOther3() {
        return other3;
    }

    public void setOther3(String other3) {
        this.other3 = other3;
    }

    public String getOther4() {
        return other4;
    }

    public void setOther4(String other4) {
        this.other4 = other4;
    }

    public String getCrawlUser() {
        return crawlUser;
    }

    public void setCrawlUser(String crawlUser) {
        this.crawlUser = crawlUser;
    }

    public Date getCrawlDate() {
        return crawlDate;
    }

    public void setCrawlDate(Date crawlDate) {
        this.crawlDate = crawlDate;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public String getPlaylistVid() {
        return playlistVid;
    }

    public void setPlaylistVid(String playlistVid) {
        this.playlistVid = playlistVid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playUrl == null) ? 0 : playUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Ugc other = (Ugc) obj;
        if (playUrl == null) {
            if (other.playUrl != null) return false;
        } else if (!playUrl.equals(other.playUrl)) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ugc [");
        if (audioBitRateX1000 != null) {
            builder.append("audioBitRateX1000=");
            builder.append(audioBitRateX1000);
            builder.append(", ");
        }
        if (audioChannels != null) {
            builder.append("audioChannels=");
            builder.append(audioChannels);
            builder.append(", ");
        }
        if (audioFormat != null) {
            builder.append("audioFormat=");
            builder.append(audioFormat);
            builder.append(", ");
        }
        if (audioResolution != null) {
            builder.append("audioResolution=");
            builder.append(audioResolution);
            builder.append(", ");
        }
        if (audioSampleRateX1000 != null) {
            builder.append("audioSampleRateX1000=");
            builder.append(audioSampleRateX1000);
            builder.append(", ");
        }
        if (category != null) {
            builder.append("category=");
            builder.append(category);
            builder.append(", ");
        }
        if (channel != null) {
            builder.append("channel=");
            builder.append(channel);
            builder.append(", ");
        }
        if (crawlDate != null) {
            builder.append("crawlDate=");
            builder.append(crawlDate);
            builder.append(", ");
        }
        if (crawlUser != null) {
            builder.append("crawlUser=");
            builder.append(crawlUser);
            builder.append(", ");
        }
        if (domain != null) {
            builder.append("domain=");
            builder.append(domain);
            builder.append(", ");
        }
        if (evid != null) {
            builder.append("evid=");
            builder.append(evid);
            builder.append(", ");
        }
        if (fileDuration != null) {
            builder.append("fileDuration=");
            builder.append(fileDuration);
            builder.append(", ");
        }
        if (fileExt != null) {
            builder.append("fileExt=");
            builder.append(fileExt);
            builder.append(", ");
        }
        if (fileFormat != null) {
            builder.append("fileFormat=");
            builder.append(fileFormat);
            builder.append(", ");
        }
        if (fileSize != null) {
            builder.append("fileSize=");
            builder.append(fileSize);
            builder.append(", ");
        }
        builder.append("id=");
        builder.append(id);
        builder.append(", length=");
        builder.append(length);
        builder.append(", ");
        if (logo != null) {
            builder.append("logo=");
            builder.append(logo);
            builder.append(", ");
        }
        if (logoStatus != null) {
            builder.append("logoStatus=");
            builder.append(logoStatus);
            builder.append(", ");
        }
        if (oriLogo != null) {
            builder.append("oriLogo=");
            builder.append(oriLogo);
            builder.append(", ");
        }
        if (other1 != null) {
            builder.append("other1=");
            builder.append(other1);
            builder.append(", ");
        }
        if (other2 != null) {
            builder.append("other2=");
            builder.append(other2);
            builder.append(", ");
        }
        if (other3 != null) {
            builder.append("other3=");
            builder.append(other3);
            builder.append(", ");
        }
        if (other4 != null) {
            builder.append("other4=");
            builder.append(other4);
            builder.append(", ");
        }
        if (playUrl != null) {
            builder.append("playUrl=");
            builder.append(playUrl);
            builder.append(", ");
        }
        if (playlistId != null) {
            builder.append("playlistId=");
            builder.append(playlistId);
            builder.append(", ");
        }
        if (playlistVid != null) {
            builder.append("playlistVid=");
            builder.append(playlistVid);
            builder.append(", ");
        }
        if (quality != null) {
            builder.append("quality=");
            builder.append(quality);
            builder.append(", ");
        }
        if (remark != null) {
            builder.append("remark=");
            builder.append(remark);
            builder.append(", ");
        }
        if (shareUrl != null) {
            builder.append("shareUrl=");
            builder.append(shareUrl);
            builder.append(", ");
        }
        if (sourceSite != null) {
            builder.append("sourceSite=");
            builder.append(sourceSite);
            builder.append(", ");
        }
        if (status != null) {
            builder.append("status=");
            builder.append(status);
            builder.append(", ");
        }
        if (summary != null) {
            builder.append("summary=");
            builder.append(summary);
            builder.append(", ");
        }
        if (tags != null) {
            builder.append("tags=");
            builder.append(tags);
            builder.append(", ");
        }
        if (title != null) {
            builder.append("title=");
            builder.append(title);
            builder.append(", ");
        }
        builder.append("totalComment=");
        builder.append(totalComment);
        builder.append(", totalFav=");
        builder.append(totalFav);
        builder.append(", totalPageView=");
        builder.append(totalPageView);
        builder.append(", ");
        if (updateDate != null) {
            builder.append("updateDate=");
            builder.append(updateDate);
            builder.append(", ");
        }
        if (updateUser != null) {
            builder.append("updateUser=");
            builder.append(updateUser);
            builder.append(", ");
        }
        if (uploadDate != null) {
            builder.append("uploadDate=");
            builder.append(uploadDate);
            builder.append(", ");
        }
        if (uploadUserblog != null) {
            builder.append("uploadUserblog=");
            builder.append(uploadUserblog);
            builder.append(", ");
        }
        if (uploadUserid != null) {
            builder.append("uploadUserid=");
            builder.append(uploadUserid);
            builder.append(", ");
        }
        if (uploadUsername != null) {
            builder.append("uploadUsername=");
            builder.append(uploadUsername);
            builder.append(", ");
        }
        if (vid != null) {
            builder.append("vid=");
            builder.append(vid);
            builder.append(", ");
        }
        if (videoBitRateX1000 != null) {
            builder.append("videoBitRateX1000=");
            builder.append(videoBitRateX1000);
            builder.append(", ");
        }
        if (videoFormat != null) {
            builder.append("videoFormat=");
            builder.append(videoFormat);
            builder.append(", ");
        }
        if (videoFrameRateX1000 != null) {
            builder.append("videoFrameRateX1000=");
            builder.append(videoFrameRateX1000);
            builder.append(", ");
        }
        if (videoHeight != null) {
            builder.append("videoHeight=");
            builder.append(videoHeight);
            builder.append(", ");
        }
        if (videoSite != null) {
            builder.append("videoSite=");
            builder.append(videoSite);
            builder.append(", ");
        }
        if (videoWidth != null) {
            builder.append("videoWidth=");
            builder.append(videoWidth);
            builder.append(", ");
        }
        builder.append("weight=");
        builder.append(weight);
        builder.append("]");
        return builder.toString();
    }
}
