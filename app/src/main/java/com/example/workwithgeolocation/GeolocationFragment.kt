package com.example.workwithgeolocation

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.workwithgeolocation.databinding.GeolocationFragmentBinding
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.location.*
import android.os.Handler
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException

class GeolocationFragment : Fragment() {

    private var _binding: GeolocationFragmentBinding? = null
    private val binding get() = _binding!!

    private val REFRESH_PERIOD = 60000L
    private val MINIMAL_DISTANCE = 100f

    private val permLocationReqLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocation()
        } else {
            context?.let { showDialog(it,
                getString(R.string.dialog_title_no_gps),
                getString(R.string.dialog_message_no_gps)) }
        }

    }

    private val onLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            context?.let {
                getAddressAsync(it, location)
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

    }

    companion object {
        fun newInstance() = GeolocationFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GeolocationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.showGeolocation.setOnClickListener { checkPermission() }
    }

    private fun checkPermission() {
        context?.let {
            when (PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    it, Manifest
                        .permission.ACCESS_FINE_LOCATION
                ) -> getLocation()
                else -> permLocationReqLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(getString(R.string.dialog_close)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun getLocation() {
        activity?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PERMISSION_GRANTED
            ) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
                    provider?.let {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            REFRESH_PERIOD,
                            MINIMAL_DISTANCE,
                            onLocationListener
                        )
                    }
                } else {
                    val location =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location == null) {
                        showDialog(context,
                            getString(R.string.dialog_title_gps_turned_off),
                            getString(R.string.dialog_message_last_location_unknown))
                    } else {
                        getAddressAsync(context, location)
                        showDialog(context,
                            getString(R.string.dialog_title_gps_turned_off),
                            getString(R.string.dialog_message_last_known_location))
                    }
                }
            } else {
                showDialog(context,
                    getString(R.string.dialog_title_no_gps),
                    getString(R.string.dialog_message_no_gps))
            }
        }
    }

    private fun getAddressAsync(context: Context, location: Location) {
        val geoCoder = Geocoder(context)
        val handler = Handler()
        Thread{
            try{
                val addresses = geoCoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                Log.d("test", (addresses[0].getAddressLine(0)))
               handler.post {showAddressDialog(addresses[0].getAddressLine(0))}
            } catch (e : IOException){
                e.printStackTrace()
            }
        }.start()
    }

    private fun showAddressDialog(addressLine: String) {
        activity?.let{
            AlertDialog.Builder(it)
                .setTitle(getString(R.string.dialog_address_title))
                .setMessage(addressLine)
                .setNegativeButton(getString(R.string.dialog_close)){dialog, _ -> dialog.dismiss()}
                .create()
                .show()
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }


}