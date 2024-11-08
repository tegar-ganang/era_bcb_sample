package de.grogra.ext.sunshine.spectral.lights;

import de.grogra.ext.sunshine.spectral.shader.SunshineChannel;
import de.grogra.ext.sunshine.spectral.shader.SunshineIDChannel;
import de.grogra.imp3d.objects.AreaLight;

public class SpectralAreaLight extends AreaLight {

    SunshineChannel channel = new SunshineIDChannel();

    public static final Type $TYPE;

    public static final Type.Field channel$FIELD;

    public static class Type extends AreaLight.Type {

        public Type(Class c, de.grogra.persistence.SCOType supertype) {
            super(c, supertype);
        }

        public Type(SpectralAreaLight representative, de.grogra.persistence.SCOType supertype) {
            super(representative, supertype);
        }

        Type(Class c) {
            super(c, AreaLight.$TYPE);
        }

        private static final int SUPER_FIELD_COUNT = AreaLight.Type.FIELD_COUNT;

        protected static final int FIELD_COUNT = AreaLight.Type.FIELD_COUNT + 1;

        static Field _addManagedField(Type t, String name, int modifiers, de.grogra.reflect.Type type, de.grogra.reflect.Type componentType, int id) {
            return t.addManagedField(name, modifiers, type, componentType, id);
        }

        @Override
        protected void setObject(Object o, int id, Object value) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 0:
                    ((SpectralAreaLight) o).channel = (SunshineChannel) value;
                    return;
            }
            super.setObject(o, id, value);
        }

        @Override
        protected Object getObject(Object o, int id) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 0:
                    return ((SpectralAreaLight) o).getChannel();
            }
            return super.getObject(o, id);
        }

        @Override
        public Object newInstance() {
            return new SpectralAreaLight();
        }
    }

    public de.grogra.persistence.ManageableType getManageableType() {
        return $TYPE;
    }

    static {
        $TYPE = new Type(SpectralAreaLight.class);
        channel$FIELD = Type._addManagedField($TYPE, "channel", 0 | Type.Field.SCO, de.grogra.reflect.ClassAdapter.wrap(SunshineChannel.class), null, Type.SUPER_FIELD_COUNT + 0);
        $TYPE.validate();
    }

    public SunshineChannel getChannel() {
        return channel;
    }

    public void setChannel(SunshineChannel value) {
        channel$FIELD.setObject(this, value);
    }
}
