package org.omnidoc;

import java.io.IOException;
import org.middleheaven.io.IOUtils;
import org.middleheaven.io.ManagedIOException;
import org.middleheaven.io.repository.BufferedMediaManagedFileContent;
import org.middleheaven.io.repository.MediaManagedFileContent;

public class BinaryDocument implements Document {

    private String name;

    private BufferedMediaManagedFileContent content;

    public BinaryDocument(String name, String contentType) {
        this.name = name;
        content = new BufferedMediaManagedFileContent();
        content.setContentType(contentType);
    }

    @Override
    public String getName() {
        return name;
    }

    public MediaManagedFileContent getContent() {
        return content;
    }

    @Override
    public Document duplicate() {
        BinaryDocument b = new BinaryDocument(this.name, this.content.getContentType());
        try {
            IOUtils.copy(this.getContent().getInputStream(), this.getContent().getOutputStream());
            return b;
        } catch (IOException e) {
            throw ManagedIOException.manage(e);
        }
    }
}
