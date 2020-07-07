
const axios = require('axios');

var moment = require('moment');
var Vue = require('vue');
Vue.use(require('bootstrap-vue'));
var oboe = require('oboe');

var L = require('leaflet');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';

const dateFormat = "YYYY-MM-DD";

function getCurrentDate() {
    return moment().format(dateFormat)
}

function addPostcodes() {
    var postcodeLayerGroup = L.featureGroup();

    mapApp.postcodes.forEach(postcode => {
        var lat = postcode.latLong.lat;
        var lon = postcode.latLong.lon;
        var marker = new L.circleMarker(L.latLng(lat,lon), {  radius: 1, title: postcode.name })
        postcodeLayerGroup.addLayer(marker);
    });

    postcodeLayerGroup.addTo(mapApp.map);
}

function addRoutes() {
    var routeLayerGroup = L.featureGroup();

    mapApp.routes.forEach(route => {
        var steps = [];
        route.stations.forEach(station => {
            steps.push([station.latLong.lat, station.latLong.lon]);
        })
        var line = L.polyline(steps, {className: route.displayClass});
        routeLayerGroup.addLayer(line);
    })

    routeLayerGroup.addTo(mapApp.map);
}

function addBoxWithCost(boxWithCost) {
    const bounds = [[boxWithCost.bottomLeft.lat, boxWithCost.bottomLeft.lon], 
        [boxWithCost.topRight.lat, boxWithCost.topRight.lon]];


    var colour = getColourForCost(boxWithCost);     
    var rectangle = L.rectangle(bounds, {weight: 1, fillColor: colour, fill: true, fillOpacity: 0.7});
    rectangle.addTo(mapApp.map);
}

function getColourForCost(boxWithCost) {
    if (boxWithCost.minutes < 0) {
        return "#ff0000";
    }
    var greenString = "00";
    if (boxWithCost.minutes > 0) {
        var red = Math.floor((255 / 112) * (112 - boxWithCost.minutes));
        greenString = red.toString(16);
        if (greenString.length == 1) {
            greenString = '0' + greenString;
        }
    }
    return '#00'+greenString+'00';
}

function addBoxs() {
    mapApp.grid.forEach(item => addBoxWithCost(item.BoxWithCost));
}

function queryForGrid(gridSize, destination, departureTime, departureDate, maxChanges, maxDuration) {
    var urlParams = {
        destination: destination, gridSize: gridSize, departureTime: departureTime, departureDate: departureDate, 
        maxChanges: maxChanges, maxDuration: maxDuration};

    const searchParams = new URLSearchParams(urlParams);

    oboe('/api/grid?'+searchParams.toString())
        .node('BoxWithCost', function(box) {
            addBoxWithCost(box)
        })
        .fail(function (errorReport) {
            console.log("Failed to load grid '" + errorReport.toString() + "'");
        });

    }

function findAndSetMapBounds() {
    let minLat = 1000;
    let maxLat = -1000;
    let minLon = 1000;
    let maxLon = -1000;
    mapApp.routes.forEach(route => {
        route.stations.forEach(position => {
            var lat = position.latLong.lat;
            if (lat < minLat) {
                minLat = lat;
            }
            else if (lat > maxLat) {
                maxLat = lat;
            }
            var lon = position.latLong.lon;
            if (lon < minLon) {
                minLon = lon;
            }
            else if (lon > maxLon) {
                maxLon = lon;
            }
        });
    })

    var corner1 = L.latLng(minLat, minLon);
    var corner2 = L.latLng(maxLat, maxLon);
    var bounds = L.latLngBounds(corner1, corner2);
    mapApp.map.fitBounds(bounds);
}

var mapApp = new Vue({
    el: '#tramMap',
    components: {
        'app-footer' : Footer
    },
    data() {
        return {
            map: null,
            grid: null,
            networkError: false,
            routes: [],
            postcodes: [],
            postcodeLayerGroup: null,
            feedinfo: [],
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            findAndSetMapBounds();
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            addRoutes();
        }
    },
    mounted () {
        this.map = L.map('leafletMap');

        axios.get('/api/feedinfo')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.feedinfo = response.data;
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
        axios.get("/api/routes")
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.routes = response.data;
                mapApp.draw();
                queryForGrid(500, "9400ZZMAAIR", "9:15", getCurrentDate(), "3", "60");
            }).catch(function (error){
                mapApp.networkError = true;
                console.log(error);
            });
        // axios.get('/api/postcodes')
        //     .then(function (response) {
        //         mapApp.networkError = false;
        //         mapApp.postcodes = response.data;
        //         addPostcodes();
        //     }).catch(function (error) {
        //         mapApp.networkError = true;
        //         console.log(error);
        //     });
    }, 
    computed: {
        havePos: function () {
            return false; // needed for display in footer
        }
    }
});

