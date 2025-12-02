/**
 * Firebase Cloud Functions for My Nottingham
 *
 * Sends push notifications when new messages are created
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.database();

/**
 * Trigger: When a new message is added to a conversation
 * Path: conversations/{conversationId}/messages/{messageId}
 *
 * This function:
 * 1. Gets the new message data
 * 2. Finds all participants of the conversation
 * 3. Sends FCM notification to each participant (except sender)
 */
exports.sendMessageNotification = functions
    .region("asia-southeast1") // Same region as your database
    .database.ref("/conversations/{conversationId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const { conversationId, messageId } = context.params;
        const messageData = snapshot.val();

        if (!messageData) {
            console.log("No message data found");
            return null;
        }

        const { senderId, senderName, message } = messageData;

        console.log(`New message in ${conversationId} from ${senderName}: ${message}`);

        try {
            // Get conversation participants
            // Try multiple paths (different data structures)
            let participants = [];

            // Path 1: conversations/{id}/participants
            const participantsSnap = await db.ref(`conversations/${conversationId}/participants`).once("value");
            if (participantsSnap.exists()) {
                participants = Object.keys(participantsSnap.val());
            }

            // Path 2: conversations/{id}/metadata/participants
            if (participants.length === 0) {
                const metadataSnap = await db.ref(`conversations/${conversationId}/metadata/participants`).once("value");
                if (metadataSnap.exists()) {
                    participants = Object.keys(metadataSnap.val());
                }
            }

            // Path 3: Parse from conversationId (format: userId1_userId2)
            if (participants.length === 0 && conversationId.includes("_")) {
                participants = conversationId.split("_");
            }

            console.log(`Participants: ${participants.join(", ")}`);

            if (participants.length === 0) {
                console.log("No participants found for conversation");
                return null;
            }

            // Send notification to each participant (except sender)
            const notificationPromises = participants
                .filter(uid => uid !== senderId)
                .map(async (recipientId) => {
                    // Get recipient's FCM token
                    const tokenSnap = await db.ref(`users/${recipientId}/fcmToken`).once("value");
                    const fcmToken = tokenSnap.val();

                    if (!fcmToken) {
                        console.log(`No FCM token for user ${recipientId}`);
                        return null;
                    }

                    // Build notification payload (WhatsApp style)
                    const payload = {
                        // Data payload - handled by app even in background
                        data: {
                            type: "message",
                            conversationId: conversationId,
                            senderId: senderId,
                            senderName: senderName || "Someone",
                            body: message || "New message"
                        },
                        // Android specific config
                        android: {
                            priority: "high",
                            notification: {
                                channelId: "messages",
                                icon: "ic_notification",
                                color: "#6750A4"
                            }
                        },
                        token: fcmToken
                    };

                    console.log(`Sending notification to ${recipientId}`);
                    return admin.messaging().send(payload);
                });

            const results = await Promise.all(notificationPromises);
            console.log(`Sent ${results.filter(r => r).length} notifications`);

            return results;
        } catch (error) {
            console.error("Error sending notification:", error);
            return null;
        }
    });

/**
 * Optional: Clean up old typing indicators
 * Runs every minute to remove stale typing status
 */
exports.cleanupTypingIndicators = functions
    .region("asia-southeast1")
    .pubsub.schedule("every 1 minutes")
    .onRun(async (context) => {
        const now = Date.now();
        const staleThreshold = 5000; // 5 seconds

        try {
            const conversationsSnap = await db.ref("conversations").once("value");

            const cleanupPromises = [];
            conversationsSnap.forEach(convSnap => {
                const typingSnap = convSnap.child("typing");
                if (typingSnap.exists()) {
                    typingSnap.forEach(userTyping => {
                        const timestamp = userTyping.child("timestamp").val();
                        if (timestamp && (now - timestamp > staleThreshold)) {
                            cleanupPromises.push(userTyping.ref.remove());
                        }
                    });
                }
            });

            await Promise.all(cleanupPromises);
            console.log(`Cleaned up ${cleanupPromises.length} stale typing indicators`);
        } catch (error) {
            console.error("Error cleaning typing indicators:", error);
        }

        return null;
    });
