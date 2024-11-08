package com.microfly.job.sitemap;

import com.microfly.core.*;
import com.microfly.util.Utils;
import com.microfly.util.tree.Node;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import java.util.Iterator;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class GoogleSitemap {

    private NpsContext ctxt = null;

    private Site site = null;

    private Topic topic = null;

    private GoogleSitemapIndex indexer = null;

    private final int MAX_COUNT = 50000;

    private final int MAX_SIZE = 10000000;

    private int sitemap_index = 1;

    private File outputfile = null;

    private OutputStreamWriter writer = null;

    private int count = 0;

    private int size = 0;

    public GoogleSitemap(NpsContext ctxt, Topic top, GoogleSitemapIndex indexer) {
        this.ctxt = ctxt;
        this.site = top.GetSite();
        this.topic = top;
        this.indexer = indexer;
    }

    public void GenerateSitemap() throws Exception {
        GenerateSitemap(topic);
        if (outputfile != null) {
            WriteSitemapFooter();
            if (writer != null) try {
                writer.close();
            } catch (Exception e1) {
            }
            File zipfile = Gzip(outputfile);
            Add2Ftp(zipfile);
            Add2SitemapIndex(zipfile);
        }
    }

    private void GenerateSitemap(Topic top) throws Exception {
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String sql = null;
        if (top.IsCustom()) {
            sql = "select a.*,b.title,b.siteid,b.topic,b.state,b.createdate,b.publishdate from " + top.GetTable() + " a," + top.GetTable() + "_prop b where b.state=3 and a.id=b.id and b.topic='" + top.GetId() + "' order by b.publishdate desc ";
        } else {
            sql = "select a.*,b.name uname,c.name deptname,d.name unitname from article a,users b,dept c,unit d,topic e where a.topic=e.id  and b.dept=c.id and c.unit=d.id and a.creator=b.id and e.id = '" + top.GetId() + "' and a.state=3 and a.siteid='" + top.GetSiteId() + "'  order by a.publishdate desc ";
        }
        try {
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Topic top_art = site.GetTopicTree().GetTopic(rs.getString("topic"));
                Article art = null;
                if (!top_art.IsCustom()) {
                    art = new NormalArticle(ctxt, top_art, rs);
                } else {
                    art = CustomArticleHelper.GetHelper().NewInstance(ctxt, top_art, rs);
                }
                WriteSitemap(art);
            }
            Iterator top_childs = site.GetTopicTree().GetChilds(top);
            while (top_childs != null && top_childs.hasNext()) {
                Node top_node = (Node) top_childs.next();
                GenerateSitemap((Topic) top_node.GetValue());
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (rs != null) try {
                rs.close();
            } catch (Exception e) {
            }
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e) {
            }
        }
    }

    private void WriteSitemap(Article art) throws Exception {
        if (art == null) return;
        String url = GenerateURL(art);
        if (url == null) return;
        if (outputfile == null) {
            CreateSitemapFile();
            WriteSitemapHeader();
        }
        int guess_count = count + 1;
        int guess_size = size + url.getBytes().length;
        if (guess_count >= MAX_COUNT || guess_size >= MAX_SIZE) {
            sitemap_index++;
            WriteSitemapFooter();
            if (writer != null) try {
                writer.close();
            } catch (Exception e1) {
            }
            File zipfile = Gzip(outputfile);
            Add2Ftp(zipfile);
            Add2SitemapIndex(zipfile);
            CreateSitemapFile();
            WriteSitemapHeader();
        }
        WriteSitemapUrl(url);
    }

    private File CreateSitemapFile() throws Exception {
        if (writer != null) writer.close();
        File sitemap_dir = new File(site.GetArticleDir(), topic.GetPath());
        sitemap_dir.mkdirs();
        String filename = "sitemap" + sitemap_index + ".xml";
        outputfile = new File(sitemap_dir, filename);
        writer = new OutputStreamWriter(new FileOutputStream(outputfile), "UTF-8");
        return outputfile;
    }

    private void WriteSitemapHeader() throws IOException {
        count = 0;
        size = 0;
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        writer.write(header);
        size += header.getBytes().length;
        header = "<urlset xmlns=\"http://www.google.com/schemas/sitemap/0.84\">\n";
        writer.write(header);
        size += header.getBytes().length;
    }

    private void WriteSitemapUrl(String url) throws IOException {
        count++;
        size += url.getBytes().length;
        writer.write(url);
    }

    private void WriteSitemapFooter() throws IOException {
        String footer = "</urlset>";
        writer.write(footer);
        size += footer.getBytes().length;
    }

    private String GenerateURL(Article art) {
        if (art == null) return null;
        String url = "<url>\n";
        url += "<loc>" + art.GetURL() + "</loc>\n";
        url += "<lastmod>" + Utils.FormateDate(art.GetPublishDate(), "yyyy-MM-dd'T'HH:mm:ss'Z'") + "</lastmod>\n";
        url += "<changefreq>monthly</changefreq>\n";
        url += "<priority>0.5</priority>\n";
        url += "</url>\n";
        return url;
    }

    private File Gzip(File f) throws IOException {
        if (f == null || !f.exists()) return null;
        File dest_dir = f.getParentFile();
        String dest_filename = f.getName() + ".gz";
        File zipfile = new File(dest_dir, dest_filename);
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(zipfile));
        FileInputStream in = new FileInputStream(f);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        out.finish();
        try {
            in.close();
        } catch (Exception e) {
        }
        try {
            out.close();
        } catch (Exception e) {
        }
        try {
            f.delete();
        } catch (Exception e) {
        }
        return zipfile;
    }

    private void Add2Ftp(File f) {
        site.Add2Ftp(f);
    }

    private void Add2SitemapIndex(File f) throws Exception {
        if (f == null || !f.exists() || !f.isFile()) return;
        indexer.Add(f, GetSitemapURL(f));
    }

    private String GetSitemapURL(File f) {
        String url = topic.GetURL() + f.getName();
        url = url.replace('\\', '/');
        url = Utils.FixURL(url);
        return url;
    }
}
