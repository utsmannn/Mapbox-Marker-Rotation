package com.utsman.mapbox

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import io.reactivex.disposables.CompositeDisposable

class MapMarkerMoveTracking(private val disposable: CompositeDisposable, private val context: Context, private var currentLatLng: LatLng) : OnMapReadyCallback {

    private var animator: ValueAnimator? = null
    private val liveRotate = MutableLiveData<Float>()

    private fun animatorUpdateListener(jsonSource: GeoJsonSource) : ValueAnimator.AnimatorUpdateListener {
        return ValueAnimator.AnimatorUpdateListener { value ->
            val animatedLatLng = value.animatedValue as LatLng
            jsonSource.setGeoJson(Point.fromLngLat(animatedLatLng.longitude, animatedLatLng.latitude))
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        val jsonSource = GeoJsonSource("jkt",
            Feature.fromGeometry(Point.fromLngLat(currentLatLng.longitude, currentLatLng.latitude)))

        mapboxMap.setStyle(Style.OUTDOORS) { style ->

            val position = CameraPosition.Builder()
                .target(currentLatLng)
                .zoom(17.0)
                .build()

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))

            style.addImage("marker", markerVector())
            style.addSource(jsonSource)

            val symbolLayer = SymbolLayer("layer-1", "jkt")

            liveRotate.postValue(0f)
            liveRotate.observe(context as LifecycleOwner, Observer {  rotation ->
               symbolLayer.withProperties(
                    PropertyFactory.iconImage("marker"),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconRotate(rotation),
                    PropertyFactory.iconAllowOverlap(true))
            })

            style.addLayer(symbolLayer)

            context.getLocationDebounce(disposable, {  oldLocation ->
                currentLatLng = oldLocation.toLatlng()
            }, { newLocation ->
                moveMarkerAnimation(newLocation.toLatlng(), jsonSource, symbolLayer)
            })
        }
    }

    private fun moveMarkerAnimation(newLatLng: LatLng, jsonSource: GeoJsonSource, symbolLayer: SymbolLayer): Boolean {
        if (animator != null && animator!!.isStarted) {
            currentLatLng = animator!!.animatedValue as LatLng
            animator!!.cancel()
        }

        animator = ObjectAnimator.ofObject(latLngEvaluator, currentLatLng, newLatLng).apply {
            duration = 2000
            addUpdateListener(animatorUpdateListener(jsonSource))
        }

        animator?.start()
        rotateMarker(symbolLayer, getAngle(currentLatLng, newLatLng).toFloat())
        return true
    }

    private fun markerVector(): Bitmap {
        val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_marker, null)
        return BitmapUtils.getBitmapFromDrawable(drawable) ?: BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_default)
    }

    private fun getAngle(fromLatLng: LatLng, toLatLng: LatLng) : Double {

        // default angle is 0.0
        var heading = 0.0

        // if marker different, update heading
        if (fromLatLng != toLatLng) {
            heading = computeHeading(fromLatLng, toLatLng)
        }

        return heading
    }

    private fun rotateMarker(symbolLayer: SymbolLayer, toRotation: Float) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startRotation = symbolLayer.iconRotate.value
        val duration: Long = 300

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = LinearInterpolator().getInterpolation(elapsed.toFloat() / duration)

                val rot = t * toRotation + (1 - t) * startRotation

                val rotation = if (-rot > 180) rot / 2 else rot
                liveRotate.postValue(rotation)
                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }
}