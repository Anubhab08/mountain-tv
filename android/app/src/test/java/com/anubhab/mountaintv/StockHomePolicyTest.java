package com.anubhab.mountaintv;

import org.junit.Test;
import static org.junit.Assert.*;

public class StockHomePolicyTest {
    private static final String OWN="com.anubhab.mountaintv";
    @Test public void excludesSelfToPreventLauncherLoop() {
        assertFalse(StockHomePolicy.isCandidate(OWN,OWN,".MainActivity"));
    }
    @Test public void excludesSystemResolversAndRecoveryScreens() {
        assertFalse(StockHomePolicy.isCandidate(OWN,"android","com.android.internal.app.ResolverActivity"));
        assertFalse(StockHomePolicy.isCandidate(OWN,"com.android.settings",".FallbackHome"));
        assertFalse(StockHomePolicy.isCandidate(OWN,"com.vendor.factory",".HomeActivity"));
        assertFalse(StockHomePolicy.isCandidate(OWN,"com.google.setupwizard",".Home"));
    }
    @Test public void permitsActualGoogleAndVendorLaunchers() {
        assertTrue(StockHomePolicy.isCandidate(OWN,"com.google.android.apps.tv.launcherx",".home.HomeActivity"));
        assertTrue(StockHomePolicy.isCandidate(OWN,"com.google.android.tvlauncher",".MainActivity"));
        assertTrue(StockHomePolicy.isCandidate(OWN,"com.vendor.launcher",".HomeActivity"));
    }
    @Test public void missingMetadataCannotBeLaunched() {
        assertFalse(StockHomePolicy.isCandidate(OWN,null,".Main"));
        assertFalse(StockHomePolicy.isCandidate(OWN,"com.vendor.launcher",null));
    }
}
