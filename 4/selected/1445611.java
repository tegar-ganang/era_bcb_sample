package com.antlersoft.classwriter.test;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import com.antlersoft.classwriter.*;
import com.antlersoft.odb.DiskAllocator;
import com.antlersoft.util.NetByte;

public class WriterTest {

    private static Class[] parameterTypes = { (new String[0]).getClass() };

    public static void rewrite(String[] args) throws IOException, CodeCheckException {
        ClassWriter writer = new ClassWriter();
        writer.readClass(new FileInputStream(args[0]));
        for (Iterator i = writer.getMethods().iterator(); i.hasNext(); ) {
            MethodInfo method = (MethodInfo) i.next();
            CodeAttribute attribute = method.getCodeAttribute();
            int origStack = attribute.getMaxStack();
            System.out.print(method.getName());
            attribute.codeCheck();
            System.out.println(" " + origStack + " " + attribute.getMaxStack());
        }
        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(args[1]));
        writer.writeClass(outStream);
        outStream.close();
    }

    public static void testclass(String[] args) throws IOException, CodeCheckException {
        ClassWriter writer = new ClassWriter();
        writer.emptyClass(ClassWriter.ACC_PUBLIC, "TestClass", "java/lang/Object");
        MethodInfo newMethod = writer.addMethod(ClassWriter.ACC_PUBLIC | ClassWriter.ACC_STATIC, "main", "([Ljava/lang/String;)V");
        CodeAttribute attribute = newMethod.getCodeAttribute();
        int constantIndex = writer.getStringConstantIndex("It's alive! It's alive!!");
        int fieldRefIndex = writer.getReferenceIndex(ClassWriter.CONSTANT_Fieldref, "java/lang/System", "out", "Ljava/io/PrintStream;");
        int methodRefIndex = writer.getReferenceIndex(ClassWriter.CONSTANT_Methodref, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        ArrayList instructions = new ArrayList();
        byte[] operands;
        operands = new byte[2];
        NetByte.intToPair(fieldRefIndex, operands, 0);
        instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("getstatic"), 0, operands, false));
        operands = new byte[1];
        operands[0] = (byte) constantIndex;
        instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("ldc"), 0, operands, false));
        operands = new byte[2];
        NetByte.intToPair(methodRefIndex, operands, 0);
        instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("invokevirtual"), 0, operands, false));
        instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("return"), 0, null, false));
        attribute.insertInstructions(0, 0, instructions);
        attribute.setMaxLocals(1);
        attribute.codeCheck();
        System.out.println("constantIndex=" + constantIndex + " fieldRef=" + fieldRefIndex + " methodRef=" + methodRefIndex);
        writer.writeClass(new FileOutputStream("c:/cygnus/home/javaodb/classes/TestClass.class"));
        writer.readClass(new FileInputStream("c:/cygnus/home/javaodb/classes/TestClass.class"));
    }

    public static void entering(String[] args) throws IOException, CodeCheckException {
        ClassWriter writer = new ClassWriter();
        writer.readClass(new BufferedInputStream(new FileInputStream(args[0])));
        int constantIndex = writer.getStringConstantIndex("Entering ");
        int fieldRefIndex = writer.getReferenceIndex(ClassWriter.CONSTANT_Fieldref, "java/lang/System", "out", "Ljava/io/PrintStream;");
        int printlnRefIndex = writer.getReferenceIndex(ClassWriter.CONSTANT_Methodref, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        int printRefIndex = writer.getReferenceIndex(ClassWriter.CONSTANT_Methodref, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");
        for (Iterator i = writer.getMethods().iterator(); i.hasNext(); ) {
            MethodInfo method = (MethodInfo) i.next();
            if (method.getName().equals("readConstant")) continue;
            CodeAttribute attribute = method.getCodeAttribute();
            ArrayList instructions = new ArrayList(10);
            byte[] operands;
            operands = new byte[2];
            NetByte.intToPair(fieldRefIndex, operands, 0);
            instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("getstatic"), 0, operands, false));
            instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("dup"), 0, null, false));
            instructions.add(Instruction.appropriateLdc(constantIndex, false));
            operands = new byte[2];
            NetByte.intToPair(printRefIndex, operands, 0);
            instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("invokevirtual"), 0, operands, false));
            instructions.add(Instruction.appropriateLdc(writer.getStringConstantIndex(method.getName()), false));
            operands = new byte[2];
            NetByte.intToPair(printlnRefIndex, operands, 0);
            instructions.add(new Instruction(OpCode.getOpCodeByMnemonic("invokevirtual"), 0, operands, false));
            attribute.insertInstructions(0, 0, instructions);
            attribute.codeCheck();
        }
        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(args[1]));
        writer.writeClass(outStream);
        outStream.close();
    }

    public static void anyclass(String[] args) throws Throwable {
        String[] newArgs = new String[args.length - 2];
        System.arraycopy(args, 2, newArgs, 0, newArgs.length);
        Class.forName(args[0]).getMethod(args[1], parameterTypes).invoke(null, new Object[] { newArgs });
    }

    public static void walkFreeList(String[] args) throws Throwable {
        DiskAllocator da = new DiskAllocator(new File(args[0]));
        System.out.println(args[0] + " free:");
        da.walkInternalFreeList(System.out);
        System.out.println(args[0] + " used:");
        da.walkRegions(System.out);
        da.close();
    }

    public static void main(String[] args) throws Throwable {
        BufferedReader commands = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
        String line;
        for (line = commands.readLine(); line != null; line = commands.readLine()) {
            if (line.startsWith("#")) continue;
            StringTokenizer tokens = new StringTokenizer(line, "|");
            if (tokens.hasMoreTokens()) {
                String method = tokens.nextToken();
                ArrayList argList = new ArrayList();
                while (tokens.hasMoreTokens()) {
                    argList.add(tokens.nextToken());
                }
                try {
                    WriterTest.class.getMethod(method, parameterTypes).invoke(null, new Object[] { argList.toArray(new String[argList.size()]) });
                } catch (InvocationTargetException ite) {
                    throw ite.getTargetException();
                }
            }
        }
    }
}
