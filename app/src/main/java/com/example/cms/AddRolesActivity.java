package com.example.cms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddRolesActivity extends AppCompatActivity {

    private static final String TAG = "AddRoles";

    // UI Components
    private TextView institutionNameText;
    private TextView currentRolesText;
    private EditText newRolesEditText;
    private Button addRolesButton;
    private Button cancelButton;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private String institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_roles);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get institution ID from intent
        institutionId = getIntent().getStringExtra("institutionId");

        if (institutionId == null) {
            Toast.makeText(this, "Error: Institution ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Load institution data
        loadInstitutionData();

        // Set up listeners
        setupListeners();
    }

    private void initializeViews() {
        institutionNameText = findViewById(R.id.institutionNameText);
        currentRolesText = findViewById(R.id.currentRolesText);
        newRolesEditText = findViewById(R.id.newRolesEditText);
        addRolesButton = findViewById(R.id.addRolesButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadInstitutionData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        String institutionName = documentSnapshot.getString("institutionName");
                        List<String> roles = (List<String>) documentSnapshot.get("roles");

                        // Set institution name
                        if (institutionName != null) {
                            institutionNameText.setText(institutionName);
                        }

                        // Set current roles
                        if (roles != null && !roles.isEmpty()) {
                            String rolesText = String.join(", ", roles);
                            currentRolesText.setText(rolesText);
                        } else {
                            currentRolesText.setText("No roles defined yet.");
                        }

                        Log.d(TAG, "Institution data loaded");
                    } else {
                        Toast.makeText(this, "Institution not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading institution data", e);
                    Toast.makeText(this, "Error loading institution data", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        // Add Roles button click
        addRolesButton.setOnClickListener(v -> {
            if (validateInputs()) {
                addNewRoles();
            }
        });

        // Cancel button click
        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private boolean validateInputs() {
        String newRolesInput = newRolesEditText.getText().toString().trim();

        if (newRolesInput.isEmpty()) {
            Toast.makeText(this, "Please enter at least one role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void addNewRoles() {
        String newRolesInput = newRolesEditText.getText().toString().trim();

        // Split by comma and trim each role
        String[] rolesArray = newRolesInput.split(",");
        List<String> newRoles = new ArrayList<>();

        for (String role : rolesArray) {
            String trimmedRole = role.trim();
            if (!trimmedRole.isEmpty()) {
                newRoles.add(trimmedRole);
            }
        }

        if (newRoles.isEmpty()) {
            Toast.makeText(this, "Please enter valid roles", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // First, get current roles to check for duplicates
        db.collection("institutions").document(institutionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> currentRoles = (List<String>) documentSnapshot.get("roles");
                        
                        if (currentRoles == null) {
                            currentRoles = new ArrayList<>();
                        }

                        // Filter out duplicates
                        List<String> rolesToAdd = new ArrayList<>();
                        List<String> duplicates = new ArrayList<>();

                        for (String role : newRoles) {
                            boolean isDuplicate = false;
                            for (String existingRole : currentRoles) {
                                if (existingRole.equalsIgnoreCase(role)) {
                                    isDuplicate = true;
                                    duplicates.add(role);
                                    break;
                                }
                            }
                            if (!isDuplicate) {
                                rolesToAdd.add(role);
                            }
                        }

                        if (rolesToAdd.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "All roles already exist", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Add new roles to Firestore
                        db.collection("institutions").document(institutionId)
                                .update("roles", FieldValue.arrayUnion(rolesToAdd.toArray()))
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    
                                    String message = rolesToAdd.size() + " role(s) added successfully";
                                    if (!duplicates.isEmpty()) {
                                        message += " (Duplicates skipped: " + String.join(", ", duplicates) + ")";
                                    }
                                    
                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                    Log.d(TAG, "Roles added successfully: " + rolesToAdd);
                                    
                                    // Go back to previous screen
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "Error adding roles", e);
                                    Toast.makeText(this, "Error adding roles: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Institution not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error checking existing roles", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
