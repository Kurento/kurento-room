/*
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Raquel Díaz González
 */

kurento_room.controller('callController', function ($scope, $window, ServiceParticipant, ServiceRoom, Fullscreen, LxNotificationService) {

    $scope.roomName = ServiceRoom.getRoomName();
    $scope.userName = ServiceRoom.getUserName();
    $scope.participants = ServiceParticipant.getParticipants();
    $scope.kurento = ServiceRoom.getKurento();
    $scope.filter = ServiceRoom.getFilterRequestParam();

    $scope.leaveRoom = function () {

        ServiceRoom.closeKurento();

        ServiceParticipant.removeParticipants();

        //redirect to login
        $window.location.href = '#/login';
    };

    window.onbeforeunload = function () {
    	//not necessary if not connected
    	if (ServiceParticipant.isConnected()) {
            ServiceRoom.closeKurento();
    	}
    };

    $scope.$on("$locationChangeStart",function () {
       console.log("Changed location to: " + document.location);
       if (ServiceParticipant.isConnected()) {
           ServiceRoom.closeKurento();
           ServiceParticipant.removeParticipants();
       }
    });

    $scope.goFullscreen = function () {

        if (Fullscreen.isEnabled())
            Fullscreen.cancel();
        else
            Fullscreen.all();

    };
    
    $scope.disableMainSpeaker = function (value) {

    	var element = document.getElementById("buttonMainSpeaker");
        if (element.classList.contains("md-person")) { //on
            element.classList.remove("md-person");
            element.classList.add("md-recent-actors");
            ServiceParticipant.enableMainSpeaker();
        } else { //off
            element.classList.remove("md-recent-actors");
            element.classList.add("md-person");
            ServiceParticipant.disableMainSpeaker();
        }
    }

    $scope.onOffVolume = function () {
        var localStream = ServiceRoom.getLocalStream();
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

    $scope.disconnectStream = function() {
    	var localStream = ServiceRoom.getLocalStream();
    	var participant = ServiceParticipant.getMainParticipant();
    	if (!localStream || !participant) {
    		LxNotificationService.alert('Error!', "Not connected yet", 'Ok', function(answer) {
            });
    		return false;
    	}
    	ServiceParticipant.disconnectParticipant(participant);
    	ServiceRoom.getKurento().disconnectParticipant(participant.getStream());
    }
    
    //chat
    $scope.message;

    $scope.sendMessage = function () {
        console.log("Sending message", $scope.message);
        var kurento = ServiceRoom.getKurento();
        kurento.sendMessage($scope.roomName, $scope.userName, $scope.message);
        $scope.message = "";
    };

    //open or close chat when click in chat button
    $scope.toggleChat = function () {
        var selectedEffect = "slide";
        // most effect types need no options passed by default
        var options = {direction: "right"};
        if ($("#effect").is(':visible')) {
            $("#content").animate({width: '100%'}, 500);
        } else {
            $("#content").animate({width: '80%'}, 500);
        }
        // run the effect
        $("#effect").toggle(selectedEffect, options, 500);
    };
    
    var style = {
        hat: {
            off: "btn--deep-purple md-mood",
            on: "btn--purple md-face-unlock"
        },
        marker: {
            off: "btn--deep-purple md-grid-off",
            on: "btn--purple md-grid-on"
        }
    };

    $scope.filterIsOn = false;
    $scope.filterState;
    $scope.filterStyle;
    updateFilterValues();

    function updateFilterValues() {
        $scope.filterState = $scope.filterIsOn ? "on" : "off";
        $scope.filterStyle = style[$scope.filter][$scope.filterState];
    }

    $scope.applyFilter = function () {
        $scope.filterIsOn = !$scope.filterIsOn;
        updateFilterValues();
        console.log("Toggle filter " + $scope.filterState);

        var reqParams = {};
        reqParams[$scope.filter] = $scope.filterIsOn;

        ServiceRoom.getKurento().sendCustomRequest(reqParams, function (error, response) {
            if (error) {
                console.error("Unable to toggle filter " + $scope.filterState, error);
                LxNotificationService.alert('Error!',
                    "Unable to toggle filter " + $scope.filterState, 'Ok',
                    function(answer) {});
                return false;
            } else {
                console.log("Response to filter toggle", response);
            }
        });

    };
});


