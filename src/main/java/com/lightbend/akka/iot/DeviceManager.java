package com.lightbend.akka.iot;

class DeviceManager {

    static final class RequestTrackDevice {
        final String groupId;
        final String deviceId;

        RequestTrackDevice(String groupId, String deviceId) {
            this.groupId = groupId;
            this.deviceId = deviceId;
        }
    }

    static final class DeviceRegistered {
    }
}