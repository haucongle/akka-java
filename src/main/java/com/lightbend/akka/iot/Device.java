package com.lightbend.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.lightbend.akka.iot.DeviceManager.DeviceRegistered;
import com.lightbend.akka.iot.DeviceManager.RequestTrackDevice;

public class Device extends AbstractActor {

    public static Props props(String groupId, String deviceId) {
        return Props.create(Device.class, () -> new Device(groupId, deviceId));
    }

    private String groupId, deviceId;
    private double lastTemperatureReading;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Device(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }

    @Override
    public void preStart() {
        log.info("Device actor {}-{} started", groupId, deviceId);
    }

    @Override
    public void postStop() {
        log.info("Device actor {}-{} stopped", groupId, deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RequestTrackDevice.class, r -> {
                    if (groupId.equals(r.groupId) && deviceId.equals(r.deviceId)) {
                        getSender().tell(new DeviceRegistered(), getSelf());
                    } else {
                        log.warning(
                                "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
                                r.groupId, r.deviceId, groupId, deviceId
                        );
                    }
                })
                .match(RecordTemperature.class, r -> {
                    log.info("Recorded temperature reading {} with {}", r.value, r.requestId);
                    lastTemperatureReading = r.value;
                    getSender().tell(new TemperatureRecorded(r.requestId), getSelf());
                })
                .match(ReadTemperature.class, r ->
                        getSender().tell(new RespondTemperature(r.requestId, lastTemperatureReading), getSelf())
                )
                .build();
    }

    static final class ReadTemperature {
        long requestId;

        ReadTemperature(long requestId) {
            this.requestId = requestId;
        }
    }

    static final class RespondTemperature {
        long requestId;
        double value;

        RespondTemperature(long requestId, double value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    static final class RecordTemperature {
        final long requestId;
        final double value;

        RecordTemperature(long requestId, double value) {
            this.requestId = requestId;
            this.value = value;
        }
    }

    static final class TemperatureRecorded {
        final long requestId;

        TemperatureRecorded(long requestId) {
            this.requestId = requestId;
        }
    }
}
