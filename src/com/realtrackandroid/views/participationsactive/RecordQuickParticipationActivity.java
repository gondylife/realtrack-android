package com.realtrackandroid.views.participationsactive;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.PagerSlidingTabStrip;
import com.realtrackandroid.R;
import com.realtrackandroid.backend.activities.ActivitiesDAO;
import com.realtrackandroid.backend.activities.ParticipationDAO;
import com.realtrackandroid.models.activities.Activities;
import com.realtrackandroid.models.activities.Participation;
import com.realtrackandroid.views.dialogs.PickDateDialogListener;
import com.realtrackandroid.views.dialogs.PickTimeDialogListener;
import com.realtrackandroid.views.help.FrameworkInfoDialog;
import com.realtrackandroid.views.help.HelpDialog;
import com.realtrackandroid.views.participationsactive.OptionalFragment;
import com.realtrackandroid.views.participationsactive.RequiredFragment;

/**
 * RecordQuickParticipationActivity is different from RecordOrEditParticipationActivity in the following ways:
 * 1. RecordOrEditParticipationActivity ALREADY has a participation associated with it (created when
 *    reminders are served (in NotificationService)). RecordQuickParticipationActivity does not have a pre-existing
 *    participation. It has to create one.
 * 2. Because RecordOrEditParticipationActivity serves an existing participation, it has a date and time associated with
 *    it. RecordQuickParticipationActivity does not have a date and time a priori, and must get these from the user.
 * @author Raj
 */
public class RecordQuickParticipationActivity extends SherlockFragmentActivity implements
PickDateDialogListener, PickTimeDialogListener, RecordParticipationFragmentInterface {
  private int activitiesId;

  private ParticipationDAO pDao;
  private Activities a;

  private OptionalFragment optionalFragment;
  private RequiredFragment requiredFragment;
  
  private ParticipationPageAdapter pageAdapter;
  List<Fragment> fragments;
  private PagerSlidingTabStrip tabs;
  private List<String> fragmentTitles;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.base_pager);
    
    fragments = createFragments();
    requiredFragment = (RequiredFragment) fragments.get(0);
    optionalFragment = (OptionalFragment) fragments.get(1);
    
    tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
    pageAdapter = new ParticipationPageAdapter(getSupportFragmentManager(), fragments);
    
    ViewPager pager = (ViewPager)findViewById(R.id.viewpager);
    pager.setAdapter(pageAdapter);
    
    tabs.setViewPager(pager);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    activitiesId = getIntent().getExtras().getInt("activitiesid");
    
    a = new ActivitiesDAO(getApplicationContext()).getActivityWithId(activitiesId);

    pDao = new ParticipationDAO(getApplicationContext());
  }
  
  private List<Fragment> createFragments(){
    fragmentTitles = new ArrayList<String>();
    fragmentTitles.add("Required");
    fragmentTitles.add("Optional");
    
    List<Fragment> fList = new ArrayList<Fragment>();
    fList.add(RequiredFragment.newInstance(fragmentTitles.get(0)));
    fList.add(OptionalFragment.newInstance(fragmentTitles.get(1)));
    
    return fList;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.recordquickparticipationmenu, menu);

    getSupportActionBar().setDisplayShowTitleEnabled(true);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // provide a back button on the actionbar
        finish();
        break;
      case R.id.action_help:
        HelpDialog helpDialog = new HelpDialog();
        helpDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        helpDialog.show(getSupportFragmentManager(), "helpdialog");
        break;
      case R.id.action_framework:
        FrameworkInfoDialog frameworkInfoDialog = new FrameworkInfoDialog();
        frameworkInfoDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        frameworkInfoDialog.show(getSupportFragmentManager(), "frameworkinfodialog");
        break;
      case R.id.action_glossary:
        HelpDialog glossaryDialog = new HelpDialog();
        glossaryDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        glossaryDialog.setDisplayUrl("file:///android_asset/glossary.html");
        glossaryDialog.show(getSupportFragmentManager(), "glossarydialog");
        break;
      case R.id.action_save:
        saveParticipation();
        break;
      default:
        return super.onOptionsItemSelected(item);
    }

    return true;
  }

  private void saveParticipation() {
    Participation p = new Participation();

    // reminderId doesn't matter because we're setting
    // serviced to true; it's here just for the not null
    // constraint
    p.setReminderid(0);
    p.setServiced(true);
    p.setActivityid(activitiesId);
    
    if(!requiredFragment.setFields(p))
      return;
    
    optionalFragment.setFields(p);
    
    // update the serviced flag for this Reminder in the Reminders table
    // so that the next time the NotificationReceiver checks, this participation
    // does not show up as unserviced
    int participationId = pDao.addParticipation(p);
    
    requiredFragment.updateParticipants(participationId);

    finish();
  }

  @Override
  public void setDate(String selectedDate) {
    requiredFragment.setDate(selectedDate);
  }

  @Override
  public void setTime(String selectedTime) {
    requiredFragment.setTime(selectedTime);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(R.anim.animation_slideinleft, R.anim.animation_slideoutright);
    finish();
  }
  
  private class ParticipationPageAdapter extends FragmentPagerAdapter {
    private List<Fragment> fragments;

      public ParticipationPageAdapter(FragmentManager fm, List<Fragment> fragments) {
          super(fm);
          this.fragments = fragments;
      }
      
      @Override
      public Fragment getItem(int position) {
          return this.fragments.get(position);
      }
      
      @Override
      public CharSequence getPageTitle(int position) {
        return fragmentTitles.get(position);
      }
   
      @Override
      public int getCount() {
          return this.fragments.size();
      }
  }

  @Override
  public Activities getActivities() {
    return a;
  }
}