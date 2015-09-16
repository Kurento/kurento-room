%%%%%%%%%%%%%%%%%%
Room demo tutorial
%%%%%%%%%%%%%%%%%%

This tutorial is a guide for developing a multiconference  application using the
Room API SDK. It is based on the development of the ``kurento-room-server``,
``kurento-room-client-js`` and ``kurento-room-demo``.

Server-side code
================

The main class of the room server library project is a Spring Boot application
class, ``KurentoRoomServerApp``. In this class we’ll be instantiating Spring
beans for the different components that make up the server-side.

Furthermore, this class with all its configuration can then be imported into
application classes of other Spring projects (using Spring’s ``@Import``
annotation).

Room management (w/ notifications)
----------------------------------

For managing rooms and their users, the server uses the Room SDK library.  We’ve
chosen the notification-flavored API, namely the class
``NotificationRoomManager``. We have to define the manager as a Spring bean
that will be injected as a dependency when needed (``@Autowired`` annotation).

But first, we need a UserNotificationService implementation to provide to the
``NotificationRoomManager`` constructor, this will be an object of type
``JsonRpcNotificationService`` that will store JSON-RPC sessions in order to
support sending responses and notifications back to the clients. We also
require a ``KurentoClientProvider`` instance that we’ve named ``KMSManager``::

   @Bean
   public NotificationRoomManager roomManager() {
       return new NotificationRoomManager(userNotificationService, kmsManager());
   }

Signaling
---------

For interacting with the clients, our demo application will be using the
JSON-RPC server library developed by Kurento. This library is using for the
transport protocol the WebSockets library provided by the Spring framework.

We register a handler (which extends ``DefaultJsonRpcHandler``) for incoming
JSON-RPC messages so that we can process each request depending on its method
name. This handler implements the WebSocket API described earlier.

The request path is indicated when adding the handler in the method
``registerJsonRpcHandlers(...)``  of the JsonRpcConfigurer API, which is
implemented by the Spring application::

   @Bean
   @ConditionalOnMissingBean
   public RoomJsonRpcHandler roomHandler() {
      return new RoomJsonRpcHandler();
   }

   @Override
   public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
      registry.addHandler(roomHandler(), "/room");
   }

The main method of the handler, ``handleRequest(...)``, will be invoked  for
each incoming request from the clients. All WebSocket communications with a
given client will be done inside a session, for which the JSON-RPC library will
provide a reference when invoking the handling method. A request-response
interchange is called a transaction, also provided and from which we obtain the
WebSocket session.

The application will store the session and transactions associated to each user
so that our ``UserNotificationService`` implementation may send responses or
server events back to the clients when invoked from the Room SDK library::

   @Override
   public final void handleRequest(Transaction transaction, 
   Request<JsonObject> request) throws Exception {
      ...
      notificationService.addTransaction(transaction, request);

      sessionId = transaction.getSession().getSessionId();
      ParticipantRequest participantRequest = new ParticipantRequest(sessionId,
      Integer.toString(request.getId()));

      ...
      transaction.startAsync();
      switch (request.getMethod()) {
        case JsonRpcProtocolElements.JOIN_ROOM_METHOD:
           userControl.joinRoom(transaction, request, participantRequest);
           break;
        ...
        default:
           log.error("Unrecognized request {}", request);
      }
   }

Manage user requests
--------------------

The handler delegates the execution of the user requests to a different
component, an instance of the ``JsonRpcUserControl`` class. This object will
extract the required parameters from the request and will invoke the necessary
code from the ``RoomManager``.

In the case of the ``joinRoom(...)`` request, it will first store the user and
the room names to the session for an easier retrieval later on::

   public void joinRoom(Transaction transaction, Request<JsonObject> request,
		ParticipantRequest participantRequest) throws ... {

      String roomName = getStringParam(request,
          JsonRpcProtocolElements.JOIN_ROOM_ROOM_PARAM);

      String userName = getStringParam(request,
          JsonRpcProtocolElements.JOIN_ROOM_USER_PARAM);

      //store info in session
      ParticipantSession participantSession = getParticipantSession(transaction);
      participantSession.setParticipantName(userName);
      participantSession.setRoomName(roomName);

      roomManager.joinRoom(userName, roomName, participantRequest);

   }

User responses and events
-------------------------

As said earlier, the ``NotificationRoomManager`` instance is created by
providing an implementation for the ``UserNotificationService`` API, which in
this case will be an object of type ``JsonRpcNotificationService``.

This class stores all opened WebSocket sessions in a map from which will obtain
the Transaction object required to send back a response to a room request. For
sending JSON-RPC events (notifications) to the clients it will use the
functionality of the Session object.

Please observe that the notification API (``sendResponse``,
``sendErrorResponse``, ``sendNotification`` and ``closeSession``) had to be
provided for the default implementation of the ``NotificationRoomHandler``
(included with the Room SDK library). Other variations of a room application
could implement their own ``NotificationRoomHandler``, thus rendering
unnecessary the notification service.

In the case of sending a response to a given request, the transaction object
will be used and removed from memory (a different request will mean a new
transaction). Same thing happens when sending an error response::

   @Override
   public void sendResponse(ParticipantRequest participantRequest, Object result) {
      Transaction t = getAndRemoveTransaction(participantRequest);
      if (t == null) {
         log.error("No transaction found for {}, unable to send result {}", 
         participantRequest, result);
         return;
      }
      try {
         t.sendResponse(result);
      } catch (Exception e) {
         log.error("Exception responding to user", e);
      }
   }

To send a notification (or server event), we’ll be using the session object.
This mustn’t be removed until the close session method is invoked (from the
room handler, as a consequence of an user departure, or directly from the
WebSocket handler, in case of connection timeouts or errors)::
 
   @Override
   public void sendNotification(final String participantId,
      final String method, final Object params) {
    
      SessionWrapper sw = sessions.get(participantId);
      if (sw == null || sw.getSession() == null) {
          log.error("No session found for id {}, unable to send notification {}: {}",
             participantId, method, params);
          return;
      }
      Session s = sw.getSession();

      try {
         s.sendNotification(method, params);
      } catch (Exception e) {
         log.error("Exception sending notification to user", e);
      }
   }

Dependencies
------------

Kurento Spring applications are managed using Maven. Our server library has  two
explicit dependencies in its ``pom.xml`` file, Kurento Room SDK and Kurento
JSON-RPC server::

   <dependencies>
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-room-sdk</artifactId>
      </dependency>
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-jsonrpc-server</artifactId>
      </dependency>
   </dependencies>

Demo customization of the server-side
=====================================

The demo adds a bit of customization to the room server by extending and
replacing some of its Spring beans. All this is done in the new Spring Boot
application class of the demo, ``KurentoRoomDemoApp``, that imports the
original application class of the server::

   @Import(KurentoRoomServerApp.class)
   public class KurentoRoomDemoApp {
      ...
      public static void main(String[] args) throws Exception {
         SpringApplication.run(KurentoRoomDemoApp.class, args);
      }
   }

Custom KurentoClientProvider
----------------------------

As substitute for the default implementation of the provider interface we’ve
created the class ``FixedNKmsManager``, which’ll allow maintaining a series of
``KurentoClient``, each created from an URI specified in the demo’s
configuration.

Custom user control
-------------------
To provide support for the additional WebSocket request type, customRequest,  an
extended version of ``JsonRpcUserControl`` was created,
``DemoJsonRpcUserControl``.

This class overrides the method ``customRequest(...)`` to allow toggling the
``FaceOverlayFilter`` which adds or removes the hat from the publisher’s head.
It stores the filter object as an attribute in the WebSocket session so that
it’d be easier to remove it::

    @Override
    public void customRequest(Transaction transaction,
    	Request<JsonObject> request, ParticipantRequest participantRequest) {
      
      try {
         if (request.getParams() == null
           || request.getParams().get(CUSTOM_REQUEST_HAT_PARAM) == null)
           throw new RuntimeException("Request element '" + CUSTOM_REQUEST_HAT_PARAM
               + "' is missing");
            
         boolean hatOn = request.getParams().get(CUSTOM_REQUEST_HAT_PARAM)
            .getAsBoolean();
            
         String pid = participantRequest.getParticipantId();
         if (hatOn) {
             if (transaction.getSession().getAttributes()
                 .containsKey(SESSION_ATTRIBUTE_HAT_FILTER))
                 throw new RuntimeException("Hat filter already on");
             
             log.info("Applying face overlay filter to session {}", pid);
             
             FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(
             roomManager.getPipeline(pid)).build();
             
             faceOverlayFilter.setOverlayedImage(this.hatUrl,
                 this.offsetXPercent, this.offsetYPercent, this.widthPercent,
                 this.heightPercent);
                 
             //add the filter using the RoomManager and store it in the WebSocket session
             roomManager.addMediaElement(pid, faceOverlayFilter);
             transaction.getSession().getAttributes().put(SESSION_ATTRIBUTE_HAT_FILTER,
                 faceOverlayFilter);
                 
         } else {
         
             if (!transaction.getSession().getAttributes()
                    .containsKey(SESSION_ATTRIBUTE_HAT_FILTER))
                 throw new RuntimeException("This user has no hat filter yet");
                
             log.info("Removing face overlay filter from session {}", pid);
            
             //remove the filter from the media server and from the session
             roomManager.removeMediaElement(pid, (MediaElement)transaction.getSession()
                .getAttributes().get(SESSION_ATTRIBUTE_HAT_FILTER));
            
             transaction.getSession().getAttributes()
                .remove(SESSION_ATTRIBUTE_HAT_FILTER);
         }
        
         transaction.sendResponse(new JsonObject());
         
      } catch (Exception e) { 
          log.error("Unable to handle custom request", e);
          try {
              transaction.sendError(e);
          } catch (IOException e1) {
              log.warn("Unable to send error response", e1);
          }
      }
   }
 
Dependencies
------------

There are several dependencies in its pom.xml file, Kurento Room Server, Kurento
Room Client JS (for the client-side library) and a Spring logging library. We
had to manually exclude some transitive dependencies in order to avoid
conflicts::

    <dependencies>
       <dependency>
          <groupId>org.kurento</groupId>
          <artifactId>kurento-room-server</artifactId>
          <exclusions>
             <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
             </exclusion>
             <exclusion>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-logging</artifactId>
             </exclusion>
          </exclusions>
       </dependency>
       <dependency>
          <groupId>org.kurento</groupId>
          <artifactId>kurento-room-client-js</artifactId>
       </dependency>
       <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-log4j</artifactId>
       </dependency>
    </dependencies>


Client-side code
================

This section describes the code from the AngularJS application
contained by kurento-room-demo. The Angular-specific code won’t be explained,
as our goal is to understand the room mechanism (the reader shouldn’t worry as
the indications below will also serve for a client app developed with plain or
conventional Javascript).

Libraries
---------

Include the required Javascript files::

	<script src="./js/jquery-2.1.1.min.js"></script>
	<script src="./js/jquery-ui.min.js"></script>
	<script src="./js/adapter.js"></script>
	<script src="./js/kurento-utils.js"></script>
	<script src="./js/kurento-jsonrpc.js"></script>
	<script src="./js/EventEmitter.js"></script>
	<script src="./js/KurentoRoom.js"></script>

* **jQuery**: is a cross-platform JavaScript library designed to simplify the client-side scripting of HTML.

* **Adapter.js**: is a WebRTC JavaScript utility library maintained by Google that abstracts away browser differences.

* **EventEmitter**: implements an events library for the browser.

* **kurento-jsonrpc**: is a small RPC library that we’ll be using for the
  signaling plane of this application.

* **kurento-utils**: is a Kurento utility library aimed to simplify the WebRTC
  management in the browser.

* **KurentoRoom**: script is the library described earlier which is included
  by the ``kurento-room-client-js`` project.

Init resources
--------------

In order to join a room, call the initialization function from
``KurentoRoom``, providing the server’s URI for listening JSON-RPC requests. In
this case, the room server listens for WebSocket connections on the request
path /room::

   var wsUri = 'ws://' + location.host + '/room';

You must also provide the room and username::

   var kurento = KurentoRoom(wsUri, function (error, kurento) {...}

If the WebSocket initialization failed, the error object will not be null and
we should check the server’s configuration or status.

Otherwise, we’re good to go and we can create a Room and the local Stream
objects.  Please observe that the constraints from the options passed to the
local stream (audio, video, data) are being ignored at the moment::

	room = kurento.Room({
	  room: $scope.roomName,
	  user: $scope.userName
	});
	var localStream = kurento.Stream(room, {
	  audio: true,
	  video: true,
	  data: true
	});

Webcam and mic access
---------------------

The choice of when to join the room is left to the application, and in this one
we must first obtain the access to the webcam and the microphone before calling
the join method. This is done by calling the init method on the local stream::

    localStream.init();

During its execution, the user will be prompted to grant access to the media
resources on her system. Depending on her response, the stream object will emit
the access-accepted or the access-denied event. The application has to register
for these events in order to continue with the join operation::

	localStream.addEventListener("access-denied", function () {
	  //alert of error and go back to login page
	}

Here, when the access is granted, we proceed with the join operation by calling
connect on the room object::

	localStream.addEventListener("access-accepted", function () {
	  //register for room-emitted events
	  room.connect();
	}

Room events
-----------

As a result of the connect call, the room might emit several event types which
the developer should generally be aware of.

If the connection results in a failure, the error-room event is generated::

	room.addEventListener("error-room", function (error) {
	  //alert the user and terminate
	});

In case the connection is successful and the user is accepted as a valid peer in
the room, room-connected event will be used.

The next code excerpts will contain references to the objects ``ServiceRoom``
and ``ServiceParticipant`` which are Angular services defined by the demo
application. And it’s worth mentioning that the ``ServiceParticipant`` uses
streams as room participants::

	room.addEventListener("room-connected", function (roomEvent) {

	  if (displayPublished ) { //demo cofig property
	    //display my video stream from the server (loopback)
	    localStream.subscribeToMyRemote();
	  }
	  localStream.publish(); //publish my local stream

	  //store a reference to the local WebRTC stream
	  ServiceRoom.setLocalStream(localStream.getWebRtcPeer());

	  //iterate over the streams which already exist in the room
	  //and add them as participants
	  var streams = roomEvent.streams;
	  for (var i = 0; i < streams.length; i++) {
	    ServiceParticipant.addParticipant(streams[i]);
	  }
	}

As we’ve just instructed our local stream to be published in the room,  we
should listen for the corresponding event and register our local stream as the
local participant in the room. Furthermore, we’ve added an option to the demo
to display our unchanged local video besides the video that was passed through
the media server (when configured as such)::

	room.addEventListener("stream-published", function (streamEvent) {
	  //register local stream as the local participant
	  ServiceParticipant.addLocalParticipant(localStream);

	  //also display local loopback
	  if (mirrorLocal && localStream.displayMyRemote()) {
	    var localVideo = kurento.Stream(room, {
	      video: true,
	      id: "localStream"
	    });
	    localVideo.mirrorLocalStream(localStream.getWrStream());
	    ServiceParticipant.addLocalMirror(localVideo);
	  }
	});

In case a participant decides to publish her media, we should be aware of  its
stream being added to the room::

	room.addEventListener("stream-added", function (streamEvent) {
	  ServiceParticipant.addParticipant(streamEvent.stream);
	});

The reverse mechanism must be employed when the stream is removed (on unpublish
or on unsubscribe)::

	room.addEventListener("stream-removed", function (streamEvent) {
	  ServiceParticipant.removeParticipantByStream(streamEvent.stream);
	});

Another important event is the one triggered by a media error on the server-side::

	room.addEventListener("error-media", function (msg) {
	  //alert the user and terminate the room connection if deemed necessary
	});

There are other events that are a direct consequence of a notification sent
from the server, such as a room evacuation::

	room.addEventListener("room-closed", function (msg) {
	  //alert the user and terminate
	});

Finally, the client API allows us to send text messages to the other peers  in
the room::

	room.addEventListener("newMessage", function (msg) {
	  ServiceParticipant.showMessage(msg.room, msg.user, msg.message);
	});

Streams interface
-----------------

After having subscribed to a new stream, the application can use one or  both of
these two methods from the stream interface.

**stream.playOnlyVideo(parentElement, thumbnailId)**:

This method will append a ``video`` HTML tag to an existing element specified by
the parentElement parameter (which can be either an identifier or directly the
HTML tag). The video element will have autoplay on and no play controls. If the
stream is local, the video will be muted.

It’s expected that an element with the identifier ``thumbnailId`` to exist and
to be selectable. This element will be displayed (jQuery .show() method) when a
WebRTC stream can be assigned to the src attribute of the video element.

**stream.playThumbnail(thumbnailId)**:

Creates a video element inside the element with identifier ``thumbnailId``. Will
display a name tag onto the video element (text inside a div element), using
the global ID of the stream. The style of the name tag is specified by the CSS
class ``name``.

The size of the thumbnail must be defined by the application. In
the demo, thumbnails start with a width of 14% which will be used until there
are more than 7 publishers in the room (7 x 14% = 98%). From this point on,
another formula will be used for calculating the width, 98% divided by the
number of publishers.
