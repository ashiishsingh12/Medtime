package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.EmailAuthScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val currentUser = FirebaseAuth.getInstance().currentUser

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (currentUser != null) {
                    // अगर यूजर पहले से लॉग-इन है, तो आप यहाँ अपनी मुख्य होम स्क्रीन दिखा सकते हैं
                    // अभी के लिए हम ईमेल ऑथेंटिकेशन स्क्रीन दिखा रहे हैं या होम स्क्रीन पर भेज सकते हैं
                    EmailAuthScreen(
                        onAuthSuccess = {
                            // लॉगिन सफल होने पर आगे का काम
                        }
                    )
                } else {
                    // अगर यूजर लॉग-इन नहीं है, तो ईमेल-पासवर्ड लॉगिन स्क्रीन दिखाएं
                    EmailAuthScreen(
                        onAuthSuccess = {
                            // लॉगिन सफल होने पर रीकंपोजिशन या अगली स्क्रीन पर जाने का लॉजिक
                        }
                    )
                }
            }
        }
    }
}
