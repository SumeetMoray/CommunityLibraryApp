package com.nearbyshops.communityLibrary.database.RetrofitRestContract;


import com.nearbyshops.communityLibrary.database.Model.FavouriteBook;
import com.nearbyshops.communityLibrary.database.ModelEndpoint.FavouriteBookEndpoint;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by sumeet on 2/4/16.
 */

public interface FavouriteBookService {

    @GET("/api/v1/FavouriteBook")
    Call<FavouriteBookEndpoint> getFavouriteBooks(
            @Query("BookID")Integer bookID,
            @Query("MemberID")Integer memberID,
            @Query("SortBy") String sortBy,
            @Query("Limit") Integer limit, @Query("Offset") Integer offset,
            @Query("metadata_only") Boolean metaonly
    );


    @POST("/api/v1/FavouriteBook")
    Call<FavouriteBook> insertFavouriteBook(@Body FavouriteBook book);

    @DELETE("/api/v1/FavouriteBook")
    Call<ResponseBody> deleteFavouriteBook(@Query("BookID")Integer bookID,
                                  @Query("MemberID")Integer memberID);

}
