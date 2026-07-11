package com.harvesthub.mobile.ui.farmer;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import com.harvesthub.mobile.model.FarmLocation;

public class RegisterRequest implements Parcelable {
    @SerializedName("fullName")
    private String fullName;
    
    @SerializedName("mobileNumber")
    private String mobileNumber;
    
    @SerializedName("nic")
    private String nic;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    @SerializedName("farmLocation")
    private FarmLocation farmLocation;

    public RegisterRequest(String fullName, String mobileNumber, String nic, String email, String password, String role) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.nic = nic;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    protected RegisterRequest(Parcel in) {
        fullName = in.readString();
        mobileNumber = in.readString();
        nic = in.readString();
        email = in.readString();
        password = in.readString();
        role = in.readString();
        farmLocation = in.readParcelable(FarmLocation.class.getClassLoader());
    }

    public static final Creator<RegisterRequest> CREATOR = new Creator<RegisterRequest>() {
        @Override
        public RegisterRequest createFromParcel(Parcel in) {
            return new RegisterRequest(in);
        }

        @Override
        public RegisterRequest[] newArray(int size) {
            return new RegisterRequest[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fullName);
        dest.writeString(mobileNumber);
        dest.writeString(nic);
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(role);
        dest.writeParcelable(farmLocation, flags);
    }

    public String getFullName() { return fullName; }
    public String getMobileNumber() { return mobileNumber; }
    public String getNic() { return nic; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public FarmLocation getFarmLocation() { return farmLocation; }
    public void setFarmLocation(FarmLocation farmLocation) { this.farmLocation = farmLocation; }
}
