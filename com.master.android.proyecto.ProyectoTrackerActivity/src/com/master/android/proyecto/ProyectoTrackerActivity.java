package com.master.android.proyecto;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class ProyectoTrackerActivity extends Activity {

	/**
	 * Variables para los componentes de iniciar y detener rastreo.
	 */
	private RelativeLayout mBtnStop;
	private RelativeLayout mBtnStart;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Establezco un layout.
		setContentView(R.layout.main);

		// Recupero los componentes gráficos.
		mBtnStart = (RelativeLayout) findViewById(R.id.layoutStartTracking);
		mBtnStop = (RelativeLayout) findViewById(R.id.layoutStopTracking);
		final RelativeLayout btnViewMap = (RelativeLayout) findViewById(R.id.layoutViewMaps);
		final RelativeLayout btnGetFromURL = (RelativeLayout) findViewById(R.id.layoutGetFromURL);

		// Botón de STOP invisible inicialmente.
		mBtnStop.setVisibility(View.INVISIBLE);

		// ////////////////////////////////////////////////////////////////////////////////////////

		// Si abro un archivo KML.
		Uri uri = getIntent().getData();
		if (uri != null) {
			KMLParserSAX parser = new KMLParserSAX(this);
			parser.execute(uri.toString());
		}
		// Listeners

		// Start
		mBtnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// Solicito un nombre para el recorrido.
				/*
				 * AlertDialog.Builder alert = new AlertDialog.Builder(
				 * ProyectoTrackerActivity.this);
				 * alert.setTitle(R.string.nuevoTrayecto);
				 * alert.setMessage(R.string.nombre); final EditText input = new
				 * EditText( ProyectoTrackerActivity.this);
				 * alert.setView(input);
				 * 
				 * // Aceptar alert.setPositiveButton(R.string.aceptar, new
				 * DialogInterface.OnClickListener() { public void
				 * onClick(DialogInterface dialog, int whichButton) {
				 * 
				 * // Recojo el nombre suministrado y lo envío al // servicio.
				 * String value = input.getText().toString();
				 * 
				 * Bundle b = new Bundle(); b.putString("nombreRecorrido",
				 * value);
				 * 
				 * Intent intent = new Intent(
				 * "com.master.android.proyecto.LocationTrackerService");
				 * intent.putExtras(b);
				 * 
				 * // Inicio el servicio de rastreo. startService(intent);
				 * 
				 * // Muestro STOP y oculto START
				 * mBtnStop.setVisibility(View.VISIBLE);
				 * mBtnStart.setVisibility(View.INVISIBLE);
				 * 
				 * } });
				 * 
				 * // Cancelar alert.setNegativeButton(R.string.cancelar, new
				 * DialogInterface.OnClickListener() {
				 * 
				 * public void onClick(DialogInterface dialog, int which) {
				 * dialog.dismiss(); return; } });
				 * 
				 * alert.show();
				 */

				// v2 Nombre automático para facilitar la tarea al usuario

				// Creo una cadena formateada con la fecha y hora actual.
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"dd/MM/yyyy - HH:mm:ss");

				String defaultName = dateFormat.format(new Date(System
						.currentTimeMillis()));

				// Envío el nombre por defecto (fecha - hora) al servicio.
				Bundle b = new Bundle();
				b.putString("nombreRecorrido", defaultName);

				Intent intent = new Intent(
						"com.master.android.proyecto.LocationTrackerService");
				intent.putExtras(b);

				// Inicio el servicio de rastreo.

				startService(intent);

				// Muestro STOP y oculto START
				mBtnStop.setVisibility(View.VISIBLE);
				mBtnStart.setVisibility(View.INVISIBLE);

			}
		});

		// Stop
		mBtnStop.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// Detengo el servicio de rastreo.
				Intent intent = new Intent(
						"com.master.android.proyecto.LocationTrackerService");
				stopService(intent);

				// Oculto STOP y muestro START.
				mBtnStop.setVisibility(View.INVISIBLE);
				mBtnStart.setVisibility(View.VISIBLE);
			}
		});

		// Ver mapa
		btnViewMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent actividadLista = new Intent(
						ProyectoTrackerActivity.this, RecorridosList.class);
				startActivity(actividadLista);
				// finish();
			}
		});

		// Descargar KML
		btnGetFromURL.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				// Solicito la URL de donde se bajará el archivo.
				AlertDialog.Builder alert = new AlertDialog.Builder(
						ProyectoTrackerActivity.this);
				alert.setTitle(R.string.descargar);
				alert.setMessage(R.string.url);

				final EditText inputURL = new EditText(
						ProyectoTrackerActivity.this);

				// v2 Pongo una URL por defecto para facilitar la prueba.
				inputURL.setText("http://db.tt/hlf9buoV");

				alert.setView(inputURL);

				// Aceptar
				alert.setPositiveButton(R.string.aceptar,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {

								// Recojo la URL y parseo el archivo KML.
								String url = inputURL.getText().toString();
								KMLParserSAX kml = new KMLParserSAX(
										ProyectoTrackerActivity.this);
								kml.execute(url);
							}
						});

				// Cancelar
				alert.setNegativeButton(R.string.cancelar,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});

				alert.show();

			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Si se está ejecutando el servicio oculto START y muestro STOP y al
		// revés en caso contrario.
		if (LocationTrackerService.sServiceRunning) {
			mBtnStop.setVisibility(View.VISIBLE);
			mBtnStart.setVisibility(View.INVISIBLE);
		} else {
			mBtnStop.setVisibility(View.INVISIBLE);
			mBtnStart.setVisibility(View.VISIBLE);
		}

	}

}