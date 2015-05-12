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

import org.kurento.commons.ConfigFileManager;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.room.demo.api.RoomManager;
import org.kurento.room.demo.api.control.JsonRpcParticipantControl;
import org.kurento.room.demo.internal.RoomManagerImpl;
import org.kurento.room.demo.internal.control.ParticipantControlImpl;
import org.kurento.room.demo.kms.FixedOneKmsManager;
import org.kurento.room.demo.kms.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.google.gson.JsonArray;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
@Configuration
@Import(JsonRpcConfiguration.class)
@EnableAutoConfiguration
@ComponentScan
public class KurentoRoomServerApp implements JsonRpcConfigurer {

	static {
		ConfigFileManager.loadConfigFile("roomserver.conf.json");
	}

	public static final String KMSS_URIS_PROPERTY = "kms.uris";
	public static final String KMSS_URIS_DEFAULT = "[ \"ws://localhost:8888/kurento\" ]";

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomServerApp.class);

	@Bean
	@ConditionalOnMissingBean
	public KmsManager kmsManager() {
		JsonArray kmsUris = getPropertyJson(KMSS_URIS_PROPERTY,
				KMSS_URIS_DEFAULT, JsonArray.class);
		List<String> kmsWsUris = JsonUtils.toStringList(kmsUris);

		log.info("Configuring Kurento Room Server to use first of the following kmss: "
				+ kmsWsUris);

		return new FixedOneKmsManager(kmsWsUris.get(0));
	}

	@Bean
	public RoomManager roomManager() {
		return new RoomManagerImpl();
	}

	@Bean
	public JsonRpcParticipantControl participantControl() {
		return new ParticipantControlImpl();
	}

	@Bean
	@ConditionalOnMissingBean
	public RoomJsonRpcHandler groupCallHandler() {
		return new RoomJsonRpcHandler();
	}

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addHandler(groupCallHandler(), "/room");
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(KurentoRoomServerApp.class, args);
	}
}
