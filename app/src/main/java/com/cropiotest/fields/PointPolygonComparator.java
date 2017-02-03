package com.cropiotest.fields;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

public class PointPolygonComparator {

    public static boolean isPointInPolygon(LatLng coordsOfPoint, List<LatLng> latlngsOfPolygon) {
        int i;
        int j;
        boolean contains = false;
        for (i = 0, j = latlngsOfPolygon.size() - 1; i < latlngsOfPolygon.size(); j = i++) {
            if ((latlngsOfPolygon.get(i).getLongitude() > coordsOfPoint.getLongitude())
                    != (latlngsOfPolygon.get(j).getLongitude() > coordsOfPoint.getLongitude()) &&
                    (coordsOfPoint.getLatitude() < (latlngsOfPolygon.get(j).getLatitude()
                            - latlngsOfPolygon.get(i).getLatitude()) *
                            (coordsOfPoint.getLongitude() - latlngsOfPolygon.get(i).getLongitude())
                            / (latlngsOfPolygon.get(j).getLongitude() - latlngsOfPolygon.get(i).getLongitude())
                            + latlngsOfPolygon.get(i).getLatitude())) {
                contains = !contains;
            }
        }
        return contains;
    }
}
