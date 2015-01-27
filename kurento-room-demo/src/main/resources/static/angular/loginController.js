kurento_room.controller('loginController', function ($scope, ServiceParticipant, $window, ServiceRoom) {

    console.log("controller1 iniciado");

    $scope.register = function (room) {

        console.log("dentro de register");
        $scope.userName = room.userName;
        $scope.roomName = room.roomName;

        var wsUri = 'ws://' + location.host + '/room';

        var kurento = KurentoRoom(wsUri, function (error, kurento) {

            if (error)
                return console.log(error);

            room = kurento.Room({
                room: $scope.roomName,
                user: $scope.userName
            });

            var localStream = kurento.Stream(room, {
                audio: true,
                video: true,
                data: true
            });

            localStream.addEventListener("access-accepted", function () {
                room.addEventListener("room-connected", function (roomEvent) {
//		    document.getElementById('room-name').innerText = room.name;

                    localStream.publish();
                    ServiceParticipant.addLocalParticipant(localStream);
                    var streams = roomEvent.streams;
                    for (var i = 0; i < streams.length; i++) {
                        ServiceParticipant.addParticipant(streams[i]);
                    }
                });

                room.addEventListener("stream-added", function (streamEvent) {
                    ServiceParticipant.addParticipant(streamEvent.stream);
                });

                room.addEventListener("stream-removed", function (streamEvent) {
                    ServiceParticipant.removeParticipant(streamEvent.stream);
                });

                room.connect();
            });

            localStream.init();

        });

        //save kurento & roomName in service
        ServiceRoom.setKurento(kurento);//SE GUARDA UNDEFINDED
        console.log("KURENTO"+JSON.stringify(kurento));
        ServiceRoom.setRoomName($scope.roomName);
        //redirect to call
        $window.location.href = '#/call';
    };

});


