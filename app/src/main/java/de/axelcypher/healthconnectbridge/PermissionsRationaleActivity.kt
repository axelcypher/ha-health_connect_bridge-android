package de.axelcypher.healthconnectbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Health Connect permission",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "HealthConnectBridge uses WRITE_WEIGHT only to transfer " +
                                "weight values received locally from Home Assistant into " +
                                "Health Connect. No health data is read, uploaded, or shared.",
                        )
                        Text(
                            text = "All status information stays on this device. You can revoke " +
                                "the permission at any time in Health Connect settings.",
                        )
                    }
                }
            }
        }
    }
}

