package fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp;

public abstract class Ocl4tstANTLRParserBase extends org.antlr.runtime3_3_0.Parser implements fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextParser {

    /**
	 * the index of the last token that was handled by retrieveLayoutInformation()
	 */
    private int lastPosition2;

    /**
	 * a collection to store all anonymous tokens
	 */
    protected java.util.List<org.antlr.runtime3_3_0.CommonToken> anonymousTokens = new java.util.ArrayList<org.antlr.runtime3_3_0.CommonToken>();

    /**
	 * A collection that is filled with commands to be executed after parsing. This
	 * collection is cleared before parsing starts and returned as part of the parse
	 * result object.
	 */
    protected java.util.Collection<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>> postParseCommands;

    private java.util.Map<?, ?> options;

    /**
	 * A flag to indicate that the parser should stop parsing as soon as possible. The
	 * flag is set to false before parsing starts. It can be set to true by invoking
	 * the terminateParsing() method from another thread. This feature is used, when
	 * documents are parsed in the background (i.e., while editing them). In order to
	 * cancel running parsers, the parsing process can be terminated. This is done
	 * whenever a document changes, because the previous content of the document is
	 * not valid anymore and parsing the old content is not necessary any longer.
	 */
    protected boolean terminateParsing;

    /**
	 * A reusable container for the result of resolving tokens.
	 */
    private fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTokenResolveResult tokenResolveResult = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTokenResolveResult();

    public Ocl4tstANTLRParserBase(org.antlr.runtime3_3_0.TokenStream input) {
        super(input);
    }

    public Ocl4tstANTLRParserBase(org.antlr.runtime3_3_0.TokenStream input, org.antlr.runtime3_3_0.RecognizerSharedState state) {
        super(input, state);
    }

    protected void retrieveLayoutInformation(org.eclipse.emf.ecore.EObject element, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstSyntaxElement syntaxElement, Object object, boolean ignoreTokensAfterLastVisibleToken) {
        if (element == null) {
            return;
        }
        boolean isElementToStore = syntaxElement == null;
        isElementToStore |= syntaxElement instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstPlaceholder;
        isElementToStore |= syntaxElement instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstKeyword;
        isElementToStore |= syntaxElement instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstEnumerationTerminal;
        isElementToStore |= syntaxElement instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.grammar.Ocl4tstBooleanTerminal;
        if (!isElementToStore) {
            return;
        }
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter layoutInformationAdapter = getLayoutInformationAdapter(element);
        for (org.antlr.runtime3_3_0.CommonToken anonymousToken : anonymousTokens) {
            layoutInformationAdapter.addLayoutInformation(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformation(syntaxElement, object, anonymousToken.getStartIndex(), anonymousToken.getText(), null));
        }
        anonymousTokens.clear();
        int currentPos = getTokenStream().index();
        if (currentPos == 0) {
            return;
        }
        int endPos = currentPos - 1;
        if (ignoreTokensAfterLastVisibleToken) {
            for (; endPos >= this.lastPosition2; endPos--) {
                org.antlr.runtime3_3_0.Token token = getTokenStream().get(endPos);
                int _channel = token.getChannel();
                if (_channel != 99) {
                    break;
                }
            }
        }
        StringBuilder hiddenTokenText = new StringBuilder();
        StringBuilder visibleTokenText = new StringBuilder();
        org.antlr.runtime3_3_0.CommonToken firstToken = null;
        for (int pos = this.lastPosition2; pos <= endPos; pos++) {
            org.antlr.runtime3_3_0.Token token = getTokenStream().get(pos);
            if (firstToken == null) {
                firstToken = (org.antlr.runtime3_3_0.CommonToken) token;
            }
            int _channel = token.getChannel();
            if (_channel == 99) {
                hiddenTokenText.append(token.getText());
            } else {
                visibleTokenText.append(token.getText());
            }
        }
        int offset = -1;
        if (firstToken != null) {
            offset = firstToken.getStartIndex();
        }
        layoutInformationAdapter.addLayoutInformation(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformation(syntaxElement, object, offset, hiddenTokenText.toString(), visibleTokenText.toString()));
        this.lastPosition2 = (endPos < 0 ? 0 : endPos + 1);
    }

    protected fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter getLayoutInformationAdapter(org.eclipse.emf.ecore.EObject element) {
        for (org.eclipse.emf.common.notify.Adapter adapter : element.eAdapters()) {
            if (adapter instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter) {
                return (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter) adapter;
            }
        }
        fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter newAdapter = new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstLayoutInformationAdapter();
        element.eAdapters().add(newAdapter);
        return newAdapter;
    }

    protected <ContainerType extends org.eclipse.emf.ecore.EObject, ReferenceType extends org.eclipse.emf.ecore.EObject> void registerContextDependentProxy(final fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstContextDependentURIFragmentFactory<ContainerType, ReferenceType> factory, final ContainerType container, final org.eclipse.emf.ecore.EReference reference, final String id, final org.eclipse.emf.ecore.EObject proxy) {
        final int position;
        if (reference.isMany()) {
            position = ((java.util.List<?>) container.eGet(reference)).size();
        } else {
            position = -1;
        }
        postParseCommands.add(new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstCommand<fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource>() {

            public boolean execute(fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.IOcl4tstTextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.registerContextDependentProxy(factory, container, reference, id, proxy, position);
                return true;
            }
        });
    }

    protected String formatTokenName(int tokenType) {
        String tokenName = "<unknown>";
        if (tokenType < 0 || tokenType == org.antlr.runtime3_3_0.Token.EOF) {
            tokenName = "EOF";
        } else {
            if (tokenType < 0) {
                return tokenName;
            }
            tokenName = getTokenNames()[tokenType];
            tokenName = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.util.Ocl4tstStringUtil.formatTokenName(tokenName);
        }
        return tokenName;
    }

    protected java.util.Map<?, ?> getOptions() {
        return options;
    }

    public void setOptions(java.util.Map<?, ?> options) {
        this.options = options;
    }

    /**
	 * Creates a dynamic Java proxy that mimics the interface of the given class.
	 */
    @SuppressWarnings("unchecked")
    public <T> T createDynamicProxy(Class<T> clazz) {
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { clazz, org.eclipse.emf.ecore.EObject.class, org.eclipse.emf.ecore.InternalEObject.class }, new java.lang.reflect.InvocationHandler() {

            private org.eclipse.emf.ecore.EObject dummyObject = new org.eclipse.emf.ecore.impl.EObjectImpl() {
            };

            public Object invoke(Object object, java.lang.reflect.Method method, Object[] args) throws Throwable {
                java.lang.reflect.Method[] methodsInDummy = dummyObject.getClass().getMethods();
                for (java.lang.reflect.Method methodInDummy : methodsInDummy) {
                    boolean matches = true;
                    if (methodInDummy.getName().equals(method.getName())) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Class<?>[] parameterTypesInDummy = methodInDummy.getParameterTypes();
                        if (parameterTypes.length == parameterTypesInDummy.length) {
                            for (int p = 0; p < parameterTypes.length; p++) {
                                Class<?> parameterType = parameterTypes[p];
                                Class<?> parameterTypeInDummy = parameterTypesInDummy[p];
                                if (!parameterType.equals(parameterTypeInDummy)) {
                                    matches = false;
                                }
                            }
                        } else {
                            matches = false;
                        }
                    } else {
                        matches = false;
                    }
                    if (matches) {
                        return methodInDummy.invoke(dummyObject, args);
                    }
                }
                return null;
            }
        });
        return (T) proxy;
    }

    public void terminate() {
        terminateParsing = true;
    }

    protected void addMapEntry(org.eclipse.emf.ecore.EObject element, org.eclipse.emf.ecore.EStructuralFeature structuralFeature, fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstDummyEObject dummy) {
        Object value = element.eGet(structuralFeature);
        Object mapKey = dummy.getValueByName("key");
        Object mapValue = dummy.getValueByName("value");
        if (value instanceof org.eclipse.emf.common.util.EMap<?, ?>) {
            org.eclipse.emf.common.util.EMap<Object, Object> valueMap = fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.util.Ocl4tstMapUtil.castToEMap(value);
            if (mapKey != null && mapValue != null) {
                valueMap.put(mapKey, mapValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean addObjectToList(org.eclipse.emf.ecore.EObject container, int featureID, Object object) {
        return ((java.util.List<Object>) container.eGet(container.eClass().getEStructuralFeature(featureID))).add(object);
    }

    @SuppressWarnings("unchecked")
    public boolean addObjectToList(org.eclipse.emf.ecore.EObject container, org.eclipse.emf.ecore.EStructuralFeature feature, Object object) {
        return ((java.util.List<Object>) container.eGet(feature)).add(object);
    }

    protected org.eclipse.emf.ecore.EObject apply(org.eclipse.emf.ecore.EObject target, java.util.List<org.eclipse.emf.ecore.EObject> dummyEObjects) {
        org.eclipse.emf.ecore.EObject currentTarget = target;
        for (org.eclipse.emf.ecore.EObject object : dummyEObjects) {
            assert (object instanceof fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstDummyEObject);
            fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstDummyEObject dummy = (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstDummyEObject) object;
            org.eclipse.emf.ecore.EObject newEObject = dummy.applyTo(currentTarget);
            currentTarget = newEObject;
        }
        return currentTarget;
    }

    protected fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstTokenResolveResult getFreshTokenResolveResult() {
        tokenResolveResult.clear();
        return tokenResolveResult;
    }

    public fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstMetaInformation getMetaInformation() {
        return new fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstMetaInformation();
    }

    protected fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstReferenceResolverSwitch getReferenceResolverSwitch() {
        return (fr.inria.uml4tst.papyrus.ocl4tst.ocl4tst.resource.ocl4tst.mopp.Ocl4tstReferenceResolverSwitch) getMetaInformation().getReferenceResolverSwitch();
    }
}
