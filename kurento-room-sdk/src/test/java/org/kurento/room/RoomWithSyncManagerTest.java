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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.PassThrough;
import org.kurento.client.ServerManager;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.RoomHandler;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.AdminException;
import org.kurento.room.internal.DefaultRoomEventHandler;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for {@link SyncRoomManager} when using a mo
 * {@link DefaultRoomEventHandler} (mocked {@link UserNotificationService} and
 * {@link KurentoClient} resources).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "org.kurento.*")
public class RoomWithSyncManagerTest {

	private static final String SDP_OFFER = "peer sdp offer";
	private static final String SDP_ANSWER = "endpoint sdp answer";
	private static final int USERS = 10;
	private static final int ROOMS = 3;

	private SyncRoomManager manager;

	@Mock
	private KurentoClientProvider kcProvider;
	@Mock
	private RoomHandler roomHandler;

	@Mock
	private KurentoClient kurentoClient;
	@Mock
	private ServerManager serverManager;
	@Captor
	private ArgumentCaptor<Continuation<MediaPipeline>> kurentoClientCaptor;

	@Mock
	private MediaPipeline pipeline;
	@Mock
	private WebRtcEndpoint.Builder webRtcBuilder;
	@Captor
	private ArgumentCaptor<Continuation<WebRtcEndpoint>> webRtcCaptor;
	@Captor
	private ArgumentCaptor<Continuation<Void>> webRtcConnectCaptor;

	@Mock
	private PassThrough.Builder passThruBuilder;

	@Mock
	private WebRtcEndpoint endpoint;
	@Mock
	private PassThrough passThru;

	@Mock
	private FaceOverlayFilter.Builder faceFilterBuilder;
	@Mock
	private FaceOverlayFilter faceFilter;
	@Captor
	private ArgumentCaptor<Continuation<Void>> faceFilterConnectCaptor;

	@Captor
	private ArgumentCaptor<EventListener<OnIceCandidateEvent>> iceEventCaptor;
	@Captor
	private ArgumentCaptor<EventListener<ErrorEvent>> mediaErrorEventCaptor;
	@Captor
	private ArgumentCaptor<EventListener<ErrorEvent>> pipelineErrorEventCaptor;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private String userx = "userx";
	private String pidx = "pidx";
	private String roomx = "roomx";

	// usernames will be used as participantIds
	private String[] users = new String[USERS];
	private String[] rooms = new String[ROOMS];


	private Map<String, String> usersParticipantIds =
			new HashMap<String, String>();
	private Map<String, UserParticipant> usersParticipants =
			new HashMap<String, UserParticipant>();

	@Before
	public void setup() {
		manager = new SyncRoomManager(roomHandler, kcProvider);

		when(kcProvider.getKurentoClient(anyString()))
				.thenReturn(kurentoClient);
		when(kurentoClient.getServerManager()).thenReturn(serverManager);

		when(serverManager.getName()).thenReturn("mocked-kurento-client");

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

		// still using the sync version
		when(passThruBuilder.build()).thenReturn(passThru);

		try { // mock the constructor for the endpoint builder
			whenNew(WebRtcEndpoint.Builder.class).withArguments(pipeline)
					.thenReturn(webRtcBuilder);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try { // mock the constructor for the passThru builder
			whenNew(PassThrough.Builder.class).withArguments(pipeline)
					.thenReturn(passThruBuilder);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// mock the SDP answer when processing the offer on the endpoint
		when(endpoint.processOffer(anyString())).thenReturn(SDP_ANSWER);

		// call onSuccess when connecting the WebRtc endpoint to any media
		// filter
		doAnswer(new Answer<Continuation<Void>>() {
			@Override
			public Continuation<Void> answer(InvocationOnMock invocation)
					throws Throwable {
				webRtcConnectCaptor.getValue().onSuccess(null);
				return null;
			}
		}).when(endpoint).connect(any(MediaElement.class),
				webRtcConnectCaptor.capture());


		try { // mock the constructor for the face filter builder
			whenNew(FaceOverlayFilter.Builder.class).withArguments(pipeline)
					.thenReturn(faceFilterBuilder);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		// using the sync version to build the face filter
		when(faceFilterBuilder.build()).thenReturn(faceFilter);

		// call onSuccess when connecting the face filter to any media element
		doAnswer(new Answer<Continuation<Void>>() {
			@Override
			public Continuation<Void> answer(InvocationOnMock invocation)
					throws Throwable {
				faceFilterConnectCaptor.getValue().onSuccess(null);
				return null;
			}
		}).when(faceFilter).connect(any(MediaElement.class),
				faceFilterConnectCaptor.capture());

		when(pipeline.getId()).thenReturn("mocked-pipeline");
		when(endpoint.getId()).thenReturn("mocked-webrtc-endpoint");
		when(passThru.getId()).thenReturn("mocked-pass-through");
		when(faceFilter.getId()).thenReturn("mocked-faceoverlay-filter");

		for (int i = 0; i < USERS; i++) {
			users[i] = "user" + i;
			usersParticipantIds.put(users[i], "pid" + i);
			usersParticipants.put(users[i], new UserParticipant("pid" + i,
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
		assertThat(manager.getRooms(), not(hasItem(roomx)));

		assertTrue(userJoinRoom(roomx, userx, pidx, true).isEmpty());

		assertThat(manager.getRooms(), hasItem(roomx));
		assertThat(manager.getParticipants(roomx), hasItem(new UserParticipant(
				pidx, userx)));
	}

	@Test
	public void joinRoomFail() throws AdminException {
		assertThat(manager.getRooms(), not(hasItem(roomx)));

		exception.expect(AdminException.class);
		exception.expectMessage(containsString("must be created before"));
		userJoinRoom(roomx, userx, pidx, false);

		assertThat(manager.getRooms(), not(hasItem(roomx)));
	}

	@Test
	public void joinManyUsersOneRoom() throws AdminException {
		int count = 0;
		for (Entry<String, String> userPid : usersParticipantIds.entrySet()) {
			String user = userPid.getKey();
			String pid = userPid.getValue();

			if (count == 0)
				assertThat(manager.getRooms(), not(hasItem(roomx)));
			else
				assertThat(manager.getParticipants(roomx),
						not(hasItem(usersParticipants.get(user))));

			Set<UserParticipant> peers =
					userJoinRoom(roomx, user, pid, count == 0);

			if (count == 0) {
				assertTrue(peers.isEmpty());
				assertThat(manager.getRooms(), hasItem(roomx));
			} else
				assertTrue(!peers.isEmpty());

			assertThat(manager.getParticipants(roomx),
					hasItem(usersParticipants.get(user)));

			count++;
		}
	}

	@Test
	public void joinManyUsersManyRooms() throws AdminException {
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
		for (String room : roomsUsers.keySet())
			manager.createRoom(room);
		for (Entry<String, String> userRoom : usersRooms.entrySet()) {
			String user = userRoom.getKey();
			String room = userRoom.getValue();
			Set<UserParticipant> peers =
					manager.joinRoom(user, room, usersParticipantIds.get(user));
			if (peers.isEmpty())
				assertEquals("Expected one peer in room " + room + ": " + user,
						1, manager.getParticipants(room).size());
		}
		// verifies create media pipeline was called once for each new room
		verify(kurentoClient, times(roomsUsers.size())).createMediaPipeline(
				kurentoClientCaptor.capture());
	}

	@Test
	public void leaveRoom() throws AdminException {
		joinManyUsersOneRoom();
		assertTrue(!userJoinRoom(roomx, userx, pidx, false).isEmpty());
		UserParticipant userxParticipant = new UserParticipant(pidx, userx);
		assertThat(manager.getParticipants(roomx), hasItem(userxParticipant));
		Set<UserParticipant> remainingUsers = manager.leaveRoom(pidx);
		assertEquals(new HashSet<UserParticipant>(usersParticipants.values()),
				remainingUsers);
		assertEquals(manager.getParticipants(roomx), remainingUsers);
		assertThat(manager.getParticipants(roomx),
				not(hasItem(userxParticipant)));
	}

	@Test
	public void publisherLifecycle() throws AdminException {
		joinManyUsersOneRoom();

		String participantId0 = usersParticipantIds.get(users[0]);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		for (String pid : usersParticipantIds.values())
			if (!pid.equals(participantId0))
				assertEquals("SDP answer doesn't match", SDP_ANSWER,
						manager.subscribe(users[0], SDP_OFFER, pid));
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		manager.unpublishMedia(participantId0);
		assertThat(manager.getPublishers(roomx).size(), is(0));

		// peers are automatically unsubscribed
		assertThat(manager.getSubscribers(roomx).size(), is(0));
	}

	@Test
	public void getPublisherEndpoint() throws AdminException,
			InterruptedException {
		joinManyUsersOneRoom();

		final String participantId0 = usersParticipantIds.get(users[0]);

		// exception.expect(AdminException.class);
		// exception.expectMessage(containsString("Timeout reached while "
		// + "waiting for publisher endpoint to be ready"));
		// manager.getPublishEndpoint(participantId0);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		WebRtcEndpoint ep = manager.getPublishEndpoint(participantId0);
		assertThat("Publish endpoint is not the mocked instance", ep,
				is(endpoint));

		manager.unpublishMedia(participantId0);
		assertThat(manager.getPublishers(roomx).size(), is(0));
	}

	@Test
	public void publishAndLeave() throws AdminException {
		joinManyUsersOneRoom();

		String participantId0 = usersParticipantIds.get(users[0]);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		for (String pid : usersParticipantIds.values())
			if (!pid.equals(participantId0))
				assertEquals("SDP answer doesn't match", SDP_ANSWER,
						manager.subscribe(users[0], SDP_OFFER, pid));
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		Set<UserParticipant> remainingUsers = manager.leaveRoom(participantId0);
		Set<UserParticipant> roomParticipants = manager.getParticipants(roomx);
		assertEquals(roomParticipants, remainingUsers);
		assertThat(roomParticipants,
				not(hasItem(usersParticipants.get(users[0]))));
		assertThat(manager.getPublishers(roomx).size(), is(0));

		// peers are automatically unsubscribed
		assertThat(manager.getSubscribers(roomx).size(), is(0));
	}

	@Test
	public void addMediaFilterInParallel() throws AdminException,
			InterruptedException, ExecutionException {
		joinManyUsersOneRoom();

		final FaceOverlayFilter filter =
				new FaceOverlayFilter.Builder(pipeline).build();
		assertNotNull("FaceOverlayFiler is null", filter);
		assertThat(
				"Filter returned by the builder is not the same as the mocked one",
				filter, is(faceFilter));

		final String participantId0 = usersParticipantIds.get(users[0]);

		ExecutorService threadPool = Executors.newFixedThreadPool(1);
		ExecutorCompletionService<Void> exec =
				new ExecutorCompletionService<>(threadPool);
		exec.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				System.out.println("Starting execution of addMediaElement");
				manager.addMediaElement(participantId0, filter);
				return null;
			}
		});

		Thread.sleep(10);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		boolean firstSubscriber = true;
		for (String pid : usersParticipantIds.values()) {
			if (pid.equals(participantId0))
				continue;
			assertEquals("SDP answer doesn't match", SDP_ANSWER,
					manager.subscribe(users[0], SDP_OFFER, pid));
			if (firstSubscriber) {
				firstSubscriber = false;
				try {
					exec.take().get();
					System.out
							.println("Execution of addMediaElement ended (just after first peer subscribed)");
				} finally {
					threadPool.shutdownNow();
				}
			}
		}
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		verify(faceFilter, times(1)).connect(passThru,
				faceFilterConnectCaptor.getValue());
		verify(endpoint, times(1)).connect(faceFilter,
				webRtcConnectCaptor.getValue());

		Set<UserParticipant> remainingUsers = manager.leaveRoom(participantId0);
		Set<UserParticipant> roomParticipants = manager.getParticipants(roomx);
		assertEquals(roomParticipants, remainingUsers);
		assertThat(roomParticipants,
				not(hasItem(usersParticipants.get(users[0]))));
		assertThat(manager.getPublishers(roomx).size(), is(0));

		// peers are automatically unsubscribed
		assertThat(manager.getSubscribers(roomx).size(), is(0));
	}

	@Test
	public void addMediaFilterBeforePublishing() throws AdminException,
			InterruptedException, ExecutionException {
		joinManyUsersOneRoom();

		final FaceOverlayFilter filter =
				new FaceOverlayFilter.Builder(pipeline).build();
		assertNotNull("FaceOverlayFiler is null", filter);
		assertThat(
				"Filter returned by the builder is not the same as the mocked one",
				filter, is(faceFilter));

		final String participantId0 = usersParticipantIds.get(users[0]);

		System.out.println("Starting execution of addMediaElement");
		manager.addMediaElement(participantId0, filter);
		System.out.println("Execution of addMediaElement ended");

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		for (String pid : usersParticipantIds.values())
			if (!pid.equals(participantId0))
				assertEquals("SDP answer doesn't match", SDP_ANSWER,
						manager.subscribe(users[0], SDP_OFFER, pid));
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		verify(faceFilter, times(1)).connect(passThru,
				faceFilterConnectCaptor.getValue());
		verify(endpoint, times(1)).connect(faceFilter,
				webRtcConnectCaptor.getValue());

		Set<UserParticipant> remainingUsers = manager.leaveRoom(participantId0);
		Set<UserParticipant> roomParticipants = manager.getParticipants(roomx);
		assertEquals(roomParticipants, remainingUsers);
		assertThat(roomParticipants,
				not(hasItem(usersParticipants.get(users[0]))));
		assertThat(manager.getPublishers(roomx).size(), is(0));

		// peers are automatically unsubscribed
		assertThat(manager.getSubscribers(roomx).size(), is(0));
	}

	@Test
	public void iceCandidate() throws AdminException {
		joinManyUsersOneRoom();

		final String participantId0 = usersParticipantIds.get(users[0]);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		// verifies listener is added to publisher
		verify(endpoint, times(1)).addOnIceCandidateListener(
				iceEventCaptor.capture());

		for (String pid : usersParticipantIds.values())
			if (!pid.equals(participantId0))
				assertEquals("SDP answer doesn't match", SDP_ANSWER,
						manager.subscribe(users[0], SDP_OFFER, pid));
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		// verifies listener is added to each subscriber
		verify(endpoint, times(usersParticipantIds.size()))
				.addOnIceCandidateListener(iceEventCaptor.capture());

		final IceCandidate ic =
				new IceCandidate("1 candidate test", "audio", 1);

		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(4));

				// first arg : roomName
				assertThat(args[0], instanceOf(String.class));
				assertEquals(roomx, args[0]);

				// second arg : participantId
				assertThat(args[1], instanceOf(String.class));
				String participantId = (String) args[1];
				assertThat(usersParticipantIds.values(), hasItem(participantId));
				// not the publisher cus the captored event
				// is for one of the subscribers
				assertThat(participantId, is(not(participantId0)));

				// third arg : endpointName == publisher's userName
				assertThat(args[2], instanceOf(String.class));
				String epn = (String) args[2];
				assertEquals(users[0], epn);

				// fourth arg : iceCandidate
				assertThat(args[3], instanceOf(IceCandidate.class));
				IceCandidate icParam = (IceCandidate) args[3];
				assertEquals(ic, icParam);

				return null;
			}
		}).when(roomHandler).onIceCandidate(anyString(), anyString(),
				anyString(), Matchers.any(IceCandidate.class));

		// triggers the last captured listener
		iceEventCaptor.getValue().onEvent(
				new OnIceCandidateEvent(endpoint, "12345", null, "candidate",
						ic));

		// verifies the handler's method was called once (we only triggered the
		// event once)
		verify(roomHandler, times(1)).onIceCandidate(anyString(), anyString(),
				anyString(), Matchers.any(IceCandidate.class));
	}

	@Test
	public void mediaError() throws AdminException {
		joinManyUsersOneRoom();

		final String participantId0 = usersParticipantIds.get(users[0]);

		assertEquals("SDP answer doesn't match", SDP_ANSWER,
				manager.publishMedia(participantId0, true, SDP_OFFER, false));

		assertThat(manager.getPublishers(roomx).size(), is(1));

		// verifies error listener is added to publisher
		verify(endpoint, times(1)).addErrorListener(
				mediaErrorEventCaptor.capture());

		final String expectedErrorMessage =
				"TEST_ERR: Fake media error(errCode=101)";

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				// first arg : roomName
				assertThat(args[0], instanceOf(String.class));
				assertEquals(roomx, args[0]);

				// second arg : participantId
				assertThat(args[1], instanceOf(String.class));
				String participantId = (String) args[1];
				assertThat(usersParticipantIds.values(), hasItem(participantId));
				// error on the publisher's endpoint
				assertThat(participantId, is(participantId0));

				// third arg : error description
				assertThat(args[2], instanceOf(String.class));
				assertEquals(expectedErrorMessage, args[2]);

				return null;
			}
		}).when(roomHandler).onMediaElementError(anyString(), anyString(),
				anyString());

		// triggers the last captured listener
		mediaErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(endpoint, "12345", null, "Fake media error",
						101, "TEST_ERR"));

		for (String pid : usersParticipantIds.values())
			if (!pid.equals(participantId0))
				assertEquals("SDP answer doesn't match", SDP_ANSWER,
						manager.subscribe(users[0], SDP_OFFER, pid));
		assertThat(manager.getSubscribers(roomx).size(), is(users.length - 1));

		// verifies listener is added to each subscriber
		verify(endpoint, times(usersParticipantIds.size())).addErrorListener(
				mediaErrorEventCaptor.capture());

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				// first arg : roomName
				assertThat(args[0], instanceOf(String.class));
				assertEquals(roomx, args[0]);

				// second arg : participantId
				assertThat(args[1], instanceOf(String.class));
				String participantId = (String) args[1];
				assertThat(usersParticipantIds.values(), hasItem(participantId));
				// error on a subscriber's endpoint
				assertThat(participantId, is(not(participantId0)));

				// third arg : error description
				assertThat(args[2], instanceOf(String.class));
				assertEquals(expectedErrorMessage, args[2]);

				return null;
			}
		}).when(roomHandler).onMediaElementError(anyString(), anyString(),
				anyString());

		// triggers the last captured listener (once again)
		mediaErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(endpoint, "12345", null, "Fake media error",
						101, "TEST_ERR"));

		// verifies the handler's method was called twice
		verify(roomHandler, times(2)).onMediaElementError(anyString(),
				anyString(), anyString());;

	}

	@Test
	public void pipelineError() throws AdminException {
		joinManyUsersOneRoom();

		// verifies pipeline error listener is added to room
		verify(pipeline, times(1)).addErrorListener(
				pipelineErrorEventCaptor.capture());

		final String expectedErrorMessage =
				"TEST_PP_ERR: Fake pipeline error(errCode=505)";

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args.length, is(3));

				// first arg : roomName
				assertThat(args[0], instanceOf(String.class));
				assertEquals(roomx, args[0]);

				// second arg : participantIds
				assertThat(args[1], instanceOf(Set.class));
				Set<String> pids = new HashSet<String>();
				for (Object o : (Set<?>) args[1]) {
					assertThat(o, instanceOf(String.class));
					pids.add((String) o);
				}
				assertThat(pids, CoreMatchers.hasItems(usersParticipantIds
						.values().toArray(
								new String[usersParticipantIds.size()])));

				// third arg : error description
				assertThat(args[2], instanceOf(String.class));
				assertEquals(expectedErrorMessage, args[2]);

				return null;
			}
		}).when(roomHandler).onPipelineError(anyString(),
				Matchers.<Set<String>>any(), anyString());

		// triggers the last captured listener
		pipelineErrorEventCaptor.getValue().onEvent(
				new ErrorEvent(pipeline, "12345", null, "Fake pipeline error",
						505, "TEST_PP_ERR"));

		// verifies the handler's method was called only once (one captor event)
		verify(roomHandler, times(1)).onPipelineError(anyString(),
				Matchers.<Set<String>>any(), anyString());;
	}

	private Set<UserParticipant> userJoinRoom(final String room, String user,
			String pid, boolean createRoomBefore) throws AdminException {
		if (createRoomBefore)
			manager.createRoom(room);

		Set<UserParticipant> existingPeers = manager.joinRoom(user, room, pid);

		// verifies create media pipeline was called once
		verify(kurentoClient, times(1)).createMediaPipeline(
				kurentoClientCaptor.capture());

		return existingPeers;
	}
}
