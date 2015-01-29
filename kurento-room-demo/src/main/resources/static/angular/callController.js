
kurento_room.controller('callController', function ($scope, $window, ServiceParticipant, ServiceRoom, Fullscreen) {
    console.log("callController iniciado");
    $scope.roomName = ServiceRoom.getRoomName();
    $scope.participants = ServiceParticipant.getParticipants();

    $scope.leaveRoom = function () {

        ServiceRoom.getKurento().close();

        ServiceParticipant.removeParticipants();

        //redirect to login
        $window.location.href = '#/login';
    };

    window.onbeforeunload = function () {
        ServiceRoom.getKurento().close();
    };

// FullScreen -----------------------------------------------------------------

    $scope.goFullscreen = function () {

        if (Fullscreen.isEnabled())
            Fullscreen.cancel();
        else
            Fullscreen.all();

    };

    $scope.onOffVolume = function () {
        var element = document.getElementById("buttonVolume");
        if (element.classList.contains("md-volume-off")) {
            element.classList.remove("md-volume-off");
            element.classList.add("md-volume-up");
        } else {
            element.classList.remove("md-volume-up");
            element.classList.add("md-volume-off");
        }
    };

    $scope.onOffVideocam = function () {
        var element = document.getElementById("buttonVideocam");
        if (element.classList.contains("md-videocam-off")) {
            element.classList.remove("md-videocam-off");
            element.classList.add("md-videocam");
        } else {
            element.classList.remove("md-videocam");
            element.classList.add("md-videocam-off");
        }
    };
});


