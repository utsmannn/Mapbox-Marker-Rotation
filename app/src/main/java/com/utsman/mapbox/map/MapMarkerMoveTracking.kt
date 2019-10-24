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

import android.content.Context
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
import com.utsman.mapbox.getLocationDebounce
import com.utsman.mapbox.toLatlng
import com.utsman.mapbox.util.MarkerUtil
import io.reactivex.disposables.CompositeDisposable

class MapMarkerMoveTracking(private val disposable: CompositeDisposable, private val context: Context, private var currentLatLng: LatLng) : OnMapReadyCallback {

    private val markerUtil = MarkerUtil(context, currentLatLng)
    private val liveRotate = MutableLiveData<Float>()

    override fun onMapReady(mapboxMap: MapboxMap) {
        val jsonSource = GeoJsonSource("jkt",
            Feature.fromGeometry(Point.fromLngLat(currentLatLng.longitude, currentLatLng.latitude)))

        mapboxMap.setStyle(Style.OUTDOORS) { style ->

            val position = CameraPosition.Builder()
                .target(currentLatLng)
                .zoom(17.0)
                .build()

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))

            val symbolLayer = symbolLayer(style, jsonSource)
            style.addLayer(symbolLayer)

            context.getLocationDebounce(disposable, { oldLocation ->
                currentLatLng = oldLocation.toLatlng()
            }, { newLocation ->
                markerUtil.moveMarkerAnimation(newLocation.toLatlng(), jsonSource, symbolLayer, liveRotate)
            })
        }
    }

    private fun symbolLayer(style: Style, jsonSource: GeoJsonSource): SymbolLayer {
        style.addImage("marker", markerUtil.markerVector())
        style.addSource(jsonSource)

        val symbolLayer = SymbolLayer("layer-1", "jkt")

        liveRotate.postValue(0f)
        liveRotate.observe(context as LifecycleOwner, Observer { rotation ->
            symbolLayer.withProperties(
                PropertyFactory.iconImage("marker"),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconRotate(rotation),
                PropertyFactory.iconAllowOverlap(true)
            )
        })
        return symbolLayer
    }
}