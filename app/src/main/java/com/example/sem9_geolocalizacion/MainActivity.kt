package com.example.sem9_geolocalizacion

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.w3c.dom.Text
import java.util.Locale
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var botonObtenerCoordenadas: Button
    private lateinit var textoLatitud: TextView
    private lateinit var textoLongitud: TextView
    private lateinit var gestorUbicacion: LocationManager
    private val lanzadorSolicitudPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            obtenerUltimaUbicacion()
        } else {
            Toast.makeText(this, "Permiso de ubicacion denegado", Toast.LENGTH_LONG).show()
        }
    }
    private lateinit var localizacion: Localizacion
    private var rastreandoUbicacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        botonObtenerCoordenadas = findViewById(R.id.btnGetCoordinates)
        textoLatitud = findViewById(R.id.tvLatitude)
        textoLongitud = findViewById(R.id.tvLongitude)

        gestorUbicacion = getSystemService(LOCATION_SERVICE) as LocationManager
        localizacion = Localizacion(textoLatitud, textoLongitud, this)

        botonObtenerCoordenadas.setOnClickListener {
            if (!rastreandoUbicacion) {
                verificarPermisoUbicacion()
                botonObtenerCoordenadas.text = "Detener rastreo"
                rastreandoUbicacion = true
            } else {
                detenerRastreo()
                botonObtenerCoordenadas.text = "Obtener Coordenadas"
                rastreandoUbicacion = false
            }
        }

        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
    }

    private fun verificarPermisoUbicacion() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                obtenerUltimaUbicacion()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Se necesita permiso de ubicacion para continuar", Toast.LENGTH_LONG).show()
                lanzadorSolicitudPermiso.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                lanzadorSolicitudPermiso.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun obtenerUltimaUbicacion() {
        if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicacion no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val ultimaUbicacion = gestorUbicacion.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: gestorUbicacion.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (ultimaUbicacion != null) {
                textoLatitud.text = String.format("%.6f", ultimaUbicacion.latitude)
                textoLongitud.text = String.format("%.6f", ultimaUbicacion.longitude)
                localizacion.setLastLocation(ultimaUbicacion)
            } else {
                textoLatitud.text = "No disponible"
                textoLongitud.text = "No disponoble"
                Toast.makeText(this@MainActivity, "No se encontro ubicacion reciente", Toast.LENGTH_SHORT).show()
            }

            gestorUbicacion.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0.5f,
                localizacion
            )

            if (gestorUbicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                gestorUbicacion.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    0.5f,
                    localizacion
                )
            }

            Toast.makeText(this, "Rastreo de ubicacion iniciado", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            textoLatitud.text = "Error"
            textoLongitud.text = "Error"
            Toast.makeText(this@MainActivity, "Error de seguridad: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            textoLatitud.text = "Error"
            textoLongitud.text = "Error"
            Toast.makeText(this@MainActivity, "Error al obtener ubicacion: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detenerRastreo() {
        gestorUbicacion.removeUpdates(localizacion)
        Toast.makeText(this, "Rastreo de ubicacion detenido", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (rastreandoUbicacion) {
            detenerRastreo()
            botonObtenerCoordenadas.text = "Obtener Coordenadas"
            rastreandoUbicacion = false
        }
    }

    class Localizacion(
        private val textoLatitud: TextView,
        private val textoLongitud: TextView,
        private val mainActivity: MainActivity
    ) : LocationListener {

        private var lastLocation: Location? = null
        private val geocoder: Geocoder by lazy { Geocoder(mainActivity, Locale.getDefault()) }

        fun setLastLocation(location: Location) {
            lastLocation = location
            updateAddress(location)
        }

        override fun onLocationChanged(loc: Location) {
            textoLatitud.text = String.format("%.6f", loc.latitude)
            textoLongitud.text = String.format("%.6f", loc.longitude)
            updateAddress(loc)
            lastLocation = loc
        }

        private fun updateAddress(location: Location) {
            try {
                val addresses: List<Address> = geocoder.getFromLocation(location.latitude, location.longitude, 1) ?: emptyList()
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0) ?: "Sin direccion"
                    mainActivity.runOnUiThread {
                        mainActivity.findViewById<TextView>(R.id.tvAddress)?.text = addressText
                    }
                } else {
                    mainActivity.runOnUiThread {
                        mainActivity.findViewById<TextView>(R.id.tvAddress)?.text = "Sin direccion"
                    }
                }
            } catch (e: Exception) {
                mainActivity.runOnUiThread {
                    mainActivity.findViewById<TextView>(R.id.tvAddress)?.text = "Error al obtener direccion"
                }
            }
        }

        override fun onProviderDisabled(provider: String) {
            Toast.makeText(mainActivity, "GPS Desactivado", Toast.LENGTH_SHORT).show()
        }

        override fun onProviderEnabled(provider: String) {
            Toast.makeText(mainActivity, "GPS Activado", Toast.LENGTH_SHORT).show()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            when (status) {
                LocationProvider.AVAILABLE -> {}
                LocationProvider.OUT_OF_SERVICE -> {}
                LocationProvider.TEMPORARILY_UNAVAILABLE -> {}
            }
        }
    }
}