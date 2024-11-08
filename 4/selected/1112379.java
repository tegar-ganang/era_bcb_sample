package com.tredart.aspects;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import com.tredart.strategy.impl.SimpleMovingAverageStrategy;
import com.tredart.utils.TimeStamp;

/**
 * Aspect to provide reporting around the SimpleMovingAverageStrategy. See
 * src/main/resources/META-INF/aop.xml for the definition of the concrete
 * aspects.<br/>
 * This class is abstract to allow run-time weaving of the aspect.<br/>
 * <br/>
 * In order to enable runtime weaving the aspectj agent needs to loaded by the
 * JVM, to do this add the following VM argument:<br/>
 * <br/>
 * <code>
 * -javaagent:&lt;path-to-aspectj&gt;/aspectjweaver&lt;version&gt;.jar
 * </code>
 * 
 * @author gnicoll
 */
@Aspect
public abstract class AbstractSimpleMovingAverageAspect {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final long NOT_SET = -1;

    private static final String FILE_NAME = "~/results/SMA";

    private static final String FILE_EXT = ".csv";

    private StringBuilder info = new StringBuilder();

    private long lastStart = NOT_SET;

    /**
     * Point cut for the clear method.
     */
    @Pointcut
    public abstract void scopeClear();

    /**
     * After advice for the clear method.
     * 
     * @param strategy
     *            the target strategy
     */
    @After("scopeClear() && target(strategy)")
    public void afterClear(final SimpleMovingAverageStrategy strategy) {
        if (lastStart == NOT_SET) {
            return;
        }
        final long diffMs = System.currentTimeMillis() - lastStart;
        final long diffSecs = diffMs / 1000;
        final int movingAverageLength = strategy.getMovingAverageLength();
        final String fileName = FILE_NAME + "_" + movingAverageLength + FILE_EXT;
        System.out.println("Finished for moving ave " + movingAverageLength + " in " + diffMs + "ms = " + diffSecs + "s");
        System.out.println("Writing to file " + fileName);
        try {
            FileOutputStream fos = null;
            FileChannel channel = null;
            try {
                fos = new FileOutputStream(fileName);
                channel = fos.getChannel();
                final ByteBuffer byteBuff = ByteBuffer.wrap(info.toString().getBytes());
                channel.write(byteBuff);
            } finally {
                if (channel != null) {
                    channel.close();
                } else if (fos != null) {
                    fos.close();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        info = new StringBuilder();
        lastStart = NOT_SET;
    }

    /**
     * Point cut for the run method.
     */
    @Pointcut
    public abstract void scopeRun();

    /**
     * Before advice for the run method.
     * 
     * @param strategy
     *            the target strategy
     * @param ts
     *            the timestamp passed into the run method
     */
    @Before("scopeRun() && target(strategy) && args(ts)")
    public void beforeRun(final SimpleMovingAverageStrategy strategy, final TimeStamp ts) {
        if (lastStart == NOT_SET) {
            lastStart = System.currentTimeMillis();
        }
    }

    /**
     * After advice for the run method.
     * 
     * @param strategy
     *            the target strategy
     * @param ts
     *            the timestamp passed into the run method
     */
    @After("scopeRun() && target(strategy) && args(ts)")
    public void afterRun(final SimpleMovingAverageStrategy strategy, final TimeStamp ts) {
        info.append(strategy.getOpenPnL(ts)).append(",");
        info.append(strategy.getMovingAverage()).append(",");
        info.append(strategy.getLastPrice()).append(",");
        info.append(strategy.getProduct()).append(",");
        info.append(ts).append(NEW_LINE);
    }
}
