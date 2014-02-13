package com.mapzen;

public class MapController {
    static private MapController mapController;

    static public MapController getInstance() {
        if (mapController == null) {
            mapController = new MapController();
        }
        return mapController;
    }
}
