package com.store;

import com.jedi.BaseObj;
import com.jedi.KeyGen;
import com.tss.util.DbConn;
import com.tss.util.DbRs;
import com.tss.util.TSSDate;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Article extends BaseObj {

    public Article() {
    }

    public Article(String id) {
        this.articleId = id;
    }

    public void increase() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            if (getId().trim().equals("")) {
                setErr("û��Ҫ���µļ�¼ID��");
                return;
            }
            String sql = "update t_article_info set" + " hit = hit + 1 where article_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            setId(KeyGen.nextID(""));
            sql = "select * from t_article_category where category_id = ?";
            conn.prepare(sql);
            conn.setString(1, getCategoryId());
            DbRs rs = conn.executeQuery();
            if (rs == null || rs.size() == 0) {
                setErr("ѡ��ķ��಻����!");
                return;
            }
            String is_leaf = get(rs, 0, "is_leaf");
            if (!is_leaf.trim().equals("1")) {
                setErr("ѡ��ķ��಻��δ������!");
                return;
            }
            conn.setAutoCommit(false);
            sql = "insert into t_article_info (" + "article_id,article_title,author_id,author_name,article_content," + "category_id,issue_time,hit,doc_info" + ") values (?,?,?,?,?,?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.setString(2, getArticleTitle());
            conn.setString(3, getAuthorId());
            conn.setString(4, getAuthorName());
            conn.setString(5, getArticleContent());
            conn.setString(6, getCategoryId());
            conn.setString(7, TSSDate.fullTime());
            conn.setInt(8, 0);
            conn.setString(9, getDocInfo());
            conn.executeUpdate();
            sql = "update userinfo set article_num = article_num + 1 where user_id = ?";
            conn.prepare(sql);
            conn.setString(1, getAuthorId());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                ex.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void update() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            if (getId().trim().equals("")) {
                setErr("û��Ҫ���µļ�¼ID��");
                return;
            }
            String sql = "update t_article_info set" + " article_title = ?,author_id = ?,author_name = ?," + " article_content = ?,category_id = ?,issue_time = ?,doc_info = ?" + " where article_id = ?";
            conn.prepare(sql);
            conn.setString(1, getArticleTitle());
            conn.setString(2, getAuthorId());
            conn.setString(3, getAuthorName());
            conn.setString(4, getArticleContent());
            conn.setString(5, getCategoryId());
            conn.setString(6, TSSDate.fullTime());
            conn.setString(7, getDocInfo());
            conn.setString(8, getId());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public void delete() {
        clearErr();
        DbConn conn = new DbConn();
        DbRs rs = null;
        try {
            String sql = "";
            if (getId().trim().equals("")) {
                setErr("û��Ҫɾ��ļ�¼!");
                return;
            }
            conn.setAutoCommit(false);
            sql = "delete from t_article_info where article_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            sql = "update userinfo set article_num = article_num - 1 where user_id = ?";
            conn.prepare(sql);
            conn.setString(1, getAuthorId());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public String getId() {
        return articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getArticleContent() {
        return articleContent;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public ArticleCategory getCategory() {
        return cate;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public int getHit() {
        return hit;
    }

    public String getDocInfo() {
        return docInfo;
    }

    public void setId(String articleId) {
        this.articleId = articleId;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setArticleContent(String articleContent) {
        this.articleContent = articleContent;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategory(ArticleCategory cate) {
        this.cate = cate;
    }

    public void setIssueTime(String issueTime) {
        this.issueTime = issueTime;
    }

    public void setHit(int hit) {
        this.hit = hit;
    }

    public void setDocInfo(String docInfo) {
        this.docInfo = docInfo;
    }

    private String articleId = "";

    private String articleTitle = "";

    private String authorId = "";

    private String authorName = "";

    private String articleContent = "";

    private String categoryId = "";

    private ArticleCategory cate = new ArticleCategory();

    private String issueTime = "";

    private String docInfo = "";

    private int hit = 0;
}
