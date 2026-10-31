package com.anubhab.mountaintv;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomePressPolicyTest {
    @Test public void shortPressLaunchesHomeOnce() {
        HomePressPolicy p=new HomePressPolicy(); p.down(1000,false);
        assertEquals(HomePressPolicy.Action.HOME,p.up(1100,false,500));
        assertEquals(HomePressPolicy.Action.NONE,p.up(1110,false,500));
    }
    @Test public void repeatsDoNotTurnHeldHomeIntoShortHome() {
        HomePressPolicy p=new HomePressPolicy(); p.down(1000,false); p.down(1500,true);
        assertEquals(HomePressPolicy.Action.VOICE,p.up(1650,false,500));
    }
    @Test public void canceledGestureNeverLaunchesAnything() {
        HomePressPolicy p=new HomePressPolicy(); p.down(1000,false);
        assertEquals(HomePressPolicy.Action.NONE,p.up(1100,true,500));
        p.down(2000,false); p.cancel();
        assertEquals(HomePressPolicy.Action.NONE,p.up(2100,false,500));
    }
    @Test public void orphanedOrInvalidReleaseIsIgnored() {
        HomePressPolicy p=new HomePressPolicy(); p.down(1000,true);
        assertEquals(HomePressPolicy.Action.NONE,p.up(1100,false,500));
        p.down(2000,false);
        assertEquals(HomePressPolicy.Action.NONE,p.up(1900,false,500));
    }
}
