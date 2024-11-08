package gnu.beanfactory;

import java.lang.reflect.*;

/**
 * Convenience class for initializing BeanFactory.  <p>
 * The BeanFactory container will initialize itself when it is first used, 
 * similar the initialization of a 
 *  JDBC driver.  This means that the container may be inactive until
 * some event causes it to start.  This class provides a simple solution
 * to that problem.  Instead of launching your application directly, 
 * you can launch
 * this wrapper which will initialize the BeanFactory container and then 
 * launch your application.<p>
 * For instance, instead of starting your application like this:<p>
 * <blockquote><code>java mypackage.MyApplication &lt;arg1&gt; &lt;arg2&gt; ...</code></blockquote><p>
 * You can start it like this:<p>
 * <blockquote><code>java gnu.beanfactory.Startup mypackage.MyApplication &lt;arg1&gt; &lt;arg2&gt; ...</code></blockquote><p>
 * The arguments to main() are properly shifted, so it is not necessary to
 * modify the original application.
*  @author  Rob Schoening <a href="mailto:rob@beanfactory.net">&lt;rob@beanfactory.net&gt;</a>
 **/
public class Startup {

    public static void initializeAndInvokeMain(String className, String[] args) throws Exception {
        gnu.beanfactory.Container.getBeanContext();
        Class c = BeanContext.classForName(className);
        Object obj = new String[0];
        Method m = c.getMethod("main", new Class[] { obj.getClass() });
        Object[] wrappedArgs = new Object[1];
        wrappedArgs[0] = args;
        m.invoke(null, wrappedArgs);
    }

    public static void execRunnableBean(String url, String[] args) throws BeanFactoryException {
        try {
            Runnable target = (Runnable) gnu.beanfactory.Container.lookup(url);
            target.run();
        } catch (ClassCastException e) {
            throw new ClassCastException(url + " must implement java.lang.Runnable");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1) {
            System.err.println("Usage: java gnu.beanfactory.Startup [class|url] [args...]");
            System.err.println();
            return;
        }
        String target = args[0];
        String[] shifted = new String[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            shifted[i] = args[i + 1];
        }
        if (target.startsWith("bean:")) {
            execRunnableBean(target, shifted);
        } else {
            initializeAndInvokeMain(target, shifted);
        }
    }
}
