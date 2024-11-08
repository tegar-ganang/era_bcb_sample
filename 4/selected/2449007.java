package com.microfly.core;

import com.microfly.exception.NpsException;
import com.microfly.exception.ErrorHelper;
import com.microfly.index.NormalArticle2Solr;
import com.microfly.index.IndexScheduler;
import com.microfly.event.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.io.*;

/**
 * ������
 *
 * 2008.10.18  jialin
 *  1.����ȱʡ�¼����?��IEventAction
 *  2.���Ӹ��׶ε��¼�֪ͨ
 *  3.Copy�����¼Դ����ID��
 *  4.���Դ����ID�ż��������б�
 *  
 * Description: a new publishing system
 * Copyright (c) 2007
 * @author jialin
 * @version 1.0
 */
public class NormalArticle extends Article implements IEventAction {

    private String subtitle = null;

    private String keyword = null;

    private String author = null;

    private int important = 0;

    private String source = null;

    private int validdays = 0;

    private float score = 0;

    private String creator = null;

    private String creator_cn = null;

    private String creator_fullname = null;

    private String source_id = null;

    private boolean bNew = true;

    public NormalArticle(NpsContext inCtxt, String id, String title, Topic top, String creator) {
        super(inCtxt, id, title, top);
        this.creator = creator;
        this.score = top.GetScore();
        this.bNew = false;
    }

    public NormalArticle(NpsContext inCtxt, String title, Topic top, String creator) throws NpsException {
        super(inCtxt, null, title, top);
        id = GenerateArticleID();
        this.creator = creator;
        this.score = top.GetScore();
        this.bNew = true;
    }

    public NormalArticle(NpsContext inCtxt, Topic top, ResultSet rs) throws Exception {
        super(inCtxt, rs.getString("id"), rs.getString("title"), top);
        creator = rs.getString("creator");
        score = rs.getFloat("score");
        subtitle = rs.getString("subtitle");
        keyword = rs.getString("keyword");
        author = rs.getString("author");
        important = rs.getInt("important");
        source = rs.getString("source");
        validdays = rs.getInt("validdays");
        createdate = rs.getTimestamp("createdate");
        publishdate = rs.getTimestamp("publishdate");
        state = rs.getInt("state");
        source_id = rs.getString("srcid");
        SetCreator(rs.getString("uname"), rs.getString("deptname"), rs.getString("unitname"));
        rowno = rs.getRow();
        bNew = false;
    }

    public static NormalArticle GetArticle(NpsContext ctxt, String id) throws NpsException {
        NormalArticle art = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "select a.*,b.name uname,c.name deptname,d.name unitname from article a,users b,dept c,unit d where a.creator=b.id and b.dept=c.id and c.unit=d.id and a.id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                String site_id = rs.getString("siteid");
                String top_id = rs.getString("topic");
                Topic top = ctxt.GetSite(site_id).GetTopicTree().GetTopic(top_id);
                if (top == null) throw new NpsException("top id:" + top_id, ErrorHelper.SYS_NOTOPIC);
                art = new NormalArticle(ctxt, id, rs.getString("title"), top, rs.getString("creator"));
                art.subtitle = rs.getString("subtitle");
                art.keyword = rs.getString("keyword");
                art.author = rs.getString("author");
                art.important = rs.getInt("important");
                art.source = rs.getString("source");
                art.validdays = rs.getInt("validdays");
                art.createdate = rs.getTimestamp("createdate");
                art.publishdate = rs.getTimestamp("publishdate");
                art.state = rs.getInt("state");
                art.score = rs.getFloat("score");
                art.source_id = rs.getString("srcid");
                art.SetCreator(rs.getString("uname"), rs.getString("deptname"), rs.getString("unitname"));
                art.bNew = false;
                art.LoadSlaveTopics();
                art.LoadAttaches();
            }
        } catch (Exception e) {
            art = null;
            com.microfly.util.DefaultLog.error(e);
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
        return art;
    }

    public static List<NormalArticle> GetArticlesBySourceId(NpsContext ctxt, Site site, Topic topic, String source_id) throws NpsException {
        List<NormalArticle> arts = new ArrayList<NormalArticle>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "select a.*,b.name uname,c.name deptname,d.name unitname from article a,users b,dept c,unit d where a.creator=b.id and b.dept=c.id and c.unit=d.id and a.srcid=?";
            if (topic != null) sql += " and a.siteid=? and a.topic=?"; else if (site != null) sql += " and a.siteid=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            int i = 1;
            pstmt.setString(i++, source_id);
            if (topic != null) {
                pstmt.setString(i++, topic.GetSiteId());
                pstmt.setString(i++, topic.GetId());
            } else if (site != null) {
                pstmt.setString(i++, site.GetId());
            }
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Topic top = null;
                if (topic != null) {
                    top = topic;
                } else {
                    String site_id = rs.getString("siteid");
                    String top_id = rs.getString("topic");
                    top = ctxt.GetSite(site_id).GetTopicTree().GetTopic(top_id);
                    if (top == null) throw new NpsException("top id:" + top_id, ErrorHelper.SYS_NOTOPIC);
                }
                NormalArticle art = new NormalArticle(ctxt, rs.getString("id"), rs.getString("title"), top, rs.getString("creator"));
                art.subtitle = rs.getString("subtitle");
                art.keyword = rs.getString("keyword");
                art.author = rs.getString("author");
                art.important = rs.getInt("important");
                art.source = rs.getString("source");
                art.validdays = rs.getInt("validdays");
                art.createdate = rs.getTimestamp("createdate");
                art.publishdate = rs.getTimestamp("publishdate");
                art.state = rs.getInt("state");
                art.score = rs.getFloat("score");
                art.source_id = rs.getString("srcid");
                art.SetCreator(rs.getString("uname"), rs.getString("deptname"), rs.getString("unitname"));
                art.bNew = false;
                art.LoadSlaveTopics();
                art.LoadAttaches();
                arts.add(art);
            }
        } catch (Exception e) {
            arts = null;
            com.microfly.util.DefaultLog.error(e);
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
        return arts;
    }

    protected void LoadAttaches() throws NpsException {
        if (attaches != null) attaches.clear();
        if (attaches_ids != null) attaches_ids.clear();
        if (attaches == null) attaches = Collections.synchronizedList(new ArrayList(10));
        if (attaches_ids == null) attaches_ids = new Hashtable();
        PreparedStatement pstmt_atts = null;
        ResultSet rs_atts = null;
        try {
            String sql = "select * from attach where artid=? order by idx";
            pstmt_atts = ctxt.GetConnection().prepareStatement(sql);
            pstmt_atts.setString(1, id);
            rs_atts = pstmt_atts.executeQuery();
            while (rs_atts.next()) {
                Attach att = AddAttach(rs_atts.getString("id"), rs_atts.getString("showname"), rs_atts.getString("suffix"), rs_atts.getInt("idx"));
                if (rs_atts.getInt("UPLOADED") == 0) ctxt.Add2Ftp(att);
            }
            if (rs_atts != null) try {
                rs_atts.close();
            } catch (Exception e) {
            }
            if (pstmt_atts != null) try {
                pstmt_atts.close();
            } catch (Exception e) {
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (rs_atts != null) try {
                rs_atts.close();
            } catch (Exception e) {
            }
            if (pstmt_atts != null) try {
                pstmt_atts.close();
            } catch (Exception e) {
            }
        }
    }

    public void GetContent(Writer writer) throws NpsException {
        Reader r = null;
        try {
            r = GetClob("content");
            int b;
            while ((b = r.read()) != -1) {
                writer.write(b);
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (r != null) try {
                r.close();
            } catch (Exception e) {
            }
        }
    }

    public void ChangeState(int st) throws NpsException {
        if (st == ARTICLE_PUBLISH) {
            ChangeStatePublished();
            return;
        }
        switch(st) {
            case ARTICLE_SUBMIT:
                FireInsertEvent();
                break;
        }
        PreparedStatement pstmt = null;
        try {
            String sql = "update article set state=? where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setInt(1, st);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            state = st;
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e) {
            }
        }
    }

    private void ChangeStatePublished() throws NpsException {
        PreparedStatement pstmt = null;
        try {
            String sql = "update article set state=?,publishdate=? where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setInt(1, ARTICLE_PUBLISH);
            pstmt.setTimestamp(2, new java.sql.Timestamp(publishdate.getTime()));
            pstmt.setString(3, id);
            pstmt.executeUpdate();
            state = ARTICLE_PUBLISH;
            try {
                Index(ARTICLE_SOLR_ADD);
            } catch (Exception e) {
                com.microfly.util.DefaultLog.error_noexception(e);
            }
            FirePublishEvent();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e) {
            }
        }
    }

    public boolean HasField(String fieldName) {
        if (fieldName == null || fieldName.length() == 0) return false;
        String key = fieldName.trim();
        if (key.length() == 0) return false;
        key = key.toUpperCase();
        if (key.equalsIgnoreCase("art_id")) return true;
        if (key.equalsIgnoreCase("art_title")) return true;
        if (key.equalsIgnoreCase("art_subtitle")) return true;
        if (key.equalsIgnoreCase("art_important")) return true;
        if (key.equalsIgnoreCase("art_keyword")) return true;
        if (key.equalsIgnoreCase("art_author")) return true;
        if (key.equalsIgnoreCase("art_source")) return true;
        if (key.equalsIgnoreCase("art_validdays")) return true;
        if (key.equalsIgnoreCase("art_creator")) return true;
        if (key.equalsIgnoreCase("art_creatorcn")) return true;
        if (key.equalsIgnoreCase("art_creatorfn")) return true;
        if (key.equalsIgnoreCase("art_createdate")) return true;
        if (key.equalsIgnoreCase("art_publishdate")) return true;
        if (key.equalsIgnoreCase("art_url")) return true;
        if (key.equalsIgnoreCase("art_content")) return true;
        if (key.equalsIgnoreCase("rowno")) return true;
        return false;
    }

    public Object GetField(String fieldName) throws NpsException {
        if (fieldName == null || fieldName.length() == 0) return null;
        String key = fieldName.trim();
        if (key.length() == 0) return null;
        key = key.toUpperCase();
        if (key.equalsIgnoreCase("art_id")) return id;
        if (key.equalsIgnoreCase("art_title")) return title;
        if (key.equalsIgnoreCase("art_subtitle")) return subtitle;
        if (key.equalsIgnoreCase("art_important")) return new Integer(important);
        if (key.equalsIgnoreCase("art_keyword")) return keyword;
        if (key.equalsIgnoreCase("art_author")) return author;
        if (key.equalsIgnoreCase("art_source")) return source;
        if (key.equalsIgnoreCase("art_validdays")) return new Integer(validdays);
        if (key.equalsIgnoreCase("art_creator")) return GetCreatorID();
        if (key.equalsIgnoreCase("art_creatorcn")) return GetCreatorCN();
        if (key.equalsIgnoreCase("art_creatorfn")) return GetCreatorFN();
        if (key.equalsIgnoreCase("art_createdate")) return createdate;
        if (key.equalsIgnoreCase("art_publishdate")) return publishdate;
        if (key.equalsIgnoreCase("art_url")) return GetURL();
        if (key.equalsIgnoreCase("art_content")) return GetClob("content");
        if (key.equalsIgnoreCase("rowno")) return rowno;
        if (key.startsWith("TOP_") && topic.HasField(key)) return topic.GetField(key);
        if (key.startsWith("ART_TOP_") && topic.HasField(key.substring(4))) return topic.GetField(key.substring(4));
        if (key.startsWith("SITE_") && topic.GetSite().HasField(key)) return topic.GetField(key);
        if (key.startsWith("UNIT_") && topic.GetSite().GetUnit().HasField(key)) return topic.GetSite().GetUnit().GetField(key);
        if (topic.HasField(key)) return topic.GetField(key);
        if (topic.GetSite().HasField(key)) return topic.GetSite().GetField(key);
        if (ctxt.HasField(key)) return ctxt.GetField(key);
        return null;
    }

    public Attach AddAttach(String id, String showname, String suffix, int index) {
        Attach att = new NormalAttach(ctxt, this, id, showname, suffix, index);
        AddAttach(att);
        return att;
    }

    public Attach AddAttach(String showname, String suffix, int index) throws NpsException {
        String att_id = GenerateAttachID();
        return AddAttach(att_id, showname, suffix, index);
    }

    public void DeleteAttach(String att_id) throws NpsException {
        if (GetAttachById(att_id) == null) return;
        PreparedStatement pstmt = null;
        try {
            String sql = "delete from attach where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, att_id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
        super.DeleteAttach(att_id);
    }

    public void Save() throws NpsException {
        if (bNew) {
            SaveBasicInfo();
        } else {
            UpdateBasicInfo();
        }
        SaveSlaveTopics();
        SaveAttach();
        if (!bNew && state >= ARTICLE_SUBMIT && state <= ARTICLE_CHECK) {
            FireUpdateEvent();
        }
    }

    private void SaveBasicInfo() throws NpsException {
        java.sql.PreparedStatement pstmt = null;
        try {
            String sql = "insert into article(id,title,subtitle,siteid,topic,keyword,author,important,source,validdays,creator,createdate,score,content,state,srcid) values(?,?,?,?,?,?,?,?,?,?,?,?,?,empty_clob(),0,?)";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, title);
            pstmt.setString(3, subtitle);
            pstmt.setString(4, topic.GetSiteId());
            pstmt.setString(5, topic.GetId());
            pstmt.setString(6, keyword);
            pstmt.setString(7, author);
            pstmt.setInt(8, important);
            pstmt.setString(9, source);
            pstmt.setInt(10, validdays);
            pstmt.setString(11, creator);
            pstmt.setTimestamp(12, new java.sql.Timestamp(createdate.getTime()));
            pstmt.setFloat(13, score);
            pstmt.setString(14, source_id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    private void UpdateBasicInfo() throws NpsException {
        PreparedStatement pstmt = null;
        try {
            String sql = "update article set title=?,subtitle=?,siteid=?,topic=?,keyword=?,author=?,important=?,source=?,validdays=?,score=? where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, subtitle);
            pstmt.setString(3, topic.GetSiteId());
            pstmt.setString(4, topic.GetId());
            pstmt.setString(5, keyword);
            pstmt.setString(6, author);
            pstmt.setInt(7, important);
            pstmt.setString(8, source);
            pstmt.setInt(9, validdays);
            pstmt.setFloat(10, score);
            pstmt.setString(11, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    public void UpdateContent(String content) throws NpsException {
        if (content == null) return;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "update article set content=empty_clob() where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
            sql = "select content from article where id=? for update";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                oracle.sql.CLOB clob = (oracle.sql.CLOB) rs.getClob(1);
                java.io.Writer writer = clob.getCharacterOutputStream();
                writer.write(content);
                writer.flush();
                try {
                    writer.close();
                } catch (Exception e1) {
                }
            }
            Clear("content");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                rs.close();
            } catch (Exception e1) {
            }
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    public void UpdateContent(InputStreamReader reader) throws NpsException {
        if (reader == null) return;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "update article set content=empty_clob() where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
            sql = "select content from article where id=? for update";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                oracle.sql.CLOB clob = (oracle.sql.CLOB) rs.getClob(1);
                java.io.Writer writer = clob.getCharacterOutputStream();
                int read = 0;
                char[] buf = new char[1024];
                while ((read = reader.read(buf)) >= 0) {
                    writer.write(buf, 0, read);
                }
                writer.flush();
                try {
                    writer.close();
                } catch (Exception e1) {
                }
                try {
                    reader.close();
                } catch (Exception e1) {
                }
            }
            Clear("content");
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                rs.close();
            } catch (Exception e1) {
            }
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    private void SaveAttach() throws NpsException {
        if (attaches == null || attaches.isEmpty()) return;
        PreparedStatement pstmt = null;
        try {
            String sql = "insert into attach(id,artid,showname,suffix,idx) values(?,?,?,?,?)";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            for (Object obj : attaches) {
                Attach att = (Attach) obj;
                try {
                    pstmt.setString(1, att.GetID());
                    pstmt.setString(2, id);
                    pstmt.setString(3, att.GetShowName());
                    pstmt.setString(4, att.GetSuffix());
                    pstmt.setInt(5, att.GetIndex());
                    pstmt.executeUpdate();
                } catch (SQLException sql_e) {
                    if (sql_e.getErrorCode() == 1) continue;
                    throw sql_e;
                }
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    public NormalArticle Copy() throws NpsException {
        return Copy(null);
    }

    public NormalArticle Copy(Topic t, User user) throws NpsException {
        NormalArticle new_art = Copy(t);
        if (new_art != null) {
            new_art.creator = user.GetUID();
            new_art.createdate = new Date();
            new_art.SetCreator(user.GetName(), user.GetDeptName(), user.GetUnitName());
        }
        return new_art;
    }

    public NormalArticle Copy(Topic t) throws NpsException {
        if (t == null) t = topic;
        if (t.IsCustom()) throw new NpsException(t.GetName() + "/" + t.GetSite().GetName() + "(" + t.GetId() + ")", ErrorHelper.SYS_INVALIDTOPIC);
        String new_id = GenerateArticleID();
        NormalArticle new_art = new NormalArticle(ctxt, new_id, title, t, creator);
        new_art.subtitle = subtitle;
        new_art.keyword = keyword;
        new_art.author = author;
        new_art.important = important;
        new_art.source = source;
        new_art.validdays = validdays;
        new_art.score = score;
        new_art.creator_cn = creator_cn;
        new_art.creator_fullname = creator_fullname;
        new_art.createdate = createdate;
        new_art.source_id = id;
        new_art.bNew = true;
        if (attaches != null) {
            for (Object obj : attaches) {
                Attach att = (Attach) obj;
                if (att == null) continue;
                Attach new_att = new_art.AddAttach(att.GetShowName(), att.GetSuffix(), att.GetIndex());
                CopyAttachFiles(att, new_att);
                new_art.AddAttach(new_att);
            }
        }
        return new_art;
    }

    public void UpdateContent(NormalArticle art) throws NpsException {
        if (art == null) return;
        PreparedStatement pstmt = null;
        try {
            String sql = "update article set content=(select content from article where id=?) where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, art.GetId());
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
            Clear("content");
        } catch (Exception e) {
            ctxt.Rollback();
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    public void Cancel() throws NpsException {
        if (state != ARTICLE_PUBLISH) return;
        PreparedStatement pstmt = null;
        try {
            for (Object obj : attaches) {
                Attach att = (Attach) obj;
                ctxt.Add2Ftp(att.GetURL());
            }
            ctxt.Add2Ftp(GetURL());
            File local_file = GetOutputFile();
            try {
                local_file.delete();
            } catch (Exception e1) {
            }
            try {
                Index(ARTICLE_SOLR_DELETE);
            } catch (Exception e) {
                com.microfly.util.DefaultLog.error_noexception(e);
            }
            String sql = "update article set state=?,publishdate=null where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setInt(1, ARTICLE_SUBMIT);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            state = ARTICLE_SUBMIT;
            publishdate = null;
            ctxt.Commit();
        } catch (Exception e) {
            ctxt.Rollback();
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
        FireCancelEvent();
    }

    public void Delete() throws NpsException {
        PreparedStatement pstmt = null;
        try {
            if (state == ARTICLE_PUBLISH) {
                for (Object obj : attaches) {
                    Attach att = (Attach) obj;
                    ctxt.Add2Ftp(att.GetURL());
                    File local_file = att.GetOutputFile();
                    try {
                        local_file.delete();
                    } catch (Exception e1) {
                    }
                }
                ctxt.Add2Ftp(GetURL());
                File local_file = GetOutputFile();
                try {
                    local_file.delete();
                } catch (Exception e1) {
                }
            }
            if (state == ARTICLE_PUBLISH) {
                try {
                    Index(ARTICLE_SOLR_DELETE);
                } catch (Exception e) {
                    com.microfly.util.DefaultLog.error_noexception(e);
                }
            }
            String sql = "delete from attach where artid=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
            sql = "delete from article_topics where artid=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
            sql = "delete from article where id=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
            FireDeleteEvent();
        } catch (Exception e) {
            ctxt.Rollback();
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }

    public void Index(int mode) throws NpsException {
        if (Config.SOLR_URL == null || !topic.GetSite().IsFulltextIndex()) return;
        NormalArticle2Solr solr_task = null;
        solr_task = new NormalArticle2Solr(topic.GetSite().GetSolrCore(), mode);
        switch(mode) {
            case ARTICLE_SOLR_DELETE:
                solr_task.Delete(this);
                break;
            default:
                solr_task.Add(this);
                break;
        }
        IndexScheduler.GetScheduler().Add(solr_task);
    }

    public void SetTitle(String s) {
        if (s == null) return;
        title = s;
    }

    public String GetTitle() {
        return title;
    }

    public void SetSubtitle(String s) {
        subtitle = s;
    }

    public String GetSubtitle() {
        return subtitle;
    }

    public void SetKeyword(String s) {
        keyword = s;
    }

    public String GetKeyword() {
        return keyword;
    }

    public void SetAuthor(String s) {
        author = s;
    }

    public String GetAuthor() {
        return author;
    }

    public void SetImportant(int i) {
        important = i;
    }

    public int GetImportant() {
        return important;
    }

    public void SetSource(String s) {
        source = s;
    }

    public String GetSource() {
        return source;
    }

    public void SetValiddays(int i) {
        validdays = i;
    }

    public int GetValiddays() {
        return validdays;
    }

    public String GetCreatorID() {
        return creator;
    }

    public String GetCreator() {
        return GetCreatorCN();
    }

    public String GetCreatorCN() {
        return creator_cn;
    }

    public String GetCreatorFN() {
        return creator_fullname;
    }

    public void SetCreator(String uname, String deptname, String unitname) {
        creator_cn = uname;
        creator_fullname = uname + "(" + deptname + "/" + unitname + ")";
    }

    public float GetScore() {
        return score;
    }

    public void SetScore(float x) {
        score = x;
    }

    public String GetSourceId() {
        return source_id;
    }

    private String GenerateArticleID() throws NpsException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = ctxt.GetConnection().prepareStatement("select seq_art.nextval artid from dual");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("artid");
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                rs.close();
            } catch (Exception e1) {
            }
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
        return null;
    }

    private String GenerateAttachID() throws NpsException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = ctxt.GetConnection().prepareStatement("select seq_attach.nextval att_id from dual");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("att_id");
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                rs.close();
            } catch (Exception e1) {
            }
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
        return null;
    }

    public void Insert(Object observer, InsertEvent event) {
    }

    public void Update(Object observer, UpdateEvent event) {
    }

    public void Delete(Object observer, DeleteEvent event) {
    }

    public void Ready(Object observer, Ready2PublishEvent event) {
    }

    public void Publish(Object observer, PublishEvent event) {
    }

    public void Cancel(Object observer, CancelEvent event) {
    }

    protected void LoadSlaveTopics() throws NpsException {
        if (topic_slaves != null) topic_slaves.clear();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "select * from article_topics where artid=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Site site_slave = ctxt.GetSite(rs.getString("siteid"));
                if (site_slave == null) continue;
                TopicTree tree_slave = site_slave.GetTopicTree();
                if (tree_slave == null) continue;
                Topic top_slave = tree_slave.GetTopic(rs.getString("topid"));
                if (top_slave == null) continue;
                if (topic_slaves == null) topic_slaves = new Hashtable();
                topic_slaves.put(top_slave.GetId(), top_slave);
            }
            if (rs != null) try {
                rs.close();
            } catch (Exception e) {
            }
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e) {
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
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

    public void DeleteSlaveTopic(String top_id) throws NpsException {
        if (topic_slaves == null || !topic_slaves.containsKey(top_id)) return;
        PreparedStatement pstmt = null;
        try {
            String sql = "delete from article_topics where artid=? and topid=?";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, top_id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            if (pstmt != null) try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
        super.DeleteSlaveTopic(top_id);
    }

    private void SaveSlaveTopics() throws NpsException {
        if (topic_slaves == null || topic_slaves.isEmpty()) return;
        PreparedStatement pstmt = null;
        try {
            String sql = "insert into article_topics(siteid,topid,artid) values(?,?,?)";
            pstmt = ctxt.GetConnection().prepareStatement(sql);
            Enumeration enum_topics = topic_slaves.elements();
            while (enum_topics.hasMoreElements()) {
                Topic slave_topic = (Topic) enum_topics.nextElement();
                try {
                    pstmt.setString(1, slave_topic.GetSiteId());
                    pstmt.setString(2, slave_topic.GetId());
                    pstmt.setString(3, id);
                    pstmt.executeUpdate();
                } catch (SQLException sql_e) {
                    if (sql_e.getErrorCode() == 1) continue;
                    throw sql_e;
                }
            }
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error(e);
        } finally {
            try {
                pstmt.close();
            } catch (Exception e1) {
            }
        }
    }
}
