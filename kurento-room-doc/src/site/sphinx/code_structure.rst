%%%%%%%%%%%%%%
Code structure
%%%%%%%%%%%%%%

Project modules:

- **kurento-room** - reactor project
- **kurento-room/kurento-room-sdk** - module that provides an interface for
  Java developers of room-type applications
- **kurento-room/kurento-room-server** - Kurento’s own implementation of a
  room API, it provides the WebSockets API for the communications between room
  clients and the server.
- **kurento-room/kurento-room-client-js** - Javascript library that acts as
  wrapper for several JS APIs (WebRTC, WebSockets, Kurento Utils). Can be used
  to implement the client-side of a room application.
- **kurento-room/kurento-room-demo** - demonstration project, contains the
  client-side implementation (HTML, Javascript, graphic resources) and embeds
  the room server to provide the functionality required for group
  communications (the so-called rooms)
- **kurento-room/kurento-room-test** - includes integration tests for the room
  server application and the demo.
- **kurento-room/kurento-room-demo-test** - includes integration tests for the
  demo application.

Server library
==============

The server project has been designed to be used as a dependency by applications
willing to provide the room functionality to their users. The server-side code
employs the Room SDK for managing the rooms and uses a Kurento library
(**kurento-jsonrpc-server**, a JSON-RPC protocol over WebSockets) to connect
with the clients. The API provided by the server has a limitation concerning an
user’s name, and is that it cannot contain lower dashes.


JavaScript client library
=========================
The JS client module exposes the library ``KurentoRoom.js`` which is build upon
other public APIs like Kurento Utils JS, Kurento JSON-RPC Client JS,
EventEmitter, etc. This module can be added as a Maven dependency to projects
implementing the client-side code for web browsers that support WebRTC.

Demo application
================
This project, named kurento-room-demo, contains the client-side implementation
(HTML, Javascript, graphic resources) of the Rooms API and embeds the room
server to provide the functionality required for group communications (the
so-called rooms). Upon launch and using the SpringBoot framework, it starts the
main application of the module kurento-room-server which exposes the rooms API
through an WebSocket interface. The client part has been implemented using
Angular JS and lumX and it’s using the room’s  JS library for the client-side
(``KurentoRoom.js``).

The demo allows web clients accessing the application’s URI to:

- login inside a room (creating the room if it doesn’t exist)
- leave the room
- publish their media stream
- mute their video and/or audio
- enter fullscreen
- automatically subscribe to any stream published in the room and play it on
  the screen (video) and through the system speakers (audio) unsubscribe from a
  stream unpublish their media send messages to the other peers apply or remove
  a media filter over their published video stream (using a face overlay filter
  that adds a hat on top of a recognized human face) select which video source
  to display in the larger area of the browser from the available thumbnails