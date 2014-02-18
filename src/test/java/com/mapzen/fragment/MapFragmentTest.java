package com.mapzen.fragment;

import com.mapzen.search.OnPoiClickListener;
import com.mapzen.support.FakeMotionEvent;
import com.mapzen.support.MapzenTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.TestMap;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static com.mapzen.support.TestHelper.initBaseActivity;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@Config(emulateSdk = 18)
@RunWith(MapzenTestRunner.class)
public class MapFragmentTest {
    private MapFragment mapFragment;
    private TestPoiClickListener listener;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        listener = new TestPoiClickListener();
        mapFragment = new MapFragment();
        mapFragment.setAct(initBaseActivity());
        mapFragment.setMap(new TestMap());
        mapFragment.setOnPoiClickListener(listener);
        startFragment(mapFragment);
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(mapFragment).isNotNull();
    }

    @Test
    public void onItemSingleTapUp_shouldNotifyListener() throws Exception {
        ItemizedLayer<MarkerItem> poiLayer = mapFragment.getPoiLayer();
        poiLayer.addItem(new MarkerItem("Title", "Description", new GeoPoint(0, 0)));
        poiLayer.onGesture(Gesture.TAP, new FakeMotionEvent(0, 0));
        assertThat(listener.getIndex()).isEqualTo(0);
        assertThat(listener.getItem().getTitle()).isEqualTo("Title");
    }

    class TestPoiClickListener implements OnPoiClickListener {
        private int index = -1;
        private MarkerItem item;

        @Override
        public void onPoiClick(int index, MarkerItem item) {
            this.index = index;
            this.item = item;
        }

        public int getIndex() {
            return index;
        }

        public MarkerItem getItem() {
            return item;
        }
    }

    @Test
    public void onPause_shouldEmptyMeMarkers() throws Exception {
        ItemizedLayer<MarkerItem> meMarkerLayer = mapFragment.getMeMarkerLayer();
        meMarkerLayer.addItem(new MarkerItem("Title", "Description", new GeoPoint(0, 0)));
        mapFragment.onPause();
        assertThat(meMarkerLayer.size()).isEqualTo(0);
    }
}
