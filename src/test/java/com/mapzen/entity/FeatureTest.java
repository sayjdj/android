package com.mapzen.entity;

import android.os.Parcel;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class FeatureTest {
    private Feature feature = new Feature();
    private double expectedLat = Double.parseDouble("51.4993491");
    private double expectedLon = Double.parseDouble("-0.12739091");

    @Before
    public void setUp() throws Exception {
        JSONObject json = new JSONObject("{\""
                + "type\":\"Feature\","
                + "\"geometry\":"
                + "{\"type\":\"Point\","
                + "\"coordinates\": [-0.12739091,51.4993491]},"
                + "\"properties\":"
                + "{\"name\":\"Feature Name to Display\","
                + "\"type\":\"testDescription\","
                + "\"country_code\":\"testUS\","
                + "\"country_name\":\"testUnited States\","
                + "\"admin1_abbr\":\"testNY\","
                + "\"admin1_name\":\"testNew York\","
                + "\"marker-color\":\"#F00\"}}");
        feature.buildFromJSON(json);
    }

    @Test
    public void hasLatDouble() throws Exception {
        assertThat(feature.getLat(), is(expectedLat));
    }

    @Test
    public void hasLonDouble() throws Exception {
        assertThat(feature.getLon(), is(expectedLon));
    }

    @Test
    public void isParcelable() throws Exception {
        Parcel parcel = Parcel.obtain();
        feature.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Feature newFeature = Feature.readFromParcel(parcel);
        assertThat(feature, equalTo(newFeature));
    }
}
