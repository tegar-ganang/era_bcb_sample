package net.tourbook.srtm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.util.IExternalTourEvents;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import de.byteholder.geoclipse.map.UI;

public class PrefPageSRTM extends PreferencePage implements IWorkbenchPreferencePage {

    public static final String PROTOCOL_HTTP = "http://";

    public static final String PROTOCOL_FTP = "ftp://";

    private static final String PREF_PAGE_SRTM_COLORS = "net.tourbook.ext.srtm.PrefPageSRTMColors";

    private IPreferenceStore _prefStore;

    private final String _defaultSRTMFilePath = Platform.getInstanceLocation().getURL().getPath();

    private Composite _prefContainer;

    private Composite _pathContainer;

    private BooleanFieldEditor _useDefaultLocation;

    private DirectoryFieldEditor _dataPathEditor;

    private Button _rdoSRTM3FtpUrl;

    private Text _txtSRTM3FtpUrl;

    private Button _rdoSRTM3HttpUrl;

    private Text _txtSRTM3HttpUrl;

    private boolean _backupIsFtp;

    private String _backupFtpUrl;

    private String _backupHttpUrl;

    @Override
    protected Control createContents(final Composite parent) {
        _prefStore = TourbookPlugin.getDefault().getPreferenceStore();
        createUI(parent);
        restoreState();
        enableControls();
        if (_useDefaultLocation.getBooleanValue() == false) {
            setErrorMessage(null);
        }
        return _prefContainer;
    }

    private void createUI(final Composite parent) {
        _prefContainer = new Composite(parent, SWT.NONE);
        GridDataFactory.swtDefaults().grab(true, false).applyTo(_prefContainer);
        GridLayoutFactory.fillDefaults().applyTo(_prefContainer);
        GridDataFactory.swtDefaults().applyTo(_prefContainer);
        {
            createUI10CacheSettings(_prefContainer);
            createUI20Srtm3Url(_prefContainer);
            createUI30SrtmPageLink(_prefContainer);
        }
    }

    private void createUI10CacheSettings(final Composite parent) {
        final Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.prefPage_srtm_group_label_data_location);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
        {
            _useDefaultLocation = new BooleanFieldEditor(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH, Messages.prefPage_srtm_chk_use_default_location, group);
            _useDefaultLocation.setPage(this);
            _useDefaultLocation.setPreferenceStore(_prefStore);
            _useDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {

                public void propertyChange(final PropertyChangeEvent event) {
                    enableControls();
                }
            });
            new Label(group, SWT.NONE);
            _pathContainer = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_pathContainer);
            {
                _dataPathEditor = new DirectoryFieldEditor(IPreferences.SRTM_DATA_FILEPATH, Messages.prefPage_srtm_editor_data_filepath, _pathContainer);
                _dataPathEditor.setPage(this);
                _dataPathEditor.setPreferenceStore(_prefStore);
                _dataPathEditor.setEmptyStringAllowed(false);
                _dataPathEditor.setPropertyChangeListener(new IPropertyChangeListener() {

                    public void propertyChange(final PropertyChangeEvent event) {
                        validateData();
                    }
                });
            }
        }
        GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
    }

    private void createUI20Srtm3Url(final Composite parent) {
        final SelectionAdapter selectListener = new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                enableControls();
                validateData();
            }
        };
        final ModifyListener modifyListener = new ModifyListener() {

            public void modifyText(final ModifyEvent e) {
                validateData();
            }
        };
        final Group group = new Group(parent, SWT.NONE);
        group.setText(Messages.prefPage_srtm_group_label_srtm3);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
        {
            _rdoSRTM3HttpUrl = new Button(group, SWT.RADIO);
            _rdoSRTM3HttpUrl.setText(Messages.prefPage_srtm_radio_srtm3HttpUrl);
            _rdoSRTM3HttpUrl.addSelectionListener(selectListener);
            _txtSRTM3HttpUrl = new Text(group, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtSRTM3HttpUrl);
            _txtSRTM3HttpUrl.addModifyListener(modifyListener);
            _rdoSRTM3FtpUrl = new Button(group, SWT.RADIO);
            _rdoSRTM3FtpUrl.setText(Messages.prefPage_srtm_radio_srtm3FtpUrl);
            _rdoSRTM3FtpUrl.addSelectionListener(selectListener);
            _rdoSRTM3FtpUrl.setEnabled(false);
            _txtSRTM3FtpUrl = new Text(group, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtSRTM3FtpUrl);
            _txtSRTM3FtpUrl.addModifyListener(modifyListener);
            final Button btnTestConnection = new Button(group, SWT.NONE);
            GridDataFactory.swtDefaults().indent(0, 10).span(2, 1).applyTo(btnTestConnection);
            btnTestConnection.setText(Messages.prefPage_srtm_button_testConnection);
            btnTestConnection.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    onCheckConnection();
                }
            });
        }
    }

    private void createUI30SrtmPageLink(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 20).applyTo(container);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
        {
            final Link link = new Link(container, SWT.NONE);
            link.setText(Messages.prefPage_srtm_link_srtmProfiles);
            link.setEnabled(true);
            link.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    PreferencesUtil.createPreferenceDialogOn(getShell(), PREF_PAGE_SRTM_COLORS, null, null);
                }
            });
        }
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return _prefStore;
    }

    private void enableControls() {
        final boolean useDefaultLocation = _useDefaultLocation.getBooleanValue();
        if (useDefaultLocation) {
            _dataPathEditor.setEnabled(false, _pathContainer);
            _dataPathEditor.setStringValue(_defaultSRTMFilePath);
        } else {
            _dataPathEditor.setEnabled(true, _pathContainer);
        }
        final boolean isFTP = _rdoSRTM3FtpUrl.getSelection();
        _txtSRTM3FtpUrl.setEnabled(isFTP);
        _txtSRTM3HttpUrl.setEnabled(!isFTP);
    }

    public void init(final IWorkbench workbench) {
    }

    @Override
    public boolean okToLeave() {
        if (validateData() == false) {
            return false;
        }
        return super.okToLeave();
    }

    private void onCheckConnection() {
        BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

            public void run() {
                String baseUrl;
                if (_rdoSRTM3FtpUrl.getSelection()) {
                } else {
                    baseUrl = _txtSRTM3HttpUrl.getText().trim();
                    try {
                        final URL url = new URL(baseUrl);
                        final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                        urlConn.connect();
                        final int response = urlConn.getResponseCode();
                        final String responseMessage = urlConn.getResponseMessage();
                        final String message = response == HttpURLConnection.HTTP_OK ? NLS.bind(Messages.prefPage_srtm_checkHTTPConnectionOK_message, baseUrl) : NLS.bind(Messages.prefPage_srtm_checkHTTPConnectionFAILED_message, new Object[] { baseUrl, Integer.toString(response), responseMessage == null ? UI.EMPTY_STRING : responseMessage });
                        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.prefPage_srtm_checkHTTPConnection_title, message);
                    } catch (final IOException e) {
                        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.prefPage_srtm_checkHTTPConnection_title, NLS.bind(Messages.prefPage_srtm_checkHTTPConnection_message, baseUrl));
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void performDefaults() {
        _useDefaultLocation.loadDefault();
        _prefStore.setToDefault(IPreferences.STATE_IS_SRTM3_FTP);
        _prefStore.setToDefault(IPreferences.STATE_SRTM3_HTTP_URL);
        _prefStore.setToDefault(IPreferences.STATE_SRTM3_FTP_URL);
        final boolean isFtp = _prefStore.getDefaultBoolean(IPreferences.STATE_IS_SRTM3_FTP);
        _rdoSRTM3FtpUrl.setSelection(isFtp);
        _rdoSRTM3HttpUrl.setSelection(!isFtp);
        _txtSRTM3FtpUrl.setText(_prefStore.getDefaultString(IPreferences.STATE_SRTM3_FTP_URL));
        _txtSRTM3HttpUrl.setText(_prefStore.getDefaultString(IPreferences.STATE_SRTM3_HTTP_URL));
        enableControls();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (_useDefaultLocation == null) {
            return super.performOk();
        }
        if (validateData() == false) {
            return false;
        }
        saveState();
        if (_backupIsFtp != _prefStore.getBoolean(IPreferences.STATE_IS_SRTM3_FTP) || _backupFtpUrl.equalsIgnoreCase(_prefStore.getString(IPreferences.STATE_SRTM3_FTP_URL)) == false || _backupHttpUrl.equalsIgnoreCase(_prefStore.getString(IPreferences.STATE_SRTM3_HTTP_URL)) == false) {
            ElevationSRTM3.clearElevationFileCache();
            net.tourbook.util.Activator.getDefault().getPreferenceStore().setValue(IExternalTourEvents.CLEAR_TOURDATA_CACHE, Math.random());
        }
        return super.performOk();
    }

    private void restoreState() {
        _useDefaultLocation.load();
        _dataPathEditor.load();
        _backupIsFtp = _prefStore.getBoolean(IPreferences.STATE_IS_SRTM3_FTP);
        _rdoSRTM3FtpUrl.setSelection(_backupIsFtp);
        _rdoSRTM3HttpUrl.setSelection(!_backupIsFtp);
        _backupFtpUrl = _prefStore.getString(IPreferences.STATE_SRTM3_FTP_URL);
        _backupHttpUrl = _prefStore.getString(IPreferences.STATE_SRTM3_HTTP_URL);
        _txtSRTM3FtpUrl.setText(_backupFtpUrl);
        _txtSRTM3HttpUrl.setText(_backupHttpUrl);
    }

    private void saveState() {
        _useDefaultLocation.store();
        _dataPathEditor.store();
        _prefStore.setValue(IPreferences.STATE_IS_SRTM3_FTP, _rdoSRTM3FtpUrl.getSelection());
        _prefStore.setValue(IPreferences.STATE_SRTM3_HTTP_URL, _txtSRTM3HttpUrl.getText().trim());
        _prefStore.setValue(IPreferences.STATE_SRTM3_FTP_URL, _txtSRTM3FtpUrl.getText().trim());
    }

    private boolean validateData() {
        boolean isValid = true;
        if (_useDefaultLocation.getBooleanValue() == false && (!_dataPathEditor.isValid() || _dataPathEditor.getStringValue().trim().length() == 0)) {
            isValid = false;
            setErrorMessage(Messages.prefPage_srtm_msg_invalid_data_path);
            _dataPathEditor.setFocus();
        } else if (_rdoSRTM3FtpUrl.getSelection()) {
            if (_txtSRTM3FtpUrl.getText().trim().toLowerCase().startsWith(PROTOCOL_FTP) == false) {
                isValid = false;
                setErrorMessage(Messages.prefPage_srtm_msg_invalidSrtm3FtpUrl);
                _txtSRTM3FtpUrl.setFocus();
            }
        } else {
            if (_txtSRTM3HttpUrl.getText().trim().toLowerCase().startsWith(PROTOCOL_HTTP) == false) {
                isValid = false;
                setErrorMessage(Messages.prefPage_srtm_msg_invalidSrtm3HttpUrl);
                _txtSRTM3HttpUrl.setFocus();
            }
        }
        if (isValid) {
            setErrorMessage(null);
        }
        setValid(isValid);
        return isValid;
    }
}
