package com.desperdartos.shoppingapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.desperdartos.shoppingapp.Constants;
import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.commands.AddProductActivityClicks;
import com.desperdartos.shoppingapp.databinding.ActivityAddProductBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddProductActivity extends AppCompatActivity {
    ActivityAddProductBinding binding;

    //UI views
    /*private ImageButton backBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedNotePriceEt, discountedPriceEt;
    private TextView categoryTv;
    private SwitchCompat discountSwitch;
    private Button addProductBtn;*/

    //Permission Constants
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;

    //permission arrays
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //image picked uri
    private Uri image_uri;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_add_product);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_product);

        //unchecked--> show/hide discount price and notes
        binding.discountedPriceEt.setVisibility(View.GONE);
        binding.discountedNotePriceEt.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        binding.setClickHandle(new AddProductActivityClicks() {
            @Override
            public void addProductBtn() {//for add product
                //1)input data
                //2)validate data
                //3)add data to database
                inputData();
            }

            @Override
            public void backBtnClick() {
                onBackPressed();
            }

            @Override
            public void productIconClick() {
                //will add validation
                showImagePickDialog();
            }

            @Override
            public void categoryTvClick() {
                //pic category
                categoryDialog();
            }
        });
        binding.discountSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //checked-->show discount price and discount note
                    binding.discountedPriceEt.setVisibility(View.VISIBLE);
                    binding.discountedNotePriceEt.setVisibility(View.VISIBLE);

                } else {
                    //unchecked   show hide discount price Et discount note Et.....
                    binding.discountedPriceEt.setVisibility(View.GONE);
                    binding.discountedNotePriceEt.setVisibility(View.GONE);
                }
            }
        });
    }


    private String productTitle, productDescription, productCategory, productQuantity, originalPrice, discountPrice, discountNote;
    private boolean discountAvailable = false;
    private void inputData() {
        //1.Input Data
        productTitle = binding.titleEt.getText().toString().trim();
        productDescription = binding.descriptionEt.getText().toString().trim();
        productCategory = binding.categoryTv.getText().toString().trim();
        productQuantity = binding.quantityEt.getText().toString().trim();
        originalPrice = binding.priceEt.getText().toString().trim();
        discountAvailable = binding.discountSwitch.isChecked();

        //2.Validate Data
        if (TextUtils.isEmpty(productTitle)){
            binding.titleEt.setError("Title is Required");
            binding.titleEt.requestFocus();
            return;  //Dont procees further
        }if (TextUtils.isEmpty(productCategory)){
            binding.categoryTv.setError("Category is Required");
            binding.categoryTv.requestFocus();
            return;
        }if (TextUtils.isEmpty(originalPrice)){
            binding.priceEt.setError("Original Price is Required");
            binding.priceEt.requestFocus();
            return;
        }if (discountAvailable){
            //Product wit discount
            discountPrice = binding.discountedPriceEt.getText().toString().trim();
            discountNote = binding.discountedNotePriceEt.getText().toString().trim();
            if (TextUtils.isEmpty(discountPrice)){
                binding.discountedPriceEt.setError("Discount  Price is Required");
                binding.discountedPriceEt.requestFocus();
                return;
            }
        }else{
            //Product without discount
            discountPrice = "0";
            discountNote = "";
        }
        //3. Add data to db
        addProduct();
    }

    private void addProduct() {
        progressDialog.setMessage("Adding Product");
        progressDialog.show();

        final String timeStamp = ""+System.currentTimeMillis();
        if (image_uri == null){
            //upload without image
            HashMap<String,Object> hashMap = new HashMap<>();
            hashMap.put("productId", ""+timeStamp);
            hashMap.put("productTitle", ""+productTitle);
            hashMap.put("productDescription", ""+productDescription);
            hashMap.put("productCategory", ""+productCategory);
            hashMap.put("productQuantity", ""+productQuantity);
            hashMap.put("productIcon", ""); //No image set
            hashMap.put("originalPrice", ""+originalPrice);
            hashMap.put("discountPrice", ""+discountPrice);
            hashMap.put("discountNote", ""+discountNote);
            hashMap.put("discountAvailable", ""+discountAvailable);
            hashMap.put("timestamp", ""+timeStamp);
            hashMap.put("uid", ""+firebaseAuth.getUid());

            //Add to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Products").child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Added to db
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this,"Product Added!", Toast.LENGTH_SHORT).show();
                            clearData(); //After saving remove words from screen
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed to add to db
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }else{
            //upload image

            //name and path of image to be uploaded
            String filePathAndName = "product_images/"+ ""+ timeStamp;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image uploaded
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();
                            if (uriTask.isSuccessful()){
                                //uri of image received
                                //upload without image
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("productId", ""+timeStamp);
                                hashMap.put("productTitle", ""+productTitle);
                                hashMap.put("productDescription", ""+productDescription);
                                hashMap.put("productCategory", ""+productCategory);
                                hashMap.put("productQuantity", ""+productQuantity);
                                hashMap.put("productIcon", ""+ downloadImageUri);
                                hashMap.put("originalPrice", ""+originalPrice);
                                hashMap.put("discountPrice", ""+discountPrice);
                                hashMap.put("discountNote", ""+discountNote);
                                hashMap.put("discountAvailable", ""+discountAvailable);
                                hashMap.put("timestamp", ""+timeStamp);
                                hashMap.put("uid", ""+firebaseAuth.getUid());

                                //Add to db
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).child("Products").child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Added to db
                                                progressDialog.dismiss();
                                                Toast.makeText(AddProductActivity.this,"Product Added!", Toast.LENGTH_SHORT).show();
                                                clearData(); //After saving remove words from screen
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed to add to db
                                                progressDialog.dismiss();
                                                Toast.makeText(AddProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Failed to upload image
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    //After saving remove words from screen
    private void clearData() {
        binding.titleEt.setText("");
        binding.descriptionEt.setText("");
        binding.categoryTv.setText("");
        binding.quantityEt.setText("");
        binding.priceEt.setText("");
        binding.discountedPriceEt.setText("");
        binding.discountedNotePriceEt.setText("");
        binding.productIconIv.setImageResource(R.drawable.ic_shopping_cart_24);
        image_uri = null;
    }

    private void categoryDialog() {
        //Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product Category")
                .setItems(Constants.PRODUCT_CATEGORY, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Pick Category
                        String category = Constants.PRODUCT_CATEGORY[i];

                        //Sets picked category
                        binding.categoryTv.setText(category);
                    }
                }).show();
    }

    private void showImagePickDialog() {
        //options to display
        String options[] = {"Camera","Gallery"};
        //Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Handles item clicks
                        if (i == 0){
                            //Camera clicked
                            if (checkCameraPermission()){
                                //Permission granted
                                pickFromCamera();
                            }else{
                                //Permission not granted
                                requestCameraPermission();
                            }
                        }else{
                            //Gallery clicked
                            if (checkStoragePermission()){
                                //Permission granted
                                pickFromGallery();
                            }else{
                                //Permission not granted
                                requestStoragePermission();
                            }
                        }
                    }
                }).create().show();
    }

    private void pickFromGallery() {
        //Intent to pick image from gallery
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //Intent to pick image from camera
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Image_Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image_Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }
    //Handle Permission Results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length >0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //Both permission granted
                        pickFromCamera();
                    }else{
                        //Both or one of permissions denied
                        Toast.makeText(this,"Camera & Storage permissions are required", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        //Permission granted
                        pickFromGallery();
                    }else{
                        //Permission Denied
                        Toast.makeText(this,"Storage permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Handle image pick results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image picked from gallery

                //save picked image uri
                image_uri = data.getData();

                //set image
                binding.productIconIv.setImageURI(image_uri);
            }else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //image picked from camera
                binding.productIconIv.setImageURI(image_uri);

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}