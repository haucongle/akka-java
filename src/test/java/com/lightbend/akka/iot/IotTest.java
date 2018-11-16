package com.lightbend.akka.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.iot.Device.ReadTemperature;
import com.lightbend.akka.iot.Device.RespondTemperature;
import org.junit.jupiter.api.*;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IotTest {
    private static final Logger LOG = Logger.getLogger(IotTest.class.getName());
    private static ActorSystem system;

    @BeforeAll
    static void setup() {
        system = ActorSystem.create();
    }

    @AfterAll
    static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @BeforeEach
    void beforeEachTest(TestInfo testInfo) {
        LOG.info(() -> String.format("About to execute [%s]",
                testInfo.getDisplayName()));
    }

    @AfterEach
    void afterEachTest(TestInfo testInfo) {
        LOG.info(() -> String.format("Finished executing [%s]",
                testInfo.getDisplayName()));
    }

    @Test
    void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
        deviceActor.tell(new ReadTemperature(42L), probe.getRef());
        RespondTemperature response = probe.expectMsgClass(RespondTemperature.class);
        assertEquals(42L, response.requestId);
        assertEquals(0.0, response.value);
    }

    @Test
    void testReplyWithLatestTemperatureReading() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new Device.RecordTemperature(1L, 24.0), probe.getRef());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new Device.ReadTemperature(2L), probe.getRef());
        Device.RespondTemperature response1 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(2L, response1.requestId);
        assertEquals(24.0, response1.value);

        deviceActor.tell(new Device.RecordTemperature(3L, 55.0), probe.getRef());
        assertEquals(3L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new Device.ReadTemperature(4L), probe.getRef());
        Device.RespondTemperature response2 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(4L, response2.requestId);
        assertEquals(55.0, response2.value);
    }

    @Test
    void testReplyToRegistrationRequests() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new DeviceManager.RequestTrackDevice("group", "device"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        assertEquals(deviceActor, probe.getLastSender());
    }

    @Test
    void testIgnoreWrongRegistrationRequests() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.getRef());
        probe.expectNoMessage();

        deviceActor.tell(new DeviceManager.RequestTrackDevice("group", "wrongDevice"), probe.getRef());
        probe.expectNoMessage();
    }
}
