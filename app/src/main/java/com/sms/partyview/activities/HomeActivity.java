package com.sms.partyview.activities;

import com.astuetz.PagerSlidingTabStrip;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.sms.partyview.R;
import com.sms.partyview.adapters.MyPagerAdapter;
import com.sms.partyview.fragments.AcceptedEventsFragment;
import com.sms.partyview.fragments.EventListFragment;
import com.sms.partyview.fragments.PendingEventsFragment;
import com.sms.partyview.fragments.ProfileSettingDialog;
import com.sms.partyview.helpers.Utils;
import com.sms.partyview.models.AttendanceStatus;
import com.sms.partyview.models.LocalEvent;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity
        extends FragmentActivity {

    // Key used to store the user name in the installation info.
    public static final String INSTALLATION_USER_NAME_KEY = "username";
    private static final String TAG = HomeActivity.class.getSimpleName() + "_DEBUG";
    private FragmentPagerAdapter mAdapterViewPager;
    private PagerSlidingTabStrip mTabs;
    private ViewPager vpPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cacheAppUsers();

        getActionBar().setTitle(
                getString(R.string.title_activity_home) +
                        " (" + ParseUser.getCurrentUser().getUsername() + ")"
        );

        setupTabs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_new_event:
                displayNewEventActivity();
                return true;
            case R.id.action_settings:
                displaySettingDialog();
                return true;
            case R.id.action_sign_out:
                signOutUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signOutUser() {
        ParseUser.getCurrentUser().logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == Utils.NEW_EVENT_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            LocalEvent event =
                    (LocalEvent) data.getSerializableExtra(EditEventActivity.SAVED_EVENT_KEY);

            AcceptedEventsFragment fragment = (AcceptedEventsFragment) mAdapterViewPager.getItem(0);
            fragment.addNewEventToList(event.getObjectId(), AttendanceStatus.ACCEPTED.toString());

            // go back to accepted events page
            mAdapterViewPager.getItem(0);
        } else if ((requestCode == Utils.RESPOND_TO_INVITE_EVENT_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            String eventId = data.getStringExtra("eventId");
            String response = data.getStringExtra("response");

            if (response.equalsIgnoreCase(AttendanceStatus.ACCEPTED.toString())) {
                // remove from pending events
                PendingEventsFragment pendingFragment = (PendingEventsFragment) mAdapterViewPager
                        .getItem(1);
                pendingFragment.removeEventFromList(eventId);
                mAdapterViewPager.getItem(1);
                mAdapterViewPager.notifyDataSetChanged();

                vpPager.setCurrentItem(0);

                // go back to accepted events page
                AcceptedEventsFragment fragment = (AcceptedEventsFragment) mAdapterViewPager
                        .getItem(0);
                fragment.addNewEventToList(eventId, response);

                mAdapterViewPager.getItem(0);
                mAdapterViewPager.notifyDataSetChanged();

            } else {
                PendingEventsFragment fragment = (PendingEventsFragment) mAdapterViewPager.getItem(1);
                fragment.removeEventFromList(eventId);

                // go back to invited events page
                mAdapterViewPager.getItem(1);
                mAdapterViewPager.notifyDataSetChanged();
                vpPager.setCurrentItem(1);
            }
        } else if (requestCode == EventListFragment.EVENT_DETAIL_REQUEST &&
                   resultCode == RESULT_OK) {
            LocalEvent event =
                    (LocalEvent) data.getSerializableExtra(
                            AcceptedEventDetailActivity.UDPATED_EVENT_INTENT_KEY);
            Log.d("DEBUG", "returned local event: " + event);
            if (event == null) {
                return;
            }
            // Replace the existing event if it was updated.
            int index = data.getIntExtra(AcceptedEventDetailActivity.EVENT_LIST_INDEX_KEY, 0);
            EventListFragment fragment =
                    (EventListFragment) mAdapterViewPager.getItem(vpPager.getCurrentItem());
            fragment.updateEvent(index, event);
        }
    }

    private void setupTabs() {
        // Initialize the ViewPager and set an adapter
        vpPager = (ViewPager) findViewById(R.id.vpPager);
        mAdapterViewPager = new MyPagerAdapter(getSupportFragmentManager(), getFragments());
        vpPager.setAdapter(mAdapterViewPager);

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mTabs.setViewPager(vpPager);
    }

    private List<Fragment> getFragments() {
        List<Fragment> fragments = new ArrayList<Fragment>();

        fragments.add(AcceptedEventsFragment.newInstance());
        fragments.add(PendingEventsFragment.newInstance());

        return fragments;
    }

    private void displayNewEventActivity() {
        Intent i = new Intent(this, NewEventActivity.class);
        startActivityForResult(i, Utils.NEW_EVENT_REQUEST_CODE);
    }

    // Registers the user with this installation's info.
    private void storeInstallationInfo() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        String currentUserName = ParseUser.getCurrentUser().getUsername();
        if (currentUserName == installation.getString(INSTALLATION_USER_NAME_KEY)) {
            return;
        }
        installation.put(INSTALLATION_USER_NAME_KEY, currentUserName);
        installation.saveInBackground();
    }

    public void displaySettingDialog() {
        ProfileSettingDialog profileSettingDialog = ProfileSettingDialog.newInstance();
        profileSettingDialog.show(getFragmentManager(), "fragment_profile_setting");
    }

    private void cacheAppUsers() {
        FindCallback<ParseUser> callback = new FindCallback<ParseUser>() {
            public void done(final List<ParseUser> users, ParseException e) {

                Log.d(TAG, "got user info");
                Log.d(TAG, users.toString());

                // Remove the previously cached results.
                ParseObject.unpinAllInBackground("users", new DeleteCallback() {
                    public void done(ParseException e) {
                        // Cache the new results.
                        ParseObject.pinAllInBackground("users", users);
                        Log.d(TAG, "pin all user info");
                    }
                });
            }
        };

        Utils.cacheAppUsers(callback);
    }
}
