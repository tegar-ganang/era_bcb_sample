package org.omnidoc.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.middleheaven.io.IOUtils;
import org.middleheaven.io.ManagedIOException;
import org.omnidoc.BinaryDocument;
import org.omnidoc.Document;
import org.omnidoc.writer.PlainXMLDocumentWriter;
import org.omnidoc.xml.XMLDOMDocument;

public class FolderTarget implements Target {

    private File parent;

    public FolderTarget(File file) {
        this.parent = file;
    }

    public static Target forForder(File file) {
        return new FolderTarget(file);
    }

    @Override
    public void setDocumentSpace(DocumentSpace space) {
        for (Document doc : space) {
            File result = new File(parent, doc.getName());
            if (doc instanceof XMLDOMDocument) {
                new PlainXMLDocumentWriter(result).writeDocument(doc);
            } else if (doc instanceof BinaryDocument) {
                BinaryDocument bin = (BinaryDocument) doc;
                try {
                    IOUtils.copy(bin.getContent().getInputStream(), new FileOutputStream(result));
                } catch (IOException e) {
                    throw ManagedIOException.manage(e);
                }
            } else {
                System.err.println("Unkown Document type");
            }
        }
    }
}
