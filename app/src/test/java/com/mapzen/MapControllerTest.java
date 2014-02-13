package com.mapzen;

import com.mapzen.support.MapzenTestRunner;

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

    @Before
    public void setUp() throws Exception {
        controller = MapController.getInstance();
    }

    @Test
    public void getInstance_shouldNotBeNull() throws Exception {
        assertThat(controller).isNotNull();
    }

    @Test
    public void getInstance_shouldBeSingleton() throws Exception {
        assertThat(controller).isSameAs(MapController.getInstance());
    }
}
