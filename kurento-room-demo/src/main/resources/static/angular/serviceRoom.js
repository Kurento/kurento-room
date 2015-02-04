
kurento_room.service('ServiceRoom', function () {

    console.log("serviceRoom iniciado");

    var kurento;
    var roomName;
    var localStream;

    this.getKurento = function () {
        return kurento;
    };

    this.getRoomName = function () {
        return roomName;
    };

    this.getLocalStream = function () {
        return localStream;
    };

    this.setKurento = function (value) {
        kurento = value;
    };

    this.setRoomName = function (value) {
        roomName = value;
    };

    this.setLocalStream = function (value) {
        localStream = value;
    };
});
