package com.example.photolog_front.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://54.180.51.124:8000/";
    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {

            // 📌 HTTP 로그
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 📌 최신 토큰을 매 요청마다 가져오는 Interceptor
            Interceptor authInterceptor = chain -> {
                SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
                String token = prefs.getString("token", null);

                Request original = chain.request();
                Request.Builder builder = original.newBuilder();

                if (token != null) {
                    builder.header("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            };

            // 📌 OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build();

            // 📌 Retrofit 싱글톤 생성
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }

        return retrofit.create(ApiService.class);
    }
}
