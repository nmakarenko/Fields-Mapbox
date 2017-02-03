package com.cropiotest.fields.model;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class Polygon implements Parcelable {
    public Map<Integer, Ring> rings;

    public Polygon() {
        rings = new HashMap<>();
    }

    public void putRing(int ringId, Ring ring) {
        rings.put(ringId, ring);
    }

    public Ring getRing(int ringId) {
        return rings.get(ringId);
    }

    public static final Parcelable.Creator<Polygon> CREATOR = new Parcelable.Creator<Polygon>() {
        public Polygon createFromParcel(Parcel in) {
            return new Polygon(in);
        }

        public Polygon[] newArray(int size) {
            return new Polygon[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public Polygon(Parcel in) {
        int size = in.readInt();
        rings = new HashMap<>(size);
        for(int i = 0; i < size; i++){
            Integer key = in.readInt();
            Ring value = new Ring(in);
            rings.put(key, value);
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(rings.size());
        for(Map.Entry<Integer, Ring> entry : rings.entrySet()){
            out.writeInt(entry.getKey());
            entry.getValue().writeToParcel(out, flags);
        }
    }
}
