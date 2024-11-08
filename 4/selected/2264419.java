package com.knowgate.scheduler.jobs;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import com.knowgate.debug.DebugFile;
import com.knowgate.dataobjs.DB;
import com.knowgate.scheduler.Atom;
import com.knowgate.scheduler.Job;
import com.knowgate.dataxslt.FastStreamReplacer;

/**
 * <p>Simple processor for PageSets with disk output</p>
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class FileDumper extends Job {

    private boolean bHasReplacements;

    private SoftReference oFileStr;

    private FastStreamReplacer oReplacer;

    public FileDumper() {
        bHasReplacements = true;
        oFileStr = null;
        oReplacer = new FastStreamReplacer();
    }

    /**
   * <p>Process PageSet pointed by Atom and dumps result to disk</p>
   * <p>Base workareas path is taken from "workareasput" property of hipergate.cnf<p>
   * <p>Processed documents are saved under /web/workareas/apps/Mailwire/html/<i>gu_pageset</i>/</p>
   * @param oAtm Atom holding a reference to PageSet instance to be dumped<br>
   * Atom must have the following parameters set:<br>
   * <table border=1 cellpadding=4>
   * <tr><td>gu_workarea</td><td>GUID of WorkArea owner of document to be saved</td></tr>
   * <tr><td>gu_pageset</td><td>GUID of PageSet to be saved</td></tr>
   * <tr><td>nm_pageset</td><td>Name of PageSet document instance to be saved</td></tr>
   * </table>
   * @return String containing the final pos-processed document
   * @throws IOException
   */
    public Object process(Atom oAtm) throws IOException {
        File oFile;
        FileReader oFileRead;
        String sPathHTML;
        char cBuffer[];
        Object oReplaced;
        final String sSep = System.getProperty("file.separator");
        if (DebugFile.trace) {
            DebugFile.writeln("Begin FileDumper.process([Job:" + getStringNull(DB.gu_job, "") + ", Atom:" + String.valueOf(oAtm.getInt(DB.pg_atom)) + "])");
            DebugFile.incIdent();
        }
        if (bHasReplacements) {
            sPathHTML = getProperty("workareasput");
            if (!sPathHTML.endsWith(sSep)) sPathHTML += sSep;
            sPathHTML += getParameter("gu_workarea") + sSep + "apps" + sSep + "Mailwire" + sSep + "html" + sSep + getParameter("gu_pageset") + sSep;
            sPathHTML += getParameter("nm_pageset").replace(' ', '_') + ".html";
            if (DebugFile.trace) DebugFile.writeln("PathHTML = " + sPathHTML);
            oReplaced = oReplacer.replace(sPathHTML, oAtm.getItemMap());
            bHasReplacements = (oReplacer.lastReplacements() > 0);
        } else {
            oReplaced = null;
            if (null != oFileStr) oReplaced = oFileStr.get();
            if (null == oReplaced) {
                sPathHTML = getProperty("workareasput");
                if (!sPathHTML.endsWith(sSep)) sPathHTML += sSep;
                sPathHTML += getParameter("gu_workarea") + sSep + "apps" + sSep + "Mailwire" + sSep + "html" + sSep + getParameter("gu_pageset") + sSep + getParameter("nm_pageset").replace(' ', '_') + ".html";
                if (DebugFile.trace) DebugFile.writeln("PathHTML = " + sPathHTML);
                oFile = new File(sPathHTML);
                cBuffer = new char[new Long(oFile.length()).intValue()];
                oFileRead = new FileReader(oFile);
                oFileRead.read(cBuffer);
                oFileRead.close();
                if (DebugFile.trace) DebugFile.writeln(String.valueOf(cBuffer.length) + " characters readed");
                oReplaced = new String(cBuffer);
                oFileStr = new SoftReference(oReplaced);
            }
        }
        String sPathJobDir = getProperty("storage");
        if (!sPathJobDir.endsWith(sSep)) sPathJobDir += sSep;
        sPathJobDir += "jobs" + sSep + getParameter("gu_workarea") + sSep + getString(DB.gu_job) + sSep;
        FileWriter oFileWrite = new FileWriter(sPathJobDir + getString(DB.gu_job) + "_" + String.valueOf(oAtm.getInt(DB.pg_atom)) + ".html", true);
        oFileWrite.write((String) oReplaced);
        oFileWrite.close();
        iPendingAtoms--;
        if (DebugFile.trace) {
            DebugFile.writeln("End FileDumper.process([Job:" + getStringNull(DB.gu_job, "") + ", Atom:" + String.valueOf(oAtm.getInt(DB.pg_atom)) + "])");
            DebugFile.decIdent();
        }
        return oReplaced;
    }
}
