package tools;

import tools.ReportMerge;
import tools.Log;
import tools.ProgressStatus;
import tools.ConfidenceMatch;
import tools.EntityView;
import tools.Person;
import tools.PersonFactory;
import tools.Info;
import tools.InfoFactory;
import genj.report.Report;
import genj.gedcom.Gedcom;
import genj.gedcom.Entity;
import genj.gedcom.Indi;
import genj.gedcom.Fam;
import genj.gedcom.Property;
import genj.gedcom.GedcomException;
import genj.gedcom.Fam;
import genj.gedcom.Note;
import genj.gedcom.Source;
import genj.gedcom.Repository;
import genj.gedcom.Submitter;
import genj.gedcom.TagPath;
import genj.gedcom.PropertyXRef;
import genj.gedcom.PropertyPlace;
import genj.gedcom.PropertyDate;
import genj.gedcom.time.PointInTime;
import genj.gedcom.PropertyVisitor;
import genj.option.PropertyOption;
import genj.util.swing.Action2;
import genj.util.Origin;
import genj.io.GedcomReader;
import genj.io.GedcomWriter;
import genj.io.GedcomIOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Stack;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.swing.ImageIcon;

/**
 * GenJ - Report
 * @author Frederic Lapeyre <frederic@lapeyre-frederic.com>
 * @version 1.0
 *
 */
public class MergeGedcomTool {

    private ReportMerge report = null;

    private Log log = null;

    private Gedcom gedcomOutput = null;

    private ProgressStatus progress = null;

    private boolean debug = false;

    private String[] typeEnt = { Gedcom.INDI, Gedcom.FAM, Gedcom.NOTE, Gedcom.SOUR, Gedcom.REPO, Gedcom.SUBM };

    int ANA1_WHOLE = 0, ANA1_SUBSET = 1;

    int ANA3_CONNEC = 0, ANA3_ENTONL = 1;

    int ALWAYS_A = 0, ALWAYS_B = 1, A_CONFLICT = 2, B_CONFLICT = 3, ASK_CONFLICT = 4;

    protected static final int OPTION_YESNO = 0, OPTION_OKCANCEL = 1, OPTION_OK = 2;

    public MergeGedcomTool(ReportMerge report, Log log) {
        this.report = report;
        this.log = log;
    }

    /**
  * Merge Gedcom files Main entry point
  */
    public boolean run(Gedcom gedcom) {
        Map typeEntsA = new TreeMap();
        Map typeEntsB = new TreeMap();
        Map confList = new TreeMap();
        Map overlaps = new TreeMap();
        Map scoreStats = new TreeMap();
        Map idMap = new TreeMap();
        double[] sizes = { 0, 0, 0 };
        displayOptions();
        progress = new ProgressStatus("Merging tool progress", "Merge", "", 100, "Stop");
        String fileNameA = gedcom.getOrigin().getFile().getAbsolutePath();
        log.write("Execution");
        log.write("---------");
        log.write("Copying gedcom to temporary file (A): " + fileNameA);
        log.timeStamp();
        Gedcom gedcomA = createGedcomFile(fileNameA + "~", false, gedcom, report.setting_displayDetails);
        if (gedcomA == null) return false;
        if (!copyGedcom(gedcom, gedcomA, report.setting_displayDetails)) return false;
        String subId = gedcom.getSubmitter().getId();
        gedcomA.setSubmitter((Submitter) gedcomA.getEntity(subId));
        log.timeStamp();
        log.write(" ");
        log.write("Opening (B) file");
        log.timeStamp();
        Gedcom gedcomB = getGedcomFromUser("What is the file to merge with?");
        if (gedcomB == null) return false;
        String fileNameB = gedcomB.getOrigin().getFile().getAbsolutePath();
        log.write("   (B) file " + fileNameB + " now opened.");
        log.timeStamp();
        log.write(" ");
        log.write("Making unique Ids across both files");
        log.timeStamp();
        if (!makeUniqueIds(gedcomA, gedcomB, report.setting_displayDetails)) return false;
        log.timeStamp();
        log.write(" ");
        log.write("Interaction with user to confirm gedcom Header:");
        log.write("   Checking placeformat compatibility...");
        int[] placeMap = mapPlaceFormat(gedcomA, gedcomB, report.setting_headerChosen);
        if (placeMap == null) {
            log.write("   OK. Same place format.");
        } else {
            log.write("   Remapping places before comparing.");
            log.timeStamp();
            remapPlaces((report.setting_headerChosen == 0) ? gedcomB.getEntities() : gedcomA.getEntities(), placeMap);
        }
        log.timeStamp();
        log.write(" ");
        log.write("Prepare entities to compare.");
        log.timeStamp();
        prepareSets(gedcomA, gedcomB, typeEntsA, typeEntsB, sizes, report.setting_displayDetails);
        log.timeStamp();
        log.write(" ");
        if (report.setting_chkdup) {
            log.write("Check for duplicates among entities to compare.");
            log.timeStamp();
            if (!assessMatches(gedcomA, gedcomA, typeEntsA, typeEntsA, confList, null, null, null, progress, true, sizes[0])) return false;
            log.write("Gedcom A:");
            displayMatches(confList, null, null, true);
            confList.clear();
            log.timeStamp();
            log.write(" ");
            if (!assessMatches(gedcomB, gedcomB, typeEntsB, typeEntsB, confList, null, null, null, progress, true, sizes[1])) return false;
            log.write("Gedcom B:");
            displayMatches(confList, null, null, true);
            confList.clear();
            log.timeStamp();
            log.write(" ");
        }
        log.write("*********************************************************");
        log.write("First assessment of overlap between the two gedcom files:");
        log.timeStamp();
        if (!assessMatches(gedcomA, gedcomB, typeEntsA, typeEntsB, confList, overlaps, scoreStats, idMap, progress, false, sizes[2])) return false;
        displayMatches(confList, overlaps, scoreStats, false);
        log.timeStamp();
        log.write(" ");
        if (report.setting_assessOnly) {
            gedcomOutput = gedcom;
            return true;
        }
        log.write("Interaction with user to confirm some of the matches");
        int keepMatching = 2;
        while (keepMatching == 2) {
            keepMatching = confirmMatchesWithUser(confList);
            if (keepMatching == -1) return false;
            log.write(" ");
            if (keepMatching > 0) {
                log.write("Re-assessing matches");
                log.timeStamp();
                if (!assessMatches(gedcomA, gedcomB, typeEntsA, typeEntsB, confList, overlaps, scoreStats, idMap, progress, false, sizes[2])) return false;
            }
        }
        log.timeStamp();
        log.write(" ");
        log.write("Check which information to keep for auto merged entities");
        log.timeStamp();
        mergeEntities(confList);
        log.timeStamp();
        log.write(" ");
        log.write("*********************************************************");
        log.write("Final assessment of overlap between the two gedcom files:");
        displayMatches(confList, overlaps, scoreStats, false);
        log.write(" ");
        int pos = fileNameA.indexOf(".ged");
        if (pos == -1) pos = fileNameA.length();
        String fileNameC = fileNameA.substring(0, pos) + report.setting_outputFileExt;
        log.write("Creating (C) file: " + fileNameC);
        log.timeStamp();
        Gedcom gedcomC = createGedcomFile(fileNameC, false, (report.setting_headerChosen == 0 ? gedcomA : gedcomB), report.setting_displayDetails);
        log.timeStamp();
        if (gedcomC == null) return false;
        log.write("   Producing result file (C)");
        log.timeStamp();
        mergeGedcom(gedcomA, gedcomB, gedcomC, overlaps, idMap, report.setting_displayDetails);
        log.timeStamp();
        log.timeStamp();
        progress.reset("Saving final Gedcom file...", "", 100);
        if (!saveGedcom(gedcomC)) return false;
        progress.terminate();
        log.timeStamp();
        log.write(" ");
        gedcomOutput = gedcomC;
        return true;
    }

    /**
  * Get Gedcom file from User
  */
    private Gedcom getGedcomFromUser(String msg) {
        Gedcom gedcomX = null;
        Origin originX = null;
        GedcomReader readerX;
        File fileX = report.getFileFromUser(msg, Action2.TXT_OK);
        if (fileX == null) return null;
        try {
            originX = Origin.create(new URL("file", "", fileX.getAbsolutePath()));
        } catch (MalformedURLException e) {
            log.write("URLexception:" + e);
            return null;
        }
        try {
            readerX = new GedcomReader(originX);
        } catch (IOException e) {
            log.write("IOexception:" + e);
            return null;
        }
        try {
            gedcomX = readerX.read();
        } catch (GedcomIOException e) {
            log.write("GedcomIOexception:" + e);
            log.write("At line:" + e.getLine());
            return null;
        }
        log.write("   LinesRead: " + readerX.getLines());
        List warnings = readerX.getWarnings();
        log.write("   Warnings: " + warnings.size());
        for (Iterator it = warnings.iterator(); it.hasNext(); ) {
            String wng = (String) it.next().toString();
            log.write("   " + wng);
        }
        linkGedcom(gedcomX);
        return gedcomX;
    }

    /**
  * Links Gedcom XReferences
  */
    private boolean linkGedcom(Gedcom gedcomX) {
        List ents = gedcomX.getEntities();
        for (Iterator it = ents.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            List ps = ent.getProperties(PropertyXRef.class);
            for (Iterator it2 = ps.iterator(); it2.hasNext(); ) {
                PropertyXRef xref = (PropertyXRef) it2.next();
                Property target = xref.getTarget();
                if (target == null) try {
                    xref.link();
                } catch (GedcomException e) {
                    log.write("Linking:GedcomException:" + e);
                    report.getOptionFromUser("Warning at Linking: " + e, OPTION_OKCANCEL);
                    return false;
                }
            }
        }
        return true;
    }

    /**
  * Copy gedcom to another gedcom assuming destination gedcom is empty
  */
    private boolean copyGedcom(Gedcom gedcomX, Gedcom gedcomY, boolean display) {
        List entitiesX = gedcomX.getEntities();
        progress.reset("Making temporary copy...", "entities", entitiesX.size());
        if (display) {
            log.write("   Number of entities found in A file: " + entitiesX.size());
        }
        int i = 0;
        try {
            for (Iterator it = entitiesX.iterator(); it.hasNext(); ) {
                Entity entX = (Entity) it.next();
                Entity entY = null;
                entY = gedcomY.createEntity(entX.getTag(), entX.getId());
                copyEntity(entX, entY);
                i++;
                if (((i / 100) * 100) == i) {
                    progress.increment(100);
                    if (!progress.isActive()) return false;
                }
            }
        } catch (GedcomException e) {
            log.write("GedcomException:" + e);
            return false;
        }
        progress.terminate();
        if (display) {
            log.write("   Number of entities temporarily copied: " + i);
        }
        return linkGedcom(gedcomY);
    }

    /**
  * Make unique Ids across 2 gedcom files 
  */
    private boolean makeUniqueIds(Gedcom gedcomX, Gedcom gedcomY, boolean display) {
        Map idParams = new TreeMap();
        List ids = null;
        int[] idp = null;
        String idStr = "";
        for (int i = 0; i < typeEnt.length; i++) {
            idp = new int[2];
            idParams.put(typeEnt[i], idp);
            List listEnt = new ArrayList(gedcomX.getEntities(typeEnt[i]));
            Collections.sort(listEnt, sortEntities);
            int s = listEnt.size();
            if (s > 0) {
                idStr = ((Entity) listEnt.get(s - 1)).getId();
                idp[1] = idStr.length();
                int start = 0, end = idStr.length() - 1;
                while (start <= end && !Character.isDigit(idStr.charAt(start))) start++;
                while (end >= start && !Character.isDigit(idStr.charAt(end))) end--;
                if (end < start) idp[0] = 0; else idp[0] = (int) Integer.parseInt(idStr.substring(start, end + 1));
            }
            if (display) {
                log.write("   Number of entities in B file of type " + typeEnt[i] + ": " + s + "; Max id for these entities : " + idp[0]);
            }
            listEnt = new ArrayList(gedcomY.getEntities(typeEnt[i]));
            Collections.sort(listEnt, sortEntities);
            s = listEnt.size();
            if (s > 0) {
                idStr = ((Entity) listEnt.get(s - 1)).getId();
                idp[1] = Math.max(idp[1], idStr.length());
                int start = 0, end = idStr.length() - 1;
                while (start <= end && !Character.isDigit(idStr.charAt(start))) start++;
                while (end >= start && !Character.isDigit(idStr.charAt(end))) end--;
                if (end < start) idp[0] = Math.max(idp[0], 0); else idp[0] = Math.max(idp[0], (int) Integer.parseInt(idStr.substring(start, end + 1)));
            }
            idp[0]++;
        }
        String idOld = "", idNew = "";
        Entity entY = null;
        try {
            for (int i = 0; i < typeEnt.length; i++) {
                String prefix = gedcomX.getEntityPrefix(typeEnt[i]);
                idp = (int[]) idParams.get(typeEnt[i]);
                StringBuffer buf = new StringBuffer(idp[1]);
                List listEnt = new ArrayList(gedcomY.getEntities(typeEnt[i]));
                Collections.sort(listEnt, sortEntities);
                for (Iterator it = listEnt.iterator(); it.hasNext(); ) {
                    entY = (Entity) it.next();
                    idOld = entY.getId();
                    buf.setLength(0);
                    buf.append(prefix);
                    buf.append(idp[0]);
                    while (buf.length() < idp[1]) {
                        buf.insert(1, '0');
                    }
                    idNew = buf.toString();
                    entY.setId(idNew);
                    idp[0]++;
                    idp[1] = Math.max(idp[1], idNew.length());
                }
            }
        } catch (GedcomException e) {
            log.write("GedcomException:" + e);
            return false;
        }
        return true;
    }

    /**
  * Prepare sets of entities to analyse
  */
    private boolean prepareSets(Gedcom gedcomX, Gedcom gedcomY, Map typeEntsX, Map typeEntsY, double[] sizes, boolean display) {
        prepareEntities(gedcomX, typeEntsX, report.setting_displayDetails);
        prepareEntities(gedcomY, typeEntsY, report.setting_displayDetails);
        trimSpans((List) typeEntsX.get(typeEnt[0]), (List) typeEntsY.get(typeEnt[0]));
        double a, b;
        for (int i = 0; i < typeEnt.length; i++) {
            if (i == 1) continue;
            a = (double) ((List) typeEntsX.get(typeEnt[i])).size();
            b = (double) ((List) typeEntsY.get(typeEnt[i])).size();
            sizes[0] += a * a;
            sizes[1] += b * b;
            sizes[2] += a * b;
        }
        return true;
    }

    /**
  * Prepare entities from Gedcom (split in types, extract subset if required)
  */
    private boolean prepareEntities(Gedcom gedcomX, Map typeEntX, boolean display) {
        int CHOICE_ANC = 0, CHOICE_DEC = 1;
        int choice = CHOICE_ANC;
        Indi indiSel = null;
        int n = -1;
        for (int i = 0; i < typeEnt.length; i++) {
            List listEnt = new ArrayList(gedcomX.getEntities(typeEnt[i]));
            if (i == 0) {
                if (report.setting_analysis1 == ANA1_SUBSET) {
                    indiSel = (Indi) report.getEntityFromUser("Please select individual from which to select subset of entities", gedcomX, Gedcom.INDI);
                    if (indiSel != null) {
                        String choices[] = { report.translate("Limit analysis to ancestors of " + indiSel.toString()), report.translate("Limit analysis to descendant of " + indiSel.toString()) };
                        String sel = (String) report.getValueFromUser("Choose subset for analysis (analsysis will be limited to these entities)", choices, choices[0]);
                        if (sel != null) {
                            if (display) log.write("   " + sel);
                            n = ((sel == choices[0]) ? 0 : 1);
                            for (Iterator it = listEnt.iterator(); it.hasNext(); ) {
                                Indi indi = (Indi) it.next();
                                if (indi == indiSel) continue;
                                if ((n == CHOICE_ANC) && (!indi.isAncestorOf(indiSel))) it.remove();
                                if ((n == CHOICE_DEC) && (!indi.isDescendantOf(indiSel))) it.remove();
                            }
                        }
                    }
                }
                List listPersons = new ArrayList();
                Map indi2Person = new TreeMap();
                for (Iterator it = listEnt.iterator(); it.hasNext(); ) {
                    Indi indi = (Indi) it.next();
                    PersonFactory pf = new PersonFactory(indi);
                    Person p = pf.create();
                    if (report.setting_inclNoBirth || p.birthInfo) {
                        indi2Person.put(indi, p);
                        listPersons.add(p);
                    }
                }
                for (Iterator it = listPersons.iterator(); it.hasNext(); ) {
                    Person p = (Person) it.next();
                    PersonFactory.getRelatives(p, indi2Person);
                }
                Collections.sort(listPersons, new PersonFactory());
                typeEntX.put(typeEnt[i], listPersons);
            }
            if (i == 1) {
                typeEntX.put(typeEnt[i], new ArrayList());
            }
            if (i > 1) {
                List listInfos = new ArrayList();
                for (Iterator it = listEnt.iterator(); it.hasNext(); ) {
                    Entity ent = (Entity) it.next();
                    InfoFactory inF = new InfoFactory(ent);
                    Info info = inF.create();
                    if (info.titleLength != 0) {
                        listInfos.add(info);
                    }
                }
                Collections.sort(listInfos, new InfoFactory());
                typeEntX.put(typeEnt[i], listInfos);
            }
        }
        return true;
    }

    /**
  * Trim edges of persons
  */
    private boolean trimSpans(List personX, List personY) {
        Person minMax = new Person();
        minMax.yearMin = -Integer.MAX_VALUE;
        minMax.yearMax = Integer.MAX_VALUE;
        Person spanMinX = new Person();
        spanMinX.yearMin = Integer.MAX_VALUE;
        spanMinX.yearMax = Integer.MAX_VALUE;
        Person spanMaxX = new Person();
        spanMaxX.yearMin = -Integer.MAX_VALUE;
        spanMaxX.yearMax = -Integer.MAX_VALUE;
        for (Iterator it = personX.iterator(); it.hasNext(); ) {
            Person p = (Person) it.next();
            if (PersonFactory.compareSpans(p, spanMaxX) > 0) PersonFactory.copy(p, spanMaxX);
            if (PersonFactory.compareSpans(p, spanMinX) < 0) PersonFactory.copy(p, spanMinX);
        }
        Person spanMinY = new Person();
        spanMinY.yearMin = Integer.MAX_VALUE;
        spanMinY.yearMax = Integer.MAX_VALUE;
        Person spanMaxY = new Person();
        spanMaxY.yearMin = -Integer.MAX_VALUE;
        spanMaxY.yearMax = -Integer.MAX_VALUE;
        for (Iterator it = personY.iterator(); it.hasNext(); ) {
            Person p = (Person) it.next();
            if (PersonFactory.compareSpans(p, spanMaxY) > 0) PersonFactory.copy(p, spanMaxY);
            if (PersonFactory.compareSpans(p, spanMinY) < 0) PersonFactory.copy(p, spanMinY);
        }
        for (Iterator it = personX.iterator(); it.hasNext(); ) {
            Person p = (Person) it.next();
            if ((PersonFactory.compareSpans(p, spanMinY) < 0) && (PersonFactory.areNotOverlapping(p, spanMinY))) {
                it.remove();
            }
            if ((PersonFactory.compareSpans(p, spanMaxY) > 0) && (PersonFactory.areNotOverlapping(p, spanMaxY))) {
                it.remove();
            }
        }
        for (Iterator it = personY.iterator(); it.hasNext(); ) {
            Person p = (Person) it.next();
            if ((PersonFactory.compareSpans(p, spanMinX) < 0) && (PersonFactory.areNotOverlapping(p, spanMinX))) {
                it.remove();
            }
            if ((PersonFactory.compareSpans(p, spanMaxX) > 0) && (PersonFactory.areNotOverlapping(p, spanMaxX))) {
                it.remove();
            }
        }
        return true;
    }

    /**
  * Assess matches across 2 Gedcom files (they can represent the same file)
  */
    private boolean assessMatches(Gedcom gedcomX, Gedcom gedcomY, Map typeEntsX, Map typeEntsY, Map confList, Map overlaps, Map scoreStats, Map idMap, ProgressStatus progress, boolean duplicates, double size) {
        if (!report.setting_appendFiles) {
            List listX = new ArrayList();
            List listY = new ArrayList();
            String msg = "Assessing matches...";
            if (duplicates) {
                msg = "Looking for duplicates...";
                if (report.setting_displayDetails) log.write(msg);
            }
            progress.reset("Merge - " + msg, "combinations checked", size);
            for (int i = 0; i < typeEnt.length; i++) {
                if (i == 1) continue;
                progress.setTitle("Merge " + typeEnt[i] + " - " + msg);
                listX = (List) typeEntsX.get(typeEnt[i]);
                listY = (List) typeEntsY.get(typeEnt[i]);
                if ((listX == null) || (listX.isEmpty())) continue;
                if (i == 0) {
                    if (duplicates) {
                        if (!runAlgoDupIndi(listX, listY, confList, progress)) return false;
                    } else {
                        if (!runAlgoMatchIndi(listX, listY, confList, progress)) return false;
                    }
                } else {
                    if (duplicates) {
                        if (!runAlgoDupInfo(listX, listY, confList, progress)) return false;
                    } else {
                        if (!runAlgoMatchInfo(listX, listY, confList, progress)) return false;
                    }
                }
            }
            progress.terminate();
            if (duplicates) return true;
            List set1 = new ArrayList();
            List set2 = new ArrayList();
            ConfidenceMatch match = null;
            List valList = new ArrayList(confList.values());
            Collections.sort(valList, new ConfidenceMatch());
            for (Iterator it = valList.iterator(); it.hasNext(); ) {
                match = (ConfidenceMatch) it.next();
                if ((set1.contains(match.ent1)) || (set2.contains(match.ent2))) {
                    match.confirmed = true;
                    match.toBeMerged = false;
                    match.choice = 0;
                } else {
                    set1.add(match.ent1);
                    set2.add(match.ent2);
                }
            }
            assessFamilies(gedcomX, gedcomY, confList);
        }
        List Xexclusive = new ArrayList();
        List Xconnected = new ArrayList();
        List Xmatching = new ArrayList();
        List Yexclusive = new ArrayList();
        List Yconnected = new ArrayList();
        List Ymatching = new ArrayList();
        List ZfromX = new ArrayList();
        List ZfromY = new ArrayList();
        HashSet Xsubtrees = getTrees(gedcomX);
        HashSet Ysubtrees = getTrees(gedcomY);
        HashSet XtempTrees = new HashSet(Xsubtrees);
        HashSet YtempTrees = new HashSet(Ysubtrees);
        ConfidenceMatch match = null;
        String fileNameB = gedcomY.getOrigin().getFile().getName();
        String key = "";
        for (Iterator it = confList.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            match = (ConfidenceMatch) confList.get(key);
            if (match.toBeMerged) {
                Xmatching.add(match.ent1);
                Ymatching.add(match.ent2);
                Iterator itmp = XtempTrees.iterator();
                while (itmp.hasNext()) {
                    HashSet listEnts = (HashSet) itmp.next();
                    if (listEnts.contains(match.ent1)) {
                        Xconnected.addAll(listEnts);
                        XtempTrees.remove(listEnts);
                        break;
                    }
                }
                itmp = YtempTrees.iterator();
                while (itmp.hasNext()) {
                    HashSet listEnts = (HashSet) itmp.next();
                    if (listEnts.contains(match.ent2)) {
                        Yconnected.addAll(listEnts);
                        YtempTrees.remove(listEnts);
                        break;
                    }
                }
                if ((match.choice == 1) || (match.choice == 3)) {
                    flagEntity(match.ent1, "UPD", fileNameB);
                    ZfromX.add(match.ent1);
                    idMap.put(match.ent2.getId(), match.ent1.getId());
                } else {
                    flagEntity(match.ent2, "UPD", fileNameB);
                    ZfromY.add(match.ent2);
                    idMap.put(match.ent1.getId(), match.ent2.getId());
                }
            }
        }
        Xconnected.removeAll(Xmatching);
        Yconnected.removeAll(Ymatching);
        for (Iterator it = Xmatching.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            if (!ZfromX.contains(ent)) it.remove();
        }
        for (Iterator it = Ymatching.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            if (!ZfromY.contains(ent)) it.remove();
        }
        while (!XtempTrees.isEmpty()) {
            HashSet listEnts = (HashSet) XtempTrees.iterator().next();
            Xexclusive.addAll(listEnts);
            XtempTrees.remove(listEnts);
        }
        while (!YtempTrees.isEmpty()) {
            HashSet listEnts = (HashSet) YtempTrees.iterator().next();
            Yexclusive.addAll(listEnts);
            YtempTrees.remove(listEnts);
        }
        if (report.setting_keepAecl) ZfromX.addAll(Xexclusive);
        if (report.setting_keepAcon) ZfromX.addAll(Xconnected);
        if (report.setting_keepBcon) ZfromY.addAll(Yconnected);
        if (report.setting_keepBecl) ZfromY.addAll(Yexclusive);
        overlaps.put("A1ecl", Xexclusive);
        overlaps.put("A2con", Xconnected);
        overlaps.put("A3mat", Xmatching);
        overlaps.put("B3ecl", Yexclusive);
        overlaps.put("B2con", Yconnected);
        overlaps.put("B1mat", Ymatching);
        overlaps.put("ZfA", ZfromX);
        overlaps.put("ZfB", ZfromY);
        int max = 0, sum = 0, count = 0, manual = 0, automatic = 0, density = 0, average = 0;
        for (Iterator it = confList.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            match = (ConfidenceMatch) confList.get(key);
            max = Math.max(match.confLevel, max);
            sum += match.confLevel;
            count += 1;
            automatic += (match.toBeMerged && (match.confLevel > report.setting_autoMergingLevel)) ? 1 : 0;
            manual += (match.toBeMerged && (match.confLevel <= report.setting_autoMergingLevel)) ? 1 : 0;
        }
        if (count == 0) count = 1;
        average = sum / count;
        for (Iterator it = confList.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            match = (ConfidenceMatch) confList.get(key);
            if ((match.confLevel > max - 5) && (match.confLevel <= max)) density += 1;
        }
        scoreStats.put(report.translate("stats_1.Average"), String.valueOf(average));
        scoreStats.put(report.translate("stats_2.Max"), String.valueOf(max));
        scoreStats.put(report.translate("stats_3.Density"), String.valueOf(density));
        scoreStats.put(report.translate("stats_4.Auto"), String.valueOf(automatic));
        scoreStats.put(report.translate("stats_5.Manual"), String.valueOf(manual));
        return true;
    }

    /**
  * Algorithms
  */
    private boolean runAlgoDupInfo(List listX, List listY, Map confList, ProgressStatus progress) {
        String key = "";
        double inc = (double) listY.size();
        ConfidenceMatch match = null;
        for (Iterator it1 = listX.iterator(); it1.hasNext(); ) {
            Info p1 = (Info) it1.next();
            for (Iterator it2 = listY.iterator(); it2.hasNext(); ) {
                Info p2 = (Info) it2.next();
                if (p1.id.compareTo(p2.id) >= 0) continue;
                key = p1.id + "x" + p2.id;
                match = (ConfidenceMatch) confList.get(key);
                if ((match == null) || ((!match.confirmed) && (!match.toBeMerged))) {
                    match = assessConfidenceInfo(p1, p2, false, confList);
                    if (match.confLevel >= report.setting_askThreshold) confList.put(key, match);
                }
            }
            progress.increment(inc);
            if (!progress.isActive()) return false;
        }
        return true;
    }

    private boolean runAlgoMatchInfo(List listX, List listY, Map confList, ProgressStatus progress) {
        String key = "";
        double inc = (double) listY.size();
        ConfidenceMatch match = null;
        for (Iterator it1 = listX.iterator(); it1.hasNext(); ) {
            Info p1 = (Info) it1.next();
            for (Iterator it2 = listY.iterator(); it2.hasNext(); ) {
                Info p2 = (Info) it2.next();
                key = p1.id + "x" + p2.id;
                match = (ConfidenceMatch) confList.get(key);
                if ((match == null) || ((!match.confirmed) && (!match.toBeMerged))) {
                    match = assessConfidenceInfo(p1, p2, false, confList);
                    if (match.confLevel >= report.setting_askThreshold) confList.put(key, match);
                }
            }
            progress.increment(inc);
            if (!progress.isActive()) return false;
        }
        return true;
    }

    private boolean runAlgoDupIndi(List listX, List listY, Map confList, ProgressStatus progress) {
        String key = "";
        double inc = (double) listY.size();
        ConfidenceMatch match = null;
        for (Iterator it1 = listX.iterator(); it1.hasNext(); ) {
            Person p1 = (Person) it1.next();
            for (Iterator it2 = listY.iterator(); it2.hasNext(); ) {
                Person p2 = (Person) it2.next();
                if (p1.id.compareTo(p2.id) >= 0) continue;
                if (p2.yearMin > p1.yearMax) break;
                if (p2.yearMax < p1.yearMin) continue;
                key = p1.id + "x" + p2.id;
                match = (ConfidenceMatch) confList.get(key);
                if ((match == null) || ((!match.confirmed) && (!match.toBeMerged))) {
                    match = assessConfidenceIndi(p1, p2, false, confList);
                    if (match.confLevel >= report.setting_askThreshold) confList.put(key, match);
                }
            }
            progress.increment(inc);
            if (!progress.isActive()) return false;
        }
        return true;
    }

    private boolean runAlgoMatchIndi(List listX, List listY, Map confList, ProgressStatus progress) {
        String key = "";
        double inc = (double) listY.size();
        ConfidenceMatch match = null;
        for (Iterator it1 = listX.iterator(); it1.hasNext(); ) {
            Person p1 = (Person) it1.next();
            for (Iterator it2 = listY.iterator(); it2.hasNext(); ) {
                Person p2 = (Person) it2.next();
                if (p2.yearMin > p1.yearMax) break;
                if (p2.yearMax < p1.yearMin) continue;
                key = p1.id + "x" + p2.id;
                match = (ConfidenceMatch) confList.get(key);
                if ((match == null) || ((!match.confirmed) && (!match.toBeMerged))) {
                    match = assessConfidenceIndi(p1, p2, false, confList);
                    if (match.confLevel >= report.setting_askThreshold) confList.put(key, match);
                }
            }
            progress.increment(inc);
            if (!progress.isActive()) return false;
        }
        return true;
    }

    /**
  * Get Trees from a gedcom
  */
    private HashSet getTrees(Gedcom gedcomX) {
        HashSet subtrees = new HashSet();
        HashSet entities = new HashSet(gedcomX.getEntities());
        while (!entities.isEmpty()) {
            Entity ent = (Entity) entities.iterator().next();
            entities.remove(ent);
            HashSet subtree = new HashSet();
            Stack todos = new Stack();
            HashSet alreadyStacked = new HashSet();
            todos.add(ent);
            alreadyStacked.add(ent);
            while (!todos.isEmpty()) {
                Entity todo = (Entity) todos.pop();
                subtree.add(todo);
                List ps = todo.getProperties(PropertyXRef.class);
                for (Iterator it = ps.iterator(); it.hasNext(); ) {
                    PropertyXRef xref = (PropertyXRef) it.next();
                    Entity target = (Entity) xref.getTargetEntity();
                    if ((target != null) && !alreadyStacked.contains(target)) {
                        entities.remove(target);
                        todos.push(target);
                        alreadyStacked.add(target);
                    }
                }
            }
            subtrees.add(subtree);
        }
        return subtrees;
    }

    /**
  * Display matches
  */
    private boolean displayMatches(Map matches, Map overlaps, Map scoreStats, boolean duplicates) {
        if (!duplicates) {
            log.write("Overall results:");
            for (Iterator it = scoreStats.keySet().iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                String value = (String) scoreStats.get(key);
                log.write("   " + key + " : \t" + value);
            }
            int[] entitiesVol = new int[6];
            int[] totalVol = new int[6];
            int total = 0;
            log.write(" ");
            log.write("Entity Sets:\tTotal\tIndi\tFam\tNote\tSour\tRepo\tSubm");
            for (Iterator it = overlaps.keySet().iterator(); it.hasNext(); ) {
                String key = (String) it.next();
                List lEnts = (List) overlaps.get(key);
                calcEntities(lEnts, entitiesVol);
                total += lEnts.size();
                String volumes = "";
                for (int i = 0; i < entitiesVol.length; i++) {
                    volumes += entitiesVol[i] + "\t";
                    totalVol[i] += entitiesVol[i];
                }
                log.write("   " + report.translate(key) + " :\t" + lEnts.size() + "\t" + volumes);
                if ((key == "A3mat") || (key == "B3ecl") || (key == "ZfB")) {
                    volumes = "";
                    for (int i = 0; i < totalVol.length; i++) {
                        volumes += totalVol[i] + "\t";
                    }
                    log.write("          \t----------------------------------------------------");
                    log.write("   Total :\t" + total + "\t" + volumes);
                    log.write(" ");
                    for (int i = 0; i < totalVol.length; i++) totalVol[i] = 0;
                    total = 0;
                }
            }
        }
        if (!report.setting_displayDetails) return true;
        log.write("Detailed matches:");
        boolean noMatch = true;
        String msg = "";
        List confList = new ArrayList(matches.values());
        Collections.sort(confList, new ConfidenceMatch());
        for (Iterator it = confList.iterator(); it.hasNext(); ) {
            ConfidenceMatch match = (ConfidenceMatch) it.next();
            if (!duplicates) {
                if (match.choice == 0) msg = "Merge posible but not confirmed"; else msg = "To be merged and confirmed by user or options.";
            } else msg = "";
            log.write("   Confidence of match between " + match.ent1.getId() + " and " + match.ent2.getId() + " is : " + match.confLevel + "%. " + msg);
        }
        if (confList.isEmpty()) log.write("   No match found.");
        return true;
    }

    /**
  * Calculate number of entity by type in a list
  */
    private boolean calcEntities(List entities, int[] volume) {
        for (int i = 0; i < volume.length; i++) volume[i] = 0;
        for (Iterator it = entities.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            for (int i = 0; i < volume.length; i++) {
                if (ent.getTag() == typeEnt[i]) {
                    volume[i]++;
                    break;
                }
            }
        }
        return true;
    }

    /**
  * USER INTERFACE - Confirm matches with User
  * Return value:
  *  -1 => user cancels
  *   0 => return OK and nothing to do (assess 0 times after that)
  *   1 => return OK and finished with assessing (assess 1 time after that)
  *   2 => return OK and not finished with assessing (keep assessing)
  */
    private int confirmMatchesWithUser(Map matches) {
        int CHOICE_NO = 0, CHOICE_1 = 1, CHOICE_2 = 2, CHOICE_3 = 3, CHOICE_SKIP = 4, CHOICE_REDO = 5;
        int choice = CHOICE_NO;
        String choices[] = { report.translate("user_no"), report.translate("user_yes_first"), report.translate("user_yes_second"), report.translate("user_yes_both"), report.translate("user_skip"), report.translate("user_reassess") };
        String msg = report.translate("user_question_merge");
        String confText = "";
        EntityView entView = new EntityView(report.translate("user_title_merge"), msg, false);
        List confList = new ArrayList(matches.values());
        Collections.sort(confList, new ConfidenceMatch());
        boolean nothingToDo = true;
        for (Iterator it = confList.iterator(); it.hasNext(); ) {
            ConfidenceMatch match = (ConfidenceMatch) it.next();
            if ((!match.confirmed) && (!match.toBeMerged)) {
                confText = "   Confidence between " + match.ent1.getId() + " and " + match.ent2.getId() + " is : " + match.confLevel;
                entView.setQuestion(msg + match.confLevel + "%)");
                choice = entView.getEntityFromUser(match.ent1, match.ent2, choices, choices[0]);
                if (choice == -1) {
                    return -1;
                }
                if (choice == CHOICE_REDO) {
                    return 2;
                }
                if (choice == CHOICE_SKIP) {
                    log.write(confText + " - skipped.");
                    continue;
                }
                if (choice == CHOICE_NO) {
                    match.toBeMerged = false;
                    log.write(confText + " - should NOT be merged.");
                }
                if (choice == CHOICE_3) {
                    match.toBeMerged = mergeEntity(match, true);
                    if (!match.toBeMerged) {
                        log.write(confText + " - assessed and should NOT be merged.");
                        continue;
                    } else {
                        log.write(confText + " - assessed and should be merged.");
                    }
                }
                if ((choice == CHOICE_1) || (choice == CHOICE_2)) {
                    match.toBeMerged = true;
                    match.choice = choice;
                    log.write(confText + " - should be merged.");
                }
                match.confirmed = true;
                matches.put(match.ent1.getId() + "x" + match.ent2.getId(), match);
                nothingToDo = false;
            }
        }
        if (nothingToDo) {
            log.write("   No confirmation required");
            return 0;
        }
        if (report.getOptionFromUser(report.translate("user_ask_completed"), OPTION_YESNO)) return 1;
        return -1;
    }

    /**
  * USER INTERFACE - Confirm property to keep with User
  */
    private boolean confirmPropertyWithUser(Property propA, Property propB) {
        int CHOICE_1 = 0, CHOICE_2 = 1;
        int choice = CHOICE_1;
        String choices[] = { report.translate("user_yes_firstProp"), report.translate("user_yes_secondProp") };
        String msg = report.translate("user_question_prop");
        EntityView entView = new EntityView(report.translate("user_title_prop"), msg, true);
        choice = entView.getEntityFromUser(propA, propB, choices, choices[0]);
        if (choice == -1) return false;
        if (choice == CHOICE_1) {
            return true;
        }
        if (choice == CHOICE_2) {
            propA.setValue(propB.getValue());
        }
        return true;
    }

    /**
  * Merge entities to be merge (information to keep, for automatically merged ones)
  */
    private boolean assessFamilies(Gedcom gedcomX, Gedcom gedcomY, Map matches) {
        HashSet entsX = new HashSet();
        HashSet entsY = new HashSet();
        Map xToY = new TreeMap();
        List confList = new ArrayList(matches.values());
        for (Iterator it = confList.iterator(); it.hasNext(); ) {
            ConfidenceMatch match = (ConfidenceMatch) it.next();
            if ((match.toBeMerged) && (match.ent1 instanceof Indi)) {
                entsX.add(match.ent1);
                entsY.add(match.ent2);
                xToY.put(match.ent1, match.ent2);
            }
        }
        List listFamsX = new ArrayList(gedcomX.getEntities(typeEnt[1]));
        List listFamsY = new ArrayList(gedcomY.getEntities(typeEnt[1]));
        for (Iterator itx = listFamsX.iterator(); itx.hasNext(); ) {
            Fam famX = (Fam) itx.next();
            Entity husbandX = (Entity) famX.getHusband();
            Entity wifeX = (Entity) famX.getWife();
            Entity matchHusbandX = null;
            Entity matchWifeX = null;
            if (!entsX.contains(husbandX)) continue; else matchHusbandX = (Entity) xToY.get(husbandX);
            if (!entsX.contains(wifeX)) continue; else matchWifeX = (Entity) xToY.get(wifeX);
            for (Iterator ity = listFamsY.iterator(); ity.hasNext(); ) {
                Fam famY = (Fam) ity.next();
                Entity husbandY = (Entity) famY.getHusband();
                Entity wifeY = (Entity) famY.getWife();
                if (((husbandY == matchHusbandX) && (wifeY == matchWifeX)) || ((husbandY == matchWifeX) && (wifeY == matchHusbandX))) {
                    ConfidenceMatch match = new ConfidenceMatch((Entity) famX, (Entity) famY);
                    match.confLevel = 100;
                    match.confirmed = true;
                    match.toBeMerged = true;
                    match.choice = 3;
                    matches.put(famX.getId() + "x" + famY.getId(), match);
                    break;
                }
            }
        }
        return true;
    }

    /**
  * Merge entities to be merge (information to keep, for automatically merged ones)
  */
    private boolean mergeEntities(Map matches) {
        List confList = new ArrayList(matches.values());
        Collections.sort(confList, new ConfidenceMatch());
        for (Iterator it = confList.iterator(); it.hasNext(); ) {
            ConfidenceMatch match = (ConfidenceMatch) it.next();
            if (match.toBeMerged && (match.choice == 3)) {
                mergeEntity(match, false);
            }
        }
        return true;
    }

    /**
  * Merge entities' properties
  */
    private boolean mergeEntity(ConfidenceMatch match, boolean askUser) {
        if (!askUser && (report.setting_ruleEntity == ALWAYS_A)) {
            match.confirmed = true;
            match.toBeMerged = true;
            match.choice = 1;
            return true;
        }
        if (!askUser && (report.setting_ruleEntity == ALWAYS_B)) {
            match.confirmed = true;
            match.toBeMerged = true;
            match.choice = 2;
            return true;
        }
        List listPropA = new LinkedList();
        List listPropB = new LinkedList();
        List listTemp = new LinkedList();
        Property[] properties = null;
        properties = match.ent1.getProperties();
        listPropA.addAll(Arrays.asList(properties));
        listTemp.addAll(Arrays.asList(properties));
        while (listTemp.size() > 0) {
            Property propItem = (Property) ((LinkedList) listTemp).removeFirst();
            Property[] subProps = propItem.getProperties();
            listPropA.addAll(Arrays.asList(subProps));
            listTemp.addAll(Arrays.asList(subProps));
        }
        properties = match.ent2.getProperties();
        listPropB.addAll(Arrays.asList(properties));
        listTemp.addAll(Arrays.asList(properties));
        while (listTemp.size() > 0) {
            Property propItem = (Property) ((LinkedList) listTemp).removeFirst();
            Property[] subProps = propItem.getProperties();
            listPropB.addAll(Arrays.asList(subProps));
            listTemp.addAll(Arrays.asList(subProps));
        }
        for (Iterator ita = listPropA.iterator(); ita.hasNext(); ) {
            Property propItemA = (Property) ita.next();
            TagPath tagPathA = propItemA.getPath();
            String valueA = propItemA.getValue().trim();
            for (Iterator itb = listPropB.iterator(); itb.hasNext(); ) {
                Property propItemB = (Property) itb.next();
                TagPath tagPathB = propItemB.getPath();
                String valueB = propItemB.getValue().trim();
                if ((tagPathA.toString().compareTo(tagPathB.toString()) == 0) && ((valueA.compareTo(valueB) == 0))) {
                    ita.remove();
                    itb.remove();
                    break;
                }
            }
        }
        for (Iterator ita = listPropA.iterator(); ita.hasNext(); ) {
            Property propItemA = (Property) ita.next();
            TagPath tagPathA = propItemA.getPath();
            Property[] subPropsB = match.ent2.getProperties(tagPathA);
            if (subPropsB.length == 0) continue;
            for (int i = 0; i < subPropsB.length; i++) {
                if (!listPropB.contains(subPropsB[i])) continue;
                if (subPropsB[i].getValue().trim().length() == 0) {
                    ita.remove();
                    listPropB.remove(subPropsB[i]);
                    break;
                }
                if (propItemA.getValue().trim().length() == 0) {
                    ita.remove();
                    listPropB.remove(subPropsB[i]);
                    propItemA.setValue(subPropsB[i].getValue());
                    break;
                }
                if (propItemA.getValue().trim().startsWith("@")) {
                    ita.remove();
                    listPropB.remove(subPropsB[i]);
                    break;
                }
                if (!askUser && (report.setting_ruleEntity == A_CONFLICT)) {
                    ita.remove();
                    listPropB.remove(subPropsB[i]);
                    break;
                }
                if (!askUser && (report.setting_ruleEntity == B_CONFLICT)) {
                    ita.remove();
                    listPropB.remove(subPropsB[i]);
                    propItemA.setValue(subPropsB[i].getValue());
                    break;
                }
                if (askUser || (report.setting_ruleEntity == ASK_CONFLICT)) {
                    if (confirmPropertyWithUser(propItemA, subPropsB[i])) {
                        ita.remove();
                        listPropB.remove(subPropsB[i]);
                        break;
                    } else {
                        return false;
                    }
                }
            }
        }
        List toKeep = new LinkedList();
        for (Iterator itb = listPropB.iterator(); itb.hasNext(); ) {
            Property propItemB = (Property) itb.next();
            TagPath tagPathB = propItemB.getPath();
            Property[] subPropsA = match.ent1.getProperties(tagPathB);
            if (subPropsA.length == 0) {
                toKeep.add(propItemB);
                itb.remove();
                continue;
            }
            for (int i = 0; i < subPropsA.length; i++) {
                if (!listPropA.contains(subPropsA[i])) {
                    toKeep.add(propItemB);
                    itb.remove();
                    listPropA.remove(subPropsA[i]);
                    break;
                }
            }
        }
        for (Iterator itb = toKeep.iterator(); itb.hasNext(); ) {
            Property propItemB = (Property) itb.next();
            addPropertyByPath(match.ent1, propItemB.getPath(), propItemB.getValue());
        }
        match.choice = 1;
        return true;
    }

    /**
   * Add a property at given path
   */
    private Property addPropertyByPath(Entity ent, final TagPath path, final String value) {
        final Property[] result = new Property[1];
        final int[] level = { 1 };
        PropertyVisitor visitor = new PropertyVisitor() {

            protected boolean leaf(Property prop) {
                level[0] = 1;
                return false;
            }

            protected boolean recursion(Property parent, String child) {
                level[0]++;
                if ((parent.getProperty(child, false) == null) && (level[0] < path.length())) {
                    parent.addProperty(child, "");
                }
                if (level[0] == path.length()) {
                    result[0] = parent.addProperty(child, value);
                }
                return true;
            }
        };
        path.iterate((Property) ent, visitor);
        return result[0];
    }

    /**
  * Merge gedcom and macthes entities in particular
  */
    private boolean flagEntity(Entity ent, String flag, String fileName) {
        if (!report.setting_flagChanges) return false;
        Property prop = (Property) ent;
        Date rightNow = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm-E");
        String date = formatter.format(rightNow);
        prop = prop.addProperty("_MRG-" + flag, "");
        prop.addProperty("DATE", date);
        prop.addProperty("FILE", fileName);
        return true;
    }

    /**
  * Copy entity from one gedcom to another one
  */
    private boolean copyEntity(Entity entA, Entity entZ) {
        List listProp = new LinkedList();
        Property[] propertiesA = entA.getProperties();
        listProp.addAll(Arrays.asList(propertiesA));
        Property propItemA = null;
        Property propItemZ = (Property) entZ;
        Property lastpropItemZ = null;
        int len = 2;
        while (listProp.size() > 0) {
            propItemA = (Property) ((LinkedList) listProp).removeFirst();
            Property[] subProps = propItemA.getProperties();
            listProp.addAll(0, Arrays.asList(subProps));
            TagPath tagPathA = propItemA.getPath();
            if (tagPathA.length() > len) {
                propItemZ = lastpropItemZ;
                len = tagPathA.length();
            }
            while (tagPathA.length() < len) {
                propItemZ = propItemZ.getParent();
                len--;
            }
            String tag = propItemA.getTag();
            if (tag == "XREF") continue;
            if (tagPathA.toString().compareTo("NOTE:NOTE") == 0) {
                entZ.setValue(propItemA.getValue());
            } else {
                lastpropItemZ = propItemZ.addProperty(tag, propItemA.getValue());
            }
        }
        return true;
    }

    /**
  * USER INTERFACE - Get user to map place Format in case they are different
  */
    private int[] mapPlaceFormat(Gedcom gedcomX, Gedcom gedcomY, int headerChosen) {
        String pf1 = "";
        String pf2 = "";
        if (headerChosen == 1) {
            pf1 = gedcomX.getPlaceFormat();
            pf2 = gedcomY.getPlaceFormat();
        } else {
            pf2 = gedcomX.getPlaceFormat();
            pf1 = gedcomY.getPlaceFormat();
        }
        if (pf1.compareTo(pf2) == 0) return null;
        if ((pf1.length() == 0) || (pf2.length() == 0)) return null;
        String[] tags1 = pf1.split("\\,");
        int[] placeMap = new int[tags1.length];
        ArrayList tags2 = new ArrayList((Collection) Arrays.asList(pf2.split("\\,")));
        ArrayList tagsTemp = new ArrayList((Collection) Arrays.asList(pf2.split("\\,")));
        for (int i = 0; i < tags1.length; i++) {
            String tag = (String) tags1[i];
            String msg = report.translate("user_place_map_tag", tag);
            String selection = (String) report.getValueFromUser(msg, (Object[]) tags2.toArray(), tags2.get(0));
            int iSel = 0;
            if (selection == null) selection = (String) tags2.get(0);
            iSel = tags2.indexOf(selection);
            placeMap[i] = tagsTemp.indexOf(selection);
            log.write("   " + tags1[i] + " -> " + tags2.get(iSel));
            if (tags2.size() > 1) tags2.remove(iSel);
        }
        return placeMap;
    }

    /**
  * Remap a list of jurisdictions 
  */
    private boolean remapPlaces(List entities, int[] placeMap) {
        if (placeMap == null) return true;
        for (Iterator it = entities.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            List places = ent.getProperties(PropertyPlace.class);
            for (Iterator itp = places.iterator(); itp.hasNext(); ) {
                Property propPlace = (Property) itp.next();
                String place = propPlace.toString();
                String[] placeTab = place.split("\\,", -1);
                String newPlace = "";
                for (int i = 0; i < placeMap.length; i++) {
                    if (placeMap[i] < placeTab.length) newPlace += placeTab[placeMap[i]] + ","; else {
                        log.write("   Warning: PLAC tag with too few fields in '" + place + "' : Expecting " + placeMap.length + " and got " + placeTab.length + ".");
                        newPlace = place + ",,,,,,,,,,,,,,,,".substring(0, placeMap.length - placeTab.length + 1);
                        break;
                    }
                }
                newPlace = newPlace.substring(0, newPlace.length() - 1);
                propPlace.setValue(newPlace);
            }
        }
        return true;
    }

    /**
  * Create gedcom file 
  */
    private Gedcom createGedcomFile(String fileNameX, boolean createSubmitter, Gedcom gedcomA, boolean display) {
        Origin originX = null;
        File fileX = new File(fileNameX);
        if (fileX.exists()) {
            if (display) log.write("   File " + fileNameX + " already exists.");
            fileX.delete();
            if (display) log.write("   Deleting file " + fileNameX + "...");
        }
        try {
            originX = Origin.create(new URL("file", "", fileX.getAbsolutePath()));
        } catch (MalformedURLException e) {
            log.write("URLexception:" + e);
            return null;
        }
        Gedcom gedcomX = new Gedcom(originX);
        gedcomX.setEncoding(gedcomA.getEncoding());
        gedcomX.setLanguage(gedcomA.getLanguage());
        gedcomX.setPassword(gedcomA.getPassword());
        gedcomX.setPlaceFormat(gedcomA.getPlaceFormat());
        if (createSubmitter) {
            try {
                Submitter sub = (Submitter) gedcomX.createEntity(Gedcom.SUBM, gedcomX.getNextAvailableID(Gedcom.SUBM));
                sub.addDefaultProperties();
                gedcomX.setSubmitter(sub);
            } catch (GedcomException e) {
                log.write("GedcomException:" + e);
                return null;
            }
        }
        return gedcomX;
    }

    /**
  * Merge gedcom and macthes entities in particular
  */
    private boolean mergeGedcom(Gedcom gedcomX, Gedcom gedcomY, Gedcom gedcomZ, Map overlaps, Map idMap, boolean display) {
        String fileNameB = gedcomY.getOrigin().getFile().getName();
        List listEnts = (List) overlaps.get("ZfB");
        for (Iterator it = listEnts.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            flagEntity(ent, "NEW", fileNameB);
        }
        List entities = new ArrayList();
        entities.addAll((List) overlaps.get("ZfB"));
        entities.addAll((List) overlaps.get("ZfA"));
        Collections.sort(entities, sortEntities);
        copyEntities(entities, gedcomZ, display);
        rebuildLinks(gedcomZ, idMap);
        linkGedcom(gedcomZ);
        return true;
    }

    /**
  * Copy gedcom to another gedcom assuming destination gedcom is empty
  */
    private boolean copyEntities(List entities, Gedcom gedcomZ, boolean display) {
        try {
            for (Iterator it = entities.iterator(); it.hasNext(); ) {
                Entity entX = (Entity) it.next();
                Entity entZ = null;
                entZ = gedcomZ.createEntity(entX.getTag(), entX.getId());
                copyEntity(entX, entZ);
            }
        } catch (GedcomException e) {
            log.write("GedcomException:" + e);
            return false;
        }
        return true;
    }

    /**
  * Rebuild links for overlapping entities
  */
    private boolean rebuildLinks(Gedcom gedcom, Map idMap) {
        List entities = gedcom.getEntities();
        for (Iterator it = entities.iterator(); it.hasNext(); ) {
            Entity ent = (Entity) it.next();
            List ps = ent.getProperties(PropertyXRef.class);
            for (Iterator itr = ps.iterator(); itr.hasNext(); ) {
                PropertyXRef xref = (PropertyXRef) itr.next();
                String targetId = (String) xref.getValue();
                targetId = targetId.substring(1, targetId.length() - 1);
                if (gedcom.getEntity(targetId) != null) continue;
                String newTargetId = (String) idMap.get(targetId);
                xref.setValue((newTargetId != null) ? "@" + newTargetId + "@" : "");
            }
        }
        return true;
    }

    /**
  * Save Gedcom file
  */
    private boolean saveGedcom(Gedcom gedcomX) {
        File fileX = gedcomX.getOrigin().getFile();
        GedcomWriter writerX = null;
        try {
            writerX = new GedcomWriter(gedcomX, fileX.getName(), "", new FileOutputStream(fileX));
        } catch (IOException e) {
            log.write("IOexception:" + e);
            return false;
        }
        try {
            writerX.write();
        } catch (GedcomIOException e) {
            log.write("GedcomIOexception:" + e);
            return false;
        }
        log.write("   LinesWritten: " + writerX.getLines());
        return true;
    }

    /**
  * Comparator to sort entities
  */
    private Comparator sortEntities = new Comparator() {

        public int compare(Object o1, Object o2) {
            Entity ent1 = (Entity) o1;
            Entity ent2 = (Entity) o2;
            String id1 = ent1.getId();
            String id2 = ent2.getId();
            String tag1 = ent1.getTag();
            String tag2 = ent2.getTag();
            String s1 = "", s2 = "";
            int n1 = 0, n2 = 0;
            if (tag1 == Gedcom.INDI) s1 = "A"; else if (tag1 == Gedcom.FAM) s1 = "B"; else if (tag1 == Gedcom.NOTE) s1 = "C"; else if (tag1 == Gedcom.SOUR) s1 = "D"; else if (tag1 == Gedcom.REPO) s1 = "E"; else if (tag1 == Gedcom.SUBM) s1 = "F";
            if (tag2 == Gedcom.INDI) s2 = "A"; else if (tag2 == Gedcom.FAM) s2 = "B"; else if (tag2 == Gedcom.NOTE) s2 = "C"; else if (tag2 == Gedcom.SOUR) s2 = "D"; else if (tag2 == Gedcom.REPO) s2 = "E"; else if (tag2 == Gedcom.SUBM) s2 = "F";
            if (s1.compareTo(s2) != 0) return s1.compareTo(s2);
            int start = 0, end = id1.length() - 1;
            while (start <= end && !Character.isDigit(id1.charAt(start))) start++;
            while (end >= start && !Character.isDigit(id1.charAt(end))) end--;
            if (end < start) n1 = 0; else n1 = (int) Integer.parseInt(id1.substring(start, end + 1));
            start = 0;
            end = id2.length() - 1;
            while (start <= end && !Character.isDigit(id2.charAt(start))) start++;
            while (end >= start && !Character.isDigit(id2.charAt(end))) end--;
            if (end < start) n2 = 0; else n2 = (int) Integer.parseInt(id2.substring(start, end + 1));
            return (n1 - n2);
        }
    };

    /**
  * Calculates confidence level of matching between 2 individuals
  */
    private ConfidenceMatch assessConfidenceIndi(Person p1, Person p2, boolean sameGedcom, Map confList) {
        ConfidenceMatch match = new ConfidenceMatch((Entity) p1.indi, (Entity) p2.indi);
        int score[] = { 0, 0 };
        if ((p1 != null) && (p2 != null)) assessIndi(p1, p2, score, true);
        if (!sameGedcom && (report.setting_analysis3 == ANA3_CONNEC)) {
            if ((p1.father != null) && (p2.father != null)) assessIndi(p1.father, p2.father, score, false);
            if ((p1.mother != null) && (p2.mother != null)) assessIndi(p1.mother, p2.mother, score, false);
            if (!p1.partners.isEmpty() && !p2.partners.isEmpty()) assessIndiTab(p1.partners, p2.partners, score);
            if (!p1.kids.isEmpty() && !p2.kids.isEmpty()) assessIndiTab(p1.kids, p2.kids, score);
            if (!p1.siblings.isEmpty() && !p2.siblings.isEmpty()) assessIndiTab(p1.siblings, p2.siblings, score);
        }
        if (score[1] == 0) score[1] = 1;
        match.confLevel = (int) (score[0] * 100 / score[1]);
        if (!sameGedcom && (match.confLevel > report.setting_autoMergingLevel)) {
            match.confirmed = true;
            match.toBeMerged = true;
            match.choice = 3;
        }
        return match;
    }

    /**
  * Calculates confidence level of matching between 2 entities (non individuals)
  */
    private ConfidenceMatch assessConfidenceInfo(Info i1, Info i2, boolean sameGedcom, Map confList) {
        ConfidenceMatch match = new ConfidenceMatch((Entity) i1.entity, (Entity) i2.entity);
        int score[] = { 0, 0 };
        if (i1.titleLength > 0) {
            if (i1.title.compareTo(i2.title) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(i1.titleCode, i1.titleLength, i2.titleCode, i2.titleLength, score, true);
        } else {
            score[1] += 100;
        }
        if (i1.textLength > 0) {
            if (i1.text.compareTo(i2.text) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(i1.textCode, i1.textLength, i2.textCode, i2.textLength, score, false);
        }
        if (i1.authLength > 0) {
            if (i1.auth.compareTo(i2.auth) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(i1.authCode, i1.authLength, i2.authCode, i2.authLength, score, false);
        }
        if (i1.abbrLength > 0) {
            if (i1.abbr.compareTo(i2.abbr) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(i1.abbrCode, i1.abbrLength, i2.abbrCode, i2.abbrLength, score, false);
        }
        if (score[1] == 0) score[1] = 1;
        match.confLevel = (int) (score[0] * 100 / score[1]);
        if (!sameGedcom && (match.confLevel > report.setting_autoMergingLevel)) {
            match.confirmed = true;
            match.toBeMerged = true;
            match.choice = 3;
        }
        return match;
    }

    /**
  * Assess confidence level between 2 individuals
  */
    private void assessIndi(Person p1, Person p2, int[] scoreTotal, boolean fullCheck) {
        int score[] = { 0, 0 };
        if (p1.sex == p2.sex) {
            score[0] += 100;
        }
        score[1] += 100;
        if (p1.lastNameLength > 0) {
            if (p1.lastName.compareTo(p2.lastName) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.lastNameCode, p1.lastNameLength, p2.lastNameCode, p2.lastNameLength, score, true && fullCheck);
        } else {
            score[1] += (fullCheck ? 100 : 0);
        }
        if (p1.firstNameLength > 0) {
            if (p1.firstName.compareTo(p2.firstName) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.firstNameCode, p1.firstNameLength, p2.firstNameCode, p2.firstNameLength, score, true && fullCheck);
        } else {
            score[1] += (fullCheck ? 100 : 0);
        }
        if (p1.bS > 0) {
            matchJD(p1.bS, p1.bE, p2.bS, p2.bE, score, true && fullCheck);
        } else {
            score[1] += (fullCheck ? 100 : 0);
        }
        if (p1.birthCityLength > 0) {
            if (p1.birthCity.compareTo(p2.birthCity) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.birthCityCode, p1.birthCityLength, p2.birthCityCode, p2.birthCityLength, score, true && fullCheck);
        } else {
            score[1] += (fullCheck ? 100 : 0);
        }
        if ((p1.birthPlaceLength > 0) && fullCheck) {
            if (p1.birthPlace.compareTo(p2.birthPlace) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.birthPlaceCode, p1.birthPlaceLength, p2.birthPlaceCode, p2.birthPlaceLength, score, false);
        } else {
            ;
        }
        if (p1.bS > 0) {
            matchJD(p1.dS, p1.dE, p2.dS, p2.dE, score, false);
        } else {
            ;
        }
        if (p1.deathCityLength > 0) {
            if (p1.deathCity.compareTo(p2.deathCity) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.deathCityCode, p1.deathCityLength, p2.deathCityCode, p2.deathCityLength, score, false);
        } else {
            ;
        }
        if ((p1.deathPlaceLength > 0) && fullCheck) {
            if (p1.deathPlace.compareTo(p2.deathPlace) == 0) {
                score[0] += 100;
                score[1] += 100;
            } else matchCode(p1.deathPlaceCode, p1.deathPlaceLength, p2.deathPlaceCode, p2.deathPlaceLength, score, false);
        } else {
            ;
        }
        if (score[1] == 0) score[1] = 1;
        int total = (int) (score[0] * 100 / score[1]);
        scoreTotal[0] += total;
        scoreTotal[1] += 100;
    }

    /**
  * Assess confidence level between 2 arrays of individuals
  */
    private void assessIndiTab(HashSet persons1, HashSet persons2, int[] score) {
        if ((persons1 == null) || (persons2 == null)) return;
        int points = 0, cnt = 0;
        HashSet set1 = new HashSet();
        HashSet set2 = new HashSet();
        if (persons1.size() > persons2.size()) {
            for (int i = 0; i < persons2.size(); i++) set1.add((Person) persons2.iterator().next());
            for (int i = 0; i < persons1.size(); i++) set2.add((Person) persons1.iterator().next());
        } else {
            for (int i = 0; i < persons1.size(); i++) set1.add((Person) persons1.iterator().next());
            for (int i = 0; i < persons2.size(); i++) set2.add((Person) persons2.iterator().next());
        }
        while ((!set1.isEmpty()) && (!set2.isEmpty())) {
            int max = 0, tmp = 0;
            int scoreTmp[] = { 0, 0 };
            Person match = null;
            Person p1 = (Person) set1.iterator().next();
            for (Iterator it = set2.iterator(); it.hasNext(); ) {
                Person p2 = (Person) it.next();
                assessIndi(p1, p2, scoreTmp, false);
                tmp = scoreTmp[0] * 100 / scoreTmp[1];
                if (tmp > max) {
                    max = tmp;
                    match = p2;
                }
            }
            set1.remove(p1);
            set2.remove(match);
            points += max;
            cnt += 100;
        }
        score[0] += points;
        score[1] += cnt;
    }

    /**
  * Match string using a representative code
  */
    private void matchCode(int[] c1, int l1, int[] c2, int l2, int[] score, boolean mandatory) {
        if (l1 * l2 == 0) return;
        int sum = 0;
        for (int i = 0; i < c1.length; i++) {
            sum += Math.min(c1[i], c2[i]) * 2;
        }
        score[0] += (int) (sum * 100 / (l1 + l2));
        if (mandatory || (sum > 0)) score[1] += 100;
    }

    /**
  * Match date using julian day 
  */
    private void matchJD(int s1, int e1, int s2, int e2, int[] score, boolean mandatory) {
        int points = 0, cnt = 0;
        if ((s1 == 0) || (s2 == 0) || (e1 == 0) || (e2 == 0)) return;
        if ((s1 == s2) && (e1 == e2)) {
            score[0] += 100;
            score[1] += 100;
            return;
        }
        s1 = s1 / 365;
        e1 = e1 / 365;
        s2 = s2 / 365;
        e2 = e2 / 365;
        if ((s1 == e2) && (s2 == e2)) {
            if (Math.abs(s1 - s2) <= 1) points = 80; else if (Math.abs(s1 - s2) <= 5) points = 50; else if (Math.abs(s1 - s2) <= 10) points = 20; else points = 0;
        } else if (e1 < s2) {
            if ((s2 - e1) <= 1) points = 80; else if ((s2 - e1) <= 5) points = 50; else if ((s2 - e1) <= 10) points = 20; else points = 0;
        } else if (e2 < s1) {
            if ((s1 - e2) <= 1) points = 80; else if ((s1 - e2) <= 5) points = 50; else if ((s1 - e2) <= 10) points = 20; else points = 0;
        } else if (s2 >= s1) {
            points = 90;
        } else {
            points = 90;
        }
        cnt = 100;
        score[0] += points;
        score[1] += cnt;
    }

    /**
  * Get Gedcom output
  */
    public Gedcom getGedcomOutput() {
        return gedcomOutput;
    }

    /**
  * Get Gedcom output
  */
    private void displayOptions() {
        log.write("Execution options");
        log.write("-----------------");
        log.write(report.translate("setting_assessOnly") + "=" + report.setting_assessOnly);
        log.write(report.translate("setting_appendFiles") + "=" + report.setting_appendFiles);
        log.write(report.translate("setting_chkdup") + "=" + report.setting_chkdup);
        log.write(report.translate("setting_analysis1") + "=" + report.setting_analysis1s[report.setting_analysis1]);
        log.write(report.translate("setting_inclNoBirth") + "=" + report.setting_inclNoBirth);
        log.write(report.translate("setting_analysis3") + "=" + report.setting_analysis3s[report.setting_analysis3]);
        log.write(report.translate("setting_autoMergingLevel") + "=" + report.setting_autoMergingLevel);
        log.write(report.translate("setting_askThreshold") + "=" + report.setting_askThreshold);
        log.write(report.translate("setting_ruleEntity") + "=" + report.setting_ruleEntitys[report.setting_ruleEntity]);
        log.write(report.translate("setting_headerChosen") + "=" + report.setting_headerChosens[report.setting_headerChosen]);
        log.write(report.translate("setting_outputFileExt") + "=" + report.setting_outputFileExt);
        log.write(report.translate("setting_keepAecl") + "=" + report.setting_keepAecl);
        log.write(report.translate("setting_keepAcon") + "=" + report.setting_keepAcon);
        log.write(report.translate("setting_keepBcon") + "=" + report.setting_keepBcon);
        log.write(report.translate("setting_keepBecl") + "=" + report.setting_keepBecl);
        log.write(report.translate("setting_flagChanges") + "=" + report.setting_flagChanges);
        log.write(report.translate("setting_displayMergeHistory") + "=" + report.setting_displayMergeHistory);
        log.write(report.translate("setting_displayDetails") + "=" + report.setting_displayDetails);
        log.write(report.translate("setting_logOption") + "=" + report.setting_logOption);
        log.write("-----------------");
        log.write(" ");
        return;
    }
}
