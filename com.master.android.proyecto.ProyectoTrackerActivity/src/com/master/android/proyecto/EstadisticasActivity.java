package com.master.android.proyecto;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

public class EstadisticasActivity extends Activity {

	private static final String TAG = "Estadísticas";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Establezco layout.
		setContentView(R.layout.layout_estadisticas);
		// Recupero los TextView
		TextView txtVelocidadMedia = (TextView) findViewById(R.id.txtVelocidadMedia);
		TextView txtTiempoTotal = (TextView) findViewById(R.id.txtTiempoTotal);
		TextView txtDistanciaTotal = (TextView) findViewById(R.id.txtDistanciaTotal);

		// Recojo el ID del recorrido que se desea analizar.
		Long idRecorrido = getIntent().getExtras().getLong("id");
		// Abro la base de datos.
		BaseDatosGPS ayudanteBD = new BaseDatosGPS(this);
		ayudanteBD.abre();
		// Recojo todas las coordenadas del recorrido a analizar.
		Cursor cursor = ayudanteBD
				.getCoordenadas(idRecorrido.intValue(), "1=1");

		// Variables para guardar las estadísticas.
		String tiempoTotal;
		Float distanciaTotal = 0f;
		Float velocidadMedia = 0f;

		// Eje x.
		List<Double> distancias = new ArrayList<Double>();
		// Eje y.
		List<Double> alturas = new ArrayList<Double>();

		// Variables Location para guardar las coordenadas mientras se recorren.
		Location location1;
		Location location2;

		// /////////////////////////////////////////////////////////////////////////////////////////////////////
		// /////////////////////////////////////////////////////////////////////////////////////////////////////

		// Recorro los puntos.

		if (cursor.moveToFirst()) {

			// Inicializo hora inicial y final.
			long horaInicial = cursor.getLong(4);
			long horaFinal = 0l;

			Log.d(TAG, "Hora inicial " + new Date(horaInicial).toLocaleString());

			// Establezco las primeras coordenadas.
			location1 = new Location("gps");
			location1.setLatitude(cursor.getDouble(1));
			location1.setLongitude(cursor.getDouble(2));
			location1.setAltitude(cursor.getDouble(3));

			// Distancia inicial.
			distancias.add(0.0);

			// Altura inicial.
			alturas.add(location1.getAltitude());

			while (cursor.moveToNext()) {

				location2 = new Location("gps");
				location2.setLatitude(cursor.getDouble(1));
				location2.setLongitude(cursor.getDouble(2));
				location2.setAltitude(cursor.getDouble(3));

				// Sumo a la distancia total.
				distanciaTotal += location1.distanceTo(location2);

				// Distancias eje x(km).
				distancias
						.add(Double.valueOf(Math.round(distanciaTotal * 100) / 100) / 1000);

				// Altura del punto (eje y).
				alturas.add(location2.getAltitude());

				// Establezco el punto final como inicial.
				location1 = location2;

				// Si es el último punto guardo la hora final.
				if (cursor.isLast()) {
					horaFinal = cursor.getLong(4);
					Log.d(TAG,
							"Hora final "
									+ new Date(horaFinal).toLocaleString());
				}
			}

			// Calculo la diferencia de fechas en milisegundos y obtengo la
			// cadena formateada con dicha diferencia.
			Long diferenciaHoras = horaFinal - horaInicial;
			tiempoTotal = timeDifference(diferenciaHoras);

			Log.d(TAG, "Tiempo total " + tiempoTotal);

			// Asigno la cadena a la interfaz gráfica.
			txtTiempoTotal.setText(tiempoTotal);

			// Paso todo el tiempo a horas y calculo la velocidad media en
			// km/h.
			Float horas = Float.valueOf(getHours(diferenciaHoras))
					+ Float.valueOf(getMinutes(diferenciaHoras)) / 60f
					+ Float.valueOf(getSeconds(diferenciaHoras)) / 3600f;

			velocidadMedia = (distanciaTotal / 1000f) / horas;

			// Asigno la velocidad media a la interfaz gráfica.
			velocidadMedia = Math.round(velocidadMedia * 1000f) / 1000f;
			String velMedia = velocidadMedia + " km/h";
			txtVelocidadMedia.setText(velMedia);

			// Asigno distancia total a la interfaz gráfica.
			String distTotal = Double
					.valueOf(Math.round(distanciaTotal * 100) / 100)
					/ 1000
					+ " km";
			txtDistanciaTotal.setText(distTotal);

			// Genero la URL de Google Charts y la asigno al componente
			// gráfico.
			String url = creaURL(1000, 300, distancias, alturas, getString(R.string.perfil),
					getString(R.string.altura));

			WebView webView = (WebView) findViewById(R.id.webView);
			webView.loadUrl(url);

		}

		// /////////////////////////////////////////////////////////////////////////////////////////////////////
		// /////////////////////////////////////////////////////////////////////////////////////////////////////

		// Cierro el cursor y la base de datos
		cursor.close();
		ayudanteBD.cierra();
	}

	/**
	 * Expresa los milisegundos dados en horas, minutos y segundos.
	 * 
	 * @param milisec
	 *            milisegundos
	 * @return Cadena HH:mm:ss
	 */
	private String timeDifference(Long milisec) {

		DecimalFormat df = new DecimalFormat("00");

		Long hours = getHours(milisec);
		Long minutes = getMinutes(milisec);
		Long seconds = getSeconds(milisec);

		return df.format(hours) + ":" + df.format(minutes) + ":"
				+ df.format(seconds);
	}

	/**
	 * Convierte de milisegundos a horas enteras.
	 * 
	 * @param milisec
	 *            milisegundos
	 * @return horas
	 */
	private Long getHours(Long milisec) {
		return milisec / (1000 * 60 * 60);
	}

	/**
	 * Convierte de milisegundos a minutos enteros.
	 * 
	 * @param milisec
	 *            milisegundos
	 * @return minutos
	 */
	private Long getMinutes(Long milisec) {
		return (milisec % (1000 * 60 * 60)) / (1000 * 60);
	}

	/**
	 * Convierte de milisegundos a segundos enteros.
	 * 
	 * @param milisec
	 *            milisegundos
	 * @return segundos
	 */
	private Long getSeconds(Long milisec) {
		return ((milisec % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
	}

	/**
	 * Crea la URL necesaria para visualizar la gráfica en Google Charts. Ancho
	 * x alto máximo 300000 píxeles.
	 * 
	 * @param ancho
	 *            Ancho de la gráfica.
	 * @param alto
	 *            Alto de la gráfica.
	 * @param x1
	 *            Valores de abscisas.
	 * @param y1
	 *            Valores de ordenadas.
	 * @param tituloGrafica
	 *            Título de la gráfica.
	 * @param leyendaDatos
	 *            Leyenda de los datos.
	 * @return URL de Google Charts.
	 */
	private String creaURL(Integer ancho, Integer alto, List<Double> x1,
			List<Double> y1, String tituloGrafica, String leyendaDatos) {

		StringBuilder x1Aux = new StringBuilder();
		StringBuilder y1Aux = new StringBuilder();

		Double xmin = 0.0;
		Double xmax = 0.0;
		Double ymin = 0.0;
		Double ymax = 0.0;

		Double[] valorx = new Double[x1.size()];
		Double[] valory = new Double[y1.size()];

		if (y1.size() > 1 && x1.size() > 1) {

			// Establezco máximos y mínimos.

			ymin = Collections.min(y1);
			ymax = Collections.max(y1);

			// Guardo los valores separados por comas y relleno los arrays.
			for (int i = 0; i < x1.size(); i++) {
				Double d = x1.get(i);

				x1Aux.append(d + ",");

				valorx[i] = d;

				// Aprovecho el bucle para encontrar el máximo.
				if (xmax < d) {
					xmax = d;
				}
			}

			// Guardo los valores separados por comas y relleno los arrays.
			for (int i = 0; i < y1.size(); i++) {
				Double d = y1.get(i);

				y1Aux.append(d + ",");

				valory[i] = d;
			}
		}

		/*
		 * for (Double d : x2) { x2Aux.append(d + ","); }
		 * 
		 * for (Double d : y2) { y2Aux.append(d + ","); }
		 */
		/*
		 * Cambiado por la versión codificada String url =
		 * "http://chart.apis.google.com/chart" + "?chxr=" + "0," + xmin + "," +
		 * xmax + "|1," + ymin + "," + ymax +
		 * "&chxs=0,676767,11.5,0,lt,676767|1,676767,11.5,0,lt,676767&chxt=x,y"
		 * + "&chs=" + ancho + "x" + alto + "&cht=lxy&chco=3072F3,FF0000" +
		 * "&chds=a" + "&chd=t:" + x1Aux.substring(0, x1Aux.length() - 1) + "|"
		 * + y1Aux.substring(0, y1Aux.length() - 1) + "|" + x2Aux.substring(0,
		 * x2Aux.length() - 1) + "|" + y2Aux.substring(0, y2Aux.length() - 1) +
		 * "&chdl=Ponies|Unicorns" + "&chdlp=b" + "&chls=2,4,1|1" +
		 * "&chma=5,5,5,25" + "&chtt=Estadisticas" + "&chts=F02A2A,11.5";
		 */

		// Codifico los valores de x e y.
		String encodex = simpleEncode(valorx, xmax);
		String encodey = simpleEncode(valory, ymax);

		String url = "http://chart.apis.google.com/chart" + "?chxr=" + "0,"
				+ xmin
				+ ","
				+ xmax
				+ "|1,"
				+ ymin
				+ ","
				+ ymax
				+ "&chxs=0,676767,11.5,0,lt,676767|1,676767,11.5,0,lt,676767&chxt=x,y"
				+ "&chs=" + ancho + "x" + alto
				+ "&cht=lxy&chco=3072F3,FF0000&chg=5,5,20,5" + "&chds=a"
				+ "&chd=s:" + encodex + "," + encodey + "&chdl=" + leyendaDatos
				+ "&chdlp=b" + "&chls=2,4,1|1" + "&chma=5,5,5,20" + "&chtt="
				+ tituloGrafica + "&chts=F02A2A,11.5";

		return url;
	}

	/**
	 * Codifica un array de números.
	 * 
	 * @param valores
	 *            Array de valores.
	 * @param maximo
	 *            Valor máximo del array (puede ser mayor).
	 * @return Cadena codificada.
	 */
	private String simpleEncode(Double[] valores, Double maximo) {
		String simpleEncoding = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		StringBuilder chartData = new StringBuilder();
		for (int i = 0; i < valores.length; i++) {
			Double currentValue = valores[i];
			if (currentValue >= 0) {
				chartData.append(""
						+ simpleEncoding.charAt((int) Math
								.round((simpleEncoding.length() - 1)
										* currentValue / maximo)));
			} else {
				chartData.append("_");
			}
		}

		return chartData.toString();
	}

	/**
	 * Codifica un array de números.
	 * 
	 * @param arrVals
	 *            Array de valores.
	 * @param maxVal
	 *            Valor máximo del array (puede ser mayor).
	 * @return Cadena codificada.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private String extendedEncode(final Double[] arrVals, final Double maxVal) {

		String EXTENDED_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.";

		int EXTENDED_MAP_LENGTH = EXTENDED_MAP.length();

		StringBuilder chartData = new StringBuilder();

		for (int i = 0, len = arrVals.length; i < len; i++) {
			// In case the array vals were translated to strings.
			Double numericVal = arrVals[i];
			// Scale the value to maxVal.
			Double scaledVal = Math.floor(EXTENDED_MAP_LENGTH
					* EXTENDED_MAP_LENGTH * numericVal / maxVal);

			if (scaledVal > (EXTENDED_MAP_LENGTH * EXTENDED_MAP_LENGTH) - 1) {
				chartData.append("..");
			} else if (scaledVal < 0) {
				chartData.append("__");
			} else {
				// Calculate first and second digits and add them to the output.
				Double quotient = Math.floor(scaledVal / EXTENDED_MAP_LENGTH);
				Double remainder = scaledVal - EXTENDED_MAP_LENGTH * quotient;
				chartData.append(EXTENDED_MAP.charAt(quotient.intValue())
						+ EXTENDED_MAP.charAt(remainder.intValue()));
			}
		}

		return chartData.toString();
	}
}
