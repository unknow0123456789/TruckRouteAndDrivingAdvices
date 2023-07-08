package com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass;

import com.google.gson.annotations.SerializedName;

public class Json_ReceivingRouteMatrix {
    @SerializedName("originIndex")
    public Double OriginalIndex;
    @SerializedName("destinationIndex")
    public Double DestinationIndex;
    @SerializedName("distanceMeters")
    public Double DistanceMeters;
    @SerializedName("duration")
    public String Duration;
}
