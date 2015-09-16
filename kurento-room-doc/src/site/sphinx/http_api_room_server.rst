%%%%%%%%%%%%%%%%%%%%%%%%
Http API for Room Server
%%%%%%%%%%%%%%%%%%%%%%%%

Server Http REST API
====================

There is one conventional Http REST primitive that can be used to obtain the
available rooms.

1 - Get all rooms
-----------------

Returns a list with all the available rooms’ names.

- **Request method and URL**: ``GET /getAllRooms``
- **Request Content-Type**: NONE
- **Request parameters**: NONE
- **Response elements**: Returns an entity of type application/json including
  a POJO of type Set<String> with the following information:

+---------+----------+---------------------------------+
| Element | Optional | Description                     |
+---------+----------+---------------------------------+
| roomN   | Yes      | Name of the N-th available room |
+---------+----------+---------------------------------+

- **Response Codes**

+--------+-----------------------------+
| Code   | Description                 |
+--------+-----------------------------+
| 200 OK | Query successfully executed |
+--------+-----------------------------+

Demo Http REST API
==================

To demonstrate one of the server methods that RoomManager implements, the demo
application provides an Http REST primitive that can be used to close a given
room directly from the server (and evict the existing participants). It also
specifies a method that allows the clients to read the configuration loopback
parameters.

1 - Close room
--------------

Closes the room

- **Request method and URL**: ``GET /close?room={roomName}``
- **Request Content-Type**: NONE
- **Request parameters**:

+------------+----------+--------------------------------------+
| Element    | Optional | Description                          |
+------------+----------+--------------------------------------+
| {roomName} | No       | Name of the room that will be closed |
+------------+----------+--------------------------------------+

- **Response elements**:

+---------------+---------------------------------------+
| Code          | Description                           |
+---------------+---------------------------------------+
| 200 OK        | Query successfully executed           |
+---------------+---------------------------------------+
| 404 Not found | No room exists with the provided name |
+---------------+---------------------------------------+

2 - Get client configuration
----------------------------

Returns a ClientConfig POJO that can be used to configure the source for the own
video (only local, remote or both).

- **Request method and URL**: ``GET /getClientConfig``
- **Request Content-Type**: NONE
- **Request parameters**: NONE
- **Response elements**: Returns an entity of type application/json including
  a POJO of type ClientConfig with the following information:

+------------------+----------+---------------------------------------------------------------------+
| Element          | Optional | Description                                                         |
+------------------+----------+---------------------------------------------------------------------+
| loopbackRemote   | Yes      | If true, display the local video from the server loopback           |
+------------------+----------+---------------------------------------------------------------------+
| loopbackAndLocal | Yes      | If the other parameter is true, enables the original source as well |
+------------------+----------+---------------------------------------------------------------------+

- **Response Codes**:

+--------+-----------------------------+
| Code   | Description                 |
+--------+-----------------------------+
| 200 OK | Query successfully executed |
+--------+-----------------------------+


