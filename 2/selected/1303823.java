package com.gcsf.books.gui.bundles.internals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.branding.IBundleGroupConstants;

/**
 * A small class to manage the information related to IBundleGroup's.
 */
public class AboutBundleGroupData extends AboutData {

    private IBundleGroup bundleGroup;

    private URL licenseUrl;

    private URL featureImageUrl;

    private Long featureImageCrc;

    private ImageDescriptor featureImage;

    public AboutBundleGroupData(IBundleGroup bundleGroup) {
        super(bundleGroup.getProviderName(), bundleGroup.getName(), bundleGroup.getVersion(), bundleGroup.getIdentifier());
        this.bundleGroup = bundleGroup;
    }

    public IBundleGroup getBundleGroup() {
        return bundleGroup;
    }

    public URL getLicenseUrl() {
        if (licenseUrl == null) {
            licenseUrl = getURL(bundleGroup.getProperty(IBundleGroupConstants.LICENSE_HREF));
        }
        return licenseUrl;
    }

    public URL getFeatureImageUrl() {
        if (featureImageUrl == null) {
            featureImageUrl = getURL(bundleGroup.getProperty(IBundleGroupConstants.FEATURE_IMAGE));
        }
        return featureImageUrl;
    }

    public ImageDescriptor getFeatureImage() {
        if (featureImage == null) {
            featureImage = getImage(getFeatureImageUrl());
        }
        return featureImage;
    }

    public Long getFeatureImageCrc() {
        if (featureImageCrc != null) {
            return featureImageCrc;
        }
        URL url = getFeatureImageUrl();
        if (url == null) {
            return null;
        }
        InputStream in = null;
        try {
            CRC32 checksum = new CRC32();
            in = new CheckedInputStream(url.openStream(), checksum);
            byte[] sink = new byte[1024];
            while (true) {
                if (in.read(sink) <= 0) {
                    break;
                }
            }
            featureImageCrc = new Long(checksum.getValue());
            return featureImageCrc;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public String getAboutText() {
        return bundleGroup.getProperty(IBundleGroupConstants.ABOUT_TEXT);
    }
}
