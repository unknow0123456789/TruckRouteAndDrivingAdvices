package com.example.truckrouteandbehaviorcontroler.APIConnection.CustomAPI_ReceiveClasses;

import com.google.gson.annotations.SerializedName;

public class Json_MPG_Receiver {
    @SerializedName("mpg")
    public Double MPG;
    @SerializedName("galon_gas")
    public Double GalonGas;
    @SerializedName("acceleration")
    public Double Acceleration;
}
