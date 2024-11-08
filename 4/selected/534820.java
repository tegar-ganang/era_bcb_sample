package jse.jaxb;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author WangShuai
 */
public class Jaxb2Test {

    private JAXBContext context = null;

    private StringWriter writer = null;

    private StringReader reader = null;

    private AccountBean bean = null;

    public Jaxb2Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        bean = new AccountBean();
        bean.setAddress("BeiJing");
        bean.setEmail("email");
        bean.setId(1);
        bean.setName("jack");
        Birthday day = new Birthday();
        day.setDate("2010-11-22");
        bean.setBirthday(day);
        try {
            context = JAXBContext.newInstance(AccountBean.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        context = null;
        bean = null;
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.gc();
    }

    public void out(Object o) {
        System.out.println(o);
    }

    public void failRed(Object o) {
        System.err.println(o);
    }

    public void testBean2XML() {
        try {
            Marshaller mar = context.createMarshaller();
            writer = new StringWriter();
            mar.marshal(bean, writer);
            out(writer);
            reader = new StringReader(writer.toString());
            Unmarshaller unmar = context.createUnmarshaller();
            bean = (AccountBean) unmar.unmarshal(reader);
            out(bean);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void testList2XML() {
        AccountListBean accountBeans = new AccountListBean();
        accountBeans.setName("google");
        List<Object> list = new ArrayList<Object>();
        list.add(bean);
        bean = new AccountBean();
        bean.setAddress("ShangHai");
        bean.setEmail("magicshuai@126.com");
        bean.setId(2);
        bean.setName("magicshuai");
        Birthday day = new Birthday("1987-11-22");
        bean.setBirthday(day);
        list.add(bean);
        accountBeans.setList(list);
        try {
            context = JAXBContext.newInstance(AccountListBean.class);
            Marshaller mar = context.createMarshaller();
            writer = new StringWriter();
            mar.marshal(accountBeans, writer);
            out(writer);
            reader = new StringReader(writer.toString());
            Unmarshaller unmar = context.createUnmarshaller();
            accountBeans = (AccountListBean) unmar.unmarshal(reader);
            out(accountBeans.getList().get(0));
            out(accountBeans.getList().get(1));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMap2XML() {
        AccountMapBean mapBean = new AccountMapBean();
        HashMap<String, AccountBean> map = new HashMap<String, AccountBean>();
        map.put("NO1", bean);
        bean = new AccountBean();
        bean.setAddress("ShangHai");
        bean.setEmail("magicshuai@126.com");
        bean.setId(2);
        bean.setName("magicshuai");
        Birthday day = new Birthday("2010-11-22");
        bean.setBirthday(day);
        map.put("NO2", bean);
        mapBean.setMap(map);
        try {
            context = JAXBContext.newInstance(AccountMapBean.class);
            Marshaller mar = context.createMarshaller();
            writer = new StringWriter();
            mar.marshal(mapBean, writer);
            out(writer);
            reader = new StringReader(writer.toString());
            Unmarshaller unmar = context.createUnmarshaller();
            mapBean = (AccountMapBean) unmar.unmarshal(reader);
            out(mapBean.getMap());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
