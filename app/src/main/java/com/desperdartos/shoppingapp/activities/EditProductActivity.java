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
import com.desperdartos.shoppingapp.commands.EditProductActivityClicks;
import com.desperdartos.shoppingapp.databinding.ActivityEditProductBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class EditProductActivity extends AppCompatActivity {
    ActivityEditProductBinding binding;
    private String productId;

    /*UI views
    private ImageButton backBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedNotePriceEt, discountedPriceEt;
    private TextView categoryTv;
    private SwitchCompat discountSwitch;
    private Button updateProductBtn;*/

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
        //setContentView(R.layout.activity_edit_product);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_product);

        //get id of product from intent
        productId = getIntent().getStringExtra("productId");


        firebaseAuth = FirebaseAuth.getInstance();
        loadProductDetails();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        binding.discountSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //checked,,show discount price Et discount note Et....
                    binding.discountedPriceEt.setVisibility(View.VISIBLE);
                    binding.discountedNotePriceEt.setVisibility(View.VISIBLE);

                } else {
                    //unchecked   show hide discount price Et discount note Et.....
                    binding.discountedPriceEt.setVisibility(View.GONE);
                    binding.discountedNotePriceEt.setVisibility(View.GONE);
                }
            }
        });
        binding.setClickHandle(new EditProductActivityClicks() {
            @Override
            public void productIconClick() {
                showImagePickDialog();
            }

            @Override
            public void categoryTvClick() {
                categoryDialog();
            }

            @Override
            public void editProductBtn() {
                //1)input data
                //2)validate data
                //3)update data to database
                inputData();
            }

            @Override
            public void backBtnClick() {
                onBackPressed();
            }
        });
    }

    private void loadProductDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Products").child(productId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Get data from db
                        String productId = ""+snapshot.child("productId").getValue();
                        String productTitle = ""+snapshot.child("productTitle").getValue();
                        String productDescription = ""+snapshot.child("productDescription").getValue();
                        String productCategory = ""+snapshot.child("productCategory").getValue();
                        String productIcon = ""+snapshot.child("productIcon").getValue();

                        String productQuantity = ""+snapshot.child("productQuantity").getValue();
                        String originalPrice = ""+snapshot.child("originalPrice").getValue();
                        String discountPrice = ""+snapshot.child("discountPrice").getValue();
                        String discountNote = ""+snapshot.child("discountNote").getValue();
                        String discountAvailable = ""+snapshot.child("discountAvailable").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String uid = ""+snapshot.child("uid").getValue();

                        //Set data to views
                        if (discountAvailable.equals("true")){
                            binding.discountSwitch.setChecked(true);
                            binding.discountedPriceEt.setVisibility(View.VISIBLE);
                            binding.discountedNotePriceEt.setVisibility(View.VISIBLE);
                        }else{
                            binding.discountSwitch.setChecked(false);
                            binding.discountedPriceEt.setVisibility(View.GONE);
                            binding.discountedNotePriceEt.setVisibility(View.GONE);
                        }
                        binding.titleEt.setText(productTitle);
                        binding.descriptionEt.setText(productDescription);
                        binding.categoryTv.setText(productCategory);
                        binding.discountedNotePriceEt.setText(discountNote);
                        binding.quantityEt.setText(productQuantity);
                        binding.priceEt.setText(originalPrice);
                        binding.discountedPriceEt.setText(discountPrice);

                        try{
                            Picasso.get().load(productIcon).placeholder(R.drawable.ic_shopping_cart_white).into(binding.productIconIv);
                        }catch (Exception e){
                            binding.productIconIv.setImageResource(R.drawable.ic_shopping_cart_white);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

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
            return;  //Don't process further
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
        //3. update data to db
        updateProduct();
    }

    private void updateProduct() {
        progressDialog.show();
        progressDialog.setMessage("Updating Product");
        final String timestamp = "" + System.currentTimeMillis();

        if (image_uri == null){
            //Update without image
            //Use hashMap to update
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productTitle",""+productTitle);
            hashMap.put("productDescription",""+productDescription);
            hashMap.put("productCategory",""+productCategory);
            hashMap.put("productQuantity",""+productQuantity);
            hashMap.put("originalPrice",""+originalPrice);
            hashMap.put("discountPrice",""+discountPrice);
            hashMap.put("discountNote",""+discountNote);
            hashMap.put("discountAvailable",""+discountAvailable);
            hashMap.put("timestamp", "" + timestamp);

            //update db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Products").child(productId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //update success
                            progressDialog.dismiss();
                            Toast.makeText(EditProductActivity.this,"Product Updated!", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //update failed
                    progressDialog.dismiss();
                    Toast.makeText(EditProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        }else{//Update with image
            String filePathAndName = "product_images/" + "" + productId;
            //Uploads image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Image uploaded
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while(!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();
                            if (uriTask.isSuccessful()){
                                //Use hashMap to update
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("productTitle",""+productTitle);
                                hashMap.put("productDescription",""+productDescription);
                                hashMap.put("productCategory",""+productCategory);
                                hashMap.put("productIcon",""+downloadImageUri);
                                hashMap.put("productQuantity",""+productQuantity);
                                hashMap.put("originalPrice",""+originalPrice);
                                hashMap.put("discountPrice",""+discountPrice);
                                hashMap.put("discountNote",""+discountNote);
                                hashMap.put("discountAvailable",""+discountAvailable);

                                //update db
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).child("Products").child(productId)
                                        .updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //update success
                                                progressDialog.dismiss();
                                                Toast.makeText(EditProductActivity.this,"Product Updated!", Toast.LENGTH_SHORT).show();
                                                clearData();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //update failed
                                        progressDialog.dismiss();
                                        Toast.makeText(EditProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Upload failed
                    progressDialog.dismiss();
                    Toast.makeText(EditProductActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
    private void clearData() {
        //clear data after uploading product
        binding.titleEt.setText("");
        binding.descriptionEt.setText("");
        binding.categoryTv.setText("");
        binding.quantityEt.setText("");
        binding.priceEt.setText("");
        binding.discountedPriceEt.setText("");
        binding.discountedNotePriceEt.setText("");
        binding.productIconIv.setImageResource(R.drawable.ic_baseline_add_shopping_cart_24);
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
                }).create().show();
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