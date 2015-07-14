var kurento;
var room;

window.onload = function() {
	console = new Console('console', console);
}

function register() {

	var userId = document.getElementById('name').value;
	var roomId = document.getElementById('roomName').value;

	var wsUri = 'ws://' + location.host + '/room';

	kurento = KurentoRoom(wsUri, function(error, kurento) {

		if (error)
			return console.log(error);

		room = kurento.Room({
			room : roomId,
			user : userId,
			subscribeToStreams : true
		});

		var localStream = kurento.Stream(room, {
			audio : true,
			video : true,
			data : true
		});

		localStream.addEventListener("access-accepted", function() {

			var playVideo = function(stream) {
				var elementId = "video-" + stream.getGlobalID();
				var div = document.createElement('div');
				div.setAttribute("id", elementId);
				document.getElementById("participants").appendChild(div);
				stream.playThumbnail(elementId);

				// Check color
				var videoTag = document.getElementById("native-" + elementId);
				var userId = stream.getGlobalID();
				var canvas = document.createElement('CANVAS');
				checkColor(videoTag, canvas, userId);
			}

			room.addEventListener("room-connected", function(roomEvent) {

				document.getElementById('room-header').innerText = 'ROOM \"'
						+ room.name + '\"';
				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				localStream.publish();

				var streams = roomEvent.streams;
				for (var i = 0; i < streams.length; i++) {
					playVideo(streams[i]);
				}
			});

			room.addEventListener("stream-added", function(streamEvent) {
				playVideo(streamEvent.stream);
			});

			room.addEventListener("stream-removed", function(streamEvent) {
				var element = document.getElementById("video-"
						+ streamEvent.stream.getGlobalID());
				if (element !== undefined) {
					element.parentNode.removeChild(element);
				}
			});

			playVideo(localStream);

			room.connect();
		});

		localStream.init();

	});
}

function leaveRoom() {

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';

	var streams = room.getStreams();
	for ( var index in streams) {
		var stream = streams[index];
		var element = document.getElementById("video-" + stream.getGlobalID());
		if (element) {
			element.parentNode.removeChild(element);
		}
	}
	kurento.close();
}

window.onbeforeunload = function() {
	kurento.close();
};
