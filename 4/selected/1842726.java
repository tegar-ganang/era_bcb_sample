package org.jprovocateur.serializer.converters.extended;

import javax.security.auth.Subject;
import org.jprovocateur.serializer.converters.MarshallingContext;
import org.jprovocateur.serializer.converters.UnmarshallingContext;
import org.jprovocateur.serializer.converters.collections.AbstractCollectionConverter;
import org.jprovocateur.serializer.io.HierarchicalStreamReader;
import org.jprovocateur.serializer.io.HierarchicalStreamWriter;
import org.jprovocateur.serializer.mapper.Mapper;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Converts a {@link Subject} instance. Note, that this Converter does only convert the contained Principals as
 * it is done by JDK serialization, but not any credentials. For other behaviour you can derive your own converter,
 * overload the appropriate methods and register it in the {@link org.jprovocateur.serializer.XStream}.
 *
 * @author J&ouml;rg Schaible
 * @since 1.1.3
 */
public class SubjectConverter extends AbstractCollectionConverter {

    public SubjectConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return type.equals(Subject.class);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Subject subject = (Subject) source;
        marshalPrincipals(subject.getPrincipals(), writer, context);
        marshalPublicCredentials(subject.getPublicCredentials(), writer, context);
        marshalPrivateCredentials(subject.getPrivateCredentials(), writer, context);
        marshalReadOnly(subject.isReadOnly(), writer);
    }

    protected void marshalPrincipals(Set principals, HierarchicalStreamWriter writer, MarshallingContext context) {
        writer.startNode("principals");
        for (final Iterator iter = principals.iterator(); iter.hasNext(); ) {
            final Object principal = iter.next();
            writeItem(principal, context, writer);
        }
        writer.endNode();
    }

    ;

    protected void marshalPublicCredentials(Set pubCredentials, HierarchicalStreamWriter writer, MarshallingContext context) {
    }

    ;

    protected void marshalPrivateCredentials(Set privCredentials, HierarchicalStreamWriter writer, MarshallingContext context) {
    }

    ;

    protected void marshalReadOnly(boolean readOnly, HierarchicalStreamWriter writer) {
        writer.startNode("readOnly");
        writer.setValue(String.valueOf(readOnly));
        writer.endNode();
    }

    ;

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Set principals = unmarshalPrincipals(reader, context);
        Set publicCredentials = unmarshalPublicCredentials(reader, context);
        Set privateCredentials = unmarshalPrivateCredentials(reader, context);
        boolean readOnly = unmarshalReadOnly(reader);
        return new Subject(readOnly, principals, publicCredentials, privateCredentials);
    }

    protected Set unmarshalPrincipals(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return populateSet(reader, context);
    }

    ;

    protected Set unmarshalPublicCredentials(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return Collections.EMPTY_SET;
    }

    ;

    protected Set unmarshalPrivateCredentials(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return Collections.EMPTY_SET;
    }

    ;

    protected boolean unmarshalReadOnly(HierarchicalStreamReader reader) {
        reader.moveDown();
        boolean readOnly = Boolean.getBoolean(reader.getValue());
        reader.moveUp();
        return readOnly;
    }

    ;

    protected Set populateSet(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Set set = new HashSet();
        reader.moveDown();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            Object elementl = readItem(reader, context, set);
            reader.moveUp();
            set.add(elementl);
        }
        reader.moveUp();
        return set;
    }
}
