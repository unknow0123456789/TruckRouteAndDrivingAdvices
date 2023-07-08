package com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ComputeRouteMatrixBody {
    @SerializedName("origins")
    JsonArray Origins;
    @SerializedName("destinations")
    JsonArray Destinations;
    @SerializedName("travelMode")
    String TravelMode;
    @SerializedName("routingPreference")
    String RoutingPreference;

    public ComputeRouteMatrixBody(LatLng origin, List<LatLng>des) {
        Origins=new JsonArray();
        Origins.add(waypoint(origin));
        Destinations=new JsonArray();
        for (LatLng latlng :
                des) {
            Destinations.add(waypoint(latlng));
        }
        TravelMode="DRIVE";
        RoutingPreference="TRAFFIC_AWARE";
    }
    private JsonObject waypoint(LatLng latLng)
    {
        JsonObject jsonObject=new JsonObject();
        JsonObject waypoint=new JsonObject();
        JsonObject location=new JsonObject();
        JsonObject latLngjson=new JsonObject();
        latLngjson.addProperty("latitude",latLng.latitude);
        latLngjson.addProperty("longitude",latLng.longitude);
        location.add("latLng",latLngjson);
        waypoint.add("location",location);
        jsonObject.add("waypoint",waypoint);
        return jsonObject;
    }
}
