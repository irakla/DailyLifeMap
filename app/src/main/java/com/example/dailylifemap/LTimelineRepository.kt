package com.example.dailylifemap

import android.content.Context
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import androidx.room.*
import org.jetbrains.anko.toast

@Entity(tableName = LTimelineDB.L_ENTITYTABLE)
data class LEntity(
    @PrimaryKey @ColumnInfo(name = LTimelineDB.L_TIME) val timeMillis: Long
    , @ColumnInfo(name = LTimelineDB.L_ACCURACY) val accuracy: Float
    , @ColumnInfo(name = LTimelineDB.L_ALTITUDE) val altitude: Double
    , @ColumnInfo(name = LTimelineDB.L_BEARING) val bearing: Float
    , @ColumnInfo(name = LTimelineDB.L_BEARINGACCURACY) val bearingAccuracyDegrees: Float
    , @ColumnInfo(name = LTimelineDB.L_LATITUDE) val latitude: Double
    , @ColumnInfo(name = LTimelineDB.L_LONGITUDE) val longitude: Double
    , @ColumnInfo(name = LTimelineDB.L_PROVIDER) val provider: String
    , @ColumnInfo(name = LTimelineDB.L_SPEED) val speed: Float
    , @ColumnInfo(name = LTimelineDB.L_VERTICALACCURACY) val verticalAccuracyMeters: Float
)

fun toLEntityFromLocation(location: Location) : LEntity{
    return LEntity(
        timeMillis = location.time
        , accuracy = location.accuracy
        , altitude = location.altitude
        , bearing = location.bearing
        , bearingAccuracyDegrees = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.bearingAccuracyDegrees
        else 0.0f
        , latitude = location.latitude
        , longitude = location.longitude
        , provider = location.provider
        , speed = location.speed
        , verticalAccuracyMeters =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.verticalAccuracyMeters
        else 0.0f
    )
}

fun toLocationFromLEntity(entity: LEntity) : Location{
    return Location(entity.provider).apply{
        time = entity.timeMillis
        accuracy = entity.accuracy
        altitude = entity.altitude
        bearing = entity.bearing
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            bearingAccuracyDegrees = entity.bearingAccuracyDegrees
        latitude = entity.latitude
        longitude = entity.longitude
        speed = entity.speed
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            verticalAccuracyMeters = entity.verticalAccuracyMeters
    }
}

@Dao
interface LDao{
    /*
    loadAll query can make the problem with freezing by data flooding
     */
    @Query("SELECT * FROM ${LTimelineDB.L_ENTITYTABLE}")
    fun loadAll(): List<LEntity>
    
    @Query("SELECT * FROM ${LTimelineDB.L_ENTITYTABLE}" +
            "WHERE ${LTimelineDB.L_TIME} BETWEEN :timeMillisFrom AND :timeMillisTo")
    fun loadAllThePeriod(timeMillisFrom: Long, timeMillisTo: Long): List<LEntity>

    @Query("SELECT * FROM ${LTimelineDB.L_ENTITYTABLE}" +
            " WHERE ${LTimelineDB.L_TIME} =" +
            " (SELECT MAX(${LTimelineDB.L_TIME}) FROM ${LTimelineDB.L_ENTITYTABLE}) LIMIT 1")
    fun loadLatest(): LEntity

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertLocation(location: LEntity)

    //@Delete
    //fun deleteLocations(vararg locations: Location)
    
    //@Delete
    //fun deleteToTheTime(theTimeMillis: Long)
}

@Database(entities = [LEntity::class], version = LTimelineDB.LDB_VER)
abstract class LTimelineDB : RoomDatabase() {
    abstract fun locationDao() : LDao

    //set static features for DB
    companion object{
        const val LDB_VER = 1
        const val LDB_NAME = "DailyLifeMapTimeline.db"

        const val L_ENTITYTABLE = "LTimeline"
        const val L_ACCURACY = "Accuracy"
        const val L_ALTITUDE = "Altitude"
        const val L_BEARING = "Bearing"
        const val L_BEARINGACCURACY = "BearingAccuracyDegrees"
        const val L_LATITUDE = "Latitude"
        const val L_LONGITUDE = "Longitude"
        const val L_PROVIDER = "Provider"
        const val L_SPEED = "Speed"
        const val L_TIME = "TimeMillis"
        const val L_VERTICALACCURACY = "VerticalAccurracyMeters"

        private var INSTANCE: LTimelineDB? = null

        fun getInstance(context: Context): LTimelineDB?{
            synchronized(LTimelineDB::class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext, LTimelineDB::class.java, LDB_NAME
                    ).build()
                }
            }

            return INSTANCE
        }
    }
}

class LTimelineRepository(private val appContext: Context) {

    private var locationDao: LDao
    private var isUsable = true

    init{
        val database = LTimelineDB.getInstance(appContext)!!
        locationDao = database?.locationDao()

        if(database == null) {
            appContext.toast("타임라인을 불러올 수 없습니다.")
            Log.d("LTimelineRepository", "Database is null!")
            isUsable = false
        }

        if(isUsable){
            SelectLatestAsyncTask(locationDao){
                timeLatestLocation = it.timeMillis
            }.execute()
        }
    }

    companion object{
        private class InsertAsyncTask(private val locationDao: LDao)
            : AsyncTask<LEntity, Void, Void>()
        {
            override fun doInBackground(vararg locationEntities: LEntity): Void? {
                locationDao.insertLocation(locationEntities[0])
                return null
            }
        }

        private class SelectAllAsyncTask(
            private val locationDao: LDao
            , private val doingWithTimeline: (List<LEntity>) -> Unit)
            : AsyncTask<Void, Void, List<LEntity>?>()
        {
            override fun doInBackground(vararg p0: Void): List<LEntity>? {
                val timeline = locationDao.loadAll()
                timeline?.let{ doingWithTimeline(it) }
                return timeline
            }
        }

        private class SelectLatestAsyncTask(
            private val locationDao: LDao
            , private val doingWithTimeline: (LEntity) -> Unit)
            : AsyncTask<Void, Void, LEntity?>()
        {
            override fun doInBackground(vararg p0: Void): LEntity? {
                val latestLocation = locationDao.loadLatest()
                if(latestLocation != null)
                    doingWithTimeline(latestLocation)
                return latestLocation
            }
        }

        private var timeLatestLocation: Long = 0
    }

    fun locationInsert(location: Location){
        val entity = toLEntityFromLocation(location)

        if(isUsable && location.time > timeLatestLocation) {
            InsertAsyncTask(locationDao).execute(entity)
            timeLatestLocation = location.time
        }
        else if(isUsable)
            Log.d("Invalid Insertion", "tried to duplicate Ltime insertion")
    }

    fun locationSelect(doingWithTimeline: (List<Location>) -> Unit){
        if(isUsable){
            SelectAllAsyncTask(locationDao){ lEntities ->
                doingWithTimeline(lEntities.map{ toLocationFromLEntity(it) })
            }.execute()
        }
    }
}