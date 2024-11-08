package frost.transferlayer;

import java.io.*;
import java.util.logging.*;
import javax.swing.*;
import frost.*;
import frost.crypt.*;
import frost.fcp.*;
import frost.gui.*;
import frost.identities.*;
import frost.messages.*;
import frost.storage.perst.*;
import frost.util.*;
import frost.util.gui.translation.*;

/**
 * This class uploads a message file to freenet. The preparation of the
 * file that is uploaded is done differently for freenet 0.5 and freenet 0.7.
 * To accomplish this the abstract method prepareMessage() is called, which is
 * implemented by MessageUploader05 and MessageUploader07.
 */
public class MessageUploader {

    private static final Logger logger = Logger.getLogger(MessageUploader.class.getName());

    /**
     * The work area for MessageUploader.
     */
    static class MessageUploaderWorkArea {

        MessageXmlFile message;

        File uploadFile;

        File unsentMessageFile;

        MessageUploaderCallback callback;

        byte[] signMetadata;

        Identity encryptForRecipient;

        LocalIdentity senderId;

        JFrame parentFrame;

        IndexSlot indexSlot;

        String logBoardName;
    }

    /**
     * Create a file to upload from the message.
     * Sets the MessageUploaderWorkArea.uploadFile value.
     * @return  true if successful, false otherwise
     */
    protected static boolean prepareMessage(final MessageUploaderWorkArea wa) {
        if (FcpHandler.isFreenet05()) {
            return prepareMessage05(wa);
        } else if (FcpHandler.isFreenet07()) {
            return prepareMessage07(wa);
        } else {
            logger.severe("Unsupported freenet version, not 0.5 or 0.7");
            return false;
        }
    }

    /**
     * Prepares and uploads the message.
     * Returns -1 if upload failed (unsentMessageFile should stay in unsent msgs folder in this case)
     * or returns a value >= 0 containing the final index where the message was uploaded to.
     *
     * If senderId is provided, the message is signed with this ID.
     * If senderId is null the message is sent anonymously.
     *
     */
    public static MessageUploaderResult uploadMessage(final MessageXmlFile message, final Identity encryptForRecipient, final LocalIdentity senderId, final MessageUploaderCallback callback, final IndexSlot indexSlot, final JFrame parentFrame, final String logBoardName) {
        final MessageUploaderWorkArea wa = new MessageUploaderWorkArea();
        wa.message = message;
        wa.unsentMessageFile = message.getFile();
        wa.parentFrame = parentFrame;
        wa.callback = callback;
        wa.indexSlot = indexSlot;
        wa.encryptForRecipient = encryptForRecipient;
        wa.senderId = senderId;
        wa.logBoardName = logBoardName;
        wa.uploadFile = new File(wa.unsentMessageFile.getPath() + ".upltmp");
        wa.uploadFile.delete();
        wa.uploadFile.deleteOnExit();
        if (prepareMessage(wa) == false) {
            return new MessageUploaderResult(true);
        }
        try {
            return uploadMessage(wa);
        } catch (final IOException ex) {
            logger.log(Level.SEVERE, "ERROR: Unexpected IOException, upload stopped.", ex);
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "Oo. EXCEPTION in MessageUploadThread", t);
        }
        return new MessageUploaderResult(true);
    }

    /**
     * Upload the message file.
     */
    protected static MessageUploaderResult uploadMessage(final MessageUploaderWorkArea wa) throws IOException {
        logger.info("TOFUP: Uploading message to board '" + wa.logBoardName + "' with HTL " + Core.frostSettings.getIntValue(SettingsClass.MESSAGE_UPLOAD_HTL));
        boolean tryAgain;
        do {
            boolean success = false;
            int index = -1;
            int tries = 0;
            final int maxTries = 10;
            boolean error = false;
            boolean retrySameIndex = false;
            String logInfo = null;
            while (success == false && error == false) {
                if (retrySameIndex == false) {
                    if (index < 0) {
                        index = wa.indexSlot.findFirstUploadSlot();
                    } else {
                        index = wa.indexSlot.findNextUploadSlot(index);
                    }
                } else {
                    retrySameIndex = false;
                }
                FcpResultPut result = null;
                try {
                    final String upKey = wa.callback.composeUploadKey(wa.message, index);
                    logInfo = " board=" + wa.logBoardName + ", key=" + upKey;
                    result = FcpHandler.inst().putFile(FcpHandler.TYPE_MESSAGE, upKey, wa.uploadFile, wa.signMetadata, false, false, true);
                } catch (final Throwable t) {
                    logger.log(Level.SEVERE, "TOFUP: Error in FcpInsert.putFile." + logInfo, t);
                }
                final int waitTime = 15000;
                if (result.isRetry()) {
                    logger.severe("TOFUP: Message upload failed (RouteNotFound)!\n" + logInfo + "\n(try no. " + tries + " of " + maxTries + "), retrying index " + index);
                    tries++;
                    retrySameIndex = true;
                    Mixed.wait(waitTime);
                } else if (result.isSuccess()) {
                    final File tmpFile = new File(wa.unsentMessageFile.getPath() + ".down");
                    int dlTries = 0;
                    while (dlTries < maxTries) {
                        Mixed.wait(waitTime);
                        tmpFile.delete();
                        if (downloadMessage(index, tmpFile, wa)) {
                            break;
                        } else {
                            logger.severe("TOFUP: Uploaded message could NOT be retrieved! " + "Download try " + dlTries + " of " + maxTries + "\n" + logInfo);
                            dlTries++;
                        }
                    }
                    if (tmpFile.length() > 0) {
                        logger.warning("TOFUP: Uploaded message was successfully retrieved." + logInfo);
                        success = true;
                    } else {
                        logger.severe("TOFUP: Uploaded message could NOT be retrieved!\n" + logInfo + "\n(try no. " + tries + " of " + maxTries + "), retrying index " + index);
                        tries++;
                        retrySameIndex = true;
                    }
                    tmpFile.delete();
                } else if (result.isKeyCollision()) {
                    logger.warning("TOFUP: Upload collided, trying next free index." + logInfo);
                    Mixed.wait(waitTime);
                } else if (result.isNoConnection()) {
                    logger.severe("TOFUP: Upload failed, no node connection." + logInfo);
                    error = true;
                } else {
                    if (tries > maxTries) {
                        error = true;
                    } else {
                        logger.warning("TOFUP: Upload failed, " + logInfo + "\n(try no. " + tries + " of " + maxTries + "), retrying index " + index);
                        tries++;
                        retrySameIndex = true;
                        Mixed.wait(waitTime);
                    }
                }
            }
            if (success) {
                wa.indexSlot.setUploadSlotUsed(index);
                logger.info("Message successfully uploaded." + logInfo + "\n");
                wa.uploadFile.delete();
                return new MessageUploaderResult(index);
            } else {
                logger.warning("TOFUP: Error while uploading message.");
                boolean retrySilently = Core.frostSettings.getBoolValue(SettingsClass.SILENTLY_RETRY_MESSAGES);
                if (!retrySilently) {
                    final MessageUploadFailedDialog faildialog = new MessageUploadFailedDialog(wa.parentFrame, wa.message, null);
                    final int answer = faildialog.startDialog();
                    if (answer == MessageUploadFailedDialog.RETRY_VALUE) {
                        logger.info("TOFUP: Will try to upload again immediately.");
                        tryAgain = true;
                    } else if (answer == MessageUploadFailedDialog.RETRY_NEXT_STARTUP_VALUE) {
                        wa.uploadFile.delete();
                        logger.info("TOFUP: Will try to upload again on next startup.");
                        return new MessageUploaderResult(true);
                    } else if (answer == MessageUploadFailedDialog.DISCARD_VALUE) {
                        wa.uploadFile.delete();
                        logger.warning("TOFUP: Will NOT try to upload message again.");
                        return new MessageUploaderResult(false);
                    } else {
                        logger.warning("TOFUP: Paranoia - will try to upload message again.");
                        tryAgain = true;
                    }
                } else {
                    tryAgain = true;
                }
            }
        } while (tryAgain);
        return new MessageUploaderResult(true);
    }

    /**
     * Download the specified index, used to check if file was correctly uploaded.
     */
    private static boolean downloadMessage(final int index, final File targetFile, final MessageUploaderWorkArea wa) {
        try {
            final String downKey = wa.callback.composeDownloadKey(wa.message, index);
            final FcpResultGet res = FcpHandler.inst().getFile(FcpHandler.TYPE_MESSAGE, downKey, null, targetFile, false, false, FcpHandler.MAX_MESSAGE_SIZE_07, -1);
            if (res != null && res.isSuccess() && targetFile.length() > 0) {
                return true;
            }
        } catch (final Throwable t) {
            logger.log(Level.WARNING, "Handled exception in downloadMessage", t);
        }
        return false;
    }

    /**
     * Encrypt, sign and zip the message into a file that is uploaded afterwards.
     */
    protected static boolean prepareMessage05(final MessageUploaderWorkArea wa) {
        if (wa.senderId != null) {
            wa.message.setFromName(wa.senderId.getUniqueName());
            wa.message.signMessageV1(wa.senderId.getPrivateKey());
            wa.message.signMessageV2(wa.senderId.getPrivateKey());
            if (!wa.message.save()) {
                logger.severe("Save of signed msg failed. This was a HARD error, please report to a dev!");
                return false;
            }
        }
        FileAccess.writeZipFile(FileAccess.readByteArray(wa.unsentMessageFile), "entry", wa.uploadFile);
        if (!wa.uploadFile.isFile() || wa.uploadFile.length() == 0) {
            logger.severe("Error: zip of message xml file failed, result file not existing or empty. Please report to a dev!");
            return false;
        }
        if (wa.senderId != null) {
            final byte[] zipped = FileAccess.readByteArray(wa.uploadFile);
            if (wa.encryptForRecipient != null) {
                final byte[] encData = Core.getCrypto().encrypt(zipped, wa.encryptForRecipient.getPublicKey());
                if (encData == null) {
                    logger.severe("Error: could not encrypt the message, please report to a dev!");
                    return false;
                }
                wa.uploadFile.delete();
                FileAccess.writeFile(encData, wa.uploadFile);
                final EncryptMetaData ed = new EncryptMetaData(encData, wa.senderId, wa.encryptForRecipient.getUniqueName());
                wa.signMetadata = XMLTools.getRawXMLDocument(ed);
            } else {
                final SignMetaData md = new SignMetaData(zipped, wa.senderId);
                wa.signMetadata = XMLTools.getRawXMLDocument(md);
            }
        } else if (wa.encryptForRecipient != null) {
            logger.log(Level.SEVERE, "TOFUP: ALERT - can't encrypt message if sender is Anonymous! Will not send message!");
            return false;
        }
        long allLength = wa.uploadFile.length();
        if (wa.signMetadata != null) {
            allLength += wa.signMetadata.length;
        }
        if (allLength > 32767) {
            final Language language = Language.getInstance();
            final String title = language.getString("MessageUploader.messageToLargeError.title");
            final String txt = language.formatMessage("MessageUploader.messageToLargeError.text", Long.toString(allLength), Integer.toString(32767));
            JOptionPane.showMessageDialog(wa.parentFrame, txt, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Encrypt and sign the message into a file that is uploaded afterwards.
     */
    protected static boolean prepareMessage07(final MessageUploaderWorkArea wa) {
        if (wa.senderId != null) {
            wa.message.setFromName(wa.senderId.getUniqueName());
            wa.message.signMessageV1(wa.senderId.getPrivateKey());
            wa.message.signMessageV2(wa.senderId.getPrivateKey());
        }
        if (!wa.message.saveToFile(wa.uploadFile)) {
            logger.severe("Save to file '" + wa.uploadFile.getPath() + "' failed. This was a HARD error, file was NOT uploaded, please report to a dev!");
            return false;
        }
        if (wa.message.getSignatureV2() != null && wa.message.getSignatureV2().length() > 0 && wa.encryptForRecipient != null) {
            if (!MessageXmlFile.encryptForRecipientAndSaveCopy(wa.uploadFile, wa.encryptForRecipient, wa.uploadFile)) {
                logger.severe("This was a HARD error, file was NOT uploaded, please report to a dev!");
                return false;
            }
        } else if (wa.encryptForRecipient != null) {
            logger.log(Level.SEVERE, "TOFUP: ALERT - can't encrypt message if sender is Anonymous! Will not send message!");
            return false;
        }
        return true;
    }
}
