package org.argouml.language.java.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import ru.novosoft.uml.foundation.core.MNamespace;

/**
   This class collects pieces of code when a source file is parsed,
   and then updates the file with new code from the model.
*/
public class CodePieceCollector {

    /** Code pieces the parser found. */
    private Vector codePieces;

    /**
       Constructor.
    */
    public CodePieceCollector() {
        codePieces = new Vector();
    }

    /**
       The parser adds a code piece here. The code pieces will be
       inserted in sorted order in the codePieces vector.

       @param codePiece A named code piece found in the code.
    */
    public void add(NamedCodePiece codePiece) {
        int index = 0;
        for (Iterator i = codePieces.iterator(); i.hasNext(); index++) {
            CodePiece cp = (CodePiece) i.next();
            if (cp.getStartLine() > codePiece.getStartLine() || (cp.getStartLine() == codePiece.getStartLine() && cp.getStartPosition() > codePiece.getStartPosition())) {
                break;
            }
        }
        codePieces.insertElementAt(codePiece, index);
    }

    /**
       Replace all the code pieces in a source file with new code from
       the model.

       @param source The source file.
       @param destination The destination file.
       @param mNamespace The package the source belongs to.
    */
    public void filter(File source, File destination, MNamespace mNamespace) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(source));
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
        int line = 0;
        int column = 0;
        Stack parseStateStack = new Stack();
        parseStateStack.push(new ParseState(mNamespace));
        for (Iterator i = codePieces.iterator(); i.hasNext(); ) {
            NamedCodePiece cp = (NamedCodePiece) i.next();
            while (line < cp.getStartLine()) {
                line++;
                column = 0;
                writer.write(reader.readLine());
                writer.newLine();
            }
            while (column < cp.getStartPosition()) {
                writer.write(reader.read());
                column++;
            }
            cp.write(writer, parseStateStack, column);
            while (line < cp.getEndLine()) {
                line++;
                column = 0;
                reader.readLine();
            }
            while (column < cp.getEndPosition()) {
                column++;
                reader.read();
            }
        }
        String data;
        while ((data = reader.readLine()) != null) {
            writer.write(data);
            writer.newLine();
        }
        reader.close();
        writer.close();
    }
}
