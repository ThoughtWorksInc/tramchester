
var moment = require('moment');
const axios = require('axios');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import './../css/tramchester.css'

import Notes from "./components/Notes";
import Journeys from './components/Journeys';
import Footer from './components/Footer';
import LiveDepartures from './components/LiveDepatures'
import LocationSelection from './components/LocationSelection';
import Closures from './components/Closures'

const dateFormat = "YYYY-MM-DD";

function getCurrentTime() {
    return moment().local().format("HH:mm");
}

function getCurrentDate() {
    return moment().format(dateFormat)
}

function livedataUrlFromLocation() {
    var place = app.location; // should not have location place holder without a valid location
    return '/api/departures/' + place.coords.latitude + '/' + place.coords.longitude;
}

function livedataUrl() {
    if (app.startStop==null || app.startStop==='MyLocationPlaceholderId') {
        return livedataUrlFromLocation(app)+'?querytime='+app.time;
    } else {
        return '/api/departures/station/'+app.startStop+'?querytime='+app.time;
    }
}

function displayLiveData(app) {
    if (app.feedinfo.bus) {
        // only live data for trams
        if ( ! app.startStop.startsWith('9400ZZ')) {
            app.liveDepartureResponse = null;
            return; 
        }
    }
    var queryDate = moment(app.date, dateFormat);
    var today = moment();
    // check live data for today only 
    if (today.month()==queryDate.month()
        && today.year()==queryDate.year()
        && today.date()==queryDate.date()) {
        queryLiveData(livedataUrl());
    }
}

function queryLiveData(url) {
 axios.get( url, { timeout: 11000 }).
            then(function (response) {
                app.liveDepartureResponse = response.data;
                app.networkError = false;
                app.liveInProgress = false;
            }).
            catch(function (error) {
                app.liveInProgress = false;
                reportError(error);
            });
}

function getStationsFromServer(app) {
    axios
         .get('/api/stations/all', { timeout: 30000}) /// potential size of data means timeout neeeded here
         .then(function (response) {
             app.networkError = false;
             app.stops.allStops = response.data;
             app.ready = true;
         })
         .catch(function (error) {
            app.ready = true;
            reportError(error);
         });
    getRecentAndNearest(app);
 }

function getRecentAndNearest(app) {
    axios
        .get('/api/stations/recent')
        .then(function (response) {
            app.networkError = false;
            app.stops.recentStops = response.data;
            app.ready = true;
        })
        .catch(function (error) {
            app.ready = true;
            reportError(error);
        });
    if (app.hasGeo && app.location!=null) {
        var place = app.location;
        const url = '/api/stations/near/?lat=' + place.coords.latitude + '&lon=' + place.coords.longitude;
        axios
            .get(url)
            .then(function (response) {
                app.networkError = false;
                app.stops.nearestStops = response.data;
                app.ready = true;
            })
            .catch(function (error) {
                app.ready = true;
                reportError(error);
            });
    }
}

 function loadPostcodes(app) {
     if (app.feedinfo.bus) {
        axios.get("/api/postcodes").then(function (response) {
            app.networkError = false;
            addPostcodes(response.data);
        }).catch(function (error){
            reportError(error);
        });
    }
}

// // TODO app needed passed in for some browsers?
// function addPostcodes(postcodes) {
//     app.stops = app.stops.concat(postcodes);
// }

 function queryServerForJourneys(app, startStop, endStop, time, date, arriveBy, changes) {
    var urlParams = {
        start: startStop, end: endStop, departureTime: time, departureDate: date, 
        arriveby: arriveBy, maxChanges: changes
    };
    if (startStop == 'MyLocationPlaceholderId' || endStop == 'MyLocationPlaceholderId') {
        const place = app.location;
        urlParams.lat = place.coords.latitude;
        urlParams.lon = place.coords.longitude;
    }
    axios.get('/api/journey/', { params: urlParams, timeout: 60000 }).
        then(function (response) {
            app.networkError = false;
            app.journeys = response.data.journeys;
            getRecentAndNearest(app);
            app.searchInProgress = false;
        }).
        catch(function (error) {
            app.ready = true;
            app.searchInProgress = false;
            reportError(error);
        });
}

 function reportError(error) {
    app.networkError = true;
    console.log(error.message);
    console.log("File: " + error.fileName);
    console.log("Line:" + error.lineNumber);
    if (error.request!=null) {
        console.log("URL: " + error.request.responseURL);
    }

 }

 var data = {
    ready: false,                   // ready to respond
    stops: {
        allStops: [],
        nearestStops: [],
        recentStops: []
    },
    startStop: null,
    endStop: null,
    arriveBy: false,
    time: getCurrentTime(),
    date: getCurrentDate(),
    maxChanges: 3,                  // todo from server side
    journeys: null,
    liveDepartureResponse: null,
    feedinfo: [],
    searchInProgress: false,    // searching for routes
    liveInProgress: false,      // looking for live data
    networkError: false,        // network error on either query
    hasGeo: false,
    location: null
}

var app = new Vue({
        el: '#journeyplan',
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'app-footer' : Footer,
            'live-departures' : LiveDepartures,
            'location-selection': LocationSelection,
            'closures' : Closures
        },
        methods: {
            plan(event){
                if (event!=null) {
                    event.preventDefault(); // stop page reload on form submission
                }
                app.searchInProgress = true;
                app.ready = false;
                this.$nextTick(function () {
                    app.queryServer();
                });
            },
            changeTime(newTime) {
                app.time = newTime;
                app.plan(null);
            },
            networkErrorOccured() {
                app.networkError = true;
            },
            queryNearbyTrams() {
                app.liveInProgress = true;
                this.$nextTick(function () {
                    queryLiveData(livedataUrlFromLocation()+'?notes=1');
                });
            },
            queryServer() {
                queryServerForJourneys(app, this.startStop, this.endStop, this.time,
                    this.date, this.arriveBy, this.maxChanges);
                displayLiveData(app);
            },
            setCookie() {
                var cookie = { 'visited' : true };
                this.$cookies.set("tramchesterVisited", cookie, "128d", "/", null, false, "Strict");
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            },
            swap() {
                let temp = app.endStop;
                app.endStop = app.startStop;
                app.startStop = temp;
            }
        },
        mounted () {
            var cookie = this.$cookies.get("tramchesterVisited");
            if (cookie==null) {
                this.$refs.cookieModal.show();
            }
            axios.get('/api/datainfo')
                .then(function (response) {
                    app.networkError = false;
                    app.feedinfo = response.data;
                })
                .catch(function (error) {
                    reportError(error);
                });
            if (this.hasGeo) {
                navigator.geolocation.getCurrentPosition(pos => {
                      this.location = pos;
                      getStationsFromServer(this);
                }, err => {
                      this.location = null;
                      getStationsFromServer(this);
                })
            } else {
                getStationsFromServer(this);
            }

        },
        created() {
            if("geolocation" in navigator) {
                this.hasGeo = true;
            }
        },
        computed: {
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            },
            busEnabled: function () {
                return this.feedinfo.bus;
            }
        }
    })




