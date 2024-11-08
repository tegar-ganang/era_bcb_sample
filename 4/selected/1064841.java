package com.apc.websiteschema.fb;

import com.apc.indextask.idx.Idx;
import com.apc.websiteschema.res.fms.FmsApcColumn;
import com.apc.websiteschema.res.fms.FmsChannel;
import com.apc.websiteschema.res.fms.FmsData;
import com.apc.websiteschema.res.fms.FmsRegion;
import com.apc.websiteschema.res.fms.FmsSource;
import java.util.List;
import org.junit.Test;
import websiteschema.cluster.analyzer.Doc;
import websiteschema.element.DocumentUtil;

/**
 *
 * @author ray
 */
public class FmsDataTest {

    @Test
    public void test() {
        FmsData fmsData = FmsData.getInstance();
        String jobname = "bank_hexun_com_20552";
        System.out.println("Jobname : " + jobname);
        FmsChannel chnl = fmsData.getChannel(jobname);
        System.out.println("Channel id : " + chnl.getId());
        System.out.println("Channel name : " + chnl.getName());
        FmsSource source = fmsData.getSource(chnl.getSourceId());
        System.out.println("Source id : " + source.getId());
        System.out.println("Source name : " + source.getName());
        System.out.println("Source big kind : " + source.getBigKind());
        System.out.println("Source small kind : " + source.getSmallKind());
        System.out.println("Source url : " + source.getUrl());
        System.out.println("Source core : " + source.getCore());
        System.out.println("Source expertise : " + source.getSourceExpertise());
        System.out.println("Source influence : " + source.getSourceInfluence());
        System.out.println("Source originality : " + source.getSourceOriginality());
        System.out.println("Source character : " + source.getSourceKindName());
        List<FmsApcColumn> apcColumns = fmsData.getApcColumnCascade(chnl.getApcColumnId());
        for (FmsApcColumn col : apcColumns) {
            System.out.println("Apc column : " + col.getName() + " depth : " + col.getDepth());
        }
        List<FmsRegion> regions = fmsData.getRegionCascade(source.getRegionId());
        for (FmsRegion region : regions) {
            System.out.println("Region : " + region.getName() + " depth : " + region.getDepth());
        }
    }

    @Test
    public void testApcColumnIsNull() {
        FmsData fmsData = FmsData.getInstance();
        String jobname = "cbt_com_cn_12977";
        System.out.println("Jobname : " + jobname);
        FmsChannel chnl = fmsData.getChannel(jobname);
        System.out.println("Channel id : " + chnl.getId());
        System.out.println("Channel name : " + chnl.getName());
        FmsSource source = fmsData.getSource(chnl.getSourceId());
        System.out.println("Source id : " + source.getId());
        System.out.println("Source name : " + source.getName());
        System.out.println("Source big kind : " + source.getBigKind());
        System.out.println("Source small kind : " + source.getSmallKind());
        System.out.println("Source url : " + source.getUrl());
        System.out.println("Source core : " + source.getCore());
        System.out.println("Source expertise : " + source.getSourceExpertise());
        System.out.println("Source influence : " + source.getSourceInfluence());
        System.out.println("Source originality : " + source.getSourceOriginality());
        assert (null == chnl.getApcColumnId());
    }

    @Test
    public void testFBFmsChannel() {
        Doc doc = new Doc();
        doc.addField("DRETITLE", "title here");
        doc.addField("DRECONTENT", "content here");
        doc.addField("AUTHOR", "张三");
        doc.addField("PUBLISHDATE", "2012-04-06 07:59");
        doc.addField("JOBNAME", "bank_hexun_com_20552");
        doc.addField("DREREFERENCE", "http://news.dichan.sina.com.cn/2012/03/05/451315.html?source=rss");
        FBFmsChannel fb = new FBFmsChannel();
        fb.doc = doc;
        fb.addTag();
        assert ("金融市场".equals(doc.getValue("REGALCHANNEL_ONE")));
        System.out.print(DocumentUtil.getXMLString(doc.toW3CDocument()));
        DocToIdxFB fb2 = new DocToIdxFB();
        fb2.doc = doc;
        fb2.convert();
        Idx idx = fb2.idx;
        System.out.println(idx.toString());
    }
}
