package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Area
import com.example.data.model.Item
import com.example.data.model.Tag
import com.example.data.model.ItemTag
import com.example.data.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // ITEMS (Single Table Inheritance: type 0=task, 1=project, 2=heading, 3=template)
    @Query("SELECT * FROM items ORDER BY sortOrder ASC, creationDate DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY sortOrder ASC, creationDate DESC")
    suspend fun getAllItemsSync(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("SELECT * FROM items WHERE googleTaskId = :googleTaskId")
    suspend fun getItemByGoogleTaskId(googleTaskId: String): Item?

    @Query("SELECT * FROM items WHERE googleTaskId IS NULL AND type = 0")
    suspend fun getUnsyncedTasks(): List<Item>

    @Query("SELECT * FROM items WHERE googleTaskListId IS NULL AND type = 1")
    suspend fun getUnsyncedProjects(): List<Item>

    // AREAS
    @Query("SELECT * FROM areas ORDER BY sortOrder ASC")
    fun getAllAreasFlow(): Flow<List<Area>>

    @Query("SELECT * FROM areas ORDER BY sortOrder ASC")
    suspend fun getAllAreas(): List<Area>

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getAreaById(id: String): Area?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: Area)

    @Delete
    suspend fun deleteArea(area: Area)

    // CHECKLIST ITEMS
    @Query("SELECT * FROM checklist_items ORDER BY sortOrder ASC")
    fun getAllChecklistItemsFlow(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items ORDER BY sortOrder ASC")
    suspend fun getAllChecklistItems(): List<ChecklistItem>

    @Query("SELECT * FROM checklist_items WHERE itemId = :itemId ORDER BY sortOrder ASC")
    suspend fun getChecklistItemsByItemId(itemId: String): List<ChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(checklistItem: ChecklistItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(checklistItems: List<ChecklistItem>)

    @Query("DELETE FROM checklist_items WHERE itemId = :itemId")
    suspend fun deleteChecklistItemsByItemId(itemId: String)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: String)

    @Query("DELETE FROM checklist_items WHERE itemId = :itemId AND id NOT IN (:keptIds)")
    suspend fun deleteRemovedChecklistItems(itemId: String, keptIds: List<String>)

    @Query("UPDATE items SET checklistItemsCount = :total, openChecklistItemsCount = :open WHERE id = :itemId")
    suspend fun updateChecklistCounters(itemId: String, total: Int, open: Int)

    @Query("SELECT COUNT(*) FROM checklist_items WHERE itemId = :itemId")
    suspend fun getTotalChecklistCount(itemId: String): Int

    @Query("SELECT COUNT(*) FROM checklist_items WHERE itemId = :itemId AND isCompleted = 0")
    suspend fun getOpenChecklistCount(itemId: String): Int

    // TAGS & ITEM TAGS
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC")
    fun getAllTagsFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE parentId IS NULL ORDER BY sortOrder ASC")
    fun observeGroups(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun observeByParent(parentId: String): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC")
    suspend fun getAllTags(): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: String): Tag?

    @Query("SELECT t.* FROM tags t INNER JOIN item_tags it ON t.id = it.tagId WHERE it.itemId = :itemId ORDER BY t.sortOrder ASC")
    suspend fun getTagsByItemId(itemId: String): List<Tag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemTag(itemTag: ItemTag)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId")
    suspend fun deleteItemTagsByItemId(itemId: String)

    @Query("SELECT itemId FROM item_tags WHERE tagId = :tagId")
    suspend fun getItemIdsByTagId(tagId: String): List<String>
}
