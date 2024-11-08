package uni.compilerbau.parser;

import java.io.IOException;
import uni.compilerbau.Token;
import uni.compilerbau.Tokens;
import uni.compilerbau.backend.ByteWriter;
import uni.compilerbau.backend.Operations;
import uni.compilerbau.exceptions.UserParseException;
import uni.compilerbau.tabelle.Field;
import uni.compilerbau.tabelle.Method;
import uni.compilerbau.tabelle.Scope;
import uni.compilerbau.tabelle.Signature;
import uni.compilerbau.tabelle.Type;
import uni.compilerbau.wrapper.Feld;
import uni.compilerbau.wrapper.Konstruktor;
import uni.compilerbau.wrapper.Methode;

public class StatementParser {

    @Feld
    private final ParserReader reader;

    @Feld
    private final Parser parser;

    @Feld
    private final Tokens tokens;

    @Feld
    private Type akType;

    @Feld
    private Scope scope;

    @Feld
    private ByteWriter writeToCodeBefore;

    @Feld
    private ByteWriter writeToCodeAfter;

    @Feld
    private ByteWriter readFromCode;

    @Feld
    private ByteWriter bytes;

    @Feld
    private boolean fertig;

    @Feld
    private boolean canBeWrittenTo;

    @Feld
    private boolean expressionStatement;

    @Feld
    private final Operations Operations;

    @Konstruktor
    public StatementParser(Parser parser, ParserReader reader) {
        this.tokens = new Tokens();
        this.parser = parser;
        this.reader = reader;
        this.Operations = new Operations();
        return;
    }

    @Methode
    private ParserReader getReader() {
        return this.reader;
    }

    @Methode
    private Parser getParser() {
        return this.parser;
    }

    @Methode
    public ParserUtils makeParserUtils() {
        return new ParserUtils(this.getParser(), this.getReader());
    }

    @Methode
    private ByteWriter start(Scope scope) throws Exception {
        boolean isDeklaration = this.isDeklaration();
        if (isDeklaration) return this.parseDeklaration(scope);
        return this.parseSonst(scope);
    }

    @Methode
    public void parseStatement(ByteWriter codeBuffer, Scope scope) throws Exception {
        codeBuffer.writeAll(this.start(scope));
        return;
    }

    /**
	 * Zeilen 11-22 in Statements.txt
	 */
    @Methode
    private ByteWriter parseSonst(Scope scope) throws Exception {
        Field f = scope.getFieldByName(this.getReader().erwarte(this.tokens.IDENTIFIER).getText());
        this.scope = scope;
        this.akType = f.getType();
        this.readFromCode = new ByteWriter();
        this.writeToCodeBefore = new ByteWriter();
        this.writeToCodeAfter = new ByteWriter();
        this.canBeWrittenTo = true;
        this.bytes = null;
        this.expressionStatement = false;
        this.fertig = false;
        this.readFromCode.write1Byte(this.makeParserUtils().getLoadOperation(f.getType()));
        this.readFromCode.write1Byte(scope.getFieldIndex(f));
        this.writeToCodeAfter.write1Byte(this.makeParserUtils().getStoreOperation(f.getType()));
        this.writeToCodeAfter.write1Byte(scope.getFieldIndex(f));
        while (this.tryParseForField()) ;
        if (!this.fertig) throw this.makeParserUtils().makeExceptionUser("Statement nicht fertig.");
        if (this.akType != null) if (!this.akType.isVoid()) this.bytes.write1Byte(this.Operations.POP);
        return this.bytes;
    }

    @Methode
    public Expression parseExpressionStatement(Scope scope) throws Exception {
        Field f;
        try {
            f = scope.getFieldByName(this.getReader().erwarte(this.tokens.IDENTIFIER).getText());
        } catch (UserParseException e) {
            throw this.makeParserUtils().makeExceptionUser("Feld nicht gefunden", e);
        }
        this.scope = scope;
        this.akType = f.getType();
        this.readFromCode = new ByteWriter();
        this.writeToCodeBefore = new ByteWriter();
        this.writeToCodeAfter = new ByteWriter();
        this.canBeWrittenTo = true;
        this.expressionStatement = true;
        this.fertig = true;
        this.readFromCode.write1Byte(this.makeParserUtils().getLoadOperation(f.getType()));
        this.readFromCode.write1Byte(scope.getFieldIndex(f));
        this.writeToCodeAfter.write1Byte(this.makeParserUtils().getStoreOperation(f.getType()));
        this.writeToCodeAfter.write1Byte(scope.getFieldIndex(f));
        while (this.tryParseForField()) ;
        if (!this.fertig) throw this.makeParserUtils().makeExceptionUser("Statement nicht fertig.");
        return new Expression(this.akType, this.readFromCode);
    }

    /**
	 * Am Ende (sobald das erste mal false geliefert wird) ist in {@link #bytes}
	 * der Code für das Statement.
	 * 
	 * Erwartet gesetzt:
	 * <ul>
	 * <li>{@link #akType}</li>
	 * <li>{@link #scope}</li>
	 * <li>{@link #writeToCodeBefore}</li>
	 * <li>{@link #writeToCodeAfter}</li>
	 * <li>{@link #readFromCode}</li>
	 * <li>{@link #canBeWrittenTo}</li>
	 * <li>{@link #expressionStatement}</li>
	 * </ul>
	 * 
	 * Ändert:
	 * <ul>
	 * <li>{@link #akType}</li>
	 * <li>{@link #writeToCodeBefore}</li>
	 * <li>{@link #writeToCodeAfter}</li>
	 * <li>{@link #readFromCode}</li>
	 * <li>{@link #fertig}</li>
	 * <li>{@link #canBeWrittenTo}</li>
	 * </ul>
	 * 
	 * @return ob der Lauf wiederholt werden soll, da noch etwas gemacht werden
	 *         könnte. Wenn false muss fertig=true sein, sonst user-fehler
	 */
    @Methode
    private boolean tryParseForField() throws Exception {
        if (!this.expressionStatement & this.getReader().lookAhead(this.tokens.ASSIGNMENT, false)) {
            if (!this.canBeWrittenTo) throw this.makeParserUtils().makeExceptionUser("Keine Zuweisung erlaubt");
            this.getReader().nextToken();
            Expression exp = this.makeExpressionParser().parseExpression(this.scope);
            if (!exp.getType().canBeAssignedTo(this.akType)) throw this.makeParserUtils().makeExceptionUser("Expression " + exp.toString() + " kann nicht zugewiesen werden. Typen nicht kompatibel");
            this.bytes = new ByteWriter();
            this.bytes.writeAll(this.writeToCodeBefore);
            this.makeParserUtils().writeExpression(this.bytes, exp);
            this.bytes.writeAll(this.writeToCodeAfter);
            this.akType = null;
            this.fertig = true;
            return false;
        } else if (this.getReader().lookAhead(this.tokens.ECKIGE_KLAMMER_AUF)) {
            if (!this.akType.isArray()) throw this.makeParserUtils().makeExceptionUser("Array-Zugriff auf nicht Array-Typ.");
            Expression index = this.makeExpressionParser().parseExpression(this.scope);
            if (!index.isInteger()) throw this.makeParserUtils().makeExceptionUser("Array-Index muss ein Integer-Wert sein.");
            this.getReader().erwarte(this.tokens.ECKIGE_KLAMMER_ZU);
            this.akType = this.akType.getBaseType();
            this.writeToCodeBefore = new ByteWriter();
            this.writeToCodeBefore.writeAll(this.readFromCode);
            this.makeParserUtils().writeExpression(this.writeToCodeBefore, index);
            this.makeParserUtils().writeExpression(this.readFromCode, index);
            this.readFromCode.write1Byte(this.makeParserUtils().getArrayLoadOperation(this.akType));
            this.writeToCodeAfter = new ByteWriter();
            this.writeToCodeAfter.write1Byte(this.makeParserUtils().getArrayStoreOperation(this.akType));
            this.canBeWrittenTo = true;
            this.fertig = this.expressionStatement;
            return true;
        } else if (this.getReader().lookAhead(this.tokens.DOT)) {
            if (this.akType.isArray()) {
                if (!this.getReader().erwarte(this.tokens.IDENTIFIER).getText().equals("length")) throw this.makeParserUtils().makeExceptionUser("Bei Arrays gibt es nur .length");
                Type type = new Type();
                this.akType = new Type(type.INT);
                this.writeToCodeBefore = null;
                this.writeToCodeAfter = null;
                this.readFromCode.write1Byte(this.Operations.ARRAYLENGTH);
                this.fertig = this.expressionStatement;
                this.canBeWrittenTo = false;
                return false;
            }
            if (!this.akType.isClass()) throw this.makeParserUtils().makeExceptionUser("Element-Zugriff auf nicht-Klassen-Typ.");
            String idName = this.getReader().erwarte(this.tokens.IDENTIFIER).getText();
            if (this.getReader().lookAhead(this.tokens.RUNDE_KLAMMER_AUF, false)) {
                ByteWriter paramBytes = new ByteWriter();
                Signature params = this.getParser().parseParameterValues(paramBytes, this.scope);
                Method method = this.akType.getClazz().getMethod(idName, params);
                this.akType = method.getReturnType();
                if (method.isStatic()) this.readFromCode.write1Byte(this.Operations.POP);
                this.readFromCode.writeAll(paramBytes);
                this.readFromCode.write1Byte(method.getInvokeOperation());
                this.readFromCode.write2Byte(method.getConstantPoolIndex());
                this.bytes = new ByteWriter();
                this.bytes.writeAll(this.readFromCode);
                this.writeToCodeBefore = null;
                this.writeToCodeAfter = null;
                this.canBeWrittenTo = false;
                this.fertig = true;
                return true;
            } else {
                Field feld = this.akType.getClazz().getField(idName);
                if (feld == null) throw new ParseException("Das Feld " + idName.toString() + " existiert nicht im Typ " + this.akType.toString());
                this.akType = feld.getType();
                this.writeToCodeBefore = new ByteWriter();
                this.writeToCodeBefore.writeAll(this.readFromCode);
                this.writeToCodeAfter = new ByteWriter();
                if (feld.isStatic()) {
                    this.writeToCodeAfter.write1Byte(this.Operations.POP);
                    this.writeToCodeAfter.write1Byte(this.Operations.PUTSTATIC);
                } else this.writeToCodeAfter.write1Byte(this.Operations.PUTFIELD);
                this.writeToCodeAfter.write2Byte(feld.getConstantPoolIndex());
                if (feld.isStatic()) {
                    this.readFromCode.write1Byte(this.Operations.POP);
                    this.readFromCode.write1Byte(this.Operations.GETSTATIC);
                } else this.readFromCode.write1Byte(this.Operations.GETFIELD);
                this.readFromCode.write2Byte(feld.getConstantPoolIndex());
                this.canBeWrittenTo = true;
                this.fertig = this.expressionStatement;
                return true;
            }
        } else if (!this.expressionStatement & this.getReader().lookAhead(this.tokens.INC, false)) {
            this.getReader().nextToken();
            if (!this.akType.isInteger()) throw this.makeParserUtils().makeExceptionUser("Typ " + this.akType.toString() + " kann nicht inkrementiert werden.");
            this.bytes = new ByteWriter();
            this.bytes.writeAll(this.writeToCodeBefore);
            this.bytes.writeAll(this.readFromCode);
            this.bytes.write1Byte(this.Operations.ICONST_1);
            this.bytes.write1Byte(this.Operations.IADD);
            this.bytes.writeAll(this.writeToCodeAfter);
            this.fertig = true;
            this.canBeWrittenTo = false;
            this.akType = null;
            return false;
        } else if (!this.expressionStatement & this.getReader().lookAhead(this.tokens.DEC, false)) {
            this.getReader().nextToken();
            if (!this.akType.isInteger()) throw this.makeParserUtils().makeExceptionUser("Typ " + this.akType.toString() + " kann nicht dekrementiert werden.");
            this.bytes = new ByteWriter();
            this.bytes.writeAll(this.writeToCodeBefore);
            this.bytes.writeAll(this.readFromCode);
            this.bytes.write1Byte(this.Operations.ICONST_1);
            this.bytes.write1Byte(this.Operations.ISUB);
            this.bytes.writeAll(this.writeToCodeAfter);
            this.fertig = true;
            this.canBeWrittenTo = false;
            this.akType = null;
            return false;
        }
        return false;
    }

    @Methode
    private ExpressionParser makeExpressionParser() {
        return new ExpressionParser(this.getParser(), this.getReader());
    }

    @Methode
    private ByteWriter parseDeklaration(Scope scope) throws Exception {
        ByteWriter bytes = new ByteWriter();
        Type t = this.makeParserUtils().makeType(this.getReader().nextToken());
        while (this.getReader().lookAhead(this.tokens.ECKIGE_KLAMMER_AUF)) {
            t = new Type(t);
            this.getReader().erwarte(this.tokens.ECKIGE_KLAMMER_ZU);
        }
        String fieldName = this.getReader().nextToken().getText();
        Field field = new Field(t, fieldName);
        scope.addField(field);
        if (this.getReader().lookAhead(this.tokens.ASSIGNMENT)) {
            Expression expression = this.parser.makeExpressionParser().parseExpression(scope);
            if (!expression.getType().canBeAssignedTo(t)) throw this.makeParserUtils().makeExceptionUser("Kann dem Feld " + field.toString() + " vom Typ " + field.getType().toString() + " die Expression " + expression.toString() + " nicht zuweisen: Typen nicht kompatibel.");
            this.makeParserUtils().writeExpression(bytes, expression);
            bytes.write1Byte(this.makeParserUtils().getStoreOperation(field.getType()));
            bytes.write1Byte(scope.getFieldIndex(field));
        }
        return bytes;
    }

    @Methode
    private boolean isDeklaration() throws ParseException, IOException {
        this.getReader().mark();
        boolean result;
        Token t = this.getReader().nextToken();
        if (this.makeParserUtils().isPrimitiveType(t)) result = true; else if (t.getToken() == this.tokens.IDENTIFIER) {
            t = this.getReader().nextToken();
            if (t.getToken() == this.tokens.IDENTIFIER) result = true; else if (t.getToken() == this.tokens.ECKIGE_KLAMMER_AUF) {
                if (this.getReader().nextToken().getToken() == this.tokens.ECKIGE_KLAMMER_ZU) result = true; else result = false;
            } else result = false;
        } else result = false;
        this.getReader().returnToMark();
        return result;
    }
}
