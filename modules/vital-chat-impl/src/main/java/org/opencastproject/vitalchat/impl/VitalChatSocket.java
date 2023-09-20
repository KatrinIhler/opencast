/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.vitalchat.impl;

import org.opencastproject.vitalchat.api.VitalChat;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Component(
    name = "vitalchat-websocket",
    immediate = true,
    property = {
        "service.description=Vital Chat Service"
    },
    service = VitalChat.class
)
@ServerEndpoint(value = VitalChat.websocketAddress)
public class VitalChatSocket implements VitalChat {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(VitalChatSocket.class);

  private static final Map<String, Set<Session>> sessions = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, List<String>> chatLogs = Collections.synchronizedMap(new HashMap<>());

  @OnOpen
  public void onOpen(Session session) throws Exception {
    session.setMaxIdleTimeout(-1L);

    String id = urlParser(session.getRequestURI());
    // Add session to chat
    try {
      sessions.get(id).add(session);
    }
    catch (NullPointerException e) {
      session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE,
          "The chat you tried to connect to does not exist"));
    }

    // Send chatlog to session
    for (String msg : chatLogs.get(id)) {
      session.getBasicRemote().sendText(msg);
    }
  }

  @OnClose
  public void onClose(Session session, int statusCode, String reason) {
    sessions.get(urlParser(session.getRequestURI())).remove(session);
  }

  @OnMessage
  public void onText(Session session, String msg) throws Exception {
    String id = urlParser(session.getRequestURI());
    chatLogs.get(id).add(msg);

    for (Session ses : sessions.get(id)) {
      ses.getBasicRemote().sendText(msg);
    }
  }

  @OnError
  public void onError(Session session, Throwable throwable) throws IOException {
    session.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "Servererror: " + throwable.getCause()));
  }

  private String urlParser(URI uri) {
    String path = uri.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  @Override
  public boolean createChat(String id) {
    if (sessions.containsKey(id)) {
      logger.debug("Cannot create chat with id {}: Already exists", id);
      return false;
    }

    sessions.put(id, new HashSet<>());
    chatLogs.put(id, new ArrayList<>());

    return true;
  }

  @Override
  public boolean deleteChat(String id) {
    if (!sessions.containsKey(id)) {
      logger.debug("Cannot delete chat with id {}: Does not exist", id);
      return false;
    }

    // Do not close sessions. Chat should "stay open" until everyone leaves
    sessions.remove(id);
    chatLogs.remove(id);

    return true;
  }

  @Override
  public String[] getChats() {
    return sessions.keySet().toArray(new String[sessions.size()]);
  }
}
