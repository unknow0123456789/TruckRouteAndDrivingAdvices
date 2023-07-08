package com.example.truckrouteandbehaviorcontroler.APIConnection.GoogleAPI_ReceiveClasses;

import com.google.android.gms.maps.model.LatLng;

public class Place {
    public String ID;
    public String Address;
    public String Name;
    public boolean IsOpening;
    public LatLng Location;

    public Place(String ID, String address, String name, boolean isOpening, LatLng location) {
        this.ID = ID;
        Address = address;
        Name = name;
        IsOpening = isOpening;
        Location = location;
    }
}
