package test.junit.filter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;
import model.Author;
import model.Book;
import model.Comment;
import model.Person;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import services.DTOAccessService;
import services.DTOService;
import blomo.dto.DTOSession;
import blomo.dto.DTOSessionFactory;
import blomo.filter.FilterManager;
import blomo.script.Scriplet;
import blomo.script.ScriptParser;
import blomo.script.scriplet.Equal;
import dtos.AuthorDTO;
import dtos.BookDTO;
import dtos.CommentDTO;
import dtos.PersonDTO;

/**
 * @author Malte Schulze
 *
 */
public class FilterCopyToTest extends TestCase {

    private static SessionFactory sf = null;

    private static DTOService dtoService = null;

    private static DTOAccessService dtoAccessService = null;

    private static Serializable pid, aid, bid, cid;

    private class FilterObjProperty extends Scriplet {

        @Override
        public Object execute(Object[] vars, Map<Object, Object> parameter) {
            if (vars.length == 2 && vars[0] != null && vars[1] != null) {
                Method method;
                try {
                    method = vars[0].getClass().getMethod((String) vars[1], new Class[0]);
                    return method.invoke(vars[0], new Object[0]);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class FilterProp extends Scriplet {

        @Override
        public Object execute(Object[] vars, Map<Object, Object> parameter) {
            if (parameter.get(vars[0]) != null) {
                return parameter.get(vars[0]);
            } else return ((DTOSession) parameter.get("dtoSession")).getParameter().get(vars[0]);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (sf == null) {
            dtoService = new DTOService();
            dtoAccessService = new DTOAccessService();
            dtoService.setDtoAccessService(dtoAccessService);
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            sf = configuration.buildSessionFactory();
        }
        Session session = sf.getCurrentSession();
        session.getTransaction().begin();
        Author author = new Author();
        Person person = new Person();
        Book book = new Book();
        Comment comment = new Comment();
        author.addAlias("test");
        author.addAwards(new Date(), "test");
        author.setPerson(person);
        person.setFirstName("test");
        person.setLastName("test");
        author.addBooks(book);
        author.addCommentBooks(comment, book);
        book.setTitle("test");
        comment.setText("test");
        session.save(comment);
        session.save(book);
        session.save(person);
        session.save(author);
        session.getTransaction().commit();
        aid = author.getEntityId();
        pid = person.getEntityId();
        bid = book.getEntityId();
        cid = comment.getEntityId();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Session session = sf.getCurrentSession();
        session.getTransaction().begin();
        Author author = (Author) session.load(Author.class, aid);
        Iterator<Comment> it2 = author.getCommentBooksReadOnly().keySet().iterator();
        while (it2.hasNext()) {
            Comment comment = it2.next();
            author.removeCommentBooks(comment);
            session.delete(comment);
        }
        Iterator<Book> it = author.getBooksReadOnly().iterator();
        while (it.hasNext()) {
            Book book = it.next();
            author.removeBooks(book);
            session.delete(book);
        }
        session.delete(author);
        session.delete(author.getPerson());
        session.getTransaction().commit();
        FilterManager.instance().clearFilter();
    }

    /**
	 * 
	 */
    public void testFilter() {
        ScriptParser.registerScriplet("equal", new Equal());
        ScriptParser.registerScriplet("filterPar", new FilterProp());
        FilterManager.instance().registerBLoMoFilter(AuthorDTO.class, BookDTO.class, "books", "#equal(#filterPar(key),test)", 1, null);
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getBooksReadOnly().size() == 0);
        dtoService.doSave(aAuthorDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilterCollection() {
        ScriptParser.registerScriplet("methodResult", new FilterObjProperty());
        ScriptParser.registerScriplet("filterPar", new FilterProp());
        ScriptParser.registerScriplet("equal", new Equal());
        FilterManager.instance().registerBLoMoFilter(AuthorDTO.class, BookDTO.class, "books", "#equal(#methodResult(#filterPar(entity),getTitle),bogus)", 1, null);
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getBooksReadOnly().size() == 0);
        dtoService.doSave(aAuthorDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilterAttribute() {
        ScriptParser.registerScriplet("methodResult", new FilterObjProperty());
        ScriptParser.registerScriplet("filterPar", new FilterProp());
        ScriptParser.registerScriplet("equal", new Equal());
        FilterManager.instance().registerBLoMoFilter(AuthorDTO.class, PersonDTO.class, "person", "#equal(#methodResult(#filterPar(entity),getFirstName),bogus)", 0, null);
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getPerson() == null);
        dtoService.doSave(aAuthorDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilterMap() {
        ScriptParser.registerScriplet("methodResult", new FilterObjProperty());
        ScriptParser.registerScriplet("filterPar", new FilterProp());
        ScriptParser.registerScriplet("equal", new Equal());
        FilterManager.instance().registerBLoMoFilter(AuthorDTO.class, CommentDTO.class, "commentBooks", "#equal(#methodResult(#filterPar(entity),getText),bogus)", 2, BookDTO.class);
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getCommentBooksReadOnly().size() == 0);
        dtoService.doSave(aAuthorDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilterEntity() {
        ScriptParser.registerScriplet("methodResult", new FilterObjProperty());
        ScriptParser.registerScriplet("filterPar", new FilterProp());
        ScriptParser.registerScriplet("equal", new Equal());
        FilterManager.instance().clearFilter();
        FilterManager.instance().registerClassFilter(BookDTO.class, "#equal(#methodResult(#filterPar(entity),getTitle),bogus)");
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getBooksReadOnly().size() == 0);
        assertTrue(aAuthorDto.getCommentBooksReadOnly().size() == 0);
        dtoService.doSave(aAuthorDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilterCopyOnlyRelationsTo() {
        FilterManager.instance().clearFilter();
        Session session = sf.getCurrentSession();
        session.beginTransaction();
        SQLQuery q = sf.getCurrentSession().createSQLQuery("CREATE ROLE test NOINHERIT VALID UNTIL 'infinity';");
        q.executeUpdate();
        q = session.createSQLQuery("CREATE ROLE \"loginTest\" LOGIN PASSWORD 'xxx' VALID UNTIL 'infinity';");
        q.executeUpdate();
        session.createSQLQuery("GRANT \"test\" TO \"loginTest\";").executeUpdate();
        session.createSQLQuery("GRANT ALL ON TABLE person TO \"test\";").executeUpdate();
        session.createSQLQuery("GRANT ALL ON TABLE book TO \"test\";").executeUpdate();
        session.createSQLQuery("GRANT ALL ON TABLE r_author_book TO \"test\";").executeUpdate();
        session.createSQLQuery("GRANT ALL ON TABLE comment TO \"test\";").executeUpdate();
        session.createSQLQuery("GRANT ALL ON TABLE r_comment_author_book TO \"test\";").executeUpdate();
        session.createSQLQuery("GRANT SELECT ON TABLE author TO \"test\";").executeUpdate();
        session.getTransaction().commit();
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("test", dtoAccessService));
        AuthorDTO aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getBooksReadOnly().size() > 0);
        assertTrue(aAuthorDto.getCommentBooksReadOnly().size() > 0);
        assertNotNull(aAuthorDto.getPerson());
        FilterManager.instance().clearFilter();
        FilterManager.instance().registerBLoMoFilter(AuthorDTO.class, BookDTO.class, "books", "#equal(#methodResult(#filterPar(entity),getTitle),bogus)", 1, null);
        dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("test", dtoAccessService));
        aAuthorDto = (AuthorDTO) dtoService.doLoad(aid, AuthorDTO.class, dtoSession);
        assertTrue(aAuthorDto.getBooksReadOnly().size() == 0);
        assertTrue(aAuthorDto.getCommentBooksReadOnly().size() > 0);
        assertNotNull(aAuthorDto.getPerson());
        dtoService.doSave(aAuthorDto, dtoSession);
        session = sf.getCurrentSession();
        session.beginTransaction();
        session.createSQLQuery("REVOKE ALL ON TABLE author FROM \"test\";").executeUpdate();
        session.createSQLQuery("REVOKE ALL ON TABLE person FROM \"test\";").executeUpdate();
        session.createSQLQuery("REVOKE ALL ON TABLE book FROM \"test\";").executeUpdate();
        session.createSQLQuery("REVOKE ALL ON TABLE r_author_book FROM \"test\";").executeUpdate();
        session.createSQLQuery("REVOKE ALL ON TABLE comment FROM \"test\";").executeUpdate();
        session.createSQLQuery("REVOKE ALL ON TABLE r_comment_author_book FROM \"test\";").executeUpdate();
        session.createSQLQuery("DROP ROLE \"loginTest\"").executeUpdate();
        session.createSQLQuery("DROP ROLE \"test\"").executeUpdate();
        session.getTransaction().commit();
        session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }

    /**
	 * 
	 */
    public void testFilteredRemoveAtrribute() {
        ScriptParser.registerScriplet("equal", new Equal());
        FilterManager.instance().clearFilter();
        DTOSession dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        BookDTO aBookDto = (BookDTO) dtoService.doLoad(bid, BookDTO.class, dtoSession);
        assertTrue("test".equals(aBookDto.getTitle()));
        aBookDto.setTitle("bogus");
        FilterManager.instance().registerBLoMoFilter(BookDTO.class, String.class, "title", "#equal(false,true)", 0, null);
        dtoService.doSave(aBookDto, dtoSession);
        Session session = sf.getCurrentSession();
        Transaction t = session.beginTransaction();
        t.begin();
        String fn = null, ln = null, title = null, text = null;
        boolean hasPerson = false, hasBook = false, hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("test".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
        FilterManager.instance().clearFilter();
        dtoSession = DTOSessionFactory.getSession(DTOSessionFactory.createSession("admin", dtoAccessService));
        aBookDto = (BookDTO) dtoService.doLoad(bid, BookDTO.class, dtoSession);
        assertTrue("test".equals(aBookDto.getTitle()));
        aBookDto.setTitle("bogus");
        FilterManager.instance().registerBLoMoFilter(BookDTO.class, String.class, "title", "#equal(true,true)", 0, null);
        dtoService.doSave(aBookDto, dtoSession);
        session = sf.getCurrentSession();
        t = session.beginTransaction();
        t.begin();
        fn = null;
        ln = null;
        title = null;
        text = null;
        hasPerson = false;
        hasBook = false;
        hasComment = false;
        try {
            Author a = (Author) session.load(Author.class, aid);
            Person p = (Person) session.load(Person.class, pid);
            Book b = (Book) session.load(Book.class, bid);
            Comment c = (Comment) session.load(Comment.class, cid);
            fn = p.getFirstName();
            ln = p.getLastName();
            title = b.getTitle();
            text = c.getText();
            hasBook = a.getBooksReadOnly().size() == 1;
            hasPerson = a.getPerson() != null;
            hasComment = a.getCommentBooksReadOnly().size() == 1;
            t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            t.rollback();
        }
        assertTrue("test".equals(fn));
        assertTrue("test".equals(ln));
        assertTrue("bogus".equals(title));
        assertTrue("test".equals(text));
        assertTrue(hasPerson && hasBook && hasComment);
    }
}
