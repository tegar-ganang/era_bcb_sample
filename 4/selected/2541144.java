package org.dfdaemon.il2.core.console;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.dfdaemon.il2.core.console.ConsoleModule;
import org.dfdaemon.il2.core.console.config.ConsoleConfig;
import org.dfdaemon.il2.core.event.Event;
import org.dfdaemon.il2.core.event.EventProcessor;
import org.dfdaemon.il2.core.event.console.ChannelCreatedEvent;
import org.dfdaemon.il2.core.event.console.ChannelLostEvent;
import org.dfdaemon.il2.core.event.console.ChatEvent;
import org.dfdaemon.il2.core.task.CallableTask;
import static org.easymock.EasyMock.getCurrentArguments;
import org.easymock.IAnswer;
import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.fail;
import org.junit.Test;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * @author aka50
 */
public class CommandExecutorTest {

    private final EventCase[] eventCases = new EventCase[] { new EventCase("Chat: Vanya: \\t<gunstat\\n\n", ChatEvent.class), new EventCase("socket channel '3', ip 127.0.1.1:21000, Vanya, is complete created\\n\n", ChannelCreatedEvent.class), new EventCase("socketConnection with 127.0.1.1:21000 on channel 3 lost.  Reason: hello\n", ChannelLostEvent.class) };

    @Test
    public void testConsoleEvents() throws InterruptedException, IOException {
        final ConsoleIoProvider ioProvider = createMock(ConsoleIoProvider.class);
        final EventProcessor eventProcessor = createMock(EventProcessor.class);
        final Injector injector = Guice.createInjector(Modules.override(new ConsoleModule()).with(new AbstractModule() {

            @Override
            protected void configure() {
                bind(ConsoleIoProvider.class).toInstance(ioProvider);
                bind(EventProcessor.class).toInstance(eventProcessor);
                bind(ConsoleConfig.class).toInstance(new ConsoleConfig());
            }
        }));
        final CallableTask console = injector.getInstance(ConsoleCommandExecutor.class);
        final Reader reader = createMock(Reader.class);
        final Writer writer = createMock(Writer.class);
        ioProvider.start();
        expect(ioProvider.getReader()).andReturn(reader).once();
        expect(ioProvider.getWriter()).andReturn(writer).once();
        for (final EventCase eventCase : eventCases) {
            expect(ioProvider.isInputValid()).andReturn(true).once();
            expect(reader.read(isA(char[].class), anyInt(), anyInt())).andAnswer(new IAnswer<Integer>() {

                public Integer answer() throws Throwable {
                    final Object[] objects = getCurrentArguments();
                    StringReader sr = new StringReader(eventCase.line);
                    sr.read((char[]) objects[0], (Integer) objects[1], (Integer) objects[2]);
                    return eventCase.line.length();
                }
            });
            eventProcessor.inject(isA(eventCase.evClass));
            expect(reader.ready()).andReturn(true).once();
        }
        expect(ioProvider.isInputValid()).andReturn(false).anyTimes();
        ioProvider.stop();
        replay(ioProvider, eventProcessor, reader, writer);
        try {
            console.call();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        verify(ioProvider, eventProcessor, reader, writer);
    }

    static class EventCase {

        public final String line;

        public final Class<? extends Event> evClass;

        public EventCase(String line, Class<? extends Event> evClass) {
            this.line = line;
            this.evClass = evClass;
        }
    }
}
