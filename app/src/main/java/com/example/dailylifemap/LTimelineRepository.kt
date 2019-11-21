package com.example.dailylifemap

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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

@Dao
interface LDao{
    /*
    loadAll query can make the problem with freezing by data flooding
     */
    @Query("SELECT * FROM ${LTimelineDB.L_ENTITYTABLE}")
    fun loadAll(): List<LEntity>
    
    /*@Query("SELECT * FROM ${LTimelineDB.L_ENTITYTABLE}" +
            "WHERE ${LTimelineDB.L_TIME} BETWEEN :timeMillisFrom AND :timeMillisTo")
    fun loadAllThePeriod(timeMillisFrom: Long, timeMillisTo: Long): List<LEntity>*/

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

class LTimelineRepository(val appContext: Context) {

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
    }

    fun locationInsert(location: Location){
        val entity = LEntity(
            accuracy = location.accuracy
            , altitude = location.altitude
            , bearing = location.bearing
            , bearingAccuracyDegrees =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.bearingAccuracyDegrees
            else 0.0f
            , latitude = location.latitude
            , longitude = location.longitude
            , provider = location.provider
            , speed = location.speed
            , timeMillis = location.time
            , verticalAccuracyMeters =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) location.verticalAccuracyMeters
            else 0.0f
            )

        if(isUsable)
            locationDao.insertLocation(entity)
    }

    fun locationSelect(){
        if(isUsable) {
            val locations = locationDao.loadAll()

            locations.forEachIndexed { index, location ->
                Log.d("Loaded Location$index", "${location.latitude}, ${location.longitude}")
            }
        }
    }
}

class LTimelineViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var timelineRepository: LTimelineRepository

    init{
        timelineRepository = LTimelineRepository(application)
    }

    fun insertLocation(location: Location){
        timelineRepository.locationInsert(location)
    }

    fun selectLocation(){
        timelineRepository.locationSelect()
    }
}