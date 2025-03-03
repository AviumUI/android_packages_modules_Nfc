package android.nfc.cts;

import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.*;
import android.os.RemoteException;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NfcFCardEmulationTest {
    private NfcAdapter mAdapter;
    private static final ComponentName mService =
        new ComponentName("android.nfc.cts", "android.nfc.cts.CtsMyHostNfcFService");

    private boolean supportsHardware() {
        final PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF);
    }

    @Before
    public void setUp() throws RemoteException {
        assumeTrue("Device must support NFC type F HCE", supportsHardware());
        Context mContext = InstrumentationRegistry.getContext();
        mAdapter = NfcAdapter.getDefaultAdapter(mContext);
        Assert.assertNotNull("NFC Adapter is null", mAdapter);
    }

    @Test
    public void getNonNullInstance() {
        NfcFCardEmulation instance = NfcFCardEmulation.getInstance(mAdapter);
        Assert.assertNotNull(instance);
    }

    @Test
    public void testRegisterAndUnregisterSystemCodeForService() throws RemoteException {
        NfcFCardEmulation instance = getInstance();
        String code = "4000";

        // Register system code
        Assert.assertTrue(instance.registerSystemCodeForService(mService, code));
        Assert.assertEquals(instance.getSystemCodeForService(mService), code);

        // Unregister system code
        Assert.assertTrue(instance.unregisterSystemCodeForService(mService));
        Assert.assertNotEquals(instance.getSystemCodeForService(mService), code);

        // Re-register system code future tests
        Assert.assertTrue(instance.registerSystemCodeForService(mService, code));
    }

    @Test
    public void testSetAndGetNfcid2ForService() throws RemoteException {
        NfcFCardEmulation instance = getInstance();
        String testNfcid2 = "02FE000000000000";
        Assert.assertTrue(instance.setNfcid2ForService(mService, testNfcid2));
        Assert.assertEquals(instance.getNfcid2ForService(mService), testNfcid2);
    }

    @Test
    public void testEnableAndDisableService() throws RemoteException {
        NfcFCardEmulation instance = getInstance();
        Activity activity = createAndResumeActivity();

        Assert.assertTrue(instance.enableService(activity, mService));
        Assert.assertTrue(instance.disableService(activity));
    }

    private Activity createAndResumeActivity() {
        CardEmulationTest.ensureUnlocked();
        Intent intent
            = new Intent(ApplicationProvider.getApplicationContext(),
            NfcFCardEmulationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().callActivityOnResume(activity);
        return activity;
    }

    private NfcFCardEmulation getInstance() {
        return NfcFCardEmulation.getInstance(mAdapter);
    }
}
