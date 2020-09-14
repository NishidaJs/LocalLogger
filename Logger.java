import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Nishida Kai on 9/9/2020.
 */
public class Logger {

    public static void insertLog(Context context, String message, String type) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss", Locale.ENGLISH);
        String time = format.format(new Date());
        LogMessageData data = new LogMessageData(year, month + 1, day, time, message, type);
        LogMessageData.Db db = new LogMessageData.Db(context);
        db.add(data);
}

    public static int fetchLog(Context context, String month, String year) {
        int counter = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss", Locale.ENGLISH);
        String time = format.format(new Date());
        String fileName = String.format("Log%s.txt", time);
        LogMessageData.Db db = new LogMessageData.Db(context);
        List<LogMessageData> list = db.getByMonth(month, year);
        File path = context.getExternalFilesDir(null);
        File file = new File(path, fileName);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            for (int x = 0; x < list.size(); x++) {
                LogMessageData data = list.get(x);
                String message = String.format("%s - (%s) %s\n", data.time, data.type, data.message);
                stream.write(message.getBytes());
                counter++;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return counter;}

    private static class LogMessageData {
    
        private static final String DATABASE_NAME = "database.db";
        private static final String TABLE_NAME = "LogMessage";

        private static final String YEAR = "year";
        private static final String MONTH = "month";
        private static final String DAY = "day";
        private static final String TIME = "time";
        private static final String MESSAGE = "message";
        private static final String TYPE = "type";

        public final String year, month, day;
        public final String time, message, type;

        private LogMessageData(int year, int month, int day, String time, String message, String type) {
            Date date = new Date();
            this.year = String.valueOf(year);
            this.month = String.valueOf(month);
            this.day = String.valueOf(day);
            this.time = time;
            this.message = message;
            this.type = type;
        }

        private LogMessageData(Cursor cursor) {
            this.year = cursor.getString(cursor.getColumnIndex(YEAR));
            this.month = cursor.getString(cursor.getColumnIndex(MONTH));
            this.day = cursor.getString(cursor.getColumnIndex(DAY));
            this.time = cursor.getString(cursor.getColumnIndex(TIME));
            this.message = cursor.getString(cursor.getColumnIndex(MESSAGE));
            this.type = cursor.getString(cursor.getColumnIndex(TYPE));
        }

        private ContentValues toCv() {
            ContentValues cv = new ContentValues();
            cv.put(YEAR, year);
            cv.put(MONTH, month);
            cv.put(DAY, day);
            cv.put(TIME, time);
            cv.put(MESSAGE, message);
            cv.put(TYPE, type);
            return cv;
        }

        private static class Db extends SQLiteOpenHelper {
            public Db(Context context) {
                super(context, DATABASE_NAME, null, 1);
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                        " _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        YEAR + " VARCHAR (32), " +
                        MONTH + " VARCHAR (32), " +
                        DAY + " VARCHAR (32), " +
                        TIME + " VARCHAR (32), " +
                        MESSAGE + " TEXT, " +
                        TYPE + " VARCHAR (32) " +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            }

            private void add(LogMessageData data) {
                SQLiteDatabase sqldb = getWritableDatabase();
                try {
                    sqldb.insert(TABLE_NAME, "_id", data.toCv());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(sqldb.isOpen()) {
                        sqldb.close();
                    }
                }
            }

            private List<LogMessageData> getByMonth(String month, String year) {
                List<LogMessageData> list = new ArrayList<>();
                SQLiteDatabase sqldb = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = sqldb.query(TABLE_NAME, null, MONTH + " = ? AND " + YEAR + " = ? ", new String[]{String.valueOf(month), String.valueOf(year)}, null, null, null, null);
                    if(cursor != null && cursor.getCount()> 0) {
                        cursor.moveToFirst();
                        do {
                            list.add(new LogMessageData(cursor));
                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(cursor != null) {
                        cursor.close();
                    }
                    if(sqldb.isOpen()) {
                        sqldb.close();
                    }
                }
                return list;
            }

            public void removeAll() {
                SQLiteDatabase sqldb = getWritableDatabase();
                sqldb.delete(TABLE_NAME, null, null);
            }

            public void removeByMonth(int month, int year) {
                SQLiteDatabase sqldb = getWritableDatabase();
                sqldb.delete(TABLE_NAME, MONTH + " = ? AND " + YEAR + " = ? ", new String[]{String.valueOf(month), String.valueOf(year)});
            }
        }
    }

}
