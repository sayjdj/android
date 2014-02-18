package com.mapzen.search;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.adapters.SearchViewAdapter;
import com.mapzen.entity.Feature;
import com.mapzen.fragment.BaseFragment;
import com.mapzen.fragment.ItemFragment;
import com.mapzen.fragment.ListResultsFragment;
import com.mapzen.fragment.MapFragment;
import com.mapzen.util.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mapzen.MapController.getMapController;

public class PagerResultsFragment extends BaseFragment {
    public static final String TAG = PagerResultsFragment.class.getSimpleName();
    private List<ItemFragment> currentCollection = new ArrayList<ItemFragment>();
    private ArrayList<Feature> features = new ArrayList<Feature>();
    private static final String PAGINATE_TEMPLATE = "%2d of %2d RESULTS";

    @InjectView(R.id.multi_result_header)
    View multiResultHeader;

    @InjectView(R.id.pagination)
    TextView indicator;

    @InjectView(R.id.view_all)
    Button viewAll;

    @InjectView(R.id.results)
    ViewPager pager;

    public static PagerResultsFragment newInstance(BaseActivity act) {
        PagerResultsFragment pagerResultsFragment = new PagerResultsFragment();
        pagerResultsFragment.setAct(act);
        pagerResultsFragment.setMapFragment(act.getMapFragment());
        return pagerResultsFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results,
                container, false);
        ButterKnife.inject(this, view);
        initOnPageChangeListener();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearMap();
        ButterKnife.reset(this);
    }

    @OnClick(R.id.view_all)
    @SuppressWarnings("unused")
    public void onClickViewAll() {
        final Fragment fragment = ListResultsFragment.newInstance(act, features);
        act.getSupportFragmentManager().beginTransaction()
                .add(R.id.full_list, fragment, ListResultsFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    public void setCurrentItem(int position) {
        pager.setCurrentItem(position);
    }

    public int getCurrentItem() {
        return pager.getCurrentItem();
    }

    private void initOnPageChangeListener() {
        this.pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                centerOnPlace(i, getMapController().getZoomScale());
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    private void centerOnPlace(int i) {
        centerOnPlace(i, Math.pow(2, MapFragment.DEFAULT_ZOOMLEVEL));
    }

    private void centerOnPlace(int i, double zoom) {
        ItemFragment srf = currentCollection.get(i);
        Feature feature = srf.getFeature();
        Logger.d("feature: " + feature.toString());
        String indicatorText = String.format(Locale.getDefault(), PAGINATE_TEMPLATE, i + 1,
                currentCollection.size());
        indicator.setText(indicatorText);
        mapFragment.centerOn(feature, zoom);
    }

    private void hide() {
        act.getSupportFragmentManager().beginTransaction()
                .hide(this)
                .commit();
    }

    public void clearMap() {
        mapFragment.clearMarkers();
        mapFragment.updateMap();
    }

    public void flipTo(Feature feature) {
        int pos = features.indexOf(feature);
        pager.setCurrentItem(pos);
    }

    public void clearAll() {
        Logger.d(String.format(Locale.US, "clearing all items: %d", currentCollection.size()));
        ItemizedLayer<MarkerItem> poiLayer = mapFragment.getPoiLayer();
        poiLayer.removeAllItems();
        pager.setCurrentItem(0);
        currentCollection.clear();
        features.clear();
    }

    public void add(Feature feature) {
        Logger.d(feature.toString());
        addMarker(feature);
        ItemFragment itemFragment = new ItemFragment();
        itemFragment.setFeature(feature);
        itemFragment.setMapFragment(mapFragment);
        itemFragment.setAct(act);
        currentCollection.add(itemFragment);
        features.add(feature);
    }

    public void setSearchResults(JSONArray jsonArray) {
        clearAll();
        if (jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                Feature feature = new Feature();
                try {
                    feature.buildFromJSON(jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    Logger.e(e.toString());
                }
                add(feature);
            }
            displayResults(jsonArray.length(), pager.getCurrentItem());
        } else {
            hide();
            Toast.makeText(act, "No results where found for: " + act.getSearchView().getQuery(),
                    Toast.LENGTH_LONG).show();
        }
    }

    public boolean executeSearchOnMap(final SearchView view, String query) {
        app.cancelAllApiRequests();
        act.showProgressDialog();
        app.setCurrentSearchTerm(query);
        JsonObjectRequest jsonObjectRequest =
                Feature.search(mapFragment.getMap(), query,
                        getSearchListener(view), getErrorListener());
        app.enqueueApiRequest(jsonObjectRequest);
        return true;
    }

    private Response.ErrorListener getErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                onServerError(volleyError);
            }
        };
    }

    private Response.Listener<JSONObject> getSearchListener(final SearchView view) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Logger.d("Search Results: " + jsonObject.toString());
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = jsonObject.getJSONArray("features");
                } catch (JSONException e) {
                    Logger.e(e.toString());
                }
                setSearchResults(jsonArray);
                act.dismissProgressDialog();
                view.clearFocus();
            }
        };
    }

    public void displayResults(int length, int currentPos) {
        SearchViewAdapter adapter = new SearchViewAdapter(getFragmentManager(), currentCollection);
        pager.setAdapter(adapter);
        String indicatorText = String.format(Locale.getDefault(), PAGINATE_TEMPLATE, 1, length);
        indicator.setText(indicatorText);
        centerOnPlace(currentPos);
        mapFragment.updateMap();
        setMultiResultHeaderVisibility(length);
    }

    private void setMultiResultHeaderVisibility(int length) {
        if (length == 1) {
            multiResultHeader.setVisibility(View.GONE);
        } else {
            multiResultHeader.setVisibility(View.VISIBLE);
        }
    }

    private void addMarker(Feature feature) {
        mapFragment.getPoiLayer().addItem(feature.getMarker());
    }
}
