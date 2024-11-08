package com.vividsolutions.jump.io.datasource;

import java.util.ArrayList;
import java.util.Collection;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.io.JUMPWriter;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * Adapts the old JUMP I/O API (Readers and Writers) to the new JUMP I/O API
 * (DataSources).
 */
public class ReaderWriterFileDataSource extends DataSource {

    protected JUMPReader reader;

    protected JUMPWriter writer;

    public ReaderWriterFileDataSource(JUMPReader reader, JUMPWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public Connection getConnection() {
        return new Connection() {

            public FeatureCollection executeQuery(String query, Collection exceptions, TaskMonitor monitor) {
                try {
                    return reader.read(getReaderDriverProperties());
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            }

            public void executeUpdate(String update, FeatureCollection featureCollection, TaskMonitor monitor) throws Exception {
                writer.write(featureCollection, getWriterDriverProperties());
            }

            public void close() {
            }

            public FeatureCollection executeQuery(String query, TaskMonitor monitor) throws Exception {
                ArrayList exceptions = new ArrayList();
                FeatureCollection featureCollection = executeQuery(query, exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }
        };
    }

    protected DriverProperties getReaderDriverProperties() {
        return getDriverProperties();
    }

    protected DriverProperties getWriterDriverProperties() {
        return getDriverProperties();
    }

    private DriverProperties getDriverProperties() {
        DriverProperties properties = new DriverProperties();
        properties.putAll(getProperties());
        return properties;
    }
}
