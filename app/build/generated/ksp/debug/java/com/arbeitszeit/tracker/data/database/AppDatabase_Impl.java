package com.arbeitszeit.tracker.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.arbeitszeit.tracker.data.dao.TimeEntryDao;
import com.arbeitszeit.tracker.data.dao.TimeEntryDao_Impl;
import com.arbeitszeit.tracker.data.dao.UserSettingsDao;
import com.arbeitszeit.tracker.data.dao.UserSettingsDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile UserSettingsDao _userSettingsDao;

  private volatile TimeEntryDao _timeEntryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `einrichtung` TEXT NOT NULL, `arbeitsumfangProzent` INTEGER NOT NULL, `wochenStundenMinuten` INTEGER NOT NULL, `arbeitsTageProWoche` INTEGER NOT NULL, `ferienbetreuung` INTEGER NOT NULL, `ueberstundenVorjahrMinuten` INTEGER NOT NULL, `letzterUebertragMinuten` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `time_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `datum` TEXT NOT NULL, `wochentag` TEXT NOT NULL, `kalenderwoche` INTEGER NOT NULL, `jahr` INTEGER NOT NULL, `startZeit` INTEGER, `endZeit` INTEGER, `pauseMinuten` INTEGER NOT NULL, `sollMinuten` INTEGER NOT NULL, `typ` TEXT NOT NULL, `notiz` TEXT NOT NULL, `arbeitszeitBereitschaft` INTEGER NOT NULL, `isManualEntry` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e9a7d285b4a2cb701838f42185ec48df')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_settings`");
        db.execSQL("DROP TABLE IF EXISTS `time_entries`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserSettings = new HashMap<String, TableInfo.Column>(11);
        _columnsUserSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("einrichtung", new TableInfo.Column("einrichtung", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("arbeitsumfangProzent", new TableInfo.Column("arbeitsumfangProzent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("wochenStundenMinuten", new TableInfo.Column("wochenStundenMinuten", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("arbeitsTageProWoche", new TableInfo.Column("arbeitsTageProWoche", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("ferienbetreuung", new TableInfo.Column("ferienbetreuung", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("ueberstundenVorjahrMinuten", new TableInfo.Column("ueberstundenVorjahrMinuten", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("letzterUebertragMinuten", new TableInfo.Column("letzterUebertragMinuten", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSettings.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserSettings = new TableInfo("user_settings", _columnsUserSettings, _foreignKeysUserSettings, _indicesUserSettings);
        final TableInfo _existingUserSettings = TableInfo.read(db, "user_settings");
        if (!_infoUserSettings.equals(_existingUserSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "user_settings(com.arbeitszeit.tracker.data.entity.UserSettings).\n"
                  + " Expected:\n" + _infoUserSettings + "\n"
                  + " Found:\n" + _existingUserSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsTimeEntries = new HashMap<String, TableInfo.Column>(15);
        _columnsTimeEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("datum", new TableInfo.Column("datum", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("wochentag", new TableInfo.Column("wochentag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("kalenderwoche", new TableInfo.Column("kalenderwoche", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("jahr", new TableInfo.Column("jahr", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("startZeit", new TableInfo.Column("startZeit", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("endZeit", new TableInfo.Column("endZeit", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("pauseMinuten", new TableInfo.Column("pauseMinuten", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("sollMinuten", new TableInfo.Column("sollMinuten", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("typ", new TableInfo.Column("typ", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("notiz", new TableInfo.Column("notiz", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("arbeitszeitBereitschaft", new TableInfo.Column("arbeitszeitBereitschaft", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("isManualEntry", new TableInfo.Column("isManualEntry", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTimeEntries.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTimeEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTimeEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTimeEntries = new TableInfo("time_entries", _columnsTimeEntries, _foreignKeysTimeEntries, _indicesTimeEntries);
        final TableInfo _existingTimeEntries = TableInfo.read(db, "time_entries");
        if (!_infoTimeEntries.equals(_existingTimeEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "time_entries(com.arbeitszeit.tracker.data.entity.TimeEntry).\n"
                  + " Expected:\n" + _infoTimeEntries + "\n"
                  + " Found:\n" + _existingTimeEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e9a7d285b4a2cb701838f42185ec48df", "ee5503fa83310e66517d62cec6cdd62f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_settings","time_entries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `user_settings`");
      _db.execSQL("DELETE FROM `time_entries`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserSettingsDao.class, UserSettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TimeEntryDao.class, TimeEntryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserSettingsDao userSettingsDao() {
    if (_userSettingsDao != null) {
      return _userSettingsDao;
    } else {
      synchronized(this) {
        if(_userSettingsDao == null) {
          _userSettingsDao = new UserSettingsDao_Impl(this);
        }
        return _userSettingsDao;
      }
    }
  }

  @Override
  public TimeEntryDao timeEntryDao() {
    if (_timeEntryDao != null) {
      return _timeEntryDao;
    } else {
      synchronized(this) {
        if(_timeEntryDao == null) {
          _timeEntryDao = new TimeEntryDao_Impl(this);
        }
        return _timeEntryDao;
      }
    }
  }
}
