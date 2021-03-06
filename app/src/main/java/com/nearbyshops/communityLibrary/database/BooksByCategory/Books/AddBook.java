package com.nearbyshops.communityLibrary.database.BooksByCategory.Books;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nearbyshops.communityLibrary.database.DaggerComponentBuilder;
import com.nearbyshops.communityLibrary.database.Dialogs.DateDialog;
import com.nearbyshops.communityLibrary.database.Model.Book;
import com.nearbyshops.communityLibrary.database.Model.BookCategory;
import com.nearbyshops.communityLibrary.database.Model.Image;
import com.nearbyshops.communityLibrary.database.R;
import com.nearbyshops.communityLibrary.database.RetrofitRestContract.BookService;
import com.nearbyshops.communityLibrary.database.Utility.ImageCalls;
import com.nearbyshops.communityLibrary.database.Utility.ImageCropUtility;
import com.nearbyshops.communityLibrary.database.Utility.UtilityGeneral;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;


import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.inject.Inject;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBook extends AppCompatActivity implements Callback<Image> , DateDialog.NotifyDate{


    @Inject
    BookService bookService;

    boolean isImageAdded = false;

//    public static final String ITEM_CATEGORY_ID_KEY = "itemCategoryIDKey";

    @BindView(R.id.bookName)
    EditText bookName;

    @BindView(R.id.authorName)
    EditText authorName;

    @BindView(R.id.bookDescription)
    EditText bookDescription;
//
//    @BindView(R.id.date_of_publish)
//    EditText dateOfPublish;

    @BindView(R.id.publisher_name)
    EditText publisherName;

    @BindView(R.id.pages_total)
    EditText pagesTotal;

    @BindView(R.id.addBookButton)
    Button addBookButton;


    BookCategory itemCategory;


    public AddBook() {

        DaggerComponentBuilder.getInstance()
                .getNetComponent()
                .Inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_item);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        itemCategory = getIntent().getParcelableExtra(BookFragment.ADD_ITEM_INTENT_KEY);


        if (savedInstanceState == null) {
            // delete previous file in the cache - This will prevent accidently uploading the previous image
            File file = new File(getCacheDir().getPath() + "/" + "SampleCropImage.jpeg");
            file.delete();

            //showMessageSnackBar("File delete Status : " + String.valueOf(file.delete()));

        }
    }





    public void onDateNotified(Calendar calendar) {

        date = new Timestamp(calendar.getTimeInMillis());

        dateText.setText("Date of Publish :\n" + date.toString());


//        Log.d("date",date.toGMTString());
    }




    void addNewItem(String imagePath) {


        Book itemForEdit = new Book();

        itemForEdit.setBookCoverImageURL(imagePath);


        if (itemCategory != null) {

            itemForEdit.setBookCategoryID(itemCategory.getBookCategoryID());
        }

       /* if(date!=null)
        {
            itemForEdit.setDateOfPublish(date);
            Log.d("date",itemForEdit.getDateOfPublish().toString());
        }*/

        itemForEdit.setBookName(bookName.getText().toString());
        itemForEdit.setAuthorName(authorName.getText().toString());
        itemForEdit.setBookDescription(bookDescription.getText().toString());


//        itemForEdit.setDateOfPublish(dateOfPublish.getText().toString());

        itemForEdit.setDateOfPublish(new Timestamp(calendar.getTimeInMillis()));

        itemForEdit.setNameOfPublisher(publisherName.getText().toString());

        if(!pagesTotal.getText().toString().equals(""))
        {
            itemForEdit.setPagesTotal(Integer.parseInt(pagesTotal.getText().toString()));
        }


        Call<Book> itemCall = bookService.insertBook(itemForEdit);


        itemCall.enqueue(new Callback<Book>() {
            @Override
            public void onResponse(Call<Book> call, Response<Book> response) {


                if (response.code() == 201) {
                    //showMessageSnackBar("Item added Successfully !");
                    showToastMessage(getString(R.string.create_book_successful));
                }

                //Item responseItem = response.body();
                //displayResult(responseItem);

            }

            @Override
            public void onFailure(Call<Book> call, Throwable t) {

                //showMessageSnackBar("Network request failed !");

                showToastMessage(getString(R.string.network_not_available));

            }
        });

    }



    @OnClick(R.id.addBookButton)
    void addItem() {

        if (isImageAdded) {

            ImageCalls.getInstance().uploadPickedImage(this, REQUEST_CODE_READ_EXTERNAL_STORAGE, this);

        } else {
            addNewItem(null);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }




    /*
        Utility Methods
     */


    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.uploadImage)
    ImageView resultView;

    void showMessageSnackBar(String message) {

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();

    }


    void loadImage(String imagePath) {

        Picasso.with(this).load(UtilityGeneral.getImageEndpointURL(this) + imagePath).into(resultView);

        //getServiceURL() + IMAGES_END_POINT_URL +
    }








    /*
        // Code for Changeing / picking image and saving it in the cache folder
    */


    // code for changing / picking image and saving it in the cache folder


    @OnClick(R.id.removePicture)
    void removeImage() {

        File file = new File(getCacheDir().getPath() + "/" + "SampleCropImage.jpeg");
        file.delete();

        resultView.setImageDrawable(null);

        // reset the flag to reflect the status of image addition
        isImageAdded = false;
    }



    @BindView(R.id.textChangePicture)
    TextView changePicture;


    @OnClick(R.id.textChangePicture)
    void pickShopImage() {

        //  mDestinationUri = Uri.fromFile(new File(getCacheDir(), SAMPLE_CROPPED_IMAGE_NAME));

        //Log.d("applog", "Cache Dir Path : " + getCacheDir().getPath());

        resultView.setImageDrawable(null);
        //Crop.pickImage(this);

        ImageCropUtility.showFileChooser(this);

        //showFileChooser();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {

        super.onActivityResult(requestCode, resultCode, result);


        if (requestCode == ImageCropUtility.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && result != null
                && result.getData() != null) {


            Uri filePath = result.getData();

            //imageUri = filePath;

            if (filePath != null) {

                ImageCropUtility.startCropActivity(result.getData(), this);

                //startCropActivity(result.getData());
            }
        }


        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {

            resultView.setImageURI(UCrop.getOutput(result));

            isImageAdded = true;


        } else if (resultCode == UCrop.RESULT_ERROR) {

            final Throwable cropError = UCrop.getError(result);

        }// request crop
    }



    /*

    // Code for Uploading Image

     */


    // Upload the image after picked up
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 56;

    Image image = null;





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case REQUEST_CODE_READ_EXTERNAL_STORAGE:

                //uploadPickedImage();

                addItem();

                break;
        }
    }


    @Override
    public void onResponse(Call<Image> call, Response<Image> response) {

        image = response.body();

        loadImage(image.getPath());

        if (image != null) {

            addNewItem(image.getPath());

        } else {

            addNewItem(null);

            showToastMessage(getString(R.string.image_upload_failed));
        }

    }

    @Override
    public void onFailure(Call<Image> call, Throwable t) {


        showToastMessage(getString(R.string.image_upload_failed));

        addNewItem(null);
    }


    void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }





    Timestamp date;

    @BindView(R.id.set_date)
    TextView dateText;


    int year, month, day = -1;
    int hourOfDay, minutes = -1;

    Calendar calendar = Calendar.getInstance();

    @BindView(R.id.date_time_label)
    TextView labelDateTime;

    boolean isDateSet = false;




    @OnClick(R.id.set_date)
    void setDateClick()
    {
        DialogFragment newFragment = new DateDialog();
        newFragment.show(getSupportFragmentManager(), "datePicker");

    }



    @Override
    public void onDateNotified(int year, int month, int day) {


        this.year = year;
        this.month = month;
        this.day = day;

        calendar.set(Calendar.YEAR,year);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DATE,day);

//        dateText.setText("Date of Meetup :\n" + calendar.getTime().toString());

        isDateSet = true;

        setDateTimeLabel();
    }



    void setDateTimeLabel()
    {

        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format_simple));
        labelDateTime.setText("Date of Publish : " + dateFormat.format(calendar.getTime()));
//        labelDateTime.setText(calendar.getTime().toString());
    }




}
