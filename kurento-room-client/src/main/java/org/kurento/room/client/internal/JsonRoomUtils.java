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

import org.kurento.jsonrpc.message.Request;
import org.kurento.room.exception.RoomException;
import org.kurento.room.exception.RoomException.Code;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * JSON tools for extracting info from request or response elements.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class JsonRoomUtils {

  public static <T> T getRequestParam(Request<JsonObject> request, String paramName, Class<T> type) {
    return getRequestParam(request, paramName, type, false);
  }

  public static <T> T getRequestParam(Request<JsonObject> request, String paramName, Class<T> type,
      boolean allowNull) {
    JsonObject params = request.getParams();
    if (params == null) {
      if (!allowNull) {
        throw new RoomException(Code.TRANSPORT_REQUEST_ERROR_CODE,
            "Invalid request lacking parameter '" + paramName + "'");
      } else {
        return null;
      }
    }
    return getConverted(params.get(paramName), paramName, type, allowNull);
  }

  public static <T> T getResponseProperty(JsonElement result, String property, Class<T> type) {
    return getResponseProperty(result, property, type, false);
  }

  public static <T> T getResponseProperty(JsonElement result, String property, Class<T> type,
      boolean allowNull) {
    if (!(result instanceof JsonObject)) {
      throw new RoomException(Code.TRANSPORT_RESPONSE_ERROR_CODE,
          "Invalid response format. The response '" + result + "' should be a Json object");
    }
    return getConverted(result.getAsJsonObject().get(property), property, type, allowNull);
  }

  public static JsonArray getResponseArray(JsonElement result) {
    if (!result.isJsonArray()) {
      throw new RoomException(Code.TRANSPORT_RESPONSE_ERROR_CODE,
          "Invalid response format. The response '" + result + "' should be a Json array");
    }
    return result.getAsJsonArray();
  }

  @SuppressWarnings("unchecked")
  private static <T> T getConverted(JsonElement paramValue, String property, Class<T> type,
      boolean allowNull) {
    if (paramValue == null) {
      if (allowNull) {
        return null;
      } else {
        throw new RoomException(Code.TRANSPORT_ERROR_CODE, "Invalid method lacking parameter '"
            + property + "'");
      }
    }

    if (type == String.class) {
      if (paramValue.isJsonPrimitive()) {
        return (T) paramValue.getAsString();
      }
    }

    if (type == Integer.class) {
      if (paramValue.isJsonPrimitive()) {
        return (T) Integer.valueOf(paramValue.getAsInt());
      }
    }

    if (type == JsonArray.class) {
      if (paramValue.isJsonArray()) {
        return (T) paramValue.getAsJsonArray();
      }
    }

    throw new RoomException(Code.TRANSPORT_ERROR_CODE, "Param '" + property + "' with value '"
        + paramValue + "' is not a " + type.getName());
  }
}
