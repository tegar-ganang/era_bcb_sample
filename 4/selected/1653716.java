package com.apc.websiteschema.fb;

import com.apc.websiteschema.res.fms.FmsApcColumn;
import com.apc.websiteschema.res.fms.FmsChannel;
import com.apc.websiteschema.res.fms.FmsData;
import com.apc.websiteschema.res.fms.FmsRegion;
import com.apc.websiteschema.res.fms.FmsSource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import websiteschema.cluster.analyzer.Doc;
import websiteschema.fb.annotation.Algorithm;
import websiteschema.fb.annotation.DI;
import websiteschema.fb.annotation.DO;
import websiteschema.fb.annotation.EI;
import websiteschema.fb.annotation.EO;
import websiteschema.fb.core.FunctionBlock;
import websiteschema.utils.StringUtil;

/**
 *
 * @author ray
 */
@EO(name = { "EO", "BAT_OUT" })
@EI(name = { "EI:ADD_ENTITY", "BAT:BATCH" })
public class FBFmsChannel extends FunctionBlock {

    @DI(name = "DOC")
    @DO(name = "DOC", relativeEvents = { "EO" })
    public Doc doc = null;

    @DI(name = "DOCS")
    @DO(name = "DOCS", relativeEvents = { "BAT_OUT" })
    public List<Doc> docs = null;

    @DI(name = "TAG")
    public String tagJobname = "JOBNAME";

    @DI(name = "TAG_DATE")
    public String tagDate = "PUBLISHDATE";

    @DI(name = "TAG_CHANNEL_NAME")
    public String tagChannelName = "CHANNELNAME";

    @DI(name = "TAG_APC_COLUMN_1")
    public String tagApcColumn1 = "REGALCHANNEL_ONE";

    @DI(name = "TAG_APC_COLUMN_2")
    public String tagApcColumn2 = "REGALCHANNEL_TWO";

    @DI(name = "TAG_APC_COLUMN_3")
    public String tagApcColumn3 = "REGALCHANNEL_THREE";

    @DI(name = "TAG_SOURCE_INFO")
    public String tagSourceInfo = "SOURCEINFO";

    @DI(name = "TAG_SOURCE_BIG_KIND")
    public String tagBigKind = "SOURCEBIGKIND";

    @DI(name = "TAG_SOURCE_SMALL_KIND")
    public String tagSmallKind = "SOURCESMALLKIND";

    @DI(name = "TAG_SOURCE_CHARACTER")
    public String tagSourceCharacter = "SOURCECHARACTER";

    @DI(name = "TAG_SOURCE_CORE")
    public String tagSourceCore = "SOURCECORE";

    @DI(name = "TAG_SOURCE_EXPERTISE")
    public String tagSourceExpertise = "SOURCEEXPERTISE";

    @DI(name = "TAG_SOURCE_INFLUENCE")
    public String tagSourceInfluence = "SOURCEINFLUENCE";

    @DI(name = "TAG_SOURCE_ORIGINAL")
    public String tagSourceOriginal = "SOURCEORIGINAL";

    @DI(name = "TAG_SOURCE_REGION_ONE")
    public String tagSourceRegion1 = "SOURCEREGION_ONE";

    @DI(name = "TAG_SOURCE_REGION_TWO")
    public String tagSourceRegion2 = "SOURCEREGION_TWO";

    @DI(name = "TAG_SITEDOMAIN")
    public String tagSiteDomain = "SITEDOMAIN";

    @DI(name = "TAG_STATDATE")
    public String tagStatDate = "STATDATE";

    @DI(name = "TAG_STATMONTH")
    public String tagStatMonth = "STATMONTH";

    @DI(name = "TAG_STATYEAR")
    public String tagStatYear = "STATYEAR";

    private FmsData fmsData = FmsData.getInstance();

    private static final Pattern pat = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2}).*");

    @Algorithm(name = "ADD_ENTITY")
    public void addTag() {
        addTag(doc);
        triggerEvent("EO");
    }

    @Algorithm(name = "BATCH")
    public void batchAddTag() {
        if (null != docs) {
            for (Doc d : docs) {
                addTag(d);
            }
        }
        triggerEvent("BAT_OUT");
    }

    private void addTag(Doc doc) {
        String jobname = doc.getValue(tagJobname);
        if (null != jobname) {
            FmsChannel chnl = fmsData.getChannel(jobname);
            if (null != chnl) {
                String name = chnl.getName();
                if (StringUtil.isNotEmpty(name)) {
                    doc.addField(tagChannelName, name);
                }
                String apcColumnId = chnl.getApcColumnId();
                addRegalChannel(doc, apcColumnId);
                String sourceId = chnl.getSourceId();
                if (StringUtil.isNotEmpty(sourceId)) {
                    FmsSource src = fmsData.getSource(sourceId);
                    addTagOfSource(doc, src);
                }
            }
        }
        String publishdate = doc.getValue(tagDate);
        if (null != publishdate) {
            addStatDate(doc, publishdate);
        }
    }

    private void addStatDate(Doc doc, String publishdate) {
        Matcher m = pat.matcher(publishdate);
        if (m.matches()) {
            String year = m.group(1);
            doc.addField(tagStatYear, year);
            String month = m.group(1) + "-" + m.group(2);
            doc.addField(tagStatMonth, month);
            String date = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
            doc.addField(tagStatDate, date);
        }
    }

    private void addRegalChannel(Doc doc, String apcColumnId) {
        if (StringUtil.isNotEmpty(apcColumnId)) {
            List<FmsApcColumn> apcColumns = fmsData.getApcColumnCascade(apcColumnId);
            if (null != apcColumns && !apcColumns.isEmpty()) {
                for (FmsApcColumn col : apcColumns) {
                    if ("1".equals(col.getDepth())) {
                        doc.addField(tagApcColumn1, col.getName());
                    } else if ("2".equals(col.getDepth())) {
                        doc.addField(tagApcColumn2, col.getName());
                    } else if ("3".equals(col.getDepth())) {
                        doc.addField(tagApcColumn3, col.getName());
                    }
                }
            }
        }
    }

    private void addTagOfSource(Doc doc, FmsSource src) {
        if (null != src) {
            String sourceInfo = src.getName();
            if (StringUtil.isNotEmpty(sourceInfo)) {
                doc.addField(tagSourceInfo, sourceInfo);
            }
            String bigKind = src.getBigKind();
            if (StringUtil.isNotEmpty(bigKind)) {
                doc.addField(tagBigKind, bigKind);
            }
            String smallKind = src.getSmallKind();
            if (StringUtil.isNotEmpty(smallKind)) {
                doc.addField(tagSmallKind, smallKind);
            }
            String sourceCharacter = src.getSourceKindName();
            if (StringUtil.isNotEmpty(sourceCharacter)) {
                String[] array = sourceCharacter.split(",");
                if (null != array && array.length > 0) {
                    for (String sc : array) {
                        if (StringUtil.isNotEmpty(sc)) {
                            doc.addField(tagSourceCharacter, sc);
                        }
                    }
                }
            }
            String sourceCore = src.getCore();
            if (StringUtil.isNotEmpty(sourceCore)) {
                doc.addField(tagSourceCore, sourceCore);
            }
            String sourceExpertise = src.getSourceExpertise();
            if (StringUtil.isNotEmpty(sourceExpertise)) {
                doc.addField(tagSourceExpertise, sourceExpertise);
            }
            String sourceInfluence = src.getSourceInfluence();
            if (StringUtil.isNotEmpty(sourceInfluence)) {
                doc.addField(tagSourceInfluence, sourceInfluence);
            }
            String sourceOriginality = src.getSourceOriginality();
            if (StringUtil.isNotEmpty(sourceOriginality)) {
                doc.addField(tagSourceOriginal, sourceOriginality);
            }
            addTagOfRegions(doc, src);
            addTagOfSiteDomain(doc, src.getUrl());
        }
    }

    private void addTagOfRegions(Doc doc, FmsSource src) {
        List<FmsRegion> regions = fmsData.getRegionCascade(src.getId());
        if (null != regions && !regions.isEmpty()) {
            for (FmsRegion region : regions) {
                if ("1".equals(region.getDepth())) {
                    String regionName = region.getName();
                    if (StringUtil.isNotEmpty(regionName)) {
                        doc.addField(tagSourceRegion1, regionName);
                    }
                } else if ("2".equals(region.getDepth())) {
                    String regionName = region.getName();
                    if (StringUtil.isNotEmpty(regionName)) {
                        doc.addField(tagSourceRegion2, regionName);
                    }
                }
            }
        }
    }

    private void addTagOfSiteDomain(Doc doc, String url) {
        if (null != url && url.contains("://")) {
            String siteDomain = url.substring(url.indexOf("//") + 2);
            if (siteDomain.contains("/")) {
                siteDomain = siteDomain.substring(0, siteDomain.indexOf("/"));
            }
            if (siteDomain.contains(":")) {
                siteDomain = siteDomain.substring(0, siteDomain.indexOf(":"));
            }
            addSiteDomain(doc, siteDomain);
        }
    }

    private void addSiteDomain(Doc doc, String siteDomain) {
        if (null != siteDomain) {
            doc.addField(tagSiteDomain, siteDomain);
            if (siteDomain.contains(".")) {
                addSiteDomain(doc, siteDomain.substring(siteDomain.indexOf('.') + 1));
            }
        }
    }
}
