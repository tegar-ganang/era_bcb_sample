package org.scribble.export.text;

import org.scribble.export.*;
import org.scribble.extensions.RegistryInfo;
import org.scribble.model.*;

/**
 * This class implements the text based export rule for the
 * Interaction entity.
 */
@RegistryInfo(extension = ExportRule.class)
public class InteractionTextExportRule implements ExportRule {

    /**
	 * This method exports the model object.
	 * 
	 * @param modelObject The model object
	 * @param context The context
	 */
    public boolean isSupported(ModelObject modelObject, Formatter format) {
        return (Interaction.class.isAssignableFrom(modelObject.getClass()) && format instanceof TextFormatter);
    }

    /**
	 * This method exports the model object.
	 * 
	 * @param modelObject The model object
	 * @param context The context
	 * @throws IOException Failed to record export information
	 */
    public void export(ModelObject modelObject, ExporterContext context) throws java.io.IOException {
        Interaction interaction = (Interaction) modelObject;
        if (interaction.getMessageSignature() != null) {
            context.export(interaction.getMessageSignature());
        }
        exportFrom(interaction, context);
        exportTo(interaction, context);
        exportVia(interaction, context);
        exportRequestLabel(interaction, context);
        exportReplyToLabel(interaction, context);
    }

    /**
	 * This method exports the text related to an interactions
	 * 'from' role.
	 * 
	 * @param interaction The interaction
	 * @param context The export context
	 * @throws java.io.IOException Failed to export information
	 */
    protected void exportFrom(Interaction interaction, ExporterContext context) throws java.io.IOException {
        TextFormatter formatter = (TextFormatter) context.getFormatter();
        if (interaction.getFromRole() != null) {
            formatter.record(" from " + interaction.getFromRole().getName());
        }
    }

    /**
	 * This method exports the text related to an interactions
	 * 'to' role.
	 * 
	 * @param interaction The interaction
	 * @param context The export context
	 * @throws java.io.IOException Failed to export information
	 */
    protected void exportTo(Interaction interaction, ExporterContext context) throws java.io.IOException {
        TextFormatter formatter = (TextFormatter) context.getFormatter();
        if (interaction.getToRole() != null) {
            formatter.record(" to " + interaction.getToRole().getName());
        }
    }

    /**
	 * This method exports the text related to an interactions
	 * 'via' channel.
	 * 
	 * @param interaction The interaction
	 * @param context The export context
	 * @throws java.io.IOException Failed to export information
	 */
    protected void exportVia(Interaction interaction, ExporterContext context) throws java.io.IOException {
        TextFormatter formatter = (TextFormatter) context.getFormatter();
        if (interaction.getChannel() != null) {
            formatter.record(" via " + interaction.getChannel().getName());
        }
    }

    /**
	 * This method exports the text related to an interactions
	 * 'request' label.
	 * 
	 * @param interaction The interaction
	 * @param context The export context
	 * @throws java.io.IOException Failed to export information
	 */
    protected void exportRequestLabel(Interaction interaction, ExporterContext context) throws java.io.IOException {
        TextFormatter formatter = (TextFormatter) context.getFormatter();
        if (interaction.getRequestLabel() != null) {
            formatter.record(" request \"" + interaction.getRequestLabel() + "\"");
        }
    }

    /**
	 * This method exports the text related to an interactions
	 * 'replyTo' label.
	 * 
	 * @param interaction The interaction
	 * @param context The export context
	 * @throws java.io.IOException Failed to export information
	 */
    protected void exportReplyToLabel(Interaction interaction, ExporterContext context) throws java.io.IOException {
        TextFormatter formatter = (TextFormatter) context.getFormatter();
        if (interaction.getReplyToLabel() != null) {
            formatter.record(" replyTo \"" + interaction.getReplyToLabel() + "\"");
        }
    }
}
