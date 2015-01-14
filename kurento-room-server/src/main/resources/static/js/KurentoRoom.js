// Room --------------------------------

function Room(kurento, options) {

	var that = this;

	that.name = options.room;

	var ee = new EventEmitter();
	var streams = {};
	var connected = false;

	this.addEventListener = function(eventName, listener) {
		ee.addListener(eventName, listener);
	}

	this.emitEvent = function(eventName, eventsArray) {
		ee.emitEvent(eventName, eventsArray);
	}

	this.connect = function() {

		kurento.sendRequest('joinRoom',{
			user : options.user,
			room : options.room
		}, function(error,response){
			if(error){
				console.error(error);
			} else {
				connected = true;
				onExistingParticipants(response.value);
			}
		});
	}

	function onExistingParticipants(participants) {

		var roomEvent = {
			streams : []
		}

		var length = participants.length;
		for (var i = 0; i < length; i++) {
			var userName =participants[i];
			var stream = new Stream(kurento, false, {
				user : userName
			});
			streams[userName] = stream;
			roomEvent.streams.push(stream);
		}

		ee.emitEvent('room-connected', [ roomEvent ]);
	}

	this.subscribe = function(stream) {
		stream.room = that;
		stream.subscribe();
	}

	this.publish = function(localStream) {
		localStream.room = that;
		streams[localStream.getID()] = localStream;
		localStream.publish();
	}

	this.onNewParticipant = function(msg) {
		var stream = new Stream(kurento, false, {
			user : msg.name
		});
		streams[msg.name] = stream;
		ee.emitEvent('stream-added', [ {
			stream : stream
		} ]);
	}

	this.onParticipantLeft = function(msg) {
		var stream = streams[msg.name];
		if (stream !== undefined) {
			delete streams[msg.name];
			ee.emitEvent('stream-removed', [ {
				stream : stream
			} ]);
			stream.dispose();
		}
	}

	this.leave = function() {

		if(connected){
			kurento.sendRequest('leaveRoom',
			function(error,response){
				if(error){
					console.error(error);
				} else {
					connected = false;
				}
			});
		}

		for (var key in streams) {
			streams[key].dispose();
		}
	}

	this.getStreams = function(){
		return streams;
	}
}

// Stream --------------------------------

/* options:
   name: XXX
   data: true (Maybe this is based on webrtc)
   audio: true, video: true, url: "file:///..." > Player
   screen: true > Desktop (implicit video:true, audio:false)
   audio: true, video: true > Webcam

   stream.hasAudio();
   stream.hasVideo();
   stream.hasData();
*/
function Stream(kurento, local, options) {

	var that = this;

	this.room = undefined;

	var ee = new EventEmitter();
	var sdpOffer;
	var wrStream;
	var wp;
	var id = options.user;
	var videoElements = [];
	var elements = [];

	this.addEventListener = function(eventName, listener) {
		ee.addListener(eventName, listener);
	}

	this.playOnlyVideo = function(element) {

		var video = document.createElement('video');

		if(typeof element === "string"){
			document.getElementById(element).appendChild(video);
		} else {
			element.appendChild(video);
		}

		video.id = 'native-video-' + id;
		video.autoplay = true;
		video.controls = false;
		if(wrStream){
			video.src = URL.createObjectURL(wrStream);
		}

		videoElements.push(video);

		if(local){
			video.setAttribute("muted","muted");
		}
	}

	this.play = function(elementId) {

		var container = document.createElement('div');
		container.className = "participant";
		container.id = id;
		document.getElementById(elementId).appendChild(container);

		elements.push(container);

		var name = document.createElement('div');
		container.appendChild(name);
		name.appendChild(document.createTextNode(id));
		name.id = "name-"+id;
		name.className = "name";

		that.playOnlyVideo(container);

	}

	this.getID = function() {
		return id;
	}

	this.init = function() {

		var constraints = {
			audio : true,
			video : {
				mandatory : {
					maxWidth : 640,
					maxFrameRate : 15,
					minFrameRate : 15
				}
			}
		};

		getUserMedia(constraints, function(userStream) {
			wrStream = userStream;
			ee.emitEvent('access-accepted', null);
		}, function(error) {
			console.error(error);
		});
	}

	function initWebRtcPeer(sdpOfferCallback) {

		var startVideoCallback = function(sdpOfferParam) {
			sdpOffer = sdpOfferParam;
			kurento.sendRequest("receiveVideoFrom",{
				sender : id,
				sdpOffer : sdpOffer
			}, function(error,response){
				if(error){
					console.error(error);
				} else {
					that.processSdpAnswer(response.sdpAnswer);
				}
			});
		}

		var onerror = function(error) {
			console.error(error);
		}

		var mode = local ? "send" : "recv";

		wp = new kurentoUtils.WebRtcPeer(mode, null, null, startVideoCallback,
				onerror, null, null);

		wp.stream = wrStream;
		wp.start();

		console.log(name + " waiting for SDP offer");
	}

	this.publish = function() {

		// FIXME: Throw error when stream is not local

		initWebRtcPeer();

		// FIXME: Now we have coupled connecting to a room and adding a
		// stream to this room. But in the new API, there are two steps.
		// This is the second step. For now, it do nothing.

	}

	this.subscribe = function() {

		// FIXME: In the current implementation all participants are subscribed
		// automatically to all other participants. We use this method only to
		// negotiate SDP

		// Refactor this to avoid code duplication
		initWebRtcPeer();
	}

	this.processSdpAnswer = function(sdpAnswer) {

		var answer = new RTCSessionDescription({
			type : 'answer',
			sdp : sdpAnswer,
		});

		console.log('SDP answer received, setting remote description');

		wp.pc.setRemoteDescription(answer, function() {
			if (!local) {
				// FIXME: This avoid to subscribe to your own stream remotely.
				// Fix this
				wrStream = wp.pc.getRemoteStreams()[0];

				for(i=0; i<videoElements.length; i++){
					videoElements[i].src = URL.createObjectURL(wrStream);
				}

				that.room.emitEvent('stream-subscribed', [ {
					stream : that
				} ]);
			}
		}, function(error) {
			console.error(error)
		});
	}

	this.dispose = function() {
		console.log("disposed");

		function disposeElement(element){
			if(element && element.parentNode){
				element.parentNode.removeChild(element);
			}
		}

		for(i=0; i<elements.length; i++){
			disposeElement(elements[i]);
		}

		for(i=0; i<videoElements.length; i++){
			disposeElement(videoElements[i]);
		}

		if (wp.pc && wp.pc.signalingState != 'closed')
			wp.pc.close();

		if (wrStream) {
			wrStream.getAudioTracks().forEach(function(track) {
				track.stop && track.stop()
			})
			wrStream.getVideoTracks().forEach(function(track) {
				track.stop && track.stop()
			})
		}
	}
}

// KurentoRoom --------------------------------

function KurentoRoom(wsUri, callback) {

	if (!(this instanceof KurentoRoom))
		return new KurentoRoom(wsUri, callback);

	// Enable and disable iceServers from code
	kurentoUtils.WebRtcPeer.prototype.server.iceServers = [];

	var that = this;

	var userName;

	var ws = new WebSocket(wsUri);

	ws.onopen = function() {
		callback(null, that);
	}

	ws.onerror = function(evt) {
		callback(evt.data);
	}

	ws.onclose = function() {
		console.log("Connection Closed");
	}

	var options = { request_timeout: 50000 };
	var rpc = new RpcBuilder(RpcBuilder.packers.JsonRPC, options, ws, function(request)
	{
		console.info('Received request: ' + request);

		switch (request.method) {
		case 'newParticipantArrived':
			onNewParticipant(request.params);
			break;
		case 'participantLeft':
			onParticipantLeft(request.params);
			break;
		default:
			console.error('Unrecognized request: '+request.method);
		};
	});

	function onNewParticipant(msg) {
		if (room !== undefined) {
			room.onNewParticipant(msg);
		}
	}

	function onParticipantLeft(msg) {
		if (room !== undefined) {
			room.onParticipantLeft(msg);
		}
	}

	this.sendRequest = function(method, params, callback) {
		rpc.encode(method, params, callback);
		console.log('Sent request: { method:"'+method+"', params: "+ params+" }");
	}

	this.close = function() {
		if (room !== undefined) {
			room.leave();
		}
		ws.close();
	}

	this.Stream = function(options) {
		return new Stream(that, true, options);
	}

	this.Room = function(options) {
		// FIXME Support more than one room
		room = new Room(that, options);
		// FIXME Include name in stream, not in room
		usarName = options.userName;
		return room;
	}
}