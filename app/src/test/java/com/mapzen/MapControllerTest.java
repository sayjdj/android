package com.mapzen;

import android.location.Location;

import com.mapzen.support.MapzenTestRunner;
import com.mapzen.support.TestBaseActivity;
import com.mapzen.support.TestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

@Config(emulateSdk = 18)
@RunWith(MapzenTestRunner.class)
public class MapControllerTest {
    private MapController controller;
    private TestBaseActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = TestHelper.initBaseActivity();
        controller = MapController.getInstance(activity);
    }

    @Test
    public void getInstance_shouldNotBeNull() throws Exception {
        assertThat(controller).isNotNull();
    }

    @Test
    public void getInstance_shouldBeSingleton() throws Exception {
        assertThat(controller).isSameAs(MapController.getInstance(activity));
    }

    @Test
    public void getMap_shouldNotBeNull() throws Exception {
        assertThat(controller.getMap()).isNotNull();
    }

    @Test
    public void getApp_shouldNotBeNull() throws Exception {
        assertThat(controller.getApp()).isNotNull();
    }

    @Test
    public void getLocation_shouldNotBeNull() throws Exception {
        controller.setLocation(new Location(""));
        assertThat(controller.getLocation()).isNotNull();
    }

    @Test
    public void setLocation_shouldUpdateLocation() throws Exception {
        Location expected = new Location("expected");
        controller.setLocation(expected);
        assertThat(controller.getLocation()).isSameAs(expected);
    }
}
