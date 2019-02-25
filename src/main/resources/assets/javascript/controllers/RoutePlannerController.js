'use strict';

techLabApp.controller('RoutePlannerController',
    function RoutePlannerController($scope, transportStops, journeyPlanner, $location, journeyPlanService, feedinfoService) {
        $scope.selectedStop = null;
        $scope.departureTime = getCurrentTime(); // in model
        $scope.departureDate = getCurrentDate(); // in model
        $scope.fromStop = journeyPlanService.getStart();
        $scope.toStop = journeyPlanService.getEnd();

        feedinfoService.getFeedInfo(function(info){
            $scope.validFrom = info.validFrom;
            $scope.validUntil = info.validUntil;
            $scope.enforceExpiry = false; // set false only if need to workaround bad metadata in feed_info.txt
        });

        journeyPlanService.removePlan();

        $scope.nearMe = function() {
            $location.url('/livenearme');
        };

        $scope.findRoute = function (fromStop, toStop, departureTime, departureDate, journeyPlanForm) {
            if (journeyPlanForm.$valid) {
                var time = moment(departureTime).format('HH:mm');
                var date = moment(departureDate).format('YYYY-MM-DD');
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
            return item.proximityGroup.name === 'Nearest Stops'
                || item.proximityGroup.name === 'All Stops'
                || item.proximityGroup.name === 'Recent';
        };

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getNearStops, positionError,
                {maximumAge: 600000, // 10 minutes cached time on location
                    timeout: 20000  // by default this call never times out...
                });
        } else {
            // can't get position
            console.warn("Unable to get current position");
            getAllStops();
        }

        function positionError(positionError) {
            console.warn("Unable to get current position")
            console.warn(positionError.message);
            getAllStops();
        }

        function getNearStops(position) {
            transportStops.getNearStops(position.coords.latitude, position.coords.longitude).get(function (stopList) {
                $scope.stops = stopList.stations;
                $scope.filterDestinationStop($scope.fromStop);
            });
        }

        function getAllStops() {
            transportStops.getAll().get(function (stopList) {
                $scope.stops = stopList.stations;
                $scope.filterDestinationStop($scope.fromStop);
            });
        }

        function getCurrentDate() {
            var current = journeyPlanService.getDepartureDate();
            if (current!=null) {
                var previous = moment(current,"YYYY-MM-DD");
                return previous.toDate();
            }
            var currentDate = new Date();
            return new Date(currentDate.getUTCFullYear(), currentDate.getUTCMonth(), currentDate.getUTCDate(),
                currentDate.getHours(), currentDate.getMinutes(), 0, 0);
        }

        function getCurrentTime() {
            var current = journeyPlanService.getDepartureTime(); // as HH:mm
            if (current!=null) {
                var previous = moment(current,"HH:mm");
                return previous.toDate();
            }
            return moment().second(0).millisecond(0).toDate(); // get time picker to display HH:mm
        }
    });
