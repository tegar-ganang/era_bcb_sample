package com.uusee.shipshape.bk.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import com.uusee.shipshape.bk.Constants;

@Entity
@Table(name = "ss_bk_baike_datasource")
@Proxy(lazy = false)
@IdClass(BaikeDataSourcePK.class)
public class BaikeDataSource implements Serializable {

    @Id
    @Column(name = "source_site")
    private String sourceSite;

    @Id
    @Column(name = "ori_id")
    private String oriId;

    private String name = "";

    @Column(name = "en_name")
    private String enName = "";

    private String alias = "";

    @Column(name = "foreign_alias")
    private String foreignAlias = "";

    private String directors = "";

    private String writers = "";

    private String stars = "";

    private String roles = "";

    @Column(name = "release_date")
    private String releaseDate = "";

    @Column(name = "release_area")
    private String releaseArea = "";

    private String language = "";

    private String length = "";

    @Column(name = "episode_count")
    private String episodeCount = "";

    @Column(name = "play_time")
    private String playTime = "";

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String synopsis = "";

    private String summary = "";

    @Column(name = "behind_the_scenes")
    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String behindTheScenes = "";

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String feature = "";

    @Column(name = "film_review")
    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    private String filmReview = "";

    @Column(name = "channel_code")
    private String channelCode = "";

    @Column(name = "channel_name")
    private String channelName = "";

    private String year = "";

    private String genre = "";

    private String area = "";

    private String tags = "";

    @Column(name = "ori_html_url")
    private String oriHtmlUrl = "";

    @Column(name = "ori_logo_url")
    private String oriLogoUrl = "";

    @Column(name = "ower_ori_id")
    private String owerOriId = "";

    @Transient
    private String owerOriName = "";

    @Column(name = "ower_ori_season")
    private String owerOriSeason = "";

    @Column(name = "rating")
    private float rating = 0.0f;

    @Column(name = "rating_count")
    private long ratingCount = 0;

    @Column(name = "favorited_count")
    private long favoritedCount = 0;

    @Column(name = "want_to_see_count")
    private long wantToSeeCount = 0;

    @Column(name = "imdb_no")
    private String imdbNo = "";

    @Temporal(TemporalType.DATE)
    @Column(name = "create_date", updatable = false)
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_date")
    private Date updateDate;

    private String publisher = "";

    @Type(type = "org.springframework.orm.hibernate3.support.ClobStringType")
    @Column(name = "item_list")
    private String itemList = "";

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

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
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

    public String getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(String episodeCount) {
        this.episodeCount = episodeCount;
    }

    public String getPlayTime() {
        return playTime;
    }

    public void setPlayTime(String playTime) {
        this.playTime = playTime;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getChannelName() {
        if (Constants.CHANNEL_CODE_MAP.containsKey(channelCode)) {
            channelName = Constants.CHANNEL_CODE_MAP.get(channelCode);
        }
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public long getFavoritedCount() {
        return favoritedCount;
    }

    public void setFavoritedCount(long favoritedCount) {
        this.favoritedCount = favoritedCount;
    }

    public long getWantToSeeCount() {
        return wantToSeeCount;
    }

    public void setWantToSeeCount(long wantToSeeCount) {
        this.wantToSeeCount = wantToSeeCount;
    }

    public String getImdbNo() {
        return imdbNo;
    }

    public void setImdbNo(String imdbNo) {
        this.imdbNo = imdbNo;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getItemList() {
        return itemList;
    }

    public void setItemList(String itemList) {
        this.itemList = itemList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oriId == null) ? 0 : oriId.hashCode());
        result = prime * result + ((sourceSite == null) ? 0 : sourceSite.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BaikeDataSource other = (BaikeDataSource) obj;
        if (oriId == null) {
            if (other.oriId != null) return false;
        } else if (!oriId.equals(other.oriId)) return false;
        if (sourceSite == null) {
            if (other.sourceSite != null) return false;
        } else if (!sourceSite.equals(other.sourceSite)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "BaikeDataSource [alias=" + alias + ", area=" + area + ", behindTheScenes=" + behindTheScenes + ", channelCode=" + channelCode + ", channelName=" + channelName + ", createDate=" + createDate + ", directors=" + directors + ", enName=" + enName + ", episodeCount=" + episodeCount + ", favoritedCount=" + favoritedCount + ", feature=" + feature + ", filmReview=" + filmReview + ", foreignAlias=" + foreignAlias + ", genre=" + genre + ", imdbNo=" + imdbNo + ", itemList=" + itemList + ", language=" + language + ", length=" + length + ", name=" + name + ", oriHtmlUrl=" + oriHtmlUrl + ", oriId=" + oriId + ", oriLogoUrl=" + oriLogoUrl + ", owerOriId=" + owerOriId + ", owerOriName=" + owerOriName + ", owerOriSeason=" + owerOriSeason + ", playTime=" + playTime + ", publisher=" + publisher + ", rating=" + rating + ", ratingCount=" + ratingCount + ", releaseArea=" + releaseArea + ", releaseDate=" + releaseDate + ", roles=" + roles + ", sourceSite=" + sourceSite + ", stars=" + stars + ", summary=" + summary + ", synopsis=" + synopsis + ", tags=" + tags + ", updateDate=" + updateDate + ", wantToSeeCount=" + wantToSeeCount + ", writers=" + writers + ", year=" + year + "]";
    }
}
