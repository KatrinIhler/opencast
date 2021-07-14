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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
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
        name = "example-websocket",
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

  private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

  private boolean notification = false;

  private static final Map<String, Set<Session>> newSessions = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, List<String>> chatLogs = Collections.synchronizedMap(new HashMap<>());

  @Reference
  private HttpService httpService;

  @OnWebSocketConnect
  public void onOpen(Session session) throws Exception {
    session.setIdleTimeout(-1);
//    sessions.add(session);

    String id = urlParser(session.getUpgradeRequest().getRequestURI());
    newSessions.get(id).add(session);

    for (String msg : chatLogs.get(id)) {
      session.getRemote().sendString(msg);
    }
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
//    sessions.remove(session);

    newSessions.get(urlParser(session.getUpgradeRequest().getRequestURI())).remove(session);
  }

  @Activate
  public void activate() throws Exception {
    httpService.registerServlet("/example-websocket", new VitalChatServlet(), null, null);

//    notification = true;
//    Thread notification = new  Thread(new NotificationThread(sessions));
//    notification.start();
  }

  @Deactivate
  public void deactivate() throws Exception {
    httpService.unregister("/example-websocket");
    notification = false;
  }

  @OnWebSocketMessage
  public void onText(Session session, String msg) throws Exception {
//    for (Session ses : sessions) {
//      ses.getRemote().sendString(msg);
//    }
    String id = urlParser(session.getUpgradeRequest().getRequestURI());
    chatLogs.get(id).add(msg);

    for (Session ses : newSessions.get(id)) {
      ses.getRemote().sendString(msg);
    }
  }

  class NotificationThread implements Runnable {

    private Set<Session> sessions;

    NotificationThread(Set<Session> sessions) {
      this.sessions = sessions;
    }

    @Override
    public void run() {
      try {
        while (notification) {
          for (Session session : sessions) {
            session.getRemote().sendString("Hello World");
          }
          Thread.sleep(1000);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  private String urlParser(URI uri) {
    String path = uri.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  public String vitalChat() {
    logger.info("message");
    return "message";
  }

  public String createChat(String id) throws Exception {
    // Create some id
    id = "42";

    newSessions.put(id, new HashSet<>());
    chatLogs.put(id, new ArrayList<>());

    // Return chat id
    return id;
  }

  public String deleteChat(String id) throws Exception {
    // Create some id
    id = "42";

    newSessions.remove(id);
    chatLogs.remove(id);

    // Return chat id
    return id;
  }

}
