package com.example.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ConnectedDevice.class, RobotModel.class}, version = 1)
public abstract class LocalDatabase extends RoomDatabase {
    public abstract ConnectedDeviceDAO connectedDeviceDAO();
    public abstract RobotModelDAO robotModelDAO();
}
