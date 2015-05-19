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
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.KurentoRoomServerApp;
import org.kurento.room.api.ParticipantSession;
import org.kurento.room.api.RoomException;
import org.kurento.room.api.RoomRequestsFilter;
import org.kurento.room.api.TrickleIceEndpoint.EndpointBuilder;
import org.kurento.room.kms.FixedNKmsManager;
import org.kurento.room.kms.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@SpringBootApplication
public class KurentoRoomDemoApp {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomDemoApp.class);

	private final String DEFAULT_APP_SERVER_URL = PropertiesManager
			.getProperty("app.uri", "http://localhost:8080");

	@Bean
	public KmsManager kmsManager() {
		JsonArray kmsUris = getPropertyJson(
				KurentoRoomServerApp.KMSS_URIS_PROPERTY,
				KurentoRoomServerApp.KMSS_URIS_DEFAULT, JsonArray.class);
		List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

		log.info("Configuring Kurento Room Server to use the following kmss: "
				+ kmsWsUris);

		return new FixedNKmsManager(kmsWsUris);
	}

	@Bean
	public EndpointBuilder endpointBuilder() {
		String appServerUrl = System.getProperty("app.server.url",
				DEFAULT_APP_SERVER_URL);
		String hatUrl;
		if (appServerUrl.endsWith("/"))
			hatUrl = appServerUrl + "img/mario-wings.png";
		else
			hatUrl = appServerUrl + "/img/mario-wings.png";
		return new PirateEndpoint.PirateEndpointBuilder(hatUrl);
	}

	@Bean
	public RoomRequestsFilter reqFilter() {
		return new RoomRequestsFilter() {
			@Override
			public void filterUserRequest(Request<JsonObject> request,
					ParticipantSession participantSession,
					SessionState sessionState) throws RoomException {
				String ps = null;
				if (participantSession != null
						&& participantSession.getParticipant() != null)
					ps = participantSession.getParticipant().toString();
				log.info("REQ-FILTER> {} | {} | {}", sessionState, request, ps);
				if (request != null) {
					String token = null;
					if (request.getParams().has("token")) {
						token = request.getParams().get("token")
								.getAsString();
						log.debug("Security token: {}", token);
						// TODO check token, etc ...
					}
					if (token == null)
						throw new RoomException(
								RoomException.REQ_FILTER_ERROR_CODE,
								"Not authorized");
				}
			}
		};
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(KurentoRoomServerApp.class, args);
	}
}
