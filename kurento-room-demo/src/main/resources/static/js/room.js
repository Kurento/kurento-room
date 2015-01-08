var kurento;
var participants = new Participants();

function register() {

	var userName = document.getElementById('name').value;
	var roomName = document.getElementById('roomName').value;

	var wsUri = 'ws://' + location.host + '/room';

	kurento = KurentoRoom(wsUri, function(error, kurento) {

		if (error)
			return console.log(error);

		var localStream = kurento.Stream({
			audio : true,
			video : true,
			data : true,
			name : userName
		});

		room = kurento.Room({
			name : roomName,
			userName : userName
		});

		localStream.addEventListener("access-accepted", function() {

			var subscribeToStreams = function(streams) {
				for ( var index in streams) {
					var stream = streams[index];
					if (localStream.getID() !== stream.getID()) {
						room.subscribe(stream);
					}
				}
			};

			room.addEventListener("room-connected", function(roomEvent) {
				room.publish(localStream);
				subscribeToStreams(roomEvent.streams);

				document.getElementById('room-name').innerText = room.name;

				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				participants.addLocalParticipant(localStream);
			});

			room.addEventListener("stream-subscribed", function(streamEvent) {
				participants.addParticipant(streamEvent.stream);
			});

			room.addEventListener("stream-added", function(streamEvent) {
				var streams = [];
				streams.push(streamEvent.stream);
				subscribeToStreams(streams);
			});

			room.addEventListener("stream-removed", function(streamEvent) {
				participants.removeParticipant(streamEvent.stream);
			});

			room.connect();
		});

		localStream.init();

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
