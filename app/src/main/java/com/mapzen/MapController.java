package com.mapzen;

import android.content.Context;
import android.location.Location;

import com.mapzen.activity.BaseActivity;

import org.oscim.map.Map;

public class MapController {
    static private MapController mapController;
    private Map map;
    private MapzenApplication app;
    private Context context;
    private Location location;

    public MapController(Context context) {
        this.context = context;
        this.app = (MapzenApplication) context.getApplicationContext();
        this.map = ((BaseActivity) context).getMap();
    }

    static public MapController getInstance(Context context) {
        if (mapController == null) {
            mapController = new MapController(context);
        }
        return mapController;
    }

    public MapzenApplication getApp() {
        return app;
    }

    public Map getMap() {
        return map;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
