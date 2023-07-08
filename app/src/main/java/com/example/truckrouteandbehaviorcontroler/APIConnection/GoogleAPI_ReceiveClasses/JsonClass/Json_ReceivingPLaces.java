package com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

public class Json_ReceivingPLaces {
    @SerializedName("candidates")
    public JsonArray Candidates;
    @SerializedName("status")
    public String Status;
}
