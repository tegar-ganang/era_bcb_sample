package net.sourceforge.javautil.grammar.impl.parser.pojo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.sourceforge.javautil.common.ReflectionUtil;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;
import net.sourceforge.javautil.common.reflection.cache.ClassConstructor;
import net.sourceforge.javautil.common.reflection.cache.ClassDescriptor;
import net.sourceforge.javautil.common.reflection.cache.ClassProperty;
import net.sourceforge.javautil.grammar.impl.parser.IGrammarTokenDefinitionResolver;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarPartToken;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarReference;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarTokenCompositeLogic;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarTokenDefinition;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarLexerComposite.BasicOrder;
import net.sourceforge.javautil.grammar.impl.parser.patterns.IGrammarTokenCompositeLogic.BasicLogic;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenComposite;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenDefinition;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenLiteral;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenPattern;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenReference;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.GrammarTokenType;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.logic.GrammarTokenCompositeLogicAnd;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.logic.GrammarTokenCompositeLogicOr;
import net.sourceforge.javautil.grammar.impl.parser.patterns.token.logic.GrammarTokenCompositeLogicSeparator;

/**
 * A factory that can generate {@link IGrammarTokenDefinition} from annotated POJO's.
 *  
 * @author elponderador
 * @author $Author$
 * @version $Id$
 */
public class PojoDefinitionFactory implements IGrammarTokenDefinitionResolver {

    public static final IGrammarTokenCompositeLogic AND_LOGIC = new GrammarTokenCompositeLogicAnd();

    public static final IGrammarTokenCompositeLogic OR_LOGIC = new GrammarTokenCompositeLogicOr();

    protected final Map<Class<?>, IGrammarTokenDefinition> classes = new HashMap<Class<?>, IGrammarTokenDefinition>();

    protected final Map<String, IGrammarTokenDefinition> definitions = new HashMap<String, IGrammarTokenDefinition>();

    protected final Map<String, IGrammarPartToken> properties = new HashMap<String, IGrammarPartToken>();

    protected final List<Class<?>> classCreating = new ArrayList<Class<?>>();

    public IGrammarTokenDefinition resolve(String name) {
        return definitions.get(name);
    }

    public IGrammarPartToken createFor(ClassProperty property) {
        String id = property.getDescriptor().getDescribedClass().getName() + "." + property.getName();
        if (!properties.containsKey(id)) {
            Class<?> type = property.getType();
            TokenConfiguration tc = null;
            int minimumOccurence = 0;
            int maximumOccurence = 1;
            Class<?> concrete = type;
            if (Collection.class.isAssignableFrom(type)) {
                concrete = ReflectionUtil.getConcreteGeneric(property.getGenericType(), 0);
                if (concrete == null || concrete == Object.class) {
                    throw new IllegalStateException("Cannot determine concrete token type for: " + property);
                }
                tc = TokenConfiguration.getFor(property, concrete);
                maximumOccurence = 0;
            } else {
                if (!property.isReadable() || !property.isWritable()) throw new IllegalArgumentException("Pojo properties must be read/write: " + property.getJavaMember());
                tc = TokenConfiguration.getFor(property, concrete);
            }
            minimumOccurence = tc.isRequired() ? 1 : 0;
            IGrammarPartToken definition = null;
            if (concrete.isPrimitive() || ReflectionUtil.isBoxedType(concrete) || String.class == concrete || Date.class == concrete || Object.class == concrete || concrete.isEnum()) {
                definition = getSingle(tc, minimumOccurence, maximumOccurence);
                definition.getMetaData().put("property", property);
                definition.getMetaData().put("type", concrete);
            } else {
                TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(concrete, concrete, tc, BasicLogic.Sequential);
                TokenConfiguration pc = TokenConfiguration.getFor(property.getJavaMember());
                if (pc.getPrefix().length != 0 || pc.getSuffix().length != 0) {
                    List<IGrammarPartToken> tokens = new ArrayList<IGrammarPartToken>();
                    GrammarTokenReference ref = getReference(concrete, tcc, 1, 1);
                    ref.getMetaData().put("property", property);
                    ref.getMetaData().put("type", concrete);
                    this.appendPrefixes(tokens, pc.getPrefix());
                    tokens.add(ref);
                    this.appendSuffixes(tokens, pc.getSuffix());
                    definition = new GrammarTokenComposite(minimumOccurence, maximumOccurence, tcc.getLogic() == BasicLogic.Random ? OR_LOGIC : AND_LOGIC, tokens.toArray(new IGrammarPartToken[tokens.size()]));
                } else {
                    definition = getReference(concrete, tcc, minimumOccurence, maximumOccurence);
                    definition.getMetaData().put("property", property);
                    definition.getMetaData().put("type", type);
                }
            }
            properties.put(id, definition);
        }
        return properties.get(id);
    }

    public GrammarTokenReference getReference(Class<?> pojoClass) {
        TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(pojoClass, pojoClass);
        return getReference(pojoClass, tcc, 1, 0);
    }

    public GrammarTokenReference getReference(Class<?> pojoClass, int minimumOccurence, int maximumOccurence) {
        return getReference(pojoClass, TokenCompositeConfiguration.getFor(pojoClass, pojoClass), minimumOccurence, maximumOccurence);
    }

    public IGrammarTokenDefinition getDefinition(Class<?> pojoClass) {
        TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(pojoClass, pojoClass);
        return getDefinition(pojoClass, tcc);
    }

    private IGrammarTokenDefinition getDefinition(Class<?> concrete, TokenCompositeConfiguration tcc) {
        if (!classes.containsKey(concrete)) {
            if (!classCreating.contains(concrete)) {
                classCreating.add(concrete);
                try {
                    ClassDescriptor<?> desc = ClassCache.getFor(concrete);
                    if (tcc.getConcrete().length > 1) {
                        IGrammarPartToken[] choices = new IGrammarPartToken[tcc.getConcrete().length];
                        for (int i = 0; i < tcc.getConcrete().length; i++) {
                            choices[i] = getReference(tcc.getConcrete()[i], 1, 1);
                        }
                        IGrammarTokenDefinition definition = null;
                        if (tcc.getPrefixes().length > 0 || tcc.getSuffixes().length > 0) {
                            List<IGrammarPartToken> patterns = new ArrayList<IGrammarPartToken>();
                            patterns.add(new GrammarTokenComposite(1, 1, OR_LOGIC, choices));
                            appendPrefixes(patterns, tcc.getPrefixes());
                            appendSuffixes(patterns, tcc.getSuffixes());
                            definition = new GrammarTokenDefinition(tcc.getName(), tcc.isFragment(), AND_LOGIC, patterns.toArray(new IGrammarPartToken[patterns.size()]));
                        } else {
                            definition = new GrammarTokenDefinition(tcc.getName(), tcc.isFragment(), OR_LOGIC, choices);
                        }
                        classes.put(concrete, definition);
                    } else {
                        List<IGrammarPartToken> tokens = new ArrayList<IGrammarPartToken>();
                        List<ClassProperty> properties = new ArrayList<ClassProperty>(desc.getProperties().values());
                        Collections.sort(properties, new Comparator<ClassProperty>() {

                            public int compare(ClassProperty o1, ClassProperty o2) {
                                Token t1 = o1.getAnnotation(Token.class);
                                Token t2 = o2.getAnnotation(Token.class);
                                if (t1 == null && t1 == t2) return 0;
                                if (t2 == null && t1.order() > -1) return 1;
                                if (t1 == null && t2.order() > -1) return -1;
                                return t1.order() == t2.order() ? 0 : (t1.order() > t2.order() ? 1 : -1);
                            }
                        });
                        for (ClassProperty property : properties) {
                            if (!Collection.class.isAssignableFrom(property.getBaseType()) && !property.isWritable()) continue;
                            if (property.getAnnotation(TokenIgnore.class) != null) continue;
                            if (property.getAnnotation(Comments.class) != null) continue;
                            if (property.getAnnotation(TokenMatch.class) != null) continue;
                            tokens.add(createFor(property));
                        }
                        appendPrefixes(tokens, tcc.getTokenConfig().getPrefix());
                        appendPrefixes(tokens, tcc.getPrefixes());
                        appendSuffixes(tokens, tcc.getTokenConfig().getSuffix());
                        appendSuffixes(tokens, tcc.getSuffixes());
                        classes.put(concrete, new GrammarTokenDefinition(tcc.getName(), tcc.isFragment(), tcc.getLogic() == BasicLogic.Sequential ? AND_LOGIC : OR_LOGIC, tokens.toArray(new IGrammarPartToken[tokens.size()])));
                    }
                    definitions.put(tcc.getName(), classes.get(concrete));
                    classes.get(concrete).getMetaData().put("class", desc);
                } finally {
                    classCreating.remove(concrete);
                }
            }
        }
        return classes.get(concrete);
    }

    public GrammarTokenReference getReference(Class<?> concrete, TokenCompositeConfiguration tcc, int minimumOccurence, int maximumOccurence) {
        if (this.getDefinition(concrete) == null && !this.classCreating.contains(concrete)) throw new IllegalArgumentException("No such definition for: " + concrete);
        return new GrammarTokenReference(minimumOccurence, maximumOccurence, tcc.getName());
    }

    public IGrammarPartToken getSingle(TokenConfiguration tc, int minimumOccurence, int maximumOccurence) {
        if (tc.getPrefix().length == 0 && tc.getSuffix().length == 0) {
            return tc.getPattern() != null ? new GrammarTokenPattern(minimumOccurence, maximumOccurence, tc.getTypes(), Pattern.compile(tc.getPattern())) : new GrammarTokenType(minimumOccurence, maximumOccurence, tc.getTypes());
        } else {
            List<IGrammarPartToken> tokens = new ArrayList<IGrammarPartToken>();
            appendPrefixes(tokens, tc.getPrefix());
            tokens.add(tc.getPattern() != null ? new GrammarTokenPattern(minimumOccurence, maximumOccurence, tc.getTypes(), Pattern.compile(tc.getPattern())) : new GrammarTokenType(minimumOccurence, maximumOccurence, tc.getTypes()));
            appendSuffixes(tokens, tc.getSuffix());
            return new GrammarTokenComposite(minimumOccurence, maximumOccurence, AND_LOGIC, tokens.toArray(new IGrammarPartToken[tokens.size()]));
        }
    }

    public void appendPrefixes(List<IGrammarPartToken> tokens, String[] prefixes) {
        for (int i = prefixes.length - 1; i >= 0; i--) {
            tokens.add(0, this.createPattern(prefixes[i], -1));
        }
    }

    public void appendSuffixes(List<IGrammarPartToken> tokens, String[] suffixes) {
        for (int i = 0; i < suffixes.length; i++) {
            tokens.add(this.createPattern(suffixes[i], i + 1000));
        }
    }

    public GrammarTokenLiteral createPattern(String pattern, int order) {
        int minimum = 1;
        int maximum = 1;
        if (pattern.endsWith("?")) minimum = 0; else if (pattern.endsWith("*")) {
            maximum = 0;
            maximum = 0;
        } else if (pattern.endsWith("+")) {
            maximum = 0;
        }
        if (minimum != 1 || maximum != 1) pattern = pattern.substring(0, pattern.length() - 1);
        GrammarTokenLiteral exp = new GrammarTokenLiteral(minimum, maximum, new String[0], pattern);
        exp.getMetaData().put("transient", true);
        return exp;
    }
}
