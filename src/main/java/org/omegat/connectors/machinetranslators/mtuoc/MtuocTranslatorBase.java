package org.omegat.connectors.machinetranslators.mtuoc;

import org.omegat.util.Language;

/**
 * Support for Microsoft Translator API machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Aaron Madlon-Kay
 *
 * @see <a href="https://www.microsoft.com/en-us/translator/translatorapi.aspx">Translator API</a>
 * @see <a href="https://docs.microsofttranslator.com/text-translate.html">Translate Method reference</a>
 */
public abstract class MtuocTranslatorBase {

    protected final MtuocPlugin parent;

    public MtuocTranslatorBase(MtuocPlugin parent) {
        this.parent = parent;
    }



    /**
     * translate text.
     * @param sLang source langauge.
     * @param tLang target language.
     * @param text source text.
     * @return translated text.
     * @throws Exception when connection error.
     */
    protected synchronized String translate(Language sLang, Language tLang, String text) throws Exception {
        //Languages are not currently used in MTUOC, but keep these for later use
        String langFrom = sLang.getLanguage();
        String langTo = tLang.getLanguage();
        return requestTranslate(langFrom, langTo, text);
    }

    protected abstract String requestTranslate(String langFrom, String langTo, String text) throws Exception;
}
