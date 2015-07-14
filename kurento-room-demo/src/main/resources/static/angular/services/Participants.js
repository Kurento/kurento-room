function AppParticipant(stream) {

    this.stream = stream;
    this.videoElement;
    this.thumbnailId;
    
    var that = this;

    this.getStream = function() {
		return this.stream;
	}

    this.setMain = function () {

        var mainVideo = document.getElementById("main-video");
        var oldVideo = mainVideo.firstChild;

        stream.playOnlyVideo("main-video", that.thumbnailId);

        that.videoElement.className += " active-video";

        if (oldVideo !== null) {
            mainVideo.removeChild(oldVideo);
        }
    }

    this.removeMain = function () {
        $(that.videoElement).removeClass("active-video");
    }

    this.remove = function () {
        if (that.videoElement !== undefined) {
            if (that.videoElement.parentNode !== null) {
                that.videoElement.parentNode.removeChild(that.videoElement);
            }
        }
    }

    function playVideo() {

        that.thumbnailId = "video-" + stream.getGlobalID();

        that.videoElement = document.createElement('div');
        that.videoElement.setAttribute("id", that.thumbnailId);
        that.videoElement.className = "video";

        var buttonVideo = document.createElement('button');
        buttonVideo.className = 'action btn btn--m btn--orange btn--fab mdi md-desktop-mac';
        //FIXME this won't work, Angular can't get to bind the directive ng-click nor lx-ripple
        buttonVideo.setAttribute("ng-click", "disconnectStream();$event.stopPropagation();");
        buttonVideo.setAttribute("lx-ripple", "");
        buttonVideo.style.position = "absolute";
        buttonVideo.style.left = "75%";
        buttonVideo.style.top = "60%";
        buttonVideo.style.zIndex = "100";
        that.videoElement.appendChild(buttonVideo);      

        document.getElementById("participants").appendChild(that.videoElement);
        
        that.stream.playThumbnail(that.thumbnailId);
    }

    playVideo();
}

function Participants() {

    var mainParticipant;
    var localParticipant;
    var mirrorParticipant;
    
    var participants = {};
    var roomName;
    var that = this;
    var connected = true;
    
    this.isConnected = function() {
    	return connected;
    }
    
    this.getRoomName = function () {
        console.log("room - getRoom " + roomName);
        roomName = room.name;
        return roomName;
    };

    this.getMainParticipant = function() {
		return mainParticipant;
	}
    
    function updateVideoStyle() {
        var MAX_WIDTH = 14;
        var numParticipants = Object.keys(participants).length;
        var maxParticipantsWithMaxWidth = 98 / MAX_WIDTH;

        if (numParticipants > maxParticipantsWithMaxWidth) {
            $('.video').css({
                "width": (98 / numParticipants) + "%"
            });
        } else {
            $('.video').css({
                "width": MAX_WIDTH + "%"
            });
        }
    };

    function updateMainParticipant(participant) {
        if (mainParticipant) {
        	mainParticipant.removeMain();
        }
        mainParticipant = participant;
        mainParticipant.setMain();
    }

    this.addLocalParticipant = function (stream) {
        localParticipant = that.addParticipant(stream);
        mainParticipant = localParticipant;
        mainParticipant.setMain();
    };

    this.addLocalMirror = function (stream) {
		mirrorParticipant = that.addParticipant(stream);
	};
    
    this.addParticipant = function (stream) {

        var participant = new AppParticipant(stream);
        participants[stream.getGlobalID()] = participant;

        updateVideoStyle();

        $(participant.videoElement).click(function (e) {
            updateMainParticipant(participant);
        });

        //updateMainParticipant(participant);

        return participant;
    };
    
    this.removeParticipantByStream = function (stream) {
        this.removeParticipant(stream.getGlobalID());
    };

    this.disconnectParticipant = function (appParticipant) {
    	this.removeParticipant(appParticipant.getStream().getGlobalID());
    };

    this.removeParticipant = function (streamId) {
    	var participant = participants[streamId];
        delete participants[streamId];
        participant.remove();
        
        if (mirrorParticipant) {
        	var otherLocal = null;
        	if (participant === localParticipant) {
        		otherLocal = mirrorParticipant;
        	}
        	if (participant === mirrorParticipant) {
        		otherLocal = localParticipant;
        	}
        	if (otherLocal) {
        		console.log("Removed local participant (or mirror) so removing the other local as well");
        		delete participants[otherLocal.getStream().getGlobalID()];
        		otherLocal.remove();
        	}
        }
        
        //setting main
        if (mainParticipant && mainParticipant === participant) {
        	var mainIsLocal = false;
        	if (localParticipant) {
        		if (participant !== localParticipant && participant !== mirrorParticipant) {
        			mainParticipant = localParticipant;
        			mainIsLocal = true;
        		} else {
        			localParticipant = null;
                	mirrorParticipant = null;
        		}
        	}
        	if (!mainIsLocal) {
        		var keys = Object.keys(participants);
        		if (keys.length > 0) {
        			mainParticipant = participants[keys[0]];
        		} else {
        			mainParticipant = null;
        		}
        	}
        	if (mainParticipant) {
        		mainParticipant.setMain();
        		console.log("Main video from " + mainParticipant.getStream().getGlobalID());
        	} else
        		console.error("No media streams left to display");
        }

        updateVideoStyle();
    };

    this.removeParticipants = function () {

        for (var index in participants) {
            var participant = participants[index];
            participant.remove();
        }
    };

    this.getParticipants = function () {
        return participants;
    };

    // Open the chat automatically when a message is received
    function autoOpenChat() {
        var selectedEffect = "slide";
        var options = {direction: "right"};
        if ($("#effect").is(':hidden')) {
            $("#content").animate({width: '80%'}, 500);
            $("#effect").toggle(selectedEffect, options, 500);
        }
    };

    this.showMessage = function (room, user, message) {
//        console.log(JSON.stringify(mainParticipant.videoElement));
//        console.log(JSON.stringify(localParticipant.videoElement()));
//        console.log(user);

        var ul = document.getElementsByClassName("list");
        console.log(ul);
        console.log(localParticipant.videoElement.innerText);
        console.log(localParticipant.videoElement.innerText.replace("_webcam", ""));
        var localUser = localParticipant.videoElement.innerText.replace("_webcam", "");
        if (room === roomName && user === localUser) { //me

            var li = document.createElement('li');
            li.className = "list-row list-row--has-primary list-row--has-separator";
            var div1 = document.createElement("div1");
            div1.className = "list-secondary-tile";
            var img = document.createElement("img");
            img.className = "list-primary-tile__img";
            img.setAttribute("src", "http://ui.lumapps.com/images/placeholder/2-square.jpg");
            var div2 = document.createElement('div');
            div2.className = "list-content-tile list-content-tile--two-lines";
            var strong = document.createElement('strong');
            strong.innerHTML = user;
            var span = document.createElement('span');
            span.innerHTML = message;
            div2.appendChild(strong);
            div2.appendChild(span);
            div1.appendChild(img);
            li.appendChild(div1);
            li.appendChild(div2);
            ul[0].appendChild(li);

//               <li class="list-row list-row--has-primary list-row--has-separator">
//                        <div class="list-secondary-tile">
//                            <img class="list-primary-tile__img" src="http://ui.lumapps.com/images/placeholder/2-square.jpg">
//                        </div>
//
//                        <div class="list-content-tile list-content-tile--two-lines">
//                            <strong>User 1</strong>
//                            <span>.............................</span>
//                        </div>
//                    </li>


        } else {//others

            var li = document.createElement('li');
            li.className = "list-row list-row--has-primary list-row--has-separator";
            var div1 = document.createElement("div1");
            div1.className = "list-primary-tile";
            var img = document.createElement("img");
            img.className = "list-primary-tile__img";
            img.setAttribute("src", "http://ui.lumapps.com/images/placeholder/1-square.jpg");
            var div2 = document.createElement('div');
            div2.className = "list-content-tile list-content-tile--two-lines";
            var strong = document.createElement('strong');
            strong.innerHTML = user;
            var span = document.createElement('span');
            span.innerHTML = message;
            div2.appendChild(strong);
            div2.appendChild(span);
            div1.appendChild(img);
            li.appendChild(div1);
            li.appendChild(div2);
            ul[0].appendChild(li);
            autoOpenChat();

//                 <li class="list-row list-row--has-primary list-row--has-separator">
//                        <div class="list-primary-tile">
//                            <img class="list-primary-tile__img" src="http://ui.lumapps.com/images/placeholder/1-square.jpg">
//                        </div>
//
//                        <div class="list-content-tile list-content-tile--two-lines">
//                            <strong>User 2</strong>
//                            <span>.............................</span>
//                        </div>
//                    </li>
        }
    };

    this.showError = function ($window, LxNotificationService, e) {
        LxNotificationService.alert('Error!', e.error.message, 'Reconnect', function(answer) {
        	connected = false;
            $window.location.href = '#/login';
        });
    };
    
    this.forceClose = function ($window, LxNotificationService, msg) {
        LxNotificationService.alert('Warning!', msg, 'Reload', function(answer) {
        	that.removeParticipants();
        	connected = false;
            $window.location.href = '/';
        });
    };
    
    this.alertMediaError = function ($window, LxNotificationService, msg, callback) {
    	LxNotificationService.confirm('Warning!', 'Server media error: <<' + msg
    			+ ">>. Please reconnect.", { cancel:'Disagree', ok:'Agree' }, 
    			function(answer) {
    	            console.log("User agrees upon media error: " + answer);
    	            if (answer) {
    	            	that.removeParticipants();
    	            	connected = false;
    	                $window.location.href = '/';
    	            }
    	            if (typeof callback === "function") {
    	            	callback(answer);
    	            }
    			});
	};
}