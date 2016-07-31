package com.master.android.proyecto;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;

public class KMLWriter extends AsyncTask<Long, Void, Boolean> {

	/*
	 * <kml> <Placemark> <name>Stage 21</name> <description> </description>
	 * <Point> <coordinates>2.4500920000000406,48.785327,44.4</coordinates>
	 * </Point> </Placemark> </kml>
	 */
	private Context context;
	/**
	 * Variable para el gestor de notificaciones.
	 */
	private NotificationManager mNotificationManager;
	/**
	 * ID para la notificación.
	 */
	private static final int EXPORT_TO_KML_ID = 2;
	/**
	 * Variable que almacena el identificador del recorrido.
	 */
	private Long idRecorrido;

	/**
	 * Constructor de la clase.
	 * 
	 * @param context
	 *            Contexto en el que se ejecuta.
	 */
	public KMLWriter(Context context) {
		this.context = context;
	}

	// TODO Guardar y leer tiempo en KML.
	@Override
	protected void onPreExecute() {

		// Muestra una notificación de exportación en curso.
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = R.drawable.kml;
		CharSequence tickerText = "Exportando a KML";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		CharSequence contentTitle = "Route Tracking";
		CharSequence contentText = "Exportando a KML";

		Intent notificationIntent = new Intent(context,
				ProyectoTrackerActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		mNotificationManager.notify(EXPORT_TO_KML_ID, notification);

		super.onPreExecute();
	}

	@Override
	protected Boolean doInBackground(Long... params) {

		// Recogemos nombre y coordenadas del recorrido.

		boolean res = false;

		idRecorrido = params[0];

		String nombreRecorrido = null;

		BaseDatosGPS ayudanteBD = new BaseDatosGPS(context);
		ayudanteBD.abre();

		Cursor cursorNombreRecorrido = ayudanteBD
				.getNombreRecorrido(idRecorrido.intValue());

		if (cursorNombreRecorrido.moveToFirst()) {
			nombreRecorrido = cursorNombreRecorrido.getString(0);
		}

		cursorNombreRecorrido.close();

		Cursor cursorCoordenadas = ayudanteBD.getCoordenadas(
				idRecorrido.intValue(), "1=1");

		// Creamos la estructura del documento y recorremos las coordenadas para
		// almacenarlas.

		if (nombreRecorrido != null
				&& !Utilidades.containsReservedChars(nombreRecorrido)) {

			try {
				DocumentBuilderFactory docFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder docBuilder;
				docBuilder = docFactory.newDocumentBuilder();

				Document doc = docBuilder.newDocument();

				Element rootElement = doc.createElement("kml");
				doc.appendChild(rootElement);

				if (cursorCoordenadas.moveToFirst()) {

					String longitud;
					String latitud;
					String altura;

					while (cursorCoordenadas.isAfterLast() == false) {

						// Placemark
						Element placeMark = doc.createElement("Placemark");
						rootElement.appendChild(placeMark);

						// Name
						Element name = doc.createElement("name");
						name.appendChild(doc.createTextNode(nombreRecorrido));
						placeMark.appendChild(name);

						// Description
						Element description = doc.createElement("description");
						placeMark.appendChild(description);

						// Point
						Element point = doc.createElement("Point");
						placeMark.appendChild(point);

						// Coordinates
						Element coordinates = doc.createElement("coordinates");

						latitud = String
								.valueOf(cursorCoordenadas.getDouble(1));
						longitud = String.valueOf(cursorCoordenadas
								.getDouble(2));
						altura = String.valueOf(cursorCoordenadas.getDouble(3));

						coordinates.appendChild(doc.createTextNode(longitud
								+ "," + latitud + "," + altura));
						point.appendChild(coordinates);

						// Siguiente punto
						cursorCoordenadas.moveToNext();

					}
					cursorCoordenadas.close();
				}

				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);

				// Guardamos el fichero.
				
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(sdCard.getAbsolutePath()
						+ "/routeTracking/kml");
				dir.mkdirs();
				File file = new File(dir, nombreRecorrido + ".kml");

				StreamResult result = new StreamResult(file);

				// StreamResult result = new StreamResult(System.out);

				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
						"yes");

				transformer.setOutputProperty(OutputKeys.INDENT, "no");

				transformer.transform(source, result);

				System.out.println("Archivo guardado");

				res = true;

			} catch (ParserConfigurationException e) {

				e.printStackTrace();
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		} else {

			res = false;

		}
		return res;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			// Conversión correcta.

			// Borro la notificación en curso.
			mNotificationManager.cancel(EXPORT_TO_KML_ID);

			// Muestro una notificación de exportación completada.
			mNotificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);

			int icon = R.drawable.kml;
			CharSequence tickerText = "Fichero exportado con éxito";
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;

			CharSequence contentTitle = "Route Tracking";
			CharSequence contentText = "Fichero exportado con éxito en SD\\routeTracking\\kml";

			Intent notificationIntent = new Intent(context,
					ProyectoTrackerActivity.class);

			PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
					notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);

			mNotificationManager.notify(EXPORT_TO_KML_ID, notification);
		} else {
			// Error al exportar.

			// Contiene caracteres especiales.
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
			alertDialog.setTitle("Error en el nombre");
			alertDialog
					.setMessage("El nombre del archivo no puede contener ninguno de estos caracteres: "
							+ Utilidades.RESERVED_CHARS.toString());
			alertDialog.setCancelable(true);

			// Aceptar.
			alertDialog.setPositiveButton(R.string.aceptar,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							Utilidades.cambiarNombreRecorrido(context,
									idRecorrido);
						}
					});
			alertDialog.show();

		}

		super.onPostExecute(result);
	}

}
