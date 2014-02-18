package com.mapzen.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mapzen.geo.GeoFeature;

import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

import java.util.Locale;

import static com.mapzen.util.ApiHelper.getUrlForSearch;
import static com.mapzen.util.ApiHelper.getUrlForSuggest;

public class Feature extends GeoFeature implements Parcelable {
    public static final String NAME = "name";
    public static final String FEATURES = "features";
    public static final String TYPE = "type";
    public static final String COUNTRY_CODE = "country_code";
    public static final String COUNTRY_NAME = "country_name";
    public static final String ADMIN1_ABBR = "admin1_abbr";
    public static final String ADMIN1_NAME = "admin1_name";

    public static JsonObjectRequest suggest(String query, Response.Listener successListener,
            Response.ErrorListener errorListener) {
        JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(getUrlForSuggest(query).toString(), null, successListener,
                        errorListener);
        return jsonObjectRequest;
    }

    public static JsonObjectRequest search(Map map, String query, Response.Listener successListener,
            Response.ErrorListener errorListener) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(getUrlForSearch(query,
                map.viewport().getBBox()), null, successListener, errorListener);
        return jsonObjectRequest;
    }

    @Override
    public String toString() {
        return "'" + getProperty(NAME) + "'[" + getLat() + ", " + getLon() + "]";
    }

    public MarkerItem getMarker() {
        GeoPoint geoPoint = new GeoPoint(getLat(), getLon());
        MarkerItem markerItem = new MarkerItem(this, getProperty(NAME),
                "Current Location", geoPoint);
        return markerItem;
    }

    public GeoPoint getGeoPoint() {
        return new GeoPoint(getLat(), getLon());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(getLat());
        out.writeDouble(getLon());
        out.writeString(getProperty(NAME));
        out.writeString(getProperty(TYPE));
        out.writeString(getProperty(COUNTRY_CODE));
        out.writeString(getProperty(COUNTRY_NAME));
        out.writeString(getProperty(ADMIN1_ABBR));
        out.writeString(getProperty(ADMIN1_NAME));
    }

    public static Feature readFromParcel(Parcel in) {
        Feature feature = new Feature();
        feature.setLat(in.readDouble());
        feature.setLon(in.readDouble());
        feature.setProperty(NAME, in.readString());
        feature.setProperty(TYPE, in.readString());
        feature.setProperty(COUNTRY_CODE, in.readString());
        feature.setProperty(COUNTRY_NAME, in.readString());
        feature.setProperty(ADMIN1_ABBR, in.readString());
        feature.setProperty(ADMIN1_NAME, in.readString());
        return feature;
    }

    public static final Parcelable.Creator<Feature> CREATOR = new Parcelable.Creator<Feature>() {
        @Override
        public Feature[] newArray(int size) {
            return new Feature[size];
        }

        public Feature createFromParcel(Parcel in) {
            return Feature.readFromParcel(in);
        }
    };

    @Override
    public boolean equals(Object o) {
        Feature other = (Feature) o;
        return getLat() == other.getLat()
                && getLon() == other.getLon()
                && getProperty(NAME).equals(other.getProperty(NAME));
    }

    public int hashCode() {
        return 0;
    }

    public static class ViewHolder {
        private TextView title;
        private TextView address;

        public void setTitle(TextView title) {
            this.title = title;
        }

        public void setAddress(TextView address) {
            this.address = address;
        }

        public void setFromFeature(Feature feature) {
            if (feature != null) {
                title.setText(feature.getProperty(NAME));
                address.setText(String.format(Locale.getDefault(), "%s, %s",
                        feature.getProperty(ADMIN1_NAME), feature.getProperty(ADMIN1_ABBR)));
            }
        }
    }
}
