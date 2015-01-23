
kurento_room.controller('callController', function ($scope, ServiceParticipant, $window) {
console.log("callController iniciado");
//    $scope.roomName = ServiceParticipant.getRoom();
    $scope.roomName = "Room Name";

    $scope.leaveRoom = function () {

        kurento.leaveRoom();

        ServiceParticipant.removeParticipants();

        //redirect to login
        $window.location.href = '#/login';
    };

    window.onbeforeunload = function () {
        kurento.close();
    };

// FullScreen -----------------------------------------------------------------

    $scope.toggleFullScreen = function () {
        
      var videoElement = document.getElementById("room");
//        var videoElement = ServiceParticipant.getRoomName();

        try {
            if (!document.mozFullScreen && !document.webkitFullScreen) {
                if (videoElement.mozRequestFullScreen) {
                    videoElement.mozRequestFullScreen();
                } else {
                    videoElement
                            .webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
                }
            } else {
                if (document.mozCancelFullScreen) {
                    document.mozCancelFullScreen();
                } else {
                    document.webkitCancelFullScreen();
                }
            }
        } catch (e) {
            console.error(e);
        }
    };



});


