package com.krzysztof.mocklocationapp

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krzysztof.mocklocationapp.ui.theme.MockLocationAppTheme

class MainActivity : ComponentActivity() {

    private val MOCK_PROVIDER = "mock_provider"

    // Nazwy + współrzędne
    private val testLocations = mapOf(
        "Warszawa – Pałac Kultury" to Pair(52.2319, 21.0067),
        "Kraków – Rynek Główny" to Pair(50.0614, 19.9366),
        "Berlin – Brama Brandenburska" to Pair(52.5163, 13.3777),
        "Paryż – Wieża Eiffla" to Pair(48.8584, 2.2945),
        "Londyn – Big Ben" to Pair(51.5007, -0.1246),
        "Nowy Jork – Times Square" to Pair(40.7580, -73.9855),
        "Rio – Chrystus Odkupiciel" to Pair(-22.9519, -43.2105),
        "Sydney – Opera House" to Pair(-33.8568, 151.2153),
        "Tokio – Shibuya Crossing" to Pair(35.6595, 139.7005),
        "Kair – Piramida Cheopsa" to Pair(29.9792, 31.1342)
    )

    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTestProvider()

        setContent {
            MockLocationAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen(
                        locations = testLocations.keys.toList(),
                        onSetMock = { name ->
                            testLocations[name]?.let { (lat, lon) ->
                                setMockLocation(lat, lon)
                            } ?: Log.e("MockLocationApp", "Brak współrzędnych dla: $name")
                        }
                    )
                }
            }
        }
    }

    private fun setupTestProvider() {
        try {
            if (locationManager.getProvider(MOCK_PROVIDER) == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager.addTestProvider(
                        MOCK_PROVIDER,
                        false, false, false, false,
                        true, true, true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        MOCK_PROVIDER,
                        false, false, false, false,
                        true, true, true,
                        1, 1
                    )
                }
            }
            locationManager.setTestProviderEnabled(MOCK_PROVIDER, true)
        } catch (e: Exception) {
            Log.e("MockLocationApp", "Błąd setupTestProvider", e)
        }
    }

    private fun setMockLocation(lat: Double, lon: Double) {
        val mockLocation = Location(MOCK_PROVIDER).apply {
            latitude = lat
            longitude = lon
            altitude = 0.0
            time = System.currentTimeMillis()
            accuracy = 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
        }
        try {
            locationManager.setTestProviderLocation(MOCK_PROVIDER, mockLocation)
            Log.d("MockLocationApp", "Ustawiono lokalizację: $lat, $lon")
        } catch (e: SecurityException) {
            Log.e("MockLocationApp", "Brak uprawnień do MOCK_LOCATION", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (locationManager.getProvider(MOCK_PROVIDER) != null) {
                locationManager.setTestProviderEnabled(MOCK_PROVIDER, false)
                locationManager.removeTestProvider(MOCK_PROVIDER)
            }
        } catch (e: Exception) {
            Log.e("MockLocationApp", "Błąd przy sprzątaniu providera", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    locations: List<String>,
    onSetMock: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf(locations.firstOrNull().orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedLocation,
                onValueChange = {},
                readOnly = true,
                label = { Text("Wybierz lokalizację") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                locations.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location) },
                        onClick = {
                            selectedLocation = location
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(
            onClick = { if (selectedLocation.isNotBlank()) onSetMock(selectedLocation) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Mock Location")
        }
    }
}