/*
 * @author Raquel Díaz González
 */

var kurento_room = angular.module('kurento_room', ['ngRoute', 'FBAngular', 'lumx']);

kurento_room.config(function ($routeProvider) {

    $routeProvider
            .when('/', {
                templateUrl: 'angular/login/login.html',
                controller: 'loginController'
            })
            .when('/login', {
                templateUrl: 'angular/login/login.html',
                controller: 'loginController'
            })
            .when('/call', {
                templateUrl: 'angular/call/call.html',
                controller: 'callController'
            });
//            .otherwise({
//                templateUrl: 'error.html',
//                controller: 'MainController',
//            });
});



