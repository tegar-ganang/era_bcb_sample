package com.osisc.autodoc.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.osisc.autodoc.CodeTokenStream;
import com.osisc.autodoc.DocWriter;
import com.osisc.autodoc.Indexable;
import com.osisc.autodoc.SourceWriter;
import com.osisc.autodoc.TokenStream;
import com.osisc.autodoc.Assertions;
import com.osisc.Assert;
import com.osisc.ReflexionException;
import com.osisc.io.FileIOException;
import com.osisc.io.FileOutputException;
import com.osisc.util.MultiplexedIterator;
import com.osisc.util.PropertiesFilter;

/**
 * This class represents the list of documented declarations of a file.
 */
public class DocList implements Indexable, Comparable {

    /** The name of the file */
    private final String fileName;

    /** The list of the docs of the file */
    private final List docList = new ArrayList();

    /** The index map of all docs */
    private Map indexMap;

    /** @param file The name of the file */
    DocList(String file) {
        fileName = file;
    }

    /**
    * @return The name of the file
    * @see com.osisc.autodoc.Indexable
    */
    public String getName() {
        if (docList.size() > 0) {
            Doc d = (Doc) docList.get(0);
            if (d.getTypeCode() == Doc.FILE) return d.getName();
        }
        return fileName.substring(1 + Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')));
    }

    /** 
    * @param context The global declaration context map 
    */
    public void setContextMap(Map context) {
        for (Iterator i = docList.iterator(); i.hasNext(); ) {
            Doc d = (Doc) i.next();
            int type = d.getTypeCode();
            if (d.isSelected(Doc.extractProps)) {
                if (type == Doc.NUMBER) {
                    if (d.getContext() != null) {
                        if (context.containsKey(d.getContext().toString())) {
                            List docs = (List) context.get(d.getContext().toString());
                            docs.add(d);
                        } else {
                            List docs = new ArrayList();
                            docs.add(d);
                            context.put(d.getContext().toString(), docs);
                        }
                    } else {
                        if (context.containsKey("Numbers")) {
                            List docs = (List) context.get("Numbers");
                            docs.add(d);
                        } else {
                            List docs = new ArrayList();
                            docs.add(d);
                            context.put("Numbers", docs);
                        }
                    }
                }
            }
        }
    }

    /**
    * @return The name of the file, including path.
    * @see com.osisc.autodoc.Indexable
    */
    public String getFile() {
        if (docList.size() > 0) {
            Doc d = (Doc) docList.get(0);
            if (d.getTypeCode() == Doc.FILE) return d.getFile();
        }
        return fileName;
    }

    /**
    * This method orders doclists based on the value of the "toc" tag of the
    * first doc comment, or if no such tags exists the filename, of the doclists.
    * @param o A doclist to compare to
    * @see java.lang.Comparable
    */
    public int compareTo(Object o) {
        return getFile().compareTo(((DocList) o).getFile());
    }

    /**
    * @param o doclist to compare to
    * @return True if this doc list has the same toc entry or file name as o
    */
    public boolean equals(Object o) {
        if (o instanceof DocList) return getFile().equals(((DocList) o).getFile()); else return false;
    }

    /** @return A brief description of the file */
    TokenStream getBrief() {
        if (docList.size() > 0) {
            Doc d = (Doc) docList.get(0);
            if (d.getTypeCode() == Doc.FILE) return d.getBrief();
        }
        return null;
    }

    /** 
    * @param filter The filter to check against 
    * @return True if this doc list is selected in filter 
    */
    public boolean isSelected(PropertiesFilter filter) {
        if (docList.size() > 0) {
            Doc doc = (Doc) docList.get(0);
            if (doc.getTypeCode() == Doc.FILE) {
                if (filter.get(doc.getScopeCode()) && (!doc.isSet(Doc.DEPRECATED) || filter.get(Doc.DEPRECATED))) {
                    return true;
                }
            } else {
                if (filter.get(Doc.DEFAULT_SCOPE)) return true;
            }
        }
        return false;
    }

    /**
    * @param doc The declaration to add (This parameter is specified as Object
    * to allow DocWorker to be independent of the Doc class)
    */
    void add(Object doc) {
        Doc d = (Doc) doc;
        PropertiesFilter verify = Doc.verifyProps;
        PropertiesFilter create = Doc.createProps;
        if ((docList.size() == 0) && (d.getTypeCode() != Doc.FILE)) {
            if (verify.get(Doc.FILE) && verify.get(Doc.PRIVATE)) {
                System.err.println(fileName + ": " + d.getLine() + ": WARNING: The file has no initial file documentation " + "comment. Check that the file documentation comment is " + "located before other comments.");
            }
            if (create.get() || verify.get()) {
                Doc f = new Doc(getFile(), getFile(), 1, Doc.SOURCE_INPUT, Doc.PRIVATE);
                f.setName(fileName.substring(1 + Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))));
                f.setType(Doc.FILE);
                docList.add(f);
            }
        }
        docList.add(d);
    }

    /** @param index The global declaration index map */
    public void setIndex(Map index) {
        Map docMap = new HashMap();
        if (Assertions.LEVEL >= Assert.LOW) Assert.correct(indexMap == null);
        indexMap = index;
        for (Iterator i = docList.iterator(); i.hasNext(); ) {
            Doc d = (Doc) i.next();
            docMap.put(d.getName(), d);
            for (Iterator it = d.getAliases(); it.hasNext(); ) {
                docMap.put(it.next(), d);
            }
        }
        for (Iterator i = docList.iterator(); i.hasNext(); ) {
            Doc d = (Doc) i.next();
            ArrayList ml = new ArrayList();
            for (Iterator it = d.getMembers(); it.hasNext(); ) {
                ml.add(it.next());
            }
            for (Iterator it = ml.iterator(); it.hasNext(); ) {
                String s = (String) it.next();
                Doc m = (Doc) docMap.get(s);
                if (m == null) {
                    System.err.println(fileName + ": " + d.getLine() + ": WARNING: Member \"" + s + "\" not found. " + "Check that a matching declaration exists in this file.");
                }
                d.resolveMember(s, m);
                m.setMember(true);
            }
        }
        for (Iterator i = docList.iterator(); i.hasNext(); ) {
            Doc d = (Doc) i.next();
            if (d.isSelected(Doc.extractProps)) {
                String s = d.getName();
                index.put(s, d);
                index.put(new TokenStream.Token(s), d);
                for (Iterator it = d.getAliases(); it.hasNext(); ) {
                    s = (String) it.next();
                    index.put(new TokenStream.Token(s), d);
                    index.put(s, d);
                }
                if (d.shouldExtract(Doc.MEMBER)) {
                    for (Iterator it = d.getMembers(); it.hasNext(); ) {
                        s = ((Doc) it.next()).getName();
                        index.put(new TokenStream.Token(s), d);
                        index.put(s, d);
                    }
                }
            }
        }
    }

    /**
    * Writes the file documentation section
    * @param out The DocWriter to use
    */
    private void emitFileSection(DocWriter out) throws IOException {
        Doc d = (Doc) docList.get(0);
        PropertiesFilter extract = Doc.extractProps;
        if (Assertions.LEVEL >= Assert.LOW) Assert.equal(d.getTypeCode(), Doc.FILE);
        if (d.isSelected(extract)) {
            boolean b, f;
            CodeTokenStream cts;
            out.writeSectionHeader(d.getName());
            if (d.shouldExtract(Doc.ALIAS)) {
                cts = new CodeTokenStream();
                for (Iterator it = d.getAliases(); it.hasNext(); ) {
                    cts.append(it.next() + ", ");
                }
                if (!cts.isEmpty()) out.writeCodeSectionItem("Alias", cts, indexMap);
            }
            if (d.shouldExtract(Doc.SCOPE)) out.writeSectionItem("Scope", d.getScope(), indexMap);
            b = d.shouldExtract(Doc.BRIEF);
            f = d.shouldExtract(Doc.FULL);
            if (b && f) out.writeSectionItems("Description", d.getDescriptions(), indexMap); else if (b) out.writeSectionItem("Description", d.getBrief(), indexMap); else if (f) out.writeSectionItems("Description", d.getFulls(), indexMap); else ;
            if (d.shouldExtract(Doc.DEPRECATED)) out.writeSectionItem("Deprecated", d.getDeprecation(), indexMap);
            if (d.shouldExtract(Doc.RESTRICTION)) out.writeSectionItem("Restriction", d.getRestriction(), indexMap);
            if (d.shouldExtract(Doc.ERROR)) out.writeSectionItemList("Error", d.getErrors(), indexMap);
            if (d.shouldExtract(Doc.VERSION)) out.writeSectionItem("Version", d.getVersion(), indexMap);
            if (d.shouldExtract(Doc.AUTHOR)) out.writeSectionItem("Author", d.getAuthor(), indexMap);
            if (d.shouldExtract(Doc.LIBRARY)) out.writeSectionItemList("Libraries", d.getLibraries(), indexMap);
            if (d.shouldExtract(Doc.STANDARD)) out.writeSectionItem("Standard", d.getStandard(), indexMap);
            if (d.shouldExtract(Doc.EXAMPLE)) out.writeCodeSectionItem("Example", d.getExample(), indexMap);
            if (d.shouldExtract(Doc.TODO)) out.writeSectionItem("Todo", d.getTodo(), indexMap);
            if (d.shouldExtract(Doc.SEEALSO)) {
                cts = new CodeTokenStream();
                for (Iterator it = d.getSeeAlsos(); it.hasNext(); ) {
                    cts.append(it.next() + "\n");
                }
                if (!cts.isEmpty()) out.writeCodeSectionItem("See Also", cts, indexMap);
            }
            if (d.shouldExtract(Doc.COPYRIGHT)) out.writeSectionItem("Copyright", d.getCopyright(), indexMap);
            out.writeSectionFooter();
        }
    }

    /**
    * Writes the overview section
    * @param out The DocWriter to use
    * @param sections The sections of the file
    */
    private void emitOverviewSection(DocWriter out, SectionList sections) throws IOException {
        Iterator h = sections.headerIterator();
        Iterator s = sections.sectionIterator();
        while (s.hasNext()) {
            out.writeOverviewSectionHeader(h.next());
            for (Iterator i = (Iterator) s.next(); i.hasNext(); ) {
                Doc d = (Doc) i.next();
                out.writeOverviewSectionItem(getFile(), d.getName(), d.getBrief());
            }
            out.writeOverviewSectionFooter();
        }
    }

    /**
    * Writes a details section
    * @param out The DocWriter to use
    * @param d The documented declaration
    */
    private void emitDocSection(DocWriter out, Doc d) throws IOException {
        PropertiesFilter extract = Doc.extractProps;
        if (d.isSelected(extract)) {
            boolean b, f;
            CodeTokenStream cts;
            MultiplexedIterator mit;
            out.writeSectionHeader(d.getName());
            if (d.shouldExtract(Doc.INCLUDE)) {
                cts = new CodeTokenStream();
                if (d.shouldExtract(Doc.MEMBER)) {
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        for (Iterator ii = ((Doc) mi.next()).getIncludes(); ii.hasNext(); ) {
                            cts.append("#include " + ii.next() + "\n");
                        }
                    }
                }
                for (Iterator it = d.getIncludes(); it.hasNext(); ) {
                    cts.append("#include " + it.next() + "\n");
                }
                if (!cts.isEmpty()) out.writeCodeSectionItem("Include", cts, indexMap);
            }
            if (d.shouldExtract(Doc.DECLARATION)) {
                cts = new CodeTokenStream();
                if (d.shouldExtract(Doc.MEMBER)) {
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        TokenStream md = ((Doc) mi.next()).getDeclaration();
                        if (md != null) {
                            cts.append(md);
                            cts.append("\n");
                        }
                    }
                }
                if (d.getDeclaration() != null) cts.append(d.getDeclaration());
                out.writeCodeSectionItem("Declaration", cts, indexMap);
            }
            b = d.shouldExtract(Doc.BRIEF);
            f = d.shouldExtract(Doc.FULL);
            if (b && f) out.writeSectionItems("Description", d.getDescriptions(), indexMap); else if (b) out.writeSectionItem("Description", d.getBrief(), indexMap); else if (f) out.writeSectionItems("Description", d.getFulls(), indexMap); else ;
            if (d.shouldExtract(Doc.MEMBER)) out.writeSectionItemList("Members", d.getMembers(), indexMap);
            if (d.shouldExtract(Doc.DEPRECATED)) out.writeSectionItem("Deprecated", d.getDeprecation(), indexMap);
            if (d.shouldExtract(Doc.RESTRICTION)) out.writeSectionItem("Restriction", d.getRestriction(), indexMap);
            if (d.shouldExtract(Doc.FIELD)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Fields", d.getFields(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getFields());
                    }
                    mit.append(d.getFields());
                    out.writeSectionItemList("Fields", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.INPUT)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Input", d.getInputs(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getInputs());
                    }
                    mit.append(d.getInputs());
                    out.writeSectionItemList("Input", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.OUTPUT)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Output", d.getOutputs(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getOutputs());
                    }
                    mit.append(d.getOutputs());
                    out.writeSectionItemList("Output", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.VALUE)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Value", d.getValues(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getValues());
                    }
                    mit.append(d.getValues());
                    out.writeSectionItemList("Value", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.RETURN)) out.writeSectionItem("Return", d.getReturns(), indexMap);
            if (d.shouldExtract(Doc.ERROR)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Error", d.getErrors(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getErrors());
                    }
                    mit.append(d.getErrors());
                    out.writeSectionItemList("Error", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.ENV)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Environment", d.getEnvs(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getEnvs());
                    }
                    mit.append(d.getEnvs());
                    out.writeSectionItemList("Environment", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.TIME)) out.writeSectionItem("Time", d.getTime(), indexMap);
            if (d.shouldExtract(Doc.SIZE)) out.writeSectionItem("Size", d.getSize(), indexMap);
            if (d.shouldExtract(Doc.VERSION)) out.writeSectionItem("Version", d.getVersion(), indexMap);
            if (d.shouldExtract(Doc.SCOPE)) out.writeSectionItem("Scope", d.getScope(), indexMap);
            if (d.shouldExtract(Doc.CONTEXT)) out.writeSectionItem("Context", d.getContext(), indexMap);
            if (d.shouldExtract(Doc.LIBRARY)) {
                if (!d.shouldExtract(Doc.MEMBER)) {
                    out.writeSectionItemList("Library", d.getLibraries(), indexMap);
                } else {
                    mit = new MultiplexedIterator();
                    for (Iterator mi = d.getMembers(); mi.hasNext(); ) {
                        mit.append(((Doc) mi.next()).getLibraries());
                    }
                    mit.append(d.getLibraries());
                    out.writeSectionItemList("Library", mit, indexMap);
                }
            }
            if (d.shouldExtract(Doc.STANDARD)) out.writeSectionItem("Standard", d.getStandard(), indexMap);
            if (d.shouldExtract(Doc.EXAMPLE)) out.writeCodeSectionItem("Example", d.getExample(), indexMap);
            if (d.shouldExtract(Doc.TODO)) out.writeSectionItem("Todo", d.getTodo(), indexMap);
            if (d.shouldExtract(Doc.SEEALSO)) {
                cts = new CodeTokenStream();
                for (Iterator it = d.getSeeAlsos(); it.hasNext(); ) {
                    cts.append(it.next() + "\n");
                }
                if (!cts.isEmpty()) out.writeCodeSectionItem("See Also", cts, indexMap);
            }
            out.writeSectionFooter();
        }
    }

    /**
    * Writes all of the documentation for this file.
    * @param control The controller to use
    * @throws FileOutputException
    * @throws ReflexionException
    */
    void emitDocumentation(DocController control) throws FileOutputException, ReflexionException {
        DocWriter out = null;
        File outfile = new File(control.getPath(), getFile());
        try {
            try {
                SectionList sections = new SectionList(docList.iterator());
                out = (DocWriter) control.getDocWriter(control.getPath(), outfile);
                out.writeTitle(getFile());
                out.writeTopIndex(topLinks.iterator());
                out.writeTitleHeader(getFile());
                out.writeSectionIndex(sections.headerIterator());
                if (docList.size() > 0) {
                    Iterator i = docList.iterator();
                    if (((Doc) docList.get(0)).getTypeCode() == Doc.FILE) {
                        emitFileSection(out);
                        i.next();
                    }
                    emitOverviewSection(out, sections);
                    while (i.hasNext()) {
                        emitDocSection(out, (Doc) i.next());
                    }
                }
                out.writeFooter();
            } finally {
                if (out != null) out.close();
            }
        } catch (IOException ioe) {
            throw new FileOutputException(ioe.getMessage(), outfile.getPath() + ((out != null) ? out.extensionString() : ""));
        }
    }

    /**
    * Writes the file documentation section
    * @param out The SourceWriter to use
    */
    private void emitFileSection(SourceWriter out) throws IOException {
        Doc d = (Doc) docList.get(0);
        PropertiesFilter create = Doc.createProps;
        if (Assertions.LEVEL >= Assert.LOW) Assert.equal(d.getTypeCode(), Doc.FILE);
        if (d.isSelected(create) || d.isFromComment()) {
            out.writeSectionHeader();
            if (d.shouldCreate(Doc.SCOPE)) out.writeSectionItem(d.getScope(), (Object) null);
            out.writeSectionItem(Doc.propertyNames[Doc.FILE], d.getName());
            if (d.shouldCreate(Doc.ALIAS)) out.writeSectionItems(Doc.propertyNames[Doc.ALIAS], d.getAliases());
            if (d.shouldCreate(Doc.BRIEF)) out.writeSectionItem(Doc.propertyNames[Doc.BRIEF], d.getBrief());
            if (d.shouldCreate(Doc.FULL)) out.writeSectionItems(Doc.propertyNames[Doc.FULL], d.getFulls());
            if (d.isSet(Doc.DEPRECATED)) out.writeSectionItem(Doc.propertyNames[Doc.DEPRECATED], d.getDeprecation());
            if (d.shouldCreate(Doc.RESTRICTION)) out.writeSectionItem(Doc.propertyNames[Doc.RESTRICTION], d.getRestriction());
            if (d.shouldCreate(Doc.ERROR)) out.writeSectionItemList(Doc.propertyNames[Doc.ERROR], d.getErrors());
            if (d.shouldCreate(Doc.VERSION)) out.writeSectionItem(Doc.propertyNames[Doc.VERSION], d.getVersion());
            if (d.shouldCreate(Doc.AUTHOR)) out.writeSectionItem(Doc.propertyNames[Doc.AUTHOR], d.getAuthor());
            if (d.shouldCreate(Doc.LIBRARY)) out.writeSectionItemList(Doc.propertyNames[Doc.LIBRARY], d.getLibraries());
            if (d.shouldCreate(Doc.STANDARD)) out.writeSectionItem(Doc.propertyNames[Doc.STANDARD], d.getStandard());
            if (d.shouldCreate(Doc.SEEALSO)) out.writeSectionItems(Doc.propertyNames[Doc.SEEALSO], d.getSeeAlsos());
            if (d.shouldCreate(Doc.EXAMPLE)) out.writeCodeSectionItem(Doc.propertyNames[Doc.EXAMPLE], d.getExample());
            if (d.shouldCreate(Doc.TODO)) out.writeSectionItem(Doc.propertyNames[Doc.TODO], d.getTodo());
            if (d.shouldCreate(Doc.COPYRIGHT)) out.writeSectionItem(Doc.propertyNames[Doc.COPYRIGHT], d.getCopyright());
            out.writeSectionFooter();
        }
    }

    /**
    * @param out The source writer to use
    * @param next The next doc comment that should be inserted
    * @param in The source input to use
    */
    private void emitSourceSection(SourceWriter out, Doc next, LineNumberReader in) throws IOException {
        String s;
        if (next != null) {
            int l = next.getLine();
            int i = in.getLineNumber();
            for (; i < l; i++) {
                if ((s = in.readLine()) == null) throw new IOException("Encoutered unexpected end of file");
                out.writeCodeLine(s);
            }
            if (next.isFromComment()) {
                l = next.getEndLine() + 1;
                for (; i < l; i++) {
                    if ((s = in.readLine()) == null) throw new IOException("Encoutered unexpected end of file");
                }
            }
        } else {
            while ((s = in.readLine()) != null) out.writeCodeLine(s);
        }
    }

    /**
    * Writes the documentation section
    * @param out The SourceWriter to use
    * @param d The documentation comment to write
    */
    private void emitDocSection(SourceWriter out, Doc d) throws IOException {
        PropertiesFilter create = Doc.createProps;
        if (d.isSelected(create) || d.isFromComment()) {
            out.writeSectionHeader();
            if (d.shouldCreate(Doc.SCOPE)) out.writeSectionItem(d.getScope(), null);
            out.writeSectionItem(Doc.propertyNames[d.getTypeCode()], d.getName());
            if (d.shouldCreate(Doc.ALIAS)) out.writeSectionItems(Doc.propertyNames[Doc.ALIAS], d.getAliases());
            if (d.shouldCreate(Doc.INCLUDE)) {
                for (Iterator i = d.getIncludes(); i.hasNext(); ) {
                    out.writeSectionItem(Doc.propertyNames[Doc.INCLUDE], i.next());
                }
            }
            if (d.shouldCreate(Doc.DECLARATION)) out.writeCodeSectionItem(Doc.propertyNames[Doc.DECLARATION], d.getDeclaration());
            if (d.shouldCreate(Doc.BRIEF)) out.writeSectionItem(Doc.propertyNames[Doc.BRIEF], d.getBrief());
            if (d.shouldCreate(Doc.FULL)) out.writeSectionItems(Doc.propertyNames[Doc.FULL], d.getFulls());
            if (d.isSet(Doc.DEPRECATED)) out.writeSectionItem(Doc.propertyNames[Doc.DEPRECATED], d.getDeprecation());
            if (d.shouldCreate(Doc.RESTRICTION)) out.writeSectionItem(Doc.propertyNames[Doc.RESTRICTION], d.getRestriction());
            if (d.shouldCreate(Doc.FIELD) && d.getFields().hasNext()) out.writeSectionItemList(Doc.propertyNames[Doc.FIELD], d.getFields());
            if (d.shouldCreate(Doc.INPUT) && d.getInputs().hasNext()) out.writeSectionItemList(Doc.propertyNames[Doc.INPUT], d.getInputs());
            if (d.shouldCreate(Doc.OUTPUT) && d.getOutputs().hasNext()) out.writeSectionItemList(Doc.propertyNames[Doc.OUTPUT], d.getOutputs());
            if (d.shouldCreate(Doc.VALUE)) out.writeSectionItemList(Doc.propertyNames[Doc.VALUE], d.getValues());
            if (d.shouldCreate(Doc.RETURN)) out.writeSectionItem(Doc.propertyNames[Doc.RETURN], d.getReturns());
            if (d.shouldCreate(Doc.ERROR)) out.writeSectionItemList(Doc.propertyNames[Doc.ERROR], d.getErrors());
            if (d.shouldCreate(Doc.ENV)) out.writeSectionItemList(Doc.propertyNames[Doc.ENV], d.getEnvs());
            if (d.shouldCreate(Doc.TIME)) out.writeSectionItem(Doc.propertyNames[Doc.TIME], d.getTime());
            if (d.shouldCreate(Doc.SIZE)) out.writeSectionItem(Doc.propertyNames[Doc.SIZE], d.getSize());
            if (d.shouldCreate(Doc.VERSION)) out.writeSectionItem(Doc.propertyNames[Doc.VERSION], d.getVersion());
            if (d.shouldCreate(Doc.LIBRARY)) out.writeSectionItemList(Doc.propertyNames[Doc.LIBRARY], d.getLibraries());
            if (d.shouldCreate(Doc.STANDARD)) out.writeSectionItem(Doc.propertyNames[Doc.STANDARD], d.getStandard());
            if (d.shouldCreate(Doc.EXAMPLE)) out.writeCodeSectionItem(Doc.propertyNames[Doc.EXAMPLE], d.getExample());
            if (d.shouldCreate(Doc.CONTEXT)) out.writeSectionItem(Doc.propertyNames[Doc.CONTEXT], d.getContext());
            if (d.shouldCreate(Doc.TODO)) out.writeSectionItem(Doc.propertyNames[Doc.TODO], d.getTodo());
            if (d.shouldCreate(Doc.SEEALSO)) out.writeSectionItems(Doc.propertyNames[Doc.SEEALSO], d.getSeeAlsos());
            if (d.shouldCreate(Doc.MEMBER)) out.writeSectionItems(Doc.propertyNames[Doc.MEMBER], d.getSeeAlsos());
            out.writeSectionFooter();
        }
    }

    /**
    * Writes the source file updated with documentation comments
    * @param control The controller to use
    * @throws FileIOException
    * @throws ReflexionException
    */
    void emitSource(SourceController control) throws FileIOException, ReflexionException {
        File inpath = control.getPwd();
        File outpath = control.getPath();
        File outfile = new File(outpath, fileName);
        File infile = new File(inpath, fileName);
        if (Assertions.LEVEL >= Assert.HIGH) {
            try {
                Assert.correct(outpath.getCanonicalFile().equals(outpath));
                Assert.correct(inpath.getCanonicalFile().equals(inpath));
            } catch (IOException ioe) {
                Assert.fail();
            }
        }
        if (inpath.equals(outpath)) {
            File tmpfile = new File(outpath, fileName + "." + System.currentTimeMillis());
            if (!infile.renameTo(tmpfile)) {
                throw new FileIOException("Could not rename file", infile.getPath(), tmpfile.getPath());
            }
            infile = tmpfile;
        }
        try {
            SourceWriter out = null;
            LineNumberReader in = null;
            try {
                out = (SourceWriter) control.getSourceWriter(outfile);
                in = new LineNumberReader(new FileReader(infile));
                if (docList.size() > 0) {
                    Iterator i = docList.iterator();
                    in.setLineNumber(1);
                    emitSourceSection(out, (Doc) i.next(), in);
                    emitFileSection(out);
                    while (i.hasNext()) {
                        Doc d = (Doc) i.next();
                        emitSourceSection(out, d, in);
                        emitDocSection(out, d);
                    }
                    emitSourceSection(out, null, in);
                }
            } finally {
                if (out != null) out.close();
                if (in != null) in.close();
            }
        } catch (IOException ioe) {
            throw new FileIOException(ioe.getMessage(), infile.getPath(), outfile.getPath());
        }
    }

    /**
    * An implementation of the OverviewSection list for the different types
    * of declarations
    */
    private static class SectionList extends OverviewSectionList {

        /**
       * @param docs The documented declarations
       */
        SectionList(Iterator docs) {
            super(overviewHeaders, new List[] { new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList() });
            while (docs.hasNext()) {
                Doc d = (Doc) docs.next();
                int type = d.getTypeCode();
                if (d.isSelected(Doc.extractProps)) {
                    switch(type) {
                        case Doc.FILE:
                            break;
                        case Doc.MACRO:
                            sectionLists[0].add(d);
                            break;
                        case Doc.TYPE:
                            sectionLists[1].add(d);
                            break;
                        case Doc.FUNCTION:
                            sectionLists[2].add(d);
                            break;
                        case Doc.DATA:
                            sectionLists[3].add(d);
                            break;
                        case Doc.GROUP:
                            sectionLists[4].add(d);
                            break;
                        case Doc.PROCESS:
                            sectionLists[5].add(d);
                            break;
                        case Doc.COMMAND:
                            sectionLists[6].add(d);
                            break;
                        case Doc.SIGNAL:
                            sectionLists[7].add(d);
                            break;
                        case Doc.NUMBER:
                            sectionLists[8].add(d);
                            break;
                        default:
                            sectionLists[9].add(d);
                            break;
                    }
                }
            }
        }

        private static final String[] overviewHeaders = { "Macros", "Types", "Functions", "Data", "Groups", "Processes", "Commands", "Signals", "Numbers", "Other" };
    }

    private static final List topLinks = Arrays.asList(new String[] { "Contents", "Overview", "Index", "Numbers" });

    /**
    * Set and return the extract filter docs
    * @return The extract filter docs
    */
    public static PropertiesFilter setExtractProps() {
        if (Assertions.LEVEL >= Assert.LOW) Assert.correct(Doc.extractProps == null);
        return Doc.extractProps = new PropertiesFilter(Doc.propertyNames);
    }

    /**
    * Set and return the verify filter for docs
    * @return The verify filter for docs
    */
    public static PropertiesFilter setVerifyProps() {
        if (Assertions.LEVEL >= Assert.LOW) Assert.correct(Doc.verifyProps == null);
        return Doc.verifyProps = new PropertiesFilter(Doc.propertyNames);
    }

    /**
    * Set and return the create filter for docs
    * @return The create filter for docs
    */
    public static PropertiesFilter setCreateProps() {
        if (Assertions.LEVEL >= Assert.LOW) Assert.correct(Doc.createProps == null);
        return Doc.createProps = new PropertiesFilter(Doc.propertyNames);
    }
}
