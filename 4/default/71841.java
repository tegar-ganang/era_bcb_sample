import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class PCEParser {

    static BufferedWriter MethodsCalled;

    static BufferedWriter AttributesRefered;

    static BufferedWriter LocalMembers;

    static BufferedWriter MethodsDestClass;

    static BufferedWriter PrincipleClassGroup;

    static BufferedWriter PrincipleClasses;

    static BufferedWriter ReadAttributes;

    static BufferedWriter WriteAttributes;

    static BufferedWriter classNames;

    static BufferedWriter classIns;

    static BufferedWriter FundView;

    static BufferedWriter IntView;

    static BufferedWriter AssocView;

    static int index;

    static boolean insideConstructor = false;

    static boolean insideMethod = false;

    static String[] info;

    static String[] ClassNames;

    static String[] objectCreationExpression = new String[100000];

    static String[] instanceExpr = new String[100000];

    static String[] fieldsAccesses = new String[10000000];

    static int noOfObject = 0;

    static int noOfInstanceExpr = 0;

    static int noOfFieldAccesses = 0;

    int readFileCount = 0;

    int writeFileCount = 0;

    int methodCallFileCount = 0;

    static String readArray[];

    static String writeArray[];

    static String callArray[];

    int principleClassCount = 0;

    int principleMethodCount = 0;

    static String principleClass[];

    static String principleMethod[];

    static String principleMethodsClass[];

    public void parseClasses(String[] filenames, int numFiles) throws Exception {
        CompilationUnit[] cu = new CompilationUnit[1000];
        FileInputStream[] in = new FileInputStream[1000];
        for (int j = 0; j < numFiles; j++) {
            in[j] = new FileInputStream(filenames[j]);
        }
        ClassNames = new String[numFiles];
        index = 0;
        LocalMembers = new BufferedWriter(new FileWriter("InfoFiles/LocalMembers.txt"));
        MethodsCalled = new BufferedWriter(new FileWriter("InfoFiles/MethodsCalled.txt"));
        AttributesRefered = new BufferedWriter(new FileWriter("InfoFiles/AttributesRefered.txt"));
        ReadAttributes = new BufferedWriter(new FileWriter("InfoFiles/ReadAttributes.txt"));
        WriteAttributes = new BufferedWriter(new FileWriter("InfoFiles/WriteAttributes.txt"));
        try {
            for (int i = 0; i < numFiles; i++) {
                cu[i] = JavaParser.parse(in[i]);
            }
        } finally {
            for (int i = 0; i < numFiles; i++) {
                in[i].close();
            }
        }
        for (int j = 0; j < numFiles; j++) {
            index = j;
            new ClassVisitor().visit(cu[j], null);
        }
        info = new String[numFiles * 2];
        for (int k = 0; k < numFiles; k++) {
            System.out.println("class names are:" + ClassNames[k]);
            info[k] = ClassNames[k];
        }
        System.out.println("Number od files" + numFiles);
        for (int i = 0; i < numFiles && ClassNames[i] != null; i++) {
            index = i;
            instanceExpr[noOfInstanceExpr++] = new String(ClassNames[i]);
            fieldsAccesses[noOfFieldAccesses++] = new String(ClassNames[i]);
            new LocalClassInstantiationVisitor().visit(cu[i], null);
            new ClassInstantiationExpr().visit(cu[i], null);
            new ObjectCreationVisitor().visit(cu[i], null);
            new ObjectNameVisitor().visit(cu[i], null);
            AttributesRefered.write(ClassNames[i]);
            AttributesRefered.newLine();
            new FieldAccessVisitor().visit(cu[i], null);
            AttributesRefered.write("EndOfClass");
            AttributesRefered.newLine();
            MethodsCalled.write(ClassNames[i]);
            MethodsCalled.newLine();
            new MethodInvocationVisitor().visit(cu[i], null);
            MethodsCalled.write("EndOfClass");
            MethodsCalled.newLine();
            LocalMembers.write(ClassNames[i]);
            LocalMembers.newLine();
            new MethodVisitor().visit(cu[i], null);
            LocalMembers.write("EndOfClass");
            LocalMembers.newLine();
            new FieldAccessedVisitor().visit(cu[i], null);
            new Fields().visit(cu[i], null);
            WriteAttributes.write(ClassNames[i]);
            WriteAttributes.newLine();
            new AttributeWriteVisitor().visit(cu[i], null);
            WriteAttributes.write("EndOfClass");
            WriteAttributes.newLine();
            ReadAttributes.write(ClassNames[i]);
            ReadAttributes.newLine();
            new AttributeReadVisitor().visit(cu[i], null);
            ReadAttributes.write("EndOfClass");
            ReadAttributes.newLine();
            instanceExpr[noOfInstanceExpr++] = new String("EndOfClass");
            fieldsAccesses[noOfFieldAccesses++] = new String(ClassNames[i]);
        }
        MethodsCalled.close();
        AttributesRefered.close();
        LocalMembers.close();
        ReadAttributes.close();
        WriteAttributes.close();
        extractPrincipalClasses(info, numFiles);
    }

    public void extractPrincipalClasses(String[] info, int numFiles) {
        String methodName = "";
        String finalClass = "";
        String WA;
        String MC;
        String RA;
        int[] readCount = new int[numFiles];
        int[] writeCount = new int[numFiles];
        int[] methodCallCount = new int[numFiles];
        int writeMax1;
        int writeMax2;
        int readMax;
        int methodCallMax;
        int readMaxIndex = 0;
        int writeMaxIndex1 = 0;
        int writeMaxIndex2;
        int methodCallMaxIndex = 0;
        try {
            MethodsDestClass = new BufferedWriter(new FileWriter("InfoFiles/MethodsDestclass.txt"));
            FileInputStream fstreamWriteAttr = new FileInputStream("InfoFiles/WriteAttributes.txt");
            DataInputStream inWriteAttr = new DataInputStream(fstreamWriteAttr);
            BufferedReader writeAttr = new BufferedReader(new InputStreamReader(inWriteAttr));
            FileInputStream fstreamMethodsCalled = new FileInputStream("InfoFiles/MethodsCalled.txt");
            DataInputStream inMethodsCalled = new DataInputStream(fstreamMethodsCalled);
            BufferedReader methodsCalled = new BufferedReader(new InputStreamReader(inMethodsCalled));
            FileInputStream fstreamReadAttr = new FileInputStream("InfoFiles/ReadAttributes.txt");
            DataInputStream inReadAttr = new DataInputStream(fstreamReadAttr);
            BufferedReader readAttr = new BufferedReader(new InputStreamReader(inReadAttr));
            while ((WA = writeAttr.readLine()) != null && (RA = readAttr.readLine()) != null && (MC = methodsCalled.readLine()) != null) {
                WA = writeAttr.readLine();
                RA = readAttr.readLine();
                MC = methodsCalled.readLine();
                while (WA.compareTo("EndOfClass") != 0 && RA.compareTo("EndOfClass") != 0 && MC.compareTo("EndOfClass") != 0) {
                    methodName = writeAttr.readLine();
                    readAttr.readLine();
                    methodsCalled.readLine();
                    WA = writeAttr.readLine();
                    MC = methodsCalled.readLine();
                    RA = readAttr.readLine();
                    while (true) {
                        if (WA.compareTo("EndOfMethod") == 0 && RA.compareTo("EndOfMethod") == 0 && MC.compareTo("EndOfMethod") == 0) {
                            break;
                        }
                        if (WA.compareTo("EndOfMethod") != 0) {
                            if (WA.indexOf(".") > 0) {
                                WA = WA.substring(0, WA.indexOf("."));
                            }
                        }
                        if (RA.compareTo("EndOfMethod") != 0) {
                            if (RA.indexOf(".") > 0) {
                                RA = RA.substring(0, RA.indexOf("."));
                            }
                        }
                        if (MC.compareTo("EndOfMethod") != 0) {
                            if (MC.indexOf(".") > 0) {
                                MC = MC.substring(0, MC.indexOf("."));
                            }
                        }
                        for (int i = 0; i < numFiles && info[i] != null; i++) {
                            if (info[i].compareTo(WA) == 0) {
                                writeCount[i]++;
                            }
                            if (info[i].compareTo(RA) == 0) {
                                readCount[i]++;
                            }
                            if (info[i].compareTo(MC) == 0) {
                                methodCallCount[i]++;
                            }
                        }
                        if (WA.compareTo("EndOfMethod") != 0) {
                            WA = writeAttr.readLine();
                        }
                        if (MC.compareTo("EndOfMethod") != 0) {
                            MC = methodsCalled.readLine();
                        }
                        if (RA.compareTo("EndOfMethod") != 0) {
                            RA = readAttr.readLine();
                        }
                    }
                    WA = writeAttr.readLine();
                    MC = methodsCalled.readLine();
                    RA = readAttr.readLine();
                    writeMax1 = writeCount[0];
                    writeMaxIndex1 = 0;
                    for (int i = 1; i < numFiles; i++) {
                        if (writeCount[i] > writeMax1) {
                            writeMax1 = writeCount[i];
                            writeMaxIndex1 = i;
                        }
                    }
                    writeCount[writeMaxIndex1] = 0;
                    writeMax2 = writeCount[0];
                    writeMaxIndex2 = 0;
                    for (int i = 1; i < numFiles; i++) {
                        if (writeCount[i] > writeMax2) {
                            writeMax2 = writeCount[i];
                            writeMaxIndex2 = i;
                        }
                    }
                    readMax = readCount[0];
                    readMaxIndex = 0;
                    for (int i = 1; i < numFiles; i++) {
                        if (readCount[i] > readMax) {
                            readMax = readCount[i];
                            readMaxIndex = i;
                        }
                    }
                    methodCallMax = methodCallCount[0];
                    methodCallMaxIndex = 0;
                    for (int i = 1; i < numFiles; i++) {
                        if (methodCallCount[i] > methodCallMax) {
                            methodCallMax = methodCallCount[i];
                            methodCallMaxIndex = i;
                        }
                    }
                    boolean isNotEmpty = false;
                    if (writeMax1 > 0 && writeMax2 == 0) {
                        finalClass = info[writeMaxIndex1];
                        isNotEmpty = true;
                    } else if (writeMax1 == 0) {
                        if (readMax != 0) {
                            finalClass = info[readMaxIndex];
                            isNotEmpty = true;
                        } else if (methodCallMax != 0) {
                            finalClass = info[methodCallMaxIndex];
                            isNotEmpty = true;
                        }
                    }
                    if (isNotEmpty == true) {
                        MethodsDestClass.write(methodName);
                        MethodsDestClass.newLine();
                        MethodsDestClass.write(finalClass);
                        MethodsDestClass.newLine();
                        isNotEmpty = false;
                    }
                    for (int j = 0; j < numFiles; j++) {
                        readCount[j] = 0;
                        writeCount[j] = 0;
                        methodCallCount[j] = 0;
                    }
                }
            }
            writeAttr.close();
            methodsCalled.close();
            readAttr.close();
            MethodsDestClass.close();
            int sizeInfoArray = 0;
            sizeInfoArray = infoArraySize();
            boolean classWritten = false;
            principleClass = new String[100];
            principleMethod = new String[100];
            principleMethodsClass = new String[100];
            String infoArray[] = new String[sizeInfoArray];
            String field;
            int counter = 0;
            FileInputStream fstreamDestMethod = new FileInputStream("InfoFiles/MethodsDestclass.txt");
            DataInputStream inDestMethod = new DataInputStream(fstreamDestMethod);
            BufferedReader destMethod = new BufferedReader(new InputStreamReader(inDestMethod));
            PrincipleClassGroup = new BufferedWriter(new FileWriter("InfoFiles/PrincipleClassGroup.txt"));
            while ((field = destMethod.readLine()) != null) {
                infoArray[counter] = field;
                counter++;
            }
            for (int i = 0; i < numFiles; i++) {
                for (int j = 0; j < counter - 1 && info[i] != null; j++) {
                    if (infoArray[j + 1].compareTo(info[i]) == 0) {
                        if (classWritten == false) {
                            PrincipleClassGroup.write(infoArray[j + 1]);
                            PrincipleClassGroup.newLine();
                            principleClass[principleClassCount] = infoArray[j + 1];
                            principleClassCount++;
                            classWritten = true;
                        }
                        PrincipleClassGroup.write(infoArray[j]);
                        principleMethod[principleMethodCount] = infoArray[j];
                        principleMethodsClass[principleMethodCount] = infoArray[j + 1];
                        principleMethodCount++;
                        PrincipleClassGroup.newLine();
                    }
                }
                if (classWritten == true) {
                    PrincipleClassGroup.write("EndOfClass");
                    PrincipleClassGroup.newLine();
                    classWritten = false;
                }
            }
            destMethod.close();
            PrincipleClassGroup.close();
            readFileCount = readFileCount();
            writeFileCount = writeFileCount();
            methodCallFileCount = methodCallFileCount();
            readArray = new String[readFileCount];
            writeArray = new String[writeFileCount];
            callArray = new String[methodCallFileCount];
            initializeArrays();
            constructFundamentalView();
            constructInteractionView();
            constructAssociationView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeArrays() {
        try {
            FileInputStream fstreamRead = new FileInputStream("InfoFiles/ReadAttributes.txt");
            DataInputStream inRead = new DataInputStream(fstreamRead);
            BufferedReader read = new BufferedReader(new InputStreamReader(inRead));
            FileInputStream fstreamWrite = new FileInputStream("InfoFiles/WriteAttributes.txt");
            DataInputStream inWrite = new DataInputStream(fstreamWrite);
            BufferedReader write = new BufferedReader(new InputStreamReader(inWrite));
            FileInputStream fstreamCall = new FileInputStream("InfoFiles/MethodsCalled.txt");
            DataInputStream inCall = new DataInputStream(fstreamCall);
            BufferedReader call = new BufferedReader(new InputStreamReader(inCall));
            for (int i = 0; i < readFileCount; i++) {
                readArray[i] = read.readLine();
            }
            for (int j = 0; j < writeFileCount; j++) {
                writeArray[j] = write.readLine();
            }
            for (int k = 0; k < methodCallFileCount; k++) {
                callArray[k] = call.readLine();
            }
            read.close();
            write.close();
            call.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int infoArraySize() {
        int counter = 0;
        try {
            FileInputStream fstreamInfo = new FileInputStream("InfoFiles/MethodsDestclass.txt");
            DataInputStream inInfo = new DataInputStream(fstreamInfo);
            BufferedReader info = new BufferedReader(new InputStreamReader(inInfo));
            while (info.readLine() != null) {
                counter++;
            }
            info.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counter;
    }

    int readFileCount() {
        int counter = 0;
        try {
            FileInputStream fstreamRead = new FileInputStream("InfoFiles/ReadAttributes.txt");
            DataInputStream inRead = new DataInputStream(fstreamRead);
            BufferedReader read = new BufferedReader(new InputStreamReader(inRead));
            while (read.readLine() != null) {
                counter++;
            }
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counter;
    }

    int writeFileCount() {
        int counter = 0;
        try {
            FileInputStream fstreamWrite = new FileInputStream("InfoFiles/WriteAttributes.txt");
            DataInputStream inWrite = new DataInputStream(fstreamWrite);
            BufferedReader write = new BufferedReader(new InputStreamReader(inWrite));
            while (write.readLine() != null) {
                counter++;
            }
            write.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counter;
    }

    int methodCallFileCount() {
        int counter = 0;
        try {
            FileInputStream fstreamCall = new FileInputStream("InfoFiles/MethodsCalled.txt");
            DataInputStream inCall = new DataInputStream(fstreamCall);
            BufferedReader call = new BufferedReader(new InputStreamReader(inCall));
            while (call.readLine() != null) {
                counter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counter;
    }

    public void constructFundamentalView() {
        String className;
        String methodName;
        String field;
        boolean foundRead = false;
        boolean foundWrite = false;
        boolean classWritten = false;
        try {
            FundView = new BufferedWriter(new FileWriter("InfoFiles/FundamentalView.txt"));
            FileInputStream fstreamPC = new FileInputStream("InfoFiles/PrincipleClassGroup.txt");
            DataInputStream inPC = new DataInputStream(fstreamPC);
            BufferedReader PC = new BufferedReader(new InputStreamReader(inPC));
            while ((field = PC.readLine()) != null) {
                className = field;
                FundView.write(className);
                FundView.newLine();
                classWritten = true;
                while ((methodName = PC.readLine()) != null) {
                    if (methodName.contentEquals("EndOfClass")) break;
                    FundView.write("StartOfMethod");
                    FundView.newLine();
                    FundView.write(methodName);
                    FundView.newLine();
                    for (int i = 0; i < readFileCount && foundRead == false; i++) {
                        if (methodName.compareTo(readArray[i]) == 0) {
                            foundRead = true;
                            for (int j = 1; readArray[i + j].compareTo("EndOfMethod") != 0; j++) {
                                if (readArray[i + j].indexOf(".") > 0) {
                                    field = readArray[i + j].substring(0, readArray[i + j].indexOf("."));
                                    if (field.compareTo(className) == 0) {
                                        FundView.write(readArray[i + j]);
                                        FundView.newLine();
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < writeFileCount && foundWrite == false; i++) {
                        if (methodName.compareTo(writeArray[i]) == 0) {
                            foundWrite = true;
                            for (int j = 1; writeArray[i + j].compareTo("EndOfMethod") != 0; j++) {
                                if (writeArray[i + j].indexOf(".") > 0) {
                                    field = writeArray[i + j].substring(0, writeArray[i + j].indexOf("."));
                                    if (field.compareTo(className) == 0) {
                                        FundView.write(writeArray[i + j]);
                                        FundView.newLine();
                                    }
                                }
                            }
                        }
                    }
                    FundView.write("EndOfMethod");
                    FundView.newLine();
                    foundRead = false;
                    foundWrite = false;
                }
                if (classWritten == true) {
                    FundView.write("EndOfClass");
                    FundView.newLine();
                    classWritten = false;
                }
            }
            PC.close();
            FundView.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void constructInteractionView() {
        String className;
        String methodName;
        String field;
        String nameExtracted;
        String methodsPrincipleClass;
        int searchIndex = 0;
        boolean found = false;
        boolean classWritten = false;
        try {
            IntView = new BufferedWriter(new FileWriter("InfoFiles/InteractionView.txt"));
            FileInputStream fstreamPC = new FileInputStream("InfoFiles/PrincipleClassGroup.txt");
            DataInputStream inPC = new DataInputStream(fstreamPC);
            BufferedReader PC = new BufferedReader(new InputStreamReader(inPC));
            while ((field = PC.readLine()) != null) {
                className = field;
                IntView.write(className);
                IntView.newLine();
                classWritten = true;
                while ((methodName = PC.readLine()) != null) {
                    if (methodName.contentEquals("EndOfClass")) break;
                    IntView.write("StartOfMethod");
                    IntView.newLine();
                    IntView.write(methodName);
                    IntView.newLine();
                    for (int i = 0; i < methodCallFileCount && found == false; i++) {
                        if (methodName.compareTo(callArray[i]) == 0) {
                            found = true;
                            for (int j = 1; callArray[i + j].compareTo("EndOfMethod") != 0; j++) {
                                if (callArray[i + j].indexOf(".") > 0) {
                                    nameExtracted = callArray[i + j].substring(callArray[i + j].indexOf(".") + 1);
                                    System.out.println("Name extracted is:" + nameExtracted);
                                    if (isPrincipleMethod(nameExtracted) == true) {
                                        for (int h = 0; h < principleMethodCount; h++) {
                                            if (nameExtracted.compareTo(principleMethod[h]) == 0) {
                                                searchIndex = h;
                                                break;
                                            }
                                        }
                                        methodsPrincipleClass = principleMethodsClass[searchIndex];
                                        IntView.write(methodsPrincipleClass + "." + nameExtracted);
                                        IntView.newLine();
                                    }
                                }
                            }
                        }
                    }
                    IntView.write("EndOfMethod");
                    IntView.newLine();
                    found = false;
                }
                if (classWritten == true) {
                    IntView.write("EndOfClass");
                    IntView.newLine();
                    classWritten = false;
                }
            }
            PC.close();
            IntView.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void constructAssociationView() {
        String className;
        String methodName;
        String field;
        boolean foundRead = false;
        boolean foundWrite = false;
        boolean classWritten = false;
        try {
            AssocView = new BufferedWriter(new FileWriter("InfoFiles/AssociationView.txt"));
            FileInputStream fstreamPC = new FileInputStream("InfoFiles/PrincipleClassGroup.txt");
            DataInputStream inPC = new DataInputStream(fstreamPC);
            BufferedReader PC = new BufferedReader(new InputStreamReader(inPC));
            while ((field = PC.readLine()) != null) {
                className = field;
                AssocView.write(className);
                AssocView.newLine();
                classWritten = true;
                while ((methodName = PC.readLine()) != null) {
                    if (methodName.contentEquals("EndOfClass")) break;
                    AssocView.write("StartOfMethod");
                    AssocView.newLine();
                    AssocView.write(methodName);
                    AssocView.newLine();
                    for (int i = 0; i < readFileCount && foundRead == false; i++) {
                        if (methodName.compareTo(readArray[i]) == 0) {
                            foundRead = true;
                            for (int j = 1; readArray[i + j].compareTo("EndOfMethod") != 0; j++) {
                                if (readArray[i + j].indexOf(".") > 0) {
                                    field = readArray[i + j].substring(0, readArray[i + j].indexOf("."));
                                    if (isPrincipleClass(field) == true) {
                                        AssocView.write(readArray[i + j]);
                                        AssocView.newLine();
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < writeFileCount && foundWrite == false; i++) {
                        if (methodName.compareTo(writeArray[i]) == 0) {
                            foundWrite = true;
                            for (int j = 1; writeArray[i + j].compareTo("EndOfMethod") != 0; j++) {
                                if (writeArray[i + j].indexOf(".") > 0) {
                                    field = writeArray[i + j].substring(0, writeArray[i + j].indexOf("."));
                                    if (isPrincipleClass(field) == true) {
                                        AssocView.write(writeArray[i + j]);
                                        AssocView.newLine();
                                    }
                                }
                            }
                        }
                    }
                    AssocView.write("EndOfMethod");
                    AssocView.newLine();
                    foundRead = false;
                    foundWrite = false;
                }
                if (classWritten == true) {
                    AssocView.write("EndOfClass");
                    AssocView.newLine();
                    classWritten = false;
                }
            }
            PC.close();
            AssocView.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            System.out.println("Class Visitor " + n.getName());
            ClassNames[index] = n.getName();
        }
    }

    public boolean isPrincipleClass(String field) {
        boolean found = false;
        for (int i = 0; i < principleClassCount; i++) {
            if (principleClass[i].compareTo(field) == 0) {
                found = true;
            }
        }
        return found;
    }

    public boolean isPrincipleMethod(String field) {
        boolean found = false;
        for (int i = 0; i < principleMethodCount; i++) {
            System.out.println("principleMethod" + principleMethod[i]);
            if (principleMethod[i].compareTo(field) == 0) {
                found = true;
            }
        }
        return found;
    }

    private static class LocalClassInstantiationVisitor extends VoidVisitorAdapter<Object> {

        public String changeFormatToInstanceExpr(String expr) {
            int index = 0;
            String instantiatedClass = null;
            String instanceName = null;
            String[] names = expr.split(" ");
            for (int i = 0; i < names.length; i++) {
            }
            if (names.length > 4) {
                index = names.length - 5;
            }
            instantiatedClass = names[index];
            instanceName = names[index + 1];
            return instantiatedClass + " " + instanceName;
        }

        @Override
        public void visit(FieldDeclaration n, Object arg) {
            try {
                String declaredFields = n.toString();
                if (declaredFields.contains(" new")) {
                    String classInstanceExprFormat = changeFormatToInstanceExpr(n.toString());
                    instanceExpr[noOfInstanceExpr++] = new String(classInstanceExprFormat);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ObjectCreationVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(ObjectCreationExpr n, Object arg) {
            try {
                objectCreationExpression[noOfObject++] = new String(n.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClassInstantiationExpr extends VoidVisitorAdapter<Object> {

        public String changeFormatToInstanceExpr(String expr) {
            int index = 0;
            String instantiatedClass = null;
            String instanceName = null;
            String[] names = expr.split(" ");
            if (names.length > 4) {
                index = names.length - 5;
            }
            instantiatedClass = names[index];
            instanceName = names[index + 1];
            return instantiatedClass + " " + instanceName;
        }

        @Override
        public void visit(VariableDeclarationExpr n, Object arg) {
            try {
                super.visit(n, arg);
                instanceExpr[noOfInstanceExpr++] = new String(changeFormatToInstanceExpr(n.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ObjectNameVisitor extends VoidVisitorAdapter<Object> {

        public String changeFormatToInstanceExpr(String expr) {
            int index = 0;
            String instantiatedClass = null;
            String instanceName = null;
            String[] names = expr.split(" ");
            if (names.length > 4) {
                index = names.length - 5;
            }
            instantiatedClass = names[index];
            instanceName = names[index + 1];
            return instantiatedClass + " " + instanceName;
        }

        @Override
        public void visit(AssignExpr n, Object arg) {
            try {
                String expression = n.toString();
                for (int i = 0; i < noOfObject; i++) {
                    if (expression.contains(objectCreationExpression[i])) {
                        instanceExpr[noOfInstanceExpr] = new String(changeFormatToInstanceExpr(expression));
                        noOfInstanceExpr++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class FieldAccessVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            try {
                insideMethod = true;
                AttributesRefered.write("StartOfMethod");
                AttributesRefered.newLine();
                AttributesRefered.write(n.getName());
                AttributesRefered.newLine();
                if (n.getBody() != null) {
                    visit(n.getBody(), arg);
                }
                AttributesRefered.write("EndOfMethod");
                AttributesRefered.newLine();
                insideMethod = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void visit(FieldAccessExpr n, Object arg) {
            try {
                super.visit(n, arg);
                String fieldAccessed = n.toString();
                String field = n.getField();
                String fieldClassName = fieldAccessed.substring(0, fieldAccessed.indexOf("."));
                for (int i = 0; i < noOfInstanceExpr; i++) {
                    if (instanceExpr[i].contentEquals(ClassNames[index])) {
                        i++;
                        while (instanceExpr[i] != null && !instanceExpr[i].contentEquals("End Of Class") && i < noOfInstanceExpr) {
                            if (instanceExpr[i].indexOf(" ") > 0) {
                                String className = instanceExpr[i].substring(0, instanceExpr[i].indexOf(" "));
                                String instanceName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
                                if (insideMethod == true && fieldClassName.contentEquals(instanceName)) {
                                    AttributesRefered.write(className + "." + field);
                                    AttributesRefered.newLine();
                                }
                            }
                            i++;
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private static class MethodInvocationVisitor extends VoidVisitorAdapter<Object> {

        public void visit(MethodDeclaration n, Object arg) {
            try {
                insideMethod = true;
                MethodsCalled.write("StartOfMethod");
                MethodsCalled.newLine();
                MethodsCalled.write(n.getName());
                MethodsCalled.newLine();
                if (n.getBody() != null) visit(n.getBody(), arg);
                MethodsCalled.write("EndOfMethod");
                MethodsCalled.newLine();
                insideMethod = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void visit(MethodCallExpr n, Object arg) {
            try {
                super.visit(n, arg);
                String fieldAccessed = n.toString();
                String field = n.getName();
                String fieldClassName = null;
                if (fieldAccessed.indexOf(".") > 0) {
                    fieldClassName = fieldAccessed.substring(0, fieldAccessed.indexOf("."));
                }
                for (int i = 0; i < noOfInstanceExpr; i++) {
                    if (instanceExpr[i].contentEquals(ClassNames[index])) {
                        i++;
                        while (instanceExpr[i] != null && !instanceExpr[i].contentEquals("EndOfClass") && i < noOfInstanceExpr) {
                            String className = instanceExpr[i].substring(0, instanceExpr[i].indexOf(" "));
                            String instanceName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
                            if (fieldClassName != null && instanceName != null) {
                                if (insideMethod == true && fieldClassName.contentEquals(instanceName)) {
                                    MethodsCalled.write(className + "." + field);
                                    MethodsCalled.newLine();
                                }
                            }
                            i++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            try {
                insideMethod = true;
                LocalMembers.write("StartOfMethod");
                LocalMembers.newLine();
                LocalMembers.write(n.getName());
                LocalMembers.newLine();
                if (n.getBody() != null) visit(n.getBody(), arg);
                LocalMembers.write("EndOfMethod");
                LocalMembers.newLine();
                insideMethod = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void visit(NameExpr ne, Object o) {
            try {
                if (insideMethod == true) {
                    LocalMembers.write(ClassNames[index] + "." + ne.getName());
                    LocalMembers.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isObjectName(String expr) {
        for (int i = 0; i < noOfInstanceExpr; i++) {
            String objName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
            if (expr.contentEquals(objName)) {
                return true;
            }
        }
        return false;
    }

    private static class AttributeWriteVisitor extends VoidVisitorAdapter<Object> {

        public void visit(MethodDeclaration n, Object arg) {
            try {
                insideMethod = true;
                WriteAttributes.write("StartOfMethod");
                WriteAttributes.newLine();
                WriteAttributes.write(n.getName());
                WriteAttributes.newLine();
                if (n.getBody() != null) visit(n.getBody(), arg);
                WriteAttributes.write("EndOfMethod");
                WriteAttributes.newLine();
                insideMethod = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void visit(AssignExpr expr, Object o) {
            try {
                String expression = expr.toString();
                if (insideMethod == true) {
                    for (int fieldAccessIndex = 0; fieldAccessIndex < noOfFieldAccesses; fieldAccessIndex++) {
                        if (fieldsAccesses[fieldAccessIndex].contentEquals(ClassNames[index])) {
                            fieldAccessIndex++;
                            while (fieldsAccesses[fieldAccessIndex] != null && !fieldsAccesses[fieldAccessIndex].contentEquals("EndOfClass") && fieldAccessIndex < noOfFieldAccesses) {
                                placeArrayWrittenInBuffer(expression, fieldAccessIndex);
                                placeAttributeWrittenInBuffer(expression, fieldAccessIndex);
                                fieldAccessIndex++;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String placeAttributeWrittenInBuffer(String expression, int fieldAccessIndex) throws IOException {
            String LeftHandSide = expression.substring(0, expression.indexOf("=") - 1);
            if (LeftHandSide != null) {
                if (LeftHandSide.contentEquals(fieldsAccesses[fieldAccessIndex])) {
                    if (LeftHandSide.indexOf(".") > 0) {
                        String instanceExpr = instanceFormatExpr(LeftHandSide);
                        if (instanceExpr != null) {
                            WriteAttributes.write(instanceExpr);
                            WriteAttributes.newLine();
                        }
                    } else {
                        WriteAttributes.write(ClassNames[index] + "." + LeftHandSide);
                        WriteAttributes.newLine();
                    }
                }
            }
            return LeftHandSide;
        }

        private void placeArrayWrittenInBuffer(String expression, int fieldAccessIndex) throws IOException {
            String ArrayName = null;
            String ArrayClass = null;
            String LeftHandSide = expression.substring(0, expression.indexOf("=") - 1);
            if (LeftHandSide.indexOf(".") < 0 && LeftHandSide.indexOf("[") > 0) {
                ArrayName = LeftHandSide.substring(0, LeftHandSide.indexOf("["));
                if (ArrayName != null) {
                    if (ArrayName.contentEquals(fieldsAccesses[fieldAccessIndex])) {
                        WriteAttributes.write(ClassNames[index] + "." + ArrayName);
                        WriteAttributes.newLine();
                    }
                }
            }
            if (LeftHandSide.indexOf(".") > 0 && LeftHandSide.indexOf("[") > 0 && LeftHandSide.indexOf(".") < LeftHandSide.indexOf("[")) {
                ArrayClass = LeftHandSide.substring(0, LeftHandSide.indexOf("."));
                ArrayName = LeftHandSide.substring(LeftHandSide.indexOf(".") + 1, LeftHandSide.indexOf("["));
                for (int i = 0; i < noOfInstanceExpr; i++) {
                    if (instanceExpr[i].contentEquals(ClassNames[index])) {
                        i++;
                        while (instanceExpr[i] != null && !instanceExpr[i].contentEquals("End Of Class") && i < noOfInstanceExpr) {
                            if (instanceExpr[i].indexOf(" ") > 0) {
                                String className = instanceExpr[i].substring(0, instanceExpr[i].indexOf(" "));
                                String instanceName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
                                if (ArrayClass.contentEquals(instanceName)) {
                                    WriteAttributes.write(className + "." + ArrayName);
                                    WriteAttributes.newLine();
                                }
                            }
                            i++;
                        }
                    }
                }
            }
        }
    }

    public static String instanceFormatExpr(String expression) {
        for (int i = 0; i < noOfFieldAccesses; i++) {
            if (instanceExpr[i] != null && ClassNames[index] != null && instanceExpr[i].contentEquals(ClassNames[index])) {
                i++;
                while (instanceExpr[i] != null && !instanceExpr[i].contentEquals("EndOfClass") && i < noOfInstanceExpr) {
                    String className = instanceExpr[i].substring(0, instanceExpr[i].indexOf(" "));
                    String instanceName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
                    String fieldName = expression.substring(0, expression.indexOf("."));
                    if (fieldName != null && instanceName != null) {
                        if (fieldName.contentEquals(instanceName)) {
                            return className + "." + expression.substring(expression.indexOf(".") + 1, expression.length());
                        }
                    }
                    i++;
                }
            }
        }
        return null;
    }

    private static class AttributeReadVisitor extends VoidVisitorAdapter<Object> {

        public void visit(MethodDeclaration n, Object arg) {
            try {
                insideMethod = true;
                ReadAttributes.write("StartOfMethod");
                ReadAttributes.newLine();
                ReadAttributes.write(n.getName());
                ReadAttributes.newLine();
                if (n.getBody() != null) visit(n.getBody(), arg);
                ReadAttributes.write("EndOfMethod");
                ReadAttributes.newLine();
                insideMethod = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void visit(AssignExpr expr, Object o) {
            try {
                if (insideMethod == true) {
                    String expression = expr.toString();
                    for (int fieldAccessIndex = 0; fieldAccessIndex < noOfFieldAccesses; fieldAccessIndex++) {
                        if (fieldsAccesses[fieldAccessIndex].contentEquals(ClassNames[index])) {
                            while (fieldsAccesses[fieldAccessIndex] != null && !fieldsAccesses[fieldAccessIndex].contentEquals("EndOfClass") && fieldAccessIndex < noOfFieldAccesses) {
                                placeArrayReadInBuffer(expression, fieldAccessIndex);
                                placeAttributesReadInBuffer(expression, fieldAccessIndex);
                                fieldAccessIndex++;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void placeAttributesReadInBuffer(String expression, int fieldAccessIndex) throws IOException {
            String RightHandSide = expression.substring(expression.indexOf("=") + 2, expression.length());
            if (RightHandSide != null) {
                if (RightHandSide.contentEquals(fieldsAccesses[fieldAccessIndex]) && RightHandSide != null) {
                    if (RightHandSide.indexOf(".") > 0) {
                        String instanceExpr = instanceFormatExpr(RightHandSide);
                        if (instanceExpr != null) {
                            ReadAttributes.write(instanceFormatExpr(RightHandSide));
                            ReadAttributes.newLine();
                        }
                    } else {
                        ReadAttributes.write(ClassNames[index] + "." + RightHandSide);
                        ReadAttributes.newLine();
                    }
                }
            }
        }

        private void placeArrayReadInBuffer(String expression, int fieldAccessesIndex) throws IOException {
            String ArrayIndex = null;
            String ArrayName = null;
            String ArrayClass = null;
            String LeftHandSide = expression.substring(0, expression.indexOf("=") - 1);
            String RightHandSide = expression.substring(expression.indexOf("=") + 2, expression.length());
            if (LeftHandSide.indexOf(".") < 0 && LeftHandSide.indexOf("[") > 0) {
                ArrayIndex = LeftHandSide.substring(LeftHandSide.indexOf("[") + 1, LeftHandSide.indexOf("]"));
                if (ArrayIndex != null && fieldsAccesses[fieldAccessesIndex] != null) {
                    if (ArrayIndex.contentEquals(fieldsAccesses[fieldAccessesIndex]) && ArrayIndex != null) {
                        ReadAttributes.write(ClassNames[index] + "." + ArrayIndex);
                        ReadAttributes.newLine();
                    }
                }
            }
            if (RightHandSide.indexOf(".") > 0 && RightHandSide.indexOf("[") > 0 && RightHandSide.indexOf(".") < RightHandSide.indexOf("[")) {
                ArrayClass = RightHandSide.substring(0, RightHandSide.indexOf("."));
                ArrayName = RightHandSide.substring(RightHandSide.indexOf(".") + 1, RightHandSide.indexOf("["));
                for (int i = 0; i < noOfInstanceExpr; i++) {
                    if (instanceExpr[i].contentEquals(ClassNames[index])) {
                        i++;
                        while (instanceExpr[i] != null && !instanceExpr[i].contentEquals("End Of Class") && i < noOfInstanceExpr) {
                            if (instanceExpr[i].indexOf(" ") > 0) {
                                String className = instanceExpr[i].substring(0, instanceExpr[i].indexOf(" "));
                                String instanceName = instanceExpr[i].substring(instanceExpr[i].indexOf(" ") + 1, instanceExpr[i].length());
                                if (ArrayClass.contentEquals(instanceName)) {
                                    ReadAttributes.write(className + "." + ArrayName);
                                    ReadAttributes.newLine();
                                }
                            }
                            i++;
                        }
                    }
                }
            }
        }
    }

    private static class Fields extends VoidVisitorAdapter {

        public void visit(FieldDeclaration n, Object arg) {
            List<VariableDeclarator> aa = n.getVariables();
            try {
                String fieldName = aa.get(0).toString();
                if (fieldName.indexOf(" ") >= 0) {
                    fieldsAccesses[noOfFieldAccesses++] = new String(fieldName.substring(0, fieldName.indexOf(" ")));
                } else if (fieldName.indexOf("=") >= 0) {
                    fieldsAccesses[noOfFieldAccesses++] = new String(fieldName.substring(0, fieldName.indexOf("=")));
                } else {
                    fieldsAccesses[noOfFieldAccesses++] = fieldName;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class FieldAccessedVisitor extends VoidVisitorAdapter<Object> {

        public void visit(FieldAccessExpr n, Object arg) {
            try {
                super.visit(n, arg);
                fieldsAccesses[noOfFieldAccesses++] = new String(n.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
