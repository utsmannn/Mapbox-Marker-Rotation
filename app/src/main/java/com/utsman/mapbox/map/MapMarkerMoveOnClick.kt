/*
 * Copyright 2019 Muhammad Utsman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.utsman.mapbox.map

import android.animation.ObjectAnimator
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
import com.utsman.mapbox.R
import com.utsman.mapbox.latLngEvaluator

class MapMarkerMoveOnClick(private val context: Context) : OnMapReadyCallback {

    private var animator: ValueAnimator? = null
    private var currentLatLng = LatLng(-6.21462, 106.84513)

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

            val markerBitmap = BitmapFactory.decodeResource(context.resources,
                R.drawable.mapbox_marker_icon_default
            )
            style.addImage("marker", markerBitmap)

            style.addSource(jsonSource)

            val symbolLayer = SymbolLayer("layer-1", "jkt").apply {
                withProperties(
                    PropertyFactory.iconImage("marker"),
                    PropertyFactory.iconRotate(90f),
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