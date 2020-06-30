package com.mattoverflow.andriod.bluetoothhelpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Heartrate;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Helpers {
    private Context context;
    private List<ConnectionParams> wahooDevices = new ArrayList<>();
    private HardwareConnector wahooHardwareConnector;
    private SensorConnection connectedSensor;
    private DiscoveryListener wahooDiscoveryListener;
    private final HardwareConnector.Listener wahooHardwareConnectorListener = new HardwareConnector.Listener () {
        @Override
        public void onHardwareConnectorStateChanged(@NonNull HardwareConnectorTypes.NetworkType networkType, @NonNull HardwareConnectorEnums.HardwareConnectorState hardwareConnectorState) {

        }

        @Override
        public void onFirmwareUpdateRequired(@NonNull SensorConnection sensorConnection, @NonNull String s, @NonNull String s1) {

        }
    };

    @SuppressLint("StaticFieldLeak")
    private static Helpers helpersInstance;

    @SuppressWarnings("unused")
    public static Helpers instance() {
        if(helpersInstance == null)
            helpersInstance = new Helpers();
        return helpersInstance;
    }

    @SuppressWarnings("unused")
    public void setContext(Context context){
        this.context = context;
    }

    public String getDevices(){
        Gson gson = new GsonBuilder().create();
        DevicesDto devicesDto = new DevicesDto();
        for(ConnectionParams bluetoothDevice : wahooDevices){
            Device device = new Device();
            device.name = bluetoothDevice.getName();
            device.id = bluetoothDevice.getId();
            devicesDto.devices.add(device);
        }
        return gson.toJson(devicesDto);
    }

    public boolean isDiscovering(){
        return wahooHardwareConnector.isDiscovering();
    }

    public void discover(){
        wahooHardwareConnector = new HardwareConnector ( context , wahooHardwareConnectorListener );
        wahooDiscoveryListener = new DiscoveryListener() {
            @Override
            public void onDeviceDiscovered(@NonNull ConnectionParams connectionParams) {
                if(!wahooDevices.contains(connectionParams) && connectionParams.getNetworkType() == HardwareConnectorTypes.NetworkType.BTLE) {
                    wahooDevices.add(connectionParams);
                }
            }

            @Override
            public void onDiscoveredDeviceLost(@NonNull ConnectionParams connectionParams) {
                if(wahooDevices.contains(connectionParams)){
                    int index = wahooDevices.indexOf(connectionParams);
                    wahooDevices.remove(connectionParams);
                }
            }

            @Override
            public void onDiscoveredDeviceRssiChanged(@NonNull ConnectionParams connectionParams, int i) {
                if(wahooDevices.contains(connectionParams)){
                    int index = wahooDevices.indexOf(connectionParams);
                    wahooDevices.remove(connectionParams);
                }
            }
        };
        wahooHardwareConnector.startDiscovery(wahooDiscoveryListener);
    }

    public void stop() {
        wahooHardwareConnector.stopDiscovery(wahooDiscoveryListener);
    }

    public void connect(String id){
        ConnectionParams device = getDevice(id);
        if (device != null) {
            connectedSensor = wahooHardwareConnector.requestSensorConnection(device, new SensorConnection.Listener() {
                @Override
                public void onNewCapabilityDetected(SensorConnection sensorConnection, Capability.CapabilityType capabilityType) {
                    Log.i("onNewCapabilityDetected", "Is connected: " + sensorConnection.isConnected());
                }

                @Override
                public void onSensorConnectionStateChanged(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionState sensorConnectionState) {
                    Log.i("onSensorConnectionStateChanged", sensorConnectionState.toString());
                }

                @Override
                public void onSensorConnectionError(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionError sensorConnectionError) {
                    Log.i("onSensorConnectionError", sensorConnectionError.toString());
                }
            });
        }
    }

    public boolean isConnected(){
        return connectedSensor.isConnected();
    }

    public double getHeartRate() {
        if (connectedSensor != null) {
            Heartrate heartrate = (Heartrate) connectedSensor.getCurrentCapability(Capability.CapabilityType.Heartrate);
            if (heartrate != null) {
                return heartrate.getHeartrateData().getHeartrate().asEventsPerMinute();
            } else {
                Log.i("getHeartRate", "The sensor connection does not currently support the heartrate capability");
                return 0;
            }
        } else {
            Log.i("getHeartRate", "Sensor not connected");
            return 0;
        }
    }

    private ConnectionParams getDevice(String id){
        for (ConnectionParams device : wahooDevices){
            if (device.getId().equals(id)){
                return device;
            }
        }
        return null;
    }
}

class Device{
    public String name;
    public String id;
}

class DevicesDto{
    public ArrayList<Device> devices = new ArrayList<>();
}