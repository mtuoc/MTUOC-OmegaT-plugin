/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2012 Alex Buloichik, Didier Briel
 *                2016-2017 Aaron Madlon-Kay
 *                2018 Didier Briel
 *                2022,2023 Hiroshi Miura
 *                Home page: https://www.omegat.org/
 *                Support center: https://omegat.org/support
 *
 *  This file is part of OmegaT.
 *
 *  OmegaT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OmegaT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.omegat.connectors.machinetranslators.mtuoc;

import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.omegat.util.PreferencesImpl;
import org.omegat.util.PreferencesXML;
import org.omegat.util.RuntimePreferences;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.commons.io.FileUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * @author Hiroshi Miura
 */
@WireMockTest
public class TestMicrosoftTranslatorAzure {

    private static final String TOKEN_PATH = "/sts/v1.0/issueToken";
    private static final String V2_API_PATH = "/v2/http.svc/Translate";
    private static final String V3_API_PATH = "/translate";
    private static final String KEY = "abcdefg";
    private static final String REGION = "uswest";

    private File tmpDir;

    /**
     * Prepare a temporary directory.
     * @throws IOException when I/O error.
     */
    @BeforeEach
    public final void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("omegat").toFile();
        Assertions.assertTrue(tmpDir.isDirectory());
    }

    /**
     * Clean up a temporary directory.
     * @throws IOException when I/O error.
     */
    @AfterEach
    public final void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    /**
     * Initialize preferences for test.
     * @param configDir to create omegat.prefs.
     */
    public static synchronized void init(String configFilePath) {
        RuntimePreferences.setConfigDir(new File(configFilePath).getParent());
        Preferences.init();
        Preferences.initFilters();
        Preferences.initSegmentation();
    }

    /**
     * A mock for parent class.
     */
    static class MtuocPluginMock extends MtuocPlugin {

    }

    @Test
    void testResponse(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        File prefsFile = new File(tmpDir, Preferences.FILE_PREFERENCES);
        Preferences.IPreferences prefs = new PreferencesImpl(new PreferencesXML(null, prefsFile));
        prefs.setPreference(MtuocPlugin.ALLOW_MTUOC, true);
        prefs.setPreference(MtuocPlugin.PROPERTY_MT_ENGINE_URL, "http://172.20.137.165");
        prefs.setPreference(MtuocPlugin.PROPERTY_MT_ENGINE_PORT, "8011");
        prefs.save();
        init(prefsFile.getAbsolutePath());

        String text = "Buy tomorrow";
        String translation = "Morgen kaufen gehen ein";

        WireMock wireMock = wireMockRuntimeInfo.getWireMock();
        Map<String, StringValuePattern> expectedParams = new HashMap<>();
        expectedParams.put("text", equalTo(text));
        expectedParams.put("id", equalTo("1"));
        wireMock.register(post(urlPathEqualTo(MtuocPlugin.PROPERTY_MT_ENGINE_URL))
                .withQueryParams(expectedParams)
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"translations\": [ {\"text\": \"" + translation + "\"}]}]")));
        int port = wireMockRuntimeInfo.getHttpPort();
        MtuocPluginMock mtuoc = new MtuocPluginMock();

        MtuocTranslator translator = new MtuocTranslator(mtuoc, mtuoc.GetTranslateEndpointUrl());
        //translator.setUrl(String.format("http://localhost:%d%s?api-version=3.0", port, V3_API_PATH));
        String result = translator.translate(new Language("EN"), new Language("DE"), text);
        Assertions.assertEquals(translation, result);
    }
}
