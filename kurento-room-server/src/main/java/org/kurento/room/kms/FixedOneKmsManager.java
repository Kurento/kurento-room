/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.room.kms;

import org.kurento.client.KurentoClient;

public class FixedOneKmsManager extends KmsManager {

  public FixedOneKmsManager(String kmsWsUri) {
    this(kmsWsUri, 1);
  }

  public FixedOneKmsManager(String kmsWsUri, int numKmss) {
    for (int i = 0; i < numKmss; i++) {
      this.addKms(new Kms(KurentoClient.create(kmsWsUri), kmsWsUri));
    }
  }
}
