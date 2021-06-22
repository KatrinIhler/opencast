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

package org.opencastproject.vitallivestream.api;

import java.net.URL;
import java.util.List;

/**
 * Api for the Hello World Service
 */
public interface VitalLivestreamService {

  /**
   * Adds a new active livestream or updates it if it is already running
   * @param livestream New livestream
   */
  void updateLivestream(JsonVitalLiveStream livestream);

  /**
   * Removes a livestream, thereby "stopping" it
   * @param livestream Livestream to stop
   * @return Whether a livestream could be stopped or not
   */
  boolean deleteLivestream(JsonVitalLiveStream livestream);

  /**
   * Get a list of all the channels that are currently configured
   * @return List of channels
   */
  List<String> getAvailableChannels();

  /**
   * Get a list of all livestreams that are currently running
   * @return List of livestreams
   */
  List<JsonVitalLiveStream> getLivestreams();

  /**
   * Get the livestream for a specific channel
   * @param channelId Id of the channel we want to get a livestream for
   * @return Livestream if found, else null
   */
  JsonVitalLiveStream getLivestreamByChannel(String channelId);

  /**
   * Struct to store information on a currently running livestream with
   */
  class JsonVitalLiveStream {
    // The unique channel ID in which this live event should be listed
    private String id;
    // The URL to use when creating new viewers
    private URL viewer;
    // Title of the live event
    private String title;
    // Teaser/Description of the live event, may contain HTML
    private String description;
    // Boolean flag that signals if the event is public (`true`) or private (`false`)
    private Boolean publically;

    // A dictionary of live streams used in this event (keys are usually *presenter* and *slides*)
    private Previews previews;

    public JsonVitalLiveStream(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
    public URL getViewer() {
      return viewer;
    }
    public String getTitle() {
      return title;
    }
    public String getDescription() {
      return description;
    }
    public Boolean getPublically() {
      return publically;
    }
    public Previews getPreview() {
      return previews;
    }

    public class Previews {
      private URL[] presenter;
      private URL[] slides;

      public URL[] getPresenter() {
        return presenter;
      }
      public URL[] getSlides() {
        return slides;
      }
    }
  }
}
