function AppParticipant(stream) {

    this.stream = stream;
    this.videoElement;

    var that = this;

    this.setMain = function () {

        var mainVideo = document.getElementById("main-video");
        var oldVideo = mainVideo.firstChild;

        stream.playOnlyVideo("main-video");

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
            that.videoElement.parentNode.removeChild(that.videoElement);
        }
    }

    function playVideo() {

        var elementId = "video-" + stream.getGlobalID();

        that.videoElement = document.createElement('div');
        that.videoElement.setAttribute("id", elementId);
        that.videoElement.className = "video";
        that.videoElement.style.display = "none";

        var buttonVideo = document.createElement('button');
        buttonVideo.className = 'action btn btn--m btn--orange btn--fab mdi md-desktop-mac';
        buttonVideo.setAttribute("ng-click", "");
        buttonVideo.style.position = "absolute";
        buttonVideo.style.left = "75%";
        buttonVideo.style.top = "60%";
        buttonVideo.style.zIndex = "100";
        that.videoElement.appendChild(buttonVideo);

        document.getElementById("participants").appendChild(that.videoElement);

        that.stream.play(elementId);
    }

    playVideo();
}

function Participants() {

    var mainParticipant;
    var localParticipant;

    var participants = {};
    var roomName;
    var that = this;

    this.getRoomName = function () {
        console.log("room - getRoom " + roomName);
        roomName = room.name;
        return roomName;
    };


    function updateVideoStyle() {

        var MAX_WIDTH = 15;
        roomName = room.name;
        var numParticipants = Object.keys(room.getStreams()).length;
        console.log("room" + room.name);
        var maxParticipantsWithMaxWidth = 95 / MAX_WIDTH;

        if (numParticipants > maxParticipantsWithMaxWidth) {
            $('.video').css({
                "width": (95 / numParticipants) + "%"
            });
        } else {
            $('.video').css({
                "width": MAX_WIDTH + "%"
            });
        }
    }
    ;

    function updateMainParticipant(participant) {
        // if (mainParticipant !== undefined) {
        //     mainParticipant.removeMain();
        // }
        mainParticipant.removeMain();
        mainParticipant = participant;
        mainParticipant.setMain();
    }

    this.addLocalParticipant = function (stream) {

        localParticipant = that.addParticipant(stream);
        mainParticipant = localParticipant;
        mainParticipant.setMain();
    };

    this.addParticipant = function (stream) {

        var participant = new AppParticipant(stream);
        participants[stream.getGlobalID()] = participant;

        updateVideoStyle();

        $(participant.videoElement).click(function () {
            updateMainParticipant(participant);
        });

        //updateMainParticipant(participant);

        return participant;
    };

    this.removeParticipant = function (stream) {

        var participant = participants[stream.getGlobalID()];
        delete participants[stream.getGlobalID()];

        if (mainParticipant === participant) {
            mainParticipant = localParticipant;
            mainParticipant.setMain();
        }

        participant.remove();

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
            $window.location.href = '#/login';
        });
    };
}