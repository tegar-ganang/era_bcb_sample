package org.emftext.language.office.resource.office.mopp;

import org.antlr.runtime.*;
import java.util.HashMap;

public class OfficeParser extends OfficeANTLRParserBase {

    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "QUOTED_34_34", "TEXT", "COMMENT", "INTEGER", "FLOAT", "WHITESPACE", "LINEBREAK", "'OfficeModel'", "'{'", "'name'", "':'", "'elements'", "'}'", "'Office'", "'Employee'", "'worksIn'", "'worksWith'" };

    public static final int INTEGER = 7;

    public static final int T__20 = 20;

    public static final int WHITESPACE = 9;

    public static final int FLOAT = 8;

    public static final int TEXT = 5;

    public static final int EOF = -1;

    public static final int T__19 = 19;

    public static final int T__16 = 16;

    public static final int T__15 = 15;

    public static final int T__18 = 18;

    public static final int T__17 = 17;

    public static final int T__12 = 12;

    public static final int T__11 = 11;

    public static final int T__14 = 14;

    public static final int T__13 = 13;

    public static final int COMMENT = 6;

    public static final int QUOTED_34_34 = 4;

    public static final int LINEBREAK = 10;

    public OfficeParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public OfficeParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
        this.state.ruleMemo = new HashMap[12 + 1];
    }

    public String[] getTokenNames() {
        return OfficeParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "D:\\TuBa\\EclipseWorkspacesNew\\TuBa_WS\\org.emftext.language.office.resource.office\\src-gen\\org\\emftext\\language\\office\\resource\\office\\mopp\\Office.g";
    }

    private org.emftext.language.office.resource.office.IOfficeTokenResolverFactory tokenResolverFactory = new org.emftext.language.office.resource.office.mopp.OfficeTokenResolverFactory();

    private int lastPosition;

    private org.emftext.language.office.resource.office.mopp.OfficeTokenResolveResult tokenResolveResult = new org.emftext.language.office.resource.office.mopp.OfficeTokenResolveResult();

    private boolean rememberExpectedElements = false;

    private java.lang.Object parseToIndexTypeObject;

    private int lastTokenIndex = 0;

    private boolean reachedIndex = false;

    private java.util.List<org.emftext.language.office.resource.office.IOfficeExpectedElement> expectedElements = new java.util.ArrayList<org.emftext.language.office.resource.office.IOfficeExpectedElement>();

    private int lastIndex = -1;

    private int mismatchedTokenRecoveryTries = 0;

    private java.util.Map<?, ?> options;

    protected java.util.List<org.antlr.runtime.RecognitionException> lexerExceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<org.antlr.runtime.RecognitionException>());

    protected java.util.List<java.lang.Integer> lexerExceptionsPosition = java.util.Collections.synchronizedList(new java.util.ArrayList<java.lang.Integer>());

    private int stopIncludingHiddenTokens;

    private int stopExcludingHiddenTokens;

    private java.util.Collection<org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>> postParseCommands;

    private boolean terminateParsing;

    protected void addErrorToResource(final java.lang.String errorMessage, final int line, final int charPositionInLine, final int startIndex, final int stopIndex) {
        postParseCommands.add(new org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>() {

            public boolean execute(org.emftext.language.office.resource.office.IOfficeTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.addProblem(new org.emftext.language.office.resource.office.IOfficeProblem() {

                    public org.emftext.language.office.resource.office.OfficeEProblemType getType() {
                        return org.emftext.language.office.resource.office.OfficeEProblemType.ERROR;
                    }

                    public java.lang.String getMessage() {
                        return errorMessage;
                    }
                }, line, charPositionInLine, startIndex, stopIndex);
                return true;
            }
        });
    }

    public void addExpectedElement(org.emftext.language.office.resource.office.IOfficeExpectedElement expectedElement, java.lang.String message) {
        if (!this.rememberExpectedElements) {
            return;
        }
        if (this.reachedIndex) {
            return;
        }
        int currentIndex = java.lang.Math.max(0, input.index());
        for (int index = lastTokenIndex; index < currentIndex; index++) {
            if (index >= input.size()) {
                break;
            }
            org.antlr.runtime.CommonToken tokenAtIndex = (org.antlr.runtime.CommonToken) input.get(index);
            stopIncludingHiddenTokens = tokenAtIndex.getStopIndex() + 1;
            if (tokenAtIndex.getChannel() != 99) {
                stopExcludingHiddenTokens = tokenAtIndex.getStopIndex() + 1;
            }
        }
        lastTokenIndex = java.lang.Math.max(0, currentIndex);
        expectedElement.setPosition(stopExcludingHiddenTokens, stopIncludingHiddenTokens);
        System.out.println("Adding expected element (" + message + "): " + expectedElement + "");
        this.expectedElements.add(expectedElement);
    }

    protected void addMapEntry(org.eclipse.emf.ecore.EObject element, org.eclipse.emf.ecore.EStructuralFeature structuralFeature, org.emftext.language.office.resource.office.mopp.OfficeDummyEObject dummy) {
        java.lang.Object value = element.eGet(structuralFeature);
        java.lang.Object mapKey = dummy.getValueByName("key");
        java.lang.Object mapValue = dummy.getValueByName("value");
        if (value instanceof org.eclipse.emf.common.util.EMap<?, ?>) {
            org.eclipse.emf.common.util.EMap<java.lang.Object, java.lang.Object> valueMap = org.emftext.language.office.resource.office.util.OfficeMapUtil.castToEMap(value);
            if (mapKey != null && mapValue != null) {
                valueMap.put(mapKey, mapValue);
            }
        }
    }

    private boolean addObjectToList(org.eclipse.emf.ecore.EObject element, int featureID, java.lang.Object proxy) {
        return ((java.util.List<java.lang.Object>) element.eGet(element.eClass().getEStructuralFeature(featureID))).add(proxy);
    }

    protected org.eclipse.emf.ecore.EObject apply(org.eclipse.emf.ecore.EObject target, java.util.List<org.eclipse.emf.ecore.EObject> dummyEObjects) {
        org.eclipse.emf.ecore.EObject currentTarget = target;
        for (org.eclipse.emf.ecore.EObject object : dummyEObjects) {
            assert (object instanceof org.emftext.language.office.resource.office.mopp.OfficeDummyEObject);
            org.emftext.language.office.resource.office.mopp.OfficeDummyEObject dummy = (org.emftext.language.office.resource.office.mopp.OfficeDummyEObject) object;
            org.eclipse.emf.ecore.EObject newEObject = dummy.applyTo(currentTarget);
            currentTarget = newEObject;
        }
        return currentTarget;
    }

    protected void collectHiddenTokens(org.eclipse.emf.ecore.EObject element) {
    }

    protected void copyLocalizationInfos(final org.eclipse.emf.ecore.EObject source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>() {

            public boolean execute(org.emftext.language.office.resource.office.IOfficeTextResource resource) {
                if (resource == null) {
                    return true;
                }
                org.emftext.language.office.resource.office.IOfficeLocationMap locationMap = resource.getLocationMap();
                locationMap.setCharStart(target, locationMap.getCharStart(source));
                locationMap.setCharEnd(target, locationMap.getCharEnd(source));
                locationMap.setColumn(target, locationMap.getColumn(source));
                locationMap.setLine(target, locationMap.getLine(source));
                return true;
            }
        });
    }

    protected void copyLocalizationInfos(final org.antlr.runtime.CommonToken source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>() {

            public boolean execute(org.emftext.language.office.resource.office.IOfficeTextResource resource) {
                if (resource == null) {
                    return true;
                }
                org.emftext.language.office.resource.office.IOfficeLocationMap locationMap = resource.getLocationMap();
                locationMap.setCharStart(target, source.getStartIndex());
                locationMap.setCharEnd(target, source.getStopIndex());
                locationMap.setColumn(target, source.getCharPositionInLine());
                locationMap.setLine(target, source.getLine());
                return true;
            }
        });
    }

    public org.emftext.language.office.resource.office.IOfficeTextParser createInstance(java.io.InputStream actualInputStream, java.lang.String encoding) {
        try {
            if (encoding == null) {
                return new OfficeParser(new org.antlr.runtime.CommonTokenStream(new OfficeLexer(new org.antlr.runtime.ANTLRInputStream(actualInputStream))));
            } else {
                return new OfficeParser(new org.antlr.runtime.CommonTokenStream(new OfficeLexer(new org.antlr.runtime.ANTLRInputStream(actualInputStream, encoding))));
            }
        } catch (java.io.IOException e) {
            org.emftext.language.office.resource.office.mopp.OfficePlugin.logError("Error while creating parser.", e);
            return null;
        }
    }

    public OfficeParser() {
        super(null);
    }

    protected org.eclipse.emf.ecore.EObject doParse() throws org.antlr.runtime.RecognitionException {
        lastPosition = 0;
        ((OfficeLexer) getTokenStream().getTokenSource()).lexerExceptions = lexerExceptions;
        ((OfficeLexer) getTokenStream().getTokenSource()).lexerExceptionsPosition = lexerExceptionsPosition;
        java.lang.Object typeObject = getTypeObject();
        if (typeObject == null) {
            return start();
        } else if (typeObject instanceof org.eclipse.emf.ecore.EClass) {
            org.eclipse.emf.ecore.EClass type = (org.eclipse.emf.ecore.EClass) typeObject;
            if (type.getInstanceClass() == org.emftext.language.office.OfficeModel.class) {
                return parse_org_emftext_language_office_OfficeModel();
            }
            if (type.getInstanceClass() == org.emftext.language.office.Office.class) {
                return parse_org_emftext_language_office_Office();
            }
            if (type.getInstanceClass() == org.emftext.language.office.Employee.class) {
                return parse_org_emftext_language_office_Employee();
            }
        }
        throw new org.emftext.language.office.resource.office.mopp.OfficeUnexpectedContentTypeException(typeObject);
    }

    private org.emftext.language.office.resource.office.mopp.OfficeTokenResolveResult getFreshTokenResolveResult() {
        tokenResolveResult.clear();
        return tokenResolveResult;
    }

    public int getMismatchedTokenRecoveryTries() {
        return mismatchedTokenRecoveryTries;
    }

    public java.lang.Object getMissingSymbol(org.antlr.runtime.IntStream arg0, org.antlr.runtime.RecognitionException arg1, int arg2, org.antlr.runtime.BitSet arg3) {
        mismatchedTokenRecoveryTries++;
        return super.getMissingSymbol(arg0, arg1, arg2, arg3);
    }

    protected java.util.Map<?, ?> getOptions() {
        return options;
    }

    public org.emftext.language.office.resource.office.mopp.OfficeMetaInformation getMetaInformation() {
        return new org.emftext.language.office.resource.office.mopp.OfficeMetaInformation();
    }

    public java.lang.Object getParseToIndexTypeObject() {
        return parseToIndexTypeObject;
    }

    protected org.emftext.language.office.resource.office.mopp.OfficeReferenceResolverSwitch getReferenceResolverSwitch() {
        return (org.emftext.language.office.resource.office.mopp.OfficeReferenceResolverSwitch) getMetaInformation().getReferenceResolverSwitch();
    }

    protected java.lang.Object getTypeObject() {
        java.lang.Object typeObject = getParseToIndexTypeObject();
        if (typeObject != null) {
            return typeObject;
        }
        java.util.Map<?, ?> options = getOptions();
        if (options != null) {
            typeObject = options.get(org.emftext.language.office.resource.office.IOfficeOptions.RESOURCE_CONTENT_TYPE);
        }
        return typeObject;
    }

    public org.emftext.language.office.resource.office.IOfficeParseResult parse() {
        terminateParsing = false;
        postParseCommands = new java.util.ArrayList<org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>>();
        org.emftext.language.office.resource.office.mopp.OfficeParseResult parseResult = new org.emftext.language.office.resource.office.mopp.OfficeParseResult();
        try {
            org.eclipse.emf.ecore.EObject result = doParse();
            if (lexerExceptions.isEmpty()) {
                parseResult.setRoot(result);
            }
        } catch (org.antlr.runtime.RecognitionException re) {
            reportError(re);
        } catch (java.lang.IllegalArgumentException iae) {
            if ("The 'no null' constraint is violated".equals(iae.getMessage())) {
            } else {
                iae.printStackTrace();
            }
        }
        for (org.antlr.runtime.RecognitionException re : lexerExceptions) {
            reportLexicalError(re);
        }
        parseResult.getPostParseCommands().addAll(postParseCommands);
        return parseResult;
    }

    public java.util.List<org.emftext.language.office.resource.office.IOfficeExpectedElement> parseToExpectedElements(org.eclipse.emf.ecore.EClass type) {
        rememberExpectedElements = true;
        parseToIndexTypeObject = type;
        parse();
        return this.expectedElements;
    }

    public java.lang.Object recoverFromMismatchedToken(org.antlr.runtime.IntStream input, int ttype, org.antlr.runtime.BitSet follow) throws org.antlr.runtime.RecognitionException {
        if (!rememberExpectedElements) {
            return super.recoverFromMismatchedToken(input, ttype, follow);
        } else {
            return null;
        }
    }

    protected <ContainerType extends org.eclipse.emf.ecore.EObject, ReferenceType extends org.eclipse.emf.ecore.EObject> void registerContextDependentProxy(final org.emftext.language.office.resource.office.mopp.OfficeContextDependentURIFragmentFactory<ContainerType, ReferenceType> factory, final ContainerType element, final org.eclipse.emf.ecore.EReference reference, final String id, final org.eclipse.emf.ecore.EObject proxy) {
        postParseCommands.add(new org.emftext.language.office.resource.office.IOfficeCommand<org.emftext.language.office.resource.office.IOfficeTextResource>() {

            public boolean execute(org.emftext.language.office.resource.office.IOfficeTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.registerContextDependentProxy(factory, element, reference, id, proxy);
                return true;
            }
        });
    }

    public void reportError(final org.antlr.runtime.RecognitionException e) {
        java.lang.String message = e.getMessage();
        if (e instanceof org.antlr.runtime.MismatchedTokenException) {
            org.antlr.runtime.MismatchedTokenException mte = (org.antlr.runtime.MismatchedTokenException) e;
            java.lang.String tokenName = "<unknown>";
            if (mte.expecting == Token.EOF) {
                tokenName = "EOF";
            } else {
                tokenName = getTokenNames()[mte.expecting];
                tokenName = org.emftext.language.office.resource.office.util.OfficeStringUtil.formatTokenName(tokenName);
            }
            message = "Syntax error on token \"" + e.token.getText() + "\", \"" + tokenName + "\" expected";
        } else if (e instanceof org.antlr.runtime.MismatchedTreeNodeException) {
            org.antlr.runtime.MismatchedTreeNodeException mtne = (org.antlr.runtime.MismatchedTreeNodeException) e;
            java.lang.String tokenName = "<unknown>";
            if (mtne.expecting == Token.EOF) {
                tokenName = "EOF";
            } else {
                tokenName = getTokenNames()[mtne.expecting];
            }
            message = "mismatched tree node: " + "xxx" + "; expecting " + tokenName;
        } else if (e instanceof org.antlr.runtime.NoViableAltException) {
            message = "Syntax error on token \"" + e.token.getText() + "\", check following tokens";
        } else if (e instanceof org.antlr.runtime.EarlyExitException) {
            message = "Syntax error on token \"" + e.token.getText() + "\", delete this token";
        } else if (e instanceof org.antlr.runtime.MismatchedSetException) {
            org.antlr.runtime.MismatchedSetException mse = (org.antlr.runtime.MismatchedSetException) e;
            message = "mismatched token: " + e.token + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime.MismatchedNotSetException) {
            org.antlr.runtime.MismatchedNotSetException mse = (org.antlr.runtime.MismatchedNotSetException) e;
            message = "mismatched token: " + e.token + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime.FailedPredicateException) {
            org.antlr.runtime.FailedPredicateException fpe = (org.antlr.runtime.FailedPredicateException) e;
            message = "rule " + fpe.ruleName + " failed predicate: {" + fpe.predicateText + "}?";
        }
        final java.lang.String finalMessage = message;
        if (e.token instanceof org.antlr.runtime.CommonToken) {
            final org.antlr.runtime.CommonToken ct = (org.antlr.runtime.CommonToken) e.token;
            addErrorToResource(finalMessage, ct.getCharPositionInLine(), ct.getLine(), ct.getStartIndex(), ct.getStopIndex());
        } else {
            addErrorToResource(finalMessage, e.token.getCharPositionInLine(), e.token.getLine(), 1, 5);
        }
    }

    public void reportLexicalError(final org.antlr.runtime.RecognitionException e) {
        java.lang.String message = "";
        if (e instanceof org.antlr.runtime.MismatchedTokenException) {
            org.antlr.runtime.MismatchedTokenException mte = (org.antlr.runtime.MismatchedTokenException) e;
            message = "Syntax error on token \"" + ((char) e.c) + "\", \"" + (char) mte.expecting + "\" expected";
        } else if (e instanceof org.antlr.runtime.NoViableAltException) {
            message = "Syntax error on token \"" + ((char) e.c) + "\", delete this token";
        } else if (e instanceof org.antlr.runtime.EarlyExitException) {
            org.antlr.runtime.EarlyExitException eee = (org.antlr.runtime.EarlyExitException) e;
            message = "required (...)+ loop (decision=" + eee.decisionNumber + ") did not match anything; on line " + e.line + ":" + e.charPositionInLine + " char=" + ((char) e.c) + "'";
        } else if (e instanceof org.antlr.runtime.MismatchedSetException) {
            org.antlr.runtime.MismatchedSetException mse = (org.antlr.runtime.MismatchedSetException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime.MismatchedNotSetException) {
            org.antlr.runtime.MismatchedNotSetException mse = (org.antlr.runtime.MismatchedNotSetException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set " + mse.expecting;
        } else if (e instanceof org.antlr.runtime.MismatchedRangeException) {
            org.antlr.runtime.MismatchedRangeException mre = (org.antlr.runtime.MismatchedRangeException) e;
            message = "mismatched char: '" + ((char) e.c) + "' on line " + e.line + ":" + e.charPositionInLine + "; expecting set '" + (char) mre.a + "'..'" + (char) mre.b + "'";
        } else if (e instanceof org.antlr.runtime.FailedPredicateException) {
            org.antlr.runtime.FailedPredicateException fpe = (org.antlr.runtime.FailedPredicateException) e;
            message = "rule " + fpe.ruleName + " failed predicate: {" + fpe.predicateText + "}?";
        }
        addErrorToResource(message, e.index, e.line, lexerExceptionsPosition.get(lexerExceptions.indexOf(e)), lexerExceptionsPosition.get(lexerExceptions.indexOf(e)));
    }

    public void setOptions(java.util.Map<?, ?> options) {
        this.options = options;
    }

    public void terminate() {
        terminateParsing = true;
    }

    public final org.eclipse.emf.ecore.EObject start() throws RecognitionException {
        org.eclipse.emf.ecore.EObject element = null;
        int start_StartIndex = input.index();
        org.emftext.language.office.OfficeModel c0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 1)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                }
                {
                    if (state.backtracking == 0) {
                    }
                    pushFollow(FOLLOW_parse_org_emftext_language_office_OfficeModel_in_start82);
                    c0 = parse_org_emftext_language_office_OfficeModel();
                    state._fsp--;
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        element = c0;
                    }
                }
                match(input, EOF, FOLLOW_EOF_in_start87);
                if (state.failed) return element;
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

    public final org.emftext.language.office.OfficeModel parse_org_emftext_language_office_OfficeModel() throws RecognitionException {
        org.emftext.language.office.OfficeModel element = null;
        int parse_org_emftext_language_office_OfficeModel_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        Token a2 = null;
        Token a3 = null;
        Token a4 = null;
        Token a5 = null;
        Token a6 = null;
        Token a8 = null;
        org.emftext.language.office.OfficeElement a7_0 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 2)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                }
                a0 = (Token) match(input, 11, FOLLOW_11_in_parse_org_emftext_language_office_OfficeModel110);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                }
                a1 = (Token) match(input, 12, FOLLOW_12_in_parse_org_emftext_language_office_OfficeModel121);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                }
                loop2: do {
                    int alt2 = 2;
                    int LA2_0 = input.LA(1);
                    if ((LA2_0 == 13 || LA2_0 == 15)) {
                        alt2 = 1;
                    }
                    switch(alt2) {
                        case 1:
                            {
                                if (state.backtracking == 0) {
                                }
                                int alt1 = 2;
                                int LA1_0 = input.LA(1);
                                if ((LA1_0 == 13)) {
                                    alt1 = 1;
                                } else if ((LA1_0 == 15)) {
                                    alt1 = 2;
                                } else {
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return element;
                                    }
                                    NoViableAltException nvae = new NoViableAltException("", 1, 0, input);
                                    throw nvae;
                                }
                                switch(alt1) {
                                    case 1:
                                        {
                                            if (state.backtracking == 0) {
                                            }
                                            a2 = (Token) match(input, 13, FOLLOW_13_in_parse_org_emftext_language_office_OfficeModel146);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a2, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            a3 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_OfficeModel163);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a3, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            {
                                                a4 = (Token) match(input, QUOTED_34_34, FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_OfficeModel185);
                                                if (state.failed) return element;
                                                if (state.backtracking == 0) {
                                                    if (terminateParsing) {
                                                        throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                                    }
                                                    if (element == null) {
                                                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                    }
                                                    if (a4 != null) {
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("QUOTED_34_34");
                                                        tokenResolver.setOptions(getOptions());
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolveResult result = getFreshTokenResolveResult();
                                                        tokenResolver.resolve(a4.getText(), element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.OFFICE_MODEL__NAME), result);
                                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                                        if (resolvedObject == null) {
                                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a4).getLine(), ((org.antlr.runtime.CommonToken) a4).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a4).getStartIndex(), ((org.antlr.runtime.CommonToken) a4).getStopIndex());
                                                        }
                                                        java.lang.String resolved = (java.lang.String) resolvedObject;
                                                        if (resolved != null) {
                                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.OFFICE_MODEL__NAME), resolved);
                                                        }
                                                        collectHiddenTokens(element);
                                                        copyLocalizationInfos((CommonToken) a4, element);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    case 2:
                                        {
                                            if (state.backtracking == 0) {
                                            }
                                            a5 = (Token) match(input, 15, FOLLOW_15_in_parse_org_emftext_language_office_OfficeModel218);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a5, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            a6 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_OfficeModel235);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a6, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            {
                                                pushFollow(FOLLOW_parse_org_emftext_language_office_OfficeElement_in_parse_org_emftext_language_office_OfficeModel257);
                                                a7_0 = parse_org_emftext_language_office_OfficeElement();
                                                state._fsp--;
                                                if (state.failed) return element;
                                                if (state.backtracking == 0) {
                                                    if (terminateParsing) {
                                                        throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                                    }
                                                    if (element == null) {
                                                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                                                    }
                                                    if (a7_0 != null) {
                                                        if (a7_0 != null) {
                                                            addObjectToList(element, org.emftext.language.office.OfficePackage.OFFICE_MODEL__ELEMENTS, a7_0);
                                                        }
                                                        collectHiddenTokens(element);
                                                        copyLocalizationInfos(a7_0, element);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                            break;
                        default:
                            break loop2;
                    }
                } while (true);
                if (state.backtracking == 0) {
                }
                if (state.backtracking == 0) {
                }
                a8 = (Token) match(input, 16, FOLLOW_16_in_parse_org_emftext_language_office_OfficeModel286);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOfficeModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a8, element);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 2, parse_org_emftext_language_office_OfficeModel_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.office.Office parse_org_emftext_language_office_Office() throws RecognitionException {
        org.emftext.language.office.Office element = null;
        int parse_org_emftext_language_office_Office_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        Token a2 = null;
        Token a3 = null;
        Token a4 = null;
        Token a5 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 3)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                }
                a0 = (Token) match(input, 17, FOLLOW_17_in_parse_org_emftext_language_office_Office312);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                }
                a1 = (Token) match(input, 12, FOLLOW_12_in_parse_org_emftext_language_office_Office323);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                }
                loop3: do {
                    int alt3 = 2;
                    int LA3_0 = input.LA(1);
                    if ((LA3_0 == 13)) {
                        alt3 = 1;
                    }
                    switch(alt3) {
                        case 1:
                            {
                                if (state.backtracking == 0) {
                                }
                                {
                                    if (state.backtracking == 0) {
                                    }
                                    a2 = (Token) match(input, 13, FOLLOW_13_in_parse_org_emftext_language_office_Office348);
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (element == null) {
                                            element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a2, element);
                                    }
                                    if (state.backtracking == 0) {
                                    }
                                    a3 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_Office365);
                                    if (state.failed) return element;
                                    if (state.backtracking == 0) {
                                        if (element == null) {
                                            element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a3, element);
                                    }
                                    if (state.backtracking == 0) {
                                    }
                                    {
                                        a4 = (Token) match(input, QUOTED_34_34, FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_Office387);
                                        if (state.failed) return element;
                                        if (state.backtracking == 0) {
                                            if (terminateParsing) {
                                                throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                            }
                                            if (element == null) {
                                                element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                                            }
                                            if (a4 != null) {
                                                org.emftext.language.office.resource.office.IOfficeTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("QUOTED_34_34");
                                                tokenResolver.setOptions(getOptions());
                                                org.emftext.language.office.resource.office.IOfficeTokenResolveResult result = getFreshTokenResolveResult();
                                                tokenResolver.resolve(a4.getText(), element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.OFFICE__NAME), result);
                                                java.lang.Object resolvedObject = result.getResolvedToken();
                                                if (resolvedObject == null) {
                                                    addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a4).getLine(), ((org.antlr.runtime.CommonToken) a4).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a4).getStartIndex(), ((org.antlr.runtime.CommonToken) a4).getStopIndex());
                                                }
                                                java.lang.String resolved = (java.lang.String) resolvedObject;
                                                if (resolved != null) {
                                                    element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.OFFICE__NAME), resolved);
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a4, element);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        default:
                            break loop3;
                    }
                } while (true);
                if (state.backtracking == 0) {
                }
                if (state.backtracking == 0) {
                }
                a5 = (Token) match(input, 16, FOLLOW_16_in_parse_org_emftext_language_office_Office420);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a5, element);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 3, parse_org_emftext_language_office_Office_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.office.Employee parse_org_emftext_language_office_Employee() throws RecognitionException {
        org.emftext.language.office.Employee element = null;
        int parse_org_emftext_language_office_Employee_StartIndex = input.index();
        Token a0 = null;
        Token a1 = null;
        Token a2 = null;
        Token a3 = null;
        Token a4 = null;
        Token a5 = null;
        Token a6 = null;
        Token a7 = null;
        Token a8 = null;
        Token a9 = null;
        Token a10 = null;
        Token a11 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 4)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                }
                a0 = (Token) match(input, 18, FOLLOW_18_in_parse_org_emftext_language_office_Employee446);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                }
                a1 = (Token) match(input, 12, FOLLOW_12_in_parse_org_emftext_language_office_Employee457);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a1, element);
                }
                if (state.backtracking == 0) {
                }
                loop5: do {
                    int alt5 = 2;
                    int LA5_0 = input.LA(1);
                    if ((LA5_0 == 13 || (LA5_0 >= 19 && LA5_0 <= 20))) {
                        alt5 = 1;
                    }
                    switch(alt5) {
                        case 1:
                            {
                                if (state.backtracking == 0) {
                                }
                                int alt4 = 3;
                                switch(input.LA(1)) {
                                    case 13:
                                        {
                                            alt4 = 1;
                                        }
                                        break;
                                    case 19:
                                        {
                                            alt4 = 2;
                                        }
                                        break;
                                    case 20:
                                        {
                                            alt4 = 3;
                                        }
                                        break;
                                    default:
                                        if (state.backtracking > 0) {
                                            state.failed = true;
                                            return element;
                                        }
                                        NoViableAltException nvae = new NoViableAltException("", 4, 0, input);
                                        throw nvae;
                                }
                                switch(alt4) {
                                    case 1:
                                        {
                                            if (state.backtracking == 0) {
                                            }
                                            a2 = (Token) match(input, 13, FOLLOW_13_in_parse_org_emftext_language_office_Employee482);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a2, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            a3 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_Employee499);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a3, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            {
                                                a4 = (Token) match(input, QUOTED_34_34, FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_Employee521);
                                                if (state.failed) return element;
                                                if (state.backtracking == 0) {
                                                    if (terminateParsing) {
                                                        throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                                    }
                                                    if (element == null) {
                                                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                    }
                                                    if (a4 != null) {
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("QUOTED_34_34");
                                                        tokenResolver.setOptions(getOptions());
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolveResult result = getFreshTokenResolveResult();
                                                        tokenResolver.resolve(a4.getText(), element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__NAME), result);
                                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                                        if (resolvedObject == null) {
                                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a4).getLine(), ((org.antlr.runtime.CommonToken) a4).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a4).getStartIndex(), ((org.antlr.runtime.CommonToken) a4).getStopIndex());
                                                        }
                                                        java.lang.String resolved = (java.lang.String) resolvedObject;
                                                        if (resolved != null) {
                                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__NAME), resolved);
                                                        }
                                                        collectHiddenTokens(element);
                                                        copyLocalizationInfos((CommonToken) a4, element);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    case 2:
                                        {
                                            if (state.backtracking == 0) {
                                            }
                                            a5 = (Token) match(input, 19, FOLLOW_19_in_parse_org_emftext_language_office_Employee554);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a5, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            a6 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_Employee571);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a6, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            {
                                                a7 = (Token) match(input, TEXT, FOLLOW_TEXT_in_parse_org_emftext_language_office_Employee593);
                                                if (state.failed) return element;
                                                if (state.backtracking == 0) {
                                                    if (terminateParsing) {
                                                        throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                                    }
                                                    if (element == null) {
                                                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                    }
                                                    if (a7 != null) {
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("TEXT");
                                                        tokenResolver.setOptions(getOptions());
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolveResult result = getFreshTokenResolveResult();
                                                        tokenResolver.resolve(a7.getText(), element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_IN), result);
                                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                                        if (resolvedObject == null) {
                                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a7).getLine(), ((org.antlr.runtime.CommonToken) a7).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a7).getStartIndex(), ((org.antlr.runtime.CommonToken) a7).getStopIndex());
                                                        }
                                                        String resolved = (String) resolvedObject;
                                                        org.emftext.language.office.Office proxy = org.emftext.language.office.OfficeFactory.eINSTANCE.createOffice();
                                                        collectHiddenTokens(element);
                                                        registerContextDependentProxy(new org.emftext.language.office.resource.office.mopp.OfficeContextDependentURIFragmentFactory<org.emftext.language.office.Employee, org.emftext.language.office.Office>(getReferenceResolverSwitch() == null ? null : getReferenceResolverSwitch().getEmployeeWorksInReferenceResolver()), element, (org.eclipse.emf.ecore.EReference) element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_IN), resolved, proxy);
                                                        if (proxy != null) {
                                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_IN), proxy);
                                                        }
                                                        collectHiddenTokens(element);
                                                        copyLocalizationInfos((CommonToken) a7, element);
                                                        copyLocalizationInfos((CommonToken) a7, proxy);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    case 3:
                                        {
                                            if (state.backtracking == 0) {
                                            }
                                            a8 = (Token) match(input, 20, FOLLOW_20_in_parse_org_emftext_language_office_Employee626);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a8, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            a9 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_office_Employee643);
                                            if (state.failed) return element;
                                            if (state.backtracking == 0) {
                                                if (element == null) {
                                                    element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                }
                                                collectHiddenTokens(element);
                                                copyLocalizationInfos((CommonToken) a9, element);
                                            }
                                            if (state.backtracking == 0) {
                                            }
                                            {
                                                a10 = (Token) match(input, TEXT, FOLLOW_TEXT_in_parse_org_emftext_language_office_Employee665);
                                                if (state.failed) return element;
                                                if (state.backtracking == 0) {
                                                    if (terminateParsing) {
                                                        throw new org.emftext.language.office.resource.office.mopp.OfficeTerminateParsingException();
                                                    }
                                                    if (element == null) {
                                                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                    }
                                                    if (a10 != null) {
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("TEXT");
                                                        tokenResolver.setOptions(getOptions());
                                                        org.emftext.language.office.resource.office.IOfficeTokenResolveResult result = getFreshTokenResolveResult();
                                                        tokenResolver.resolve(a10.getText(), element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_WITH), result);
                                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                                        if (resolvedObject == null) {
                                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a10).getLine(), ((org.antlr.runtime.CommonToken) a10).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a10).getStartIndex(), ((org.antlr.runtime.CommonToken) a10).getStopIndex());
                                                        }
                                                        String resolved = (String) resolvedObject;
                                                        org.emftext.language.office.Employee proxy = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                                                        collectHiddenTokens(element);
                                                        registerContextDependentProxy(new org.emftext.language.office.resource.office.mopp.OfficeContextDependentURIFragmentFactory<org.emftext.language.office.Employee, org.emftext.language.office.Employee>(getReferenceResolverSwitch() == null ? null : getReferenceResolverSwitch().getEmployeeWorksWithReferenceResolver()), element, (org.eclipse.emf.ecore.EReference) element.eClass().getEStructuralFeature(org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_WITH), resolved, proxy);
                                                        if (proxy != null) {
                                                            addObjectToList(element, org.emftext.language.office.OfficePackage.EMPLOYEE__WORKS_WITH, proxy);
                                                        }
                                                        collectHiddenTokens(element);
                                                        copyLocalizationInfos((CommonToken) a10, element);
                                                        copyLocalizationInfos((CommonToken) a10, proxy);
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                            break;
                        default:
                            break loop5;
                    }
                } while (true);
                if (state.backtracking == 0) {
                }
                if (state.backtracking == 0) {
                }
                a11 = (Token) match(input, 16, FOLLOW_16_in_parse_org_emftext_language_office_Employee698);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.office.OfficeFactory.eINSTANCE.createEmployee();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a11, element);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 4, parse_org_emftext_language_office_Employee_StartIndex);
            }
        }
        return element;
    }

    public final org.emftext.language.office.OfficeElement parse_org_emftext_language_office_OfficeElement() throws RecognitionException {
        org.emftext.language.office.OfficeElement element = null;
        int parse_org_emftext_language_office_OfficeElement_StartIndex = input.index();
        org.emftext.language.office.Office c0 = null;
        org.emftext.language.office.Employee c1 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 5)) {
                return element;
            }
            int alt6 = 2;
            int LA6_0 = input.LA(1);
            if ((LA6_0 == 17)) {
                alt6 = 1;
            } else if ((LA6_0 == 18)) {
                alt6 = 2;
            } else {
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
                        pushFollow(FOLLOW_parse_org_emftext_language_office_Office_in_parse_org_emftext_language_office_OfficeElement718);
                        c0 = parse_org_emftext_language_office_Office();
                        state._fsp--;
                        if (state.failed) return element;
                        if (state.backtracking == 0) {
                            element = c0;
                        }
                    }
                    break;
                case 2:
                    {
                        pushFollow(FOLLOW_parse_org_emftext_language_office_Employee_in_parse_org_emftext_language_office_OfficeElement726);
                        c1 = parse_org_emftext_language_office_Employee();
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
                memoize(input, 5, parse_org_emftext_language_office_OfficeElement_StartIndex);
            }
        }
        return element;
    }

    public static final BitSet FOLLOW_parse_org_emftext_language_office_OfficeModel_in_start82 = new BitSet(new long[] { 0x0000000000000000L });

    public static final BitSet FOLLOW_EOF_in_start87 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_11_in_parse_org_emftext_language_office_OfficeModel110 = new BitSet(new long[] { 0x0000000000001000L });

    public static final BitSet FOLLOW_12_in_parse_org_emftext_language_office_OfficeModel121 = new BitSet(new long[] { 0x000000000001A000L });

    public static final BitSet FOLLOW_13_in_parse_org_emftext_language_office_OfficeModel146 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_OfficeModel163 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_OfficeModel185 = new BitSet(new long[] { 0x000000000001A000L });

    public static final BitSet FOLLOW_15_in_parse_org_emftext_language_office_OfficeModel218 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_OfficeModel235 = new BitSet(new long[] { 0x0000000000060000L });

    public static final BitSet FOLLOW_parse_org_emftext_language_office_OfficeElement_in_parse_org_emftext_language_office_OfficeModel257 = new BitSet(new long[] { 0x000000000001A000L });

    public static final BitSet FOLLOW_16_in_parse_org_emftext_language_office_OfficeModel286 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_17_in_parse_org_emftext_language_office_Office312 = new BitSet(new long[] { 0x0000000000001000L });

    public static final BitSet FOLLOW_12_in_parse_org_emftext_language_office_Office323 = new BitSet(new long[] { 0x0000000000012000L });

    public static final BitSet FOLLOW_13_in_parse_org_emftext_language_office_Office348 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_Office365 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_Office387 = new BitSet(new long[] { 0x0000000000012000L });

    public static final BitSet FOLLOW_16_in_parse_org_emftext_language_office_Office420 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_18_in_parse_org_emftext_language_office_Employee446 = new BitSet(new long[] { 0x0000000000001000L });

    public static final BitSet FOLLOW_12_in_parse_org_emftext_language_office_Employee457 = new BitSet(new long[] { 0x0000000000192000L });

    public static final BitSet FOLLOW_13_in_parse_org_emftext_language_office_Employee482 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_Employee499 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_office_Employee521 = new BitSet(new long[] { 0x0000000000192000L });

    public static final BitSet FOLLOW_19_in_parse_org_emftext_language_office_Employee554 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_Employee571 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_TEXT_in_parse_org_emftext_language_office_Employee593 = new BitSet(new long[] { 0x0000000000192000L });

    public static final BitSet FOLLOW_20_in_parse_org_emftext_language_office_Employee626 = new BitSet(new long[] { 0x0000000000004000L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_office_Employee643 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_TEXT_in_parse_org_emftext_language_office_Employee665 = new BitSet(new long[] { 0x0000000000192000L });

    public static final BitSet FOLLOW_16_in_parse_org_emftext_language_office_Employee698 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_office_Office_in_parse_org_emftext_language_office_OfficeElement718 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_parse_org_emftext_language_office_Employee_in_parse_org_emftext_language_office_OfficeElement726 = new BitSet(new long[] { 0x0000000000000002L });
}
