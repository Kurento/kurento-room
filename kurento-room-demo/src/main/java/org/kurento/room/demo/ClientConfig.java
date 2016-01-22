/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.room.demo;

class ClientConfig {
  private boolean loopbackRemote;
  private boolean loopbackAndLocal;

  public boolean isLoopbackRemote() {
    return loopbackRemote;
  }

  public void setLoopbackRemote(boolean loopbackRemote) {
    this.loopbackRemote = loopbackRemote;
  }

  public boolean isLoopbackAndLocal() {
    return loopbackAndLocal;
  }

  public void setLoopbackAndLocal(boolean loopbackAndLocal) {
    this.loopbackAndLocal = loopbackAndLocal;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Loopback [remote=").append(loopbackRemote).append(", andLocal=")
    .append(loopbackAndLocal).append("]");
    return builder.toString();
  }
}
