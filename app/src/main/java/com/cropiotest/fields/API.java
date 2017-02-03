package com.cropiotest.fields;

import retrofit2.Call;
import retrofit2.http.GET;

public interface API {

    String KEY_FIELD = "field";
    String KEY_FIELDS = "fields";
    String KEY_LIST_FIELDS = "list_fields";
    String KEY_IS_MY_LOCATION_ENABLED = "is_my_location_enabled";

    String BASE_URL = "https://dl.dropboxusercontent.com";
    @GET("/u/2994945/android_test/fields.json")
    Call<String> getData();
}
