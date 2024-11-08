package evolaris.mgbl.gamemgmt.business;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import si.unimb.cot.mgbl.gamemgmt.datamodel.GameHiber;
import evolaris.framework.blog.datamodel.Article;
import evolaris.framework.blog.datamodel.Blog;
import evolaris.framework.blog.datamodel.Label;
import evolaris.framework.sys.business.ApplicationManager;
import evolaris.framework.sys.business.GroupManager;
import evolaris.framework.sys.business.PermissionManager;
import evolaris.framework.sys.business.exception.ConfigurationException;
import evolaris.framework.sys.datamodel.Application;
import evolaris.framework.um.datamodel.Group;
import evolaris.framework.um.datamodel.Role;
import evolaris.framework.um.datamodel.User;

/**
 * This class links the game management to the general framework database tables
 * @author richard.hable
 *
 */
public class DbManager {

    public static final String MGBL_GROUPNAME = "mGBL";

    public static final String MGBL_GAMEINSTANCES_BLOG_CODE = "mgblgameinstances";

    @SuppressWarnings("unused")
    private Locale locale;

    private Session session;

    /**
	 * @param locale  locale to be used for exception messages, if necessary
	 * @param session Hibernate session for database access
	 */
    public DbManager(Locale locale, Session session) {
        this.locale = locale;
        this.session = session;
    }

    private Group mgblGroup() {
        GroupManager groupManager = new GroupManager(locale, session);
        Group group = groupManager.getGroup(MGBL_GROUPNAME);
        if (group == null) {
            throw new ConfigurationException("No group with mGBL groupname `" + MGBL_GROUPNAME + "` found");
        }
        return group;
    }

    /**
	 * Creates a new application entry in the database with all required additional references 
	 * @param name  name of the new application
	 * @param description optional description of the application (may be null)
	 * @return the created application object containing a valid database ID
	 */
    public Application createApplication(String name) {
        Group group = mgblGroup();
        Application application = new Application();
        application.setGroup(group);
        application.setName(name);
        ApplicationManager applicationManager = new ApplicationManager(locale, session);
        applicationManager.createApplication(application);
        session.saveOrUpdate(application);
        return application;
    }

    /**
	 * Creates a new blog in the database with explicitly passed access rights
	 * 
	 * @deprecated use createGameBlogArticle() instead
	 * 
	 * @param name  name of the new blog
	 * @param readRoles  roles a user has to be assigned to in order to read within the blog; may be empty or null
	 * @param writeRoles roles a user has to be assigned to in order to write within the blog; may be empty or null
	 * @param commentRoles roles a user has to be assigned to in order to comment within the blog; may be empty or null
	 * @return the new blog object containing a valid database ID
	 */
    public Blog createBlog(String name, List<Role> readRoles, List<Role> writeRoles, List<Role> commentRoles) {
        Group group = mgblGroup();
        Blog blog = new Blog();
        blog.setName(name);
        blog.setGroup(group);
        blog.setCode("blogcode" + (new Date().getTime()));
        PermissionManager permissionManager = new PermissionManager(locale, session);
        blog.setAccessControlledClass(permissionManager.getAccessControlledClass("BLOG"));
        blog.setCreatedAt(new Date());
        session.save(blog);
        for (Role readRole : readRoles) {
            permissionManager.setRolePermission(blog, readRole, PermissionManager.READ_PERMISSION);
        }
        for (Role writeRole : writeRoles) {
            permissionManager.setRolePermission(blog, writeRole, PermissionManager.WRITE_PERMISSION);
        }
        for (Role commentRole : commentRoles) {
            permissionManager.setRolePermission(blog, commentRole, PermissionManager.ADD_COMMENT_PERMISSION);
        }
        return blog;
    }

    /**
	 * Create a new article in the game-instances blog.
	 * Please always provide at least the gameType of the template, the game is based on,
	 * as this is required in the game-selection tool to attach the game to the correct game type.
	 * 
	 * @param game The game this article is for
	 * @param author The currently logged in user
	 * @param labels An array of labels, used to tag the article.  
	 * @return
	 */
    public Article createGameBlogArticle(GameHiber game, User author, String[] labels) {
        Blog gameBlog = getGameBlog();
        Article article = new Article();
        article.setTitle(game.getName());
        article.setContent(game.getDescription());
        article.setAuthor(author);
        article.setCreatedAt(new Date());
        article.setBlog(gameBlog);
        gameBlog.getArticles().add(article);
        session.save(article);
        session.saveOrUpdate(gameBlog);
        article.setLabels(new HashSet<Label>());
        if (labels != null && labels.length > 0) {
            for (String s : labels) {
                s = s.replaceAll("[\"',;]", "");
                Label label = getLabel(gameBlog, s);
                if (label == null) {
                    label = new Label();
                    label.setLabel(s);
                    label.setBlog(article.getBlog());
                    label.getArticles().add(article);
                    addLabel(gameBlog, label);
                }
                article.getLabels().add(label);
                label.getArticles().add(article);
            }
            session.update(article);
        }
        return article;
    }

    private Blog getGameBlog() {
        Group group = mgblGroup();
        Query q = session.createQuery("from Blog b where b.code=:CODE and b.group=:GROUP");
        q.setParameter("CODE", MGBL_GAMEINSTANCES_BLOG_CODE);
        q.setEntity("GROUP", group);
        try {
            return (Blog) q.list().get(0);
        } catch (Exception e) {
            throw new ConfigurationException("No blog with code `" + MGBL_GAMEINSTANCES_BLOG_CODE + "` found for group `" + MGBL_GROUPNAME + "`");
        }
    }

    private Label getLabel(Blog blog, String label) {
        Criteria crit = session.createCriteria(Label.class).add(Restrictions.eq("blog", blog)).add(Restrictions.ilike("label", label, MatchMode.EXACT));
        List list = crit.setMaxResults(1).list();
        if (list != null && list.size() > 0) {
            return (Label) list.get(0);
        } else {
            return null;
        }
    }

    private Label addLabel(Blog blog, Label label) {
        label.setBlog(blog);
        blog.getLabels().add(label);
        session.save(label);
        session.saveOrUpdate(blog);
        return label;
    }
}
