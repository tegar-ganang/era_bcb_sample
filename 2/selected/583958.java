package org.jpc.emulator.memory.codeblock;

import org.javanile.wrapper.java.util.*;
import org.javanile.wrapper.java.util.jar.*;
import org.javanile.wrapper.java.net.*;
import java.io.*;
import org.javanile.wrapper.java.util.logging.*;
import org.jpc.emulator.memory.codeblock.fastcompiler.FASTCompiler;
import org.jpc.emulator.memory.codeblock.fastcompiler.prot.ProtectedModeTemplateBlock;
import org.jpc.emulator.memory.codeblock.fastcompiler.real.RealModeTemplateBlock;

/**
 *
 * @author Ian Preston
 */
public class CachedCodeBlockCompiler implements CodeBlockCompiler {

    private HashSet availableClassNames = null;

    private boolean loadedClass = false, listedClassNames = false;

    private void getAvailableClassNames() {
        listedClassNames = true;
        try {
            HashSet buffer = new HashSet();
            URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
            URL[] urls = cl.getURLs();
            for (int i = 0; i < urls.length; i++) {
                if (!urls[i].toString().endsWith(".jar")) continue;
                try {
                    URLConnection conn = urls[i].openConnection();
                    conn.setUseCaches(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    JarInputStream jin = new JarInputStream(conn.getInputStream());
                    while (true) {
                        JarEntry jen = jin.getNextJarEntry();
                        if (jen == null) break;
                        String name = jen.getName().trim();
                        if (name.endsWith(".class")) name = name.substring(0, name.length() - 6);
                        name = name.replace("/", ".");
                        name = name.replace("\\", ".");
                        buffer.add(name);
                    }
                    jin.close();
                } catch (Exception e) {
                    System.out.println("Warning: exception listing contents of JAR resource " + urls[i]);
                    e.printStackTrace();
                }
            }
            availableClassNames = buffer;
        } catch (Exception e) {
        }
    }

    public RealModeCodeBlock getRealModeCodeBlock(InstructionSource source) {
        if (!listedClassNames) getAvailableClassNames();
        try {
            int[] newMicrocodes = getMicrocodesArray(source);
            String className = "org.jpc.dynamic.FAST_RM_" + FASTCompiler.getHash(newMicrocodes);
            if (availableClassNames != null) {
                if (!availableClassNames.contains(className)) return null;
            }
            Class oldClass = Class.forName(className);
            int[] oldMicrocodes = ((RealModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
            boolean same = true;
            if (oldMicrocodes.length != newMicrocodes.length) same = false; else {
                for (int i = 0; i < oldMicrocodes.length; i++) {
                    if (oldMicrocodes[i] != newMicrocodes[i]) same = false;
                }
            }
            if (same) {
                if (!loadedClass) {
                    loadedClass = true;
                    System.out.println("Loaded Precompiled Class");
                }
                return (RealModeCodeBlock) oldClass.newInstance();
            } else return null;
        } catch (InstantiationException ex) {
            Logger.getLogger(CachedCodeBlockCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CachedCodeBlockCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VerifyError e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    public ProtectedModeCodeBlock getProtectedModeCodeBlock(InstructionSource source) {
        if (!listedClassNames) getAvailableClassNames();
        try {
            int[] newMicrocodes = getMicrocodesArray(source);
            String className = "org.jpc.dynamic.FAST_PM_" + FASTCompiler.getHash(newMicrocodes);
            if (availableClassNames != null) {
                if (!availableClassNames.contains(className)) return null;
            }
            Class oldClass = Class.forName(className);
            int[] oldMicrocodes = ((ProtectedModeTemplateBlock) oldClass.newInstance()).getMicrocodes();
            boolean same = true;
            if (oldMicrocodes.length != newMicrocodes.length) {
                same = false;
            } else {
                for (int i = 0; i < oldMicrocodes.length; i++) {
                    if (oldMicrocodes[i] != newMicrocodes[i]) {
                        same = false;
                    }
                }
            }
            if (same) {
                if (!loadedClass) {
                    loadedClass = true;
                    System.out.println("Loaded Precompiled Class");
                }
                return (ProtectedModeCodeBlock) oldClass.newInstance();
            } else return null;
        } catch (InstantiationException ex) {
            Logger.getLogger(CachedCodeBlockCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CachedCodeBlockCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VerifyError e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(InstructionSource source) {
        if (!listedClassNames) getAvailableClassNames();
        return null;
    }

    private int[] getMicrocodesArray(InstructionSource source) {
        source.reset();
        List m = new ArrayList();
        while (source.getNext()) {
            int uCodeLength = source.getLength();
            for (int i = 0; i < uCodeLength; i++) {
                int data = source.getMicrocode();
                m.add(data);
            }
        }
        int[] ans = new int[m.size()];
        for (int i = 0; i < ans.length; i++) ans[i] = m.get(i);
        return ans;
    }
}
