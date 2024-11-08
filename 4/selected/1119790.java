package com.uusee.shipshape.bk.model;

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
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import com.uusee.shipshape.bk.Constants;
import com.uusee.util.StringUtils;

/**
 * 百科信息表对应Model。
 * 
 * @author <a href="mailto:wangruibj@hotmail.com">WangRui</a>
 *
 */
@Entity
@Table(name = "ss_bk_baike")
@Proxy(lazy = false)
@SequenceGenerator(name = "baike_sq", sequenceName = "ss_bk_baike_sq", allocationSize = 1)
public class Baike implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "baike_sq")
    private long id;

    private String name;

    @Column(name = "en_name")
    private String enName;

    private String alias;

    @Column(name = "foreign_alias")
    private String foreignAlias;

    private String directors;

    private String writers;

    private String stars;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "release_area")
    private String releaseArea;

    private String language;

    private String length;

    @Column(name = "play_time")
    private String playTime;

    private String summary;

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String synopsis;

    @Column(name = "behind_the_scenes")
    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String behindTheScenes;

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String feature;

    @Column(name = "film_review")
    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String filmReview;

    private String tags;

    @Column(name = "channel_code")
    private String channelCode;

    @Column(name = "channel_name")
    private String channelName;

    private String year;

    private String genre;

    private String area;

    @Column(name = "first_letter")
    private String firstLetter;

    @Column(name = "source_site")
    private String sourceSite;

    @Column(name = "ori_id")
    private String oriId;

    @Column(name = "ower_ori_id")
    private String owerOriId;

    @Transient
    private String owerOriName;

    @Column(name = "ower_ori_season")
    private String owerOriSeason;

    @Column(name = "ori_html_url")
    private String oriHtmlUrl;

    @Column(name = "ori_logo_url")
    private String oriLogoUrl;

    @Column(name = "ori_logo_url1")
    private String oriLogoUrlOne;

    @Column(name = "ori_logo_url2")
    private String oriLogoUrlTwo;

    @Column(name = "ori_logo_url3")
    private String oriLogoUrlThree;

    @Column(name = "video_status")
    private String videoStatus = "00";

    @Column(name = "synopsis_status")
    private String synopsisStatus = "00";

    @Column(name = "review_status")
    private String reviewStatus = "00";

    @Column(name = "online_status")
    private String onlineStatus = "00";

    @Column(name = "logo_status")
    private String logoStatus = "00";

    @Column(name = "image_status")
    private String imageStatus = "00";

    @Column(name = "favorited_count")
    private long favoritedCount = 0;

    @Column(name = "rating")
    private float rating = 0.0f;

    @Column(name = "rating_count")
    private long ratingCount = 0;

    @Column(name = "want_to_see_count")
    private long wantToSeeCount = 0;

    private String remarks = "";

    @Column(name = "editor_create_flag")
    private String editorCreateFlag = "N";

    @Transient
    private String prohibitAutoUpdate = "N";

    @Column(name = "other_1")
    private String other1 = "";

    @Column(name = "other_2")
    private String other2 = "";

    @Column(name = "other_3")
    private String other3 = "";

    @Column(name = "other_4")
    private String other4 = "";

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "update_user")
    private String updateUser;

    @Column(name = "update_date")
    private Date updateDate;

    @Column(name = "program_id")
    private String programId = "";

    @Transient
    private String imdbNo = "";

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getForeignAlias() {
        return foreignAlias;
    }

    public void setForeignAlias(String foreignAlias) {
        this.foreignAlias = foreignAlias;
    }

    public String getDirectors() {
        return directors;
    }

    public void setDirectors(String directors) {
        this.directors = directors;
    }

    public String getWriters() {
        return writers;
    }

    public void setWriters(String writers) {
        this.writers = writers;
    }

    public String getStars() {
        return stars;
    }

    public void setStars(String stars) {
        this.stars = stars;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getReleaseArea() {
        return releaseArea;
    }

    public void setReleaseArea(String releaseArea) {
        this.releaseArea = releaseArea;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getPlayTime() {
        return playTime;
    }

    public void setPlayTime(String playTime) {
        this.playTime = playTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        if (StringUtils.isNotEmpty(summary) && summary.length() > 900) {
            summary = summary.substring(0, 900) + "......";
        }
        this.summary = summary;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getBehindTheScenes() {
        return behindTheScenes;
    }

    public void setBehindTheScenes(String behindTheScenes) {
        this.behindTheScenes = behindTheScenes;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getFilmReview() {
        return filmReview;
    }

    public void setFilmReview(String filmReview) {
        this.filmReview = filmReview;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getFirstLetter() {
        return firstLetter;
    }

    public void setFirstLetter(String firstLetter) {
        this.firstLetter = firstLetter;
    }

    public String getSourceSite() {
        return sourceSite;
    }

    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    public String getOriId() {
        return oriId;
    }

    public void setOriId(String oriId) {
        this.oriId = oriId;
    }

    public String getOwerOriId() {
        return owerOriId;
    }

    public void setOwerOriId(String owerOriId) {
        this.owerOriId = owerOriId;
    }

    public String getOwerOriName() {
        return owerOriName;
    }

    public void setOwerOriName(String owerOriName) {
        this.owerOriName = owerOriName;
    }

    public String getOwerOriSeason() {
        return owerOriSeason;
    }

    public void setOwerOriSeason(String owerOriSeason) {
        this.owerOriSeason = owerOriSeason;
    }

    public String getOriHtmlUrl() {
        return oriHtmlUrl;
    }

    public void setOriHtmlUrl(String oriHtmlUrl) {
        this.oriHtmlUrl = oriHtmlUrl;
    }

    public String getOriLogoUrl() {
        return oriLogoUrl;
    }

    public void setOriLogoUrl(String oriLogoUrl) {
        this.oriLogoUrl = oriLogoUrl;
    }

    public String getOriLogoUrlOne() {
        return oriLogoUrlOne;
    }

    public void setOriLogoUrlOne(String oriLogoUrlOne) {
        this.oriLogoUrlOne = oriLogoUrlOne;
    }

    public String getOriLogoUrlTwo() {
        return oriLogoUrlTwo;
    }

    public void setOriLogoUrlTwo(String oriLogoUrlTwo) {
        this.oriLogoUrlTwo = oriLogoUrlTwo;
    }

    public String getOriLogoUrlThree() {
        return oriLogoUrlThree;
    }

    public void setOriLogoUrlThree(String oriLogoUrlThree) {
        this.oriLogoUrlThree = oriLogoUrlThree;
    }

    public String getVideoStatus() {
        return videoStatus;
    }

    public void setVideoStatus(String videoStatus) {
        this.videoStatus = videoStatus;
    }

    public String getSynopsisStatus() {
        return synopsisStatus;
    }

    public void setSynopsisStatus(String synopsisStatus) {
        this.synopsisStatus = synopsisStatus;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getLogoStatus() {
        return logoStatus;
    }

    public void setLogoStatus(String logoStatus) {
        this.logoStatus = logoStatus;
    }

    public String getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(String imageStatus) {
        this.imageStatus = imageStatus;
    }

    public long getFavoritedCount() {
        return favoritedCount;
    }

    public void setFavoritedCount(long favoritedCount) {
        this.favoritedCount = favoritedCount;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public long getWantToSeeCount() {
        return wantToSeeCount;
    }

    public void setWantToSeeCount(long wantToSeeCount) {
        this.wantToSeeCount = wantToSeeCount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getEditorCreateFlag() {
        return editorCreateFlag;
    }

    public void setEditorCreateFlag(String editorCreateFlag) {
        this.editorCreateFlag = editorCreateFlag;
    }

    public String getProhibitAutoUpdate() {
        return prohibitAutoUpdate;
    }

    public void setProhibitAutoUpdate(String prohibitAutoUpdate) {
        this.prohibitAutoUpdate = prohibitAutoUpdate;
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

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
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

    public String getLogo() {
        String ids = this.id + "";
        String subId = ids.substring(ids.length() - 3, ids.length());
        String logo = this.channelCode + "/new/" + subId + "/" + ids + "/" + ids + ".jpg";
        return logo;
    }

    public String getLogo1() {
        String ids = this.id + "";
        String subId = ids.substring(ids.length() - 3, ids.length());
        String logo2 = this.channelCode + "/new/" + subId + "/" + ids + "/" + ids + "_200X150.jpg";
        return logo2;
    }

    public String getLogo2() {
        String ids = this.id + "";
        String subId = ids.substring(ids.length() - 3, ids.length());
        String logo2 = this.channelCode + "/new/" + subId + "/" + ids + "/" + ids + "_jz_150X200.jpg";
        return logo2;
    }

    public String getLogo3() {
        String ids = this.id + "";
        String subId = ids.substring(ids.length() - 3, ids.length());
        String logo2 = this.channelCode + "/new/" + subId + "/" + ids + "/" + ids + "_jz_200X150.jpg";
        return logo2;
    }

    public String getListPicture() {
        String ids = this.id + "";
        String subId = ids.substring(ids.length() - 3, ids.length());
        String listPicture = this.channelCode + "/new/" + subId + "/" + ids + "/" + ids + "_96X128.jpg";
        return listPicture;
    }

    public String getVideoStatusName() {
        String videoStatusName = "";
        if (Constants.VIDEO_STATUS_MAP.containsKey(this.videoStatus)) {
            videoStatusName = Constants.VIDEO_STATUS_MAP.get(this.videoStatus);
        }
        return videoStatusName;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getImdbNo() {
        return imdbNo;
    }

    public void setImdbNo(String imdbNo) {
        this.imdbNo = imdbNo;
    }

    public boolean checkChangeWhenCrawl(Baike baike) {
        HashCodeBuilder oldHashCode = new HashCodeBuilder();
        HashCodeBuilder newHashCode = new HashCodeBuilder();
        float rating = baike.getRating();
        if (rating > 0.0f) {
            oldHashCode.append(this.getRating());
            newHashCode.append(rating);
        }
        long ratingCount = baike.getRatingCount();
        if (ratingCount > 0) {
            oldHashCode.append(this.getRatingCount());
            newHashCode.append(ratingCount);
        }
        long favoritedCount = baike.getFavoritedCount();
        if (favoritedCount > 0) {
            oldHashCode.append(this.getFavoritedCount());
            newHashCode.append(favoritedCount);
        }
        long wantToSeeCount = baike.getWantToSeeCount();
        if (wantToSeeCount > 0) {
            oldHashCode.append(this.getWantToSeeCount());
            newHashCode.append(wantToSeeCount);
        }
        String enName = baike.getEnName();
        if (StringUtils.isNotEmpty(enName)) {
            oldHashCode.append(this.getEnName());
            newHashCode.append(enName);
        }
        String alias = baike.getAlias();
        if (StringUtils.isNotEmpty(alias)) {
            oldHashCode.append(this.getAlias());
            newHashCode.append(alias);
        }
        String foreignAlias = baike.getForeignAlias();
        if (StringUtils.isNotEmpty(foreignAlias)) {
            oldHashCode.append(this.getForeignAlias());
            newHashCode.append(foreignAlias);
        }
        String directors = baike.getDirectors();
        if (StringUtils.isNotEmpty(directors)) {
            oldHashCode.append(this.getDirectors());
            newHashCode.append(directors);
        }
        String writers = baike.getWriters();
        if (StringUtils.isNotEmpty(writers)) {
            oldHashCode.append(this.getWriters());
            newHashCode.append(writers);
        }
        String stars = baike.getStars();
        if (StringUtils.isNotEmpty(stars)) {
            oldHashCode.append(this.getStars());
            newHashCode.append(stars);
        }
        String releaseDate = baike.getReleaseDate();
        if (StringUtils.isNotEmpty(releaseDate)) {
            oldHashCode.append(this.getReleaseDate());
            newHashCode.append(releaseDate);
        }
        String releaseArea = baike.getReleaseArea();
        if (StringUtils.isNotEmpty(releaseArea)) {
            oldHashCode.append(this.getReleaseArea());
            newHashCode.append(releaseArea);
        }
        String language = baike.getLanguage();
        if (StringUtils.isNotEmpty(language)) {
            oldHashCode.append(this.getLanguage());
            newHashCode.append(language);
        }
        String length = baike.getLength();
        if (StringUtils.isNotEmpty(length)) {
            oldHashCode.append(this.getLength());
            newHashCode.append(length);
        }
        String playTime = baike.getPlayTime();
        if (StringUtils.isNotEmpty(playTime)) {
            oldHashCode.append(this.getPlayTime());
            newHashCode.append(playTime);
        }
        String summary = baike.getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            oldHashCode.append(this.getSummary());
            newHashCode.append(summary);
        }
        String synopsis = baike.getSynopsis();
        if (StringUtils.isNotEmpty(synopsis)) {
            oldHashCode.append(this.getSynopsis());
            newHashCode.append(synopsis);
        }
        String behindTheScenes = baike.getBehindTheScenes();
        if (StringUtils.isNotEmpty(behindTheScenes)) {
            oldHashCode.append(this.getBehindTheScenes());
            newHashCode.append(behindTheScenes);
        }
        String feature = baike.getFeature();
        if (StringUtils.isNotEmpty(feature)) {
            oldHashCode.append(this.getFeature());
            newHashCode.append(feature);
        }
        String filmReview = baike.getFilmReview();
        if (StringUtils.isNotEmpty(filmReview)) {
            oldHashCode.append(this.getFilmReview());
            newHashCode.append(filmReview);
        }
        String tags = baike.getTags();
        if (StringUtils.isNotEmpty(tags)) {
            oldHashCode.append(this.getTags());
            newHashCode.append(tags);
        }
        String year = baike.getYear();
        if (StringUtils.isNotEmpty(year)) {
            oldHashCode.append(this.getYear());
            newHashCode.append(year);
        }
        String genre = baike.getGenre();
        if (StringUtils.isNotEmpty(genre)) {
            oldHashCode.append(this.getGenre());
            newHashCode.append(genre);
        }
        String area = baike.getArea();
        if (StringUtils.isNotEmpty(area)) {
            oldHashCode.append(this.getArea());
            newHashCode.append(area);
        }
        if (oldHashCode.hashCode() == newHashCode.hashCode()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasChangeWhenEdit(Baike baike) {
        HashCodeBuilder oldHashCode = new HashCodeBuilder();
        HashCodeBuilder newHashCode = new HashCodeBuilder();
        String name = baike.getName();
        if (StringUtils.isNotEmpty(name)) {
            oldHashCode.append(this.getName());
            newHashCode.append(name);
        }
        String enName = baike.getEnName();
        if (StringUtils.isNotEmpty(enName)) {
            oldHashCode.append(this.getEnName());
            newHashCode.append(enName);
        }
        String alias = baike.getAlias();
        if (StringUtils.isNotEmpty(alias)) {
            oldHashCode.append(this.getAlias());
            newHashCode.append(alias);
        }
        String foreignAlias = baike.getForeignAlias();
        if (StringUtils.isNotEmpty(foreignAlias)) {
            oldHashCode.append(this.getForeignAlias());
            newHashCode.append(foreignAlias);
        }
        String directors = baike.getDirectors();
        if (StringUtils.isNotEmpty(directors)) {
            oldHashCode.append(this.getDirectors());
            newHashCode.append(directors);
        }
        String writers = baike.getWriters();
        if (StringUtils.isNotEmpty(writers)) {
            oldHashCode.append(this.getWriters());
            newHashCode.append(writers);
        }
        String stars = baike.getStars();
        if (StringUtils.isNotEmpty(stars)) {
            oldHashCode.append(this.getStars());
            newHashCode.append(stars);
        }
        String releaseDate = baike.getReleaseDate();
        if (StringUtils.isNotEmpty(releaseDate)) {
            oldHashCode.append(this.getReleaseDate());
            newHashCode.append(releaseDate);
        }
        String releaseArea = baike.getReleaseArea();
        if (StringUtils.isNotEmpty(releaseArea)) {
            oldHashCode.append(this.getReleaseArea());
            newHashCode.append(releaseArea);
        }
        String language = baike.getLanguage();
        if (StringUtils.isNotEmpty(language)) {
            oldHashCode.append(this.getLanguage());
            newHashCode.append(language);
        }
        String length = baike.getLength();
        if (StringUtils.isNotEmpty(length)) {
            oldHashCode.append(this.getLength());
            newHashCode.append(length);
        }
        String playTime = baike.getPlayTime();
        if (StringUtils.isNotEmpty(playTime)) {
            oldHashCode.append(this.getPlayTime());
            newHashCode.append(playTime);
        }
        String summary = baike.getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            oldHashCode.append(this.getSummary());
            newHashCode.append(summary);
        }
        String synopsis = baike.getSynopsis();
        if (StringUtils.isNotEmpty(synopsis)) {
            oldHashCode.append(this.getSynopsis());
            newHashCode.append(synopsis);
        }
        String behindTheScenes = baike.getBehindTheScenes();
        if (StringUtils.isNotEmpty(behindTheScenes)) {
            oldHashCode.append(this.getBehindTheScenes());
            newHashCode.append(behindTheScenes);
        }
        String feature = baike.getFeature();
        if (StringUtils.isNotEmpty(feature)) {
            oldHashCode.append(this.getFeature());
            newHashCode.append(feature);
        }
        String filmReview = baike.getFilmReview();
        if (StringUtils.isNotEmpty(filmReview)) {
            oldHashCode.append(this.getFilmReview());
            newHashCode.append(filmReview);
        }
        String tags = baike.getTags();
        if (StringUtils.isNotEmpty(tags)) {
            oldHashCode.append(this.getTags());
            newHashCode.append(tags);
        }
        String channelCode = baike.getChannelCode();
        if (StringUtils.isNotEmpty(channelCode)) {
            oldHashCode.append(this.getChannelCode());
            newHashCode.append(channelCode);
        }
        String channelName = baike.getChannelName();
        if (StringUtils.isNotEmpty(channelName)) {
            oldHashCode.append(this.getChannelName());
            newHashCode.append(channelName);
        }
        String year = baike.getYear();
        if (StringUtils.isNotEmpty(year)) {
            oldHashCode.append(this.getYear());
            newHashCode.append(year);
        }
        String genre = baike.getGenre();
        if (StringUtils.isNotEmpty(genre)) {
            oldHashCode.append(this.getGenre());
            newHashCode.append(genre);
        }
        String area = baike.getArea();
        if (StringUtils.isNotEmpty(area)) {
            oldHashCode.append(this.getArea());
            newHashCode.append(area);
        }
        float rating = baike.getRating();
        if (rating > 0.0f) {
            oldHashCode.append(this.getRating());
            newHashCode.append(rating);
        }
        long ratingCount = baike.getRatingCount();
        if (ratingCount > 0) {
            oldHashCode.append(this.getRatingCount());
            newHashCode.append(ratingCount);
        }
        long favoritedCount = baike.getFavoritedCount();
        if (favoritedCount > 0) {
            oldHashCode.append(this.getFavoritedCount());
            newHashCode.append(favoritedCount);
        }
        long wantToSeeCount = baike.getWantToSeeCount();
        if (wantToSeeCount > 0) {
            oldHashCode.append(this.getWantToSeeCount());
            newHashCode.append(wantToSeeCount);
        }
        String oriLogoUrl = baike.getOriLogoUrl();
        if (StringUtils.isNotEmpty(oriLogoUrl)) {
            oldHashCode.append(this.getOriLogoUrl());
            newHashCode.append(oriLogoUrl);
        }
        String oriLogoUrlOne = baike.getOriLogoUrlOne();
        if (StringUtils.isNotEmpty(oriLogoUrlOne)) {
            oldHashCode.append(this.getOriLogoUrlOne());
            newHashCode.append(oriLogoUrlOne);
        }
        String oriLogoUrlTwo = baike.getOriLogoUrlTwo();
        if (StringUtils.isNotEmpty(oriLogoUrlTwo)) {
            oldHashCode.append(this.getOriLogoUrlTwo());
            newHashCode.append(oriLogoUrlTwo);
        }
        String oriLogoUrlThree = baike.getOriLogoUrlThree();
        if (StringUtils.isNotEmpty(oriLogoUrlThree)) {
            oldHashCode.append(this.getOriLogoUrlThree());
            newHashCode.append(oriLogoUrlThree);
        }
        String reviewStatus = baike.getReviewStatus();
        if (StringUtils.isNotEmpty(reviewStatus)) {
            oldHashCode.append(this.getReviewStatus());
            newHashCode.append(reviewStatus);
        }
        String remarks = baike.getRemarks();
        if (StringUtils.isNotEmpty(remarks)) {
            oldHashCode.append(this.getRemarks());
            newHashCode.append(remarks);
        }
        String other1 = baike.getOther1();
        if (StringUtils.isNotEmpty(other1)) {
            oldHashCode.append(this.getOther1());
            newHashCode.append(other1);
        }
        String other2 = baike.getOther2();
        if (StringUtils.isNotEmpty(other2)) {
            oldHashCode.append(this.getOther2());
            newHashCode.append(other2);
        }
        String other3 = baike.getOther3();
        if (StringUtils.isNotEmpty(other3)) {
            oldHashCode.append(this.getOther3());
            newHashCode.append(other3);
        }
        String other4 = baike.getOther4();
        if (StringUtils.isNotEmpty(other4)) {
            oldHashCode.append(this.getOther4());
            newHashCode.append(other4);
        }
        String programId = baike.getProgramId();
        if (StringUtils.isNotEmpty(programId)) {
            oldHashCode.append(this.getProgramId());
            newHashCode.append(programId);
        }
        if (oldHashCode.hashCode() == newHashCode.hashCode()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Baike other = (Baike) obj;
        if (id != other.id) return false;
        return true;
    }
}
