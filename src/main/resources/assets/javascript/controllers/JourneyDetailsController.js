'use strict';

techLabApp.controller('JourneyDetailsController',
    function JourneyDetailsController(journeyPlanService, $scope, $routeParams, $location) {
        $scope.journey = journeyPlanService.getJourney($routeParams.journeyIndex);
        $scope.serviceTimeIndex = 0;
        $scope.disableNextTram = false;
        $scope.disablePreviousTram = true;
        $scope.showChangeIndicator = false;
        console.log($scope.journey);
        $scope.showMap = function (stage) {
            if ($scope.journey.stages.indexOf(stage) == 0) {
                $location.url('/map?lat=' + stage.beginStop.lat + "&lon=" + stage.beginStop.lon + "&name=" + stage.beginStop.name + "&direction=1");
            } else {
                $location.url('/map?lat=' + stage.beginStop.lat + "&lon=" + stage.beginStop.lon + "&name=" + stage.beginStop.name + "&direction=0");
            }
        }

        $scope.goBack = function (journey) {
            if (journey != null) {
                $location.url('/routeDetails?start=' + journeyPlanService.getStart() + '&end=' + journeyPlanService.getEnd() + '&departureTime=' + journeyPlanService.getDepartureTime());
            } else {
                $location.url('/routePlanner');
            }
        }

        $scope.nextTram = function () {
            if ($scope.serviceTimeIndex < 4) {
                $scope.serviceTimeIndex++;
                $scope.disablePreviousTram = false;
            } else {
                $scope.disableNextTram = true;
            }
        }

        $scope.previousTram = function () {
            if ($scope.serviceTimeIndex > 0) {
                $scope.serviceTimeIndex--;
                $scope.disableNextTram = false;
            } else {
                $scope.disablePreviousTram = true;
            }
        }
    }
);