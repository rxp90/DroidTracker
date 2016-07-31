package com.master.android.proyecto;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class LocationTrackerService extends Service implements LocationListener {

	private static final int TRACKING_SERVICE_ID = 1;

	/*
	 * ¿Por qué una variable para saber el estado del servicio?
	 * 
	 * If your client and server code is part of the same .apk and you are
	 * binding to the service with a concrete Intent (one that specifies the
	 * exact service class), then you can simply have your service set a global
	 * variable when it is running that your client can check.
	 * 
	 * We deliberately don't have an API to check whether a service is running
	 * because, nearly without fail, when you want to do something like that you
	 * end up with race conditions in your code.
	 */

	/**
	 * Variable para indicar si está o no funcionando el servicio.
	 */
	public static boolean sServiceRunning;
	/**
	 * Variable para el gestor de notificaciones.
	 */
	private NotificationManager mNotificationManager;
	/**
	 * Variable para el gestor de localizaciones.
	 */
	private LocationManager mLocationManager;
	/**
	 * ID del recorrido actual.
	 */
	private Long mIDRecorrido;

	/**
	 * Constructor vacío.
	 */
	public LocationTrackerService() {
	}

	@Override
	public void onCreate() {

		// Me suscribo al evento de cambio de localización.
		locationUpdates();
		
		// TODO startForeground;
		// Creo una notificación indicando que se ha iniciado el servicio.
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.satellite;
		CharSequence tickerText = "Servicio de tracking activo";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = "Route Tracking";
		CharSequence contentText = "Servicio de rastreo iniciado";

		Intent notificationIntent = new Intent(this,
				ProyectoTrackerActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(this, contentTitle, contentText,
				contentIntent);

		mNotificationManager.notify(TRACKING_SERVICE_ID, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Aviso de que el servicio está corriendo.
		sServiceRunning = true;

		// Obtengo el nombre del nuevo recorrido.
		Bundle b = intent.getExtras();
		String nombreRecorrido = b.getString("nombreRecorrido");

		// Creo el recorrido en la base de datos.
		BaseDatosGPS ayudanteBD = new BaseDatosGPS(this);
		ayudanteBD.abre();
		mIDRecorrido = ayudanteBD.nuevoRecorrido(nombreRecorrido);
		ayudanteBD.cierra();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {

		// Aviso de que se ha parado el servicio.
		sServiceRunning = false;

		// Elimino la suscripción al cambio de localización.
		mLocationManager.removeUpdates(this);

		// Borro la notificación.
		mNotificationManager.cancel(TRACKING_SERVICE_ID);

		// Muestro un aviso de servicio detenido.
		CharSequence text = "Servicio de rastreo detenido";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(this, text, duration);
		toast.show();

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Método que se suscribe al cambio de localización.
	 */
	public void locationUpdates() {
		this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		String provider = LocationManager.GPS_PROVIDER;
		long minTime = 1000;
		float minDistance = 0;
		LocationListener listener = this;

		this.mLocationManager.requestLocationUpdates(provider, minTime,
				minDistance, listener);
	}

	// Métodos de la interfaz LocationListener.

	/**
	 * Guarda en la base de datos la localización que se pasa por parámetro.
	 */
	public void onLocationChanged(Location location) {
		if (mIDRecorrido != null) {
			BaseDatosGPS ayudanteBD = new BaseDatosGPS(this);
			ayudanteBD.abre();
			ayudanteBD.nuevoPunto(mIDRecorrido.intValue(),
					location.getLatitude(), location.getLongitude(),
					location.getAltitude(), location.getTime());
			ayudanteBD.cierra();
		}
	}

	public void onProviderDisabled(String provider) {

	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

}
