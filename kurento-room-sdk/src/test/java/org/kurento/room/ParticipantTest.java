/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.room;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;
import org.kurento.test.services.KurentoClientTestFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.JsonArray;

public class ParticipantTest {

	private RoomManager manager;
	private UserNotificationService notificationService;
	private KurentoClientProvider kcProvider;

	private String user0 = "user0";
	private String room0 = "room0";

	@Before
	public void setup() {
		notificationService = mock(UserNotificationService.class);
		kcProvider = mock(KurentoClientProvider.class);
		manager = new RoomManager(notificationService, kcProvider);
	}

	@After
	public void tearDown() {
		manager.close();
	}
	
	@Test
	public void joinNewRoom() {
		ParticipantRequest preq = new ParticipantRequest(user0, null);

		JsonArray result = new JsonArray();
		Mockito.doThrow(
				new RoomException(Code.GENERIC_ERROR_CODE,
						"Generic room exception")).when(notificationService)
				.sendResponse(preq, result);

		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, CoreMatchers.is(2));
				assertThat(args[0],
						CoreMatchers.instanceOf(ParticipantRequest.class));
				assertThat(args[1], CoreMatchers.instanceOf(JsonArray.class));

				return null;
			}
		})
				.when(notificationService)
				.sendResponse(Mockito.any(ParticipantRequest.class),
						Mockito.anyString());;

		Mockito.doAnswer(new Answer<KurentoClient>() {

			@Override
			public KurentoClient answer(InvocationOnMock invocation)
					throws Throwable {
				// FIXME it requires KMS to be functioning
				return KurentoClientTestFactory.createKurentoForTest();
			}
		}).when(kcProvider).getKurentoClient(user0);

		manager.joinRoom(user0, room0, preq);

		Assert.assertTrue(manager.getRooms().contains(room0));
		Assert.assertTrue(manager.getParticipants(room0).contains(
				new UserParticipant(user0, user0)));
	}
}
