package com.desperdartos.shoppingapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.commands.EditUserProfileClicks;
import com.desperdartos.shoppingapp.databinding.ActivityProfileEditUserBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.List;
import java.util.Locale;

public class ProfileEditUserActivity extends AppCompatActivity implements LocationListener {
    ActivityProfileEditUserBinding binding;
    //permission constants
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    //image,location pick constraints
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;

    //Permission arrays
    private String locationPermissions[];
    private String cameraPermissions[];
    private String storagePermissions[];

    private ProgressDialog progressDialog; //progress Dialog
    private FirebaseAuth firebaseAuth;

    private LocationManager locationManager;
    private Uri image_uri; //image uri

    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_profile_edit_user);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_edit_user);
        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        binding.setClickHandle(new EditUserProfileClicks() {
            @Override
            public void editUserProfileBackbtn() {
                onBackPressed();
            }

            @Override
            public void editUserProfileGpsBtnClick() {
                //Detect location
                if (checkLocationPermission()){
                    //Already allowed
                    detectLocation();
                }else{
                    //Not allowed, request for location permission
                    requestLocationPermission();
                }
            }

            @Override
            public void editUserProfileImageBtnClick() {
                //Show image pick dilaog
                showImagePickDialog();
            }

            @Override
            public void editUserProfileUpdateBtnClick() {
                inputData();
            }
        });

        //init permission arrays
        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    //Takes data from phone into db
    private String name, phone,country, zone, city, address;
    private boolean shopOpen;

    private void inputData() {
        name = binding.nameEt.getText().toString().trim();
        phone = binding.phoneEt.getText().toString().trim();
        country = binding.countryEt.getText().toString().trim();
        zone = binding.zoneEt.getText().toString().trim();
        city = binding.cityEt.getText().toString().trim();
        address = binding.addressEt.getText().toString().trim();

        //validate
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show();
            binding.nameEt.requestFocus();
            return;
        }if (phone.length() < 8) {
            binding.phoneEt.setError("Phone # should be 8 digits");
        }if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Enter phone Number", Toast.LENGTH_SHORT).show();
            binding.phoneEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(country)) {
            Toast.makeText(this, "Enter Country", Toast.LENGTH_SHORT).show();
            binding.countryEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(zone)) {
            Toast.makeText(this, "Enter Zone", Toast.LENGTH_SHORT).show();
            binding.zoneEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Enter City", Toast.LENGTH_SHORT).show();
            binding.cityEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Enter Address", Toast.LENGTH_SHORT).show();
            binding.addressEt.requestFocus();
            return;
        }else {
            updateProfile();
        }
    }

    private void updateProfile() {
        progressDialog.setMessage("Updating Profile...");
        progressDialog.show();

        if (image_uri == null){
            //update without image

            //setup data to update
            HashMap<String,Object> hashMap = new HashMap<>();
            hashMap.put("name",""+name);
            hashMap.put("phone",""+phone);
            hashMap.put("country",""+country);
            hashMap.put("zone",""+zone);
            hashMap.put("city",""+city);
            hashMap.put("address",""+address);
            hashMap.put("latitude",""+latitude);
            hashMap.put("longitude",""+longitude);

            //update to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //updated successfully
                            progressDialog.dismiss();
                            Toast.makeText(ProfileEditUserActivity.this,"Profile Updated",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed to update
                    progressDialog.dismiss();
                    Toast.makeText(ProfileEditUserActivity.this,""+ e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            //update with image
            /*---------------------------------Upload Image First------------------------*/
            String filePathAndName = "profile_images/" + ""+ firebaseAuth.getUid();
            //Get storage Reference
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image uploaded, get url of uploaded image
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()){
                                //image uri received
                                //setup data to update
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("name",""+name);
                                hashMap.put("phone",""+phone);
                                hashMap.put("country",""+country);
                                hashMap.put("zone",""+zone);
                                hashMap.put("city",""+city);
                                hashMap.put("address",""+address);
                                hashMap.put("latitude",""+latitude);
                                hashMap.put("longitude",""+longitude);
                                hashMap.put("profileImage",""+downloadImageUri);

                                //update to db
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //updated successfully
                                                progressDialog.dismiss();
                                                Toast.makeText(ProfileEditUserActivity.this,"Profile Updated",Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed to update
                                        progressDialog.dismiss();
                                        Toast.makeText(ProfileEditUserActivity.this,""+ e.getMessage(),Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileEditUserActivity.this,""+ e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    //Method checks
    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null){
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }else{
            loadMyInfo();
        }
    }

    private void loadMyInfo() {
        //Load user info, and set to views
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String accountType = "" +ds.child("accountType").getValue();
                            String address = "" +ds.child("address").getValue();
                            String city = "" +ds.child("city").getValue();
                            String zone = "" +ds.child("zone").getValue();
                            String country = "" +ds.child("country").getValue();
                            String email = "" +ds.child("email").getValue();
                            latitude = Double.parseDouble( "" +ds.child("latitude").getValue());
                            longitude = Double.parseDouble( "" +ds.child("longitude").getValue());
                            String name = "" +ds.child("name").getValue();
                            String online = "" +ds.child("online").getValue();
                            String phone = "" +ds.child("phone").getValue();
                            String profileImage = "" +ds.child("profileImage").getValue();
                            String timeStamp = "" +ds.child("timeStamp").getValue();
                            String uid = "" +ds.child("uid").getValue();

                            //
                            binding.nameEt.setText(name);
                            binding.countryEt.setText(country);
                            binding.nameEt.setText(name);
                            binding.zoneEt.setText(zone);
                            binding.cityEt.setText(city);
                            binding.addressEt.setText(address);

                            try{
                                Picasso.get().load(profileImage).placeholder(R.drawable.ic_store_24).into(binding.profileIv);
                            }catch(Exception e){
                                binding.profileIv.setImageResource(R.drawable.ic_person_24);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showImagePickDialog() {
        //Options to display in dialog
        String options[] = {"Camera","Gallery"};
        //Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Handle item clicks
                        if (i==0){
                            //Camera clicked
                            if (checkCameraPermission()){
                                //Allowed, to open camera
                                pickFromCamera();
                            }else{
                                //Not allowed to open camera
                                requestCameraPermission();
                            }
                        }else{
                            //Gallery clicked
                            if (checkStoragePermission()){
                                //Allowed, open gallery
                                pickFromGallery();
                            }else{
                                //Not allowed, request
                                requestStoragePermission();
                            }
                        }
                    }
                }).create().show();
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermissions, STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
    }

    private void pickFromGallery() {
        //Intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //Intent to pick image from Camera
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);

    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);
        return  result && result1;
    }

    private boolean checkLocationPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @SuppressLint("MissingPermission")
    private void detectLocation() {
        Toast.makeText(this,"Please Wait",Toast.LENGTH_SHORT).show();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        findAddress();
    }

    private void findAddress() {
        //Find address, country, zone, city
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try{
            addresses = geocoder.getFromLocation(latitude,longitude,1);
            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String zone = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            //set addresses
            binding.countryEt.setText(country);
            binding.zoneEt.setText(zone);
            binding.cityEt.setText(city);
            binding.addressEt.setText(address);
        }catch (Exception e){
            Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {

    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this,"Location is disabled!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted){
                        //permission allowed
                        detectLocation();
                    }else{
                        //permission denied
                        Toast.makeText(this,"Location permission is necessary...",Toast.LENGTH_SHORT).show();

                    }
                }
            }
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){
                        //permission allowed
                        pickFromCamera();
                    }else{
                        //permission denied
                        Toast.makeText(this, "Camera permissions are necessary!", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){
                        //permission allowed
                        pickFromGallery();
                    }else{
                        //permission denied
                        Toast.makeText(this, "Storage permission is necessary!", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //handle image pick result
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //picked from gallery
                image_uri = data.getData();
                //now set the image to imageView
                binding.profileIv.setImageURI(image_uri);
            }else if (requestCode==IMAGE_PICK_CAMERA_CODE){
                //picked from camera
                binding.profileIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}