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

package org.opencastproject.vitalchat.api;

/**
 * Api for the Vital Chat Service
 */
public interface VitalChat {

  /**
   * The endpoint for the chat websocket
   * Specific chats are under `websocketAddress/{chat-id}`
   */
  String websocketAddress = "/vitalchat-websocket";

  /**
   * Creates a new chat with the given id
   * @param id chat-id
   * @return Whether the chat could be created or not
   */
  boolean createChat(String id);

  /**
   * Deletes an existing chat with the given id
   * @param id chat-id
   * @return Whether the chat could be deleted or not
   */
  boolean deleteChat(String id);

  /**
   * Get all chat ids
   * @return String array of chat ids
   */
  String[] getChats();
}
