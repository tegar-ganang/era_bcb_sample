package jpatch.entity;

import java.lang.reflect.Field;
import java.util.Iterator;

public abstract class AbstractJPatchXObject implements JPatchObject {

    private Iterable<ScalarAttribute> attributes = new Iterable<ScalarAttribute>() {

        public Iterator<ScalarAttribute> iterator() {
            return createAttributeIterator();
        }
    };

    private Iterable<ScalarAttribute> channels = new Iterable<ScalarAttribute>() {

        public Iterator<ScalarAttribute> iterator() {
            return createChannelIterator();
        }
    };

    public Iterable<ScalarAttribute> getAttributes() {
        return attributes;
    }

    public Iterable<ScalarAttribute> getChannels() {
        return channels;
    }

    public ScalarAttribute getAttribute(int index) {
        int i = 0;
        for (Field field : getClass().getFields()) {
            if (ScalarAttribute.class.isAssignableFrom(field.getType())) {
                if (i == index) {
                    try {
                        return (ScalarAttribute) field.get(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                i++;
            }
        }
        return null;
    }

    private Iterator<ScalarAttribute> createAttributeIterator() {
        return new Iterator<ScalarAttribute>() {

            private int index = 0;

            public boolean hasNext() {
                return getAttribute(index + 1) != null;
            }

            public ScalarAttribute next() {
                return getAttribute(index++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Iterator<ScalarAttribute> createChannelIterator() {
        return new Iterator<ScalarAttribute>() {

            private int index = searchNextChannel();

            public boolean hasNext() {
                return getAttribute(index + 1) != null;
            }

            public ScalarAttribute next() {
                ScalarAttribute a = getAttribute(index++);
                searchNextChannel();
                return a;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private int searchNextChannel() {
                ScalarAttribute a;
                for (a = getAttribute(index); a != null; index++) if (a.isKeyed()) break;
                if (a != null) index--;
                return index;
            }
        };
    }
}
