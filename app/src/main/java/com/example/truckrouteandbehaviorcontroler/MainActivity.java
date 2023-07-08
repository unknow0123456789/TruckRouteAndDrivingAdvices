package com.example.truckrouteandbehaviorcontroler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.example.truckrouteandbehaviorcontroler.APIConnection.API_client;
import com.example.truckrouteandbehaviorcontroler.APIConnection.API_interface;
import com.example.truckrouteandbehaviorcontroler.APIConnection.CustomAPI_ReceiveClasses.Json_MPG_Receiver;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingPLaces;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingRouteMatrix;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.JsonClass.Json_ReceivingRoutes;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses.Place;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses.ComputeRouteBody;
import com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_SendClasses.ComputeRouteMatrixBody;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.Manifest; // new

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapView mapView;
    GoogleMap map;
    LatLng LastPressedLatLng = null, StampPoint = null;
    boolean RouteBTNState = false;
    API_interface GoogleMapAPI, GoogleRoadAPI, GoogleRouteAPI,CustomServerAPI;
    ArrayList<Place> GasStationGlobal;

    ArrayList<Marker> GasStationMarkers, RouteMatrixMakers;
    Polyline CurrentPolyline;
    int CountRouteMatrixRequest;    //only use in SearchLocationRouteMatrix

    Marker LastedUsedMarker, StampMarker;

    Handler GetCurrentLocationHandler = new Handler();
    Runnable GetCurrentLocationRunnable;

    FusedLocationProviderClient fusedLocationProviderClient;

    Json_MPG_Receiver FirstRequestedMPG=null;

    static int LOCATION_REQUEST_CODE = 10001;
    private static final String TAG = "Current loc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Working ground------------------------

        //Map---------------
        mapView = findViewById(R.id.MapView);
        mapView.getMapAsync(this);
        mapView.onCreate(savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //------------------------------------


        GoogleMapAPI = API_client.getClient("https://maps.googleapis.com/maps/api/").create(API_interface.class);
        GoogleRoadAPI = API_client.getClient("https://roads.googleapis.com/").create(API_interface.class);
        GoogleRouteAPI = API_client.getClient("https://routes.googleapis.com/").create(API_interface.class);
        CustomServerAPI = API_client.getClient("https://192.168.x.x:8000/").create(API_interface.class);

        ImageButton RouteButton = findViewById(R.id.PanelCenter_ButtonPanel_RouteBTN);
        RouteButtonCustomOnClick(RouteButton);

        RouteBreaker(9.0, 500000.0, 5.0);

    }

    private void RouteButtonCustomOnClick(ImageButton routebtn) {
        routebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RouteBTNState == false) {
                    LatLng MyCurrentLocation=new LatLng(map.getMyLocation().getLatitude(),map.getMyLocation().getLongitude());
                    if (MyCurrentLocation != null) {
                        StampPoint = MyCurrentLocation;
                        //MarkerOptions StampMarkerOption = new MarkerOptions().position(StampPoint);
                        //StampMarker = map.addMarker(StampMarkerOption);
                        routebtn.setImageDrawable(getDrawable(R.mipmap.no_route_icon));
                        RouteBTNState = true;
                    }
                } else if (RouteBTNState == true) {
                    //StampMarker.remove();
                    StampPoint = null;
                    if (CurrentPolyline != null) CurrentPolyline.remove();
                    RemoveAllMarkerFromList(GasStationMarkers);
                    RemoveAllMarkerFromList(RouteMatrixMakers);
                    routebtn.setImageDrawable(getDrawable(R.mipmap.route_icon));
                    RouteBTNState = false;

                }
            }
        });
    }

    public ArrayList<Place> FindPostResponse(Object obj) {
        Json_ReceivingPLaces receivingPLaces = (Json_ReceivingPLaces) obj;
        ArrayList<Place> GasStations = new ArrayList<>();
        if (receivingPLaces.Candidates.size() > 0) {
            for (JsonElement element :
                    receivingPLaces.Candidates) {
                JsonObject currentPlace = element.getAsJsonObject();
                LatLng tempLocation = new LatLng(currentPlace.get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lat").getAsDouble(), currentPlace.get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lng").getAsDouble());
                boolean OpenState = false;
                try {
                    OpenState = currentPlace.get("opening_hours").getAsBoolean();
                } catch (Exception ex) {
                    Log.e("OpenStateError", currentPlace.get("place_id").getAsString());
                }
                Place tempPlace = new Place(currentPlace.get("place_id").getAsString(), currentPlace.get("formatted_address").getAsString(), currentPlace.get("name").getAsString(), OpenState, tempLocation);
                boolean nonExistFlag = true;
                for (Place place :
                        GasStations) {
                    if (place.ID.equals(tempPlace.ID)) {
                        nonExistFlag = false;
                        break;
                    }
                }
                if (nonExistFlag) GasStations.add(tempPlace);
            }

            Log.d("TestGasStation", receivingPLaces.Candidates.get(0).toString());
        }
        return GasStations;
    }

    public void RemoveAllMarkerFromList(ArrayList<Marker> dalist) {
        if (dalist != null) {
            for (Marker maker :
                    dalist) {
                maker.remove();
            }
        }
    }

    public void SearchForPLaces(API_interface UsingInterface, LatLng AtLocation, String SearchText, CustomResponse CR) {
        if (UsingInterface != null) {
            for (int i = 100; i <= 5000; i += 100) {
                Call<Json_ReceivingPLaces> ReceivingGasStation = UsingInterface.GetGasStation("formatted_address,name,place_id,opening_hours,geometry",
                        SearchText,
                        "textquery",
                        "circle:" + i + "@" + AtLocation.latitude + "," + AtLocation.longitude,
                        getResources().getString(R.string.google_map_key));

                Log.d("TestGasStationURL", ReceivingGasStation.request().url().toString());

                ReceivingGasStation.enqueue(new Callback<Json_ReceivingPLaces>() {
                    @Override
                    public void onResponse(Call<Json_ReceivingPLaces> call, Response<Json_ReceivingPLaces> response) {
                        CR.OnResponse(response.body());
                    }

                    @Override
                    public void onFailure(Call<Json_ReceivingPLaces> call, Throwable t) {
                        Log.e("TestGasStation", "Failed");
                    }
                });
            }
        }
    }

    public void ComputeRoute(LatLng origin, LatLng Des, API_interface RouteInterface,GoogleMap googleMap,Double MPG,Double GalonGas,int UseFor) {
        RequestRoute(origin, Des, RouteInterface, new CustomResponse() {
            @Override
            public void OnResponse(Object obj) {
                Json_ReceivingRoutes responseRoute = (Json_ReceivingRoutes) obj;
                JsonArray jsonArray = responseRoute.Routes;
                if (jsonArray != null) for (JsonElement jsonElement :
                        jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String EncodedPolyline = jsonObject.get("polyline").getAsJsonObject().get("encodedPolyline").getAsString();
                    double RouteLength=jsonObject.get("distanceMeters").getAsDouble();
                    if(UseFor==0)DrawPolyline(EncodedPolyline,googleMap,MPG,GalonGas,RouteLength);
                    if(UseFor==1)RecalculateRouteBreaker(EncodedPolyline,googleMap,MPG,GalonGas,RouteLength);
                }
            }
        });
    }
    private void RecalculateRouteBreaker(String encodedPolyline,GoogleMap googleMap,double mpg,double gas,double routeLength)
    {
        List<LatLng> ListLatLng = PolyUtil.decode(encodedPolyline);
        SearchLatLngAfterXMiles(GoogleRouteAPI, ListLatLng, RouteBreaker(mpg,routeLength,gas), new CustomResponse() {
            @Override
            public void OnResponse(Object obj) {
                ArrayList<Double> FindIndexes = (ArrayList<Double>) obj;
                RefreshBreakPoint(FindIndexes,ListLatLng);
            }
        });
    }

    public void RequestRoute(LatLng origin, LatLng Des, API_interface UsingInterface, CustomResponse CR) {
        ComputeRouteBody computeRouteBody = new ComputeRouteBody(origin, Des, "DRIVE");
        Call<Json_ReceivingRoutes> CallForRoute = UsingInterface.GetRoutes(getString(R.string.google_map_key), "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline", computeRouteBody);
        CallForRoute.enqueue(new Callback<Json_ReceivingRoutes>() {
            @Override
            public void onResponse(Call<Json_ReceivingRoutes> call, Response<Json_ReceivingRoutes> response) {
                Log.d("TestComputeRoute", response.code() + "");
                CR.OnResponse(response.body());
            }

            @Override
            public void onFailure(Call<Json_ReceivingRoutes> call, Throwable t) {
                Log.e("TestComputeRoute", t.getMessage());
            }
        });
    }

    public void DrawPolyline(String encodedPath,GoogleMap googleMap,Double MPG,Double GalonGas,Double RouteLength) {
        List<LatLng> ListLatLng = PolyUtil.decode(encodedPath);
        PolylineOptions polylineOptions = new PolylineOptions();
        for (LatLng latLng :
                ListLatLng) {
            polylineOptions.add(latLng);
        }
        if (CurrentPolyline != null) CurrentPolyline.remove();
        CurrentPolyline = googleMap.addPolyline(polylineOptions.color(getColor(R.color.blue)));


        SearchLatLngAfterXMiles(GoogleRouteAPI, CurrentPolyline.getPoints(), RouteBreaker(MPG,RouteLength,GalonGas), new CustomResponse() {
            @Override
            public void OnResponse(Object obj) {
                ArrayList<Double> FindIndexes = (ArrayList<Double>) obj;
                RefreshBreakPoint(FindIndexes,CurrentPolyline.getPoints());
            }
        });
    }
    public void RefreshBreakPoint(ArrayList<Double> FindIndexes,List<LatLng> PolylinePoints)
    {
        RemoveAllMarkerFromList(GasStationMarkers);
        GasStationMarkers=new ArrayList<>();
        RemoveAllMarkerFromList(RouteMatrixMakers);
        RouteMatrixMakers = new ArrayList<>();
        for (Double doubleIndex :
                FindIndexes) {
            int findindex = (int) Math.round(doubleIndex);
            LatLng findResult = PolylinePoints.get(findindex);
            MarkerOptions resultMarker = new MarkerOptions().position(findResult).icon(BitmapDescriptorFactory.fromBitmap(CustomBitMap(R.mipmap.out_of_gas_icon, 170, 170)));
            Marker out_Of_Gas_Marker = map.addMarker(resultMarker);
            RouteMatrixMakers.add(out_Of_Gas_Marker);
        }
    }

    public Bitmap CustomBitMap(int resourceID, int width, int height) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceID);
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    ArrayList<Double> RouteBreaker(Double MPG, Double RouteLenghtInMeters, Double GalonGas) {
        ArrayList<Double> routeBreaker = new ArrayList();
        Double RouteLenghtInMiles = RouteLenghtInMeters * 0.000621;
        Double Maximum1GasRefillRunTime = MPG * GalonGas;
        for (int i = 1; i * Maximum1GasRefillRunTime < RouteLenghtInMiles; i++) {
            routeBreaker.add(i * Maximum1GasRefillRunTime);
        }
        return routeBreaker;
    }

    ArrayList<Double> TestSearchPointArray() {
        ArrayList<Double> SearchAt = new ArrayList<>();
        SearchAt.add(10.0);
        SearchAt.add(20.0);
        SearchAt.add(30.0);
        SearchAt.add(40.0);
        SearchAt.add(50.0);
        return SearchAt;
    }

    public void SearchLatLngAfterXMiles(API_interface RouteInterface, List<LatLng> Points, ArrayList<Double> XMiles, CustomResponse CR) {
        ArrayList<Json_ReceivingRouteMatrix> result = new ArrayList<>();
        List<LatLng> SearchPoints;
        LatLng Ori = Points.get(0);
        int SearchIndexMax = Points.size(), SearchIndex = 1;
        CountRouteMatrixRequest = 0;
        while (SearchIndex < Points.size()) {
            CountRouteMatrixRequest++;
            if (Points.size() - SearchIndex > 625) SearchIndexMax = SearchIndex + 625;
            else SearchIndexMax = Points.size();
            SearchPoints = new ArrayList<>();
            for (; SearchIndex < SearchIndexMax; SearchIndex++) {
                SearchPoints.add(Points.get(SearchIndex));
            }
            RequestRouteMatrix(Ori, SearchPoints, RouteInterface, new CustomResponse() {
                @Override
                public void OnResponse(Object obj) {
                    CountRouteMatrixRequest--;
                    ArrayList<Json_ReceivingRouteMatrix> response = (ArrayList<Json_ReceivingRouteMatrix>) obj;
                    result.addAll(response);
                    if (CountRouteMatrixRequest == 0) {
                        ArrayArrange(result, 0, false);
                        ResetDestinationIndex(result);
                        ArrayList<Double> DestinationsIndex = new ArrayList<>();
                        for (Double Xmile :
                                XMiles) {
                            break_For_Xmile:
                            for (Json_ReceivingRouteMatrix point :
                                    result) {
                                double Miles = point.DistanceMeters * 0.000621;
                                if (Miles >= Xmile) {
                                    DestinationsIndex.add(point.DestinationIndex + 1);//using this index (+1 because the des array is from the polyline array which doesn't count the 0 index since it's the origin point), to track back to the Polyline Array to know the LatLng
                                    break break_For_Xmile;
                                }
                            }
                        }
                        CR.OnResponse(DestinationsIndex);
                    }
                }
            });
        }

    }

    public void ResetDestinationIndex(ArrayList<Json_ReceivingRouteMatrix> datlist) {
        for (int i = 0; i < datlist.size(); i++) {
            Json_ReceivingRouteMatrix temp = datlist.get(i);
            temp.DestinationIndex = Double.valueOf(i);
            datlist.set(i, temp);
        }
    }

    public void RequestRouteMatrix(LatLng ori, List<LatLng> des, API_interface RouteInterface, CustomResponse CR) {
        ComputeRouteMatrixBody computeRouteMatrixBody = new ComputeRouteMatrixBody(ori, des);
        Call<ArrayList<Json_ReceivingRouteMatrix>> CallRouteMatrix = RouteInterface.GetRouteMatrix(getString(R.string.google_map_key), "originIndex,destinationIndex,duration,distanceMeters", computeRouteMatrixBody);
        CallRouteMatrix.enqueue(new Callback<ArrayList<Json_ReceivingRouteMatrix>>() {
            @Override
            public void onResponse(Call<ArrayList<Json_ReceivingRouteMatrix>> call, Response<ArrayList<Json_ReceivingRouteMatrix>> response) {
                Log.d("TestRouteMatrix", "url: " + CallRouteMatrix.request().url());
                Log.d("TestRouteMatrix", "response code: " + response.code());
                if (response.body() != null) {
                    CR.OnResponse(response.body());
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Json_ReceivingRouteMatrix>> call, Throwable t) {
                Log.e("TestRouteMatrix", t.getMessage());
            }
        });
    }

    public void ArrayArrange(ArrayList<Json_ReceivingRouteMatrix> daList, int i, boolean revertCheck) {
        //Log.e("testArange", "i normal="+i);
        if (i < 0 || i + 1 >= daList.size()) {
            //Log.e("testArange", "i Log out="+i);
            return;
        }
        if (daList.get(i).DistanceMeters > daList.get(i + 1).DistanceMeters) {
            //Log.e("testArange", "i swap="+i);
            Json_ReceivingRouteMatrix temp = daList.get(i);
            daList.set(i, daList.get(i + 1));
            daList.set(i + 1, temp);
            ArrayArrange(daList, i - 1, true);
        }
        if (!revertCheck) ArrayArrange(daList, i + 1, false);
    }

    public void CallCustomAPI (API_interface CustomAPIInterface,CustomResponse CR)
    {
        if(CustomAPIInterface==null) return;
        Call<Json_MPG_Receiver> CallForMPG=CustomAPIInterface.GetMPG();
        CallForMPG.enqueue(new Callback<Json_MPG_Receiver>() {
            @Override
            public void onResponse(Call<Json_MPG_Receiver> call, Response<Json_MPG_Receiver> response) {
                Log.d("TestCustomAPI", "Code:"+response.code());
                if(response.body()!=null)CR.OnResponse(response.body());
            }

            @Override
            public void onFailure(Call<Json_MPG_Receiver> call, Throwable t) {

            }
        });
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.792330, 106.699389), 15));
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                if (RouteBTNState == true)
                    CallCustomAPI(CustomServerAPI, new CustomResponse() {
                        @Override
                        public void OnResponse(Object obj) {
                            Json_MPG_Receiver jsonMpgReceiver=(Json_MPG_Receiver)obj;
                            FirstRequestedMPG = jsonMpgReceiver;
                            Double MPG=jsonMpgReceiver.MPG, GalonGas=jsonMpgReceiver.GalonGas;
                            if(MPG!=null && GalonGas!=null)ComputeRoute(StampPoint, latLng, GoogleRouteAPI,map,MPG ,GalonGas,0);
                        }
                    });
                LastPressedLatLng = latLng;
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(latLng.latitude + "," + latLng.longitude).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                if (LastedUsedMarker != null) LastedUsedMarker.remove();
                LastedUsedMarker = googleMap.addMarker(markerOptions);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                if (RouteMatrixMakers != null) {

                    RemoveAllMarkerFromList(GasStationMarkers);
                    GasStationMarkers = new ArrayList<>();
                    for (Marker RMmarker :
                            RouteMatrixMakers) {
                        if (marker.getPosition().equals(RMmarker.getPosition())) {
                            SearchForPLaces(GoogleMapAPI, marker.getPosition(), "gas station", new CustomResponse() {
                                @Override
                                public void OnResponse(Object obj) {
                                    GasStationGlobal = new ArrayList<>();
                                    for (Place gasStation :
                                            FindPostResponse(obj)) {
                                        GasStationGlobal.add(gasStation);
                                        Marker marker = map.addMarker(new MarkerOptions().title(gasStation.Name).position(gasStation.Location).icon(BitmapDescriptorFactory.fromBitmap(CustomBitMap(R.mipmap.gas_station_icon, 170, 170))));
                                        GasStationMarkers.add(marker);
                                    }
                                }
                            });
                        }
                    }
                }
                return false;
            }
        });

         //--- new
        while(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            askLastLocationPermission();
        }
        getCurrentLocation();

        // --- new




    }

    // --- new
    private void askLastLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Log.d("Current loc", "Dialog showed !");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(@NonNull Location location) {
                CallCustomAPI(CustomServerAPI, new CustomResponse() {
                    @Override
                    public void OnResponse(Object obj) {
                        Json_MPG_Receiver jsonMpgReceiver=(Json_MPG_Receiver)obj;
                        if(CurrentPolyline!=null)
                        {
                            LatLng destination=CurrentPolyline.getPoints().get(CurrentPolyline.getPoints().size()-1);
                            ComputeRoute(new LatLng(location.getLatitude(),location.getLongitude()),destination,GoogleRouteAPI,map,jsonMpgReceiver.MPG,jsonMpgReceiver.GalonGas,1);
                        }
                        if(FirstRequestedMPG.MPG-jsonMpgReceiver.MPG>=5)
                        {
                            if(FirstRequestedMPG.Acceleration>jsonMpgReceiver.Acceleration)
                            {
                                //Suggest to increase acceleration
                            }
                            else
                            {
                                //Suggest to decrease acceleration
                            }
                        }
                    }
                });
            }
        });
    }
    // --- new

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}