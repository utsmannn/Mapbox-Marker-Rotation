package com.utsman.mapbox

import android.Manifest
import android.animation.TypeEvaluator
import android.content.Context
import android.location.Location
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationRequest
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val token = "pk.eyJ1Ijoia3VjaW5nYXBlcyIsImEiOiJjazFjZXB4aDIyb3gwM2Nxajlza2c2aG8zIn0.htmYJKp9aaJnh-JhWZA85Q"

fun logi(msg: String?) = Log.i("anjay", msg)
fun loge(msg: String?) = Log.e("anjay", msg)

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Location.toLatlng() = LatLng(latitude, longitude)

val latLngEvaluator = object : TypeEvaluator<LatLng> {
    val latLng = LatLng()

    override fun evaluate(f: Float, startLatLng: LatLng, endLatlng: LatLng): LatLng {
        latLng.latitude = startLatLng.latitude + (endLatlng.latitude - startLatLng.latitude) * f
        latLng.longitude = startLatLng.longitude + (endLatlng.longitude - startLatLng.longitude) * f
        return latLng
    }
}

fun withPermission(activity: AppCompatActivity, listener: Context.() -> Unit) {
    Dexter.withActivity(activity)
        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        .withListener(object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                // setup your listener
                listener(activity)
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {

                // if permission denied, application will be close on 3 detik
                Handler().postDelayed({
                    activity.finish()
                }, 3000)
            }

        })
        .check()
}

fun Context.getLocationDebounce(disposable: CompositeDisposable, oldLocation: (Location) -> Unit, newLocation: (Location) -> Unit) {
    val request = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(100)

    val provider = ReactiveLocationProvider(this)
    val subscription = provider.getUpdatedLocation(request)
        .subscribeOn(Schedulers.io())
        .doOnNext {  oldLoc ->
            logi("old loc is -> ${oldLoc.toLatlng()}")
            oldLocation.invoke(oldLoc)
        }
        .debounce(300, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ updateLocation ->
            logi("new loc is -> ${updateLocation.toLatlng()}")
            newLocation.invoke(updateLocation)

        }, {  thr ->
            loge(thr.localizedMessage)
        })

    disposable.add(subscription)
}

fun Context.getLocation(disposable: CompositeDisposable, update: Boolean, locationListener: Context.(loc: Location) -> Unit) {
    val request = if (!update) {
        LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setNumUpdates(1)
            .setInterval(100)
    } else {
        LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(100)
    }

    val provider = ReactiveLocationProvider(this)
    val subscription = provider.getUpdatedLocation(request)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ updateLocation ->
            locationListener(this, updateLocation)

        }, {  thr ->
            loge(thr.localizedMessage)
        })

    disposable.add(subscription)
}

fun computeHeading(from: LatLng, to: LatLng): Double {
    val fromLat = Math.toRadians(from.latitude)
    val fromLng = Math.toRadians(from.longitude)
    val toLat = Math.toRadians(to.latitude)
    val toLng = Math.toRadians(to.longitude)
    val dLng = toLng - fromLng
    val heading = atan2(
        sin(dLng) * cos(toLat),
        cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng)
    )
    return wrap(Math.toDegrees(heading), -180.0, 180.0)
}

fun wrap(n: Double, min: Double, max: Double): Double {
    return if (n >= min && n < max) n else mod(n - min, max - min) + min
}

private fun mod(x: Double, m: Double): Double {
    return (x % m + m) % m
}