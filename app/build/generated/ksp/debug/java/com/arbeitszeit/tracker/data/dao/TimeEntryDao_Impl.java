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
import com.arbeitszeit.tracker.data.entity.TimeEntry;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TimeEntryDao_Impl implements TimeEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TimeEntry> __insertionAdapterOfTimeEntry;

  private final EntityDeletionOrUpdateAdapter<TimeEntry> __deletionAdapterOfTimeEntry;

  private final EntityDeletionOrUpdateAdapter<TimeEntry> __updateAdapterOfTimeEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDate;

  public TimeEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTimeEntry = new EntityInsertionAdapter<TimeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `time_entries` (`id`,`datum`,`wochentag`,`kalenderwoche`,`jahr`,`startZeit`,`endZeit`,`pauseMinuten`,`sollMinuten`,`typ`,`notiz`,`arbeitszeitBereitschaft`,`isManualEntry`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TimeEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDatum());
        statement.bindString(3, entity.getWochentag());
        statement.bindLong(4, entity.getKalenderwoche());
        statement.bindLong(5, entity.getJahr());
        if (entity.getStartZeit() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getStartZeit());
        }
        if (entity.getEndZeit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getEndZeit());
        }
        statement.bindLong(8, entity.getPauseMinuten());
        statement.bindLong(9, entity.getSollMinuten());
        statement.bindString(10, entity.getTyp());
        statement.bindString(11, entity.getNotiz());
        statement.bindLong(12, entity.getArbeitszeitBereitschaft());
        final int _tmp = entity.isManualEntry() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getCreatedAt());
        statement.bindLong(15, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfTimeEntry = new EntityDeletionOrUpdateAdapter<TimeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `time_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TimeEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTimeEntry = new EntityDeletionOrUpdateAdapter<TimeEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `time_entries` SET `id` = ?,`datum` = ?,`wochentag` = ?,`kalenderwoche` = ?,`jahr` = ?,`startZeit` = ?,`endZeit` = ?,`pauseMinuten` = ?,`sollMinuten` = ?,`typ` = ?,`notiz` = ?,`arbeitszeitBereitschaft` = ?,`isManualEntry` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TimeEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDatum());
        statement.bindString(3, entity.getWochentag());
        statement.bindLong(4, entity.getKalenderwoche());
        statement.bindLong(5, entity.getJahr());
        if (entity.getStartZeit() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getStartZeit());
        }
        if (entity.getEndZeit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getEndZeit());
        }
        statement.bindLong(8, entity.getPauseMinuten());
        statement.bindLong(9, entity.getSollMinuten());
        statement.bindString(10, entity.getTyp());
        statement.bindString(11, entity.getNotiz());
        statement.bindLong(12, entity.getArbeitszeitBereitschaft());
        final int _tmp = entity.isManualEntry() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getCreatedAt());
        statement.bindLong(15, entity.getUpdatedAt());
        statement.bindLong(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteByDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM time_entries WHERE datum = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TimeEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTimeEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final TimeEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTimeEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final TimeEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTimeEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDate(final String date, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDate.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, date);
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
          __preparedStmtOfDeleteByDate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getEntryByDate(final String date,
      final Continuation<? super TimeEntry> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE datum = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TimeEntry>() {
      @Override
      @Nullable
      public TimeEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final TimeEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
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

  @Override
  public Flow<TimeEntry> getEntryByDateFlow(final String date) {
    final String _sql = "SELECT * FROM time_entries WHERE datum = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"time_entries"}, new Callable<TimeEntry>() {
      @Override
      @Nullable
      public TimeEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final TimeEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getEntriesByYear(final int year,
      final Continuation<? super List<TimeEntry>> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE jahr = ? ORDER BY datum ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEntriesByWeek(final int year, final int kw,
      final Continuation<? super List<TimeEntry>> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE jahr = ? AND kalenderwoche = ? ORDER BY datum ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    _argIndex = 2;
    _statement.bindLong(_argIndex, kw);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEntriesByWeekRange(final int year, final int startKW, final int endKW,
      final Continuation<? super List<TimeEntry>> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE jahr = ? AND kalenderwoche BETWEEN ? AND ? ORDER BY datum ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startKW);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endKW);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEntriesByDateRange(final String startDate, final String endDate,
      final Continuation<? super List<TimeEntry>> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE datum BETWEEN ? AND ? ORDER BY datum ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TimeEntry>> getEntriesByDateRangeFlow(final String startDate,
      final String endDate) {
    final String _sql = "SELECT * FROM time_entries WHERE datum BETWEEN ? AND ? ORDER BY datum ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"time_entries"}, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
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
  public Flow<List<TimeEntry>> getWeekEntriesFlow(final int year, final int kw) {
    final String _sql = "SELECT * FROM time_entries WHERE jahr = ? AND kalenderwoche = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    _argIndex = 2;
    _statement.bindLong(_argIndex, kw);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"time_entries"}, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
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
  public Object getIncompleteEntries(final String date,
      final Continuation<? super List<TimeEntry>> $completion) {
    final String _sql = "SELECT * FROM time_entries WHERE startZeit IS NULL AND endZeit IS NULL AND typ = 'NORMAL' AND datum <= ? ORDER BY datum DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TimeEntry>>() {
      @Override
      @NonNull
      public List<TimeEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDatum = CursorUtil.getColumnIndexOrThrow(_cursor, "datum");
          final int _cursorIndexOfWochentag = CursorUtil.getColumnIndexOrThrow(_cursor, "wochentag");
          final int _cursorIndexOfKalenderwoche = CursorUtil.getColumnIndexOrThrow(_cursor, "kalenderwoche");
          final int _cursorIndexOfJahr = CursorUtil.getColumnIndexOrThrow(_cursor, "jahr");
          final int _cursorIndexOfStartZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "startZeit");
          final int _cursorIndexOfEndZeit = CursorUtil.getColumnIndexOrThrow(_cursor, "endZeit");
          final int _cursorIndexOfPauseMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "pauseMinuten");
          final int _cursorIndexOfSollMinuten = CursorUtil.getColumnIndexOrThrow(_cursor, "sollMinuten");
          final int _cursorIndexOfTyp = CursorUtil.getColumnIndexOrThrow(_cursor, "typ");
          final int _cursorIndexOfNotiz = CursorUtil.getColumnIndexOrThrow(_cursor, "notiz");
          final int _cursorIndexOfArbeitszeitBereitschaft = CursorUtil.getColumnIndexOrThrow(_cursor, "arbeitszeitBereitschaft");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<TimeEntry> _result = new ArrayList<TimeEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TimeEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDatum;
            _tmpDatum = _cursor.getString(_cursorIndexOfDatum);
            final String _tmpWochentag;
            _tmpWochentag = _cursor.getString(_cursorIndexOfWochentag);
            final int _tmpKalenderwoche;
            _tmpKalenderwoche = _cursor.getInt(_cursorIndexOfKalenderwoche);
            final int _tmpJahr;
            _tmpJahr = _cursor.getInt(_cursorIndexOfJahr);
            final Integer _tmpStartZeit;
            if (_cursor.isNull(_cursorIndexOfStartZeit)) {
              _tmpStartZeit = null;
            } else {
              _tmpStartZeit = _cursor.getInt(_cursorIndexOfStartZeit);
            }
            final Integer _tmpEndZeit;
            if (_cursor.isNull(_cursorIndexOfEndZeit)) {
              _tmpEndZeit = null;
            } else {
              _tmpEndZeit = _cursor.getInt(_cursorIndexOfEndZeit);
            }
            final int _tmpPauseMinuten;
            _tmpPauseMinuten = _cursor.getInt(_cursorIndexOfPauseMinuten);
            final int _tmpSollMinuten;
            _tmpSollMinuten = _cursor.getInt(_cursorIndexOfSollMinuten);
            final String _tmpTyp;
            _tmpTyp = _cursor.getString(_cursorIndexOfTyp);
            final String _tmpNotiz;
            _tmpNotiz = _cursor.getString(_cursorIndexOfNotiz);
            final int _tmpArbeitszeitBereitschaft;
            _tmpArbeitszeitBereitschaft = _cursor.getInt(_cursorIndexOfArbeitszeitBereitschaft);
            final boolean _tmpIsManualEntry;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new TimeEntry(_tmpId,_tmpDatum,_tmpWochentag,_tmpKalenderwoche,_tmpJahr,_tmpStartZeit,_tmpEndZeit,_tmpPauseMinuten,_tmpSollMinuten,_tmpTyp,_tmpNotiz,_tmpArbeitszeitBereitschaft,_tmpIsManualEntry,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getEntryCountByYear(final int year,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM time_entries WHERE jahr = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
