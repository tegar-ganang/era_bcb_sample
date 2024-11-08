package org.timedex.beans;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.SQLException;

public class PageText implements DBObject {

    private long id;

    private Blob text;

    private Blob flags;

    private String textString;

    /**
	 * Hibernated required
	 *
	 */
    PageText() {
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((flags == null) ? 0 : flags.hashCode());
        result = PRIME * result + (int) (id ^ (id >>> 32));
        result = PRIME * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PageText other = (PageText) obj;
        if (flags == null) {
            if (other.flags != null) return false;
        } else if (!flags.equals(other.flags)) return false;
        if (id != other.id) return false;
        if (text == null) {
            if (other.text != null) return false;
        } else if (!text.equals(other.text)) return false;
        return true;
    }

    public String getTextAsString() {
        return textString;
    }

    public Blob getText() {
        return text;
    }

    public void setText(Blob text) {
        this.text = text;
        try {
            InputStreamReader istr = new InputStreamReader(text.getBinaryStream());
            StringWriter sw = new StringWriter();
            while (istr.ready()) {
                sw.write(istr.read());
            }
            this.textString = sw.getBuffer().toString();
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid stream!", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Caught IOException!", e);
        }
    }

    public Blob getFlags() {
        return flags;
    }

    public void setFlags(Blob flags) {
        this.flags = flags;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
