var rtcStats = {};
var rtcStatsSum = {};
var rtcStatsAvg = {};
var avgMetrics = [ 'video_googCurrentDelayMs', 'audio_googDecodingCNG',
		'audio_googPreferredJitterBufferMs', 'audio_audioOutputLevel',
		'video_googFrameRateDecoded', 'video_googFrameHeightReceived',
		'audio_googDecodingPLC', 'video_googJitterBufferMs',
		'audio_googDecodingCTSG', 'video_googTargetDelayMs',
		'video_googMaxDecodeMs', 'audio_googCurrentDelayMs',
		'audio_googJitterBufferMs', 'video_googFrameWidthReceived',
		'video_googMinPlayoutDelayMs', 'audio_googJitterReceived',
		'video_googDecodeMs', 'video_googFrameRateReceived',
		'video_googRenderDelayMs', 'video_googFrameRateOutput' ];

function activateRtcStats() {
	var rate = 100; // by default each 100 ms
	if (arguments.length) {
		rate = arguments[0];
	}
	setInterval(updateRtcStats, rate);
}

function updateRtcStats() {
	if (room) {
		var streams = room.getStreams();
		var avgCount = 0;

		for ( var index in streams) {
			var stream = streams[index];
			var webRtcPeer = stream.getWebRtcPeer();
			var remoteStream = stream.getWrStream();

			if (webRtcPeer && remoteStream) {
				var videoTrack = remoteStream.getVideoTracks()[0];
				var audioTrack = remoteStream.getAudioTracks()[0];
				updateStats(webRtcPeer, videoTrack, "video_");
				updateStats(webRtcPeer, audioTrack, "audio_");
				avgCount++;
			}
		}
		makeAverage(avgCount);
	}
}

function updateStats(webRtcPeer, track, type) {
	webRtcPeer.pc.getStats(function(stats) {
		var result = stats.result()[2];
		if (result) {
			result.names().forEach(function(name) {
				if (isNumber(rtcStatsSum[type + name])) {
					rtcStatsSum[type + name] += parseInt(result.stat(name));
					rtcStatsAvg[type + name]++;
				} else {
					rtcStatsSum[type + name] = parseInt(result.stat(name));
					rtcStatsAvg[type + name] = 1;
				}
				// console.info(name + "=" + result.stat(name));
			});
		}
	}, track);
}

function isNumber(n) {
	return !isNaN(parseFloat(n)) && isFinite(n);
}

function makeAverage(count) {
	for ( var key in rtcStatsSum) {
		if (avgMetrics.indexOf(key) != -1) {
			rtcStats[key] = rtcStatsSum[key] / rtcStatsAvg[key];
		} else {
			rtcStats[key] = rtcStatsSum[key];
		}
	}
	rtcStatsSum = {};
	rtcStatsAvg = {};
}

// Uncomment this line to debug
// activateRtcStats();
