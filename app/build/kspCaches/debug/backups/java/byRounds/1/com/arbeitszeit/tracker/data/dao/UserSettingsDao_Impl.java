package com.arbeitszeit.tracker.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.arbeitszeit.tracker.data.entity.UserSettings;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserSettingsDao_Impl implements UserSettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserSettings> __insertionAdapterOfUserSettings;

  private final EntityDeletionOrUpdateAdapter<UserSettings> __updateAdapterOfUserSettings;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTimestamp;

  public UserSettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserSettings = new EntityInsertionAdapter<UserSettings>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_settings` (`id`,`name`,`einrichtung`,`arbeitsumfangProzent`,`wochenStundenMinuten`,`arbeitsTageProWoche`,`ferienbetreuung`,`ueberstundenVorjahrMinuten`,`letzterUebertragMinuten`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserSettings entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getEinrichtung());
        statement.bindLong(4, entity.getArbeitsumfangProzent());
        statement.bindLong(5, entity.getWochenStundenMinuten());
        statement.bindLong(6, entity.getArbeitsTageProWoche());
        final int _tmp = entity.getFerienbetreuung() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getUeberstundenVorjahrMinuten());
        statement.bindLong(9, entity.getLetzterUebertragMinuten());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfUserSettings = new EntityDeletionOrUpdateAdapter<UserSettings>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `user_settings` SET `id` = ?,`name` = ?,`einrichtung` = ?,`arbeitsumfangProzent` = ?,`wochenStundenMinuten` = ?,`arbeitsTageProWoche` = ?,`ferienbetreuung` = ?,`ueberstundenVorjahrMinuten` = ?,`letzterUebertragMinuten` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserSettings entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getEinrichtung());
        statement.bindLong(4, entity.getArbeitsumfangProzent());
        statement.bindLong(5, entity.getWochenStundenMinuten());
        statement.bindLong(6, entity.getArbeitsTageProWoche());
        final int _tmp = entity.getFerienbetreuung() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getUeberstundenVorjahrMinuten());
        statement.bindLong(9, entity.getLetzterUebertragMinuten());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateTimestamp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_settings SET updatedAt = ? WHERE id = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final UserSettings settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserSettings.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final UserSettings settings, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserSettings.handle(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTimestamp(final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTimestamp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateTimestamp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserSettings> getSettingsFlow() {
    final String _sql = "SELECT * FROM user_settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_settings"}, new Callable<UserSettings>() {
      @Override
      @Nullable
      public UserSettings call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEinrichtung = CursorUtil.getColumnIndexOrThrow(_cursor, "einrichtung");
          final int _cursorIndexOfArbeitsumfangProzent = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitsumfangProzent");
          final int _cursorIndexOfWochenStundenMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "wochenStundenMinuten");
          final int _cursorIndexOfArbeitsTageProWoche = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitsTageProWoche");
          final int _cursorIndexOfFerienbetreuung = CursorUtil.getColumnIndexOrThrow(_cursor, "ferienbetreuung");
          final int _cursorIndexOfUeberstundenVorjahrMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "ueberstundenVorjahrMinuten");
          final int _cursorIndexOfLetzterUebertragMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "letzterUebertragMinuten");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserSettings _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEinrichtung;
            _tmpEinrichtung = _cursor.getString(_cursorIndexOfEinrichtung);
            final int _tmpArbeitsumfangProzent;
            _tmpArbeitsumfangProzent = _cursor.getInt(_cursorIndexOfArbeitsumfangProzent);
            final int _tmpWochenStundenMinuten;
            _tmpWochenStundenMinuten = _cursor.getInt(_cursorIndexOfWochenStundenMinuten);
            final int _tmpArbeitsTageProWoche;
            _tmpArbeitsTageProWoche = _cursor.getInt(_cursorIndexOfArbeitsTageProWoche);
            final boolean _tmpFerienbetreuung;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFerienbetreuung);
            _tmpFerienbetreuung = _tmp != 0;
            final int _tmpUeberstundenVorjahrMinuten;
            _tmpUeberstundenVorjahrMinuten = _cursor.getInt(_cursorIndexOfUeberstundenVorjahrMinuten);
            final int _tmpLetzterUebertragMinuten;
            _tmpLetzterUebertragMinuten = _cursor.getInt(_cursorIndexOfLetzterUebertragMinuten);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserSettings(_tmpId,_tmpName,_tmpEinrichtung,_tmpArbeitsumfangProzent,_tmpWochenStundenMinuten,_tmpArbeitsTageProWoche,_tmpFerienbetreuung,_tmpUeberstundenVorjahrMinuten,_tmpLetzterUebertragMinuten,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSettings(final Continuation<? super UserSettings> $completion) {
    final String _sql = "SELECT * FROM user_settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserSettings>() {
      @Override
      @Nullable
      public UserSettings call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEinrichtung = CursorUtil.getColumnIndexOrThrow(_cursor, "einrichtung");
          final int _cursorIndexOfArbeitsumfangProzent = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitsumfangProzent");
          final int _cursorIndexOfWochenStundenMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "wochenStundenMinuten");
          final int _cursorIndexOfArbeitsTageProWoche = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitsTageProWoche");
          final int _cursorIndexOfFerienbetreuung = CursorUtil.getColumnIndexOrThrow(_cursor, "ferienbetreuung");
          final int _cursorIndexOfUeberstundenVorjahrMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "ueberstundenVorjahrMinuten");
          final int _cursorIndexOfLetzterUebertragMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "letzterUebertragMinuten");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserSettings _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEinrichtung;
            _tmpEinrichtung = _cursor.getString(_cursorIndexOfEinrichtung);
            final int _tmpArbeitsumfangProzent;
            _tmpArbeitsumfangProzent = _cursor.getInt(_cursorIndexOfArbeitsumfangProzent);
            final int _tmpWochenStundenMinuten;
            _tmpWochenStundenMinuten = _cursor.getInt(_cursorIndexOfWochenStundenMinuten);
            final int _tmpArbeitsTageProWoche;
            _tmpArbeitsTageProWoche = _cursor.getInt(_cursorIndexOfArbeitsTageProWoche);
            final boolean _tmpFerienbetreuung;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFerienbetreuung);
            _tmpFerienbetreuung = _tmp != 0;
            final int _tmpUeberstundenVorjahrMinuten;
            _tmpUeberstundenVorjahrMinuten = _cursor.getInt(_cursorIndexOfUeberstundenVorjahrMinuten);
            final int _tmpLetzterUebertragMinuten;
            _tmpLetzterUebertragMinuten = _cursor.getInt(_cursorIndexOfLetzterUebertragMinuten);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserSettings(_tmpId,_tmpName,_tmpEinrichtung,_tmpArbeitsumfangProzent,_tmpWochenStundenMinuten,_tmpArbeitsTageProWoche,_tmpFerienbetreuung,_tmpUeberstundenVorjahrMinuten,_tmpLetzterUebertragMinuten,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
