package net.sf.dslrunner.difactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import net.sf.dslrunner.BoxFactory;
import net.sf.dslrunner.DslRunner;
import net.sf.dslrunner.FunctionFactory;

public final class DslRunnerFactory {

    private final BoxFactory boxFactory;

    private final FunctionFactory functionFactory;

    public DslRunnerFactory(String name) {
        this(getDslXml(name));
    }

    public DslRunnerFactory() {
        this(getDslXml());
    }

    private DslRunnerFactory(DslXml dslXml) {
        final Container container = createDslContainer(dslXml);
        boxFactory = new DIBoxFactory(dslXml.getBoxes(), container);
        functionFactory = new DIFunctionFactory(dslXml.getFunctions(), container);
    }

    private static DslXml getDslXml(String name) {
        try {
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(new StringBuilder(name).append(".dsl").toString());
            return new DslXmlParser().parse(read(stream));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static DslXml getDslXml() {
        try {
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/dsl.xml");
            return new DslXmlParser().parse(read(stream));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String read(InputStream stream) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(stream);
        ByteBuffer buf = ByteBuffer.allocateDirect(1024);
        byte[] array = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = channel.read(buf)) >= 0) {
            buf.rewind();
            buf.get(array, 0, read);
            baos.write(array, 0, read);
            buf.rewind();
        }
        baos.close();
        channel.close();
        return new StringBuilder(baos.toString("UTF-8")).toString();
    }

    private Container createDslContainer(DslXml dslXml) {
        final Container container = new Container();
        for (BoxSettings box : dslXml.getBoxes()) container.add(box.getClazz());
        for (FunctionSettings function : dslXml.getFunctions()) container.add(function.getClazz());
        for (String dependency : dslXml.getDependencies()) container.add(dependency);
        return container;
    }

    public DslRunner createDslRunner() {
        return new DslRunner(boxFactory, functionFactory);
    }
}
