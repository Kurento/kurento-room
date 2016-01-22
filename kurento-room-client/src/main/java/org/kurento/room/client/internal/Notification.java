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

package org.kurento.room.client.internal;

import org.kurento.room.internal.ProtocolElements;

/**
 * Wrapper for server events.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public abstract class Notification {

  public enum Method {
    ICECANDIDATE_METHOD(ProtocolElements.ICECANDIDATE_METHOD), MEDIAERROR_METHOD(
        ProtocolElements.MEDIAERROR_METHOD), PARTICIPANTJOINED_METHOD(
            ProtocolElements.PARTICIPANTJOINED_METHOD), PARTICIPANTLEFT_METHOD(
                ProtocolElements.PARTICIPANTLEFT_METHOD), PARTICIPANTEVICTED_METHOD(
                    ProtocolElements.PARTICIPANTEVICTED_METHOD), PARTICIPANTPUBLISHED_METHOD(
                        ProtocolElements.PARTICIPANTPUBLISHED_METHOD), PARTICIPANTUNPUBLISHED_METHOD(
                            ProtocolElements.PARTICIPANTUNPUBLISHED_METHOD), ROOMCLOSED_METHOD(
                                ProtocolElements.ROOMCLOSED_METHOD), PARTICIPANTSENDMESSAGE_METHOD(
                                    ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD);

    private String methodValue;

    private Method(String val) {
      this.methodValue = val;
    }

    public String getMethodValue() {
      return methodValue;
    }

    public static Method getFromValue(String val) {
      for (Method m : Method.values()) {
        if (m.methodValue.equals(val)) {
          return m;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return getMethodValue().toString();
    }
  }

  private Method method;

  public Notification(Method method) {
    this.setMethod(method);
  }

  public Notification(String methodValue) {
    this(Method.getFromValue(methodValue));
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    if (method != null) {
      builder.append("method=").append(method);
    }
    builder.append("]");
    return builder.toString();
  }
}
