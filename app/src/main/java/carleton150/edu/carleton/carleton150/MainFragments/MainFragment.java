package carleton150.edu.carleton.carleton150.MainFragments;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import carleton150.edu.carleton.carleton150.Constants;
import carleton150.edu.carleton.carleton150.POJO.EventObject.EventContent;
import carleton150.edu.carleton.carleton150.POJO.Quests.Quest;

/**
 * Created on 10/28/15.
 * Super class for all of the main view fragments. Ensures that they have
 * some methods in common so that the MainActivity can call these methods
 * without checking which type of fragment is currently in view
 */
public class MainFragment extends Fragment{

    public boolean isVisible = false;

    /**
     * Required empty constructor
     */
    public MainFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Checks if this is app's first launch to display history tutorial
     */
    public boolean checkFirstHistoryRun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor;

        boolean isFirstHistoryRun = sharedPreferences.getBoolean(Constants.IS_FIRST_HISTORY_RUN_STRING, true);
        if (isFirstHistoryRun) {
            editor = sharedPreferences.edit();
            editor.putBoolean(Constants.IS_FIRST_HISTORY_RUN_STRING, false);
            editor.commit();
        }
        return isFirstHistoryRun;
    }

    /**
     * Checks if this is app's first launch to display quest tutorial
     */
    public boolean checkFirstQuestRun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isFirstQuestRun = sharedPreferences.getBoolean(Constants.IS_FIRST_QUEST_RUN_STRING, true);
        if (isFirstQuestRun) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Constants.IS_FIRST_QUEST_RUN_STRING, false);
            editor.commit();
        }
        return isFirstQuestRun;
    }

    /**
     * handles when the user's location changes
     * @param newLocation
     */
    public void handleLocationChange(Location newLocation){

    }

    /**
     * handles new events from server, called by VolleyRequester
     * @param eventsMapByDate
     */
    public void handleNewEvents(LinkedHashMap<String, Integer> eventsMapByDate, ArrayList<EventContent> events){

    }

    public void fragmentOutOfView(){
        isVisible = false;
    }

    public void fragmentInView(){
        isVisible = true;
    }

    public void handleNewQuests(ArrayList<Quest> newQuests){

    }
}


