package com.master.android.proyecto;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

public class Utilidades {

	public static final int CORRECTO = 0;
	public static final int ERROR = 1;
	public static final int NOMBRE_VACIO = 2;
	/**
	 * Caracteres reservados.
	 */
	public static final String[] RESERVED_CHARS = { "|", "\\", "?", "*", "<",
			"\"", ":", ">", "/", "+", "[", "]" };

	public static int RESULTADO;

	public static void cambiarNombreRecorrido(final Context context,
			final Long id) {

		// Solicito el nuevo nombre.

		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle("Nuevo nombre");
		alert.setCancelable(true);

		final EditText input = new EditText(context);

		alert.setView(input);

		// Aceptar
		alert.setPositiveButton(R.string.aceptar,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						// Recupero el nuevo nombre.
						String nuevoNombre = input.getText().toString().trim();

						if (nuevoNombre.length() > 0
								&& !containsReservedChars(nuevoNombre)) {

							// Abro la base de datos y cambio el nombre.
							BaseDatosGPS ayudanteBD = new BaseDatosGPS(context);
							ayudanteBD.abre();
							boolean correcto = ayudanteBD
									.cambiarNombreRecorrido(id, nuevoNombre);
							ayudanteBD.cierra();

							if (correcto) {

								// Actualizo la lista y muestro aviso.
								/*
								 * RecorridosTask async = new RecorridosTask();
								 * async.execute(new BaseDatosGPS( context));
								 */

								CharSequence text = "Nombre cambiado correctamente";
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(context, text,
										duration);
								toast.show();

								RESULTADO = CORRECTO;

							} else {

								// Aviso de error.
								CharSequence text = "Error al cambiar el nombre";
								int duration = Toast.LENGTH_SHORT;

								Toast toast = Toast.makeText(context, text,
										duration);
								toast.show();
							}

						} else {

							// Nombre vacío.
							AlertDialog.Builder alertDialog = new AlertDialog.Builder(
									context);
							alertDialog.setTitle("Error en el nombre");
							alertDialog
									.setMessage("El nombre no puede ser vacío ni puede contener ninguno de estos caracteres: "
											+ Utilidades.RESERVED_CHARS
													.toString());
							alertDialog.setCancelable(true);

							// Aceptar.
							alertDialog.setPositiveButton(R.string.aceptar,
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											Utilidades.cambiarNombreRecorrido(
													context, id);
										}
									});

							// Cancelar
							alertDialog.setNegativeButton(R.string.cancelar,
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
							alertDialog.show();
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
	 * Comprueba que el nombre del recorrido no contenga caracteres inválidos
	 * para exportar.
	 * 
	 * @param s
	 *            Nombre del recorrido.
	 * @return true si contiene caracteres reservados, false en caso contrario.
	 */
	public static boolean containsReservedChars(String s) {

		boolean contains = false;

		for (String reserved : RESERVED_CHARS) {
			if (!contains && s.contains(reserved)) {
				contains = true;
			}
		}
		return contains;
	}

}
