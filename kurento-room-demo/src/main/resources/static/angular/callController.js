
kurento_room.controller('callController', function ($scope, ServiceParticipant, Fullscreen) {
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

    $scope.goFullscreen = function () {

        if (Fullscreen.isEnabled())
            Fullscreen.cancel();
        else
            Fullscreen.all();

    };


});


