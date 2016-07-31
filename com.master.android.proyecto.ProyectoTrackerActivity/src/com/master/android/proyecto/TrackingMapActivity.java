package com.master.android.proyecto;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Activity que controla el mapa de la aplicación.
 * 
 * @author Raul
 * 
 */
public class TrackingMapActivity extends MapActivity {

	/**
	 * Constantes para establecer los puntos candidatos a ser dibujados según su
	 * visibilidad en pantalla.
	 */
	static final int SIN_MARGENES = 0;
	static final int MARGENES_SEGUROS = 1;
	static final int SOLO_VISIBLES = 2;

	/**
	 * Constantes para definir el nivel de detalle del recorrido a dibujar según
	 * la distancia entre píxeles.
	 */
	static final int DETALLE_MUY_ALTO = 10;
	static final int DETALLE_ALTO = 30;
	static final int DETALLE_MEDIO = 50;
	static final int DETALLE_BAJO = 80;
	static final int DETALLE_MUY_BAJO = 100;

	/**
	 * Lista de overlays del mapa.
	 */
	private List<Overlay> mMapOverlays;
	/**
	 * Variable para establecer la relación coordenadas - píxel.
	 */
	private Projection mProjection;
	/**
	 * Variable para almacenar el nivel de detalle del mapa.
	 */
	private Integer mNivelDetalle;
	/**
	 * Variable para guardar el margen que se tendrá en cuenta a la hora de
	 * seleccionar puntos.
	 */
	private Integer mMargen;
	/**
	 * Variable para almacenar el MapView.
	 */
	private MapView mMapView;
	/**
	 * Variable que guarda el ID del recorrido mostrado.
	 */
	private Long mIDRecorrido;
	/**
	 * Variable que guarda si está o no activa la vista satélite.
	 */
	private boolean mVistaSatelite;
	/*
	 * latitudeE6 - The point's latitude. This will be clamped to between -80
	 * degrees and +80 degrees inclusive, in order to maintain accuracy in the
	 * Mercator projection.
	 */
	/**
	 * Latitudes máxima y mínima.
	 */
	static final Double LATITUDE_MAX = 80 * 1E6;
	static final Double LATITUDE_MIN = -80 * 1E6;

	/*
	 * longitudeE6 - The point's longitude. This will be normalized to be
	 * greater than -180 degrees and less than or equal to +180 degrees.
	 */
	/**
	 * Longitudes máxima y mínima.
	 */
	static final Double LONGITUDE_MAX = 180 * 1E6;
	static final Double LONGITUDE_MIN = -179.999999 * 1E6;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Asociamos el layout del mapa con la Activity.
		setContentView(R.layout.layout_mapa);

		// Inicialización de las variables.
		mMapView = (MapView) findViewById(R.id.mapa);
		mProjection = mMapView.getProjection();
		mNivelDetalle = DETALLE_MEDIO;
		mMargen = MARGENES_SEGUROS;
		mVistaSatelite = true;
		mMapView.setSatellite(mVistaSatelite);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mIDRecorrido = extras.getLong("id");
		}

		// Obtengo el primer punto desde la base de datos y centro el mapa.

		BaseDatosGPS ayudanteBD = new BaseDatosGPS(TrackingMapActivity.this);
		ayudanteBD.abre();

		Cursor cursor = ayudanteBD.getCoordenadas(mIDRecorrido.intValue(),
				"1=1");

		if (cursor.moveToFirst()) {
			Double lat = cursor.getDouble(1);
			Double lon = cursor.getDouble(2);
			cursor.close();
			ayudanteBD.cierra();
			mMapView.getController().setCenter(geopointFromLatLong(lat, lon));
			mMapView.getController().setZoom(16);

		}

		// Cierro el Cursor y la base de datos.
		cursor.close();
		ayudanteBD.cierra();

		// Dibujamos Overlays
		mMapOverlays = mMapView.getOverlays();
		mMapOverlays.add(new TrackOverlay());

	}

	/*
	 * Menú de opciones (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		// Añadimos el menú para aumentar el número de puntos del trazado.
		menu.add(0, 0, 0, R.string.detalleTrazado).setIcon(R.drawable.detail);
		// Añadimos el menú para dibujar puntos no visibles.
		menu.add(0, 1, 0, R.string.margenes).setIcon(R.drawable.margin);
		// Añadimos el menú para cambiar el tipo de vista.
		menu.add(0, 2, 0, R.string.cambiarVista).setIcon(R.drawable.changeview);
		return result;
	}

	/*
	 * Opción del menú seleccionada (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		AlertDialog.Builder dialogo = new AlertDialog.Builder(
				TrackingMapActivity.this);

		switch (item.getItemId()) {

		// Si se ha pulsado sobre Nivel de detalle del trazado.
		case 0:
			dialogo.setTitle(R.string.detalleTrazado);
			dialogo.setCancelable(true);

			// Selección por defecto
			int selectedItem = 0;
			switch (mNivelDetalle) {
			case DETALLE_MUY_ALTO:
				selectedItem = 0;
				break;
			case DETALLE_ALTO:
				selectedItem = 1;
				break;
			case DETALLE_MEDIO:
				selectedItem = 2;
				break;
			case DETALLE_BAJO:
				selectedItem = 3;
				break;
			case DETALLE_MUY_BAJO:
				selectedItem = 4;
				break;
			}

			// Selección de un nivel en la lista.
			dialogo.setSingleChoiceItems(R.array.nivelDetalleArray,
					selectedItem, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							// TODO Poner distancias en función del tamaño de la
							// pantalla.
							case 0:
								mNivelDetalle = DETALLE_MUY_ALTO;
								break;
							case 1:
								mNivelDetalle = DETALLE_ALTO;
								break;
							case 2:
								mNivelDetalle = DETALLE_MEDIO;
								break;
							case 3:
								mNivelDetalle = DETALLE_BAJO;
								break;
							case 4:
								mNivelDetalle = DETALLE_MUY_BAJO;
								break;
							}
						}
					});

			// Aceptar
			dialogo.setPositiveButton(R.string.aceptar,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							// Redibujo el mapa y cierro el diálogo.
							mMapView.invalidate();
							dialog.dismiss();
						}
					});
			dialogo.show();
			break;

		case 1:

			// Si se ha pulsado sobre Márgenes.

			dialogo.setTitle(R.string.margenes);
			dialogo.setCancelable(true);

			// Selección por defecto. El valor del entero es igual que el orden
			// en la lista.
			int selectedItem2 = mMargen;

			// Selección de un nivel en la lista.
			dialogo.setSingleChoiceItems(R.array.puntosNoVisiblesArray,
					selectedItem2, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// El valor del entero es igual que el orden en la
							// lista.
							mMargen = which;
						}
					});

			// Aceptar.
			dialogo.setPositiveButton(R.string.aceptar,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// Cuando se haga clic en Aceptar redibujamos el
							// mapa
							mMapView.invalidate();
							dialog.dismiss();
						}
					});

			dialogo.show();

			break;
		case 2:

			// Si se ha pulsado sobre Cambiar vista.
			mVistaSatelite = !mVistaSatelite;
			mMapView.setSatellite(mVistaSatelite);

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Crea un Geopoint dadas su latitud y longitud.
	 * 
	 * @param latitud
	 * @param longitud
	 * @return Geopoint
	 */
	public GeoPoint geopointFromLatLong(double latitud, double longitud) {
		return new GeoPoint((int) (latitud * 1E6), (int) (longitud * 1E6));
	}

	/**
	 * Clase interna que extiende de Overlay y crea uno con el recorrido
	 * trazado.
	 * 
	 * @author Raul
	 * 
	 */
	private class TrackOverlay extends Overlay {

		/**
		 * Constructor por defecto.
		 */
		public TrackOverlay() {
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);

			// Color y estilo de la línea.

			Paint mPaint = new Paint();
			mPaint.setColor(Color.BLUE);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setDither(true);
			mPaint.setPathEffect(new CornerPathEffect(10));
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setStrokeWidth(3);
			mPaint.setAntiAlias(true);

			// PRIMER FILTRO: Sólo dibujaremos los puntos que se vean (Opción
			// seleccionable)

			/*
			 * Si somos completamente estrictos en dibujar líneas sólo entre los
			 * puntos visibles en pantalla puede perderse detalle y mostrar una
			 * trayectoria errónea haciendo desaparece líneas, por lo que
			 * daremos un margen para evitar algunos de estos fallos.
			 */

			String select = getWhereQuery(canvas);

			/*
			 * PathTask async = new PathTask(canvas, mPaint);
			 * 
			 * async.execute(select);
			 * 
			 * if (path != null) { canvas.drawPath(path, mPaint);
			 * mapView.postInvalidate(); }
			 */

			// Abro la base de datos y recojo los puntos que pasan el primer
			// filtro.
			BaseDatosGPS ayudanteBD = new BaseDatosGPS(TrackingMapActivity.this);
			ayudanteBD.abre();

			Cursor cursor = ayudanteBD.getCoordenadas(mIDRecorrido.intValue(),
					select);

			// Par de objetos GeoPoint para ayudar en la iteración.
			GeoPoint gP1;
			GeoPoint gP2;

			/*
			 * In Android, if you are in a situation in which you are drawing
			 * lots of connected lines (like routes on MapView) try to use
			 * "canvas.drawPath". In one of my applications I am drawing nearly
			 * 500 routes containing 10.000 points and using "canvas.drawPath"
			 * instead of "canvas.drawLine" decreases the drawing time as up to
			 * %30.
			 */

			// Objeto Path sobre el que se plasmará el recorrido.
			Path path = new Path();

			// Itero sobre los puntos para ir rellenando el Path.
			int i = 0;
			if (cursor.moveToFirst()) {

				gP1 = geopointFromLatLong(cursor.getDouble(1),
						cursor.getDouble(2));

				while (cursor.moveToNext()) {

					gP2 = geopointFromLatLong(cursor.getDouble(1),
							cursor.getDouble(2));

					// Obtenemos los puntos (píxeles) del canvas a los que
					// pertenecen los
					// GeoPoints.
					Point p1 = mProjection.toPixels(gP1, null);
					Point p2 = mProjection.toPixels(gP2, null);

					// SEGUNDO FILTRO.
					// Para mejorar el rendimiento filtraremos
					// sólo los puntos de la pantalla que tengan una distancia
					// mínima de píxeles entre sí. Al filtrar por puntos de
					// pantalla y no por distancias // reales conseguimos que al
					// hacer zum se mejore el detalle, // ya que la distancia
					// real siempre es constante y la que // varía es la de la
					// pantalla.
					if (mayorQueDistanciaMinima(p1, p2, mNivelDetalle)) {
						mProjection.toPixels(gP1, p1);
						mProjection.toPixels(gP2, p2);
						path.moveTo(p2.x, p2.y);
						path.lineTo(p1.x, p1.y);

						gP1 = gP2;

						// Datos de consola.
						// TODO (Buenas práticas) - Cambiar por Log.
						i++;
						System.out.println("Puntos dibujados: " + i);
					}
				}

				cursor.close();
				ayudanteBD.cierra();

				// Dibujamos el camino.
				canvas.drawPath(path, mPaint);
			}
		}

		/**
		 * Devuelve una cadena referente al WHERE con la que se consultan los
		 * puntos candidatos a ser dibujados tras el primer filtro (márgenes)
		 * 
		 * @param canvas
		 * @return WHERE de la consulta SQL.
		 */
		private String getWhereQuery(Canvas canvas) {

			Integer margenAncho = canvas.getWidth();
			Integer margenAlto = canvas.getHeight();

			GeoPoint origen;
			GeoPoint puntoMaximo;

			switch (mMargen) {

			case SIN_MARGENES:
				// Todos los puntos cumplirían esta condición.
				origen = new GeoPoint(LATITUDE_MAX.intValue(),
						LONGITUDE_MIN.intValue());
				puntoMaximo = new GeoPoint(LATITUDE_MIN.intValue(),
						LONGITUDE_MAX.intValue());
				break;

			case MARGENES_SEGUROS:
				origen = mProjection
						.fromPixels(0 - margenAncho, 0 - margenAlto);
				puntoMaximo = mProjection.fromPixels(canvas.getWidth()
						+ margenAncho, canvas.getHeight() + margenAlto);
				break;

			case SOLO_VISIBLES:
				origen = mProjection.fromPixels(0, 0);
				puntoMaximo = mProjection.fromPixels(canvas.getWidth(),
						canvas.getHeight());
				break;

			default:
				// Márgenes seguros
				origen = mProjection
						.fromPixels(0 - margenAncho, 0 - margenAlto);
				puntoMaximo = mProjection.fromPixels(canvas.getWidth()
						+ margenAncho, canvas.getHeight() + margenAlto);
			}

			String select = BaseDatosGPS.colLongitud
					+ " > "
					+ ((Double) (origen.getLongitudeE6() / 1e6)).toString()
					+ " AND "
					+ BaseDatosGPS.colLongitud
					+ " < "
					+ ((Double) (puntoMaximo.getLongitudeE6() / 1e6))
							.toString() + " AND " + BaseDatosGPS.colLatitud
					+ " < "
					+ ((Double) (origen.getLatitudeE6() / 1e6)).toString()
					+ " AND " + BaseDatosGPS.colLatitud + " > "
					+ ((Double) (puntoMaximo.getLatitudeE6() / 1e6)).toString();

			// String selection = ""; Eiminado para mantener la misma estructura
			// de SELECT siempre.

			return select;
		}

		/**
		 * Decide si la distancia entre los dos puntos dados es mayor a la que
		 * se especifica.
		 * 
		 * @param a
		 *            Primer punto
		 * @param b
		 *            Segundo punto
		 * @param distanciaMinima
		 *            Distancia mínima
		 * @return true si la distancia es mayor, falso en cualquier otro caso.
		 */
		private Boolean mayorQueDistanciaMinima(Point a, Point b,
				Integer distanciaMinima) {
			boolean res = false;
			double distanciaEntrePuntos = java.lang.Math
					.sqrt((double) (a.x - b.x) * (a.x - b.x))
					+ ((a.y - b.y) * (a.y - b.y));
			if (distanciaEntrePuntos > distanciaMinima) {
				res = true;
			}
			return res;
		}

		/**
		 * AsyncTask para obtener el Path para el mapa.
		 * 
		 * @author Sobremesa
		 * 
		 */
		@SuppressWarnings("unused")
		@Deprecated
		private class PathTask extends AsyncTask<String, Void, Path> {

			private String select;
			private Paint mPaint;
			private Canvas canvas;

			public PathTask(Canvas canvas, Paint paint) {
				this.canvas = canvas;
				this.mPaint = paint;
			}

			@Override
			protected void onPostExecute(Path result) {
				// path = result;
				// mMapView.postInvalidate();
				// super.onPostExecute(result);
			}

			@Override
			protected Path doInBackground(String... params) {

				select = params[0];
				BaseDatosGPS ayudanteBD = new BaseDatosGPS(
						TrackingMapActivity.this);
				ayudanteBD.abre();
				Cursor cursor = ayudanteBD.getCoordenadas(
						mIDRecorrido.intValue(), select);

				GeoPoint gP1;
				GeoPoint gP2;
				Path path = new Path();

				int i = 0;
				if (cursor.moveToFirst()) {
					gP1 = geopointFromLatLong(cursor.getDouble(1),
							cursor.getDouble(2));
					while (cursor.moveToNext()) {

						gP2 = geopointFromLatLong(cursor.getDouble(1),
								cursor.getDouble(2));

						// Obtenemos los puntos del canvas a los que pertenecen
						// los
						// GeoPoints.
						Point p1 = mProjection.toPixels(gP1, null);
						Point p2 = mProjection.toPixels(gP2, null);

						// SEGUNDO FILTRO
						// Para mejorar el rendimiento filtraremos sólo los
						// puntos
						// de la pantalla que tengan una distancia mínima de
						// píxeles
						// entre sí.
						// Al filtrar por puntos de pantalla y no por distancias
						// reales conseguimos que al hacer zum se mejore el
						// detalle,
						// ya que la distancia real siempre es constante y la
						// que
						// varía es la de la pantalla.
						if (mayorQueDistanciaMinima(p1, p2, mNivelDetalle)) {
							mProjection.toPixels(gP1, p1);
							mProjection.toPixels(gP2, p2);
							path.moveTo(p2.x, p2.y);
							path.lineTo(p1.x, p1.y);
							i++;
							System.out.println("Puntos dibujados: " + i);

							/*
							 * In Android, if you are in a situation in which
							 * you are drawing lots of connected lines (like
							 * routes on MapView) try to use "canvas.drawPath".
							 * In one of my applications I am drawing nearly 500
							 * routes containing 10.000 points and using
							 * "canvas.drawPath" instead of "canvas.drawLine"
							 * decreases the drawing time as up to %30.
							 */

							gP1 = gP2;
						}
					}

					// Dibujamos el camino.
					// canvas.drawPath(path, mPaint);
				}

				cursor.close();
				ayudanteBD.cierra();
				return path;

			}
		}

	}
}