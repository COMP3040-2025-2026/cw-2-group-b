package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.ForumCategory
import com.nottingham.mynottingham.data.model.ForumComment
import com.nottingham.mynottingham.data.model.ForumPost
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Forum data models
 *
 * Tests the forum system data classes and enums:
 * - ForumPost data class
 * - ForumComment data class
 * - ForumCategory enum
 */
class ForumModelTest {

    /**
     * Test ForumCategory enum has all expected values
     */
    @Test
    fun `ForumCategory enum has all expected values`() {
        val categories = ForumCategory.values()

        assertEquals("Should have 9 forum categories", 9, categories.size)
        assertTrue("Should contain ACADEMIC", categories.contains(ForumCategory.ACADEMIC))
        assertTrue("Should contain EVENTS", categories.contains(ForumCategory.EVENTS))
        assertTrue("Should contain SPORTS", categories.contains(ForumCategory.SPORTS))
        assertTrue("Should contain SOCIAL", categories.contains(ForumCategory.SOCIAL))
        assertTrue("Should contain GENERAL", categories.contains(ForumCategory.GENERAL))
        assertTrue("Should contain ANNOUNCEMENTS", categories.contains(ForumCategory.ANNOUNCEMENTS))
        assertTrue("Should contain QUESTIONS", categories.contains(ForumCategory.QUESTIONS))
        assertTrue("Should contain CAREER", categories.contains(ForumCategory.CAREER))
        assertTrue("Should contain FOOD", categories.contains(ForumCategory.FOOD))
    }

    /**
     * Test ForumCategory display names
     */
    @Test
    fun `ForumCategory has correct display names`() {
        assertEquals("Academic", ForumCategory.ACADEMIC.displayName)
        assertEquals("Events", ForumCategory.EVENTS.displayName)
        assertEquals("Sports", ForumCategory.SPORTS.displayName)
        assertEquals("Social", ForumCategory.SOCIAL.displayName)
        assertEquals("General", ForumCategory.GENERAL.displayName)
        assertEquals("Announcements", ForumCategory.ANNOUNCEMENTS.displayName)
        assertEquals("Questions", ForumCategory.QUESTIONS.displayName)
        assertEquals("Career", ForumCategory.CAREER.displayName)
        assertEquals("Food & Dining", ForumCategory.FOOD.displayName)
    }

    /**
     * Test ForumCategory valueOf
     */
    @Test
    fun `ForumCategory valueOf returns correct enum`() {
        assertEquals(ForumCategory.ACADEMIC, ForumCategory.valueOf("ACADEMIC"))
        assertEquals(ForumCategory.EVENTS, ForumCategory.valueOf("EVENTS"))
        assertEquals(ForumCategory.SPORTS, ForumCategory.valueOf("SPORTS"))
        assertEquals(ForumCategory.GENERAL, ForumCategory.valueOf("GENERAL"))
    }

    /**
     * Test ForumPost creation with required fields
     */
    @Test
    fun `ForumPost creation with required fields`() {
        val post = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John Doe",
            category = "GENERAL",
            title = "Test Post Title",
            content = "This is the content of the test post.",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        assertEquals("post123", post.id)
        assertEquals("user456", post.authorId)
        assertEquals("John Doe", post.authorName)
        assertEquals("GENERAL", post.category)
        assertEquals("Test Post Title", post.title)
        assertEquals("This is the content of the test post.", post.content)
    }

    /**
     * Test ForumPost default values
     */
    @Test
    fun `ForumPost has correct default values`() {
        val post = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John Doe",
            category = "GENERAL",
            title = "Test",
            content = "Content",
            createdAt = 0L,
            updatedAt = 0L
        )

        assertNull("authorAvatar should be null by default", post.authorAvatar)
        assertNull("imageUrl should be null by default", post.imageUrl)
        assertNull("tags should be null by default", post.tags)
        assertFalse("isPinned should be false by default", post.isPinned)
        assertNull("pinnedAt should be null by default", post.pinnedAt)
        assertEquals("likes should be 0 by default", 0, post.likes)
        assertEquals("comments should be 0 by default", 0, post.comments)
        assertEquals("views should be 0 by default", 0, post.views)
        assertFalse("isLiked should be false by default", post.isLiked)
    }

    /**
     * Test ForumPost with all optional fields
     */
    @Test
    fun `ForumPost with all optional fields`() {
        val post = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John Doe",
            authorAvatar = "avatar1",
            category = "ACADEMIC",
            title = "Academic Discussion",
            content = "Let's discuss algorithms.",
            imageUrl = "https://example.com/image.jpg",
            tags = listOf("programming", "algorithms", "study"),
            isPinned = true,
            pinnedAt = 1000L,
            likes = 42,
            comments = 15,
            views = 200,
            isLiked = true,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals("avatar1", post.authorAvatar)
        assertEquals("https://example.com/image.jpg", post.imageUrl)
        assertEquals(3, post.tags?.size)
        assertTrue(post.tags!!.contains("programming"))
        assertTrue(post.isPinned)
        assertEquals(1000L, post.pinnedAt)
        assertEquals(42, post.likes)
        assertEquals(15, post.comments)
        assertEquals(200, post.views)
        assertTrue(post.isLiked)
    }

    /**
     * Test ForumPost with different categories
     */
    @Test
    fun `ForumPost with different categories`() {
        val academicPost = ForumPost(
            id = "1", authorId = "u1", authorName = "User1",
            category = ForumCategory.ACADEMIC.name,
            title = "Study Tips", content = "Content",
            createdAt = 0L, updatedAt = 0L
        )

        val eventsPost = ForumPost(
            id = "2", authorId = "u2", authorName = "User2",
            category = ForumCategory.EVENTS.name,
            title = "Club Event", content = "Content",
            createdAt = 0L, updatedAt = 0L
        )

        assertEquals("ACADEMIC", academicPost.category)
        assertEquals("EVENTS", eventsPost.category)
    }

    /**
     * Test ForumPost data class equality
     */
    @Test
    fun `ForumPost data class equality`() {
        val post1 = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John",
            category = "GENERAL",
            title = "Test",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val post2 = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John",
            category = "GENERAL",
            title = "Test",
            content = "Content",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals("Posts with same data should be equal", post1, post2)
    }

    /**
     * Test ForumPost copy function
     */
    @Test
    fun `ForumPost copy function works correctly`() {
        val original = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John",
            category = "GENERAL",
            title = "Original Title",
            content = "Original Content",
            likes = 5,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val updated = original.copy(title = "Updated Title", likes = 10, updatedAt = 2000L)

        assertEquals("post123", updated.id)
        assertEquals("Updated Title", updated.title)
        assertEquals(10, updated.likes)
        assertEquals(2000L, updated.updatedAt)
        assertEquals("Original Title", original.title)
    }

    /**
     * Test ForumComment creation with required fields
     */
    @Test
    fun `ForumComment creation with required fields`() {
        val comment = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane Doe",
            content = "This is a great post!",
            createdAt = System.currentTimeMillis()
        )

        assertEquals("comment123", comment.id)
        assertEquals("post456", comment.postId)
        assertEquals("user789", comment.authorId)
        assertEquals("Jane Doe", comment.authorName)
        assertEquals("This is a great post!", comment.content)
    }

    /**
     * Test ForumComment default values
     */
    @Test
    fun `ForumComment has correct default values`() {
        val comment = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane",
            content = "Comment",
            createdAt = 0L
        )

        assertNull("authorAvatar should be null by default", comment.authorAvatar)
        assertEquals("likes should be 0 by default", 0, comment.likes)
        assertFalse("isLiked should be false by default", comment.isLiked)
        assertFalse("isPinned should be false by default", comment.isPinned)
        assertNull("pinnedAt should be null by default", comment.pinnedAt)
    }

    /**
     * Test ForumComment with all optional fields
     */
    @Test
    fun `ForumComment with all optional fields`() {
        val comment = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane Doe",
            authorAvatar = "avatar2",
            content = "Excellent content!",
            likes = 25,
            isLiked = true,
            isPinned = true,
            pinnedAt = 1000L,
            createdAt = 1000L
        )

        assertEquals("avatar2", comment.authorAvatar)
        assertEquals(25, comment.likes)
        assertTrue(comment.isLiked)
        assertTrue(comment.isPinned)
        assertEquals(1000L, comment.pinnedAt)
    }

    /**
     * Test ForumComment data class equality
     */
    @Test
    fun `ForumComment data class equality`() {
        val comment1 = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane",
            content = "Comment",
            createdAt = 1000L
        )

        val comment2 = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane",
            content = "Comment",
            createdAt = 1000L
        )

        assertEquals("Comments with same data should be equal", comment1, comment2)
    }

    /**
     * Test ForumComment copy function
     */
    @Test
    fun `ForumComment copy function works correctly`() {
        val original = ForumComment(
            id = "comment123",
            postId = "post456",
            authorId = "user789",
            authorName = "Jane",
            content = "Original",
            likes = 5,
            createdAt = 1000L
        )

        val updated = original.copy(content = "Updated content", likes = 10)

        assertEquals("comment123", updated.id)
        assertEquals("Updated content", updated.content)
        assertEquals(10, updated.likes)
        assertEquals("Original", original.content)
    }

    /**
     * Test ForumPost pinned functionality
     */
    @Test
    fun `ForumPost pinned functionality`() {
        val regularPost = ForumPost(
            id = "1", authorId = "u1", authorName = "User",
            category = "GENERAL", title = "Regular Post", content = "Content",
            isPinned = false, pinnedAt = null,
            createdAt = 0L, updatedAt = 0L
        )

        val pinnedPost = ForumPost(
            id = "2", authorId = "u2", authorName = "Admin",
            category = "ANNOUNCEMENTS", title = "Important Announcement", content = "Content",
            isPinned = true, pinnedAt = 1000L,
            createdAt = 0L, updatedAt = 0L
        )

        assertFalse(regularPost.isPinned)
        assertNull(regularPost.pinnedAt)
        assertTrue(pinnedPost.isPinned)
        assertNotNull(pinnedPost.pinnedAt)
    }

    /**
     * Test ForumPost tags list
     */
    @Test
    fun `ForumPost tags list operations`() {
        val postWithTags = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John",
            category = "ACADEMIC",
            title = "Programming Tips",
            content = "Content",
            tags = listOf("kotlin", "android", "programming"),
            createdAt = 0L,
            updatedAt = 0L
        )

        val postWithoutTags = ForumPost(
            id = "post456",
            authorId = "user789",
            authorName = "Jane",
            category = "GENERAL",
            title = "General Post",
            content = "Content",
            tags = null,
            createdAt = 0L,
            updatedAt = 0L
        )

        assertNotNull(postWithTags.tags)
        assertEquals(3, postWithTags.tags?.size)
        assertTrue(postWithTags.tags!!.contains("kotlin"))
        assertNull(postWithoutTags.tags)
    }

    /**
     * Test ForumPost engagement metrics
     */
    @Test
    fun `ForumPost engagement metrics`() {
        val popularPost = ForumPost(
            id = "post123",
            authorId = "user456",
            authorName = "John",
            category = "GENERAL",
            title = "Popular Post",
            content = "Content",
            likes = 100,
            comments = 50,
            views = 500,
            createdAt = 0L,
            updatedAt = 0L
        )

        assertEquals(100, popularPost.likes)
        assertEquals(50, popularPost.comments)
        assertEquals(500, popularPost.views)
        assertTrue(popularPost.likes > 0)
        assertTrue(popularPost.views > popularPost.likes)
    }

    /**
     * Test ForumComment belongs to post
     */
    @Test
    fun `ForumComment belongs to specific post`() {
        val comment1 = ForumComment(
            id = "c1", postId = "post123",
            authorId = "u1", authorName = "User1",
            content = "Comment 1", createdAt = 1000L
        )

        val comment2 = ForumComment(
            id = "c2", postId = "post123",
            authorId = "u2", authorName = "User2",
            content = "Comment 2", createdAt = 2000L
        )

        val comment3 = ForumComment(
            id = "c3", postId = "post456",
            authorId = "u3", authorName = "User3",
            content = "Comment 3", createdAt = 3000L
        )

        assertEquals(comment1.postId, comment2.postId)
        assertNotEquals(comment1.postId, comment3.postId)
    }
}
