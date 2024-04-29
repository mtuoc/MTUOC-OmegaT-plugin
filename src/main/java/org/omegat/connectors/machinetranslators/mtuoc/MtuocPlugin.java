package org.omegat.connectors.machinetranslators.mtuoc;

import org.omegat.core.Core;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.exttrans.IMachineTranslation;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.CredentialsManager;
import org.omegat.util.Language;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;
import org.omegat.util.StringUtil;
import org.omegat.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * Support for Microsoft Translator API machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Aaron Madlon-Kay
 * @author Hiroshi Miura
 *
 * @see <a href="https://www.microsoft.com/en-us/translator/translatorapi.aspx">Translator API</a>
 * @see <a href="https://docs.microsofttranslator.com/text-translate.html">Translate Method reference</a>
 */
public class MtuocPlugin extends BaseCachedTranslate implements IMachineTranslation {

    protected static final String ALLOW_MTUOC = "allow_mtuoc";

    protected static final String PROPERTY_MT_ENGINE_URL = "mtuoc.engine_url";//"http://172.20.137.165"

    protected static final String PROPERTY_MT_ENGINE_PORT = "mtuoc.engine_port";//8011

    //Not used yet, keep for later possible use
    protected static final String PROPERTY_API_KEY = "mtuoc.apikey";

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("MtuocBundle");

    private MtuocTranslatorBase translator = null;

    /**
     * Constructor of the connector.
     */
    public MtuocPlugin() {
        super();
    }

    /**
     * Utility function to get a localized message.
     * @param key bundle key.
     * @return a localized string.
     */
    static String getString(String key) {
        return BUNDLE.getString(key);
    }

    /**
     * Register plugin into OmegaT.
     */
    @SuppressWarnings("unused")
    public static void loadPlugins() {
        String requiredVersion = "5.8.0";
        String requiredUpdate = "0";
        try {
            Class<?> clazz = Class.forName("org.omegat.util.VersionChecker");
            Method compareVersions =
                    clazz.getMethod("compareVersions", String.class, String.class, String.class, String.class);
            if ((int) compareVersions.invoke(clazz, OStrings.VERSION, OStrings.UPDATE, requiredVersion, requiredUpdate)
                    < 0) {
                Core.pluginLoadingError("MTUOC Plugin cannot be loaded because OmegaT Version "
                        + OStrings.VERSION + " is lower than required version " + requiredVersion);
                return;
            }
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            Core.pluginLoadingError(
                    "MTUOC plugin cannot be loaded because this OmegaT version is not supported");
            return;
        }
        Core.registerMachineTranslationClass(MtuocPlugin.class);
    }

    /**
     * Unregister plugin.
     * Currently not supported.
     */
    @SuppressWarnings("unused")
    public static void unloadPlugins() {}

    /**
     * Return a name of the connector.
     * @return connector name.
     */
    public String getName() {
        return getString("MT_ENGINE_MTUOC");
    }

    @Override
    protected String getPreferenceName() {
        return ALLOW_MTUOC;
    }

    /**
     * Store a credential. Credentials are stored in temporary system properties and, if
     * <code>temporary</code> is <code>false</code>, in the program's persistent preferences encoded in
     * Base64. Retrieve a credential with {@link #getCredential(String)}.
     *
     * @param id
     *            ID or key of the credential to store
     * @param value
     *            value of the credential to store
     * @param temporary
     *            if <code>false</code>, encode with Base64 and store in persistent preferences as well
     */
    protected void setCredential(String id, String value, boolean temporary) {
        System.setProperty(id, value);
        if (temporary) {
            CredentialsManager.getInstance().store(id, "");
        } else {
            CredentialsManager.getInstance().store(id, value);
        }
    }

    /**
     * Retrieve a credential with the given ID. First checks temporary system properties, then falls back to
     * the program's persistent preferences. Store a credential with
     * {@link #setCredential(String, String, boolean)}.
     *
     * @param id
     *            ID or key of the credential to retrieve
     * @return the credential value in plain text
     */
    protected String getCredential(String id) {
        String property = System.getProperty(id);
        if (property != null) {
            return property;
        }
        return CredentialsManager.getInstance().retrieve(id).orElse("");
    }

    protected void setKey(String key, boolean temporary) {
        setCredential(PROPERTY_API_KEY, key, temporary);
    }

    protected String getKey() throws Exception {
        String key = getCredential(PROPERTY_API_KEY);
        if (StringUtil.isEmpty(key)) {
            throw new Exception(getString("MT_ENGINE_API_KEY_NOTFOUND"));
        }
        return key;
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        translator = new MtuocTranslator(this,this.GetTranslateEndpointUrl());
        String translation = translator.translate(sLang, tLang, text);
        if (translation != null)
        {
            return translation;
        }
        else {
            Log.logWarningRB("MT_ENGINE_NO_RESPONSE");
            return null;
        }
    }

    public String GetTranslateEndpointUrl() {
        String url = Preferences.getPreferenceDefault(PROPERTY_MT_ENGINE_URL,null);
        String port = Preferences.getPreferenceDefault(PROPERTY_MT_ENGINE_PORT,null);
        if (url == null || port == null) {
            return "";
        }
        else {
            return String.format("%s:%s/translate", url, port);
        }

    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    //Add http protocol if name does not include one yet
    private static String validateUrl(String url) {
        if (!url.matches("^https?://.*")) {
            url = "http://" + url;
        }
        return url;
    }

    @Override
    public void showConfigurationUI(Window parent) {

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {

                Preferences.setPreference(
                        PROPERTY_MT_ENGINE_URL, MtuocPlugin.validateUrl(panel.valueField1.getText()));
                Preferences.setPreference(
                        PROPERTY_MT_ENGINE_PORT, panel.valueField2.getText().trim());
            }


        };
        //Keep these if API key functionality added
        //dialog.panel.valueLabel1.setText(getString("MT_ENGINE_MTUOC_KEY_LABEL"));
        //dialog.panel.valueField1.setText(getCredential(PROPERTY_API_KEY));
        int height = dialog.panel.getFont().getSize();
        //dialog.panel.valueField1.setPreferredSize(new Dimension(height * 24, height * 2));
        dialog.panel.valueLabel1.setText(getString("MT_ENGINE_URL"));
        dialog.panel.valueField1.setText(Preferences.getPreferenceDefault(PROPERTY_MT_ENGINE_URL, ""));
        dialog.panel.valueField1.setPreferredSize(new Dimension(height * 12, height * 2));
        dialog.panel.valueLabel2.setText(getString("MT_ENGINE_PORT"));
        dialog.panel.valueField2.setText(Preferences.getPreferenceDefault(PROPERTY_MT_ENGINE_PORT, ""));
        dialog.panel.valueField2.setPreferredSize(new Dimension(height * 12, height * 2));

        //Test connection button adapted from https://github.com/omegat-org/moses-plugin.git
        JLabel messageLabel = new JLabel();
        JButton testButton = new JButton(BUNDLE.getString("MT_ENGINE_TEST_BUTTON"));
        testButton.addActionListener(e -> {
            messageLabel.setText(BUNDLE.getString("MT_ENGINE_TEST_TESTING"));
            String host = MtuocPlugin.validateUrl(dialog.panel.valueField1.getText().trim());
            String port = dialog.panel.valueField2.getText().trim();
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    String url = String.format("%s:%s/translate", host, port);
                    MtuocTranslator translator = new MtuocTranslator(null,url);
                    String response = translator.requestTranslate("xx","xx", "This is a test");
                    if (response != null) {
                        return BUNDLE.getString("MT_ENGINE_TEST_RESULT_OK");
                    } else {
                        return BUNDLE.getString("MT_ENGINE_TEST_RESULT_NO_TRANSLATE");
                    }
                }

                @Override
                protected void done() {
                    String message;
                    try {
                        message = get();
                    } catch (Exception e) {
                        message = e.getLocalizedMessage();
                    }
                    messageLabel.setText(message);
                }
            }.execute();

        });
        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.LINE_AXIS));
        testPanel.add(testButton);
        testPanel.add(messageLabel);
        testPanel.setAlignmentX(0);
        dialog.panel.itemsPanel.add(testPanel);

        dialog.show();
    }
}
