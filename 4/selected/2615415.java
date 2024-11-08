package com.volantis.testtools.mock.impl.concurrency;

import com.volantis.testtools.mock.Expectation;
import com.volantis.testtools.mock.ExpectationBuilder;
import com.volantis.testtools.mock.ExpectationContainer;
import com.volantis.testtools.mock.concurrency.PerThreadExpectationBuilder;
import com.volantis.testtools.mock.concurrency.ThreadMatcher;
import com.volantis.testtools.mock.expectations.Expectations;
import com.volantis.testtools.mock.impl.ExpectationBuilderImpl;
import com.volantis.testtools.mock.method.MethodCall;
import com.volantis.testtools.mock.method.Occurrences;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Iterator;

/**
 * Encapsulates a set of per thread expectation builders.
 *
 * <p>This is intended to be used when a mock is going to be used from a number
 * of different threads where each thread is responsible for doing a specific
 * job. e.g. It is suitable for
 */
public class PerThreadExpectationBuilderImpl extends ExpectationBuilderImpl implements PerThreadExpectationBuilder {

    private final List outstandingBindings;

    /**
     * A Map from thread to ExpectationBuilder.
     */
    private final Map boundThreads2Builder;

    private ExpectationBuilder currentBuilder;

    /**
     * Initialise.
     */
    public PerThreadExpectationBuilderImpl() {
        this(null);
    }

    /**
     * Initialise.
     *
     * @param description A description of the builder, for debugging purposes.
     */
    public PerThreadExpectationBuilderImpl(String description) {
        super(description == null ? null : "PerThreadBuilder: " + description);
        this.outstandingBindings = new ArrayList();
        this.boundThreads2Builder = new HashMap();
    }

    public synchronized void addThreadSpecificBuilder(ThreadMatcher matcher, ExpectationBuilder builder) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher cannot be null");
        }
        if (builder == null) {
            throw new IllegalArgumentException("builder cannot be null");
        }
        ThreadBinder binder = new ThreadBinder(matcher, builder);
        ListIterator iterator = outstandingBindings.listIterator(outstandingBindings.size());
        while (iterator.hasPrevious()) {
            ThreadBinder existing = (ThreadBinder) iterator.previous();
            int result = binder.matcher.compareTo(existing.matcher);
            if (result <= 0) {
                iterator.set(binder);
                iterator.add(existing);
                return;
            }
        }
        outstandingBindings.add(binder);
    }

    public Occurrences add(ThreadMatcher matcher, Expectations expectations) {
        ExpectationBuilder builder = null;
        synchronized (this) {
            for (int i = 0; i < outstandingBindings.size(); i++) {
                ThreadBinder binder = (ThreadBinder) outstandingBindings.get(i);
                if (binder.matcher == matcher) {
                    builder = binder.builder;
                }
            }
        }
        if (builder == null) {
            throw new IllegalStateException("Could not find builder that is bound to " + matcher);
        }
        ExpectationBuilder oldBuilder = currentBuilder;
        this.currentBuilder = builder;
        try {
            return super.add(expectations);
        } finally {
            this.currentBuilder = oldBuilder;
        }
    }

    public void add(Expectation expectation) {
        ExpectationBuilder builder = currentBuilder;
        if (builder == null) {
            builder = getThreadSpecificBuilder();
        }
        builder.add(expectation);
    }

    public Object doMethodCall(MethodCall methodCall) throws Throwable {
        ExpectationContainer threadContainer = getThreadSpecificBuilder();
        return threadContainer.doMethodCall(methodCall);
    }

    private synchronized ExpectationBuilder getThreadSpecificBuilder() {
        Thread thread = Thread.currentThread();
        ExpectationBuilder builder = (ExpectationBuilder) boundThreads2Builder.get(thread);
        if (builder != null) {
            return builder;
        }
        for (Iterator i = outstandingBindings.iterator(); i.hasNext(); ) {
            ThreadBinder binder = (ThreadBinder) i.next();
            if (binder.matcher.matches(thread)) {
                builder = binder.builder;
                boundThreads2Builder.put(thread, builder);
                i.remove();
                return builder;
            }
        }
        if (builder == null) {
            throw new IllegalStateException("Could not find expectations for thread '" + thread + "'");
        }
        return builder;
    }

    public void dump(Writer writer) throws IOException {
        ExpectationContainer threadContainer = getThreadSpecificBuilder();
        threadContainer.dump(writer);
    }

    public void verify() {
    }

    private static class ThreadBinder {

        private final ThreadMatcher matcher;

        private final ExpectationBuilder builder;

        public ThreadBinder(ThreadMatcher matcher, ExpectationBuilder builder) {
            this.matcher = matcher;
            this.builder = builder;
        }
    }
}
