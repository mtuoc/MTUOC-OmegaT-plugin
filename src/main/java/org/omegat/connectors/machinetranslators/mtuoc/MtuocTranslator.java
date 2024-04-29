package org.omegat.connectors.machinetranslators.mtuoc;

import org.omegat.util.HttpConnectionUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Support for Microsoft Translator API machine translation.
 * @author Hiroshi Miura
 */
public class MtuocTranslator extends MtuocTranslatorBase {



    private String urlTranslate;
    private final ObjectMapper mapper = new ObjectMapper();

    public MtuocTranslator(MtuocPlugin parent, String translateEndpointUrl) {
        super(parent);
        urlTranslate = translateEndpointUrl;
    }

    @Override
    protected String requestTranslate(String langFrom, String langTo, String text) throws Exception {
        Map<String, String> p = new TreeMap<>();
        //Modify this if API key support is need
        //p.put("Ocp-Apim-Subscription-Key", parent.getKey());
        String url = urlTranslate;
        String json = createJsonRequest(text);
        try {
            String res = HttpConnectionUtils.postJSON(url, json, p);
            JsonNode root = mapper.readValue(res, JsonNode.class);
            JsonNode translation = root.get("tgt");

            if (translation == null) {
                return null;
            }

            return translation.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Method for test.
     * @param url alternative url.
     */
    public void setUrl(String url) {
        urlTranslate = url;
    }

    /**
     * Create request and return as json string.
     */
    protected String createJsonRequest(String trText) throws JsonProcessingException {
        Map<String, Object> param = new TreeMap<>();
        param.put("src", trText);
        param.put("id", java.util.UUID.randomUUID());
        return new ObjectMapper().writeValueAsString(param);
    }
}
