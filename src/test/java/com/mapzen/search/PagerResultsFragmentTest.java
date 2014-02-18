package com.mapzen.search;

import android.widget.SearchView;
import android.widget.Toast;
import com.android.volley.Request;
import com.mapzen.MapzenApplication;
import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.entity.Feature;
import com.mapzen.fragment.ListResultsFragment;
import com.mapzen.shadows.ShadowVolley;
import com.mapzen.support.MapzenTestRunner;
import com.mapzen.util.MapzenProgressDialogFragment;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.tester.android.view.TestMenu;
import org.robolectric.util.FragmentTestUtil;

import java.util.Iterator;
import java.util.List;

import static com.mapzen.support.TestHelper.initBaseActivityWithMenu;
import static com.mapzen.support.TestHelper.initMapFragment;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Robolectric.application;

@Config(emulateSdk = 18)
@RunWith(MapzenTestRunner.class)
public class PagerResultsFragmentTest {
    private PagerResultsFragment fragment;
    private MapzenApplication app;
    private BaseActivity act;
    private TestMenu menu;

    @Before
    public void setUp() throws Exception {
        ShadowVolley.clearMockRequestQueue();
        menu = new TestMenu();
        act = initBaseActivityWithMenu(menu);
        initMapFragment(act);
        fragment = PagerResultsFragment.newInstance(act);
        FragmentTestUtil.startFragment(fragment);
        app = (MapzenApplication) application;
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(fragment).isNotNull();
    }

    @Test
    public void shouldInjectViewPager() throws Exception {
        assertThat(fragment.pager).isNotNull();
    }

    @Test
    public void shouldInjectPaginationIndicator() throws Exception {
        assertThat(fragment.indicator).isNotNull();
    }

    @Test
    public void shouldInjectViewAllButton() throws Exception {
        assertThat(fragment.viewAll).isNotNull();
    }

    @Test
    public void executeSearchOnMap_shouldCancelOutstandingAutoCompleteRequests() throws Exception {
        app.enqueueApiRequest(Feature.suggest("Empire", null, null));
        app.enqueueApiRequest(Feature.suggest("Empire State", null, null));
        fragment.executeSearchOnMap(new SearchView(app), "Empire State Building");
        List<Request> requestSet = ShadowVolley.getMockRequestQueue().getRequests();
        assertThat(requestSet).hasSize(3);

        Iterator<Request> iterator = requestSet.iterator();
        assertRequest(iterator.next());
        assertRequest(iterator.next());
        assertRequest(iterator.next());
    }

    @Test
    public void executeSearchOnMap_shouldDismissProgressDialogOnError() throws Exception {
        MapzenProgressDialogFragment dialogFragment = act.getProgressDialogFragment();
        fragment.executeSearchOnMap(new SearchView(app), "Empire State Building");
        assertThat(dialogFragment).isAdded();
        List<Request> requestSet = ShadowVolley.getMockRequestQueue().getRequests();
        Request<JSONObject> request = requestSet.iterator().next();
        request.deliverError(null);
        assertThat(dialogFragment).isNotAdded();
    }

    @Test
    public void executeSearchOnMap_shouldToastAnError() {
        fragment.executeSearchOnMap(new SearchView(app), "Empire State Building");
        assertThat(act.getProgressDialogFragment()).isAdded();
        List<Request> requestSet = ShadowVolley.getMockRequestQueue().getRequests();
        Request<JSONObject> request = requestSet.iterator().next();
        request.deliverError(null);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(app.getString(R.string.generic_server_error));
        assertThat(ShadowToast.getLatestToast()).hasDuration(Toast.LENGTH_LONG);
    }

    @Test
    public void viewAll_shouldAddListResultsFragment() throws Exception {
        fragment.viewAll.performClick();
        assertThat(act.getSupportFragmentManager()).hasFragmentWithTag(ListResultsFragment.TAG);
    }

    @Test
    public void displayResults_shouldShowMultiResultHeaderForMultipleResults() throws Exception {
        fragment.add(new Feature());
        fragment.add(new Feature());
        fragment.displayResults(2, 0);
        assertThat(fragment.multiResultHeader).isVisible();
    }

    @Test
    public void displayResults_shouldHideMultiResultHeaderForSingleResult() throws Exception {
        fragment.add(new Feature());
        fragment.displayResults(1, 0);
        assertThat(fragment.multiResultHeader).isGone();
    }

    private void assertRequest(Request request) {
        if (request.getUrl().contains("Building")) {
            assertThat(request.isCanceled()).isFalse();
        } else {
            assertThat(request.isCanceled()).isTrue();
        }
    }
}
