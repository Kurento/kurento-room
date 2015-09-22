%%%%%%%%%%%%%%%%
Cloud deployment
%%%%%%%%%%%%%%%%

How to install, configure and deploy to cloud the Rooms app and how to publish
the API. On a cloud instance which meets the following requirements, one can
install Kurento Rooms applications as a system service (e.g.
``kurento-room-demo``). There's also the possibility to run the demo manually
using the provided script.

System requirements:

- Ubuntu 14.04
- JDK 7 or 8
- Kurento Media Server or connection with at least a running instance (to
  install follow the official
  `guide <http://www.kurento.org/docs/current/installation_guide.html>`_)

Kurento room demo installer
===========================

To build the installation binaries from the source code, follow the instructions
from the README file that can be found in the project ``kurento-room-demo``.

Running the application
=======================
After having built and unzipped the installation files, there are two options
for running the demo application server:

- **user-level execution** - doesn't need additional installation steps, can
  be done right away after uncompressing the installer
- **system-level execution** - requires installation of the demo application
  as a system service, which enables automatic startup after system reboots

In both cases, the application uses Spring Boot framework to run inside an
embedded Tomcat container server, so there's no need for deployment inside an
existing servlet container. If this is a requirement, modifications will have
to be made to the project's build configuration (Maven) so that instead of a
JAR with dependencies, the build process would generate a WAR file.

To update to a newer version, it's suffices to follow once again the
installation procedures.

Configuration file
==================

The file kroomdemo.conf.json contains the configuration of the demo application::

    {
       "kms": {
          "uris": ["ws://localhost:8888/kurento","ws://127.0.0.1:8888/kurento"]
       },
       "app": {
          "uri": "http://localhost:8080/"
       },
       "kurento": {
          "client": {
             //milliseconds
             "requestTimeout": 20000
          }
       },
       "demo": {
          //mario-wings.png or wizard.png
          "hatUrl": "mario-wings.png"
          "hatCoords": {
             // mario-wings hat
             "offsetXPercent": -0.35F,
             "offsetYPercent": -1.2F,
             "widthPercent": 1.6F,
             "heightPercent": 1.6F

             //wizard hat
             //"offsetXPercent": -0.2F,
             //"offsetYPercent": -1.35F,
             //"widthPercent": 1.5F,
             //"heightPercent": 1.5F
          },
          "loopback" : {
             "remote": false,
             //matters only when remote is true
             "andLocal": false
          },
          "authRegex": ".*",
          "kmsLimit": 10
       }
    }

With the following key meanings:

- **kms.uris** is an array of WebSocket addresses used to initialize
  KurentoClient instances (each instance represents a Kurento Media Server). In
  the default configuration, for the same KMS the application will create two
  KurentoClient objects. The KurentoClientProvider implementation for this demo
  (org.kurento.room.demo.FixedNKmsManager) will return KurentoClient instances
  on a round-robin base or, if the user's name follows a certain pattern, will
  return the less loaded instance. The pattern check is hardcoded and SLA users
  are considered those whose name starts with the string special (e.g.
  specialUser).

- **app.uri** is the demo application's URL and is mainly used for building
  URLs of images used in media filters (such as the hat filter). This URL must
  be accessible from any KMS defined in kms.uris.

- **kurento.client.requestTimeout** is a tweak to prevent timeouts in the KMS
  communications during heavy load (e.g. lots of peers). The default value of
  the timeout is 10 seconds. demo configuration:

  - **hatUrl** sets the image used for the FaceOverlayFilter applied to the
    streamed  media when the user presses the corresponding button in the demo
    interface. The filename of the image is relative to the static web
    resources folder img/.
  - **hatCoords** represents the JSON encoding of the parameters required to
    configure the overlaid image. We provide the coordinates for two hat
    images, mario-wings.png and wizard.png.
  - **loopback.remote** if true, the users will see their own video using
    the loopbacked stream from the server. Thus, if the user enables the hat
    filter on her video stream, she'll be able to visualize the end result
    after having applied the filter.
  - **loopback.andLocal** if true, besides displaying the loopback media,
    the client interface will also provide the original (and local) media stream
  - **authRegex** is the username pattern that allows the creation of a room
    only when the requester's name matches the pattern. This is done during the
    call to obtain an instance of KurentoClient, the provider will throw an
    exception if the pattern has been specified and it doesn't match the name.
  - **kmsLimit** is the maximum number of pipelines that can be created in a
    KurentoClient.
