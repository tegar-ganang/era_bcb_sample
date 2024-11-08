package com.google.code.jetm.reporting.xml;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import etm.core.aggregation.Aggregate;

/**
 * Unit tests for {@link XmlAggregateBinderTest}.
 * 
 * @author jrh3k5
 * 
 */
public class XmlAggregateBinderTest {

    private final XmlAggregateBinder binder = new XmlAggregateBinder();

    /**
     * Test the marshalling of an aggregate to XML.
     * 
     * @throws Exception
     *             If any errors occur during the test run.
     */
    @Test
    public void testTransform() throws Exception {
        final double min = 1.0;
        final double max = 2.0;
        final double total = 4.0;
        final long measurements = 1500;
        final String name = "measurement-name";
        final XmlAggregate aggregate = mock(XmlAggregate.class);
        when(aggregate.getMax()).thenReturn(max);
        when(aggregate.getMin()).thenReturn(min);
        when(aggregate.getTotal()).thenReturn(total);
        when(aggregate.getMeasurements()).thenReturn(measurements);
        when(aggregate.getName()).thenReturn(name);
        final StringWriter writer = new StringWriter();
        binder.bind(Collections.singletonList(aggregate), writer);
        final StringReader reader = new StringReader(writer.toString());
        final Collection<Aggregate> unmarshalledList = binder.unbind(reader);
        assertThat(unmarshalledList).hasSize(1);
        final Aggregate unmarshalled = unmarshalledList.iterator().next();
        assertThat(unmarshalled.getMax()).isEqualTo(max);
        assertThat(unmarshalled.getMin()).isEqualTo(min);
        assertThat(unmarshalled.getTotal()).isEqualTo(total);
        assertThat(unmarshalled.getMeasurements()).isEqualTo(measurements);
        assertThat(unmarshalled.getName()).isEqualTo(name);
    }
}
