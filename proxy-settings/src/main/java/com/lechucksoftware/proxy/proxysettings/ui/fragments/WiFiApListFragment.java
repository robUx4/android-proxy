package com.lechucksoftware.proxy.proxysettings.ui.fragments;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.constants.Constants;
import com.lechucksoftware.proxy.proxysettings.loaders.ProxyConfigurationTaskLoader;
import com.lechucksoftware.proxy.proxysettings.ui.activities.WiFiApDetailActivity;
import com.lechucksoftware.proxy.proxysettings.ui.adapters.WifiAPSelectorListAdapter;
import com.lechucksoftware.proxy.proxysettings.ui.components.ActionsView;
import com.lechucksoftware.proxy.proxysettings.ui.base.BaseListFragment;
import com.lechucksoftware.proxy.proxysettings.ui.base.IBaseFragment;
import com.lechucksoftware.proxy.proxysettings.utils.EventReportingUtils;
import com.lechucksoftware.proxy.proxysettings.utils.Utils;

import java.util.List;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.ProxyConfiguration;
import be.shouldit.proxy.lib.enums.SecurityType;

/**
 * Created by marco on 17/05/13.
 */
public class WiFiApListFragment extends BaseListFragment implements IBaseFragment, LoaderManager.LoaderCallbacks<List<ProxyConfiguration>>
{
    private static final String TAG = "WiFiApListFragment";
    private static final int LOADER_PROXYCONFIGURATIONS = 1;
    private static WiFiApListFragment instance;
    int mCurCheckPosition = 0;
    private WifiAPSelectorListAdapter apListAdapter;
    private TextView emptyText;
    private Loader<List<ProxyConfiguration>> loader;
    private RelativeLayout progress;
    private List<ProxyConfiguration> apConfigurations;
    private ActionsView actionsView;
    private RelativeLayout emptySection;

    public static WiFiApListFragment getInstance()
    {
        if (instance == null)
            instance = new WiFiApListFragment();

        return instance;
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
//        setMenuVisibility(true);
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        App.getLogger().startTrace(TAG, "onCreateView", Log.DEBUG);

        View v = inflater.inflate(R.layout.wifi_ap_list_fragment, container, false);

        progress = (RelativeLayout) v.findViewById(R.id.progress);
        actionsView = (ActionsView) v.findViewById(R.id.actions_view);
        emptyText = (TextView) v.findViewById(android.R.id.empty);
        emptySection = (RelativeLayout) v.findViewById(R.id.empty_message_section);

        App.getLogger().stopTrace(TAG, "onCreateView", Log.DEBUG);
        return v;
    }

    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                                    ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE |    // ENABLE
                                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM);  // DISABLE

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        showDetails(position);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        progress.setVisibility(View.VISIBLE);
        if (apListAdapter == null)
        {
            apListAdapter = new WifiAPSelectorListAdapter(getActivity());
            setListAdapter(apListAdapter);
        }

        actionsView.enableWifiAction(false);
        actionsView.configureWifiAction(false);

        loader = getLoaderManager().initLoader(LOADER_PROXYCONFIGURATIONS, new Bundle(), this);
        loader.forceLoad();
    }

    public void refreshUI()
    {
        if (apListAdapter != null)
            apListAdapter.notifyDataSetChanged();

        if (Utils.isAirplaneModeOn(getActivity()))
        {
            emptySection.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(getActivity().getString(R.string.airplane_mode_message));

            actionsView.configureWifiAction(false);
            actionsView.enableWifiAction(false);
        }
        else
        {
            if (APL.getWifiManager().isWifiEnabled())
            {
                loader.forceLoad();

                actionsView.enableWifiAction(false);

                if (apConfigurations != null && apConfigurations.size() > 0)
                {
                    apListAdapter.setData(apConfigurations);

                    getListView().setVisibility(View.VISIBLE);
                    emptySection.setVisibility(View.GONE);
                    emptyText.setVisibility(View.GONE);

                    actionsView.configureWifiAction(false);
//                if (proxyConfigurations.size() > 10)
//                {
//                    getListView().setFastScrollEnabled(true);
//                    getListView().setFastScrollAlwaysVisible(true);
//                    getListView().setSmoothScrollbarEnabled(true);
//                }
//                else
//                {
//                    getListView().setFastScrollEnabled(false);
//                    getListView().setFastScrollAlwaysVisible(false);
//                    getListView().setSmoothScrollbarEnabled(false);
//                }
                }
                else
                {
                    getListView().setVisibility(View.GONE);
                    emptySection.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText(getResources().getString(R.string.wifi_empty_list_no_ap));

                    actionsView.configureWifiAction(true);
                }
            }
            else
            {
                // Do not display results when Wi-Fi is not enabled
//            apListAdapter.setData(new ArrayList<ProxyConfiguration>());
                getListView().setVisibility(View.GONE);
                emptySection.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText(getResources().getString(R.string.wifi_empty_list_wifi_off));

                actionsView.enableWifiAction(true);
                actionsView.configureWifiAction(false);
            }
        }

//        Toast.makeText(getActivity(), TAG + " REFRESHUI ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<List<ProxyConfiguration>> onCreateLoader(int i, Bundle bundle)
    {
        App.getLogger().startTrace(TAG, "onCreateLoader", Log.DEBUG);

        ProxyConfigurationTaskLoader proxyConfigurationTaskLoader = new ProxyConfigurationTaskLoader(getActivity());
        App.getLogger().stopTrace(TAG, "onCreateLoader", Log.DEBUG);

        return proxyConfigurationTaskLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<ProxyConfiguration>> listLoader, List<ProxyConfiguration> aps)
    {
        App.getLogger().startTrace(TAG, "onLoadFinished", Log.DEBUG);

        progress.setVisibility(View.GONE);
        apConfigurations = aps;

        refreshUI();

        App.getLogger().stopTrace(TAG, "onLoadFinished", Log.DEBUG);
        App.getLogger().stopTrace(TAG, "STARTUP", Log.ERROR);
    }

    @Override
    public void onLoaderReset(Loader<List<ProxyConfiguration>> listLoader)
    {
//        Toast.makeText(getActivity(), TAG + " LOADRESET", Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */

    void showDetails(int index)
    {
        mCurCheckPosition = index;

        try
        {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            getListView().setItemChecked(index, true);

            ProxyConfiguration selectedConfiguration = (ProxyConfiguration) getListView().getItemAtPosition(index);

            if (selectedConfiguration.ap.security == SecurityType.SECURITY_EAP)
            {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.oops)
                        .setMessage(getResources().getString(R.string.not_supported_network_8021x_error_message))
                        .setPositiveButton(R.string.proxy_error_dismiss, null)
                        .show();

                EventReportingUtils.sendEvent(R.string.analytics_cat_user_action,
                        R.string.analytics_act_button_click,
                        R.string.analytics_lab_8021x_security_not_supported);
            }
            else
            {
                App.getLogger().d(TAG, "Selected proxy configuration: " + selectedConfiguration.toShortString());

                Intent i = new Intent(getActivity(), WiFiApDetailActivity.class);
                i.putExtra(Constants.SELECTED_AP_CONF_ARG, selectedConfiguration.internalWifiNetworkId);
                startActivity(i);
            }
        }
        catch (Exception e)
        {
            EventReportingUtils.sendException(new Exception("Exception during WiFiApListFragment showDetails(" + index + ") " + e.toString()));
        }
    }
}
