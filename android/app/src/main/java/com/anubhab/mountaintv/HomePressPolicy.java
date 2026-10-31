package com.anubhab.mountaintv;

/** Consume a Home gesture only once; repeats, cancellation and held presses stay distinct. */
final class HomePressPolicy {
    enum Action { NONE, HOME, VOICE }
    private boolean armed;
    private long pressedAt;
    void down(long now,boolean repeat) {
        if(!repeat) { armed=true; pressedAt=now; }
    }
    Action up(long now,boolean canceled,long holdMillis) {
        if(!armed) return Action.NONE;
        armed=false;
        if(canceled || now<pressedAt) return Action.NONE;
        return now-pressedAt>=holdMillis ? Action.VOICE : Action.HOME;
    }
    void cancel() { armed=false; }
}
