'use strict';

techLabApp.controller('RoutePlannerController',
    function RoutePlannerController($scope, transportStops, journeyPlanner, $location, journeyPlanService) {
        $scope.selectedStop = null;
        $scope.departureTime = getCurrentTime();
        $scope.fromStop = journeyPlanService.getStart();
        $scope.toStop = journeyPlanService.getEnd();

        journeyPlanService.removePlan();

        $scope.findRoute = function (fromStop, toStop, departureTime, journeyPlanForm) {
            if (journeyPlanForm.$valid) {
                var time = moment(departureTime).format('HH:mm');
                var date = moment(departureTime).format('YYYY-MM-DD');
                $location.url('/routeDetails?start=' + fromStop +
                    '&end=' + toStop +
                    '&departureTime=' + time +
                        '&departureDate=' + date
                );
            }
        };

        $scope.fromStopSelected = function() {
           $scope.filterDestinationStop($scope.fromStop);
        };

        $scope.filterDestinationStop = function(selectedId) {
            if (selectedId==null) {
                $scope.endStops = $scope.stops;
            } else {
                $scope.endStops = new Array();
                if ($scope.fromStop != null) {
                    angular.forEach($scope.stops, function (stop) {
                        if (stop.id != selectedId) {
                            $scope.endStops.push(stop);
                        }
                    });
                }
            }
        };

        $scope.groupFilter = function (item) {
            return item.proximityGroup === 'Nearest Stops' || item.proximityGroup === 'All Stops';
        };

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getNearStops, positionError,
                {maximumAge: 600000, // 10 minutes cached time on location
                    timeout: 20000  // by default this call never times out...
                });
        } else {
            // can't get position
            console.log("Unable to get current position");
            getAllStops();
        }

        function positionError(error) {
            console.log(error);
            getAllStops();
        }

        function getNearStops(position) {
            transportStops.getNearStops(position.coords.latitude, position.coords.longitude).query(function (stopList) {
                $scope.stops = stopList;
                $scope.filterDestinationStop($scope.fromStop);
            });
        }

        function getAllStops() {
            transportStops.getAll().query(function (stopList) {
                $scope.stops = stopList;
                $scope.filterDestinationStop($scope.fromStop);
                tramchesterCacheService.addStops("All", stopList);
            });
        }

        function getCurrentTime() {
            var currentDate = new Date();
            return  new Date(currentDate.getUTCFullYear(), currentDate.getUTCMonth(), currentDate.getUTCDate(),
                currentDate.getHours(), currentDate.getMinutes(), 0, 0);
        }
    });
