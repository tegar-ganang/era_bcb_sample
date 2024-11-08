import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.makeThreadSafe;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import com.google.testing.threadtester.Script;
import com.google.testing.threadtester.ScriptedTask;
import com.google.testing.threadtester.Scripter;
import com.google.testing.threadtester.ThreadedTest;
import com.google.testing.threadtester.ThreadedTestRunner;
import junit.framework.TestCase;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tests {@link UserCache} using a {@link Script}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UserCacheTest extends TestCase {

    private static final String USER = "user";

    private static final String AVATAR1 = "A1";

    private static final String AVATAR2 = "A2";

    private UserDb db;

    private RenderingContext context;

    private UserCache cache;

    private void createCache() {
        db = createMock(UserDb.class);
        makeThreadSafe(db, true);
        context = createMock(RenderingContext.class);
        makeThreadSafe(context, true);
        expect(db.getAvatar(USER)).andReturn(AVATAR1);
        replay(db, context);
        cache = new UserCache(db, USER, context);
        verify(db, context);
    }

    private void resetMocks() {
        reset(db);
        makeThreadSafe(db, true);
        reset(context);
        makeThreadSafe(context, true);
    }

    /** Simple test for non-threaded operation */
    public void testUserCacheBasicOperation() {
        createCache();
        resetMocks();
        context.draw(AVATAR1);
        replay(db, context);
        cache.drawUserAvatar();
        verify(db, context);
        resetMocks();
        db.update(USER);
        expect(db.getAvatar(USER)).andReturn(AVATAR2);
        context.draw(AVATAR2);
        replay(db, context);
        cache.updateUser();
        cache.drawUserAvatar();
        verify(db, context);
    }

    /** Runner for the threaded tests */
    public void testThreadedTests() {
        ThreadedTestRunner runner = new ThreadedTestRunner();
        runner.setDebug(false);
        runner.runTests(getClass(), UserCache.class);
    }

    /**
   * Multithreaded test demonstrating the use of scripts.
   */
    @ThreadedTest
    public void runUserCacheMultiThreaded() throws Exception {
        createCache();
        resetMocks();
        db.update(USER);
        expect(db.getAvatar(USER)).andReturn(AVATAR2);
        context.draw(AVATAR2);
        replay(db, context);
        cache.updateUser();
        final Script<UserCache> main = new Script<UserCache>(cache);
        final Script<UserCache> second = new Script<UserCache>(main);
        final UserCache control = main.object();
        final UserDb dbTarget = main.createTarget(UserDb.class);
        final RenderingContext contextTarget = main.createTarget(RenderingContext.class);
        final ReadWriteLock lockTarget = main.createTarget(ReadWriteLock.class);
        control.updateCache();
        main.inLastMethod().beforeCalling(dbTarget.getAvatar("")).releaseTo(second);
        control.drawUserAvatar();
        main.inLastMethod();
        contextTarget.draw("");
        main.beforeCallingLastMethod().releaseTo(second);
        main.addTask(new ScriptedTask<UserCache>() {

            @Override
            public void execute() {
                cache.drawUserAvatar();
            }
        });
        second.addTask(new ScriptedTask<UserCache>() {

            @Override
            public void execute() {
                ReentrantReadWriteLock lock = cache.rwl;
                System.out.printf("First release - write = %s, num readers = %d\n", lock.isWriteLocked(), lock.getReadLockCount());
                releaseTo(main);
                System.out.printf("Second release - write = %s, num readers = %d\n", lock.isWriteLocked(), lock.getReadLockCount());
                releaseTo(main);
            }
        });
        new Scripter<UserCache>(main, second).execute();
    }
}
