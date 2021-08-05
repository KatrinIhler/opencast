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
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

  /** JSON parse utility */
  private static final Gson gson = new Gson();

  /** Configuration file prefix for parsing channel Ids */
  private static final String CHANNELS_PREFIX = "channels.";
  /** Configuration file variable for the remote channel endpoint */
  private static final String CHANNEL_ENDPOINT = "channelEndpoint";
  /** We are only interested in channels that are associated with this portal number */
  private static final int portalNumber = 1;
  /** Configuration file variable for the remote channel endpoint */
  private static final String VIEWER_CREDENTIALS = "viewerCredentials";

  /** Internal representation of currently running livestreams */
  private List<JsonVitalLiveStream> livestreams = new ArrayList<JsonVitalLiveStream>();

  /** Internal representation of currently existing channels */
  private List<Channel> channels = new ArrayList();
  /** Separate representation for channels from the config file, so as to not accidentally overwrite them */
  private List<Channel> configChannels = new ArrayList();
  /** URI to send get requests to */
  private String channelEndpoint;
  private String viewerCredentials;

  /** The chat service */
  protected VitalChat vitalChatService;

  /** The http client */
  private TrustedHttpClient httpClient;

  /**
   * Set the chat service
   * @param service the chat service
   */
  @Reference(name = "vitalchat-service")
  public void setVitalChatService(VitalChat service) {
    this.vitalChatService = service;
  }

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

  @Override
  // Runs when config file is changed
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Example at: userdirectory.InMemoryUserAndRoleProvider
    if (properties == null) {
      channels.clear();
      channelEndpoint = null;
      viewerCredentials = null;
      return;
    }

    // Get credentials
    viewerCredentials = StringUtils.trimToEmpty(((String) properties.get(VIEWER_CREDENTIALS)));
    if (viewerCredentials == null) {
      logger.debug("Viewer credentials not specified. Are you sure authorization is not necessary?");
    }

    // Get channels from endpoint
    channelEndpoint = StringUtils.trimToEmpty(((String) properties.get(CHANNEL_ENDPOINT)));
    List<Channel> fetchChannels = null;
    try {
      fetchChannels = fetchChannels();
    } catch (IOException e) {
      logger.warn("Could not fetch channels from endpoint: {}", e.getMessage());
    }

    // Read channels from config file
    configChannels = new ArrayList<>();
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
      final String name = Objects.toString(properties.get(key), null);
      List<Integer> portals = new ArrayList<>();
      portals.add(portalNumber);

      configChannels.add(new Channel(channelId, name, portals));
    }

    // Update list of channels
    try {
      channels = mergeChannels(configChannels, fetchChannels);
    } catch (NullPointerException e) { }
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
      logger.warn("Could not fetch channels from endpoint: {}", e.getMessage());
    }

    channels = mergeChannels(configChannels, fetchChannels);

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
   * {@inheritDoc}
   * @see org.opencastproject.vitallivestream.api.VitalLivestreamService#getViewerCredentials()
   */
  public String getViewerCredentials() {
    return viewerCredentials;
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

  /**
   * For parsing responses from the remote channel endpoint
   */
  class ChannelResponseResult {
    // Pagination
    private int count;
    private String next;
    private String previous;
    // Channels on this "page"
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

  /**
   * Fetch all channels from endpoint
   * @return
   * @throws IOException
   */
  private List<Channel> fetchChannels() throws IOException {
    List<Channel> fetchedChannels = new ArrayList<>();
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

        ChannelResponseResult result = gson.fromJson(inString, ChannelResponseResult.class);

        if (result.results != null) {
          fetchedChannels.addAll(result.results);
        }

        next = result.next;
      } while (next != null);
    } catch (Exception e) {
      logger.warn("Could not fetch channel from {} because of {}", channelEndpoint, e.getMessage());
    } finally {
      IOUtils.closeQuietly(in);
      httpClient.close(response);
    }

    // Filter out channels that do not interest us
    fetchedChannels = fetchedChannels.stream()
            .filter(a -> a.getPortals().contains(1))
            .collect(Collectors.toList());

    return fetchedChannels;
  }

  /**
   * Merge two lists of objects, remove duplicates based on id
   * Could make this more generic if needed
   * @return combined list without duplicates
   * @throws NullPointerException
   */
  private List<Channel> mergeChannels(List<Channel> channels1, List<Channel> channels2) throws NullPointerException {
    List<Channel> channels = new ArrayList<>(
            Stream.of(channels1, channels2)
                    .flatMap(List::stream)
                    .collect(Collectors.toMap(Channel::getId,
                            d -> d,
                            (Channel x, Channel y) -> x == null ? y : x))
                    .values());
    return channels;
  }
}
