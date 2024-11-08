package uk.ac.cam.caret.tagphage.parser;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.commons.io.*;

/** A root class for parsing a particular class of document, which can generate
 * parsers for such documents. Typically, you will receive one of these from a
 * {@link uk.ac.cam.caret.tagphage.parser.ruleparser.Schema}, and you do not construct
 * them yourself. However you may create rules programatically, if you have some odd,
 * compelling reason to do so, by using {@link #addAction(String, Map, AnyAction)}.  
 * 
 * @author Dan Sheppard <dan@caret.cam.ac.uk>
 *
 */
public class ParserFactory {

    private AbstractRules rules = new AbstractRules();

    private static Logger log = Logger.getLogger(ParserFactory.class);

    private FactoryStore fs = new FactoryStore();

    private String default_ns;

    private Set<ResolvedExpression> prefixes = new HashSet<ResolvedExpression>();

    private boolean empty_prefix = false;

    private LinkedList<byte[]> transformers = new LinkedList<byte[]>();

    private List<ParseStateInterceptor> interceptions = new ArrayList<ParseStateInterceptor>();

    /** <b>ADVANCED</b> create parser factory for manual rule addition. This is not
	 * the typical way of creating a parser, see 
	 * {@link uk.ac.cam.caret.tagphage.parser.ruleparser.Schema}.
	 */
    public ParserFactory() {
    }

    /** TODO
	 * 
	 * @param in
	 */
    public void addXSLT(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(in, baos);
        transformers.addFirst(baos.toByteArray());
    }

    byte[][] getXSLTs() {
        return transformers.toArray(new byte[0][]);
    }

    private ResolvedExpression cookAndRegisterExpression(ParserFactory src, ResolvedExpression in) {
        Map<String, String> remapping = new HashMap<String, String>();
        for (String ns : in.getAllUsedNamespaces()) {
            Set<String> map = src.rules.applyNamespaceExpander(ns);
            String anon = rules.addAnonymousNamespace(map);
            remapping.put(ns, anon);
        }
        ResolvedExpression out = new ResolvedExpression(in);
        out.renameNamespaces(remapping);
        return out;
    }

    private void addPrefixesFrom(String name, ParserFactory src) {
        for (ResolvedExpression in : src.prefixes) {
            ResolvedExpression out = cookAndRegisterExpression(src, in);
            fs.addFactory(name, out, src);
        }
        if (src.empty_prefix) fs.addFactory(name, null, src);
    }

    private ParseState makeParser() throws BadConfigException {
        ParseState out = new ParseState(this, rules.makeConcrete(this));
        if (default_ns != null) out.imposeDefaultNamespace(default_ns);
        return out;
    }

    void runInterceptions(ParseState out) {
        for (ParseStateInterceptor in : interceptions) in.intercept(out);
    }

    /** If the default namespace is missing in a document you parse, assume it is this.
	 * 
	 * @param in URI of namespace to assume.
	 */
    public void imposeDefaultNamespace(String in) {
        default_ns = in;
    }

    /** Create a parser to parse a document.
	 * 
	 * @return the parser's {@link ParseState}.
	 * @throws BadConfigException an error occured parsing the rules for this parser
	 */
    public ParseState getParser() throws BadConfigException {
        return makeParser();
    }

    /** <b>ADVANCED</b> Create an inner parser during a nested parse action. 
	 * <em>Do not</em> use this method to create parsers, or nest parsers: it is only
	 * of use in writing very advanced actions. Unless you <em>know</em> you need this
	 * method, it is not the one you are looking for.
	 * 
	 * @param outer Outer parser
	 * @return inner parser
	 * @throws BadConfigException creating inner parser failed
	 */
    public ParseState getParser(ParseState outer) throws BadConfigException {
        ParseState out = makeParser();
        out.setRootParseState(outer);
        return out;
    }

    /** Add an inner {@link ParserFactory} to be used at nest points in outer parser.
	 * This is the correct way to link together parsers. A parser may add a suffix
	 * such that it is only triggered when the nest pattern is followed by that suffix.
	 * This will typically be the outer tag of the inner parser.
	 * 
	 * @param name Name of nest point
	 * @param expr Pattern representing suffix to add
	 * @param ns Namespace resolver to resolve any prefixes used in expr parameter.
	 * May be null, if expr uses no prefixes (eg absolute syntax)
	 * @param pf Inner {@link ParserFactory} to use to parse document.
	 * @throws BadConfigException error in suffix expression or nest point specification
	 */
    public void addNestedFactory(String name, String expr, Map ns, ParserFactory pf) throws BadConfigException {
        ResolvedExpression suffix = null;
        if (expr != null) suffix = new ResolvedExpression(expr, new NamespaceMap(ns));
        fs.addFactory(name, suffix, pf);
    }

    /** Add an inner {@link ParserFactory} to be used at nest points in outer parser.
	 * This is the correct way to link together parsers.
	 * 
	 * @param name Name of nest point
	 * @param pf Inner {@link ParserFactory} to use to parse document.
	 * @throws BadConfigException error in suffix expression or nest point specification
	 */
    public void addNestedFactory(String name, ParserFactory pf) {
        addPrefixesFrom(name, pf);
    }

    /** <b>ADVANCED</b> Return the factory store to use to implement internal nesting
	 * actions. Only of use if heavily customizing nest actions. 
	 * 
	 * @return the factory store.
	 */
    public FactoryStore getFactoryStore() {
        return fs;
    }

    /** <b>ADVANCED</b> Add a rule manually, rather than through a rules file.
	 * 
	 * @param pattern Pattern for rule
	 * @param ns Namespace for supplied pattern
	 * @param a Action to execute
	 * @throws BadConfigException error adding rule (eg. parsing pattern)
	 */
    public void addAction(String pattern, Map ns, AnyAction a) throws BadConfigException {
        log.debug("pattern=" + pattern + " ns=" + new StackPattern(pattern, ns) + " a=" + a.getClass().getName());
        rules.addRule(pattern, ns, a);
    }

    public void addCompositeNamesspace(String composite, String[] components) {
        rules.addCompositeNamespace(composite, new HashSet<String>(Arrays.asList(components)));
    }

    /** <b>ADVANCED</b> Add a prefix which will be used during nesting.  A parser may add a suffix
	 * such that it is only triggered when the nest pattern is followed by that suffix.
	 * This will typically be the outer tag of the inner parser.
	 * 
	 * @param pattern prefix pattern
	 * @param ns namespace for supplied pattern
	 * @throws BadConfigException error adding prefix (eg. bad pattern)
	 */
    public void addPrefix(String pattern, Map ns) throws BadConfigException {
        if (pattern == null || "".equals(pattern)) empty_prefix = true; else prefixes.add(new ResolvedExpression(pattern, new NamespaceMap(ns)));
    }

    public void addInterception(ParseStateInterceptor in) {
        interceptions.add(in);
    }
}
