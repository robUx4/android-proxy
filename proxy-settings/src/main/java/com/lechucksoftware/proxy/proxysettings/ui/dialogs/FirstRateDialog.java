package com.lechucksoftware.proxy.proxysettings.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.constants.StartupActionStatus;
import com.lechucksoftware.proxy.proxysettings.utils.startup.StartupAction;
import com.lechucksoftware.proxy.proxysettings.utils.Utils;

public class FirstRateDialog extends DialogFragment
{
    public static String TAG = "FirstRateDialog";
    private StartupAction startupAction;

    public FirstRateDialog(StartupAction action)
    {
        startupAction = action;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());
//        builder.setTitle(getResources().getString(R.string.app_rater_dialog_title));
        builder.setMessage(getResources().getString(R.string.app_rater_dialog_text));
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface paramDialogInterface, int paramInt)
            {

//                App.getLogger().d(TAG, "Starting Market activity");
                startupAction.updateStatus(StartupActionStatus.DONE);
                Utils.startMarketActivity(getActivity());
            }
        });

        builder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface paramDialogInterface, int paramInt)
            {

                startupAction.updateStatus(StartupActionStatus.REJECTED);
            }
        });

        AlertDialog alert = builder.create();
        return alert;
    }

    public static FirstRateDialog newInstance(StartupAction action)
    {
        FirstRateDialog frag = new FirstRateDialog(action);
        return frag;
    }
}