package annone.local;

import java.io.File;
import annone.database.Database;
import annone.database.LocalConnection;
import annone.engine.Channel;
import annone.engine.ComponentId;
import annone.engine.ComponentNotFoundException;
import annone.util.Checks;

public class LocalChannel extends Channel {

    private final LocalEngine engine;

    private final Database metadata;

    private LocalConnection connection;

    private final File location;

    public LocalChannel(LocalEngine engine, String id) {
        super(id);
        this.engine = engine;
        this.metadata = engine.getWorkspace().getMetadata();
        this.location = new File(engine.getWorkspace().getChannelsDirectory(), getId());
    }

    @Override
    public boolean isOpen() {
        return (connection != null);
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public ComponentId getComponentId(String name) throws ComponentNotFoundException {
        Checks.notEmpty("name", name);
        return engine.getComponentId(name);
    }
}
