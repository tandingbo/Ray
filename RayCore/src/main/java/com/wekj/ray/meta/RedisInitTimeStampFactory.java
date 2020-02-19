package com.wekj.ray.meta;

import com.wekj.ray.support.redis.RedisConnections;
import com.wekj.ray.util.ScheduleUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @author liuna
 */
public class RedisInitTimeStampFactory implements InitTimeStampFactory {

    private static final String LAST_TIME = "last:time";

    public RedisInitTimeStampFactory() {
        ScheduleUtil.schedule(() -> {
            AtomicReference<TimeStampAndSequence> atomicReference = TimeAndSequences.getAtomicReference();
            Long maxTimeStamp = Stream.of(atomicReference)
                    .filter(Objects::nonNull)
                    .map(AtomicReference::get)
                    .map(TimeStampAndSequence::getTimestamp)
                    .max(Comparator.comparing(x -> x))
                    .orElse(0L);
            String remoteValue = RedisConnections.getConnection().sync().get(LAST_TIME);
            long remoteTimeStamp = parseLong(remoteValue);
            if (maxTimeStamp > remoteTimeStamp) {
                RedisConnections.getConnection().sync().set(LAST_TIME, Objects.toString(maxTimeStamp));
            }
        }, 60 * 1000, 60 * 1000);
    }

    @Override
    public long getTimeStamp() {
        String value = RedisConnections.getConnection().sync().get(LAST_TIME);
        return parseLong(value);
    }

    private long parseLong(String value) {
        return value == null ? 0 : Long.parseLong(value);
    }
}