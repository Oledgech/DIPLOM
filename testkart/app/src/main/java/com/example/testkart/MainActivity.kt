package com.example.testkart
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.location.Purpose
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.FitnessOptions
import com.yandex.mapkit.transport.masstransit.PedestrianRouter
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.transport.masstransit.Section
import com.yandex.mapkit.transport.masstransit.SectionMetadata.SectionData
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.runtime.Error
import kotlin.math.abs
import kotlin.random.Random
class MainActivity : AppCompatActivity(), com.yandex.mapkit.transport.masstransit.Session.RouteListener {
    lateinit var mapview:MapView
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var E_LOCATION: Point
    private var ROUTE_START_LOCATION=Point(68.998500, 33.082719)
    private var mapObject:MapObjectCollection?=null
    private var drivingRouter: PedestrianRouter?=null
    private var f:Int=0
    private var p:Int=0
    private var g:Int=0
    private var g1:Int=0
    private var Nugn:Int=500
    private lateinit var pointS: List<Point>
    var closestSection: Section? = null
    var closestDifference = Double.MAX_VALUE
    var closestGeometry: Polyline? = null
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = intent.extras?.getString("username").toString()
        Nugn=user.toInt()/2
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Данные о маршруте")
        builder.setMessage("Задан маршрут ${Nugn} м.\nПримерное количество шагов ${(Nugn/0.75).toInt()}\n")
        builder.setPositiveButton("ОК", null)
        builder.show()
        MapKitFactory.setApiKey("ba15629d-90ac-40ae-96b7-006bca150821")
        MapKitFactory.initialize(this)
        setContentView(R.layout.activity_main)
        mapview=findViewById(R.id.mapview)
        var mapKit:MapKit=MapKitFactory.getInstance()
        requestLocationPermission()
        var locationmapkit=mapKit.createUserLocationLayer(mapview.mapWindow)
        locationmapkit.isVisible=true
        readcord(object : OnCoordinatesReceivedListener {
            override fun onCoordinatesReceived(lat: Double, lon: Double) {
                E_LOCATION = Point(lat, lon)
                if(g1==0)
                {
                    initializeLocationManager()
                    g1+=1
                }
            }
        })
    }
    private fun initializeLocationManager() {
        locationManager = MapKitFactory.getInstance().createLocationManager()

        locationListener = object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                val userLocation = location.position
                println("Текущее местоположение: ${userLocation.latitude}, ${userLocation.longitude}")
                mapview.map.move(CameraPosition(userLocation, 15.0f, 0.0f, 0.0f))
                val startPoint = Point(userLocation.latitude, userLocation.longitude)
                val endPoint = Point(E_LOCATION.latitude, E_LOCATION.longitude)
                buildRouteWithApproximateLength(startPoint, endPoint)
//                buildCircularRoute(ROUTE_START_LOCATION)
//                buildRouteWithApproximateLength(endPoint, startPoint)
//                buildRouteBack(endPoint,startPoint)
            }
            override fun onLocationStatusUpdated(status: LocationStatus) {
                println("Статус местоположения: $status")
            }
        }
        locationManager.subscribeForLocationUpdates(0.0, 60000, 1.0, false, FilteringMode.OFF, Purpose.GENERAL, locationListener)

    }

    private fun requestLocationPermission()
    {
        if(ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),0)
            return
        }
    }
    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
    interface OnCoordinatesReceivedListener {
        fun onCoordinatesReceived(lat: Double, lon: Double)
    }
    override fun onStart() {
        mapview.onStart()
        MapKitFactory.getInstance().onStart()
        super.onStart()
    }
    override fun onMasstransitRoutes(p0: MutableList<Route>) {
        Log.d("Pit", "P: ${p0.size}")

        if (p0.size > 0) {
            for (section in p0.get(0).getSections()) {
                if(f==0)
                {
                    drawSection(
                        section.getMetadata().getData(),
                        SubpolylineHelper.subpolyline(
                            p0.get(0).getGeometry(), section.getGeometry()
                        )
                    )
                    f+=1
                }
                if(f!=0)
                {
                    val difference = abs(Nugn - section.getMetadata().getWeight().getWalkingDistance().value)
                    if (difference < closestDifference) {
                        closestDifference = difference
                        closestSection = section
                        closestGeometry = SubpolylineHelper.subpolyline(
                            p0[0].geometry,
                            section.geometry
                        )

                    }

                }


            }

        }
        closestSection?.let { section ->
            val walkingDistance = section.metadata.weight.walkingDistance.value
            val tolerance = Nugn / 2

            if (abs(walkingDistance - Nugn) <= tolerance && g==0) {
                g+=1
                Log.d("BestSection", "Лучший отрезок: ${walkingDistance} м (целевой: ${Nugn} м)")
                closestGeometry?.let { geometry ->
                    drawSectiontest(section.metadata.data, geometry)
                }
            } else {
                Log.d("RouteMismatch", "Длина маршрута: ${walkingDistance} м не соответствует ${Nugn} м (погрешность: ±${tolerance} м)")
            }
        } ?: run {
            Log.d("RouteError", "Не найден подходящий маршрут")
        }
    }
    private fun drawSection(data: SectionData, geometry: Polyline) {
        pointS = geometry.points
        for (point in pointS) {
            if(p==0)
            {
                ROUTE_START_LOCATION=point
                p+=1
            }
            test(point)

        }
    }
    private fun readcord(listener: OnCoordinatesReceivedListener) {
        val db=Firebase.firestore
        db.collection("cord")
            .get()
            .addOnSuccessListener { result ->
                val documents = result.documents

                if (documents.isNotEmpty()) {
                    val randomIndex = Random.nextInt(documents.size)
                    val randomDocument = documents[randomIndex]
                    val data = randomDocument.data
                    val lat= data?.get("lat").toString().toDouble()
                    val lon= data?.get("lon").toString().toDouble()
                    E_LOCATION=Point(lat,lon)
                    listener.onCoordinatesReceived(lat, lon)
                    println("Случайный документ: $lon")
                } else {
                    println("Коллекция пуста.")
                }
            }
    }
    private fun drawSectiontest(data: SectionData, geometry: Polyline) {
        val polylineMapObject: PolylineMapObject = mapObject!!.addPolyline(geometry)
        polylineMapObject.setStrokeColor(-0x10000)
        val points = polylineMapObject.geometry.points
        val endPoint = points.last()
        buildRouteBack(ROUTE_START_LOCATION, endPoint)
    }
    private fun test(point1: Point) {
        val routeOptions = RouteOptions(FitnessOptions(true))
        val points: MutableList<RequestPoint> = ArrayList()
        points.add(
            RequestPoint(
                ROUTE_START_LOCATION, RequestPointType.WAYPOINT, null, null
            )
        )
        points.add(
            RequestPoint(
                point1, RequestPointType.WAYPOINT, null, null
            )
        )
        drivingRouter = TransportFactory.getInstance().createPedestrianRouter()
        drivingRouter!!.requestRoutes(points, TimeOptions(), routeOptions, this)


    }
    override fun onMasstransitRoutesError(p0: Error) {
        var error="Ошибка"
        Toast.makeText(this,error,Toast.LENGTH_SHORT)
    }
    private fun buildRouteWithApproximateLength(startPoint: Point, endPoint: Point) {
        mapObject=mapview.map.mapObjects.addCollection()
        val avoidSteep = false
        val routeOptions = RouteOptions(FitnessOptions(avoidSteep))
        val requestPoints = mutableListOf<RequestPoint>()
        requestPoints.add(RequestPoint(startPoint, RequestPointType.WAYPOINT, null,null))
        requestPoints.add(RequestPoint(endPoint, RequestPointType.WAYPOINT, null,null))
        drivingRouter =  TransportFactory.getInstance().createPedestrianRouter()
        drivingRouter!!.requestRoutes(requestPoints, TimeOptions(), routeOptions, this)

    }


    private fun buildRouteBack(startPoint: Point, intermediatePoint: Point) {
        val routeOptions = RouteOptions(FitnessOptions(true))
        val requestPoints = mutableListOf<RequestPoint>()
        requestPoints.add(RequestPoint(intermediatePoint, RequestPointType.WAYPOINT, null, null))
        requestPoints.add(RequestPoint(startPoint, RequestPointType.WAYPOINT, null, null))
        drivingRouter = TransportFactory.getInstance().createPedestrianRouter()
        drivingRouter!!.requestRoutes(requestPoints, TimeOptions(), routeOptions, object : com.yandex.mapkit.transport.masstransit.Session.RouteListener {
            override fun onMasstransitRoutes(routes: MutableList<Route>) {
                if (routes.isNotEmpty()) {
                    val returnGeometry = routes[0].geometry
                    val polylineMapObjectBack: PolylineMapObject = mapObject!!.addPolyline(returnGeometry)
                    polylineMapObjectBack.setStrokeColor(-0x11000)
                }
            }

            override fun onMasstransitRoutesError(error: Error) {
                Toast.makeText(this@MainActivity, "Ошибка построения обратного пути", Toast.LENGTH_SHORT).show()
            }
        })
    }


}