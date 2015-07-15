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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.PassThrough;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.AdminException;
import org.kurento.room.exception.RoomException;
import org.kurento.room.internal.DefaultRoomEventHandler;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Tests for {@link RoomManager} when using {@link DefaultRoomEventHandler}
 * (mocked {@link UserNotificationService} and {@link KurentoClient} resources).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "org.kurento.*")
public class RoomWithDefaultHandlerTest {

	private static final String SDP_OFFER = "peer sdp offer";
	private static final String SDP_ANSWER = "endpoint sdp answer";
	private static final int USERS = 10;
	private static final int ROOMS = 3;

	private RoomManager manager;

	@Mock
	private UserNotificationService notificationService;
	@Mock
	private KurentoClientProvider kcProvider;

	@Mock
	private KurentoClient kurentoClient;
	@Captor
	private ArgumentCaptor<Continuation<MediaPipeline>> kurentoClientCaptor;

	@Mock
	private MediaPipeline pipeline;
	@Mock
	private WebRtcEndpoint.Builder webRtcBuilder;
	@Captor
	private ArgumentCaptor<Continuation<WebRtcEndpoint>> webRtcCaptor;

	@Mock
	private PassThrough.Builder passThruBuilder;

	@Mock
	private WebRtcEndpoint endpoint;
	@Mock
	private PassThrough passThru;
	@Captor
	private ArgumentCaptor<EventListener<OnIceCandidateEvent>> iceEventCaptor;
	@Captor
	private ArgumentCaptor<EventListener<ErrorEvent>> mediaErrorEventCaptor;
	@Captor
	private ArgumentCaptor<EventListener<ErrorEvent>> pipelineErrorEventCaptor;

	private String userx = "userx";
	private String requestIdx = "requestx";
	private String roomx = "roomx";

	// usernames will be used as participantIds
	private String[] users = new String[USERS];
	private String[] requestIds = new String[USERS];
	private String[] rooms = new String[ROOMS];


	private Map<String, ParticipantRequest> usersParticipantRequests =
			new HashMap<String, ParticipantRequest>();
	private Map<String, UserParticipant> usersParticipants =
			new HashMap<String, UserParticipant>();

	@Before
	public void setup() {
		manager = new RoomManager(notificationService, kcProvider);

		doAnswer(new Answer<KurentoClient>() {
			@Override
			public KurentoClient answer(InvocationOnMock invocation)
					throws Throwable {
				return kurentoClient;
			}
		}).when(kcProvider).getKurentoClient(anyString());

		// not used anymore, replaced by the Continuation version
		// when(kurentoClient.createMediaPipeline()).thenAnswer(
		// new Answer<MediaPipeline>() {
		// @Override
		// public MediaPipeline answer(InvocationOnMock invocation)
		// throws Throwable {
		// return pipeline;
		// }
		// });

		// call onSuccess when creating the pipeline to use the mocked instance
		doAnswer(new Answer<Continuation<MediaPipeline>>() {
			@Override
			public Continuation<MediaPipeline> answer(
					InvocationOnMock invocation) throws Throwable {
				kurentoClientCaptor.getValue().onSuccess(pipeline);
				return null;
			}
		}).when(kurentoClient).createMediaPipeline(
				kurentoClientCaptor.capture());

		// call onSuccess when building the endpoint to use the mocked instance
		doAnswer(new Answer<Continuation<WebRtcEndpoint>>() {
			@Override
			public Continuation<WebRtcEndpoint> answer(
					InvocationOnMock invocation) throws Throwable {
				webRtcCaptor.getValue().onSuccess(endpoint);
				return null;
			}
		}).when(webRtcBuilder).buildAsync(webRtcCaptor.capture());

		// not used anymore, replaced by the Continuation version
		// when(webRtcBuilder.build()).thenReturn(endpoint);

		// still using the sync version
		when(passThruBuilder.build()).thenReturn(passThru);

		try { // mock the constructor for the endpoint builder
			whenNew(WebRtcEndpoint.Builder.class).withArguments(pipeline)
					.thenAnswer(new Answer<WebRtcEndpoint.Builder>() {
						@Override
						public WebRtcEndpoint.Builder answer(
								InvocationOnMock invocation) throws Throwable {
							return webRtcBuilder;
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try { // mock the constructor for the passThru builder
			whenNew(PassThrough.Builder.class).withArguments(pipeline)
					.thenAnswer(new Answer<PassThrough.Builder>() {

						@Override
						public PassThrough.Builder answer(
								InvocationOnMock invocation) throws Throwable {

							return passThruBuilder;
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		when(endpoint.processOffer(anyString())).thenReturn(SDP_ANSWER);

		for (int i = 0; i < USERS; i++) {
			users[i] = "user" + i;
			requestIds[i] = "requestId" + i;
			usersParticipantRequests.put(users[i], new ParticipantRequest(
					users[i], requestIds[i]));
			usersParticipants.put(users[i], new UserParticipant(users[i],
					users[i]));
		}
		for (int i = 0; i < ROOMS; i++)
			rooms[i] = "room" + i;
	}

	@After
	public void tearDown() {
		manager.close();
	}

	@Test
	public void joinNewRoom() throws AdminException {
		final ParticipantRequest participantRequest =
				new ParticipantRequest(userx, requestIdx);
		Map<String, ParticipantRequest> participantsRequests =
				new HashMap<String, ParticipantRequest>();
		participantsRequests.put(userx, participantRequest);

		assertThat(manager.getRooms(), not(hasItem(roomx)));

		userJoinRoom(roomx, userx, participantRequest, participantsRequests);

		assertThat(manager.getRooms(), hasItem(roomx));
		assertThat(manager.getParticipants(roomx), hasItem(new UserParticipant(
				userx, userx)));

		verifyNotificationService(1, 0, 0,
				DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD);
	}

	@Test
	public void joinManyUsersOneRoom() throws AdminException {
		int responses = 0;
		int joinedNotifications = 0;
		for (Entry<String, ParticipantRequest> request : usersParticipantRequests
				.entrySet()) {
			String user = request.getKey();
			if (responses == 0)
				assertThat(manager.getRooms(), not(hasItem(roomx)));
			else
				assertThat(manager.getParticipants(roomx),
						not(hasItem(usersParticipants.get(user))));

			userJoinRoom(roomx, user, request.getValue(),
					usersParticipantRequests);

			if (responses == 0)
				assertThat(manager.getRooms(), hasItem(roomx));
			assertThat(manager.getParticipants(roomx),
					hasItem(usersParticipants.get(user)));

			verifyNotificationService(++responses, 0, joinedNotifications +=
					(responses - 1),
					DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD);
		}
	}

	@Test
	public void joinManyUsersManyRooms() {
		final Map<String, String> usersRooms = new HashMap<String, String>();
		final Map<String, List<String>> roomsUsers =
				new HashMap<String, List<String>>();
		for (int i = 0; i < users.length; i++) {
			String room = rooms[i % rooms.length];
			usersRooms.put(users[i], room);
			if (!roomsUsers.containsKey(room))
				roomsUsers.put(room, new ArrayList<String>());
			roomsUsers.get(room).add(users[i]);
		}

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				assertThat(usersRooms.keySet(), hasItem(participantId));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject notifParams = (JsonObject) args[2];
				assertNotNull(notifParams.get("id"));
				String joinedId = notifParams.get("id").getAsString();
				assertThat(joinedId, is(not(participantId)));
				assertThat(usersRooms.keySet(), hasItem(joinedId));

				String participantRoom = usersRooms.get(participantId);
				assertThat(roomsUsers.get(participantRoom), hasItem(joinedId));
				for (String otherRoom : rooms) {
					if (!participantRoom.equals(otherRoom))
						assertThat(roomsUsers.get(otherRoom),
								not(hasItem(joinedId)));
				}

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD),
				isA(JsonObject.class));

		for (Entry<String, String> userRoom : usersRooms.entrySet()) {
			manager.joinRoom(userRoom.getKey(), userRoom.getValue(),
					usersParticipantRequests.get(userRoom.getKey()));
		}
		// verifies create media pipeline was called once for each new room
		verify(kurentoClient, times(roomsUsers.size())).createMediaPipeline(
				kurentoClientCaptor.capture());

		int expectedNotifications = 0;
		for (Entry<String, List<String>> roomUser : roomsUsers.entrySet())
			expectedNotifications +=
					roomUser.getValue().size()
							* (roomUser.getValue().size() - 1) / 2;
		verifyNotificationService(users.length, 0, expectedNotifications,
				DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD);
	}

	@Test
	public void leaveRoom() throws AdminException {
		joinManyUsersOneRoom();

		final ParticipantRequest participantRequestX =
				new ParticipantRequest(userx, requestIdx);
		usersParticipantRequests.put(userx, participantRequestX);

		userJoinRoom(roomx, userx, participantRequestX,
				usersParticipantRequests);

		assertThat(manager.getParticipants(roomx), hasItem(new UserParticipant(
				userx, userx)));

		verifyNotificationService(users.length + 1, 0, users.length
				* (users.length + 1) / 2,
				DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				assertThat(Arrays.asList(users), hasItem((String) args[0]));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = new JsonObject();
				params.addProperty("name", userx);
				assertThat((JsonObject) args[2], is(params));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.PARTICIPANT_LEFT_METHOD),
				isA(JsonObject.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				assertThat((ParticipantRequest) args[0],
						is(participantRequestX));

				assertThat(args[1], instanceOf(JsonObject.class));
				assertThat((JsonObject) args[1], is(new JsonObject()));

				return null;
			}
		}).when(notificationService).sendResponse(eq(participantRequestX),
				isA(JsonObject.class));

		manager.leaveRoom(participantRequestX);

		assertThat(manager.getParticipants(roomx),
				not(hasItem(new UserParticipant(userx, userx))));

		verifyNotificationService(users.length + 2, 0, users.length,
				DefaultRoomEventHandler.PARTICIPANT_LEFT_METHOD);
	}

	@Test
	public void onePublisher() throws AdminException {
		joinManyUsersOneRoom();

		final ParticipantRequest participantRequest0 =
				usersParticipantRequests.get(users[0]);

		participantPublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(1));
		verifyNotificationService(users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD);

		participantsSubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));
		verifyNotificationService(2 * users.length, 0, -1, null);

		participantUnpublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(0));
		verifyNotificationService(2 * users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_UNPUBLISHED_METHOD);

		participantsUnsubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(0));
		verifyNotificationService(3 * users.length, 0, -1, null);
	}

	@Test
	public void joinAndPublish() throws AdminException {
		int responses = 0;
		int joinedNotifications = 0;
		int published = 0;
		for (Entry<String, ParticipantRequest> request : usersParticipantRequests
				.entrySet()) {
			joinedNotifications++;
			String user = request.getKey();
			if (responses == 0)
				assertThat(manager.getRooms(), not(hasItem(roomx)));
			else
				assertThat(manager.getParticipants(roomx),
						not(hasItem(usersParticipants.get(user))));

			userJoinRoom(roomx, user, request.getValue(),
					usersParticipantRequests);

			if (responses == 0)
				assertThat(manager.getRooms(), hasItem(roomx));
			assertThat(manager.getParticipants(roomx),
					hasItem(usersParticipants.get(user)));

			verifyNotificationService(++responses, 0, joinedNotifications
					* (joinedNotifications - 1) / 2,
					DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD);

			assertThat(manager.getPublishers(roomx).size(), is(published));

			participantPublish(request.getValue());
			usersParticipants.get(user).setStreaming(true);

			assertThat(manager.getPublishers(roomx).size(), is(++published));

			verifyNotificationService(++responses, 0, published
					* (published - 1) / 2,
					DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD);
		}
	}

	@Test
	public void manyPublishers() throws AdminException {
		joinManyUsersOneRoom();

		int published = 0;
		int responses = users.length;
		for (ParticipantRequest publisher : usersParticipantRequests.values()) {
			participantPublish(publisher);
			assertThat(manager.getPublishers(roomx).size(), is(++published));
			verifyNotificationService(responses += 1, 0, published
					* (users.length - 1),
					DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD);

			participantsSubscribe(publisher);
			if (published == 1)
				assertThat(manager.getSubscribers(roomx).size(),
						is(users.length - 1));
			else
				assertThat(manager.getSubscribers(roomx).size(),
						is(users.length));
			verifyNotificationService(responses += (users.length - 1), 0, -1,
					null);
		}

		for (UserParticipant userParticipant : usersParticipants.values()) {
			Set<UserParticipant> allOthers =
					new HashSet<UserParticipant>(usersParticipants.values());
			allOthers.remove(userParticipant);
			assertThat(manager.getPeerPublishers(userParticipant
					.getParticipantId()), equalTo(allOthers));
			assertThat(manager.getPeerSubscribers(userParticipant
					.getParticipantId()), equalTo(allOthers));
		}

		for (ParticipantRequest publisher : usersParticipantRequests.values()) {
			participantUnpublish(publisher);
			assertThat(manager.getPublishers(roomx).size(), is(--published));
			verifyNotificationService(responses += 1, 0,
					(users.length - published) * (users.length - 1),
					DefaultRoomEventHandler.PARTICIPANT_UNPUBLISHED_METHOD);

			participantsUnsubscribe(publisher);
			int expectedSubscribers = users.length;
			switch (published) {
				case 0:
					expectedSubscribers = 0;
					break;
				case 1:
					expectedSubscribers = users.length - 1;
					break;
			}
			assertThat(manager.getSubscribers(roomx).size(),
					is(expectedSubscribers));
			verifyNotificationService(responses += (users.length - 1), 0, -1,
					null);
		}
	}

	@Test
	public void sendManyMessages() throws AdminException {
		joinManyUsersOneRoom();
		int responses = users.length;
		int sentMessages = 0;
		for (final ParticipantRequest sender : usersParticipantRequests
				.values()) {
			final String message = "New message from " + sender;
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation)
						throws Throwable {
					Object[] args = invocation.getArguments();

					assertThat(args.length, is(2));

					assertThat(args[0], instanceOf(ParticipantRequest.class));
					ParticipantRequest participantRequest =
							(ParticipantRequest) args[0];
					assertThat(participantRequest, is(sender));
					assertThat(usersParticipantRequests.values(),
							hasItem(participantRequest));

					assertThat(args[1], instanceOf(JsonObject.class));
					assertThat((JsonObject) args[1], is(new JsonObject()));

					return null;
				}
			}).when(notificationService).sendResponse(
					isA(ParticipantRequest.class), isA(JsonObject.class));

			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation)
						throws Throwable {
					Object[] args = invocation.getArguments();

					assertThat(args.length, is(3));

					assertThat(args[0], instanceOf(String.class));
					String participantId = (String) args[0];
					assertThat(Arrays.asList(users), hasItem(participantId));

					assertThat(args[2], instanceOf(JsonObject.class));
					JsonObject params = new JsonObject();
					params.addProperty("room", roomx);
					params.addProperty("user", sender.getParticipantId());
					params.addProperty("message", message);
					assertThat((JsonObject) args[2], is(params));

					return null;
				}
			}).when(notificationService)
					.sendNotification(
							anyString(),
							eq(DefaultRoomEventHandler.PARTICIPANT_SEND_MESSAGE_METHOD),
							isA(JsonObject.class));

			manager.sendMessage(message, sender.getParticipantId(), roomx,
					sender);

			verifyNotificationService(responses += 1, 0, ++sentMessages
					* users.length,
					DefaultRoomEventHandler.PARTICIPANT_SEND_MESSAGE_METHOD);
		}
	}

	@Test
	public void iceCandidate() throws AdminException {

		joinManyUsersOneRoom();

		final ParticipantRequest participantRequest0 =
				usersParticipantRequests.get(users[0]);

		participantPublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(1));
		verifyNotificationService(users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD);

		// verifies listener is added to publisher
		verify(endpoint, times(1)).addOnIceCandidateListener(
				iceEventCaptor.capture());

		participantsSubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));
		verifyNotificationService(2 * users.length, 0, -1, null);

		// verifies listener is added to each subscriber
		// (publisher + all others)
		verify(endpoint, times(usersParticipantRequests.size()))
				.addOnIceCandidateListener(iceEventCaptor.capture());

		// stub sendNotification of type ICE_CANDIDATE_METHOD
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				// not the publisher, the captor is for one of the subscribers
				assertThat(participantId, is(not(users[0])));
				// must belong to the registered peers
				assertThat(usersParticipantRequests.keySet(),
						hasItem(participantId));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = (JsonObject) args[2];
				assertNotNull(params
						.get(DefaultRoomEventHandler.ON_ICE_EP_NAME_PARAM));
				String endpointName =
						params.get(DefaultRoomEventHandler.ON_ICE_EP_NAME_PARAM)
								.getAsString();
				assertThat(endpointName, is(users[0]));

				assertNotNull(params
						.get(DefaultRoomEventHandler.ON_ICE_SDP_M_LINE_INDEX_PARAM));
				int index =
						params.get(
								DefaultRoomEventHandler.ON_ICE_SDP_M_LINE_INDEX_PARAM)
								.getAsInt();
				assertThat(index, is(1));

				assertNotNull(params
						.get(DefaultRoomEventHandler.ON_ICE_SDP_MID_PARAM));
				String sdpMid =
						params.get(DefaultRoomEventHandler.ON_ICE_SDP_MID_PARAM)
								.getAsString();
				assertThat(sdpMid, is("audio"));

				assertNotNull(params
						.get(DefaultRoomEventHandler.ON_ICE_CANDIDATE_PARAM));
				String candidate =
						params.get(
								DefaultRoomEventHandler.ON_ICE_CANDIDATE_PARAM)
								.getAsString();
				assertThat(candidate, is("1 candidate test"));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.ICE_CANDIDATE_METHOD),
				isA(JsonObject.class));

		// triggers the last captured listener
		iceEventCaptor.getValue().onEvent(
				new OnIceCandidateEvent(endpoint, "12345", null, "candidate",
						new IceCandidate("1 candidate test", "audio", 1)));

		verifyNotificationService(2 * users.length, 0, 1,
				DefaultRoomEventHandler.ICE_CANDIDATE_METHOD);

		participantUnpublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(0));
		verifyNotificationService(2 * users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_UNPUBLISHED_METHOD);

		participantsUnsubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(0));
		verifyNotificationService(3 * users.length, 0, -1, null);
	}

	@Test
	public void mediaError() throws AdminException {

		joinManyUsersOneRoom();

		final ParticipantRequest participantRequest0 =
				usersParticipantRequests.get(users[0]);

		participantPublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(1));
		verifyNotificationService(users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD);

		// verifies error listener is added to publisher
		verify(endpoint, times(1)).addErrorListener(
				mediaErrorEventCaptor.capture());

		// stub sendNotification of type MEDIA_ERROR_METHOD
		final String expectedErrorMessage =
				"TEST_ERR: Fake media error(errCode=101)";
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				// must be the publisher
				assertThat(participantId, is(users[0]));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = (JsonObject) args[2];
				assertNotNull(params.get("error"));
				String error = params.get("error").getAsString();
				assertThat(error, is(expectedErrorMessage));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.MEDIA_ERROR_METHOD),
				isA(JsonObject.class));

		// triggers the captured listener
		mediaErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(endpoint, "12345", null, "Fake media error",
						101, "TEST_ERR"));

		// subscribe all to publisher
		participantsSubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));
		verifyNotificationService(2 * users.length, 0, -1, null);

		// verifies listener is added to each subscriber
		verify(endpoint, times(usersParticipantRequests.size()))
				.addErrorListener(mediaErrorEventCaptor.capture());

		// stub sendNotification of type MEDIA_ERROR_METHOD
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				// can be any peer but not the publisher
				// (capturing an event for one of the subscribers)
				assertThat(usersParticipantRequests.keySet(),
						hasItem(participantId));
				assertThat(participantId, is(not(users[0])));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = (JsonObject) args[2];
				assertNotNull(params.get("error"));
				String error = params.get("error").getAsString();
				assertThat(error, is(expectedErrorMessage));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.MEDIA_ERROR_METHOD),
				isA(JsonObject.class));

		// triggers the last captured listener (once again)
		mediaErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(endpoint, "12345", null, "Fake media error",
						101, "TEST_ERR"));

		// the error was "triggered" two times, thus 2 notifications
		verifyNotificationService(2 * users.length, 0, 2,
				DefaultRoomEventHandler.MEDIA_ERROR_METHOD);

		participantUnpublish(participantRequest0);
		assertThat(manager.getPublishers(roomx).size(), is(0));
		verifyNotificationService(2 * users.length + 1, 0, users.length - 1,
				DefaultRoomEventHandler.PARTICIPANT_UNPUBLISHED_METHOD);

		participantsUnsubscribe(participantRequest0);
		assertThat(manager.getSubscribers(roomx).size(), is(0));
		verifyNotificationService(3 * users.length, 0, -1, null);
	}

	@Test
	public void pipelineError() throws AdminException {
		joinManyUsersOneRoom();

		// verifies pipeline error listener is added to room
		verify(pipeline, times(1)).addErrorListener(
				pipelineErrorEventCaptor.capture());

		// stub sendNotification of type MEDIA_ERROR_METHOD
		final String expectedErrorMessage =
				"TEST_PP_ERR: Fake pipeline error(errCode=505)";
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				assertThat(usersParticipantRequests.keySet(),
						hasItem(participantId));
				assertThat(participantId, is(not(userx)));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = (JsonObject) args[2];
				assertNotNull(params.get("error"));
				String error = params.get("error").getAsString();
				assertThat(error, is(expectedErrorMessage));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.MEDIA_ERROR_METHOD),
				isA(JsonObject.class));

		// triggers the last captured listener
		pipelineErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(pipeline, "12345", null, "Fake pipeline error",
						505, "TEST_PP_ERR"));

		// the error was "triggered" one time and all participants get notified
		verifyNotificationService(users.length, 0,
				usersParticipantRequests.size(),
				DefaultRoomEventHandler.MEDIA_ERROR_METHOD);
	}

	private void userJoinRoom(final String room, String user,
			final ParticipantRequest participantRequest,
			final Map<String, ParticipantRequest> participants) {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				assertThat(participants.keySet(), hasItem(participantId));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject notifParams = (JsonObject) args[2];
				assertNotNull(notifParams.get("id"));
				String joinedId = notifParams.get("id").getAsString();
				assertThat(joinedId, is(not(participantId)));
				assertThat(joinedId, is(participantRequest.getParticipantId()));
				assertThat(participants.keySet(), hasItem(joinedId));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.PARTICIPANT_JOINED_METHOD),
				isA(JsonObject.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				ParticipantRequest responseParticipantRequest =
						(ParticipantRequest) args[0];
				assertThat(participants.values(),
						hasItem(responseParticipantRequest));
				assertThat(responseParticipantRequest, is(participantRequest));

				assertThat(args[1], instanceOf(JsonArray.class));
				JsonArray result = (JsonArray) args[1];
				Iterator<JsonElement> resultIt = result.iterator();
				while (resultIt.hasNext()) {
					JsonElement elem = resultIt.next();
					assertThat(elem, instanceOf(JsonObject.class));
					JsonObject peerJson = (JsonObject) elem;
					assertNotNull(peerJson.get("id"));
					String peerName = peerJson.get("id").getAsString();
					assertThat(peerName, is(not(responseParticipantRequest
							.getParticipantId())));
					assertThat(participants.keySet(), hasItem(peerName));

					if (peerJson.get("streams") != null) {
						assertThat(manager.getPublishers(room),
								hasItem(usersParticipants.get(peerName)));
						JsonElement streamsElem = peerJson.get("streams");
						assertThat(streamsElem, instanceOf(JsonArray.class));
						JsonArray streamsArray = (JsonArray) streamsElem;
						JsonObject stream = new JsonObject();
						stream.addProperty("id", "webcam");
						assertThat(streamsArray, hasItem(stream));
					}
				}

				return null;
			}
		}).when(notificationService).sendResponse(
				isA(ParticipantRequest.class), isA(JsonArray.class));


		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));
				assertThat(args[2], instanceOf(RoomException.class));
				RoomException error = (RoomException) args[2];
				fail(error.getCode() + ": " + error.getMessage());

				return null;
			}
		}).when(notificationService).sendErrorResponse(
				any(ParticipantRequest.class), any(), any(RoomException.class));

		manager.joinRoom(user, room, participantRequest);

		// verifies create media pipeline was called once
		verify(kurentoClient, times(1)).createMediaPipeline(
				kurentoClientCaptor.capture());
	}

	private void participantsSubscribe(final ParticipantRequest publisher) {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				ParticipantRequest participantRequest =
						(ParticipantRequest) args[0];
				assertThat(participantRequest, is(not(publisher)));
				assertThat(usersParticipantRequests.values(),
						hasItem(participantRequest));

				assertThat(args[1], instanceOf(JsonObject.class));
				JsonObject result = (JsonObject) args[1];
				assertNotNull(result.get("sdpAnswer"));
				String sdpAnswer = result.get("sdpAnswer").getAsString();
				assertThat(sdpAnswer, containsString(SDP_ANSWER));

				return null;
			}
		}).when(notificationService).sendResponse(
				isA(ParticipantRequest.class), isA(JsonObject.class));

		for (ParticipantRequest subscriber : usersParticipantRequests.values())
			if (!subscriber.equals(publisher))
				manager.subscribe(publisher.getParticipantId(), SDP_OFFER,
						subscriber);
	}

	private void participantsUnsubscribe(final ParticipantRequest publisher) {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				ParticipantRequest participantRequest =
						(ParticipantRequest) args[0];
				assertThat(participantRequest, is(not(publisher)));
				assertThat(usersParticipantRequests.values(),
						hasItem(participantRequest));

				assertThat(args[1], instanceOf(JsonObject.class));
				assertThat((JsonObject) args[1], is(new JsonObject()));

				return null;
			}
		}).when(notificationService).sendResponse(
				isA(ParticipantRequest.class), isA(JsonObject.class));

		for (ParticipantRequest subscriber : usersParticipantRequests.values())
			if (!subscriber.equals(publisher))
				manager.unsubscribe(publisher.getParticipantId(), subscriber);
	}

	private void participantPublish(final ParticipantRequest participantRequest) {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				assertThat(Arrays.asList(users), hasItem(participantId));
				assertThat(participantId,
						is(not(participantRequest.getParticipantId())));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = new JsonObject();
				params.addProperty("id", participantRequest.getParticipantId());
				JsonObject stream = new JsonObject();
				stream.addProperty("id", "webcam");
				JsonArray streamsArray = new JsonArray();
				streamsArray.add(stream);
				params.add("streams", streamsArray);
				assertThat((JsonObject) args[2], is(params));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.PARTICIPANT_PUBLISHED_METHOD),
				isA(JsonObject.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				assertThat((ParticipantRequest) args[0], is(participantRequest));

				assertThat(args[1], instanceOf(JsonObject.class));
				JsonObject result = (JsonObject) args[1];
				assertNotNull(result.get("sdpAnswer"));
				String sdpAnswer = result.get("sdpAnswer").getAsString();
				assertThat(sdpAnswer, containsString(SDP_ANSWER));

				return null;
			}
		}).when(notificationService).sendResponse(eq(participantRequest),
				isA(JsonObject.class));

		manager.publishMedia(participantRequest, SDP_OFFER, false);
	}

	private void participantUnpublish(
			final ParticipantRequest participantRequest) {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(3));

				assertThat(args[0], instanceOf(String.class));
				String participantId = (String) args[0];
				assertThat(Arrays.asList(users), hasItem(participantId));
				assertThat(participantId,
						is(not(participantRequest.getParticipantId())));

				assertThat(args[2], instanceOf(JsonObject.class));
				JsonObject params = new JsonObject();
				params.addProperty("name",
						participantRequest.getParticipantId());
				assertThat((JsonObject) args[2], is(params));

				return null;
			}
		}).when(notificationService).sendNotification(anyString(),
				eq(DefaultRoomEventHandler.PARTICIPANT_UNPUBLISHED_METHOD),
				isA(JsonObject.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				assertThat(args.length, is(2));

				assertThat(args[0], instanceOf(ParticipantRequest.class));
				assertThat((ParticipantRequest) args[0], is(participantRequest));

				assertThat(args[1], instanceOf(JsonObject.class));
				assertThat((JsonObject) args[1], is(new JsonObject()));

				return null;
			}
		}).when(notificationService).sendResponse(eq(participantRequest),
				isA(JsonObject.class));

		manager.unpublishMedia(participantRequest);
	}

	private void verifyNotificationService(int responses, int errorResponses,
			int notifications, String notificationMethod) {
		if (responses > -1)
			verify(notificationService, times(responses)).sendResponse(
					any(ParticipantRequest.class), isA(JsonElement.class));

		if (errorResponses > -1)
			verify(notificationService, times(errorResponses))
					.sendErrorResponse(any(ParticipantRequest.class), any(),
							any(RoomException.class));

		if (notifications > -1)
			verify(notificationService, times(notifications)).sendNotification(
					anyString(), eq(notificationMethod), isA(JsonObject.class));
	}

}
