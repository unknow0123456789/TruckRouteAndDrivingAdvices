package com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ComputeRouteBody {
    @SerializedName("origin")
    JsonObject Origin;
    @SerializedName("destination")
    JsonObject Destination;
    @SerializedName("travelMode")
    String TravelMode;
    @SerializedName("routingPreference")
    String RoutingPreference;
    @SerializedName("computeAlternativeRoutes")
    boolean ComputeAlternativeRoutes;
    @SerializedName("routeModifiers")
    JsonObject RouteModifiers;
    @SerializedName("languageCode")
    String LanguageCode;
    @SerializedName("units")
    String Units;

    public ComputeRouteBody(LatLng OriginalLatLng, LatLng DestinationLatLng,String travelMode) {
        Origin=CreateLocation(OriginalLatLng);
        Destination=CreateLocation(DestinationLatLng);
        TravelMode=travelMode;
        RoutingPreference="TRAFFIC_AWARE";
        ComputeAlternativeRoutes=false;
        RouteModifiers=new JsonObject();
        RouteModifiers.addProperty("avoidTolls",false);
        RouteModifiers.addProperty("avoidHighways",false);
        RouteModifiers.addProperty("avoidFerries",false);
        LanguageCode="en-US";
        Units="IMPERIAL";
    }
    private JsonObject CreateLocation(LatLng latLng)
    {
        JsonObject latlngJson=new JsonObject();
        JsonObject location=new JsonObject();
        JsonObject temp=new JsonObject();
        latlngJson.addProperty("latitude",latLng.latitude);
        latlngJson.addProperty("longitude",latLng.longitude);
        location.add("latLng",latlngJson);
        temp.add("location",location);
        return temp;
    }
}
