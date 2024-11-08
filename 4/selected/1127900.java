package org.emftext.language.OCL.resource.OCL.mopp;

import org.antlr.runtime3_3_0.*;
import java.util.HashMap;

@SuppressWarnings("unused")
public class OCLParser extends OCLANTLRParserBase {

    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "SIMPLE_NAME", "EQUALITY_OPERATOR", "SL_COMMENT", "ML_COMMENT", "NAVIGATION_OPERATOR", "ADDITIVE_OPERATOR", "MULT_OPERATOR", "RELATIONAL_OPERATOR", "NEQUALITY_OPERATOR", "NOT_OPERATOR", "AND_OPERATOR", "OR_OPERATOR", "XOR_OPERATOR", "IMPLIES_OPERATOR", "IS_MARKED_PRE", "BOOLEAN_LITERAL", "COLLECTION_TYPES", "ITERATOR_NAME", "STATIC", "INTEGER_0", "INTEGER_LITERAL", "WHITESPACE", "LINEBREAKS", "':'", "'let'", "','", "'in'", "'if'", "'then'", "'else'", "'endif'" };

    public static final int EOF = -1;

    public static final int T__27 = 27;

    public static final int T__28 = 28;

    public static final int T__29 = 29;

    public static final int T__30 = 30;

    public static final int T__31 = 31;

    public static final int T__32 = 32;

    public static final int T__33 = 33;

    public static final int T__34 = 34;

    public static final int SIMPLE_NAME = 4;

    public static final int EQUALITY_OPERATOR = 5;

    public static final int SL_COMMENT = 6;

    public static final int ML_COMMENT = 7;

    public static final int NAVIGATION_OPERATOR = 8;

    public static final int ADDITIVE_OPERATOR = 9;

    public static final int MULT_OPERATOR = 10;

    public static final int RELATIONAL_OPERATOR = 11;

    public static final int NEQUALITY_OPERATOR = 12;

    public static final int NOT_OPERATOR = 13;

    public static final int AND_OPERATOR = 14;

    public static final int OR_OPERATOR = 15;

    public static final int XOR_OPERATOR = 16;

    public static final int IMPLIES_OPERATOR = 17;

    public static final int IS_MARKED_PRE = 18;

    public static final int BOOLEAN_LITERAL = 19;

    public static final int COLLECTION_TYPES = 20;

    public static final int ITERATOR_NAME = 21;

    public static final int STATIC = 22;

    public static final int INTEGER_0 = 23;

    public static final int INTEGER_LITERAL = 24;

    public static final int WHITESPACE = 25;

    public static final int LINEBREAKS = 26;

    public OCLParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public OCLParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
        this.state.ruleMemo = new HashMap[14 + 1];
    }

    public String[] getTokenNames() {
        return OCLParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "OCL.g";
    }

    private org.emftext.language.OCL.resource.OCL.IOCLTokenResolverFactory tokenResolverFactory = new org.emftext.language.OCL.resource.OCL.mopp.OCLTokenResolverFactory();

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
    private java.util.List<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal> expectedElements = new java.util.ArrayList<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal>();

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
        postParseCommands.add(new org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>() {

            public boolean execute(org.emftext.language.OCL.resource.OCL.IOCLTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.addProblem(new org.emftext.language.OCL.resource.OCL.IOCLProblem() {

                    public org.emftext.language.OCL.resource.OCL.OCLEProblemSeverity getSeverity() {
                        return org.emftext.language.OCL.resource.OCL.OCLEProblemSeverity.ERROR;
                    }

                    public org.emftext.language.OCL.resource.OCL.OCLEProblemType getType() {
                        return org.emftext.language.OCL.resource.OCL.OCLEProblemType.SYNTAX_ERROR;
                    }

                    public String getMessage() {
                        return errorMessage;
                    }

                    public java.util.Collection<org.emftext.language.OCL.resource.OCL.IOCLQuickFix> getQuickFixes() {
                        return null;
                    }
                }, column, line, startIndex, stopIndex);
                return true;
            }
        });
    }

    public void addExpectedElement(org.emftext.language.OCL.resource.OCL.IOCLExpectedElement terminal, int followSetID, org.eclipse.emf.ecore.EStructuralFeature... containmentTrace) {
        if (!this.rememberExpectedElements) {
            return;
        }
        org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal expectedElement = new org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal(terminal, followSetID, containmentTrace);
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
        postParseCommands.add(new org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>() {

            public boolean execute(org.emftext.language.OCL.resource.OCL.IOCLTextResource resource) {
                org.emftext.language.OCL.resource.OCL.IOCLLocationMap locationMap = resource.getLocationMap();
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
        postParseCommands.add(new org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>() {

            public boolean execute(org.emftext.language.OCL.resource.OCL.IOCLTextResource resource) {
                org.emftext.language.OCL.resource.OCL.IOCLLocationMap locationMap = resource.getLocationMap();
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
    protected void setLocalizationEnd(java.util.Collection<org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>> postParseCommands, final org.eclipse.emf.ecore.EObject object, final int endChar, final int endLine) {
        postParseCommands.add(new org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>() {

            public boolean execute(org.emftext.language.OCL.resource.OCL.IOCLTextResource resource) {
                org.emftext.language.OCL.resource.OCL.IOCLLocationMap locationMap = resource.getLocationMap();
                if (locationMap == null) {
                    return true;
                }
                locationMap.setCharEnd(object, endChar);
                locationMap.setLine(object, endLine);
                return true;
            }
        });
    }

    public org.emftext.language.OCL.resource.OCL.IOCLTextParser createInstance(java.io.InputStream actualInputStream, String encoding) {
        try {
            if (encoding == null) {
                return new OCLParser(new org.antlr.runtime3_3_0.CommonTokenStream(new OCLLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream))));
            } else {
                return new OCLParser(new org.antlr.runtime3_3_0.CommonTokenStream(new OCLLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream, encoding))));
            }
        } catch (java.io.IOException e) {
            org.emftext.language.OCL.resource.OCL.mopp.OCLPlugin.logError("Error while creating parser.", e);
            return null;
        }
    }

    /**
    	 * This default constructor is only used to call createInstance() on it.
    	 */
    public OCLParser() {
        super(null);
    }

    protected org.eclipse.emf.ecore.EObject doParse() throws org.antlr.runtime3_3_0.RecognitionException {
        this.lastPosition = 0;
        ((OCLLexer) getTokenStream().getTokenSource()).lexerExceptions = lexerExceptions;
        ((OCLLexer) getTokenStream().getTokenSource()).lexerExceptionsPosition = lexerExceptionsPosition;
        Object typeObject = getTypeObject();
        if (typeObject == null) {
            return start();
        } else if (typeObject instanceof org.eclipse.emf.ecore.EClass) {
            org.eclipse.emf.ecore.EClass type = (org.eclipse.emf.ecore.EClass) typeObject;
            if (type.getInstanceClass() == org.emftext.language.OCL.SimpleName.class) {
                return parse_org_emftext_language_OCL_SimpleName();
            }
            if (type.getInstanceClass() == org.emftext.language.OCL.Exp.class) {
                return parse_org_emftext_language_OCL_Exp();
            }
            if (type.getInstanceClass() == org.emftext.language.OCL.TypePathNameSimple.class) {
                return parse_org_emftext_language_OCL_TypePathNameSimple();
            }
            if (type.getInstanceClass() == org.emftext.language.OCL.VariableDeclarationWithInit.class) {
                return parse_org_emftext_language_OCL_VariableDeclarationWithInit();
            }
        }
        throw new org.emftext.language.OCL.resource.OCL.mopp.OCLUnexpectedContentTypeException(typeObject);
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
            typeObject = options.get(org.emftext.language.OCL.resource.OCL.IOCLOptions.RESOURCE_CONTENT_TYPE);
        }
        return typeObject;
    }

    /**
    	 * Implementation that calls {@link #doParse()} and handles the thrown
    	 * RecognitionExceptions.
    	 */
    public org.emftext.language.OCL.resource.OCL.IOCLParseResult parse() {
        terminateParsing = false;
        postParseCommands = new java.util.ArrayList<org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource>>();
        org.emftext.language.OCL.resource.OCL.mopp.OCLParseResult parseResult = new org.emftext.language.OCL.resource.OCL.mopp.OCLParseResult();
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

    public java.util.List<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal> parseToExpectedElements(org.eclipse.emf.ecore.EClass type, org.emftext.language.OCL.resource.OCL.IOCLTextResource dummyResource, int cursorOffset) {
        this.rememberExpectedElements = true;
        this.parseToIndexTypeObject = type;
        this.cursorOffset = cursorOffset;
        this.lastStartIncludingHidden = -1;
        final org.antlr.runtime3_3_0.CommonTokenStream tokenStream = (org.antlr.runtime3_3_0.CommonTokenStream) getTokenStream();
        org.emftext.language.OCL.resource.OCL.IOCLParseResult result = parse();
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
            for (org.emftext.language.OCL.resource.OCL.IOCLCommand<org.emftext.language.OCL.resource.OCL.IOCLTextResource> command : result.getPostParseCommands()) {
                command.execute(dummyResource);
            }
        }
        expectedElements = expectedElements.subList(0, expectedElementsIndexOfLastCompleteElement + 1);
        int lastFollowSetID = expectedElements.get(expectedElementsIndexOfLastCompleteElement).getFollowSetID();
        java.util.Set<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal> currentFollowSet = new java.util.LinkedHashSet<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal>();
        java.util.List<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal> newFollowSet = new java.util.ArrayList<org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal>();
        for (int i = expectedElementsIndexOfLastCompleteElement; i >= 0; i--) {
            org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal expectedElementI = expectedElements.get(i);
            if (expectedElementI.getFollowSetID() == lastFollowSetID) {
                currentFollowSet.add(expectedElementI);
            } else {
                break;
            }
        }
        int followSetID = 25;
        int i;
        for (i = tokenIndexOfLastCompleteElement; i < tokenStream.size(); i++) {
            org.antlr.runtime3_3_0.CommonToken nextToken = (org.antlr.runtime3_3_0.CommonToken) tokenStream.get(i);
            if (nextToken.getType() < 0) {
                break;
            }
            if (nextToken.getChannel() == 99) {
            } else {
                for (org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal nextFollow : newFollowSet) {
                    lastTokenIndex = 0;
                    setPosition(nextFollow, i);
                }
                newFollowSet.clear();
                for (org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal nextFollow : currentFollowSet) {
                    if (nextFollow.getTerminal().getTokenNames().contains(getTokenNames()[nextToken.getType()])) {
                        java.util.Collection<org.emftext.language.OCL.resource.OCL.util.OCLPair<org.emftext.language.OCL.resource.OCL.IOCLExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]>> newFollowers = nextFollow.getTerminal().getFollowers();
                        for (org.emftext.language.OCL.resource.OCL.util.OCLPair<org.emftext.language.OCL.resource.OCL.IOCLExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]> newFollowerPair : newFollowers) {
                            org.emftext.language.OCL.resource.OCL.IOCLExpectedElement newFollower = newFollowerPair.getLeft();
                            org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal newFollowTerminal = new org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal(newFollower, followSetID, newFollowerPair.getRight());
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
        for (org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal nextFollow : newFollowSet) {
            lastTokenIndex = 0;
            setPosition(nextFollow, i);
        }
        return this.expectedElements;
    }

    public void setPosition(org.emftext.language.OCL.resource.OCL.mopp.OCLExpectedTerminal expectedElement, int tokenIndex) {
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
        org.emftext.language.OCL.Exp c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 1)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 0, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 0, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    expectedElementsIndexOfLastCompleteElement = 0;
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_Exp_in_start82);
                    c0 = parse_org_emftext_language_OCL_Exp();
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

    public final org.emftext.language.OCL.SimpleName parse_org_emftext_language_OCL_SimpleName() throws RecognitionException {
        org.emftext.language.OCL.SimpleName element = null;
        int parse_org_emftext_language_OCL_SimpleName_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 2)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, SIMPLE_NAME, FOLLOW_SIMPLE_NAME_in_parse_org_emftext_language_OCL_SimpleName119);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createSimpleName();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("SIMPLE_NAME");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.SIMPLE_NAME__NAME), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.SIMPLE_NAME__NAME), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_0_0_0_0, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_3, 1);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_4, 1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 2, parse_org_emftext_language_OCL_SimpleName_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.Exp parse_org_emftext_language_OCL_Exp() throws RecognitionException {
        org.emftext.language.OCL.Exp element = null;
        int parse_org_emftext_language_OCL_Exp_StartIndex = input.index();
        org.emftext.language.OCL.OCLExpression a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 3)) {
                return element;
            }
            {
                loop1: do {
                    int alt1 = 2;
                    int LA1_0 = input.LA(1);
                    if ((LA1_0 == 28 || LA1_0 == 31)) {
                        alt1 = 1;
                    }
                    switch(alt1) {
                        case 1:
                            {
                                {
                                    {
                                        pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_Exp170);
                                        a0_0 = parse_org_emftext_language_OCL_OCLExpression();
                                        state._fsp--;
                                        if (state.failed) return element;
                                        if (state.backtracking == 0) {
                                            if (terminateParsing) {
                                                throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                                            }
                                            if (element == null) {
                                                element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createExp();
                                                incompleteObjects.push(element);
                                            }
                                            if (a0_0 != null) {
                                                if (a0_0 != null) {
                                                    Object value = a0_0;
                                                    addObjectToList(element, org.emftext.language.OCL.OCLPackage.EXP__EXP, value);
                                                    completedElement(value, true);
                                                }
                                                collectHiddenTokens(element);
                                                retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_1_0_0_0_0_0_0, a0_0, true);
                                                copyLocalizationInfos(a0_0, element);
                                            }
                                        }
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 2, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                                        addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 2, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                                    }
                                }
                            }
                            break;
                        default:
                            break loop1;
                    }
                } while (true);
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 3, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 3, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 3, parse_org_emftext_language_OCL_Exp_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.TypePathNameSimple parse_org_emftext_language_OCL_TypePathNameSimple() throws RecognitionException {
        org.emftext.language.OCL.TypePathNameSimple element = null;
        int parse_org_emftext_language_OCL_TypePathNameSimple_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 4)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, SIMPLE_NAME, FOLLOW_SIMPLE_NAME_in_parse_org_emftext_language_OCL_TypePathNameSimple230);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createTypePathNameSimple();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("SIMPLE_NAME");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.TYPE_PATH_NAME_SIMPLE__TYPE_NAME), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            String resolved = (String) resolvedObject;
                            org.emftext.language.OCL.Type proxy = org.emftext.language.OCL.OCLFactory.eINSTANCE.createTypePathNameSimple();
                            collectHiddenTokens(element);
                            registerContextDependentProxy(new org.emftext.language.OCL.resource.OCL.mopp.OCLContextDependentURIFragmentFactory<org.emftext.language.OCL.TypePathNameSimple, org.emftext.language.OCL.Type>(getReferenceResolverSwitch() == null ? null : getReferenceResolverSwitch().getTypePathNameSimpleTypeNameReferenceResolver()), element, (org.eclipse.emf.ecore.EReference) element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.TYPE_PATH_NAME_SIMPLE__TYPE_NAME), resolved, proxy);
                            if (proxy != null) {
                                Object value = proxy;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.TYPE_PATH_NAME_SIMPLE__TYPE_NAME), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_2_0_0_0, proxy, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, proxy);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_4, 4);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 4, parse_org_emftext_language_OCL_TypePathNameSimple_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.VariableDeclarationWithInit parse_org_emftext_language_OCL_VariableDeclarationWithInit() throws RecognitionException {
        org.emftext.language.OCL.VariableDeclarationWithInit element = null;
        int parse_org_emftext_language_OCL_VariableDeclarationWithInit_StartIndex = input.index();
        Token a1 = null;
        Token a3 = null;
        org.emftext.language.OCL.SimpleName a0_0 = null;
        org.emftext.language.OCL.Type a2_0 = null;
        org.emftext.language.OCL.OCLExpression a4_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 5)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_SimpleName_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit270);
                    a0_0 = parse_org_emftext_language_OCL_SimpleName();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.VARIABLE_DECLARATION_WITH_INIT__VARIABLE_NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_3_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_3, 5);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_4, 5);
                }
                int alt2 = 2;
                int LA2_0 = input.LA(1);
                if ((LA2_0 == 27)) {
                    alt2 = 1;
                }
                switch(alt2) {
                    case 1:
                        {
                            {
                                a1 = (Token) match(input, 27, FOLLOW_27_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit297);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (element == null) {
                                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createVariableDeclarationWithInit();
                                        incompleteObjects.push(element);
                                    }
                                    collectHiddenTokens(element);
                                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_3_0_0_1_0_0_0, null, true);
                                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a1, element);
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_5, 6, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_1);
                                }
                                {
                                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_Type_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit323);
                                    a2_0 = parse_org_emftext_language_OCL_Type();
                                    state._fsp--;
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (terminateParsing) {
                                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                                        }
                                        if (element == null) {
                                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createVariableDeclarationWithInit();
                                            incompleteObjects.push(element);
                                        }
                                        if (a2_0 != null) {
                                            if (a2_0 != null) {
                                                Object value = a2_0;
                                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.VARIABLE_DECLARATION_WITH_INIT__TYPE_NAME), value);
                                                completedElement(value, true);
                                            }
                                            collectHiddenTokens(element);
                                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_3_0_0_1_0_0_1, a2_0, true);
                                            copyLocalizationInfos(a2_0, element);
                                        }
                                    }
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_4, 7);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_4, 8);
                }
                {
                    a3 = (Token) match(input, EQUALITY_OPERATOR, FOLLOW_EQUALITY_OPERATOR_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit368);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a3 != null) {
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("EQUALITY_OPERATOR");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.OCL.resource.OCL.IOCLTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a3.getText(), element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.VARIABLE_DECLARATION_WITH_INIT__EQUAL), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a3).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a3).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a3).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a3).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.VARIABLE_DECLARATION_WITH_INIT__EQUAL), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_3_0_0_2, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a3, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 9, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_2);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 9, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_2);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit393);
                    a4_0 = parse_org_emftext_language_OCL_OCLExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createVariableDeclarationWithInit();
                            incompleteObjects.push(element);
                        }
                        if (a4_0 != null) {
                            if (a4_0 != null) {
                                Object value = a4_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.VARIABLE_DECLARATION_WITH_INIT__INITIALIZATION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_3_0_0_3, a4_0, true);
                            copyLocalizationInfos(a4_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 10);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 10);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 5, parse_org_emftext_language_OCL_VariableDeclarationWithInit_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.OCLExpression parseop_OCLExpression_level_20() throws RecognitionException {
        org.emftext.language.OCL.OCLExpression element = null;
        int parseop_OCLExpression_level_20_StartIndex = input.index();
        org.emftext.language.OCL.LetExp c0 = null;
        org.emftext.language.OCL.IfExp c1 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 6)) {
                return element;
            }
            int alt3 = 2;
            int LA3_0 = input.LA(1);
            if ((LA3_0 == 28)) {
                alt3 = 1;
            } else if ((LA3_0 == 31)) {
                alt3 = 2;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return element;
                }
                NoViableAltException nvae = new NoViableAltException("", 3, 0, input);
                throw nvae;
            }
            switch(alt3) {
                case 1:
                    {
                        pushFollow(FOLLOW_parse_org_emftext_language_OCL_LetExp_in_parseop_OCLExpression_level_20426);
                        c0 = parse_org_emftext_language_OCL_LetExp();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c0;
                        }
                    }
                    break;
                case 2:
                    {
                        pushFollow(FOLLOW_parse_org_emftext_language_OCL_IfExp_in_parseop_OCLExpression_level_20436);
                        c1 = parse_org_emftext_language_OCL_IfExp();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c1;
                        }
                    }
                    break;
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 6, parseop_OCLExpression_level_20_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.LetExp parse_org_emftext_language_OCL_LetExp() throws RecognitionException {
        org.emftext.language.OCL.LetExp element = null;
        int parse_org_emftext_language_OCL_LetExp_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        Token a4 = null;
        org.emftext.language.OCL.VariableDeclarationWithInit a1_0 = null;
        org.emftext.language.OCL.VariableDeclarationWithInit a3_0 = null;
        org.emftext.language.OCL.OCLExpression a5_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 7)) {
                return element;
            }
            {
                a0 = (Token) match(input, 28, FOLLOW_28_in_parse_org_emftext_language_OCL_LetExp459);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_2, 11, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_3, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_4);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_VariableDeclarationWithInit_in_parse_org_emftext_language_OCL_LetExp477);
                    a1_0 = parse_org_emftext_language_OCL_VariableDeclarationWithInit();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                addObjectToList(element, org.emftext.language.OCL.OCLPackage.LET_EXP__VARIABLE_DECLARATIONS, value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 12);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 12);
                }
                loop4: do {
                    int alt4 = 2;
                    int LA4_0 = input.LA(1);
                    if ((LA4_0 == 29)) {
                        alt4 = 1;
                    }
                    switch(alt4) {
                        case 1:
                            {
                                {
                                    a2 = (Token) match(input, 29, FOLLOW_29_in_parse_org_emftext_language_OCL_LetExp504);
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (element == null) {
                                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                                            incompleteObjects.push(element);
                                        }
                                        collectHiddenTokens(element);
                                        retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_2_0_0_1, null, true);
                                        copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_2, 13, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_3, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_4);
                                    }
                                    {
                                        pushFollow(FOLLOW_parse_org_emftext_language_OCL_VariableDeclarationWithInit_in_parse_org_emftext_language_OCL_LetExp530);
                                        a3_0 = parse_org_emftext_language_OCL_VariableDeclarationWithInit();
                                        state._fsp--;
                                        if (state.failed) return element;
                                        if (state.backtracking == 0) {
                                            if (terminateParsing) {
                                                throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                                            }
                                            if (element == null) {
                                                element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                                                incompleteObjects.push(element);
                                            }
                                            if (a3_0 != null) {
                                                if (a3_0 != null) {
                                                    Object value = a3_0;
                                                    addObjectToList(element, org.emftext.language.OCL.OCLPackage.LET_EXP__VARIABLE_DECLARATIONS, value);
                                                    completedElement(value, true);
                                                }
                                                collectHiddenTokens(element);
                                                retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_2_0_0_2, a3_0, true);
                                                copyLocalizationInfos(a3_0, element);
                                            }
                                        }
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 14);
                                        addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 14);
                                    }
                                }
                            }
                            break;
                        default:
                            break loop4;
                    }
                } while (true);
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 15);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 15);
                }
                a4 = (Token) match(input, 30, FOLLOW_30_in_parse_org_emftext_language_OCL_LetExp571);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_3, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a4, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 16, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_5);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 16, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_5);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_LetExp589);
                    a5_0 = parse_org_emftext_language_OCL_OCLExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createLetExp();
                            incompleteObjects.push(element);
                        }
                        if (a5_0 != null) {
                            if (a5_0 != null) {
                                Object value = a5_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.LET_EXP__OCL_EXPRESSION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_4_0_0_5, a5_0, true);
                            copyLocalizationInfos(a5_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 17, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 17, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 17);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 17);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_8, 17);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 7, parse_org_emftext_language_OCL_LetExp_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.IfExp parse_org_emftext_language_OCL_IfExp() throws RecognitionException {
        org.emftext.language.OCL.IfExp element = null;
        int parse_org_emftext_language_OCL_IfExp_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        Token a4 = null;
        Token a6 = null;
        org.emftext.language.OCL.OCLExpression a1_0 = null;
        org.emftext.language.OCL.OCLExpression a3_0 = null;
        org.emftext.language.OCL.OCLExpression a5_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 8)) {
                return element;
            }
            {
                a0 = (Token) match(input, 31, FOLLOW_31_in_parse_org_emftext_language_OCL_IfExp622);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 18, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_6);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 18, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_6);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp640);
                    a1_0 = parse_org_emftext_language_OCL_OCLExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.IF_EXP__CONDITION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_8, 19);
                }
                a2 = (Token) match(input, 32, FOLLOW_32_in_parse_org_emftext_language_OCL_IfExp658);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_3, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 20, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_7);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 20, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_7);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp676);
                    a3_0 = parse_org_emftext_language_OCL_OCLExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                            incompleteObjects.push(element);
                        }
                        if (a3_0 != null) {
                            if (a3_0 != null) {
                                Object value = a3_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.IF_EXP__THEN_BRANCH), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_5, a3_0, true);
                            copyLocalizationInfos(a3_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_9, 21);
                }
                a4 = (Token) match(input, 33, FOLLOW_33_in_parse_org_emftext_language_OCL_IfExp694);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_7, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a4, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 22, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_8);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 22, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_8);
                }
                {
                    pushFollow(FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp712);
                    a5_0 = parse_org_emftext_language_OCL_OCLExpression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.OCL.resource.OCL.mopp.OCLTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                            incompleteObjects.push(element);
                        }
                        if (a5_0 != null) {
                            if (a5_0 != null) {
                                Object value = a5_0;
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.OCL.OCLPackage.IF_EXP__ELSE_BRANCH), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_9, a5_0, true);
                            copyLocalizationInfos(a5_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_10, 23);
                }
                a6 = (Token) match(input, 34, FOLLOW_34_in_parse_org_emftext_language_OCL_IfExp730);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.OCL.OCLFactory.eINSTANCE.createIfExp();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, org.emftext.language.OCL.resource.OCL.grammar.OCLGrammarInformationProvider.OCL_5_0_0_11, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a6, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_0, 24, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_1, 24, org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.FEATURE_0);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_6, 24);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_7, 24);
                    addExpectedElement(org.emftext.language.OCL.resource.OCL.grammar.OCLFollowSetProvider.TERMINAL_8, 24);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 8, parse_org_emftext_language_OCL_IfExp_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.OCLExpression parse_org_emftext_language_OCL_OCLExpression() throws RecognitionException {
        org.emftext.language.OCL.OCLExpression element = null;
        int parse_org_emftext_language_OCL_OCLExpression_StartIndex = input.index();
        org.emftext.language.OCL.OCLExpression c = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 9)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parseop_OCLExpression_level_20_in_parse_org_emftext_language_OCL_OCLExpression755);
                c = parseop_OCLExpression_level_20();
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
                memoize(input, 9, parse_org_emftext_language_OCL_OCLExpression_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.OCL.Type parse_org_emftext_language_OCL_Type() throws RecognitionException {
        org.emftext.language.OCL.Type element = null;
        int parse_org_emftext_language_OCL_Type_StartIndex = input.index();
        org.emftext.language.OCL.TypePathNameSimple c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 10)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parse_org_emftext_language_OCL_TypePathNameSimple_in_parse_org_emftext_language_OCL_Type776);
                c0 = parse_org_emftext_language_OCL_TypePathNameSimple();
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
                memoize(input, 10, parse_org_emftext_language_OCL_Type_StartIndex);
            }
        }
        return element;
    }

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_Exp_in_start82 = new BitSet(new long[] { 0x0000000000000000L });

    public static final BitSet FOLLOW_EOF_in_start89 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_SIMPLE_NAME_in_parse_org_emftext_language_OCL_SimpleName119 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_Exp170 = new BitSet(new long[] { 0x0000000090000002L });

    public static final BitSet FOLLOW_SIMPLE_NAME_in_parse_org_emftext_language_OCL_TypePathNameSimple230 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_SimpleName_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit270 = new BitSet(new long[] { 0x0000000008000020L });

    public static final BitSet FOLLOW_27_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit297 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_Type_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit323 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_EQUALITY_OPERATOR_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit368 = new BitSet(new long[] { 0x0000000090000000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_VariableDeclarationWithInit393 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_LetExp_in_parseop_OCLExpression_level_20426 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_IfExp_in_parseop_OCLExpression_level_20436 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_28_in_parse_org_emftext_language_OCL_LetExp459 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_VariableDeclarationWithInit_in_parse_org_emftext_language_OCL_LetExp477 = new BitSet(new long[] { 0x0000000060000000L });

    public static final BitSet FOLLOW_29_in_parse_org_emftext_language_OCL_LetExp504 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_VariableDeclarationWithInit_in_parse_org_emftext_language_OCL_LetExp530 = new BitSet(new long[] { 0x0000000060000000L });

    public static final BitSet FOLLOW_30_in_parse_org_emftext_language_OCL_LetExp571 = new BitSet(new long[] { 0x0000000090000000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_LetExp589 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_31_in_parse_org_emftext_language_OCL_IfExp622 = new BitSet(new long[] { 0x0000000190000000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp640 = new BitSet(new long[] { 0x0000000100000000L });

    public static final BitSet FOLLOW_32_in_parse_org_emftext_language_OCL_IfExp658 = new BitSet(new long[] { 0x0000000290000000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp676 = new BitSet(new long[] { 0x0000000200000000L });

    public static final BitSet FOLLOW_33_in_parse_org_emftext_language_OCL_IfExp694 = new BitSet(new long[] { 0x0000000490000000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_OCLExpression_in_parse_org_emftext_language_OCL_IfExp712 = new BitSet(new long[] { 0x0000000400000000L });

    public static final BitSet FOLLOW_34_in_parse_org_emftext_language_OCL_IfExp730 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parseop_OCLExpression_level_20_in_parse_org_emftext_language_OCL_OCLExpression755 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_OCL_TypePathNameSimple_in_parse_org_emftext_language_OCL_Type776 = new BitSet(new long[] { 0x0000000000000002L });
}
