
kurento_room.service('ServiceRoom', function () {

    console.log("serviceRoom iniciado");

    var kurento;
    var roomName;

    this.getKurento = function () {
        return kurento;
    };

    this.getRoomName = function () {
        return roomName;
    };

    this.setKurento = function (value) {
        kurento = value;
    };

    this.setRoomName = function (value) {
        roomName = value;
    };

});
