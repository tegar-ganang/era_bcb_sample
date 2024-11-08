package com.volantis.mcs.migrate.impl.framework;

import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.mcs.migrate.api.framework.InputMetadata;
import com.volantis.mcs.migrate.api.framework.OutputCreator;
import com.volantis.mcs.migrate.api.framework.ResourceMigrationException;
import com.volantis.mcs.migrate.api.framework.ResourceMigrator;
import com.volantis.mcs.migrate.api.framework.StepType;
import com.volantis.mcs.migrate.api.notification.NotificationType;
import com.volantis.mcs.migrate.impl.framework.identification.Match;
import com.volantis.mcs.migrate.impl.framework.identification.ResourceIdentifier;
import com.volantis.mcs.migrate.impl.framework.identification.Step;
import com.volantis.mcs.migrate.impl.framework.io.RestartInputStream;
import com.volantis.mcs.migrate.impl.framework.io.StreamBuffer;
import com.volantis.mcs.migrate.impl.framework.io.StreamBufferFactory;
import com.volantis.mcs.migrate.notification.NotificationFactory;
import com.volantis.mcs.migrate.api.notification.NotificationReporter;
import com.volantis.synergetics.io.IOUtils;
import com.volantis.synergetics.log.LogDispatcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Default implementation of {@link ResourceMigrator}.
 * <p>
 * This is implemented to delegate all the work of identification off to
 * a resource identifier, and then take the migration steps it returns and
 * iterate over them, connecting any intermedieate inputs and outputs together
 * where nececssary, and processing each one to migrate30 the input version to
 * the target version via all the intermediate versions (if any).
 *
 * todo: later: allow debug logging of intermediate version content.
 */
public class DefaultResourceMigrator implements ResourceMigrator {

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(DefaultResourceMigrator.class);

    NotificationFactory notificationFactory = NotificationFactory.getDefaultInstance();

    /**
     * A factory to create stream buffers if we need to have intermediate
     * versions.
     */
    private StreamBufferFactory streamBufferFactory;

    /**
     * The resource identifier that does all the hard work of identifying the
     * resource and deciding which steps are required to migrate30 it.
     */
    private ResourceIdentifier resourceIdentifier;

    /**
     * Used to report user messages.
     */
    private NotificationReporter reporter;

    /**
     * Initialise.
     *
     * @param streamBufferFactory creates stream buffers used for intermediate
     *      versions as required.
     * @param resourceIdentifier does all the heavy lifting of identification.
     * @param reporter used to report notifications to the user.
     */
    public DefaultResourceMigrator(StreamBufferFactory streamBufferFactory, ResourceIdentifier resourceIdentifier, NotificationReporter reporter) {
        this.streamBufferFactory = streamBufferFactory;
        this.resourceIdentifier = resourceIdentifier;
        this.reporter = reporter;
    }

    public void migrate(InputMetadata meta, InputStream input, OutputCreator outputCreator) throws IOException, ResourceMigrationException {
        RestartInputStream restartInput = new RestartInputStream(input);
        Match match = resourceIdentifier.identifyResource(meta, restartInput);
        restartInput.restart();
        if (match != null) {
            reporter.reportNotification(notificationFactory.createLocalizedNotification(NotificationType.INFO, "migration-resource-migrating", new Object[] { meta.getURI(), match.getTypeName(), match.getVersionName() }));
            processMigrationSteps(match, restartInput, outputCreator);
        } else {
            reporter.reportNotification(notificationFactory.createLocalizedNotification(NotificationType.INFO, "migration-resource-copying", new Object[] { meta.getURI() }));
            IOUtils.copyAndClose(restartInput, outputCreator.createOutputStream());
        }
    }

    /**
     * Given a sequence of migration steps, process each in turn in order to
     * translate the input version into the target version via any intermediate
     * versions required.
     *
     * @param match the identification match containing the sequence of steps
     *      to perform.
     * @param inputStream the input data.
     * @param outputCreator an object which allows us to create the output
     *      when required.
     * @throws ResourceMigrationException if a migration error occurs.
     */
    private void processMigrationSteps(Match match, InputStream inputStream, OutputCreator outputCreator) throws ResourceMigrationException {
        Iterator steps = match.getSequence();
        if (steps.hasNext()) {
            StreamBuffer buffer = null;
            InputStream stepInput;
            OutputStream stepOutput;
            StepType stepType = null;
            while (steps.hasNext()) {
                final Step step = (Step) steps.next();
                if (logger.isDebugEnabled()) {
                    logger.debug("Invoking migration step :" + step);
                }
                if (buffer != null) {
                    stepInput = buffer.getInput();
                } else {
                    stepInput = inputStream;
                }
                if (steps.hasNext()) {
                    buffer = streamBufferFactory.create();
                    stepOutput = buffer.getOutput();
                } else {
                    stepOutput = outputCreator.createOutputStream();
                }
                stepType = getCurrentStepType(stepType, steps);
                Exception firstException = null;
                try {
                    step.migrate(stepInput, stepOutput, stepType);
                } catch (ResourceMigrationException e) {
                    firstException = e;
                } finally {
                    try {
                        stepInput.close();
                    } catch (IOException e) {
                        if (firstException == null) {
                            firstException = e;
                        }
                    }
                    try {
                        stepOutput.close();
                    } catch (IOException e) {
                        if (firstException == null) {
                            firstException = e;
                        }
                    }
                }
                if (firstException != null) {
                    throw new ResourceMigrationException("Error processing type " + match.getTypeName() + ", step " + step, firstException);
                }
            }
        } else {
            throw new IllegalStateException("No steps found!");
        }
    }

    /**
     * Determine the type of the current step.
     *
     * @param oldStepType of the last step
     * @param steps iterator
     * @return type of the current step
     */
    private StepType getCurrentStepType(StepType oldStepType, Iterator steps) {
        final boolean firstStep = (oldStepType == null);
        final boolean lastStep = !steps.hasNext();
        return StepType.getType(firstStep, lastStep);
    }
}
