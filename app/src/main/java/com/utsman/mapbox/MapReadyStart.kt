package com.utsman.mapbox

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
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

class MapReadyStart(private val context: Context) : OnMapReadyCallback {

    private var animator: ValueAnimator? = null
    private var currentLatLng = LatLng(-6.21462, 106.84513)

    private val latLngEvaluator = object : TypeEvaluator<LatLng> {
        val latLng = LatLng()

        override fun evaluate(f: Float, startLatLng: LatLng, endLatlng: LatLng): LatLng {
            latLng.latitude = startLatLng.latitude + (endLatlng.latitude - startLatLng.latitude) * f
            latLng.longitude = startLatLng.longitude + (endLatlng.longitude - startLatLng.longitude) * f
            return latLng
        }
    }

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

            val markerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_default)
            style.addImage("marker", markerBitmap)

            style.addSource(jsonSource)

            val symbolLayer = SymbolLayer("layer-1", "jkt").apply {
                withProperties(
                    PropertyFactory.iconImage("marker"),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAllowOverlap(true))
            }

            style.addLayer(symbolLayer)

            mapboxMap.addOnMapClickListener {  latLng ->
                setOnClickMap(latLng, jsonSource)
            }
        }
    }

    private fun setOnClickMap(latLng: LatLng, jsonSource: GeoJsonSource): Boolean {
        if (animator != null && animator!!.isStarted) {
            currentLatLng = animator!!.animatedValue as LatLng
            animator!!.cancel()
        }

        animator = ObjectAnimator.ofObject(latLngEvaluator, currentLatLng, latLng).apply {
            duration = 2000
            addUpdateListener(animatorUpdateListener(jsonSource))
        }

        animator?.start()

        currentLatLng = latLng
        return true
    }
}