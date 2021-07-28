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

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.vitalchat.api.VitalChat;
import org.opencastproject.vitallivestream.api.VitalLivestreamService;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
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

  /** The chat service */
  protected VitalChat vitalChatService;

  @Reference(name = "vitalchat-service")
  public void setVitalChatService(VitalChat service) {
    this.vitalChatService = service;
  }

  /** Configuration file prefix for parsing channel Ids */
  public static final String CHANNELS_PREFIX = "channels.";

  public static final String DOWNLOAD_SOURCE = "channelEndpoint";

  /** Internal representation of currently running livestreams */
  private List<JsonVitalLiveStream> livestreams = new ArrayList<JsonVitalLiveStream>();

  /** Internal representation of currently existing channels */
  private List<Channel> channels = new ArrayList();

  private String channelEndpoint;

  /** The http client */
  private TrustedHttpClient httpClient;

  /**
   * Sets the trusted http client
   *
   * @param httpClient
   *          the http client
   */
  @Reference
  public void setHttpClient(TrustedHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /** JSON parse utility */
  private static final Gson gson = new Gson();


  @Override
  // Read config file
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Example at: userdirectory.InMemoryUserAndRoleProvider
    if (properties == null) {
      channels.clear();
      channelEndpoint = null;
      return;
    }

    channelEndpoint = StringUtils.trimToEmpty(((String) properties.get(DOWNLOAD_SOURCE)));
    List<Channel> fetchChannels = null;
    try {
      fetchChannels = fetchChannels();
    } catch (IOException e) {
      e.printStackTrace();
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
      if (channel.length != 1) {
        logger.warn("Ignoring invalid channel definition. Should be {}.<channelId>, was {}",
                CHANNELS_PREFIX, key);
        continue;
      }
      final String channelId = channel[0];

      newChannels.add(channelId);
    }

    // Update list of channels
    try {
      channels = mergeChannels(newChannels, fetchChannels);
    } catch (NullPointerException e) {
      logger.info(e.getMessage());
    }
  }

  private List<Channel> fetchChannels() throws IOException {
    List<Channel> fetchedChannels = new ArrayList<>();
    Warg warg = null;
    String next = null;
    HttpResponse response = null;
    InputStream in = null;
    try {
      do {
        URI uri = new URI(channelEndpoint);
        HttpGet getDirectStream = new HttpGet(uri);
        response = httpClient.execute(getDirectStream);
        in = response.getEntity().getContent();

        String inString = IOUtils.toString(in, "UTF-8");
        in.close();

        warg = gson.fromJson(inString, Warg.class); //new TypeToken<List<String>>() { } .getType());

        if (warg.results != null) {
          fetchedChannels.addAll(warg.results);
        }
      } while (next != null);
    } catch (Exception e) {
      logger.warn("Could not fetch channel from {} because of {}", channelEndpoint, e.getMessage());
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }
    if (fetchedChannels != null) {
      logger.info(fetchedChannels.toString());
    } else {
      logger.info("FetchedChannels is null");
    }
    return fetchedChannels;
  }

  private List<Channel> mergeChannels(List<Channel> channels1, List<Channel> channels2) throws NullPointerException {
    Set<Channel> channelSet = new LinkedHashSet<>(channels1);
    channelSet.addAll(channels2);
    return new ArrayList<>(channelSet);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#getAvailableChannels()
   */
  public List<Channel> getAvailableChannels() {
    List<Channel> fetchChannels = null;
    try {
      fetchChannels = fetchChannels();
    } catch (IOException e) {
      e.printStackTrace();
    }

    channels = mergeChannels(channels, fetchChannels);

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
  public boolean updateLivestream(JsonVitalLiveStream livestream) {
    // Find by channel id
    OptionalInt indexOpt = findById(livestream);
    if (indexOpt == null) {
      return false;
    }
    // Found? Replace
    if (indexOpt.isPresent()) {
      livestreams.set(indexOpt.getAsInt(), livestream);
    } else {
      // Not found? Add
      livestreams.add(livestream);
      vitalChatService.createChat(livestream.getId());
    }

    return true;
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
      vitalChatService.deleteChat(livestream.getId());
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



  class Warg {
    // The unique channel ID in which this live event should be listed
    private int count;
    private String next;
    private String previous;
    private List<Channel> results;

    public int getCount() {
      return count;
    }
    public String getNext() {
      return next;
    }
    public String getPrevious() {
      return previous;
    }
    public List<Channel> getResults() {
      return results;
    }

  }
}
