package annone.ui;

import annone.engine.Channel;
import annone.engine.ComponentId;
import annone.engine.Engine;
import annone.engine.ids.MethodId;
import annone.engine.ids.ObjectId;
import annone.util.Text;

public class InitUserInterfaceTask extends Task {

    private ObjectId userInterfaceTargetId;

    public InitUserInterfaceTask(Ui ui, Layer layer) {
        super(ui, layer, "InitUserInterfaceTask");
    }

    @Override
    protected void runImpl() {
        setDisplayText(Text.get("Starting engine..."));
        Engine engine = ui.getEngine();
        engine.start();
        setDisplayText(Text.get("Opening channel..."));
        Channel channel = engine.openChannel();
        setDisplayText(Text.get("Channel open (''{0}'').", channel.getId()));
        ui.setChannel(channel);
        setDisplayText(Text.get("Initializing user interface..."));
        if (createUserInterface()) setDisplayText(Text.get("User interface initialized."));
    }

    private boolean createUserInterface() {
        ComponentId userInterfaceId = ui.getChannel().getComponentId("annone/userinterface/UserInterface");
        MethodId userInterface_newId = userInterfaceId.getMethodId("new()");
        userInterfaceTargetId = userInterface_newId.invoke(null, null, null);
        if (userInterfaceTargetId == null) {
            addError(new Exception(Text.get("User interface not created.")));
            return false;
        }
        return true;
    }
}
