package de.mpiwg.vspace.diagram.edit.commands;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.common.core.command.ICommand;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.emf.type.core.commands.CreateElementCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.ConfigureRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.CreateElementRequest;
import org.eclipse.gmf.runtime.notation.View;
import de.mpiwg.vspace.diagram.part.ExhibitionDiagramEditorPlugin;
import de.mpiwg.vspace.diagram.util.FunmodeManager;
import de.mpiwg.vspace.filehandler.services.FileHandler;
import de.mpiwg.vspace.metamodel.Exhibition;
import de.mpiwg.vspace.metamodel.ExhibitionFactory;
import de.mpiwg.vspace.metamodel.Scene;

/**
 * @generated NOT
 */
public class SceneCreateCommand extends CreateElementCommand {

    /**
	 * @generated
	 */
    public SceneCreateCommand(CreateElementRequest req) {
        super(req);
    }

    /**
	 * @generated
	 */
    protected EObject getElementToEdit() {
        EObject container = ((CreateElementRequest) getRequest()).getContainer();
        if (container instanceof View) {
            container = ((View) container).getElement();
        }
        return container;
    }

    /**
	 * @generated
	 */
    public boolean canExecute() {
        return true;
    }

    /**
	 * @generated
	 */
    protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        Scene newElement = ExhibitionFactory.eINSTANCE.createScene();
        Exhibition owner = (Exhibition) getElementToEdit();
        owner.getScenes().add(newElement);
        doConfigure(newElement, monitor, info);
        ((CreateElementRequest) getRequest()).setNewElement(newElement);
        return CommandResult.newOKCommandResult(newElement);
    }

    /**
	 * @generated
	 */
    protected void doConfigure(Scene newElement, IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        IElementType elementType = ((CreateElementRequest) getRequest()).getElementType();
        ConfigureRequest configureRequest = new ConfigureRequest(getEditingDomain(), newElement, elementType);
        configureRequest.setClientContext(((CreateElementRequest) getRequest()).getClientContext());
        configureRequest.addParameters(getRequest().getParameters());
        ICommand configureCommand = elementType.getEditCommand(configureRequest);
        if (configureCommand != null && configureCommand.canExecute()) {
            configureCommand.execute(monitor, info);
        }
    }

    @Override
    protected EObject doDefaultElementCreation() {
        EObject element = super.doDefaultElementCreation();
        if (FunmodeManager.INSTANCE.getFunmode() == FunmodeManager.ENABLED) {
            Runnable runnable = new Runnable() {

                public void run() {
                    AudioInputStream inStrom = null;
                    AudioFormat format = null;
                    try {
                        String path = FileHandler.getAbsolutePath(ExhibitionDiagramEditorPlugin.ID, "/files/sounds/Moeb.wav");
                        File datei = new File(path);
                        inStrom = AudioSystem.getAudioInputStream(datei);
                        format = inStrom.getFormat();
                        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                            AudioFormat neu = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 2 * format.getSampleSizeInBits(), format.getChannels(), 2 * format.getFrameSize(), format.getFrameRate(), true);
                            inStrom = AudioSystem.getAudioInputStream(neu, inStrom);
                            format = neu;
                        }
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    SourceDataLine line = null;
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    try {
                        line = (SourceDataLine) AudioSystem.getLine(info);
                        line.open(format);
                        line.start();
                        int num = 0;
                        byte[] audioPuffer = new byte[5000];
                        while (num != -1) {
                            try {
                                num = inStrom.read(audioPuffer, 0, audioPuffer.length);
                                if (num >= 0) line.write(audioPuffer, 0, num);
                            } catch (Exception ex) {
                                System.out.println(ex);
                            }
                        }
                        line.drain();
                        line.close();
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }
        return element;
    }
}
