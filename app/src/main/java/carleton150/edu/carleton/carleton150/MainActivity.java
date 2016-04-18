package carleton150.edu.carleton.carleton150;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.lsjwzh.widget.recyclerviewpager.RecyclerViewPager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import carleton150.edu.carleton.carleton150.ExtraFragments.QuestCompletedFragment;
import carleton150.edu.carleton.carleton150.Interfaces.FragmentChangeListener;
import carleton150.edu.carleton.carleton150.MainFragments.EventsFragment;
import carleton150.edu.carleton.carleton150.MainFragments.HistoryFragment;
import carleton150.edu.carleton.carleton150.MainFragments.MainFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestFragment;
import carleton150.edu.carleton.carleton150.MainFragments.QuestInProgressFragment;
import carleton150.edu.carleton.carleton150.Models.GeofenceErrorMessages;
import carleton150.edu.carleton.carleton150.Models.VolleyRequester;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.POJO.EventObject.Events;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoContent;
import carleton150.edu.carleton.carleton150.POJO.GeofenceInfoObject.GeofenceInfoObject;
import carleton150.edu.carleton.carleton150.POJO.GeofenceObject.GeofenceObjectContent;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Monitors location and geofence information and calls methods in the main view fragments
 * to handle geofence and location changes. Also controls which fragment is in view.
 */
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status>, FragmentChangeListener {

    //things for location

    public Location mLastLocation = null;
    // Google client to interact with Google API
    public GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    private static Constants constants = new Constants();
    private LogMessages logMessages = new LogMessages();
    MainFragment curFragment = null;
    public boolean needToShowGPSAlert = true;
    private boolean geofenceRetrievalSuccessful = false;
    private GeofenceInfoObject allGeofenceInfo = null;
    private GeofenceObjectContent[] allGeofences = null;
    private boolean requestingGeofences = false;
    private HashMap<String, GeofenceObjectContent> allGeofencesMap = new HashMap<>();

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.i(logMessages.GEOFENCE_MONITORING, "MainActivity: trying to connect mGoogleApiClient");
            mGoogleApiClient.connect();
        }
    };

    public VolleyRequester mVolleyRequester = new VolleyRequester();
    AlertDialog networkAlertDialog;
    AlertDialog playServicesConnectivityAlertDialog;


    private boolean requestingQuests = false;
    private ArrayList<Quest> questInfo = null;

    private LinkedHashMap<String, ArrayList<EventContent>> eventsMapByDate = new LinkedHashMap<String, ArrayList<EventContent>>();
    private ArrayList<EventContent> tempEventContentLst = new ArrayList<EventContent>();
    private boolean requestingEvents = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("GEOFENCE MONITORING", "onCreate in MainActivity called");


        networkAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        playServicesConnectivityAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        // check availability of play services for location data and geofencing
        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
            if (isConnectedToNetwork()) {
                mGoogleApiClient.connect();
            }
        } else {
            showGooglePlayServicesUnavailableDialog();
        }

        //managing fragments and UI
        // fragmentManager = getSupportFragmentManager();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        }
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.history)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.events)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.quests)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        curFragment = new HistoryFragment();
        int commit = getSupportFragmentManager()
                .beginTransaction().replace(R.id.containerLayout, curFragment).commit();



        /*
        Overrides onTabSelected to notify the fragment going out of view that it is
        going out of view.
        This is because fragments are kept in onResumed state for the viewPager, so
        since no lifecycle methods are called, this has to be used so that geofences
        can be registered and unregistered depending which fragment is in view
         */
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    if (curFragment instanceof HistoryFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new HistoryFragment();
                    }
                }
                if (tab.getPosition() == 1) {
                    if (curFragment instanceof EventsFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new EventsFragment();
                    }
                }
                if (tab.getPosition() == 2) {
                    if (curFragment instanceof QuestFragment == false) {
                        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
                        curFragment = new QuestFragment();
                    }
                }
                int commit = getSupportFragmentManager()
                        .beginTransaction().replace(R.id.containerLayout, curFragment).commit();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        checkIfGPSEnabled();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Stops location updates to save battery
     */
    @Override
    protected void onPause() {
        super.onPause();
        needToShowGPSAlert = true;
        stopLocationUpdates();
    }

    /**
     * Overridden lifecycle method to start location updates if possible
     * and necessary, and connect mGoogleApiClient if possible and necessary
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            isConnectedToNetwork();
            if (mRequestingLocationUpdates) {
                if(checkIfGPSEnabled()) {
                    startLocationUpdates();
                }
            }
        } else {
            checkIfGPSEnabled();
            if (isConnectedToNetwork()) {
                // check availability of play services for location data and geofencing
                if (checkPlayServices()) {
                    mGoogleApiClient.connect();
                } else {
                    showGooglePlayServicesUnavailableDialog();
                }
            }
        }
    }


    /**
     * Method that is called when google API Client is connected
     *
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        // Once connected with google api, get the location
        if(checkIfGPSEnabled()) {
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            tellFragmentLocationChanged();
        }

        //starts periodic location updates
        if (mRequestingLocationUpdates) {
            if(checkIfGPSEnabled()) {
                startLocationUpdates();
            }
        }

    }

    /**
     * If google api client connection was suspended, keeps trying to connect
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        handler.postDelayed(runnable, 1000);
    }

    /**
     * Displays an alert dialog if unable to connect to the GoogleApiClient
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showAlertDialog("Connection to play services failed with message: " +
                        connectionResult.getErrorMessage() + "\nCode: " + connectionResult.getErrorCode(),
                playServicesConnectivityAlertDialog);
    }

    /**
     * Builds a GoogleApiClient
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Method to verify google play services on the device
     */
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        constants.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Method called by the google location client when the user's
     * location changes. Records the location and passes the new
     * location information to the fragment
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        Log.i("GEOFENCE MONITORING", "onLocationChanged ");

        // Assign the new location
        mLastLocation = location;
        tellFragmentLocationChanged();
        requestAllGeofences();
    }

    /**
     * Calls a method in the current fragment to handle a location change.
     */
    private void tellFragmentLocationChanged() {
        if (curFragment != null) {
            curFragment.handleLocationChange(mLastLocation);
        }
    }

    /**
     *
     * @return last known location
     */
    public Location getLastLocation() {
        return mLastLocation;
    }



    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(constants.FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(constants.DISPLACEMENT);
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {
        if(checkIfGPSEnabled()) {
            if (mGoogleApiClient.isConnected()) {
                if (mRequestingLocationUpdates) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                }
            }
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            Log.i(logMessages.LOCATION, "stopLocationUpdates : location updates stopped");
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }


    /**
     * checks whether phone has network connection. If not, displays a dialog
     * requesting that the user connects to a network.
     *
     * @return
     */
    public boolean isConnectedToNetwork() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            if (activeNetworkInfo.isConnected()) {
                return true;
            } else {
                showNetworkNotConnectedDialog();
                return false;
            }
        } else {
            showNetworkNotConnectedDialog();
            return false;
        }
    }

    /**
     * displays a dialog requesting that the user connect to a network
     */
    public void showNetworkNotConnectedDialog() {
        showAlertDialog(getResources().getString(R.string.no_network_connection),
                networkAlertDialog);
    }

    /**
     * Shows a dialog to tell user google play services is unavailable
     */
    private void showGooglePlayServicesUnavailableDialog() {
        showAlertDialog(getResources().getString(R.string.no_google_services), playServicesConnectivityAlertDialog);
    }

    /**
     * shows an alert dialog with the specified message
     *
     * @param message
     */
    public void showAlertDialog(String message, AlertDialog dialog) {
        if (!dialog.isShowing()) {
            dialog.setTitle("Alert");
            dialog.setMessage(message);
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener()

                    {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
            );
            dialog.show();
        }
    }

    public void showAlertDialogNoNeutralButton(AlertDialog dialog) {
        dialog.show();
    }

    /**
     * Method called from VolleyRequester when new geofences are retrieved
     * from server. Calls a function on whatever fragment is currently in view to
     * handle the new geofences
     *
     * @param content
     */
    public void handleNewGeofences(GeofenceObjectContent[] content) {
        Log.i("GEOFENCE MONITORING", "handleNewGeofences ");

        if(content == null){
            Log.i("GEOFENCE MONITORING", "handleNewGeofences: content is null ");
        }else{
            geofenceRetrievalSuccessful = true;
        }
        requestAllGeofenceInfo(content);
        for(int i = 0; i<content.length; i++){
            allGeofencesMap.put(content[i].getName(), content[i]);
        }
        allGeofences = content;
    }

    public HashMap<String, GeofenceObjectContent> getAllGeofencesMap(){
        return allGeofencesMap;
    }


    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     * The activity implements ResultCallback, so this is a required method
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
        } else {
            // Get the status code and log it.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(logMessages.GEOFENCE_MONITORING, "onResult error: " + errorMessage);
        }
    }

    /**
     * Overridden from FragmentChangeListener interface to replace
     * the QuestFragment with a new QuestInProgressFragment
     * when a quest is started from the QuestFragment
     *
     * @param fragment
     */
    @Override
    public void replaceFragment(MainFragment fragment) {
        //adapter.replaceFragment(fragment);
        getSupportFragmentManager().beginTransaction().remove(curFragment).commit();

        curFragment = fragment;

        int commit = getSupportFragmentManager()
                .beginTransaction().replace(R.id.containerLayout, curFragment).commit();
    }


    /**
     * If QuestInProgressFragment is the current fragment,
     * overrides back button to replaces the QuestInProgressFragment
     * with a new QuestFragment
     */
    @Override
    public void onBackPressed() {
        if (curFragment instanceof QuestInProgressFragment || curFragment instanceof QuestCompletedFragment) {
            getSupportFragmentManager().beginTransaction().remove(curFragment).commit();
            curFragment = new QuestFragment();
            int commit = getSupportFragmentManager()
                    .beginTransaction().replace(R.id.containerLayout, curFragment).commit();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Reterns the Preferences for the information stored about the user's
     * progress in a quest. This method is so the user can resume quests even
     * after killing the app or going back to the quest selection screen
     */
    public SharedPreferences getPersistentQuestStorage() {
        return getSharedPreferences(constants.QUEST_PREFERENCES_KEY, 0);

    }

    /**
     * gets the memory class of the device
     *
     * @return
     */
    public int getMemoryClass() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.v(logMessages.MEMORY_MONITORING, "memoryClass:" + Integer.toString(memoryClass));
        return memoryClass;
    }

    /**
     * Checks if gps is enabled on the device
     * @return true if enabled, false otherwise
     */
    public boolean checkIfGPSEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            if(needToShowGPSAlert) {
                needToShowGPSAlert = false;
            buildAlertMessageNoGps();
        }
            return false;
        }return true;
    }

    /**
     * Alerts the user that their GPS is not enabled and gives them option to enable it
     */
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.gps_not_enabled))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

   public void requestAllGeofences(){
        if(allGeofences == null && !requestingGeofences){
            Log.i("GEOFENCE MONITORING", "requestiongAllGeofences. about to request them ");

            requestingGeofences = true;
            if(geofenceRetrievalSuccessful != true) {
                Log.i("GEOFENCE MONITORING", "requestAllGeofences: geofenceRetrieval successful is not true ");
                requestingGeofences = getNewGeofences();
            }else{
                Log.i("GEOFENCE MONITORING", "requestAllGeofences: geofenceRetrieval successful is true ");
            }
        }
    }

    public void requestAllGeofenceInfo(GeofenceObjectContent[] geofences){
        Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo ");

        if(allGeofenceInfo == null){
            Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo : allGeofenceInfo is null ");
        }
        if(geofences != null){
            Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo : geofences not null ");

        }

        if(allGeofenceInfo == null && geofences != null) {
            Log.i("GEOFENCE MONITORING", "requestingAllGeofenceInfo: in if loop: ");

            GeofenceObjectContent[] singleGeofence = new GeofenceObjectContent[1];
            for(int i = 0; i<geofences.length; i++){
                Log.i("GEOFENCE MONITORING", "requestAllGeofenceInfo: in for loop");

                singleGeofence[0] = geofences[i];
                mVolleyRequester.request(this, singleGeofence);

            }
        }
    }

    /**
     * Method called from the VolleyRequester when it recieves info about a geofence
     * @param geofenceInfoObject
     */
    public void handleGeofenceInfo(GeofenceInfoObject geofenceInfoObject){
        Log.i("GEOFENCE MONITORING", "handleGeofenceInfo");


        if(allGeofenceInfo == null){
            Log.i("GEOFENCE MONITORING", "allGeofenceInfo was null");

            allGeofenceInfo = geofenceInfoObject;
        }else {
            Log.i("GEOFENCE MONITORING", "allGeofenceInfo was not null");

            if (geofenceInfoObject != null) {
                Log.i("GEOFENCE MONITORING", "geofenceInfoObject was not null");

                HashMap<String, GeofenceInfoContent[]> newGeofence = new HashMap<>();
                for (HashMap.Entry<String, GeofenceInfoContent[]> e : geofenceInfoObject.getContent().entrySet()) {
                    Log.i("GEOFENCE MONITORING", "handleGeofenceInfo: in for loop");

                    this.allGeofenceInfo.getContent().put(e.getKey(), e.getValue());
                    newGeofence.clear();
                    newGeofence.put(e.getKey(), e.getValue());
                    curFragment.addNewGeofenceInfo(newGeofence);
                }
            }
        }

        if(eventsMapByDate.size() == 0){
            requestEvents();
        }if(questInfo == null){
            requestQuests();
        }
    }

    public HashMap<String, GeofenceInfoContent[]> getAllGeofenceInfo(){
        if(allGeofenceInfo != null) {
            return this.allGeofenceInfo.getContent();
        }else{
            return null;
        }
    }

    /**
     * Requests geofences from server using VolleyRequester
     */
    public boolean getNewGeofences(){
        if(mLastLocation == null){
            Log.i("GEOFENCE MONITORING", "getNewGeofences: mLastLocation is null ");
        }else {
            try {
                this.mVolleyRequester.requestGeofences(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude(), this);
                return true;
            } catch (Exception e) {
                Log.i("GEOFENCE MONITORING", "getNewGeofences: error is :   " + e.getMessage());

                e.printStackTrace();
                return false;
            }
        }
        return false;
    }



    /**
     * Called by VolleyRequester, handles new quests from the server
     * @param newQuests
     */
    public void handleNewQuests(ArrayList<Quest> newQuests) {
        /*This is a call from the VolleyRequester, so this check prevents the app from
        crashing if the user leaves the tab while the app is trying
        to get quests from the server
         */

        requestingQuests = false;

        if(newQuests != null) {
            questInfo = newQuests;
        }

        if(curFragment instanceof QuestFragment){
            curFragment.handleNewQuests(questInfo);
        }

    }

    public void requestQuests(){
        if(questInfo == null && !requestingQuests)
        mVolleyRequester.requestQuests(this);
        requestingQuests = true;
    }

    public ArrayList<Quest> getQuests(){
        return this.questInfo;
    }

    /**
     * Requests events from server using the volleyRequester
     */
    public void requestEvents(){
        if(!requestingEvents && eventsMapByDate.size() == 0) {
            requestingEvents = true;
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH) - 1;
            String monthString = String.format("%02d", month);
            String dayString = String.format("%02d", day);
            String startTime = monthString + "/" + dayString + "/" + year;

            mVolleyRequester.requestEvents(startTime, 1000, this);
        }
    }



    /**
     * Called from VolleyRequester. Handles new events from server
     * @param events
     */
    public void handleNewEvents(Events events) {
        requestingEvents = false;
        String completeDate;
        String[] completeDateArray;
        String dateByDay;
        eventsMapByDate.clear();


                EventContent[] eventContents = events.getContent();
                for (int i = 0; i < eventContents.length; i++) {

                    // Add new date values to hashmap if not already there
                    completeDate = eventContents[i].getStartTime();
                    completeDateArray = completeDate.split("T");
                    dateByDay = completeDateArray[0];


                    // If key already there, add + update new values
                    if (!eventsMapByDate.containsKey(dateByDay)) {
                        tempEventContentLst.clear();
                        tempEventContentLst.add(eventContents[i]);
                        ArrayList<EventContent> eventContents1 = new ArrayList<>();
                        for (int k = 0; k < tempEventContentLst.size(); k++) {
                            eventContents1.add(tempEventContentLst.get(k));
                        }
                        eventsMapByDate.put(dateByDay, eventContents1);
                    } else {
                        tempEventContentLst.add(eventContents[i]);
                        ArrayList<EventContent> eventContents1 = new ArrayList<>();
                        for (int k = 0; k < tempEventContentLst.size(); k++) {
                            eventContents1.add(tempEventContentLst.get(k));
                        }
                        eventsMapByDate.put(dateByDay, eventContents1);
                    }

                }

        if(curFragment instanceof EventsFragment){
            curFragment.handleNewEvents(eventsMapByDate);
        }


    }

    public LinkedHashMap<String, ArrayList<EventContent>> getEventsMapByDate(){
        return this.eventsMapByDate;
    }


}
