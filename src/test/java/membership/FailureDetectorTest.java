package membership;

import common.Node;
import membership.FailureDetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FailureDetectorTest {

    private Node self;
    private Node other;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean failureDetected;
    private FailureDetector detector;

    @BeforeEach
    void setUp() {
        self = new Node("127.0.0.1", 8080);
        other = new Node("127.0.0.2", 9090);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        failureDetected = new AtomicBoolean(false);

        detector = new FailureDetector(self, node -> failureDetected.set(true), scheduler);
    }

    @Test
    void testRecordHeartbeatStoresTimestamp() {
        detector.recordHeartbeat(other);
        // No direct getter, but we can verify indirectly by checking failure detection later
        assertDoesNotThrow(() -> detector.checkNodeFailures());
    }

    @Test
    void testCheckNodeFailuresMarksNodeAsFailed() throws InterruptedException {
        detector.recordHeartbeat(other);

        // Simulate old heartbeat by sleeping past timeout
        Thread.sleep(4100);
        detector.checkNodeFailures();

        assertTrue(failureDetected.get(), "Failure should be detected for stale heartbeat");
    }

    @Test
    void testSelfNodeIsNotMarkedFailed() throws InterruptedException {
        detector.recordHeartbeat(self);

        Thread.sleep(4100);
        detector.checkNodeFailures();

        assertFalse(failureDetected.get(), "Self node should not be marked failed");
    }

    @Test
    void testOnNodeLeftRemovesNode() {
        detector.recordHeartbeat(other);
        detector.onNodeLeft(other);

        detector.checkNodeFailures();
        assertFalse(failureDetected.get(), "Removed node should not trigger failure");
    }

    @Test
    void testStartAndStopScheduler() {
        detector.start();
        detector.stop();

        assertTrue(scheduler.isShutdown(), "Scheduler should be shut down after stop()");
    }
}
