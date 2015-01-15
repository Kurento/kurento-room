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

		var localStream = kurento.Stream({
			audio : true,
			video : true,
			data : true,
			id: userId
		});

		room = kurento.Room({
			room : roomId,
			user : userId
		});

		localStream.addEventListener("access-accepted", function() {

			var subscribeToStreams = function(streams) {
				for (var key in streams) {
					var stream = streams[key];
					if (localStream.getID() !== stream.getID()) {
						room.subscribe(stream);
					}
				}
			};

			var playVideo = function(stream){
				var elementId = "video-" + stream.getGlobalID();
				var div = document.createElement('div');
				div.setAttribute("id", elementId);
				document.getElementById("participants").appendChild(div);
				stream.play(elementId);
			}

			room.addEventListener("room-connected", function(roomEvent) {

				document.getElementById('room-header').innerText = 'ROOM \"' + room.name+'\"';
				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				room.publish(localStream);
				for(var i=0; i<roomEvent.participants.length; i++){
					var streams = roomEvent.participants[i].getStreams();
					subscribeToStreams(streams);
					for(key in streams){
						playVideo(streams[key]);
					}
				}
			});

			room.addEventListener("stream-subscribed", function(streamEvent) {
				// We don't do anything because video element is created when
				// stream-added event is received
			});

			room.addEventListener("stream-added", function(streamEvent) {

				var stream = streamEvent.stream;

				var streams = [];
				streams.push(stream);
				subscribeToStreams(streams);

				playVideo(stream);
			});

			room.addEventListener("stream-removed", function(streamEvent) {
				var element = document.getElementById("video-"+streamEvent.stream.getGlobalID());
				if (element !== undefined) {
					element.parentNode.removeChild(element);
				}
			});

			room.connect();

			var element = document.getElementById("myVideo");
			var video = document.createElement('div');
			video.id = "video-"+localStream.getGlobalID();
			element.appendChild(video);

			localStream.play(video.id);
		});

		localStream.init();

	});
}

function leaveRoom(){

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';

	var streams = room.getStreams();
	for (var index in streams) {
		var stream = streams[index];
		var element = document.getElementById("video-"+stream.getGlobalID());
		if (element) {
			element.parentNode.removeChild(element);
		}
	}

	room.leave();
}

window.onbeforeunload = function() {
	kurento.close();
};
