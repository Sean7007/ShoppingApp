package com.desperdartos.shoppingapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
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
import android.util.Patterns;
import android.widget.Toast;

import com.desperdartos.shoppingapp.R;
import com.desperdartos.shoppingapp.databinding.ActivityRegisterSellerBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RegisterSellerActivity extends AppCompatActivity implements LocationListener {
    ActivityRegisterSellerBinding binding;
    //Permission Constraints
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;

    //Permission Arrays
    private String locationPermissions[];
    private String cameraPermissions[];
    private String storagePermissions[];

    private LocationManager locationManager;
    private double latitude, longitude;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    //image picked uri
    private Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_register_seller);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register_seller);

        //init permissions array
        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.setClickHandle(new com.desperdartos.shoppingapp.commands.RegisterSellerActivity() {
            @Override
            public void sellerBackBtnClick() {
                onBackPressed();
            }

            @Override
            public void sellerGpsBtnClick() {
                //Detect current location
                if(checkLocationPermission()){
                    //Already allowed so
                    detectLocation();
                }else{
                    //Not allowed request for location permssion
                    requestLocationPermission();
                }
            }

            @Override
            public void sellerProfileIvClick() {
                //Pick image
                showImagePickDialog();
            }

            @Override
            public void sellerRegisterBtnClick() {
                //register seller
                inputData();
            }
        });
    }

    //Var declaration
    private String fullName, shopName, phoneNumber, deliveryFee, country, zone, city, address, email, password, confirmPassword;
    private void inputData() {
        fullName = binding.nameEt.getText().toString().trim();
        shopName = binding.shopNameEt.getText().toString().trim();
        phoneNumber = binding.phoneEt.getText().toString().trim();
        deliveryFee = binding.deliveryEt.getText().toString().trim();
        country = binding.countryEt.getText().toString().trim();
        zone = binding.zoneEt.getText().toString().trim();
        city = binding.cityEt.getText().toString().trim();
        address = binding.addressEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        confirmPassword = binding.cPasswordEt.getText().toString().trim();

        //Validate Data
        if (TextUtils.isEmpty(fullName)){
            Toast.makeText(this, "Enter Name!",Toast.LENGTH_SHORT).show();
            binding.nameEt.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(shopName)){
            Toast.makeText(this, "Enter Shop Name!",Toast.LENGTH_SHORT).show();
            binding.shopNameEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(phoneNumber)){
            Toast.makeText(this, "Enter Phone #",Toast.LENGTH_SHORT).show();
            binding.phoneEt.requestFocus();
            return;
        }if (phoneNumber.length() < 8) {
            binding.phoneEt.setError("phone number should be 8 digits");
        }if (TextUtils.isEmpty(deliveryFee)){
            Toast.makeText(this, "Enter Delivery Fee!",Toast.LENGTH_SHORT).show();
            binding.deliveryEt.requestFocus();
            return;
        }if (latitude==0.0 || longitude==0.0){
            Toast.makeText(this, "Please Click GPS button for location detection!",Toast.LENGTH_SHORT).show();
            return;
        } if (TextUtils.isEmpty(country)) {
            Toast.makeText(this, "Enter Country Name", Toast.LENGTH_SHORT).show();
            binding.countryEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(zone)){
            Toast.makeText(this, "Enter Zone!",Toast.LENGTH_SHORT).show();
            binding.zoneEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Enter city!", Toast.LENGTH_SHORT).show();
            binding.cityEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(address)){
            Toast.makeText(this, "Enter Address!",Toast.LENGTH_SHORT).show();
            binding.addressEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show();
            binding.emailEt.requestFocus();
            return;
        }if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.emailEt.setError("Invalid Email Address");
            binding.emailEt.requestFocus();
            return;
        }if (password.length()<8){
            binding.passwordEt.setError("Password length should be 8 or above");
            binding.passwordEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
            binding.passwordEt.requestFocus();
            return;
        }if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Enter confirm password", Toast.LENGTH_SHORT).show();
            binding.cPasswordEt.requestFocus();
            return;
        }if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "don't match password", Toast.LENGTH_SHORT).show();
            binding.passwordEt.setError("Password & confirm password should be same");
            binding.cPasswordEt.setError("Password & confirm password should be same");
            binding.passwordEt.requestFocus();
            binding.cPasswordEt.requestFocus();
        }
        //When all validations are met, CreateAccount
        createAccount();
    }

    private void createAccount() {
        progressDialog.setMessage("Account is in creation");
        progressDialog.show();

        //Create account
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                progressDialog.dismiss();
                //Account created
                saveFirebaseData();
            }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Account failed
                        progressDialog.dismiss();
                        Toast.makeText(RegisterSellerActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveFirebaseData() {
        progressDialog.setMessage("Saving Account Info!");
        final String timeStamp = "" + System.currentTimeMillis();

        if (image_uri == null){
            //Save info without image

            //Setup data to save
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid",""+firebaseAuth.getUid());
            hashMap.put("email",""+email);
            hashMap.put("name",""+fullName);
            hashMap.put("shopName",""+shopName);
            hashMap.put("phone",""+phoneNumber);
            hashMap.put("deliveryFee",""+deliveryFee);
            hashMap.put("country",""+country);
            hashMap.put("zone",""+zone);
            hashMap.put("city",""+city);
            hashMap.put("address",""+address);
            hashMap.put("latitude",""+latitude);
            hashMap.put("longitude",""+longitude);
            hashMap.put("timeStamp",""+timeStamp);
            hashMap.put("accountType","" +"Seller");
            hashMap.put("online","true");
            hashMap.put("shopOpen","true");
            hashMap.put("profileImage","");

            //Save to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //DB updated
                            progressDialog.dismiss();
                            startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failure to update DB
                    progressDialog.dismiss();
                    startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                    finish();
                }
            });
        }else{
            //Save info without image

            //Name and path of Image
            String filePathAndName="profile_images/" + ""+firebaseAuth.getUid();
            //upload image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Get url for uploading image
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();
                            if (uriTask.isSuccessful()){
                                //Setup data to save
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("uid",""+firebaseAuth.getUid());
                                hashMap.put("email",""+email);
                                hashMap.put("name",""+fullName);
                                hashMap.put("shopName",""+shopName);
                                hashMap.put("phone",""+phoneNumber);
                                hashMap.put("deliveryFee",""+deliveryFee);

                                hashMap.put("country",""+country);
                                hashMap.put("zone",""+zone);
                                hashMap.put("city",""+city);
                                hashMap.put("address",""+address);
                                hashMap.put("latitude",""+latitude);
                                hashMap.put("longitude",""+longitude);
                                hashMap.put("timeStamp",""+timeStamp);
                                hashMap.put("accountType",""+"Seller");
                                hashMap.put("online",""+"true");
                                hashMap.put("shopOpen",""+"true");
                                hashMap.put("profileImage","" + downloadImageUri); //Url of uploaded image

                                //Save to db
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //DB updated
                                                progressDialog.dismiss();
                                                startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                                                finish();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failure to update DB
                                        progressDialog.dismiss();
                                        startActivity(new Intent(RegisterSellerActivity.this, MainSellerActivity.class));
                                        finish();
                                    }
                                });
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterSellerActivity.this,""+ e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showImagePickDialog() {
        //Options to Display in dialog
        String options[] = {"Camera","Gallery"};
        //Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Handle clicks
                        if (i == 0){
                            //Camera clicked
                            if (checkCameraPermission()){
                                //Camera Permissions allowed
                                pickFromCamera();
                            }else{
                                //Not allowed Request
                                requestCameraPermission();
                            }
                        }else{
                            //Gallery Clicked
                            if (checkStoragePermission()){
                                //Storage Permissions allowed
                                pickFromGallery();
                            }else{
                                //Not allowed Request
                                requestStoragePermission();
                            }
                        }
                    }
                }).show();
    }

    private void pickFromGallery(){
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    //Checks for Location Permission
    private boolean checkLocationPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    //Request Location Permission
    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
    }

    //
    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    //
    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted){
                        //Permission Allowed
                        detectLocation();
                    }else{
                        //Permission Denied
                        Toast.makeText(this,"Location permission is necessary...",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){
                        //Permission Allowed
                        pickFromCamera();
                    }else{
                        //Permission Denied
                        Toast.makeText(this,"Camera permission are necessary...",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){
                        //Permission Allowed
                        pickFromGallery();
                    }else{
                        //Permission Denied
                        Toast.makeText(this,"Storage permission is necessary...",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
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
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //picked from camera
                binding.profileIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressWarnings("MissingPermission")
    private void detectLocation() {

        Toast.makeText(this,"Please wait",Toast.LENGTH_LONG).show();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        //Location detect
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
            String address = addresses.get(0).getAddressLine(0); //
            String city = addresses.get(0).getLocality();
            String zone = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryCode();

            //Set address
            binding.countryEt.setText(country);
            binding.zoneEt.setText(zone);
            binding.cityEt.setText(city);
            binding.countryEt.setText(country);
            binding.addressEt.setText(address);
        }catch(Exception e){
            Toast.makeText(this,""+ e.getMessage(),Toast.LENGTH_SHORT).show();
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
        //GPS Location disabled
        Toast.makeText(this,"In your Phone: please turn on Location!",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}