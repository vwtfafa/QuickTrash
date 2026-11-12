package org.vwtfafa.quicktrash.util;

import java.util.concurrent.atomic.AtomicInteger;

/** Process-lifetime counters for bStats custom charts. */
public final class MetricsCounters {
    private final AtomicInteger opens = new AtomicInteger();
    private final AtomicInteger confirms = new AtomicInteger();

    public void opened() { opens.incrementAndGet(); }
    public void confirmShown() { confirms.incrementAndGet(); }

    public int drainOpens() { return opens.getAndSet(0); }
    public int drainConfirms() { return confirms.getAndSet(0); }
}
