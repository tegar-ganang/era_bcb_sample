package com.amazon.carbonado.filter;

import java.io.*;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.amazon.carbonado.stored.Address;
import com.amazon.carbonado.stored.Order;
import com.amazon.carbonado.stored.Shipment;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestFilterSerialize extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static TestSuite suite() {
        return new TestSuite(TestFilterSerialize.class);
    }

    public TestFilterSerialize(String name) {
        super(name);
    }

    public void testOpen() throws Exception {
        Filter<Address> filter = Filter.getOpenFilter(Address.class);
        assertTrue(filter == writeAndRead(filter));
    }

    public void testClosed() throws Exception {
        Filter<Address> filter = Filter.getClosedFilter(Address.class);
        assertTrue(filter == writeAndRead(filter));
    }

    public void testExpression() throws Exception {
        {
            Filter<Address> filter = Filter.filterFor(Address.class, "addressID = ?");
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? & addressZip > ?");
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? | addressZip > ?");
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? | (addressZip > ? & customData < ?)");
            assertTrue(filter == writeAndRead(filter));
        }
        {
            Filter<Order> filter = Filter.filterFor(Order.class, "orderID != ? & address.addressZip = ?");
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Order.class, "orderID != ? & (address).addressZip = ?");
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Order.class, "orderID != ? & shipments(shipmentDate > ? & shipmentID != ?)");
            assertTrue(filter == writeAndRead(filter));
        }
    }

    public void testBoundExpression() throws Exception {
        {
            Filter<Address> filter = Filter.filterFor(Address.class, "addressID = ?").bind();
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? & addressZip > ?").bind();
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? | addressZip > ?").bind();
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Address.class, "addressID = ? | (addressZip > ? & customData < ?)").bind();
            assertTrue(filter == writeAndRead(filter));
        }
        {
            Filter<Order> filter = Filter.filterFor(Order.class, "orderID != ? & address.addressZip = ?").bind();
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Order.class, "orderID != ? & (address).addressZip = ?").bind();
            ;
            assertTrue(filter == writeAndRead(filter));
            filter = Filter.filterFor(Order.class, "orderID != ? & shipments(shipmentDate > ? & shipmentID != ?)").bind();
            assertTrue(filter == writeAndRead(filter));
        }
    }

    public void testFilterValues() throws Exception {
        Filter<Address> filter = Filter.filterFor(Address.class, "addressID = ?").bind();
        FilterValues<Address> fv = filter.initialFilterValues().with(5);
        FilterValues<Address> read = writeAndRead(fv);
        assertTrue(fv.getFilter() == read.getFilter());
        assertEquals(fv, read);
        filter = Filter.filterFor(Address.class, "addressID = ? | (addressZip > ? & customData < ?)").bind();
        fv = filter.initialFilterValues().withValues(5, "12345", "foo");
        read = writeAndRead(fv);
        assertTrue(fv.getFilter() == read.getFilter());
        assertEquals(fv, read);
        Filter<Address> inner = Filter.getOpenFilter(Address.class);
        inner = inner.and("addressZip", RelOp.GT, "12345");
        inner = inner.and("customData < ?");
        filter = Filter.filterFor(Address.class, "addressID = ?").or(inner);
        filter = filter.bind();
        fv = filter.initialFilterValues().withValues(5, "foo");
        read = writeAndRead(fv);
        assertTrue(fv.getFilter() == read.getFilter());
        assertEquals(fv, read);
    }

    private <J> J writeAndRead(J obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(obj);
        oout.close();
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bin);
        obj = (J) oin.readObject();
        oin.close();
        return obj;
    }
}
