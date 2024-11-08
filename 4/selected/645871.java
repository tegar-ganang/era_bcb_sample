package org.emftext.language.models.resource.model.mopp;

import org.antlr.runtime.*;
import java.util.HashMap;

public class ModelParser extends ModelANTLRParserBase {

    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "QUOTED_34_34", "NUMBER", "BLOND", "BLUE", "RED", "TEXT", "SL_COMMENT", "ML_COMMENT", "WHITESPACE", "LINEBREAKS", "'prototype'", "'['", "','", "']'", "'{'", "'hair'", "';'", "'eyes'", "'lips'", "'legs'", "'}'" };

    public static final int T__24 = 24;

    public static final int T__23 = 23;

    public static final int T__22 = 22;

    public static final int T__21 = 21;

    public static final int T__20 = 20;

    public static final int NUMBER = 5;

    public static final int WHITESPACE = 12;

    public static final int TEXT = 9;

    public static final int LINEBREAKS = 13;

    public static final int EOF = -1;

    public static final int RED = 8;

    public static final int BLUE = 7;

    public static final int ML_COMMENT = 11;

    public static final int T__19 = 19;

    public static final int T__16 = 16;

    public static final int T__15 = 15;

    public static final int BLOND = 6;

    public static final int T__18 = 18;

    public static final int T__17 = 17;

    public static final int T__14 = 14;

    public static final int SL_COMMENT = 10;

    public static final int QUOTED_34_34 = 4;

    public ModelParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public ModelParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
        this.state.ruleMemo = new HashMap[8 + 1];
    }

    public String[] getTokenNames() {
        return ModelParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "D:\\TuBa\\EclipseWorkspacesNew\\TuBa_WS\\org.emftext.language.models.resource.model\\src-gen\\org\\emftext\\language\\models\\resource\\model\\mopp\\Model.g";
    }

    private org.emftext.language.models.resource.model.IModelTokenResolverFactory tokenResolverFactory = new org.emftext.language.models.resource.model.mopp.ModelTokenResolverFactory();

    private int lastPosition;

    private org.emftext.language.models.resource.model.mopp.ModelTokenResolveResult tokenResolveResult = new org.emftext.language.models.resource.model.mopp.ModelTokenResolveResult();

    private boolean rememberExpectedElements = false;

    private java.lang.Object parseToIndexTypeObject;

    private int lastTokenIndex = 0;

    private boolean reachedIndex = false;

    private java.util.List<org.emftext.language.models.resource.model.IModelExpectedElement> expectedElements = new java.util.ArrayList<org.emftext.language.models.resource.model.IModelExpectedElement>();

    private int lastIndex = -1;

    private int mismatchedTokenRecoveryTries = 0;

    private java.util.Map<?, ?> options;

    protected java.util.List<org.antlr.runtime.RecognitionException> lexerExceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<org.antlr.runtime.RecognitionException>());

    protected java.util.List<java.lang.Integer> lexerExceptionsPosition = java.util.Collections.synchronizedList(new java.util.ArrayList<java.lang.Integer>());

    private int stopIncludingHiddenTokens;

    private int stopExcludingHiddenTokens;

    private java.util.Collection<org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>> postParseCommands;

    private boolean terminateParsing;

    protected void addErrorToResource(final java.lang.String errorMessage, final int line, final int charPositionInLine, final int startIndex, final int stopIndex) {
        postParseCommands.add(new org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>() {

            public boolean execute(org.emftext.language.models.resource.model.IModelTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.addProblem(new org.emftext.language.models.resource.model.IModelProblem() {

                    public org.emftext.language.models.resource.model.ModelEProblemType getType() {
                        return org.emftext.language.models.resource.model.ModelEProblemType.ERROR;
                    }

                    public java.lang.String getMessage() {
                        return errorMessage;
                    }
                }, line, charPositionInLine, startIndex, stopIndex);
                return true;
            }
        });
    }

    public void addExpectedElement(org.emftext.language.models.resource.model.IModelExpectedElement expectedElement, java.lang.String message) {
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

    protected void addMapEntry(org.eclipse.emf.ecore.EObject element, org.eclipse.emf.ecore.EStructuralFeature structuralFeature, org.emftext.language.models.resource.model.mopp.ModelDummyEObject dummy) {
        java.lang.Object value = element.eGet(structuralFeature);
        java.lang.Object mapKey = dummy.getValueByName("key");
        java.lang.Object mapValue = dummy.getValueByName("value");
        if (value instanceof org.eclipse.emf.common.util.EMap<?, ?>) {
            org.eclipse.emf.common.util.EMap<java.lang.Object, java.lang.Object> valueMap = org.emftext.language.models.resource.model.util.ModelMapUtil.castToEMap(value);
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
            assert (object instanceof org.emftext.language.models.resource.model.mopp.ModelDummyEObject);
            org.emftext.language.models.resource.model.mopp.ModelDummyEObject dummy = (org.emftext.language.models.resource.model.mopp.ModelDummyEObject) object;
            org.eclipse.emf.ecore.EObject newEObject = dummy.applyTo(currentTarget);
            currentTarget = newEObject;
        }
        return currentTarget;
    }

    protected void collectHiddenTokens(org.eclipse.emf.ecore.EObject element) {
        int currentPos = getTokenStream().index();
        if (currentPos == 0) {
            return;
        }
        int endPos = currentPos - 1;
        for (; endPos >= lastPosition; endPos--) {
            org.antlr.runtime.Token token = getTokenStream().get(endPos);
            int _channel = token.getChannel();
            if (_channel != 99) {
                break;
            }
        }
        for (int pos = lastPosition; pos < endPos; pos++) {
            org.antlr.runtime.Token token = getTokenStream().get(pos);
            int _channel = token.getChannel();
            if (_channel == 99) {
                if (token.getType() == ModelLexer.SL_COMMENT) {
                    org.eclipse.emf.ecore.EStructuralFeature feature = element.eClass().getEStructuralFeature("comments");
                    if (feature != null) {
                        org.emftext.language.models.resource.model.IModelTokenResolver resolvedResolver = tokenResolverFactory.createCollectInTokenResolver("comments");
                        resolvedResolver.setOptions(getOptions());
                        org.emftext.language.models.resource.model.IModelTokenResolveResult resolvedResult = getFreshTokenResolveResult();
                        resolvedResolver.resolve(token.getText(), feature, resolvedResult);
                        java.lang.Object resolvedObject = resolvedResult.getResolvedToken();
                        if (resolvedObject == null) {
                            addErrorToResource(resolvedResult.getErrorMessage(), ((org.antlr.runtime.CommonToken) token).getLine(), ((org.antlr.runtime.CommonToken) token).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) token).getStartIndex(), ((org.antlr.runtime.CommonToken) token).getStopIndex());
                        }
                        if (java.lang.String.class.isInstance(resolvedObject)) {
                            ((java.util.List) element.eGet(feature)).add((java.lang.String) resolvedObject);
                        } else {
                            System.out.println("WARNING: Attribute comments for token " + token + " has wrong type in element " + element + " (expected java.lang.String).");
                        }
                    } else {
                        System.out.println("WARNING: Attribute comments for token " + token + " was not found in element " + element + ".");
                    }
                }
                if (token.getType() == ModelLexer.ML_COMMENT) {
                    org.eclipse.emf.ecore.EStructuralFeature feature = element.eClass().getEStructuralFeature("comments");
                    if (feature != null) {
                        org.emftext.language.models.resource.model.IModelTokenResolver resolvedResolver = tokenResolverFactory.createCollectInTokenResolver("comments");
                        resolvedResolver.setOptions(getOptions());
                        org.emftext.language.models.resource.model.IModelTokenResolveResult resolvedResult = getFreshTokenResolveResult();
                        resolvedResolver.resolve(token.getText(), feature, resolvedResult);
                        java.lang.Object resolvedObject = resolvedResult.getResolvedToken();
                        if (resolvedObject == null) {
                            addErrorToResource(resolvedResult.getErrorMessage(), ((org.antlr.runtime.CommonToken) token).getLine(), ((org.antlr.runtime.CommonToken) token).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) token).getStartIndex(), ((org.antlr.runtime.CommonToken) token).getStopIndex());
                        }
                        if (java.lang.String.class.isInstance(resolvedObject)) {
                            ((java.util.List) element.eGet(feature)).add((java.lang.String) resolvedObject);
                        } else {
                            System.out.println("WARNING: Attribute comments for token " + token + " has wrong type in element " + element + " (expected java.lang.String).");
                        }
                    } else {
                        System.out.println("WARNING: Attribute comments for token " + token + " was not found in element " + element + ".");
                    }
                }
            }
        }
        lastPosition = (endPos < 0 ? 0 : endPos);
    }

    protected void copyLocalizationInfos(final org.eclipse.emf.ecore.EObject source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>() {

            public boolean execute(org.emftext.language.models.resource.model.IModelTextResource resource) {
                if (resource == null) {
                    return true;
                }
                org.emftext.language.models.resource.model.IModelLocationMap locationMap = resource.getLocationMap();
                locationMap.setCharStart(target, locationMap.getCharStart(source));
                locationMap.setCharEnd(target, locationMap.getCharEnd(source));
                locationMap.setColumn(target, locationMap.getColumn(source));
                locationMap.setLine(target, locationMap.getLine(source));
                return true;
            }
        });
    }

    protected void copyLocalizationInfos(final org.antlr.runtime.CommonToken source, final org.eclipse.emf.ecore.EObject target) {
        postParseCommands.add(new org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>() {

            public boolean execute(org.emftext.language.models.resource.model.IModelTextResource resource) {
                if (resource == null) {
                    return true;
                }
                org.emftext.language.models.resource.model.IModelLocationMap locationMap = resource.getLocationMap();
                locationMap.setCharStart(target, source.getStartIndex());
                locationMap.setCharEnd(target, source.getStopIndex());
                locationMap.setColumn(target, source.getCharPositionInLine());
                locationMap.setLine(target, source.getLine());
                return true;
            }
        });
    }

    public org.emftext.language.models.resource.model.IModelTextParser createInstance(java.io.InputStream actualInputStream, java.lang.String encoding) {
        try {
            if (encoding == null) {
                return new ModelParser(new org.antlr.runtime.CommonTokenStream(new ModelLexer(new org.antlr.runtime.ANTLRInputStream(actualInputStream))));
            } else {
                return new ModelParser(new org.antlr.runtime.CommonTokenStream(new ModelLexer(new org.antlr.runtime.ANTLRInputStream(actualInputStream, encoding))));
            }
        } catch (java.io.IOException e) {
            org.emftext.language.models.resource.model.mopp.ModelPlugin.logError("Error while creating parser.", e);
            return null;
        }
    }

    public ModelParser() {
        super(null);
    }

    protected org.eclipse.emf.ecore.EObject doParse() throws org.antlr.runtime.RecognitionException {
        lastPosition = 0;
        ((ModelLexer) getTokenStream().getTokenSource()).lexerExceptions = lexerExceptions;
        ((ModelLexer) getTokenStream().getTokenSource()).lexerExceptionsPosition = lexerExceptionsPosition;
        java.lang.Object typeObject = getTypeObject();
        if (typeObject == null) {
            return start();
        } else if (typeObject instanceof org.eclipse.emf.ecore.EClass) {
            org.eclipse.emf.ecore.EClass type = (org.eclipse.emf.ecore.EClass) typeObject;
            if (type.getInstanceClass() == org.emftext.language.models.Model.class) {
                return parse_org_emftext_language_models_Model();
            }
        }
        throw new org.emftext.language.models.resource.model.mopp.ModelUnexpectedContentTypeException(typeObject);
    }

    private org.emftext.language.models.resource.model.mopp.ModelTokenResolveResult getFreshTokenResolveResult() {
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

    public org.emftext.language.models.resource.model.mopp.ModelMetaInformation getMetaInformation() {
        return new org.emftext.language.models.resource.model.mopp.ModelMetaInformation();
    }

    public java.lang.Object getParseToIndexTypeObject() {
        return parseToIndexTypeObject;
    }

    protected org.emftext.language.models.resource.model.mopp.ModelReferenceResolverSwitch getReferenceResolverSwitch() {
        return (org.emftext.language.models.resource.model.mopp.ModelReferenceResolverSwitch) getMetaInformation().getReferenceResolverSwitch();
    }

    protected java.lang.Object getTypeObject() {
        java.lang.Object typeObject = getParseToIndexTypeObject();
        if (typeObject != null) {
            return typeObject;
        }
        java.util.Map<?, ?> options = getOptions();
        if (options != null) {
            typeObject = options.get(org.emftext.language.models.resource.model.IModelOptions.RESOURCE_CONTENT_TYPE);
        }
        return typeObject;
    }

    public org.emftext.language.models.resource.model.IModelParseResult parse() {
        terminateParsing = false;
        postParseCommands = new java.util.ArrayList<org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>>();
        org.emftext.language.models.resource.model.mopp.ModelParseResult parseResult = new org.emftext.language.models.resource.model.mopp.ModelParseResult();
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

    public java.util.List<org.emftext.language.models.resource.model.IModelExpectedElement> parseToExpectedElements(org.eclipse.emf.ecore.EClass type) {
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

    protected <ContainerType extends org.eclipse.emf.ecore.EObject, ReferenceType extends org.eclipse.emf.ecore.EObject> void registerContextDependentProxy(final org.emftext.language.models.resource.model.mopp.ModelContextDependentURIFragmentFactory<ContainerType, ReferenceType> factory, final ContainerType element, final org.eclipse.emf.ecore.EReference reference, final String id, final org.eclipse.emf.ecore.EObject proxy) {
        postParseCommands.add(new org.emftext.language.models.resource.model.IModelCommand<org.emftext.language.models.resource.model.IModelTextResource>() {

            public boolean execute(org.emftext.language.models.resource.model.IModelTextResource resource) {
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
                tokenName = org.emftext.language.models.resource.model.util.ModelStringUtil.formatTokenName(tokenName);
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
        org.emftext.language.models.Model c0 = null;
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
                    pushFollow(FOLLOW_parse_org_emftext_language_models_Model_in_start82);
                    c0 = parse_org_emftext_language_models_Model();
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

    public final org.emftext.language.models.Model parse_org_emftext_language_models_Model() throws RecognitionException {
        org.emftext.language.models.Model element = null;
        int parse_org_emftext_language_models_Model_StartIndex = input.index();
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
        Token a12 = null;
        Token a13 = null;
        Token a14 = null;
        Token a15 = null;
        Token a16 = null;
        Token a17 = null;
        Token a18 = null;
        Token a19 = null;
        Token a20 = null;
        Token a21 = null;
        Token a22 = null;
        Token a23 = null;
        Token a24 = null;
        Token a25 = null;
        Token a26 = null;
        Token a27 = null;
        Token a28 = null;
        try {
            if (state.backtracking > 0 && alreadyParsedRule(input, 2)) {
                return element;
            }
            {
                if (state.backtracking == 0) {
                }
                a0 = (Token) match(input, 14, FOLLOW_14_in_parse_org_emftext_language_models_Model110);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a0, element);
                }
                if (state.backtracking == 0) {
                }
                {
                    a1 = (Token) match(input, QUOTED_34_34, FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_models_Model124);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                        }
                        if (a1 != null) {
                            org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("QUOTED_34_34");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a1.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__NAME), result);
                            java.lang.Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a1).getLine(), ((org.antlr.runtime.CommonToken) a1).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a1).getStartIndex(), ((org.antlr.runtime.CommonToken) a1).getStopIndex());
                            }
                            java.lang.String resolved = (java.lang.String) resolvedObject;
                            if (resolved != null) {
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__NAME), resolved);
                            }
                            collectHiddenTokens(element);
                            copyLocalizationInfos((CommonToken) a1, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                }
                a2 = (Token) match(input, 15, FOLLOW_15_in_parse_org_emftext_language_models_Model139);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a2, element);
                }
                if (state.backtracking == 0) {
                }
                {
                    a3 = (Token) match(input, NUMBER, FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model153);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                        }
                        if (a3 != null) {
                            org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("NUMBER");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a3.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE1), result);
                            java.lang.Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a3).getLine(), ((org.antlr.runtime.CommonToken) a3).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a3).getStartIndex(), ((org.antlr.runtime.CommonToken) a3).getStopIndex());
                            }
                            java.lang.Integer resolved = (java.lang.Integer) resolvedObject;
                            if (resolved != null) {
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE1), resolved);
                            }
                            collectHiddenTokens(element);
                            copyLocalizationInfos((CommonToken) a3, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                }
                a4 = (Token) match(input, 16, FOLLOW_16_in_parse_org_emftext_language_models_Model168);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a4, element);
                }
                if (state.backtracking == 0) {
                }
                {
                    a5 = (Token) match(input, NUMBER, FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model182);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                        }
                        if (a5 != null) {
                            org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("NUMBER");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a5.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE2), result);
                            java.lang.Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a5).getLine(), ((org.antlr.runtime.CommonToken) a5).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a5).getStartIndex(), ((org.antlr.runtime.CommonToken) a5).getStopIndex());
                            }
                            java.lang.Integer resolved = (java.lang.Integer) resolvedObject;
                            if (resolved != null) {
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE2), resolved);
                            }
                            collectHiddenTokens(element);
                            copyLocalizationInfos((CommonToken) a5, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                }
                a6 = (Token) match(input, 16, FOLLOW_16_in_parse_org_emftext_language_models_Model197);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a6, element);
                }
                if (state.backtracking == 0) {
                }
                {
                    a7 = (Token) match(input, NUMBER, FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model211);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                        }
                        if (a7 != null) {
                            org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("NUMBER");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a7.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE3), result);
                            java.lang.Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a7).getLine(), ((org.antlr.runtime.CommonToken) a7).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a7).getStartIndex(), ((org.antlr.runtime.CommonToken) a7).getStopIndex());
                            }
                            java.lang.Integer resolved = (java.lang.Integer) resolvedObject;
                            if (resolved != null) {
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__SIZE3), resolved);
                            }
                            collectHiddenTokens(element);
                            copyLocalizationInfos((CommonToken) a7, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                }
                a8 = (Token) match(input, 17, FOLLOW_17_in_parse_org_emftext_language_models_Model226);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a8, element);
                }
                if (state.backtracking == 0) {
                }
                a9 = (Token) match(input, 18, FOLLOW_18_in_parse_org_emftext_language_models_Model237);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a9, element);
                }
                if (state.backtracking == 0) {
                }
                int alt1 = 3;
                switch(input.LA(1)) {
                    case BLOND:
                        {
                            alt1 = 1;
                        }
                        break;
                    case BLUE:
                        {
                            alt1 = 2;
                        }
                        break;
                    case RED:
                        {
                            alt1 = 3;
                        }
                        break;
                    default:
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
                            {
                                a10 = (Token) match(input, BLOND, FOLLOW_BLOND_in_parse_org_emftext_language_models_Model258);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a10 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLOND");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a10.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a10).getLine(), ((org.antlr.runtime.CommonToken) a10).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a10).getStartIndex(), ((org.antlr.runtime.CommonToken) a10).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a10, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 2:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a11 = (Token) match(input, BLUE, FOLLOW_BLUE_in_parse_org_emftext_language_models_Model287);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a11 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLUE");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a11.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a11).getLine(), ((org.antlr.runtime.CommonToken) a11).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a11).getStartIndex(), ((org.antlr.runtime.CommonToken) a11).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a11, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 3:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a12 = (Token) match(input, RED, FOLLOW_RED_in_parse_org_emftext_language_models_Model316);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a12 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("RED");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a12.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a12).getLine(), ((org.antlr.runtime.CommonToken) a12).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a12).getStartIndex(), ((org.antlr.runtime.CommonToken) a12).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__HAIR), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a12, element);
                                    }
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                }
                a13 = (Token) match(input, 19, FOLLOW_19_in_parse_org_emftext_language_models_Model338);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a13, element);
                }
                if (state.backtracking == 0) {
                }
                a14 = (Token) match(input, 20, FOLLOW_20_in_parse_org_emftext_language_models_Model349);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a14, element);
                }
                if (state.backtracking == 0) {
                }
                int alt2 = 3;
                switch(input.LA(1)) {
                    case BLOND:
                        {
                            alt2 = 1;
                        }
                        break;
                    case BLUE:
                        {
                            alt2 = 2;
                        }
                        break;
                    case RED:
                        {
                            alt2 = 3;
                        }
                        break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return element;
                        }
                        NoViableAltException nvae = new NoViableAltException("", 2, 0, input);
                        throw nvae;
                }
                switch(alt2) {
                    case 1:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a15 = (Token) match(input, BLOND, FOLLOW_BLOND_in_parse_org_emftext_language_models_Model370);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a15 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLOND");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a15.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a15).getLine(), ((org.antlr.runtime.CommonToken) a15).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a15).getStartIndex(), ((org.antlr.runtime.CommonToken) a15).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a15, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 2:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a16 = (Token) match(input, BLUE, FOLLOW_BLUE_in_parse_org_emftext_language_models_Model399);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a16 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLUE");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a16.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a16).getLine(), ((org.antlr.runtime.CommonToken) a16).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a16).getStartIndex(), ((org.antlr.runtime.CommonToken) a16).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a16, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 3:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a17 = (Token) match(input, RED, FOLLOW_RED_in_parse_org_emftext_language_models_Model428);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a17 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("RED");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a17.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a17).getLine(), ((org.antlr.runtime.CommonToken) a17).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a17).getStartIndex(), ((org.antlr.runtime.CommonToken) a17).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__EYES), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a17, element);
                                    }
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                }
                a18 = (Token) match(input, 21, FOLLOW_21_in_parse_org_emftext_language_models_Model450);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a18, element);
                }
                if (state.backtracking == 0) {
                }
                a19 = (Token) match(input, 20, FOLLOW_20_in_parse_org_emftext_language_models_Model461);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a19, element);
                }
                if (state.backtracking == 0) {
                }
                int alt3 = 3;
                switch(input.LA(1)) {
                    case BLOND:
                        {
                            alt3 = 1;
                        }
                        break;
                    case BLUE:
                        {
                            alt3 = 2;
                        }
                        break;
                    case RED:
                        {
                            alt3 = 3;
                        }
                        break;
                    default:
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
                            if (state.backtracking == 0) {
                            }
                            {
                                a20 = (Token) match(input, BLOND, FOLLOW_BLOND_in_parse_org_emftext_language_models_Model482);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a20 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLOND");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a20.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a20).getLine(), ((org.antlr.runtime.CommonToken) a20).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a20).getStartIndex(), ((org.antlr.runtime.CommonToken) a20).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a20, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 2:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a21 = (Token) match(input, BLUE, FOLLOW_BLUE_in_parse_org_emftext_language_models_Model511);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a21 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("BLUE");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a21.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a21).getLine(), ((org.antlr.runtime.CommonToken) a21).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a21).getStartIndex(), ((org.antlr.runtime.CommonToken) a21).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a21, element);
                                    }
                                }
                            }
                        }
                        break;
                    case 3:
                        {
                            if (state.backtracking == 0) {
                            }
                            {
                                a22 = (Token) match(input, RED, FOLLOW_RED_in_parse_org_emftext_language_models_Model540);
                                if (state.failed) return element;
                                if (state.backtracking == 0) {
                                    if (terminateParsing) {
                                        throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                                    }
                                    if (element == null) {
                                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                                    }
                                    if (a22 != null) {
                                        org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("RED");
                                        tokenResolver.setOptions(getOptions());
                                        org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                                        tokenResolver.resolve(a22.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), result);
                                        java.lang.Object resolvedObject = result.getResolvedToken();
                                        if (resolvedObject == null) {
                                            addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a22).getLine(), ((org.antlr.runtime.CommonToken) a22).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a22).getStartIndex(), ((org.antlr.runtime.CommonToken) a22).getStopIndex());
                                        }
                                        org.emftext.language.models.Color resolved = (org.emftext.language.models.Color) resolvedObject;
                                        if (resolved != null) {
                                            element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LIPS), resolved);
                                        }
                                        collectHiddenTokens(element);
                                        copyLocalizationInfos((CommonToken) a22, element);
                                    }
                                }
                            }
                        }
                        break;
                }
                if (state.backtracking == 0) {
                }
                a23 = (Token) match(input, 22, FOLLOW_22_in_parse_org_emftext_language_models_Model562);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a23, element);
                }
                if (state.backtracking == 0) {
                }
                a24 = (Token) match(input, 20, FOLLOW_20_in_parse_org_emftext_language_models_Model573);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a24, element);
                }
                if (state.backtracking == 0) {
                }
                {
                    a25 = (Token) match(input, TEXT, FOLLOW_TEXT_in_parse_org_emftext_language_models_Model587);
                    if (state.failed) return element;
                    if (state.backtracking == 0) {
                        if (terminateParsing) {
                            throw new org.emftext.language.models.resource.model.mopp.ModelTerminateParsingException();
                        }
                        if (element == null) {
                            element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                        }
                        if (a25 != null) {
                            org.emftext.language.models.resource.model.IModelTokenResolver tokenResolver = tokenResolverFactory.createTokenResolver("TEXT");
                            tokenResolver.setOptions(getOptions());
                            org.emftext.language.models.resource.model.IModelTokenResolveResult result = getFreshTokenResolveResult();
                            tokenResolver.resolve(a25.getText(), element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LEGS), result);
                            java.lang.Object resolvedObject = result.getResolvedToken();
                            if (resolvedObject == null) {
                                addErrorToResource(result.getErrorMessage(), ((org.antlr.runtime.CommonToken) a25).getLine(), ((org.antlr.runtime.CommonToken) a25).getCharPositionInLine(), ((org.antlr.runtime.CommonToken) a25).getStartIndex(), ((org.antlr.runtime.CommonToken) a25).getStopIndex());
                            }
                            org.emftext.language.models.Size resolved = (org.emftext.language.models.Size) resolvedObject;
                            if (resolved != null) {
                                element.eSet(element.eClass().getEStructuralFeature(org.emftext.language.models.ModelsPackage.MODEL__LEGS), resolved);
                            }
                            collectHiddenTokens(element);
                            copyLocalizationInfos((CommonToken) a25, element);
                        }
                    }
                }
                if (state.backtracking == 0) {
                }
                a26 = (Token) match(input, 23, FOLLOW_23_in_parse_org_emftext_language_models_Model602);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a26, element);
                }
                if (state.backtracking == 0) {
                }
                a27 = (Token) match(input, 20, FOLLOW_20_in_parse_org_emftext_language_models_Model613);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a27, element);
                }
                if (state.backtracking == 0) {
                }
                a28 = (Token) match(input, 24, FOLLOW_24_in_parse_org_emftext_language_models_Model624);
                if (state.failed) return element;
                if (state.backtracking == 0) {
                    if (element == null) {
                        element = org.emftext.language.models.ModelsFactory.eINSTANCE.createModel();
                    }
                    collectHiddenTokens(element);
                    copyLocalizationInfos((CommonToken) a28, element);
                }
            }
        } catch (RecognitionException re) {
            reportError(re);
            recover(input, re);
        } finally {
            if (state.backtracking > 0) {
                memoize(input, 2, parse_org_emftext_language_models_Model_StartIndex);
            }
        }
        return element;
    }

    public static final BitSet FOLLOW_parse_org_emftext_language_models_Model_in_start82 = new BitSet(new long[] { 0x0000000000000000L });

    public static final BitSet FOLLOW_EOF_in_start87 = new BitSet(new long[] { 0x0000000000000002L });

    public static final BitSet FOLLOW_14_in_parse_org_emftext_language_models_Model110 = new BitSet(new long[] { 0x0000000000000010L });

    public static final BitSet FOLLOW_QUOTED_34_34_in_parse_org_emftext_language_models_Model124 = new BitSet(new long[] { 0x0000000000008000L });

    public static final BitSet FOLLOW_15_in_parse_org_emftext_language_models_Model139 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model153 = new BitSet(new long[] { 0x0000000000010000L });

    public static final BitSet FOLLOW_16_in_parse_org_emftext_language_models_Model168 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model182 = new BitSet(new long[] { 0x0000000000010000L });

    public static final BitSet FOLLOW_16_in_parse_org_emftext_language_models_Model197 = new BitSet(new long[] { 0x0000000000000020L });

    public static final BitSet FOLLOW_NUMBER_in_parse_org_emftext_language_models_Model211 = new BitSet(new long[] { 0x0000000000020000L });

    public static final BitSet FOLLOW_17_in_parse_org_emftext_language_models_Model226 = new BitSet(new long[] { 0x0000000000040000L });

    public static final BitSet FOLLOW_18_in_parse_org_emftext_language_models_Model237 = new BitSet(new long[] { 0x00000000000001C0L });

    public static final BitSet FOLLOW_BLOND_in_parse_org_emftext_language_models_Model258 = new BitSet(new long[] { 0x0000000000080000L });

    public static final BitSet FOLLOW_BLUE_in_parse_org_emftext_language_models_Model287 = new BitSet(new long[] { 0x0000000000080000L });

    public static final BitSet FOLLOW_RED_in_parse_org_emftext_language_models_Model316 = new BitSet(new long[] { 0x0000000000080000L });

    public static final BitSet FOLLOW_19_in_parse_org_emftext_language_models_Model338 = new BitSet(new long[] { 0x0000000000100000L });

    public static final BitSet FOLLOW_20_in_parse_org_emftext_language_models_Model349 = new BitSet(new long[] { 0x00000000000001C0L });

    public static final BitSet FOLLOW_BLOND_in_parse_org_emftext_language_models_Model370 = new BitSet(new long[] { 0x0000000000200000L });

    public static final BitSet FOLLOW_BLUE_in_parse_org_emftext_language_models_Model399 = new BitSet(new long[] { 0x0000000000200000L });

    public static final BitSet FOLLOW_RED_in_parse_org_emftext_language_models_Model428 = new BitSet(new long[] { 0x0000000000200000L });

    public static final BitSet FOLLOW_21_in_parse_org_emftext_language_models_Model450 = new BitSet(new long[] { 0x0000000000100000L });

    public static final BitSet FOLLOW_20_in_parse_org_emftext_language_models_Model461 = new BitSet(new long[] { 0x00000000000001C0L });

    public static final BitSet FOLLOW_BLOND_in_parse_org_emftext_language_models_Model482 = new BitSet(new long[] { 0x0000000000400000L });

    public static final BitSet FOLLOW_BLUE_in_parse_org_emftext_language_models_Model511 = new BitSet(new long[] { 0x0000000000400000L });

    public static final BitSet FOLLOW_RED_in_parse_org_emftext_language_models_Model540 = new BitSet(new long[] { 0x0000000000400000L });

    public static final BitSet FOLLOW_22_in_parse_org_emftext_language_models_Model562 = new BitSet(new long[] { 0x0000000000100000L });

    public static final BitSet FOLLOW_20_in_parse_org_emftext_language_models_Model573 = new BitSet(new long[] { 0x0000000000000200L });

    public static final BitSet FOLLOW_TEXT_in_parse_org_emftext_language_models_Model587 = new BitSet(new long[] { 0x0000000000800000L });

    public static final BitSet FOLLOW_23_in_parse_org_emftext_language_models_Model602 = new BitSet(new long[] { 0x0000000000100000L });

    public static final BitSet FOLLOW_20_in_parse_org_emftext_language_models_Model613 = new BitSet(new long[] { 0x0000000001000000L });

    public static final BitSet FOLLOW_24_in_parse_org_emftext_language_models_Model624 = new BitSet(new long[] { 0x0000000000000002L });
}
