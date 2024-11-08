package org.dwgsoftware.raistlin.composition.data.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.dwgsoftware.raistlin.composition.data.builder.ContainmentProfileCreator;
import org.apache.avalon.framework.configuration.Configuration;
import org.dwgsoftware.raistlin.composition.data.ContainmentProfile;
import org.dwgsoftware.raistlin.meta.ConfigurationBuilder;
import org.dwgsoftware.raistlin.util.i18n.ResourceManager;
import org.dwgsoftware.raistlin.util.i18n.Resources;
import org.xml.sax.InputSource;

/**
 * A ContainmentProfileBuilder is responsible for building {@link ContainmentProfile}
 * objects from a configuration object.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Revision: 1.1 $ $Date: 2005/09/06 00:58:17 $
 */
public final class ContainmentProfileBuilder implements ContainmentProfileCreator {

    private static final Resources REZ = ResourceManager.getPackageResources(ContainmentProfileBuilder.class);

    private XMLContainmentProfileCreator m_xml = new XMLContainmentProfileCreator();

    private final SerializedContainmentProfileCreator m_serial = new SerializedContainmentProfileCreator();

    /**
     * Create a {@link ContainmentProfile} from a stream.
     *
     * @param inputStream the stream that the resource is loaded from
     * @return the containment profile
     * @exception Exception if a error occurs during profile creation
     */
    public ContainmentProfile createContainmentProfile(InputStream inputStream) throws Exception {
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int read = 0; read >= 0; ) {
            baos.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        inputStream = new ByteArrayInputStream(baos.toByteArray());
        try {
            final ContainmentProfile profile = buildFromSerDescriptor(inputStream);
            if (null != profile) {
                return profile;
            }
        } catch (Throwable e) {
            inputStream = new ByteArrayInputStream(baos.toByteArray());
        }
        return buildFromXMLDescriptor(inputStream);
    }

    /**
     * Build ContainmentProfile from the serialized format.
     *
     * @throws Exception if an error occurs
     */
    private ContainmentProfile buildFromSerDescriptor(InputStream inputStream) throws Exception {
        return m_serial.createContainmentProfile(inputStream);
    }

    /**
     * Build ContainmentProfile from an XML descriptor.
     * @param stream the input stream
     * @throws Exception if an error occurs
     */
    private ContainmentProfile buildFromXMLDescriptor(InputStream stream) throws Exception {
        final InputSource source = new InputSource(stream);
        Configuration config = ConfigurationBuilder.build(source);
        return m_xml.createContainmentProfile(config);
    }
}
