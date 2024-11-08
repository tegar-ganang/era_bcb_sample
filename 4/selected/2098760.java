package org.rubypeople.rdt.debug.core.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.rubypeople.rdt.internal.debug.core.RubyDebuggerProxy;
import org.rubypeople.rdt.internal.debug.core.RubyExceptionBreakpoint;
import org.rubypeople.rdt.internal.debug.core.model.ThreadInfo;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class FTC_DebuggerProxyTest extends TestCase {

    private PrintWriter writer;

    private RubyDebuggerProxy proxy;

    private TestRubyDebugTarget target;

    private BufferedReader proxyOutputReader;

    public FTC_DebuggerProxyTest(String name) {
        super(name);
    }

    protected PrintWriter getPrintWriter() {
        return writer;
    }

    public RubyDebuggerProxy getProxy() {
        return proxy;
    }

    public TestRubyDebugTarget getTarget() {
        return target;
    }

    public void writeToDebuggerProxy(String text) throws Exception {
        getPrintWriter().println(text);
        getPrintWriter().flush();
    }

    public String getLineFromDebuggerProxy() throws IOException {
        return proxyOutputReader.readLine();
    }

    public void setUp() throws Exception {
        target = new TestRubyDebugTarget();
        proxy = new RubyDebuggerProxy(target);
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
        proxy.setWriter(new PrintWriter(pipedOutputStream));
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance("org.kxml2.io.KXmlParser,org.kxml2.io.KXmlSerializer", null);
        XmlPullParser xpp = factory.newPullParser();
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream(outputStream);
        xpp.setInput(new InputStreamReader(inputStream));
        proxy.setXpp(xpp);
        proxy.startRubyLoop();
        writer = new PrintWriter(new OutputStreamWriter(outputStream));
        proxyOutputReader = new BufferedReader(new InputStreamReader(pipedInputStream));
    }

    public void testMultipleBreakpoints() throws Exception {
        writeToDebuggerProxy("<breakpoint file=\"\" line=\"44\" threadId=\"2\"/>");
        Thread.sleep(2000);
        assertNotNull(getTarget().getLastSuspensionPoint());
        assertEquals(44, getTarget().getLastSuspensionPoint().getLine());
        new Thread() {

            public void run() {
                try {
                    Thread.sleep(2000);
                    writeToDebuggerProxy("<breakpoint file=\"\" line=\"55\" threadId=\"2\"/>");
                    writeToDebuggerProxy("<threads><thread id=\"1\" status=\"sleep\"/></threads>");
                } catch (Exception ex) {
                    fail();
                }
            }
        }.start();
        ThreadInfo[] threadInfos = getProxy().readThreads();
        assertEquals(1, threadInfos.length);
        Thread.sleep(1000);
        assertEquals(55, getTarget().getLastSuspensionPoint().getLine());
    }

    public void testExceptionBreakpoint() throws IOException, CoreException {
        RubyExceptionBreakpoint rubyExceptionBreakpoint = new RubyExceptionBreakpoint("MyStandardError");
        proxy.addBreakpoint(rubyExceptionBreakpoint);
        proxy.getWriter().flush();
        assertEquals("cont", this.getLineFromDebuggerProxy());
        assertEquals("catch MyStandardError", this.getLineFromDebuggerProxy());
    }
}
