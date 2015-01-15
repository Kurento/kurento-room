var kurento;
var participants = new Participants();

function register() {

	var userName = document.getElementById('name').value;
	var roomName = document.getElementById('roomName').value;

	var wsUri = 'ws://' + location.host + '/room';

	kurento = KurentoRoom(wsUri, function(error, kurento) {

		if (error)
			return console.log(error);

		room = kurento.Room({
			room : roomName,
			user : userName
		});

		var localStream = kurento.Stream(room,{
			audio : true,
			video : true,
			data : true
		});

		localStream.addEventListener("access-accepted", function() {

			var subscribeToStreams = function(streams) {
				for (var i=0; i<streams.length; i++) {
					var stream = streams[i];
					if (localStream.getGlobalID() !== stream.getGlobalID()) {
						room.subscribe(stream);
					}
				}
			};

			room.addEventListener("room-connected", function(roomEvent) {

				document.getElementById('room-name').innerText = room.name;
				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				localStream.publish();
				participants.addLocalParticipant(localStream);

				var streams = roomEvent.streams;
				subscribeToStreams(streams);

				for(var i=0; i<streams.length; i++){
					participants.addParticipant(streams[i]);
				}
			});

			room.addEventListener("stream-subscribed", function(streamEvent) {
				// We don't do anything because video element is created when
				// stream-added event is received
			});

			room.addEventListener("stream-added", function(streamEvent) {
				var streams = [];
				streams.push(streamEvent.stream);
				subscribeToStreams(streams);

				participants.addParticipant(streamEvent.stream);
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
