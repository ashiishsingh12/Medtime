val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
if (currentUser != null) {
    // यूजर पहले से लॉग-इन है, सीधे मुख्य स्क्रीन दिखाएं
} else {
    // लॉगिन स्क्रीन दिखाएं
}
