package org.kurento.room;

import org.kurento.client.KurentoClient;
import org.kurento.client.Properties;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.KurentoClientSessionInfo;
import org.kurento.room.exception.RoomException;

public class AutodiscoveryKurentoClientProvider
		implements KurentoClientProvider {

	private static final int ROOM_PIPELINE_LOAD_POINTS = 50;

	@Override
	public KurentoClient getKurentoClient(KurentoClientSessionInfo sessionInfo)
			throws RoomException {

		return KurentoClient
				.create(Properties.of("loadPoints", ROOM_PIPELINE_LOAD_POINTS));

	}

	@Override
	public boolean destroyWhenUnused() {
		return true;
	}
}
