/*
 * Licensed under the Apache License, Version 2.0
 * See accompanying LICENSE file.
 */

(function() {

  // rootPath of this site, it has a tailing slash /
  var rootPath = function() {
    var root = location.origin + location.pathname;
    return root.substring(0, root.lastIndexOf("/") + 1);
  }();

  angular.module('dashboard', [
    'ngAnimate',
    'ngSanitize',
    'ngCookies',
    'ngTouch',
    'mgcrea.ngStrap',
    'ui.router',
    'ui.select',
    'cfp.loadingBarInterceptor',
    'ngFileUpload',
    'dashing',
    'io.gearpump.models'
  ])

  // configure routes
  .config(['$stateProvider', '$urlRouterProvider',
    function($stateProvider, $urlRouterProvider) {
      'use strict';

      $urlRouterProvider
        .when('', '/')
        .when('/', '/cluster')
        .when('/cluster', '/cluster/master');

      $stateProvider
        .state('cluster', {
          abstract: true, // todo: we have a sidebar for cluster only
          url: '/cluster',
          templateUrl: 'views/cluster/overview.html'
        });
      // Please check every controller for corresponding state definition
    }])

  // configure loading bar effect
  .config(['cfpLoadingBarProvider', function(cfpLoadingBarProvider) {
    'use strict';

    cfpLoadingBarProvider.includeSpinner = false;
    cfpLoadingBarProvider.latencyThreshold = 1000;
  }])

  // configure angular-strap
  .config(['$tooltipProvider', function($tooltipProvider) {
    'use strict';

    angular.extend($tooltipProvider.defaults, {
      html: true
    });
  }])

  // disable logging for production
  .config(['$compileProvider', function($compileProvider) {
    'use strict';

    $compileProvider.debugInfoEnabled(false);
  }])

  // constants
  .constant('conf', {
    restapiProtocol: 'v1.0',
    restapiRoot: rootPath,
    restapiQueryInterval: 3 * 1000, // in milliseconds
    restapiQueryTimeout: 30 * 1000, // in milliseconds
    restapiTaskLevelMetricsQueryLimit: 100,
    loginUrl: rootPath + 'login'
  })

  /* add a retry delay for angular-ui-router, when resolving a data is failed */
  .run(['$rootScope', '$state', 'conf', function($rootScope, $state, conf) {
    'use strict';

    $rootScope.$on('$stateChangeError', function(event, toState) {
      event.preventDefault();
      _.delay($state.go, conf.restapiQueryTimeout, toState);
    });
  }])

  /* enable a health check service */
  .run(['$modal', 'HealthCheckService', 'conf', function($modal, HealthCheckService, conf) {
    'use strict';

    var dialog = $modal({
      templateUrl: 'views/service_unreachable_notice.html',
      backdrop: 'static',
      show: false
    });

    var showDialogFn = function() {
      dialog.$promise.then(dialog.show);
    };

    var hideDialogFn = function() {
      // simply refresh the page, to make sure page status is fresh
      location.reload();
    };

    HealthCheckService.config(
      conf.restapiRoot + 'version',
      conf.restapiQueryInterval,
      showDialogFn,
      hideDialogFn
    );
    HealthCheckService.checkForever();
  }]);
})();