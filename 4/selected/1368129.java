package se.snigel.net.servlet.util;

import se.snigel.net.servlet.ServletRequest;
import se.snigel.net.servlet.ServletResponse;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * User: kalle
 * Date: 2004-mar-20
 * Time: 20:26:30
 */
public class BeanForm {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, hh:mm.ss");

    private String namePrefix;

    private Object bean;

    public Object getBean() {
        return bean;
    }

    public BeanForm(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public BeanForm(Object bean) {
        this.bean = bean;
    }

    public BeanForm(String namePrefix, Object bean) {
        this.namePrefix = namePrefix;
        this.bean = bean;
    }

    public void removeBeanFormFromSession(ServletRequest request, ServletResponse response) throws IOException {
        StringBuffer tmp = new StringBuffer();
        if (namePrefix != null) tmp.append(namePrefix);
        String prefix = tmp.toString();
        String id = request.getParameter(prefix + "_bean.id");
        if (id == null) id = String.valueOf(System.currentTimeMillis());
        request.getSession().setAttribute(prefix + "_bean.id." + id, null);
    }

    public boolean renderBeanForm(ServletRequest request, ServletResponse response) throws IOException {
        return renderBeanForm(request, response, response.getWriter());
    }

    public boolean renderBeanForm(ServletRequest request, ServletResponse response, PrintWriter out) throws IOException {
        StringWriter buf = new StringWriter();
        StringBuffer tmp = new StringBuffer();
        if (namePrefix != null) tmp.append(namePrefix);
        String prefix = tmp.toString();
        try {
            String id = request.getParameter(prefix + "_bean.id");
            if (id == null) id = String.valueOf(System.currentTimeMillis());
            Object bean = this.bean;
            if (bean == null) bean = request.getSession().getAttribute(prefix + "_bean.id." + id);
            this.bean = bean;
            BeanInfo bi = Introspector.getBeanInfo(bean.getClass());
            Object[][] listview = new Object[bi.getPropertyDescriptors().length + 1][];
            listview[0] = new Object[] { "Attribute", "Value" };
            for (int i = 0; i < bi.getPropertyDescriptors().length; i++) {
                String attributeName = null;
                PropertyDescriptor pd = bi.getPropertyDescriptors()[i];
                tmp = new StringBuffer();
                tmp.append(prefix);
                tmp.append(pd.getName());
                String formParameterName = tmp.toString();
                attributeName = pd.getName();
                listview[i + 1] = new Object[] { attributeName, renderPropertyDescriptorEditor(request, response, formParameterName, pd, bean) };
            }
            buf.write("<table border=0 cellspacing=0 cellpadding=5 bgcolor=#ffffff><tr><td>");
            if (bean != null) {
                buf.write("Instance of <b>");
                buf.write(bean.getClass().getName());
                buf.write("</b><br>");
            }
            Listview.render(listview, new PrintWriter(buf));
            buf.write("<input type='checkbox' name='");
            buf.write(prefix);
            buf.write("commit'><input type='hidden' name='");
            buf.write(prefix);
            buf.write("_bean.id' value='");
            buf.write(id);
            buf.write("'> Commit &nbsp; <input type='submit'></td></tr></table>");
            out.write(buf.toString());
            request.getSession().setAttribute(prefix + "_bean.id." + id, bean);
            if (request.getParameter(prefix + "commit") != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("<hr><pre>");
            e.printStackTrace(new PrintWriter(out));
            out.write("</pre><hr>");
            if (buf != null) out.write(buf.toString());
        }
        return false;
    }

    public static String renderPropertyDescriptorEditor(ServletRequest request, ServletResponse response, String formParameterName, PropertyDescriptor pd, Object bean) {
        if (pd.getReadMethod() != null) {
            Class returnType = pd.getReadMethod().getReturnType();
            try {
                StringWriter buf = new StringWriter();
                if (pd.getReadMethod().getParameterTypes().length == 0) {
                    Object obj = pd.getReadMethod().invoke(bean, null);
                    if (obj == null) {
                        if (pd.getWriteMethod() != null) {
                            if (request.getParameter(formParameterName + "_newInstance") != null) {
                                obj = pd.getReadMethod().getReturnType().newInstance();
                                pd.getWriteMethod().invoke(bean, new Object[] { obj });
                            }
                        }
                    }
                    if (boolean.class.equals(returnType) || Boolean.class.equals(returnType)) {
                        boolean val = ((Boolean) obj).booleanValue();
                        boolean in = "on".equalsIgnoreCase(request.getParameter(formParameterName));
                        if (in != val) {
                            pd.getWriteMethod().invoke(bean, new Object[] { new Boolean(in) });
                            val = ((Boolean) pd.getReadMethod().invoke(bean, null)).booleanValue();
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='checkbox'");
                            if (val) buf.write(" checked");
                            buf.write("' enabled='false'>");
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='checkbox'");
                            if (val) buf.write(" checked");
                            buf.write(">");
                        }
                    } else if (short.class.equals(returnType) || Short.class.equals(returnType)) {
                        short val = ((Short) obj).shortValue();
                        if (request.getParameterMap().containsKey(formParameterName)) {
                            short in = Short.parseShort(request.getParameter(formParameterName));
                            if (in != val) {
                                pd.getWriteMethod().invoke(bean, new Object[] { new Short(in) });
                                val = ((Short) pd.getReadMethod().invoke(bean, null)).shortValue();
                            }
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write(String.valueOf(val));
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size=16 value='");
                            buf.write(String.valueOf(val));
                            buf.write("'>");
                        }
                    } else if (int.class.equals(returnType) || Integer.class.equals(returnType)) {
                        int val = ((Integer) obj).intValue();
                        if (request.getParameterMap().containsKey(formParameterName)) {
                            int in = Integer.parseInt(request.getParameter(formParameterName));
                            if (in != val) {
                                pd.getWriteMethod().invoke(bean, new Object[] { new Integer(in) });
                                val = ((Integer) pd.getReadMethod().invoke(bean, null)).intValue();
                            }
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write(String.valueOf(val));
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size=32 value='");
                            buf.write(String.valueOf(val));
                            buf.write("'>");
                        }
                    } else if (long.class.equals(returnType) || Long.class.equals(returnType)) {
                        long val = ((Long) obj).longValue();
                        if (request.getParameterMap().containsKey(formParameterName)) {
                            long in = Long.parseLong(request.getParameter(formParameterName));
                            if (in != val) {
                                pd.getWriteMethod().invoke(bean, new Object[] { new Long(in) });
                                val = ((Long) pd.getReadMethod().invoke(bean, null)).longValue();
                            }
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write(String.valueOf(val));
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size=64 value='");
                            buf.write(String.valueOf(val));
                            buf.write("'>");
                        }
                    } else if (double.class.equals(returnType) || Double.class.equals(returnType)) {
                        double val = ((Double) obj).doubleValue();
                        if (request.getParameterMap().containsKey(formParameterName)) {
                            double in = Double.parseDouble(request.getParameter(formParameterName));
                            if (in != val) {
                                pd.getWriteMethod().invoke(bean, new Object[] { new Double(in) });
                                val = ((Double) pd.getReadMethod().invoke(bean, null)).doubleValue();
                            }
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write(String.valueOf(val));
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size=64 value='");
                            buf.write(String.valueOf(val));
                            buf.write("'>");
                        }
                    } else if (obj instanceof String) {
                        String val = (String) obj;
                        if (pd.getWriteMethod() != null) {
                            String in;
                            if ((in = request.getParameter(formParameterName)) != null && !in.equals(val)) {
                                pd.getWriteMethod().invoke(bean, new Object[] { in });
                                val = (String) pd.getReadMethod().invoke(bean, null);
                            }
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size='75' value='");
                            buf.write(val);
                            buf.write("'>");
                        } else {
                            buf.write(val);
                        }
                    } else if (obj instanceof Date) {
                        Date d = (Date) obj;
                        String formattedDate = sdf.format(d);
                        String in;
                        if ((in = request.getParameter(formParameterName)) != null && !in.equals(formattedDate)) {
                            pd.getWriteMethod().invoke(bean, new Object[] { sdf.parse(in) });
                            formattedDate = sdf.format((Date) pd.getReadMethod().invoke(bean, null));
                        }
                        if (pd.getWriteMethod() == null) {
                            buf.write(formattedDate);
                        } else {
                            buf.write("<input name='");
                            buf.write(formParameterName);
                            buf.write("' type='text' size=20 value='");
                            buf.write(formattedDate);
                            buf.write("'>");
                        }
                    } else if (obj instanceof Collection) {
                        Collection coll = (Collection) obj;
                        if (request.getParameter(formParameterName + "_newInstance") != null && !"".equals(request.getParameter(formParameterName + "_newInstance"))) {
                            Object instance = Class.forName(request.getParameter(formParameterName + "_newInstance"));
                            coll.add(instance);
                            buf.write("New instance added to list<br>.");
                        }
                        buf.write("<br>");
                        buf.write("<input type='text' size=35 name='");
                        buf.write(formParameterName);
                        buf.write("_newInstance");
                        buf.write("'><input type='submit' value='add new instance to list'><br>");
                        if (coll.size() == 0) {
                            buf.write("Empty list");
                        } else {
                            ArrayList pds = null;
                            ArrayList listviewData = null;
                            Class lastClass = null;
                            Iterator it = coll.iterator();
                            while (it.hasNext()) {
                                Object collectionItem = it.next();
                                BeanInfo collectionBeanInfo = Introspector.getBeanInfo(collectionItem.getClass());
                                if (!collectionItem.getClass().equals(lastClass)) {
                                    if (listviewData != null && listviewData.size() > 0) renderBeanCollectionSegment(pds, listviewData, new PrintWriter(buf), lastClass);
                                    pds = new ArrayList();
                                    listviewData = new ArrayList();
                                    for (int i = 0; i < collectionBeanInfo.getPropertyDescriptors().length; i++) {
                                        PropertyDescriptor collectionPropertyDescriptor = collectionBeanInfo.getPropertyDescriptors()[i];
                                        if ((collectionPropertyDescriptor.getReadMethod() != null && collectionPropertyDescriptor.getReadMethod().getParameterTypes().length == 0) && ((collectionPropertyDescriptor.getReadMethod() != null && collectionPropertyDescriptor.getWriteMethod() == null) || (collectionPropertyDescriptor.getReadMethod() != null && collectionPropertyDescriptor.getWriteMethod() != null))) {
                                            pds.add(collectionPropertyDescriptor);
                                        }
                                    }
                                }
                                Object[] row = new Object[pds.size()];
                                for (int i = 0; i < pds.size(); i++) {
                                    PropertyDescriptor collectionPropertyDescriptor = (PropertyDescriptor) pds.get(i);
                                    StringBuffer tmp = new StringBuffer();
                                    tmp.append(formParameterName);
                                    tmp.append('.');
                                    tmp.append(i);
                                    tmp.append('.');
                                    String colletionItemFormParameterName = tmp.toString();
                                    row[i] = BeanForm.renderPropertyDescriptorEditor(request, response, colletionItemFormParameterName, collectionPropertyDescriptor, collectionItem);
                                }
                                listviewData.add(row);
                                lastClass = collectionItem.getClass();
                            }
                            if (listviewData != null && listviewData.size() > 0) renderBeanCollectionSegment(pds, listviewData, new PrintWriter(buf), lastClass);
                        }
                    } else {
                        if (obj == null) {
                            buf.write("<b>null</b> ");
                            if (pd.getWriteMethod() != null) {
                                buf.write("<input name='");
                                buf.write(formParameterName + "_newInstance' type='checkbox'> create new instance.");
                            }
                        } else {
                            buf.write("<input type='checkbox' name='");
                            buf.write(formParameterName);
                            buf.write("_maximized'");
                            if ("on".equalsIgnoreCase(request.getParameter(formParameterName + "_maximized"))) {
                                buf.write(" checked> maximized<br>");
                                BeanForm subForm = new BeanForm(formParameterName + ".", obj);
                                if (subForm.renderBeanForm(request, response, new PrintWriter(buf))) {
                                    pd.getWriteMethod().invoke(bean, new Object[] { subForm.getBean() });
                                }
                            } else {
                                buf.write("> maximized<br>");
                            }
                            if (pd.getReadMethod() != null && pd.getWriteMethod() == null) {
                                buf.write("<b>read only<b>");
                            } else if (pd.getWriteMethod() != null && obj == null) {
                            }
                        }
                    }
                    return buf.toString();
                } else {
                    return "parameters required";
                }
            } catch (Exception e) {
                StringWriter out = new StringWriter();
                out.write("<pre>");
                e.printStackTrace(new PrintWriter(out));
                out.write("</pre>");
                return out.toString();
            }
        } else {
            return "no reader method";
        }
    }

    private static void renderBeanCollectionSegment(ArrayList pds, ArrayList listviewData, PrintWriter out, Class listClass) throws IOException {
        out.write("<b>");
        out.write(listClass.getName());
        out.write("</b>");
        Object[][] listview = new Object[listviewData.size() + 1][];
        listview[0] = new Object[pds.size()];
        for (int i = 0; i < pds.size(); i++) listview[0][i] = ((PropertyDescriptor) pds.get(i)).getDisplayName();
        for (int i = 0; i < listviewData.size(); i++) listview[i + 1] = (Object[]) listviewData.get(i);
        Listview.render(listview, out);
        out.write("<br>");
    }
}
