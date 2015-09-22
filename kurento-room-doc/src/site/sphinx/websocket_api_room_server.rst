%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
WebSocket API for Room Server
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

The Kurento room server exposes a Websocket at http://localhost:8080/room, where
the hostname and port depend on the current setup. The WebSocket enables the
Kurento room app to instantly push events to the clients, as soon as they
happen.

The exchanged messages between server and clients are
`JSON-RPC 2.0 <http://www.jsonrpc.org/specification>`_ requests and responses.
The events are sent from the server in the same way as a server's request, but
without requiring a response and they don't include an identifier.

WebSocket messages
==================

1 - Join room
-------------

Represents a client's request to join a room. If the room
does not exist, it is created. To obtain the available rooms, the client should
previously use the REST method getAllRooms.

- **Method**: joinRoom

- **Parameters**:

  - **user** - user's name
  - **room** -  room's name

- **Example request**::

    {"jsonrpc":"2.0","method":"joinRoom",
     "params":{"user":"USER1","room":"ROOM_1"},"id":0}

- **Server response (result)**:

   - **sessionId** - id of the WebSocket session between the browser and
     the server
   - **value** - list of existing users in this room, empty when the room
     is a fresh one:

     - **id** - an already existing user's name
     - **streams** - list of stream identifiers that the other
       participant has opened to connect with the room. As only webcam is
       supported, will always be ``[{"id":"webcam"}]``.

- **Example response**::


      {"id":0,"result":{"value":[{"id":"USER0","streams":[{"id":"webcam"}]}],
      "sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

2 - Participant joined event
----------------------------

Event sent by server to all other participants in the room as a result of a new
user joining in.

- **Method**: participantJoined

- **Parameters**:

  - **id:** the new participant's id (username)

- **Example message**::

	 {"jsonrpc":"2.0","method":"participantJoined","params":{"id":"USER1"}}

3 - Publish video
-----------------

Represents a client's request to start streaming her local media to anyone
inside  the room. The user can use the SDP answer from the response to display
her local media after having passed through the KMS server (as opposed or
besides using just the local stream), and thus check what other users in the
room are receiving from her stream. The loopback can be enabled using the
corresponding parameter.

- **Method**: publishVideo

- **Parameters**:

  - **sdpOffer**: SDP offer sent by this client
  - **doLoopback**: boolean enabling media loopback

- **Example request**::

	{"jsonrpc":"2.0","method":"publishVideo","params":{"sdpOffer":
        "v=0....apt=100\r\n"},"doLoopback":false,"id":1}

- **Server response (result)**

  - **sessionId:** id of the WebSocket session
  - **sdpAnswer:** SDP answer build by the the user's server WebRTC endpoint

- **Example response**::

   {"id":1,"result":{"sdpAnswer":"v=0....apt=100\r\n",
   "sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

4 - Participant published event
-------------------------------

Event sent by server to all other participants in the room as a result of a user
publishing her local media stream.

- **Method**: participantPublished

- **Parameters**:

  - **id**: publisher's username
  - **streams**: list of stream identifiers that the participant has opened
    to connect with the room. As only webcam is supported, will always be
    ``[{"id":"webcam"}]``.

- **Example message**::

        {"jsonrpc":"2.0","method":"participantPublished",
        "params":{"id":"USER1","streams":[{"id":"webcam"}]}}

5 - Unpublish video
-------------------

Represents a client's request to stop streaming her local media to her room peers.

- **Method**: unpublishVideo

- **Parameters**: No parameters required

- **Example request**::

	{"jsonrpc":"2.0","method":"unpublishVideo","id":38}

- **Server response (result)**

  - **sessionId**: id of the WebSocket session

- **Example response**::

    {"id":1,"result":{"sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

6 - Participant unpublished event
---------------------------------

Event sent by server to all other participants in the room as a result of a user
having stopped publishing her local media stream.

- **Method**: participantUnpublished

- **Parameters**:

  - **name** - publisher's username

- **Example message**

     {"method":"participantUnpublished","params":{"name":"USER1"}, "jsonrpc":"2.0"}

7 - Receive video
-----------------

Represents a client's request to receive media from participants in the room
that  published their media. This method can also be used for loopback
connections.

- **Method**: receiveVideoFrom

- **Parameters**:

   - **sender**: id of the publisher's endpoint, build by appending the
     publisher's  name and her currently opened stream (usually webcam)
   - **sdpOffer**: SDP offer sent by this client

- **Example request**::

         {"jsonrpc":"2.0","method":"receiveVideoFrom","params":{"sender":
         "USER0_webcam","sdpOffer":"v=0....apt=100\r\n"},"id":2}

- **Server response (result)**

   - **sessionId**: id of the WebSocket session
   - **sdpAnswer**: SDP answer build by the other participant's WebRTC
     endpoint

- **Example response**

    {"id":2,"result":{"sdpAnswer":"v=0....apt=100\r\n", "sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

8 - Unsubscribe from video
--------------------------

Represents a client's request to stop receiving media from a given publisher.

- **Method**: unsubscribeFromVideo

- **Parameters**:

   - **sender**: id of the publisher's endpoint, build by appending the
     publisher's name and her currently opened stream (usually webcam)

- **Example request**::

     {"jsonrpc":"2.0","method":"unsubscribeFromVideo","params":{"sender":
     "USER0_webcam"},"id":67}

- **Server response (result)**

    "sessionId" - id of the WebSocket session

- **Example response**::

    {"id":2,"result":{"sdpAnswer":"v=0....apt=100\r\n",
     "sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

9 - Send ICE Candidate
----------------------

Request that carries info about an ICE candidate gathered on the client  side.
This information is required to implement the trickle ICE mechanism. Should be
sent whenever an icecandidate event is created by a RTCPeerConnection.

- **Method**: onIceCandidate

- **Parameters**:

   - **endpointName**: the name of the peer whose ICE candidate was found
   - **candidate**: the candidate attribute information
   - **sdpMLineIndex**: the index (starting at zero) of the m-line in the
     SDP  this candidate is associated with
   - **sdpMid**: media stream identification, "audio" or "video", for the
     m-line this candidate is associated with

- **Example request**::

     {"jsonrpc":"2.0","method":"onIceCandidate","params":
         {"endpointName":"USER1","candidate":
             "candidate:2023387037 1 udp 2122260223 127.0.16.1 48156 typ host generation 0",
             "sdpMid":"audio",
             "sdpMLineIndex":0
         },"id":3}

- **Server response (result)**:

   - **sessionId**: id of the WebSocket session

- **Example response**::

    {"id":3,"result":{"sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}
    
10 - Receive ICE Candidate event
--------------------------------

Server event that carries info about an ICE candidate gathered on the server
side. This information is required to implement the trickle ICE mechanism. Will
be received by the client whenever a new candidate is gathered for the local
peer on the server.

- **Method**: iceCandidate

- **Parameters**:

   - **endpointName**: the name of the peer whose ICE candidate was found
   - **candidate**: the candidate attribute information
   - **sdpMLineIndex**: the index (starting at zero) of the m-line in the
     SDP  this candidate is associated with
   - **sdpMid**: media stream identification, "audio" or "video", for the
     m-line  this candidate is associated with

- **Example message**::

    {"method":"iceCandidate","params":{"endpointName":"USER1",
    "sdpMLineIndex":1,"sdpMid":"video","candidate":
    "candidate:2 1 UDP 1677721855 127.0.1.1 58322 typ srflx 
    raddr 172.16.181.129 rport 59597"},"jsonrpc":"2.0"}

11 - Leave room
---------------

Represents a client's notification that she's leaving the room.

- **Method**: leaveRoom

- **Parameters**: NONE

- **Example request**::

	{"jsonrpc":"2.0","method":"leaveRoom","id":4}

- **Server response (result)**:

    - **sessionId**: id of the WebSocket session

- **Example response**::

    {"id":4,"result":{"sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

12 - Participant left event
---------------------------

Event sent by server to all other participants in the room as a consequence of
an user leaving the room.

- **Method**: participantLeft

- **Parameters**:

    - **name**: username of the participant that has disconnected

- **Example message**::
 
    {"jsonrpc":"2.0","method":"participantLeft","params":{"name":"USER1"}}

13 - Participant evicted event
------------------------------

Event sent by server to a participant in the room as a consequence of a
server-side action requiring the participant to leave the room.

- **Method**: participantEvicted

- **Parameters**: NONE

- **Example message**::

    {"jsonrpc":"2.0","method":"participantLeft","params":{}}

14 - Send message
-----------------

Used by clients to send written messages to all other participants in the room.

- **Method**: sendMessage

- **Parameters**:

    - **message**: the text message
    - **userMessage**: message originator (username)
    - **roomMessage**: room identifier (room name)

- **Example request**::

     {"jsonrpc":"2.0","method":"sendMessage","params":{"message":"My message",
     "userMessage":"USER1","roomMessage":"ROOM_1"},"id":5}

- **Server response (result)**:

   - **sessionId**: id of the WebSocket session

- **Example response**::

    {"id":5,"result":{"sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

15 - Message sent event
-----------------------

Broadcast event that propagates a written message to all room participants.

- **Method**: sendMessage

- **Parameters**:

    - **room**: current room name
    - **name**: username of the text message source
    - **message**: the text message

- **Example message**::

    {"method":"sendMessage","params":{"room":"ROOM_1","user":"USER1",
    "message":"My message"},"jsonrpc":"2.0"}

16 - Media error event
----------------------

Event sent by server to all participants affected by an error event intercepted
on a pipeline or media element.

- **Method**: mediaError

- **Parameters**:

   - **error**: description of the error

- **Example message**::

    {"method":"mediaError","params":{
    "error":"ERR_CODE: Pipeline generic error"},"jsonrpc":"2.0"}

17 - Custom request
-------------------

Provides a custom envelope for requests not directly implemented by the Room
server. The default server implementation of handling this call is to throw a
RuntimeException. There is one implementation of this request, and it's used by
the demo application to toggle the hat filter overlay.

- **Method**: customRequest

- **Parameters**: Parameters specification is left to the actual implementation

- **Example request**::

	{"jsonrpc":"2.0","method":"customRequest","params":{...},"id":6}

- **Server response (result)**:

   - **sessionId**: id of the WebSocket session
   - other result parameters are not specified (left to the implementation)

- **Example response**::

    {"id":6,"result":{"sessionId":"dv41ks9hj761rndhcc8nd8cj8q"},"jsonrpc":"2.0"}

