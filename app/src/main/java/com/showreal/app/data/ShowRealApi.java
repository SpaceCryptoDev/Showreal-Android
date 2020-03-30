package com.showreal.app.data;

import com.google.gson.JsonObject;
import com.showreal.app.data.model.AppSetting;
import com.showreal.app.data.model.Device;
import com.showreal.app.data.model.Like;
import com.showreal.app.data.model.LikedResponse;
import com.showreal.app.data.model.Login;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.MutualFriends;
import com.showreal.app.data.model.NewLogin;
import com.showreal.app.data.model.PasswordChange;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.QuestionsResponse;
import com.showreal.app.data.model.Report;
import com.showreal.app.data.model.RevieweeCount;
import com.showreal.app.data.model.Reviewees;
import com.showreal.app.data.model.Session;
import com.showreal.app.data.model.Settings;
import com.showreal.app.data.model.VideoResponse;
import com.showreal.app.data.model.VideoViewCount;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface ShowRealApi {

    @GET("settings")
    Observable<List<AppSetting>> getAppSettings();

    @POST("sessions/register")
    Observable<Session> register(@Body NewLogin login);

    @POST("sessions")
    Observable<Session> login(@Body Login login);

    @GET("profile/{id}/videos")
    Observable<VideoResponse> getVideos(@Path("id") int id);

    @GET("profile/{id}")
    Observable<Profile> getProfile(@Path("id") int id);

    @GET("profile/me")
    Observable<Profile> getProfile();

    @Multipart
    @PATCH("profile/me")
    Observable<Profile> updateProfile(@PartMap Map<String, RequestBody> profile);

    @PATCH("profile/me")
    Observable<Profile> updateProfile(@Body JsonObject json);

    @Multipart
    @PATCH("profile/me")
    Observable<Profile> updateProfile(@Part("profile_image\"; filename=\nphoto.jpeg\"") RequestBody image, @PartMap Map<String, RequestBody> profile);

    @POST("sessions/reset_password")
    Observable<Void> resetPassword(@Query("email") String email);

    @GET("settings/me")
    Observable<Settings> getSettings();

    @PATCH("settings/me")
    Observable<Settings> updateSettings(@Body Settings settings);

    @POST("application/me/ask_question")
    Observable<Report> postReport(@Body Report report);

    @POST("matches/report")
    Observable<Report> reportUser(@Body Report report);

    @PATCH("sessions/change_password")
    Observable<Void> changePassword(@Body PasswordChange passwordChange);

    @DELETE("profile/me")
    Observable<Void> deleteProfile();

    @GET("reviewees")
    Observable<Reviewees> getReviewees();

    @GET("reviewees/preview_reviewees")
    Observable<List<Profile>> getPreviewReviewees();

    @GET("likes")
    Observable<LikedResponse> getPotential();

    @GET("matches")
    Observable<List<Match>> getMatches();

    @DELETE("reviewees/{id}/cut")
    Observable<Void> cutUser(@Path("id") int id);

    @PATCH("reviewees/{id}/second_chance")
    Observable<Void> chanceUser(@Path("id") int id);

    @POST("likes/like")
    Observable<Like> likeUser(@Body Like like);

    @GET("profile/me/mutual_friends")
    Observable<MutualFriends> getMutualFriends(@Query("access_token") String token, @Query("friend_facebook_id") String friendId);

    @GET("profile/me/mutual_friends")
    Observable<MutualFriends> getNextMutualFriends(@Query("access_token") String token, @Query("friend_facebook_id") String friendId, @Query("after") String after);

    @PATCH("profile/me/facebook_friends")
    Observable<Void> updateFacebookToken(@Query("token") String token);

    @DELETE("matches/{id}")
    Observable<Void> deleteMatch(@Path("id") int matchId);

    @PATCH("matches/match_seen/{id}")
    Observable<Void> resetMatch(@Path("id") int matchId);

    @POST("sessions/registerdevice")
    Observable<Void> registerDevice(@Body Device device);

    @GET("questions/me")
    Observable<QuestionsResponse> getMyQuestions();

    @GET("questions")
    Observable<QuestionsResponse> getQuestions();

    @Multipart
    @POST("profile/me/videos/{id}")
    Observable<ResponseBody> uploadVideo(@Path("id") int questionId, @Part("video\"; filename=\nvideo.mp4\"") RequestBody video, @PartMap Map<String, RequestBody> data);

    @Multipart
    @PATCH("profile/me/videos/{id}")
    Observable<ResponseBody> editVideo(@Path("id") int questionId, @Part("video\"; filename=\nvideo.mp4\"") RequestBody video, @PartMap Map<String, RequestBody> data);

    @Multipart
    @PATCH("profile/me/videos/{id}")
    Observable<ResponseBody> editVideoData(@Path("id") int questionId, @PartMap Map<String, RequestBody> data);

    @DELETE("profile/me/videos/{id}")
    Observable<ResponseBody> deleteVideo(@Path("id") int id);

    @GET
    Observable<ResponseBody> downloadFile(@Url String url);

    @GET("reviewees/local_user_count")
    Observable<RevieweeCount> getLocalCount(@Query("lat") double latitude, @Query("lon") double longitude);

    @PATCH("videos/view_reel/{id}")
    Observable<VideoViewCount> videoWatched(@Path("id") int id);
}
