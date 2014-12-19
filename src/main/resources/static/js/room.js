var kurento;
var participants = new Participants();

function register() {

	var userName = document.getElementById('name').value;
	var roomName = document.getElementById('roomName').value;

	var wsUri = 'ws://' + location.host + '/room';

	kurento = KurentoBasicRoom(wsUri, function(error, kurento) {

		if (error)
			return console.log(error);

		kurento.addEventListener("room-connected", function(event) {

			document.getElementById('room-name').innerText = room.name;

			document.getElementById('join').style.display = 'none';
			document.getElementById('room').style.display = 'block';

			participants.addLocalParticipant(event.localStream);
		});

		kurento.addEventListener("stream-added", function(streamEvent) {

			participants.addParticipant(streamEvent.stream);
		});

		kurento.addEventListener("stream-removed", function(streamEvent) {

			participants.removeParticipant(streamEvent.stream);
		});

		kurento.joinRoom({
			userName : userName,
			roomName : roomName,
		});
	});
}

function leaveRoom() {

	kurento.leaveRoom();

	participants.removeParticipants();

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';
}

window.onbeforeunload = function() {
	kurento.close();
};

// FullScreen -----------------------------------------------------------------

function toggleFullScreen() {

	var videoElement = document.getElementById("room");

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
}
