package com.nearbyshops.communityLibrary.database.AllBooks;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nearbyshops.communityLibrary.database.DaggerComponentBuilder;
import com.nearbyshops.communityLibrary.database.Data.LibraryDBContract;
import com.nearbyshops.communityLibrary.database.Dialogs.SortFIlterBookDialog;
import com.nearbyshops.communityLibrary.database.Model.Book;
import com.nearbyshops.communityLibrary.database.ModelEndpoint.BookEndpoint;
import com.nearbyshops.communityLibrary.database.R;
import com.nearbyshops.communityLibrary.database.RetrofitRestContract.BookService;
import com.nearbyshops.communityLibrary.database.Utility.UtilityGeneral;
import com.nearbyshops.communityLibrary.database.zzzDeprecatedCode.AllBookAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import icepick.Icepick;
import icepick.State;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BooksActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, SortFIlterBookDialog.NotifySort,
        LoaderManager.LoaderCallbacks<Cursor>{

    private static final int CURSOR_LOADER_ID = 2;
    ArrayList<Book> dataset = new ArrayList<>();

    @BindView(R.id.recyclerView)
    RecyclerView reviewsList;

    @BindView(R.id.offline_message)
    TextView offlineMessage;


    AllBookAdapter adapter;

    GridLayoutManager layoutManager;

    @Inject
    BookService bookService;

    // scroll variables
    private int limit = 30;
    @State int offset = 0;
    @State int item_count = 0;

    // step size cp should be equal to limit value at the time of initialization.
    private int step_size_cp = 30;
    @State int limit_cp = step_size_cp;
    @State int offset_cp = 0;
    @State int item_count_cp = 0;
    private BookCursorAdapter cursorAdapter;
    Cursor dataset_cp;

    Unbinder unbinder;


    // Default
    @State int current_sort_by;
    @State boolean current_whether_descending;




    public BooksActivity() {

        // Inject the dependencies using Dependency Injection
        DaggerComponentBuilder.getInstance()
                .getNetComponent().Inject(this);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_book);

        unbinder = ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




//        Cursor cursor = getContentResolver().query(LibraryDBContract.BookContract.CONTENT_URI,
//                LibraryDBContract.BookContract.PROJECTION_ALL,null,null,null);

//        item_count_cp = cursor.getCount();

        setupRecyclerView(null);
        setupSwipeContainer();


        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // set default sort options
        setDefaultSort();

        if(savedInstanceState==null)
        {

            swipeContainer.post(new Runnable() {
                @Override
                public void run() {
                    swipeContainer.setRefreshing(true);

                    dataset.clear();
                    makeNetworkCall();
                }
            });

        }



        getSupportLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);


    }


    void setDefaultSort()
    {
        current_sort_by = UtilityGeneral.getBookSortOptions(this).getSort_by();
        current_whether_descending = UtilityGeneral.getBookSortOptions(this).isWhether_descending();
    }



    void setOfflineMessage(boolean isOffline)
    {
        if(offlineMessage==null)
        {
            return;
        }

        if(isOffline)
        {
            offlineMessage.setVisibility(View.VISIBLE);

        }else
        {
            offlineMessage.setVisibility(View.GONE);
        }
    }


    void setupRecyclerView(Cursor data)
    {

//        adapter = new AllBookAdapter(dataset,this);
//        reviewsList.setAdapter(adapter);

        cursorAdapter = new BookCursorAdapter(data, this);
        reviewsList.setAdapter(cursorAdapter);

        layoutManager = new GridLayoutManager(this,1);
        reviewsList.setLayoutManager(layoutManager);

        reviewsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if(layoutManager.findLastVisibleItemPosition() == dataset_cp.getCount()-1)
                {
                    // trigger fetch next page

                    if((offset+limit)<=item_count)
                    {
                        offset = offset + limit;

                        makeNetworkCall();
                    }



//                    Cursor cursor = getContentResolver().query(LibraryDBContract.BookContract.CONTENT_URI,
//                            LibraryDBContract.BookContract.PROJECTION_ALL,null,null,null);
//
//                    item_count_cp = cursor.getCount();


                    Log.d("cursor", "Item Count : " + String.valueOf(item_count_cp));


                    if((limit_cp)<=item_count_cp)
                    {
                        Log.d("cursor", "Limit : " + String.valueOf(limit_cp));
                        limit_cp = limit_cp + step_size_cp;
                        getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID, null, BooksActivity.this);
                    }


                }

            }
        });


//        reviewsList.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL_LIST));

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);


        int spanCount = (int) (metrics.widthPixels/(150 * metrics.density));
        if(spanCount == 0)
        {
            spanCount = 1;
        }

        layoutManager.setSpanCount(spanCount);
    }



    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout swipeContainer;

    void setupSwipeContainer()
    {

        if(swipeContainer!=null) {

            swipeContainer.setOnRefreshListener(this);
            swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
        }

    }






    private void showToastMessage(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(unbinder!=null)
        {
            unbinder.unbind();
        }

    }

    @Override
    public void onRefresh() {

        dataset.clear();
        offset = 0 ; // reset the offset
        makeNetworkCall();

        limit_cp = step_size_cp;
        getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID, null, BooksActivity.this);
        Log.d("applog","refreshed");
    }



    void onRefreshSwipeIndicator()
    {

        swipeContainer.post(new Runnable() {
            @Override
            public void run() {
                swipeContainer.setRefreshing(true);

                onRefresh();
            }
        });
    }

    private void makeNetworkCall() {


        String sort_string = "";

        if(current_sort_by == SortFIlterBookDialog.SORT_BY_RATING)
        {
            sort_string = "avg_rating";
        }
        else if(current_sort_by == SortFIlterBookDialog.SORT_BY_RELEASE_DATE)
        {
            sort_string = "DATE_OF_PUBLISH";
        }
        else if(current_sort_by == SortFIlterBookDialog.SORT_BY_TITLE)
        {
            sort_string = "BOOK_NAME";
        }


        if(current_whether_descending)
        {
            sort_string = sort_string + " " + "desc NULLS LAST";
        }



        Call<BookEndpoint> call = bookService.getBooks(null,null,sort_string,
                limit,offset,null);


        call.enqueue(new Callback<BookEndpoint>() {
            @Override
            public void onResponse(Call<BookEndpoint> call, Response<BookEndpoint> response) {

                if(response.body().getResults()!=null)
                {
//                    dataset.addAll(response.body().getResults());


                    addToContentProvider(response.body().getResults());

//                    adapter.notifyDataSetChanged();
                    item_count = response.body().getItemCount();
                }



                setOfflineMessage(false);

                stopRefresh();
            }

            @Override
            public void onFailure(Call<BookEndpoint> call, Throwable t) {

//                showToastMessage("Network Request failed !");

                setOfflineMessage(true);

                stopRefresh();

            }
        });
    }



    void addToContentProvider(List<Book> books)
    {

        for(Book book : books)
        {

            ContentValues values = new ContentValues();

            values.put(Book.BOOK_ID,book.getBookID());
            values.put(Book.BOOK_CATEGORY_ID,book.getBookCategoryID());
            values.put(Book.BOOK_NAME,book.getBookName());
            values.put(Book.BOOK_COVER_IMAGE_URL,book.getBookCoverImageURL());
            values.put(Book.BACKDROP_IMAGE_URL,book.getBackdropImageURL());
            values.put(Book.AUTHOR_NAME,book.getAuthorName());
            values.put(Book.BOOK_DESCRIPTION,book.getBookDescription());
//            values.put(Book.TIMESTAMP_CREATED,book.getTimestampCreated());
            if(book.getDateOfPublish()!=null)
            {
                values.put(Book.DATE_OF_PUBLISH_LONG,book.getDateOfPublish().getTime());
            }

            values.put(Book.PUBLISHER_NAME,book.getNameOfPublisher());
            values.put(Book.PAGES_TOTAL,book.getPagesTotal());
            values.put(Book.RT_RATING_AVG,book.getRt_rating_avg());
            values.put(Book.RT_RATING_COUNT,book.getRt_rating_count());

            getContentResolver().insert(LibraryDBContract.BookContract.CONTENT_URI,values);

        }



    }



    void stopRefresh()
    {
        if(swipeContainer!=null)
        {
            swipeContainer.setRefreshing(false);
        }
    }




    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Icepick.saveInstanceState(this,outState);
        outState.putParcelableArrayList("dataset",dataset);
    }



    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Icepick.restoreInstanceState(this,savedInstanceState);

        if(savedInstanceState!=null)
        {
/*
            ArrayList<Book> tempCat = savedInstanceState.getParcelableArrayList("dataset");
            dataset.clear();
            dataset.addAll(tempCat);
            adapter.notifyDataSetChanged();
*/
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_book_all, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if(id == R.id.action_sort)
        {

//            showToastMessage("Sort !");

            action_sort();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    void action_sort()
    {
        current_sort_by = UtilityGeneral.getBookSortOptions(this).getSort_by();
        current_whether_descending = UtilityGeneral.getBookSortOptions(this).isWhether_descending();

        FragmentManager fm = getSupportFragmentManager();
        SortFIlterBookDialog sortDialog = new SortFIlterBookDialog();
        sortDialog.setCurrentSort(current_sort_by,current_whether_descending);
        sortDialog.show(fm,"sort");
    }




    @Override
    public void applySort(int sortBy, boolean whetherDescendingLocal) {

//        showToastMessage("Applied Fragment !");
        current_sort_by = sortBy;
        current_whether_descending = whetherDescendingLocal;

        UtilityGeneral.saveSortBooks(sortBy,whetherDescendingLocal);
        onRefreshSwipeIndicator();

        // restart content loader
        getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }



    //


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String sort_string = "";

        if(current_sort_by == SortFIlterBookDialog.SORT_BY_RATING)
        {
            sort_string = "RT_RATING_AVG";
        }
        else if(current_sort_by == SortFIlterBookDialog.SORT_BY_RELEASE_DATE)
        {
            sort_string = "DATE_OF_PUBLISH";
        }
        else if(current_sort_by == SortFIlterBookDialog.SORT_BY_TITLE)
        {
            sort_string = "BOOK_NAME";
        }


        if(current_whether_descending)
        {
            sort_string = sort_string + " " + "desc";
        }



        Cursor cursor = getContentResolver().query(LibraryDBContract.BookContract.CONTENT_URI,
                LibraryDBContract.BookContract.PROJECTION_ALL,null,null,null);

        item_count_cp = cursor.getCount();

//        setupRecyclerView(cursor);

        String limit_offset = " limit " + String.valueOf(limit_cp) + " offset " + String.valueOf(offset_cp);


        return new CursorLoader(this, LibraryDBContract.BookContract.CONTENT_URI,
                LibraryDBContract.BookContract.PROJECTION_ALL,null,null, sort_string + limit_offset);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        data.setNotificationUri(getContentResolver(),LibraryDBContract.BookContract.CONTENT_URI);

//        reviewsList.setAdapter(adapter);


        dataset_cp = data;

        if(cursorAdapter!=null)
        {
            cursorAdapter.swapCursor(data);
        }

        Log.d("cursor",String.valueOf(data.getCount()));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


}
