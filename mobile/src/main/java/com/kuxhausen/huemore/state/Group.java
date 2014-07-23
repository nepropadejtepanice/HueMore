package com.kuxhausen.huemore.state;

import android.content.Context;
import android.database.Cursor;

import com.kuxhausen.huemore.persistence.Definitions;
import com.kuxhausen.huemore.persistence.Definitions.GroupColumns;

import java.util.ArrayList;

public class Group {

  private String mName;
  private ArrayList<Long> mNetworkBulbDatabaseIds;

  public String getName() {
    return mName;
  }

  public ArrayList<Long> getNetworkBulbDatabaseIds() {
    return mNetworkBulbDatabaseIds;
  }

  public Group(ArrayList<Long> netBulbBaseIds, String name) {
    mNetworkBulbDatabaseIds = netBulbBaseIds;
    mName = name;
  }

  public static Group loadFromDatabase(String name, Context c) {

    String[] groupColumns = {GroupColumns.BULB_DATABASE_ID};
    String[] gWhereClause = {name};
    Cursor cursor =
        c.getContentResolver().query(GroupColumns.GROUPBULBS_URI, groupColumns,
                                     GroupColumns.GROUP + "=?", gWhereClause, null);

    ArrayList<Long> netBulbDbIds = new ArrayList<Long>();
    while (cursor.moveToNext()) {
      netBulbDbIds.add(cursor.getLong(0));
    }

    Group result = new Group(netBulbDbIds, name);
    return result;
  }

  public static Group loadFromLegacyData(Integer[] bulbs, String groupName, Context c) {
    ArrayList<Long> netBulbDbIds = new ArrayList<Long>();

    String[] projections = {Definitions.NetBulbColumns._ID};
    for (Integer deviceId : bulbs) {
      String[] selectionArgs =
          {"" + deviceId, "" + Definitions.NetBulbColumns.NetBulbType.PHILIPS_HUE};

      Cursor cursor =
          c.getContentResolver().query(
              Definitions.NetBulbColumns.URI,
              projections,
              Definitions.NetBulbColumns.DEVICE_ID_COLUMN + " =? AND "
              + Definitions.NetBulbColumns.TYPE_COLUMN + " =?", selectionArgs, null
          );

      if (cursor.moveToFirst()) {
        netBulbDbIds.add(cursor.getLong(0));
      }
    }

    Group result = new Group(netBulbDbIds, groupName);
    return result;
  }

  public boolean conflictsWith(Group other) {
    for (Long mBulbId : mNetworkBulbDatabaseIds) {
      for (Long oBulbId : other.mNetworkBulbDatabaseIds) {
        if (mBulbId.equals(oBulbId)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean equals(Group g) {
    if (this.mName.equals(g.getName())) {
      return true;
    }
    return false;
  }
}
