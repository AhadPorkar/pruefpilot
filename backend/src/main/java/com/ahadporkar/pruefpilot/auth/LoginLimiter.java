package com.ahadporkar.pruefpilot.auth;

import java.time.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded, per-process IP limiter. Multi-node deployments additionally need an edge limiter. */
public final class LoginLimiter {
    private record Bucket(long minute,int count) {}
    private final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();
    private final Clock clock;
    public LoginLimiter(Clock clock) {this.clock=clock;}
    public boolean allow(String remote) {
        long minute=clock.instant().getEpochSecond()/60;
        if(buckets.size()>10000) buckets.entrySet().removeIf(e->e.getValue().minute()<minute);
        if(buckets.size()>=10000 && !buckets.containsKey(remote)) return false;
        Bucket b=buckets.compute(remote,(key,old)->old==null || old.minute()!=minute?new Bucket(minute,1):new Bucket(minute,old.count()+1));
        return b.count()<=12;
    }
}
