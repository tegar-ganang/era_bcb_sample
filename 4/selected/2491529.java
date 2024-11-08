package com.uusee.crawler.dbwriter;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Type;
import com.uusee.crawler.framework.Processor;
import com.uusee.crawler.model.CrawlURI;
import com.uusee.framework.bo.UniversalBo;
import com.uusee.framework.util.query.CriteriaInfo;
import com.uusee.framework.util.query.Filter;
import com.uusee.shipshape.bk.Constants;
import com.uusee.shipshape.bk.model.Baike;
import com.uusee.shipshape.bk.model.BaikeSource;
import com.uusee.util.HashCodeBuilder;
import com.uusee.util.StringUtils;

public class BaikeDbWriter extends Processor {

    private static Log log = LogFactory.getLog("BaikeDbWriter");

    private UniversalBo universalBo;

    protected void innerProcess(CrawlURI crawlURI) {
        Baike baike = (Baike) crawlURI.getModel();
        String oriId = baike.getOriId();
        String sourceSite = baike.getSourceSite();
        try {
            if (StringUtils.isEmpty(oriId) || StringUtils.isEmpty(sourceSite)) {
                return;
            }
            Baike oldBaike = getOldBaikeFromBaikeByOriIdAndSourceSite(baike);
            if (oldBaike == null) {
                oldBaike = getOldBaikeFromBaikeSource(baike);
            }
            if (oldBaike == null) {
            }
            if (oldBaike != null) {
                if (oldBaike.checkChangeWhenCrawl(baike)) {
                    log.info(sourceSite + "-" + oriId + "-更新数据。");
                }
            } else {
                universalBo.doSave(baike);
                log.info(sourceSite + "-" + oriId + "-增加数据。");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(sourceSite + "-" + oriId + "-DB时发生错误。", e);
        }
    }

    private Baike getOldBaikeFromBaikeByOriIdAndSourceSite(Baike baike) {
        Baike oldBaike = null;
        String oriId = baike.getOriId();
        String sourceSite = baike.getSourceSite();
        CriteriaInfo ci = new CriteriaInfo();
        ci.eq("sourceSite", sourceSite);
        ci.eq("oriId", oriId);
        List<Baike> baikeList = universalBo.getEntitiesByCriteriaInfo(Baike.class, ci);
        if (baikeList != null && baikeList.size() == 1) {
            oldBaike = baikeList.get(0);
        }
        return oldBaike;
    }

    private Baike getOldBaikeFromBaikeSource(Baike baike) {
        Baike oldBaike = null;
        String oriId = baike.getOriId();
        String sourceSite = baike.getSourceSite();
        CriteriaInfo ci = new CriteriaInfo();
        ci.eq("sourceSite", sourceSite);
        ci.eq("oriId", oriId);
        List<BaikeSource> baikeSourceList = universalBo.getEntitiesByCriteriaInfo(BaikeSource.class, ci);
        if (baikeSourceList != null && baikeSourceList.size() == 1) {
            BaikeSource bs = baikeSourceList.get(0);
            long baikeId = bs.getBaikeId();
            oldBaike = universalBo.getById(Baike.class, baikeId);
        }
        return oldBaike;
    }

    private Baike getOldBaikeFromBaikeByNameAndYear(Baike baike) {
        Baike oldBaike = null;
        String name = baike.getName();
        String directors = baike.getDirectors();
        String year = baike.getYear();
        String channelCode = baike.getChannelCode();
        Filter f1 = new Filter("name", name, Filter.EQ);
        Filter f2 = new Filter("alias", name, Filter.LIKE);
        CriteriaInfo ci = new CriteriaInfo();
        ci.or(f1, f2);
        if (StringUtils.isNotEmpty(year)) {
            ci.eq("year", year);
        }
        ci.eq("channelCode", channelCode);
        List<Baike> baikeList = universalBo.getEntitiesByCriteriaInfo(Baike.class, ci);
        if (baikeList.size() > 1) {
            ci = new CriteriaInfo();
            ci.eq("name", name);
            if (StringUtils.isNotEmpty(year)) {
                ci.eq("year", year);
            }
            ci.eq("channelCode", channelCode);
            baikeList = universalBo.getEntitiesByCriteriaInfo(Baike.class, ci);
        }
        if (baikeList.size() > 1) {
            ci = new CriteriaInfo();
            ci.eq("name", name);
            if (StringUtils.isNotEmpty(directors)) {
                ci.eq("directors", directors);
            }
            if (StringUtils.isNotEmpty(year)) {
                ci.eq("year", year);
            }
            ci.eq("channelCode", channelCode);
            baikeList = universalBo.getEntitiesByCriteriaInfo(Baike.class, ci);
        }
        if (baikeList.size() > 1) {
            throw new RuntimeException("新百科无法与老百科合并！" + baike.getSourceSite() + "-" + baike.getId());
        }
        if (baikeList.size() == 1) {
            oldBaike = baikeList.get(0);
            return oldBaike;
        }
        return oldBaike;
    }

    private void updateOldBaike(Baike oldBaike, Baike baike) {
        String oldSourceSite = oldBaike.getSourceSite();
        String newSourceSite = baike.getSourceSite();
        if (newSourceSite.equalsIgnoreCase(oldSourceSite) || newSourceSite.equalsIgnoreCase(Constants.SOURCE_SITE_MTIME)) {
            updateValueForBaikeProperty(oldBaike, baike);
            float rating = baike.getRating();
            if (rating > 0.0f) {
                oldBaike.setRating(rating);
            }
            long ratingCount = baike.getRatingCount();
            if (ratingCount > 0) {
                oldBaike.setRatingCount(ratingCount);
            }
            long favoritedCount = baike.getFavoritedCount();
            if (favoritedCount > 0) {
                oldBaike.setFavoritedCount(favoritedCount);
            }
            long wantToSeeCount = baike.getWantToSeeCount();
            if (wantToSeeCount > 0) {
                oldBaike.setWantToSeeCount(wantToSeeCount);
            }
            oldBaike.setSourceSite(baike.getSourceSite());
            oldBaike.setOriId(baike.getOriId());
            oldBaike.setOriHtmlUrl(baike.getOriHtmlUrl());
        } else {
            setValueForBaikeEmptyProperty(oldBaike, baike);
        }
        if (newSourceSite.equalsIgnoreCase(Constants.SOURCE_SITE_XUNLEI)) {
            oldBaike.setOriLogoUrl(baike.getOriLogoUrl());
        }
        oldBaike.setUpdateUser(baike.getUpdateUser());
        oldBaike.setUpdateDate(baike.getUpdateDate());
    }

    private void setValueForBaikeEmptyProperty(Baike oldBaike, Baike baike) {
        if (StringUtils.isEmpty(oldBaike.getEnName())) {
            oldBaike.setEnName(baike.getEnName());
        }
        if (StringUtils.isEmpty(oldBaike.getAlias())) {
            oldBaike.setAlias(baike.getAlias());
        }
        if (StringUtils.isEmpty(oldBaike.getForeignAlias())) {
            oldBaike.setForeignAlias(baike.getForeignAlias());
        }
        if (StringUtils.isEmpty(oldBaike.getDirectors())) {
            oldBaike.setDirectors(baike.getDirectors());
        }
        if (StringUtils.isEmpty(oldBaike.getWriters())) {
            oldBaike.setWriters(baike.getWriters());
        }
        if (StringUtils.isEmpty(oldBaike.getStars())) {
            oldBaike.setStars(baike.getStars());
        }
        if (StringUtils.isEmpty(oldBaike.getReleaseDate())) {
            oldBaike.setReleaseDate(baike.getReleaseDate());
        }
        if (StringUtils.isEmpty(oldBaike.getReleaseArea())) {
            oldBaike.setReleaseArea(baike.getReleaseArea());
        }
        if (StringUtils.isEmpty(oldBaike.getLanguage())) {
            oldBaike.setLanguage(baike.getLanguage());
        }
        if (StringUtils.isEmpty(oldBaike.getLength())) {
            oldBaike.setLength(baike.getLength());
        }
        if (StringUtils.isEmpty(oldBaike.getSummary())) {
            oldBaike.setSummary(baike.getSummary());
        }
        if (StringUtils.isEmpty(oldBaike.getSynopsis())) {
            oldBaike.setSynopsis(baike.getSynopsis());
        }
        if (StringUtils.isEmpty(oldBaike.getTags())) {
            oldBaike.setTags(baike.getTags());
        }
        if (StringUtils.isEmpty(oldBaike.getYear())) {
            oldBaike.setYear(baike.getYear());
        }
        if (StringUtils.isEmpty(oldBaike.getGenre())) {
            oldBaike.setGenre(baike.getGenre());
        }
        if (StringUtils.isEmpty(oldBaike.getArea())) {
            oldBaike.setArea(baike.getArea());
        }
    }

    private void updateValueForBaikeProperty(Baike oldBaike, Baike baike) {
        String enName = baike.getEnName();
        if (StringUtils.isNotEmpty(enName)) {
            oldBaike.setEnName(enName);
        }
        String alias = baike.getAlias();
        if (StringUtils.isNotEmpty(alias)) {
            oldBaike.setAlias(alias);
        }
        String foreignAlias = baike.getForeignAlias();
        if (StringUtils.isNotEmpty(foreignAlias)) {
            oldBaike.setForeignAlias(foreignAlias);
        }
        String directors = baike.getDirectors();
        if (StringUtils.isNotEmpty(directors)) {
            oldBaike.setDirectors(directors);
        }
        String writers = baike.getWriters();
        if (StringUtils.isNotEmpty(writers)) {
            oldBaike.setWriters(writers);
        }
        String stars = baike.getStars();
        if (StringUtils.isNotEmpty(stars)) {
            oldBaike.setStars(stars);
        }
        String releaseDate = baike.getReleaseDate();
        if (StringUtils.isNotEmpty(releaseDate)) {
            oldBaike.setReleaseDate(releaseDate);
        }
        String releaseArea = baike.getReleaseArea();
        if (StringUtils.isNotEmpty(releaseArea)) {
            oldBaike.setReleaseArea(releaseArea);
        }
        String language = baike.getLanguage();
        if (StringUtils.isNotEmpty(language)) {
            oldBaike.setLanguage(language);
        }
        String length = baike.getLength();
        if (StringUtils.isNotEmpty(length)) {
            oldBaike.setLength(length);
        }
        String summary = baike.getSummary();
        if (StringUtils.isNotEmpty(summary)) {
            oldBaike.setSummary(summary);
        }
        String synopsis = baike.getSynopsis();
        if (StringUtils.isNotEmpty(synopsis)) {
            oldBaike.setSynopsis(synopsis);
        }
        String tags = baike.getTags();
        if (StringUtils.isNotEmpty(tags)) {
            oldBaike.setTags(tags);
        }
        String year = baike.getYear();
        if (StringUtils.isNotEmpty(year)) {
            oldBaike.setYear(year);
        }
        String genre = baike.getGenre();
        if (StringUtils.isNotEmpty(genre)) {
            oldBaike.setGenre(genre);
        }
        String area = baike.getArea();
        if (StringUtils.isNotEmpty(area)) {
            oldBaike.setArea(area);
        }
        String owerOriId = baike.getOwerOriId();
        if (StringUtils.isNotEmpty(owerOriId)) {
            oldBaike.setOwerOriId(owerOriId);
            oldBaike.setOwerOriName(baike.getOwerOriName());
            oldBaike.setOwerOriSeason(baike.getOwerOriSeason());
        }
    }

    private BaikeSource getBaikeSoure(Baike oldBaike, Baike baike) {
        long baikeId = baike.getId();
        if (oldBaike != null) {
            baikeId = oldBaike.getId();
        }
        String oriId = baike.getOriId();
        String sourceSite = baike.getSourceSite();
        BaikeSource bs = new BaikeSource();
        bs.setBaikeId(baikeId);
        CriteriaInfo ci = new CriteriaInfo();
        ci.eq("sourceSite", sourceSite);
        ci.eq("oriId", oriId);
        List<BaikeSource> baikeSourceList = universalBo.getEntitiesByCriteriaInfo(BaikeSource.class, ci);
        if (baikeSourceList != null && baikeSourceList.size() > 0) {
            bs = baikeSourceList.get(0);
        }
        bs.setSourceSite(sourceSite);
        bs.setOriId(oriId);
        bs.setUpdateUser("auto");
        bs.setUpdateDate(new Date());
        return bs;
    }

    public void setUniversalBo(UniversalBo universalBo) {
        this.universalBo = universalBo;
    }
}
