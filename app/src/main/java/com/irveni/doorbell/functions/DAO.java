package com.irveni.doorbell.functions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.irveni.doorbell.Models.IPCameras;

import java.util.ArrayList;
import java.util.HashMap;


public class DAO extends SQLiteOpenHelper {


    public static final String DATABASE_NAME = "IPCameras.db";
    public static final String CONTACTS_TABLE_NAME = "ipcams";
    public static final String CONTACTS_COLUMN_ID = "id";
    public static final String CONTACTS_COLUMN_TITLE = "title";
    public static final String CONTACTS_COLUMN_IP = "ip";
    public static final String CONTACTS_COLUMN_STATUS = "status";

    private HashMap hp;

    public DAO(Context context) {
        super(context, DATABASE_NAME , null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table  " +CONTACTS_TABLE_NAME+
                        "(id integer primary key AUTOINCREMENT, title text,ip text,status text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS "+CONTACTS_TABLE_NAME);
        onCreate(db);
    }

    public long insertCam (IPCameras cameras) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                "title",cameras.getTitle()
        );
        contentValues.put(
                "ip",cameras.getIp()
        );
        contentValues.put(
                "status","Connected"
        );
        long id = db.insert(CONTACTS_TABLE_NAME, null, contentValues);

        return id;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME+" where id="+id+"", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
        return numRows;
    }

    public boolean updateCam (IPCameras ipCameras) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(
                "title",ipCameras.getTitle()
        );
        contentValues.put(
                "ip",ipCameras.getIp()
        );

        db.update("userfaces", contentValues, "id = ? ", new String[] { Long.toString(ipCameras.getId()) } );
        return true;
    }

    public Integer deleteCam(Long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(CONTACTS_TABLE_NAME,
                "id = ? ",
                new String[] { Long.toString(id) });
    }
    public Integer deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(CONTACTS_TABLE_NAME,
                "1",
                null);
    }

    public ArrayList<IPCameras> getAllIpCams() {
        ArrayList<IPCameras> array_list = new ArrayList<IPCameras>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " +CONTACTS_TABLE_NAME, null );
        res.moveToFirst();

        while(res.isAfterLast() == false){

            IPCameras temp = new IPCameras(
                    res.getLong(0),
                    res.getString(1),
                    res.getString(2),
                    res.getString(3)
            );

            array_list.add(temp);
            res.moveToNext();
        }
        return array_list;
    }
}
