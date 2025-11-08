package com.nottingham.mynottingham.data.local.database.dao

import androidx.room.*
import com.nottingham.mynottingham.data.local.database.entities.ErrandEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Errand operations
 */
@Dao
interface ErrandDao {

    @Query("SELECT * FROM errands WHERE status = 'pending' ORDER BY createdAt DESC")
    fun getAvailableErrands(): Flow<List<ErrandEntity>>

    @Query("SELECT * FROM errands WHERE requesterId = :userId ORDER BY createdAt DESC")
    fun getUserRequestedErrands(userId: String): Flow<List<ErrandEntity>>

    @Query("SELECT * FROM errands WHERE providerId = :userId ORDER BY createdAt DESC")
    fun getUserAcceptedErrands(userId: String): Flow<List<ErrandEntity>>

    @Query("SELECT * FROM errands WHERE id = :errandId")
    suspend fun getErrandById(errandId: String): ErrandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrand(errand: ErrandEntity)

    @Update
    suspend fun updateErrand(errand: ErrandEntity)

    @Delete
    suspend fun deleteErrand(errand: ErrandEntity)
}
