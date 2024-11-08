package org.emftext.language.pl0.resource.pl0.mopp;

public abstract class Pl0ANTLRParserBase extends org.antlr.runtime3_2_0.Parser implements org.emftext.language.pl0.resource.pl0.IPl0TextParser {

    /**
	 * the index of the last token that was handled by retrieveLayoutInformation()
	 */
    private int lastPosition2;

    /**
	 * a collection to store all anonymous tokens
	 */
    protected java.util.List<org.antlr.runtime3_2_0.CommonToken> anonymousTokens = new java.util.ArrayList<org.antlr.runtime3_2_0.CommonToken>();

    /**
	 * A collection that is filled with commands to be executed after parsing. This
	 * collection is cleared before parsing starts and returned as part of the parse
	 * result object.
	 */
    protected java.util.Collection<org.emftext.language.pl0.resource.pl0.IPl0Command<org.emftext.language.pl0.resource.pl0.IPl0TextResource>> postParseCommands;

    public Pl0ANTLRParserBase(org.antlr.runtime3_2_0.TokenStream input) {
        super(input);
    }

    public Pl0ANTLRParserBase(org.antlr.runtime3_2_0.TokenStream input, org.antlr.runtime3_2_0.RecognizerSharedState state) {
        super(input, state);
    }

    protected void retrieveLayoutInformation(org.eclipse.emf.ecore.EObject element, org.emftext.language.pl0.resource.pl0.grammar.Pl0SyntaxElement syntaxElement, Object object, boolean ignoreTokensAfterLastVisibleToken) {
        if (element == null) {
            return;
        }
        boolean isElementToStore = syntaxElement == null;
        isElementToStore |= syntaxElement instanceof org.emftext.language.pl0.resource.pl0.grammar.Pl0Placeholder;
        isElementToStore |= syntaxElement instanceof org.emftext.language.pl0.resource.pl0.grammar.Pl0Keyword;
        isElementToStore |= syntaxElement instanceof org.emftext.language.pl0.resource.pl0.grammar.Pl0EnumerationTerminal;
        isElementToStore |= syntaxElement instanceof org.emftext.language.pl0.resource.pl0.grammar.Pl0BooleanTerminal;
        if (!isElementToStore) {
            return;
        }
        org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter layoutInformationAdapter = getLayoutInformationAdapter(element);
        for (org.antlr.runtime3_2_0.CommonToken anonymousToken : anonymousTokens) {
            layoutInformationAdapter.addLayoutInformation(new org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformation(syntaxElement, object, anonymousToken.getStartIndex(), anonymousToken.getText(), null));
        }
        anonymousTokens.clear();
        int currentPos = getTokenStream().index();
        if (currentPos == 0) {
            return;
        }
        int endPos = currentPos - 1;
        if (ignoreTokensAfterLastVisibleToken) {
            for (; endPos >= this.lastPosition2; endPos--) {
                org.antlr.runtime3_2_0.Token token = getTokenStream().get(endPos);
                int _channel = token.getChannel();
                if (_channel != 99) {
                    break;
                }
            }
        }
        StringBuilder hiddenTokenText = new StringBuilder();
        StringBuilder visibleTokenText = new StringBuilder();
        org.antlr.runtime3_2_0.CommonToken firstToken = null;
        for (int pos = this.lastPosition2; pos <= endPos; pos++) {
            org.antlr.runtime3_2_0.Token token = getTokenStream().get(pos);
            if (firstToken == null) {
                firstToken = (org.antlr.runtime3_2_0.CommonToken) token;
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
        layoutInformationAdapter.addLayoutInformation(new org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformation(syntaxElement, object, offset, hiddenTokenText.toString(), visibleTokenText.toString()));
        this.lastPosition2 = (endPos < 0 ? 0 : endPos + 1);
    }

    protected org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter getLayoutInformationAdapter(org.eclipse.emf.ecore.EObject element) {
        for (org.eclipse.emf.common.notify.Adapter adapter : element.eAdapters()) {
            if (adapter instanceof org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter) {
                return (org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter) adapter;
            }
        }
        org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter newAdapter = new org.emftext.language.pl0.resource.pl0.mopp.Pl0LayoutInformationAdapter();
        element.eAdapters().add(newAdapter);
        return newAdapter;
    }

    protected <ContainerType extends org.eclipse.emf.ecore.EObject, ReferenceType extends org.eclipse.emf.ecore.EObject> void registerContextDependentProxy(final org.emftext.language.pl0.resource.pl0.mopp.Pl0ContextDependentURIFragmentFactory<ContainerType, ReferenceType> factory, final ContainerType container, final org.eclipse.emf.ecore.EReference reference, final String id, final org.eclipse.emf.ecore.EObject proxy) {
        final int position;
        if (reference.isMany()) {
            position = ((java.util.List<?>) container.eGet(reference)).size();
        } else {
            position = -1;
        }
        postParseCommands.add(new org.emftext.language.pl0.resource.pl0.IPl0Command<org.emftext.language.pl0.resource.pl0.IPl0TextResource>() {

            public boolean execute(org.emftext.language.pl0.resource.pl0.IPl0TextResource resource) {
                if (resource == null) {
                    return true;
                }
                resource.registerContextDependentProxy(factory, container, reference, id, proxy, position);
                return true;
            }
        });
    }
}
