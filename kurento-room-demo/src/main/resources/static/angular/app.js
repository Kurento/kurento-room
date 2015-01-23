var kurento_room = angular.module('kurento_room', ['ngRoute','FBAngular','lumx']);

kurento_room.config(function ($routeProvider) {

    $routeProvider
            .when('/', {
                templateUrl: 'angular/login.html',
                controller: 'loginController'
            })
            .when('/login', {
                templateUrl: 'angular/login.html',
                controller: 'loginController'
            })
            .when('/call', {
                templateUrl: 'angular/call.html',
                controller: 'callController'
            });
//            .otherwise({
//                templateUrl: 'error.html',
//                controller: 'MainController',
//            });
});

//kurento_room.controller('MainController', function ($scope) {
//
//
//});



