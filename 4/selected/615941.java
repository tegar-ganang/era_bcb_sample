package com.gcapmedia.dab.epg.binary;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.gcapmedia.dab.epg.Epg;
import com.gcapmedia.dab.epg.EpgMarshaller;
import com.gcapmedia.dab.epg.MarshallException;
import com.gcapmedia.dab.epg.binary.decode.EpgDeconstructor;
import com.gcapmedia.dab.epg.binary.encode.EpgConstructor;
import com.gcapmedia.dab.epg.xml.EpgXmlMarshaller;

/**
 * 
 */
public class EpgBinaryMarshaller implements EpgMarshaller {

    /**
	 * Serial version
	 */
    private static final long serialVersionUID = 465124274489815903L;

    /**
	 * @see com.gcapmedia.dab.epg.EpgMarshaller#marshall(com.gcapmedia.dab.epg.Epg)
	 */
    public byte[] marshall(Epg epg) {
        EpgConstructor constructor = new EpgConstructor();
        Element element = constructor.construct(epg);
        return element.getBytes();
    }

    /**
	 * @see com.gcapmedia.dab.epg.EpgMarshaller#unmarshall(byte[])
	 */
    public Epg unmarshall(byte[] bytes) {
        Element element = Element.fromBytes(bytes);
        if (element.getTag() != ElementTag.epg) {
            throw new UnsupportedOperationException("Toplevel tags other than 'epg' are not yet supported");
        }
        Epg epg = new EpgDeconstructor().deconstruct(element);
        return epg;
    }

    public static void main(String[] args) throws IOException, MarshallException {
        File file = new File(args[0]);
        EpgBinaryMarshaller binary = new EpgBinaryMarshaller();
        EpgXmlMarshaller xml = new EpgXmlMarshaller();
        FileChannel channel = new FileInputStream(file).getChannel();
        ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
        channel.read(buf);
        Epg epg = binary.unmarshall(buf.array());
        System.out.println(new String(xml.marshall(epg)));
    }
}
