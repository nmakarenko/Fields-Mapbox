package com.cropiotest.fields.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

public class Ring implements Parcelable {

    public ArrayList<LatLng> coordinates;

    public Ring() {
        coordinates = new ArrayList<>();
    }

    public void setCoordinates(ArrayList<LatLng> coordinates) {
        this.coordinates = coordinates;
    }

    public ArrayList<LatLng> getCoordinates() {
        return coordinates;
    }

    public static final Parcelable.Creator<Ring> CREATOR = new Parcelable.Creator<Ring>() {
        public Ring createFromParcel(Parcel in) {
            return new Ring(in);
        }

        public Ring[] newArray(int size) {
            return new Ring[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public Ring(Parcel in) {
        coordinates = in.createTypedArrayList(LatLng.CREATOR);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedList(coordinates);
    }
}
