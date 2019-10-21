'use strict';

techLabApp.factory('journeyPlanner', function($resource){
    return {
        quickestRoute: function(fromStop, toStop, departureTime, departureDate, lat, lon){
            departureTime = departureTime + ":00";
            if (fromStop=='MyLocationPlaceholderId') {
                return $resource('/api/journey',
                    {start: fromStop, end: toStop, departureTime: departureTime,
                        departureDate: departureDate, lat: lat,lon: lon});
            } else {
                return $resource('/api/journey',
                    {start: fromStop, end: toStop, departureTime: departureTime, departureDate: departureDate});
            }
        }
    };
});
