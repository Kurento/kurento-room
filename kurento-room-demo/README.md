[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

kurento-room-demo
=================

Kurento Room Demo implements the group communications with WebRTC service 
using services from Kurento Room Server.

The client-side implementation of this demo application is an AngularJS module
that uses the KurentoRoom.js library from the room server package. 
It also integrates the room server's Spring application for the server-side 
of the Room API. This API is made up of the Room SDK and the signaling 
component. For client-server communications the API uses JSON-RPC messages 
over WebSockets.

Installation details
---------------

#### Execute Kurento Room Demo 6.0.x

* Build
```sh
cd kurento-room-demo
mvn clean install -U
```

* Unzip distribution files
```sh
cd target
unzip kurento-room-demo-6.0.0-SNAPSHOT.zip
```

* Execute start script
```sh
cd kurento-room-demo-6.0.0-SNAPSHOT
./bin/start.sh
```

* Configure logging
```sh
vim kurento-room-demo-6.0.0-SNAPSHOT/config/kroomdemo-log4j.properties
```
> Log file by default will be located in kurento-room-demo-6.0.0-SNAPSHOT/logs/

* Configure server
```sh
vim kurento-room-demo-6.0.0-SNAPSHOT/config/kroomdemo.conf.json
```

#### Start Kurento Room Demo 6.0.x as daemon (kroomdemo) in Ubuntu or CentOS

* Build
```sh
cd kurento-room-demo
mvn clean install -U
```

* Unzip distribution files
```sh
cd target
unzip kurento-room-demo-6.0.0-SNAPSHOT.zip
```

* Execute install script
```sh
cd kurento-room-demo-6.0.0-SNAPSHOT
sudo ./bin/install.sh
```
> The service (kroomdemo) will be automatically started.

* Control the service (Ubuntu)
```sh
sudo service kroomdemo {start|stop|status|restart|reload}
```

* Configure logging
```sh
sudo vim /etc/kurento/kroomdemo-log4j.properties
```
> Log file by default will be located in /var/log/kurento/

* Configure server
```sh
sudo vim /etc/kurento/kroomdemo.conf.json
```

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming
based on standards.

This platform has several APIs and components which provide solutions
to the requirements of multimedia content application developers.
These include:

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.


Source
------
The source code of this project can be cloned from the [GitHub repository].
Code for other Kurento projects can be found in the [GitHub Kurento group].


News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].


[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[GitHub repository]: https://github.com/Kurento/kurento-tutorial-java
[GitHub Kurento group]: https://github.com/kurento
[website]: http://kurento.org
