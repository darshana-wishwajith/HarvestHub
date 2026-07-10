package com.harvesthub.mobile.ui.farmer;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class RegisterRequest implements Parcelable {
    @SerializedName("fullName")
    private String fullName;
    
    @SerializedName("mobileNumber")
    private String mobileNumber;
    
    @SerializedName("nic")
    private String nic;
    
    @SerializedName("password")
    private String password;

    public RegisterRequest(String fullName, String mobileNumber, String nic, String password) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.nic = nic;
        this.password = password;
    }

    protected RegisterRequest(Parcel in) {
        fullName = in.readString();
        mobileNumber = in.readString();
        nic = in.readString();
        password = in.readString();
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
        dest.writeString(password);
    }

    public String getFullName() { return fullName; }
    public String getMobileNumber() { return mobileNumber; }
    public String getNic() { return nic; }
    public String getPassword() { return password; }
}
