package com.volantis.mps.attachment;

import com.volantis.mcs.repository.RepositoryException;
import com.volantis.mps.message.MessageException;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.synergetics.log.LogDispatcher;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>MessageAttachments</code> class encapsulates an ordered list of
 * attachments  to be associated with a message. The list is maintained in the
 * order in  which individual message attachments are added to it. New
 * attachments are always added to the end.
 *
 * @volantis-api-include-in PublicAPI
 */
public class MessageAttachments {

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MessageAttachments.class);

    /**
     * List of attachements
     */
    List attachments;

    /**
     * Creates a MessageAttachments object with no attachments defined.
     */
    public MessageAttachments() {
        attachments = new ArrayList();
    }

    /**
     * Adds the specified device attachment to this list of recipients.
     * Attachments are always added to the end of the list.
     * <p>
     * @param deviceMessageAttachment the device attachment to add.
     */
    public void addAttachment(DeviceMessageAttachment deviceMessageAttachment) {
        attachments.add(deviceMessageAttachment);
    }

    /**
     * Removes a device specific attachment from the attachment list.
     * <p>
     * @param deviceMessageAttachment the device attachment to remove.
     */
    public void removeAttachment(DeviceMessageAttachment deviceMessageAttachment) {
        attachments.remove(deviceMessageAttachment);
    }

    /**
     * Gets an iterator for the current attachments.
     * <p>
     * @return an iterator of DeviceMessageAttachment objects.
     */
    public Iterator iterator() {
        return attachments.iterator();
    }

    /**
     * Package scope method to get a list of DeviceMessageAttachments for a
     * specific channel. If a channel is not specified in the message attachment, 
     * the attachment is used on any channel.
     *
     * Use AttachmentUtilities to get access to this method as it is not 
     * part of the public API.
     * @param channelName The channel for which the recipients should be
     *                    returned for
     *
     * @return MessageAttachments Those attachments that match
     */
    MessageAttachments getAttachmentsForChannel(String channelName) {
        MessageAttachments ret = new MessageAttachments();
        Iterator itr = iterator();
        while (itr.hasNext()) {
            DeviceMessageAttachment dma = (DeviceMessageAttachment) itr.next();
            try {
                if (dma.getValueType() != MessageAttachment.UNDEFINED) {
                    if (dma.getChannelName().equals(channelName) || dma.getChannelName() == null) {
                        ret.addAttachment(dma);
                    }
                }
            } catch (MessageException mse) {
                logger.error("channel-message-list-error", channelName, mse);
            }
        }
        return ret;
    }

    /**
     * Package scope method to get a list of DeviceMessageAttachments for a
     * specific device. If the device specified in the message attachment is the 
     * same as or is an ancestor of the device specified the attachment is added 
     * 
     * Use AttachmentUtilities to get access to this method as it is not 
     * part of the public API.
     *
     * @param deviceName The device for which the recipients should be
     *                   returned for
     *
     * @return MessageAttachments Those attachments that match
     */
    MessageAttachments getAttachmentsForDevice(String deviceName) {
        MessageAttachments ret = new MessageAttachments();
        Iterator itr = iterator();
        while (itr.hasNext()) {
            DeviceMessageAttachment dma = (DeviceMessageAttachment) itr.next();
            try {
                if (dma.getValueType() != MessageAttachment.UNDEFINED) {
                    if (dma.getDeviceName().equals(deviceName) || checkDeviceAncestry(dma, deviceName)) {
                        ret.addAttachment(dma);
                    }
                }
            } catch (Exception mse) {
                logger.error("device-message-list-error", deviceName, mse);
            }
        }
        return ret;
    }

    /**
     * Checks the device ancestry of the device contained within the message
     * attachment against the given device name.
     * <p>
     * @param dma        The device message attachment to check for ancestry
     *                   against the specified device.
     * @param deviceName The device name to check for being a descendant of
     *                   <code>dma</code>'s device.
     *
     * @return true if deviceName has <code>dma</code>'s device as an ancestor,
     *              false otherwise.
     *
     * @throws RepositoryException if there is a problem accessing the
     *                             repository.
     * @throws MessageException if there is a problem accessing the device
     *                          contained within the message attachment.
     */
    protected boolean checkDeviceAncestry(DeviceMessageAttachment dma, String deviceName) throws RepositoryException, MessageException {
        return AttachmentUtilities.isAncestorDevice(dma.getDeviceName(), deviceName);
    }
}
