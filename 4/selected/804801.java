package net.sourceforge.javautil.grammar.pojo;

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
import org.omg.CORBA.OMGVMCID;
import net.sourceforge.javautil.common.ReflectionUtil;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;
import net.sourceforge.javautil.common.reflection.cache.ClassConstructor;
import net.sourceforge.javautil.common.reflection.cache.ClassDescriptor;
import net.sourceforge.javautil.common.reflection.cache.ClassProperty;
import net.sourceforge.javautil.grammar.impl.ast.IASTDefinition;
import net.sourceforge.javautil.grammar.impl.ast.IASTPattern;
import net.sourceforge.javautil.grammar.impl.ast.IASTPatternCompositeLogic;
import net.sourceforge.javautil.grammar.impl.ast.IASTPatternReference;
import net.sourceforge.javautil.grammar.impl.ast.IASTDefinitionResolver;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTDefinition;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTDefinitionAbstract;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTPatternComposite;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTPatternExpression;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTPatternReference;
import net.sourceforge.javautil.grammar.impl.ast.patterns.ASTPatternType;
import net.sourceforge.javautil.grammar.impl.ast.state.ASTPatternCompositeLogicAnd;
import net.sourceforge.javautil.grammar.impl.ast.state.ASTPatternCompositeLogicOr;
import net.sourceforge.javautil.grammar.impl.ast.state.ASTPatternCompositeLogicSeparator;
import net.sourceforge.javautil.grammar.lexer.GrammarComment;
import net.sourceforge.javautil.grammar.lexer.GrammarToken;
import net.sourceforge.javautil.grammar.lexer.GrammarTokenType;
import net.sourceforge.javautil.grammar.pojo.TokenCompositeConfiguration.CompositeLogic;
import net.sourceforge.javautil.grammar.pojo.annotation.Comments;
import net.sourceforge.javautil.grammar.pojo.annotation.TokenIgnore;
import net.sourceforge.javautil.grammar.pojo.annotation.TokenMatch;

/**
 * A factory that can generate {@link IGrammarTokenDefinition} from annotated POJO's.
 *  
 * @author elponderador
 * @author $Author$
 * @version $Id$
 */
public class PojoDefinitionFactory implements IASTDefinitionResolver {

    public static final ASTPatternCompositeLogicAnd AND_LOGIC = new ASTPatternCompositeLogicAnd();

    public static final ASTPatternCompositeLogicOr OR_LOGIC = new ASTPatternCompositeLogicOr();

    public static final ASTPatternCompositeLogicSeparator SEPARATOR_LOGIC = new ASTPatternCompositeLogicSeparator();

    public static final GrammarTokenType[] NO_TYPES = new GrammarTokenType[0];

    public static final GrammarTokenType[] COMPOSITE_TYPES = new GrammarTokenType[] { GrammarTokenType.COMPOSITE };

    public static final GrammarTokenType[] STRING_TYPES = new GrammarTokenType[] { GrammarTokenType.STRING_LITERAL, GrammarTokenType.WHITESPACE };

    public static final GrammarTokenType[] PATTERN_TYPES = new GrammarTokenType[] { GrammarTokenType.STRING_LITERAL, GrammarTokenType.OPERATOR, GrammarTokenType.SEPARATOR, GrammarTokenType.WHITESPACE, GrammarTokenType.BLOCK_START, GrammarTokenType.BLOCK_END, GrammarTokenType.OPEN_COMPOSITE_EXPRESION, GrammarTokenType.CLOSE_COMPOSITE_EXPRESSION };

    public static final GrammarTokenType[] NUMBER_TYPES = new GrammarTokenType[] { GrammarTokenType.NUMBER };

    public static final GrammarTokenType[] BOOLEAN_TYPES = new GrammarTokenType[] { GrammarTokenType.STRING_LITERAL, GrammarTokenType.NUMBER };

    public static GrammarTokenType[] getDefaultTypes(Class<?> type) {
        if (type == String.class || type == Date.class) return STRING_TYPES; else if (type.isPrimitive() && Number.class.isAssignableFrom(ReflectionUtil.getBoxedType(type))) {
            return NUMBER_TYPES;
        } else if (type == boolean.class || type == Boolean.class) {
            return BOOLEAN_TYPES;
        }
        throw new IllegalArgumentException("Cannot detect default types for: " + type);
    }

    protected final Map<Class<?>, IASTDefinition> classes = new HashMap<Class<?>, IASTDefinition>();

    protected final Map<String, IASTDefinition> definitions = new HashMap<String, IASTDefinition>();

    protected final Map<String, IASTPattern> properties = new HashMap<String, IASTPattern>();

    protected final List<Class<?>> classCreating = new ArrayList<Class<?>>();

    public IASTDefinition resolve(String name) {
        return definitions.get(name);
    }

    public IASTPattern createFor(ClassProperty property) {
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
            IASTPattern definition = null;
            if (concrete.isPrimitive() || ReflectionUtil.isBoxedType(concrete) || String.class == concrete || Date.class == concrete || Object.class == concrete || concrete.isEnum()) {
                definition = getSingle(tc, minimumOccurence, maximumOccurence);
                definition.getMetaData().put("property", property);
                definition.getMetaData().put("type", type);
            } else {
                TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(concrete, concrete, tc, CompositeLogic.And);
                definition = getReference(concrete, tcc, minimumOccurence, maximumOccurence);
                definition.getMetaData().put("property", property);
                definition.getMetaData().put("type", type);
                if ((maximumOccurence == 0 || maximumOccurence > 1) && tc.getSeparator() != null) {
                    definition = new ASTPatternComposite(0, 1, 1, SEPARATOR_LOGIC, createPattern(tc.getSeparator(), 0), definition);
                }
            }
            properties.put(id, definition);
        }
        return properties.get(id);
    }

    public IASTPatternReference getReference(Class<?> pojoClass) {
        TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(pojoClass, pojoClass);
        return getReference(pojoClass, tcc, 1, 0);
    }

    public IASTPatternReference getReference(Class<?> pojoClass, int minimumOccurence, int maximumOccurence) {
        return getReference(pojoClass, TokenCompositeConfiguration.getFor(pojoClass, pojoClass), minimumOccurence, maximumOccurence);
    }

    public IASTDefinition getDefinition(Class<?> pojoClass) {
        TokenCompositeConfiguration tcc = TokenCompositeConfiguration.getFor(pojoClass, pojoClass);
        return getDefinition(pojoClass, tcc);
    }

    private IASTDefinition getDefinition(Class<?> concrete, TokenCompositeConfiguration tcc) {
        if (!classes.containsKey(concrete)) {
            if (!classCreating.contains(concrete)) {
                classCreating.add(concrete);
                try {
                    ClassDescriptor<?> desc = ClassCache.getFor(concrete);
                    if (tcc.getConcrete().length > 1) {
                        IASTPattern[] choices = new IASTPattern[tcc.getConcrete().length];
                        for (int i = 0; i < tcc.getConcrete().length; i++) {
                            choices[i] = getReference(tcc.getConcrete()[i], 1, 1);
                        }
                        ASTDefinition definition = null;
                        if (tcc.getPrefixes().length > 0 || tcc.getSuffixes().length > 0) {
                            List<IASTPattern> patterns = new ArrayList<IASTPattern>();
                            patterns.add(new ASTPatternComposite(0, 1, 1, OR_LOGIC, choices));
                            appendPrefixes(patterns, tcc.getPrefixes());
                            appendSuffixes(patterns, tcc.getSuffixes());
                            definition = new ASTDefinition(tcc.getName(), tcc.isFragment(), AND_LOGIC, patterns.toArray(new IASTPattern[patterns.size()]));
                        } else {
                            definition = new ASTDefinition(tcc.getName(), tcc.isFragment(), OR_LOGIC, choices);
                        }
                        classes.put(concrete, definition);
                    } else {
                        List<IASTPattern> tokens = new ArrayList<IASTPattern>();
                        for (String name : desc.getPropertyNames()) {
                            ClassProperty property = desc.getProperty(name);
                            if (!Collection.class.isAssignableFrom(property.getBaseType()) && !property.isWritable()) continue;
                            if (property.getAnnotation(TokenIgnore.class) != null) continue;
                            if (property.getAnnotation(Comments.class) != null) continue;
                            if (property.getAnnotation(TokenMatch.class) != null) continue;
                            tokens.add(createFor(property));
                        }
                        Collections.sort(tokens, new Comparator<IASTPattern>() {

                            public int compare(IASTPattern o1, IASTPattern o2) {
                                if (o1.getOrder() == o2.getOrder()) return 0;
                                return o1.getOrder() > o2.getOrder() ? 1 : -1;
                            }
                        });
                        appendPrefixes(tokens, tcc.getTokenConfig().getPrefix());
                        appendPrefixes(tokens, tcc.getPrefixes());
                        appendSuffixes(tokens, tcc.getTokenConfig().getSuffix());
                        appendSuffixes(tokens, tcc.getSuffixes());
                        classes.put(concrete, new ASTDefinition(tcc.getName(), tcc.isFragment(), tcc.getLogic() == CompositeLogic.And ? AND_LOGIC : OR_LOGIC, tokens.toArray(new IASTPattern[tokens.size()])));
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

    public IASTPatternReference getReference(Class<?> concrete, TokenCompositeConfiguration tcc, int minimumOccurence, int maximumOccurence) {
        if (this.getDefinition(concrete) == null && !this.classCreating.contains(concrete)) throw new IllegalArgumentException("No such definition for: " + concrete);
        return new ASTPatternReference(tcc.getName(), tcc.getTokenConfig().getOrder(), minimumOccurence, maximumOccurence);
    }

    public IASTPattern getSingle(TokenConfiguration tc, int minimumOccurence, int maximumOccurence) {
        if (tc.getPrefix().length == 0 && tc.getSuffix().length == 0) {
            return tc.getPattern() != null ? new ASTPatternExpression(tc.getOrder(), minimumOccurence, maximumOccurence, tc.getTypes(), Pattern.compile(tc.getPattern())) : new ASTPatternType(tc.getOrder(), minimumOccurence, maximumOccurence, tc.getTypes());
        } else {
            List<IASTPattern> tokens = new ArrayList<IASTPattern>();
            appendPrefixes(tokens, tc.getPrefix());
            tokens.add(tc.getPattern() != null ? new ASTPatternExpression(tc.getOrder(), minimumOccurence, maximumOccurence, tc.getTypes(), Pattern.compile(tc.getPattern())) : new ASTPatternType(tc.getOrder(), minimumOccurence, maximumOccurence, tc.getTypes()));
            appendSuffixes(tokens, tc.getSuffix());
            return new ASTPatternComposite(tc.getOrder(), minimumOccurence, maximumOccurence, AND_LOGIC, tokens.toArray(new IASTPattern[tokens.size()]));
        }
    }

    public void appendPrefixes(List<IASTPattern> tokens, String[] prefixes) {
        for (int i = prefixes.length - 1; i >= 0; i--) {
            tokens.add(0, this.createPattern(prefixes[i], -1));
        }
    }

    public void appendSuffixes(List<IASTPattern> tokens, String[] suffixes) {
        for (int i = 0; i < suffixes.length; i++) {
            tokens.add(this.createPattern(suffixes[i], i + 1000));
        }
    }

    public ASTPatternExpression createPattern(String pattern, int order) {
        int minimum = 1;
        int maximum = 1;
        if (pattern.endsWith("?")) minimum = 0; else if (pattern.endsWith("*")) {
            maximum = 0;
            maximum = 0;
        } else if (pattern.endsWith("+")) {
            maximum = 0;
        }
        if (minimum != 1 || maximum != 1) pattern = pattern.substring(0, pattern.length() - 1);
        ASTPatternExpression exp = new ASTPatternExpression(order, minimum, maximum, PATTERN_TYPES, Pattern.compile(pattern));
        exp.getMetaData().put("transient", true);
        return exp;
    }
}
