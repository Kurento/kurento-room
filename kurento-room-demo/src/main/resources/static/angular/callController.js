
kurento_room.controller('callController', function ($scope, $window, ServiceParticipant, ServiceRoom, Fullscreen) {
    console.log("callController iniciado");
    $scope.roomName = ServiceRoom.getRoomName();
    $scope.participants = ServiceParticipant.getParticipants();
    console.log("SERVICE ROOM " + JSON.stringify(ServiceRoom));
    console.log("LOCAL STREAM " + JSON.stringify(ServiceRoom.getKurento()));
    console.log("KURENTO " + JSON.stringify(ServiceRoom.getKurento()));
    console.log("ROOM NAME " + JSON.stringify(ServiceRoom.getRoomName()));

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
//        var kurento = ServiceRoom.getKurento();
        var localStream = ServiceRoom.getLocalStream();
//        var stream = ServiceRoom.getKurento().getStream();
//        var stream2 = ServiceRoom.getKurento().getStream().getWebRTcPeer();
        var element = document.getElementById("buttonVolume");
        if (element.classList.contains("md-volume-off")) { //on
            element.classList.remove("md-volume-off");
            element.classList.add("md-volume-up");
            localStream.audioEnabled = true;
        } else { //off
            element.classList.remove("md-volume-up");
            element.classList.add("md-volume-off");
            localStream.audioEnabled = false;

        }
    };

    $scope.onOffVideocam = function () {
        var localStream = ServiceRoom.getLocalStream();
        var element = document.getElementById("buttonVideocam");
        if (element.classList.contains("md-videocam-off")) {//on
            element.classList.remove("md-videocam-off");
            element.classList.add("md-videocam");
            localStream.videoEnabled = true;
        } else {//off
            element.classList.remove("md-videocam");
            element.classList.add("md-videocam-off");
            localStream.videoEnabled = false;
        }
    };
});


