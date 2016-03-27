package ioio.examples.watchdog;

public interface TimeoutObserver {

    /**
     * Called when the watchdog times out.
     *
     * @param w the watchdog that timed out.
     */
    void timeoutOccured(Watchdog w);
}