package com.showreal.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.showreal.app.data.model.DeviceReel;
import com.showreal.app.data.model.Message;
import com.showreal.app.data.model.MessageNotification;
import com.showreal.app.data.model.Notification;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Reel;
import com.showreal.app.data.model.Video;

import nl.qbusict.cupboard.CupboardBuilder;
import nl.qbusict.cupboard.CupboardFactory;
import nl.qbusict.cupboard.DatabaseCompartment;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class DatabaseHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "RxCupboard.db";
    private static final int DATABASE_VERSION = 12;

    private static SQLiteDatabase database;

    static final Class[] classes = new Class[]{
            Message.class,
            Question.class,
            Reel.class,
            Video.class,
            DeviceReel.class,
            MessageNotification.class,
            Notification.class
    };

    static {
        CupboardFactory.setCupboard(new CupboardBuilder().useAnnotations().build());

        // Register our models with Cupboard as usual
        for (Class cls : classes) {
            cupboard().register(cls);
        }
    }

    public synchronized static SQLiteDatabase getConnection(Context context) {
        if (database == null || !database.isOpen()) {
            // Construct the single helper and open the unique(!) db connection for the app
            database = new DatabaseHelper(context.getApplicationContext()).getWritableDatabase();
        }
        return database;
    }

    public static void deleteAll(Context context) {
        SQLiteDatabase database = getConnection(context);
        DatabaseCompartment compartment = cupboard().withDatabase(database);

        for (Class cls : classes) {
            if (cls == Video.class) {
                compartment.delete(cls, "dummyVideo = ?", String.valueOf(0));
            } else if (cls == DeviceReel.class) {
                compartment.delete(cls, "dummyReel = ?", String.valueOf(0));
            } else if (cls == Question.class) {
                compartment.delete(cls, "dummyQuestion = ?", String.valueOf(0));
            } else {
                compartment.delete(cls, null);
            }
        }

        database.close();
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        cupboard().withDatabase(db).createTables();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        cupboard().withDatabase(db).upgradeTables();
    }

}