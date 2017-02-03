package com.cropiotest.fields;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cropiotest.fields.model.Field;
import com.cropiotest.fields.model.Polygon;
import com.cropiotest.fields.model.Ring;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ListActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout llToMap;
    RecyclerView rvFields;
    ArrayList<Field> fields;
    Map<Long, Field> fieldsHM;

    FieldsRVAdapter adapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Field list");
        }

        llToMap = (LinearLayout) toolbar.findViewById(R.id.llToMap);
        llToMap.setOnClickListener(this);

        if (savedInstanceState != null && savedInstanceState.containsKey("list_fields")) {
            fields = savedInstanceState.getParcelableArrayList("list_fields");
        } else {
            fields = new ArrayList<>();
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            loadData();
        }

        rvFields = (RecyclerView) findViewById(R.id.rvFields);
        adapter = new FieldsRVAdapter(this, fields, new FieldsRVAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(getApplicationContext(), FieldInfoActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(API.KEY_FIELD, fields.get(position));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        rvFields.setAdapter(adapter);
        rvFields.setLayoutManager(layoutManager);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llToMap:
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(API.KEY_FIELDS, fields);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
        }
    }

    void clearTables() {
        String sqlStmt = "DELETE FROM fields;";
        MyDBHelper.getInstance(getApplicationContext()).openDatabase().execSQL(sqlStmt);
        MyDBHelper.getInstance(getApplicationContext()).closeDatabase();
        sqlStmt = "DELETE FROM coordinates;";
        MyDBHelper.getInstance(getApplicationContext()).openDatabase().execSQL(sqlStmt);
        MyDBHelper.getInstance(getApplicationContext()).closeDatabase();
    }

    void loadData() {
        clearTables();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API.BASE_URL)
                .addConverterFactory(new ToStringConverterFactory())
                .build();

        API service = retrofit.create(API.class);

        Call<String> call = service.getData();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, final Response<String> response) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String name;
                            String crop;
                            double tillArea;
                            JSONObject jObjResponse = new JSONObject(response.body());
                            JSONArray jArrFeatures = jObjResponse.optJSONArray("features");
                            for (int i = 0; i < jArrFeatures.length(); i++) {
                                JSONObject jObjFeature = jArrFeatures.optJSONObject(i);
                                JSONObject jObjProperties = jObjFeature.optJSONObject("properties");
                                name = jObjProperties.optString("name");
                                crop = jObjProperties.optString("crop");
                                tillArea = jObjProperties.optDouble("till_area");

                                Field field = new Field(name, crop, tillArea);
                                long fieldId = insertField(field);
                                field.setId(fieldId);

                                JSONObject jObjGeometry = jObjFeature.optJSONObject("geometry");
                                JSONArray jArrCoordinates = jObjGeometry.optJSONArray("coordinates");

                                for (int j = 0; j < jArrCoordinates.length(); j++) {//polygons
                                    JSONArray jArrPolygon = jArrCoordinates.optJSONArray(j);

                                    for (int k = 0; k < jArrPolygon.length(); k++) {//rings
                                        JSONArray jArrRing = jArrPolygon.optJSONArray(k);

                                        for (int p = 0; p < jArrRing.length(); p++) {//points
                                            JSONArray jArrPoint = jArrRing.optJSONArray(p);
                                            double x = jArrPoint.optDouble(1);
                                            double y = jArrPoint.optDouble(0);

                                            insertCoordinate(fieldId,x, y, j, k);
                                        }
                                    }
                                }
                            }

                            selectFields();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFields() {
        final String selectStmt = "SELECT * FROM coordinates LEFT JOIN fields ON fields.id = coordinates.field_id;";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    fieldsHM = new HashMap<>();
                    Cursor c = MyDBHelper.getInstance(getApplicationContext()).openDatabase().rawQuery(selectStmt, null);
                    int columns = c.getColumnCount();

                    if (!c.moveToFirst()) {
                        return;
                    }
                    while (!c.isAfterLast()) {
                        for (int i = 0; i < columns; i++) {
                            long id = c.getLong(c.getColumnIndex("id"));
                            if (!fieldsHM.containsKey(id)) {
                                String name = c.getString(c.getColumnIndex("name"));
                                String crop = c.getString(c.getColumnIndex("crop"));
                                double tillArea = c.getDouble(c.getColumnIndex("till_area"));
                                Field field = new Field(name, crop, tillArea);
                                fieldsHM.put(id, field);
                            }

                            LatLng point = new LatLng(c.getDouble(c.getColumnIndex("coord_x")),
                                    c.getDouble(c.getColumnIndex("coord_y")));
                            Field curField = fieldsHM.get(id);
                            Polygon polygon;
                            Ring ring;

                            int polygonId = c.getInt(c.getColumnIndex("polygon_id"));
                            int ringId = c.getInt(c.getColumnIndex("ring_id"));

                            if (curField.polygons.containsKey(polygonId)) {
                                polygon = curField.polygons.get(polygonId);
                                if (polygon.rings.containsKey(ringId)) {
                                    ring = polygon.rings.get(ringId);
                                    ring.coordinates.add(point);
                                } else {
                                    ring = new Ring();
                                    ring.coordinates.add(point);
                                    polygon.putRing(ringId, ring);
                                }
                            } else {
                                ring = new Ring();
                                ring.coordinates.add(point);
                                polygon = new Polygon();
                                polygon.putRing(ringId, ring);
                                curField.putPolygon(polygonId, polygon);
                            }
                            c.moveToNext();
                        }
                    }

                    c.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    MyDBHelper.getInstance(getApplicationContext()).closeDatabase();
                    for (Map.Entry<Long, Field> entry : fieldsHM.entrySet()) {
                        fields.add(entry.getValue());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            progressDialog.dismiss();
                        }
                    });
                }
            }
        }).start();
    }

    private void insertCoordinate(long fieldId, double x, double y, int polygonId, int ringId) {
        final ContentValues values = new ContentValues();
        values.put("field_id", fieldId);
        values.put("coord_x", x);
        values.put("coord_y", y);
        values.put("polygon_id", polygonId);
        values.put("ring_id", ringId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Long> callable = new Callable<Long>() {
            @Override
            public Long call() {
                return MyDBHelper.getInstance(getApplicationContext()).openDatabase().insert("coordinates", null, values);
            }
        };
        Future<Long> future = executor.submit(callable);
        executor.shutdown();
        try {
            future.get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MyDBHelper.getInstance(getApplicationContext()).closeDatabase();
        }
    }

    private long insertField(Field field) {
        final ContentValues values = new ContentValues();
        values.put("name", field.name);
        values.put("crop", field.crop);
        values.put("till_area", field.tillArea);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Long> callable = new Callable<Long>() {
            @Override
            public Long call() {
                return MyDBHelper.getInstance(getApplicationContext()).openDatabase().insert("fields", null, values);

            }
        };
        Future<Long> future = executor.submit(callable);
        executor.shutdown();
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            MyDBHelper.getInstance(getApplicationContext()).closeDatabase();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (fields != null) {
            outState.putParcelableArrayList(API.KEY_LIST_FIELDS, fields);
        }
        super.onSaveInstanceState(outState);
    }

}
