package com.ivis.xprocess.ui.licensing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.Window;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.util.DatasourceUtil;
import com.ivis.xprocess.ui.util.ViewUtil;
import com.ivis.xprocess.ui.util.debug.DebugUtil;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.License;
import com.ivis.xprocess.util.LicenseException;
import com.ivis.xprocess.util.LicenseNotValidForThisVersion;
import com.ivis.xprocess.util.LicensingEnums.LicenseType;

public class LicenseVerifier {

    private static boolean isLicensed = false;

    private static LicenseAction licenseAction;

    private LicenseVerifier() {
    }

    /**
     * @return - false if license is corrupt
     */
    public static boolean verifyLicence() {
        String licenseFile = UIPlugin.getLicencePath();
        isLicensed = true;
        File dir = new File(DatasourceUtil.getDatasourceRoot());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(licenseFile);
        if (!file.exists()) {
            copyDefaultLicense();
            if (!file.exists()) {
                licenseAction = LicenseAction.NO_LICENSE;
                isLicensed = false;
                return isLicensed;
            }
            licenseAction = LicenseAction.DEFAULT;
        }
        try {
            Reader stream = new InputStreamReader(new FileInputStream(file), "UTF-8");
            License.initialize(stream);
            stream.close();
            if (License.getLicense().getAccountName().equals("default_user")) {
                if (License.getLicense().getOrganization().equals("Default Organization")) {
                    licenseAction = LicenseAction.DEFAULT;
                }
            }
        } catch (LicenseNotValidForThisVersion licenseException) {
            DebugUtil.out.println(licenseException.getMessage());
            licenseAction = LicenseAction.NOT_SUPPORTED;
            isLicensed = false;
            return isLicensed;
        } catch (LicenseException licenseException) {
            DebugUtil.out.println(licenseException.getMessage());
            licenseAction = LicenseAction.CORRUPTED;
            isLicensed = false;
            return isLicensed;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        License lic = License.getLicense();
        if (licenseAction == LicenseAction.DEFAULT) {
            return isLicensed;
        }
        if (lic.hasExpired()) {
            if (lic.getLicenseType() == LicenseType.EVALUATION) {
                licenseAction = LicenseAction.EXPIRED_EVALUATION;
            } else {
                licenseAction = LicenseAction.EXPIRED;
            }
            isLicensed = false;
        } else if (lic.getLicenseType() == LicenseType.EVALUATION) {
            licenseAction = LicenseAction.EVALUATION;
        } else {
            licenseAction = LicenseAction.DO_NOTHING;
        }
        return isLicensed;
    }

    public static void showNagRegisterDialog() {
        if (licenseAction == LicenseAction.DO_NOTHING || licenseAction == LicenseAction.DEFAULT) {
            return;
        }
        UIPlugin.getDefault().labelShell();
        int retCode = NagRegisterDialog.open(licenseAction);
        if (retCode == Window.CANCEL) {
            isLicensed = false;
        } else {
            isLicensed = true;
        }
    }

    public static void showDefaultNagDialog() {
        if (licenseAction == LicenseAction.DEFAULT) {
            DefaultNagDialog defaultNagDialog = new DefaultNagDialog(ViewUtil.getCurrentShell());
            defaultNagDialog.open();
        }
    }

    public static boolean isLicensed() {
        return isLicensed;
    }

    private static void copyDefaultLicense() {
        String pathToDefaultLicense = UIPlugin.getDefault().getPluginLocation() + File.separator + "lib" + File.separator + "default.lic";
        File defaultLicenseFile = new File(pathToDefaultLicense);
        if (defaultLicenseFile.exists()) {
            File licenseFile = new File(UIPlugin.getLicencePath());
            try {
                FileUtils.copyFile(defaultLicenseFile, licenseFile);
            } catch (IOException e) {
                UIPlugin.log("Error copying default license", IStatus.ERROR, null);
            }
        } else {
            UIPlugin.log("Unable to locate the default license at: " + defaultLicenseFile, IStatus.ERROR, null);
        }
    }
}
