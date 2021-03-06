package com.lechucksoftware.proxy.proxysettings.ui.base;

import android.app.DialogFragment;
import android.os.Bundle;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.utils.EventReportingUtils;

/**
 * Created by marco on 24/05/13.
 */
public class BaseDialogFragment extends DialogFragment
{
    /**
     *  Fragment life-cycle
     *
     *  onAttach()	The fragment instance is associated with an activity instance.The activity is not yet fully initialized
     *
     *  onCreate()	Fragment is created
     *
     *  onCreateView()	The fragment instance creates its view hierarchy. The inflated views become part of the view hierarchy of its containing activity.
     *
     *  onActivityCreated()	 Activity and fragment instance have been created as well as thier view hierarchy.
     *
     *  onResume()	 Fragment becomes visible and active.
     *
     *  onPause()	 Fragment is visibile but becomes not active anymore, e.g., if another activity is animating on top of the activity which contains the fragment.
     *
     *  onStop()	 Fragment becomes not visible.
     */

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        App.getLogger().d(this.getClass().getSimpleName(), "onResume " + this.getClass().getSimpleName());

        EventReportingUtils.sendScreenView(this.getClass().getSimpleName());
    }


    @Override
    public void onResume()
    {
        super.onResume();
        App.getLogger().d(this.getClass().getSimpleName(), "onResume " + this.getClass().getSimpleName());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        App.getLogger().d(this.getClass().getSimpleName(), "onPause " + this.getClass().getSimpleName());
    }
}
