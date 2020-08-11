package geolocamapbox.unitec

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.lang.ref.WeakReference

//para poder activar la geolocalizacion debemos implementar  una neuva interface  que nos permite
//manipular los sensores y servicios del dispositivo desde el codigo.
class MainActivity : AppCompatActivity(),OnMapReadyCallback, PermissionsListener {



//Primero creamos un atibuto que es el tiempo en milisegundos  de solicutud de localizacion

  private var INTERVALO_DE_DEFECTO=1000L
  private var MAXIMO_INTERVALO_ESPERA=INTERVALO_DE_DEFECTO*2;
  //Declaramos un atributo de tipo permissionManager para acceder al GPS si esta apagado
   private var permissionsManager:PermissionsManager = PermissionsManager(this)

  //El Pemiso del GPS es una cosa y otro es la localizacion. Para ello declaramos el atrbuto locationEngine
   private var locationEngine:LocationEngine?=null

    //Una vez que se autoriza el GPS debemos informar al anterior objeto que se ponga a chambiar
    //El siguiente objeto le indica al locationEngine que ya se informo del prendido o autorizacion del GPS
    //A este tipo de atributos se les denomina callbacks
    private val callback=LocationCallbackActivity(this)



    //Creamos el atributo de tipo mapbox
    private  lateinit var mapboxMap: MapboxMap


    //Implementamos esa clasesisita
     internal inner class LocationCallbackActivity(activity:MainActivity):LocationEngineCallback<LocationEngineResult>{
        //En esta parte espere el objeto de tipo callback para acceder a la localizacion en el rango establecido
        private var activityWeak: WeakReference<MainActivity>?=null
        init {
            activityWeak= WeakReference(activity)
        }

        /******************************************************************************************************
         * Tercer bloque de código agregamos los siguiente métodos para
         */
        //Aqui en  el metodo  onSuccess , es donde constantemenete se esta checando la localizacion en el rango
        //que indicamos previamente en los atibutos.
        override fun onSuccess(result: LocationEngineResult?) {


            val activity: MainActivity? = activityWeak!!.get()
            if (activity != null) {
                val location = result?.lastLocation ?: return

                // Cada vez que se actualice con exito una nueva localización,  aparecera este Toast
                //Puedes eliminarlo si gustas para que no apareca cada rato y este mame y mame en la pantalla del usuario
                //
                Toast.makeText(
                    activity,
                    "Lat. actualizada:"+  result.lastLocation!!.latitude.toString(),
                    Toast.LENGTH_SHORT
                ).show()

                //La nueva posicion actualiada es la variable "pos"; los dobles signos de exclamacion en kotlin implican que este valor NO ES NULO
               // esta posicion es la que debe de irse agregando a un ArrayList en tu Perfil.
               //ES LA QUE NOS INTERESA PARA FINES DE MONITOREO O TRACKING DEL USUARIO!, HAY QUE PINCHE EMOCION!!
                var pos= LatLng()
                pos.latitude=result.lastLocation!!.latitude
                pos.longitude=result.lastLocation!!.longitude
// La variable posicion es la que va a  verse reflejada y atualizada en el mapa y lo trasladamos con el método:
                //animateCamera, pudes cambiar el zoom de la camara y el metodo "tilt", es para poner que tan inclinado se muestra tu mapita
    //para que simule que estas en 3D, es una mamada pero bueno!!, se ve chingon e impresiona a los no-ingenieros.
                val posicion = CameraPosition.Builder()
                    .target(pos)
                    .zoom(18.0  )
                    .tilt(20.0)
                    .build()

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(posicion), 500)

                // Pasamos la nueva localizacion actualizada  al mapbox cada que se cumpla el rango de tiempo
                if (activity.mapboxMap != null && result.lastLocation != null) {
                    activity.mapboxMap.getLocationComponent()
                        .forceLocationUpdate(result.lastLocation)
                }
            }

        }

        /***********************
         *  En caso de fallo es decie excepcion dejamos al sistema de excepciones que haga su tarea, que
         *  Nos de la que crea conveniente
         *
         */
        override fun onFailure(exception: Exception) {
            val activity: MainActivity? = activityWeak!!.get()
            if (activity != null) {
                Toast.makeText(
                    activity, exception.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }





//NOTAAAAAA: CAMBIA TU CLAVE AQUI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Antes de invocar el token se de hacer en esta seccion antes del activit_main
        //sino lo haces te marcara error al ejecutar la app.
        Mapbox.getInstance(this,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")

        setContentView(R.layout.activity_main)
        //Ahora si inicializamos el mapView para que contenga neustro mapa
        mapView.onCreate(savedInstanceState)
        //Descargamos el mapa asincronicamente
        mapView.getMapAsync(this)


    }

    override fun onMapReady(mapboxMap: MapboxMap) {
            //En este momento nuestro atributo mapBoxMap lo asignamos al argumento mapboxMap
        this.mapboxMap=mapboxMap
        //Despues de esto, ya deberia de aparecer el mapa en el zoom y las coordendas dadas peroo.
        //aqui pondremos el estilo del mapa
        mapboxMap.setStyle(Style.Builder().fromUri("mapbox://styles/daton2/ckdkw6nlu1jqr1hpdx5y82q77")){
            /*
        LA CEREZA DEL PASTEL!!!!!!!!!!!!!!!!!!
            */
            habilitarLocalizacion(it)

        }

    }

    /************************************************************************************************
     * 1.- PRIMER BLOQUE DE CODIGO: CHECAMOS QUE EL USUARIO CONCEDA EL PERMISO:METODO ROCK-STAR ES QUE HACE TODO!!!
     * Para mayor informacion de lint check verifica el siguiente link que con todo cariño les proporciono:
     *  https://developer.android.com/studio/write/lint
     *************************************************************************************************
     * */
    @SuppressLint("MissingPermission")
    private fun habilitarLocalizacion(loadedMapStyle: Style) {
        // Checamos si los permisos han sifo concedidos sino, forzara  que a la de a guevo los conceda,
       // posteriormete, si es aceptado y concedido se el cuerpo del IF
        //Generamos la variable miLocationComponentOptios que con ella agregamos el continua tracking o
        //Seguimiento asi como el color del trackng, aqui podras peonalizar el color , aqui le puse
        //el del color pimary pero pdras poner el que se te de la rechingada gana.
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val miLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .locationComponentOptions(miLocationComponentOptions)
                .build()

            // Aqui con la ayuda de la las bibliotecas standarizadas de kotlin  le pasamos al mapa
            //las opciones de al variable miLocationComponentOptions
            //Para saber de estas bibliotecas en koltin te paso el link oficial. Aqui usamos la funcion de
            // orden superior apply :
          //  https://kotlinlang.org/api/latest/jvm/stdlib/
            mapboxMap.locationComponent.apply {

                activateLocationComponent(locationComponentActivationOptions)

                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                //Sacamos el valor de lastKnownLocation que e este cso sria a inicial
                var loca=   lastKnownLocation
                Toast.makeText(applicationContext, "Latitud  inicial es esta!! ${loca?.latitude}", Toast.LENGTH_LONG).show()

                // Ajustamos el modo de Reder de la camara a Brújula e iniciamos el tracking invocanso el emtodo de
                //busqueda eta funcion esta implementada mas abajito
                renderMode = RenderMode.COMPASS
                iniciarMaquinaDeSeguimiento();
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }
    /********************************************************************************************
    Metodo de seguimiento impornatntisimo!!
    Ya casi terminamos,  ya no te desesperes, aguanta!! querías ser ingeniero no???!!
     */
    @SuppressLint("MissingPermission")
    //En la siguiente funcion iniciamos la maquina de segumiemnto autonomo!!
    private fun iniciarMaquinaDeSeguimiento() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        val request =
            LocationEngineRequest.Builder(INTERVALO_DE_DEFECTO)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(MAXIMO_INTERVALO_ESPERA).build()
        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        locationEngine!!.getLastLocation(callback)
    }
    /***************************************************************************************************
    Termina primer bloque de código
     ******************************************************************************************************/

    // En este metodo nosotros vamos a implementar un mensaje donde el usuario verifica o se le informa
    // porqué prenderemos el GPS
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
          Toast.makeText(this, "Esta aplicacion necesita el GPS para ubicarte,", Toast.LENGTH_LONG).show()
    }

    /**************************************************************************************************
    2.- SEGUNDO BLOQUE DE CÓDIGO: cOMPLETAMOS EL onPermissionResult
     */
    override fun onPermissionResult(granted: Boolean) {
        //Se se concede el permiso entonces hanilitamos neustra localizacion con el metodo habilitarLocalizacion
        if (granted) {
            habilitarLocalizacion(mapboxMap.style!!)
        } else {
            Toast.makeText(this,"No aceptaste que te localice:CULERO!!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    //Agregamos el metodo donde se verifica el estatus del permiso

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /************************************************************************************************************
    Termina el segundo bloque de codigo
     */

/***************************************************************************************************
Finalmente los bloques de manejo d memoria
 */
override fun onStart() {
    super.onStart()
    mapView.onStart()
}

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

}