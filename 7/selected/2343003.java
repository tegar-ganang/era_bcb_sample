package org.armedbear.j;

public final class History {

    private final int limit;

    private final String name;

    private final String[] strings;

    private int count;

    private int index;

    public History(String name) {
        this(name, 10);
    }

    public History(String name, int limit) {
        this.name = name;
        this.limit = limit;
        strings = new String[limit];
        if (name != null) {
            SessionProperties sessionProperties = Editor.getSessionProperties();
            int i;
            for (i = 0; i < limit; i++) {
                String key = "history." + name + "." + String.valueOf(i);
                String value = sessionProperties.getStringProperty(key, null);
                if (value == null) break;
                strings[i] = value;
            }
            count = i;
        }
        reset();
    }

    public int size() {
        return count;
    }

    public void save() {
        if (name != null) {
            SessionProperties sessionProperties = Editor.getSessionProperties();
            for (int i = 0; i < count; i++) {
                if (strings[i] == null) break;
                String key = "history." + name + "." + String.valueOf(i);
                sessionProperties.setStringProperty(key, strings[i]);
            }
        }
    }

    public void append(String s) {
        if (s.length() == 0) return;
        for (int i = 0; i < count; i++) {
            if (s.equals(strings[i])) {
                for (int j = i + 1; j < count; j++) strings[j - 1] = strings[j];
                --count;
                break;
            }
        }
        if (count == limit) {
            for (int i = 0; i < limit - 1; i++) strings[i] = strings[i + 1];
            --count;
        }
        strings[count++] = s;
        reset();
    }

    public String get(int i) {
        if (i < 0) return null;
        if (i > count - 1) return null;
        return strings[i];
    }

    public String getPrevious() {
        if (index > 0) return get(--index);
        return null;
    }

    public String getNext() {
        if (index < count - 1) return get(++index);
        return null;
    }

    public final void reset() {
        index = count;
    }
}
