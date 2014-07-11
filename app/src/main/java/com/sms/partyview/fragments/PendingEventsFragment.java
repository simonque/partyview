package com.sms.partyview.fragments;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sms.partyview.AttendanceStatus;
import com.sms.partyview.models.Event;
import com.sms.partyview.models.EventUser;

import android.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by myho on 7/3/14.
 */
public class PendingEventsFragment extends EventListFragment {

    public static PendingEventsFragment newInstance() {
        PendingEventsFragment fragment = new PendingEventsFragment();
        return fragment;
    }

    @Override
    protected void populateEventList() {
        // Define the class we would like to query
        ParseQuery<EventUser> query = ParseQuery.getQuery(EventUser.class);

        // Define our query conditions

        // get list of events where user
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.whereEqualTo("status", AttendanceStatus.INVITED.toString());
        query.addAscendingOrder("date");
        query.include("event.host");

        query.findInBackground(
                new FindCallback<EventUser>() {
                    @Override
                    public void done(List<EventUser> eventUsers, ParseException e) {

                        Log.d("DEBUG", "invited eventUsers");
                        Log.d("DEBUG", eventUsers.size() + eventUsers.toString());
                        List<Event> events = new ArrayList<Event>();
                        for (EventUser eventUser : eventUsers) {
                            events.add(eventUser.getEvent());
                        }
                        eventAdapter.addAll(events);
                    }
                }
        );
    }
}
