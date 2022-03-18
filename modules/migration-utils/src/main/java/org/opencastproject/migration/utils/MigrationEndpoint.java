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

package org.opencastproject.migration.utils;

import static com.entwinemedia.fn.data.Opt.nul;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.util.QueryPreprocessor;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.theme.IndexTheme;
import org.opencastproject.elasticsearch.index.objects.theme.ThemeIndexSchema;
import org.opencastproject.elasticsearch.index.objects.theme.ThemeSearchQuery;
import org.opencastproject.index.service.resources.list.query.ThemesListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.staticfiles.api.StaticFileService;
import org.opencastproject.staticfiles.endpoint.StaticFileRestService;
import org.opencastproject.themes.Theme;
import org.opencastproject.themes.ThemesServiceDatabase;
import org.opencastproject.themes.persistence.ThemesServiceDatabaseException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "migrationutils", title = "Migration Utils", notes = {},
        abstractText = "Provides utility functionality for the migration")
@Component(
    immediate = true,
    service = MigrationEndpoint.class,
    property = {
        "service.description=Migration Utility Endpoint",
        "opencast.service.type=org.opencastproject.migration.utils.MigrationEndpoint",
        "opencast.service.path=/migration-utils",
    })
public class MigrationEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MigrationEndpoint.class);

  @Reference
  private SecurityService securityService;
  @Reference
  private StaticFileService staticFileService;
  @Reference
  private ThemesServiceDatabase themesServiceDatabase;
  @Reference
  private UserDirectoryService userDirectoryService;
  @Reference
  private StaticFileRestService staticFileRestService;
  @Reference
  private ElasticsearchIndex searchIndex;


  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("themes.json")
  @RestQuery(name = "getThemes", description = "Return all of the known themes on the system", restParameters = {
      @RestParameter(name = "filter", isRequired = false,
          description = "The filter used for the query. They should be formated like that: "
              + "'filter1:value1,filter2:value2'", type = STRING),
      @RestParameter(defaultValue = "0", description = "The maximum number of items to return per page.",
          isRequired = false, name = "limit", type = RestParameter.Type.INTEGER),
      @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset",
          type = RestParameter.Type.INTEGER),
      @RestParameter(name = "sort", isRequired = false,
          description = "The sort order. May include any of the following: NAME, CREATOR.  Add '_DESC' to reverse the "
              + "sort order (e.g. CREATOR_DESC).", type = STRING) },
      responses = { @RestResponse(description = "A JSON representation of the themes",
          responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getThemes(@QueryParam("filter") String filter, @QueryParam("limit") int limit,
      @QueryParam("offset") int offset, @QueryParam("sort") String sort) {
    Option<Integer> optLimit = Option.option(limit);
    Option<Integer> optOffset = Option.option(offset);
    Option<String> optSort = Option.option(trimToNull(sort));

    ThemeSearchQuery query = new ThemeSearchQuery(securityService.getOrganization().getId(), securityService.getUser());

    // If the limit is set to 0, this is not taken into account
    if (optLimit.isSome() && limit == 0) {
      optLimit = Option.none();
    }

    if (optLimit.isSome()) {
      query.withLimit(optLimit.get());
    }
    if (optOffset.isSome()) {
      query.withOffset(offset);
    }

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      if (ThemesListQuery.FILTER_CREATOR_NAME.equals(name)) {
        query.withCreator(filters.get(name));
      }
      if (ThemesListQuery.FILTER_TEXT_NAME.equals(name)) {
        query.withText(QueryPreprocessor.sanitize(filters.get(name)));
      }
    }

    if (optSort.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case ThemeIndexSchema.NAME:
            query.sortByName(criterion.getOrder());
            break;
          case ThemeIndexSchema.DESCRIPTION:
            query.sortByDescription(criterion.getOrder());
            break;
          case ThemeIndexSchema.CREATOR:
            query.sortByCreator(criterion.getOrder());
            break;
          case ThemeIndexSchema.DEFAULT:
            query.sortByDefault(criterion.getOrder());
            break;
          case ThemeIndexSchema.CREATION_DATE:
            query.sortByCreatedDateTime(criterion.getOrder());
            break;
          default:
            logger.info("Unknown sort criteria {}", criterion.getFieldName());
            return Response.status(SC_BAD_REQUEST).build();
        }
      }
    }

    logger.trace("Using Query: " + query.toString());

    SearchResult<IndexTheme> results = null;
    try {
      results = searchIndex.getByQuery(query);
    } catch (SearchIndexException e) {
      logger.error("The admin UI Search Index was not able to get the themes list:", e);
      return RestUtil.R.serverError();
    }

    List<JValue> themesJSON = new ArrayList<JValue>();

    // If the results list if empty, we return already a response.
    if (results.getPageSize() == 0) {
      logger.debug("No themes match the given filters.");
      return okJsonList(themesJSON, nul(offset).getOr(0), nul(limit).getOr(0), 0);
    }

    for (SearchResultItem<IndexTheme> item : results.getItems()) {
      IndexTheme theme = item.getSource();
      themesJSON.add(themeToJSON(theme, true));
    }

    return okJsonList(themesJSON, nul(offset).getOr(0), nul(limit).getOr(0), results.getHitCount());
  }

  /**
   * Returns the JSON representation of this theme.
   *
   * @param theme
   *          the theme
   * @param editResponse
   *          whether the returning representation should contain edit information
   * @return the JSON representation of this theme.
   */
  private JValue themeToJSON(IndexTheme theme, boolean editResponse) {
    List<Field> fields = new ArrayList<Field>();
    fields.add(f("id", v(theme.getIdentifier())));
    fields.add(f("creationDate", v(DateTimeSupport.toUTC(theme.getCreationDate().getTime()))));
    fields.add(f("default", v(theme.isDefault())));
    fields.add(f("name", v(theme.getName())));
    fields.add(f("creator", v(theme.getCreator())));
    fields.add(f("description", v(theme.getDescription(), Jsons.BLANK)));
    fields.add(f("bumperActive", v(theme.isBumperActive())));
    fields.add(f("bumperFile", v(theme.getBumperFile(), Jsons.BLANK)));
    fields.add(f("trailerActive", v(theme.isTrailerActive())));
    fields.add(f("trailerFile", v(theme.getTrailerFile(), Jsons.BLANK)));
    fields.add(f("titleSlideActive", v(theme.isTitleSlideActive())));
    fields.add(f("titleSlideMetadata", v(theme.getTitleSlideMetadata(), Jsons.BLANK)));
    fields.add(f("titleSlideBackground", v(theme.getTitleSlideBackground(), Jsons.BLANK)));
    fields.add(f("licenseSlideActive", v(theme.isLicenseSlideActive())));
    fields.add(f("licenseSlideDescription", v(theme.getLicenseSlideDescription(), Jsons.BLANK)));
    fields.add(f("licenseSlideBackground", v(theme.getLicenseSlideBackground(), Jsons.BLANK)));
    fields.add(f("watermarkActive", v(theme.isWatermarkActive())));
    fields.add(f("watermarkFile", v(theme.getWatermarkFile(), Jsons.BLANK)));
    fields.add(f("watermarkPosition", v(theme.getWatermarkPosition(), Jsons.BLANK)));
    if (editResponse) {
      extendStaticFileInfo("bumperFile", theme.getBumperFile(), fields);
      extendStaticFileInfo("trailerFile", theme.getTrailerFile(), fields);
      extendStaticFileInfo("titleSlideBackground", theme.getTitleSlideBackground(), fields);
      extendStaticFileInfo("licenseSlideBackground", theme.getLicenseSlideBackground(), fields);
      extendStaticFileInfo("watermarkFile", theme.getWatermarkFile(), fields);
    }
    return obj(fields);
  }

  private void extendStaticFileInfo(String fieldName, String staticFileId, List<Field> fields) {
    if (StringUtils.isNotBlank(staticFileId)) {
      try {
        fields.add(f(fieldName.concat("Name"), v(staticFileService.getFileName(staticFileId))));
        fields.add(f(fieldName.concat("Url"), v(staticFileRestService.getStaticFileURL(staticFileId).toString(),
            Jsons.BLANK)));
      } catch (IllegalStateException | NotFoundException e) {
        logger.error("Error retreiving static file '{}' ", staticFileId, e);
      }
    }
  }

  @POST
  @Path("themes/new")
  @RestQuery(name = "createTheme", description = "Add a theme", returnDescription = "Return the created theme",
          restParameters = {
          @RestParameter(name = "id", description = "The theme identifier", isRequired = true, type = Type.STRING),
          @RestParameter(name = "date", description = "The creation date", isRequired = true, type = Type.STRING),
          @RestParameter(name = "creator", description = "The creator", isRequired = true, type = Type.STRING),
          @RestParameter(name = "default", description = "Whether the theme is default", isRequired = true,
                  type = Type.BOOLEAN),
          @RestParameter(name = "name", description = "The theme name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The theme description", isRequired = false,
                  type = Type.TEXT),
          @RestParameter(name = "bumperActive", description = "Whether the theme bumper is active", isRequired = false,
                  type = Type.BOOLEAN),
          @RestParameter(name = "trailerActive", description = "Whether the theme trailer is active",
                  isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "titleSlideActive", description = "Whether the theme title slide is active",
                  isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "licenseSlideActive", description = "Whether the theme license slide is active",
                  isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "watermarkActive", description = "Whether the theme watermark is active",
                  isRequired = false, type = Type.BOOLEAN),
          @RestParameter(name = "bumperFile", description = "The theme bumper file", isRequired = false,
                  type = Type.STRING),
          @RestParameter(name = "trailerFile", description = "The theme trailer file", isRequired = false,
                  type = Type.STRING),
          @RestParameter(name = "watermarkFile", description = "The theme watermark file", isRequired = false,
                  type = Type.STRING),
          @RestParameter(name = "titleSlideBackground", description = "The theme title slide background file",
                  isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideBackground", description = "The theme license slide background file",
                  isRequired = false, type = Type.STRING),
          @RestParameter(name = "titleSlideMetadata", description = "The theme title slide metadata",
                  isRequired = false, type = Type.STRING),
          @RestParameter(name = "licenseSlideDescription", description = "The theme license slide description",
                  isRequired = false, type = Type.STRING),
          @RestParameter(name = "watermarkPosition", description = "The theme watermark position", isRequired = false,
                  type = Type.STRING), }, responses = {
          @RestResponse(responseCode = SC_OK, description = "Theme created"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The theme references a non-existing file") })
  public Response createTheme(@FormParam("id") String id, @FormParam("date") String date,
                              @FormParam("creator") String creator,
                              @FormParam("default") boolean isDefault, @FormParam("name") String name,
                              @FormParam("description") String description,
                              @FormParam("bumperActive") Boolean bumperActive,
                              @FormParam("trailerActive") Boolean trailerActive,
                              @FormParam("titleSlideActive") Boolean titleSlideActive,
                              @FormParam("licenseSlideActive") Boolean licenseSlideActive,
                              @FormParam("watermarkActive") Boolean watermarkActive,
                              @FormParam("bumperFile") String bumperFile,
                              @FormParam("trailerFile") String trailerFile,
                              @FormParam("watermarkFile") String watermarkFile,
                              @FormParam("titleSlideBackground") String titleSlideBackground,
                              @FormParam("licenseSlideBackground") String licenseSlideBackground,
                              @FormParam("titleSlideMetadata") String titleSlideMetadata,
                              @FormParam("licenseSlideDescription") String licenseSlideDescription,
                              @FormParam("watermarkPosition") String watermarkPosition) throws ParseException {
    User user = userDirectoryService.loadUser(creator);
    if (user == null) {
      user = securityService.getUser();
    }
    Long identifier = Long.parseLong(id);
    Theme theme = new Theme(Option.some(identifier), new Date(DateTimeSupport.fromUTC(date)), isDefault, user, name,
            StringUtils.trimToNull(description), BooleanUtils.toBoolean(bumperActive),
            StringUtils.trimToNull(bumperFile), BooleanUtils.toBoolean(trailerActive),
            StringUtils.trimToNull(trailerFile), BooleanUtils.toBoolean(titleSlideActive),
            StringUtils.trimToNull(titleSlideMetadata), StringUtils.trimToNull(titleSlideBackground),
            BooleanUtils.toBoolean(licenseSlideActive), StringUtils.trimToNull(licenseSlideBackground),
            StringUtils.trimToNull(licenseSlideDescription), BooleanUtils.toBoolean(watermarkActive),
            StringUtils.trimToNull(watermarkFile), StringUtils.trimToNull(watermarkPosition));

    try {
      if (isNotBlank(theme.getBumperFile())) {
        staticFileService.persistFile(theme.getBumperFile());
      }
      if (isNotBlank(theme.getLicenseSlideBackground())) {
        staticFileService.persistFile(theme.getLicenseSlideBackground());
      }
      if (isNotBlank(theme.getTitleSlideBackground())) {
        staticFileService.persistFile(theme.getTitleSlideBackground());
      }
      if (isNotBlank(theme.getTrailerFile())) {
        staticFileService.persistFile(theme.getTrailerFile());
      }
      if (isNotBlank(theme.getWatermarkFile())) {
        staticFileService.persistFile(theme.getWatermarkFile());
      }
    } catch (NotFoundException e) {
      logger.warn("A file that is referenced in theme '{}' was not found: {}", theme, e.getMessage());
      return RestUtil.R.badRequest("Referenced non-existing file");
    } catch (IOException e) {
      logger.warn("Error while persisting file: {}", e.getMessage());
      return RestUtil.R.serverError();
    }

    try {
      themesServiceDatabase.updateTheme(theme);
      return RestUtil.R.ok();
    } catch (ThemesServiceDatabaseException e) {
      logger.error("Unable to create a theme");
      return RestUtil.R.serverError();
    }
  }
}
