/*
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Raquel Díaz González
 */

kurento_room.controller('loginController', function ($scope, $http, ServiceParticipant, $window, ServiceRoom, LxNotificationService) {


    $http.get('/getAllRooms').
            success(function (data, status, headers, config) {
                console.log(JSON.stringify(data));
                $scope.listRooms = data;
            }).
            error(function (data, status, headers, config) {
            });

    $scope.register = function (room) {

        $scope.userName = room.userName;
        $scope.roomName = room.roomName;

        var wsUri = 'ws://' + location.host + '/room';

        //TODO obtain token dynamically from demo server
        var rpcParams = {
        	token : "abc123"
        };
        
        var kurento = KurentoRoom(wsUri, function (error, kurento) {

            if (error)
                return console.log(error);

            kurento.setRpcParams(rpcParams);

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
                	var streams = roomEvent.streams;
                    if (streams.length == 0) //I'm 1st publisher so show my stream from remote
                    	localStream.subscribeToMyRemote();
                	localStream.publish();
                    ServiceRoom.setLocalStream(localStream.getWebRtcPeer());
                    ServiceParticipant.addLocalParticipant(localStream);
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

                room.addEventListener("newMessage", function (msg) {
                    ServiceParticipant.showMessage(msg.room, msg.user, msg.message);
                });

                room.addEventListener("error-room", function (error) {
                    ServiceParticipant.showError($window, LxNotificationService, error);
                });

                room.connect();
            });

            localStream.init();
        });

        //save kurento & roomName & userName in service
        ServiceRoom.setKurento(kurento);
        ServiceRoom.setRoomName($scope.roomName);
        ServiceRoom.setUserName($scope.userName);

        //redirect to call
        $window.location.href = '#/call';
    };
    $scope.clear = function () {
        $scope.room = "";
        $scope.userName = "";
        $scope.roomName = "";
    };
});


