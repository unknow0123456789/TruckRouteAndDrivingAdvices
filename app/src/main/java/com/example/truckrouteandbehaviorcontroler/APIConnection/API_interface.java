package com.example.truckrouteandbehaviorcontroler.APIConnection;
import com.example.truckrouteandbehaviorcontroler.APIConnection.CustomAPI_ReceiveClasses.Json_MPG_Receiver;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingPLaces;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingRouteMatrix;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingRoutes;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingSTR;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses.ComputeRouteBody;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses.ComputeRouteMatrixBody;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface API_interface {

    //MAP API
    @GET("place/findplacefromtext/json")
    Call<Json_ReceivingPLaces> GetGasStation(@Query("fields")String RequestedFields,
                                             @Query("input")String InputText,
                                             @Query("inputtype")String TypeOfInput,
                                             @Query("locationbias") String RadiusLatLng,
                                             @Query("key")String APIKey);

    //ROUTE API
    @POST("directions/v2:computeRoutes")
    Call<Json_ReceivingRoutes> GetRoutes(@Header("X-Goog-Api-Key")String APIKey,
                                         @Header("X-Goog-FieldMask")String RequestedFields,
                                         @Body ComputeRouteBody computeRouteBody);

    @POST("distanceMatrix/v2:computeRouteMatrix")
    Call<ArrayList<Json_ReceivingRouteMatrix>> GetRouteMatrix(@Header("X-Goog-Api-Key")String APIKey,
                                                              @Header("X-Goog-FieldMask")String RequestedFields,
                                                              @Body ComputeRouteMatrixBody computeRouteMatrixBody);

    //ROAD API

    @GET("v1/snapToRoads")
    Call<Json_ReceivingSTR> GetRoutePoints(@Query("interpolate") boolean Interpolate,
                                           @Query("path") String Path,
                                           @Query("key") String APIKey);

    //Custom API
    @GET("ML/MPG")
    Call<Json_MPG_Receiver> GetMPG();

}
