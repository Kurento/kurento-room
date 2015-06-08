/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.room.demo;

import static org.kurento.commons.PropertiesManager.getPropertyJson;

import java.util.List;

import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.room.KurentoRoomServerApp;
import org.kurento.room.kms.KmsManager;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.gson.JsonArray;

@SpringBootApplication
public class KurentoRoomDemoApp {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomDemoApp.class);

	private final String DEFAULT_APP_SERVER_URL = PropertiesManager
			.getProperty("app.uri", "http://localhost:8080");

	private final Integer DEMO_KMS_NODE_LIMIT = PropertiesManager
			.getProperty("demo.kmsLimit", 10);

	private final String DEMO_AUTH_REGEX = PropertiesManager.getProperty("demo.authRegex", "");
	private final boolean DEMO_HAT_FILTER = PropertiesManager.getProperty("demo.hat.filter", false);
	private final boolean DEMO_HAT_ONLY_ON_FIRST = PropertiesManager.getProperty("demo.hat.onlyOnFirst", false);

	@Bean
	public KmsManager kmsManager() {
		JsonArray kmsUris = getPropertyJson(
				KurentoRoomServerApp.KMSS_URIS_PROPERTY,
				KurentoRoomServerApp.KMSS_URIS_DEFAULT, JsonArray.class);
		List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

		log.info("Configuring Kurento Room Server to use the following kmss: "
				+ kmsWsUris);

		FixedNKmsManager fixedKmsManager = new FixedNKmsManager(kmsWsUris, DEMO_KMS_NODE_LIMIT);
		fixedKmsManager.setAuthRegex(DEMO_AUTH_REGEX);
		return fixedKmsManager;
	}

	@Bean
	public JsonRpcUserControl userControl() {
		DemoJsonRpcUserControl uc = new DemoJsonRpcUserControl();
		uc.setEnableHatFilter(DEMO_HAT_FILTER);
		uc.setHatOnlyOnFirst(DEMO_HAT_ONLY_ON_FIRST);
		if (DEMO_HAT_FILTER) {
			String appServerUrl = System.getProperty("app.server.url",
					DEFAULT_APP_SERVER_URL);
			String hatUrl;
			if (appServerUrl.endsWith("/"))
				hatUrl = appServerUrl + "img/mario-wings.png";
			else
				hatUrl = appServerUrl + "/img/mario-wings.png";
			uc.setHatUrl(hatUrl);
		}
		return uc;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(KurentoRoomServerApp.class, args);
	}
}
