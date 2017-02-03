package com.cropiotest.fields.model;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class Field implements Parcelable {
    long id;
    public String name;
    public String crop;
    public double tillArea;
    public Map<Integer, Polygon> polygons;

    public Field(String name, String crop, double tillArea) {
        this.name = name;
        this.crop = crop;
        this.tillArea = tillArea;
        polygons = new HashMap<>();
    }

    public void putPolygon(int polygonId, Polygon polygon) {
        polygons.put(polygonId, polygon);
    }

    public Polygon getPolygon(int polygonId) {
        return polygons.get(polygonId);
    }

    public void setId(long id) {
        this.id = id;
    }

    public static final Parcelable.Creator<Field> CREATOR = new Parcelable.Creator<Field>() {
        public Field createFromParcel(Parcel in) {
            return new Field(in);
        }

        public Field[] newArray(int size) {
            return new Field[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeString(crop);
        out.writeDouble(tillArea);

        out.writeInt(polygons.size());
        for(Map.Entry<Integer, Polygon> entry : polygons.entrySet()){
            out.writeInt(entry.getKey());
            entry.getValue().writeToParcel(out, flags);
        }
    }

    private Field(Parcel in) {
        id = in.readLong();
        name = in.readString();
        crop = in.readString();
        tillArea = in.readDouble();

        int size = in.readInt();
        polygons = new HashMap<Integer, Polygon>(size);
        for(int i = 0; i < size; i++){
            Integer key = in.readInt();
            Polygon value = new Polygon(in);
            polygons.put(key, value);
        }
    }
}
