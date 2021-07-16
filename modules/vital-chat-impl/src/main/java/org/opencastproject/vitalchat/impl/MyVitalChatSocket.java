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

import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(
        name = "vitalchat-websocket",
        immediate = true,
        property = {
                "service.description=Vital Chat Service"
        },
        service = VitalChat.class
)
@WebSocket
public class MyVitalChatSocket implements VitalChat {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(MyVitalChatSocket.class);

  private static final Map<String, Set<Session>> sessions = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, List<String>> chatLogs = Collections.synchronizedMap(new HashMap<>());

  @Reference
  private HttpService httpService;

  @Activate
  public void activate() throws Exception {
    httpService.registerServlet(websocketAddress, new VitalChatServlet(), null, null);
  }

  @Deactivate
  public void deactivate() {
    httpService.unregister(websocketAddress);
  }

  @OnWebSocketConnect
  public void onOpen(Session session) throws Exception {
    session.setIdleTimeout(-1);

    String id = urlParser(session.getUpgradeRequest().getRequestURI());
    // Add session to chat
    try {
      sessions.get(id).add(session);
    }
    catch (NullPointerException e) {
      session.close(new CloseStatus(-1, "The chat you tried to connect to does not exist"));
    }

    // Send chatlog to session
    for (String msg : chatLogs.get(id)) {
      session.getRemote().sendString(msg);
    }
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    sessions.get(urlParser(session.getUpgradeRequest().getRequestURI())).remove(session);
  }

  @OnWebSocketMessage
  public void onText(Session session, String msg) throws Exception {
    String id = urlParser(session.getUpgradeRequest().getRequestURI());
    chatLogs.get(id).add(msg);

    for (Session ses : sessions.get(id)) {
      ses.getRemote().sendString(msg);
    }
  }

  @OnWebSocketError
  public void onError(Session session, Throwable throwable) {
    session.close(new CloseStatus(-1, "Servererror: " + throwable.getCause()));
  }


  private String urlParser(URI uri) {
    String path = uri.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  public boolean createChat(String id) {
    if (sessions.containsKey(id)) {
      return false;
    }

    sessions.put(id, new HashSet<>());
    chatLogs.put(id, new ArrayList<>());

    return true;
  }

  public boolean deleteChat(String id) {
    if (!sessions.containsKey(id)) {
      return false;
    }

    // Do not close sessions. Chat should "stay open" until everyone leaves
    sessions.remove(id);
    chatLogs.remove(id);

    return true;
  }

  public String[] getChats() {
    return sessions.keySet().toArray(new String[sessions.size()]);
  }

}
