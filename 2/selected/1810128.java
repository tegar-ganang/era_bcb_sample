package ca.umontreal.iro.rali.gate.jape.parser;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import gate.util.*;
import gate.*;
import ca.umontreal.iro.rali.gate.jape.*;
import gate.event.*;

/**
  * A parser for the CPSL language. Generated using JavaCC.
  * @author Hamish Cunningham
  */
public class ParseCpsl implements JapeConstants, ParseCpslConstants {

    /** Construct from a URL and an encoding
    */
    public ParseCpsl(URL url, String encoding) throws IOException {
        this(url, encoding, new HashMap());
    }

    /** Construct from a URL and an encoding
    */
    public ParseCpsl(URL url, String encoding, HashMap existingMacros) throws IOException {
        this(new InputStreamReader(new BufferedInputStream(url.openStream()), encoding));
        macrosMap = existingMacros;
        baseURL = url;
        this.encoding = encoding;
    }

    public void addStatusListener(StatusListener listener) {
        myStatusListeners.add(listener);
    }

    public void removeStatusListener(StatusListener listener) {
        myStatusListeners.remove(listener);
    }

    protected void fireStatusChangedEvent(String text) {
        java.util.Iterator listenersIter = myStatusListeners.iterator();
        while (listenersIter.hasNext()) ((StatusListener) listenersIter.next()).statusChanged(text);
    }

    private transient java.util.List myStatusListeners = new java.util.LinkedList();

    /** Position of the current rule */
    private int ruleNumber;

    /** A list of all the bindings we made this time, for checking
    * the RHS during parsing.
    */
    private HashSet bindingNameSet = null;

    /** A table of macro definitions. */
    private HashMap macrosMap;

    URL baseURL;

    String encoding;

    public final MultiPhaseTransducer MultiPhaseTransducer() throws ParseException {
        SinglePhaseTransducer s = null;
        MultiPhaseTransducer m = new MultiPhaseTransducer();
        m.setBaseURL(baseURL);
        Token mptNameTok = null;
        Token phaseNameTok = null;
        switch(jj_nt.kind) {
            case multiphase:
                jj_consume_token(multiphase);
                mptNameTok = jj_consume_token(ident);
                m.setName(mptNameTok.image);
                break;
            default:
                jj_la1[0] = jj_gen;
                ;
        }
        switch(jj_nt.kind) {
            case phase:
                label_1: while (true) {
                    s = SinglePhaseTransducer();
                    m.addPhase(s.getName(), s);
                    s.setBaseURL(baseURL);
                    switch(jj_nt.kind) {
                        case phase:
                            ;
                            break;
                        default:
                            jj_la1[1] = jj_gen;
                            break label_1;
                    }
                }
                break;
            case phases:
                jj_consume_token(phases);
                label_2: while (true) {
                    phaseNameTok = jj_consume_token(path);
                    ParseCpsl parser = null;
                    String sptPath = phaseNameTok.image + ".jape";
                    URL sptURL = null;
                    try {
                        sptURL = new URL(baseURL, sptPath);
                    } catch (MalformedURLException mue) {
                        {
                            if (true) throw (new ParseException("Read error " + mue.toString()));
                        }
                    }
                    if (sptURL == null) {
                        {
                            if (true) throw (new ParseException("Resource not found: base = " + baseURL.toString() + " path = " + sptPath));
                        }
                    }
                    fireStatusChangedEvent("Reading " + phaseNameTok.image + "...");
                    try {
                        parser = new ParseCpsl(sptURL, encoding, macrosMap);
                    } catch (IOException e) {
                        {
                            if (true) throw (new ParseException("Cannot open URL " + sptURL.toExternalForm()));
                        }
                    }
                    if (parser != null) {
                        s = parser.SinglePhaseTransducer();
                        if (s != null) m.addPhase(s.getName(), s);
                    }
                    switch(jj_nt.kind) {
                        case path:
                            ;
                            break;
                        default:
                            jj_la1[2] = jj_gen;
                            break label_2;
                    }
                }
                break;
            default:
                jj_la1[3] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        jj_consume_token(0);
        m.finish();
        {
            if (true) return m;
        }
        throw new Error("Missing return statement in function");
    }

    public final SinglePhaseTransducer SinglePhaseTransducer() throws ParseException {
        ruleNumber = 0;
        Token phaseNameTok = null;
        Token inputTok = null;
        SinglePhaseTransducer t = null;
        Rule newRule = null;
        bindingNameSet = new HashSet();
        Token optionNameTok = null;
        Token optionValueTok = null;
        Object newMacro = null;
        jj_consume_token(phase);
        phaseNameTok = jj_consume_token(ident);
        t = new SinglePhaseTransducer(phaseNameTok.image);
        switch(jj_nt.kind) {
            case input:
                jj_consume_token(input);
                label_3: while (true) {
                    switch(jj_nt.kind) {
                        case ident:
                            ;
                            break;
                        default:
                            jj_la1[4] = jj_gen;
                            break label_3;
                    }
                    inputTok = jj_consume_token(ident);
                    t.addInput(inputTok.image);
                }
                break;
            default:
                jj_la1[5] = jj_gen;
                ;
        }
        switch(jj_nt.kind) {
            case option:
                jj_consume_token(option);
                label_4: while (true) {
                    switch(jj_nt.kind) {
                        case ident:
                            ;
                            break;
                        default:
                            jj_la1[6] = jj_gen;
                            break label_4;
                    }
                    optionNameTok = jj_consume_token(ident);
                    jj_consume_token(assign);
                    switch(jj_nt.kind) {
                        case ident:
                            optionValueTok = jj_consume_token(ident);
                            break;
                        case bool:
                            optionValueTok = jj_consume_token(bool);
                            break;
                        default:
                            jj_la1[7] = jj_gen;
                            jj_consume_token(-1);
                            throw new ParseException();
                    }
                    t.setOption(optionNameTok.image, optionValueTok.image);
                    if (optionNameTok.image.equalsIgnoreCase("control")) {
                        if (optionValueTok.image.equalsIgnoreCase("appelt")) t.setRuleApplicationStyle(APPELT_STYLE); else if (optionValueTok.image.equalsIgnoreCase("first")) t.setRuleApplicationStyle(FIRST_STYLE); else if (optionValueTok.image.equalsIgnoreCase("brill")) t.setRuleApplicationStyle(BRILL_STYLE); else if (optionValueTok.image.equalsIgnoreCase("once")) t.setRuleApplicationStyle(ONCE_STYLE); else System.err.println("ignoring unknown control strategy " + option + " (should be brill, appelt or first)");
                    } else if (optionNameTok.image.equalsIgnoreCase("debug")) {
                        if (optionValueTok.image.equalsIgnoreCase("true") || optionValueTok.image.equalsIgnoreCase("yes") || optionValueTok.image.equalsIgnoreCase("y")) t.setDebugMode(true); else t.setDebugMode(false);
                    }
                }
                break;
            default:
                jj_la1[8] = jj_gen;
                ;
        }
        label_5: while (true) {
            switch(jj_nt.kind) {
                case rule:
                case macro:
                    ;
                    break;
                default:
                    jj_la1[9] = jj_gen;
                    break label_5;
            }
            switch(jj_nt.kind) {
                case rule:
                    newRule = Rule(phaseNameTok.image);
                    t.addRule(newRule);
                    break;
                case macro:
                    MacroDef();
                    break;
                default:
                    jj_la1[10] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
        }
        if (ruleNumber == 0) {
            if (true) throw (new ParseException("no rules defined in transducer " + t.getName()));
        }
        t.finish();
        t.setBaseURL(baseURL);
        {
            if (true) return t;
        }
        throw new Error("Missing return statement in function");
    }

    public final Rule Rule(String phaseName) throws ParseException {
        Token ruleNameTok = null;
        String ruleName = null;
        Token priorityTok = null;
        int rulePriority = 0;
        LeftHandSide lhs = null;
        RightHandSide rhs = null;
        Rule newRule = null;
        jj_consume_token(rule);
        ruleNameTok = jj_consume_token(ident);
        ruleName = ruleNameTok.image;
        switch(jj_nt.kind) {
            case priority:
                jj_consume_token(priority);
                priorityTok = jj_consume_token(integer);
                try {
                    rulePriority = Integer.parseInt(priorityTok.image);
                } catch (NumberFormatException e) {
                    System.err.println("bad priority spec(" + priorityTok.image + "), rule(" + ruleName + ") - treating as 0");
                    rulePriority = 0;
                }
                break;
            default:
                jj_la1[11] = jj_gen;
                ;
        }
        lhs = LeftHandSide();
        jj_consume_token(58);
        rhs = RightHandSide(phaseName, ruleName, lhs);
        try {
            rhs.createActionClass();
        } catch (JapeException e) {
            {
                if (true) throw new ParseException("couldn't create rule RHS: " + e.toString());
            }
        }
        newRule = new Rule(ruleName, ruleNumber, rulePriority, lhs, rhs);
        ruleNumber++;
        {
            if (true) return newRule;
        }
        throw new Error("Missing return statement in function");
    }

    public final void MacroDef() throws ParseException {
        Token macroNameTok = null;
        Object body = null;
        jj_consume_token(macro);
        macroNameTok = jj_consume_token(ident);
        if (jj_2_1(2)) {
            body = PatternElement(null);
        } else {
            switch(jj_nt.kind) {
                case ident:
                case colon:
                case leftBrace:
                case colonplus:
                    body = Action();
                    break;
                default:
                    jj_la1[12] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
        }
        macrosMap.put(macroNameTok.image, body);
    }

    public final LeftHandSide LeftHandSide() throws ParseException {
        ConstraintGroup cg = new ConstraintGroup();
        LeftHandSide lhs = new LeftHandSide(cg);
        ConstraintGroup(lhs, cg);
        {
            if (true) return lhs;
        }
        throw new Error("Missing return statement in function");
    }

    public final void ConstraintGroup(LeftHandSide lhs, ConstraintGroup cg) throws ParseException {
        PatternElement pat = null;
        label_6: while (true) {
            pat = PatternElement(lhs);
            cg.addPatternElement(pat);
            switch(jj_nt.kind) {
                case string:
                case ident:
                case leftBrace:
                case leftBracket:
                    ;
                    break;
                default:
                    jj_la1[13] = jj_gen;
                    break label_6;
            }
        }
        label_7: while (true) {
            switch(jj_nt.kind) {
                case bar:
                    ;
                    break;
                default:
                    jj_la1[14] = jj_gen;
                    break label_7;
            }
            jj_consume_token(bar);
            cg.createDisjunction();
            label_8: while (true) {
                pat = PatternElement(lhs);
                cg.addPatternElement(pat);
                switch(jj_nt.kind) {
                    case string:
                    case ident:
                    case leftBrace:
                    case leftBracket:
                        ;
                        break;
                    default:
                        jj_la1[15] = jj_gen;
                        break label_8;
                }
            }
        }
    }

    public final PatternElement PatternElement(LeftHandSide lhs) throws ParseException {
        PatternElement pat = null;
        Token macroRefTok = null;
        boolean macroRef = false;
        switch(jj_nt.kind) {
            case ident:
                macroRefTok = jj_consume_token(ident);
                macroRef = true;
                Object macro = macrosMap.get(macroRefTok.image);
                if (macro == null) {
                    if (true) throw (new ParseException("unknown macro name " + macroRefTok.image));
                } else if (macro instanceof String[]) {
                    if (true) throw (new ParseException("macro " + macroRefTok.image + " references an Action, not a PatternElement"));
                } else if (!(macro instanceof PatternElement)) {
                    if (true) throw (new ParseException("macro " + macroRefTok.image + " doesn't reference a PatternElement!"));
                } else {
                    pat = (PatternElement) ((PatternElement) macro).clone();
                }
                break;
            case string:
            case leftBrace:
                pat = BasicPatternElement();
                break;
            case leftBracket:
                pat = ComplexPatternElement(lhs);
                break;
            default:
                jj_la1[16] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        if (pat instanceof ComplexPatternElement) {
            String bindingName = ((ComplexPatternElement) pat).getBindingName();
            if (bindingName != null && lhs != null) {
                try {
                    lhs.addBinding(bindingName, (ComplexPatternElement) pat, bindingNameSet, macroRef);
                } catch (JapeException e) {
                    System.err.println("duplicate binding name " + bindingName + " - ignoring this binding! exception was: " + e.toString());
                }
            }
        }
        {
            if (true) return pat;
        }
        throw new Error("Missing return statement in function");
    }

    public final BasicPatternElement BasicPatternElement() throws ParseException {
        Token shortTok = null;
        ArrayList constraints = new ArrayList();
        Token constrTok = null;
        Constraint c = null;
        BasicPatternElement bpe = new BasicPatternElement();
        switch(jj_nt.kind) {
            case leftBrace:
                jj_consume_token(leftBrace);
                c = Constraint();
                bpe.addConstraint(c);
                label_9: while (true) {
                    switch(jj_nt.kind) {
                        case comma:
                            ;
                            break;
                        default:
                            jj_la1[17] = jj_gen;
                            break label_9;
                    }
                    jj_consume_token(comma);
                    c = Constraint();
                    bpe.addConstraint(c);
                }
                jj_consume_token(rightBrace);
                break;
            case string:
                shortTok = jj_consume_token(string);
                System.err.println("string shorthand not supported yet, ignoring: " + shortTok.image);
                break;
            default:
                jj_la1[18] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        {
            if (true) return bpe;
        }
        throw new Error("Missing return statement in function");
    }

    public final ComplexPatternElement ComplexPatternElement(LeftHandSide lhs) throws ParseException {
        Token kleeneOpTok = null;
        Token bindingNameTok = null;
        ConstraintGroup cg = new ConstraintGroup();
        jj_consume_token(leftBracket);
        ConstraintGroup(lhs, cg);
        jj_consume_token(rightBracket);
        switch(jj_nt.kind) {
            case kleeneOp:
                kleeneOpTok = jj_consume_token(kleeneOp);
                break;
            default:
                jj_la1[19] = jj_gen;
                ;
        }
        switch(jj_nt.kind) {
            case colon:
                jj_consume_token(colon);
                switch(jj_nt.kind) {
                    case ident:
                        bindingNameTok = jj_consume_token(ident);
                        break;
                    case integer:
                        bindingNameTok = jj_consume_token(integer);
                        break;
                    default:
                        jj_la1[20] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                }
                break;
            default:
                jj_la1[21] = jj_gen;
                ;
        }
        int kleeneOp = NO_KLEENE_OP;
        if (kleeneOpTok != null) {
            String k = kleeneOpTok.image;
            if (k.equals("*")) kleeneOp = KLEENE_STAR; else if (k.equals("?")) kleeneOp = KLEENE_QUERY; else if (k.equals("+")) kleeneOp = KLEENE_PLUS; else System.err.println("ignoring uninterpretable Kleene op " + k);
        }
        String bindingName = null;
        if (bindingNameTok != null) bindingName = bindingNameTok.image;
        {
            if (true) return new ComplexPatternElement(cg, kleeneOp, bindingName);
        }
        throw new Error("Missing return statement in function");
    }

    public final Constraint Constraint() throws ParseException {
        Token annotTypeTok = null;
        Token attrNameTok = null;
        Token opTok = null;
        Object attrValObj = null;
        Pair attrValPair = null;
        boolean negate = false;
        Constraint c = null;
        int opInt = -1;
        switch(jj_nt.kind) {
            case pling:
                jj_consume_token(pling);
                negate = true;
                break;
            default:
                jj_la1[22] = jj_gen;
                ;
        }
        annotTypeTok = jj_consume_token(ident);
        c = new Constraint(annotTypeTok.image);
        if (negate) c.negate();
        switch(jj_nt.kind) {
            case period:
                jj_consume_token(period);
                attrNameTok = jj_consume_token(ident);
                opTok = jj_consume_token(attrOp);
                attrValPair = AttrVal();
                attrValObj = attrValPair.second;
                if (opTok.image.equals("==")) opInt = EQUAL;
                if (opTok.image.equals("!=")) {
                    opInt = EQUAL;
                    c.changeSign();
                }
                if (opTok.image.equals(">")) opInt = GREATER;
                if (opTok.image.equals("<")) opInt = LESSER;
                if (opTok.image.equals(">=")) opInt = GREATER_OR_EQUAL;
                if (opTok.image.equals("<=")) opInt = LESSER_OR_EQUAL;
                if (opTok.image.equals("=~")) opInt = REGEXP;
                if (opTok.image.equals("!~")) {
                    opInt = REGEXP;
                    c.changeSign();
                }
                if (opInt == REGEXP || opInt == NOT_REGEXP) {
                    if (((Integer) attrValPair.first).intValue() == string) {
                        try {
                            attrValObj = Pattern.compile((String) attrValObj);
                            c.addAttribute(new JdmAttribute(attrNameTok.image, attrValObj, opInt));
                        } catch (PatternSyntaxException pse) {
                            System.err.println("Malformed regular expression: \"" + pse.getPattern());
                            System.err.println("PatternSyntaxException says: " + pse.getMessage());
                        }
                    } else {
                        System.out.println("regular expression \"" + attrValObj + "\" for pattern matching (\"=~\" or \"!~\") should be a string between double quotes. Ignored.");
                    }
                } else {
                    c.addAttribute(new JdmAttribute(attrNameTok.image, attrValObj, opInt));
                }
                break;
            default:
                jj_la1[23] = jj_gen;
                ;
        }
        {
            if (true) return c;
        }
        throw new Error("Missing return statement in function");
    }

    public final Pair AttrVal() throws ParseException {
        Token attrValTok = null;
        Pair val = new Pair();
        switch(jj_nt.kind) {
            case string:
                attrValTok = jj_consume_token(string);
                break;
            case ident:
                attrValTok = jj_consume_token(ident);
                break;
            case integer:
                attrValTok = jj_consume_token(integer);
                break;
            case floatingPoint:
                attrValTok = jj_consume_token(floatingPoint);
                break;
            case bool:
                attrValTok = jj_consume_token(bool);
                break;
            default:
                jj_la1[24] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        val.first = new Integer(attrValTok.kind);
        switch(attrValTok.kind) {
            case string:
                val.second = attrValTok.image.substring(1, attrValTok.image.length() - 1);
                break;
            case integer:
                try {
                    val.second = Long.valueOf(attrValTok.image);
                } catch (NumberFormatException e) {
                    System.err.println("couldn't parse integer " + attrValTok.image + " - treating as 0");
                    val.second = new Long(0);
                }
                break;
            case ident:
                val.second = new String(attrValTok.image);
                break;
            case bool:
                val.second = Boolean.valueOf(attrValTok.image);
                break;
            case floatingPoint:
                try {
                    val.second = Double.valueOf(attrValTok.image);
                } catch (NumberFormatException e) {
                    System.err.println("couldn't parse float " + attrValTok.image + " - treating as 0.0");
                    val.second = new Double(0.0);
                }
                break;
            default:
                System.err.println("didn't understand type of " + attrValTok.image + ": ignoring");
                val.second = new String("");
                break;
        }
        {
            if (true) return val;
        }
        throw new Error("Missing return statement in function");
    }

    public final RightHandSide RightHandSide(String phaseName, String ruleName, LeftHandSide lhs) throws ParseException {
        String[] block = new String[2];
        RightHandSide rhs = new RightHandSide(phaseName, ruleName, lhs);
        block = Action();
        if (block[0] != null) if (!bindingNameSet.contains(block[0])) {
            {
                if (true) throw (new ParseException("unknown label in RHS action: " + block[0]));
            }
        }
        rhs.addBlock(block[0], block[1]);
        label_10: while (true) {
            switch(jj_nt.kind) {
                case comma:
                    ;
                    break;
                default:
                    jj_la1[25] = jj_gen;
                    break label_10;
            }
            jj_consume_token(comma);
            block = Action();
            if (block[0] != null) if (!bindingNameSet.contains(block[0])) {
                {
                    if (true) throw (new ParseException("unknown label in RHS action: " + block[0]));
                }
            }
            rhs.addBlock(block[0], block[1]);
        }
        {
            if (true) return rhs;
        }
        throw new Error("Missing return statement in function");
    }

    public final String[] Action() throws ParseException {
        String[] block = new String[2];
        Token macroRefTok = null;
        if (jj_2_2(3)) {
            block = NamedJavaBlock();
        } else {
            switch(jj_nt.kind) {
                case leftBrace:
                    block = AnonymousJavaBlock();
                    break;
                case colon:
                case colonplus:
                    block = AssignmentExpression();
                    break;
                case ident:
                    macroRefTok = jj_consume_token(ident);
                    Object macro = macrosMap.get(macroRefTok.image);
                    if (macro == null) {
                        if (true) throw (new ParseException("unknown macro name " + macroRefTok.image));
                    } else if (macro instanceof PatternElement) {
                        if (true) throw (new ParseException("macro " + macroRefTok.image + " references a PatternElement, not an Action"));
                    } else if (!(macro instanceof String[])) {
                        if (true) throw (new ParseException("macro " + macroRefTok.image + " doesn't reference an Action!"));
                    } else {
                        block = (String[]) macro;
                    }
                    break;
                default:
                    jj_la1[26] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
        }
        {
            if (true) return block;
        }
        throw new Error("Missing return statement in function");
    }

    public final String[] NamedJavaBlock() throws ParseException {
        String[] block = new String[2];
        Token nameTok = null;
        jj_consume_token(colon);
        nameTok = jj_consume_token(ident);
        block[0] = nameTok.image;
        jj_consume_token(leftBrace);
        block[1] = ConsumeBlock();
        {
            if (true) return block;
        }
        throw new Error("Missing return statement in function");
    }

    public final String[] AnonymousJavaBlock() throws ParseException {
        String[] block = new String[2];
        block[0] = null;
        jj_consume_token(leftBrace);
        block[1] = ConsumeBlock();
        {
            if (true) return block;
        }
        throw new Error("Missing return statement in function");
    }

    public final String[] AssignmentExpression() throws ParseException {
        String[] block = new String[2];
        StringBuffer blockBuffer = new StringBuffer();
        boolean simpleSpan = true;
        Token nameTok = null;
        String newAnnotType = null;
        String newAttrName = null;
        String nl = Strings.getNl();
        String annotSetName = null;
        Pair attrVal = null;
        String existingAnnotSetName = null;
        String existingAnnotType = null;
        String existingAttrName = null;
        blockBuffer.append("// RHS assignment block" + nl);
        blockBuffer.append("      FeatureMap features = Factory.newFeatureMap();" + nl);
        switch(jj_nt.kind) {
            case colon:
                jj_consume_token(colon);
                simpleSpan = true;
                break;
            case colonplus:
                jj_consume_token(colonplus);
                simpleSpan = false;
                {
                    if (true) throw new ParseException(":+ not a legal operator (no multi-span annots)");
                }
                break;
            default:
                jj_la1[27] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        nameTok = jj_consume_token(ident);
        block[0] = nameTok.image;
        annotSetName = block[0] + "Annots";
        jj_consume_token(period);
        nameTok = jj_consume_token(ident);
        newAnnotType = nameTok.image;
        blockBuffer.append("      String newAnnotType = \"" + newAnnotType + "\";" + nl);
        blockBuffer.append("      Object val = null;" + nl);
        jj_consume_token(assign);
        jj_consume_token(leftBrace);
        label_11: while (true) {
            switch(jj_nt.kind) {
                case ident:
                    ;
                    break;
                default:
                    jj_la1[28] = jj_gen;
                    break label_11;
            }
            nameTok = jj_consume_token(ident);
            jj_consume_token(assign);
            newAttrName = nameTok.image;
            switch(jj_nt.kind) {
                case integer:
                case string:
                case bool:
                case ident:
                case floatingPoint:
                    attrVal = AttrVal();
                    switch(((Integer) attrVal.first).intValue()) {
                        case string:
                            blockBuffer.append("      val = new String(\"" + attrVal.second.toString() + "\");" + nl);
                            break;
                        case integer:
                            blockBuffer.append("      try { " + "val = new Long(" + attrVal.second.toString() + "); }" + nl + "      catch(NumberFormatException e) { }" + nl);
                            break;
                        case ident:
                            blockBuffer.append("      val = new String(\"" + attrVal.second.toString() + "\");" + nl);
                            break;
                        case bool:
                            blockBuffer.append("      val = new Boolean(\"" + attrVal.second.toString() + "\");" + nl);
                            break;
                        case floatingPoint:
                            blockBuffer.append("      try { " + "val = new Double(" + attrVal.second.toString() + "); }" + nl + "      catch(NumberFormatException e) { }" + nl);
                            break;
                        default:
                            blockBuffer.append("      val = new String(\"\");" + nl);
                            break;
                    }
                    blockBuffer.append("      features.put(\"" + newAttrName + "\", val);");
                    blockBuffer.append(nl);
                    break;
                case colon:
                    jj_consume_token(colon);
                    nameTok = jj_consume_token(ident);
                    existingAnnotSetName = nameTok.image + "ExistingAnnots";
                    if (!bindingNameSet.contains(nameTok.image)) {
                        if (true) throw (new ParseException("unknown label in RHS action(2): " + nameTok.image));
                    }
                    blockBuffer.append("      { // need a block for the existing annot set" + nl + "        AnnotationSet " + existingAnnotSetName + " = (AnnotationSet)bindings.get(\"" + nameTok.image + "\"); " + nl);
                    jj_consume_token(period);
                    nameTok = jj_consume_token(ident);
                    existingAnnotType = nameTok.image;
                    jj_consume_token(period);
                    nameTok = jj_consume_token(ident);
                    existingAttrName = nameTok.image;
                    blockBuffer.append("        AnnotationSet existingAnnots = " + nl + "        " + existingAnnotSetName + ".get(\"" + existingAnnotType + "\");" + nl + "        Iterator iter = existingAnnots.iterator();" + nl + "        while(iter.hasNext()) {" + nl + "          Annotation existingA = (Annotation) iter.next();" + nl + "          Object existingFeatureValue = existingA.getFeatures().get(\"" + existingAttrName + "\");" + nl + "          if(existingFeatureValue != null) {" + nl + "            features.put(\"" + existingAttrName + "\", existingFeatureValue);" + nl + "            break;" + nl + "          }" + nl + "        } // while" + nl + "      } // block for existing annots" + nl);
                    break;
                default:
                    jj_la1[29] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
            switch(jj_nt.kind) {
                case comma:
                    jj_consume_token(comma);
                    break;
                default:
                    jj_la1[30] = jj_gen;
                    ;
            }
        }
        jj_consume_token(rightBrace);
        blockBuffer.append("      annotations.add(" + nl);
        blockBuffer.append("        " + annotSetName + ".firstNode(), ");
        blockBuffer.append(annotSetName + ".lastNode(), " + nl);
        blockBuffer.append("        \"" + newAnnotType + "\", features" + nl);
        blockBuffer.append("      );" + nl);
        blockBuffer.append("      // end of RHS assignment block");
        block[1] = blockBuffer.toString();
        {
            if (true) return block;
        }
        throw new Error("Missing return statement in function");
    }

    String ConsumeBlock() throws ParseException {
        StringBuffer block = new StringBuffer();
        int nesting = 1;
        while (nesting != 0) {
            Token nextTok = getNextToken();
            if (nextTok.specialToken != null) {
                Token special = nextTok.specialToken;
                while (special != null) {
                    block.append(special.image);
                    special = special.next;
                }
            }
            if (nextTok.image.equals("{")) {
                nesting++;
            } else if (nextTok.image.equals("}")) {
                nesting--;
            }
            if (nesting > 0) block.append(nextTok.image);
        }
        return block.toString();
    }

    private final boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_1();
        } catch (LookaheadSuccess ls) {
            return true;
        } finally {
            jj_save(0, xla);
        }
    }

    private final boolean jj_2_2(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_2();
        } catch (LookaheadSuccess ls) {
            return true;
        } finally {
            jj_save(1, xla);
        }
    }

    private final boolean jj_3R_21() {
        Token xsp;
        if (jj_3R_23()) return true;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_23()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private final boolean jj_3R_16() {
        if (jj_3R_18()) return true;
        return false;
    }

    private final boolean jj_3R_15() {
        if (jj_3R_17()) return true;
        return false;
    }

    private final boolean jj_3R_19() {
        if (jj_scan_token(leftBrace)) return true;
        if (jj_3R_22()) return true;
        return false;
    }

    private final boolean jj_3R_14() {
        if (jj_scan_token(ident)) return true;
        return false;
    }

    private final boolean jj_3_1() {
        if (jj_3R_12()) return true;
        return false;
    }

    private final boolean jj_3R_12() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_14()) {
            jj_scanpos = xsp;
            if (jj_3R_15()) {
                jj_scanpos = xsp;
                if (jj_3R_16()) return true;
            }
        }
        return false;
    }

    private final boolean jj_3R_17() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_19()) {
            jj_scanpos = xsp;
            if (jj_3R_20()) return true;
        }
        return false;
    }

    private final boolean jj_3R_24() {
        if (jj_scan_token(pling)) return true;
        return false;
    }

    private final boolean jj_3R_22() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_24()) jj_scanpos = xsp;
        if (jj_scan_token(ident)) return true;
        return false;
    }

    private final boolean jj_3_2() {
        if (jj_3R_13()) return true;
        return false;
    }

    private final boolean jj_3R_13() {
        if (jj_scan_token(colon)) return true;
        if (jj_scan_token(ident)) return true;
        if (jj_scan_token(leftBrace)) return true;
        return false;
    }

    private final boolean jj_3R_20() {
        if (jj_scan_token(string)) return true;
        return false;
    }

    private final boolean jj_3R_23() {
        if (jj_3R_12()) return true;
        return false;
    }

    private final boolean jj_3R_18() {
        if (jj_scan_token(leftBracket)) return true;
        if (jj_3R_21()) return true;
        return false;
    }

    public ParseCpslTokenManager token_source;

    SimpleCharStream jj_input_stream;

    public Token token, jj_nt;

    private Token jj_scanpos, jj_lastpos;

    private int jj_la;

    public boolean lookingAhead = false;

    private boolean jj_semLA;

    private int jj_gen;

    private final int[] jj_la1 = new int[31];

    private static int[] jj_la1_0;

    private static int[] jj_la1_1;

    static {
        jj_la1_0();
        jj_la1_1();
    }

    private static void jj_la1_0() {
        jj_la1_0 = new int[] { 0x400, 0x80000, 0x1000, 0x80800, 0x0, 0x100000, 0x0, 0x0, 0x200000, 0xc00000, 0xc00000, 0x1000000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x4000000, 0x10000000, 0x0, 0x2000000, 0x0, 0x10000000, 0x0, 0x0, 0x0, 0x0, 0x10000000, 0x0 };
    }

    private static void jj_la1_1() {
        jj_la1_1 = new int[] { 0x0, 0x0, 0x0, 0x0, 0x20, 0x0, 0x20, 0x30, 0x0, 0x0, 0x0, 0x0, 0x42120, 0xa028, 0x800, 0xa028, 0xa028, 0x1000, 0x2008, 0x0, 0x20, 0x100, 0x0, 0x400, 0x78, 0x1000, 0x42120, 0x40100, 0x20, 0x178, 0x1000 };
    }

    private final JJCalls[] jj_2_rtns = new JJCalls[2];

    private boolean jj_rescan = false;

    private int jj_gc = 0;

    public ParseCpsl(java.io.InputStream stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new ParseCpslTokenManager(jj_input_stream);
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    public void ReInit(java.io.InputStream stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    public ParseCpsl(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new ParseCpslTokenManager(jj_input_stream);
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    public ParseCpsl(ParseCpslTokenManager tm) {
        token_source = tm;
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    public void ReInit(ParseCpslTokenManager tm) {
        token_source = tm;
        token = new Token();
        token.next = jj_nt = token_source.getNextToken();
        jj_gen = 0;
        for (int i = 0; i < 31; i++) jj_la1[i] = -1;
        for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
    }

    private final Token jj_consume_token(int kind) throws ParseException {
        Token oldToken = token;
        if ((token = jj_nt).next != null) jj_nt = jj_nt.next; else jj_nt = jj_nt.next = token_source.getNextToken();
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (int i = 0; i < jj_2_rtns.length; i++) {
                    JJCalls c = jj_2_rtns[i];
                    while (c != null) {
                        if (c.gen < jj_gen) c.first = null;
                        c = c.next;
                    }
                }
            }
            return token;
        }
        jj_nt = token;
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    private static final class LookaheadSuccess extends java.lang.Error {
    }

    private final LookaheadSuccess jj_ls = new LookaheadSuccess();

    private final boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            } else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        } else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0;
            Token tok = token;
            while (tok != null && tok != jj_scanpos) {
                i++;
                tok = tok.next;
            }
            if (tok != null) jj_add_error_token(kind, i);
        }
        if (jj_scanpos.kind != kind) return true;
        if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
        return false;
    }

    public final Token getNextToken() {
        if ((token = jj_nt).next != null) jj_nt = jj_nt.next; else jj_nt = jj_nt.next = token_source.getNextToken();
        jj_gen++;
        return token;
    }

    public final Token getToken(int index) {
        Token t = lookingAhead ? jj_scanpos : token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) t = t.next; else t = t.next = token_source.getNextToken();
        }
        return t;
    }

    private java.util.Vector jj_expentries = new java.util.Vector();

    private int[] jj_expentry;

    private int jj_kind = -1;

    private int[] jj_lasttokens = new int[100];

    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100) return;
        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        } else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];
            for (int i = 0; i < jj_endpos; i++) {
                jj_expentry[i] = jj_lasttokens[i];
            }
            boolean exists = false;
            for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements(); ) {
                int[] oldentry = (int[]) (e.nextElement());
                if (oldentry.length == jj_expentry.length) {
                    exists = true;
                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            exists = false;
                            break;
                        }
                    }
                    if (exists) break;
                }
            }
            if (!exists) jj_expentries.addElement(jj_expentry);
            if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
        }
    }

    public ParseException generateParseException() {
        jj_expentries.removeAllElements();
        boolean[] la1tokens = new boolean[59];
        for (int i = 0; i < 59; i++) {
            la1tokens[i] = false;
        }
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 31; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 59; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.addElement(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = (int[]) jj_expentries.elementAt(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    public final void enable_tracing() {
    }

    public final void disable_tracing() {
    }

    private final void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 2; i++) {
            JJCalls p = jj_2_rtns[i];
            do {
                if (p.gen > jj_gen) {
                    jj_la = p.arg;
                    jj_lastpos = jj_scanpos = p.first;
                    switch(i) {
                        case 0:
                            jj_3_1();
                            break;
                        case 1:
                            jj_3_2();
                            break;
                    }
                }
                p = p.next;
            } while (p != null);
        }
        jj_rescan = false;
    }

    private final void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) {
                p = p.next = new JJCalls();
                break;
            }
            p = p.next;
        }
        p.gen = jj_gen + xla - jj_la;
        p.first = token;
        p.arg = xla;
    }

    static final class JJCalls {

        int gen;

        Token first;

        int arg;

        JJCalls next;
    }
}
