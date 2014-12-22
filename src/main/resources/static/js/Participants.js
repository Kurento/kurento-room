function Participant(stream) {

	this.stream = stream;
	this.videoElement;

	var that = this;

	this.setMain = function() {

		var mainVideo = document.getElementById("main-video");
		var oldVideo = mainVideo.firstChild;

		stream.playOnlyVideo("main-video");

		that.videoElement.className += " active-video";

		if (oldVideo !== null) {
			mainVideo.removeChild(oldVideo);
		}
	}

	this.removeMain = function() {
		$(that.videoElement).removeClass("active-video");
	}

	this.remove = function() {
		if (that.videoElement !== undefined) {
			that.videoElement.parentNode.removeChild(that.videoElement);
		}
	}

	function playVideo() {

		var elementId = "video-" + stream.getID();

		that.videoElement = document.createElement('div');
		that.videoElement.setAttribute("id", elementId);
		that.videoElement.className = "video";

		document.getElementById("participants").appendChild(that.videoElement);

		that.stream.play(elementId);
	}

	playVideo();
}

function Participants() {

	var mainParticipant;
	var localParticipant;

	var participants = {};

	var that = this;

	function updateVideoStyle() {

		var MAX_WIDTH = 15;

		var numParticipants = Object.keys(room.getStreams()).length;

		var maxParticipantsWithMaxWidth = 95 / MAX_WIDTH;

		if (numParticipants > maxParticipantsWithMaxWidth) {
			$('.video').css({
				"width" : (95 / numParticipants) + "%"
			});
		} else {
			$('.video').css({
				"width" : MAX_WIDTH + "%"
			});
		}
	}

	function updateMainParticipant(participant) {
		if (mainParticipant !== undefined) {
			mainParticipant.removeMain();
		}
		participant.setMain();
		mainParticipant = participant;
	}

	this.addLocalParticipant = function(stream) {

		localParticipant = that.addParticipant(stream);
	}

	this.addParticipant = function(stream) {

		var participant = new Participant(stream);

		participants[stream.getID()] = participant;

		updateVideoStyle();

		$(participant.videoElement).click(function() {
			updateMainParticipant(participant);
		});

		updateMainParticipant(participant);

		return participant;
	}

	this.removeParticipant = function(stream) {

		var participant = participants[stream.getID()];
		delete participants[stream.getID()];

		if (mainParticipant === participant) {
			mainPartipant = localParticipant;
			localParticipant.setMain();
		}

		participant.remove();

		updateVideoStyle();
	}

	this.removeParticipants = function() {

		for ( var index in participants) {
			var participant = participants[index];
			participant.remove();
		}
	}

	this.getParticipants = function() {
		return participants;
	}
}