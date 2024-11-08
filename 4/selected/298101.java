package com.siemens.ct.exi.context;

import java.util.ArrayList;
import java.util.List;
import com.siemens.ct.exi.core.container.ValueAndDatatype;
import com.siemens.ct.exi.grammar.event.StartElement;
import com.siemens.ct.exi.grammar.rule.SchemaLessStartTag;
import com.siemens.ct.exi.values.StringValue;

public abstract class AbstractCoderContext implements CoderContext {

    final int numberOfGrammarQNameContexts;

    final int numberOfGrammarUriContexts;

    List<EvolvingUriContext> runtimeUriContexts;

    QNameContext qncXsiNil;

    QNameContext qncXsiType;

    RuntimeQNameContextEntries[] grammarQNameContexts;

    List<RuntimeQNameContextEntries> runtimeQNameContexts;

    public AbstractCoderContext(GrammarContext grammarContext) {
        this.numberOfGrammarUriContexts = grammarContext.getNumberOfGrammarUriContexts();
        this.numberOfGrammarQNameContexts = grammarContext.getNumberOfGrammarQNameContexts();
        grammarQNameContexts = new RuntimeQNameContextEntries[numberOfGrammarQNameContexts];
        for (int i = 0; i < numberOfGrammarQNameContexts; i++) {
            grammarQNameContexts[i] = new RuntimeQNameContextEntries();
        }
        runtimeUriContexts = new ArrayList<EvolvingUriContext>();
        for (int i = 0; i < numberOfGrammarUriContexts; i++) {
            GrammarUriContext guc = grammarContext.getGrammarUriContext(i);
            runtimeUriContexts.add(new GrammarEvolvingUriContext(guc));
            if (i == 2) {
                qncXsiNil = guc.getQNameContext(0);
                qncXsiType = guc.getQNameContext(1);
            }
        }
        runtimeQNameContexts = new ArrayList<RuntimeQNameContextEntries>();
        channelOrders = new ArrayList<QNameContext>();
    }

    public QNameContext getXsiTypeContext() {
        return qncXsiType;
    }

    public QNameContext getXsiNilContext() {
        return qncXsiNil;
    }

    public final void addStringValue(QNameContext qnc, StringValue value) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(qnc);
        if (rqne.strings == null) {
            rqne.strings = new ArrayList<StringValue>();
        }
        rqne.strings.add(value);
    }

    public StringValue freeStringValue(QNameContext qnc, int localValueID) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(qnc);
        StringValue prev = rqne.strings.set(localValueID, null);
        return prev;
    }

    public final int getNumberOfStringValues(QNameContext qnc) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(qnc);
        if (rqne.strings == null) {
            return 0;
        } else {
            return rqne.strings.size();
        }
    }

    public final StringValue getStringValue(QNameContext context, int localID) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(context);
        return rqne.strings.get(localID);
    }

    public StartElement getGlobalStartElement(QNameContext qnc) {
        StartElement se;
        int qNameID = qnc.getQNameID();
        if (qNameID < this.numberOfGrammarQNameContexts) {
            se = qnc.getGlobalStartElement();
            if (se == null) {
                se = grammarQNameContexts[qNameID].globalStartElement;
                if (se == null) {
                    se = new StartElement(qnc);
                    se.setRule(new SchemaLessStartTag());
                    grammarQNameContexts[qNameID].globalStartElement = se;
                }
            }
        } else {
            int runtimeQNameID = qNameID - numberOfGrammarQNameContexts;
            se = runtimeQNameContexts.get(runtimeQNameID).globalStartElement;
            if (se == null) {
                se = new StartElement(qnc);
                se.setRule(new SchemaLessStartTag());
                runtimeQNameContexts.get(runtimeQNameID).globalStartElement = se;
            }
        }
        assert (se != null);
        return se;
    }

    protected QNameContext addQNameContext(EvolvingUriContext uc, String localName) {
        int qNameID = numberOfGrammarQNameContexts + runtimeQNameContexts.size();
        QNameContext qnc = uc.addQNameContext(localName, qNameID);
        runtimeQNameContexts.add(new RuntimeQNameContextEntries());
        return qnc;
    }

    public int getNumberOfUris() {
        return runtimeUriContexts.size();
    }

    public EvolvingUriContext getUriContext(int namespaceUriID) {
        return runtimeUriContexts.get(namespaceUriID);
    }

    public EvolvingUriContext getUriContext(String namespaceUri) {
        for (EvolvingUriContext ruc : runtimeUriContexts) {
            if (ruc.getNamespaceUri().equals(namespaceUri)) {
                return ruc;
            }
        }
        return null;
    }

    public EvolvingUriContext addUriContext(String namespaceUri) {
        assert (getUriContext(namespaceUri) == null);
        EvolvingUriContext ruc = new RuntimeEvolvingUriContext(getNumberOfUris(), namespaceUri);
        runtimeUriContexts.add(ruc);
        return ruc;
    }

    public void clear() {
        while (runtimeUriContexts.size() > numberOfGrammarUriContexts) {
            runtimeUriContexts.remove(runtimeUriContexts.size() - 1);
        }
        for (EvolvingUriContext ruc : runtimeUriContexts) {
            ruc.clear();
        }
        for (RuntimeQNameContextEntries rqe : grammarQNameContexts) {
            rqe.clear();
        }
        runtimeQNameContexts.clear();
        channelOrders.clear();
    }

    public RuntimeQNameContextEntries getRuntimeQNameContextEntries(QNameContext qnc) {
        int qNameID = qnc.getQNameID();
        RuntimeQNameContextEntries rqne;
        if (qNameID < this.numberOfGrammarQNameContexts) {
            rqne = grammarQNameContexts[qNameID];
        } else {
            rqne = runtimeQNameContexts.get(qNameID - numberOfGrammarQNameContexts);
        }
        return rqne;
    }

    public void addValueAndDatatype(QNameContext qnc, ValueAndDatatype vd) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(qnc);
        if (rqne.valuesAndDataypes == null) {
            this.channelOrders.add(qnc);
            rqne.valuesAndDataypes = new ArrayList<ValueAndDatatype>();
        }
        rqne.valuesAndDataypes.add(vd);
    }

    public List<ValueAndDatatype> getValueAndDatatypes(QNameContext qnc) {
        RuntimeQNameContextEntries rqne = getRuntimeQNameContextEntries(qnc);
        return rqne.valuesAndDataypes;
    }

    public void initCompressionBlock() {
        channelOrders.clear();
        for (RuntimeQNameContextEntries rqe : this.grammarQNameContexts) {
            rqe.valuesAndDataypes = null;
        }
        for (RuntimeQNameContextEntries rqe : this.runtimeQNameContexts) {
            rqe.valuesAndDataypes = null;
        }
    }

    protected List<QNameContext> channelOrders;

    public List<QNameContext> getChannelOrders() {
        return this.channelOrders;
    }
}
