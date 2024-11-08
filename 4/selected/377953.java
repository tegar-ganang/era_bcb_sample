package fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp;

import org.antlr.runtime3_3_0.*;
import java.util.HashMap;

@SuppressWarnings("unused")
public class Ocl4tstParser extends Ocl4tstANTLRParserBase {

    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUALITY_OPERATOR", "SIMPLE_NAME", "BOOLEAN_LITERAL", "NEQUALITY_OPERATOR", "NOT_OPERATOR", "WHITESPACE", "LINEBREAKS", "':'", "'('", "')'", "'let'", "','", "'in'" };

    public static final int EOF = -1;

    public static final int T__11 = 11;

    public static final int T__12 = 12;

    public static final int T__13 = 13;

    public static final int T__14 = 14;

    public static final int T__15 = 15;

    public static final int T__16 = 16;

    public static final int EQUALITY_OPERATOR = 4;

    public static final int SIMPLE_NAME = 5;

    public static final int BOOLEAN_LITERAL = 6;

    public static final int NEQUALITY_OPERATOR = 7;

    public static final int NOT_OPERATOR = 8;

    public static final int WHITESPACE = 9;

    public static final int LINEBREAKS = 10;

    public Ocl4tstParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public Ocl4tstParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
        this.state.ruleMemo = new HashMap[13 + 1];
    }

    public String[] getTokenNames() {
        return Ocl4tstParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "Ocl4tst.g";
    }

    private fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolverFactory tokenResolverFactory = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTokenResolverFactory();

    /**
    	 * the index of the last token that was handled by collectHiddenTokens()
    	 */
    private int lastPosition;

    /**
    	 * A flag that indicates whether the parser should remember all expected elements.
    	 * This flag is set to true when using the parse for code completion. Otherwise it
    	 * is set to false.
    	 */
    private boolean rememberExpectedElements = false;

    private Object parseToIndexTypeObject;

    private int lastTokenIndex = 0;

    /**
    	 * A list of expected elements the were collected while parsing the input stream.
    	 * This list is only filled if <code>rememberExpectedElements</code> is set to
    	 * true.
    	 */
    private java.util.List<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal> expectedElements = new java.util.ArrayList<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal>();

    private int mismatchedTokenRecoveryTries = 0;

    /**
    	 * A helper list to allow a lexer to pass errors to its parser
    	 */
    protected java.util.List<org.antlr.runtime3_3_0.RecognitionException> lexerExceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<org.antlr.runtime3_3_0.RecognitionException>());

    /**
    	 * Another helper list to allow a lexer to pass positions of errors to its parser
    	 */
    protected java.util.List<Integer> lexerExceptionsPosition = java.util.Collections.synchronizedList(new java.util.ArrayList<Integer>());

    /**
    	 * A stack for incomplete objects. This stack is used filled when the parser is
    	 * used for code completion. Whenever the parser starts to read an object it is
    	 * pushed on the stack. Once the element was parser completely it is popped from
    	 * the stack.
    	 */
    protected java.util.Stack<org.eclipse.emf.ecore.EObject> incompleteObjects = new java.util.Stack<org.eclipse.emf.ecore.EObject>();

    private int stopIncludingHiddenTokens;

    private int stopExcludingHiddenTokens;

    private int tokenIndexOfLastCompleteElement;

    private int expectedElementsIndexOfLastCompleteElement;

    /**
    	 * The offset indicating the cursor position when the parser is used for code
    	 * completion by calling parseToExpectedElements().
    	 */
    private int cursorOffset;

    /**
    	 * The offset of the first hidden token of the last expected element. This offset
    	 * is used to discard expected elements, which are not needed for code completion.
    	 */
    private int lastStartIncludingHidden;

    protected void addErrorToResource(final String errorMessage, final int column, final int line, final int startIndex, final int stopIndex) {
        postParseCommands.add(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>() {

            public boolean execute(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.addProblem(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstProblem() {

                    public fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.Ocl4tstEProblemSeverity getSeverity() {
                        return fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.Ocl4tstEProblemSeverity.ERROR;
                    }

                    public fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.Ocl4tstEProblemType getType() {
                        return fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.Ocl4tstEProblemType.SYNTAX_ERROR;
                    }

                    public String getMessage() {
                        return errorMessage;
                    }

                    public java.util.Collection<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstQuickFix> getQuickFixes() {
                        return null;
                    }
                }, column, line, startIndex, stopIndex);
                return true;
            }
        });
    }

    public void addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstExpectedElement terminal, int followSetID, org.eclipse.emf.ecore.EStructuralFeature... containmentTrace) {
        if (!this.rememberExpectedElements) {
            return;
        }
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal expectedElement = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal(terminal, followSetID, containmentTrace);
        setPosition(expectedElement, input.index());
        int startIncludingHiddenTokens = expectedElement.getStartIncludingHiddenTokens();
        if (lastStartIncludingHidden >= 0 && lastStartIncludingHidden < startIncludingHiddenTokens && cursorOffset > startIncludingHiddenTokens) {
            this.expectedElements.clear();
        }
        lastStartIncludingHidden = startIncludingHiddenTokens;
        this.expectedElements.add(expectedElement);
    }

    protected void collectHiddenTokens(org.eclipse.emf.ecore.EObject element) {
    }

    protected void copyLocalizationInfos(final org.eclipse.emf.ecore.EObject source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>() {

            public boolean execute(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource resource) {
                fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstLocationMap locationMap = resource.getLocationMap();
                if (locationMap == null) {
                    return true;
                }
                locationMap.setCharStart(target, locationMap.getCharStart(source));
                locationMap.setCharEnd(target, locationMap.getCharEnd(source));
                locationMap.setColumn(target, locationMap.getColumn(source));
                locationMap.setLine(target, locationMap.getLine(source));
                return true;
            }
        });
    }

    protected void copyLocalizationInfos(final org.antlr.runtime3_3_0.CommonToken source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>() {

            public boolean execute(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource resource) {
                fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstLocationMap locationMap = resource.getLocationMap();
                if (locationMap == null) {
                    return true;
                }
                if (source == null) {
                    return true;
                }
                locationMap.setCharStart(target, source.getStartIndex());
                locationMap.setCharEnd(target, source.getStopIndex());
                locationMap.setColumn(target, source.getCharPositionInLine());
                locationMap.setLine(target, source.getLine());
                return true;
            }
        });
    }

    /**
    	 * Sets the end character index and the last line for the given object in the
    	 * location map.
    	 */
    protected void setLocalizationEnd(java.util.Collection<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>> postParseCommands, final org.eclipse.emf.ecore.EObject object, final int endChar, final int endLine) {
        postParseCommands.add(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>() {

            public boolean execute(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource resource) {
                fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstLocationMap locationMap = resource.getLocationMap();
                if (locationMap == null) {
                    return true;
                }
                locationMap.setCharEnd(object, endChar);
                locationMap.setLine(object, endLine);
                return true;
            }
        });
    }

    public fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextParser createInstance(java.io.InputStream actualInputStream, String encoding) {
        try {
            if (encoding == null) {
                return new Ocl4tstParser(new org.antlr.runtime3_3_0.CommonTokenStream(new Ocl4tstLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream))));
            } else {
                return new Ocl4tstParser(new org.antlr.runtime3_3_0.CommonTokenStream(new Ocl4tstLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream, encoding))));
            }
        } catch (java.io.IOException e) {
            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstPlugin.logError("Error while creating parser.", e);
            return null;
        }
    }

    /**
    	 * This default constructor is only used to call createInstance() on it.
    	 */
    public Ocl4tstParser() {
        super(null);
    }

    protected org.eclipse.emf.ecore.EObject doParse() throws org.antlr.runtime3_3_0.RecognitionException {
        this.lastPosition = 0;
        ((Ocl4tstLexer) getTokenStream().getTokenSource()).lexerExceptions = lexerExceptions;
        ((Ocl4tstLexer) getTokenStream().getTokenSource()).lexerExceptionsPosition = lexerExceptionsPosition;
        Object typeObject = getTypeObject();
        if (typeObject == null) {
            return start();
        } else if (typeObject instanceof org.eclipse.emf.ecore.EClass) {
            org.eclipse.emf.ecore.EClass type = (org.eclipse.emf.ecore.EClass) typeObject;
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclDeclaration.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration();
            }
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclarationWithInit.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit();
            }
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Type.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type();
            }
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.SimpleName.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName();
            }
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BooleanLiteralExp.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp();
            }
            if (type.getInstanceClass() == fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BracketExp.class) {
                return parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp();
            }
        }
        throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstUnexpectedContentTypeException(typeObject);
    }

    public int getMismatchedTokenRecoveryTries() {
        return mismatchedTokenRecoveryTries;
    }

    public Object getMissingSymbol(org.antlr.runtime3_3_0.IntStream arg0, org.antlr.runtime3_3_0.RecognitionException arg1, int arg2, org.antlr.runtime3_3_0.BitSet arg3) {
        mismatchedTokenRecoveryTries++;
        return super.getMissingSymbol(arg0, arg1, arg2, arg3);
    }

    public Object getParseToIndexTypeObject() {
        return parseToIndexTypeObject;
    }

    protected Object getTypeObject() {
        Object typeObject = getParseToIndexTypeObject();
        if (typeObject != null) {
            return typeObject;
        }
        java.util.Map<?, ?> options = getOptions();
        if (options != null) {
            typeObject = options.get(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstOptions.RESOURCE_CONTENT_TYPE);
        }
        return typeObject;
    }

    /**
    	 * Implementation that calls {@link #doParse()} and handles the thrown
    	 * RecognitionExceptions.
    	 */
    public fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstParseResult parse() {
        terminateParsing = false;
        postParseCommands = new java.util.ArrayList<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>>();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstParseResult parseResult = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstParseResult();
        try {
            org.eclipse.emf.ecore.EObject result = doParse();
            if (lexerExceptions.isEmpty()) {
                parseResult.setRoot(result);
            }
        } catch (org.antlr.runtime3_3_0.RecognitionException re) {
            reportError(re);
        } catch (java.lang.IllegalArgumentException iae) {
            if ("The 'no null' constraint is violated".equals(iae.getMessage())) {
            } else {
                iae.printStackTrace();
            }
        }
        for (org.antlr.runtime3_3_0.RecognitionException re : lexerExceptions) {
            reportLexicalError(re);
        }
        parseResult.getPostParseCommands().addAll(postParseCommands);
        return parseResult;
    }

    public java.util.List<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal> parseToExpectedElements(org.eclipse.emf.ecore.EClass type, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource dummyResource, int cursorOffset) {
        this.rememberExpectedElements = true;
        this.parseToIndexTypeObject = type;
        this.cursorOffset = cursorOffset;
        this.lastStartIncludingHidden = -1;
        final org.antlr.runtime3_3_0.CommonTokenStream tokenStream = (org.antlr.runtime3_3_0.CommonTokenStream) getTokenStream();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstParseResult result = parse();
        for (org.eclipse.emf.ecore.EObject incompleteObject : incompleteObjects) {
            org.antlr.runtime3_3_0.Lexer lexer = (org.antlr.runtime3_3_0.Lexer) tokenStream.getTokenSource();
            int endChar = lexer.getCharIndex();
            int endLine = lexer.getLine();
            setLocalizationEnd(result.getPostParseCommands(), incompleteObject, endChar, endLine);
        }
        if (result != null) {
            org.eclipse.emf.ecore.EObject root = result.getRoot();
            if (root != null) {
                dummyResource.getContentsInternal().add(root);
            }
            for (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource> command : result.getPostParseCommands()) {
                command.execute(dummyResource);
            }
        }
        expectedElements = expectedElements.subList(0, expectedElementsIndexOfLastCompleteElement + 1);
        int lastFollowSetID = expectedElements.get(expectedElementsIndexOfLastCompleteElement).getFollowSetID();
        java.util.Set<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal> currentFollowSet = new java.util.LinkedHashSet<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal>();
        java.util.List<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal> newFollowSet = new java.util.ArrayList<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal>();
        for (int i = expectedElementsIndexOfLastCompleteElement; i >= 0; i--) {
            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal expectedElementI = expectedElements.get(i);
            if (expectedElementI.getFollowSetID() == lastFollowSetID) {
                currentFollowSet.add(expectedElementI);
            } else {
                break;
            }
        }
        int followSetID = 21;
        int i;
        for (i = tokenIndexOfLastCompleteElement; i < tokenStream.size(); i++) {
            org.antlr.runtime3_3_0.CommonToken nextToken = (org.antlr.runtime3_3_0.CommonToken) tokenStream.get(i);
            if (nextToken.getType() < 0) {
                break;
            }
            if (nextToken.getChannel() == 99) {
            } else {
                for (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal nextFollow : newFollowSet) {
                    lastTokenIndex = 0;
                    setPosition(nextFollow, i);
                }
                newFollowSet.clear();
                for (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal nextFollow : currentFollowSet) {
                    if (nextFollow.getTerminal().getTokenNames().contains(getTokenNames()[nextToken.getType()])) {
                        java.util.Collection<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.util.Ocl4tstPair<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]>> newFollowers = nextFollow.getTerminal().getFollowers();
                        for (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.util.Ocl4tstPair<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]> newFollowerPair : newFollowers) {
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstExpectedElement newFollower = newFollowerPair.getLeft();
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal newFollowTerminal = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal(newFollower, followSetID, newFollowerPair.getRight());
                            newFollowSet.add(newFollowTerminal);
                            expectedElements.add(newFollowTerminal);
                        }
                    }
                }
                currentFollowSet.clear();
                currentFollowSet.addAll(newFollowSet);
            }
            followSetID++;
        }
        for (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal nextFollow : newFollowSet) {
            lastTokenIndex = 0;
            setPosition(nextFollow, i);
        }
        return this.expectedElements;
    }

    public void setPosition(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstExpectedTerminal expectedElement, int tokenIndex) {
        int currentIndex = Math.max(0, tokenIndex);
        for (int index = lastTokenIndex; index < currentIndex; index++) {
            if (index >= input.size()) {
                break;
            }
            org.antlr.runtime3_3_0.CommonToken tokenAtIndex = (org.antlr.runtime3_3_0.CommonToken) input.get(index);
            stopIncludingHiddenTokens = tokenAtIndex.getStopIndex() + 1;
            if (tokenAtIndex.getChannel() != 99) {
                stopExcludingHiddenTokens = tokenAtIndex.getStopIndex() + 1;
            }
        }
        lastTokenIndex = Math.max(0, currentIndex);
        expectedElement.setPosition(stopExcludingHiddenTokens, stopIncludingHiddenTokens);
    }

    public Object recoverFromMismatchedToken(org.antlr.runtime3_3_0.IntStream input, int ttype, org.antlr.runtime3_3_0.BitSet follow) throws org.antlr.runtime3_3_0.RecognitionException {
        if (!rememberExpectedElements) {
            return super.recoverFromMismatchedToken(input, ttype, follow);
        } else {
            return null;
        }
    }

    /**
    	 * Translates errors thrown by the parser into human readable messages.
    	 */
    public void reportError(final org.antlr.runtime3_3_0.RecognitionException e) {
        String message = e.getMessage();
        if (e instanceof org.antlr.runtime3_3_0.MismatchedTokenException) {
            org.antlr.runtime3_3_0.MismatchedTokenException mte = (org.antlr.runtime3_3_0.MismatchedTokenException) e;
            String expectedTokenName = formatTokenName(mte.expecting);
            String actualTokenName = formatTokenName(e.token.getType());
            message = "Syntax error on token \"" + e.token.getText() + " (" + actualTokenName + ")\", \"" + expectedTokenName + "\" expected";
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedTreeNodeException) {
            org.antlr.runtime3_3_0.MismatchedTreeNodeException mtne = (org.antlr.runtime3_3_0.MismatchedTreeNodeException) e;
            String expectedTokenName = formatTokenName(mtne.expecting);
            message = "mismatched tree node: " + "xxx" + "; tokenName " + expectedTokenName;
        } else if (e instanceof org.antlr.runtime3_3_0.NoViableAltException) {
            message = "Syntax error on token \"" + e.token.getText() + "\", check following tokens";
        } else if (e instanceof org.antlr.runtime3_3_0.EarlyExitException) {
            message = "Syntax error on token \"" + e.token.getText() + "\", delete this token";
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedSetException) {
            org.antlr.runtime3_3_0.MismatchedSetException mse = (org.antlr.runtime3_3_0.MismatchedSetException) e;
            message = "mismatched token: " + e.token + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedNotSetException) {
            org.antlr.runtime3_3_0.MismatchedNotSetException mse = (org.antlr.runtime3_3_0.MismatchedNotSetException) e;
            message = "mismatched token: " + e.token + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime3_3_0.FailedPredicateException) {
            org.antlr.runtime3_3_0.FailedPredicateException fpe = (org.antlr.runtime3_3_0.FailedPredicateException) e;
            message = "rule " + fpe.ruleName + " failed predicate: {" + fpe.predicateText + "}?";
        }
        final String finalMessage = message;
        if (e.token instanceof org.antlr.runtime3_3_0.CommonToken) {
            final org.antlr.runtime3_3_0.CommonToken ct = (org.antlr.runtime3_3_0.CommonToken) e.token;
            addErrorToResource(finalMessage, ct.getCharPositionInLine(), ct.getLine(), ct.getStartIndex(), ct.getStopIndex());
        } else {
            addErrorToResource(finalMessage, e.token.getCharPositionInLine(), e.token.getLine(), 1, 5);
        }
    }

    /**
    	 * Translates errors thrown by the lexer into human readable messages.
    	 */
    public void reportLexicalError(final org.antlr.runtime3_3_0.RecognitionException e) {
        String message = "";
        if (e instanceof org.antlr.runtime3_3_0.MismatchedTokenException) {
            org.antlr.runtime3_3_0.MismatchedTokenException mte = (org.antlr.runtime3_3_0.MismatchedTokenException) e;
            message = "Syntax error on token \"" + ((char) e.c) + "\", \"" + (char) mte.expecting + "\" expected";
        } else if (e instanceof org.antlr.runtime3_3_0.NoViableAltException) {
            message = "Syntax error on token \"" + ((char) e.c) + "\", delete this token";
        } else if (e instanceof org.antlr.runtime3_3_0.EarlyExitException) {
            org.antlr.runtime3_3_0.EarlyExitException eee = (org.antlr.runtime3_3_0.EarlyExitException) e;
            message = "required (...)+ loop (decision=" + eee.decisionNumber + ") did not match anything; on line " + e.line + ":" + e.charPositionInLine + " char=" + ((char) e.c) + "'";
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedSetException) {
            org.antlr.runtime3_3_0.MismatchedSetException mse = (org.antlr.runtime3_3_0.MismatchedSetException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedNotSetException) {
            org.antlr.runtime3_3_0.MismatchedNotSetException mse = (org.antlr.runtime3_3_0.MismatchedNotSetException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime3_3_0.MismatchedRangeException) {
            org.antlr.runtime3_3_0.MismatchedRangeException mre = (org.antlr.runtime3_3_0.MismatchedRangeException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set '" + (char) mre.a + "'..'" + (char) mre.b + "'";
        } else if (e instanceof org.antlr.runtime3_3_0.FailedPredicateException) {
            org.antlr.runtime3_3_0.FailedPredicateException fpe = (org.antlr.runtime3_3_0.FailedPredicateException) e;
            message = "rule " + fpe.ruleName + " failed predicate: {" + fpe.predicateText + "}?";
        }
        addErrorToResource(message, e.charPositionInLine, e.line, lexerExceptionsPosition.get(lexerExceptions.indexOf(e)), lexerExceptionsPosition.get(lexerExceptions.indexOf(e)));
    }

    protected void completedElement(Object object, boolean isContainment) {
        if (isContainment && !this.incompleteObjects.isEmpty()) {
            this.incompleteObjects.pop();
        }
        if (object instanceof org.eclipse.emf.ecore.EObject) {
            this.tokenIndexOfLastCompleteElement = getTokenStream().index();
            this.expectedElementsIndexOfLastCompleteElement = expectedElements.size() - 1;
        }
    }

    public final org.eclipse.emf.ecore.EObject start() throws RecognitionException {
        org.eclipse.emf.ecore.EObject element = null;
        int start_StartIndex = input.index();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclDeclaration c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 1)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_0, 0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_1, 0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_2, 0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0);
                    expectedElementsIndexOfLastCompleteElement = 0;
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_in_start82);
                    c0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        element = c0;
                    }
                }
                match(input, EOF, FOLLOW_EOF_in_start89);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    retrieveLayoutInformation(element, null, null, false);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 1, start_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclDeclaration parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclDeclaration element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_StartIndex = input.index();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 2)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration119);
                    a0_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createOclDeclaration();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.OCL_DECLARATION__EXPRESSION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_0_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 1);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 2, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclarationWithInit parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclarationWithInit element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit_StartIndex = input.index();
        Token a1 = null;
        Token a3 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.SimpleName a0_0 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Type a2_0 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclDeclaration a4_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 3)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit156);
                    a0_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.VARIABLE_DECLARATION_WITH_INIT__VARIABLE_NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_2_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_5, 2);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_6, 2);
                }
                int alt1 = 2;
                int LA1_0 = input.LA(1);
                if ((LA1_0 == 11)) {
                    alt1 = 1;
                }
                switch(alt1) {
                    case 1:
                        {
                            {
                                a1 = (Token) match(input, 11, FOLLOW_11_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit183);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (element == null) {
                                        element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createVariableDeclarationWithInit();
                                        incompleteObjects.push(element);
                                    }
                                    collectHiddenTokens(element);
                                    retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_2_0_0_1_0_0_0, null, true);
                                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a1, element);
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_7, 3, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_1);
                                }
                                {
                                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit209);
                                    a2_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type();
                                    state._fsp--;
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (terminateParsing) {
                                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                                        }
                                        if (element == null) {
                                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createVariableDeclarationWithInit();
                                            incompleteObjects.push(element);
                                        }
                                        if (a2_0 != null) {
                                            if (a2_0 != null) {
                                                Object value = a2_0;
                                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.VARIABLE_DECLARATION_WITH_INIT__TYPE), value);
                                                completedElement(value, true);
                                            }
                                            collectHiddenTokens(element);
                                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_2_0_0_1_0_0_1, a2_0, true);
                                            copyLocalizationInfos(a2_0, element);
                                        }
                                    }
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_6, 4);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_6, 5);
                }
                {
                    a3 = (Token) match(input, EQUALITY_OPERATOR, FOLLOW_EQUALITY_OPERATOR_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit254);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a3 != null) {
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("EQUALITY_OPERATOR");
                            tokenResolver.setOptions(getOptions());
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a3.getText(), element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.VARIABLE_DECLARATION_WITH_INIT__EQUAL), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a3).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a3).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a3).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a3).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.VARIABLE_DECLARATION_WITH_INIT__EQUAL), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_2_0_0_2, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a3, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_0, 6, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_1, 6, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_2, 6, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_0, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_2);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit279);
                    a4_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a4_0 != null) {
                            if (a4_0 != null) {
                                Object value = a4_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.VARIABLE_DECLARATION_WITH_INIT__INITIALIZATION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_2_0_0_3, a4_0, true);
                            copyLocalizationInfos(a4_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 7);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 7);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 3, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Type parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Type element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 4)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, SIMPLE_NAME, FOLLOW_SIMPLE_NAME_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type316);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createType();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("SIMPLE_NAME");
                            tokenResolver.setOptions(getOptions());
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.TYPE__NAME), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.TYPE__NAME), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_3_0_0_0, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_6, 8);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 4, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.SimpleName parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.SimpleName element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 5)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, SIMPLE_NAME, FOLLOW_SIMPLE_NAME_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName356);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createSimpleName();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("SIMPLE_NAME");
                            tokenResolver.setOptions(getOptions());
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.SIMPLE_NAME__VALUE), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.SIMPLE_NAME__VALUE), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_4_0_0_0, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_5, 9);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_6, 9);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 5, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BooleanLiteralExp parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BooleanLiteralExp element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 6)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, BOOLEAN_LITERAL, FOLLOW_BOOLEAN_LITERAL_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp396);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createBooleanLiteralExp();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BOOLEAN_LITERAL");
                            tokenResolver.setOptions(getOptions());
                            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.BOOLEAN_LITERAL_EXP__VALUE), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            java.lang.Boolean resolved = (java.lang.Boolean) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.BOOLEAN_LITERAL_EXP__VALUE), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_5_0_0_0, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 10);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 10);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_9, 10);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 6, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BracketExp parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.BracketExp element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression a1_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 7)) {
                return element;
            }
            {
                a0 = (Token) match(input, 12, FOLLOW_12_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp432);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createBracketExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_6_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_0, 11, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_3);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_1, 11, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_3);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_2, 11, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_3);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp450);
                    a1_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createBracketExp();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.BRACKET_EXP__EXPRESSION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_6_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_9, 12);
                }
                a2 = (Token) match(input, 13, FOLLOW_13_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp468);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createBracketExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_6_0_0_2, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 13);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 13);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_9, 13);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 7, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression parseop_OclExpression_level_20() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression element = null;
        int parseop_OclExpression_level_20_StartIndex = input.index();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.LetExp c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 8)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp_in_parseop_OclExpression_level_20497);
                c0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp();
                state._fsp--;
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    element = c0;
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 8, parseop_OclExpression_level_20_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.LetExp parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.LetExp element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        Token a4 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclaration a1_0 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclaration a3_0 = null;
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression a5_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 9)) {
                return element;
            }
            {
                a0 = (Token) match(input, 14, FOLLOW_14_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp520);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_8, 14, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_4, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_5);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp538);
                    a1_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                addObjectToList(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.LET_EXP__VARIABLE_DECLARATIONS, value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 15);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 15);
                }
                loop2: do {
                    int alt2 = 2;
                    int LA2_0 = input.LA(1);
                    if ((LA2_0 == 15)) {
                        alt2 = 1;
                    }
                    switch(alt2) {
                        case 1:
                            {
                                {
                                    a2 = (Token) match(input, 15, FOLLOW_15_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp565);
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (element == null) {
                                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                                            incompleteObjects.push(element);
                                        }
                                        collectHiddenTokens(element);
                                        retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_2_0_0_1, null, true);
                                        copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_8, 16, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_4, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_5);
                                    }
                                    {
                                        pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp591);
                                        a3_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration();
                                        state._fsp--;
                                        if (state.failed) return element;
                                        if (state.backtracking == 0) {
                                            if (terminateParsing) {
                                                throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                                            }
                                            if (element == null) {
                                                element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                                                incompleteObjects.push(element);
                                            }
                                            if (a3_0 != null) {
                                                if (a3_0 != null) {
                                                    Object value = a3_0;
                                                    addObjectToList(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.LET_EXP__VARIABLE_DECLARATIONS, value);
                                                    completedElement(value, true);
                                                }
                                                collectHiddenTokens(element);
                                                retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_2_0_0_2, a3_0, true);
                                                copyLocalizationInfos(a3_0, element);
                                            }
                                        }
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 17);
                                        addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 17);
                                    }
                                }
                            }
                            break;
                        default:
                            break loop2;
                    }
                } while (true);
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 18);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 18);
                }
                a4 = (Token) match(input, 16, FOLLOW_16_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp632);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_3, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a4, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_0, 19, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_6);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_1, 19, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_6);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_2, 19, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.FEATURE_6);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp650);
                    a5_0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstFactory.eINSTANCE.createLetExp();
                            incompleteObjects.push(element);
                        }
                        if (a5_0 != null) {
                            if (a5_0 != null) {
                                Object value = a5_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.Ocl4tstPackage.LET_EXP__EXPRESSION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstGrammarInformationProvider.OCL4TST_1_0_0_5, a5_0, true);
                            copyLocalizationInfos(a5_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_3, 20);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_4, 20);
                    addExpectedElement(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstFollowSetProvider.TERMINAL_9, 20);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 9, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_StartIndex = input.index();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.OclExpression c = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 10)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parseop_OclExpression_level_20_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression679);
                c = parseop_OclExpression_level_20();
                state._fsp--;
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    element = c;
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 10, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclaration parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration() throws RecognitionException {
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclaration element = null;
        int parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_StartIndex = input.index();
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.VariableDeclarationWithInit c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 11)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration700);
                c0 = parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit();
                state._fsp--;
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    element = c0;
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 11, parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_StartIndex);
            }
        }
        return element;
    }

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_in_start82 = new BitSet(new long[] { 0x0000000000000000L });

    public static final BitSet FOLLOW_EOF_in_start89 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration119 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit156 = new BitSet(new long[] { 0x0000000000000810L });

    public static final BitSet FOLLOW_11_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit183 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit209 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_EQUALITY_OPERATOR_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit254 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit279 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_SIMPLE_NAME_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_Type316 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_SIMPLE_NAME_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_SimpleName356 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_BOOLEAN_LITERAL_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BooleanLiteralExp396 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_12_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp432 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp450 = new BitSet(new long[] { 0x0000000000002000L });

    public static final BitSet FOLLOW_13_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_BracketExp468 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp_in_parseop_OclExpression_level_20497 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_14_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp520 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp538 = new BitSet(new long[] { 0x0000000000018000L });

    public static final BitSet FOLLOW_15_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp565 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp591 = new BitSet(new long[] { 0x0000000000018000L });

    public static final BitSet FOLLOW_16_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp632 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_LetExp650 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parseop_OclExpression_level_20_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_OclExpression679 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclarationWithInit_in_parse_fr_inria_uml4tst_papyrus_ocl4tst_ocl4tst_VariableDeclaration700 = new BitSet(new long[] { 0x0000000000000002L });
}
