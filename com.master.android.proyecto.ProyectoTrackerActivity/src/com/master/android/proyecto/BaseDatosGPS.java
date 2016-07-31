package com.master.android.proyecto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BaseDatosGPS {

	protected static final int DATABASE_VERSION = 3;

	protected static final String nombreBD = "gpsdata.db";

	protected static final String TABLA_COORDENADAS = "gpspoints";
	protected static final String colCoordenadasID = "idCoordenada";
	protected static final String colLatitud = "latitud";
	protected static final String colLongitud = "longitud";
	protected static final String colAltura = "altura";

	protected static final String colHora = "hora";
	protected static final String colRecorrido = "recorrido";

	protected static final String TABLA_RECORRIDOS = "TRecorridos";
	protected static final String colRecorridoID = "idRecorrido";
	protected static final String colNombreRecorrido = "nombreRecorrido";
	protected static final String vistaCoordenadas = "vistaCoordenadas";

	// INTEGER PRIMARY KEY hace que se autoincremente
	private static String sqlCreateRecorridos = "CREATE TABLE "
			+ TABLA_RECORRIDOS + " (" + colRecorridoID
			+ " INTEGER PRIMARY KEY, " + colNombreRecorrido + " TEXT)";

	private static String sqlCreateCoordenadas = "CREATE TABLE "
			+ TABLA_COORDENADAS + " (" + colCoordenadasID
			+ " INTEGER PRIMARY KEY," + colLatitud + " REAL," + colLongitud
			+ " REAL," + colAltura + " REAL," + colHora + " INTEGER,"
			+ colRecorrido + " INTEGER, FOREIGN KEY(" + colRecorrido
			+ ") REFERENCES " + TABLA_RECORRIDOS + "( " + colRecorridoID
			+ ") ON DELETE CASCADE);";

	private static String sqlTrigger = "CREATE TRIGGER fk_coordID_recoID "
			+ " BEFORE INSERT " + " ON " + TABLA_COORDENADAS
			+ " FOR EACH ROW BEGIN" + " SELECT CASE WHEN  ((SELECT "
			+ colRecorridoID + " FROM " + TABLA_RECORRIDOS + " WHERE "
			+ colRecorridoID + "=new." + colRecorrido + " ) IS NULL)"
			+ " THEN RAISE (ABORT,'Foreign Key Violation') END;" + "  END;";

	private static String createView = ("CREATE VIEW " + vistaCoordenadas
			+ " AS SELECT " + TABLA_COORDENADAS + "." + colCoordenadasID
			+ " AS _id," + " " + TABLA_COORDENADAS + "." + colLatitud + ","
			+ " " + TABLA_COORDENADAS + "." + colLongitud + "," + " "
			+ TABLA_RECORRIDOS + "." + colNombreRecorrido + "" + " FROM "
			+ TABLA_COORDENADAS + " JOIN " + TABLA_RECORRIDOS + " ON "
			+ TABLA_COORDENADAS + "." + colRecorrido + " =" + TABLA_RECORRIDOS
			+ "." + colRecorridoID);

	/**
	 * Variable que guarda el objeto que accede a la base de datos.
	 */
	private DataBaseHelper mAyudanteBD;

	/**
	 * Variable que guarda la base de datos en sí.
	 */
	private SQLiteDatabase mBd;

	/**
	 * Contexto donde se usará la base de datos.
	 */
	private final Context mContexto;

	/**
	 * Clase interna para crear un ayudante para la base de datos.
	 * 
	 * @author Sobremesa
	 * 
	 */
	private static class DataBaseHelper extends SQLiteOpenHelper {
		public DataBaseHelper(Context context) {
			super(context, nombreBD, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(sqlCreateRecorridos);
			db.execSQL(sqlCreateCoordenadas);
			db.execSQL(sqlTrigger);
			db.execSQL(createView);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLA_COORDENADAS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLA_RECORRIDOS);
			db.execSQL("DROP TRIGGER IF EXISTS fk_coordID_recoID");
			db.execSQL("DROP VIEW IF EXISTS " + vistaCoordenadas);
			onCreate(db);
		}
	}

	/**
	 * Constructor de la clase.
	 * 
	 * @param context
	 *            contexto en el que se usará.
	 */
	public BaseDatosGPS(Context context) {
		mContexto = context;
	}

	/**
	 * Método que se usa para abrir el acceso a la base de datos.
	 * 
	 * @throws SQLException
	 *             Si ha habido algún error al abrir la base de datos.
	 * 
	 * @return base de datos.
	 */
	public BaseDatosGPS abre() throws SQLException {
		// Creamos el ayudante de acceso para la base de datos.
		mAyudanteBD = new DataBaseHelper(mContexto);
		// Creamos la variable para la base de datos.
		mBd = mAyudanteBD.getWritableDatabase();
		mBd.execSQL("PRAGMA foreign_keys=ON;");
		return this;
	}

	/**
	 * Método que cierra la base de datos.
	 * 
	 */
	public void cierra() {

		mAyudanteBD.close();
	}

	/**
	 * Crea un nuevo recorrido con el nombre dado.
	 * 
	 * @param nombre
	 *            nombre del recorrido
	 * @return id del recorrido en la BD.
	 */
	public long nuevoRecorrido(String nombre) {

		ContentValues valoresIniciales = new ContentValues();
		valoresIniciales.put(colNombreRecorrido, nombre);

		return mBd.insert(TABLA_RECORRIDOS, null, valoresIniciales);
	}

	/**
	 * Crea un nuevo punto en la base de datos.
	 * 
	 * @param idRecorrido
	 *            id del recorrido al que pertenece.
	 * @param latitud
	 *            latitud del punto
	 * @param longitud
	 *            longitud del punto
	 * @param altura
	 *            altura del punto
	 * @param hora
	 *            hora a la que fue registrado
	 * @return true si se insertó y false en caso contrario.
	 */
	public boolean nuevoPunto(Integer idRecorrido, Double latitud,
			Double longitud, Double altura, long hora) {

		ContentValues valoresIniciales = new ContentValues();
		valoresIniciales.put(colRecorrido, idRecorrido);
		valoresIniciales.put(colLatitud, latitud);
		valoresIniciales.put(colLongitud, longitud);
		valoresIniciales.put(colAltura, altura);
		valoresIniciales.put(colHora, hora);
		System.out.println("DATO INSERTADO: " + idRecorrido + ", " + latitud
				+ " - " + longitud);
		return mBd.insert(TABLA_COORDENADAS, null, valoresIniciales) > 0;
	}

	/**
	 * Devuelve todas las coordenadas de un recorrido según un filtro.
	 * 
	 * @param idRecorrido
	 *            id del recorrido
	 * @param select
	 *            filtro
	 * @return Cursor con todas las coordenadas.
	 */
	public Cursor getCoordenadas(Integer idRecorrido, String select) {
		return mBd.query(TABLA_COORDENADAS, new String[] { colCoordenadasID,
				colLatitud, colLongitud, colAltura, colHora }, select + " AND "
				+ colRecorrido + " = " + idRecorrido, null, null, null, null);
	}

	/**
	 * Devuelve el nombre del recorrido con id suministrado.
	 * 
	 * @param idRecorrido
	 *            id del recorrido
	 * @return Cursor con el nombre del recorrido.
	 */
	public Cursor getNombreRecorrido(Integer idRecorrido) {
		return mBd.query(TABLA_RECORRIDOS, new String[] { colNombreRecorrido },
				colRecorridoID + "=" + idRecorrido, null, null, null, null);
	}

	/**
	 * Devuelve todos los recorridos.
	 * 
	 * @return Cursor con todos los recorridos.
	 */
	public Cursor getRecorridos() {
		// Para obtener el _id que pide el SimpleCursorAdapter
		return mBd.rawQuery("SELECT rowid as _id, " + colNombreRecorrido + ", "
				+ colRecorridoID + " from " + TABLA_RECORRIDOS, null);
	}

	/**
	 * Borra todos los datos de la BD.
	 * 
	 * @return true si se borraron datos, false en caso contrario.
	 */
	public boolean borrarTodo() {
		int filasBorradas = 0;
		filasBorradas += mBd.delete(TABLA_COORDENADAS, "1", null);
		filasBorradas += mBd.delete(TABLA_RECORRIDOS, "1", null);
		return filasBorradas > 0;
	}

	/**
	 * Cambia el nombre de un recorrido.
	 * 
	 * @param idRecorrido
	 *            id del recorrido
	 * @param nuevoNombre
	 *            nuevo nombre
	 * @return true si se cambió el nombre, false si no se hizo.
	 */
	public boolean cambiarNombreRecorrido(Long idRecorrido, String nuevoNombre) {
		ContentValues values = new ContentValues();
		values.put(colNombreRecorrido, nuevoNombre);
		return mBd.update(TABLA_RECORRIDOS, values, colRecorridoID + "="
				+ idRecorrido, null) > 0;
	}

	/**
	 * Borra un recorrido.
	 * 
	 * @param idRecorrido
	 *            id del recorrido a borrar.
	 * @return true si se borró el recorrido, false en caso contrario.
	 */
	public boolean borrarRecorrido(Long idRecorrido) {
		return mBd.delete(TABLA_RECORRIDOS, colRecorridoID + "=" + idRecorrido,
				null) > 0;
	}

}
