package android.nfc.cts;

import static android.Manifest.permission.NFC_SET_CONTROLLER_ALWAYS_ON;
import static android.nfc.NfcOemExtension.HCE_ACTIVATE;
import static android.nfc.NfcRoutingTableEntry.TYPE_AID;
import static android.nfc.NfcRoutingTableEntry.TYPE_PROTOCOL;
import static android.nfc.NfcRoutingTableEntry.TYPE_SYSTEM_CODE;
import static android.nfc.NfcRoutingTableEntry.TYPE_TECHNOLOGY;
import static android.nfc.cardemulation.CardEmulation.PROTOCOL_AND_TECHNOLOGY_ROUTE_ESE;
import static android.nfc.cardemulation.CardEmulation.PROTOCOL_AND_TECHNOLOGY_ROUTE_UNSET;

import static com.android.compatibility.common.util.PropertyUtil.getVsrApiLevel;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Flags;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAntennaInfo;
import android.nfc.NfcOemExtension;
import android.nfc.NfcRoutingTableEntry;
import android.nfc.OemLogItems;
import android.nfc.RoutingStatus;
import android.nfc.RoutingTableAidEntry;
import android.nfc.RoutingTableProtocolEntry;
import android.nfc.RoutingTableSystemCodeEntry;
import android.nfc.RoutingTableTechnologyEntry;
import android.nfc.T4tNdefNfcee;
import android.nfc.T4tNdefNfceeCcFileInfo;
import android.nfc.Tag;
import android.nfc.WlcListenerDeviceInfo;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.IsoDep;
import android.nfc.tech.TagTechnology;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.RequiresDevice;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RunWith(JUnit4.class)
public class NfcAdapterTest {

    private static final long MAX_POLLING_PAUSE_TIMEOUT = 40000;
    static final int MOCK_NATIVE_HANDLE = 0;
    static boolean sTagRemoved = false;
    @Mock private DevicePolicyManager mDevicePolicyManager;
    private Context mContext;
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private boolean supportsHardware() {
        final PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    @Before
    public void setUp() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
        assumeTrue("Device must support NFC", supportsHardware());
        // Backup the original service. It is being overridden
        // when creating a mocked adapter.
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mContext);
        Assume.assumeNotNull("NFC Adapter is null", adapter);
        assumeTrue("NFC Adapter could not be enabled", NfcUtils.enableNfc(adapter, mContext));
    }

    @Test
    public void testGetDefaultAdapter() {
        NfcAdapter adapter = getDefaultAdapter();
        Assert.assertNotNull(adapter);
    }

    @Test
    public void testAddAndRemoveNfcUnlockHandler() {
        NfcAdapter adapter = getDefaultAdapter();
        CtsNfcUnlockHandler unlockHandler = new CtsNfcUnlockHandler();

        adapter.addNfcUnlockHandler(unlockHandler, new String[]{"IsoDep"});
        adapter.removeNfcUnlockHandler(unlockHandler);
    }

    @Test
    public void testEnableAndDisable() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        Assert.assertTrue(adapter.isEnabled());

        // Disable NFC
        Assert.assertTrue(NfcUtils.disableNfc(adapter, mContext));
        Assert.assertFalse(adapter.isEnabled());

        // Re-enable NFC
        Assert.assertTrue(NfcUtils.enableNfc(adapter, mContext));
        Assert.assertTrue(adapter.isEnabled());
    }

    @Test
    public void testEnableAndDisablePersist() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        Assert.assertTrue(adapter.isEnabled());

        // Disable NFC
        Assert.assertTrue(NfcUtils.disableNfc(adapter, mContext, /* persist = */ false));
        Assert.assertFalse(adapter.isEnabled());

        // Re-enable NFC
        Assert.assertTrue(NfcUtils.enableNfc(adapter, mContext));
        Assert.assertTrue(adapter.isEnabled());
    }

    @Test
    public void testEnableAndDisableForegroundDispatch() throws RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        Activity activity = createAndResumeActivity();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
            NfcFCardEmulationActivity.class);
        PendingIntent pendingIntent
            = PendingIntent.getActivityAsUser(ApplicationProvider.getApplicationContext(), 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, null,
            UserHandle.of(mContext.getUser().getIdentifier()));
        String[][] techLists = new String[][]{new String[]{}};

        adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists);
        adapter.disableForegroundDispatch(activity);
    }

    @Test
    public void testEnableAndDisableReaderMode() {
        NfcAdapter adapter = getDefaultAdapter();
        Activity activity = createAndResumeActivity();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
            NfcFCardEmulationActivity.class);
        PendingIntent pendingIntent
            = PendingIntent.getActivityAsUser(ApplicationProvider.getApplicationContext(), 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, null,
            UserHandle.of(mContext.getUser().getIdentifier()));
        String[][] techLists = new String[][]{new String[]{}};
        // Move activity to the foreground
        adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists);

        adapter.enableReaderMode(activity, new CtsReaderCallback(),
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, new Bundle());
        adapter.disableReaderMode(activity);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_READER_OPTION)
    public void testEnableAndDisableReaderOption() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        assumeTrue(adapter.isReaderOptionSupported());

        Assert.assertTrue(adapter.enableReaderOption(/* enable = */ true));
        Assert.assertTrue(adapter.isReaderOptionEnabled());

        Assert.assertTrue(adapter.enableReaderOption(/* enable = */ false));
        Assert.assertFalse(adapter.isReaderOptionEnabled());
    }

    @Test
    public void testEnableAndDisableSecureNfc() throws RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        assumeTrue(adapter.isSecureNfcSupported());

        Assert.assertTrue(adapter.enableSecureNfc(/* enable = */ true));
        Assert.assertTrue(adapter.isSecureNfcEnabled());

        Assert.assertTrue(adapter.enableSecureNfc(/* enable = */ false));
        Assert.assertFalse(adapter.isSecureNfcEnabled());
    }

    @Test
    public void testGetNfcAntennaInfo() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        NfcAntennaInfo antenna = adapter.getNfcAntennaInfo();
        Assert.assertNotNull(antenna);
    }

    @Test
    public void testIgnore() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        CtsOnTagRemovedListener listener = new CtsOnTagRemovedListener();
        sTagRemoved = false;
        IsoDep isoDep = createIsoDepTag();

        Assert.assertTrue(adapter.ignore(isoDep.getTag(), 0, listener, null));
        // Wait a second to make sure listener callback fired
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
        Assert.assertTrue(sTagRemoved);
    }

    @Test
    public void testEnableThenDisableControllerAlwaysOnSupported()
        throws NoSuchFieldException, InterruptedException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .getUiAutomation().adoptShellPermissionIdentity(NFC_SET_CONTROLLER_ALWAYS_ON);
        assumeTrue(adapter.isControllerAlwaysOnSupported());
        NfcControllerAlwaysOnListener cb = null;
        CountDownLatch countDownLatch;
        try {
            countDownLatch = new CountDownLatch(1);
            cb = new NfcControllerAlwaysOnListener(countDownLatch);
            adapter.registerControllerAlwaysOnListener(Executors.newSingleThreadExecutor(), cb);
            Assert.assertTrue(adapter.setControllerAlwaysOn(true));
            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
            Assert.assertTrue(adapter.isControllerAlwaysOn());
            adapter.unregisterControllerAlwaysOnListener(cb);

            countDownLatch = new CountDownLatch(1);
            cb = new NfcControllerAlwaysOnListener(countDownLatch);
            adapter.registerControllerAlwaysOnListener(Executors.newSingleThreadExecutor(), cb);
            Assert.assertTrue(adapter.setControllerAlwaysOn(false));
            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
            Assert.assertFalse(adapter.isControllerAlwaysOn());
            adapter.unregisterControllerAlwaysOnListener(cb);
        } finally {
            if (cb != null)
                adapter.unregisterControllerAlwaysOnListener(cb);
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().dropShellPermissionIdentity();
        }

    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_MAINLINE)
    public void testAdapterState() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();

        Assert.assertTrue(NfcUtils.enableNfc(adapter, mContext));
        Assert.assertTrue(adapter.getAdapterState() == NfcAdapter.STATE_ON);

        Assert.assertTrue(NfcUtils.disableNfc(adapter, mContext));
        Assert.assertTrue(adapter.getAdapterState() == NfcAdapter.STATE_OFF);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_SET_DISCOVERY_TECH)
    public void testResetDiscoveryTechnology() {
        NfcAdapter adapter = getDefaultAdapter();
        Activity activity = createAndResumeActivity();
        adapter.setDiscoveryTechnology(activity, NfcAdapter.FLAG_READER_KEEP,
                NfcAdapter.FLAG_LISTEN_KEEP);
        adapter.resetDiscoveryTechnology(activity);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_SET_DISCOVERY_TECH)
    public void testSetDiscoveryTechnology() {
        NfcAdapter adapter = getDefaultAdapter();
        Activity activity = createAndResumeActivity();
        adapter.setDiscoveryTechnology(activity,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                | NfcAdapter.FLAG_READER_NFC_F,
                NfcAdapter.FLAG_LISTEN_NFC_PASSIVE_A | NfcAdapter.FLAG_LISTEN_NFC_PASSIVE_B
                | NfcAdapter.FLAG_LISTEN_NFC_PASSIVE_F);
        adapter.resetDiscoveryTechnology(activity);
        adapter.setDiscoveryTechnology(activity, NfcAdapter.FLAG_READER_DISABLE,
                NfcAdapter.FLAG_LISTEN_KEEP);
        adapter.resetDiscoveryTechnology(activity);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_SET_DEFAULT_DISC_TECH)
    public void testSetDefaultDiscoveryTechnology() {
        NfcAdapter adapter = getDefaultAdapter();
        Activity activity = createAndResumeActivity();
        adapter.setDiscoveryTechnology(activity,
                NfcAdapter.FLAG_READER_KEEP,
                NfcAdapter.FLAG_LISTEN_NFC_PASSIVE_B
                | NfcAdapter.FLAG_SET_DEFAULT_TECH);
        adapter.setDiscoveryTechnology(activity, NfcAdapter.FLAG_READER_KEEP,
                NfcAdapter.FLAG_LISTEN_KEEP | NfcAdapter.FLAG_SET_DEFAULT_TECH | 0xff);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_MAINLINE)
    public void testSetReaderMode() {
        NfcAdapter adapter = getDefaultAdapter();
        // Verify the API does not crash or throw any exceptions.
        adapter.setReaderModePollingEnabled(true);
        adapter.setReaderModePollingEnabled(false);
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    @RequiresFlagsDisabled(android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED)
    public void testAllowTransaction() {
        ComponentName originalDefault = null;
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        try {
            originalDefault = setDefaultPaymentService(CtsMyHostApduService.class);
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
            assumeTrue(adapter.isObserveModeSupported());
            boolean result = adapter.setObserveModeEnabled(false);
            Assert.assertTrue(result);
        } finally {
            setDefaultPaymentService(originalDefault);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    @RequiresFlagsDisabled(android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED)
    public void testDisallowTransaction() {
        ComponentName originalDefault = null;
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        try {
            originalDefault = setDefaultPaymentService(CtsMyHostApduService.class);

            assumeTrue(adapter.isObserveModeSupported());
            boolean result = adapter.setObserveModeEnabled(true);
            Assert.assertTrue(result);
        } finally {
            setDefaultPaymentService(originalDefault);
            adapter.notifyHceDeactivated();
        }
    }


    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    @RequiresFlagsDisabled(android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED)
    public void testDefaultObserveModePaymentDynamic() {
        ComponentName originalDefault = null;
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        try {
            cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                    CustomHostApduService.class), true);
            originalDefault = setDefaultPaymentService(CustomHostApduService.class);
            CardEmulationTest.ensurePreferredService(CustomHostApduService.class, mContext);
            Assert.assertTrue(adapter.isObserveModeEnabled());
            setDefaultPaymentService(CtsMyHostApduService.class);
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
            Assert.assertFalse(adapter.isObserveModeEnabled());
        } finally {
            cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                    CustomHostApduService.class), false);
            setDefaultPaymentService(originalDefault);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    public void testDefaultObserveModeForegroundDynamic() {
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        assumeTrue(adapter.isObserveModeSupported());
        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        try {
            Activity activity = createAndResumeActivity();
            cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                    CustomHostApduService.class), true);
            cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                    CtsMyHostApduService.class), false);
            Assert.assertTrue(cardEmulation.setPreferredService(activity,
                    new ComponentName(mContext, CustomHostApduService.class)));
            CardEmulationTest.ensurePreferredService(CustomHostApduService.class, mContext);
            Assert.assertTrue(adapter.isObserveModeEnabled());
            Assert.assertTrue(cardEmulation.setPreferredService(activity,
                    new ComponentName(mContext, CtsMyHostApduService.class)));
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
            adapter.notifyHceDeactivated();
            Assert.assertFalse(adapter.isObserveModeEnabled());
        } finally {
            cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                    CustomHostApduService.class), false);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    public void testDefaultObserveModeOnlyWithServiceChange() {
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        assumeTrue(adapter.isObserveModeSupported());
        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                CtsMyHostApduService.class), true);
        WalletRoleTestUtils.runWithRole(mContext, WalletRoleTestUtils.CTS_PACKAGE_NAME, () -> {
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
            Assert.assertTrue(adapter.isObserveModeEnabled());
            Assert.assertTrue(adapter.setObserveModeEnabled(false));
            Assert.assertFalse(adapter.isObserveModeEnabled());
            try {
                Activity activity = createAndResumeActivity();
                Assert.assertTrue(cardEmulation.setPreferredService(activity,
                        new ComponentName(mContext, CtsMyHostApduService.class)));
                CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
                Assert.assertFalse(adapter.isObserveModeEnabled());
                Assert.assertTrue(adapter.setObserveModeEnabled(true));
                Assert.assertTrue(adapter.isObserveModeEnabled());
                Assert.assertTrue(cardEmulation.setPreferredService(activity,
                        new ComponentName(mContext, CustomHostApduService.class)));
                CardEmulationTest.ensurePreferredService(CustomHostApduService.class, mContext);
                Assert.assertFalse(adapter.isObserveModeEnabled());
            } finally {
                cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                        CustomHostApduService.class), false);
                cardEmulation.setShouldDefaultToObserveModeForService(new ComponentName(mContext,
                        CtsMyHostApduService.class), false);
                adapter.notifyHceDeactivated();
            }
        });
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    @RequiresFlagsDisabled(android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED)
    public void testDefaultObserveModePayment() {
        ComponentName originalDefault = null;
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        try {
            originalDefault = setDefaultPaymentService(BackgroundHostApduService.class);
            CardEmulationTest.ensurePreferredService(BackgroundHostApduService.class, mContext);
            Assert.assertTrue(adapter.isObserveModeEnabled());
            setDefaultPaymentService(CtsMyHostApduService.class);
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
            Assert.assertFalse(adapter.isObserveModeEnabled());
        } finally {
            setDefaultPaymentService(originalDefault);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(android.nfc.Flags.FLAG_NFC_OBSERVE_MODE)
    public void testDefaultObserveModeForeground() {
        NfcAdapter adapter = getDefaultAdapter();
        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        cardEmulation.setShouldDefaultToObserveModeForService(
            new ComponentName(mContext, CtsMyHostApduService.class), false);
        Activity activity = createAndResumeActivity();
        adapter.notifyHceDeactivated();
        assumeTrue(adapter.isObserveModeSupported());
        Assert.assertTrue(cardEmulation.setPreferredService(activity,
                new ComponentName(mContext, BackgroundHostApduService.class)));
        CardEmulationTest.ensurePreferredService(BackgroundHostApduService.class, mContext);
        Assert.assertTrue(adapter.isObserveModeEnabled());
        Assert.assertTrue(cardEmulation.setPreferredService(activity,
                new ComponentName(mContext, CtsMyHostApduService.class)));
        CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);
        Assert.assertFalse(adapter.isObserveModeEnabled());
    }

    @Test
    @RequiresFlagsEnabled({android.nfc.Flags.FLAG_NFC_OBSERVE_MODE,
            android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED})
    public void testAllowTransaction_walletRoleEnabled() {
        WalletRoleTestUtils.runWithRole(mContext, WalletRoleTestUtils.CTS_PACKAGE_NAME, () -> {
            NfcAdapter adapter = getDefaultAdapter();
            adapter.notifyHceDeactivated();
            assumeTrue(adapter.isObserveModeSupported());
            adapter.setObserveModeEnabled(false);
            Assert.assertFalse(adapter.isObserveModeEnabled());
            adapter.notifyHceDeactivated();
        });
    }

    @Test
    @RequiresFlagsEnabled({android.nfc.Flags.FLAG_NFC_OBSERVE_MODE,
            android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED})
    public void testDisallowTransaction_walletRoleEnabled() {
        WalletRoleTestUtils.runWithRole(mContext, WalletRoleTestUtils.CTS_PACKAGE_NAME, () -> {
            NfcAdapter adapter = getDefaultAdapter();
            adapter.notifyHceDeactivated();
            assumeTrue(adapter.isObserveModeSupported());
            adapter.setObserveModeEnabled(true);
            Assert.assertTrue(adapter.isObserveModeEnabled());
            adapter.notifyHceDeactivated();
        });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_CHARGING)
    public void testDisableAndEnableNfcCharging() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();

        // Disable charging feature
        Assert.assertTrue(adapter.setWlcEnabled(false));
        Assert.assertFalse(adapter.isWlcEnabled());

        // Enable charging feature
        Assert.assertTrue(adapter.setWlcEnabled(true));
        Assert.assertTrue(adapter.isWlcEnabled());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_VENDOR_CMD)
    public void testSendVendorCmd() throws InterruptedException, RemoteException {
        assumeTrue(getVsrApiLevel() > Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
        CountDownLatch rspCountDownLatch = new CountDownLatch(1);
        CountDownLatch ntfCountDownLatch = new CountDownLatch(1);
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcVendorNciCallback cb =
                new NfcVendorNciCallback(rspCountDownLatch, ntfCountDownLatch);
        try {
            nfcAdapter.registerNfcVendorNciCallback(
                    Executors.newSingleThreadExecutor(), cb);

            // Android GET_CAPS command
            int gid = 0xF;
            int oid = 0xC;
            byte[] payload = new byte[1];
            payload[0] = 0;
            nfcAdapter.sendVendorNciMessage(NfcAdapter.MESSAGE_TYPE_COMMAND, gid, oid, payload);

            // Wait for response.
            assertThat(rspCountDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(cb.gid).isEqualTo(gid);
            assertThat(cb.oid).isEqualTo(oid);
            assertThat(cb.payload).isNotEmpty();
        } finally {
            nfcAdapter.unregisterNfcVendorNciCallback(cb);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_STATE_CHANGE)
    public void testEnableByDeviceOwner() throws NoSuchFieldException, RemoteException {
        denyPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS);
        when(mDevicePolicyManager.getDeviceOwnerUser())
                .thenReturn(new UserHandle(UserHandle.getCallingUserId()));
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(ComponentName.createRelative("com.android.nfc", ".AdapterTest"));
        when(mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mDevicePolicyManager);
        NfcAdapter adapter = getDefaultAdapter();
        boolean result = adapter.enable();
        Assert.assertTrue(result);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_STATE_CHANGE)
    public void testDisableByDeviceOwner() throws NoSuchFieldException, RemoteException {
        denyPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS);
        when(mDevicePolicyManager.getDeviceOwnerUser())
                .thenReturn(new UserHandle(UserHandle.getCallingUserId()));
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(ComponentName.createRelative("com.android.nfc", ".AdapterTest"));
        when(mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mDevicePolicyManager);
        NfcAdapter adapter = getDefaultAdapter();
        boolean result = adapter.disable();
        Assert.assertTrue(result);
        result = adapter.enable();
        Assert.assertTrue(result);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OBSERVE_MODE)
    public void testShouldDefaultToObserveModeAfterNfcOffOn() throws InterruptedException {
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        assumeTrue(adapter.isObserveModeSupported());
        adapter.notifyHceDeactivated();
        Activity activity = createAndResumeActivity();
        final CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        ComponentName ctsService = new ComponentName(mContext, CtsMyHostApduService.class);

        try {
            Assert.assertTrue(cardEmulation.setShouldDefaultToObserveModeForService(ctsService,
                    true));

            Assert.assertTrue(cardEmulation.setPreferredService(activity, ctsService));
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);

            Assert.assertTrue(adapter.isObserveModeEnabled());
            Assert.assertTrue(NfcUtils.disableNfc(adapter, mContext));
            Assert.assertTrue(NfcUtils.enableNfc(adapter, mContext));
            Assert.assertTrue(adapter.isObserveModeEnabled());
        } finally {
            cardEmulation.setShouldDefaultToObserveModeForService(ctsService,
                    false);
            cardEmulation.unsetPreferredService(activity);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OBSERVE_MODE)
    public void testShouldDefaultToObserveModeWithNfcOff() throws InterruptedException {
        NfcAdapter adapter = getDefaultAdapter();
        adapter.notifyHceDeactivated();
        assumeTrue(adapter.isObserveModeSupported());
        Activity activity = createAndResumeActivity();
        final CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        ComponentName ctsService = new ComponentName(mContext, CtsMyHostApduService.class);
        try {
            Assert.assertTrue(NfcUtils.disableNfc(adapter, mContext));
            Assert.assertTrue(cardEmulation.setShouldDefaultToObserveModeForService(ctsService,
                    true));

            Assert.assertTrue(cardEmulation.setPreferredService(activity, ctsService));
            CardEmulationTest.ensurePreferredService(CtsMyHostApduService.class, mContext);

            Assert.assertTrue(NfcUtils.enableNfc(adapter, mContext));
            Assert.assertTrue(adapter.isObserveModeEnabled());
        } finally {
            cardEmulation.setShouldDefaultToObserveModeForService(ctsService,
                    false);
            cardEmulation.unsetPreferredService(activity);
            adapter.notifyHceDeactivated();
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OEM_EXTENSION)
    public void testOemExtension() throws InterruptedException {
        CountDownLatch tagDetectedCountDownLatch = new CountDownLatch(3);
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcOemExtension nfcOemExtension = nfcAdapter.getNfcOemExtension();
        Assert.assertNotNull(nfcOemExtension);
        NfcOemExtensionCallback cb =
                new NfcOemExtensionCallback(tagDetectedCountDownLatch);
        try {
            nfcOemExtension.registerCallback(
                    Executors.newSingleThreadExecutor(), cb);
            tagDetectedCountDownLatch.await();

            // TODO: Fix these tests as we add more functionality to this API surface.
            nfcOemExtension.clearPreference();
            nfcOemExtension.synchronizeScreenState();
            Map<String, Integer> nfceeMap = nfcOemExtension.getActiveNfceeList();
            for (var nfcee : nfceeMap.entrySet()) {
                assertThat(nfcee.getKey()).isNotEmpty();
            }
            nfcOemExtension.hasUserEnabledNfc();
            nfcOemExtension.isTagPresent();
            nfcOemExtension.pausePolling(0);
            nfcOemExtension.resumePolling();
            RoutingStatus status = nfcOemExtension.getRoutingStatus();
            status.getDefaultRoute();
            status.getDefaultIsoDepRoute();
            status.getDefaultOffHostRoute();
            nfcOemExtension.setAutoChangeEnabled(true);
            assertThat(nfcOemExtension.isAutoChangeEnabled()).isTrue();
            T4tNdefNfcee ndefNfcee = nfcOemExtension.getT4tNdefNfcee();
            assertThat(ndefNfcee).isNotNull();
            if (ndefNfcee.isSupported()) {
                byte[] ndefData = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
                assertThat(ndefNfcee.writeData(5, ndefData))
                               .isEqualTo(T4tNdefNfcee.WRITE_DATA_SUCCESS);
                assertThat(ndefNfcee.readData(5)).isEqualTo(ndefData);
                assertThat(ndefNfcee.isOperationOngoing()).isEqualTo(false);
                T4tNdefNfceeCcFileInfo ccFileInfo = ndefNfcee.readCcfile();
                assertThat(ccFileInfo).isNotNull();
                assertThat(ccFileInfo.getCcFileLength()).isGreaterThan(0);
                assertThat(ccFileInfo.getVersion()).isGreaterThan(0);
                assertThat(ccFileInfo.getFileId()).isGreaterThan(5);
                assertThat(ccFileInfo.getMaxSize()).isGreaterThan(0);
                assertThat(ccFileInfo.isReadAllowed()).isEqualTo(true);
                assertThat(ccFileInfo.isWriteAllowed()).isEqualTo(true);
                assertThat(ndefNfcee.clearData()).isEqualTo(T4tNdefNfcee.CLEAR_DATA_SUCCESS);
            }
            if (Flags.nfcOverrideRecoverRoutingTable()) {
                nfcOemExtension.overwriteRoutingTable(PROTOCOL_AND_TECHNOLOGY_ROUTE_ESE,
                        PROTOCOL_AND_TECHNOLOGY_ROUTE_UNSET, PROTOCOL_AND_TECHNOLOGY_ROUTE_UNSET,
                        PROTOCOL_AND_TECHNOLOGY_ROUTE_UNSET);
            }
            List<NfcRoutingTableEntry> entries = nfcOemExtension.getRoutingTable();
            assertThat(entries).isNotNull();
            for (NfcRoutingTableEntry entry : entries) {
                switch (entry.getType()) {
                    case TYPE_AID:
                        ((RoutingTableAidEntry) entry).getAid();
                        break;
                    case TYPE_PROTOCOL:
                        ((RoutingTableProtocolEntry) entry).getProtocol();
                        break;
                    case TYPE_TECHNOLOGY:
                        ((RoutingTableTechnologyEntry) entry).getTechnology();
                        break;
                    case TYPE_SYSTEM_CODE:
                        ((RoutingTableSystemCodeEntry) entry).getSystemCode();
                        break;
                    default:
                }
                entries.getFirst().getRouteType();
                entries.getFirst().getNfceeId();
            }
            nfcOemExtension.forceRoutingTableCommit();
            assertEquals(MAX_POLLING_PAUSE_TIMEOUT,
                    nfcOemExtension.getMaxPausePollingTimeoutMills());
        } finally {
            nfcOemExtension.unregisterCallback(cb);
        }
    }

    @Test
    @RequiresDevice
    @Ignore
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OEM_EXTENSION)
    public void testOemExtensionMaybeTriggerFirmwareUpdate()
            throws InterruptedException, RemoteException {
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcOemExtension nfcOemExtension = nfcAdapter.getNfcOemExtension();
        Assert.assertNotNull(nfcOemExtension);
        nfcOemExtension.maybeTriggerFirmwareUpdate();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OEM_EXTENSION)
    public void testOemExtensionTriggerInitialization()
            throws InterruptedException, RemoteException {
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcOemExtension nfcOemExtension = nfcAdapter.getNfcOemExtension();
        Assert.assertNotNull(nfcOemExtension);
        nfcOemExtension.triggerInitialization();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OEM_EXTENSION)
    public void testOemExtensionCallback()
            throws RemoteException, InterruptedException {
        CountDownLatch tagDetectedCountDownLatch = new CountDownLatch(5);
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcOemExtension nfcOemExtension = nfcAdapter.getNfcOemExtension();

        Assert.assertNotNull(nfcOemExtension);
        NfcOemExtension.Callback cb = new NfcOemExtensionCallback(tagDetectedCountDownLatch);
        try {
            nfcOemExtension.registerCallback(
                    Executors.newSingleThreadExecutor(), cb);
            NfcOemExtension.NfcOemExtensionCallback callback =
                    nfcOemExtension.getOemNfcExtensionCallback();
            callback.onTagConnected(true);
            tagDetectedCountDownLatch.await();
            // Test onStateUpdated
            callback.onStateUpdated(NfcAdapter.STATE_OFF);

            // Test onApplyRouting
            CountDownLatch latch = new CountDownLatch(1);
            MockResultReceiver isSkipped = new MockResultReceiver(latch);
            callback.onApplyRouting(isSkipped);
            latch.await();
            assertEquals(0, isSkipped.getResultCode());

            // Test onNdefRead
            latch = new CountDownLatch(1);
            MockResultReceiver isNdefReadSkipped = new MockResultReceiver(latch);
            callback.onNdefRead(isNdefReadSkipped);
            latch.await();
            assertEquals(1, isNdefReadSkipped.getResultCode());

            // Test onEnableRequested
            latch = new CountDownLatch(1);
            MockResultReceiver isEnableAllowed = new MockResultReceiver(latch);
            callback.onEnable(isEnableAllowed);
            latch.await();
            assertEquals(1, isEnableAllowed.getResultCode());

            // Test onDisableRequested
            latch = new CountDownLatch(1);
            MockResultReceiver isDisableAllowed = new MockResultReceiver(latch);
            callback.onDisable(isDisableAllowed);
            latch.await();
            assertEquals(1, isDisableAllowed.getResultCode());

            callback.onBootStarted();
            callback.onEnableStarted();
            callback.onDisableStarted();
            callback.onBootFinished(0);
            callback.onEnableFinished(0);
            callback.onDisableFinished(0);

            // Test onTagDispatch
            latch = new CountDownLatch(1);
            MockResultReceiver isTagDispatchSkipped = new MockResultReceiver(latch);
            callback.onTagDispatch(isTagDispatchSkipped);
            latch.await();
            assertEquals(1, isTagDispatchSkipped.getResultCode());

            // Test onRoutingChanged
            latch = new CountDownLatch(1);
            MockResultReceiver isCommitRoutingSkipped = new MockResultReceiver(latch);
            callback.onRoutingChanged(isCommitRoutingSkipped);
            latch.await();
            assertEquals(0, isCommitRoutingSkipped.getResultCode());

            callback.onHceEventReceived(HCE_ACTIVATE);
            callback.onReaderOptionChanged(true);
            callback.onCardEmulationActivated(true);
            callback.onRfFieldDetected(true);
            callback.onRfDiscoveryStarted(true);
            callback.onEeListenActivated(true);
            callback.onEeUpdated();

            // Test onGetOemAppSearchIntent
            List<String> packages = new ArrayList<>();
            packages.add("com.example.app1");
            packages.add("com.example.app2");
            latch = new CountDownLatch(1);
            MockResultReceiver intentReceiver = new MockResultReceiver(latch);
            callback.onGetOemAppSearchIntent(packages, intentReceiver);
            latch.await();
            assertNotNull(intentReceiver.getResultData());

            // Test onNdefMessage
            latch = new CountDownLatch(1);
            MockResultReceiver receiver = new MockResultReceiver(latch);
            callback.onNdefMessage(null, null, receiver);
            latch.await();
            assertEquals(1, receiver.getResultCode());

            // Test onLaunchHceAppChooserActivity
            String selectedAid = "A0000000031010";
            List<ApduServiceInfo> services = new ArrayList<>();
            ComponentName failedComponent =
                    new ComponentName("com.example", "com.example.Activity");
            String category = "android.nfc.cardemulation.category.DEFAULT";
            callback.onLaunchHceAppChooserActivity(
                    selectedAid, services, failedComponent, category);

            // Test onLaunchHceTapAgainDialog
            callback.onLaunchHceTapAgainActivity(null, category);

            // Test onRoutingTableFull
            callback.onRoutingTableFull();

            // Test onLogEventNotified
            OemLogItems logItem =
                    new OemLogItems.Builder(OemLogItems.LOG_ACTION_NFC_TOGGLE).build();
            callback.onLogEventNotified(logItem);

            // Test onExtractOemPackages
            latch = new CountDownLatch(1);
            MockResultReceiver packageReceiver = new MockResultReceiver(latch);
            callback.onExtractOemPackages(null, packageReceiver);
            latch.await();
            assertNotNull(packageReceiver.getResultData());
        } finally {
            nfcOemExtension.unregisterCallback(cb);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_OEM_EXTENSION)
    public void testOemExtensionSetControllerAlwaysOn() throws InterruptedException {
        NfcAdapter nfcAdapter = getDefaultAdapter();
        Assert.assertNotNull(nfcAdapter);
        NfcOemExtension nfcOemExtension = nfcAdapter.getNfcOemExtension();
        Assert.assertNotNull(nfcOemExtension);
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().adoptShellPermissionIdentity(NFC_SET_CONTROLLER_ALWAYS_ON);
        assumeTrue(nfcAdapter.isControllerAlwaysOnSupported());
        NfcControllerAlwaysOnListener cb = null;
        CountDownLatch countDownLatch;
        try {
            countDownLatch = new CountDownLatch(1);
            cb = new NfcControllerAlwaysOnListener(countDownLatch);
            nfcAdapter.registerControllerAlwaysOnListener(
                    Executors.newSingleThreadExecutor(), cb);
            nfcOemExtension.setControllerAlwaysOnMode(NfcOemExtension.ENABLE_TRANSPARENT);
            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
            nfcAdapter.unregisterControllerAlwaysOnListener(cb);

            countDownLatch = new CountDownLatch(1);
            cb = new NfcControllerAlwaysOnListener(countDownLatch);
            nfcAdapter.registerControllerAlwaysOnListener(
                    Executors.newSingleThreadExecutor(), cb);
            nfcOemExtension.setControllerAlwaysOnMode(NfcOemExtension.DISABLE);
            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
            nfcAdapter.unregisterControllerAlwaysOnListener(cb);
        } finally {
            if (cb != null) nfcAdapter.unregisterControllerAlwaysOnListener(cb);
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().dropShellPermissionIdentity();
        }
    }

    private class NfcControllerAlwaysOnListener implements NfcAdapter.ControllerAlwaysOnListener {
        private final CountDownLatch mCountDownLatch;

        NfcControllerAlwaysOnListener(CountDownLatch countDownLatch) {
            mCountDownLatch = countDownLatch;
        }

        @Override
        public void onControllerAlwaysOnChanged(boolean isEnabled) {
            mCountDownLatch.countDown();
        }
    }

    private class NfcOemExtensionCallback implements NfcOemExtension.Callback {
        private final CountDownLatch mTagDetectedCountDownLatch;

        NfcOemExtensionCallback(CountDownLatch countDownLatch) {
            mTagDetectedCountDownLatch = countDownLatch;
        }

        @Override
        public void onTagConnected(boolean connected) {
            mTagDetectedCountDownLatch.countDown();
        }

        @Override
        public void onStateUpdated(int state) {
        }

        @Override
        public void onApplyRouting(@NonNull Consumer<Boolean> isSkipped) {
            isSkipped.accept(false);
        }

        @Override
        public void onNdefRead(@NonNull Consumer<Boolean> isSkipped) {
            isSkipped.accept(true);
        }

        @Override
        public void onEnableRequested(@NonNull Consumer<Boolean> isAllowed) {
            isAllowed.accept(true);
        }

        @Override
        public void onDisableRequested(@NonNull Consumer<Boolean> isAllowed) {
            isAllowed.accept(true);
        }

        @Override
        public void onBootStarted() {
        }

        @Override
        public void onEnableStarted() {
        }

        @Override
        public void onDisableStarted() {
        }

        @Override
        public void onBootFinished(int status) {
        }

        @Override
        public void onEnableFinished(int status) {
        }

        @Override
        public void onDisableFinished(int status) {
        }

        @Override
        public void onTagDispatch(@NonNull Consumer<Boolean> isSkipped) {
            isSkipped.accept(true);
        }

        @Override
        public void onRoutingChanged(@NonNull Consumer<Boolean> isSkipped) {
            isSkipped.accept(false);
        }

        @Override
        public void onHceEventReceived(int action) {
        }

        @Override
        public void onReaderOptionChanged(boolean enabled) {
        }

        public void onCardEmulationActivated(boolean isActivated) {
            mTagDetectedCountDownLatch.countDown();
        }

        @Override
        public void onRfFieldDetected(boolean isActive) {
            mTagDetectedCountDownLatch.countDown();
        }

        @Override
        public void onRfDiscoveryStarted(boolean isDiscoveryStarted) {
            mTagDetectedCountDownLatch.countDown();
        }

        @Override
        public void onEeListenActivated(boolean isActivated) {
            mTagDetectedCountDownLatch.countDown();
        }

        @Override
        public void onEeUpdated() {
        }

        @Override
        public void onGetOemAppSearchIntent(@NonNull List<String> packages,
                                            @NonNull Consumer<Intent> intentConsumer) {
            intentConsumer.accept(new Intent());
        }

        @Override
        public void onNdefMessage(@NonNull Tag tag, @NonNull NdefMessage message,
                                  @NonNull Consumer<Boolean> hasOemExecutableContent) {
            hasOemExecutableContent.accept(true);
        }

        @Override
        public void onLaunchHceAppChooserActivity(@NonNull String selectedAid,
                                                  @NonNull List<ApduServiceInfo> services,
                                                  @NonNull ComponentName failedComponent,
                                                  @NonNull String category) {
        }

        @Override
        public void onLaunchHceTapAgainDialog(@NonNull ApduServiceInfo service,
                                              @NonNull String category) {
        }

        @Override
        public void onRoutingTableFull() {
        }

        @Override
        public void onLogEventNotified(@NonNull OemLogItems item) {
            item.getAction();
            item.getEvent();
            item.getCallingPid();
            item.getTag();
            item.getCommandApdu();
            item.getResponseApdu();
            item.getRfFieldEventTimeMillis();
        }

        @Override
        public void onExtractOemPackages(@NonNull NdefMessage message,
                @NonNull Consumer<List<String>> packageConsumer) {
            packageConsumer.accept(new ArrayList<>());
        }
    }

    private class MockResultReceiver extends ResultReceiver {

        int mResultCode;
        Bundle mResultData;
        CountDownLatch mLatch;

        MockResultReceiver(CountDownLatch latch) {
            super(null);
            mLatch = latch;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mResultCode = resultCode;
            mResultData = resultData;
            mLatch.countDown();
        }

        public int getResultCode() {
            return mResultCode;
        }

        public Bundle getResultData() {
            return mResultData;
        }
    }

    private class NfcVendorNciCallback implements NfcAdapter.NfcVendorNciCallback {
        private final CountDownLatch mRspCountDownLatch;
        private final CountDownLatch mNtfCountDownLatch;

        public int gid;
        public int oid;
        public byte[] payload;

        NfcVendorNciCallback(CountDownLatch rspCountDownLatch, CountDownLatch ntfCountDownLatch) {
            mRspCountDownLatch = rspCountDownLatch;
            mNtfCountDownLatch = ntfCountDownLatch;
        }

        @Override
        public void onVendorNciResponse(int gid, int oid, byte[] payload) {
            this.gid = gid;
            this.oid = oid;
            this.payload = payload;
            mRspCountDownLatch.countDown();
        }

        @Override
        public void onVendorNciNotification(int gid, int oid, byte[] payload) {
            this.gid = gid;
            this.oid = oid;
            this.payload = payload;
            mNtfCountDownLatch.countDown();
        }
    }

    private class CtsReaderCallback implements NfcAdapter.ReaderCallback {
        @Override
        public void onTagDiscovered(Tag tag) {}
    }

    private class CtsNfcUnlockHandler implements NfcAdapter.NfcUnlockHandler {
        @Override
        public boolean onUnlockAttempted(Tag tag) {
            return true;
        }
    }

    private static class CtsWlcStateListener implements NfcAdapter.WlcStateListener {
        @Override
        public void onWlcStateChanged(WlcListenerDeviceInfo deviceInfo) {}
    }

    private static class CtsOnTagRemovedListener implements NfcAdapter.OnTagRemovedListener {
        @Override
        public void onTagRemoved() {
            sTagRemoved = true;
        }
    }

    private Activity createAndResumeActivity() {
        CardEmulationTest.ensureUnlocked();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
            NfcFCardEmulationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().callActivityOnResume(activity);
        return activity;
    }

    private NfcAdapter getDefaultAdapter() {
        return NfcAdapter.getDefaultAdapter(mContext);
    }

    private IsoDep createIsoDepTag() {
        Bundle extras = new Bundle();
        extras.putByteArray(IsoDep.EXTRA_HI_LAYER_RESP, new byte[]{0x00});
        extras.putByteArray(IsoDep.EXTRA_HIST_BYTES, new byte[]{0x00});
        Tag tag = new Tag(new byte[]{0x00}, new int[]{TagTechnology.ISO_DEP},
                new Bundle[]{extras}, MOCK_NATIVE_HANDLE, 0L, null);
        return IsoDep.get(tag);
    }

    private ComponentName setDefaultPaymentService(Class serviceClass) {
        ComponentName componentName = setDefaultPaymentService(
                new ComponentName(mContext, serviceClass));
        if (componentName == null) {
            return null;
        }
        return componentName;
    }

    private ComponentName setDefaultPaymentService(ComponentName serviceName) {
        if (serviceName == null) {
            return null;
        }
        return DefaultPaymentProviderTestUtils.setDefaultPaymentService(serviceName, mContext);
    }

    private void denyPermission(String permission) {
        when(mContext.checkCallingOrSelfPermission(permission))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        doThrow(new SecurityException()).when(mContext)
                .enforceCallingOrSelfPermission(eq(permission), anyString());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NFC_CHECK_TAG_INTENT_PREFERENCE)
    public void testSetTagIntentAppPreference() throws NoSuchFieldException, RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        assumeTrue(adapter.isTagIntentAppPreferenceSupported());
        int user = mContext.getUser().getIdentifier();

        // Disallow package
        Assert.assertEquals(NfcAdapter.TAG_INTENT_APP_PREF_RESULT_SUCCESS,
            adapter.setTagIntentAppPreferenceForUser(
            user, "android.nfc.cts", /* allow = */ false));
        Map<String, Boolean> disallowMap = adapter.getTagIntentAppPreferenceForUser(user);
        Assert.assertNotNull(disallowMap);
        Assert.assertFalse(disallowMap.isEmpty());
        Assert.assertEquals(false, disallowMap.get("android.nfc.cts"));
        Assert.assertFalse(adapter.isTagIntentAllowed());

        // Allow package
        Assert.assertEquals(NfcAdapter.TAG_INTENT_APP_PREF_RESULT_SUCCESS,
            adapter.setTagIntentAppPreferenceForUser(
                user, "android.nfc.cts", /* allow = */ true));
        Map<String, Boolean> allowMap = adapter.getTagIntentAppPreferenceForUser(user);
        Assert.assertNotNull(allowMap);
        Assert.assertFalse(allowMap.isEmpty());
        Assert.assertEquals(true, allowMap.get("android.nfc.cts"));
        Assert.assertTrue(adapter.isTagIntentAllowed());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_NFC_CHARGING)
    public void testRegisterAndUnregisterWlcStateListener() throws RemoteException {
        NfcAdapter adapter = getDefaultAdapter();
        CtsWlcStateListener listener = new CtsWlcStateListener();

        adapter.registerWlcStateListener(Executors.newSingleThreadExecutor(), listener);
        adapter.unregisterWlcStateListener(listener);
    }
}
