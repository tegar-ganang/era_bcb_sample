package fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp;

import org.antlr.runtime3_3_0.*;
import java.util.HashMap;

@SuppressWarnings("unused")
public class AlfParser extends AlfANTLRParserBase {

    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "IDENTIFIER", "DOCUMENTATION_COMMENT", "WHITESPACE", "LINEBREAKS", "'{'", "'}'", "'let'", "':'", "'['", "']'", "'='", "';'", "'new'", "'('", "')'" };

    public static final int EOF = -1;

    public static final int T__8 = 8;

    public static final int T__9 = 9;

    public static final int T__10 = 10;

    public static final int T__11 = 11;

    public static final int T__12 = 12;

    public static final int T__13 = 13;

    public static final int T__14 = 14;

    public static final int T__15 = 15;

    public static final int T__16 = 16;

    public static final int T__17 = 17;

    public static final int T__18 = 18;

    public static final int IDENTIFIER = 4;

    public static final int DOCUMENTATION_COMMENT = 5;

    public static final int WHITESPACE = 6;

    public static final int LINEBREAKS = 7;

    public AlfParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public AlfParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
        this.state.ruleMemo = new HashMap[25 + 1];
    }

    public String[] getTokenNames() {
        return AlfParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "Alf.g";
    }

    private fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTokenResolverFactory tokenResolverFactory = new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTokenResolverFactory();

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
    private java.util.List<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal> expectedElements = new java.util.ArrayList<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal>();

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
        postParseCommands.add(new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>() {

            public boolean execute(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.addProblem(new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfProblem() {

                    public fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.AlfEProblemSeverity getSeverity() {
                        return fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.AlfEProblemSeverity.ERROR;
                    }

                    public fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.AlfEProblemType getType() {
                        return fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.AlfEProblemType.SYNTAX_ERROR;
                    }

                    public String getMessage() {
                        return errorMessage;
                    }

                    public java.util.Collection<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfQuickFix> getQuickFixes() {
                        return null;
                    }
                }, column, line, startIndex, stopIndex);
                return true;
            }
        });
    }

    public void addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfExpectedElement terminal, int followSetID, org.eclipse.emf.ecore.EStructuralFeature... containmentTrace) {
        if (!this.rememberExpectedElements) {
            return;
        }
        fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal expectedElement = new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal(terminal, followSetID, containmentTrace);
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
        postParseCommands.add(new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>() {

            public boolean execute(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource resource) {
                fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfLocationMap locationMap = resource.getLocationMap();
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
        postParseCommands.add(new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>() {

            public boolean execute(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource resource) {
                fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfLocationMap locationMap = resource.getLocationMap();
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
    protected void setLocalizationEnd(java.util.Collection<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>> postParseCommands, final org.eclipse.emf.ecore.EObject object, final int endChar, final int endLine) {
        postParseCommands.add(new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>() {

            public boolean execute(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource resource) {
                fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfLocationMap locationMap = resource.getLocationMap();
                if (locationMap == null) {
                    return true;
                }
                locationMap.setCharEnd(object, endChar);
                locationMap.setLine(object, endLine);
                return true;
            }
        });
    }

    public fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextParser createInstance(java.io.InputStream actualInputStream, String encoding) {
        try {
            if (encoding == null) {
                return new AlfParser(new org.antlr.runtime3_3_0.CommonTokenStream(new AlfLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream))));
            } else {
                return new AlfParser(new org.antlr.runtime3_3_0.CommonTokenStream(new AlfLexer(new org.antlr.runtime3_3_0.ANTLRInputStream(actualInputStream, encoding))));
            }
        } catch (java.io.IOException e) {
            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfPlugin.logError("Error while creating parser.", e);
            return null;
        }
    }

    /**
    	 * This default constructor is only used to call createInstance() on it.
    	 */
    public AlfParser() {
        super(null);
    }

    protected org.eclipse.emf.ecore.EObject doParse() throws org.antlr.runtime3_3_0.RecognitionException {
        this.lastPosition = 0;
        ((AlfLexer) getTokenStream().getTokenSource()).lexerExceptions = lexerExceptions;
        ((AlfLexer) getTokenStream().getTokenSource()).lexerExceptionsPosition = lexerExceptionsPosition;
        Object typeObject = getTypeObject();
        if (typeObject == null) {
            return start();
        } else if (typeObject instanceof org.eclipse.emf.ecore.EClass) {
            org.eclipse.emf.ecore.EClass type = (org.eclipse.emf.ecore.EClass) typeObject;
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.Name.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_Name();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.TypeName.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.QualifiedName.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.UnqualifiedName.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.NameBinding.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.Block.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_Block();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.StatementSequence.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.DocumentedStatement.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatement.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.MultiplicityIndicator.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatementCompletion.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.InstanceInitializationExpression.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.Tuple.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression();
            }
            if (type.getInstanceClass() == fr.inria.papyrus.uml4tst.emftext.alf.SequenceElements.class) {
                return parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements();
            }
        }
        throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfUnexpectedContentTypeException(typeObject);
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
            typeObject = options.get(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfOptions.RESOURCE_CONTENT_TYPE);
        }
        return typeObject;
    }

    /**
    	 * Implementation that calls {@link #doParse()} and handles the thrown
    	 * RecognitionExceptions.
    	 */
    public fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfParseResult parse() {
        terminateParsing = false;
        postParseCommands = new java.util.ArrayList<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource>>();
        fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfParseResult parseResult = new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfParseResult();
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

    public java.util.List<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal> parseToExpectedElements(org.eclipse.emf.ecore.EClass type, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource dummyResource, int cursorOffset) {
        this.rememberExpectedElements = true;
        this.parseToIndexTypeObject = type;
        this.cursorOffset = cursorOffset;
        this.lastStartIncludingHidden = -1;
        final org.antlr.runtime3_3_0.CommonTokenStream tokenStream = (org.antlr.runtime3_3_0.CommonTokenStream) getTokenStream();
        fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfParseResult result = parse();
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
            for (fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfCommand<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTextResource> command : result.getPostParseCommands()) {
                command.execute(dummyResource);
            }
        }
        expectedElements = expectedElements.subList(0, expectedElementsIndexOfLastCompleteElement + 1);
        int lastFollowSetID = expectedElements.get(expectedElementsIndexOfLastCompleteElement).getFollowSetID();
        java.util.Set<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal> currentFollowSet = new java.util.LinkedHashSet<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal>();
        java.util.List<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal> newFollowSet = new java.util.ArrayList<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal>();
        for (int i = expectedElementsIndexOfLastCompleteElement; i >= 0; i--) {
            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal expectedElementI = expectedElements.get(i);
            if (expectedElementI.getFollowSetID() == lastFollowSetID) {
                currentFollowSet.add(expectedElementI);
            } else {
                break;
            }
        }
        int followSetID = 37;
        int i;
        for (i = tokenIndexOfLastCompleteElement; i < tokenStream.size(); i++) {
            org.antlr.runtime3_3_0.CommonToken nextToken = (org.antlr.runtime3_3_0.CommonToken) tokenStream.get(i);
            if (nextToken.getType() < 0) {
                break;
            }
            if (nextToken.getChannel() == 99) {
            } else {
                for (fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal nextFollow : newFollowSet) {
                    lastTokenIndex = 0;
                    setPosition(nextFollow, i);
                }
                newFollowSet.clear();
                for (fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal nextFollow : currentFollowSet) {
                    if (nextFollow.getTerminal().getTokenNames().contains(getTokenNames()[nextToken.getType()])) {
                        java.util.Collection<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.util.AlfPair<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]>> newFollowers = nextFollow.getTerminal().getFollowers();
                        for (fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.util.AlfPair<fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfExpectedElement, org.eclipse.emf.ecore.EStructuralFeature[]> newFollowerPair : newFollowers) {
                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfExpectedElement newFollower = newFollowerPair.getLeft();
                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal newFollowTerminal = new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal(newFollower, followSetID, newFollowerPair.getRight());
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
        for (fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal nextFollow : newFollowSet) {
            lastTokenIndex = 0;
            setPosition(nextFollow, i);
        }
        return this.expectedElements;
    }

    public void setPosition(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfExpectedTerminal expectedElement, int tokenIndex) {
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
        fr.inria.papyrus.uml4tst.emftext.alf.Block c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 1)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_0, 0);
                    expectedElementsIndexOfLastCompleteElement = 0;
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block_in_start82);
                    c0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Block();
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

    public final fr.inria.papyrus.uml4tst.emftext.alf.Name parse_fr_inria_papyrus_uml4tst_emftext_alf_Name() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.Name element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_StartIndex = input.index();
        Token a0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 2)) {
                return element;
            }
            {
                {
                    a0 = (Token) match(input, IDENTIFIER, FOLLOW_IDENTIFIER_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name119);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createName();
                            incompleteObjects.push(element);
                        }
                        if (a0 != null) {
                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("IDENTIFIER");
                            tokenResolver.setOptions(getOptions());
                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.NAME__NAME), result);
                            Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                Object value = resolved;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.NAME__NAME), value);
                                completedElement(value, false);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_0_0_0_0, resolved, true);
                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 1, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 1, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 2, parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.TypeName parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.TypeName element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.QualifiedName a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 3)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName159);
                    a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createTypeName();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.TYPE_NAME__QUALIFIED_NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_1_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 2, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 2, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 3, parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.QualifiedName parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.QualifiedName element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.UnqualifiedName a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 4)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName196);
                    a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createQualifiedName();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.QUALIFIED_NAME__UNQUALIFIED_NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_2_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 3, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 3, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 4, parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.UnqualifiedName parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.UnqualifiedName element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.NameBinding a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 5)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName233);
                    a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createUnqualifiedName();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.UNQUALIFIED_NAME__NAME_BINDING), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_3_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 5, parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.NameBinding parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.NameBinding element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.Name a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 6)) {
                return element;
            }
            {
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding270);
                    a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Name();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createNameBinding();
                            incompleteObjects.push(element);
                        }
                        if (a0_0 != null) {
                            if (a0_0 != null) {
                                Object value = a0_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.NAME_BINDING__NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_4_0_0_0, a0_0, true);
                            copyLocalizationInfos(a0_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 5, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 5, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 6, parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.Block parse_fr_inria_papyrus_uml4tst_emftext_alf_Block() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.Block element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_Block_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.StatementSequence a1_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 7)) {
                return element;
            }
            {
                a0 = (Token) match(input, 8, FOLLOW_8_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block303);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createBlock();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_5_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 6, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_3);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 6, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_3);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 6);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block321);
                    a1_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createBlock();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.BLOCK__STATEMENT_SEQUENCE), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_5_0_0_2, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 7);
                }
                a2 = (Token) match(input, 9, FOLLOW_9_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block339);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createBlock();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_5_0_0_4, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 7, parse_fr_inria_papyrus_uml4tst_emftext_alf_Block_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.StatementSequence parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.StatementSequence element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.DocumentedStatement a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 8)) {
                return element;
            }
            {
                loop1: do {
                    int alt1 = 2;
                    int LA1_0 = input.LA(1);
                    if ((LA1_0 == DOCUMENTATION_COMMENT || LA1_0 == 10)) {
                        alt1 = 1;
                    }
                    switch(alt1) {
                        case 1:
                            {
                                {
                                    {
                                        pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence383);
                                        a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement();
                                        state._fsp--;
                                        if (state.failed) return element;
                                        if (state.backtracking == 0) {
                                            if (terminateParsing) {
                                                throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                                            }
                                            if (element == null) {
                                                element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createStatementSequence();
                                                incompleteObjects.push(element);
                                            }
                                            if (a0_0 != null) {
                                                if (a0_0 != null) {
                                                    Object value = a0_0;
                                                    addObjectToList(element, fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.STATEMENT_SEQUENCE__DOCUMENTED_STATEMENT, value);
                                                    completedElement(value, true);
                                                }
                                                collectHiddenTokens(element);
                                                retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_6_0_0_0_0_0_0, a0_0, true);
                                                copyLocalizationInfos(a0_0, element);
                                            }
                                        }
                                    }
                                    if (state.backtracking == 0) {
                                        addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 9, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                                        addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 9, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                                        addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 9);
                                    }
                                }
                            }
                            break;
                        default:
                            break loop1;
                    }
                } while (true);
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 10, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 10, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 10);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 8, parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.DocumentedStatement parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.DocumentedStatement element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement_StartIndex = input.index();
        Token a0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.Statement a1_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 9)) {
                return element;
            }
            {
                int alt2 = 2;
                int LA2_0 = input.LA(1);
                if ((LA2_0 == DOCUMENTATION_COMMENT)) {
                    alt2 = 1;
                }
                switch(alt2) {
                    case 1:
                        {
                            {
                                {
                                    a0 = (Token) match(input, DOCUMENTATION_COMMENT, FOLLOW_DOCUMENTATION_COMMENT_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement454);
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (terminateParsing) {
                                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                                        }
                                        if (element == null) {
                                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createDocumentedStatement();
                                            incompleteObjects.push(element);
                                        }
                                        if (a0 != null) {
                                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("DOCUMENTATION_COMMENT");
                                            tokenResolver.setOptions(getOptions());
                                            fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.IAlfTokenResolveResult result = getFreshTokenResolveResult();
                                            tokenResolver.resolve(a0.getText(), element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.DOCUMENTED_STATEMENT__DOCUMENTATION_COMMENT), result);
                                            Object resolvedObject = result.getResolvedToken();
                                            if (resolvedObject == null) {
                                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime3_3_0.CommonToken) a0).getLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getCharPositionInLine(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStartIndex(), ((org.antlr.runtime3_3_0.CommonToken) a0).getStopIndex());
                                            }
                                            java.lang.String resolved = (java.lang.String) resolvedObject;
                                            if (resolved != null) {
                                                Object value = resolved;
                                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.DOCUMENTED_STATEMENT__DOCUMENTATION_COMMENT), value);
                                                completedElement(value, false);
                                            }
                                            collectHiddenTokens(element);
                                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_7_0_0_0_0_0_0, resolved, true);
                                            copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                                        }
                                    }
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 11, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 12, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement504);
                    a1_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createDocumentedStatement();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.DOCUMENTED_STATEMENT__STATEMENT), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_7_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 13, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 13, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 13);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 9, parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatement parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatement element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.Name a1_0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.TypeName a3_0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.MultiplicityIndicator a4_0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatementCompletion a5_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 10)) {
                return element;
            }
            {
                a0 = (Token) match(input, 10, FOLLOW_10_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement537);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_1, 14, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_5);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement555);
                    a1_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Name();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.LOCAL_NAME_DECLARATION_STATEMENT__NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_2, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_7, 15);
                }
                a2 = (Token) match(input, 11, FOLLOW_11_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement573);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_4, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_1, 16, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_6, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_7, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_8, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_9, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_10);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement591);
                    a3_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                            incompleteObjects.push(element);
                        }
                        if (a3_0 != null) {
                            if (a3_0 != null) {
                                Object value = a3_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.LOCAL_NAME_DECLARATION_STATEMENT__TYPE_NAME), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_6, a3_0, true);
                            copyLocalizationInfos(a3_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_2, 17, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_0);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 17, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
                int alt3 = 2;
                int LA3_0 = input.LA(1);
                if ((LA3_0 == 12)) {
                    alt3 = 1;
                }
                switch(alt3) {
                    case 1:
                        {
                            {
                                {
                                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement624);
                                    a4_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator();
                                    state._fsp--;
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (terminateParsing) {
                                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                                        }
                                        if (element == null) {
                                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                                            incompleteObjects.push(element);
                                        }
                                        if (a4_0 != null) {
                                            if (a4_0 != null) {
                                                Object value = a4_0;
                                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.LOCAL_NAME_DECLARATION_STATEMENT__MULTIPLICITY_INDICATOR), value);
                                                completedElement(value, true);
                                            }
                                            collectHiddenTokens(element);
                                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_7_0_0_0, a4_0, true);
                                            copyLocalizationInfos(a4_0, element);
                                        }
                                    }
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 18, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 19, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement669);
                    a5_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatement();
                            incompleteObjects.push(element);
                        }
                        if (a5_0 != null) {
                            if (a5_0 != null) {
                                Object value = a5_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.LOCAL_NAME_DECLARATION_STATEMENT__LOCAL_NAME_DECLARATION_STATEMENT_COMPLETION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_8_0_0_9, a5_0, true);
                            copyLocalizationInfos(a5_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 20, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 20, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 20);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 10, parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.MultiplicityIndicator parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.MultiplicityIndicator element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 11)) {
                return element;
            }
            {
                a0 = (Token) match(input, 12, FOLLOW_12_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator702);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createMultiplicityIndicator();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_9_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_8, 21);
                }
                a1 = (Token) match(input, 13, FOLLOW_13_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator716);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createMultiplicityIndicator();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_9_0_0_1, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_3, 22, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_1);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 11, parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatementCompletion parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatementCompletion element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion_StartIndex = input.index();
        Token a0 = null;
        Token a2 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.Expression a1_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 12)) {
                return element;
            }
            {
                a0 = (Token) match(input, 14, FOLLOW_14_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion745);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatementCompletion();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_10_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_9, 23, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_11);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_10, 23, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_11);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_11, 23, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_11);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 23);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion763);
                    a1_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatementCompletion();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.LOCAL_NAME_DECLARATION_STATEMENT_COMPLETION__INITIALIZATION_EXPRESSION), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_10_0_0_2, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 24);
                }
                a2 = (Token) match(input, 15, FOLLOW_15_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion781);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createLocalNameDeclarationStatementCompletion();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_10_0_0_3, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_4, 25, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_5, 25, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_4, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_2);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_6, 25);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 12, parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.InstanceInitializationExpression parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.InstanceInitializationExpression element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_StartIndex = input.index();
        Token a0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.Tuple a1_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 13)) {
                return element;
            }
            {
                a0 = (Token) match(input, 16, FOLLOW_16_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression810);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createInstanceInitializationExpression();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_11_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_13, 26, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_12);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression828);
                    a1_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createInstanceInitializationExpression();
                            incompleteObjects.push(element);
                        }
                        if (a1_0 != null) {
                            if (a1_0 != null) {
                                Object value = a1_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.INSTANCE_INITIALIZATION_EXPRESSION__TUPLE), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_11_0_0_1, a1_0, true);
                            copyLocalizationInfos(a1_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 27);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 13, parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.Tuple parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.Tuple element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 14)) {
                return element;
            }
            {
                a0 = (Token) match(input, 17, FOLLOW_17_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple861);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createTuple();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_12_0_0_0, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_14, 28);
                }
                a1 = (Token) match(input, 18, FOLLOW_18_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple875);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createTuple();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_12_0_0_1, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 29);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 14, parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        Token a3 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceElements a2_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 15)) {
                return element;
            }
            {
                int alt4 = 2;
                int LA4_0 = input.LA(1);
                if ((LA4_0 == 16)) {
                    alt4 = 1;
                }
                switch(alt4) {
                    case 1:
                        {
                            {
                                a0 = (Token) match(input, 16, FOLLOW_16_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression913);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (element == null) {
                                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createSequenceInitializationExpression();
                                        incompleteObjects.push(element);
                                    }
                                    collectHiddenTokens(element);
                                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_13_0_0_0_0_0_0, null, true);
                                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a0, element);
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_11, 30);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_11, 31);
                }
                a1 = (Token) match(input, 8, FOLLOW_8_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression946);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createSequenceInitializationExpression();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_13_0_0_1, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_10, 32, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_13, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_14);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_11, 32, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_13, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.FEATURE_14);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_15, 32);
                }
                {
                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression964);
                    a2_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                        }
                        if (element == null) {
                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createSequenceInitializationExpression();
                            incompleteObjects.push(element);
                        }
                        if (a2_0 != null) {
                            if (a2_0 != null) {
                                Object value = a2_0;
                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.SEQUENCE_INITIALIZATION_EXPRESSION__SEQUENCE_ELEMENTS), value);
                                completedElement(value, true);
                            }
                            collectHiddenTokens(element);
                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_13_0_0_2, a2_0, true);
                            copyLocalizationInfos(a2_0, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_15, 33);
                }
                a3 = (Token) match(input, 9, FOLLOW_9_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression982);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createSequenceInitializationExpression();
                        incompleteObjects.push(element);
                    }
                    collectHiddenTokens(element);
                    retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_13_0_0_3, null, true);
                    copyLocalizationInfos((org.antlr.runtime3_3_0.CommonToken) a3, element);
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 34);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_15, 34);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 15, parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.SequenceElements parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceElements element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression a0_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 16)) {
                return element;
            }
            {
                int alt5 = 2;
                int LA5_0 = input.LA(1);
                if ((LA5_0 == 8 || LA5_0 == 16)) {
                    alt5 = 1;
                }
                switch(alt5) {
                    case 1:
                        {
                            {
                                {
                                    pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements1026);
                                    a0_0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression();
                                    state._fsp--;
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (terminateParsing) {
                                            throw new fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.mopp.AlfTerminateParsingException();
                                        }
                                        if (element == null) {
                                            element = fr.inria.papyrus.uml4tst.emftext.alf.AlfFactory.eINSTANCE.createSequenceElements();
                                            incompleteObjects.push(element);
                                        }
                                        if (a0_0 != null) {
                                            if (a0_0 != null) {
                                                Object value = a0_0;
                                                element.eSet(element.eClass().getEStructuralFeature(fr.inria.papyrus.uml4tst.emftext.alf.AlfPackage.SEQUENCE_ELEMENTS__SEQUENCE_INITIALIZATION_EXPRESSION), value);
                                                completedElement(value, true);
                                            }
                                            collectHiddenTokens(element);
                                            retrieveLayoutInformation(element, fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfGrammarInformationProvider.ALF_14_0_0_0_0_0_0, a0_0, true);
                                            copyLocalizationInfos(a0_0, element);
                                        }
                                    }
                                }
                                if (state.backtracking == 0) {
                                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 35);
                                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_15, 35);
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_12, 36);
                    addExpectedElement(fr.inria.papyrus.uml4tst.emftext.alf.resource.alf.grammar.AlfFollowSetProvider.TERMINAL_15, 36);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 16, parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.Statement parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.Statement element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.LocalNameDeclarationStatement c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 17)) {
                return element;
            }
            {
                pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement1078);
                c0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement();
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
                memoize(input, 17, parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement_StartIndex);
            }
        }
        return element;
    }

    public final fr.inria.papyrus.uml4tst.emftext.alf.Expression parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.Expression element = null;
        int parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression_StartIndex = input.index();
        fr.inria.papyrus.uml4tst.emftext.alf.InstanceInitializationExpression c0 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression c1 = null;
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceElements c2 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 18)) {
                return element;
            }
            int alt6 = 3;
            switch(input.LA(1)) {
                case 16:
                    {
                        int LA6_1 = input.LA(2);
                        if ((synpred6_Alf())) {
                            alt6 = 1;
                        } else if ((synpred7_Alf())) {
                            alt6 = 2;
                        } else if ((true)) {
                            alt6 = 3;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return element;
                            }
                            NoViableAltException nvae = new NoViableAltException("", 6, 1, input);
                            throw nvae;
                        }
                    }
                    break;
                case 8:
                    {
                        int LA6_2 = input.LA(2);
                        if ((synpred7_Alf())) {
                            alt6 = 2;
                        } else if ((true)) {
                            alt6 = 3;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return element;
                            }
                            NoViableAltException nvae = new NoViableAltException("", 6, 2, input);
                            throw nvae;
                        }
                    }
                    break;
                case 15:
                    {
                        alt6 = 3;
                    }
                    break;
                default:
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return element;
                    }
                    NoViableAltException nvae = new NoViableAltException("", 6, 0, input);
                    throw nvae;
            }
            switch(alt6) {
                case 1:
                    {
                        pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1099);
                        c0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c0;
                        }
                    }
                    break;
                case 2:
                    {
                        pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1109);
                        c1 = parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c1;
                        }
                    }
                    break;
                case 3:
                    {
                        pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1119);
                        c2 = parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c2;
                        }
                    }
                    break;
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 18, parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression_StartIndex);
            }
        }
        return element;
    }

    public final void synpred6_Alf_fragment() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.InstanceInitializationExpression c0 = null;
        {
            pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_in_synpred6_Alf1099);
            c0 = parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression();
            state._fsp--;
            if (state.failed) return;
        }
    }

    public final void synpred7_Alf_fragment() throws RecognitionException {
        fr.inria.papyrus.uml4tst.emftext.alf.SequenceInitializationExpression c1 = null;
        {
            pushFollow(FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_synpred7_Alf1109);
            c1 = parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression();
            state._fsp--;
            if (state.failed) return;
        }
    }

    public final boolean synpred6_Alf() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred6_Alf_fragment();
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred7_Alf() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred7_Alf_fragment();
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block_in_start82 = new BitSet(new long[] { 0x0000000000000000L });

    public static final BitSet FOLLOW_EOF_in_start89 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_IDENTIFIER_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name119 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName159 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_QualifiedName196 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_UnqualifiedName233 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_NameBinding270 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_8_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block303 = new BitSet(new long[] { 0x0000000000000620L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block321 = new BitSet(new long[] { 0x0000000000000200L });

    public static final BitSet FOLLOW_9_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Block339 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_StatementSequence383 = new BitSet(new long[] { 0x0000000000000422L });

    public static final BitSet FOLLOW_DOCUMENTATION_COMMENT_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement454 = new BitSet(new long[] { 0x0000000000000420L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_DocumentedStatement504 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_10_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement537 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Name_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement555 = new BitSet(new long[] { 0x0000000000000800L });

    public static final BitSet FOLLOW_11_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement573 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_TypeName_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement591 = new BitSet(new long[] { 0x0000000000005000L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement624 = new BitSet(new long[] { 0x0000000000005000L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement669 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_12_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator702 = new BitSet(new long[] { 0x0000000000002000L });

    public static final BitSet FOLLOW_13_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_MultiplicityIndicator716 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_14_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion745 = new BitSet(new long[] { 0x0000000000010100L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion763 = new BitSet(new long[] { 0x0000000000008000L });

    public static final BitSet FOLLOW_15_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatementCompletion781 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_16_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression810 = new BitSet(new long[] { 0x0000000000020000L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression828 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_17_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple861 = new BitSet(new long[] { 0x0000000000040000L });

    public static final BitSet FOLLOW_18_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Tuple875 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_16_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression913 = new BitSet(new long[] { 0x0000000000000100L });

    public static final BitSet FOLLOW_8_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression946 = new BitSet(new long[] { 0x0000000000010300L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression964 = new BitSet(new long[] { 0x0000000000000200L });

    public static final BitSet FOLLOW_9_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression982 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements1026 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_LocalNameDeclarationStatement_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Statement1078 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1099 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1109 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceElements_in_parse_fr_inria_papyrus_uml4tst_emftext_alf_Expression1119 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_InstanceInitializationExpression_in_synpred6_Alf1099 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_fr_inria_papyrus_uml4tst_emftext_alf_SequenceInitializationExpression_in_synpred7_Alf1109 = new BitSet(new long[] { 0x0000000000000002L });
}
