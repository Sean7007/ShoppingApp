package com.desperdartos.shoppingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.desperdartos.shoppingapp.adapters.AdapterProductUser;
import com.desperdartos.shoppingapp.models.ModelProduct;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ShopDetailsActivity extends AppCompatActivity {

    private EditText searchProductEt;
    private TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv,addressTv, filteredProductsTv;
    private ImageView shopIv;
    private ImageButton callBtn, mapBtn, cartBtn, backBtn, filterProductBtn;
    private RecyclerView productsRv;

    private String shopUid;
    private String myLatitude, myLongitude;
    private String shopName, shopEmail, shopPhone, shopAddress, shopLatitude, shopLongitude;
    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelProduct> productsList;
    private AdapterProductUser adapterProductUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        //
        filteredProductsTv = findViewById(R.id.filteredProductsTv);
        shopIv = findViewById(R.id.shopIv);
        shopNameTv = findViewById(R.id.shopNameTv);
        phoneTv = findViewById(R.id.phoneTv);
        openCloseTv = findViewById(R.id.openCloseTv);
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv);
        emailTv = findViewById(R.id.emailTv);
        addressTv = findViewById(R.id.addressTv);
        callBtn = findViewById(R.id.callBtn);
        mapBtn = findViewById(R.id.mapBtn);
        cartBtn = findViewById(R.id.cartBtn);
        backBtn = findViewById(R.id.backBtn);
        filterProductBtn = findViewById(R.id.filterProductBtn);
        searchProductEt = findViewById(R.id.searchProductEt);
        productsRv = findViewById(R.id.productRv);

        shopUid = getIntent().getStringExtra("shopUid");
        firebaseAuth = FirebaseAuth.getInstance();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();

        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                 try{
                     adapterProductUser.getFilter().filter(charSequence);
                 }catch (Exception e){
                     e.printStackTrace();
                 }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Go back button
                onBackPressed();
            }
        });
        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // user

            }
        });
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                      dialPhone();
            }
        });

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                      openMap();
            }
        });
        filterProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShopDetailsActivity.this);
                builder.setTitle("Filter Products")
                        .setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //get selected item
                                String selected = Constants.productCategories1[i];
                                filteredProductsTv.setText(selected);
                                if (selected.equals("All")){
                                    //Load all
                                    loadShopProducts();
                                }else{
                                    //Load filtered
                                    adapterProductUser.getFilter().filter(selected);
                                }
                            }
                        }).show();
                          
            }
        });

    }

    private void openMap() {
       String address = "https://maps.google.com/maps?saddr=" + myLatitude + "," + myLongitude + "&daddr=" + shopLatitude + "," +shopLongitude;
       Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
       startActivity(intent);
    }

    private void dialPhone() {
         startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();
    }

    private void loadShopProducts() {
        //init list
        productsList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Clear list before adding item to db
                        productsList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }
                        //setup adapter
                        adapterProductUser = new AdapterProductUser(ShopDetailsActivity.this, productsList);
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadShopDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Get Shop data
                String name = ""+ snapshot.child("name").getValue();
                 shopName = ""+ snapshot.child("shopName").getValue();
                shopEmail = ""+ snapshot.child("email").getValue();
                shopPhone = ""+ snapshot.child("phone").getValue();
                 shopLatitude = ""+ snapshot.child("latitude").getValue();
                 shopLongitude = ""+ snapshot.child("longitude").getValue();
                shopAddress = ""+ snapshot.child("address").getValue();
                String deliveryFee = ""+snapshot.child("deliveryFee").getValue();
                 String profileImage = ""+snapshot.child("profileImage").getValue();
                String shopOpen = ""+snapshot.child("shopOpen").getValue();

                 //Set data
                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("DeliveryFee:"+deliveryFee);
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);
               if (shopOpen.equals("true")){
                    openCloseTv.setText("Open");
                }else{
                   openCloseTv.setText("Close");
                }try{
                    Picasso.get().load(profileImage).into(shopIv);
                }catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //Get user data
                            String name = "" +ds.child("name").getValue();
                            String email = "" +ds.child("email").getValue();
                            String phone = "" +ds.child("phone").getValue();
                            String profileImage = "" +ds.child("profileImage").getValue();
                            String accountType = "" +ds.child("accountType").getValue();
                            String city = "" +ds.child("city").getValue();
                            myLatitude = ""+ds.child("latitude").getValue();
                            myLongitude = ""+ds.child("longitude").getValue();


                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}