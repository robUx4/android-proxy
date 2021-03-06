package be.shouldit.proxy.lib.reflection.android;

import android.os.Handler;
import android.os.Message;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.reflection.ReflectionUtils;

public class WifiServiceHandler extends Handler
{

    private static final String TAG = WifiServiceHandler.class.getSimpleName();

    @Override
    public void handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case ReflectionUtils.CMD_CHANNEL_HALF_CONNECTED:
                if (msg.arg1 == ReflectionUtils.STATUS_SUCCESSFUL)
                {
//                        //AsyncChannel in msg.obj
                }
                else
                {
//                        //AsyncChannel set up failure, ignore
                        APL.getLogger().e(TAG, "Failed to establish AsyncChannel connection");
                }
                break;
            case ReflectionUtils.CMD_WPS_COMPLETED:
                APL.getLogger().d(TAG, "WPS result not handled");
//                try
//                {
//                    Class classToInvestigate = Class.forName("android.net.wifi.WpsResult");
//                    Object wpsResult = classToInvestigate.cast(msg.obj);
//                    WpsResult result = (WpsResult) msg.obj;
//                    if (result == null) break;
//                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
//                            .setTitle(R.string.wifi_wps_setup_title)
//                            .setPositiveButton(android.R.string.ok, null);
//                    switch (result.status) {
//                        case FAILURE:
//                            dialog.setMessage(R.string.wifi_wps_failed);
//                            dialog.show();
//                            break;
//                        case IN_PROGRESS:
//                            dialog.setMessage(R.string.wifi_wps_in_progress);
//                            dialog.show();
//                            break;
//                        default:
//                            if (result.pin != null) {
//                                dialog.setMessage(getResources().getString(
//                                        R.string.wifi_wps_pin_output, result.pin));
//                                dialog.show();
//                            }
//                            break;
//                    }
//                }
//                catch (Exception e)
//                {
//                    APL.getEventReport().send(e);
//                }
                break;
            //TODO: more connectivity feedback
            default:
                //Ignore
                APL.getLogger().d(TAG, msg.toString());
                break;
        }
    }
}
