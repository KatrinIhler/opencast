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

package org.opencastproject.index.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchQuery;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import com.entwinemedia.fn.data.Opt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexUtils {

  private static final Logger logger = LoggerFactory.getLogger(IndexUtils.class);

  private AuthorizationService authorizationService;
  private AssetManager assetManager;
  private SecurityService securityService;

  void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public Event getEventFromIndex(AbstractSearchIndex index, String id) throws SearchIndexException {
    final Organization organization = securityService.getOrganization();
    final User user = securityService.getUser();
    EventSearchQuery query = new EventSearchQuery(organization.getId(), user);
    query = query.withIdentifier(id);
    SearchResult<Event> results = index.getByQuery(query);
    SearchResultItem<Event>[] resultItems = results.getItems();
    Event e = resultItems[0].getSource();
    return e;
  }

  public void deleteEventFromIndex(AbstractSearchIndex index, String id) throws SearchIndexException {
    index.delete(Event.DOCUMENT_TYPE, id.concat(securityService.getOrganization().getId()));
    logger.info("Removed event {} from {} index.", id, index.getIndexName());
  }

  public JSONObject getEventsWithEmptyAclInIndex(AbstractSearchIndex index) throws SearchIndexException {
    Map<String, List<String>> eventsWithEmptyAcls = new HashMap();
    Map<String, List<String>> eventsWithBrokenAcls = new HashMap();

    int limit = 100;
    int offset = 0;
    int countEmptyAcls = 0;
    int countBrokenAcls = 0;
    int analyzedCount = 0;
    int total = 0;

    EventSearchQuery query;
    SearchResult<Event> result;
    do {
      query = new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
      query.withLimit(limit);
      query.withOffset(offset);
      query.sortByTitle(SearchQuery.Order.Ascending);
      query.sortByDate(SearchQuery.Order.Ascending);

      result = index.getByQuery(query);

      for (SearchResultItem<Event> r : result.getItems()) {
        Event event = r.getSource();
        total++;
        String aclString = event.getAccessPolicy();

        boolean emptyAcl = false;
        boolean brokenAcl = false;

        // check if acl is not-empty or broken
        if (isBlank(aclString)) {
          emptyAcl = true;
        } else {
          try {
            AccessControlList acl = AccessControlParser.parseAcl(aclString);
            if (acl.getEntries().isEmpty()) {
              emptyAcl = true;
            }
          } catch (IOException | AccessControlParsingException e) {
            brokenAcl = true;
          }
        }

        if (brokenAcl) {
          countBrokenAcls++;
          String seriesId = event.getSeriesId();
          if (isNotBlank(seriesId)) {
            putInList(eventsWithBrokenAcls, seriesId, event.getIdentifier());
          } else {
            putInList(eventsWithBrokenAcls, "No series", event.getIdentifier());
          }
        } else if (emptyAcl) {
          countEmptyAcls++;
          String seriesId = event.getSeriesId();
          if (isNotBlank(seriesId)) {
            putInList(eventsWithEmptyAcls, seriesId, event.getIdentifier());
          } else {
            putInList(eventsWithEmptyAcls, "No series", event.getIdentifier());
          }
        }
      }

      offset += limit;
      analyzedCount += result.getDocumentCount();

      logger.debug("Analyzed " + (analyzedCount) + " event acls so far.");
    } while (result.getDocumentCount() != 0);

    JSONObject o = new JSONObject();
    o.put("total", total);

    o.put("countEmptyAcls", countEmptyAcls);
    o.put("countBrokenAcls", countBrokenAcls);

    o.put("emptyAcls", mapToJson(eventsWithEmptyAcls));
    o.put("brokenAcls", mapToJson(eventsWithBrokenAcls));

    return o;
  }

  public JSONObject fixEventsWithEmptyAclInIndex(AbstractSearchIndex index, boolean fixBrokenAcls)
          throws SearchIndexException {
    List<String> fixed = new ArrayList<>();
    List<String> notFixed = new ArrayList<>();

    int limit = 100;
    int offset = 0;

    EventSearchQuery query;
    SearchResult<Event> result;
    do {
      query = new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser());
      query.withLimit(limit);
      query.withOffset(offset);
      query.sortByTitle(SearchQuery.Order.Ascending);
      query.sortByDate(SearchQuery.Order.Ascending);
      result = index.getByQuery(query);

      for (SearchResultItem<Event> r : result.getItems()) {
        Event event = r.getSource();
        String aclString = event.getAccessPolicy();

        boolean emptyAcl = false;
        boolean brokenAcl = false;

        // check if acl is not-empty or broken
        if (isBlank(aclString)) {
          emptyAcl = true;
        }
        else {
          try {
            AccessControlList acl = AccessControlParser.parseAcl(aclString);
            if (acl.getEntries().isEmpty()) {
              emptyAcl = true;
            }
          } catch (IOException | AccessControlParsingException e) {
            brokenAcl = true;
          }
        }

        if (emptyAcl || (brokenAcl && fixBrokenAcls)) {
          try {
            AQueryBuilder queryAM = assetManager.createQuery();
            final AResult aResult = queryAM.select(queryAM.snapshot()).where(queryAM.mediaPackageId(
                    event.getIdentifier()).and(queryAM.version().isLatest())).run();
            final Opt<ARecord> optARecord = aResult.getRecords().head();
            if (optARecord.isNone()) {
              throw new IllegalStateException("No Snapshot exists for event " + event.getIdentifier());
            }
            final Opt<Snapshot> optSnapshot = optARecord.get().getSnapshot();
            if (optSnapshot.isNone()) {
              throw new IllegalStateException("No Snapshot exists for event " + event.getIdentifier());
            }
            final Snapshot snapshot = optSnapshot.get();
            AccessControlList acl = authorizationService.getActiveAcl(snapshot.getMediaPackage()).getA();
            if (acl.getEntries().isEmpty()) {
              throw new IllegalStateException("ACL is empty in AssetManager for event " + event.getIdentifier());
            }
            event.setAccessPolicy(AccessControlParser.toJsonSilent(acl));
            index.addOrUpdate(event);
            fixed.add(event.getIdentifier());
          } catch (Exception e) {
            logger.warn("Failed to fix ACL of event [}", event.getIdentifier(), e);
            notFixed.add(event.getIdentifier());
          }
        }
      }

      offset += limit;
      logger.debug("Fixed " + (fixed.size()) + " event acls so far.");
    } while (result.getDocumentCount() != 0);

    JSONObject o = new JSONObject();
    o.put("total", fixed.size() + notFixed.size());
    o.put("fixedCount", fixed.size());
    o.put("notFixedCount", notFixed.size());
    o.put("fixed", listToJSON(fixed));
    o.put("notFixed", listToJSON(notFixed));

    return o;
  }


  private JSONObject mapToJson(Map<String, List<String>> results) {
    JSONObject r = new JSONObject();
    for (String key: results.keySet()) {
      List<String> list = results.get(key);
      r.put(key, listToJSON(list));
    }
    return r;
  }

  private JSONArray listToJSON(List<String> list) {
    JSONArray a = new JSONArray();
    for (String value : list) {
      a.add(value);
    }
    return a;
  }

  private void putInList(Map<String, List<String>> map, String key, String value) {
    List<String> list = map.computeIfAbsent(key, k -> new ArrayList<String>());
    list.add(value);
  }
}
