package com.l2fprod.skinbuilder.synth;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ClassGen;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.renderer.ColorCellRenderer;
import com.l2fprod.common.util.converter.ConverterRegistry;

/**
 * SynthJarBuilder. <br>
 * 
 */
public class SynthJarBuilder {

    private Document doc;

    private File jarPath;

    private String className;

    private SynthConfig config;

    private Map imagesToCopy;

    public SynthJarBuilder(SynthConfig config) {
        this.config = config;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public File getJarPath() {
        return jarPath;
    }

    public void setJarPath(File jarPath) {
        this.jarPath = jarPath;
    }

    public synchronized void write() throws IOException {
        ZipOutputStream jar = new ZipOutputStream(new FileOutputStream(jarPath));
        int index = className.lastIndexOf('.');
        String packageName = className.substring(0, index);
        String clazz = className.substring(index + 1);
        String directory = packageName.replace('.', '/');
        ZipEntry dummyClass = new ZipEntry(directory + "/" + clazz + ".class");
        jar.putNextEntry(dummyClass);
        ClassGen classgen = new ClassGen(getClassName(), "java.lang.Object", "<generated>", Constants.ACC_PUBLIC | Constants.ACC_SUPER, null);
        byte[] bytes = classgen.getJavaClass().getBytes();
        jar.write(bytes);
        jar.closeEntry();
        ZipEntry synthFile = new ZipEntry(directory + "/synth.xml");
        jar.putNextEntry(synthFile);
        Comment comment = new Comment("Generated by SynthBuilder from L2FProd.com");
        Element root = new Element("synth");
        root.addAttribute(new Attribute("version", "1"));
        root.appendChild(comment);
        Element defaultStyle = new Element("style");
        defaultStyle.addAttribute(new Attribute("id", "default"));
        Element defaultFont = new Element("font");
        defaultFont.addAttribute(new Attribute("name", "SansSerif"));
        defaultFont.addAttribute(new Attribute("size", "12"));
        defaultStyle.appendChild(defaultFont);
        Element defaultState = new Element("state");
        defaultStyle.appendChild(defaultState);
        root.appendChild(defaultStyle);
        Element bind = new Element("bind");
        bind.addAttribute(new Attribute("style", "default"));
        bind.addAttribute(new Attribute("type", "region"));
        bind.addAttribute(new Attribute("key", ".*"));
        root.appendChild(bind);
        doc = new Document(root);
        imagesToCopy = new HashMap();
        ComponentStyle[] styles = config.getStyles();
        for (ComponentStyle element : styles) {
            write(element);
        }
        Serializer writer = new Serializer(jar);
        writer.setIndent(2);
        writer.write(doc);
        writer.flush();
        jar.closeEntry();
        for (Iterator iter = imagesToCopy.keySet().iterator(); iter.hasNext(); ) {
            String element = (String) iter.next();
            File pathToImage = (File) imagesToCopy.get(element);
            ZipEntry image = new ZipEntry(directory + "/" + element);
            jar.putNextEntry(image);
            FileInputStream input = new FileInputStream(pathToImage);
            int read = -1;
            while ((read = input.read()) != -1) {
                jar.write(read);
            }
            input.close();
            jar.flush();
            jar.closeEntry();
        }
        jar.flush();
        jar.close();
    }

    private void write(ComponentStyle style) {
        if (!style.isChanged()) {
            return;
        }
        Element node = new Element("style");
        node.addAttribute(new Attribute("id", style.getId()));
        node.addAttribute(new Attribute("clone", "default"));
        String[] states = style.getStates();
        for (String element : states) {
            if (!style.isChanged(element)) {
                continue;
            }
            Element state = new Element("state");
            if (!"ENABLED".equals(element)) {
                state.addAttribute(new Attribute("value", element));
            } else {
            }
            Property[] props = style.getProperties(element);
            Map propValues = new HashMap();
            for (int j = 0; j < props.length; j++) {
                Property clone;
                try {
                    clone = (Property) props[j].clone();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                clone.setValue(style.findPropertyValue(element, props[j].getName(), props[j].getType()));
                props[j] = clone;
                propValues.put(clone.getName(), clone.getValue());
            }
            for (Property p : props) {
                if (p.getValue() != null) {
                    if ("opaque".equals(p.getName())) {
                        Element opaque = new Element("opaque");
                        opaque.addAttribute(new Attribute("value", String.valueOf(p.getValue())));
                        state.appendChild(opaque);
                    } else if ("font".equals(p.getName())) {
                        Font f = (Font) p.getValue();
                        Element fontE = new Element("font");
                        fontE.addAttribute(new Attribute("name", f.getName()));
                        fontE.addAttribute(new Attribute("size", String.valueOf(f.getSize())));
                        if (!f.isPlain()) {
                            String fontStyle = "";
                            if (f.isBold()) {
                                fontStyle = "BOLD";
                            }
                            if (f.isItalic()) {
                                fontStyle += f.isBold() ? " ITALIC" : "ITALIC";
                            }
                            fontE.addAttribute(new Attribute("style", fontStyle));
                        }
                        state.appendChild(fontE);
                    } else if (Color.class.equals(p.getType())) {
                        Color c = (Color) p.getValue();
                        Element colorE = new Element("color");
                        colorE.addAttribute(new Attribute("type", p.getName()));
                        colorE.addAttribute(new Attribute("value", ColorCellRenderer.toHex(c)));
                        state.appendChild(colorE);
                    } else if (p.getName().startsWith("painterCenter")) {
                    } else if (p.getName().startsWith("painterStretch")) {
                    } else if (p.getName().startsWith("painterSourceInsets")) {
                    } else if (p.getName().startsWith("painterDestinationInsets")) {
                    } else if (p.getName().startsWith("painterImage")) {
                        String method = "Background";
                        String methodSuffix = "";
                        int index = p.getName().indexOf(".");
                        if (index != -1) {
                            method = p.getName().substring(index + 1);
                            methodSuffix = "." + method;
                            method = method.toUpperCase().charAt(0) + method.substring(1);
                        }
                        File path = (File) p.getValue();
                        String imageId = "image-" + style.getId() + method + "-" + element;
                        String extension;
                        index = path.getName().lastIndexOf('.');
                        if (index == -1) {
                            extension = "";
                        } else {
                            extension = path.getName().substring(index);
                        }
                        imagesToCopy.put(imageId + extension, path);
                        Element image = new Element("imagePainter");
                        image.addAttribute(new Attribute("id", imageId));
                        image.addAttribute(new Attribute("path", imageId + extension));
                        image.addAttribute(new Attribute("paintCenter", String.valueOf(Boolean.TRUE.equals(propValues.get("painterCenter" + methodSuffix)))));
                        image.addAttribute(new Attribute("stretch", String.valueOf(Boolean.TRUE.equals(propValues.get("painterStretch" + methodSuffix)))));
                        image.addAttribute(new Attribute("method", style.getId().toLowerCase().charAt(0) + style.getId().substring(1) + method));
                        state.appendChild(image);
                        Insets sourceInsets = (Insets) propValues.get("painterSourceInsets" + methodSuffix);
                        if (sourceInsets != null) {
                            image.addAttribute(new Attribute("sourceInsets", String.valueOf(sourceInsets.top) + " " + String.valueOf(sourceInsets.left) + " " + String.valueOf(sourceInsets.bottom) + " " + String.valueOf(sourceInsets.right)));
                        }
                        Insets destinationInsets = (Insets) propValues.get("painterDestinationInsets" + methodSuffix);
                        if (destinationInsets != null) {
                            image.addAttribute(new Attribute("destinationInsets", String.valueOf(destinationInsets.top) + " " + String.valueOf(destinationInsets.left) + " " + String.valueOf(destinationInsets.bottom) + " " + String.valueOf(destinationInsets.right)));
                        }
                    } else if ("insets".equals(p.getName())) {
                        Insets insets = (Insets) p.getValue();
                        Element insetsElement = new Element("insets");
                        insetsElement.addAttribute(new Attribute("top", String.valueOf(insets.top)));
                        insetsElement.addAttribute(new Attribute("left", String.valueOf(insets.left)));
                        insetsElement.addAttribute(new Attribute("bottom", String.valueOf(insets.bottom)));
                        insetsElement.addAttribute(new Attribute("right", String.valueOf(insets.right)));
                        state.appendChild(insetsElement);
                    } else if (p.getName().toLowerCase().endsWith("icon")) {
                        File path = (File) p.getValue();
                        String iconId = "icon-" + style.getId() + "-" + p.getName() + "-" + element;
                        String extension;
                        int index = path.getName().lastIndexOf('.');
                        if (index == -1) {
                            extension = "";
                        } else {
                            extension = path.getName().substring(index);
                        }
                        imagesToCopy.put(iconId + extension, path);
                        Element icon = new Element("imageIcon");
                        icon.addAttribute(new Attribute("id", iconId));
                        icon.addAttribute(new Attribute("path", iconId + extension));
                        Element property = new Element("property");
                        property.addAttribute(new Attribute("key", style.getRegion() + "." + p.getName()));
                        property.addAttribute(new Attribute("value", iconId));
                        state.appendChild(icon);
                        state.appendChild(property);
                    } else {
                        Element property = new Element("property");
                        property.addAttribute(new Attribute("key", style.getRegion() + "." + p.getName()));
                        property.addAttribute(new Attribute("type", getSynthType(p.getType())));
                        property.addAttribute(new Attribute("value", (String) converter.convert(String.class, p.getValue())));
                        state.appendChild(property);
                    }
                }
            }
            if (state.getChildCount() > 0) {
                node.appendChild(state);
            }
        }
        if (node.getChildCount() > 0) {
            doc.getRootElement().appendChild(node);
            Element bind = new Element("bind");
            bind.addAttribute(new Attribute("style", style.getId()));
            bind.addAttribute(new Attribute("type", style.getType()));
            bind.addAttribute(new Attribute("key", style.getId()));
            doc.getRootElement().appendChild(bind);
        }
    }

    static Map<Class, String> typeToSynthType = new HashMap<Class, String>();

    static {
        typeToSynthType.put(boolean.class, "boolean");
        typeToSynthType.put(Boolean.class, "boolean");
        typeToSynthType.put(int.class, "integer");
        typeToSynthType.put(Integer.class, "integer");
        typeToSynthType.put(Insets.class, "insets");
        typeToSynthType.put(Dimension.class, "dimension");
    }

    static String getSynthType(Class clazz) {
        String result = typeToSynthType.get(clazz);
        assert result != null : "no mapping found for " + clazz.getName();
        return result;
    }

    static ConverterRegistry converter = new ConverterRegistry();
}