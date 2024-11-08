package org.gzigzag.impl;

import org.gzigzag.*;
import org.gzigzag.mediaserver.Mediaserver;
import java.util.*;
import java.io.*;

/** A class for merge that makes sure a cell with a given Id exists in another
 *  space. If the cell does not exists, a new one <em>with the same id</em>
 *  is created; in the case of a span transclusion cell, that whole span
 *  is transcluded.
 */
class GZZ1Ugliness {

    String rcsid = "$Id: GZZ1Ugliness.java,v 1.10 2002/03/15 18:44:16 bfallenstein Exp $";

    PermanentSpace readFrom, writeTo;

    GZZ1Ugliness(PermanentSpace readFrom, PermanentSpace writeTo) {
        this.readFrom = readFrom;
        this.writeTo = writeTo;
    }

    /** Make sure that a cell with id <code>s</code> exists in
     *  <code>writeTo</code>.
     */
    void makeExist(String s) {
        if (s.indexOf("$") < 0) {
            if (writeTo.exists(s)) return;
            writeTo.gzz1_NewCell(s);
            writeTo.newCells.add(s);
        } else {
            try {
                String tid = s.substring(0, s.lastIndexOf(";"));
                if (writeTo.getTranscludedSpans(tid) != null) return;
                for (Iterator i = readFrom.getTranscludedSpans(tid).iterator(); i.hasNext(); ) {
                    Span1D sp = (Span1D) i.next();
                    if (sp == null) throw new ZZError("ARGH. No span at: " + s);
                    String block = s.substring(s.lastIndexOf(";") + 1, s.lastIndexOf("$"));
                    Mediaserver.Id blockId = new Mediaserver.Id(block);
                    makeExist(tid);
                    writeTo.gzz1_transcludeSpan(writeTo.getCell(tid), blockId, sp.offset(), sp.offset() + sp.length() - 1);
                    writeTo.transcludedSpans.put(tid, sp);
                }
            } catch (ScrollBlockManager.CannotLoadScrollBlockException e) {
                throw new ZZError(e.getMessage());
            }
        }
    }
}
