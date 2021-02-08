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
var switchAcls = [
  {
    id: 1,
    template: 'public',
    name: 'SWITCHCAST.ACL.TEMPLATE.PUBLIC.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.PUBLIC.DESCRIPTION',
    role: 'ROLE_ANONYMOUS',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role:'ROLE_ANONYMOUS'
      },
      {
        action: 'cast-discover',
        allow: true,
        role:'ROLE_ANONYMOUS'
      },
      {
        action: 'read',
        allow: false,
        role: 'ROLE_ANONYMOUS'
      },
      {
        action: 'write',
        allow: false,
        role: 'ROLE_ANONYMOUS'
      }
    ]
  },
  {
    id: 2,
    template: 'federation',
    name: 'SWITCHCAST.ACL.TEMPLATE.FEDERATION.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.FEDERATION.DESCRIPTION',
    role: 'ROLE_AAI_FEDERATION_MEMBER',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role: 'ROLE_AAI_FEDERATION_MEMBER'
      },
      {
        action: 'cast-discover',
        allow: true,
        role: 'ROLE_AAI_FEDERATION_MEMBER'
      },
      {
        action: 'read',
        allow: false,
        role: 'ROLE_AAI_FEDERATION_MEMBER'
      },
      {
        action: 'write',
        allow: false,
        role: 'ROLE_AAI_FEDERATION_MEMBER'
      }
    ]
  },
  {
    id: 3,
    template: 'organization',
    name: 'SWITCHCAST.ACL.TEMPLATE.ORGANIZATION.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.ORGANIZATION.DESCRIPTION',
    role: 'ROLE_AAI_ORG_<org-domain>_MEMBER',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role: 'ROLE_AAI_ORG_<org-domain>_MEMBER'
      },
      {
        action: 'cast-discover',
        allow: true,
        role: 'ROLE_AAI_ORG_<org-domain>_MEMBER'
      },
      {
        action: 'read',
        allow: false,
        role: 'ROLE_AAI_ORG_<org-domain>_MEMBER'
      },
      {
        action: 'write',
        allow: false,
        role: 'ROLE_AAI_ORG_<org-domain>_MEMBER'
      }
    ]
  },
  {
    id: 4,
    template: 'private',
    name: 'SWITCHCAST.ACL.TEMPLATE.PRIVATE.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.PRIVATE.DESCRIPTION',
    role: 'ROLE_AAI_PRIVATE_MEMBER',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role: 'ROLE_AAI_PRIVATE_MEMBER'
      },
      {
        action: 'cast-discover',
        allow: true,
        role: 'ROLE_AAI_PRIVATE_MEMBER'
      },
      {
        action: 'read',
        allow: false,
        role: 'ROLE_AAI_PRIVATE_MEMBER'
      },
      {
        action: 'write',
        allow: false,
        role: 'ROLE_AAI_PRIVATE_MEMBER'
      }
    ]
  },
  {
    id: 5,
    template: 'lms',
    name: 'SWITCHCAST.ACL.TEMPLATE.LMS.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.LMS.DESCRIPTION',
    role: 'ROLE_EXTERNAL_APPLICATION',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role: 'ROLE_EXTERNAL_APPLICATION'
      },
      {
        action: 'read',
        allow: true,
        role: 'ROLE_EXTERNAL_APPLICATION'
      },
      {
        action: 'write',
        allow: true,
        role: 'ROLE_EXTERNAL_APPLICATION'
      }
    ]
  },
  {
    id: 6,
    template: 'hidden',
    name: 'SWITCHCAST.ACL.TEMPLATE.HIDDEN.NAME',
    description: 'SWITCHCAST.ACL.TEMPLATE.HIDDEN.DESCRIPTION',
    role: 'ROLE_ANONYMOUS',
    acls: [
      {
        action: 'cast-view',
        allow: true,
        role: 'ROLE_ANONYMOUS'
      },
      {
        action: 'cast-discover',
        allow: false,
        role: 'ROLE_ANONYMOUS'
      },
      {
        action: 'read',
        allow: false,
        role: 'ROLE_ANONYMOUS'
      },
      {
        action: 'write',
        allow: false,
        role: 'ROLE_ANONYMOUS'
      }
    ]
  },
];

var customAcl = {
  id: 7,
  template: 'custom',
  name: 'SWITCHCAST.ACL.TEMPLATE.CUSTOM.NAME',
  description: 'SWITCHCAST.ACL.TEMPLATE.CUSTOM.DESCRIPTION',
  acls: [
  ]
};

angular.module('adminNg.services')
.constant('switchAcls', switchAcls)
.constant('customAcl', customAcl)

.factory('NewSeriesAccessSwitch', ['$http', 'ResourcesListResource', 'SeriesAccessResource', 'AuthService',
  'UserRolesResource', 'Notifications', '$timeout', '$q','$filter',
  function ($http, ResourcesListResource, SeriesAccessResource, AuthService, UserRolesResource, Notifications, $timeout,
    $q, $filter) {
    var Access = function () {
      var roleSlice = 100;
      var roleOffset = 0;
      var loading = false;
      var rolePromise = null;

      var me = this,
          NOTIFICATION_CONTEXT = 'series-acl',
          aclNotification,
          createPolicy = function (role) {
            return {
              role  : role,
              read  : false,
              write : false,
              actions : {
                name : 'new-series-acl-actions',
                value : []
              }
            };
          },
          checkNotification = function () {
            if (me.unvalidRule) {
              if (!angular.isUndefined(me.notificationRules)) {
                Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
              }
              me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRules)) {
              Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
              me.notificationRules = undefined;
            }

            if (!me.hasRights) {
              if (!angular.isUndefined(me.notificationRights)) {
                Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
              }
              me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRights)) {
              Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
              me.notificationRights = undefined;
            }

            $timeout(function () {
              checkNotification();
            }, 200);
          },
          addUserRolePolicy = function (policies) {
            if (angular.isDefined(AuthService.getUserRole())) {
              var currentUserPolicy = createPolicy(AuthService.getUserRole());
              currentUserPolicy.read = true;
              currentUserPolicy.write = true;
              policies.push(currentUserPolicy);
            }
            return policies;
          };
      me.ud = {};
      me.ud.id = {};
      me.ud.policies = [];
      // Add the current user's role to the ACL upon the first startup
      me.ud.policies = addUserRolePolicy(me.ud.policies);
      me.ud.switchrole = {};

      me.usersList = [];

      $http({
        method: 'GET',
        url: '/admin-ng/resources/USERS.SWITCH.ROLE.json'
      }).then(function successCallback(response) {
        Object.keys(response.data).forEach(function(key,index) {
          me.usersList.push({
            key: key,
            name: key,
            value: response.data[key]
          });
        });
      });

      me.tags = [];

      me.loadTags = function(query){
        return me.usersList.filter(function(user){
          return user.name.toLowerCase().indexOf(query.toLowerCase()) != -1;
        });
      };

      me.removeUserTag = function(tag) {
        var policy = createPolicy(tag.value);
        policy.read = true;
        policy.write = true;
        me.deletePolicy(policy);
      };

      me.addUserTag = function(tag){
        var policy = createPolicy(tag.value);
        policy.read = true;
        policy.write = true;
        me.ud.policies.push(policy);
      };


      this.changeBaseAcl = function (preSet) {
        if(me.ud.aclSelected.template.id) { me.ud.switchrole = me.ud.aclSelected.template.id; }
        if(me.ud.switchrole) {
          AuthService.getUser().$promise.then(function (user) {
            var orgProperties = user.org.properties;
            //Variables needed to determine an event's start time
            var aaiOrg = orgProperties['aai.org'] ;

            var newPolicies = {};
            // get the selected switchacl object
            // we are aware that me.ud.id.id is ugly but if we do it that
            // way we dont need to change in the community version and can leave
            // all the changes in this file.
            var selectedTemplate;
            if(!preSet){
              selectedTemplate = $filter('filter')(switchAcls, {id: me.ud.switchrole});
              me.ud.aclSelected.template = selectedTemplate[0];
            }
            var selectedAcls = angular.copy(me.ud.aclSelected.template.acls);

            if(me.ud.allowDownload){
              selectedAcls.push({
                action: 'cast-download',
                allow: true,
                role: me.ud.aclSelected.template.role
              });
            }
            if(me.ud.allowAnnotate){
              selectedAcls.push({
                action: 'cast-annotate',
                allow: true,
                role: me.ud.aclSelected.template.role
              });
            }
            angular.forEach(selectedAcls, function (acl) {
              if (acl.role.indexOf('<org-domain>') != -1){
                acl.role = acl.role.replace('<org-domain>', aaiOrg);
              }
              var policy = newPolicies[acl.role];
              if (angular.isUndefined(policy)) {
                newPolicies[acl.role] = createPolicy(acl.role);
              }
              if (acl.action === 'read' || acl.action === 'write') {
                newPolicies[acl.role][acl.action] = acl.allow;
              } else if (acl.allow === true || acl.allow === 'true'){
                newPolicies[acl.role].actions.value.push(acl.action);
              }
            });

            me.ud.policies = [];

            // After loading an ACL template add the user's role to the top of the ACL list if it isn't included
            if (angular.isDefined(AuthService.getUserRole())
              && !angular.isDefined(newPolicies[AuthService.getUserRole()])) {
              me.ud.policies = addUserRolePolicy(me.ud.policies);
            }

            angular.forEach(newPolicies, function (policy) {
              me.ud.policies.push(policy);
            });
            // TBD SWITCH: This causes the description no being displayed when changing the template in the wizard
            //             if(!preSet){
            //                 me.ud.aclSelected.template = '';
            //             }

          });
        }
      };
      me.changeBaseAcl = this.changeBaseAcl;

      this.addPolicy = function () {
        me.ud.policies.push(createPolicy());
      };

      this.deletePolicy = function (policyToDelete) {
        var index;

        angular.forEach(me.ud.policies, function (policy, idx) {
          if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
            index = idx;
          }
        });

        if (angular.isDefined(index)) {
          me.ud.policies.splice(index, 1);
        }
      };

      this.isValid = function () {
        var hasRights = false,
            rulesValid = true;

        angular.forEach(me.ud.policies, function (policy) {
          rulesValid = false;

          if (policy.read && policy.write) {
            hasRights = true;
          }

          if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
            rulesValid = true;
          }
        });

        me.unvalidRule = !rulesValid;
        me.hasRights = hasRights;

        if (hasRights && angular.isDefined(aclNotification)) {
          Notifications.remove(aclNotification, 'series-acl');
        }

        if (!hasRights && !angular.isDefined(aclNotification)) {
          aclNotification = Notifications.add('warning', 'SERIES_ACL_MISSING_READWRITE_ROLE', 'series-acl', -1);
        }

        return rulesValid && hasRights;
      };

      checkNotification();
      me.acls = switchAcls;

      me.actions = {};
      me.hasActions = false;
      ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
        angular.forEach(data, function (value, key) {
          if (key.charAt(0) !== '$') {
            me.actions[key] = value;
            me.hasActions = true;
          }
        });
      });

      me.roles = {};

      me.getMoreRoles = function (value) {

        if (me.loading)
          return rolePromise;

        me.loading = true;

        // the offset should actually be setto roleOffset, but when used doesn't display the correct roles
        var queryParams = {limit: roleSlice, offset: 0};

        if ( angular.isDefined(value) && (value != '')) {
          //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
          //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
          queryParams['filter'] = 'role_name:' + value + ',role_target:ACL';
          queryParams['offset'] = 0;
        } else {
          queryParams['filter'] = 'role_target:ACL';
        }
        rolePromise = UserRolesResource.query(queryParams);
        rolePromise.$promise.then(function (data) {
          angular.forEach(data, function (role) {
            me.roles[role.name] = role.value;
          });
          roleOffset = Object.keys(me.roles).length;
        }).finally(function () {
          me.loading = false;
        });
        return rolePromise;
      };

      me.getMoreRoles();

      this.reset = function () {
        AuthService.getUser().$promise.then(function (user) {
          var orgProperties = user.org.properties;
          var defaultTemplate = orgProperties['switch.acl.default_template'];
          var defaultDownload = orgProperties['switch.acl.default_download'] === 'true';
          var defaultAnnotate = orgProperties['switch.acl.default_annotate'] === 'true';
          var selectedTemplate = $filter('filter')(switchAcls, {template: defaultTemplate});
          me.ud.aclSelected = {template: selectedTemplate[0]};
          me.ud.allowDownload = defaultDownload;
          me.ud.allowAnnotate = defaultAnnotate;
          me.ud.policies = addUserRolePolicy(me.ud.policies);
          me.changeBaseAcl(true);
        });

        me.ud = {
          id: {},
          policies: []
        };
        me.tags = [];
        // Add the user's role upon resetting
        me.ud.policies = addUserRolePolicy(me.ud.policies);
      };

      this.reload = function () {
        //me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.roles = {};
        me.roleOffset = 0;
        me.getMoreRoles();
      };

      this.reset();
    };

    return new Access();
  }]);
