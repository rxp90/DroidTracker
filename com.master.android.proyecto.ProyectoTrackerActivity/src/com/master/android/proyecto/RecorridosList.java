package com.master.android.proyecto;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class RecorridosList extends ListActivity {

	private SimpleCursorAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Asigno un layout.
		setContentView(R.layout.layout_lista);

		// Abro la base de datos y obtengo un Cursor con todos los recorridos.
		BaseDatosGPS ayudanteBD = new BaseDatosGPS(RecorridosList.this);
		ayudanteBD.abre();

		String[] displayFields = new String[] { BaseDatosGPS.colNombreRecorrido };
		int[] displayViews = new int[] { android.R.id.text1 };

		Cursor cursor = ayudanteBD.getRecorridos();

		// Deprecated, el nuevo constructor está a partir de la API 11 (3.0)
		adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_expandable_list_item_1, cursor,
				displayFields, displayViews);

		setListAdapter(adapter);

		// Cierro la base de datos. (No cerrar el cursor o no mostrará los
		// datos).
		ayudanteBD.cierra();
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
		menu.add(0, 0, 0, R.string.borrarTodos).setIcon(R.drawable.eraser);
		return result;
	}

	/*
	 * Opción del menú seleccionada (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		// Si se ha pulsado sobre Nivel de detalle del trazado.
		case 0:
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
			alertDialog.setTitle(R.string.borrarTodo);
			alertDialog.setMessage(R.string.confirmacionBorrarTodo);
			alertDialog.setCancelable(true);

			// Aceptar.
			alertDialog.setPositiveButton(R.string.aceptar,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							// Abro la base de datos y borro todos los
							// recorridos.
							BaseDatosGPS ayudanteBD = new BaseDatosGPS(
									RecorridosList.this);
							ayudanteBD.abre();
							boolean borrado = ayudanteBD.borrarTodo();
							ayudanteBD.cierra();

							if (borrado) {
								// Si se ha borrado correctamente actualizo la
								// lista y muestro un aviso.
								RecorridosTask async = new RecorridosTask();
								async.execute(new BaseDatosGPS(
										RecorridosList.this));

								CharSequence text = getString(R.string.datosBorrados);
								int duration = Toast.LENGTH_SHORT;
								Toast toast = Toast.makeText(
										RecorridosList.this, text, duration);
								toast.show();
							}
						}
					});

			// Cancelar.
			alertDialog.setNegativeButton(R.string.cancelar,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			alertDialog.show();

		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Muestra una lista de opciones para cada recorrido (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, final int position,
			final long id) {
		super.onListItemClick(l, v, position, id);

		// Opciones para cada recorrido.

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Opciones");
		alert.setItems(R.array.opcionesRecorrido,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case 0:
							// Ver mapa.
							Intent actividadMapa = new Intent(
									RecorridosList.this,
									TrackingMapActivity.class);

							// Documentación: If a table contains a column of
							// type
							// INTEGER
							// PRIMARY KEY, then that column becomes an alias
							// for the
							// ROWID.
							actividadMapa.putExtra("id", id);
							startActivity(actividadMapa);
							// finish();
							break;
						case 1:
							// Ver estadísticas.
							Intent actividadEstadisticas = new Intent(
									RecorridosList.this,
									EstadisticasActivity.class);
							actividadEstadisticas.putExtra("id", id);
							startActivity(actividadEstadisticas);
							break;
						case 2:
							// Cambiar nombre.
							// cambiarNombreRecorrido(id);
							Utilidades.cambiarNombreRecorrido(
									RecorridosList.this, id);
							break;
						case 3:
							// Exportar a KML
							KMLWriter exportToKML = new KMLWriter(
									RecorridosList.this);
							exportToKML.execute(id);
							break;
						case 4:
							// Borrar.
							borrarRecorrido(id);
							break;
						}
					}
				});

		// Muestro el diálogo.
		alert.show();
	}

	/**
	 * Cambia el nombre de un recorrido.
	 * 
	 * @param id
	 *            ID del recorrido.
	 */
	@Deprecated
	public void cambiarNombreRecorrido(final Long id) {

		// TODO Crear clase con métodos estáticos

		// Solicito el nuevo nombre.

		AlertDialog.Builder alert = new AlertDialog.Builder(RecorridosList.this);
		alert.setTitle("Nuevo nombre");
		alert.setCancelable(true);

		final EditText input = new EditText(RecorridosList.this);

		alert.setView(input);

		// Aceptar
		alert.setPositiveButton(R.string.aceptar,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						// Recupero el nuevo nombre.
						String nuevoNombre = input.getText().toString().trim();

						if (nuevoNombre.length() > 0) {

							// Abro la base de datos y cambio el nombre.
							BaseDatosGPS ayudanteBD = new BaseDatosGPS(
									RecorridosList.this);
							ayudanteBD.abre();
							boolean correcto = ayudanteBD
									.cambiarNombreRecorrido(id, nuevoNombre);
							ayudanteBD.cierra();

							if (correcto) {

								// Actualizo la lista y muestro aviso.
								RecorridosTask async = new RecorridosTask();
								async.execute(new BaseDatosGPS(
										RecorridosList.this));

								CharSequence text = getString(R.string.nombreCambiado);
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(
										RecorridosList.this, text, duration);
								toast.show();

							} else {

								// Aviso de error.
								CharSequence text = getString(R.string.errorCambiarNombre);
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(
										RecorridosList.this, text, duration);
								toast.show();
							}

						} else {

							// Nombre vacío.
							CharSequence text = getString(R.string.introduzcaNombre);
							int duration = Toast.LENGTH_SHORT;

							Toast toast = Toast.makeText(RecorridosList.this,
									text, duration);
							toast.show();
						}
					}

				});

		// Cancelar
		alert.setNegativeButton(R.string.cancelar,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		// Muestro el diálogo.
		alert.show();

	}

	/**
	 * Borra un recorrido.
	 * 
	 * @param id
	 *            ID del recorrido.
	 */
	private void borrarRecorrido(final Long id) {

		// Pido confirmación del usuario.
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.borrarRecorrido);
		alert.setMessage(R.string.borrarRecorrido);
		alert.setCancelable(true);

		// Aceptar
		alert.setPositiveButton(R.string.aceptar,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						// Abro la base de datos y borro el recorrido.
						BaseDatosGPS ayudanteBD = new BaseDatosGPS(
								RecorridosList.this);
						ayudanteBD.abre();
						boolean borrado = ayudanteBD.borrarRecorrido(id);
						ayudanteBD.cierra();

						if (borrado) {

							// Actualizo la lista y muestro aviso.
							RecorridosTask async = new RecorridosTask();
							async.execute(new BaseDatosGPS(RecorridosList.this));

							CharSequence text = getString(R.string.recorridoBorrado);
							int duration = Toast.LENGTH_SHORT;
							Toast toast = Toast.makeText(RecorridosList.this,
									text, duration);
							toast.show();
						}
					}
				});

		// Cancelar
		alert.setNegativeButton(R.string.cancelar,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		// Muestro el diálogo.
		alert.show();
	}

	/*
	 * AsyncTask en su lugar.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void actualizaListView() {
		BaseDatosGPS ayudanteBD = new BaseDatosGPS(RecorridosList.this);
		ayudanteBD.abre();
		String[] displayFields = new String[] { "nombreRecorrido" };
		int[] displayViews = new int[] { android.R.id.text1 };
		Cursor cursor = ayudanteBD.getRecorridos();
		adapter = new SimpleCursorAdapter(RecorridosList.this,
				android.R.layout.simple_expandable_list_item_1, cursor,
				displayFields, displayViews);
		setListAdapter(adapter);
		ayudanteBD.cierra();
	}

	/**
	 * AsyncTask para actualizar la lista de recorridos.
	 * 
	 * @author Sobremesa
	 * 
	 */
	private class RecorridosTask extends AsyncTask<BaseDatosGPS, Void, Void> {
		private BaseDatosGPS ayudanteBD;
		private Cursor cursor;

		@Override
		protected Void doInBackground(BaseDatosGPS... params) {

			// Abro la base de datos y obtengo los recorridos.
			ayudanteBD = params[0];
			ayudanteBD.abre();
			cursor = ayudanteBD.getRecorridos();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// No se puede hacer si no es en el hilo principal, por ello no se
			// llama en doInBackground.

			// Asigno el nuevo Adapter y cierro la base de datos.
			String[] displayFields = new String[] { "nombreRecorrido" };
			int[] displayViews = new int[] { android.R.id.text1 };

			SimpleCursorAdapter newAdapter = new SimpleCursorAdapter(
					RecorridosList.this,
					android.R.layout.simple_expandable_list_item_1, cursor,
					displayFields, displayViews);
			setListAdapter(newAdapter);

			ayudanteBD.cierra();

			super.onPostExecute(result);
		}

	}
}
