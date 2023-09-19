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

package org.opencastproject.vitallivestream.impl;

import org.opencastproject.vitallivestream.api.VitalLivestreamService;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Middleware between the Vital Streaming Agent and the video portal Valerie
 * Stores information on available channels (rooms) and livestreams that are currently running in these channels
 */
@Component(
    property = {
        "service.description=Vital Livestream Service"
    },
    immediate = true,
    service = { VitalLivestreamService.class, ManagedService.class }
)
public class VitalLivestreamServiceImpl implements VitalLivestreamService, ManagedService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(VitalLivestreamServiceImpl.class);

  /** Configuration file prefix for parsing channel Ids */
  public static final String CHANNELS_PREFIX = "channels.";

  /** Internal representation of currently running livestreams */
  private List<JsonVitalLiveStream> livestreams = new ArrayList<JsonVitalLiveStream>();

  /** Internal representation of currently existing channels */
  private List<String> channels = new ArrayList();

  @Override
  // Read config file
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Example at: userdirectory.InMemoryUserAndRoleProvider
    if (properties == null) {
      channels.clear();
      return;
    }

    List<String> newChannels = new ArrayList<>();

    Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      final String key = keys.nextElement();
      // Skip non user definition keys
      if (!key.startsWith(CHANNELS_PREFIX)) {
        continue;
      }
      final String[] channel = key.substring(CHANNELS_PREFIX.length()).split("\\.");
//      if (orgUser.length != 2) {
//        logger.warn("Ignoring invalid capture agent user definition. Should be {}.<organization>.<username>, was {}",
//                CAPTURE_AGENT_USER_PREFIX, key);
//      }
      final String channelId = channel[0];
//      final String name = Objects.toString(properties.get(key), null);

      newChannels.add(channelId);
    }

    // Update list of channels
    channels = newChannels;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#getAvailableChannels()
   */
  public List<String> getAvailableChannels() {
    return channels;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#getLivestreams()
   */
  public List<JsonVitalLiveStream> getLivestreams() {
    return livestreams;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#getLivestreamByChannel(String)
   */
  public JsonVitalLiveStream getLivestreamByChannel(String channelId) {
    return livestreams.stream().
            filter(l -> l.getId().equals(channelId)).
            findFirst()
            .orElse(null);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#updateLivestream(JsonVitalLiveStream)
   */
  public void updateLivestream(JsonVitalLiveStream livestream) {
    // Find by channel id
    OptionalInt indexOpt = findById(livestream);
    if (indexOpt == null) {
      return;
    }
    // Found? Replace
    if (indexOpt.isPresent()) {
      livestreams.set(indexOpt.getAsInt(), livestream);
    } else {
      // Not found? Add
      livestreams.add(livestream);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#deleteLivestream(JsonVitalLiveStream)
   */
  public boolean deleteLivestream(JsonVitalLiveStream livestream) {
    // Find by channel id
    OptionalInt indexOpt = findById(livestream);
    if (indexOpt == null) {
      return false;
    }
    // Found? Remove
    if (indexOpt.isPresent()) {
      livestreams.remove(indexOpt.getAsInt());
      return true;
    }
    return false;
  }

  /**
   * Get index of a livestream from the running livestreams
   * @param livestream Livestream to get the index of
   * @return OptionalInt, contains index if it could be found
   */
  private OptionalInt findById(JsonVitalLiveStream livestream) {
    if (livestream == null
            || (livestream != null && livestream.getId() == null)
    ) {
      return null;
    }
    OptionalInt indexOpt = IntStream.range(0, livestreams.size())
            .filter(i -> livestream.getId().equals(livestreams.get(i).getId()))
            .findFirst();
    return indexOpt;
  }
}
