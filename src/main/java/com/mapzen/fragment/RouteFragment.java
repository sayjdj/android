package com.mapzen.fragment;

import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.entity.Feature;
import com.mapzen.osrm.Instruction;
import com.mapzen.osrm.Route;
import com.mapzen.util.DisplayHelper;
import com.mapzen.util.LocationDatabaseHelper;
import com.mapzen.util.Logger;
import com.mapzen.widget.DistanceView;

import org.json.JSONObject;
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.map.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.mapzen.MapController.getMapController;
import static com.mapzen.activity.BaseActivity.COM_MAPZEN_UPDATES_LOCATION;
import static com.mapzen.entity.Feature.NAME;

public class RouteFragment extends BaseFragment implements DirectionListFragment.DirectionListener,
        ViewPager.OnPageChangeListener {
    public static final String TAG = RouteFragment.class.getSimpleName();
    public static final int WALKING_ADVANCE_DEFAULT_RADIUS = 15;
    public static final int WALKING_LOST_THRESHOLD = 70;
    public static final int ROUTE_ZOOM_LEVEL = 17;

    @InjectView(R.id.overflow_menu) ImageButton overflowMenu;
    @InjectView(R.id.routes) ViewPager pager;

    private ArrayList<Instruction> instructions;
    private RoutesAdapter adapter;
    private Route route;
    private LocationReceiver locationReceiver;
    private Feature feature;
    private DistanceView distanceLeftView;
    private int previousPosition;
    private boolean locationPassThrough = false;
    private boolean hasFoundPath = false;
    private boolean instructionIsCompleted = false;

    public static RouteFragment newInstance(BaseActivity act, Feature feature) {
        final RouteFragment fragment = new RouteFragment();
        fragment.setAct(act);
        fragment.setMapFragment(act.getMapFragment());
        fragment.setFeature(feature);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.route_widget, container, false);
        ButterKnife.inject(this, rootView);
        adapter = new RoutesAdapter(act, this, instructions);
        TextView destinationName = (TextView) rootView.findViewById(R.id.destination_name);
        destinationName.setText(feature.getProperty(NAME));
        distanceLeftView = (DistanceView) rootView.findViewById(R.id.destination_distance);
        distanceLeftView.setRealTime(true);
        if (route != null) {
            distanceLeftView.setDistance(route.getTotalDistance());
        }
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(this);
        adapter.notifyDataSetChanged();
        previousPosition = pager.getCurrentItem();
        return rootView;
    }

    @OnClick(R.id.overflow_menu)
    @SuppressWarnings("unused")
    public void onClickOverFlowMenu() {
        PopupMenu popup = new PopupMenu(getActivity(), overflowMenu);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.route_options_menu, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.route_menu_steps) {
                    showDirectionListFragment();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        initLocationReceiver();
        act.hideActionBar();
        act.deactivateMapLocationUpdates();
        drawRoute();
    }

    @Override
    public void onPause() {
        super.onPause();
        act.unregisterReceiver(locationReceiver);
        act.activateMapLocationUpdates();
        clearRoute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        act.showActionBar();
        clearRoute();
    }

    public int getWalkingAdvanceRadius() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(act);
        final String walkingAdvanceString =
                prefs.getString(act.getString(R.string.settings_key_walking_advance_radius),
                        Integer.toString(WALKING_ADVANCE_DEFAULT_RADIUS));
        return Integer.valueOf(walkingAdvanceString);
    }

    public void setInstructions(ArrayList<Instruction> instructions) {
        Logger.d("instructions: " + instructions.toString());
        this.instructions = instructions;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public GeoPoint getDestinationPoint() {
        return feature.getGeoPoint();
    }

    private Location getNextTurnTo(Instruction nextInstruction) {
        Location nextTurn = new Location(getResources().getString(R.string.application_name));
        nextTurn.setLatitude(nextInstruction.getPoint()[0]);
        nextTurn.setLongitude(nextInstruction.getPoint()[1]);
        return nextTurn;
    }

    public void setLocationPassThrough(boolean locationPassThrough) {
        this.locationPassThrough = locationPassThrough;
    }

    private Location snapTo(Location location) {
        if (!locationPassThrough) {
            Instruction instruction = instructions.get(getCurrentItem());
            double[] locationPoint = {location.getLatitude(), location.getLongitude()};
            logToDatabase("RouteFragment::onLocationChange: current location: "
                    + String.valueOf(location.getLatitude()) + " ,"
                    + String.valueOf(location.getLongitude()));
            logToDatabase("RouteFragment::onLocationChange: reference location: "
                    + instruction.toString());
            double[] onRoadPoint;
            onRoadPoint = instruction.snapTo(locationPoint, -90);
            if (onRoadPoint == null) {
                onRoadPoint = instruction.snapTo(locationPoint, 90);
            }

            if (onRoadPoint != null) {
                Location correctedLocation = new Location("Corrected");
                correctedLocation.setLatitude(onRoadPoint[0]);
                correctedLocation.setLongitude(onRoadPoint[1]);
                return correctedLocation;
            }
        }
        return location;
    }

    private Location getStartLocation() {
        Location beginning = new Location("begin point");
        beginning.setLatitude(route.getStartCoordinates()[0]);
        beginning.setLongitude(route.getStartCoordinates()[1]);
        return beginning;
    }

    public void onLocationChanged(Location location) {
        logToDatabase("==== 'BEGIN' ====");
        logToDatabase("original location accuracy: " + String.valueOf(location.getAccuracy()));
        logToDatabase("original location: " + location.toString());
        logToDatabase("current item" + String.valueOf(pager.getCurrentItem()));
        Location correctedLocation = snapTo(location);
        storeLocationInfo(location, correctedLocation);

        //// Did we find reasonable corrected location?
        if (correctedLocation != null) {
         // && location.distanceTo(correctedLocation) < location.getAccuracy()) {
            getMapController().setLocation(correctedLocation);
            mapFragment.findMe();
            hasFoundPath = true;
            logToDatabase(
                    "RouteFragment::onLocationChange: Corrected: " + correctedLocation.toString());
        } else {
            logToDatabase("RouteFragment::onLocationChange: ambigous location");
        }

        // Are we outside of given lost threshold
        if (WALKING_LOST_THRESHOLD < location.distanceTo(correctedLocation) &&
                location.getAccuracy() < getWalkingAdvanceRadius()) {
            // execute reroute query and reset the path
            logToDatabase("RouteFragment::onLocationChange: probably off course");
        }

        // Have we found anything useful
        if (!hasFoundPath && getStartLocation().distanceTo(location) > getWalkingAdvanceRadius()) {
            logToDatabase("RouteFragment::onLocationChange: hasn't hit first location and is"
                    + "probably off course");
        }

        Location nextTurn = null;
        final int currentItem = pager.getCurrentItem();
        if (currentItem < (instructions.size() - 1)) {
            nextTurn = getNextTurnTo(instructions.get(pager.getCurrentItem() + 1));
        }

        if (correctedLocation != null && nextTurn != null) {
            int distanceToNextTurn = (int) Math.floor(correctedLocation.distanceTo(nextTurn));

            // Inside of radius of next instruction
            if (distanceToNextTurn < getWalkingAdvanceRadius()) {
                logToDatabase("RouteFragment::onLocationChangeLocation: " +
                        "inside defined radius advancing");
                // inside walking radius
                goToNextInstruction();
                instructionIsCompleted = false;
                return;
            } else {
                logToDatabase("RouteFragment::onLocationChangeLocation: " +
                        "outside defined radius");
            }

            logToDatabase("instructions: " + instructions.toString());
            // Lets make sure that it's valid
            if (pager.getCurrentItem() > 0 && pager.getCurrentItem() < (instructions.size() - 1)) {
                // get previous instruction to see if we have walked out side of it
                logToDatabase("instruction is completed flag: " +
                        String.valueOf(instructionIsCompleted));
                logToDatabase("current Item: " + String.valueOf(pager.getCurrentItem()));
                logToDatabase("next turn: " + nextTurn.toString());
                Location prevTurn = getNextTurnTo(instructions.get(pager.getCurrentItem()));
                logToDatabase("previus turn: " + prevTurn.toString());
                logToDatabase("correctedLocation: " + correctedLocation.toString());
                logToDatabase("distance from pre turn with corrected: " +
                        String.valueOf(correctedLocation.distanceTo(prevTurn)));
                logToDatabase("distance from pre turn with original: " +
                        String.valueOf(location.distanceTo(prevTurn)));
                if (correctedLocation.distanceTo(prevTurn) > getWalkingAdvanceRadius()) {
                    // Sets what to do after action has been completed
                    logToDatabase("should be switching language now? " +
                            String.valueOf(instructionIsCompleted));
                    if (!instructionIsCompleted) {
                        View v = pager.getChildAt(pager.getCurrentItem());
                        if (v != null) {
                            TextView tv1 = (TextView) v.findViewById(R.id.full_instruction);
                            logToDatabase("exisiting language: " + tv1.getText().toString());
                            logToDatabase("exisiting language visible?: " +
                                    String.valueOf(tv1.getVisibility() == View.VISIBLE));
                            tv1.setVisibility(View.GONE);
                            TextView tv2 = (TextView)
                                    v.findViewById(R.id.full_instruction_after_action);
                            logToDatabase("new language: " + tv2.getText().toString());
                            logToDatabase("new language visible?: " +
                                    String.valueOf(tv2.getVisibility() == View.VISIBLE));
                            tv2.setVisibility(View.VISIBLE);
                            ImageView iv1 = (ImageView) v.findViewById(R.id.turn_icon);
                            iv1.setVisibility(View.GONE);
                            ImageView iv2 = (ImageView) v.findViewById(R.id.turn_icon_after_action);
                            iv2.setVisibility(View.VISIBLE);
                            instructionIsCompleted = true;
                        }
                    }
                } else {
                    logToDatabase("outside should no need for anything");
                }
            }

            logToDatabase("RouteFragment::onLocationChangeLocation: " +
                    "new current location: " + location.toString());
            logToDatabase("RouteFragment::onLocationChangeLocation: " +
                    "next turn: " + nextTurn.toString());
            logToDatabase("RouteFragment::onLocationChangeLocation: " +
                    "distance to next turn: " + String.valueOf(distanceToNextTurn));
            logToDatabase("RouteFragment::onLocationChangeLocation: " +
                    "threshold: " + String.valueOf(getWalkingAdvanceRadius()));
        } else {
            if (correctedLocation == null) {
                logToDatabase("RouteFragment::onLocationChangeLocation: " +
                        "**next turn** is null screw it");
            }
            if (nextTurn == null) {
                logToDatabase("RouteFragment::onLocationChangeLocation: " +
                        "**location** is null screw it");
            }
        }
        logToDatabase("==== 'END' ====");
    }

    private void showDirectionListFragment() {
        final Fragment fragment = DirectionListFragment.newInstance(instructions, this);
        act.getSupportFragmentManager().beginTransaction()
                .add(R.id.full_list, fragment, DirectionListFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    public void goToNextInstruction() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    private void changeDistance(int difference) {
        if (!distanceLeftView.getText().toString().isEmpty()) {
            int newDistance = distanceLeftView.getDistance() + difference;
            distanceLeftView.setDistance(newDistance);
        }
    }

    public void goToPrevInstruction() {
        int nextItemIndex = pager.getCurrentItem() - 1;
        pager.setCurrentItem(nextItemIndex);
    }

    public int getCurrentItem() {
        return pager.getCurrentItem();
    }

    public void onRouteSuccess(JSONObject rawRoute) {
        this.route = new Route(rawRoute);
        if (route.foundRoute()) {
            setInstructions(route.getRouteInstructions());
            drawRoute();
            displayRoute();
            setMapPerspectiveForInstruction(instructions.get(0));
        } else {
            Toast.makeText(act, act.getString(R.string.no_route_found), Toast.LENGTH_LONG).show();
            act.dismissProgressDialog();
            act.showActionBar();
        }
    }

    private void drawRoute() {
        PathLayer layer = mapFragment.getPathLayer();
        layer.clearPath();
        if (route != null) {
            for (double[] pair : route.getGeometry()) {
                layer.addPoint(new GeoPoint(pair[0], pair[1]));
            }
        }
    }

    public Route getRoute() {
        return route;
    }

    private void displayRoute() {
        act.getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .add(R.id.routes_container, this, RouteFragment.TAG)
                .commit();
    }

    private void clearRoute() {
        PathLayer layer = mapFragment.getPathLayer();
        layer.clearPath();
        mapFragment.updateMap();
    }

    @Override
    public void onInstructionSelected(int index) {
        pager.setCurrentItem(index, true);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int i) {
        if (previousPosition > i) {
            changeDistance(instructions.get(i + 1).getDistance());
        } else if (previousPosition < i) {
            changeDistance(-instructions.get(previousPosition).getDistance());
        }
        previousPosition = i;
        setMapPerspectiveForInstruction(instructions.get(i));
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    public void setMapPerspectiveForInstruction(Instruction instruction) {
        Map map = act.getMap();
        double[] point = instruction.getPoint();
        map.setMapPosition(point[0], point[1], Math.pow(2, ROUTE_ZOOM_LEVEL));
        map.viewport().setRotation(instruction.getRotationBearing());
        map.updateMap(true);
    }

    private void initLocationReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(COM_MAPZEN_UPDATES_LOCATION);
        locationReceiver = new LocationReceiver();
        act.registerReceiver(locationReceiver, filter);
    }

    private void logToDatabase(String msg) {
        if (act.isInDebugMode()) {
            SQLiteDatabase db = act.getDb();
            String insertSql =
                    String.format("insert into log_entries (log_entry) values (\"%s\")", msg);
            db.execSQL(insertSql);
        }
    }

    private void storeLocationInfo(Location location, Location correctedLocation) {
        if (act.isInDebugMode()) {
            SQLiteDatabase db = act.getDb();
            db.execSQL(LocationDatabaseHelper.insertSQLForLocationCorrection(location,
                    correctedLocation, instructions.get(pager.getCurrentItem())));
        }
    }

    private static class RoutesAdapter extends PagerAdapter {
        private List<Instruction> instructions = new ArrayList<Instruction>();
        private RouteFragment parent;
        private Context context;
        private Instruction currentInstruction;

        public RoutesAdapter(Context context, RouteFragment parent,
                List<Instruction> instructions) {
            this.context = context;
            this.instructions = instructions;
            this.parent = parent;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            currentInstruction = instructions.get(position);
            View view = View.inflate(context, R.layout.instruction, null);

            if (position == instructions.size() - 1) {
                view.setBackgroundColor(context.getResources().getColor(R.color.destination_green));
            } else {
                view.setBackgroundColor(context.getResources().getColor(R.color.dark_gray));
            }

            TextView fullInstruction = (TextView) view.findViewById(R.id.full_instruction);
            fullInstruction.setText(
                    getFullInstructionWithBoldName(currentInstruction.getFullInstruction()));

            TextView fullInstructionAfterAction =
                    (TextView) view.findViewById(R.id.full_instruction_after_action);
            fullInstructionAfterAction.setText(
                    getFullInstructionWithBoldName(
                            currentInstruction.getFullInstructionAfterAction()));

            ImageView turnIcon = (ImageView) view.findViewById(R.id.turn_icon);
            turnIcon.setImageResource(DisplayHelper.getRouteDrawable(context,
                    currentInstruction.getTurnInstruction(), DisplayHelper.IconStyle.WHITE));

            ImageView turnIconAfterAction =
                    (ImageView) view.findViewById(R.id.turn_icon_after_action);
            turnIconAfterAction.setImageResource(DisplayHelper.getRouteDrawable(context,
                    10, DisplayHelper.IconStyle.WHITE));

            if (instructions.size() != position + 1) {
                ImageButton next = (ImageButton) view.findViewById(R.id.route_next);
                next.setVisibility(View.VISIBLE);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        parent.goToNextInstruction();
                    }
                });
            }

            if (position > 0) {
                ImageButton prev = (ImageButton) view.findViewById(R.id.route_previous);
                prev.setVisibility(View.VISIBLE);
                prev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        parent.goToPrevInstruction();
                    }
                });
            }
            view.setTag("Instruction_" + String.valueOf(position));
            container.addView(view);
            return view;
        }

        private SpannableStringBuilder getFullInstructionWithBoldName(String fullInstruction) {
            final String name = currentInstruction.getName();
            final int startOfName = fullInstruction.indexOf(name);
            final int endOfName = startOfName + name.length();
            final StyleSpan boldStyleSpan = new StyleSpan(Typeface.BOLD);

            final SpannableStringBuilder ssb = new SpannableStringBuilder(fullInstruction);
            ssb.setSpan(boldStyleSpan, startOfName, endOfName, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return ssb;
        }

        @Override
        public int getCount() {
            return instructions.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Location location = bundle.getParcelable("location");
            onLocationChanged(location);
        }
    }
}
