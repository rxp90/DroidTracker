package com.master.android.proyecto;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;
import android.widget.Toast;

/**
 * Clase que parsea un archivo KML y lo guarda en la base de datos.
 * 
 * @author Sobremesa
 * 
 */
public class KMLParserSAX extends AsyncTask<String, Void, Integer> {
	/**
	 * URL del fichero.
	 */
	private URL kmlUrl;
	/**
	 * Objeto Location con las coordenadas que están siendo procesadas.
	 */
	private Location locationActual;
	/**
	 * Ayudante de la base de datos.
	 */
	private BaseDatosGPS ayudanteBD;
	/**
	 * ID del recorrido que se crea.
	 */
	private Long idRecorrido;
	/**
	 * Contexto de la aplicación.
	 */
	private Context context;
	/**
	 * Variable para el gestor de notificaciones.
	 */
	private NotificationManager mNotificationManager;
	
	private static final int PARSING_FILE = 3;

	/**
	 * Constantes para identificar los posibles errores e informar al usuario.
	 */
	private static final int CORRECTO = 0;
	private static final int MALFORMED_URL = 1;
	private static final int IO_EXCEPTION = 2;
	private static final int SAX_EXCEPTION = 3;

	/**
	 * Constructor de la clase.
	 * 
	 * @param context
	 *            contexto de la aplicación.
	 */
	public KMLParserSAX(Context context) {
		this.context = context;
	}
	/*
	 * Muestra una notificación de parseo en curso. (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	@Override
	protected void onPreExecute() {

		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.kml;
		CharSequence tickerText = "Abriendo KML";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = "Route Tracking";
		CharSequence contentText = "Abriendo KML";

		Intent notificationIntent = new Intent(context,
				ProyectoTrackerActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		mNotificationManager.notify(PARSING_FILE, notification);

		super.onPreExecute();
	}

	/*
	 * Parsea y guarda en la base de datos. (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Integer doInBackground(String... params) {
		Integer res = CORRECTO;
		try {
			this.kmlUrl = new URL(params[0]);
			ayudanteBD = new BaseDatosGPS(context);
			ayudanteBD.abre();
			parse();
			ayudanteBD.cierra();
		} catch (MalformedURLException e) {
			res = MALFORMED_URL;
			e.printStackTrace();
		} catch (IOException e) {
			res = IO_EXCEPTION;
			e.printStackTrace();
		} catch (SAXException e) {
			res = SAX_EXCEPTION;
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * Muestra un Toast avisando del resultado de la operación.
	 */
	@Override
	protected void onPostExecute(Integer result) {
		CharSequence text;
		switch (result) {
		case CORRECTO:
			text = "Recorrido almacenado";
			break;
		case MALFORMED_URL:
			text = "URL incorrecta";
			break;
		case IO_EXCEPTION:
			text = "Error al leer la URL";
			break;
		case SAX_EXCEPTION:
			text = "Estructura de KML incorrecta";
			break;
		default:
			text = "Error";
		}
		
		mNotificationManager.cancel(PARSING_FILE);
		
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();

		super.onPostExecute(result);

	}

	/**
	 * Parsea el fichero KML.
	 * 
	 * @return Lista de Location.
	 * @throws SAXException
	 * @throws IOException
	 */
	private List<Location> parse() throws IOException, SAXException {

		final List<Location> puntos = new ArrayList<Location>();

		RootElement root = new RootElement("kml");
		Element placemark = root.getChild("Placemark");
		// Element timestamp = placemark.getChild("TimeStamp");

		placemark.getChild("name").setEndTextElementListener(
				new EndTextElementListener() {

					public void end(String body) {
						// Asigna al recorrido el nombre del primer <name> que
						// encuentre.
						if (idRecorrido == null) {
							idRecorrido = ayudanteBD.nuevoRecorrido(body);

						}
					}
				});

		Element point = placemark.getChild("Point");

		point.setStartElementListener(new StartElementListener() {

			public void start(Attributes attributes) {
				locationActual = new Location("gps");
			}
		});

		point.setEndElementListener(new EndElementListener() {

			public void end() {
				puntos.add(locationActual);
			}
		});
		point.getChild("coordinates").setEndTextElementListener(
				new EndTextElementListener() {

					public void end(String body) {

						String[] coordenadas = body.split(",");
						double longitud = Double.valueOf(coordenadas[0]);
						double latitud = Double.valueOf(coordenadas[1]);
						double altura = Double.valueOf(coordenadas[2]);

						locationActual.setLongitude(longitud);
						locationActual.setLatitude(latitud);
						locationActual.setAltitude(altura);
						// Dado que el DDMS no soporta <TimeStamp> se usará la
						// fecha del sistema.
						if (idRecorrido != null) {
							ayudanteBD.nuevoPunto(idRecorrido.intValue(),
									latitud, longitud, altura,
									System.currentTimeMillis());
						}
					}
				});

		Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8,
				root.getContentHandler());

		return puntos;

	}

	private InputStream getInputStream() throws IOException {
		InputStream inputStream = null;
		inputStream = kmlUrl.openConnection().getInputStream();
		return inputStream;
	}

}
