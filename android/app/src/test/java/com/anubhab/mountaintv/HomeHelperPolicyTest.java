package com.anubhab.mountaintv;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeHelperPolicyTest {
    private static final String PKG="com.google.android.tvlauncher", ACT=PKG+".MainActivity";
    @Test public void bootProbeRunsOnceAndOnlyDuringStartup() {
        assertTrue(HomeHelperPolicy.shouldProbeAtBoot(12,11,45_000,false));
        assertFalse(HomeHelperPolicy.shouldProbeAtBoot(12,12,45_000,false));
        assertFalse(HomeHelperPolicy.shouldProbeAtBoot(12,11,180_000,false));
    }
    @Test public void pausedOrUnknownBootNeverProbes() {
        assertFalse(HomeHelperPolicy.shouldProbeAtBoot(12,11,45_000,true));
        assertFalse(HomeHelperPolicy.shouldProbeAtBoot(-1,11,45_000,false));
    }
    @Test public void redirectsOnlyTheSelectedHomeActivity() {
        assertTrue(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,ACT,false));
        assertTrue(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,".MainActivity",false));
    }
    @Test public void leavesStockSearchAndMovieDetailsAlone() {
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,PKG+".entity.EntityDetailsActivity",false));
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,PKG+".search.SearchActivity",false));
    }
    @Test public void dotSuspendsRedirectionUntilMountainIsReopened() {
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,ACT,true));
    }
    @Test public void unrelatedAppsAndSelfCannotTriggerIt() {
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,"com.anubhab.mountaintv",ACT,false));
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,"com.amazon.amazonvideo.livingroom",ACT,false));
    }
    @Test public void incompleteEventsAndMissingTargetAreIgnored() {
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,null,ACT,false));
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,ACT,PKG,null,false));
        assertFalse(HomeHelperPolicy.shouldRedirect(PKG,null,PKG,ACT,false));
    }
}
