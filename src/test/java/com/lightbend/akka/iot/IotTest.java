package com.lightbend.akka.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.iot.Device.ReadTemperature;
import com.lightbend.akka.iot.Device.RecordTemperature;
import com.lightbend.akka.iot.Device.RespondTemperature;
import com.lightbend.akka.iot.DeviceGroup.ReplyDeviceList;
import com.lightbend.akka.iot.DeviceGroup.RequestDeviceList;
import com.lightbend.akka.iot.DeviceManager.DeviceRegistered;
import com.lightbend.akka.iot.DeviceManager.RequestTrackDevice;
import org.junit.jupiter.api.*;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        LOG.info(() -> String.format("About to execute [%s]", testInfo.getDisplayName()));
    }

    @AfterEach
    void afterEachTest(TestInfo testInfo) {
        LOG.info(() -> String.format("Finished executing [%s]", testInfo.getDisplayName()));
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

        deviceActor.tell(new RecordTemperature(1L, 24.0), probe.getRef());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new ReadTemperature(2L), probe.getRef());
        Device.RespondTemperature response1 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(2L, response1.requestId);
        assertEquals(24.0, response1.value);

        deviceActor.tell(new RecordTemperature(3L, 55.0), probe.getRef());
        assertEquals(3L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);

        deviceActor.tell(new ReadTemperature(4L), probe.getRef());
        Device.RespondTemperature response2 = probe.expectMsgClass(Device.RespondTemperature.class);
        assertEquals(4L, response2.requestId);
        assertEquals(55.0, response2.value);
    }

    @Test
    void testReplyToRegistrationRequests() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new RequestTrackDevice("group", "device"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        assertEquals(deviceActor, probe.getLastSender());
    }

    @Test
    void testIgnoreWrongRegistrationRequests() {
        TestKit probe = new TestKit(system);
        ActorRef deviceActor = system.actorOf(Device.props("group", "device"));

        deviceActor.tell(new RequestTrackDevice("wrongGroup", "device"), probe.getRef());
        probe.expectNoMessage();

        deviceActor.tell(new RequestTrackDevice("group", "wrongDevice"), probe.getRef());
        probe.expectNoMessage();
    }

    @Test
    void testRegisterDeviceActor() {
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor1 = probe.getLastSender();

        groupActor.tell(new RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor2 = probe.getLastSender();
        assertNotEquals(deviceActor1, deviceActor2);

        // Check that the device actors are working
        deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.getRef());
        assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
        deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.getRef());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
    }

    @Tag("device-group")
    @Test
    void testIgnoreRequestsForWrongGroupId() {
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("wrongGroup", "device1"), probe.getRef());
        probe.expectNoMessage();
    }

    @Tag("device-group")
    @Test
    void testReturnSameActorForSameDeviceId() {
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor1 = probe.getLastSender();

        groupActor.tell(new RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        ActorRef deviceActor2 = probe.getLastSender();
        assertEquals(deviceActor1, deviceActor2);
    }

    @Tag("device-group")
    @Tag("device-list")
    @Test
    void testListActiveDevices() {
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);

        groupActor.tell(new RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);

        groupActor.tell(new RequestDeviceList(0L), probe.getRef());
        ReplyDeviceList reply = probe.expectMsgClass(ReplyDeviceList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
    }

    @Tag("device-group")
    @Tag("device-list")
    @Test
    void testListActiveDevicesAfterOneShutsDown() {
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);
        ActorRef toShutDown = probe.getLastSender();

        groupActor.tell(new RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(DeviceRegistered.class);

        groupActor.tell(new RequestDeviceList(0L), probe.getRef());
        ReplyDeviceList reply = probe.expectMsgClass(ReplyDeviceList.class);
        assertEquals(0L, reply.requestId);
        assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);

        probe.watch(toShutDown);
        toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender());
        probe.expectTerminated(toShutDown);

        // using awaitAssert to retry because it might take longer for the groupActor
        // to see the Terminated, that order is undefined
        probe.awaitAssert(() -> {
            groupActor.tell(new RequestDeviceList(1L), probe.getRef());
            ReplyDeviceList r =
                    probe.expectMsgClass(ReplyDeviceList.class);
            assertEquals(1L, r.requestId);
            assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
            return null;
        });
    }
}
