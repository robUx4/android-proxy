package be.shouldit.proxy.lib;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import be.shouldit.proxy.lib.enums.SecurityType;
import be.shouldit.proxy.lib.log.DefaultEventReport;
import be.shouldit.proxy.lib.log.IEventReporting;
import be.shouldit.proxy.lib.log.LogWrapper;
import be.shouldit.proxy.lib.reflection.ReflectionUtils;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;

/**
 * Main class that contains utilities for getting the proxy configuration of the
 * current or the all configured networks
 */
public class APL
{
    public static final String TAG = "APL";

    private static ConnectivityManager mConnManager;
    private static WifiManager mWifiManager;
    private static Context gContext;
    private static boolean sSetupCalled;
    private static int deviceVersion;
    private static IEventReporting eventReport;

    public static LogWrapper getLogger()
    {
        return logger;
    }

    private static LogWrapper logger;

    public static boolean setup(Context context)
    {
        return setup(context, Log.ERROR, null);
    }

    public static boolean setup(Context context, int logLevel, IEventReporting eRep)
    {
        gContext = context;
        deviceVersion = Build.VERSION.SDK_INT;

        // Make sure this is only called once.
        if (sSetupCalled)
        {
            return false;
        }

        sSetupCalled = true;
        logger = new LogWrapper(logLevel);

        getLogger().d(TAG,"APL setup executed");

        if (eRep != null)
        {
            eventReport = eRep;
        }
        else
        {
            eventReport = new DefaultEventReport();
        }

        return sSetupCalled;
    }

    public static IEventReporting getEventReport()
    {
        return eventReport;
    }

    public static Context getContext()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        return gContext;
    }

    public static int getDeviceVersion()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        return deviceVersion;
    }

    public static WifiManager getWifiManager()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        if (mWifiManager == null)
        {
            mWifiManager = (WifiManager) gContext.getSystemService(Context.WIFI_SERVICE);
        }

        return mWifiManager;
    }

    public static void enableWifi() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        WifiManager wm = getWifiManager();
        wm.setWifiEnabled(true);
    }

    public static ConnectivityManager getConnectivityManager()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        if (mConnManager == null)
        {
            mConnManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        return mConnManager;
    }

    /**
     * Main entry point to access the proxy settings
     */
    public static ProxyConfiguration getCurrentProxyConfiguration(URI uri) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        ProxyConfiguration proxyConfig;

        if (deviceVersion >= 12) // Honeycomb 3.1
        {
            proxyConfig = getProxySelectorConfiguration(uri);
        }
        else
        {
            proxyConfig = getGlobalProxy();
        }

        /**
         * Set direct connection if no proxyConfig received
         * */
        if (proxyConfig == null)
        {
            proxyConfig = new ProxyConfiguration(ProxySetting.NONE, null, null, null, null);
        }

        /**
         * Add connection details
         * */
        ConnectivityManager connManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
//		proxyConfig.currentNetworkInfo = activeNetInfo;

        if (activeNetInfo != null)
        {
            switch (activeNetInfo.getType())
            {
                case ConnectivityManager.TYPE_WIFI:
                    WifiManager wifiManager = (WifiManager) gContext.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
                    for (WifiConfiguration wc : wifiConfigurations)
                    {
                        if (wc.networkId == wifiInfo.getNetworkId())
                        {
                            proxyConfig.ap = new AccessPoint(wc);
                            break;
                        }
                    }
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    break;
                default:
                    throw new UnsupportedOperationException("Not yet implemented support for" + activeNetInfo.getTypeName() + " network type");
            }
        }

        return proxyConfig;
    }

    /**
     * For API >= 12: Returns the current proxy configuration based on the URI,
     * this implementation is a wrapper of the Android's ProxySelector class.
     * Just add some other details that can be useful to the developer.
     */
    public static ProxyConfiguration getProxySelectorConfiguration(URI uri) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        ProxySelector defaultProxySelector = ProxySelector.getDefault();

        Proxy proxy = null;

        List<Proxy> proxyList = defaultProxySelector.select(uri);
        if (proxyList.size() > 0)
        {
            proxy = proxyList.get(0);
            getLogger().d(TAG, "Current Proxy Configuration: " + proxy.toString());
        }
        else
            throw new Exception("Not found valid proxy configuration!");

        ConnectivityManager connManager = (ConnectivityManager) gContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        ProxyConfiguration proxyConfig = null;
        if (proxy == Proxy.NO_PROXY)
        {
            proxyConfig = new ProxyConfiguration(ProxySetting.STATIC, null, null, null, null);
        }
        else
        {
            InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
            proxyConfig = new ProxyConfiguration(ProxySetting.NONE, proxyAddress.getHostName(), proxyAddress.getPort(), null, null);
        }

        return proxyConfig;
    }

    /**
     * Return the current proxy configuration for HTTP protocol
     */
    public static ProxyConfiguration getCurrentHttpProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("http", "wwww.google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * Return the current proxy configuration for HTTPS protocol
     */
    public static ProxyConfiguration getCurrentHttpsProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("https", "wwww.google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * Return the current proxy configuration for FTP protocol
     */
    public static ProxyConfiguration getCurrentFtpProxyConfiguration() throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        URI uri = new URI("ftp", "google.it", null, null);
        return getCurrentProxyConfiguration(uri);
    }

    /**
     * For API < 12: Get global proxy configuration.
     */
    @Deprecated
    public static ProxyConfiguration getGlobalProxy()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        ProxyConfiguration proxyConfig = null;

        ContentResolver contentResolver = gContext.getContentResolver();
        String proxyString = Settings.Secure.getString(contentResolver, Settings.Secure.HTTP_PROXY);

        if (!TextUtils.isEmpty(proxyString) && proxyString.contains(":"))
        {
            String[] proxyParts = proxyString.split(":");
            if (proxyParts.length == 2)
            {
                String proxyAddress = proxyParts[0];
                try
                {
                    int proxyPort = Integer.parseInt(proxyParts[1]);
                    proxyConfig = new ProxyConfiguration(ProxySetting.STATIC, proxyAddress, proxyPort, null, null);
                }
                catch (NumberFormatException e)
                {
                    APL.getEventReport().send(new Exception("Port is not a number: " + proxyParts[1],e));
                }
            }
        }

        return proxyConfig;
    }

    /**
     * Get proxy configuration for Wi-Fi access point. Valid for API >= 12
     */
    @Deprecated
    @TargetApi(12)
    public static ProxyConfiguration getProxySdk12(WifiConfiguration wifiConf)
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        ProxyConfiguration proxyHost = null;

        try
        {
            Object proxySettings = getProxySettingsField(wifiConf);

            if (proxySettings != null)
            {
                int ordinal = ((Enum) proxySettings).ordinal();

                if (ordinal == ProxySetting.NONE.ordinal() || ordinal == ProxySetting.UNASSIGNED.ordinal())
                {
                    proxyHost = new ProxyConfiguration(ProxySetting.NONE, null, null, "", wifiConf);
                }
                else
                {
                    Object linkProperties = getLinkPropertiesField(wifiConf);
                    Field mHttpProxyField = ReflectionUtils.getField(linkProperties.getClass().getDeclaredFields(), "mHttpProxy");
                    mHttpProxyField.setAccessible(true);
                    Object mHttpProxy = mHttpProxyField.get(linkProperties);

                    if (mHttpProxy != null)
                    {
                        Field mHostField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mHost");
                        mHostField.setAccessible(true);
                        String mHost = (String) mHostField.get(mHttpProxy);

                        Field mPortField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mPort");
                        mPortField.setAccessible(true);
                        Integer mPort = (Integer) mPortField.get(mHttpProxy);

                        Field mExclusionListField = ReflectionUtils.getField(mHttpProxy.getClass().getDeclaredFields(), "mExclusionList");
                        mExclusionListField.setAccessible(true);
                        String mExclusionList = (String) mExclusionListField.get(mHttpProxy);

                        //LogWrapper.d(TAG, "Proxy configuration: " + mHost + ":" + mPort + " , Exclusion List: " + mExclusionList);

                        proxyHost = new ProxyConfiguration(ProxySetting.STATIC, mHost, mPort, mExclusionList, wifiConf);
                    }
                }
            }
            else
            {
                APL.getEventReport().send(new Exception("Cannot find "));
                proxyHost = new ProxyConfiguration(ProxySetting.NONE, null, null, "", wifiConf);
            }
        }
        catch (Exception e)
        {
            APL.getEventReport().send(e);
        }

        return proxyHost;
    }

    private static Object getProxySettingsField(WifiConfiguration wifiConf) throws Exception
    {
        Field proxySettingsField = null;
        Object proxySettings = null;

        if (Build.VERSION.SDK_INT >= 20)
        {
            Field mIpConfigurationField = ReflectionUtils.getField(wifiConf.getClass().getDeclaredFields(), "mIpConfiguration");
            if (mIpConfigurationField != null)
            {
                mIpConfigurationField.setAccessible(true);
                Object mIpConfiguration = mIpConfigurationField.get(wifiConf);
                if (mIpConfiguration != null)
                {
                    proxySettingsField = ReflectionUtils.getField(mIpConfiguration.getClass().getFields(), "proxySettings");
                    proxySettings = proxySettingsField.get(mIpConfiguration);
                }
            }
        }
        else
        {
            proxySettingsField = ReflectionUtils.getField(wifiConf.getClass().getFields(), "proxySettings");
            proxySettings = proxySettingsField.get(wifiConf);
        }

        return proxySettings;
    }

    private static Object getLinkPropertiesField(WifiConfiguration wifiConf) throws Exception
    {
        Field proxySettingsField = null;
        Object proxySettings = null;

        if (Build.VERSION.SDK_INT >= 20)
        {
            Field mIpConfigurationField = ReflectionUtils.getField(wifiConf.getClass().getDeclaredFields(), "mIpConfiguration");
            if (mIpConfigurationField != null)
            {
                mIpConfigurationField.setAccessible(true);
                Object mIpConfiguration = mIpConfigurationField.get(wifiConf);
                if (mIpConfiguration != null)
                {
                    proxySettingsField = ReflectionUtils.getField(mIpConfiguration.getClass().getFields(), "linkProperties");
                    proxySettings = proxySettingsField.get(mIpConfiguration);
                }
            }
        }
        else
        {
            proxySettingsField = ReflectionUtils.getField(wifiConf.getClass().getFields(), "linkProperties");
            proxySettings = proxySettingsField.get(wifiConf);
        }

        return proxySettings;
    }

    @Deprecated
    @TargetApi(12)
    public static List<ProxyConfiguration> getProxiesConfigurations()
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

//        LogWrapper.startTrace(TAG,"getConfiguredNetworks", Log.DEBUG);
        List<ProxyConfiguration> proxyConfigurations = new ArrayList<ProxyConfiguration>();
        List<WifiConfiguration> configuredNetworks = getWifiManager().getConfiguredNetworks();
//        LogWrapper.stopTrace(TAG,"getConfiguredNetworks", Log.DEBUG);

//        LogWrapper.startTrace(TAG,"getProxySdk12", Log.DEBUG);
        if (configuredNetworks != null)
        {
            for (WifiConfiguration wifiConf : configuredNetworks)
            {
                ProxyConfiguration conf = getProxySdk12(wifiConf);
                proxyConfigurations.add(conf);
            }
        }
//        LogWrapper.stopTrace(TAG,"getProxySdk12", Log.DEBUG);


        // Commented out sorting, not useful here
//        LogWrapper.startTrace(TAG,"sortConfigurations", Log.DEBUG);
//        if (proxyConfigurations.size() > 0)
//            Collections.sort(proxyConfigurations);
//        LogWrapper.stopTrace(TAG,"sortConfigurations", Log.DEBUG);

        return proxyConfigurations;
    }


    /**
     * Get proxy configuration for Wi-Fi access point. Valid for API >= 12
     */
    @Deprecated
    @TargetApi(12)
    public static void writeProxySdk12(ProxyConfiguration apConf) throws Exception
    {
        if (!sSetupCalled && gContext == null)
            throw new RuntimeException("you need to call setup() first");

        if (apConf.ap == null)
        {
            Exception e = new Exception("Doesn't seems a valid Wi-Fi access point");
            throw e;
        }

        if (apConf.ap.security == SecurityType.SECURITY_EAP)
        {
            Exception e = new Exception("writeConfiguration does not support Wi-Fi security 802.1x");
            throw e;
        }

        WifiManager wifiManager = (WifiManager) APL.getContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();

        if (configuredNetworks == null || configuredNetworks.size() == 0)
            throw new Exception("Cannot find any configured network during writing configuration to the device: " + apConf.toShortString());

        WifiConfiguration selectedConfiguration = null;
        for (WifiConfiguration conf : configuredNetworks)
        {
            if (conf.networkId == apConf.ap.wifiConfig.networkId)
            {
                selectedConfiguration = conf;
                break;
            }
        }

        if (selectedConfiguration != null)
        {
            Constructor wfconfconstr = WifiConfiguration.class.getConstructors()[1];
            WifiConfiguration newConf = (WifiConfiguration) wfconfconstr.newInstance((Object) selectedConfiguration);

            if (Build.VERSION.SDK_INT >= 20)
            {
                Field mIpConfigurationField = ReflectionUtils.getField(newConf.getClass().getDeclaredFields(), "mIpConfiguration");
                if (mIpConfigurationField != null)
                {
                    mIpConfigurationField.setAccessible(true);
                    Object mIpConfiguration = mIpConfigurationField.get(newConf);
                    if (mIpConfiguration != null)
                    {
                        Field proxySettingsField = ReflectionUtils.getField(mIpConfiguration.getClass().getFields(), "proxySettings");
                        proxySettingsField.set(mIpConfiguration, (Object) proxySettingsField.getType().getEnumConstants()[apConf.getProxySettings().ordinal()]);
                    }
                }
            }
            else
            {
                Field proxySettingsField = newConf.getClass().getField("proxySettings");
                proxySettingsField.set(newConf, (Object) proxySettingsField.getType().getEnumConstants()[apConf.getProxySettings().ordinal()]);
            }

            Object proxySettings = getProxySettingsField(newConf);
            int ordinal = ((Enum) proxySettings).ordinal();
            if (ordinal != apConf.getProxySettings().ordinal())
                throw new Exception("Cannot set proxySettings variable");

            Object linkProperties = null;
            if (Build.VERSION.SDK_INT >= 20)
            {
                Field mIpConfigurationField = ReflectionUtils.getField(newConf.getClass().getDeclaredFields(), "mIpConfiguration");
                if (mIpConfigurationField != null)
                {
                    mIpConfigurationField.setAccessible(true);
                    Object mIpConfiguration = mIpConfigurationField.get(newConf);
                    if (mIpConfiguration != null)
                    {
                        Field linkPropertiesField = ReflectionUtils.getField(mIpConfiguration.getClass().getFields(), "linkProperties");
                        linkProperties = linkPropertiesField.get(mIpConfiguration);
                    }
                }
            }
            else
            {
                Field linkPropertiesField = newConf.getClass().getField("linkProperties");
                linkProperties = linkPropertiesField.get(newConf);
            }

            Field mHttpProxyField = ReflectionUtils.getField(linkProperties.getClass().getDeclaredFields(), "mHttpProxy");
            mHttpProxyField.setAccessible(true);

            if (apConf.getProxySettings() == ProxySetting.NONE || apConf.getProxySettings() == ProxySetting.UNASSIGNED)
            {
                mHttpProxyField.set(linkProperties, null);
            }
            else if (apConf.getProxySettings() == ProxySetting.STATIC)
            {
                Class ProxyPropertiesClass = mHttpProxyField.getType();
                Integer port = apConf.getProxyPort();

                if (port == null)
                {
                    Constructor constr = ProxyPropertiesClass.getConstructors()[0];
                    Object ProxyProperties = constr.newInstance((Object) null);
                    mHttpProxyField.set(linkProperties, ProxyProperties);
                }
                else
                {
                    Constructor constr;

                    // NOTE: Hardcoded sdk version number.
                    // Instead of comparing against Build.VERSION_CODES.KITKAT, we directly compare against the version
                    // number to allow devs to compile with an older version of the sdk.
                    if (Build.VERSION.SDK_INT < 19)
                    {
                        constr = ProxyPropertiesClass.getConstructors()[1];
                    }
                    else if (Build.VERSION.SDK_INT == 19)
                    {
                        // SDK 19 = KITKAT
                        constr = ProxyPropertiesClass.getConstructors()[3];
                    }
                    else
                    {
                        // SDK 20 = L
                        constr = ProxyPropertiesClass.getConstructors()[4];
                    }

                    Object ProxyProperties = constr.newInstance(apConf.getProxyHostString(), port, apConf.getProxyExclusionList());
                    mHttpProxyField.set(linkProperties, ProxyProperties);
                }
            }

//            Object mHttpProxy = mHttpProxyField.get(linkProperties);
//            mHttpProxy = mHttpProxyField.get(linkProperties);

            APL.getLogger().startTrace(TAG,"saveWifiConfiguration", Log.DEBUG);
            ReflectionUtils.saveWifiConfiguration(wifiManager, newConf);
            APL.getLogger().getPartial(TAG,"saveWifiConfiguration", Log.DEBUG);
            /***************************************************************************************
             * TODO: improve method adding callback in order to return the result of the operation
             */
            boolean succesfullySaved = false;
            int tries = 0;
            while (tries < 10)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                ProxyConfiguration savedConf = APL.getProxySdk12(newConf);
                succesfullySaved = apConf.isSameConfiguration(savedConf);

                if (succesfullySaved)
                {
                    apConf.updateConfiguration(savedConf);
                    break;
                }

                tries++;
            }

            if (!succesfullySaved)
            {
                throw new Exception(String.format("Cannot save proxy configuration after %s tries", tries));
            }
            /**************************************************************************************/

            APL.getLogger().stopTrace(TAG,"saveWifiConfiguration", Log.DEBUG);
            apConf.status.clear();

            APL.getLogger().d(TAG, String.format("Succesfully updated configuration %s, after %d tries", apConf.toShortString(), tries));

            APL.getLogger().i(TAG, "Sending broadcast intent: " + APLIntents.APL_UPDATED_PROXY_CONFIGURATION);
            Intent intent = new Intent(APLIntents.APL_UPDATED_PROXY_CONFIGURATION);
            APL.getContext().sendBroadcast(intent);
        }
        else
        {
            throw new Exception("Cannot find selected configuration among configured networks during writing to the device: " + apConf.toShortString());
        }
    }
}
