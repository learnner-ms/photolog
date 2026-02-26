package com.example.photolog_front.network;

import com.example.photolog_front.model.ChatMessageRequest;
import com.example.photolog_front.model.ChatMessageResponse;
import com.example.photolog_front.model.DiaryStartResponse;
import com.example.photolog_front.model.FamilyCommentRequest;
import com.example.photolog_front.model.FamilyCommentResponse;
import com.example.photolog_front.model.FamilyCreateRequest;
import com.example.photolog_front.model.FamilyJoinRequest;
import com.example.photolog_front.model.LoginRequest;
import com.example.photolog_front.model.LoginResponse;
import com.example.photolog_front.model.SignupRequest;
import com.example.photolog_front.model.SignupResponse;
import com.example.photolog_front.model.UserResponse;
import com.example.photolog_front.model.FamilyMemberResponse;
import com.example.photolog_front.model.MyDiaryListResponse;
import com.google.gson.JsonObject;
import com.example.photolog_front.model.FamilyItem;
import com.example.photolog_front.model.FamilyPostItem;


import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    // ---------------------- Auth ----------------------
    @POST("/signup")
    Call<SignupResponse> signup(@Body SignupRequest request);

    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest request);


    // ---------------------- 사진 업로드 ----------------------
    @Multipart
    @POST("/photos/upload-start")
    Call<DiaryStartResponse> uploadPhoto(
            @Part MultipartBody.Part file
    );


    // ---------------------- 챗봇 / 세션 ----------------------
    @POST("/sessions/{session_id}/answer")
    Call<ChatMessageResponse> sendChatAnswer(
            @Path("session_id") int sessionId,
            @Body ChatMessageRequest request
    );

    @POST("/sessions/{session_id}/stop")
    Call<ChatMessageResponse> stopSession(
            @Path("session_id") int sessionId
    );

    @POST("/api/chat")
    Call<ResponseBody> sendUserMessage(@Body JsonObject body);


    // ---------------------- 가족 기능 ----------------------
    @POST("/families")
    Call<Object> createFamily(@Body FamilyCreateRequest request);
    @GET("/me")
    Call<UserResponse> getUserInfo();

    @POST("/families/join")
    Call<Object> joinFamily(@Body FamilyJoinRequest request);


    //----마이페이지
    @GET("/families/{family_id}/members")
    Call<List<FamilyMemberResponse>> getFamilyMembers(
            @Path("family_id") int familyId
    );
    @GET("/diaries")
    Call<MyDiaryListResponse> getMyDiaries();

    @GET("/myfamily")
    Call<List<FamilyItem>> getMyFamily();

    @GET("/families/{familyId}/posts")
    Call<List<FamilyPostItem>> getFamilyPosts(@Path("familyId") int familyId);


    // ---------------------- 댓글 ----------------------
    @GET("/posts/{post_id}/comments")
    Call<List<FamilyCommentResponse>> getComments(@Path("post_id") int postId);

    @POST("/posts/{post_id}/cFamilyCommentResponseomments")
    Call<FamilyCommentResponse> addComment(
            @Path("post_id") int postId,
            @Body FamilyCommentRequest request
    );
}
